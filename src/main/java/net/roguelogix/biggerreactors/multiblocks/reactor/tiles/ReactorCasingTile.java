package net.roguelogix.biggerreactors.multiblocks.reactor.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;

@RegisterTileEntity(name = "reactor_casing")
public class ReactorCasingTile extends ReactorBaseTile {
    
    @RegisterTileEntity.Type
    public static BlockEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final BlockEntityType.BlockEntitySupplier<ReactorCasingTile> SUPPLIER = ReactorCasingTile::new;
    
    public ReactorCasingTile(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
}
