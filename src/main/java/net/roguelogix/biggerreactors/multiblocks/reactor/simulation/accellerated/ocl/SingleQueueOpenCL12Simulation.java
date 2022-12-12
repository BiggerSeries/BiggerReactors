package net.roguelogix.biggerreactors.multiblocks.reactor.simulation.accellerated.ocl;

import net.roguelogix.biggerreactors.Config;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.SimulationDescription;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.base.ModeratorCache;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.base.SimUtil;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.cpu.FullPassReactorSimulation;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import static net.roguelogix.biggerreactors.multiblocks.reactor.simulation.accellerated.ocl.CLUtil.*;
import static org.lwjgl.opencl.CL12.*;

public class SingleQueueOpenCL12Simulation extends FullPassReactorSimulation {
    
    private boolean dispatchedLastTick = false;
    
    private final CLUtil clUtil = new CLUtil();
    
    private final long queue;
    private final int totalRayCount;
    private final int raysPerBatch;
    private final int batches;
    
    
    private final long simKernel;
    private final long reductionKernel;
    
    private final long reactorInfoBuffer;
    private final IntBuffer reactorInfoIB;
    private final FloatBuffer reactorInfoFB;
    
    private final long moderatorBuffer;
    private final FloatBuffer moderatorFB;
    
    private final long controlRodInsertionsBuffer;
    private final FloatBuffer controlRodInsertions;
    
    
    private final long rodRayInfoBuffer;
    private final FloatBuffer rodRayInfoFB;
    private final long rayResultsBuffer;
    private final FloatBuffer rayResultsFB;
    
    private final PointerBuffer rayGlobalWorkSize = clUtil.allocPointer(3);
    private final PointerBuffer rayLocalWorkSize = clUtil.allocPointer(3);
    
    private final PointerBuffer rayReductionGlobalWorkSize = clUtil.allocPointer(3);
    
