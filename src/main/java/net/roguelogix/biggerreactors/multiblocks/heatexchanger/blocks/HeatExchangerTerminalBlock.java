package net.roguelogix.biggerreactors.multiblocks.heatexchanger.blocks;


import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles.HeatExchangerTerminalTile;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nullable;

@RegisterBlock(name = "heat_exchanger_terminal", tileEntityClass = HeatExchangerTerminalTile.class)
public class HeatExchangerTerminalBlock extends HeatExchangerBaseBlock {
    
    @RegisterBlock.Instance
    public static HeatExchangerTerminalBlock INSTANCE;
    
    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new HeatExchangerTerminalTile();
    }
    
    @Override
    public boolean usesFaceDirection() {
        return true;
    }
    
    @Override
    public boolean usesAssemblyState() {
        return true;
    }
}
