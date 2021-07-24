package net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;

@RegisterTileEntity(name = "heat_exchanger_condenser_channel")
public class HeatExchangerCondensorChannelTile extends HeatExchangerBaseTile {
    
    public long lastCheckedTick;
    
    @RegisterTileEntity.Type
    public static BlockEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final BlockEntityType.BlockEntitySupplier<HeatExchangerCondensorChannelTile> SUPPLIER = HeatExchangerCondensorChannelTile::new;
    
    public HeatExchangerCondensorChannelTile(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
}
