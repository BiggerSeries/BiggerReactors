package net.roguelogix.biggerreactors.classic.reactor.simulation;

import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.INBTSerializable;
import net.roguelogix.phosphophyllite.fluids.IPhosphophylliteFluidHandler;

public interface IReactorCoolantTank extends INBTSerializable<CompoundNBT>, IPhosphophylliteFluidHandler {
    
    default long perSideCapacity(){
        return getTankCapacity(0);
    }
    
    default long liquidAmount(){
        return fluidAmountInTank(0);
    }
    
    default long vaporAmount(){
        return fluidAmountInTank(1);
    }
    
    void dumpLiquid();
    
    void dumpVapor();
}
