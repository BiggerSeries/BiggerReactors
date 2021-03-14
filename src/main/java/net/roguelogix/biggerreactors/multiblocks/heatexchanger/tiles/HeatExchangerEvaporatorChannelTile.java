package net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles;

import net.minecraft.tileentity.TileEntityType;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;
import net.roguelogix.phosphophyllite.registry.TileSupplier;

@RegisterTileEntity(name = "heat_exchanger_evaporator_channel")
public class HeatExchangerEvaporatorChannelTile extends HeatExchangerBaseTile {
    
    @RegisterTileEntity.Type
    public static TileEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final TileSupplier SUPPLIER = HeatExchangerEvaporatorChannelTile::new;
    
    public HeatExchangerEvaporatorChannelTile() {
        super(TYPE);
    }
}
