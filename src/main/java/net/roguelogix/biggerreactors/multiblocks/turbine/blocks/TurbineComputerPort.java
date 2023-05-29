package net.roguelogix.biggerreactors.multiblocks.turbine.blocks;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.biggerreactors.multiblocks.turbine.tiles.TurbineComputerPortTile;
import net.roguelogix.phosphophyllite.multiblock2.IAssemblyStateBlock;
import net.roguelogix.phosphophyllite.multiblock2.rectangular.IFaceDirectionBlock;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TurbineComputerPort extends TurbineBaseBlock implements IAssemblyStateBlock, IFaceDirectionBlock {
    
    @RegisterBlock(name = "turbine_computer_port", tileEntityClass = TurbineComputerPortTile.class)
    public static final TurbineComputerPort INSTANCE = new TurbineComputerPort();
    
    public TurbineComputerPort() {
        super();
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TurbineComputerPortTile.SUPPLIER.create(pos, state);
    }
}
