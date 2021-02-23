package net.roguelogix.biggerreactors.classic.reactor.blocks;

import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.roguelogix.biggerreactors.classic.reactor.tiles.ReactorManifoldTile;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nullable;

@RegisterBlock(name = "reactor_manifold", tileEntityClass = ReactorManifoldTile.class)
public class ReactorManifold extends ReactorBaseBlock {
    
    @RegisterBlock.Instance
    public static ReactorManifold INSTANCE;
    
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
}