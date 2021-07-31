package net.roguelogix.biggerreactors.multiblocks.reactor.blocks;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.biggerreactors.multiblocks.reactor.tiles.ReactorTerminalTile;
import net.roguelogix.phosphophyllite.multiblock.IAssemblyStateBlock;
import net.roguelogix.phosphophyllite.multiblock.rectangular.IFaceDirectionBlock;
import net.roguelogix.phosphophyllite.registry.CreativeTabBlock;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@CreativeTabBlock
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ReactorTerminal extends ReactorBaseBlock implements IAssemblyStateBlock, IFaceDirectionBlock {
    
    @RegisterBlock(name = "reactor_terminal", tileEntityClass = ReactorTerminalTile.class)
    public static final ReactorTerminal INSTANCE = new ReactorTerminal();
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactorTerminalTile(pos, state);
    }
    
    @Override
    public boolean usesReactorState() {
        return true;
    }
}
