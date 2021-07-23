package net.roguelogix.biggerreactors.multiblocks.reactor.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.biggerreactors.multiblocks.reactor.tiles.ReactorControlRodTile;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nullable;

@RegisterBlock(name = "reactor_control_rod", tileEntityClass = ReactorControlRodTile.class)
public class ReactorControlRod extends ReactorBaseBlock {
    
    @RegisterBlock.Instance
    public static ReactorControlRod INSTANCE;
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactorControlRodTile(pos, state);
    }
}