    public SingleQueueOpenCL12Simulation(SimulationDescription simulationDescription) {
        super(simulationDescription);
    
        try (var stack = MemoryStack.stackPush()) {
            final var returnCode = stack.mallocInt(1);
            final var argIntBuffer = stack.mallocInt(1);
            final var argLongBuffer = stack.mallocLong(1);
    
            queue = clUtil.createCommandQueue(CLUtil.nextDevice(), returnCode);
            
    
            totalRayCount = SimUtil.rays.size();
            raysPerBatch = totalRayCount / 8; // TODO: make this variable
            batches = totalRayCount / raysPerBatch;
            
            {
                // TODO: global shared ray buffers
                //       these dont change over the lifetime of a simulation
                
                final long rayBuffer = clUtil.createCLBuffer(CL_MEM_READ_ONLY | CL_MEM_HOST_WRITE_ONLY, totalRayCount * CLUtil.SIZEOF_RAY, returnCode);
                final var mappedRayBuffer = clEnqueueMapBuffer(queue, rayBuffer, true, CL_MAP_WRITE_INVALIDATE_REGION, 0, (long) totalRayCount * 2 * 4, null, null, returnCode, null);
                checkReturnCode(returnCode.get(0));
                if (mappedRayBuffer == null) {
                    throw new IllegalStateException("Memory map failed");
                }
                final var mappedRayIntBuffer = mappedRayBuffer.asIntBuffer();
                final var raySteps = new ArrayList<SimUtil.RayStep>();
    
                for (int i = 0; i < totalRayCount; i++) {
                    final var ray = SimUtil.rays.get(i);
                    mappedRayIntBuffer.put(i * 2, raySteps.size());
                    mappedRayIntBuffer.put(i * 2 + 1, ray.size());
                    raySteps.addAll(ray);
                }
    
                clEnqueueUnmapMemObject(queue, rayBuffer, mappedRayBuffer, null, null);
                
                final long rayStepBuffer = clUtil.createCLBuffer(CL_MEM_READ_ONLY | CL_MEM_HOST_WRITE_ONLY, raySteps.size() * CLUtil.SIZEOF_RAY_STEP, returnCode);
                final var mappedRayStepBuffer = clEnqueueMapBuffer(queue, rayStepBuffer, true, CL_MAP_WRITE_INVALIDATE_REGION, 0, (long) raySteps.size() * 4 * 4, null, null, returnCode, null);
                checkReturnCode(returnCode.get(0));
                if (mappedRayStepBuffer == null) {
                    throw new IllegalStateException("Memory map failed");
                }
                final var mappedRayStepFloatBuffer = mappedRayStepBuffer.asFloatBuffer();
                final var mappedRayStepIntBuffer = mappedRayStepBuffer.asIntBuffer();
    
                for (int i = 0; i < raySteps.size(); i++) {
                    final var step = raySteps.get(i);
                    mappedRayStepFloatBuffer.put(i * 4, (float) step.length);
                    mappedRayStepIntBuffer.put(i * 4 + 1, step.offset.x);
                    mappedRayStepIntBuffer.put(i * 4 + 2, step.offset.y);
                    mappedRayStepIntBuffer.put(i * 4 + 3, step.offset.z);
                }
    
                clEnqueueUnmapMemObject(queue, rayStepBuffer, mappedRayStepBuffer, null, null);
    
                
                simKernel = clUtil.createCLKernel("raySim", returnCode);
                reductionKernel = clUtil.createCLKernel("rayReduction", returnCode);
    
                reactorInfoBuffer = clUtil.createCLBuffer(CL_MEM_READ_ONLY | CL_MEM_HOST_WRITE_ONLY, SIZEOF_REACTOR_INFO, returnCode);
                reactorInfoIB = clUtil.allocInt(9);
                reactorInfoFB = MemoryUtil.memFloatBuffer(MemoryUtil.memAddress(reactorInfoIB), reactorInfoIB.capacity());
    
                final long moderatorIndexBuffer = clUtil.createCLBuffer(CL_MEM_READ_ONLY | CL_MEM_HOST_WRITE_ONLY, (long) x * y * z, returnCode);
                moderatorBuffer = clUtil.createCLBuffer(CL_MEM_READ_ONLY | CL_MEM_HOST_WRITE_ONLY, (long) moderatorCaches.size() * SIZEOF_MODERATOR, returnCode);
                moderatorFB = clUtil.allocFloat(moderatorCaches.size() * 3);
                
                final long controlRodPositionsBuffer = clUtil.createCLBuffer(CL_MEM_READ_ONLY | CL_MEM_HOST_WRITE_ONLY, (long) controlRods.length * 2 * 2, returnCode);
                controlRodInsertionsBuffer = clUtil.createCLBuffer(CL_MEM_READ_ONLY | CL_MEM_HOST_WRITE_ONLY, (long) x * z * 4, returnCode);
                controlRodInsertions = clUtil.allocFloat(x * z);
                for (int i = 0; i < x * z; i++) {
                    controlRodInsertions.put(i, -1.0f);
                }
    
                rodRayInfoBuffer = clUtil.createCLBuffer(CL_MEM_READ_ONLY | CL_MEM_HOST_WRITE_ONLY, (long) controlRods.length * y * batches *  SIZEOF_ROD_RAY_INFO, returnCode);
                rodRayInfoFB = clUtil.allocFloat(controlRods.length * y * batches);
                final long rayResultsIntermediateBuffer = clUtil.createCLBuffer(CL_MEM_READ_WRITE | CL_MEM_HOST_NO_ACCESS, (long) controlRods.length * y * batches *  SIZEOF_RAY_BURN_INFO, returnCode);
                rayResultsBuffer = clUtil.createCLBuffer(CL_MEM_WRITE_ONLY | CL_MEM_HOST_READ_ONLY, (long) controlRods.length *  SIZEOF_RAY_BURN_INFO, returnCode);
                rayResultsFB = clUtil.allocFloat((int) (controlRods.length * (SIZEOF_RAY_BURN_INFO / 4)));
                
                final var mappedModeratorIndexBuffer = clEnqueueMapBuffer(queue, moderatorIndexBuffer, true, CL_MAP_WRITE_INVALIDATE_REGION, 0, (long) x * y * z, null, null, returnCode, null);
                checkReturnCode(returnCode.get(0));
                if (mappedModeratorIndexBuffer == null) {
                    throw new IllegalStateException("Memory map failed");
                }
    
                for (int i = 0; i < x; i++) {
                    for (int j = 0; j < z; j++) {
                        for (int k = 0; k < y; k++) {
                            final int moderatorIndexIndex = (((i * z) + j) * y) + k;
                            mappedModeratorIndexBuffer.put(moderatorIndexIndex, getModeratorIndex(moderatorIndexIndex));
                        }
                    }
                }
    
                clEnqueueUnmapMemObject(queue, moderatorIndexBuffer, mappedModeratorIndexBuffer, null, null);
    
                final var mappedControlRodPositionBuffer = clEnqueueMapBuffer(queue, controlRodPositionsBuffer, true, CL_MAP_WRITE_INVALIDATE_REGION, 0, (long) controlRods.length * 2 * 2, null, null, returnCode, null);
                checkReturnCode(returnCode.get(0));
                if (mappedControlRodPositionBuffer == null) {
                    throw new IllegalStateException("Memory map failed");
                }
                final var mappedControlRodPositionSB = mappedControlRodPositionBuffer.asShortBuffer();
                for (int i = 0; i < controlRods.length; i++) {
                    mappedControlRodPositionSB.put(i * 2, (short) controlRods[i].x);
                    mappedControlRodPositionSB.put(i * 2 + 1, (short) controlRods[i].z);
                }
                clEnqueueUnmapMemObject(queue, controlRodPositionsBuffer, mappedControlRodPositionBuffer, null, null);
    
                rayGlobalWorkSize.put(0, controlRods.length);
                rayGlobalWorkSize.put(1, y);
                rayGlobalWorkSize.put(2, batches);
                rayLocalWorkSize.put(0, 1);
                rayLocalWorkSize.put(1, y); // TODO: this needs to be dynamic based on GPU capabilities, this will cause problems
                rayLocalWorkSize.put(2, 1); // TODO: potentially larger local size based off batch count
    
                rayReductionGlobalWorkSize.put(0, controlRods.length);
    
                // globalBaseRay
                argIntBuffer.put(0, 0);
                clSetKernelArg(simKernel, 0, argIntBuffer);
                // raysPerBatch
                argIntBuffer.put(0, raysPerBatch);
                clSetKernelArg(simKernel, 1, argIntBuffer);
                
                // moderatorIndices
                argLongBuffer.put(0, moderatorIndexBuffer);
                clSetKernelArg(simKernel, 2, argLongBuffer);
                // moderatorsGlobal
                argLongBuffer.put(0, moderatorBuffer);
                clSetKernelArg(simKernel, 3, argLongBuffer);
                // moderatorsLocal
                clSetKernelArg(simKernel, 4, (long) moderatorCaches.size() * SIZEOF_MODERATOR);
                // moderatorCount
                argIntBuffer.put(0, moderatorCaches.size());
                clSetKernelArg(simKernel, 5, argIntBuffer);
                
                // controlRodPositions
                argLongBuffer.put(0, controlRodPositionsBuffer);
                clSetKernelArg(simKernel, 6, argLongBuffer);
                // controlRodInsertions
                argLongBuffer.put(0, controlRodInsertionsBuffer);
                clSetKernelArg(simKernel, 7, argLongBuffer);
                // localInsertions
                clSetKernelArg(simKernel, 8, (long) (Math.pow(Config.CONFIG.Reactor.IrradiationDistance * 2 + 1, 2) * 4));
                
                // rodRayInfosGlobal
                argLongBuffer.put(0, rodRayInfoBuffer);
                clSetKernelArg(simKernel, 9, argLongBuffer);
                // reactorInfoGlobal
                argLongBuffer.put(0, reactorInfoBuffer);
                clSetKernelArg(simKernel, 10, argLongBuffer);
                
                // rays
                argLongBuffer.put(0, rayBuffer);
                clSetKernelArg(simKernel, 11, argLongBuffer);
                // rayStepsGlobal
                argLongBuffer.put(0, rayStepBuffer);
                clSetKernelArg(simKernel, 12, argLongBuffer);
    
                // results
                argLongBuffer.put(0, rayResultsIntermediateBuffer);
                clSetKernelArg(simKernel, 13, argLongBuffer);
                
                
                
                // inputs
                argLongBuffer.put(0, rayResultsIntermediateBuffer);
                clSetKernelArg(reductionKernel, 0, argLongBuffer);
                // outputs
                argLongBuffer.put(0, rayResultsBuffer);
                clSetKernelArg(reductionKernel, 1, argLongBuffer);
                // reduction count
                argIntBuffer.put(0, y * batches);
                clSetKernelArg(reductionKernel, 2, argIntBuffer);
            }
            
            clFlush(queue);
        }
    }
    
