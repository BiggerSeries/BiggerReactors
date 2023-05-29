package net.roguelogix.biggerreactors.multiblocks.turbine.tiles;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.biggerreactors.multiblocks.turbine.TurbineMultiblockController;
import net.roguelogix.biggerreactors.multiblocks.turbine.blocks.TurbineBaseBlock;
import net.roguelogix.phosphophyllite.modular.tile.PhosphophylliteTile;
import net.roguelogix.phosphophyllite.multiblock2.IMultiblockTile;
import net.roguelogix.phosphophyllite.multiblock2.common.IPersistentMultiblockTile;
import net.roguelogix.phosphophyllite.multiblock2.rectangular.IRectangularMultiblockTile;
import net.roguelogix.phosphophyllite.multiblock2.touching.ITouchingMultiblockTile;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TurbineBaseTile extends PhosphophylliteTile implements IMultiblockTile<TurbineBaseTile, TurbineBaseBlock, TurbineMultiblockController>,
        IRectangularMultiblockTile<TurbineBaseTile, TurbineBaseBlock, TurbineMultiblockController>,
        IPersistentMultiblockTile<TurbineBaseTile, TurbineBaseBlock, TurbineMultiblockController>,
        ITouchingMultiblockTile<TurbineBaseTile, TurbineBaseBlock, TurbineMultiblockController> {
    public TurbineBaseTile(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
    }
    
    @Override
    public final TurbineMultiblockController createController() {
        return new TurbineMultiblockController(level);
    }
    
    public void runRequest(String requestName, Object requestData) {
        if (nullableController() != null) {
            controller().runRequest(requestName, requestData);
        }
    }
    
    public boolean isCurrentController(TurbineMultiblockController turbineMultiblockController) {
        return controller() == turbineMultiblockController;
    }
}
