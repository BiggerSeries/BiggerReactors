package net.roguelogix.biggerreactors.multiblocks.turbine.blocks;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.roguelogix.biggerreactors.multiblocks.turbine.state.TurbineShaftRotationState;
import net.roguelogix.biggerreactors.multiblocks.turbine.tiles.TurbineRotorShaftTile;
import net.roguelogix.phosphophyllite.multiblock2.IAssemblyStateBlock;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static net.roguelogix.biggerreactors.multiblocks.turbine.state.TurbineShaftRotationState.Y;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TurbineRotorShaft extends TurbineBaseBlock implements IAssemblyStateBlock {
    
    @RegisterBlock(name = "turbine_rotor_shaft", tileEntityClass = TurbineRotorShaftTile.class)
    public static final TurbineRotorShaft INSTANCE = new TurbineRotorShaft();
    
    public TurbineRotorShaft() {
        super(false);
        registerDefaultState(defaultBlockState().setValue(TurbineShaftRotationState.TURBINE_SHAFT_ROTATION_STATE_ENUM_PROPERTY, Y));
    }
    
    @Override
    protected void buildStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TurbineShaftRotationState.TURBINE_SHAFT_ROTATION_STATE_ENUM_PROPERTY);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TurbineRotorShaftTile.SUPPLIER.create(pos, state);
    }
    
    @SuppressWarnings("deprecation")
    public float getShadeBrightness(BlockState state, BlockGetter worldIn, BlockPos pos) {
        return 1.0F;
    }
    
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }
    
    @Override
    public boolean isGoodForInterior() {
        return true;
    }
    
    @Override
    public boolean isGoodForExterior() {
        return false;
    }
}
