package net.roguelogix.biggerreactors.blocks.materials;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

public class GraphiteBlock extends Block {
    
    @RegisterBlock(name = "graphite_block")
    public static final GraphiteBlock INSTANCE = new GraphiteBlock();
    
    public GraphiteBlock() {
        super(
                BlockBehaviour.Properties.of(Material.METAL)
                        .sound(SoundType.STONE)
                        .explosionResistance(1.0F)
						.requiresCorrectToolForDrops()
        );
    }
}