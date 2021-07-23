package net.roguelogix.biggerreactors.multiblocks.turbine.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.biggerreactors.multiblocks.turbine.tiles.TurbineTerminalTile;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nullable;

@RegisterBlock(name = "turbine_terminal", tileEntityClass = TurbineTerminalTile.class)
public class TurbineTerminal extends TurbineBaseBlock {
    
    @RegisterBlock.Instance
    public static TurbineTerminal INSTANCE;
    
    public TurbineTerminal() {
        super();
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TurbineTerminalTile(pos, state);
    }
    
    @Override
    public boolean usesTurbineState() {
        return true;
    }
}
