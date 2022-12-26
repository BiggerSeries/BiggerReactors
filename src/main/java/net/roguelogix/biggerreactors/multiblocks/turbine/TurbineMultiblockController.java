package net.roguelogix.biggerreactors.multiblocks.turbine;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import net.roguelogix.biggerreactors.Config;
import net.roguelogix.biggerreactors.multiblocks.turbine.blocks.TurbineBaseBlock;
import net.roguelogix.biggerreactors.multiblocks.turbine.blocks.TurbineRotorBlade;
import net.roguelogix.biggerreactors.multiblocks.turbine.blocks.TurbineRotorShaft;
import net.roguelogix.biggerreactors.multiblocks.turbine.simulation.ITurbineSimulation;
import net.roguelogix.biggerreactors.multiblocks.turbine.simulation.modern.ModernTurbineSimulation;
import net.roguelogix.biggerreactors.multiblocks.turbine.state.TurbineActivity;
import net.roguelogix.biggerreactors.multiblocks.turbine.state.TurbineState;
import net.roguelogix.biggerreactors.multiblocks.turbine.state.VentState;
import net.roguelogix.biggerreactors.multiblocks.turbine.tiles.*;
import net.roguelogix.biggerreactors.registries.TurbineCoilRegistry;
import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.debug.DebugInfo;
import net.roguelogix.phosphophyllite.multiblock.ValidationError;
import net.roguelogix.phosphophyllite.multiblock.rectangular.RectangularMultiblockController;
import net.roguelogix.phosphophyllite.util.Util;
import org.joml.*;

