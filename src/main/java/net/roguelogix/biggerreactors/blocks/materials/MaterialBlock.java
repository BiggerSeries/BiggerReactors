package net.roguelogix.biggerreactors.blocks.materials;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

public class MaterialBlock extends Block {
    @RegisterBlock(name = "blutonium_block")
    public static final MaterialBlock BLUTONIUM = new MaterialBlock();
    @RegisterBlock(name = "cyanite_block")
    public static final MaterialBlock CYANITE = new MaterialBlock();
    @RegisterBlock(name = "graphite_block")
    public static final MaterialBlock GRAPHITE = new MaterialBlock();
    @RegisterBlock(name = "ludicrite_block")
    public static final MaterialBlock LUDICRITE = new MaterialBlock();
    @RegisterBlock(name = "uranium_block")
    public static final MaterialBlock URANIUM = new MaterialBlock();
    
    public MaterialBlock() {
        super(Properties.of(Material.METAL)
                .sound(SoundType.STONE)
                .explosionResistance(1.0F)
                .destroyTime(2.0f)
                .requiresCorrectToolForDrops());
    }
}
