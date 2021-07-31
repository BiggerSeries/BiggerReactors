package net.roguelogix.biggerreactors.multiblocks.turbine;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.roguelogix.biggerreactors.Config;
import net.roguelogix.biggerreactors.multiblocks.turbine.blocks.TurbineBaseBlock;
import net.roguelogix.biggerreactors.multiblocks.turbine.blocks.TurbineRotorBearing;
import net.roguelogix.biggerreactors.multiblocks.turbine.blocks.TurbineRotorBlade;
import net.roguelogix.biggerreactors.multiblocks.turbine.blocks.TurbineRotorShaft;
import net.roguelogix.biggerreactors.multiblocks.turbine.simulation.ITurbineSimulation;
import net.roguelogix.biggerreactors.multiblocks.turbine.simulation.classic.ClassicTurbineSimulation;
import net.roguelogix.biggerreactors.multiblocks.turbine.simulation.modern.ModernTurbineSimulation;
import net.roguelogix.biggerreactors.multiblocks.turbine.state.TurbineActivity;
import net.roguelogix.biggerreactors.multiblocks.turbine.state.TurbineState;
import net.roguelogix.biggerreactors.multiblocks.turbine.state.VentState;
import net.roguelogix.biggerreactors.multiblocks.turbine.tiles.*;
import net.roguelogix.biggerreactors.registries.TurbineCoilRegistry;
import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.multiblock.ValidationError;
import net.roguelogix.phosphophyllite.multiblock.rectangular.RectangularMultiblockController;
import net.roguelogix.phosphophyllite.repack.org.joml.*;
import net.roguelogix.phosphophyllite.util.Util;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.Math;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TurbineMultiblockController extends RectangularMultiblockController<TurbineBaseTile, TurbineMultiblockController> {
    public TurbineMultiblockController(Level world) {
        super(world, tile -> tile instanceof TurbineBaseTile, block -> block instanceof TurbineBaseBlock);
        minSize.set(5, 4, 5);
        maxSize.set(Config.Turbine.MaxLength, Config.Turbine.MaxHeight, Config.Turbine.MaxWidth);
        frameValidator = block -> false;
        exteriorValidator = frameValidator;
        interiorValidator = block -> TurbineCoilRegistry.isBlockAllowed(block) || block instanceof AirBlock;
        setAssemblyValidator(multiblockController -> {
            if (rotorBearings.size() != 2) {
                throw new ValidationError("multiblock.error.biggerreactors.turbine.rotor_bearing_count");
            }
            
            Iterator<TurbineRotorBearingTile> iterator = rotorBearings.iterator();
            TurbineRotorBearingTile primaryBearing = iterator.next();
            TurbineRotorBearingTile secondaryBearing = iterator.next();
            BlockPos bearingPosition = primaryBearing.getBlockPos();
            Direction marchDirection;
            if (bearingPosition.getX() == minCoord().x()) {
                marchDirection = Direction.EAST;
            } else if (bearingPosition.getX() == maxCoord().x()) {
                marchDirection = Direction.WEST;
            } else if (bearingPosition.getY() == minCoord().y()) {
                marchDirection = Direction.UP;
            } else if (bearingPosition.getY() == maxCoord().y()) {
                marchDirection = Direction.DOWN;
            } else if (bearingPosition.getZ() == minCoord().z()) {
                marchDirection = Direction.SOUTH;
            } else if (bearingPosition.getZ() == maxCoord().z()) {
                marchDirection = Direction.NORTH;
            } else {
                throw new ValidationError("multiblock.error.biggerreactors.turbine.rotor_bearing_position_undefined");
            }
            int marchedBlocks = 0;
            BlockPos currentPos = bearingPosition.relative(marchDirection);
            while (world.getBlockState(currentPos).getBlock() instanceof TurbineRotorShaft) {
                currentPos = currentPos.relative(marchDirection);
                marchedBlocks++;
            }
            if (!(world.getBlockState(currentPos).getBlock() instanceof TurbineRotorBearing)) {
                throw new ValidationError("multiblock.error.biggerreactors.turbine.rotor_shaft_bearing_ends");
            }
            
            if (rotorShafts.size() != marchedBlocks) {
                throw new ValidationError("multiblock.error.biggerreactors.turbine.rotor_shaft_off_shaft");
            }
            
            marchedBlocks = 0;
            
            for (TurbineRotorShaftTile rotorShaft : rotorShafts) {
                for (Direction value : Direction.values()) {
                    BlockPos pos = rotorShaft.getBlockPos();
                    while (true) {
                        pos = pos.relative(value);
                        Block block = world.getBlockState(pos).getBlock();
                        if (!(block instanceof TurbineRotorBlade)) {
                            break;
                        }
                        marchedBlocks++;
                    }
                }
            }
            
            if (marchedBlocks != rotorBlades.size()) {
                throw new ValidationError("multiblock.error.biggerreactors.turbine.rotor_blade_off_blade");
            }
            
            boolean inCoil = false;
            boolean inBlades = false;
            boolean switched = false;
            
            final int[] validCoilBlocks = {0};
            
            currentPos = bearingPosition;
            Vector3i sliceMin = new Vector3i(minCoord()).add(1, 1, 1);
            Vector3i sliceMax = new Vector3i(maxCoord()).sub(1, 1, 1);
            int axisComponent = marchDirection.getAxis().choose(0, 1, 2);
            sliceMin.setComponent(axisComponent, marchDirection.getAxisDirection().getStep() < 0 ? sliceMax.get(axisComponent) : sliceMin.get(axisComponent));
            sliceMax.setComponent(axisComponent, sliceMin.get(axisComponent));
            
            boolean[] flags = new boolean[2];
            
            while (true) {
                currentPos = currentPos.relative(marchDirection);
                BlockEntity te = world.getBlockEntity(currentPos);
                if (!(te instanceof TurbineRotorShaftTile)) {
                    break;
                }
                
                flags[0] = false;
                flags[1] = false;
                
                Util.chunkCachedBlockStateIteration(sliceMin, sliceMax, world, (state, pos) -> {
                    Block block = state.getBlock();
                    if (block instanceof AirBlock || block instanceof TurbineRotorShaft) {
                        // shafts and air are ignored
                        return;
                    }
                    if (block instanceof TurbineRotorBlade) {
                        // its a blade, so we have blades on this layer
                        flags[0] = true;
                        return;
                    }
                    // its not air, its not a shaft, it has to be a coil
                    flags[1] = true;
                    validCoilBlocks[0]++;
                });
                
                if (flags[0] && flags[1]) {
                    throw new ValidationError("multiblock.error.biggerreactors.turbine.mixed_blades_and_coil");
                }
                
                if (flags[1]) {
                    if (inBlades) {
                        if (switched) {
                            throw new ValidationError("multiblock.error.biggerreactors.turbine.multiple_groups");
                        }
                        inBlades = false;
                        switched = true;
                        primaryBearing.isRenderBearing = true;
                        secondaryBearing.isRenderBearing = false;
                    }
                    inCoil = true;
                }
                if (flags[0]) {
                    if (inCoil) {
                        if (switched) {
                            throw new ValidationError("multiblock.error.biggerreactors.turbine.multiple_groups");
                        }
                        inCoil = false;
                        switched = true;
                        primaryBearing.isRenderBearing = false;
                        secondaryBearing.isRenderBearing = true;
                    }
                    inBlades = true;
                }
                
                sliceMin.setComponent(axisComponent, sliceMin.get(axisComponent) + marchDirection.getAxisDirection().getStep());
                sliceMax.setComponent(axisComponent, sliceMax.get(axisComponent) + marchDirection.getAxisDirection().getStep());
            }
            if (!switched) {
                primaryBearing.isRenderBearing = true;
                secondaryBearing.isRenderBearing = false;
            }
            
            int[] totalCoilBlocks = new int[]{0};
            
            Util.chunkCachedBlockStateIteration(new Vector3i(1).add(minCoord()), new Vector3i(-1).add(maxCoord()), world, (block, pos) -> {
                if (block.getBlock() instanceof TurbineBaseBlock) {
                    BlockEntity te = world.getBlockEntity(new BlockPos(pos.x, pos.y, pos.z));
                    if (te instanceof TurbineBaseTile) {
                        if (!((TurbineBaseTile) te).isCurrentController(this)) {
                            throw new ValidationError("multiblock.error.biggerreactors.dangling_internal_part");
                        }
                    }
                    return;
                }
                if (block.getBlock() instanceof AirBlock) {
                    return;
                }
                totalCoilBlocks[0]++;
            });
            
            if (totalCoilBlocks[0] != validCoilBlocks[0]) {
                throw new ValidationError("multiblock.error.biggerreactors.turbine.dangling_coil");
            }
            
            return true;
        });
    }
    
    private final Set<TurbineTerminalTile> terminals = new HashSet<>();
    private final Set<TurbineCoolantPortTile> coolantPorts = new HashSet<>();
    private final Set<TurbineRotorBearingTile> rotorBearings = new HashSet<>();
    private final Set<TurbineRotorShaftTile> rotorShafts = new HashSet<>();
    private final Set<TurbineRotorBladeTile> rotorBlades = new HashSet<>();
    private final Set<TurbinePowerTapTile> powerTaps = new HashSet<>();
    private long glassCount = 0;
    
    @Override
    protected void onPartPlaced( TurbineBaseTile placed) {
        onPartAttached(placed);
    }
    
    @Override
    protected void onPartAttached( TurbineBaseTile tile) {
        if (tile instanceof TurbineTerminalTile) {
            terminals.add((TurbineTerminalTile) tile);
        }
        if (tile instanceof TurbineCoolantPortTile) {
            coolantPorts.add((TurbineCoolantPortTile) tile);
        }
        if (tile instanceof TurbineRotorBearingTile) {
            rotorBearings.add((TurbineRotorBearingTile) tile);
        }
        if (tile instanceof TurbineRotorShaftTile) {
            rotorShafts.add((TurbineRotorShaftTile) tile);
        }
        if (tile instanceof TurbineRotorBladeTile) {
            rotorBlades.add((TurbineRotorBladeTile) tile);
        }
        if (tile instanceof TurbinePowerTapTile) {
            powerTaps.add((TurbinePowerTapTile) tile);
        }
        if (tile instanceof TurbineGlassTile) {
            glassCount++;
        }
    }
    
    @Override
    protected void onPartBroken( TurbineBaseTile broken) {
        onPartDetached(broken);
    }
    
    @Override
    protected void onPartDetached( TurbineBaseTile tile) {
        if (tile instanceof TurbineTerminalTile) {
            terminals.remove(tile);
        }
        if (tile instanceof TurbineCoolantPortTile) {
            coolantPorts.remove(tile);
        }
        if (tile instanceof TurbineRotorBearingTile) {
            rotorBearings.remove(tile);
        }
        if (tile instanceof TurbineRotorShaftTile) {
            rotorShafts.remove(tile);
        }
        if (tile instanceof TurbineRotorBladeTile) {
            rotorBlades.remove(tile);
        }
        if (tile instanceof TurbinePowerTapTile) {
            powerTaps.remove(tile);
        }
        if (tile instanceof TurbineGlassTile) {
            glassCount--;
        }
    }
    
    public void updateBlockStates() {
        terminals.forEach(terminal -> {
            world.setBlockAndUpdate(terminal.getBlockPos(), terminal.getBlockState().setValue(TurbineActivity.TURBINE_STATE_ENUM_PROPERTY, simulation.active() ? TurbineActivity.ACTIVE : TurbineActivity.INACTIVE));
            terminal.setChanged();
        });
    }
    
    public final ArrayList<Vector4i> rotorConfiguration = new ArrayList<>();
    public Vec3i rotationAxis = new Vec3i(0, 0, 0);
    
    ITurbineSimulation simulation = createSimulation();
    
    private static ITurbineSimulation createSimulation() {
        switch (Config.mode) {
            case CLASSIC:
                return new ClassicTurbineSimulation();
            case MODERN:
            case EXPERIMENTAL:
            default:
                return new ModernTurbineSimulation();
        }
    }
    
    public ITurbineSimulation simulation() {
        return simulation;
    }
    
    @Override
    protected void onAssembled() {
        onUnpaused();
        simulation.reset();
    }
    
    @Override
    protected void onUnpaused() {
        for (TurbinePowerTapTile powerPort : powerTaps) {
            powerPort.updateOutputDirection();
        }
        
        Vector3i internalVolume = new Vector3i().add(maxCoord()).sub(minCoord()).sub(1, 1, 1);
        
        
        BlockPos bearingPos = rotorBearings.iterator().next().getBlockPos();
        if (bearingPos.getX() == minCoord().x() || bearingPos.getX() == maxCoord().x()) {
            internalVolume.y ^= internalVolume.x;
            internalVolume.x ^= internalVolume.y;
            internalVolume.y ^= internalVolume.x;
        }
        if (bearingPos.getZ() == minCoord().z() || bearingPos.getZ() == maxCoord().z()) {
            internalVolume.y ^= internalVolume.z;
            internalVolume.z ^= internalVolume.y;
            internalVolume.y ^= internalVolume.z;
        }
        
        simulation.resize(internalVolume.x, internalVolume.y, internalVolume.z);
        
        for (TurbineRotorBearingTile rotorBearing : rotorBearings) {
            if (!rotorBearing.isRenderBearing) {
                continue;
            }
            
            for (Direction value : Direction.values()) {
                BlockPos possibleRotorPos = rotorBearing.getBlockPos().relative(value);
                if (world.getBlockState(possibleRotorPos).getBlock() == TurbineRotorShaft.INSTANCE) {
                    
                    rotationAxis = value.getNormal();
                    
                    rotorConfiguration.clear();
                    
                    Direction.Axis shaftAxis = value.getAxis();
                    BlockPos currentRotorPosition = possibleRotorPos;
                    BlockPos currentBladePosition;
                    while (world.getBlockState(currentRotorPosition).getBlock() == TurbineRotorShaft.INSTANCE) {
                        Vector4i shaftSectionConfiguration = new Vector4i();
                        int i = 0;
                        for (Direction bladeDirection : Direction.values()) {
                            if (bladeDirection.getAxis() == shaftAxis) {
                                continue;
                            }
                            
                            int bladeCount = 0;
                            
                            currentBladePosition = currentRotorPosition;
                            currentBladePosition = currentBladePosition.relative(bladeDirection);
                            while (world.getBlockState(currentBladePosition).getBlock() == TurbineRotorBlade.INSTANCE) {
                                bladeCount++;
                                currentBladePosition = currentBladePosition.relative(bladeDirection);
                            }
                            
                            shaftSectionConfiguration.setComponent(i, bladeCount);
                            
                            i++;
                        }
                        
                        rotorConfiguration.add(shaftSectionConfiguration);
                        currentRotorPosition = currentRotorPosition.relative(value);
                    }
                    
                    break;
                }
            }
            
        }
        
        simulation.setRotorConfiguration(rotorConfiguration);
        
        if (glassCount <= 0) {
            for (TurbineRotorBearingTile rotorBearing : rotorBearings) {
                rotorBearing.isRenderBearing = false;
            }
        }
        
        Matrix4f blockToRotorRelativePos = new Matrix4f();
        if (rotationAxis.getY() == -1) {
            blockToRotorRelativePos.rotate((float) (Math.PI), 0, 0, 1);
        } else {
            Vector3f cross = new Vector3f();
            cross.set(rotationAxis.getX(), rotationAxis.getY(), rotationAxis.getZ());
            cross.cross(0, 1, 0);
            cross.normalize();
            blockToRotorRelativePos.rotate((float) (Math.PI / 2.0), cross);
        }
        blockToRotorRelativePos.translate(-bearingPos.getX(), -bearingPos.getY(), -bearingPos.getZ());
        
        Vector4f translationPos = new Vector4f();
        Util.chunkCachedBlockStateIteration(new Vector3i(1).add(minCoord()), new Vector3i(-1).add(maxCoord()), world, (blockState, pos) -> {
            Block block = blockState.getBlock();
            if (block instanceof AirBlock) {
                return;
            }
            TurbineCoilRegistry.CoilData coilData = TurbineCoilRegistry.getCoilData(block);
            if (coilData != null) {
                translationPos.set(pos, 1);
                translationPos.mul(blockToRotorRelativePos);
                simulation.setCoilData((int) translationPos.x, (int) translationPos.z, coilData);
            }
        });
        simulation.updateInternalValues();
    }
    
    @Override
    public void tick() {
        
        simulation.tick();
        
        long totalPowerRequested = 0;
        for (TurbinePowerTapTile powerPort : powerTaps) {
            totalPowerRequested += powerPort.distributePower(simulation.battery().stored(), true);
        }
        long startingPower = simulation.battery().stored();
        
        double distributionMultiplier = Math.min(1f, (double) startingPower / (double) totalPowerRequested);
        for (TurbinePowerTapTile powerPort : powerTaps) {
            long powerRequested = powerPort.distributePower(startingPower, true);
            powerRequested *= distributionMultiplier;
            powerRequested = Math.min(simulation.battery().stored(), powerRequested); // just in case
            simulation.battery().extract(powerPort.distributePower(powerRequested, false));
        }
        
        for (TurbineCoolantPortTile coolantPort : coolantPorts) {
            if (simulation.fluidTank().liquidAmount() < 0) {
                break;
            }
            simulation.fluidTank().drain(Fluids.WATER, null, coolantPort.pushFluid(), false);
        }
        
        if (Phosphophyllite.tickNumber() % 10 == 0) {
            for (TurbineRotorBearingTile rotorBearing : rotorBearings) {
                world.sendBlockUpdated(rotorBearing.getBlockPos(), rotorBearing.getBlockState(), rotorBearing.getBlockState(), 0);
            }
        }
        
        if (Phosphophyllite.tickNumber() % 2 == 0) {
            markDirty();
        }
    }
    
    public void updateDataPacket( TurbineState turbineState) {
        turbineState.turbineActivity = simulation.active() ? TurbineActivity.ACTIVE : TurbineActivity.INACTIVE;
        turbineState.ventState = simulation.ventState();
        turbineState.coilStatus = simulation.coilEngaged();
        
        turbineState.flowRate = simulation.nominalFlowRate();
        turbineState.efficiencyRate = simulation.bladeEfficiencyLastTick();
        turbineState.turbineOutputRate = simulation.FEGeneratedLastTick();
        
        turbineState.currentRPM = simulation.RPM();
        turbineState.maxRPM = 2200.0;
        
        turbineState.intakeStored = simulation.fluidTank().vaporAmount();
        turbineState.intakeCapacity = simulation.fluidTank().perSideCapacity();
        turbineState.intakeResourceLocation = simulation().fluidTank().vaporType().getRegistryName().toString();
        
        turbineState.exhaustStored = simulation.fluidTank().liquidAmount();
        turbineState.exhaustCapacity = simulation.fluidTank().perSideCapacity();
        turbineState.exhaustResourceLocation = simulation().fluidTank().liquidType().getRegistryName().toString();
        
        turbineState.energyStored = simulation.battery().stored();
        turbineState.energyCapacity = simulation.battery().capacity();
    }
    
    @SuppressWarnings("UnnecessaryReturnStatement")
    public void runRequest( String requestName, @Nullable Object requestData) {
        switch (requestName) {
            // Set the turbine to ACTIVE or INACTIVE.
            case "setActive": {
                if (!(requestData instanceof Integer)) {
                    return;
                }
                setActive(TurbineActivity.fromInt((Integer) requestData) == TurbineActivity.ACTIVE);
                return;
            }
            // Change flow rate by value.
            case "changeFlowRate": {
                if (!(requestData instanceof Long)) {
                    return;
                }
                simulation.setNominalFlowRate(simulation.nominalFlowRate() + (Long) requestData);
                return;
            }
            // Set coils to engaged or disengaged.
            case "setCoilEngaged": {
                if (!(requestData instanceof Integer)) {
                    return;
                }
                setCoilEngaged(((Integer) requestData != 0));
                return;
            }
            // Set vent state to OVERFLOW, ALL, or CLOSED.
            case "setVentState": {
                if (!(requestData instanceof Integer)) {
                    return;
                }
                setVentState(VentState.fromInt((int) requestData));
                return;
            }
        }
    }
    
    private void setVentState( VentState newVentState) {
        simulation.setVentState(newVentState);
    }
    
    private void setMaxFlowRate(long flowRate) {
        if (flowRate < 0) {
            flowRate = 0;
        }
        if (flowRate > simulation.flowRateLimit()) {
            flowRate = simulation.flowRateLimit();
        }
        simulation.setNominalFlowRate(flowRate);
    }
    
    private void setCoilEngaged(boolean engaged) {
        simulation.setCoilEngaged(engaged);
    }
    
    
    protected CompoundTag write() {
        return simulation().serializeNBT();
    }
    
    protected void read( CompoundTag compound) {
        simulation.deserializeNBT(compound);
        updateBlockStates();
    }
    
    public void toggleActive() {
        setActive(!simulation.active());
    }
    
    public void setActive(boolean active) {
        if (simulation.active() != active) {
            simulation.setActive(active);
            updateBlockStates();
        }
    }
    
    @Override
    
    public String getDebugString() {
        return super.getDebugString() + "\n" +
                simulation.debugString() +
                "";
    }
}