    @Override
    public boolean isAsync() {
        return true;
    }
    
    @Override
    protected void startNextRadiate() {
        if (fuelTank.fuel() <= 0) {
            return;
        }
        
        setupIrradiationTick();
        fullPassIrradiationRequest.updateCache();
        for (int i = 0; i < moderatorCaches.size(); i++) {
            var cache = moderatorCaches.get(i);
            moderatorFB.put(i * 3, (float) cache.absorption);
            moderatorFB.put((i * 3) + 1, (float) cache.heatEfficiency);
            moderatorFB.put((i * 3) + 2, (float) cache.moderation);
        }
        checkReturnCode(clEnqueueWriteBuffer(queue, moderatorBuffer, false, 0, moderatorFB, null, null));
        
        for (SimUtil.ControlRod controlRod : controlRods) {
            int linearIndex = controlRod.x * z + controlRod.z;
            controlRodInsertions.put(linearIndex, (float) (controlRod.insertion * 0.01));
        }
        checkReturnCode(clEnqueueWriteBuffer(queue, controlRodInsertionsBuffer, false, 0, controlRodInsertions, null, null));
    
        for (int i = 0; i < controlRods.length; i++) {
            rodRayInfoFB.put(i, (float)initialIntensties[i]);
        }
        checkReturnCode(clEnqueueWriteBuffer(queue, rodRayInfoBuffer, false, 0, rodRayInfoFB, null, null));
        
        reactorInfoIB.put(0, x);
        reactorInfoIB.put(1, y);
        reactorInfoIB.put(2, z);
        reactorInfoFB.put(3, (float) fuelAbsorptionTemperatureCoefficient);
        reactorInfoFB.put(4, (float) initialHardness);
        reactorInfoFB.put(5, (float) Config.CONFIG.Reactor.FEPerRadiationUnit);
        reactorInfoFB.put(6, (float) FuelAbsorptionCoefficient);
        reactorInfoFB.put(7, (float) FuelModerationFactor);
        reactorInfoFB.put(8, (float) fuelHardnessMultiplier);
        checkReturnCode(clEnqueueWriteBuffer(queue, reactorInfoBuffer, false, 0, reactorInfoFB, null, null));
        
        checkReturnCode(clEnqueueNDRangeKernel(queue, simKernel, 3, null, rayGlobalWorkSize, rayLocalWorkSize, null, null));
        checkReturnCode(clEnqueueNDRangeKernel(queue, reductionKernel, 1, null, rayReductionGlobalWorkSize, null, null, null));
    
        dispatchedLastTick = true;
    }
    
