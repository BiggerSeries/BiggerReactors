package net.roguelogix.biggerreactors.multiblocks.reactor.simulation;

import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.INBTSerializable;

public interface IReactorFuelTank extends INBTSerializable<CompoundNBT> {
    long capacity();
    
    long totalStored();
    
    long fuel();
    
    long waste();
    
    long insertFuel(long amount, boolean simulated);
    
    long insertWaste(long amount, boolean simulated);
    
    long extractFuel(long amount, boolean simulated);
    
    long extractWaste(long amount, boolean simulated);
}
