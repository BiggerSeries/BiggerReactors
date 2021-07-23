package net.roguelogix.biggerreactors.multiblocks.reactor.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Material;
import net.roguelogix.biggerreactors.multiblocks.reactor.ReactorMultiblockController;
import net.roguelogix.biggerreactors.multiblocks.reactor.state.ReactorActivity;
import net.roguelogix.biggerreactors.multiblocks.reactor.tiles.ReactorBaseTile;
import net.roguelogix.phosphophyllite.multiblock.rectangular.RectangularMultiblockBlock;

import javax.annotation.Nonnull;

public abstract class ReactorBaseBlock extends RectangularMultiblockBlock<ReactorMultiblockController, ReactorBaseTile, ReactorBaseBlock> {
    
    public static final Properties PROPERTIES_SOLID = Properties.of(Material.METAL).sound(SoundType.METAL).destroyTime(2).explosionResistance(10).isValidSpawn((a, b, c, d) -> false);
    public static final Properties PROPERTIES_GLASS = Properties.of(Material.METAL).sound(SoundType.GLASS).noOcclusion().destroyTime(2).explosionResistance(2).isValidSpawn((a, b, c, d) -> false);
    
    
    public ReactorBaseBlock() {
        this(true);
    }
    
    public ReactorBaseBlock(boolean solid) {
        super(solid ? PROPERTIES_SOLID : PROPERTIES_GLASS);
        if (usesReactorState()) {
            registerDefaultState(defaultBlockState().setValue(ReactorActivity.REACTOR_ACTIVITY_ENUM_PROPERTY, ReactorActivity.INACTIVE));
        }
    }
    
    public boolean usesReactorState() {
        return false;
    }
    
    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        if (usesReactorState()) {
            builder.add(ReactorActivity.REACTOR_ACTIVITY_ENUM_PROPERTY);
        }
    }
    
    @Override
    public boolean isGoodForInterior() {
        return false;
    }
    
    @Override
    public boolean isGoodForExterior() {
        return true;
    }
    
    @Override
    public boolean isGoodForFrame() {
        return false;
    }
}
