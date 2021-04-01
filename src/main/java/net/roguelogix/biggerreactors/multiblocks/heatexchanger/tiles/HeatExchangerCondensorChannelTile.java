package net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles;

import net.minecraft.tileentity.TileEntityType;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;
import net.roguelogix.phosphophyllite.registry.TileSupplier;

@RegisterTileEntity(name = "heat_exchanger_condenser_channel")
public class HeatExchangerCondensorChannelTile extends HeatExchangerBaseTile {
    
    public long lastCheckedTick;
    
    @RegisterTileEntity.Type
    public static TileEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final TileSupplier SUPPLIER = HeatExchangerCondensorChannelTile::new;
    
    public HeatExchangerCondensorChannelTile() {
        super(TYPE);
    }
}
