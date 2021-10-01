package net.roguelogix.biggerreactors.multiblocks.reactor.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.IReactorSimulation;
import net.roguelogix.biggerreactors.registries.FluidTransitionRegistry;
import net.roguelogix.biggerreactors.registries.ReactorModeratorRegistry;
import net.roguelogix.biggerreactors.util.FluidTransitionTank;
import net.roguelogix.phosphophyllite.util.HeatBody;
import net.roguelogix.phosphophyllite.util.MethodsReturnNonnullByDefault;
import org.apache.commons.lang3.NotImplementedException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ReactorTransitionTank extends FluidTransitionTank {
    
    private final IReactorSimulation.ICoolantTank internalTank;
    
    @Override
    public long transitionedLastTick() {
        return internalTank.transitionedLastTick();
    }
    
    @Override
    public long maxTransitionedLastTick() {
        return internalTank.maxTransitionedLastTick();
    }
    
    @Override
    public double transferWith(HeatBody body, double rfkt) {
        throw new NotImplementedException("");
    }
    
    @Override
    public double absorbRF(double rf) {
        throw new NotImplementedException("");
    }
    
    public ReactorTransitionTank(IReactorSimulation.ICoolantTank internalTank) {
        super(false);
        this.internalTank = internalTank;
        perSideCapacity = internalTank.perSideCapacity();
        inAmount = internalTank.liquidAmount();
        outAmount = internalTank.vaporAmount();
    }
    
    public long perSideCapacity() {
        return getTankCapacity(0);
    }
    
    public long liquidAmount() {
        return fluidAmountInTank(0);
    }
    
    public Fluid liquidType() {
        return fluidTypeInTank(0);
    }
    
    public long vaporAmount() {
        return fluidAmountInTank(1);
    }
    
    public Fluid vaporType() {
        return fluidTypeInTank(1);
    }
    
    public void dumpLiquid() {
        dumpTank(IN_TANK);
    }
    
    public void dumpVapor() {
        dumpTank(OUT_TANK);
    }
    
    @Override
    public void dumpTank(int tank) {
        super.dumpTank(tank);
        syncTanks();
    }
    
    private void syncTanks() {
        internalTank.insertLiquid(liquidAmount() - internalTank.liquidAmount());
        internalTank.insertVapor(vaporAmount() - internalTank.vaporAmount());
    }
    
    @Override
    @SuppressWarnings("deprecation")
    public long fill(@Nonnull Fluid fluid, long amount, boolean simulate, @Nullable FluidTransitionRegistry.FluidTransition transition) {
        long filled = super.fill(fluid, amount, simulate, transition);
        if (!simulate) {
            syncTanks();
        }
        return filled;
    }
    
    public long drain(@Nonnull Fluid fluid, @Nullable CompoundTag tag, long amount, boolean simulate) {
        long drained = super.drain(fluid, tag, amount, simulate);
        if (!simulate) {
            syncTanks();
        }
        return drained;
    }
    
    @Override
    protected void transitionUpdate() {
        var airProperties = ReactorModeratorRegistry.blockModeratorProperties(Blocks.AIR);
        if (airProperties == null) {
            airProperties = ReactorModeratorRegistry.ModeratorProperties.EMPTY_MODERATOR;
        }
        var liquidProperties = airProperties;
        Fluid liquid = inFluid;
        if (liquid != null) {
            liquidProperties = ReactorModeratorRegistry.blockModeratorProperties(liquid.defaultFluidState().createLegacyBlock().getBlock());
            if (liquidProperties == null) {
                liquidProperties = airProperties;
            }
        }
        internalTank.setTransitionProperties(activeTransition);
        internalTank.setModeratorProperties(liquidProperties);
    }
}
