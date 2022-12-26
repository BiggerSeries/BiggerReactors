package net.roguelogix.biggerreactors.multiblocks.heatexchanger.blocks;


import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles.HeatExchangerCondenserChannelTile;
import net.roguelogix.phosphophyllite.modular.block.IConnectedTexture;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class HeatExchangerCondenserChannelBlock extends HeatExchangerBaseBlock implements IConnectedTexture {
    
    @RegisterBlock(name = "heat_exchanger_condenser_channel", tileEntityClass = HeatExchangerCondenserChannelTile.class)
    public static final HeatExchangerCondenserChannelBlock INSTANCE = new HeatExchangerCondenserChannelBlock();
    
    public HeatExchangerCondenserChannelBlock() {
        super(PROPERTIES_GLASS);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return HeatExchangerCondenserChannelTile.SUPPLIER.create(pos, state);
    }
    
    @SuppressWarnings("deprecation")
    public float getShadeBrightness(BlockState state, BlockGetter worldIn, BlockPos pos) {
        return 1.0F;
    }
    
    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }
    
    @Override
    public boolean connectToBlock(Block block) {
        return IConnectedTexture.super.connectToBlock(block) || block == HeatExchangerFluidPortBlock.INSTANCE;
    }
    
    @Override
    public boolean isGoodForExterior() {
        return false;
    }
    
    @Override
    public boolean isGoodForInterior() {
        return true;
    }
}
