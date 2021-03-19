package net.roguelogix.biggerreactors.classic.turbine.tiles;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.roguelogix.phosphophyllite.energy.EnergyStorageWrapper;
import net.roguelogix.phosphophyllite.energy.IPhosphophylliteEnergyStorage;
import net.roguelogix.phosphophyllite.multiblock.generic.MultiblockController;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;
import net.roguelogix.phosphophyllite.registry.TileSupplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static net.roguelogix.biggerreactors.classic.turbine.blocks.TurbinePowerTap.ConnectionState.*;


@RegisterTileEntity(name = "turbine_power_tap")
public class TurbinePowerTapTile extends TurbineBaseTile implements IEnergyStorage {
    @RegisterTileEntity.Type
    public static TileEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final TileSupplier SUPPLIER = TurbinePowerTapTile::new;
    
    public TurbinePowerTapTile() {
        super(TYPE);
    }
    
    
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityEnergy.ENERGY) {
            return LazyOptional.of(() -> this).cast();
        }
        return super.getCapability(cap, side);
    }
    
    private boolean connected = false;
    Direction powerOutputDirection = null;
    
    private static final EnergyStorage ENERGY_ZERO = new EnergyStorage(0);
    
    private void setConnected(boolean newState) {
        if (newState != connected) {
            connected = newState;
            assert world != null;
            world.setBlockState(pos, getBlockState().with(CONNECTION_STATE_ENUM_PROPERTY, connected ? CONNECTED : DISCONNECTED));
        }
    }
    
    @Nonnull
    LazyOptional<?> outputOptional = LazyOptional.empty();
    IPhosphophylliteEnergyStorage output;
    
    public long distributePower(long toDistribute, boolean simulate) {
        if (outputOptional.isPresent()) {
            return output.insertEnergy(toDistribute, simulate);
        }
        return 0;
    }
    
    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return 0;
    }
    
    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }
    
    @Override
    public int getEnergyStored() {
        if (controller != null) {
            return (int) controller.simulation().battery().stored();
        }
        return 0;
    }
    
    @Override
    public int getMaxEnergyStored() {
        if (controller != null) {
            return (int) controller.simulation().battery().capacity();
        }
        return 0;
    }
    
    @Override
    public boolean canExtract() {
        return false;
    }
    
    @Override
    public boolean canReceive() {
        return false;
    }
    
    @SuppressWarnings("DuplicatedCode")
    public void updateOutputDirection() {
        if (controller.assemblyState() == MultiblockController.AssemblyState.DISASSEMBLED) {
            powerOutputDirection = null;
        } else if (pos.getX() == controller.minCoord().x()) {
            powerOutputDirection = Direction.WEST;
        } else if (pos.getX() == controller.maxCoord().x()) {
            powerOutputDirection = Direction.EAST;
        } else if (pos.getY() == controller.minCoord().y()) {
            powerOutputDirection = Direction.DOWN;
        } else if (pos.getY() == controller.maxCoord().y()) {
            powerOutputDirection = Direction.UP;
        } else if (pos.getZ() == controller.minCoord().z()) {
            powerOutputDirection = Direction.NORTH;
        } else if (pos.getZ() == controller.maxCoord().z()) {
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
        assert world != null;
        TileEntity te = world.getTileEntity(pos.offset(powerOutputDirection));
        if (te == null) {
            setConnected(false);
            return;
        }
        LazyOptional<IEnergyStorage> energyOptional = te.getCapability(CapabilityEnergy.ENERGY, powerOutputDirection.getOpposite());
        setConnected(energyOptional.isPresent());
        if(connected){
            outputOptional = energyOptional;
            output = EnergyStorageWrapper.wrap(energyOptional.orElse(ENERGY_ZERO));
        }
    }
    
}
