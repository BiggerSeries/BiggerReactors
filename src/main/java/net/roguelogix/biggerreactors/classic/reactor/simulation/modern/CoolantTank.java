package net.roguelogix.biggerreactors.classic.reactor.simulation.modern;

import net.roguelogix.biggerreactors.classic.reactor.simulation.IReactorCoolantTank;
import net.roguelogix.biggerreactors.util.FluidTransitionTank;

public class CoolantTank extends FluidTransitionTank implements IReactorCoolantTank {
    public CoolantTank() {
        super(false);
    }
    
    public double getCoolantTemperature(double reactorHeat) {
        if (inAmount <= 0) {
            return reactorHeat;
        }
        return Math.min(reactorHeat, activeTransition.boilingPoint);
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