    @Override
    protected double radiate() {
        if (dispatchedLastTick) {
            dispatchedLastTick = false;
    
            checkReturnCode(clEnqueueReadBuffer(queue, rayResultsBuffer, true, 0, rayResultsFB, null, null));
    
            fuelRFAdded *= Config.CONFIG.Reactor.FEPerRadiationUnit;
    
            collectResults();
            
            fuelRFAdded /= controlRods.length;
            fuelRadAdded /= controlRods.length;
            caseRFAdded /= controlRods.length;
            
            if (!Double.isNaN(fuelRadAdded)) {
                if (Config.CONFIG.Reactor.fuelRadScalingMultiplier != 0) {
                    fuelRadAdded *= Config.CONFIG.Reactor.fuelRadScalingMultiplier * (Config.CONFIG.Reactor.PerFuelRodCapacity / Math.max(1.0, (double) fuelTank().totalStored()));
                }
                fuelFertility += fuelRadAdded;
            }
            if (!Double.isNaN(fuelRFAdded)) {
                fuelHeat.absorbRF(fuelRFAdded);
            }
            if (!Double.isNaN(caseRFAdded)) {
                stackHeat.absorbRF(caseRFAdded);
            }
    
            fuelRFAdded = 0;
            fuelRadAdded = 0;
            caseRFAdded = 0;
        }
        return rawFuelUsage;
    }
    
    private void collectResults() {
        for (int i = 0; i < controlRods.length; i++) {
            fuelRFAdded += rayResultsFB.get(i * 3) * rayMultiplier;
            fuelRadAdded += rayResultsFB.get(i * 3 + 1) * rayMultiplier;
            caseRFAdded += rayResultsFB.get(i * 3 + 2) * rayMultiplier;
        }
    }
}
