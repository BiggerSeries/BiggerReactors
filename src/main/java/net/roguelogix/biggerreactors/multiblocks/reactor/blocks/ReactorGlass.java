package net.roguelogix.biggerreactors.multiblocks.reactor.blocks;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.biggerreactors.multiblocks.reactor.tiles.ReactorGlassTile;
import net.roguelogix.phosphophyllite.modular.block.IConnectedTexture;
import net.roguelogix.phosphophyllite.multiblock.IAssemblyStateBlock;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ReactorGlass extends ReactorBaseBlock implements IAssemblyStateBlock, IConnectedTexture {
    
    @RegisterBlock(name = "reactor_glass", tileEntityClass = ReactorGlassTile.class)
    public static final ReactorGlass INSTANCE = new ReactorGlass();
    
    public ReactorGlass() {
        super(false);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ReactorGlassTile.SUPPLIER.create(pos, state);
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public float getShadeBrightness(BlockState p_60472_, BlockGetter p_60473_, BlockPos p_60474_) {
        return 1.0f;
    }
    
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }
}
