package net.roguelogix.biggerreactors.multiblocks.reactor.blocks;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.roguelogix.biggerreactors.multiblocks.reactor.tiles.ReactorFuelRodTile;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ReactorFuelRod extends ReactorBaseBlock {
    
    public static IntegerProperty FUEL_HEIGHT_PROPERTY = IntegerProperty.create("fuel_level", 0, 16);
    public static IntegerProperty WASTE_HEIGHT_PROPERTY = IntegerProperty.create("waste_level", 0, 16);
    
    @RegisterBlock(name = "reactor_fuel_rod", tileEntityClass = ReactorFuelRodTile.class)
    public static final ReactorFuelRod INSTANCE = new ReactorFuelRod();
    
    public ReactorFuelRod() {
        super(false);
        this.registerDefaultState(this.defaultBlockState().setValue(FUEL_HEIGHT_PROPERTY, 0).setValue(WASTE_HEIGHT_PROPERTY, 0));
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactorFuelRodTile(pos, state);
    }
    
    @OnlyIn(Dist.CLIENT)
    @RegisterBlock.RenderLayer
    RenderType renderLayer() {
        return RenderType.cutout();
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public float getShadeBrightness(BlockState p_60472_, BlockGetter p_60473_, BlockPos p_60474_) {
        return 1.0f;
    }
    
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }
    
    @Override
    protected void buildStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.buildStateDefinition(builder);
        builder.add(FUEL_HEIGHT_PROPERTY);
        builder.add(WASTE_HEIGHT_PROPERTY);
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
