package net.roguelogix.biggerreactors.multiblocks.reactor.tiles;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.phosphophyllite.registry.RegisterTile;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ReactorManifoldTile extends ReactorBaseTile {
    
    @RegisterTile("reactor_manifold")
    public static final BlockEntityType.BlockEntitySupplier<ReactorManifoldTile> SUPPLIER = new RegisterTile.Producer<>(ReactorManifoldTile::new);
    
    public ReactorManifoldTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
    
    public long lastCheckedTick = Long.MIN_VALUE;
}
