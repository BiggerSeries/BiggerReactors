package net.roguelogix.biggerreactors.multiblocks.turbine.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;
import net.roguelogix.phosphophyllite.registry.TileSupplier;

@RegisterTileEntity(name = "turbine_rotor_blade")
public class TurbineRotorBladeTile extends TurbineBaseTile{
    
    @RegisterTileEntity.Type
    public static BlockEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final TileSupplier SUPPLIER = TurbineRotorBladeTile::new;
    
    public TurbineRotorBladeTile(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
}
