package net.roguelogix.biggerreactors.multiblocks.heatexchanger;

import net.minecraft.block.AirBlock;
import net.minecraft.world.World;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.blocks.HeatExchangerBaseBlock;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.blocks.HeatExchangerCasingBlock;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles.HeatExchangerBaseTile;
import net.roguelogix.phosphophyllite.multiblock.generic.Validator;
import net.roguelogix.phosphophyllite.multiblock.rectangular.RectangularMultiblockController;

import javax.annotation.Nonnull;

public class HeatExchangerMultiblockController extends RectangularMultiblockController<HeatExchangerMultiblockController, HeatExchangerBaseTile, HeatExchangerBaseBlock> {
    public HeatExchangerMultiblockController(@Nonnull World world) {
        super(world, tile -> tile instanceof HeatExchangerBaseTile, block -> block instanceof HeatExchangerBaseBlock);
        minSize.set(4, 3, 3);
        maxSize.set(-1, -1, -1);
        setAssemblyValidator(HeatExchangerMultiblockController::validate);
        frameValidator = block -> block instanceof HeatExchangerCasingBlock;
        exteriorValidator = Validator.or(frameValidator, block -> false);
        interiorValidator = block -> block instanceof AirBlock;
    }
    
    private boolean validate(){
        return true;
    }
}
