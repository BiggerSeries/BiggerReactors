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
public class FluidTransitionTank implements IPhosphophylliteFluidHandler, INBTSerializable<CompoundNBT> {
    
    public final boolean condenser;
    
    public long perSideCapacity;
    private FluidTransitionRegistry.FluidTransition activeTransition;
    
    private static final int IN_TANK = 0;
    Fluid inFluid;
    long inAmount = 0;
    
    private static final int OUT_TANK = 1;
    Fluid outFluid;
    long outAmount = 0;
    
    public FluidTransitionTank(boolean condenser) {
        this.condenser = condenser;
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
        if (tank == IN_TANK) {
            return inFluid;
        }
        if (tank == OUT_TANK) {
            return outFluid;
        }
        return Fluids.EMPTY;
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
    public long fill(@Nonnull Fluid fluid, long amount, boolean simulate) {
        return fill(fluid, amount, simulate, activeTransition);
    }
    
    private long fill(@Nonnull Fluid fluid, long amount, boolean simulate, @Nullable FluidTransitionRegistry.FluidTransition transition) {
        if (transition == null) {
            if (condenser) {
                transition = FluidTransitionRegistry.gasTransition(fluid);
            } else {
                transition = FluidTransitionRegistry.liquidTransition(fluid);
            }
        }
        if (transition == null) {
            return 0;
        }
        if (fluid == inFluid || (condenser ? transition.gases.contains(fluid) : transition.liquids.contains(fluid))) {
            inFluid = fluid;
            long maxFill = perSideCapacity - inAmount;
            long toFill = Math.min(amount, maxFill);
            if (!simulate) {
                if(activeTransition == null){
                    activeTransition = transition;
                    outFluid = condenser ? activeTransition.liquids.get(0) : activeTransition.gases.get(0);
                }
                inFluid = fluid;
                inAmount += toFill;
            }
            return toFill;
        } else {
            return fill(fluid, amount, simulate, null);
        }
    }
    
    @Override
    public long drain(@Nonnull Fluid fluid, long amount, boolean simulate) {
        if (activeTransition == null) {
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
    
    public void transferWith(HeatBody body, int surfaceArea) {
        if (activeTransition == null || inAmount <= 0 || outAmount >= perSideCapacity) {
            return;
        }
    
        double rfkt = surfaceArea * (condenser ? activeTransition.gasRFMKT : activeTransition.liquidRFMKT);
    
        rfkt *= (double) inAmount / (double) perSideCapacity;
    
        double newTemp = body.temperature - activeTransition.boilingPoint;
        newTemp *= Math.exp(-rfkt / body.rfPerKelvin);
        newTemp += activeTransition.boilingPoint;
    
        double toTransfer = newTemp - body.temperature;
        toTransfer *= body.rfPerKelvin;
    
        if ((toTransfer > 0 && !condenser) || (toTransfer < 0 && condenser)) {
            return;
        }
        
        toTransfer = Math.abs(toTransfer);
        
        double maxTransitionable = Math.min(inAmount, perSideCapacity - outAmount);
        toTransfer = Math.min(maxTransitionable * activeTransition.latentHeat, toTransfer);
        
        long toTransition = (long) (toTransfer / activeTransition.latentHeat);
        inAmount -= toTransition;
        outAmount += toTransition;
    
        toTransfer = toTransition * activeTransition.latentHeat;
        if (!condenser) {
            toTransfer *= -1;
        }
    
        body.absorbRF(toTransfer);
    }
    
    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putString("inFluid", inFluid.getRegistryName().toString());
        nbt.putLong("inAmount", inAmount);
        nbt.putString("outFluid", outFluid.getRegistryName().toString());
        nbt.putLong("outAmount", outAmount);
        return nbt;
    }
    
    @Override
    public void deserializeNBT(CompoundNBT nbt) {
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
                if(outFluidList.contains(oldOutFluid)){
                    newOutFluid = oldOutFluid;
                }
            }
            if(newOutFluid == null){
                newOutFluid = outFluidList.get(0);
            }
            activeTransition = newTransition;
            inFluid = newInFluid;
            outFluid = newOutFluid;
            inAmount = nbt.getLong("inAmount");
            outAmount = nbt.getLong("outAmount");
        }
    }
}
