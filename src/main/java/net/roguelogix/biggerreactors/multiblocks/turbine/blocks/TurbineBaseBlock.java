package net.roguelogix.biggerreactors.multiblocks.turbine.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.state.StateContainer;
import net.roguelogix.biggerreactors.multiblocks.turbine.TurbineMultiblockController;
import net.roguelogix.biggerreactors.multiblocks.turbine.state.TurbineActivity;
import net.roguelogix.biggerreactors.multiblocks.turbine.tiles.TurbineBaseTile;
import net.roguelogix.phosphophyllite.multiblock.rectangular.RectangularMultiblockBlock;

import javax.annotation.Nonnull;

public class TurbineBaseBlock extends RectangularMultiblockBlock<TurbineMultiblockController, TurbineBaseTile, TurbineBaseBlock> {
    public static final Block.Properties PROPERTIES_SOLID = Block.Properties.create(Material.IRON).sound(SoundType.METAL).hardnessAndResistance(2, 10).setAllowsSpawn((a, b, c, d) -> false);
    public static final Block.Properties PROPERTIES_GLASS = Block.Properties.create(Material.IRON).sound(SoundType.METAL).notSolid().hardnessAndResistance(2).setAllowsSpawn((a, b, c, d) -> false);
    
    
    public TurbineBaseBlock() {
        this(true);
    }
    
    public TurbineBaseBlock(boolean solid) {
        super(solid ? PROPERTIES_SOLID : PROPERTIES_GLASS);
        if (usesTurbineState()) {
            setDefaultState(getDefaultState().with(TurbineActivity.TURBINE_STATE_ENUM_PROPERTY, TurbineActivity.INACTIVE));
        }
    }
    
    public boolean usesTurbineState() {
        return false;
    }
    
    @Override
    protected void fillStateContainer(@Nonnull StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        if(usesTurbineState()) {
            builder.add(TurbineActivity.TURBINE_STATE_ENUM_PROPERTY);
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
