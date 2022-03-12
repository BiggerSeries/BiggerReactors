package net.roguelogix.biggerreactors.multiblocks.reactor.tiles;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.phosphophyllite.registry.RegisterTile;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ReactorGlassTile extends ReactorBaseTile {
    
    @RegisterTile("reactor_glass")
    public static final BlockEntityType.BlockEntitySupplier<ReactorGlassTile> SUPPLIER = new RegisterTile.Producer<>(ReactorGlassTile::new);
    
    public ReactorGlassTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
}
