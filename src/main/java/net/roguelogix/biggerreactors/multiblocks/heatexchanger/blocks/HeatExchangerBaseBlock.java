package net.roguelogix.biggerreactors.multiblocks.heatexchanger.blocks;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.HeatExchangerMultiblockController;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles.HeatExchangerBaseTile;
import net.roguelogix.phosphophyllite.multiblock.rectangular.RectangularMultiblockBlock;

public abstract class HeatExchangerBaseBlock extends RectangularMultiblockBlock<HeatExchangerMultiblockController, HeatExchangerBaseTile, HeatExchangerBaseBlock> {
    
    public static final Properties PROPERTIES_SOLID = Properties.of(Material.METAL).sound(SoundType.METAL).destroyTime(2).explosionResistance(10).isValidSpawn((a, b, c, d) -> false);
    public static final Properties PROPERTIES_GLASS = Properties.of(Material.GLASS).sound(SoundType.GLASS).noOcclusion().destroyTime(2).explosionResistance(2).isValidSpawn((a, b, c, d) -> false);
    
    public HeatExchangerBaseBlock() {
        super(PROPERTIES_SOLID);
    }
    
    public HeatExchangerBaseBlock(Properties properties) {
        super(properties);
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
