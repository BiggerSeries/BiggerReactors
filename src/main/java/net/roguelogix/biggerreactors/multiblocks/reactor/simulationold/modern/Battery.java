package net.roguelogix.biggerreactors.multiblocks.reactor.simulationold.modern;

import net.minecraft.nbt.CompoundTag;
import net.roguelogix.biggerreactors.Config;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulationold.IReactorBattery;
import net.roguelogix.phosphophyllite.util.HeatBody;

public class Battery extends HeatBody implements IReactorBattery {
    private long capacity;
    private long stored;
    private long generatedLastTick;
    
    {
        setInfinite(true);
    }
    
    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }
    
    @Override
    public double transferWith(HeatBody other, double rfkt) {
        double newTemp = other.temperature() - temperature();
        newTemp *= Math.exp(-rfkt / other.rfPerKelvin());
        newTemp += temperature();
        
        double rfTransferred = (newTemp - other.temperature()) * other.rfPerKelvin();
    
        generatedLastTick = (long) (-rfTransferred * Config.CONFIG.Reactor.OutputMultiplier * Config.CONFIG.Reactor.PassiveOutputMultiplier);
        stored += generatedLastTick;
        if(stored > capacity){
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
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putLong("storedPower", stored);
        return nbt;
    }
    
    @Override
    public void deserializeNBT(CompoundTag nbt) {
        stored = nbt.getLong("storedPower");
    }
    
    public long generatedLastTick() {
        return generatedLastTick;
    }
}
