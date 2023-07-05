package net.roguelogix.biggerreactors.multiblocks.reactor2.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.biggerreactors.multiblocks.reactor2.blocks.ReactorBlock;
import net.roguelogix.biggerreactors.multiblocks.reactor2.ReactorMultiblockController;
import net.roguelogix.phosphophyllite.modular.tile.PhosphophylliteTile;
import net.roguelogix.phosphophyllite.multiblock.IMultiblockTile;
import net.roguelogix.phosphophyllite.multiblock.common.IPersistentMultiblockTile;
import net.roguelogix.phosphophyllite.multiblock.rectangular.IRectangularMultiblockTile;
import net.roguelogix.phosphophyllite.multiblock.touching.ITouchingMultiblockTile;
import net.roguelogix.phosphophyllite.util.NonnullDefault;

@NonnullDefault
public class ReactorTile extends PhosphophylliteTile implements IMultiblockTile<ReactorTile, ReactorBlock, ReactorMultiblockController>, IPersistentMultiblockTile<ReactorTile, ReactorBlock, ReactorMultiblockController>, IRectangularMultiblockTile<ReactorTile, ReactorBlock, ReactorMultiblockController>, ITouchingMultiblockTile<ReactorTile, ReactorBlock, ReactorMultiblockController> {
    
    public long lastCheckedTick = 0;
    
    public ReactorTile(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
    }
    
    @Override
    public ReactorMultiblockController createController() {
        assert level != null;
        return new ReactorMultiblockController(level);
    }
}
