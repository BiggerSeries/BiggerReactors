package net.roguelogix.biggerreactors.multiblocks.reactor.simulationold.modern;

import net.minecraft.nbt.CompoundTag;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulationold.IReactorFuelTank;

public class FuelTank implements IReactorFuelTank {
    
    private long capacity;
    
    private long fuel = 0;
    private long waste = 0;
    
    private double partialUsed = 0;
    
    void setCapacity(long capacity) {
        this.capacity = capacity;
    }
    
    double burn(double amount) {
        if (Double.isInfinite(amount) || Double.isNaN(amount)) {
            return 0;
        }
        double toProcess = partialUsed + amount;
        toProcess = Math.min(toProcess, (double) fuel);
        
        double burnedThisTick = toProcess - partialUsed;
        
        partialUsed = toProcess;
        if (toProcess >= 1) {
            long toBurn = (long) toProcess;
            fuel -= toBurn;
            waste += toBurn;
            partialUsed -= toBurn;
        }
        
        return burnedThisTick;
    }
    
    @Override
    public long capacity() {
        return capacity;
    }
    
    @Override
    public long totalStored() {
        return fuel + waste;
    }
    
    @Override
    public long fuel() {
        return fuel;
    }
    
    @Override
    public long waste() {
        return waste;
    }
    
    @Override
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
    
    @Override
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
    
    @Override
    public long extractFuel(long amount, boolean simulated) {
        amount = Math.min(fuel, amount);
        if (!simulated) {
            fuel -= amount;
        }
        return amount;
    }
    
    @Override
    public long extractWaste(long amount, boolean simulated) {
        amount = Math.min(waste, amount);
        if (!simulated) {
            waste -= amount;
        }
        return amount;
    }
    
    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putLong("capacity", capacity);
        nbt.putLong("fuel", fuel);
        nbt.putLong("waste", waste);
        nbt.putDouble("partialUsed", partialUsed);
        return nbt;
    }
    
    @Override
    public void deserializeNBT(CompoundTag nbt) {
        capacity = nbt.getLong("capacity");
        fuel = nbt.getLong("fuel");
        waste = nbt.getLong("waste");
        partialUsed = nbt.getDouble("partialUsed");
    }
}
