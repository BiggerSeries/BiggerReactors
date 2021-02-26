package net.roguelogix.biggerreactors.classic.turbine.simulation;

import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.INBTSerializable;
import net.roguelogix.phosphophyllite.fluids.IPhosphophylliteFluidHandler;

public interface ITurbineFluidTank extends INBTSerializable<CompoundNBT>, IPhosphophylliteFluidHandler {
    default long perSideCapacity() {
        return getTankCapacity(0);
    }
    
    default long liquidAmount() {
        return fluidAmountInTank(1);
    }
    
    default Fluid liquidType() {
        return fluidTypeInTank(1);
    }
    
    default long vaporAmount() {
        return fluidAmountInTank(0);
    }
    
    default Fluid vaporType() {
        return fluidTypeInTank(0);
    }
    
    void dumpLiquid();
    
    void dumpVapor();
}
