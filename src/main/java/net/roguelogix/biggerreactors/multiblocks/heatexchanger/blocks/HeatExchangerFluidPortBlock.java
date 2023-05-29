package net.roguelogix.biggerreactors.multiblocks.heatexchanger.blocks;


import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles.HeatExchangerFluidPortTile;
import net.roguelogix.phosphophyllite.multiblock2.IAssemblyStateBlock;
import net.roguelogix.phosphophyllite.multiblock2.rectangular.IFaceDirectionBlock;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;
import net.roguelogix.phosphophyllite.util.Util;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static net.roguelogix.phosphophyllite.util.BlockStates.PORT_DIRECTION;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class HeatExchangerFluidPortBlock extends HeatExchangerBaseBlock implements IAssemblyStateBlock, IFaceDirectionBlock {
    
    public static final BooleanProperty CONDENSER = BooleanProperty.create("condenser");
    
    
    public HeatExchangerFluidPortBlock() {
        super();
        registerDefaultState(defaultBlockState().setValue(PORT_DIRECTION, false));
        registerDefaultState(defaultBlockState().setValue(CONDENSER, false));
    }
    
    @RegisterBlock(name = "heat_exchanger_fluid_port", tileEntityClass = HeatExchangerFluidPortTile.class)
    public static final HeatExchangerFluidPortBlock INSTANCE = new HeatExchangerFluidPortBlock();
    
    @Override
    protected void buildStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PORT_DIRECTION);
        builder.add(CONDENSER);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return HeatExchangerFluidPortTile.SUPPLIER.create(pos, state);
    }
    
    @Override
    public InteractionResult onUse(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        if (handIn == InteractionHand.MAIN_HAND) {
            if (Util.isWrench(player.getMainHandItem().getItem())) {
                boolean direction = !state.getValue(PORT_DIRECTION);
                state = state.setValue(PORT_DIRECTION, direction);
                worldIn.setBlock(pos, state, 3);
                if (!worldIn.isClientSide()) {
                    BlockEntity te = worldIn.getBlockEntity(pos);
                    if (te instanceof HeatExchangerFluidPortTile) {
                        ((HeatExchangerFluidPortTile) te).setInletOtherOutlet(direction);
                    }
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.onUse(state, worldIn, pos, player, handIn, hit);
    }
    
    @Override
    public void onNeighborChange(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        super.onNeighborChange(state, worldIn, pos, blockIn, fromPos, isMoving);
        BlockEntity te = worldIn.getBlockEntity(pos);
        if (te instanceof HeatExchangerFluidPortTile) {
            ((HeatExchangerFluidPortTile) te).neighborChanged();
        }
    }
}
