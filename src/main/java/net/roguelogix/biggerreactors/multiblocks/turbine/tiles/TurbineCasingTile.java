package net.roguelogix.biggerreactors.multiblocks.turbine.tiles;

import net.minecraft.tileentity.TileEntityType;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;
import net.roguelogix.phosphophyllite.registry.TileSupplier;

@RegisterTileEntity(name = "turbine_casing")
public class TurbineCasingTile extends TurbineBaseTile {
    
    @RegisterTileEntity.Type
    public static TileEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final TileSupplier SUPPLIER = TurbineCasingTile::new;
    
    public TurbineCasingTile() {
        super(TYPE);
    }
}
