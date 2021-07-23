package net.roguelogix.biggerreactors.multiblocks.reactor.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.roguelogix.biggerreactors.multiblocks.reactor.tiles.ReactorRedstonePortTile;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@RegisterBlock(name = "reactor_redstone_port", tileEntityClass = ReactorRedstonePortTile.class)
public class ReactorRedstonePort extends ReactorBaseBlock {
    @RegisterBlock.Instance
    public static ReactorRedstonePort INSTANCE;
    
    public ReactorRedstonePort() {
        super();
        registerDefaultState(defaultBlockState().setValue(IS_LIT_BOOLEAN_PROPERTY, false));
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactorRedstonePortTile(pos, state);
    }
    
    @Override
    public boolean isSignalSource(BlockState p_60457_) {
        return true;
    }
    
    @Override
    public int getSignal(@Nonnull BlockState blockState, @Nonnull BlockGetter blockAccess, @Nonnull BlockPos pos, @Nonnull Direction side) {
        BlockEntity tile = blockAccess.getBlockEntity(pos);
        if (tile instanceof ReactorRedstonePortTile) {
            return ((ReactorRedstonePortTile) tile).isEmitting(side) ? 15 : 0;
        }
        return super.getSignal(blockState, blockAccess, pos, side);
    }
    
    @Override
    public int getDirectSignal(@Nonnull BlockState blockState, @Nonnull BlockGetter blockAccess, @Nonnull BlockPos pos, @Nonnull Direction side) {
        BlockEntity tile = blockAccess.getBlockEntity(pos);
        if (tile instanceof ReactorRedstonePortTile) {
            return ((ReactorRedstonePortTile) tile).isEmitting(side) ? 15 : 0;
        }
        return super.getDirectSignal(blockState, blockAccess, pos, side);
    }
    
    @Override
    public void neighborChanged(@Nonnull BlockState state, @Nonnull Level worldIn, @Nonnull BlockPos pos, @Nonnull Block blockIn, @Nonnull BlockPos fromPos, boolean isMoving) {
        BlockEntity tile = worldIn.getBlockEntity(pos);
        if (tile instanceof ReactorRedstonePortTile) {
            ((ReactorRedstonePortTile) tile).updatePowered();
        }
    }
    
    @Override
    public boolean usesFaceDirection() {
        return true;
    }
    
    public static BooleanProperty IS_LIT_BOOLEAN_PROPERTY = BooleanProperty.create("is_lit");
    
    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(IS_LIT_BOOLEAN_PROPERTY);
        super.createBlockStateDefinition(builder);
    }
}
