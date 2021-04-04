package net.roguelogix.biggerreactors.multiblocks.turbine.blocks;

import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;
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
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new TurbineComputerPortTile();
    }
}
