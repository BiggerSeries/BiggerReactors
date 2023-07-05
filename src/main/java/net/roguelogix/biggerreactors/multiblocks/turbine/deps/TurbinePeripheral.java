package net.roguelogix.biggerreactors.multiblocks.turbine.deps;

import cpw.mods.modlauncher.api.LamdbaExceptionUtils;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.ForgeRegistries;
import net.roguelogix.biggerreactors.BiggerReactors;
import net.roguelogix.biggerreactors.multiblocks.turbine.TurbineMultiblockController;
import net.roguelogix.biggerreactors.multiblocks.turbine.state.VentState;
import net.roguelogix.phosphophyllite.multiblock.validated.IValidatedMultiblock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Supplier;

public class TurbinePeripheral implements IPeripheral {

    @Nonnull
    private final Supplier<TurbineMultiblockController> rawControllerSupplier;
    @Nonnull
    private final LamdbaExceptionUtils.Supplier_WithExceptions<TurbineMultiblockController, LuaException> controllerSupplier;

    public static LazyOptional<Object> create(@Nonnull Supplier<TurbineMultiblockController> controllerSupplier) {
        return LazyOptional.of(() -> new TurbinePeripheral(controllerSupplier));
    }

    public TurbinePeripheral(@Nonnull Supplier<TurbineMultiblockController> rawControllerSupplier) {
        this.rawControllerSupplier = rawControllerSupplier;
        this.controllerSupplier = this::getController;
        battery = new Battery(controllerSupplier);
        rotor = new Rotor(controllerSupplier);
        tank = new FluidTank(controllerSupplier);
    }

    @LuaFunction
    public String apiVersion() {
        return BiggerReactors.modVersion();
    }

    @LuaFunction
    public boolean connected() {
        if (rawControllerSupplier.get() == null) {
            return false;
        }
        return rawControllerSupplier.get().assemblyState() == IValidatedMultiblock.AssemblyState.ASSEMBLED;
    }

    @Nonnull
    private TurbineMultiblockController getController() throws LuaException {
        TurbineMultiblockController controller = rawControllerSupplier.get();
        if (controller == null || controller.assemblyState() != IValidatedMultiblock.AssemblyState.ASSEMBLED) {
            throw new LuaException("Invalid multiblock controller");
        }
        return controller;
    }

    @LuaFunction
    public boolean active() throws LuaException {
        return controllerSupplier.get().simulation().active();
    }

    @LuaFunction
    public void setActive(boolean active) throws LuaException {
        controllerSupplier.get().setActive(active);
    }


    public static class Battery {

        private final LamdbaExceptionUtils.Supplier_WithExceptions<TurbineMultiblockController, LuaException> controllerSupplier;

        public Battery(LamdbaExceptionUtils.Supplier_WithExceptions<TurbineMultiblockController, LuaException> controllerSupplier) {
            this.controllerSupplier = controllerSupplier;
        }

        @LuaFunction
        public long stored() throws LuaException {
            return controllerSupplier.get().simulation().battery().stored();
        }

        @LuaFunction
        public long capacity() throws LuaException {
            return controllerSupplier.get().simulation().battery().capacity();
        }

        @LuaFunction
        public long producedLastTick() throws LuaException {
            return controllerSupplier.get().simulation().FEGeneratedLastTick();
        }
    }

    private final Battery battery;

    @LuaFunction
    public Battery battery() {
        return battery;
    }


    public static class Rotor {
        private final LamdbaExceptionUtils.Supplier_WithExceptions<TurbineMultiblockController, LuaException> controllerSupplier;

        public Rotor(LamdbaExceptionUtils.Supplier_WithExceptions<TurbineMultiblockController, LuaException> controllerSupplier) {
            this.controllerSupplier = controllerSupplier;
        }

        @LuaFunction
        public double RPM() throws LuaException {
            return controllerSupplier.get().simulation().RPM();
        }

        @LuaFunction
        public double efficiencyLastTick() throws LuaException {
            return controllerSupplier.get().simulation().bladeEfficiencyLastTick();
        }
    }

    private final Rotor rotor;

