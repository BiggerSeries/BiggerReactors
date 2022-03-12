package net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.phosphophyllite.registry.RegisterTile;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class HeatExchangerEvaporatorChannelTile extends HeatExchangerBaseTile {
    
    public long lastCheckedTick;
    
    @RegisterTile("heat_exchanger_evaporator_channel")
    public static final BlockEntityType.BlockEntitySupplier<HeatExchangerEvaporatorChannelTile> SUPPLIER = new RegisterTile.Producer<>(HeatExchangerEvaporatorChannelTile::new);
    
    public HeatExchangerEvaporatorChannelTile(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
    }
}
