package net.roguelogix.biggerreactors.multiblocks.turbine.blocks;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.roguelogix.biggerreactors.multiblocks.turbine.state.TurbineShaftRotationState;
import net.roguelogix.biggerreactors.multiblocks.turbine.tiles.TurbineRotorBladeTile;
import net.roguelogix.phosphophyllite.multiblock2.IAssemblyStateBlock;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static net.roguelogix.biggerreactors.multiblocks.turbine.state.TurbineShaftRotationState.Y;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TurbineRotorBlade extends TurbineBaseBlock implements IAssemblyStateBlock {
    
    public static final IntegerProperty BLADE_POSITION = IntegerProperty.create("blade_position", 0, 3);
    
    @RegisterBlock(name = "turbine_rotor_blade", tileEntityClass = TurbineRotorBladeTile.class)
    public static final TurbineRotorBlade INSTANCE = new TurbineRotorBlade();
    
    public TurbineRotorBlade() {
        super(false);
        registerDefaultState(defaultBlockState().setValue(TurbineShaftRotationState.TURBINE_SHAFT_ROTATION_STATE_ENUM_PROPERTY, Y));
    }
    
    @Override
    protected void buildStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TurbineShaftRotationState.TURBINE_SHAFT_ROTATION_STATE_ENUM_PROPERTY);
        builder.add(BLADE_POSITION);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TurbineRotorBladeTile.SUPPLIER.create(pos, state);
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
