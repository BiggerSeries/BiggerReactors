//package net.roguelogix.biggerreactors.multiblocks.heatexchanger.deps;
//
//import cpw.mods.modlauncher.api.LamdbaExceptionUtils;
//import dan200.computercraft.api.lua.LuaException;
//import dan200.computercraft.api.lua.LuaFunction;
//import dan200.computercraft.api.peripheral.IPeripheral;
//import net.minecraftforge.common.util.LazyOptional;
//import net.roguelogix.biggerreactors.BiggerReactors;
//import net.roguelogix.biggerreactors.multiblocks.heatexchanger.HeatExchangerMultiblockController;
//import net.roguelogix.biggerreactors.util.FluidTransitionTank;
//import net.roguelogix.phosphophyllite.multiblock.MultiblockController;
//import net.roguelogix.phosphophyllite.util.HeatBody;
//
//import javax.annotation.Nonnull;
//import javax.annotation.Nullable;
//import java.util.concurrent.locks.Lock;
//import java.util.function.Supplier;
//
//public class HeatExchangerPeripheral implements IPeripheral {
//
//    final Supplier<HeatExchangerMultiblockController> rawControllerSupplier;
//    final LamdbaExceptionUtils.Supplier_WithExceptions<HeatExchangerMultiblockController, LuaException> controllerSupplier;
//    final LamdbaExceptionUtils.Supplier_WithExceptions<Lock, LuaException> lockSupplier;
//
//    final Channel condenser;
//    final Channel evaporator;
//    final InternalEnvironment internalEnvironment;
//
//    public static LazyOptional<Object> create(@Nonnull Supplier<HeatExchangerMultiblockController> controllerSupplier) {
//        return LazyOptional.of(() -> new HeatExchangerPeripheral(controllerSupplier));
//    }
//
//    public HeatExchangerPeripheral(Supplier<HeatExchangerMultiblockController> rawControllerSupplier) {
//        this.rawControllerSupplier = rawControllerSupplier;
//        this.controllerSupplier = this::getController;
//        lockSupplier = () -> controllerSupplier.get().locks.readLock();
//        condenser = new Channel(() -> controllerSupplier.get().condenserHeatBody, () -> controllerSupplier.get().condenserTank, () -> controllerSupplier.get().condenserChannels.size(), lockSupplier);
//        evaporator = new Channel(() -> controllerSupplier.get().evaporatorHeatBody, () -> controllerSupplier.get().evaporatorTank, () -> controllerSupplier.get().evaporatorChannels.size(), lockSupplier);
//        internalEnvironment = new InternalEnvironment(() -> controllerSupplier.get().airHeatBody);
//    }
//
//    @LuaFunction
//    public String apiVersion() {
//        return BiggerReactors.modVersion();
//    }
//
//    @LuaFunction
//    public boolean connected() {
//        HeatExchangerMultiblockController controller = rawControllerSupplier.get();
//        if (controller == null) {
//            return false;
//        }
//        return controller.assemblyState() == MultiblockController.AssemblyState.ASSEMBLED;
//    }
//
//    @Nonnull
//    private HeatExchangerMultiblockController getController() throws LuaException {
//        HeatExchangerMultiblockController controller = rawControllerSupplier.get();
//        if (controller == null || controller.assemblyState() != MultiblockController.AssemblyState.ASSEMBLED) {
//            throw new LuaException("Invalid multiblock controller");
//        }
//        return controller;
//    }
//
//
//    public static class Channel {
//        final LamdbaExceptionUtils.Supplier_WithExceptions<HeatBody, LuaException> heatBodySupplier;
//        final LamdbaExceptionUtils.Supplier_WithExceptions<FluidTransitionTank, LuaException> transitionTankSupplier;
//        final LamdbaExceptionUtils.Supplier_WithExceptions<Integer, LuaException> countSupplier;
//        final LamdbaExceptionUtils.Supplier_WithExceptions<Lock, LuaException> lockSupplier;
//
//        final ChannelFluid inputFluid;
//        final ChannelFluid outputFluid;
//
//        Channel(LamdbaExceptionUtils.Supplier_WithExceptions<HeatBody, LuaException> heatBodySupplier, LamdbaExceptionUtils.Supplier_WithExceptions<FluidTransitionTank, LuaException> transitionTankSupplier,
//                LamdbaExceptionUtils.Supplier_WithExceptions<Integer, LuaException> countSupplier, LamdbaExceptionUtils.Supplier_WithExceptions<Lock, LuaException> lockSupplier) {
//            this.heatBodySupplier = heatBodySupplier;
//            this.transitionTankSupplier = transitionTankSupplier;
//            this.countSupplier = countSupplier;
//            this.lockSupplier = lockSupplier;
//            inputFluid = new ChannelFluid(transitionTankSupplier, 0);
//            outputFluid = new ChannelFluid(transitionTankSupplier, 1);
//        }
//
//        public static class ChannelFluid {
//            final LamdbaExceptionUtils.Supplier_WithExceptions<FluidTransitionTank, LuaException> transitionTankSupplier;
//            final int tankNum;
//
//            public ChannelFluid(LamdbaExceptionUtils.Supplier_WithExceptions<FluidTransitionTank, LuaException> transitionTankSupplier, int tankNum) {
//                this.transitionTankSupplier = transitionTankSupplier;
//                this.tankNum = tankNum;
//            }
//
//            @LuaFunction
//            public String name() throws LuaException {
//                return transitionTankSupplier.get().fluidTypeInTank(tankNum).getRegistryName().toString();
//            }
//
//            @LuaFunction
//            public long amount() throws LuaException {
//                return transitionTankSupplier.get().fluidAmountInTank(tankNum);
//            }
//
//            @LuaFunction
//            public long maxAmount() throws LuaException {
//                return transitionTankSupplier.get().perSideCapacity;
//            }
//        }
//
//        @LuaFunction
//        public double temperature() throws LuaException {
//            return heatBodySupplier.get().temperature();
//        }
//
//        @LuaFunction
//        public double rfPerKelvin() throws LuaException {
//            return heatBodySupplier.get().rfPerKelvin();
//        }
//
//        @LuaFunction
//        public ChannelFluid input() {
//            return inputFluid;
//        }
//
//        @LuaFunction
//        public ChannelFluid output() {
//            return outputFluid;
//        }
//
//        @LuaFunction
//        public long transitionedLastTick() throws LuaException {
//            return transitionTankSupplier.get().transitionedLastTick();
//        }
//
//        @LuaFunction
//        public long maxTransitionedLastTick() throws LuaException {
//            return transitionTankSupplier.get().maxTransitionedLastTick();
//        }
//
//        @LuaFunction
//        public double transitionEnergy() throws LuaException {
//            return transitionTankSupplier.get().activeTransition().latentHeat;
//        }
//    }
//
//    @LuaFunction
//    public Channel condenser() {
//        return condenser;
//    }
//
//    @LuaFunction
//    public Channel evaporator() {
//        return evaporator;
//    }
//
//    public static class InternalEnvironment {
//        final LamdbaExceptionUtils.Supplier_WithExceptions<HeatBody, LuaException> heatBodySupplier;
//
//        public InternalEnvironment(LamdbaExceptionUtils.Supplier_WithExceptions<HeatBody, LuaException> heatBodySupplier) {
//            this.heatBodySupplier = heatBodySupplier;
//        }
//
//        @LuaFunction
//        public double temperature() throws LuaException {
//            return heatBodySupplier.get().temperature();
//        }
//
//        @LuaFunction
//        public double rfPerKelvin() throws LuaException {
//            return heatBodySupplier.get().rfPerKelvin();
//        }
//
//    }
//
//    @LuaFunction
//    public InternalEnvironment internalEnvironment() {
//        return internalEnvironment;
//    }
//
//    @LuaFunction
//    public double ambientTemperature() throws LuaException {
//        return controllerSupplier.get().ambientHeatBody.temperature();
//    }
//
//    @LuaFunction
//    public double ambientInternalRFKT() throws LuaException {
//        return controllerSupplier.get().airAmbientRFKT;
//    }
//
//    @LuaFunction
//    public double condenserInternalRFKT() throws LuaException {
//        return controllerSupplier.get().condenserAirRFKT;
//    }
//
//    @LuaFunction
//    public double evaporatorInternalRFKT() throws LuaException {
//        return controllerSupplier.get().evaporatorAirRFKT;
//    }
//
//    @LuaFunction
//    public double condenserEvaporatorRFKT() throws LuaException {
//        return controllerSupplier.get().channelRFKT;
//    }
//
//    @Nonnull
//    @Override
//    public String getType() {
//        return "BiggerReactors_Heat-Exchanger";
//    }
//
//    @Override
//    public boolean equals(@Nullable IPeripheral other) {
//        if(other == this){
//            return true;
//        }
//        if (other instanceof HeatExchangerPeripheral) {
//            if (rawControllerSupplier.get() == null) {
//                return false;
//            }
//            return ((HeatExchangerPeripheral) other).rawControllerSupplier.get() == rawControllerSupplier.get();
//        }
//        return false;
//    }
//}
