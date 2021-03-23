package net.roguelogix.biggerreactors.classic.turbine.deps;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraftforge.common.util.LazyOptional;
import net.roguelogix.biggerreactors.classic.turbine.TurbineMultiblockController;
import net.roguelogix.biggerreactors.classic.turbine.state.VentState;
import net.roguelogix.phosphophyllite.multiblock.generic.MultiblockController;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Supplier;

public class TurbinePeripheral implements IPeripheral {
    
    @Nonnull
    private final Supplier<TurbineMultiblockController> controllerSupplier;
    
    public static LazyOptional<Object> create(@Nonnull Supplier<TurbineMultiblockController> controllerSupplier) {
        return LazyOptional.of(() -> new TurbinePeripheral(controllerSupplier));
    }
    
    public TurbinePeripheral(@Nonnull Supplier<TurbineMultiblockController> controllerSupplier) {
        this.controllerSupplier = controllerSupplier;
        battery = new Battery(controllerSupplier);
        rotor = new Rotor(controllerSupplier);
        tank = new FluidTank(controllerSupplier);
    }
    
    @LuaFunction
    public boolean getConnected() {
        if (controllerSupplier.get() == null) {
            return false;
        }
        return controllerSupplier.get().assemblyState() == MultiblockController.AssemblyState.ASSEMBLED;
    }
    
    
    @LuaFunction
    public boolean active() {
        return controllerSupplier.get().simulation().active();
    }
    
    @LuaFunction
    public void setActive(boolean active) {
        controllerSupplier.get().setActive(active);
    }
    
    
    private static class Battery {
        
        private final Supplier<TurbineMultiblockController> controllerSupplier;
        
        public Battery(Supplier<TurbineMultiblockController> controllerSupplier) {
            this.controllerSupplier = controllerSupplier;
        }
        
        @LuaFunction
        public long stored() {
            TurbineMultiblockController controller = controllerSupplier.get();
            if (controller == null) {
                return 0;
            }
            return controller.simulation().battery().stored();
        }
        
        @LuaFunction
        public long capacity() {
            TurbineMultiblockController controller = controllerSupplier.get();
            if (controller == null) {
                return 0;
            }
            return controller.simulation().battery().capacity();
        }
        
        @LuaFunction
        public long producedLastTick() {
            TurbineMultiblockController controller = controllerSupplier.get();
            if (controller == null) {
                return 0;
            }
            return controller.simulation().FEGeneratedLastTick();
        }
    }
    
    private final Battery battery;
    
    @LuaFunction
    public Battery battery() {
        return battery;
    }
    
    
    public static class Rotor {
        private final Supplier<TurbineMultiblockController> controllerSupplier;
        
        public Rotor(Supplier<TurbineMultiblockController> controllerSupplier) {
            this.controllerSupplier = controllerSupplier;
        }
        
        @LuaFunction
        public double RPM() {
            return controllerSupplier.get().simulation().RPM();
        }
        
        @LuaFunction
        public double efficiencyLastTick() {
            return controllerSupplier.get().simulation().bladeEfficiencyLastTick();
        }
    }
    
    private final Rotor rotor;
    
    @LuaFunction
    public Rotor rotor() {
        return rotor;
    }
    
    public static class FluidTank {
        private final Supplier<TurbineMultiblockController> controllerSupplier;
        
        final TankFluid input;
        final TankFluid output;
        
        public FluidTank(Supplier<TurbineMultiblockController> controllerSupplier) {
            this.controllerSupplier = controllerSupplier;
            input = new TankFluid(controllerSupplier, 0);
            output = new TankFluid(controllerSupplier, 1);
        }
        
        public static class TankFluid {
            final Supplier<TurbineMultiblockController> controllerSupplier;
            final int tankNum;
            
            public TankFluid(Supplier<TurbineMultiblockController> controllerSupplier, int tankNum) {
                this.controllerSupplier = controllerSupplier;
                this.tankNum = tankNum;
            }
            
            @LuaFunction
            public String name() {
                return controllerSupplier.get().simulation().fluidTank().fluidTypeInTank(tankNum).getRegistryName().toString();
            }
            
            @LuaFunction
            public long amount() {
                return controllerSupplier.get().simulation().fluidTank().fluidAmountInTank(tankNum);
            }
            
            @LuaFunction
            public long maxAmount() {
                return controllerSupplier.get().simulation().fluidTank().perSideCapacity();
            }
        }
        
        @LuaFunction
        public TankFluid input() {
            return input;
        }
        
        @LuaFunction
        public TankFluid output() {
            return output;
        }
        
        @LuaFunction
        public long flowLastTick() {
            return controllerSupplier.get().simulation().flowLastTick();
        }
        
        @LuaFunction
        public long nominalFlowRate() {
            return controllerSupplier.get().simulation().nominalFlowRate();
        }
        
        @LuaFunction
        public void setNominalFlowRate(long rate) {
            controllerSupplier.get().simulation().setNominalFlowRate(rate);
        }
        
        
        @LuaFunction
        public long flowRateLimit() {
            return controllerSupplier.get().simulation().flowRateLimit();
        }
    }
    
    private final FluidTank tank;
    
    @LuaFunction
    public FluidTank fluidTank() {
        return tank;
    }
    
    public static class Vent {
        private final Supplier<TurbineMultiblockController> controllerSupplier;
        
        public Vent(Supplier<TurbineMultiblockController> controllerSupplier) {
            this.controllerSupplier = controllerSupplier;
        }
        
        @LuaFunction
        boolean closed(){
            return controllerSupplier.get().simulation().ventState() == VentState.CLOSED;
        }
    
        @LuaFunction
        boolean overflow(){
            return controllerSupplier.get().simulation().ventState() == VentState.OVERFLOW;
        }
    
        @LuaFunction
        boolean all(){
            return controllerSupplier.get().simulation().ventState() == VentState.ALL;
        }
    
        @LuaFunction
        void setClosed(){
            controllerSupplier.get().simulation().setVentState(VentState.CLOSED);
        }
    
        @LuaFunction
        void setOverflow(){
            controllerSupplier.get().simulation().setVentState(VentState.OVERFLOW);
        }
    
        @LuaFunction
        void setAll(){
            controllerSupplier.get().simulation().setVentState(VentState.ALL);
        }
    }
    
    
    @LuaFunction
    boolean coilEngaged(){
        return controllerSupplier.get().simulation().coilEngaged();
    }
    
    @LuaFunction
    void setCoilEngaged(boolean engaged){
        controllerSupplier.get().simulation().setCoilEngaged(engaged);
    }
    
    @Nonnull
    @Override
    public String getType() {
        return "BiggerReactors_Turbine_";
    }
    
    @Override
    public boolean equals(@Nullable IPeripheral other) {
        if (other instanceof TurbinePeripheral) {
            if (controllerSupplier.get() == null) {
                return false;
            }
            return ((TurbinePeripheral) other).controllerSupplier.get() == controllerSupplier.get();
        }
        return false;
    }
}
