package net.roguelogix.biggerreactors.multiblocks.turbine.blocks;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.biggerreactors.multiblocks.turbine.tiles.TurbineCasingTile;
import net.roguelogix.phosphophyllite.multiblock.modular.IAssemblyStateBlock;
import net.roguelogix.phosphophyllite.multiblock.modular.rectangular.IAxisPositionBlock;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@RegisterBlock(name = "turbine_casing", tileEntityClass = TurbineCasingTile.class)
public class TurbineCasing extends TurbineBaseBlock implements IAssemblyStateBlock, IAxisPositionBlock {
    
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
    public boolean isGoodForFrame() {
        return true;
    }
}
