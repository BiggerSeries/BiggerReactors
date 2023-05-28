package net.roguelogix.biggerreactors.multiblocks.reactor2.simulation;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.roguelogix.biggerreactors.blocks.materials.MaterialBlock;
import net.roguelogix.phosphophyllite.util.NonnullDefault;

@NonnullDefault
public class ModeratorRegistry {
    private static final ObjectOpenHashSet<Block> validModeratorBlocks = new ObjectOpenHashSet<>();
    
    static {
        validModeratorBlocks.add(Blocks.AIR);
        validModeratorBlocks.add(Blocks.CAVE_AIR);
        validModeratorBlocks.add(Blocks.VOID_AIR);
        validModeratorBlocks.add(Blocks.DIAMOND_BLOCK);
        validModeratorBlocks.add(Blocks.EMERALD_BLOCK);
        validModeratorBlocks.add(MaterialBlock.GRAPHITE);
        validModeratorBlocks.add(MaterialBlock.LUDICRITE);
    }
    
    public static boolean isModerator(Block block) {
        return validModeratorBlocks.contains(block);
    }
}
