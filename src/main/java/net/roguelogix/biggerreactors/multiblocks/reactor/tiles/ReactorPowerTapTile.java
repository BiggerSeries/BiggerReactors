package net.roguelogix.biggerreactors.multiblocks.reactor.tiles;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.phosphophyllite.capability.CachedWrappedBlockCapability;
import net.roguelogix.phosphophyllite.energy.IEnergyTile;
import net.roguelogix.phosphophyllite.energy.IPhosphophylliteEnergyHandler;
import net.roguelogix.phosphophyllite.multiblock.common.IEventMultiblock;
import net.roguelogix.phosphophyllite.multiblock.validated.IValidatedMultiblock;
import net.roguelogix.phosphophyllite.registry.RegisterTile;
import net.roguelogix.phosphophyllite.util.BlockStates;
import net.roguelogix.phosphophyllite.util.NonnullDefault;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static net.roguelogix.biggerreactors.multiblocks.reactor.blocks.ReactorPowerTap.ConnectionState.*;

@NonnullDefault
public class ReactorPowerTapTile extends ReactorBaseTile implements IEnergyTile, IPhosphophylliteEnergyHandler, IEventMultiblock.AssemblyStateTransition {
    
    @RegisterTile("reactor_power_tap")
    public static final BlockEntityType.BlockEntitySupplier<ReactorPowerTapTile> SUPPLIER = new RegisterTile.Producer<>(ReactorPowerTapTile::new);
    
    public ReactorPowerTapTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
    
    private boolean connected = false;
    @Nullable
    Direction powerOutputDirection = null;
    
    
    @Override
    public IPhosphophylliteEnergyHandler energyHandler(Direction direction) {
        return this;
    }
    
    private void setConnected(boolean newState) {
        if (newState != connected) {
            connected = newState;
            assert level != null;
            level.setBlock(worldPosition, getBlockState().setValue(CONNECTION_STATE_ENUM_PROPERTY, connected ? CONNECTED : DISCONNECTED), 3);
        }
    }
    
    @Nullable
    CachedWrappedBlockCapability<IPhosphophylliteEnergyHandler, Direction> output = null;
    
    public long distributePower(long toDistribute, boolean simulate) {
        if(output == null) {
            return 0;
        }
        @Nullable
        final var outputCap = output.getCapability();
        if (outputCap == null) {
            return 0;
        }
        return Math.max(0, outputCap.insertEnergy(toDistribute, simulate));
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
        @Nullable var reactorSim = controller().simulation();
        if (reactorSim == null) {
            return 0;
        }
        @Nullable var battery = reactorSim.battery();
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
            @Nullable var reactorSim = controller().simulation();
            if (reactorSim == null) {
                return 0;
            }
            @Nullable var battery = reactorSim.battery();
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
            @Nullable var reactorSim = controller().simulation();
            if (reactorSim == null) {
                return 0;
            }
            @Nullable var battery = reactorSim.battery();
            if (battery == null) {
                return 0;
            }
            return Math.max(0, battery.capacity());
        }
        return 0;
    }
    
    @SuppressWarnings("DuplicatedCode")
    public void neighborChanged() {
        output = null;
        if (powerOutputDirection == null) {
            setConnected(false);
            return;
        }
        output = findEnergyCapability(powerOutputDirection);
        setConnected(output.getCapability() != null);
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
