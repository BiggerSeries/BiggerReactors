//package net.roguelogix.biggerreactors.multiblocks.reactor.deps;
//
//import cpw.mods.modlauncher.api.LamdbaExceptionUtils;
//import dan200.computercraft.api.lua.LuaException;
//import dan200.computercraft.api.lua.LuaFunction;
//import dan200.computercraft.api.peripheral.IPeripheral;
//import net.minecraftforge.common.util.LazyOptional;
//import net.roguelogix.biggerreactors.BiggerReactors;
//import net.roguelogix.biggerreactors.multiblocks.reactor.ReactorMultiblockController;
//import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.IReactorSimulation;
//import net.roguelogix.biggerreactors.multiblocks.reactor.state.ReactorActivity;
//import net.roguelogix.phosphophyllite.multiblock.MultiblockController;
//
//import javax.annotation.Nonnull;
//import javax.annotation.Nullable;
//import java.util.ArrayList;
//import java.util.function.Supplier;
//
//public class ReactorPeripheral implements IPeripheral {
//
//    public static LazyOptional<ReactorPeripheral> create(@Nonnull Supplier<ReactorMultiblockController> controllerSupplier) {
//        return LazyOptional.of(() -> new ReactorPeripheral(controllerSupplier));
//    }
//
//    @Nonnull
//    private final Supplier<ReactorMultiblockController> rawControllerSupplier;
//    @Nonnull
//    private final LamdbaExceptionUtils.Supplier_WithExceptions<ReactorMultiblockController, LuaException> controllerSupplier;
//    @Nonnull
//    private final LamdbaExceptionUtils.Supplier_WithExceptions<IReactorSimulation, LuaException> simulationSupplier;
//
//    private ReactorPeripheral(@Nonnull Supplier<ReactorMultiblockController> rawControllerSupplier) {
//        this.rawControllerSupplier = rawControllerSupplier;
//        this.controllerSupplier = this::getController;
//        this.simulationSupplier = this::getSimulation;
//        battery = new Battery(simulationSupplier);
//        coolantTank = new CoolantTank(simulationSupplier);
//        fuelTank = new FuelTank(controllerSupplier, simulationSupplier);
//    }
//
//    @LuaFunction
//    public String apiVersion() {
//        return BiggerReactors.modVersion();
//    }
//
//    @LuaFunction
//    public boolean connected() {
//        ReactorMultiblockController controller = rawControllerSupplier.get();
//        if (controller == null) {
//            return false;
//        }
//        if (controller.simulation() == null) {
//            return false;
//        }
//        return controller.assemblyState() == MultiblockController.AssemblyState.ASSEMBLED;
//    }
//
//    @Nonnull
//    private ReactorMultiblockController getController() throws LuaException {
//        ReactorMultiblockController controller = rawControllerSupplier.get();
//        if (controller == null || controller.assemblyState() != MultiblockController.AssemblyState.ASSEMBLED) {
//            throw new LuaException("Invalid multiblock controller");
//        }
//        return controller;
//    }
//
//    @Nonnull
//    private IReactorSimulation getSimulation() throws LuaException {
//        var sim = getController().simulation();
//        if (sim == null) {
//            // this shoudl only be possible when the reactor is disassembled, so invalid anyway
//            throw new LuaException("Invalid multiblock controller");
//        }
//        return sim;
//    }
//
//    @LuaFunction
//    public boolean active() throws LuaException {
//        return controllerSupplier.get().isActive();
//    }
//
//    @LuaFunction
//    public void setActive(boolean active) throws LuaException {
//        controllerSupplier.get().setActive(active ? ReactorActivity.ACTIVE : ReactorActivity.INACTIVE);
//    }
//
//    public static class Battery {
//
//        @Nonnull
//        private final LamdbaExceptionUtils.Supplier_WithExceptions<IReactorSimulation, LuaException> simulationSupplier;
//
//        public Battery(@Nonnull LamdbaExceptionUtils.Supplier_WithExceptions<IReactorSimulation, LuaException> simulationSupplier) {
//            this.simulationSupplier = simulationSupplier;
//        }
//
//        @LuaFunction
//        public long stored() throws LuaException {
//            return simulationSupplier.get().battery().stored();
//        }
//
//        @LuaFunction
//        public long capacity() throws LuaException {
//            return simulationSupplier.get().battery().capacity();
//        }
//
//        @LuaFunction
//        public long producedLastTick() throws LuaException {
//            return simulationSupplier.get().battery().generatedLastTick();
//        }
//    }
//
//    private final Battery battery;
//
//    @LuaFunction
//    public Battery battery() throws LuaException {
//        if (simulationSupplier.get().battery() == null) {
//            return null;
//        }
//        return battery;
//    }
//
//    public static class CoolantTank {
//
//        @Nonnull
//        private final LamdbaExceptionUtils.Supplier_WithExceptions<IReactorSimulation, LuaException> simulationSupplier;
//
//        public CoolantTank(@Nonnull LamdbaExceptionUtils.Supplier_WithExceptions<IReactorSimulation, LuaException> simulationSupplier) {
//            this.simulationSupplier = simulationSupplier;
//        }
//
//        @LuaFunction
//        public long coldFluidAmount() throws LuaException {
//            return simulationSupplier.get().coolantTank().liquidAmount();
//        }
//
//        @LuaFunction
//        public long hotFluidAmount() throws LuaException {
//            return simulationSupplier.get().coolantTank().vaporAmount();
//        }
//
//        @LuaFunction
//        public long capacity() throws LuaException {
//            return simulationSupplier.get().coolantTank().perSideCapacity();
//        }
//
////        @LuaFunction
////        public double transitionPoint(){
////            ReactorMultiblockController controller = controllerSupplier.get();
////            if (controller == null) {
////                return 0;
////            }
////            return controller.simulation().coolantTank().
////        }
//
////        @LuaFunction
////        public double transitionEnergy(){
////            ReactorMultiblockController controller = controllerSupplier.get();
////            if (controller == null) {
////                return 0;
////            }
////            return controller.simulation().coolantTank().
////        }
//
//        @LuaFunction
//        public long transitionedLastTick() throws LuaException {
//            return simulationSupplier.get().coolantTank().transitionedLastTick();
//        }
//
//        @LuaFunction
//        public long maxTransitionedLastTick() throws LuaException {
//            return simulationSupplier.get().coolantTank().maxTransitionedLastTick();
//        }
//    }
//
//    private final CoolantTank coolantTank;
//
//    @LuaFunction
//    public CoolantTank coolantTank() throws LuaException {
//        if (simulationSupplier.get().battery() != null) {
//            return null;
//        }
//        return coolantTank;
//    }
//
//    public static class FuelTank {
//
//        @Nonnull
//        private final LamdbaExceptionUtils.Supplier_WithExceptions<ReactorMultiblockController, LuaException> controllerSupplier;
//        @Nonnull
//        private final LamdbaExceptionUtils.Supplier_WithExceptions<IReactorSimulation, LuaException> simulationSupplier;
//
//        public FuelTank(@Nonnull LamdbaExceptionUtils.Supplier_WithExceptions<ReactorMultiblockController, LuaException> controllerSupplier, @Nonnull LamdbaExceptionUtils.Supplier_WithExceptions<IReactorSimulation, LuaException> simulationSupplier) {
//            this.controllerSupplier = controllerSupplier;
//            this.simulationSupplier = simulationSupplier;
//        }
//
//        @LuaFunction
//        public long capacity() throws LuaException {
//            return simulationSupplier.get().fuelTank().capacity();
//        }
//
//        @LuaFunction
//        public long totalReactant() throws LuaException {
//            return simulationSupplier.get().fuelTank().totalStored();
//        }
//
//        @LuaFunction
//        public long fuel() throws LuaException {
//            return simulationSupplier.get().fuelTank().fuel();
//        }
//
//        @LuaFunction
//        public long waste() throws LuaException {
//            return simulationSupplier.get().fuelTank().waste();
//        }
//
////        @LuaFunction
////        public void ejectFuel() throws LuaException {
////            ReactorMultiblockController controller = controllerSupplier.get();
////            if (controller == null) {
////                return;
////            }
////            controller.ejectFuel();
////        }
//
//        @LuaFunction
//        public void ejectWaste() throws LuaException {
//            controllerSupplier.get().ejectWaste();
//        }
//
//        @LuaFunction
//        public double fuelReactivity() throws LuaException {
//            return simulationSupplier.get().fertility();
//        }
//
//        @LuaFunction
//        public double burnedLastTick() throws LuaException {
//            return simulationSupplier.get().fuelTank().burnedLastTick();
//        }
//    }
//
//    private final FuelTank fuelTank;
//
//    @LuaFunction
//    public FuelTank fuelTank() throws LuaException {
//        return fuelTank;
//    }
//
//    public static class ControlRod {
//        private final LamdbaExceptionUtils.Supplier_WithExceptions<ReactorMultiblockController, LuaException> controllerSupplier;
//        private final int index;
//        private boolean isValid = true;
//
//        public ControlRod(LamdbaExceptionUtils.Supplier_WithExceptions<ReactorMultiblockController, LuaException> controllerSupplier, int index) {
//            this.controllerSupplier = controllerSupplier;
//            this.index = index;
//        }
//
//        @LuaFunction
//        public boolean valid() {
//            return isValid;
//        }
//
//        @LuaFunction
//        public int index() {
//            return index;
//        }
//
//        @LuaFunction
//        public double level() throws LuaException {
//            if (!isValid) {
//                throw new LuaException("Invalid control rod object");
//            }
//            return controllerSupplier.get().controlRodLevel(index);
//        }
//
//        @LuaFunction
//        public void setLevel(double newLevel) throws LuaException {
//            if (!isValid) {
//                throw new LuaException("Invalid control rod object");
//            }
//            controllerSupplier.get().setControlRodLevel(index, newLevel);
//        }
//
//        @LuaFunction
//        public String name() throws LuaException {
//            if (!isValid) {
//                throw new LuaException("Invalid control rod object");
//            }
//            return controllerSupplier.get().controlRodName(index);
//        }
//
//        @LuaFunction
//        public void setName(String newName) throws LuaException {
//            if (!isValid) {
//                throw new LuaException("Invalid control rod object");
//            }
//            controllerSupplier.get().setControlRodName(index, newName);
//        }
//
//        void invalidate() {
//            isValid = false;
//        }
//    }
//
//    ArrayList<ControlRod> controlRods = new ArrayList<>();
//
//    @LuaFunction
//    public int controlRodCount() {
//        return controlRods.size();
//    }
//
//    @LuaFunction
//    public ControlRod getControlRod(int index) {
//        return controlRods.get(index);
//    }
//
//    @LuaFunction
//    public void setAllControlRodLevels(double newLevel) throws LuaException {
//        controllerSupplier.get().setAllControlRodLevels(newLevel);
//    }
//
//    public void rebuildControlRodList() {
//        ReactorMultiblockController controller = rawControllerSupplier.get();
//        if (controller == null) {
//            return;
//        }
//        controlRods.forEach(ControlRod::invalidate);
//        controlRods.clear();
//        for (int i = 0; i < controller.controlRodCount(); i++) {
//            controlRods.add(new ControlRod(controllerSupplier, i));
//        }
//    }
//
//    @LuaFunction
//    public double fuelTemperature() throws LuaException {
//        return simulationSupplier.get().fuelHeat();
//    }
//
//    @LuaFunction
//    @Deprecated(forRemoval = true, since = "0.6.0")
//    public double casingTemperature() throws LuaException {
//        return stackTemperature();
//    }
//
//    @LuaFunction
//    public double stackTemperature() throws LuaException {
//        return simulationSupplier.get().stackHeat();
//    }
//
//    @LuaFunction
//    public double ambientTemperature() throws LuaException {
//        return simulationSupplier.get().ambientTemperature();
//    }
//
//    @Nonnull
//    @Override
//    public String getType() {
//        return "BiggerReactors_Reactor";
//    }
//
//    @Override
//    public boolean equals(@Nullable IPeripheral other) {
//        if (other == this) {
//            return true;
//        }
//        if (other instanceof ReactorPeripheral) {
//            if (rawControllerSupplier.get() == null) {
//                return false;
//            }
//            return ((ReactorPeripheral) other).rawControllerSupplier.get() == rawControllerSupplier.get();
//        }
//        return false;
//    }
//}