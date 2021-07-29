package net.roguelogix.biggerreactors.multiblocks.turbine.blocks;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.biggerreactors.multiblocks.turbine.tiles.TurbineComputerPortTile;
import net.roguelogix.phosphophyllite.multiblock.modular.IAssemblyStateBlock;
import net.roguelogix.phosphophyllite.multiblock.modular.rectangular.IFaceDirectionBlock;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@RegisterBlock(name = "turbine_computer_port", tileEntityClass = TurbineComputerPortTile.class)
public class TurbineComputerPort extends TurbineBaseBlock implements IAssemblyStateBlock, IFaceDirectionBlock {
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
