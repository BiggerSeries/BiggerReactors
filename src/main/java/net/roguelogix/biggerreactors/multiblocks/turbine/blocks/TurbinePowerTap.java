package net.roguelogix.biggerreactors.multiblocks.turbine.blocks;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.roguelogix.biggerreactors.multiblocks.turbine.tiles.TurbinePowerTapTile;
import net.roguelogix.phosphophyllite.multiblock.IAssemblyStateBlock;
import net.roguelogix.phosphophyllite.multiblock.rectangular.IFaceDirectionBlock;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import java.util.Locale;

import static net.roguelogix.biggerreactors.multiblocks.turbine.blocks.TurbinePowerTap.ConnectionState.CONNECTION_STATE_ENUM_PROPERTY;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TurbinePowerTap extends TurbineBaseBlock implements IAssemblyStateBlock, IFaceDirectionBlock {
    
    @RegisterBlock(name = "turbine_power_tap", tileEntityClass = TurbinePowerTapTile.class)
    public static final TurbinePowerTap INSTANCE = new TurbinePowerTap();
    
    public TurbinePowerTap() {
        super();
        registerDefaultState(defaultBlockState().setValue(CONNECTION_STATE_ENUM_PROPERTY, ConnectionState.DISCONNECTED));
    }
    
    public enum ConnectionState implements StringRepresentable {
        CONNECTED,
        DISCONNECTED;
        
        @SuppressWarnings("SpellCheckingInspection")
        public static final EnumProperty<ConnectionState> CONNECTION_STATE_ENUM_PROPERTY = EnumProperty.create("connectionstate", ConnectionState.class);
        
        @Override
        public String getSerializedName() {
            return toString().toLowerCase(Locale.US);
        }
        
    }
    
    @Override
    protected void buildStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONNECTION_STATE_ENUM_PROPERTY);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TurbinePowerTapTile.SUPPLIER.create(pos, state);
    }
    
    @Override
    public void onNeighborChange(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        super.onNeighborChange(state, worldIn, pos, blockIn, fromPos, isMoving);
        BlockEntity te = worldIn.getBlockEntity(pos);
        if (te instanceof TurbinePowerTapTile) {
            ((TurbinePowerTapTile) te).neighborChanged();
        }
    }
}
