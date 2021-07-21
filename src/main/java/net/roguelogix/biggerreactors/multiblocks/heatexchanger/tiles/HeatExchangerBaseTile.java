package net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.HeatExchangerMultiblockController;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.blocks.HeatExchangerBaseBlock;
import net.roguelogix.phosphophyllite.multiblock.rectangular.RectangularMultiblockTile;

import javax.annotation.Nonnull;

public class HeatExchangerBaseTile extends RectangularMultiblockTile<HeatExchangerMultiblockController, HeatExchangerBaseTile, HeatExchangerBaseBlock> {
    public HeatExchangerBaseTile(@Nonnull TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }
    
    @Nonnull
    @Override
    public HeatExchangerMultiblockController createController() {
        if (world == null) {
            throw new IllegalStateException("Attempt to create controller with null world");
        }
        return new HeatExchangerMultiblockController(world);
    }
    
    
    @Override
    public CompoundNBT getUpdateTag() {
        return new CompoundNBT();
    }
    
    @Override
    public void handleUpdateTag(BlockState state, CompoundNBT tag) {
    }
}
