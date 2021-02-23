package net.roguelogix.biggerreactors.classic.reactor;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.roguelogix.biggerreactors.BiggerReactors;
import net.roguelogix.biggerreactors.Config;
import net.roguelogix.biggerreactors.classic.reactor.blocks.ReactorBaseBlock;
import net.roguelogix.biggerreactors.classic.reactor.blocks.ReactorFuelRod;
import net.roguelogix.biggerreactors.classic.reactor.simulation.IReactorSimulation;
import net.roguelogix.biggerreactors.classic.reactor.simulation.classic.ClassicReactorSimulation;
import net.roguelogix.biggerreactors.classic.reactor.simulation.modern.ModernReactorSimulation;
import net.roguelogix.biggerreactors.classic.reactor.state.ReactorActivity;
import net.roguelogix.biggerreactors.classic.reactor.state.ReactorState;
import net.roguelogix.biggerreactors.classic.reactor.state.ReactorType;
import net.roguelogix.biggerreactors.classic.reactor.tiles.*;
import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.multiblock.generic.MultiblockController;
import net.roguelogix.phosphophyllite.multiblock.generic.ValidationError;
import net.roguelogix.phosphophyllite.multiblock.rectangular.RectangularMultiblockController;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3i;
import net.roguelogix.phosphophyllite.util.Util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class ReactorMultiblockController extends RectangularMultiblockController<ReactorMultiblockController, ReactorBaseTile, ReactorBaseBlock> {
    
    public ReactorMultiblockController(@Nonnull World world) {
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
            BlockPos.Mutable mutableBlockPos = new BlockPos.Mutable();
            
            long tick = Phosphophyllite.tickNumber();
            
            for (ReactorControlRodTile controlRod : controlRods) {
                mutableBlockPos.setPos(controlRod.getPos());
                if (mutableBlockPos.getY() != maxCoord().y()) {
                    throw new ValidationError(new TranslationTextComponent("multiblock.error.biggerreactors.control_rod_not_on_top", controlRod.getPos().getX(), controlRod.getPos().getY(), controlRod.getPos().getZ()));
                }
                for (int i = 0; i < maxCoord().y() - minCoord().y() - 1; i++) {
                    mutableBlockPos.move(0, -1, 0);
                    ReactorBaseTile tile = blocks.getTile(mutableBlockPos);
                    if (!(tile instanceof ReactorFuelRodTile)) {
                        throw new ValidationError(new TranslationTextComponent("multiblock.error.biggerreactors.fuel_rod_gap", controlRod.getPos().getX(), controlRod.getPos().getY() + (-1 - i), controlRod.getPos().getZ()));
                    }
                    ((ReactorFuelRodTile) tile).lastCheckedTick = tick;
                }
            }
            
            for (ReactorFuelRodTile fuelRod : fuelRods) {
                if (fuelRod.lastCheckedTick != tick) {
                    throw new ValidationError(new TranslationTextComponent("multiblock.error.biggerreactors.no_control_rod_for_fuel_rod", fuelRod.getPos().getX(), fuelRod.getPos().getZ()));
                }
            }
            
            Util.chunkCachedBlockStateIteration(minCoord(), maxCoord(), world, (block, pos) -> {
                if (block.getBlock() instanceof ReactorBaseBlock) {
                    mutableBlockPos.setPos(pos.x, pos.y, pos.z);
                    if (!blocks.containsPos(mutableBlockPos)) {
                        throw new ValidationError(new TranslationTextComponent("multiblock.error.biggerreactors.dangling_internal_part", pos.x, pos.y, pos.z));
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
    
    @Override
    protected void onPartPlaced(@Nonnull ReactorBaseTile placed) {
        onPartAttached(placed);
    }
    
    
    @Override
    protected void onPartAttached(@Nonnull ReactorBaseTile tile) {
        if (tile instanceof ReactorTerminalTile) {
            terminals.add((ReactorTerminalTile) tile);
        }
        if (tile instanceof ReactorControlRodTile) {
            synchronized (controlRods) {
                if (!controlRods.contains(tile)) {
                    controlRods.add((ReactorControlRodTile) tile);
                }
            }
        }
        if (tile instanceof ReactorFuelRodTile) {
            fuelRods.add((ReactorFuelRodTile) tile);
        }
        if (tile instanceof ReactorPowerTapTile) {
            powerPorts.add((ReactorPowerTapTile) tile);
        }
        if (tile instanceof ReactorAccessPortTile) {
            synchronized (accessPorts) {
                accessPorts.add((ReactorAccessPortTile) tile);
            }
        }
        if (tile instanceof ReactorCoolantPortTile) {
            coolantPorts.add((ReactorCoolantPortTile) tile);
        }
    }
    
    @Override
    protected void onPartBroken(@Nonnull ReactorBaseTile broken) {
        onPartDetached(broken);
    }
    
    @Override
    protected void onPartDetached(@Nonnull ReactorBaseTile tile) {
        if (tile instanceof ReactorTerminalTile) {
            terminals.remove(tile);
        }
        if (tile instanceof ReactorControlRodTile) {
            synchronized (controlRods) {
                // because order doesnt matter after a reactor is disassembled
                // should help with chunk unload times
                // yes this is specific to the arraylist
                int index = controlRods.indexOf(tile);
                if (index != -1) {
                    controlRods.set(index, controlRods.get(controlRods.size() - 1));
                    controlRods.remove(controlRods.size() - 1);
                }
            }
        }
        if (tile instanceof ReactorFuelRodTile) {
            fuelRods.remove(tile);
        }
        if (tile instanceof ReactorPowerTapTile) {
            powerPorts.remove(tile);
        }
        if (tile instanceof ReactorAccessPortTile) {
            synchronized (accessPorts) {
                accessPorts.remove(tile);
            }
        }
        if (tile instanceof ReactorCoolantPortTile) {
            coolantPorts.remove(tile);
        }
    }
    
    public void updateBlockStates() {
        terminals.forEach(terminal -> {
            world.setBlockState(terminal.getPos(), terminal.getBlockState().with(ReactorActivity.REACTOR_ACTIVITY_ENUM_PROPERTY, reactorActivity));
            terminal.markDirty();
        });
    }
    
    public synchronized void setActive(@Nonnull ReactorActivity newState) {
        if (reactorActivity != newState) {
            reactorActivity = newState;
            updateBlockStates();
        }
        simulation.setActive(reactorActivity == ReactorActivity.ACTIVE);
    }
    
    public void toggleActive() {
        setActive(reactorActivity == ReactorActivity.ACTIVE ? ReactorActivity.INACTIVE : ReactorActivity.ACTIVE);
    }
    
    protected void read(@Nonnull CompoundNBT compound) {
        if (compound.contains("reactorState")) {
            reactorActivity = ReactorActivity.valueOf(compound.getString("reactorState").toUpperCase());
            simulation.setActive(reactorActivity == ReactorActivity.ACTIVE);
        }
        
        if (compound.contains("simulationData")) {
            simulation.deserializeNBT(compound.getCompound("simulationData"));
        }
        
        updateBlockStates();
    }
    
    @Nonnull
    protected CompoundNBT write() {
        CompoundNBT compound = new CompoundNBT();
        {
            compound.putString("reactorState", reactorActivity.toString());
            compound.put("simulationData", simulation.serializeNBT());
        }
        return compound;
    }
    
    @Override
    protected void onMerge(@Nonnull ReactorMultiblockController otherController) {
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
            if (state.getBlock() != ReactorFuelRod.INSTANCE) {
                simulation.setModeratorProperties(pos.x, pos.y, pos.z, ReactorModeratorRegistry.blockModeratorProperties(state.getBlock()));
            }
        });
        for (ReactorControlRodTile controlRod : controlRods) {
            BlockPos rodPos = controlRod.getPos();
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
            int rodLevel = rod.getPos().getY();
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
            case EXPERIMENTAL:
                return new ModernReactorSimulation(20);
        }
    }
    
    public IReactorSimulation simulation() {
        return simulation;
    }
    
    private boolean forceDirty = false;
    
    @Override
    public void tick() {
        
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
        long wastePixels = (simulation.fuelTank().totalStored() * rodPixels) / simulation.fuelTank().capacity();
        
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
                    BlockState newState = state.with(ReactorFuelRod.FUEL_HEIGHT_PROPERTY, levelFuelPixel);
                    if (newState != state) {
                        newStates.put(reactorFuelRodTile.getPos(), newState);
                        updatedLevels[(int) i] = true;
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
                    BlockState newState = state.with(ReactorFuelRod.WASTE_HEIGHT_PROPERTY, levelWastePixel);
                    if (newState != state) {
                        state = newStates.getOrDefault(reactorFuelRodTile.getPos(), state);
                        newState = state.with(ReactorFuelRod.WASTE_HEIGHT_PROPERTY, levelWastePixel);
                        newStates.put(reactorFuelRodTile.getPos(), newState);
                        updatedLevels[(int) i] = true;
                    }
                }
                
            }
        }
        
        Util.setBlockStates(newStates, world);
        
        for (int i = 0; i < updatedLevels.length; i++) {
            if (!updatedLevels[i]) {
                continue;
            }
            for (ReactorFuelRodTile reactorFuelRodTile : fuelRodsByLevel.get(i)) {
                reactorFuelRodTile.updateContainingBlockInfo();
            }
        }
        
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
                BiggerReactors.LOGGER.warn("Reactor overfilled with fuel at " + fuelRod.getPos().toString());
                // for now, just void the fuel
                fuelRod.fuel = 0;
                fuelRod.waste = 0;
            }
        }
        markDirty();
    }
    
    private boolean autoEjectWaste = true;
    
    public void ejectWaste() {
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
    
    public long extractWaste(long mb, boolean simulated) {
        if (assemblyState() != AssemblyState.ASSEMBLED) {
            return 0;
        }
        long wasteExtracted = simulation.fuelTank().extractWaste(mb, simulated);
        forceDirty = wasteExtracted > 0 && !simulated;
        return wasteExtracted;
    }
    
    public long extractFuel(long mb, boolean simulated) {
        if (assemblyState() != AssemblyState.ASSEMBLED) {
            return 0;
        }
        long fuelExtracted = simulation.fuelTank().extractFuel(mb, simulated);
        forceDirty = fuelExtracted > 0 && !simulated;
        return fuelExtracted;
    }
    
    public long refuel(long mb, boolean simulated) {
        if (assemblyState() != AssemblyState.ASSEMBLED) {
            return 0;
        }
        long fuelInserted = simulation.fuelTank().insertFuel(mb, simulated);
        forceDirty = fuelInserted > 0 && !simulated;
        return fuelInserted;
    }
    
    public void updateReactorState(@Nonnull ReactorState reactorState) {
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
        
        reactorState.steamStored = simulation.coolantTank().vaporAmount();
        reactorState.steamCapacity = simulation.coolantTank().perSideCapacity();
        
        reactorState.caseHeatStored = simulation.caseHeat();
        reactorState.fuelHeatStored = simulation.fuelHeat();
        
        reactorState.reactivityRate = simulation.fertility();
        reactorState.fuelUsageRate = simulation.fuelConsumptionLastTick();
        reactorState.reactorOutputRate = simulation.outputLastTick();
    }
    
    public void runRequest(@Nonnull String requestName, @Nullable Object requestData) {
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
    @Nonnull
    public String getDebugInfo() {
        return super.getDebugInfo() +
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
    
    public void setAllControlRodLevels(double newLevel) {
        synchronized (controlRods) {
            controlRods.forEach(rod -> {
                rod.setInsertion(newLevel);
            });
            updateControlRodLevels();
        }
    }
    
    public double controlRdoLevel(int index) {
        return controlRods.get(index).getInsertion();
    }
    
    public void updateControlRodLevels() {
        controlRods.forEach(rod -> {
            BlockPos pos = rod.getPos();
            simulation.setControlRodInsertion(pos.getX() - minCoord().x() - 1, pos.getZ() - minCoord().z() - 1, rod.getInsertion());
        });
    }
    
    // -- Mekanism compat
    
    public long getSteamCapacity() {
        return simulation.coolantTank().perSideCapacity();
    }
    
    public long getSteamAmount() {
        return simulation.coolantTank().vaporAmount();
    }
    
    // -- ComputerCraft API --
    
    public boolean CCgetConnected() {
        return state != MultiblockController.AssemblyState.DISASSEMBLED;
    }
    
    public boolean CCgetActive() {
        return reactorActivity == ReactorActivity.ACTIVE;
    }
    
    public int CCgetNumberOfControlRods() {
        return controlRods.size();
    }
    
    public long CCgetEnergyStored() {
        // backwards compatible with the old CC API, which requires this assumption
        return (simulation.battery().stored() * 10_000_000) / simulation.battery().capacity();
    }
    
    public long CCgetEnergyStoredUnscaled() {
        return simulation.battery().stored();
    }
    
    public long CCgetMaxEnergyStored() {
        return simulation.battery().capacity();
    }
    
    public double CCgetFuelTemperature() {
        return simulation.fuelHeat();
    }
    
    public double CCgetCasingTemperature() {
        return simulation.caseHeat();
    }
    
    public long CCgetFuelAmount() {
        return simulation.fuelTank().fuel();
    }
    
    public long CCgetWasteAmount() {
        return simulation.fuelTank().waste();
    }
    
    public long CCgetReactantAmount() {
        return simulation.fuelTank().totalStored();
    }
    
    public long CCgetFuelAmountMax() {
        return simulation.fuelTank().capacity();
    }
    
    @Nonnull
    public String CCgetControlRodName(int index) {
        synchronized (controlRods) {
            if (index >= controlRods.size()) {
                throw new RuntimeException("control rod index out of bounds");
            }
            return controlRods.get(index).getName();
        }
    }
    
    public double CCgetControlRodLevel(int index) {
        synchronized (controlRods) {
            if (index >= controlRods.size()) {
                throw new RuntimeException("control rod index out of bounds");
            }
            return controlRods.get(index).getInsertion();
        }
    }
    
    public double CCgetEnergyProducedLastTick() {
        return simulation.outputLastTick();
    }
    
    public double CCgetHotFluidProducedLastTick() {
        return simulation.MBProducedLastTick();
    }
    
    public double CCgetMaxHotFluidProducedLastTick() {
        if (simulation.isPassive()) {
            return 0;
        }
        return simulation.maxMBProductionLastTick();
    }
    
    
    @Nullable
    public String CCgetCoolantType() {
        if (simulation.coolantTank().liquidAmount() == 0 || simulation.coolantTank().liquidType() == null) {
            return null;
        }
        return Objects.requireNonNull(simulation.coolantTank().liquidType().getRegistryName()).toString();
    }
    
    public long CCgetCoolantAmount() {
        return simulation.coolantTank().liquidAmount();
    }
    
    @Nullable
    public String CCgetHotFluidType() {
        if (simulation.coolantTank().vaporAmount() == 0 || simulation.coolantTank().vaporType() == null) {
            return null;
        }
        return Objects.requireNonNull(simulation.coolantTank().vaporType().getRegistryName()).toString();
    }
    
    public long CCgetHotFluidAmount() {
        return simulation.coolantTank().vaporAmount();
    }
    
    public double CCgetFuelReactivity() {
        return simulation.fertility() * 100;
    }
    
    public double CCgetFuelConsumedLastTick() {
        return simulation.fuelConsumptionLastTick();
    }
    
    public BlockPos CCgetControlRodLocation(int index) {
        if (index >= controlRods.size()) {
            throw new RuntimeException("control rod index out of bounds");
        }
        return controlRods.get(index).getPos();
    }
    
    public boolean CCisActivelyCooled() {
        return !simulation.isPassive();
    }
    
    public void CCsetActive(boolean active) {
        setActive(active ? ReactorActivity.ACTIVE : ReactorActivity.INACTIVE);
    }
    
    public void CCsetAllControlRodLevels(double insertion) {
        setAllControlRodLevels(insertion);
    }
    
    public void CCsetControlRodLevel(double insertion, int index) {
        synchronized (controlRods) {
            if (index >= controlRods.size()) {
                throw new RuntimeException("control rod index out of bounds");
            }
            controlRods.get(index).setInsertion(insertion);
            updateControlRodLevels();
        }
    }
    
    public void CCsetControlRodName(int index, String name) {
        if (index >= controlRods.size()) {
            throw new RuntimeException("control rod index out of bounds");
        }
        controlRods.get(index).setName(name);
    }
    
    public void CCdoEjectWaste() {
        synchronized (accessPorts) {
            ejectWaste();
        }
    }
    
    public void CCdoEjectFuel() {
        synchronized (accessPorts) {
//            ejectWaste();
        }
    }
    
    public long CCgetCoolantAmountMax() {
        return simulation.coolantTank().perSideCapacity();
    }
    
    public long CCgetHotFluidAmountMax() {
        return simulation.coolantTank().perSideCapacity();
    }
}