    @LuaFunction
    public Rotor rotor() {
        return rotor;
    }

    public static class FluidTank {
        private final LamdbaExceptionUtils.Supplier_WithExceptions<TurbineMultiblockController, LuaException> controllerSupplier;

        final TankFluid input;
        final TankFluid output;

        public FluidTank(LamdbaExceptionUtils.Supplier_WithExceptions<TurbineMultiblockController, LuaException> controllerSupplier) {
            this.controllerSupplier = controllerSupplier;
            input = new TankFluid(controllerSupplier, 0);
            output = new TankFluid(controllerSupplier, 1);
        }

        public static class TankFluid {
            final LamdbaExceptionUtils.Supplier_WithExceptions<TurbineMultiblockController, LuaException> controllerSupplier;
            final int tankNum;

            public TankFluid(LamdbaExceptionUtils.Supplier_WithExceptions<TurbineMultiblockController, LuaException> controllerSupplier, int tankNum) {
                this.controllerSupplier = controllerSupplier;
                this.tankNum = tankNum;
            }

            @LuaFunction
            public String name() throws LuaException {
                return ForgeRegistries.FLUIDS.getKey(controllerSupplier.get().simulation().fluidTank().fluidTypeInTank(tankNum)).toString();
            }

            @LuaFunction
            public long amount() throws LuaException {
                return controllerSupplier.get().simulation().fluidTank().fluidAmountInTank(tankNum);
            }

            @LuaFunction
            public long maxAmount() throws LuaException {
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
        public long flowLastTick() throws LuaException {
            return controllerSupplier.get().simulation().flowLastTick();
        }

        @LuaFunction
        public long nominalFlowRate() throws LuaException {
            return controllerSupplier.get().simulation().nominalFlowRate();
        }

        @LuaFunction
        public void setNominalFlowRate(long rate) throws LuaException {
            controllerSupplier.get().simulation().setNominalFlowRate(rate);
        }


        @LuaFunction
        public long flowRateLimit() throws LuaException {
            return controllerSupplier.get().simulation().flowRateLimit();
        }
    }

    private final FluidTank tank;

    @LuaFunction
    public FluidTank fluidTank() {
        return tank;
    }

    public static class Vent {
        private final LamdbaExceptionUtils.Supplier_WithExceptions<TurbineMultiblockController, LuaException> controllerSupplier;

        public Vent(LamdbaExceptionUtils.Supplier_WithExceptions<TurbineMultiblockController, LuaException> controllerSupplier) {
            this.controllerSupplier = controllerSupplier;
        }

        @LuaFunction
        public boolean closed() throws LuaException {
            return controllerSupplier.get().simulation().ventState() == VentState.CLOSED;
        }

        @LuaFunction
        public boolean overflow() throws LuaException {
            return controllerSupplier.get().simulation().ventState() == VentState.OVERFLOW;
        }

        @LuaFunction
        public boolean all() throws LuaException {
            return controllerSupplier.get().simulation().ventState() == VentState.ALL;
        }

        @LuaFunction
        public void setClosed() throws LuaException {
            controllerSupplier.get().simulation().setVentState(VentState.CLOSED);
        }

        @LuaFunction
        public void setOverflow() throws LuaException {
            controllerSupplier.get().simulation().setVentState(VentState.OVERFLOW);
        }

        @LuaFunction
        public void setAll() throws LuaException {
            controllerSupplier.get().simulation().setVentState(VentState.ALL);
        }
    }


    @LuaFunction
    public boolean coilEngaged() throws LuaException {
        return controllerSupplier.get().simulation().coilEngaged();
    }

    @LuaFunction
    public void setCoilEngaged(boolean engaged) throws LuaException {
        controllerSupplier.get().simulation().setCoilEngaged(engaged);
    }

    @Nonnull
    @Override
    public String getType() {
        return "BiggerReactors_Turbine";
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        if(other == this){
            return true;
        }
        if (other instanceof TurbinePeripheral) {
            if (rawControllerSupplier.get() == null) {
                return false;
            }
            return ((TurbinePeripheral) other).rawControllerSupplier.get() == rawControllerSupplier.get();
        }
        return false;
    }
}
