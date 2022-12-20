package net.roguelogix.biggerreactors.multiblocks.reactor2;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.roguelogix.biggerreactors.Config;
import net.roguelogix.biggerreactors.multiblocks.reactor2.blocks.ReactorBaseBlock;
import net.roguelogix.biggerreactors.multiblocks.reactor2.tiles.ReactorBaseTile;
import net.roguelogix.biggerreactors.multiblocks.reactor2.tiles.ReactorControlRodTile;
import net.roguelogix.biggerreactors.multiblocks.reactor2.tiles.ReactorFuelRodTile;
import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.debug.DebugInfo;
import net.roguelogix.phosphophyllite.multiblock2.MultiblockController;
import net.roguelogix.phosphophyllite.multiblock2.ValidationException;
import net.roguelogix.phosphophyllite.multiblock2.common.IPersistentMultiblock;
import net.roguelogix.phosphophyllite.multiblock2.rectangular.IRectangularMultiblock;
import net.roguelogix.phosphophyllite.multiblock2.touching.ITouchingMultiblock;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3i;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3ic;
import net.roguelogix.phosphophyllite.util.FastArraySet;
import net.roguelogix.phosphophyllite.util.NonnullDefault;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@NonnullDefault
public class ReactorMultiblockController extends MultiblockController<ReactorBaseTile, ReactorBaseBlock, ReactorMultiblockController> implements IPersistentMultiblock<ReactorBaseTile, ReactorBaseBlock, ReactorMultiblockController>, IRectangularMultiblock<ReactorBaseTile, ReactorBaseBlock, ReactorMultiblockController>, ITouchingMultiblock<ReactorBaseTile, ReactorBaseBlock, ReactorMultiblockController> {
    
    private final FastArraySet<ReactorControlRodTile> controlRods = new FastArraySet<>();
    private final FastArraySet<ReactorBaseTile> fuelRods = new FastArraySet<>();
    
    private int foundRods = 0;
    private int foundManifolds = 0;
    
    public ReactorMultiblockController(Level level) {
        super(level, ReactorBaseTile.class, ReactorBaseBlock.class);
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
    }
    
    @Override
    public void rectangularValidationStarted() {
        foundRods = 0;
    }
    
    @Override
    public void rectangularBlockValidated(Block block) {
        if (block == ReactorBaseBlock.FUEL_ROD) {
            foundRods++;
        }
//            if (block == ReactorManifold.INSTANCE) {
//                foundManifolds++;
//            }
    }
    
    @Override
    public void validateStage2() throws ValidationException {
        if (foundRods > fuelRods.size()) {
            // TODO: find the position of it
            throw new ValidationException(Component.translatable("multiblock.error.biggerreactors.dangling_rod"));
        }
        
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
                    throw new ValidationException(Component.translatable("multiblock.error.biggerreactors.fuel_rod_gap", controlRodPos.getX(), controlRodPos.getY() + (-1 - i), controlRodPos.getZ()));
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
                    throw new ValidationException(Component.translatable("multiblock.error.biggerreactors.no_control_rod_for_fuel_rod", fuelRod.getBlockPos().getX(), fuelRod.getBlockPos().getZ()));
                }
            }
        }
    }
    
    @Override
    public boolean allowedInteriorBlock(Block block) {
        return true;
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
    protected void onPartAdded(@Nonnull ReactorBaseTile tile) {
        if (tile instanceof ReactorControlRodTile rod) {
            controlRods.add(rod);
        }
        if (tile instanceof ReactorFuelRodTile rod) {
            fuelRods.add(rod);
        }
    }
    
    @Override
    protected void onPartRemoved(@Nonnull ReactorBaseTile tile) {
        if (tile instanceof ReactorControlRodTile rod) {
            controlRods.remove(rod);
        }
        if (tile instanceof ReactorFuelRodTile rod) {
            fuelRods.remove(rod);
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
