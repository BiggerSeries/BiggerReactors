package net.roguelogix.biggerreactors.multiblocks.heatexchanger.blocks;

import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.roguelogix.phosphophyllite.multiblock.rectangular.RectangularMultiblockBlock;

public class HeatExchangerBaseBlock extends RectangularMultiblockBlock {
    
    public static final Properties PROPERTIES_SOLID = Properties.create(Material.IRON).sound(SoundType.METAL).hardnessAndResistance(2, 10).setAllowsSpawn((a, b, c, d) -> false);
    
    public HeatExchangerBaseBlock() {
        super(PROPERTIES_SOLID);
    }
    
    @Override
    public final boolean hasTileEntity(BlockState state) {
        return true;
    }
}
