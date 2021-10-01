package net.roguelogix.biggerreactors.multiblocks.reactor.simulation;

import net.roguelogix.biggerreactors.BiggerReactors;
import net.roguelogix.phosphophyllite.serialization.IPhosphophylliteSerializable;
import net.roguelogix.phosphophyllite.serialization.PhosphophylliteCompound;
import net.roguelogix.phosphophyllite.util.HeatBody;
import net.roguelogix.phosphophyllite.util.MethodsReturnNonnullByDefault;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class Battery extends HeatBody implements IReactorSimulation.IBattery, IPhosphophylliteSerializable {
    private final long capacity;
    private long stored;
    private long generatedLastTick;
    
    {
        setInfinite(true);
    }
    
    Battery(long capacity) {
        this.capacity = capacity;
    }
    
    @Override
    public double transferWith(HeatBody other, double rfkt) {
        double newTemp = other.temperature() - temperature();
        newTemp *= Math.exp(-rfkt / other.rfPerKelvin());
        newTemp += temperature();
        
        double rfTransferred = (newTemp - other.temperature()) * other.rfPerKelvin();
        
        generatedLastTick = (long) (-rfTransferred * BiggerReactors.CONFIG.Reactor.OutputMultiplier * BiggerReactors.CONFIG.Reactor.PassiveOutputMultiplier);
        stored += generatedLastTick;
        if (stored > capacity) {
            stored = capacity;
        }
        
        other.setTemperature(newTemp);
        
        return rfTransferred;
    }
    
    @Override
    public long extract(long toExtract) {
        stored -= toExtract;
        return toExtract;
    }
    
    @Override
    public long stored() {
        return stored;
    }
    
    @Override
    public long capacity() {
        return capacity;
    }
    
    @Override
    public long generatedLastTick() {
        return generatedLastTick;
    }
    
    @Nullable
    @Override
    public PhosphophylliteCompound save() {
        var compound = new PhosphophylliteCompound();
        compound.put("storedPower", stored);
        return compound;
    }
    
    @Override
    public void load(@Nonnull PhosphophylliteCompound compound) {
        stored = compound.getLong("storedPower");
    }
}