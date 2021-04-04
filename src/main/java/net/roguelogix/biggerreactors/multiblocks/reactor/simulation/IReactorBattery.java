package net.roguelogix.biggerreactors.multiblocks.reactor.simulation;

import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.INBTSerializable;

public interface IReactorBattery extends INBTSerializable<CompoundNBT> {
    long extract(long toExtract);
    
    long stored();
    
    long capacity();
}
