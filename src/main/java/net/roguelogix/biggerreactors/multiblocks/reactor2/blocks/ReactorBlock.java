package net.roguelogix.biggerreactors.multiblocks.reactor2.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.roguelogix.biggerreactors.multiblocks.reactor2.tiles.ReactorTile;
import net.roguelogix.biggerreactors.multiblocks.reactor2.tiles.ReactorControlRodTile;
import net.roguelogix.biggerreactors.multiblocks.reactor2.tiles.ReactorFuelRodTile;
import net.roguelogix.biggerreactors.multiblocks.reactor2.tiles.ReactorTiles;
import net.roguelogix.phosphophyllite.modular.block.IConnectedTexture;
import net.roguelogix.phosphophyllite.modular.block.PhosphophylliteBlock;
import net.roguelogix.phosphophyllite.multiblock2.IAssemblyStateBlock;
import net.roguelogix.phosphophyllite.multiblock2.rectangular.IAxisPositionBlock;
import net.roguelogix.phosphophyllite.multiblock2.rectangular.IFaceDirectionBlock;
import net.roguelogix.phosphophyllite.multiblock2.rectangular.IRectangularMultiblockBlock;
import net.roguelogix.phosphophyllite.util.NonnullDefault;

import static net.roguelogix.phosphophyllite.util.BlockStates.PORT_DIRECTION;

@NonnullDefault
public abstract class ReactorBlock extends PhosphophylliteBlock implements IRectangularMultiblockBlock {
    
    private final BlockEntityType.BlockEntitySupplier<? extends ReactorTile> tileSupplier;
    
    public ReactorBlock(Properties properties) {
        super(properties.isValidSpawn((a, b, c, d) -> false));
        tileSupplier = ReactorTiles.BASE_SUPPLIER;
    }
    
    public ReactorBlock(Properties properties, BlockEntityType.BlockEntitySupplier<? extends ReactorTile> tileSupplier) {
        super(properties.isValidSpawn((a, b, c, d) -> false));
        this.tileSupplier = tileSupplier;
    }
    
    @Override
    public final BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return tileSupplier.create(pos, state);
    }
    
    public static abstract class Casing extends ReactorBlock implements IAssemblyStateBlock, IAxisPositionBlock {
        public Casing(Properties properties) {
            super(properties);
        }
    }
    
    
    public static abstract class Glass extends ReactorBlock implements IAssemblyStateBlock, IConnectedTexture {
        public Glass(Properties properties) {
            super(properties);
        }
        
        @Override
        public boolean propagatesSkylightDown(BlockState p_49928_, BlockGetter p_49929_, BlockPos p_49930_) {
            return true;
        }
        
        @SuppressWarnings("deprecation")
        @Override
        public float getShadeBrightness(BlockState p_60472_, BlockGetter p_60473_, BlockPos p_60474_) {
            return 1.0f;
        }
    }
    
    public static class FuelRod extends ReactorBlock {
        public FuelRod() {
            this(ReactorTiles.FUEL_ROD_SUPPLIER);
        }
        
        public FuelRod(BlockEntityType.BlockEntitySupplier<? extends ReactorFuelRodTile> tileSupplier) {
            super(Properties.of().noOcclusion(), tileSupplier);
        }
        
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
        
        @SuppressWarnings("deprecation")
        @Override
        public float getShadeBrightness(BlockState p_60472_, BlockGetter p_60473_, BlockPos p_60474_) {
            return 1.0f;
        }
    }
    
    public static abstract class ControlRod extends ReactorBlock implements IAssemblyStateBlock {
        public ControlRod() {
            this(ReactorTiles.CONTROL_ROD_SUPPLIER);
        }
        
        public ControlRod(BlockEntityType.BlockEntitySupplier<? extends ReactorControlRodTile> tileSupplier) {
            super(BlockBehaviour.Properties.of(), tileSupplier);
        }
    }
    
    public static class Port extends ReactorBlock implements IAssemblyStateBlock, IFaceDirectionBlock {
        public Port(Properties properties) {
            super(properties);
        }
        
        public Port(Properties properties, BlockEntityType.BlockEntitySupplier<? extends ReactorTile> tileSupplier) {
            super(properties, tileSupplier);
        }
        
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
    }
    
    
    static class DirectionalPort extends Port {
        public DirectionalPort(Properties properties) {
            super(properties);
        }
        
        public DirectionalPort(Properties properties, BlockEntityType.BlockEntitySupplier<? extends ReactorTile> tileSupplier) {
            super(properties, tileSupplier);
        }
        
        @Override
        protected BlockState buildDefaultState(BlockState state) {
            return state.setValue(PORT_DIRECTION, false);
        }
        
        @Override
        protected void buildStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(PORT_DIRECTION);
        }
    }
}