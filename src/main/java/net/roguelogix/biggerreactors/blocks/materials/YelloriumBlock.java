package net.roguelogix.biggerreactors.blocks.materials;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

public class YelloriumBlock extends Block {
    
    @RegisterBlock(name = "yellorium_block")
    public static final YelloriumBlock INSTANCE = new YelloriumBlock();
    
    public YelloriumBlock() {
        super(
                Properties.of(Material.METAL)
                        .sound(SoundType.STONE)
                        .explosionResistance(1.0F)
        );
    }
}