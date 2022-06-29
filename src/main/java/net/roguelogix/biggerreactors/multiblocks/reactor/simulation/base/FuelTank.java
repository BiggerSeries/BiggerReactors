package net.roguelogix.biggerreactors.multiblocks.reactor.simulation.base;

import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.IReactorSimulation;
import net.roguelogix.phosphophyllite.serialization.IPhosphophylliteSerializable;
import net.roguelogix.phosphophyllite.serialization.PhosphophylliteCompound;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FuelTank implements IReactorSimulation.IFuelTank, IPhosphophylliteSerializable {
    
    private final long capacity;
    
    private long fuel = 0;
    private long waste = 0;
    
    private double partialUsed = 0;
    
    private double burnedLastTick = 0;
    
    public FuelTank(long capacity) {
        this.capacity = capacity;
    }
    
    public void burn(double amount) {
        if (Double.isInfinite(amount) || Double.isNaN(amount) || amount == 0) {
            burnedLastTick = 0;
            return;
        }
        
        double toProcess = partialUsed + amount;
        toProcess = Math.min(toProcess, (double) fuel);
        
        burnedLastTick = toProcess - partialUsed;
        
        partialUsed = toProcess;
        if (toProcess >= 1) {
            long toBurn = (long) toProcess;
            fuel -= toBurn;
            waste += toBurn;
            partialUsed -= toBurn;
        }
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
    public double burnedLastTick() {
        return burnedLastTick;
    }
    
    @Nullable
    @Override
    public PhosphophylliteCompound save() {
        PhosphophylliteCompound compound = new PhosphophylliteCompound();
        compound.put("fuel", fuel);
        compound.put("waste", waste);
        compound.put("partialUsed", partialUsed);
        return compound;
    }
    
    @Override
    public void load(@Nonnull PhosphophylliteCompound compound) {
        fuel = compound.getLong("fuel");
        waste = compound.getLong("waste");
        partialUsed = compound.getDouble("partialUsed");
    }
}