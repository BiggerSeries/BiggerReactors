package net.roguelogix.biggerreactors.multiblocks.turbine.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.biggerreactors.multiblocks.turbine.tiles.TurbineCasingTile;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nullable;

@RegisterBlock(name = "turbine_casing", tileEntityClass = TurbineCasingTile.class)
public class TurbineCasing extends TurbineBaseBlock {
    
    @RegisterBlock.Instance
    public static TurbineCasing INSTANCE;
    
    public TurbineCasing() {
        super();
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TurbineCasingTile(pos, state);
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
