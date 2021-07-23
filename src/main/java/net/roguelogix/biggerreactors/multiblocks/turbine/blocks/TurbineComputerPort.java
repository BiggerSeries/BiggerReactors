package net.roguelogix.biggerreactors.multiblocks.turbine.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.biggerreactors.multiblocks.turbine.tiles.TurbineComputerPortTile;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nullable;

@RegisterBlock(name = "turbine_computer_port", tileEntityClass = TurbineComputerPortTile.class)
public class TurbineComputerPort extends TurbineBaseBlock {
    @RegisterBlock.Instance
    public static TurbineComputerPort INSTANCE;
    
    public TurbineComputerPort() {
        super();
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TurbineComputerPortTile(pos, state);
    }
}
