package net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles;

import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.util.LazyOptional;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.deps.HeatExchangerPeripheral;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@RegisterTileEntity(name = "heat_exchanger_computer_port")
public class HeatExchangerComputerPortTile extends HeatExchangerBaseTile {
    
    @RegisterTileEntity.Type
    public static BlockEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final BlockEntityType.BlockEntitySupplier<HeatExchangerComputerPortTile> SUPPLIER = HeatExchangerComputerPortTile::new;
    
    public HeatExchangerComputerPortTile(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    @CapabilityInject(IPeripheral.class)
    public static Capability<IPeripheral> CAPABILITY_PERIPHERAL = null;
    
    @Override
    public <T> LazyOptional<T> capability(Capability<T> cap, final @Nullable Direction side) {
        if (cap == CAPABILITY_PERIPHERAL) {
            return HeatExchangerPeripheral.create(this::controller).cast();
        }
        return LazyOptional.empty();
    }
    
}
