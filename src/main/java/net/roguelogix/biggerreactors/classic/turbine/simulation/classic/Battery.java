package net.roguelogix.biggerreactors.classic.turbine.simulation.classic;

import net.minecraft.nbt.CompoundNBT;
import net.roguelogix.biggerreactors.classic.turbine.simulation.ITurbineBattery;

public class Battery implements ITurbineBattery {
    @Override
    public long extract(long toExtract) {
        return 0;
    }
    
    @Override
    public long stored() {
        return 0;
    }
    
    @Override
    public long capacity() {
        return 0;
    }
    
    @Override
    public CompoundNBT serializeNBT() {
        return null;
    }
    
    @Override
    public void deserializeNBT(CompoundNBT nbt) {
    
    }
}
