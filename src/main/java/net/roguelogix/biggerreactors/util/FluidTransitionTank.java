package net.roguelogix.biggerreactors.util;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.registries.ForgeRegistries;
import net.roguelogix.biggerreactors.registries.FluidTransitionRegistry;
import net.roguelogix.phosphophyllite.fluids.IPhosphophylliteFluidHandler;
import net.roguelogix.phosphophyllite.util.HeatBody;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class FluidTransitionTank extends HeatBody implements IPhosphophylliteFluidHandler, INBTSerializable<CompoundNBT> {
    
    public final boolean condenser;
    
    public long perSideCapacity;
    protected FluidTransitionRegistry.FluidTransition activeTransition;
    
    public static final int IN_TANK = 0;
    protected Fluid inFluid;
    protected long inAmount = 0;
    
    public static final int OUT_TANK = 1;
    protected Fluid outFluid;
    protected long outAmount = 0;
    
    @Deprecated
    protected long rfTransferredLastTick;
    protected long transitionedLastTick;
    protected long maxTransitionedLastTick;
    
    public FluidTransitionTank(boolean condenser) {
        this.condenser = condenser;
        setInfinite(true);
    }
    
    @Override
    public int tankCount() {
        return 2;
    }
    
    @Override
    public long tankCapacity(int tank) {
        return Long.min(perSideCapacity, Integer.MAX_VALUE);
    }
    
    @Nonnull
    @Override
    public Fluid fluidTypeInTank(int tank) {
        if (tank == IN_TANK && inFluid != null) {
            return inFluid;
        }
        if (tank == OUT_TANK && outFluid != null) {
            return outFluid;
        }
        return Fluids.EMPTY;
    }
    
    @Nullable
    @Override
    public CompoundNBT fluidTagInTank(int tank) {
        return null;
    }
    
    @Override
    public long fluidAmountInTank(int tank) {
        if (tank == IN_TANK) {
            return inAmount;
        }
        if (tank == OUT_TANK) {
            return outAmount;
        }
        return 0;
    }
    
    public long transitionedLastTick() {
        return transitionedLastTick;
    }
    
    public long maxTransitionedLastTick() {
        return maxTransitionedLastTick;
    }
    
    @Deprecated
    public long rfTransferredLastTick() {
        return rfTransferredLastTick;
    }
    
    @Override
    public boolean fluidValidForTank(int tank, @Nonnull Fluid fluid) {
        if (tank == IN_TANK) {
            return FluidTransitionRegistry.liquidTransition(fluid) != null;
        }
        if (tank == OUT_TANK) {
            return FluidTransitionRegistry.gasTransition(fluid) != null;
        }
        return false;
    }
    
    @Override
    public long fill(@Nonnull Fluid fluid, @Nullable CompoundNBT tag, long amount, boolean simulate) {
        if (tag != null) {
            return 0;
        }
        return fill(fluid, amount, simulate, activeTransition);
    }
    
    private long fill(@Nonnull Fluid fluid, long amount, boolean simulate, @Nullable FluidTransitionRegistry.FluidTransition transition) {
        if (transition == null) {
            transition = selectTransition(fluid);
        }
        if (transition == null) {
            return 0;
        }
        if (fluid == inFluid || (condenser ? transition.gases.contains(fluid) : transition.liquids.contains(fluid))) {
            long maxFill = perSideCapacity - inAmount;
            long toFill = Math.min(amount, maxFill);
            if (!simulate) {
                inFluid = fluid;
                if (activeTransition != transition) {
                    outFluid = condenser ? transition.liquids.get(0) : transition.gases.get(0);
                    activeTransition = transition;
                    transitionUpdate();
                }
                inAmount += toFill;
            }
            return toFill;
        } else if (inAmount == 0 && outAmount == 0) {
            return fill(fluid, amount, simulate, null);
        }
        return 0;
    }
    
    @Override
    public long drain(@Nonnull Fluid fluid, @Nullable CompoundNBT tag, long amount, boolean simulate) {
        if (activeTransition == null || tag != null) {
            return 0;
        }
        if (fluid == outFluid || (condenser ? activeTransition.liquids.contains(fluid) : activeTransition.gases.contains(fluid))) {
            long maxDrain = outAmount;
            long toDrain = Math.min(amount, maxDrain);
            if (!simulate) {
                outFluid = fluid;
                outAmount -= toDrain;
            }
            return toDrain;
        }
        return 0;
    }
    
    public void dumpTank(int tank) {
        if (tank == IN_TANK) {
            inAmount = 0;
        }
        if (tank == OUT_TANK) {
            outAmount = 0;
        }
    }
    
    public double transferWith(HeatBody body, double rfkt) {
        if (activeTransition == null) {
            return 0;
        }
        
        rfkt *= (condenser ? activeTransition.gasRFMKT : activeTransition.liquidRFMKT);
        
        double multiplier = (double) inAmount / (double) perSideCapacity;
        rfkt *= Math.max(multiplier, 0.01);
        
        double newTemp = body.temperature() - activeTransition.boilingPoint;
        newTemp *= Math.exp(-rfkt / body.rfPerKelvin());
        newTemp += activeTransition.boilingPoint;
        
        double toTransfer = newTemp - body.temperature();
        toTransfer *= body.rfPerKelvin();
        
        toTransfer = absorbRF(toTransfer);
        
        body.absorbRF(toTransfer);
        return -toTransfer;
    }
    
    @Override
    public double absorbRF(double rf) {
        if ((rf > 0 && !condenser) || (rf < 0 && condenser)) {
            return 0;
        }
        
        rf = Math.abs(rf);
        
        long toTransition = (long) (rf / activeTransition.latentHeat);
        long maxTransitionable = Math.min(inAmount, perSideCapacity - outAmount);
        
        maxTransitionedLastTick = toTransition;
        toTransition = Math.min(maxTransitionable, toTransition);
        transitionedLastTick = toTransition;
        
        inAmount -= toTransition;
        outAmount += toTransition;
        
        rf = toTransition * activeTransition.latentHeat;
        if (!condenser) {
            rf *= -1;
        }
        
        return rf;
    }
    
    protected void transitionUpdate() {
    }
    
    public FluidTransitionRegistry.FluidTransition activeTransition() {
        return activeTransition;
    }
    
    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        if (inFluid != null) {
            nbt.putString("inFluid", inFluid.getRegistryName().toString());
            nbt.putLong("inAmount", inAmount);
            nbt.putString("outFluid", outFluid.getRegistryName().toString());
            nbt.putLong("outAmount", outAmount);
        }
        return nbt;
    }
    
    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        if (!nbt.contains("inFluid")) {
            return;
        }
        ResourceLocation inFluidLocation = new ResourceLocation(nbt.getString("inFluid"));
        if (ForgeRegistries.FLUIDS.containsKey(inFluidLocation)) {
            Fluid newInFluid = ForgeRegistries.FLUIDS.getValue(inFluidLocation);
            if (newInFluid == null) {
                return;
            }
            FluidTransitionRegistry.FluidTransition newTransition;
            if (condenser) {
                newTransition = FluidTransitionRegistry.gasTransition(newInFluid);
            } else {
                newTransition = FluidTransitionRegistry.liquidTransition(newInFluid);
            }
            if (newTransition == null) {
                return;
            }
            List<Fluid> outFluidList = (condenser ? newTransition.liquids : newTransition.gases);
            Fluid newOutFluid = null;
            ResourceLocation outFluidLocation = new ResourceLocation(nbt.getString("outFluid"));
            if (ForgeRegistries.FLUIDS.containsKey(outFluidLocation)) {
                Fluid oldOutFluid = ForgeRegistries.FLUIDS.getValue(outFluidLocation);
                if (outFluidList.contains(oldOutFluid)) {
                    newOutFluid = oldOutFluid;
                }
            }
            if (newOutFluid == null) {
                newOutFluid = outFluidList.get(0);
            }
            activeTransition = newTransition;
            inFluid = newInFluid;
            outFluid = newOutFluid;
            inAmount = nbt.getLong("inAmount");
            outAmount = nbt.getLong("outAmount");
        }
    }
    
    @Nullable
    protected FluidTransitionRegistry.FluidTransition selectTransition(Fluid fluid) {
        if (condenser) {
            return FluidTransitionRegistry.gasTransition(fluid);
        } else {
            return FluidTransitionRegistry.liquidTransition(fluid);
        }
    }
}
