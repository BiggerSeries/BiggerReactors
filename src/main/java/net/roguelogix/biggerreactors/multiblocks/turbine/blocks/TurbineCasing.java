package net.roguelogix.biggerreactors.multiblocks.turbine.blocks;

import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;
import net.roguelogix.biggerreactors.multiblocks.turbine.tiles.TurbineCasingTile;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nullable;

@RegisterBlock(name = "turbine_casing", tileEntityClass = TurbineCasingTile.class)
public class TurbineCasing extends TurbineBaseBlock {
    
    @RegisterBlock.Instance
    public static TurbineCasing INSTANCE;
    
    public TurbineCasing() {
        super();
    }
    
    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new TurbineCasingTile();
    }
    
    @Override
    public boolean usesAxisPositions() {
        return true;
    }
    
    @Override
    public boolean isGoodForFrame() {
        return true;
    }
}
