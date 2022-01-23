package net.roguelogix.biggerreactors.blocks.materials;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

public class UraniumBlock extends Block {
    
    @RegisterBlock(name = "uranium_block")
    public static final UraniumBlock INSTANCE = new UraniumBlock();
    
    public UraniumBlock() {
        super(
                Properties.of(Material.METAL)
                        .sound(SoundType.STONE)
                        .explosionResistance(1.0F)
						.requiresCorrectToolForDrops()
        );
    }
}