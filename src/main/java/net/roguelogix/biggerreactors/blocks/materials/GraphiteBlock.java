package net.roguelogix.biggerreactors.blocks.materials;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

@RegisterBlock(name = "graphite_block")
public class GraphiteBlock extends Block {
    
    @RegisterBlock.Instance
    public static GraphiteBlock INSTANCE;
    
    public GraphiteBlock() {
        super(
                BlockBehaviour.Properties.of(Material.METAL)
                        .sound(SoundType.STONE)
                        .explosionResistance(1.0F)
        );
    }
}