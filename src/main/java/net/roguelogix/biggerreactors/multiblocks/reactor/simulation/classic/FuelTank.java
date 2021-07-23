package net.roguelogix.biggerreactors.multiblocks.reactor.simulation.classic;

import net.minecraft.nbt.CompoundTag;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.IReactorFuelTank;

import javax.annotation.Nonnull;

public class FuelTank implements IReactorFuelTank {
    private long capacity;
    
    private long fuel = 0;
    private long waste = 0;
    
    private double partialUsed = 0;
    
    void burn(double amount) {
        if (Double.isInfinite(amount) || Double.isNaN(amount)) {
            return;
        }
        
        partialUsed += amount;
        
        if (partialUsed < 1f) {
            return;
        }
        
        long toBurn = Math.min(fuel, (long) partialUsed);
        partialUsed -= toBurn;
        
        if (toBurn <= 0) {
            return;
        }
        
        fuel -= toBurn;
        waste += toBurn;
    }
    
    void setCapacity(long capacity) {
        this.capacity = capacity;
    }
    
    public long capacity() {
        return capacity;
    }
    
    public long insertFuel(long amount, boolean simulated) {
        if (totalStored() >= capacity) {
            // if we are overfilled, then we need to *not* insert more
            return 0;
        }
        
        amount = Math.min(amount, capacity - totalStored());
        
        if (!simulated) {
            fuel += amount;
        }
        
        return amount;
    }
    
    public long insertWaste(long amount, boolean simulated) {
        if (totalStored() >= capacity) {
            // if we are overfilled, then we need to *not* insert more
            return 0;
        }
        
        amount = Math.min(amount, capacity - totalStored());
        
        if (!simulated) {
            waste += amount;
        }
        
        return amount;
    }
    
    public long spaceAvailable() {
        return capacity() - totalStored();
    }
    
    public long extractFuel(long toExtract, boolean simulated) {
        toExtract = Math.min(fuel, toExtract);
        if (!simulated) {
            fuel -= toExtract;
        }
        return toExtract;
    }
    
    public long extractWaste(long toExtract, boolean simulated) {
        toExtract = Math.min(waste, toExtract);
        if (!simulated) {
            waste -= toExtract;
        }
        return toExtract;
    }
    
    public long totalStored() {
        return fuel + waste;
    }
    
    public long fuel() {
        return fuel;
    }
    
    public long waste() {
        return waste;
    }
    
    @Override
    @Nonnull
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putLong("capacity", capacity);
        nbt.putLong("fuel", fuel);
        nbt.putLong("waste", waste);
        nbt.putDouble("partialUsed", partialUsed);
        return nbt;
    }
    
    @Override
    public void deserializeNBT(@Nonnull CompoundTag nbt) {
        if (nbt.contains("capacity")) {
            capacity = nbt.getLong("capacity");
        }
        if (nbt.contains("fuel")) {
            fuel = nbt.getLong("fuel");
        }
        if (nbt.contains("waste")) {
            waste = nbt.getLong("waste");
        }
        if (nbt.contains("partialUsed")) {
            partialUsed = nbt.getDouble("partialUsed");
        }
    }
}
