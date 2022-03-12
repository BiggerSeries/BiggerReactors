package net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles;

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
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.deps.HeatExchangerPeripheral;
import net.roguelogix.phosphophyllite.registry.RegisterTile;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class HeatExchangerComputerPortTile extends HeatExchangerBaseTile {
    
    
    @RegisterTile("heat_exchanger_computer_port")
    public static final BlockEntityType.BlockEntitySupplier<HeatExchangerComputerPortTile> SUPPLIER = new RegisterTile.Producer<>(HeatExchangerComputerPortTile::new);
    
    public HeatExchangerComputerPortTile(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
    }
    
    private static final Capability<IPeripheral> CAPABILITY_PERIPHERAL = CapabilityManager.get(new CapabilityToken<>(){});
    
    @Override
    public <T> LazyOptional<T> capability(Capability<T> cap, final @Nullable Direction side) {
        if (cap == CAPABILITY_PERIPHERAL) {
            return HeatExchangerPeripheral.create(this::controller).cast();
        }
        return super.capability(cap, side);
    }
    
}
