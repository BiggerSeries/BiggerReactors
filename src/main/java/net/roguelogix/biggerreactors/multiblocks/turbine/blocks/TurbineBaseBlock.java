package net.roguelogix.biggerreactors.multiblocks.turbine.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Material;
import net.roguelogix.biggerreactors.multiblocks.turbine.TurbineMultiblockController;
import net.roguelogix.biggerreactors.multiblocks.turbine.state.TurbineActivity;
import net.roguelogix.biggerreactors.multiblocks.turbine.tiles.TurbineBaseTile;
import net.roguelogix.phosphophyllite.multiblock.rectangular.RectangularMultiblockBlock;

import javax.annotation.Nonnull;

public abstract class TurbineBaseBlock extends RectangularMultiblockBlock<TurbineMultiblockController, TurbineBaseTile, TurbineBaseBlock> {
    public static final Block.Properties PROPERTIES_SOLID = Block.Properties.of(Material.METAL).sound(SoundType.METAL).destroyTime(2).explosionResistance(10).isValidSpawn((a, b, c, d) -> false);
    public static final Block.Properties PROPERTIES_GLASS = Block.Properties.of(Material.METAL).sound(SoundType.METAL).noOcclusion().destroyTime(2).explosionResistance(2).isValidSpawn((a, b, c, d) -> false);
    
    
    public TurbineBaseBlock() {
        this(true);
    }
    
    public TurbineBaseBlock(boolean solid) {
        super(solid ? PROPERTIES_SOLID : PROPERTIES_GLASS);
        if (usesTurbineState()) {
            registerDefaultState(defaultBlockState().setValue(TurbineActivity.TURBINE_STATE_ENUM_PROPERTY, TurbineActivity.INACTIVE));
        }
    }
    
    public boolean usesTurbineState() {
        return false;
    }
    
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
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
