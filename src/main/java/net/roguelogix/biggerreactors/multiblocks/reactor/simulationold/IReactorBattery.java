package net.roguelogix.biggerreactors.multiblocks.reactor.simulationold;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public interface IReactorBattery extends INBTSerializable<CompoundTag> {
    long extract(long toExtract);
    
    long stored();
    
    long capacity();
}
