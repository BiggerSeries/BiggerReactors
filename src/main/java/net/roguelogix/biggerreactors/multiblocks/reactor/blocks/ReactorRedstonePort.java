package net.roguelogix.biggerreactors.multiblocks.reactor.blocks;

import net.minecraft.MethodsReturnNonnullByDefault;
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
import net.roguelogix.phosphophyllite.multiblock2.IAssemblyStateBlock;
import net.roguelogix.phosphophyllite.multiblock2.rectangular.IFaceDirectionBlock;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ReactorRedstonePort extends ReactorBaseBlock implements IAssemblyStateBlock, IFaceDirectionBlock {
    
    public static BooleanProperty IS_LIT_BOOLEAN_PROPERTY = BooleanProperty.create("is_lit");
    
    @RegisterBlock(name = "reactor_redstone_port", tileEntityClass = ReactorRedstonePortTile.class)
    public static final ReactorRedstonePort INSTANCE = new ReactorRedstonePort();
    
    public ReactorRedstonePort() {
        super();
        registerDefaultState(defaultBlockState().setValue(IS_LIT_BOOLEAN_PROPERTY, false));
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ReactorRedstonePortTile.SUPPLIER.create(pos, state);
    }
    
    @Override
    public boolean isSignalSource(BlockState p_60457_) {
        return true;
    }
    
    @Override
    public int getSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
        BlockEntity tile = blockAccess.getBlockEntity(pos);
        if (tile instanceof ReactorRedstonePortTile) {
            return ((ReactorRedstonePortTile) tile).isEmitting(side) ? 15 : 0;
        }
        return super.getSignal(blockState, blockAccess, pos, side);
    }
    
    @Override
    public int getDirectSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
        BlockEntity tile = blockAccess.getBlockEntity(pos);
        if (tile instanceof ReactorRedstonePortTile) {
            return ((ReactorRedstonePortTile) tile).isEmitting(side) ? 15 : 0;
        }
        return super.getDirectSignal(blockState, blockAccess, pos, side);
    }
    
    @Override
    public void onNeighborChange(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        BlockEntity tile = worldIn.getBlockEntity(pos);
        if (tile instanceof ReactorRedstonePortTile) {
            ((ReactorRedstonePortTile) tile).updatePowered();
        }
    }
    
    @Override
    protected void buildStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(IS_LIT_BOOLEAN_PROPERTY);
    }
}
