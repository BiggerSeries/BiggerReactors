package net.roguelogix.biggerreactors.multiblocks.reactor.tiles;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.biggerreactors.multiblocks.reactor.ReactorMultiblockController;
import net.roguelogix.biggerreactors.multiblocks.reactor.blocks.ReactorBaseBlock;
import net.roguelogix.phosphophyllite.modular.tile.PhosphophylliteTile;
import net.roguelogix.phosphophyllite.multiblock2.rectangular.IRectangularMultiblockTile;
import net.roguelogix.phosphophyllite.multiblock2.IMultiblockTile;
import net.roguelogix.phosphophyllite.multiblock2.common.IPersistentMultiblockTile;
import net.roguelogix.phosphophyllite.multiblock2.touching.ITouchingMultiblockTile;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ReactorBaseTile extends PhosphophylliteTile implements IMultiblockTile<ReactorBaseTile, ReactorBaseBlock, ReactorMultiblockController>,
        IRectangularMultiblockTile<ReactorBaseTile, ReactorBaseBlock, ReactorMultiblockController>,
        IPersistentMultiblockTile<ReactorBaseTile, ReactorBaseBlock, ReactorMultiblockController>,
        ITouchingMultiblockTile<ReactorBaseTile, ReactorBaseBlock, ReactorMultiblockController> {
    
    public int index = -1;
    
    public ReactorBaseTile(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
    }
    
    @Override
    public final ReactorMultiblockController createController() {
        if (level == null) {
            throw new IllegalStateException("Attempt to create controller with null world");
        }
        return new ReactorMultiblockController(level);
    }
    
    public void runRequest(String requestName, Object requestData) {
        if (nullableController() != null) {
            controller().runRequest(requestName, requestData);
        }
    }
    
    public boolean isCurrentController(@Nullable ReactorMultiblockController reactorMultiblockController) {
        return controller() == reactorMultiblockController;
    }
}
