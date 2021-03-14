package net.roguelogix.biggerreactors.classic.reactor.tiles;

import net.minecraft.tileentity.TileEntityType;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;
import net.roguelogix.phosphophyllite.registry.TileSupplier;

@RegisterTileEntity(name = "reactor_glass")
public class ReactorGlassTile extends ReactorBaseTile {
    
    @RegisterTileEntity.Type
    public static TileEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final TileSupplier SUPPLIER = ReactorGlassTile::new;
    
    public ReactorGlassTile() {
        super(TYPE);
    }
}
