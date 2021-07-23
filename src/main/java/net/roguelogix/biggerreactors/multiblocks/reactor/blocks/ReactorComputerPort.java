package net.roguelogix.biggerreactors.multiblocks.reactor.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.biggerreactors.multiblocks.reactor.tiles.ReactorComputerPortTile;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nullable;

@RegisterBlock(name = "reactor_computer_port", tileEntityClass = ReactorComputerPortTile.class)
public class ReactorComputerPort extends ReactorBaseBlock {
    
    @RegisterBlock.Instance
    public static ReactorComputerPort INSTANCE;
    
    public ReactorComputerPort() {
        super();
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactorComputerPortTile(pos, state);
    }
    
    @Override
    public boolean usesFaceDirection() {
        return true;
    }
}
