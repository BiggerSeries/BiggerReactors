package net.roguelogix.biggerreactors.multiblocks.reactor.blocks;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.biggerreactors.multiblocks.reactor.tiles.ReactorCasingTile;
import net.roguelogix.phosphophyllite.multiblock.modular.IAssemblyStateBlock;
import net.roguelogix.phosphophyllite.multiblock.modular.rectangular.IAxisPositionBlock;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@RegisterBlock(name = "reactor_casing", tileEntityClass = ReactorCasingTile.class)
public class ReactorCasing extends ReactorBaseBlock implements IAssemblyStateBlock, IAxisPositionBlock {
    
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
    public boolean isGoodForFrame() {
        return true;
    }
}
