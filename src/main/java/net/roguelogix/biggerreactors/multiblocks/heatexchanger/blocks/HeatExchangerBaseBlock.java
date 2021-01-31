package net.roguelogix.biggerreactors.multiblocks.heatexchanger.blocks;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.HeatExchangerMultiblockController;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles.HeatExchangerBaseTile;
import net.roguelogix.phosphophyllite.multiblock.rectangular.RectangularMultiblockBlock;

public class HeatExchangerBaseBlock extends RectangularMultiblockBlock<HeatExchangerMultiblockController, HeatExchangerBaseTile, HeatExchangerBaseBlock> {
    
    public static final Properties PROPERTIES_SOLID = Properties.create(Material.IRON).sound(SoundType.METAL).hardnessAndResistance(2, 10).setAllowsSpawn((a, b, c, d) -> false);
    
    public HeatExchangerBaseBlock() {
        super(PROPERTIES_SOLID);
    }
    
    @Override
    public boolean isGoodForInterior() {
        return false;
    }
    
    @Override
    public boolean isGoodForExterior() {
        return true;
    }
    
    @Override
    public boolean isGoodForFrame() {
        return false;
    }
}
