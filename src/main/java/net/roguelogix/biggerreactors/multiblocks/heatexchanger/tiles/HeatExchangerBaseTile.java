package net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.HeatExchangerMultiblockController;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.blocks.HeatExchangerBaseBlock;
import net.roguelogix.phosphophyllite.modular.tile.PhosphophylliteTile;
import net.roguelogix.phosphophyllite.multiblock.common.IPersistentMultiblockTile;
import net.roguelogix.phosphophyllite.multiblock.rectangular.IRectangularMultiblockTile;
import net.roguelogix.phosphophyllite.multiblock.touching.ITouchingMultiblockTile;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class HeatExchangerBaseTile extends PhosphophylliteTile implements IRectangularMultiblockTile<HeatExchangerBaseTile, HeatExchangerBaseBlock, HeatExchangerMultiblockController>,
        IPersistentMultiblockTile<HeatExchangerBaseTile, HeatExchangerBaseBlock, HeatExchangerMultiblockController>,
        ITouchingMultiblockTile<HeatExchangerBaseTile, HeatExchangerBaseBlock, HeatExchangerMultiblockController> {
    public HeatExchangerBaseTile(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
    }
    
    @Nonnull
    @Override
    public HeatExchangerMultiblockController createController() {
        if (level == null) {
            throw new IllegalStateException("Attempt to create controller with null world");
        }
        return new HeatExchangerMultiblockController(level);
    }

    public void runRequest(String requestName, Object requestData) {
        if (nullableController() != null) {
            controller().runRequest(requestName, requestData);
        }
    }
}
