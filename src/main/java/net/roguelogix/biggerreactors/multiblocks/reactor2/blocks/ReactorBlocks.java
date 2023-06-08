package net.roguelogix.biggerreactors.multiblocks.reactor2.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.roguelogix.biggerreactors.multiblocks.reactor2.tiles.*;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

public class ReactorBlocks {
    @RegisterBlock(name = "reactor2_casing", tileEntityClass = ReactorTile.class)
    public static final ReactorBlock CASING = new ReactorBlock.Casing(BlockBehaviour.Properties.of()) {
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
    
    @RegisterBlock(name = "reactor2_glass", tileEntityClass = ReactorTile.class)
    public static final ReactorBlock GLASS = new ReactorBlock.Glass(BlockBehaviour.Properties.of().noOcclusion()) {
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
    };
    
    @RegisterBlock(name = "reactor2_manifold", tileEntityClass = ReactorTile.class)
    public static final ReactorBlock MANIFOLD = new ReactorBlock.Glass(BlockBehaviour.Properties.of().noOcclusion()) {
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
        public boolean connectToBlock(Block block) {
            if (block instanceof ReactorBlock reactorBlock) {
                return (reactorBlock != GLASS && (reactorBlock).isGoodForExterior()) || reactorBlock == this;
            }
            return false;
        }
    };
    
    @RegisterBlock(name = "reactor2_fuel_rod_copper", tileEntityClass = ReactorFuelRodTile.class)
    public static final ReactorBlock COPPER_FUEL_ROD = new ReactorBlock.FuelRod();
    @RegisterBlock(name = "reactor2_fuel_rod_iron", tileEntityClass = ReactorFuelRodTile.class)
    public static final ReactorBlock IRON_FUEL_ROD = new ReactorBlock.FuelRod();
    @RegisterBlock(name = "reactor2_fuel_rod_gold", tileEntityClass = ReactorFuelRodTile.class)
    public static final ReactorBlock GOLD_FUEL_ROD = new ReactorBlock.FuelRod();
    
    @RegisterBlock(name = "reactor2_control_rod", tileEntityClass = ReactorControlRodTile.class)
    public static final ReactorBlock CONTROL_ROD = new ReactorBlock.ControlRod() {
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
    };
    
    @RegisterBlock(name = "reactor2_terminal", tileEntityClass = ReactorTerminalTile.class)
    public static final ReactorBlock TERMINAL = new ReactorBlock.Port(BlockBehaviour.Properties.of(), ReactorTiles.TERMINAL_SUPPLIER);
    
    // TODO: this needs its own tile type
    @RegisterBlock(name = "reactor2_coolant_port", tileEntityClass = ReactorTile.class)
    public static final ReactorBlock COOLANT_PORT = new ReactorBlock.DirectionalPort(BlockBehaviour.Properties.of());
}
