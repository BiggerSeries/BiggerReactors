package net.roguelogix.biggerreactors.multiblocks.reactor.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.biggerreactors.multiblocks.reactor.tiles.ReactorTerminalTile;
import net.roguelogix.phosphophyllite.registry.CreativeTabBlock;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nullable;

@CreativeTabBlock
@RegisterBlock(name = "reactor_terminal", tileEntityClass = ReactorTerminalTile.class)
public class ReactorTerminal extends ReactorBaseBlock {
    
    @RegisterBlock.Instance
    public static ReactorTerminal INSTANCE;
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactorTerminalTile(pos, state);
    }
    
    @Override
    public boolean usesReactorState() {
        return true;
    }
    
    @Override
    public boolean usesFaceDirection() {
        return true;
    }
}
