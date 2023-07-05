package net.roguelogix.biggerreactors.multiblocks.reactor2;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.roguelogix.biggerreactors.Config;
import net.roguelogix.biggerreactors.multiblocks.reactor.blocks.ReactorManifold;
import net.roguelogix.biggerreactors.multiblocks.reactor2.blocks.ReactorBlock;
import net.roguelogix.biggerreactors.multiblocks.reactor2.blocks.ReactorBlocks;
import net.roguelogix.biggerreactors.multiblocks.reactor2.simulation.ModeratorRegistry;
import net.roguelogix.biggerreactors.multiblocks.reactor2.tiles.ReactorTile;
import net.roguelogix.biggerreactors.multiblocks.reactor2.tiles.ReactorControlRodTile;
import net.roguelogix.biggerreactors.multiblocks.reactor2.tiles.ReactorFuelRodTile;
import net.roguelogix.biggerreactors.multiblocks.reactor2.tiles.ReactorTerminalTile;
import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.debug.DebugInfo;
import net.roguelogix.phosphophyllite.multiblock.MultiblockController;
import net.roguelogix.phosphophyllite.multiblock.MultiblockTileModule;
import net.roguelogix.phosphophyllite.multiblock.ValidationException;
import net.roguelogix.phosphophyllite.multiblock.common.IPersistentMultiblock;
import net.roguelogix.phosphophyllite.multiblock.rectangular.IRectangularMultiblock;
import net.roguelogix.phosphophyllite.multiblock.touching.ITouchingMultiblock;
import net.roguelogix.phosphophyllite.util.FastArraySet;
import net.roguelogix.phosphophyllite.util.NonnullDefault;
import net.roguelogix.phosphophyllite.util.Util;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Set;

@NonnullDefault
public class ReactorMultiblockController extends MultiblockController<ReactorTile, ReactorBlock, ReactorMultiblockController> implements IPersistentMultiblock<ReactorTile, ReactorBlock, ReactorMultiblockController>, IRectangularMultiblock<ReactorTile, ReactorBlock, ReactorMultiblockController>, ITouchingMultiblock<ReactorTile, ReactorBlock, ReactorMultiblockController> {
    
    private final FastArraySet<ReactorControlRodTile> controlRods = new FastArraySet<>();
    private final FastArraySet<ReactorTile> fuelRods = new FastArraySet<>();
    private final Set<ReactorTerminalTile> terminals = new ObjectOpenHashSet<>();
    
    private final Set<ReactorTile> powerTaps = new ObjectOpenHashSet<>();
    private final Set<ReactorTile> accessPorts = new ObjectOpenHashSet<>();
    private final Set<ReactorTile> coolantPorts = new ObjectOpenHashSet<>();
    private final Set<ReactorTile> manifolds = new ObjectOpenHashSet<>();
    
    private int foundRods = 0;
    private int foundManifolds = 0;
    
    public ReactorMultiblockController(Level level) {
        super(level, ReactorTile.class, ReactorBlock.class);
    }
    
    @Override
    public void validateStage1() throws ValidationException {
        if (blocks.size() < 27) {
            throw new ValidationException("minblocks");
        }
        if (controlRods.size() == 0) {
            throw new ValidationException("controlRodCount");
        }
        for (int i = 0; i < controlRods.size(); i++) {
            if (controlRods.get(i).getBlockPos().getY() != max().y()) {
                throw new ValidationException("controlRodsOnTop");
            }
        }
//         if (terminals.isEmpty()) {
//            throw new ValidationException("multiblock.error.biggerreactors.no_terminal");
//        }
    }
    
    @Override
    public void rectangularValidationStarted() {
        foundRods = 0;
        foundManifolds = 0;
    }
    
    @Override
    public void rectangularBlockValidated(Block block) {
        if (block instanceof ReactorBlock.FuelRod) {
            foundRods++;
        }
        if (block == ReactorManifold.INSTANCE) {
            foundManifolds++;
        }
    }
    
