package net.roguelogix.biggerreactors.multiblocks.reactor2.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.roguelogix.biggerreactors.multiblocks.reactor2.tiles.ReactorBaseTile;
import net.roguelogix.biggerreactors.multiblocks.reactor2.tiles.ReactorControlRodTile;
import net.roguelogix.biggerreactors.multiblocks.reactor2.tiles.ReactorFuelRodTile;
import net.roguelogix.phosphophyllite.modular.block.PhosphophylliteBlock;
import net.roguelogix.phosphophyllite.multiblock2.IAssemblyStateBlock;
import net.roguelogix.phosphophyllite.multiblock2.rectangular.IAxisPositionBlock;
import net.roguelogix.phosphophyllite.multiblock2.rectangular.IRectangularMultiblockBlock;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;
import net.roguelogix.phosphophyllite.util.NonnullDefault;

@NonnullDefault
public abstract class ReactorBaseBlock extends PhosphophylliteBlock implements IRectangularMultiblockBlock {
    
    public ReactorBaseBlock(Properties properties) {
        super(properties);
    }
    
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ReactorBaseTile.BASE_SUPPLIER.create(pos, state);
    }
    
    private static abstract class Casing extends ReactorBaseBlock implements IAssemblyStateBlock, IAxisPositionBlock {
        public Casing(Properties properties) {
            super(properties);
        }
    }
    
    @RegisterBlock(name = "reactor2_casing", tileEntityClass = ReactorBaseTile.class)
    public static final ReactorBaseBlock CASING = new Casing(Properties.of(Material.METAL)) {
        @Override
        public boolean isGoodForInterior() {
            return false;
        }
        
        @Override
        public boolean isGoodForExterior() {
            return true;
        }
        
        @Override
        public boolean isGoodForFrame() {
            return true;
        }
    };
    
    @RegisterBlock(name = "reactor2_fuel_rod", tileEntityClass = ReactorFuelRodTile.class)
    public static final ReactorBaseBlock FUEL_ROD = new ReactorBaseBlock(Properties.of(Material.GLASS).noOcclusion()) {
        @Override
        public boolean isGoodForInterior() {
            return true;
        }
        
        @Override
        public boolean isGoodForExterior() {
            return false;
        }
        
        @Override
        public boolean isGoodForFrame() {
            return false;
        }
    
        @Override
        public boolean propagatesSkylightDown(BlockState p_49928_, BlockGetter p_49929_, BlockPos p_49930_) {
            return true;
        }
    
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return ReactorFuelRodTile.FUEL_ROD_SUPPLIER.create(pos, state);
        }
    };
    
    @RegisterBlock(name = "reactor2_control_rod", tileEntityClass = ReactorControlRodTile.class)
    public static final ReactorBaseBlock CONTROL_ROD = new ReactorBaseBlock(Properties.of(Material.METAL)) {
        @Override
        public boolean isGoodForInterior() {
            return false;
        }
        
        @Override
        public boolean isGoodForExterior() {
            return true;
        }
        
        @Override
        public boolean isGoodForFrame() {
            return false;
        }
    
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return ReactorControlRodTile.CONTROL_ROD_SUPPLIER.create(pos, state);
        }
    };
}
