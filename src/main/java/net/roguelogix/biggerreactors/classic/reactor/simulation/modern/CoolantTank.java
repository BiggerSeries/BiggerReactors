package net.roguelogix.biggerreactors.classic.reactor.simulation.modern;

import net.minecraft.block.Blocks;
import net.roguelogix.biggerreactors.classic.reactor.ReactorModeratorRegistry;
import net.roguelogix.biggerreactors.classic.reactor.simulation.IReactorCoolantTank;
import net.roguelogix.biggerreactors.util.FluidTransitionTank;

public class CoolantTank extends FluidTransitionTank implements IReactorCoolantTank, ReactorModeratorRegistry.IModeratorProperties {
    public CoolantTank() {
        super(false);
    }
    
    @Override
    public void dumpLiquid() {
        dumpTank(IN_TANK);
    }
    
    @Override
    public void dumpVapor() {
        dumpTank(OUT_TANK);
    }
    
    private ReactorModeratorRegistry.IModeratorProperties airProperties = ReactorModeratorRegistry.EMPTY_MODERATOR;
    private ReactorModeratorRegistry.IModeratorProperties liquidProperties = ReactorModeratorRegistry.EMPTY_MODERATOR;
    private ReactorModeratorRegistry.IModeratorProperties vaporProperties = ReactorModeratorRegistry.EMPTY_MODERATOR;
    
    @Override
    protected void transitionUpdate() {
        airProperties = ReactorModeratorRegistry.blockModeratorProperties(Blocks.AIR);
        if(airProperties == null){
            airProperties = ReactorModeratorRegistry.EMPTY_MODERATOR;
        }
        liquidProperties = ReactorModeratorRegistry.blockModeratorProperties(inFluid.getFluid().getDefaultState().getBlockState().getBlock());
        if(liquidProperties == null){
            liquidProperties = airProperties;
        }
        vaporProperties = ReactorModeratorRegistry.blockModeratorProperties(outFluid.getFluid().getDefaultState().getBlockState().getBlock());
        if(vaporProperties == null){
            vaporProperties = airProperties;
        }
    }
    
    @Override
    public double absorption() {
        double absorption = 0;
        absorption += airProperties.absorption() * ((perSideCapacity * 2) - (inAmount + outAmount));
        absorption += liquidProperties.absorption() * inAmount;
        absorption += vaporProperties.absorption() * outAmount;
        absorption /= perSideCapacity * 2;
        return absorption;
    }
    
    @Override
    public double heatEfficiency() {
        double heatEfficiency = 0;
        heatEfficiency += airProperties.heatEfficiency() * ((perSideCapacity * 2) - (inAmount + outAmount));
        heatEfficiency += liquidProperties.heatEfficiency() * inAmount;
        heatEfficiency += vaporProperties.heatEfficiency() * outAmount;
        heatEfficiency /= perSideCapacity * 2;
        return heatEfficiency;
    }
    
    @Override
    public double moderation() {
        double moderation = 0;
        moderation += airProperties.moderation() * ((perSideCapacity * 2) - (inAmount + outAmount));
        moderation += liquidProperties.moderation() * inAmount;
        moderation += vaporProperties.moderation() * outAmount;
        moderation /= perSideCapacity * 2;
        return moderation;
    }
    
    @Override
    public double heatConductivity() {
        double heatConductivity = 0;
        heatConductivity += airProperties.heatConductivity() * ((perSideCapacity * 2) - (inAmount + outAmount));
        heatConductivity += liquidProperties.heatConductivity() * inAmount;
        heatConductivity += vaporProperties.heatConductivity() * outAmount;
        heatConductivity /= perSideCapacity * 2;
        return heatConductivity;
    }
}