    @Override
    public void validateStage2() throws ValidationException {
        if (foundRods > fuelRods.size()) {
            Util.chunkCachedBlockStateIteration(min().add(1, 1, 1, new Vector3i()), max().sub(1, 1, 1, new Vector3i()), level, (state, position) -> {
                if (!(state.getBlock() instanceof ReactorBlock.FuelRod)) {
                    return;
                }
                if (blocks.getTile(position) instanceof ReactorFuelRodTile fuelRodTile && fuelRodTile.controller() != this) {
                    throw new ValidationException(Component.translatable("multiblock.error.biggerreactors.reactor.dangling_rod", position.toString()));
                }
            });
            throw new ValidationException(Component.translatable("multiblock.error.biggerreactors.reactor.dangling_rod_unknown_position"));
        }
        if(foundRods < fuelRods.size()) {
            throw new ValidationException(Component.translatable("multiblock.error.biggerreactors.reactor.unable_to_find_all_rods"));
        }
    }
    
    @Override
    public void validateStage3() throws ValidationException {
        final long tick = Phosphophyllite.tickNumber();
        
        final int maxY = max().y();
        final int internalMinY = min().y() + 1;
        
        int rodCount = 0;
        
        for (int i = 0; i < controlRods.size(); i++) {
            var controlRodPos = controlRods.get(i).getBlockPos();
            final int x = controlRodPos.getX(), z = controlRodPos.getZ();
            for (int j = internalMinY; j < maxY; j++) {
                final var tile = blocks.getTile(x, j, z);
                
                if (!(tile instanceof ReactorFuelRodTile)) {
                    throw new ValidationException(Component.translatable("multiblock.error.biggerreactors.reactor.fuel_rod_gap", controlRodPos.getX(), controlRodPos.getY() + (-1 - i), controlRodPos.getZ()));
                }
                
                tile.lastCheckedTick = tick;
                rodCount++;
            }
        }
        
        if (rodCount != fuelRods.size()) {
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < fuelRods.size(); i++) {
                final var fuelRod = fuelRods.get(i);
                if (fuelRod.lastCheckedTick != tick) {
                    throw new ValidationException(Component.translatable("multiblock.error.biggerreactors.reactor.no_control_rod_for_fuel_rod", fuelRod.getBlockPos().getX(), fuelRod.getBlockPos().getZ()));
                }
            }
            throw new ValidationException(Component.translatable("multiblock.error.biggerreactors.reactor.no_control_rod_for_fuel_rod_unknown_position"));
        }
        
