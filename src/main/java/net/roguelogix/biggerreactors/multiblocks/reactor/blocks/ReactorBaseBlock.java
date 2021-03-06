package net.roguelogix.biggerreactors.multiblocks.reactor.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.state.StateContainer;
import net.roguelogix.biggerreactors.multiblocks.reactor.ReactorMultiblockController;
import net.roguelogix.biggerreactors.multiblocks.reactor.state.ReactorActivity;
import net.roguelogix.biggerreactors.multiblocks.reactor.tiles.ReactorBaseTile;
import net.roguelogix.phosphophyllite.multiblock.rectangular.RectangularMultiblockBlock;

import javax.annotation.Nonnull;

public class ReactorBaseBlock extends RectangularMultiblockBlock<ReactorMultiblockController, ReactorBaseTile, ReactorBaseBlock> {
    
    public static final Properties PROPERTIES_SOLID = Properties.create(Material.IRON).sound(SoundType.METAL).hardnessAndResistance(2, 10).setAllowsSpawn((a, b, c, d) -> false);
    public static final Properties PROPERTIES_GLASS = Properties.create(Material.IRON).sound(SoundType.GLASS).notSolid().hardnessAndResistance(2).setAllowsSpawn((a, b, c, d) -> false);
    
    
    public ReactorBaseBlock() {
        this(true);
    }
    
    public ReactorBaseBlock(boolean solid) {
        super(solid ? PROPERTIES_SOLID : PROPERTIES_GLASS);
        if (usesReactorState()) {
            setDefaultState(getDefaultState().with(ReactorActivity.REACTOR_ACTIVITY_ENUM_PROPERTY, ReactorActivity.INACTIVE));
        }
    }
    
    public boolean usesReactorState() {
        return false;
    }
    
    @Override
    protected void fillStateContainer(@Nonnull StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
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
