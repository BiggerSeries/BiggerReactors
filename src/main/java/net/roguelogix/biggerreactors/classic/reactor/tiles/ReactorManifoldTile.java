package net.roguelogix.biggerreactors.classic.reactor.tiles;

import net.minecraft.tileentity.TileEntityType;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;
import net.roguelogix.phosphophyllite.registry.TileSupplier;

@RegisterTileEntity(name = "reactor_manifold")
public class ReactorManifoldTile extends ReactorBaseTile {
    @RegisterTileEntity.Type
    public static TileEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final TileSupplier SUPPLIER = ReactorManifoldTile::new;
    
    public ReactorManifoldTile() {
        super(TYPE);
    }
    
    public long lastCheckedTick = Long.MIN_VALUE;
}
