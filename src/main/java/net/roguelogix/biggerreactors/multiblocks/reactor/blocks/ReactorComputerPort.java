package net.roguelogix.biggerreactors.multiblocks.reactor.blocks;

import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;
import net.roguelogix.biggerreactors.multiblocks.reactor.tiles.ReactorComputerPortTile;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nullable;

@RegisterBlock(name = "reactor_computer_port", tileEntityClass = ReactorComputerPortTile.class)
public class ReactorComputerPort extends ReactorBaseBlock {
    
    @RegisterBlock.Instance
    public static ReactorComputerPort INSTANCE;
    
    public ReactorComputerPort() {
        super();
    }
    
    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new ReactorComputerPortTile();
    }
    
    @Override
    public boolean usesFaceDirection() {
        return true;
    }
}
