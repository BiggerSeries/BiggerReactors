package net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles;

import net.minecraft.tileentity.TileEntityType;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;
import net.roguelogix.phosphophyllite.registry.TileSupplier;

@RegisterTileEntity(name = "heat_exchanger_glass")
public class HeatExchangerGlassTile extends HeatExchangerBaseTile {
    
    @RegisterTileEntity.Type
    public static TileEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final TileSupplier SUPPLIER = HeatExchangerGlassTile::new;
    
    public HeatExchangerGlassTile() {
        super(TYPE);
    }
}
