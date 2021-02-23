package net.roguelogix.biggerreactors.classic.reactor.tiles;

import net.minecraft.tileentity.TileEntityType;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;

@RegisterTileEntity(name = "reactor_manifold")
public class ReactorManifoldTile extends ReactorBaseTile {
    @RegisterTileEntity.Type
    public static TileEntityType<?> TYPE;
    
    public ReactorManifoldTile() {
        super(TYPE);
    }
}
