package net.roguelogix.biggerreactors.classic.reactor.deps;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraftforge.common.util.LazyOptional;
import net.roguelogix.biggerreactors.classic.reactor.ReactorMultiblockController;
import net.roguelogix.biggerreactors.classic.reactor.state.ReactorActivity;
import net.roguelogix.phosphophyllite.multiblock.generic.MultiblockController;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.function.Supplier;

public class ReactorPeripheral implements IPeripheral {
    
    public static LazyOptional<ReactorPeripheral> create(@Nonnull Supplier<ReactorMultiblockController> controllerSupplier) {
        return LazyOptional.of(() -> new ReactorPeripheral(controllerSupplier));
    }
    
    private final Supplier<ReactorMultiblockController> controllerSupplier;
    
    private ReactorPeripheral(Supplier<ReactorMultiblockController> controllerSupplier) {
        this.controllerSupplier = controllerSupplier;
        battery = new Battery(controllerSupplier);
        coolantTank = new CoolantTank(controllerSupplier);
        fuelTank = new FuelTank(controllerSupplier);
    }
    
    @LuaFunction
    public boolean connected() {
        ReactorMultiblockController controller = controllerSupplier.get();
        if (controller == null) {
            return false;
        }
        return controller.assemblyState() != MultiblockController.AssemblyState.DISASSEMBLED;
    }
    
    @LuaFunction
    public boolean active() {
        ReactorMultiblockController controller = controllerSupplier.get();
        if (controller == null) {
            return false;
        }
        return controller.isActive();
    }
    
    @LuaFunction
    public void setActive(boolean active) {
        ReactorMultiblockController controller = controllerSupplier.get();
        if (controller == null) {
            return;
        }
        controller.setActive(active ? ReactorActivity.ACTIVE : ReactorActivity.INACTIVE);
    }
    
    private static class Battery {
        
        private final Supplier<ReactorMultiblockController> controllerSupplier;
        
        public Battery(Supplier<ReactorMultiblockController> controllerSupplier) {
            this.controllerSupplier = controllerSupplier;
        }
        
        @LuaFunction
        public long stored() {
            ReactorMultiblockController controller = controllerSupplier.get();
            if (controller == null) {
                return 0;
            }
            return controller.simulation().battery().stored();
        }
        
        @LuaFunction
        public long capacity() {
            ReactorMultiblockController controller = controllerSupplier.get();
            if (controller == null) {
                return 0;
            }
            return controller.simulation().battery().capacity();
        }
        
        @LuaFunction
        public long producedLastTick() {
            ReactorMultiblockController controller = controllerSupplier.get();
            if (controller == null) {
                return 0;
            }
            return controller.simulation().FEProducedLastTick();
        }
    }
    
    private final Battery battery;
    
    @LuaFunction
    public Battery battery() {
        ReactorMultiblockController controller = controllerSupplier.get();
        if (controller == null) {
            return null;
        }
        if (!controller.simulation().isPassive()) {
            return null;
        }
        return battery;
    }
    
    public static class CoolantTank {
        
        private final Supplier<ReactorMultiblockController> controllerSupplier;
        
        public CoolantTank(Supplier<ReactorMultiblockController> controllerSupplier) {
            this.controllerSupplier = controllerSupplier;
        }
        
        @LuaFunction
        public long coldFluidAmount() {
            ReactorMultiblockController controller = controllerSupplier.get();
            if (controller == null) {
                return 0;
            }
            return controller.simulation().coolantTank().liquidAmount();
        }
        
        @LuaFunction
        public long hotFluidAmount() {
            ReactorMultiblockController controller = controllerSupplier.get();
            if (controller == null) {
                return 0;
            }
            return controller.simulation().coolantTank().vaporAmount();
        }
        
        @LuaFunction
        public long capacity() {
            ReactorMultiblockController controller = controllerSupplier.get();
            if (controller == null) {
                return 0;
            }
            return controller.simulation().coolantTank().perSideCapacity();
        }

//        @LuaFunction
//        public double transitionPoint(){
//            ReactorMultiblockController controller = controllerSupplier.get();
//            if (controller == null) {
//                return 0;
//            }
//            return controller.simulation().coolantTank().
//        }

//        @LuaFunction
//        public double transitionEnergy(){
//            ReactorMultiblockController controller = controllerSupplier.get();
//            if (controller == null) {
//                return 0;
//            }
//            return controller.simulation().coolantTank().
//        }
        
        @LuaFunction
        public long transitionedLastTick() {
            ReactorMultiblockController controller = controllerSupplier.get();
            if (controller == null) {
                return 0;
            }
            return controller.simulation().MBProducedLastTick();
        }
        
        @LuaFunction
        public long maxTransitionedLastTick() {
            ReactorMultiblockController controller = controllerSupplier.get();
            if (controller == null) {
                return 0;
            }
            return controller.simulation().maxMBProductionLastTick();
        }
    }
    
    
    private final CoolantTank coolantTank;
    
