package net.roguelogix.biggerreactors.multiblocks.turbine.tiles;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.roguelogix.biggerreactors.multiblocks.turbine.TurbineMultiblockController;
import net.roguelogix.biggerreactors.multiblocks.turbine.blocks.TurbineBaseBlock;
import net.roguelogix.phosphophyllite.multiblock.rectangular.RectangularMultiblockTile;

import javax.annotation.Nonnull;

public class TurbineBaseTile extends RectangularMultiblockTile<TurbineMultiblockController, TurbineBaseTile, TurbineBaseBlock> {
    public TurbineBaseTile(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }
    
    @Nonnull
    @Override
    public final TurbineMultiblockController createController() {
        return new TurbineMultiblockController(world);
    }
    
    public void runRequest(String requestName, Object requestData) {
        if (this.controller != null) {
            controller.runRequest(requestName, requestData);
        }
    }
    
    public boolean isCurrentController(TurbineMultiblockController turbineMultiblockController) {
        return controller == turbineMultiblockController;
    }
    
    
    @Override
    public CompoundNBT getUpdateTag() {
        return new CompoundNBT();
    }
    
    @Override
    public void handleUpdateTag(BlockState state, CompoundNBT tag) {
    }
}
