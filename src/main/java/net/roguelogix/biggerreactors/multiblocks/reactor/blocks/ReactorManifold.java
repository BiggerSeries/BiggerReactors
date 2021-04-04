package net.roguelogix.biggerreactors.multiblocks.reactor.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
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
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new ReactorManifoldTile();
    }
    
    @OnlyIn(Dist.CLIENT)
    @RegisterBlock.RenderLayer
    RenderType renderLayer() {
        return RenderType.getCutout();
    }
    
    @OnlyIn(Dist.CLIENT)
    public float getAmbientOcclusionLightValue(@Nonnull BlockState state, @Nonnull IBlockReader worldIn, @Nonnull BlockPos pos) {
        return 1.0F;
    }
    
    public boolean propagatesSkylightDown(@Nonnull BlockState state, @Nonnull IBlockReader reader, @Nonnull BlockPos pos) {
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