import javax.annotation.Nonnull;
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
        maxSize.set(Config.CONFIG.Turbine.MaxLength, Config.CONFIG.Turbine.MaxHeight, Config.CONFIG.Turbine.MaxWidth);
        frameValidator = block -> false;
        exteriorValidator = frameValidator;
        interiorValidator = block -> block.defaultBlockState().isAir() || TurbineCoilRegistry.isBlockAllowed(block);
        validationStartedCallback = () -> {
            foundShafts = 0;
            foundBlades = 0;
        };
        blockValidatedCallback = block -> {
            if (block == TurbineRotorShaft.INSTANCE) {
                foundShafts++;
            }
            if (block == TurbineRotorBlade.INSTANCE) {
                foundBlades++;
            }
        };
        setAssemblyValidator(TurbineMultiblockController::validate);
    }
    
    private boolean validate() {
        if (foundShafts != rotorShafts.size()) {
            throw new ValidationError("multiblock.error.biggerreactors.dangling_internal_part");
        }
        if (foundBlades != attachedBladeCount) {
            throw new ValidationError("multiblock.error.biggerreactors.dangling_internal_part");
        }
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
        {
            int x = bearingPosition.getX();
            int y = bearingPosition.getY();
            int z = bearingPosition.getZ();
            final int marchX = marchDirection.getNormal().getX();
            final int marchY = marchDirection.getNormal().getY();
            final int marchZ = marchDirection.getNormal().getZ();
            marchedBlocks--;
            do {
                x += marchX;
                y += marchY;
                z += marchZ;
                marchedBlocks++;
            } while (blocks.getTile(x, y, z) instanceof TurbineRotorShaftTile);
            if (rotorShafts.size() != marchedBlocks) {
                throw new ValidationError("multiblock.error.biggerreactors.turbine.rotor_shaft_off_shaft");
            }
            if (!(blocks.getTile(x, y, z) instanceof TurbineRotorBearingTile)) {
                throw new ValidationError("multiblock.error.biggerreactors.turbine.rotor_shaft_bearing_ends");
            }
        }
        
        marchedBlocks = 0;
        
        final Vec3i[] bladeDirections = new Vec3i[4];
        
        {
            int i = 0;
            for (Direction value : Direction.values()) {
                if (value != marchDirection && value != marchDirection.getOpposite()) {
                    bladeDirections[i++] = value.getNormal();
                }
            }
        }
        
        
        for (int i = 0; i < rotorShafts.size(); i++) {
            final var rotorShaftPos = rotorShafts.get(i).getBlockPos();
            for (int j = 0; j < 4; j++) {
                int x = rotorShaftPos.getX();
                int y = rotorShaftPos.getY();
                int z = rotorShaftPos.getZ();
                final var bladeDirection = bladeDirections[j];
                final int marchX = bladeDirection.getX();
                final int marchY = bladeDirection.getY();
                final int marchZ = bladeDirection.getZ();
                marchedBlocks--;
                do {
                    x += marchX;
                    y += marchY;
                    z += marchZ;
                    marchedBlocks++;
                } while (blocks.getTile(x, y, z) instanceof TurbineRotorBladeTile);
            }
        }
        
        if (marchedBlocks != attachedBladeCount) {
            throw new ValidationError("multiblock.error.biggerreactors.turbine.rotor_blade_off_blade");
        }
        
        Vector3i sliceMin = new Vector3i(minCoord()).add(1, 1, 1);
        Vector3i sliceMax = new Vector3i(maxCoord()).sub(1, 1, 1);
        
        final int layerCount = rotorShafts.size();
        int[] layerFlags = new int[layerCount];
        final int axisComponent = marchDirection.getAxis().choose(0, 1, 2);
        final int axisPosOffset = sliceMin.get(axisComponent);
        
        Util.chunkCachedBlockStateIteration(sliceMin, sliceMax, world, (state, pos) -> {
            if (state.isAir()) {
                return;
            }
            Block block = state.getBlock();
            if (block == TurbineRotorShaft.INSTANCE) {
                return;
            }
            int axisPos = pos.get(axisComponent);
            int layerIndex = axisPos - axisPosOffset;
            if (block == TurbineRotorBlade.INSTANCE) {
                layerFlags[layerIndex] |= 1;
            } else {
                layerFlags[layerIndex] |= 2;
            }
        });
        
        boolean inBlades = false;
        boolean bladesLower = false;
        int switches = 0;
        for (int i = 0; i < layerCount; i++) {
            switch (layerFlags[i]) {
                case 0 -> {
                }
                case 1 -> {
                    if (!inBlades && switches >= 2) {
                        throw new ValidationError("multiblock.error.biggerreactors.turbine.multiple_groups");
                    }
                    if (!inBlades) {
                        switches++;
                        bladesLower = true;
                    }
                    inBlades = true;
                }
                case 2 -> {
                    if (inBlades && switches >= 2) {
                        throw new ValidationError("multiblock.error.biggerreactors.turbine.multiple_groups");
                    }
                    if (inBlades) {
                        switches++;
                        bladesLower = false;
                    }
                    inBlades = false;
                }
                case 3 -> throw new ValidationError("multiblock.error.biggerreactors.turbine.mixed_blades_and_coil");
            }
        }
        
        int primaryAxisPos = primaryBearing.getBlockPos().get(marchDirection.getAxis());
        int secondaryAxisPos = secondaryBearing.getBlockPos().get(marchDirection.getAxis());
        if ((primaryAxisPos < secondaryAxisPos) ^ bladesLower) {
            primaryBearing.isRenderBearing = true;
            secondaryBearing.isRenderBearing = false;
        } else {
            primaryBearing.isRenderBearing = false;
            secondaryBearing.isRenderBearing = true;
        }
        
        if (switches <= 1) {
            if (primaryAxisPos > secondaryAxisPos) {
                primaryBearing.isRenderBearing = true;
                secondaryBearing.isRenderBearing = false;
            } else {
                primaryBearing.isRenderBearing = false;
                secondaryBearing.isRenderBearing = true;
            }
        }
        
        return true;
    }
    
    private int foundShafts = 0;
    private int foundBlades = 0;
    
    private boolean updateBlockStates = false;
    
    private final Set<TurbineTerminalTile> terminals = new HashSet<>();
    private final Set<TurbineFluidPortTile> fluidPorts = new HashSet<>();
    private final Set<TurbineRotorBearingTile> rotorBearings = new HashSet<>();
    private final ObjectArrayList<TurbineRotorShaftTile> rotorShafts = new ObjectArrayList<>();
    private int attachedBladeCount = 0;
    private final Set<TurbinePowerTapTile> powerTaps = new HashSet<>();
    private long glassCount = 0;
    
    @Override
    protected void onPartPlaced(TurbineBaseTile placed) {
        onPartAttached(placed);
    }
    
    @Override
    protected void onPartAttached(TurbineBaseTile tile) {
        if (tile instanceof TurbineTerminalTile) {
            terminals.add((TurbineTerminalTile) tile);
        }
        if (tile instanceof TurbineFluidPortTile) {
            fluidPorts.add((TurbineFluidPortTile) tile);
        }
        if (tile instanceof TurbineRotorBearingTile) {
            rotorBearings.add((TurbineRotorBearingTile) tile);
        }
        if (tile instanceof TurbineRotorShaftTile) {
            rotorShafts.add((TurbineRotorShaftTile) tile);
        }
        if (tile instanceof TurbineRotorBladeTile) {
//            rotorBlades.add((TurbineRotorBladeTile) tile);
            attachedBladeCount++;
        }
        if (tile instanceof TurbinePowerTapTile) {
            powerTaps.add((TurbinePowerTapTile) tile);
        }
        if (tile instanceof TurbineGlassTile) {
            glassCount++;
        }
    }
    
    @Override
    protected void onPartBroken(TurbineBaseTile broken) {
        onPartDetached(broken);
    }
    
    @Override
    protected void onPartDetached(TurbineBaseTile tile) {
        if (tile instanceof TurbineTerminalTile) {
            terminals.remove(tile);
        }
        if (tile instanceof TurbineFluidPortTile) {
            fluidPorts.remove(tile);
        }
        if (tile instanceof TurbineRotorBearingTile) {
            rotorBearings.remove(tile);
        }
        if (tile instanceof TurbineRotorShaftTile) {
            int index = rotorShafts.indexOf(tile);
            final var endFuelRod = rotorShafts.pop();
            if (index != rotorShafts.size()) {
                rotorShafts.set(index, endFuelRod);
            }
        }
        if (tile instanceof TurbineRotorBladeTile) {
//            rotorBlades.remove(tile);
            attachedBladeCount--;
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
    
    private final ITurbineSimulation simulation = createSimulation();
    
    private static ITurbineSimulation createSimulation() {
        return new ModernTurbineSimulation();
    }
    
    public ITurbineSimulation simulation() {
        return simulation;
    }
    
    @Override
    protected void onAssembled() {
        simulation.reset();
    }
    
    @Override
    protected void onValidationPassed() {
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
    protected void onDisassembled() {
        for (TurbineRotorBearingTile rotorBearing : rotorBearings) {
            world.sendBlockUpdated(rotorBearing.getBlockPos(), rotorBearing.getBlockState(), rotorBearing.getBlockState(), 0);
        }
    }
    
    @Override
    public void tick() {
        
        if (updateBlockStates) {
            updateBlockStates = false;
            updateBlockStates();
        }
        
        simulation.tick();
        
        long totalPowerRequested = 0;
        long stored = simulation.battery().stored();
        for (TurbinePowerTapTile powerPort : powerTaps) {
            long requested = powerPort.distributePower(stored, true);
            if(requested > stored){
                // bugged impl, ignoring
                continue;
            }
            totalPowerRequested += requested;
        }
        long startingPower = simulation.battery().stored();
        
        double distributionMultiplier = Math.min(1f, (double) startingPower / (double) totalPowerRequested);
        for (TurbinePowerTapTile powerPort : powerTaps) {
            long powerRequested = powerPort.distributePower(startingPower, true);
            if(powerRequested > startingPower){
                // bugged impl, ignoring
                continue;
            }
            powerRequested *= distributionMultiplier;
            powerRequested = Math.min(simulation.battery().stored(), powerRequested); // just in case
            simulation.battery().extract(powerPort.distributePower(powerRequested, false));
        }
        
        for (TurbineFluidPortTile coolantPort : fluidPorts) {
            if (simulation.fluidTank().liquidAmount() < 0) {
                break;
            }
            coolantPort.pushFluid();
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
    
    public void updateDataPacket(TurbineState turbineState) {
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
        turbineState.intakeResourceLocation = ForgeRegistries.FLUIDS.getKey(simulation().fluidTank().vaporType()).toString();
        
        turbineState.exhaustStored = simulation.fluidTank().liquidAmount();
        turbineState.exhaustCapacity = simulation.fluidTank().perSideCapacity();
        turbineState.exhaustResourceLocation = ForgeRegistries.FLUIDS.getKey(simulation().fluidTank().liquidType()).toString();
        
        turbineState.energyStored = simulation.battery().stored();
        turbineState.energyCapacity = simulation.battery().capacity();
    }
    
    @SuppressWarnings("UnnecessaryReturnStatement")
    public void runRequest(String requestName, @Nullable Object requestData) {
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
    
    private void setVentState(VentState newVentState) {
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
    
    @Nonnull
    protected CompoundTag write() {
        return simulation().serializeNBT();
    }
    
    protected void read(CompoundTag compound) {
        simulation.deserializeNBT(compound);
        updateBlockStates = true;
    }
    
    public void toggleActive() {
        setActive(!simulation.active());
    }
    
    public void setActive(boolean active) {
        if (simulation.active() != active) {
            simulation.setActive(active);
            updateBlockStates = true;
        }
    }
    
    @Nonnull
    @Override
    public DebugInfo getDebugInfo() {
        return super.getDebugInfo().add(simulation.debugString());
    }
}
