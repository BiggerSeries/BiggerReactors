package net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.phosphophyllite.registry.RegisterTile;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class HeatExchangerCondenserChannelTile extends HeatExchangerBaseTile {
    
    public long lastCheckedTick;
    
    @RegisterTile("heat_exchanger_condenser_channel")
    public static final BlockEntityType.BlockEntitySupplier<HeatExchangerCondenserChannelTile> SUPPLIER = new RegisterTile.Producer<>(HeatExchangerCondenserChannelTile::new);
    
    public HeatExchangerCondenserChannelTile(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
    }
}
