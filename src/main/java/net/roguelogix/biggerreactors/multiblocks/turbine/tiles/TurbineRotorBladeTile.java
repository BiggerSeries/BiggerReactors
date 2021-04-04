package net.roguelogix.biggerreactors.multiblocks.turbine.tiles;

import net.minecraft.tileentity.TileEntityType;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;
import net.roguelogix.phosphophyllite.registry.TileSupplier;

@RegisterTileEntity(name = "turbine_rotor_blade")
public class TurbineRotorBladeTile extends TurbineBaseTile{
    
    @RegisterTileEntity.Type
    public static TileEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final TileSupplier SUPPLIER = TurbineRotorBladeTile::new;
    
    public TurbineRotorBladeTile() {
        super(TYPE);
    }
}
