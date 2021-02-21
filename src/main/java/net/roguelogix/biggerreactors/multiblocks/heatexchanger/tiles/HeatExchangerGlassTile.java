package net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles;

import net.minecraft.tileentity.TileEntityType;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;

@RegisterTileEntity(name = "heat_exchanger_glass")
public class HeatExchangerGlassTile extends HeatExchangerBaseTile {
    
    @RegisterTileEntity.Type
    public static TileEntityType<?> TYPE;
    
    public HeatExchangerGlassTile() {
        super(TYPE);
    }
}
