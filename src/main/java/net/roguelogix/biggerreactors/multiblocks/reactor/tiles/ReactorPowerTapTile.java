package net.roguelogix.biggerreactors.multiblocks.reactor.tiles;

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
import net.roguelogix.phosphophyllite.multiblock.generic.IOnAssemblyTile;
import net.roguelogix.phosphophyllite.multiblock.generic.IOnDisassemblyTile;
import net.roguelogix.phosphophyllite.multiblock.generic.MultiblockController;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;
import net.roguelogix.phosphophyllite.registry.TileSupplier;
import net.roguelogix.phosphophyllite.util.BlockStates;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static net.roguelogix.biggerreactors.multiblocks.reactor.blocks.ReactorPowerTap.ConnectionState.*;


@RegisterTileEntity(name = "reactor_power_tap")
public class ReactorPowerTapTile extends ReactorBaseTile implements IPhosphophylliteEnergyStorage, IOnAssemblyTile, IOnDisassemblyTile {
    @RegisterTileEntity.Type
    public static TileEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final TileSupplier SUPPLIER = ReactorPowerTapTile::new;
    
    public ReactorPowerTapTile() {
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
    public long insertEnergy(long maxInsert, boolean simulate) {
        return 0;
    }
    
    @Override
    public long extractEnergy(long maxExtract, boolean simulate) {
        if(controller == null || controller.assemblyState() != MultiblockController.AssemblyState.ASSEMBLED){
            return 0;
        }
        long toExtract = controller.simulation().battery().stored();
        toExtract = Math.min(maxExtract, toExtract);
        if (!simulate) {
            toExtract = controller.simulation().battery().extract(toExtract);
        }
        return toExtract;
    }
    
    @Override
    public long energyStored() {
        if (controller != null) {
            return controller.simulation().battery().stored();
        }
        return 0;
    }
    
    @Override
    public long maxEnergyStored() {
        if (controller != null) {
            return controller.simulation().battery().capacity();
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
        assert world != null;
        TileEntity te = world.getTileEntity(pos.offset(powerOutputDirection));
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
    public void onAssembly() {
        powerOutputDirection = getBlockState().get(BlockStates.FACING);
        neighborChanged();
    }
    
    @Override
    public void onDisassembly() {
        powerOutputDirection = null;
        neighborChanged();
    }
}