    @LuaFunction
    public CoolantTank coolantTank() {
        ReactorMultiblockController controller = controllerSupplier.get();
        if (controller == null) {
            return null;
        }
        if (controller.simulation().isPassive()) {
            return null;
        }
        return coolantTank;
    }
    
    public static class FuelTank {
        private final Supplier<ReactorMultiblockController> controllerSupplier;
        
        public FuelTank(Supplier<ReactorMultiblockController> controllerSupplier) {
            this.controllerSupplier = controllerSupplier;
        }
        
        
        @LuaFunction
        public long capacity() {
            ReactorMultiblockController controller = controllerSupplier.get();
            if (controller == null) {
                return 0;
            }
            return controller.simulation().fuelTank().capacity();
        }
        
        @LuaFunction
        public long totalReactant() {
            ReactorMultiblockController controller = controllerSupplier.get();
            if (controller == null) {
                return 0;
            }
            return controller.simulation().fuelTank().totalStored();
        }
        
        @LuaFunction
        public long fuel() {
            ReactorMultiblockController controller = controllerSupplier.get();
            if (controller == null) {
                return 0;
            }
            return controller.simulation().fuelTank().fuel();
        }
        
        @LuaFunction
        public long waste() {
            ReactorMultiblockController controller = controllerSupplier.get();
            if (controller == null) {
                return 0;
            }
            return controller.simulation().fuelTank().waste();
        }

//        @LuaFunction
//        public void ejectFuel() {
//            ReactorMultiblockController controller = controllerSupplier.get();
//            if (controller == null) {
//                return;
//            }
//            controller.ejectFuel();
//        }
        
        @LuaFunction
        public void ejectWaste() {
            ReactorMultiblockController controller = controllerSupplier.get();
            if (controller == null) {
                return;
            }
            controller.ejectWaste();
        }
    
        @LuaFunction
        public double fuelReactivity() {
            return controllerSupplier.get().simulation().fertility();
        }
    
        @LuaFunction
        public double burnedLastTick() {
            return controllerSupplier.get().simulation().fuelConsumptionLastTick();
        }
    }
    
    private final FuelTank fuelTank;
    
    @LuaFunction
    public FuelTank fuelTank() {
        ReactorMultiblockController controller = controllerSupplier.get();
        if (controller == null) {
            return null;
        }
        return fuelTank;
    }
    
    public static class ControlRod {
        private final Supplier<ReactorMultiblockController> controllerSupplier;
        private final int index;
        private boolean isValid = true;
        
        public ControlRod(Supplier<ReactorMultiblockController> controllerSupplier, int index) {
            this.controllerSupplier = controllerSupplier;
            this.index = index;
        }
        
        @LuaFunction
        public int index() {
            return index;
        }
        
        @LuaFunction
        public double level() {
            return controllerSupplier.get().controlRodLevel(index);
        }
        
        @LuaFunction
        public void setLevel(double newLevel) {
            controllerSupplier.get().setControlRodLevel(index, newLevel);
        }
        
        @LuaFunction
        public String name() {
            return controllerSupplier.get().controlRodName(index);
        }
    
        @LuaFunction
        public void setName(String newName){
            controllerSupplier.get().setControlRodName(index, newName);
        }
        
        void invalidate() {
            isValid = false;
        }
    }
    
    ArrayList<ControlRod> controlRods = new ArrayList<>();
    
    @LuaFunction
    int controlRodCount() {
        return controlRods.size();
    }
    
    @LuaFunction
    public ControlRod getControlRod(int index) {
        return controlRods.get(index);
    }
    
    @LuaFunction
    public void setAllControlRodLevels(double newLevel){
        controllerSupplier.get().setAllControlRodLevels(newLevel);
    }
    
    public void rebuildControlRodList() {
        controlRods.forEach(ControlRod::invalidate);
        controlRods.clear();
        for (int i = 0; i < controllerSupplier.get().controlRodCount(); i++) {
            controlRods.add(new ControlRod(controllerSupplier, i));
        }
    }
    
    @LuaFunction
    public double fuelTemperature() {
        return controllerSupplier.get().simulation().fuelHeat();
    }
    
    @LuaFunction
    public double casingTemperature() {
        return controllerSupplier.get().simulation().caseHeat();
    }
    
    @LuaFunction
    public double ambientTemperature() {
        return controllerSupplier.get().simulation().ambientTemperature();
    }
    
    @Nonnull
    @Override
    public String getType() {
        return "BiggerReactors_Reactor_";
    }
    
    @Override
    public boolean equals(@Nullable IPeripheral other) {
        if (other instanceof ReactorPeripheral) {
            if (controllerSupplier.get() == null) {
                return false;
            }
            return ((ReactorPeripheral) other).controllerSupplier.get() == controllerSupplier.get();
        }
        return false;
    }
}