package net.roguelogix.biggerreactors.multiblocks.reactor.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;

@RegisterTileEntity(name = "reactor_manifold")
public class ReactorManifoldTile extends ReactorBaseTile {
    @RegisterTileEntity.Type
    public static BlockEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final BlockEntityType.BlockEntitySupplier<ReactorManifoldTile> SUPPLIER = ReactorManifoldTile::new;
    
    public ReactorManifoldTile(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
    
    public long lastCheckedTick = Long.MIN_VALUE;
}
