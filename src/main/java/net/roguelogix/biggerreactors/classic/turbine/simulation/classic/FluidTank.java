package net.roguelogix.biggerreactors.classic.turbine.simulation.classic;

import net.roguelogix.biggerreactors.classic.turbine.simulation.ITurbineFluidTank;
import net.roguelogix.biggerreactors.util.FluidTransitionTank;

public class FluidTank extends FluidTransitionTank implements ITurbineFluidTank {
    public FluidTank() {
        super(true);
    }
    
    @Override
    public void dumpLiquid() {
        dumpTank(1);
    }
    
    @Override
    public void dumpVapor() {
        dumpTank(0);
    }
    
    void flow(long amount, boolean ventExcess){
        long maxTransitionable = inAmount;
    
        if(!ventExcess){
            maxTransitionable = Math.min(maxTransitionable, perSideCapacity - outAmount);
        }
        
        maxTransitionedLastTick = amount;
        amount = Math.min(maxTransitionable, amount);
        transitionedLastTick = amount;
    
        inAmount -= amount;
        outAmount += amount;
        
        if(ventExcess){
            outAmount = Math.min(outAmount, perSideCapacity);
        }
    }
}
