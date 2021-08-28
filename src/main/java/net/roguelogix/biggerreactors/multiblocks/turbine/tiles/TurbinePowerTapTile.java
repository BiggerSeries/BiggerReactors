package net.roguelogix.biggerreactors.multiblocks.turbine.tiles;

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
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static net.roguelogix.biggerreactors.multiblocks.turbine.blocks.TurbinePowerTap.ConnectionState.*;
import static net.roguelogix.phosphophyllite.multiblock.MultiblockController.AssemblyState.ASSEMBLED;
import static net.roguelogix.phosphophyllite.multiblock.MultiblockController.AssemblyState.DISASSEMBLED;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@RegisterTileEntity(name = "turbine_power_tap")
public class TurbinePowerTapTile extends TurbineBaseTile implements IPhosphophylliteEnergyStorage {
    @RegisterTileEntity.Type
    public static BlockEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final BlockEntityType.BlockEntitySupplier<TurbinePowerTapTile> SUPPLIER = TurbinePowerTapTile::new;
    
    public TurbinePowerTapTile(BlockPos pos, BlockState state) {
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
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(CONNECTION_STATE_ENUM_PROPERTY, connected ? CONNECTED : DISCONNECTED));
        }
    }
    
    LazyOptional<?> outputOptional = LazyOptional.empty();
    IPhosphophylliteEnergyStorage output;
    
    public long distributePower(long toDistribute, boolean simulate) {
        if (outputOptional.isPresent()) {
            return output.insertEnergy(toDistribute, simulate);
        }
        return 0;
    }
    
    @Override
    public long insertEnergy(long maxInsert, boolean simulate) {
        return 0;
    }
    
    @Override
    public long extractEnergy(long maxExtract, boolean simulate) {
        if (nullableController() == null || controller().assemblyState() != ASSEMBLED) {
            return 0;
        }
        long toExtract = controller().simulation().battery().stored();
        toExtract = Math.min(maxExtract, toExtract);
        if (!simulate) {
            toExtract = controller().simulation().battery().extract(toExtract);
        }
        return toExtract;
    }
    
    @Override
    public long energyStored() {
        if (nullableController() != null) {
            return controller().simulation().battery().stored();
        }
        return 0;
    }
    
    @Override
    public long maxEnergyStored() {
        if (nullableController() != null) {
            return controller().simulation().battery().capacity();
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
    public void updateOutputDirection() {
        if (controller().assemblyState() == DISASSEMBLED) {
            powerOutputDirection = null;
        } else if (worldPosition.getX() == controller().minCoord().x()) {
            powerOutputDirection = Direction.WEST;
        } else if (worldPosition.getX() == controller().maxCoord().x()) {
            powerOutputDirection = Direction.EAST;
        } else if (worldPosition.getY() == controller().minCoord().y()) {
            powerOutputDirection = Direction.DOWN;
        } else if (worldPosition.getY() == controller().maxCoord().y()) {
            powerOutputDirection = Direction.UP;
        } else if (worldPosition.getZ() == controller().minCoord().z()) {
            powerOutputDirection = Direction.NORTH;
        } else if (worldPosition.getZ() == controller().maxCoord().z()) {
            powerOutputDirection = Direction.SOUTH;
        }
        neighborChanged();
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
    
}
