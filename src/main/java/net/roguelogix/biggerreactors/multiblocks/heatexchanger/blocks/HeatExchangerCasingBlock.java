package net.roguelogix.biggerreactors.multiblocks.heatexchanger.blocks;


import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles.HeatExchangerCasingTile;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nullable;

@RegisterBlock(name = "heat_exchanger_casing", tileEntityClass = HeatExchangerCasingTile.class)
public class HeatExchangerCasingBlock extends HeatExchangerBaseBlock{
    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new HeatExchangerCasingTile();
    }
    
    @Override
    public boolean usesAxisPositions() {
        return true;
    }
}
