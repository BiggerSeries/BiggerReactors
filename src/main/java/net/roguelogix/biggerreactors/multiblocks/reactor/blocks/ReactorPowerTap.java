package net.roguelogix.biggerreactors.multiblocks.reactor.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.roguelogix.biggerreactors.multiblocks.reactor.tiles.ReactorPowerTapTile;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static net.roguelogix.biggerreactors.multiblocks.reactor.blocks.ReactorPowerTap.ConnectionState.CONNECTION_STATE_ENUM_PROPERTY;

@RegisterBlock(name = "reactor_power_tap", tileEntityClass = ReactorPowerTapTile.class)
public class ReactorPowerTap extends ReactorBaseBlock {
    
    @RegisterBlock.Instance
    public static ReactorPowerTap INSTANCE;
    
    public ReactorPowerTap() {
        super();
        registerDefaultState(defaultBlockState().setValue(CONNECTION_STATE_ENUM_PROPERTY, ConnectionState.DISCONNECTED));
    }
    
    public enum ConnectionState implements StringRepresentable {
        CONNECTED,
        DISCONNECTED;
        
        public static final EnumProperty<ConnectionState> CONNECTION_STATE_ENUM_PROPERTY = EnumProperty.create("connectionstate", ConnectionState.class);
    
        @Override
        public String getSerializedName() {
            return toString().toLowerCase();
        }
        
    }
    
    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CONNECTION_STATE_ENUM_PROPERTY);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactorPowerTapTile(pos, state);
    }
    
    @Override
    public void neighborChanged(@Nonnull BlockState state, @Nonnull Level worldIn, @Nonnull BlockPos pos, @Nonnull Block blockIn, @Nonnull BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, worldIn, pos, blockIn, fromPos, isMoving);
        BlockEntity te = worldIn.getBlockEntity(pos);
        if (te instanceof ReactorPowerTapTile) {
            ((ReactorPowerTapTile) te).neighborChanged();
        }
    }
    
    @Override
    public boolean usesFaceDirection() {
        return true;
    }
}
