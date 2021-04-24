package net.roguelogix.biggerreactors.multiblocks.reactor.tiles;

import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.util.LazyOptional;
import net.roguelogix.biggerreactors.multiblocks.reactor.deps.ReactorPeripheral;
import net.roguelogix.phosphophyllite.multiblock.generic.IOnAssemblyTile;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;
import net.roguelogix.phosphophyllite.registry.TileSupplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@RegisterTileEntity(name = "reactor_computer_port")
public class ReactorComputerPortTile extends ReactorBaseTile implements IOnAssemblyTile {
    
    @RegisterTileEntity.Type
    public static TileEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final TileSupplier SUPPLIER = ReactorComputerPortTile::new;
    
    public ReactorComputerPortTile() {
        super(TYPE);
    }
    
    @CapabilityInject(IPeripheral.class)
    public static Capability<IPeripheral> CAPABILITY_PERIPHERAL = null;
    
    private LazyOptional<ReactorPeripheral> peripheralCapability;
    
    {
        // avoids classloading without CC existing
        if (CAPABILITY_PERIPHERAL != null) {
            peripheralCapability = ReactorPeripheral.create(() -> controller);
        }
    }
    
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, final @Nullable Direction side) {
        if (cap == CAPABILITY_PERIPHERAL) {
            return peripheralCapability.cast();
        }
        return LazyOptional.empty();
    }
    
    @Override
    public void onAssembly() {
        // class loading BS, dont remove this if
        if (CAPABILITY_PERIPHERAL != null) {
            peripheralCapability.ifPresent(ReactorPeripheral::rebuildControlRodList);
        }
    }
}
