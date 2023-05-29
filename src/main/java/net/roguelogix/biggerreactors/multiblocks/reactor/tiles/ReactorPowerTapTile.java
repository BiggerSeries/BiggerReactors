package net.roguelogix.biggerreactors.multiblocks.reactor.tiles;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.roguelogix.phosphophyllite.energy.EnergyStorageWrapper;
import net.roguelogix.phosphophyllite.energy.IPhosphophylliteEnergyStorage;
import net.roguelogix.phosphophyllite.multiblock2.common.IEventMultiblock;
import net.roguelogix.phosphophyllite.multiblock2.validated.IValidatedMultiblock;
import net.roguelogix.phosphophyllite.registry.RegisterTile;
import net.roguelogix.phosphophyllite.util.BlockStates;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static net.roguelogix.biggerreactors.multiblocks.reactor.blocks.ReactorPowerTap.ConnectionState.*;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ReactorPowerTapTile extends ReactorBaseTile implements IPhosphophylliteEnergyStorage, IEventMultiblock.AssemblyStateTransition {
    
    @RegisterTile("reactor_power_tap")
    public static final BlockEntityType.BlockEntitySupplier<ReactorPowerTapTile> SUPPLIER = new RegisterTile.Producer<>(ReactorPowerTapTile::new);
    
    public ReactorPowerTapTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
    
    @Override
    public <T> LazyOptional<T> capability(Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityEnergy.ENERGY) {
            return LazyOptional.of(() -> this).cast();
        }
        return super.capability(cap, side);
    }
    
    private boolean connected = false;
    Direction powerOutputDirection = null;
    
    private static final EnergyStorage ENERGY_ZERO = new EnergyStorage(0);
    
    private void setConnected(boolean newState) {
        if (newState != connected) {
            connected = newState;
            assert level != null;
            level.setBlock(worldPosition, getBlockState().setValue(CONNECTION_STATE_ENUM_PROPERTY, connected ? CONNECTED : DISCONNECTED), 3);
        }
    }
    
    LazyOptional<?> outputOptional = LazyOptional.empty();
    IPhosphophylliteEnergyStorage output;
    
    public long distributePower(long toDistribute, boolean simulate) {
        if (outputOptional.isPresent()) {
            return Math.max(0, output.insertEnergy(toDistribute, simulate));
        }
        return 0;
    }
    
    @Override
    public long insertEnergy(long maxInsert, boolean simulate) {
        return 0;
    }
    
    @Override
    public long extractEnergy(long maxExtract, boolean simulate) {
        if (maxExtract <= 0 || nullableController() == null || controller().assemblyState() != IValidatedMultiblock.AssemblyState.ASSEMBLED) {
            return 0;
        }
        var reactorSim = controller().simulation();
        if (reactorSim == null) {
            return 0;
        }
        var battery = reactorSim.battery();
        if (battery == null) {
            return 0;
        }
        long toExtract = battery.stored();
        toExtract = Math.min(maxExtract, toExtract);
        if (!simulate) {
            toExtract = battery.extract(toExtract);
        }
        return toExtract;
    }
    
    @Override
    public long energyStored() {
        if (nullableController() != null) {
            var reactorSim = controller().simulation();
            if (reactorSim == null) {
                return 0;
            }
            var battery = reactorSim.battery();
            if (battery == null) {
                return 0;
            }
            return Math.max(0, battery.stored());
        }
        return 0;
    }
    
    @Override
    public long maxEnergyStored() {
        if (nullableController() != null) {
            var reactorSim = controller().simulation();
            if (reactorSim == null) {
                return 0;
            }
            var battery = reactorSim.battery();
            if (battery == null) {
                return 0;
            }
            return Math.max(0, battery.capacity());
        }
        return 0;
    }
    
    @Override
    public boolean canInsert() {
        return false;
    }
    
    @Override
    public boolean canExtract() {
        return true;
    }
    
    @SuppressWarnings("DuplicatedCode")
    public void neighborChanged() {
        outputOptional = LazyOptional.empty();
        output = null;
        if (powerOutputDirection == null) {
            setConnected(false);
            return;
        }
        assert level != null;
        BlockEntity te = level.getBlockEntity(worldPosition.relative(powerOutputDirection));
        if (te == null) {
            setConnected(false);
            return;
        }
        LazyOptional<IEnergyStorage> energyOptional = te.getCapability(CapabilityEnergy.ENERGY, powerOutputDirection.getOpposite());
        setConnected(energyOptional.isPresent());
        if (connected) {
            outputOptional = energyOptional;
            output = EnergyStorageWrapper.wrap(energyOptional.orElse(ENERGY_ZERO));
        }
    }
    
    @Override
    public void onAssemblyStateTransition(IValidatedMultiblock.AssemblyState oldState, IValidatedMultiblock.AssemblyState newState) {
        if (newState == IValidatedMultiblock.AssemblyState.ASSEMBLED) {
            powerOutputDirection = getBlockState().getValue(BlockStates.FACING);
        } else {
            powerOutputDirection = null;
        }
        neighborChanged();
    }
}
