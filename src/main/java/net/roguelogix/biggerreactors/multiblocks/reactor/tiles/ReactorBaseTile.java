package net.roguelogix.biggerreactors.multiblocks.reactor.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.biggerreactors.multiblocks.reactor.ReactorMultiblockController;
import net.roguelogix.biggerreactors.multiblocks.reactor.blocks.ReactorBaseBlock;
import net.roguelogix.phosphophyllite.multiblock.rectangular.RectangularMultiblockTile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ReactorBaseTile extends RectangularMultiblockTile<ReactorMultiblockController, ReactorBaseTile, ReactorBaseBlock> {
    
    public ReactorBaseTile(@Nonnull BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
    }
    
    @Override
    @Nonnull
    public final ReactorMultiblockController createController() {
        if (level == null) {
            throw new IllegalStateException("Attempt to create controller with null world");
        }
        return new ReactorMultiblockController(level);
    }
    
    public void runRequest(String requestName, Object requestData) {
        if (this.controller != null) {
            controller.runRequest(requestName, requestData);
        }
    }
    
    public boolean isCurrentController(@Nullable ReactorMultiblockController reactorMultiblockController) {
        return controller == reactorMultiblockController;
    }
    
    @Override
    public CompoundTag getUpdateTag() {
        return new CompoundTag();
    }
    
    @Override
    public void handleUpdateTag(CompoundTag tag) {
    }
}
