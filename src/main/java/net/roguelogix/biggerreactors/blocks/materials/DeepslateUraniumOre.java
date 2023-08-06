package net.roguelogix.biggerreactors.blocks.materials;

import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;


public class DeepslateUraniumOre extends DropExperienceBlock {

    @RegisterBlock(name = "deepslate_uranium_ore")
    public static final DeepslateUraniumOre INSTANCE = new DeepslateUraniumOre();

    public DeepslateUraniumOre() {
        super(
                Properties.of()
                        .sound(SoundType.DEEPSLATE)
                        .explosionResistance(3.0F)
                        .destroyTime(4.5f)
                        .requiresCorrectToolForDrops()
        );
    }
}
