package net.roguelogix.biggerreactors.multiblocks.reactor.simulation.base;

import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.SimulationConfiguration;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.IReactorSimulation;
import net.roguelogix.biggerreactors.registries.FluidTransitionRegistry;
import net.roguelogix.biggerreactors.registries.ReactorModeratorRegistry;
import net.roguelogix.phosphophyllite.serialization.IPhosphophylliteSerializable;
import net.roguelogix.phosphophyllite.serialization.PhosphophylliteCompound;
import net.roguelogix.phosphophyllite.util.HeatBody;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CoolantTank extends HeatBody implements IReactorSimulation.ICoolantTank, ReactorModeratorRegistry.IModeratorProperties, IPhosphophylliteSerializable {
    private final SimulationConfiguration configuration;
    
    private final long perSideCapacity;
    private long liquidAmount = 0;
    private long vaporAmount = 0;
    
    private final ReactorModeratorRegistry.IModeratorProperties defaultModeratorProperties;
    @Nullable
    private ReactorModeratorRegistry.IModeratorProperties moderatorProperties = null;
    @Nullable
    private FluidTransitionRegistry.ITransitionProperties transitionProperties = null;
    private long maxTransitionedLastTick;
    private long transitionedLastTick;
    private long rfTransferredLastTick;
    
    CoolantTank(long perSideCapacity, ReactorModeratorRegistry.IModeratorProperties defaultModeratorProperties, SimulationConfiguration configuration) {
        this.configuration = configuration;
        this.perSideCapacity = perSideCapacity;
        this.defaultModeratorProperties = defaultModeratorProperties;
        this.setInfinite(true);
    }
    
    @Override
    public double transferWith(HeatBody body, double rfkt) {
        if (transitionProperties == null) {
            maxTransitionedLastTick = 0;
            transitionedLastTick = 0;
            rfTransferredLastTick = 0;
            return 0;
        }
        
        rfkt *= transitionProperties.liquidRFMKT();
        
        double multiplier = (double) liquidAmount / (double) perSideCapacity;
        rfkt *= Math.max(multiplier, 0.01);
        
        double newTemp = body.temperature() - transitionProperties.boilingPoint();
        newTemp *= Math.exp(-rfkt / body.rfPerKelvin());
        newTemp += transitionProperties.boilingPoint();
        
        double toTransfer = newTemp - body.temperature();
        toTransfer *= body.rfPerKelvin();
        
        toTransfer = absorbRF(toTransfer);
        
        body.absorbRF(toTransfer);
        return -toTransfer;
    }
    
    @Override
    public double absorbRF(double rf) {
        if (rf > 0 || transitionProperties == null) {
            maxTransitionedLastTick = 0;
            transitionedLastTick = 0;
            rfTransferredLastTick = 0;
            return 0;
        }
        
        rf = Math.abs(rf);
    
        final double transitionMultiplier = configuration.outputMultiplier() * configuration.activeOutputMultiplier();
        final double effectiveLatentHeat = transitionProperties.latentHeat() * transitionMultiplier;
        
        long toTransition = (long) (rf / effectiveLatentHeat);
        final long maxTransitionable = Math.min(liquidAmount, perSideCapacity - vaporAmount);
        
        maxTransitionedLastTick = toTransition;
        toTransition = Math.min(maxTransitionable, toTransition);
        transitionedLastTick = toTransition;
        
        liquidAmount -= transitionedLastTick;
        vaporAmount += transitionedLastTick;
        
        rf = toTransition * effectiveLatentHeat;
        rfTransferredLastTick = (long) rf;
        rf *= -1;
        
        return rf;
    }
    
    @Override
    public void dumpLiquid() {
        liquidAmount = 0;
    }
    
    @Override
    public void dumpVapor() {
        vaporAmount = 0;
    }
    
    @Override
    public long insertLiquid(long amount) {
        liquidAmount += amount;
        return amount;
    }
    
    @Override
    public long extractLiquid(long amount) {
        liquidAmount -= amount;
        return amount;
    }
    
    @Override
    public long insertVapor(long amount) {
        vaporAmount += amount;
        return amount;
    }
    
    @Override
    public long extractVapor(long amount) {
        vaporAmount -= amount;
        return amount;
    }
    
    @Override
    public long liquidAmount() {
        return liquidAmount;
    }
    
    @Override
    public long vaporAmount() {
        return vaporAmount;
    }
    
    @Override
    public long perSideCapacity() {
        return perSideCapacity;
    }
    
    @Override
    public void setModeratorProperties(ReactorModeratorRegistry.IModeratorProperties moderatorProperties) {
        this.moderatorProperties = moderatorProperties;
    }
    
    @Override
    public void setTransitionProperties(FluidTransitionRegistry.ITransitionProperties transitionProperties) {
        this.transitionProperties = transitionProperties;
    }
    
    @Override
    public long transitionedLastTick() {
        return transitionedLastTick;
    }
    
    @Override
    public long maxTransitionedLastTick() {
        return maxTransitionedLastTick;
    }
    
    @Override
    public long rfTransferredLastTick() {
        return rfTransferredLastTick;
    }
    
    @Override
    public double absorption() {
        if (perSideCapacity == 0 || moderatorProperties == null) {
            return defaultModeratorProperties.absorption();
        }
        double absorption = 0;
        absorption += defaultModeratorProperties.absorption() * ((perSideCapacity) - (liquidAmount));
        absorption += moderatorProperties.absorption() * liquidAmount;
        absorption /= perSideCapacity;
        return absorption;
    }
    
    @Override
    public double heatEfficiency() {
        if (perSideCapacity == 0 || moderatorProperties == null) {
            return defaultModeratorProperties.heatEfficiency();
        }
        double heatEfficiency = 0;
        heatEfficiency += defaultModeratorProperties.heatEfficiency() * ((perSideCapacity) - (liquidAmount));
        heatEfficiency += moderatorProperties.heatEfficiency() * liquidAmount;
        heatEfficiency /= perSideCapacity;
        return heatEfficiency;
    }
    
    @Override
    public double moderation() {
        if (perSideCapacity == 0 || moderatorProperties == null) {
            return defaultModeratorProperties.moderation();
        }
        double moderation = 0;
        moderation += defaultModeratorProperties.moderation() * ((perSideCapacity) - (liquidAmount));
        moderation += moderatorProperties.moderation() * liquidAmount;
        moderation /= perSideCapacity;
        return moderation;
    }
    
    @Override
    public double heatConductivity() {
        if (perSideCapacity == 0 || moderatorProperties == null) {
            return defaultModeratorProperties.heatConductivity();
        }
        double heatConductivity = 0;
        heatConductivity += defaultModeratorProperties.heatConductivity() * ((perSideCapacity) - (liquidAmount));
        heatConductivity += moderatorProperties.heatConductivity() * liquidAmount;
        heatConductivity /= perSideCapacity;
        return heatConductivity;
    }
    
    @Nullable
    @Override
    public PhosphophylliteCompound save() {
        var compound = new PhosphophylliteCompound();
        compound.put("liquidAmount", liquidAmount);
        compound.put("gasAmount", vaporAmount);
        return compound;
    }
    
    @Override
    public void load(@Nonnull PhosphophylliteCompound compound) {
        liquidAmount = compound.getLong("liquidAmount");
        vaporAmount = compound.getLong("gasAmount");
    }
}