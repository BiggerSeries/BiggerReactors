package net.roguelogix.biggerreactors.multiblocks.heatexchanger.blocks;


import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles.HeatExchangerGlassTile;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@RegisterBlock(name = "heat_exchanger_glass", tileEntityClass = HeatExchangerGlassTile.class)
public class HeatExchangerGlassBlock extends HeatExchangerBaseBlock {
    
    @RegisterBlock.Instance
    public static HeatExchangerGlassBlock INSTANCE;
    
    public HeatExchangerGlassBlock() {
        super(PROPERTIES_GLASS);
    }
    
    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new HeatExchangerGlassTile();
    }
    
    @OnlyIn(Dist.CLIENT)
    @RegisterBlock.RenderLayer
    public RenderType renderLayer() {
        return RenderType.getCutout();
    }
    
    @SuppressWarnings("deprecation")
    @OnlyIn(Dist.CLIENT)
    public float getAmbientOcclusionLightValue(@Nonnull BlockState state, @Nonnull IBlockReader worldIn, @Nonnull BlockPos pos) {
        return 1.0F;
    }
    
    public boolean propagatesSkylightDown(@Nonnull BlockState state, @Nonnull IBlockReader reader, @Nonnull BlockPos pos) {
        return true;
    }
    
    @Override
    public boolean connectedTexture() {
        return true;
    }
}
