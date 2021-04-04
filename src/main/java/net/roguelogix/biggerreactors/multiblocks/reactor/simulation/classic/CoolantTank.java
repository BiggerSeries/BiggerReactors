package net.roguelogix.biggerreactors.multiblocks.reactor.simulation.classic;

import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.CompoundNBT;
import net.roguelogix.biggerreactors.multiblocks.reactor.ReactorModeratorRegistry;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.IReactorCoolantTank;
import net.roguelogix.biggerreactors.util.FluidTransitionTank;

import javax.annotation.Nonnull;

public class CoolantTank extends FluidTransitionTank implements IReactorCoolantTank, ReactorModeratorRegistry.IModeratorProperties {
    
    public CoolantTank() {
        // always an evaporator here
        super(false);
        transitionUpdate();
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
        return Math.min(reactorHeat, activeTransition.boilingPoint - 273.15);
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
    
    
    private ReactorModeratorRegistry.IModeratorProperties airProperties = ReactorModeratorRegistry.EMPTY_MODERATOR;
    private ReactorModeratorRegistry.IModeratorProperties liquidProperties = ReactorModeratorRegistry.EMPTY_MODERATOR;
    private ReactorModeratorRegistry.IModeratorProperties vaporProperties = ReactorModeratorRegistry.EMPTY_MODERATOR;
    
    @Override
    protected void transitionUpdate() {
        airProperties = ReactorModeratorRegistry.blockModeratorProperties(Blocks.AIR);
        if (airProperties == null) {
            airProperties = ReactorModeratorRegistry.EMPTY_MODERATOR;
        }
        liquidProperties = airProperties;
        vaporProperties = airProperties;
        Fluid liquid = inFluid;
        if (liquid != null) {
            liquidProperties = ReactorModeratorRegistry.blockModeratorProperties(liquid.getDefaultState().getBlockState().getBlock());
            if (liquidProperties == null) {
                liquidProperties = airProperties;
            }
        }
        Fluid vapor = inFluid;
        if (vapor != null) {
            vaporProperties = ReactorModeratorRegistry.blockModeratorProperties(vapor.getDefaultState().getBlockState().getBlock());
            if (vaporProperties == null) {
                vaporProperties = airProperties;
            }
        }
    }
    
    @Override
    public double absorption() {
        if (perSideCapacity == 0) {
            return airProperties.absorption();
        }
        double absorption = 0;
        absorption += airProperties.absorption() * ((perSideCapacity * 2) - (inAmount + outAmount));
        absorption += liquidProperties.absorption() * inAmount;
        absorption += vaporProperties.absorption() * outAmount;
        absorption /= perSideCapacity * 2;
        return absorption;
    }
    
    @Override
    public double heatEfficiency() {
        if (perSideCapacity == 0) {
            return airProperties.heatEfficiency();
        }
        double heatEfficiency = 0;
        heatEfficiency += airProperties.heatEfficiency() * ((perSideCapacity * 2) - (inAmount + outAmount));
        heatEfficiency += liquidProperties.heatEfficiency() * inAmount;
        heatEfficiency += vaporProperties.heatEfficiency() * outAmount;
        heatEfficiency /= perSideCapacity * 2;
        return heatEfficiency;
    }
    
    @Override
    public double moderation() {
        if (perSideCapacity == 0) {
            return airProperties.moderation();
        }
        double moderation = 0;
        moderation += airProperties.moderation() * ((perSideCapacity * 2) - (inAmount + outAmount));
        moderation += liquidProperties.moderation() * inAmount;
        moderation += vaporProperties.moderation() * outAmount;
        moderation /= perSideCapacity * 2;
        return moderation;
    }
    
    @Override
    public double heatConductivity() {
        if (perSideCapacity == 0) {
            return airProperties.heatConductivity();
        }
        double heatConductivity = 0;
        heatConductivity += airProperties.heatConductivity() * ((perSideCapacity * 2) - (inAmount + outAmount));
        heatConductivity += liquidProperties.heatConductivity() * inAmount;
        heatConductivity += vaporProperties.heatConductivity() * outAmount;
        heatConductivity /= perSideCapacity * 2;
        return heatConductivity;
    }
}
