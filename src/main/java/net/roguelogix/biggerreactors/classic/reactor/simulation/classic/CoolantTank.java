package net.roguelogix.biggerreactors.classic.reactor.simulation.classic;

import net.minecraft.nbt.CompoundNBT;
import net.roguelogix.biggerreactors.classic.reactor.simulation.IReactorCoolantTank;
import net.roguelogix.biggerreactors.util.FluidTransitionTank;

import javax.annotation.Nonnull;

public class CoolantTank extends FluidTransitionTank implements IReactorCoolantTank {
    
    public CoolantTank() {
        // always an evaporator here
        super(false);
    }
    
    double absorbHeat(double rfTransferred) {
        transitionedLastTick = 0;
        if (inAmount <= 0 || rfTransferred <= 0) {
            return rfTransferred;
        }
        
        long amountVaporized = (long) (rfTransferred / activeTransition.latentHeat);
        maxTransitionedLastTick = amountVaporized;
        
        amountVaporized = Math.min(inAmount, amountVaporized);
        amountVaporized = Math.min(amountVaporized, perSideCapacity - outAmount);
        
        if (amountVaporized < 1) {
            return rfTransferred;
        }
        
        transitionedLastTick = amountVaporized;
        inAmount -= amountVaporized;
        outAmount += amountVaporized;
        
        double energyUsed = amountVaporized * activeTransition.latentHeat;
        
        return Math.max(0, rfTransferred - energyUsed);
    }
    
    public double getCoolantTemperature(double reactorHeat) {
        if (inAmount <= 0) {
            return reactorHeat;
        }
        return Math.min(reactorHeat, activeTransition.boilingPoint);
    }
    
    @Override
    @Nonnull
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = super.serializeNBT();
        nbt.putLong("perSideCapacity", perSideCapacity);
        return nbt;
    }
    
    @Override
    public void deserializeNBT(@Nonnull CompoundNBT nbt) {
        super.deserializeNBT(nbt);
        perSideCapacity = nbt.getLong("perSideCapacity");
    }
    
    @Override
    public void dumpLiquid() {
        dumpTank(IN_TANK);
    }
    
    @Override
    public void dumpVapor() {
        dumpTank(OUT_TANK);
    }
}
