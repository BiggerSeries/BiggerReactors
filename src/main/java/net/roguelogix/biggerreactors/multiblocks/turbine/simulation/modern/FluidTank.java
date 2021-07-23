package net.roguelogix.biggerreactors.multiblocks.turbine.simulation.modern;

import net.minecraft.world.level.material.Fluid;
import net.roguelogix.biggerreactors.multiblocks.turbine.simulation.ITurbineFluidTank;
import net.roguelogix.biggerreactors.registries.FluidTransitionRegistry;
import net.roguelogix.biggerreactors.util.FluidTransitionTank;

import javax.annotation.Nullable;

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
    
    long flow(long amount, boolean ventExcess){
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
        
        return transitionedLastTick;
    }
    
    @Nullable
    @Override
    protected FluidTransitionRegistry.FluidTransition selectTransition(Fluid fluid) {
        FluidTransitionRegistry.FluidTransition transition = super.selectTransition(fluid);
        if(transition != null && transition.turbineMultiplier == 0){
            return null;
        }
        return transition;
    }
}
