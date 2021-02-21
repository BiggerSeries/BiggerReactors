package net.roguelogix.biggerreactors.classic.reactor.deps;

import mekanism.api.Action;
import mekanism.api.chemical.ChemicalTankBuilder;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.LazyOptional;
import net.roguelogix.biggerreactors.fluids.FluidIrradiatedSteam;
import net.roguelogix.phosphophyllite.fluids.IPhosphophylliteFluidHandler;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public class ReactorGasHandler implements IGasHandler {
    
    private final Supplier<IPhosphophylliteFluidHandler> fluidHandlerSupplier;
    public static final Gas steam;
    public static GasStack steamStack;
    public static IGasHandler EMPTY_TANK;
    
    static {
        steam = Gas.getFromRegistry(new ResourceLocation("mekanism:steam"));
        steamStack = new GasStack(steam, 0);
        EMPTY_TANK = (IGasHandler) ChemicalTankBuilder.GAS.createDummy(0);
    }
    
    public static long pushSteamToHandler(IGasHandler handler, long amount) {
        steamStack.setAmount(amount);
        return amount - handler.insertChemical(steamStack, Action.EXECUTE).getAmount();
    }
    
    public static boolean isValidHandler(IGasHandler handler) {
        for (int i = 0; i < handler.getTanks(); i++) {
            if (handler.isValid(i, steamStack)) {
                return true;
            }
        }
        return false;
    }
    
    public static LazyOptional<Object> create(@Nonnull Supplier<IPhosphophylliteFluidHandler> fluidHandlerSupplier) {
        return LazyOptional.of(() -> new ReactorGasHandler(fluidHandlerSupplier));
    }
    
    public ReactorGasHandler(@Nonnull Supplier<IPhosphophylliteFluidHandler> fluidHandlerSupplier) {
        this.fluidHandlerSupplier = fluidHandlerSupplier;
    }
    
    @Override
    public int getTanks() {
        return 1;
    }
    
    @Override
    @Nonnull
    public GasStack getChemicalInTank(int tank) {
        if (tank == 0 && fluidHandlerSupplier.get() != null) {
            if (!fluidHandlerSupplier.get().fluidTypeInTank(1).getTags().contains(new ResourceLocation("forge:steam"))) {
                return GasStack.EMPTY;
            }
            steamStack.setAmount(fluidHandlerSupplier.get().fluidAmountInTank(1));
            return steamStack;
        }
        return GasStack.EMPTY;
    }
    
    @Override
    public void setChemicalInTank(int tank, @Nonnull GasStack stack) {
    }
    
    @Override
    public long getTankCapacity(int tank) {
        if (tank == 0 && fluidHandlerSupplier.get() != null) {
            IPhosphophylliteFluidHandler handler = fluidHandlerSupplier.get();
            return handler.getTankCapacity(0);
        }
        return 0;
    }
    
    @Override
    public boolean isValid(int tank, @Nonnull GasStack stack) {
        if (tank == 0) {
            return stack.getRaw() == steam;
        }
        return false;
    }
    
    @Override
    @Nonnull
    public GasStack insertChemical(int tank, @Nonnull GasStack stack, @Nonnull Action action) {
        return stack;
    }
    
    @Override
    @Nonnull
    public GasStack extractChemical(int tank, long amount, @Nonnull Action action) {
        if (tank == 1 && fluidHandlerSupplier.get() != null) {
            boolean simulate = action.simulate();
            long extracted = fluidHandlerSupplier.get().drain(FluidIrradiatedSteam.INSTANCE, amount, simulate);
            steamStack.setAmount(extracted);
            return steamStack.copy();
        }
        return GasStack.EMPTY;
    }
}
