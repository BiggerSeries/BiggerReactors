package net.roguelogix.biggerreactors.multiblocks.reactor.tiles;

import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.LazyOptional;
import net.roguelogix.biggerreactors.multiblocks.reactor.deps.ReactorPeripheral;
import net.roguelogix.phosphophyllite.multiblock.IOnAssemblyTile;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@RegisterTileEntity(name = "reactor_computer_port")
public class ReactorComputerPortTile extends ReactorBaseTile implements IOnAssemblyTile {
    
    @RegisterTileEntity.Type
    public static BlockEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final BlockEntityType.BlockEntitySupplier<ReactorComputerPortTile> SUPPLIER = ReactorComputerPortTile::new;
    
    public ReactorComputerPortTile(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
    
    private static final Capability<IPeripheral> CAPABILITY_PERIPHERAL = CapabilityManager.get(new CapabilityToken<>(){});

    private LazyOptional<ReactorPeripheral> peripheralCapability;

    {
        // avoids classloading without CC existing
        if (CAPABILITY_PERIPHERAL.isRegistered()) {
            peripheralCapability = ReactorPeripheral.create(this::controller);
        }
    }
    
    @Override
    public <T> LazyOptional<T> capability(Capability<T> cap, final @Nullable Direction side) {
        if (cap == CAPABILITY_PERIPHERAL) {
            return peripheralCapability.cast();
        }
        return super.capability(cap, side);
    }
    
    @Override
    public void onAssembly() {
        // class loading BS, dont remove this if
        if (CAPABILITY_PERIPHERAL.isRegistered()) {
            peripheralCapability.ifPresent(ReactorPeripheral::rebuildControlRodList);
        }
    }
}
