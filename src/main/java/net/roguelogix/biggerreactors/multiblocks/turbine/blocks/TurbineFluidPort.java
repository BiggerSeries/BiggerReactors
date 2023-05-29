package net.roguelogix.biggerreactors.multiblocks.turbine.blocks;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.roguelogix.biggerreactors.multiblocks.turbine.tiles.TurbineFluidPortTile;
import net.roguelogix.phosphophyllite.multiblock2.IAssemblyStateBlock;
import net.roguelogix.phosphophyllite.multiblock2.rectangular.IFaceDirectionBlock;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;
import net.roguelogix.phosphophyllite.util.Util;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Locale;

import static net.roguelogix.biggerreactors.multiblocks.turbine.blocks.TurbineFluidPort.PortDirection.*;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TurbineFluidPort extends TurbineBaseBlock implements IAssemblyStateBlock, IFaceDirectionBlock {
    @RegisterBlock(name = "turbine_fluid_port", tileEntityClass = TurbineFluidPortTile.class)
    public static final TurbineFluidPort INSTANCE = new TurbineFluidPort();
    
    public TurbineFluidPort() {
        super();
        registerDefaultState(defaultBlockState().setValue(PORT_DIRECTION_ENUM_PROPERTY, INLET));
    }
    
    public enum PortDirection implements StringRepresentable {
        INLET,
        OUTLET;
        
        @SuppressWarnings("SpellCheckingInspection")
        public static final EnumProperty<PortDirection> PORT_DIRECTION_ENUM_PROPERTY = EnumProperty.create("portdirection", PortDirection.class);
        
        @Override
        public String getSerializedName() {
            return toString().toLowerCase(Locale.US);
        }
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TurbineFluidPortTile.SUPPLIER.create(pos, state);
    }
    
    @Override
    protected void buildStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PORT_DIRECTION_ENUM_PROPERTY);
    }
    
    @Override
    public InteractionResult onUse(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        if (handIn == InteractionHand.MAIN_HAND) {
            if (Util.isWrench(player.getMainHandItem().getItem())) {
                PortDirection direction = state.getValue(PORT_DIRECTION_ENUM_PROPERTY);
                direction = direction == INLET ? OUTLET : INLET;
                state = state.setValue(PORT_DIRECTION_ENUM_PROPERTY, direction);
                worldIn.setBlockAndUpdate(pos, state);
                if (!worldIn.isClientSide()) {
                    BlockEntity te = worldIn.getBlockEntity(pos);
                    if (te instanceof TurbineFluidPortTile) {
                        ((TurbineFluidPortTile) te).setDirection(direction);
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
        if (te instanceof TurbineFluidPortTile) {
            ((TurbineFluidPortTile) te).neighborChanged();
        }
    }
}
