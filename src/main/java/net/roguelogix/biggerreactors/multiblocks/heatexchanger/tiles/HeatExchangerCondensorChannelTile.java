package net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles;

import net.minecraft.tileentity.TileEntityType;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;

@RegisterTileEntity(name = "heat_exchanger_condenser_channel")
public class HeatExchangerCondensorChannelTile extends HeatExchangerBaseTile {
    
    @RegisterTileEntity.Type
    public static TileEntityType<?> TYPE;
    
    public HeatExchangerCondensorChannelTile() {
        super(TYPE);
    }
}
