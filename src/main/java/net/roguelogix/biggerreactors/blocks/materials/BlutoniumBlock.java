package net.roguelogix.biggerreactors.blocks.materials;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

public class BlutoniumBlock extends Block {
    
    @RegisterBlock(name = "blutonium_block")
    public static final BlutoniumBlock INSTANCE = new BlutoniumBlock();
    
    public BlutoniumBlock() {
        super(
                Properties.of(Material.METAL)
                        .sound(SoundType.STONE)
                        .explosionResistance(1.0F)
        );
    }
}