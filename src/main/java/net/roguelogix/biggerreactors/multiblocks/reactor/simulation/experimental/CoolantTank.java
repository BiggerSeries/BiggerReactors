package net.roguelogix.biggerreactors.multiblocks.reactor.simulation.experimental;

import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluid;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.IReactorCoolantTank;
import net.roguelogix.biggerreactors.registries.ReactorModeratorRegistry;
import net.roguelogix.biggerreactors.util.FluidTransitionTank;

public class CoolantTank extends FluidTransitionTank implements IReactorCoolantTank, ReactorModeratorRegistry.IModeratorProperties {
    public CoolantTank() {
        super(false);
        transitionUpdate();
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
    
    @Override
    protected void transitionUpdate() {
        airProperties = ReactorModeratorRegistry.blockModeratorProperties(Blocks.AIR);
        if (airProperties == null) {
            airProperties = ReactorModeratorRegistry.EMPTY_MODERATOR;
        }
        liquidProperties = airProperties;
        Fluid liquid = inFluid;
        if (liquid != null) {
            liquidProperties = ReactorModeratorRegistry.blockModeratorProperties(liquid.getDefaultState().getBlockState().getBlock());
            if (liquidProperties == null) {
                liquidProperties = airProperties;
            }
        }
    }
    
    @Override
    public double absorption() {
        if (perSideCapacity == 0) {
            return airProperties.absorption();
        }
        double absorption = 0;
        absorption += airProperties.absorption() * ((perSideCapacity) - (inAmount));
        absorption += liquidProperties.absorption() * inAmount;
        absorption /= perSideCapacity;
        return absorption;
    }
    
    @Override
    public double heatEfficiency() {
        if (perSideCapacity == 0) {
            return airProperties.heatEfficiency();
        }
        double heatEfficiency = 0;
        heatEfficiency += airProperties.heatEfficiency() * ((perSideCapacity) - (inAmount));
        heatEfficiency += liquidProperties.heatEfficiency() * inAmount;
        heatEfficiency /= perSideCapacity;
        return heatEfficiency;
    }
    
    @Override
    public double moderation() {
        if (perSideCapacity == 0) {
            return airProperties.moderation();
        }
        double moderation = 0;
        moderation += airProperties.moderation() * ((perSideCapacity) - (inAmount));
        moderation += liquidProperties.moderation() * inAmount;
        moderation /= perSideCapacity;
        return moderation;
    }
    
    @Override
    public double heatConductivity() {
        if (perSideCapacity == 0) {
            return airProperties.heatConductivity();
        }
        double heatConductivity = 0;
        heatConductivity += airProperties.heatConductivity() * ((perSideCapacity) - (inAmount));
        heatConductivity += liquidProperties.heatConductivity() * inAmount;
        heatConductivity /= perSideCapacity;
        return heatConductivity;
    }
}
