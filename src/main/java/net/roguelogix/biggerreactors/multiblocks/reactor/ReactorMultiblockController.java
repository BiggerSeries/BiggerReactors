package net.roguelogix.biggerreactors.multiblocks.reactor;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.roguelogix.biggerreactors.BiggerReactors;
import net.roguelogix.biggerreactors.Config;
import net.roguelogix.biggerreactors.multiblocks.reactor.blocks.ReactorBaseBlock;
import net.roguelogix.biggerreactors.multiblocks.reactor.blocks.ReactorFuelRod;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.IReactorSimulation;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.classic.ClassicReactorSimulation;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.experimental.MultithreadedReactorSimulation;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.modern.ModernReactorSimulation;
import net.roguelogix.biggerreactors.multiblocks.reactor.state.ReactorActivity;
import net.roguelogix.biggerreactors.multiblocks.reactor.state.ReactorState;
import net.roguelogix.biggerreactors.multiblocks.reactor.state.ReactorType;
import net.roguelogix.biggerreactors.multiblocks.reactor.tiles.*;
import net.roguelogix.biggerreactors.registries.ReactorModeratorRegistry;
import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.multiblock.ValidationError;
import net.roguelogix.phosphophyllite.multiblock.rectangular.RectangularMultiblockController;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3i;
import net.roguelogix.phosphophyllite.util.Util;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ReactorMultiblockController extends RectangularMultiblockController<ReactorBaseTile, ReactorMultiblockController> {
    
    public ReactorMultiblockController(Level world) {
        super(world, tile -> tile instanceof ReactorBaseTile, block -> block instanceof ReactorBaseBlock);
        
        minSize.set(3);
        maxSize.set(Config.Reactor.MaxLength, Config.Reactor.MaxHeight, Config.Reactor.MaxWidth);
        interiorValidator = ReactorModeratorRegistry::isBlockAllowed;
        setAssemblyValidator(genericController -> {
            if (terminals.isEmpty()) {
                throw new ValidationError("multiblock.error.biggerreactors.no_terminal");
            }
            if (controlRods.isEmpty()) {
                throw new ValidationError("multiblock.error.biggerreactors.no_rods");
            }
            if (!powerPorts.isEmpty() && !coolantPorts.isEmpty()) {
                throw new ValidationError("multiblock.error.biggerreactors.coolant_and_power_ports");
            }
            BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
            
            long tick = Phosphophyllite.tickNumber();
            
            for (ReactorControlRodTile controlRod : controlRods) {
                mutableBlockPos.set(controlRod.getBlockPos());
                if (mutableBlockPos.getY() != maxCoord().y()) {
                    throw new ValidationError(new TranslatableComponent("multiblock.error.biggerreactors.control_rod_not_on_top", controlRod.getBlockPos().getX(), controlRod.getBlockPos().getY(), controlRod.getBlockPos().getZ()));
                }
                for (int i = 0; i < maxCoord().y() - minCoord().y() - 1; i++) {
                    mutableBlockPos.move(0, -1, 0);
                    ReactorBaseTile tile = blocks.getTile(mutableBlockPos);
                    if (!(tile instanceof ReactorFuelRodTile)) {
                        throw new ValidationError(new TranslatableComponent("multiblock.error.biggerreactors.fuel_rod_gap", controlRod.getBlockPos().getX(), controlRod.getBlockPos().getY() + (-1 - i), controlRod.getBlockPos().getZ()));
                    }
                    ((ReactorFuelRodTile) tile).lastCheckedTick = tick;
                }
            }
            
            for (ReactorFuelRodTile fuelRod : fuelRods) {
                if (fuelRod.lastCheckedTick != tick) {
                    throw new ValidationError(new TranslatableComponent("multiblock.error.biggerreactors.no_control_rod_for_fuel_rod", fuelRod.getBlockPos().getX(), fuelRod.getBlockPos().getZ()));
                }
            }
            
            ArrayList<ReactorManifoldTile> manifoldsToCheck = new ArrayList<>();
            
            for (ReactorManifoldTile manifold : manifolds) {
                BlockPos pos = manifold.getBlockPos();
                
                if (pos.getX() == minCoord().x() + 1 || pos.getX() == maxCoord().x() - 1 ||
                        pos.getY() == minCoord().y() + 1 || pos.getY() == maxCoord().y() - 1 ||
                        pos.getZ() == minCoord().z() + 1 || pos.getZ() == maxCoord().z() - 1) {
                    for (Direction value : Direction.values()) {
                        mutableBlockPos.set(pos);
                        mutableBlockPos.move(value);
                        BlockEntity edgeTile = blocks.getTile(mutableBlockPos);
                        if (edgeTile == null) {
                            continue;
                        }
                        if (!(edgeTile instanceof ReactorGlassTile) && ((ReactorBaseBlock) edgeTile.getBlockState().getBlock()).isGoodForExterior()) {
                            manifoldsToCheck.add(manifold);
                            manifold.lastCheckedTick = tick;
                            break;
                        }
                    }
                }
            }
            
            while (!manifoldsToCheck.isEmpty()) {
                // done like this to avoid array shuffling
                ReactorManifoldTile manifoldTile = manifoldsToCheck.remove(manifoldsToCheck.size() - 1);
                manifoldTile.lastCheckedTick = tick;
                for (Direction value : Direction.values()) {
                    mutableBlockPos.set(manifoldTile.getBlockPos());
                    mutableBlockPos.move(value);
                    ReactorBaseTile tile = blocks.getTile(mutableBlockPos);
                    if (tile instanceof ReactorManifoldTile) {
                        if (((ReactorManifoldTile) tile).lastCheckedTick != tick) {
                            manifoldsToCheck.add((ReactorManifoldTile) tile);
                        }
                    }
                }
            }
            
            for (ReactorManifoldTile manifold : manifolds) {
                if (manifold.lastCheckedTick != tick) {
                    BlockPos pos = manifold.getBlockPos();
                    throw new ValidationError(new TranslatableComponent("multiblock.error.biggerreactors.dangling_manifold", pos.getX(), pos.getY(), pos.getZ()));
                }
            }
            
            Util.chunkCachedBlockStateIteration(minCoord(), maxCoord(), world, (block, pos) -> {
                if (block.getBlock() instanceof ReactorBaseBlock) {
                    mutableBlockPos.set(pos.x, pos.y, pos.z);
                    if (!blocks.containsPos(mutableBlockPos)) {
                        throw new ValidationError(new TranslatableComponent("multiblock.error.biggerreactors.dangling_internal_part", pos.x, pos.y, pos.z));
                    }
                }
            });
            
            return true;
        });
    }
    
    private ReactorActivity reactorActivity = ReactorActivity.INACTIVE;
    
    private final Set<ReactorTerminalTile> terminals = new HashSet<>();
    private final List<ReactorControlRodTile> controlRods = new ArrayList<>();
    private final Set<ReactorFuelRodTile> fuelRods = new LinkedHashSet<>();
    private final ArrayList<Set<ReactorFuelRodTile>> fuelRodsByLevel = new ArrayList<>();
    private final Set<ReactorPowerTapTile> powerPorts = new HashSet<>();
    private final Set<ReactorAccessPortTile> accessPorts = new HashSet<>();
    private final Set<ReactorCoolantPortTile> coolantPorts = new HashSet<>();
    private final Set<ReactorManifoldTile> manifolds = new LinkedHashSet<>();
    
    @Override
    protected void onPartPlaced(ReactorBaseTile placed) {
        onPartAttached(placed);
    }
    
    
    @Override
    protected synchronized void onPartAttached(ReactorBaseTile tile) {
        if (tile instanceof ReactorTerminalTile) {
            terminals.add((ReactorTerminalTile) tile);
        }
        if (tile instanceof ReactorControlRodTile) {
            if (!controlRods.contains(tile)) {
                controlRods.add((ReactorControlRodTile) tile);
            }
        }
        if (tile instanceof ReactorFuelRodTile) {
            fuelRods.add((ReactorFuelRodTile) tile);
        }
        if (tile instanceof ReactorPowerTapTile) {
            powerPorts.add((ReactorPowerTapTile) tile);
        }
        if (tile instanceof ReactorAccessPortTile) {
            accessPorts.add((ReactorAccessPortTile) tile);
        }
        if (tile instanceof ReactorCoolantPortTile) {
            coolantPorts.add((ReactorCoolantPortTile) tile);
        }
        if (tile instanceof ReactorManifoldTile) {
            manifolds.add((ReactorManifoldTile) tile);
        }
    }
    
    @Override
    protected void onPartBroken(ReactorBaseTile broken) {
        onPartDetached(broken);
    }
    
    @Override
    protected synchronized void onPartDetached(ReactorBaseTile tile) {
        if (tile instanceof ReactorTerminalTile) {
            terminals.remove(tile);
        }
        if (tile instanceof ReactorControlRodTile) {
            // because order doesnt matter after a reactor is disassembled
            // should help with chunk unload times
            // yes this is specific to the arraylist
            int index = controlRods.indexOf(tile);
            if (index != -1) {
                controlRods.set(index, controlRods.get(controlRods.size() - 1));
                controlRods.remove(controlRods.size() - 1);
            }
        }
        if (tile instanceof ReactorFuelRodTile) {
            fuelRods.remove(tile);
        }
        if (tile instanceof ReactorPowerTapTile) {
            powerPorts.remove(tile);
        }
        if (tile instanceof ReactorAccessPortTile) {
            accessPorts.remove(tile);
        }
        if (tile instanceof ReactorCoolantPortTile) {
            coolantPorts.remove(tile);
        }
        if (tile instanceof ReactorManifoldTile) {
            manifolds.remove(tile);
        }
    }
    
    public void updateBlockStates() {
        terminals.forEach(terminal -> {
            world.setBlock(terminal.getBlockPos(), terminal.getBlockState().setValue(ReactorActivity.REACTOR_ACTIVITY_ENUM_PROPERTY, reactorActivity), 3);
            terminal.setChanged();
        });
    }
    
    public synchronized void setActive(ReactorActivity newState) {
        if (reactorActivity != newState) {
            reactorActivity = newState;
            updateBlockStates();
        }
        simulation.setActive(reactorActivity == ReactorActivity.ACTIVE);
    }
    
    public void toggleActive() {
        setActive(reactorActivity == ReactorActivity.ACTIVE ? ReactorActivity.INACTIVE : ReactorActivity.ACTIVE);
    }
    
    public boolean isActive() {
        return reactorActivity == ReactorActivity.ACTIVE;
    }
    
    protected void read(CompoundTag compound) {
        if (compound.contains("reactorState")) {
            reactorActivity = ReactorActivity.valueOf(compound.getString("reactorState").toUpperCase());
            simulation.setActive(reactorActivity == ReactorActivity.ACTIVE);
        }
        
        if (compound.contains("simulationData")) {
            simulation.deserializeNBT(compound.getCompound("simulationData"));
        }
        
        updateBlockStates();
    }
    
    
    protected CompoundTag write() {
        CompoundTag compound = new CompoundTag();
        {
            compound.putString("reactorState", reactorActivity.toString());
            compound.put("simulationData", simulation.serializeNBT());
        }
        return compound;
    }
    
    @Override
    protected void onMerge(ReactorMultiblockController otherController) {
        setActive(ReactorActivity.INACTIVE);
        distributeFuel();
        otherController.distributeFuel();
        simulation = createSimulation();
    }
    
    @Override
    protected void onAssembled() {
        simulation.resize(maxCoord().x() - minCoord().x() - 1, maxCoord().y() - minCoord().y() - 1, maxCoord().z() - minCoord().z() - 1);
        Vector3i start = new Vector3i(1).add(minCoord());
        Vector3i end = new Vector3i(-1).add(maxCoord());
        Util.chunkCachedBlockStateIteration(start, end, world, (state, pos) -> {
            pos.sub(start);
            if (!(state.getBlock() instanceof ReactorBaseBlock)) {
                simulation.setModeratorProperties(pos.x, pos.y, pos.z, ReactorModeratorRegistry.blockModeratorProperties(state.getBlock()));
            }
        });
        for (ReactorManifoldTile manifold : manifolds) {
            BlockPos manifoldPos = manifold.getBlockPos();
            simulation.setManifold(manifoldPos.getX() - start.x, manifoldPos.getY() - start.y, manifoldPos.getZ() - start.z);
        }
        for (ReactorControlRodTile controlRod : controlRods) {
            BlockPos rodPos = controlRod.getBlockPos();
            simulation.setControlRod(rodPos.getX() - start.x, rodPos.getZ() - start.z);
        }
        simulation.setPassivelyCooled(coolantPorts.isEmpty());
        simulation.updateInternalValues();
        updateControlRodLevels();
        collectFuel();
        
        int levels = this.maxCoord().y() - this.minCoord().y() - 1;
        fuelRodsByLevel.clear();
        fuelRodsByLevel.ensureCapacity(levels);
        for (int i = 0; i < levels; i++) {
            fuelRodsByLevel.add(new LinkedHashSet<>());
        }
        
        fuelRods.forEach(rod -> {
            int rodLevel = rod.getBlockPos().getY();
            rodLevel -= this.minCoord().y();
            rodLevel -= 1;
            fuelRodsByLevel.get(rodLevel).add(rod);
        });
        
        updateFuelRenderingLevel(true);
    }
    
    @Override
    protected void onUnpaused() {
        onAssembled();
    }
    
    @Override
    protected void onDisassembled() {
        distributeFuel();
        setActive(ReactorActivity.INACTIVE);
    }
    
    
    private IReactorSimulation simulation = createSimulation();
    
    IReactorSimulation createSimulation() {
        switch (Config.mode) {
            case CLASSIC:
                return new ClassicReactorSimulation();
            default:
            case MODERN:
                return new ModernReactorSimulation(20);
            case EXPERIMENTAL:
                return new MultithreadedReactorSimulation(20);
        }
    }
    
    public IReactorSimulation simulation() {
        return simulation;
    }
    
    private boolean forceDirty = false;
    
    @Override
    public synchronized void tick() {
        
        simulation.tick();
        if (autoEjectWaste) {
            ejectWaste();
        }
        
        
        long totalPowerRequested = 0;
        final long startingPower = simulation.battery().stored();
        for (ReactorPowerTapTile powerPort : powerPorts) {
            totalPowerRequested += powerPort.distributePower(startingPower, true);
        }
        
        float distributionMultiplier = Math.min(1f, (float) startingPower / (float) totalPowerRequested);
        for (ReactorPowerTapTile powerPort : powerPorts) {
            long powerRequested = powerPort.distributePower(startingPower, true);
            powerRequested *= distributionMultiplier;
            powerRequested = Math.min(simulation.battery().stored(), powerRequested); // just in casei
            long powerAccepted = powerPort.distributePower(powerRequested, false);
            simulation.battery().extract(powerAccepted);
        }
        
        // i know this is just a hose out, not sure if it should be changed or not
        coolantPorts.forEach(ReactorCoolantPortTile::pushFluid);
        
        updateFuelRenderingLevel();
        
        if (Phosphophyllite.tickNumber() % 2 == 0 || forceDirty) {
            forceDirty = false;
            markDirty();
        }
    }
    
    long currentFuelRenderLevel = 0;
    long currentWasteRenderLevel = 0;
    
    private void updateFuelRenderingLevel() {
        updateFuelRenderingLevel(false);
    }
    
    private void updateFuelRenderingLevel(boolean forceFullUpdate) {
        
        if (simulation.fuelTank().capacity() == 0) {
            return;
        }
        
        long rodPixels = fuelRodsByLevel.size() * 16L;
        long fuelPixels = (simulation.fuelTank().totalStored() * rodPixels) / simulation.fuelTank().capacity();
        long wastePixels = (simulation.fuelTank().waste() * rodPixels) / simulation.fuelTank().capacity();
        
        if (fuelPixels == currentFuelRenderLevel && wastePixels == currentWasteRenderLevel) {
            return;
        }
        
        long lowerFuelPixel = Math.min(currentFuelRenderLevel, fuelPixels);
        long upperFuelPixel = Math.max(currentFuelRenderLevel, fuelPixels);
        
        long lowerWastePixel = Math.min(currentWasteRenderLevel, wastePixels);
        long upperWastePixel = Math.max(currentWasteRenderLevel, wastePixels);
        
        if (forceFullUpdate) {
            lowerFuelPixel = lowerWastePixel = 0;
            upperFuelPixel = upperWastePixel = rodPixels;
        }
        
        long lowerFuelUpdateLevel = lowerFuelPixel / 16;
        long upperFuelUpdateLevel = upperFuelPixel / 16 + (((upperFuelPixel % 16) > 0) ? 1 : 0);
        
        long lowerWasteUpdateLevel = lowerWastePixel / 16;
        long upperWasteUpdateLevel = upperWastePixel / 16 + (((upperWastePixel % 16) > 0) ? 1 : 0);
        
        HashMap<BlockPos, BlockState> newStates = new LinkedHashMap<>();
        boolean[] updatedLevels = new boolean[(int) upperFuelUpdateLevel];
        
        if (lowerFuelPixel != upperFuelPixel) {
            for (long i = lowerFuelUpdateLevel; i < upperFuelUpdateLevel; i++) {
                long levelBasePixel = i * 16;
                int levelFuelPixel = (int) Math.max(Math.min(fuelPixels - levelBasePixel, 16), 0);
                
                for (ReactorFuelRodTile reactorFuelRodTile : fuelRodsByLevel.get((int) i)) {
                    BlockState state = reactorFuelRodTile.getBlockState();
                    BlockState newState = state.setValue(ReactorFuelRod.FUEL_HEIGHT_PROPERTY, levelFuelPixel);
                    if (newState != state) {
                        newStates.put(reactorFuelRodTile.getBlockPos(), newState);
                        updatedLevels[(int) i] = true;
                        reactorFuelRodTile.setBlockState(newState);
                    }
                }
                
            }
        }
        
        if (lowerWastePixel != upperWastePixel) {
            for (long i = lowerWasteUpdateLevel; i < upperWasteUpdateLevel; i++) {
                long levelBasePixel = i * 16;
                int levelWastePixel = (int) Math.max(Math.min(wastePixels - levelBasePixel, 16), 0);
                
                for (ReactorFuelRodTile reactorFuelRodTile : fuelRodsByLevel.get((int) i)) {
                    BlockState state = reactorFuelRodTile.getBlockState();
                    BlockState newState = state.setValue(ReactorFuelRod.WASTE_HEIGHT_PROPERTY, levelWastePixel);
                    if (newState != state) {
                        state = newStates.getOrDefault(reactorFuelRodTile.getBlockPos(), state);
                        newState = state.setValue(ReactorFuelRod.WASTE_HEIGHT_PROPERTY, levelWastePixel);
                        newStates.put(reactorFuelRodTile.getBlockPos(), newState);
                        updatedLevels[(int) i] = true;
                        reactorFuelRodTile.setBlockState(newState);
                    }
                }
                
            }
        }
        
        Util.setBlockStates(newStates, world);
        
        currentFuelRenderLevel = fuelPixels;
        currentWasteRenderLevel = wastePixels;
    }
    
    private void distributeFuel() {
        if (simulation.fuelTank().totalStored() > 0 && !fuelRods.isEmpty()) {
            long fuelToDistribute = simulation.fuelTank().fuel();
            long wasteToDistribute = simulation.fuelTank().waste();
            fuelToDistribute /= fuelRods.size();
            wasteToDistribute /= fuelRods.size();
            for (ReactorFuelRodTile fuelRod : fuelRods) {
                fuelRod.fuel += simulation.fuelTank().extractFuel(fuelToDistribute, false);
                fuelRod.waste += simulation.fuelTank().extractWaste(wasteToDistribute, false);
            }
            for (ReactorFuelRodTile fuelRod : fuelRods) {
                fuelRod.fuel += simulation.fuelTank().extractFuel(Long.MAX_VALUE, false);
                fuelRod.waste += simulation.fuelTank().extractWaste(Long.MAX_VALUE, false);
            }
            markDirty();
        }
    }
    
    private void collectFuel() {
        for (ReactorFuelRodTile fuelRod : fuelRods) {
            fuelRod.fuel -= simulation.fuelTank().insertFuel(fuelRod.fuel, false);
            fuelRod.waste -= simulation.fuelTank().insertWaste(fuelRod.waste, false);
            if (fuelRod.fuel != 0 || fuelRod.waste != 0) {
                BiggerReactors.LOGGER.warn("Reactor overfilled with fuel at " + fuelRod.getBlockPos().toString());
                // for now, just void the fuel
                fuelRod.fuel = 0;
                fuelRod.waste = 0;
            }
        }
        markDirty();
    }
    
    private boolean autoEjectWaste = true;
    
    public synchronized void ejectWaste() {
        for (ReactorAccessPortTile accessPort : accessPorts) {
            if (accessPort.isInlet()) {
                continue;
            }
            long wastePushed = accessPort.pushWaste((int) simulation.fuelTank().waste(), false);
            forceDirty = simulation.fuelTank().extractWaste(wastePushed, false) > 0;
            
        }
        
        // outlets have already taken as much as they can, now just hose it out the inlets too
        // this will only actually do anything with items, so, we only care if there is a full ingot or more
        // if/when fluid fueling is added, only oulets will output it
        if (simulation.fuelTank().waste() > Config.Reactor.FuelMBPerIngot) {
            for (ReactorAccessPortTile accessPort : accessPorts) {
                long wastePushed = accessPort.pushWaste((int) simulation.fuelTank().waste(), false);
                forceDirty = simulation.fuelTank().extractWaste(wastePushed, false) > 0;
            }
        }
    }
    
    public synchronized long extractWaste(long mb, boolean simulated) {
        if (assemblyState() != AssemblyState.ASSEMBLED) {
            return 0;
        }
        long wasteExtracted = simulation.fuelTank().extractWaste(mb, simulated);
        forceDirty = wasteExtracted > 0 && !simulated;
        return wasteExtracted;
    }
    
    public synchronized long extractFuel(long mb, boolean simulated) {
        if (assemblyState() != AssemblyState.ASSEMBLED) {
            return 0;
        }
        long fuelExtracted = simulation.fuelTank().extractFuel(mb, simulated);
        forceDirty = fuelExtracted > 0 && !simulated;
        return fuelExtracted;
    }
    
    public synchronized long refuel(long mb, boolean simulated) {
        if (assemblyState() != AssemblyState.ASSEMBLED) {
            return 0;
        }
        long fuelInserted = simulation.fuelTank().insertFuel(mb, simulated);
        forceDirty = fuelInserted > 0 && !simulated;
        return fuelInserted;
    }
    
    public void updateReactorState(ReactorState reactorState) {
        // TODO: These are mixed between the new enums and old booleans. Migrate them fully to enums.
        reactorState.reactorActivity = reactorActivity;
        reactorState.reactorType = simulation.isPassive() ? ReactorType.PASSIVE : ReactorType.ACTIVE;
        
        reactorState.doAutoEject = autoEjectWaste;
        
        reactorState.energyStored = simulation.battery().stored();
        reactorState.energyCapacity = simulation.battery().capacity();
        
        reactorState.wasteStored = simulation.fuelTank().waste();
        reactorState.fuelStored = simulation.fuelTank().fuel();
        reactorState.fuelCapacity = simulation.fuelTank().capacity();
        
        reactorState.coolantStored = simulation.coolantTank().liquidAmount();
        reactorState.coolantCapacity = simulation.coolantTank().perSideCapacity();
        reactorState.coolantResourceLocation = (simulation.coolantTank().liquidType() != null)
                ? Objects.requireNonNull(simulation.coolantTank().liquidType().getRegistryName()).toString()
                : Objects.requireNonNull(Fluids.EMPTY.getRegistryName()).toString();
        
        reactorState.exhaustStored = simulation.coolantTank().vaporAmount();
        reactorState.exhaustCapacity = simulation.coolantTank().perSideCapacity();
        reactorState.exhaustResourceLocation = (simulation.coolantTank().vaporType() != null)
                ? Objects.requireNonNull(simulation.coolantTank().vaporType().getRegistryName()).toString()
                : Objects.requireNonNull(Fluids.EMPTY.getRegistryName()).toString();
        
        reactorState.caseHeatStored = simulation.caseHeat();
        reactorState.fuelHeatStored = simulation.fuelHeat();
        
        reactorState.reactivityRate = simulation.fertility();
        reactorState.fuelUsageRate = simulation.fuelConsumptionLastTick();
        reactorState.reactorOutputRate = simulation.outputLastTick();
    }
    
    public void runRequest(String requestName, @Nullable Object requestData) {
        switch (requestName) {
            // Set the reactor to ACTIVE or INACTIVE.
            case "setActive": {
                if (!(requestData instanceof Integer)) {
                    return;
                }
                setActive(ReactorActivity.fromInt((Integer) requestData));
                return;
            }
            // Enable or disable waste ejection.
            case "setAutoEject": {
                if (!(requestData instanceof Integer)) {
                    return;
                }
                autoEjectWaste = ((Integer) requestData != 0);
                return;
            }
            // Manually eject waste.
            case "ejectWaste": {
                ejectWaste();
                return;
            }
        }
    }
    
    @Override
    
    public String getDebugString() {
        return super.getDebugString() +
                "State: " + reactorActivity.toString() + "\n" +
                "StoredPower: " + simulation.battery().stored() + "\n" +
                "PowerProduction: " + simulation.FEProducedLastTick() + "\n" +
                "MBProduction: " + simulation.MBProducedLastTick() + "\n" +
                "FuelUsage: " + simulation.fuelConsumptionLastTick() + "\n" +
                "ReactantCapacity: " + simulation.fuelTank().capacity() + "\n" +
                "TotalReactant: " + simulation.fuelTank().totalStored() + "\n" +
                "PercentFull: " + (float) simulation.fuelTank().totalStored() * 100 / simulation.fuelTank().capacity() + "\n" +
                "Fuel: " + simulation.fuelTank().fuel() + "\n" +
                "Waste: " + simulation.fuelTank().waste() + "\n" +
                "AutoEjectWaste: " + autoEjectWaste + "\n" +
                "Fertility: " + simulation.fertility() + "\n" +
                "FuelHeat: " + simulation.fuelHeat() + "\n" +
                "ReactorHeat: " + simulation.caseHeat() + "\n" +
                "CoolantTankSize: " + simulation.coolantTank().perSideCapacity() + "\n" +
                "LiquidType: " + simulation.coolantTank().liquidType() + "\n" +
                "Liquid: " + simulation.coolantTank().liquidAmount() + "\n" +
                "VaporType: " + simulation.coolantTank().vaporType() + "\n" +
                "Vapor: " + simulation.coolantTank().vaporAmount() + "\n" +
                "";
    }
    
    public synchronized void setAllControlRodLevels(double newLevel) {
        controlRods.forEach(rod -> {
            rod.setInsertion(newLevel);
        });
        updateControlRodLevels();
    }
    
    public synchronized void setControlRodLevel(int index, double newLevel) {
        controlRods.get(index).setInsertion(newLevel);
        updateControlRodLevels();
    }
    
    public double controlRodLevel(int index) {
        return controlRods.get(index).getInsertion();
    }
    
    public void updateControlRodLevels() {
        controlRods.forEach(rod -> {
            BlockPos pos = rod.getBlockPos();
            simulation.setControlRodInsertion(pos.getX() - minCoord().x() - 1, pos.getZ() - minCoord().z() - 1, rod.getInsertion());
        });
    }
    
    public int controlRodCount() {
        return controlRods.size();
    }
    
    public String controlRodName(int index) {
        synchronized (controlRods) {
            return controlRods.get(index).getName();
        }
    }
    
    public void setControlRodName(int index, String newName) {
        synchronized (controlRods) {
            controlRods.get(index).setName(newName);
        }
    }
}