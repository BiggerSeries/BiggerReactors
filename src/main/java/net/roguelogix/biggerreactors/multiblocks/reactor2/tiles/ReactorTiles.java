package net.roguelogix.biggerreactors.multiblocks.reactor2.tiles;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.roguelogix.phosphophyllite.registry.RegisterTile;

public class ReactorTiles {
    @RegisterTile("reactor2_basic_tile")
    public static final BlockEntityType.BlockEntitySupplier<ReactorTile> BASE_SUPPLIER = new RegisterTile.Producer<>(ReactorTile::new);
    
    @RegisterTile("reactor2_terminal")
    public static final BlockEntityType.BlockEntitySupplier<ReactorTerminalTile> TERMINAL_SUPPLIER = new RegisterTile.Producer<>(ReactorTerminalTile::new);
    
    @RegisterTile("reactor2_fuel_rod")
    public static final BlockEntityType.BlockEntitySupplier<ReactorFuelRodTile> FUEL_ROD_SUPPLIER = new RegisterTile.Producer<>(ReactorFuelRodTile::new);
    
    @RegisterTile("reactor2_control_rod")
    public static final BlockEntityType.BlockEntitySupplier<ReactorControlRodTile> CONTROL_ROD_SUPPLIER = new RegisterTile.Producer<>(ReactorControlRodTile::new);
}
