package net.roguelogix.biggerreactors.multiblocks.reactor;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;
import net.roguelogix.biggerreactors.Config;
import net.roguelogix.biggerreactors.multiblocks.reactor.blocks.ReactorBaseBlock;
import net.roguelogix.biggerreactors.multiblocks.reactor.blocks.ReactorFuelRod;
import net.roguelogix.biggerreactors.multiblocks.reactor.blocks.ReactorManifold;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.IReactorSimulation;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.SimulationDescription;
import net.roguelogix.biggerreactors.multiblocks.reactor.state.ReactorActivity;
import net.roguelogix.biggerreactors.multiblocks.reactor.state.ReactorState;
import net.roguelogix.biggerreactors.multiblocks.reactor.state.ReactorType;
import net.roguelogix.biggerreactors.multiblocks.reactor.tiles.*;
import net.roguelogix.biggerreactors.multiblocks.reactor.util.ReactorTransitionTank;
import net.roguelogix.biggerreactors.registries.ReactorModeratorRegistry;
import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.debug.DebugInfo;
import net.roguelogix.phosphophyllite.multiblock.MultiblockController;
import net.roguelogix.phosphophyllite.multiblock.MultiblockTileModule;
import net.roguelogix.phosphophyllite.multiblock.ValidationException;
import net.roguelogix.phosphophyllite.multiblock.common.IEventMultiblock;
import net.roguelogix.phosphophyllite.multiblock.common.IPersistentMultiblock;
import net.roguelogix.phosphophyllite.multiblock.common.ITickablePartsMultiblock;
import net.roguelogix.phosphophyllite.multiblock.rectangular.IRectangularMultiblock;
import net.roguelogix.phosphophyllite.multiblock.touching.ITouchingMultiblock;
import net.roguelogix.phosphophyllite.serialization.PhosphophylliteCompound;
import net.roguelogix.phosphophyllite.util.NonnullDefault;
import net.roguelogix.phosphophyllite.util.Util;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@NonnullDefault
@ParametersAreNonnullByDefault
public class ReactorMultiblockController extends MultiblockController<ReactorBaseTile, ReactorBaseBlock, ReactorMultiblockController> implements
        IRectangularMultiblock<ReactorBaseTile, ReactorBaseBlock, ReactorMultiblockController>,
        IPersistentMultiblock<ReactorBaseTile, ReactorBaseBlock, ReactorMultiblockController>,
        ITouchingMultiblock<ReactorBaseTile, ReactorBaseBlock, ReactorMultiblockController>,
        IEventMultiblock<ReactorBaseTile, ReactorBaseBlock, ReactorMultiblockController>,
        ITickablePartsMultiblock<ReactorBaseTile, ReactorBaseBlock, ReactorMultiblockController> {
    
    public ReactorMultiblockController(Level level) {
        super(level, ReactorBaseTile.class, ReactorBaseBlock.class);
    }
    
    @Nullable
    @Override
    public Vector3ic minSize() {
        return new Vector3i(3);
    }
    
    @Nullable
    @Override
    public Vector3ic maxSize() {
        return new Vector3i(Config.CONFIG.Reactor.MaxLength, Config.CONFIG.Reactor.MaxHeight, Config.CONFIG.Reactor.MaxWidth);
    }
    
    @Override
    public void rectangularValidationStarted() {
        foundRods = 0;
        foundManifolds = 0;
    }
    
    @Override
    public void rectangularBlockValidated(Block block) {
        if (block == ReactorFuelRod.INSTANCE) {
            foundRods++;
        }
        if (block == ReactorManifold.INSTANCE) {
            foundManifolds++;
        }
    }
    
    @Override
    public boolean allowedInteriorBlock(Block block) {
        return ReactorModeratorRegistry.isBlockAllowed(block);
    }
    
    @Override
    public void validateStage1() throws ValidationException {
        if (terminals.isEmpty()) {
            throw new ValidationException("multiblock.error.biggerreactors.no_terminal");
        }
        if (controlRods.isEmpty()) {
            throw new ValidationException("multiblock.error.biggerreactors.no_rods");
        }
        if (!powerPorts.isEmpty() && !coolantPorts.isEmpty()) {
            throw new ValidationException("multiblock.error.biggerreactors.coolant_and_power_ports");
        }
    }
    
    @Override
    public void validateStage2() throws ValidationException {
        if (foundRods > fuelRods.size()) {
            Util.chunkCachedBlockStateIteration(min().add(1, 1, 1, new Vector3i()), max().sub(1, 1, 1, new Vector3i()), level, (state, position) -> {
                if (!(state.getBlock() instanceof ReactorFuelRod)) {
                    return;
                }
                final var tile = blocks.getTile(position);
                if (tile == null || (tile instanceof ReactorFuelRodTile fuelRodTile &&fuelRodTile.controller() != this)){
                    throw new ValidationException(Component.translatable("multiblock.error.biggerreactors.reactor.dangling_rod", position.toString()));
                }
            });
            throw new ValidationException(Component.translatable("multiblock.error.biggerreactors.reactor.dangling_rod_unknown_position"));
        }
        if (foundManifolds > manifolds.size()) {
            Util.chunkCachedBlockStateIteration(min().add(1, 1, 1, new Vector3i()), max().sub(1, 1, 1, new Vector3i()), level, (state, position) -> {
                if (!(state.getBlock() instanceof ReactorManifold)) {
                    return;
                }
                final var tile = blocks.getTile(position);
                if (tile == null || (tile instanceof ReactorManifoldTile manifoldTile && manifoldTile.controller() != this)) {
                    throw new ValidationException(Component.translatable("multiblock.error.biggerreactors.reactor.dangling_manifold", position.toString()));
                }
            });
            throw new ValidationException(Component.translatable("multiblock.error.biggerreactors.reactor.dangling_manifold_unknown_position"));
        }
        
        final long tick = Phosphophyllite.tickNumber();
        
        final int maxY = max().y();
        final int internalMinY = min().y() + 1;
        
        int rodCount = 0;
        
        //noinspection ForLoopReplaceableByForEach
        for (int j = 0; j < controlRods.size(); j++) {
            var controlRodPos = controlRods.get(j).getBlockPos();
            if (controlRodPos.getY() != max().y()) {
                throw new ValidationException(Component.translatable("multiblock.error.biggerreactors.control_rod_not_on_top", controlRodPos.getX(), controlRodPos.getY(), controlRodPos.getZ()));
            }
            final int x = controlRodPos.getX(), z = controlRodPos.getZ();
            for (int i = internalMinY; i < maxY; i++) {
                final var tile = blocks.getTile(x, i, z);
                
                if (!(tile instanceof ReactorFuelRodTile)) {
                    throw new ValidationException(Component.translatable("multiblock.error.biggerreactors.fuel_rod_gap", controlRodPos.getX(), controlRodPos.getY() + (-1 - i), controlRodPos.getZ()));
                }
                
                ((ReactorFuelRodTile) tile).lastCheckedTick = tick;
                rodCount++;
            }
        }
        
        if (rodCount != fuelRods.size()) {
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < fuelRods.size(); i++) {
                final var fuelRod = fuelRods.get(i);
                if (fuelRod.lastCheckedTick != tick) {
                    throw new ValidationException(Component.translatable("multiblock.error.biggerreactors.no_control_rod_for_fuel_rod", fuelRod.getBlockPos().getX(), fuelRod.getBlockPos().getZ()));
                }
            }
        }
        
        if (!manifolds.isEmpty()) {
            final var manifoldsToCheck = new ArrayList<MultiblockTileModule<?, ?, ?>>();
            
            final var directions = Direction.values();
            int minx = min().x() + 1, miny = min().y() + 1, minz = min().z() + 1;
            int maxx = max().x() - 1, maxy = max().y() - 1, maxz = max().z() - 1;
            for (ReactorManifoldTile manifold : manifolds) {
                BlockPos pos = manifold.getBlockPos();
                
                if (pos.getX() == minx || pos.getX() == maxx ||
                        pos.getY() == miny || pos.getY() == maxy ||
                        pos.getZ() == minz || pos.getZ() == maxz) {
                    var manifoldModule = manifold.multiblockModule();
                    for (int i = 0; i < 6; i++) {
                        final var direction = directions[i];
                        final MultiblockTileModule<?, ?, ?> neighborModule = manifoldModule.getNeighbor(direction);
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
                MultiblockTileModule<?, ?, ?> manifoldModule = manifoldsToCheck.remove(manifoldsToCheck.size() - 1);
                for (int i = 0; i < 6; i++) {
                    final var direction = directions[i];
                    final MultiblockTileModule<?, ?, ?> neighborModule = manifoldModule.getNeighbor(direction);
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
                    throw new ValidationException(Component.translatable("multiblock.error.biggerreactors.disconnected_manifold", pos.getX(), pos.getY(), pos.getZ()));
                }
            }
        }
    }
    
    private int foundRods = 0;
    private int foundManifolds = 0;
    
    private boolean updateBlockStates = false;
    
    private ReactorActivity reactorActivity = ReactorActivity.INACTIVE;
    
    private final Set<ReactorTerminalTile> terminals = new HashSet<>();
    private final ObjectArrayList<ReactorControlRodTile> controlRods = new ObjectArrayList<>();
    private final ObjectArrayList<ReactorFuelRodTile> fuelRods = new ObjectArrayList<>();
    // because class cast exception when getting elements, wrapping is required
    @SuppressWarnings("unchecked")
    private final ObjectArrayList<ObjectArrayList<ReactorFuelRodTile>> fuelRodsByLevel = ObjectArrayList.wrap(new ObjectArrayList[0]);
    ;
    private final Set<ReactorPowerTapTile> powerPorts = new HashSet<>();
    private final Set<ReactorAccessPortTile> accessPorts = new HashSet<>();
    private final Set<ReactorCoolantPortTile> coolantPorts = new HashSet<>();
    private final ObjectArrayList<ReactorManifoldTile> manifolds = new ObjectArrayList<>();
    
    @Override
    protected synchronized void onPartAdded(ReactorBaseTile tile) {
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
    protected synchronized void onPartRemoved(ReactorBaseTile tile) {
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
            level.setBlock(terminal.getBlockPos(), terminal.getBlockState().setValue(ReactorActivity.REACTOR_ACTIVITY_ENUM_PROPERTY, reactorActivity), 3);
            terminal.setChanged();
        });
    }
    
    public synchronized void setActive(ReactorActivity newState) {
        if (reactorActivity != newState) {
            reactorActivity = newState;
            updateBlockStates = true;
        }
    }
    
    public void toggleActive() {
        setActive(reactorActivity == ReactorActivity.ACTIVE ? ReactorActivity.INACTIVE : ReactorActivity.ACTIVE);
    }
    
    public boolean isActive() {
        return reactorActivity == ReactorActivity.ACTIVE;
    }
    
    @Override
    public CompoundTag mergeNBTs(CompoundTag nbtA, CompoundTag nbtB) {
        return nbtA;
    }
    
    @Override
    public void read(CompoundTag compound) {
        if (compound.contains("reactorState")) {
            reactorActivity = ReactorActivity.valueOf(compound.getString("reactorState").toUpperCase(Locale.US));
        }
        if (compound.contains("autoEjectWaste")) {
            autoEjectWaste = compound.getBoolean("autoEjectWaste");
        }
        
        if (compound.contains("simulationData")) {
            simulation = null;
            simulationData = new PhosphophylliteCompound(compound.getByteArray("simulationData"));
        }
        if (compound.contains("coolantTankWrapper")) {
            coolantTankNBT = compound.getCompound("coolantTankWrapper");
        }
        
        updateBlockStates = true;
    }
    
    @Nonnull
    public CompoundTag write() {
        CompoundTag compound = new CompoundTag();
        {
            compound.putString("reactorState", reactorActivity.toString());
            compound.putBoolean("autoEjectWaste", autoEjectWaste);
            if (simulation != null) {
                var phosCompound = simulation.save();
                if (phosCompound != null) {
                    compound.putByteArray("simulationData", phosCompound.toROBN());
                }
            }
            if (coolantTank != null) {
                compound.put("coolantTankWrapper", coolantTank.serializeNBT());
            }
        }
        return compound;
    }
    
    
    @Override
    protected void merge(ReactorMultiblockController other) {
//        if (state != AssemblyState.PAUSED) {
//            setActive(ReactorActivity.INACTIVE);
//        }
        distributeFuel();
        other.distributeFuel();
    }
    
    @Override
    public void onStateTransition(AssemblyState oldAssemblyState, AssemblyState newAssemblyState) {
        IRectangularMultiblock.super.onStateTransition(oldAssemblyState, newAssemblyState);
        if(newAssemblyState == AssemblyState.ASSEMBLED){
            onValidationPassed();
        }
    }
    
    protected void onValidationPassed() {
        SimulationDescription simulationDescription = new SimulationDescription();
        simulationDescription.setSize(max().x() - min().x() - 1, max().y() - min().y() - 1, max().z() - min().z() - 1);
        Vector3i start = new Vector3i(1).add(min());
        Vector3i end = new Vector3i(-1).add(max());
        Util.chunkCachedBlockStateIteration(start, end, level, (state, pos) -> {
            if (!(state.getBlock() instanceof ReactorBaseBlock)) {
                pos.sub(start);
                simulationDescription.setModeratorProperties(pos.x, pos.y, pos.z, ReactorModeratorRegistry.blockModeratorProperties(state.getBlock()));
            }
        });
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < manifolds.size(); i++) {
            BlockPos manifoldPos = manifolds.get(i).getBlockPos();
            simulationDescription.setManifold(manifoldPos.getX() - start.x, manifoldPos.getY() - start.y, manifoldPos.getZ() - start.z, true);
        }
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < controlRods.size(); i++) {
            BlockPos rodPos = controlRods.get(i).getBlockPos();
            simulationDescription.setControlRod(rodPos.getX() - start.x, rodPos.getZ() - start.z, true);
        }
        simulationDescription.setPassivelyCooled(coolantPorts.isEmpty());
        simulationDescription.setAmbientTemperature(293.15);
        var airProperties = ReactorModeratorRegistry.blockModeratorProperties(Blocks.AIR);
        if (airProperties == null) {
            airProperties = ReactorModeratorRegistry.ModeratorProperties.EMPTY_MODERATOR;
        }
        simulationDescription.setDefaultIModeratorProperties(airProperties);
        // if we already have a simulation, it is the reference
        if (simulation != null) {
            simulationData = simulation.save();
        }
        final var simulationBuilder = new SimulationDescription.Builder(Config.CONFIG.mode == Config.Mode.EXPERIMENTAL, Config.CONFIG.Reactor.useFullPassSimulation, Config.CONFIG.Reactor.allowOffThreadSimulation, Config.CONFIG.Reactor.allowMultiThreadSimulation, Config.CONFIG.Reactor.allowAcceleratedSimulation);
        simulation = simulationBuilder.build(simulationDescription);
        if (simulationData != null) {
            simulation.load(simulationData);
        }
        var simCoolantTank = simulation.coolantTank();
        if (simCoolantTank != null) {
            coolantTank = new ReactorTransitionTank(simCoolantTank);
            if (coolantTankNBT != null) {
                coolantTank.deserializeNBT(coolantTankNBT);
            }
        }
        
        updateControlRodLevels();
        collectFuel();
        
        int levels = this.max().y() - this.min().y() - 1;
        final int rodsPerLevel = fuelRods.size() / levels;
        fuelRodsByLevel.clear();
        fuelRodsByLevel.ensureCapacity(levels);
        for (int i = 0; i < levels; i++) {
            var newList = new ObjectArrayList<ReactorFuelRodTile>(rodsPerLevel);
            fuelRodsByLevel.add(newList);
        }
        final var levelArrays = fuelRodsByLevel.elements();
        final int minY = this.min().y() + 1;
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < fuelRods.size(); i++) {
            final var rod = fuelRods.get(i);
            int rodLevel = rod.getBlockPos().getY();
            rodLevel -= minY;
            levelArrays[rodLevel].add(rod);
        }
        
        updateFuelRenderingLevel(true);
    }
    
    @Override
    public void onDisassembled() {
        distributeFuel();
        setActive(ReactorActivity.INACTIVE);
        if (simulation != null) {
            simulationData = simulation.save();
            simulation = null;
        }
    }
    
    @Nullable
    private IReactorSimulation simulation;
    @Nullable
    PhosphophylliteCompound simulationData;
    @Nullable
    ReactorTransitionTank coolantTank;
    @Nullable
    CompoundTag coolantTankNBT;
    
    @Nullable
    public IReactorSimulation simulation() {
        return simulation;
    }
    
    @Nullable
    public ReactorTransitionTank coolantTank() {
        return coolantTank;
    }
    
    private boolean forceDirty = false;
    
    @Override
    public synchronized void tick() {
        
        if (updateBlockStates) {
            updateBlockStates = false;
            updateBlockStates();
        }
        
        if (simulation == null) {
            return;
        }
        
        simulation.tick(reactorActivity == ReactorActivity.ACTIVE);
        if (autoEjectWaste) {
            ejectWaste();
        }
        
        var battery = simulation.battery();
        if (battery != null) {
            long totalPowerRequested = 0;
            final long startingPower = battery.stored();
            for (ReactorPowerTapTile powerPort : powerPorts) {
                final long requested = powerPort.distributePower(startingPower, true);
                if (requested > startingPower) {
                    // bugged impl, ignoring
                    continue;
                }
                totalPowerRequested += requested;
            }
            
            float distributionMultiplier = Math.min(1f, (float) startingPower / (float) totalPowerRequested);
            for (ReactorPowerTapTile powerPort : powerPorts) {
                long powerRequested = powerPort.distributePower(startingPower, true);
                if (powerRequested > startingPower) {
                    // bugged impl, ignoring
                    continue;
                }
                powerRequested *= distributionMultiplier;
                powerRequested = Math.min(battery.stored(), powerRequested); // just in casei
                long powerAccepted = powerPort.distributePower(powerRequested, false);
                battery.extract(powerAccepted);
            }
        }
        
        // i know this is just a hose out, not sure if it should be changed or not
        if (coolantTank != null) {
            coolantPorts.forEach(ReactorCoolantPortTile::pushFluid);
        }
        
        updateFuelRenderingLevel();
        
        if (Phosphophyllite.tickNumber() % 2 == 0 || forceDirty) {
            forceDirty = false;
            dirty();
        }
    }
    
    long currentFuelRenderLevel = 0;
    long currentWasteRenderLevel = 0;
    
    private void updateFuelRenderingLevel() {
        updateFuelRenderingLevel(false);
    }
    
    private void updateFuelRenderingLevel(boolean forceFullUpdate) {
        
        if (simulation == null || simulation.fuelTank().capacity() == 0) {
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
                //noinspection ForLoopReplaceableByForEach
                for (int j = 0; j < levelRodCount; j++) {
                    final var currentFuelRod = rodLevel.get(j);
                    if (currentFuelRod.getBlockState() != newState) {
                        newStates.put(currentFuelRod.getBlockPos().asLong(), newState);
                        //noinspection deprecation
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
                //noinspection ForLoopReplaceableByForEach
                for (int j = 0; j < levelRodCount; j++) {
                    final var currentFuelRod = rodLevel.get(j);
                    if (currentFuelRod.getBlockState() != newState) {
                        newStates.put(currentFuelRod.getBlockPos().asLong(), newState);
                        //noinspection deprecation
                        currentFuelRod.setBlockState(newState);
                    }
                }
                
            }
        }
        
        Util.setBlockStates(newStates, level);
        
        currentFuelRenderLevel = fuelPixels;
        currentWasteRenderLevel = wastePixels;
    }
    
    private void distributeFuel() {
        if (simulation == null) {
            return;
        }
        if (simulation.fuelTank().totalStored() > 0 && !fuelRods.isEmpty()) {
            long fuelToDistribute = simulation.fuelTank().fuel();
            long fuelLeft = simulation.fuelTank().fuel();
            long wasteToDistribute = simulation.fuelTank().waste();
            long wasteLeft = simulation.fuelTank().waste();
            simulation.fuelTank().extractFuel(fuelToDistribute, false);
            simulation.fuelTank().extractWaste(wasteToDistribute, false);
            fuelToDistribute /= fuelRods.size();
            wasteToDistribute /= fuelRods.size();
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < fuelRods.size(); i++) {
                final var fuelRod = fuelRods.get(i);
                fuelRod.fuel = fuelToDistribute;
                fuelLeft -= fuelToDistribute;
                fuelRod.waste = wasteToDistribute;
                wasteLeft -= wasteToDistribute;
            }
            if (fuelLeft > 0 || wasteLeft > 0) {
                // got some residual, hose it out to whatever rod still has room
                final var fuelRodCapacity = Config.CONFIG.Reactor.PerFuelRodCapacity;
                for (int i = 0; i < fuelRods.size(); i++) {
                    final var fuelRod = fuelRods.get(i);
                    long spaceLeft = fuelRodCapacity - fuelRod.waste - fuelRod.fuel;
                    if (spaceLeft <= 0) {
                        continue;
                    }
                    long fuelToAdd = Math.min(spaceLeft, fuelLeft);
                    fuelLeft -= fuelToAdd;
                    spaceLeft -= fuelToAdd;
                    fuelRod.fuel += fuelToAdd;
                    long wasteToAdd = Math.min(spaceLeft, wasteLeft);
                    wasteLeft -= wasteToAdd;
                    fuelRod.waste += wasteToAdd;
                    if (fuelLeft <= 0 && wasteLeft <= 0) {
                        break;
                    }
                }
            }
            dirty();
        }
    }
    
    private void collectFuel() {
        if (simulation == null) {
            return;
        }
        long totalFuel = 0;
        long totalWaste = 0;
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < fuelRods.size(); i++) {
            final var fuelRod = fuelRods.get(i);
            totalFuel += fuelRod.fuel;
            totalWaste += fuelRod.waste;
            fuelRod.fuel = 0;
            fuelRod.waste = 0;
        }
        simulation.fuelTank().insertFuel(totalFuel, false);
        simulation.fuelTank().insertWaste(totalWaste, false);
        dirty();
    }
    
    private boolean autoEjectWaste = true;
    
    public synchronized void ejectWaste() {
        if (simulation == null) {
            return;
        }
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
        if (simulation.fuelTank().waste() > Config.CONFIG.Reactor.FuelMBPerIngot) {
            for (ReactorAccessPortTile accessPort : accessPorts) {
                long wastePushed = accessPort.pushWaste((int) simulation.fuelTank().waste(), false);
                forceDirty = simulation.fuelTank().extractWaste(wastePushed, false) > 0;
            }
        }
    }
    
    public synchronized long extractWaste(long mb, boolean simulated) {
        if (simulation == null || assemblyState() != AssemblyState.ASSEMBLED) {
            return 0;
        }
        long wasteExtracted = simulation.fuelTank().extractWaste(mb, simulated);
        forceDirty = wasteExtracted > 0 && !simulated;
        return wasteExtracted;
    }
    
    public synchronized long extractFuel(long mb, boolean simulated) {
        if (simulation == null || assemblyState() != AssemblyState.ASSEMBLED) {
            return 0;
        }
        long fuelExtracted = simulation.fuelTank().extractFuel(mb, simulated);
        forceDirty = fuelExtracted > 0 && !simulated;
        return fuelExtracted;
    }
    
    public synchronized long refuel(long mb, boolean simulated) {
        if (simulation == null || assemblyState() != AssemblyState.ASSEMBLED) {
            return 0;
        }
        long fuelInserted = simulation.fuelTank().insertFuel(mb, simulated);
        forceDirty = fuelInserted > 0 && !simulated;
        return fuelInserted;
    }
    
    public void updateReactorState(ReactorState reactorState) {
        if (simulation == null) {
            return;
        }
        
        final var battery = simulation.battery();
        final var coolantTank = simulation.coolantTank();
        final var coolantTankWrapper = this.coolantTank;
        if (battery == null && (coolantTank == null || coolantTankWrapper == null)) {
            return;
        }
        
        // TODO: These are mixed between the new enums and old booleans. Migrate them fully to enums.
        reactorState.reactorActivity = reactorActivity;
        reactorState.reactorType = simulation.battery() != null ? ReactorType.PASSIVE : ReactorType.ACTIVE;
        
        reactorState.doAutoEject = autoEjectWaste;
        
        if (battery != null) {
            reactorState.energyStored = battery.stored();
            reactorState.energyCapacity = battery.capacity();
        } else {
            reactorState.energyStored = 0;
            reactorState.energyCapacity = 0;
        }
        
        reactorState.wasteStored = simulation.fuelTank().waste();
        reactorState.fuelStored = simulation.fuelTank().fuel();
        reactorState.fuelCapacity = simulation.fuelTank().capacity();
        
        if (coolantTank != null && coolantTankWrapper != null) {
            reactorState.coolantStored = coolantTank.liquidAmount();
            reactorState.coolantCapacity = coolantTank.perSideCapacity();
            coolantTankWrapper.liquidType();
            reactorState.coolantResourceLocation = Objects.requireNonNull(ForgeRegistries.FLUIDS.getKey(coolantTankWrapper.liquidType())).toString();
            
            reactorState.exhaustStored = coolantTank.vaporAmount();
            reactorState.exhaustCapacity = coolantTank.perSideCapacity();
            coolantTankWrapper.vaporType();
            reactorState.exhaustResourceLocation = Objects.requireNonNull(ForgeRegistries.FLUIDS.getKey(coolantTankWrapper.vaporType())).toString();
        } else {
            reactorState.coolantStored = 0;
            reactorState.coolantCapacity = 0;
            reactorState.coolantResourceLocation = Objects.requireNonNull(ForgeRegistries.FLUIDS.getKey(Fluids.EMPTY)).toString();
            
            reactorState.exhaustStored = 0;
            reactorState.exhaustCapacity = 0;
            reactorState.exhaustResourceLocation = Objects.requireNonNull(ForgeRegistries.FLUIDS.getKey(Fluids.EMPTY)).toString();
        }
        reactorState.caseHeatStored = simulation.stackHeat();
        reactorState.fuelHeatStored = simulation.fuelHeat();
        
        reactorState.reactivityRate = simulation.fertility();
        reactorState.fuelUsageRate = simulation.fuelTank().burnedLastTick();
        reactorState.reactorOutputRate = battery != null ? battery.generatedLastTick() : coolantTank.transitionedLastTick();
    }
    
    public void runRequest(String requestName, @Nullable Object requestData) {
        switch (requestName) {
            // Set the reactor to ACTIVE or INACTIVE.
            case "setActive" -> {
                if (!(requestData instanceof Integer)) {
                    return;
                }
                setActive(ReactorActivity.fromInt((Integer) requestData));
            }
            
            // Enable or disable waste ejection.
            case "setAutoEject" -> {
                if (!(requestData instanceof Integer)) {
                    return;
                }
                autoEjectWaste = ((Integer) requestData != 0);
            }
            
            // Manually eject waste.
            case "ejectWaste" -> ejectWaste();
            
            // Manually dump tanks.
            case "dumpTanks" -> {
                if (coolantTank != null) {
                    coolantTank.dumpLiquid();
                    coolantTank.dumpVapor();
                }
            }
        }
    }
    
    @Nullable
    @Override
    public DebugInfo getControllerDebugInfo() {
        final var info = new DebugInfo("Reactor Controller");
        info.add("State: " + reactorActivity);
        info.add("AutoEjectWaste: " + autoEjectWaste);
        if (simulation == null) {
            info.add("Simulation is null");
        } else {
            info.add(simulation.getDebugInfo());
        }
        return info;
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
            if (simulation != null) {
                var simRod = simulation.controlRodAt(pos.getX() - min().x() - 1, pos.getZ() - min().z() - 1);
                if (simRod != null) {
                    simRod.setInsertion(rod.getInsertion());
                }
            }
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