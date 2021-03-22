package net.roguelogix.biggerreactors.multiblocks.heatexchanger.deps;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraftforge.common.util.LazyOptional;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.HeatExchangerMultiblockController;
import net.roguelogix.biggerreactors.util.FluidTransitionTank;
import net.roguelogix.phosphophyllite.util.HeatBody;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

public class HeatExchangerPeripheral implements IPeripheral {
    
    final Supplier<HeatExchangerMultiblockController> controllerSupplier;
    final Supplier<Lock> lockSupplier;
    
    final Channel condenser;
    final Channel evaporator;
    final InternalEnvironment internalEnvironment;
    
    public static LazyOptional<Object> create(@Nonnull Supplier<HeatExchangerMultiblockController> controllerSupplier) {
        return LazyOptional.of(() -> new HeatExchangerPeripheral(controllerSupplier));
    }
    
    public HeatExchangerPeripheral(Supplier<HeatExchangerMultiblockController> controllerSupplier) {
        this.controllerSupplier = controllerSupplier;
        lockSupplier = () -> controllerSupplier.get().locks.readLock();
        condenser = new Channel(() -> controllerSupplier.get().condenserHeatBody, () -> controllerSupplier.get().condenserTank, () -> controllerSupplier.get().condenserChannels.size(), lockSupplier);
        evaporator = new Channel(() -> controllerSupplier.get().evaporatorHeatBody, () -> controllerSupplier.get().evaporatorTank, () -> controllerSupplier.get().evaporatorChannels.size(), lockSupplier);
        internalEnvironment = new InternalEnvironment(() -> controllerSupplier.get().airHeatBody);
    }
    
    public static class Channel {
        final Supplier<HeatBody> heatBodySupplier;
        final Supplier<FluidTransitionTank> transitionTankSupplier;
        final Supplier<Integer> countSupplier;
        final Supplier<Lock> lockSupplier;
        
        final ChannelFluid inputFluid;
        final ChannelFluid outputFluid;
        
        Channel(Supplier<HeatBody> heatBodySupplier, Supplier<FluidTransitionTank> transitionTankSupplier, Supplier<Integer> countSupplier, Supplier<Lock> lockSupplier) {
            this.heatBodySupplier = heatBodySupplier;
            this.transitionTankSupplier = transitionTankSupplier;
            this.countSupplier = countSupplier;
            this.lockSupplier = lockSupplier;
            inputFluid = new ChannelFluid(transitionTankSupplier, 0);
            outputFluid = new ChannelFluid(transitionTankSupplier, 1);
        }
        
        public static class ChannelFluid {
            final Supplier<FluidTransitionTank> transitionTankSupplier;
            final int tankNum;
            
            public ChannelFluid(Supplier<FluidTransitionTank> transitionTankSupplier, int tankNum) {
                this.transitionTankSupplier = transitionTankSupplier;
                this.tankNum = tankNum;
            }
            
            @LuaFunction
            public String name() {
                return transitionTankSupplier.get().fluidTypeInTank(tankNum).getRegistryName().toString();
            }
            
            @LuaFunction
            public long amount() {
                return transitionTankSupplier.get().fluidAmountInTank(tankNum);
            }
            
            @LuaFunction
            public long maxAmount() {
                return transitionTankSupplier.get().perSideCapacity;
            }
        }
        
        @LuaFunction
        public double temperature() {
            return heatBodySupplier.get().temperature();
        }
        
        @LuaFunction
        public double rfPerKelvin() {
            return heatBodySupplier.get().rfPerKelvin();
        }
        
        @LuaFunction
        public ChannelFluid input() {
            return inputFluid;
        }
        
        @LuaFunction
        public ChannelFluid output() {
            return outputFluid;
        }
        
        @LuaFunction
        public long transitionedLastTick() {
            return transitionTankSupplier.get().transitionedLastTick();
        }
        
        @LuaFunction
        public long maxTransitionedLastTick() {
            return transitionTankSupplier.get().maxTransitionedLastTick();
        }
        
        @LuaFunction
        public double transitionEnergy() {
            return transitionTankSupplier.get().activeTransition().latentHeat;
        }
    }
    
    @LuaFunction
    public Channel condenser() {
        return condenser;
    }
    
    @LuaFunction
    public Channel evaporator() {
        return evaporator;
    }
    
    public static class InternalEnvironment {
        final Supplier<HeatBody> heatBodySupplier;
        
        public InternalEnvironment(Supplier<HeatBody> heatBodySupplier) {
            this.heatBodySupplier = heatBodySupplier;
        }
        
        @LuaFunction
        public double temperature() {
            return heatBodySupplier.get().temperature();
        }
        
        @LuaFunction
        public double rfPerKelvin() {
            return heatBodySupplier.get().rfPerKelvin();
        }
        
    }
    
    @LuaFunction
    public InternalEnvironment internalEnvironment() {
        return internalEnvironment;
    }
    
    @LuaFunction
    public double ambientTemperature() {
        return controllerSupplier.get().ambientHeatBody.temperature();
    }
    
    @LuaFunction
    public double ambientInternalRFKT() {
        return controllerSupplier.get().airAmbientRFKT;
    }
    
    @LuaFunction
    public double condenserInternalRFKT() {
        return controllerSupplier.get().condenserAirRFKT;
    }
    
    @LuaFunction
    public double evaporatorInternalRFKT() {
        return controllerSupplier.get().evaporatorAirRFKT;
    }
    
    @LuaFunction
    public double condenserEvaporatorRFKT() {
        return controllerSupplier.get().channelRFKT;
    }
    
    @Nonnull
    @Override
    public String getType() {
        return "BiggerReactors_Heat-Exchanger_";
    }
    
    @Override
    public boolean equals(@Nullable IPeripheral iPeripheral) {
        return iPeripheral == this;
    }
}
