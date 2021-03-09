package net.roguelogix.biggerreactors.classic.turbine.simulation.modern;


import net.minecraft.nbt.CompoundNBT;
import net.roguelogix.biggerreactors.classic.turbine.simulation.ITurbineBattery;

public class Battery implements ITurbineBattery {
    private long capacity;
    private long stored;
    private long generatedLastTick;
    
    @Override
    public long extract(long toExtract) {
        stored -= toExtract;
        return toExtract;
    }
    
    public long generate(long maxToGenerate){
        generatedLastTick = Math.min(maxToGenerate, capacity - stored);
        stored += generatedLastTick;
        return generatedLastTick;
    }
    
    public long generatedLastTick(){
        return generatedLastTick;
    }
    
    @Override
    public long stored() {
        return stored;
    }
    
    @Override
    public long capacity() {
        return capacity;
    }
    
    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }
    
    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putLong("storedPower", stored);
        return nbt;
    }
    
    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        stored = nbt.getLong("storedPower");
    }
}
