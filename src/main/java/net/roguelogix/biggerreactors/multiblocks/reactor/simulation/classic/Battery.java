package net.roguelogix.biggerreactors.multiblocks.reactor.simulation.classic;

import net.minecraft.nbt.CompoundNBT;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.IReactorBattery;

public class Battery implements IReactorBattery {
    private double partialStored = 0;
    private long storedPower = 0;
    private long maxStoredPower = 0;
    
    void setMaxStoredPower(long maxStoredPower) {
        this.maxStoredPower = maxStoredPower;
    }
    
    long addPower(double powerProduced) {
        if (Double.isInfinite(powerProduced) || Double.isNaN(powerProduced)) {
            return 0;
        }
        
        partialStored += powerProduced;
        
        if (partialStored < 1f) {
            return 0;
        }
        
        long toAdd = (long) partialStored;
        partialStored -= toAdd;
        
        storedPower += toAdd;
        
        if (storedPower > maxStoredPower) {
            storedPower = maxStoredPower;
        }
        
        return toAdd;
    }
    
    public long extract(long toExtract) {
        storedPower -= toExtract;
        return toExtract;
    }
    
    public long stored() {
        return storedPower;
    }
    
    public long capacity() {
        return maxStoredPower;
    }
    
    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putLong("storedPower", storedPower);
        nbt.putLong("maxStoredPower", maxStoredPower);
        return nbt;
    }
    
    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        storedPower = nbt.getLong("storedPower");
        maxStoredPower = nbt.getLong("maxStoredPower");
    }
}
