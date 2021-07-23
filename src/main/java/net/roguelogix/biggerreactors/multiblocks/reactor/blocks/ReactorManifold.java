package net.roguelogix.biggerreactors.multiblocks.reactor.blocks;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.roguelogix.biggerreactors.multiblocks.reactor.tiles.ReactorManifoldTile;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@RegisterBlock(name = "reactor_manifold", tileEntityClass = ReactorManifoldTile.class)
public class ReactorManifold extends ReactorBaseBlock {
    
    @RegisterBlock.Instance
    public static ReactorManifold INSTANCE;
    
    public ReactorManifold() {
        super(false);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactorManifoldTile(pos, state);
    }
    
    @OnlyIn(Dist.CLIENT)
    @RegisterBlock.RenderLayer
    RenderType renderLayer() {
        return RenderType.cutout();
    }
    
    @Override
    public float getShadeBrightness(BlockState p_60472_, BlockGetter p_60473_, BlockPos p_60474_) {
        return 1.0f;
    }
    
    public boolean propagatesSkylightDown(@Nonnull BlockState state, @Nonnull BlockGetter reader, @Nonnull BlockPos pos) {
        return true;
    }
    
    @Override
    public boolean usesAssemblyState() {
        return false;
    }
    
    @Override
    public boolean connectedTexture() {
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
    
    @Override
    protected boolean connectToBlock(Block block) {
        if(block instanceof ReactorBaseBlock){
            return !(block instanceof ReactorGlass) && ((ReactorBaseBlock) block).isGoodForExterior() || block == INSTANCE;
        }
        return false;
    }
}
