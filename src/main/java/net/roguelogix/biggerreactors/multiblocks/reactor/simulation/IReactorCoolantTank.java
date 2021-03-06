package net.roguelogix.biggerreactors.multiblocks.reactor.simulation;

import net.minecraft.fluid.Fluid;
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
    
    default Fluid liquidType(){
        return fluidTypeInTank(0);
    }
    
    default long vaporAmount(){
        return fluidAmountInTank(1);
    }
    
    default Fluid vaporType(){
        return fluidTypeInTank(1);
    }
    
    void dumpLiquid();
    
    void dumpVapor();
}
