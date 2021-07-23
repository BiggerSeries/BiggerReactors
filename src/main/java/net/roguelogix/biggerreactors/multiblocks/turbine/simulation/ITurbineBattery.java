package net.roguelogix.biggerreactors.multiblocks.turbine.simulation;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public interface ITurbineBattery extends INBTSerializable<CompoundTag> {
    long extract(long toExtract);
    
    long stored();
    
    long capacity();
}