        if (!manifolds.isEmpty()) {
            int checkedManifolds = 0;
            final var manifoldsToCheck = new ArrayList<MultiblockTileModule<ReactorTile, ReactorBlock, ReactorMultiblockController>>();
            
            final var directions = Direction.values();
            int minx = min().x() + 1, miny = min().y() + 1, minz = min().z() + 1;
            int maxx = max().x() - 1, maxy = max().y() - 1, maxz = max().z() - 1;
            for (ReactorTile manifold : manifolds) {
                BlockPos pos = manifold.getBlockPos();
                
                if (pos.getX() == minx || pos.getX() == maxx ||
                        pos.getY() == miny || pos.getY() == maxy ||
                        pos.getZ() == minz || pos.getZ() == maxz) {
                    var manifoldModule = manifold.multiblockModule();
                    for (int i = 0; i < 6; i++) {
                        final var direction = directions[i];
                        final var neighborModule = manifoldModule.getNeighbor(direction);
                        if (neighborModule == null) {
                            continue;
                        }
                        final ReactorTile neighborTile = neighborModule.iface;
                        if (!(neighborTile.getBlockState().getBlock() == ReactorBlocks.GLASS) && ((ReactorBlock) neighborTile.getBlockState().getBlock()).isGoodForExterior()) {
                            manifoldsToCheck.add(manifoldModule);
                            manifold.lastCheckedTick = tick;
                            checkedManifolds++;
                            break;
                        }
                    }
                }
            }
            
            while (!manifoldsToCheck.isEmpty()) {
                // done like this to avoid array shuffling
                var manifoldModule = manifoldsToCheck.remove(manifoldsToCheck.size() - 1);
                for (int i = 0; i < 6; i++) {
                    final var direction = directions[i];
                    final var neighborModule = manifoldModule.getNeighbor(direction);
                    if (neighborModule == null) {
                        continue;
                    }
                    final ReactorTile neighborTile = neighborModule.iface;
                    if (neighborTile.getBlockState().getBlock() == ReactorBlocks.MANIFOLD) {
                        if (neighborTile.lastCheckedTick != tick) {
                            manifoldsToCheck.add(neighborModule);
                            neighborTile.lastCheckedTick = tick;
                            checkedManifolds++;
                        }
                    }
                }
            }
            
            if (manifolds.size() != checkedManifolds) {
                for (ReactorTile manifold : manifolds) {
                    if (manifold.lastCheckedTick != tick) {
                        BlockPos pos = manifold.getBlockPos();
                        throw new ValidationException(Component.translatable("multiblock.error.biggerreactors.disconnected_manifold", pos.getX(), pos.getY(), pos.getZ()));
                    }
                }
                throw new ValidationException(Component.translatable("multiblock.error.biggerreactors.disconnected_manifold_unknown_pos"));
            }
        }
    }
    
    @Override
    public boolean allowedInteriorBlock(Block block) {
        return ModeratorRegistry.isModerator(block);
    }
    
    @Override
    public CompoundTag mergeNBTs(CompoundTag nbtA, CompoundTag nbtB) {
        return nbtA;
    }
    
    String nbtTestValue = Integer.toHexString(this.hashCode());
    
    @Override
    public void read(CompoundTag nbt) {
        if (nbt.contains("testValue")) {
            nbtTestValue = nbt.getString("testValue");
        }
    }
    
    @Nullable
    @Override
    public CompoundTag write() {
        final var tag = new CompoundTag();
        tag.putString("testValue", nbtTestValue);
        return tag;
    }
    
    @Nullable
    @Override
    public Vector3ic minSize() {
        return new Vector3i(3, 3, 3);
    }
    
    @Nullable
    @Override
    public Vector3ic maxSize() {
        return new Vector3i(Config.CONFIG.Reactor.MaxLength, Config.CONFIG.Reactor.MaxHeight, Config.CONFIG.Reactor.MaxWidth);
    }
    
    @Override
    protected void onPartAdded(@Nonnull ReactorTile tile) {
        if (tile instanceof ReactorControlRodTile rod) {
            controlRods.add(rod);
        }
        if (tile instanceof ReactorFuelRodTile rod) {
            fuelRods.add(rod);
        }
        final var block = tile.getBlockState().getBlock();
        if(block == ReactorBlocks.MANIFOLD) {
            manifolds.add(tile);
        }
    }
    
    @Override
    protected void onPartRemoved(@Nonnull ReactorTile tile) {
        if (tile instanceof ReactorControlRodTile rod) {
            controlRods.remove(rod);
        }
        if (tile instanceof ReactorFuelRodTile rod) {
            fuelRods.remove(rod);
        }
        final var block = tile.getBlockState().getBlock();
        if(block == ReactorBlocks.MANIFOLD) {
            manifolds.remove(tile);
        }
    }
    
    @Nonnull
    @Override
    public DebugInfo getControllerDebugInfo() {
        final var debugInfo = new DebugInfo(this.getClass().getSimpleName());
        debugInfo.add("TestValue: " + nbtTestValue);
        return debugInfo;
    }
    
    @Override
    public void tick() {
        dirty();
    }
}
