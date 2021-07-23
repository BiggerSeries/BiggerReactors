package net.roguelogix.biggerreactors.multiblocks.reactor.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.biggerreactors.multiblocks.reactor.tiles.ReactorCasingTile;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nullable;

@RegisterBlock(name = "reactor_casing", tileEntityClass = ReactorCasingTile.class)
public class ReactorCasing extends ReactorBaseBlock {
    
    @RegisterBlock.Instance
    public static ReactorCasing INSTANCE;
    
    public ReactorCasing() {
        super();
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactorCasingTile(pos, state);
    }
    
    @Override
    public boolean usesAxisPositions() {
        return true;
    }
    
    @Override
    public boolean isGoodForFrame() {
        return true;
    }
}
