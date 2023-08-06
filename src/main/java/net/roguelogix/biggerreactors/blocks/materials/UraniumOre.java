package net.roguelogix.biggerreactors.blocks.materials;

import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

public class UraniumOre extends DropExperienceBlock {

    @RegisterBlock(name = "uranium_ore")
    public static final UraniumOre INSTANCE = new UraniumOre();

    public UraniumOre() {
        super(
                Properties.of(Material.METAL)
                        .sound(SoundType.STONE)
                        .explosionResistance(3.0F)
                        .destroyTime(3.0f)
                        .requiresCorrectToolForDrops()
        );
    }
}
