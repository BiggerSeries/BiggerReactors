package net.roguelogix.biggerreactors.classic.reactor.tiles;

import net.minecraft.tileentity.TileEntityType;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;
import net.roguelogix.phosphophyllite.registry.TileSupplier;

@RegisterTileEntity(name = "reactor_casing")
public class ReactorCasingTile extends ReactorBaseTile {
    
    @RegisterTileEntity.Type
    public static TileEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final TileSupplier SUPPLIER = ReactorCasingTile::new;
    
    public ReactorCasingTile() {
        super(TYPE);
    }
}
