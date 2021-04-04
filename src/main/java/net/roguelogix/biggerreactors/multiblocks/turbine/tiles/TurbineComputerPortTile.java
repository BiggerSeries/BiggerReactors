package net.roguelogix.biggerreactors.multiblocks.turbine.tiles;

import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.util.LazyOptional;
import net.roguelogix.biggerreactors.multiblocks.turbine.deps.TurbinePeripheral;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;
import net.roguelogix.phosphophyllite.registry.TileSupplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@RegisterTileEntity(name = "turbine_computer_port")
public class TurbineComputerPortTile extends TurbineBaseTile {
    
    @RegisterTileEntity.Type
    public static TileEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final TileSupplier SUPPLIER = TurbineComputerPortTile::new;
    
    public TurbineComputerPortTile() {
        super(TYPE);
    }
    
    @CapabilityInject(IPeripheral.class)
    public static Capability<IPeripheral> CAPABILITY_PERIPHERAL = null;
    
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, final @Nullable Direction side) {
        if (cap == CAPABILITY_PERIPHERAL) {
            return TurbinePeripheral.create(() -> controller).cast();
        }
        return LazyOptional.empty();
    }
    
}
