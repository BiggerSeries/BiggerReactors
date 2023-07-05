package net.roguelogix.biggerreactors.multiblocks.turbine.blocks;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.biggerreactors.multiblocks.turbine.tiles.TurbineTerminalTile;
import net.roguelogix.phosphophyllite.multiblock.IAssemblyStateBlock;
import net.roguelogix.phosphophyllite.multiblock.rectangular.IFaceDirectionBlock;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TurbineTerminal extends TurbineBaseBlock implements IAssemblyStateBlock, IFaceDirectionBlock {
    
    @RegisterBlock(name = "turbine_terminal", tileEntityClass = TurbineTerminalTile.class)
    public static final TurbineTerminal INSTANCE = new TurbineTerminal();
    
    public TurbineTerminal() {
        super();
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TurbineTerminalTile.SUPPLIER.create(pos, state);
    }
    
    @Override
    public boolean usesTurbineState() {
        return true;
    }
}
