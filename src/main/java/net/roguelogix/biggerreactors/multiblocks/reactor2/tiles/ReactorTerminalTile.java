package net.roguelogix.biggerreactors.multiblocks.reactor2.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.phosphophyllite.registry.RegisterTile;

public class ReactorTerminalTile extends ReactorBaseTile {
    @RegisterTile("reactor2_terminal")
    public static final BlockEntityType.BlockEntitySupplier<ReactorTerminalTile> TERMINAL_SUPPLIER = new RegisterTile.Producer<>(ReactorTerminalTile::new);
    
    public ReactorTerminalTile(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
    }
}
