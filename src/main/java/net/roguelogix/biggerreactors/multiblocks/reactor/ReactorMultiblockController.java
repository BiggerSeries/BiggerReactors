package net.roguelogix.biggerreactors.multiblocks.reactor;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
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
import net.roguelogix.biggerreactors.multiblocks.reactor.blocks.ReactorBaseBlock;
import net.roguelogix.biggerreactors.multiblocks.reactor.blocks.ReactorFuelRod;
import net.roguelogix.biggerreactors.multiblocks.reactor.blocks.ReactorManifold;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.IReactorSimulation;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.modern.MultithreadedReactorSimulation;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.modern.ModernReactorSimulation;
import net.roguelogix.biggerreactors.multiblocks.reactor.state.ReactorActivity;
import net.roguelogix.biggerreactors.multiblocks.reactor.state.ReactorState;
import net.roguelogix.biggerreactors.multiblocks.reactor.state.ReactorType;
import net.roguelogix.biggerreactors.multiblocks.reactor.tiles.*;
import net.roguelogix.biggerreactors.registries.ReactorModeratorRegistry;
import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.multiblock.MultiblockTileModule;
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
        maxSize.set(BiggerReactors.CONFIG.Reactor.MaxLength, BiggerReactors.CONFIG.Reactor.MaxHeight, BiggerReactors.CONFIG.Reactor.MaxWidth);
        interiorValidator = ReactorModeratorRegistry::isBlockAllowed;
        validationStartedCallback = () -> {
            foundRods = 0;
            foundManifolds = 0;
        };
        blockValidatedCallback = (block) -> {
            if (block == ReactorFuelRod.INSTANCE) {
                foundRods++;
            }
            if (block == ReactorManifold.INSTANCE) {
                foundManifolds++;
            }
        };
        setAssemblyValidator(ReactorMultiblockController::validate);
    }
    
    private boolean validate() {
        if (foundRods > fuelRods.size()) {
            throw new ValidationError(new TranslatableComponent("multiblock.error.biggerreactors.dangling_rod"));
        }
        if (foundManifolds > manifolds.size()) {
            throw new ValidationError(new TranslatableComponent("multiblock.error.biggerreactors.dangling_manifold"));
        }
        if (terminals.isEmpty()) {
            throw new ValidationError("multiblock.error.biggerreactors.no_terminal");
        }
        if (controlRods.isEmpty()) {
            throw new ValidationError("multiblock.error.biggerreactors.no_rods");
        }
        if (!powerPorts.isEmpty() && !coolantPorts.isEmpty()) {
            throw new ValidationError("multiblock.error.biggerreactors.coolant_and_power_ports");
        }
        
        long tick = Phosphophyllite.tickNumber();
        
        final int maxY = maxCoord().y();
        final int internalMinY = minCoord().y() + 1;
        
        int rodCount = 0;
        
        for (int j = 0; j < controlRods.size(); j++) {
            var controlRodPos = controlRods.get(j).getBlockPos();
            if (controlRodPos.getY() != maxCoord().y()) {
                throw new ValidationError(new TranslatableComponent("multiblock.error.biggerreactors.control_rod_not_on_top", controlRodPos.getX(), controlRodPos.getY(), controlRodPos.getZ()));
            }
            final int x = controlRodPos.getX(), z = controlRodPos.getZ();
            for (int i = internalMinY; i < maxY; i++) {
                final var tile = blocks.getTile(x, i, z);
                
                if (!(tile instanceof ReactorFuelRodTile)) {
                    throw new ValidationError(new TranslatableComponent("multiblock.error.biggerreactors.fuel_rod_gap", controlRodPos.getX(), controlRodPos.getY() + (-1 - i), controlRodPos.getZ()));
                }
                
                ((ReactorFuelRodTile) tile).lastCheckedTick = tick;
                rodCount++;
            }
        }
        
        if (rodCount != fuelRods.size()) {
            for (int i = 0; i < fuelRods.size(); i++) {
                final var fuelRod = fuelRods.get(i);
                if (fuelRod.lastCheckedTick != tick) {
                    throw new ValidationError(new TranslatableComponent("multiblock.error.biggerreactors.no_control_rod_for_fuel_rod", fuelRod.getBlockPos().getX(), fuelRod.getBlockPos().getZ()));
                }
            }
        }
        
        
        if (!manifolds.isEmpty()) {
            final var manifoldsToCheck = new ArrayList<MultiblockTileModule<?, ?>>();
            
            final var directions = Direction.values();
            int minx = minCoord().x() + 1, miny = minCoord().y() + 1, minz = minCoord().z() + 1;
            int maxx = maxCoord().x() - 1, maxy = maxCoord().y() - 1, maxz = maxCoord().z() - 1;
            for (ReactorManifoldTile manifold : manifolds) {
                BlockPos pos = manifold.getBlockPos();
                
                if (pos.getX() == minx || pos.getX() == maxx ||
                        pos.getY() == miny || pos.getY() == maxy ||
                        pos.getZ() == minz || pos.getZ() == maxz) {
                    var manifoldModule = manifold.multiblockModule();
                    for (int i = 0; i < 6; i++) {
                        final var direction = directions[i];
                        final MultiblockTileModule<?, ?> neighborModule = manifoldModule.getNeighbor(direction);
                        if (neighborModule == null) {
                            continue;
                        }
                        final BlockEntity neighborTile = neighborModule.iface;
                        if (!(neighborTile instanceof ReactorGlassTile) && ((ReactorBaseBlock) neighborTile.getBlockState().getBlock()).isGoodForExterior()) {
                            manifoldsToCheck.add(manifoldModule);
                            manifold.lastCheckedTick = tick;
                            break;
                        }
                    }
                }
            }
            
            while (!manifoldsToCheck.isEmpty()) {
                // done like this to avoid array shuffling
                MultiblockTileModule<?, ?> manifoldModule = manifoldsToCheck.remove(manifoldsToCheck.size() - 1);
                for (int i = 0; i < 6; i++) {
                    final var direction = directions[i];
                    final MultiblockTileModule<?, ?> neighborModule = manifoldModule.getNeighbor(direction);
                    if (neighborModule == null) {
                        continue;
                    }
                    final BlockEntity neighborTile = neighborModule.iface;
                    if (neighborTile instanceof ReactorManifoldTile neighborManifoldTile) {
                        if (neighborManifoldTile.lastCheckedTick != tick) {
                            manifoldsToCheck.add(neighborModule);
                            neighborManifoldTile.lastCheckedTick = tick;
                        }
                    }
                }
            }
            
            for (ReactorManifoldTile manifold : manifolds) {
                if (manifold.lastCheckedTick != tick) {
                    BlockPos pos = manifold.getBlockPos();
                    throw new ValidationError(new TranslatableComponent("multiblock.error.biggerreactors.disconnected_manifold", pos.getX(), pos.getY(), pos.getZ()));
                }
            }
        }
        
        return true;
    }
    
    private int foundRods = 0;
    private int foundManifolds = 0;
    
    private boolean updateBlockStates = false;
    
    private ReactorActivity reactorActivity = ReactorActivity.INACTIVE;
    
    private final Set<ReactorTerminalTile> terminals = new HashSet<>();
    private final ObjectArrayList<ReactorControlRodTile> controlRods = new ObjectArrayList<>();
    private final ObjectArrayList<ReactorFuelRodTile> fuelRods = new ObjectArrayList<>();
    private final ObjectArrayList<ObjectArrayList<ReactorFuelRodTile>> fuelRodsByLevel = ObjectArrayList.wrap(new ObjectArrayList[0]);
    private final Set<ReactorPowerTapTile> powerPorts = new HashSet<>();
    private final Set<ReactorAccessPortTile> accessPorts = new HashSet<>();
    private final Set<ReactorCoolantPortTile> coolantPorts = new HashSet<>();
    private final ObjectArrayList<ReactorManifoldTile> manifolds = new ObjectArrayList<>();
    
    @Override
    protected void onPartPlaced(ReactorBaseTile placed) {
        onPartAttached(placed);
    }
    
    @Override
    protected synchronized void onPartAttached(ReactorBaseTile tile) {
        if (tile instanceof ReactorTerminalTile) {
            tile.index = terminals.size();
            terminals.add((ReactorTerminalTile) tile);
        }
        if (tile instanceof ReactorControlRodTile) {
            tile.index = controlRods.size();
            controlRods.add((ReactorControlRodTile) tile);
        }
        if (tile instanceof ReactorFuelRodTile) {
            tile.index = fuelRods.size();
            fuelRods.add((ReactorFuelRodTile) tile);
        }
        if (tile instanceof ReactorPowerTapTile) {
            tile.index = powerPorts.size();
            powerPorts.add((ReactorPowerTapTile) tile);
        }
        if (tile instanceof ReactorAccessPortTile) {
            tile.index = accessPorts.size();
            accessPorts.add((ReactorAccessPortTile) tile);
        }
        if (tile instanceof ReactorCoolantPortTile) {
            tile.index = coolantPorts.size();
            coolantPorts.add((ReactorCoolantPortTile) tile);
        }
        if (tile instanceof ReactorManifoldTile) {
            tile.index = manifolds.size();
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
            int index = tile.index;
            var endControlRod = controlRods.pop();
            if (index != controlRods.size()) {
                endControlRod.index = index;
                controlRods.set(index, endControlRod);
            }
        }
        if (tile instanceof ReactorFuelRodTile) {
            int index = tile.index;
            final var endFuelRod = fuelRods.pop();
            if (index != fuelRods.size()) {
                endFuelRod.index = index;
                fuelRods.set(index, endFuelRod);
            }
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
            int index = tile.index;
            final var endManifold = manifolds.pop();
            if (index != manifolds.size()) {
                endManifold.index = index;
                manifolds.set(index, endManifold);
            }
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
            updateBlockStates = true;
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
        
        updateBlockStates = true;
    }
    
    @Nonnull
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
            if (!(state.getBlock() instanceof ReactorBaseBlock)) {
                pos.sub(start);
                simulation.setModeratorProperties(pos.x, pos.y, pos.z, ReactorModeratorRegistry.blockModeratorProperties(state.getBlock()));
            }
        });
        for (int i = 0; i < manifolds.size(); i++) {
            BlockPos manifoldPos = manifolds.get(i).getBlockPos();
            simulation.setManifold(manifoldPos.getX() - start.x, manifoldPos.getY() - start.y, manifoldPos.getZ() - start.z);
        }
        for (int i = 0; i < controlRods.size(); i++) {
            BlockPos rodPos = controlRods.get(i).getBlockPos();
            simulation.setControlRod(rodPos.getX() - start.x, rodPos.getZ() - start.z);
        }
        simulation.setPassivelyCooled(coolantPorts.isEmpty());
        simulation.updateInternalValues();
        updateControlRodLevels();
        collectFuel();
        
        int levels = this.maxCoord().y() - this.minCoord().y() - 1;
        final int rodsPerLevel = fuelRods.size() / levels;
        fuelRodsByLevel.clear();
        fuelRodsByLevel.ensureCapacity(levels);
        for (int i = 0; i < levels; i++) {
            var newList = new ObjectArrayList<ReactorFuelRodTile>(rodsPerLevel);
            fuelRodsByLevel.add(newList);
        }
        final var levelArrays = fuelRodsByLevel.elements();
        final int minY = this.minCoord().y() + 1;
        for (int i = 0; i < fuelRods.size(); i++) {
            final var rod = fuelRods.get(i);
            int rodLevel = rod.getBlockPos().getY();
            rodLevel -= minY;
            levelArrays[rodLevel].add(rod);
        }
        
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
        switch (BiggerReactors.CONFIG.mode) {
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
        
        if (updateBlockStates) {
            updateBlockStates = false;
            updateBlockStates();
        }
        
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
        
        if (!forceFullUpdate && fuelPixels == currentFuelRenderLevel && wastePixels == currentWasteRenderLevel) {
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
        
        Long2ObjectLinkedOpenHashMap<BlockState> newStates = new Long2ObjectLinkedOpenHashMap<>();
        
        if (lowerFuelPixel != upperFuelPixel) {
            for (long i = lowerFuelUpdateLevel; i < upperFuelUpdateLevel; i++) {
                long levelBasePixel = i * 16;
                int levelFuelPixel = (int) Math.max(Math.min(fuelPixels - levelBasePixel, 16), 0);
                
                final var rodLevel = fuelRodsByLevel.get((int) i);
                BlockState state = rodLevel.get(0).getBlockState();
                BlockState newState = state.setValue(ReactorFuelRod.FUEL_HEIGHT_PROPERTY, levelFuelPixel);
                
                final var levelRodCount = rodLevel.size();
                for (int j = 0; j < levelRodCount; j++) {
                    final var currentFuelRod = rodLevel.get(0);
                    if (currentFuelRod.getBlockState() != newState) {
                        newStates.put(currentFuelRod.getBlockPos().asLong(), newState);
                        currentFuelRod.setBlockState(newState);
                    }
                }
            }
        }
        
        if (lowerWastePixel != upperWastePixel) {
            for (long i = lowerWasteUpdateLevel; i < upperWasteUpdateLevel; i++) {
                long levelBasePixel = i * 16;
                int levelWastePixel = (int) Math.max(Math.min(wastePixels - levelBasePixel, 16), 0);
                final var rodLevel = fuelRodsByLevel.get((int) i);
                final var baseFuelRod = fuelRodsByLevel.get((int) i).get(0);
                BlockState state = baseFuelRod.getBlockState();
                state = newStates.getOrDefault(baseFuelRod.getBlockPos().asLong(), state);
                BlockState newState = state.setValue(ReactorFuelRod.WASTE_HEIGHT_PROPERTY, levelWastePixel);
                
                final var levelRodCount = rodLevel.size();
                for (int j = 0; j < levelRodCount; j++) {
                    final var currentFuelRod = rodLevel.get(0);
                    if (currentFuelRod.getBlockState() != newState) {
                        newStates.put(currentFuelRod.getBlockPos().asLong(), newState);
                        currentFuelRod.setBlockState(newState);
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
            simulation.fuelTank().extractFuel(fuelToDistribute, false);
            simulation.fuelTank().extractWaste(wasteToDistribute, false);
            fuelToDistribute /= fuelRods.size();
            wasteToDistribute /= fuelRods.size();
            for (int i = 0; i < fuelRods.size(); i++) {
                final var fuelRod = fuelRods.get(i);
                fuelRod.fuel = fuelToDistribute;
                fuelRod.waste = wasteToDistribute;
            }
            markDirty();
        }
    }
    
    private void collectFuel() {
        long totalFuel = 0;
        long totalWaste = 0;
        for (int i = 0; i < fuelRods.size(); i++) {
            final var fuelRod = fuelRods.get(i);
            totalFuel += fuelRod.fuel;
            totalFuel += fuelRod.waste;
            fuelRod.fuel = 0;
            fuelRod.waste = 0;
        }
        simulation.fuelTank().insertFuel(totalFuel, false);
        simulation.fuelTank().insertFuel(totalWaste, false);
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
        if (simulation.fuelTank().waste() > BiggerReactors.CONFIG.Reactor.FuelMBPerIngot) {
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