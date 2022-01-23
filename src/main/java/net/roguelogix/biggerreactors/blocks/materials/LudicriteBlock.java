package net.roguelogix.biggerreactors.blocks.materials;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

public class LudicriteBlock extends Block {
    
    @RegisterBlock(name = "ludicrite_block")
    public static final LudicriteBlock INSTANCE = new LudicriteBlock();
    
    public LudicriteBlock() {
        super(
                Properties.of(Material.METAL)
                        .sound(SoundType.STONE)
                        .explosionResistance(1.0F)
						.requiresCorrectToolForDrops()
        );
    }
}