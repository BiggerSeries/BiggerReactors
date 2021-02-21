package net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles;

import net.minecraft.tileentity.TileEntityType;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;

@RegisterTileEntity(name = "heat_exchanger_evaporator_channel")
public class HeatExchangerEvaporatorChannelTile extends HeatExchangerBaseTile {
    
    @RegisterTileEntity.Type
    public static TileEntityType<?> TYPE;
    
    public HeatExchangerEvaporatorChannelTile() {
        super(TYPE);
    }
}
