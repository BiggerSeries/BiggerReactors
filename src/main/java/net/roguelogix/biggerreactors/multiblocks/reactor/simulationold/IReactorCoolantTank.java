package net.roguelogix.biggerreactors.multiblocks.reactor.simulationold;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.util.INBTSerializable;
import net.roguelogix.phosphophyllite.fluids.IPhosphophylliteFluidHandler;

public interface IReactorCoolantTank extends INBTSerializable<CompoundTag>, IPhosphophylliteFluidHandler {
    
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
