package net.roguelogix.biggerreactors.multiblocks.reactor2.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.biggerreactors.multiblocks.reactor2.blocks.ReactorBaseBlock;
import net.roguelogix.biggerreactors.multiblocks.reactor2.ReactorMultiblockController;
import net.roguelogix.phosphophyllite.modular.tile.PhosphophylliteTile;
import net.roguelogix.phosphophyllite.multiblock2.IMultiblockTile;
import net.roguelogix.phosphophyllite.multiblock2.persistent.IPersistentMultiblockTile;
import net.roguelogix.phosphophyllite.multiblock2.rectangular.IRectangularMultiblockTile;
import net.roguelogix.phosphophyllite.multiblock2.touching.ITouchingMultiblockTile;
import net.roguelogix.phosphophyllite.registry.RegisterTile;
import net.roguelogix.phosphophyllite.util.NonnullDefault;

@NonnullDefault
public class ReactorBaseTile extends PhosphophylliteTile implements IMultiblockTile<ReactorBaseTile, ReactorBaseBlock, ReactorMultiblockController>, IPersistentMultiblockTile<ReactorBaseTile, ReactorBaseBlock, ReactorMultiblockController>, IRectangularMultiblockTile<ReactorBaseTile, ReactorBaseBlock, ReactorMultiblockController>, ITouchingMultiblockTile<ReactorBaseTile, ReactorBaseBlock, ReactorMultiblockController> {
    
    @RegisterTile("reactor2_basic_tile")
    public static final BlockEntityType.BlockEntitySupplier<ReactorBaseTile> BASE_SUPPLIER = new RegisterTile.Producer<>(ReactorBaseTile::new);
    
    public long lastCheckedTick = 0;
    
    public ReactorBaseTile(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
    }
    
    @Override
    public ReactorMultiblockController createController() {
        assert level != null;
        return new ReactorMultiblockController(level);
    }
}
