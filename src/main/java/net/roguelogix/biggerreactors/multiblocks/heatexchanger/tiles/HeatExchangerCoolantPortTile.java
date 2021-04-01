package net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles;

import mekanism.api.chemical.gas.IGasHandler;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.roguelogix.phosphophyllite.fluids.FluidHandlerWrapper;
import net.roguelogix.phosphophyllite.fluids.IPhosphophylliteFluidHandler;
import net.roguelogix.phosphophyllite.fluids.MekanismGasWrappers;
import net.roguelogix.phosphophyllite.multiblock.generic.IOnAssemblyTile;
import net.roguelogix.phosphophyllite.multiblock.generic.IOnDisassemblyTile;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;
import net.roguelogix.phosphophyllite.registry.TileSupplier;
import net.roguelogix.phosphophyllite.util.BlockStates;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static net.roguelogix.biggerreactors.classic.turbine.blocks.TurbineCoolantPort.PortDirection.INLET;
import static net.roguelogix.biggerreactors.multiblocks.heatexchanger.blocks.HeatExchangerCoolantPortBlock.CONDENSER;
import static net.roguelogix.phosphophyllite.util.BlockStates.PORT_DIRECTION;

@RegisterTileEntity(name = "heat_exchanger_coolant_port")
public class HeatExchangerCoolantPortTile extends HeatExchangerBaseTile implements IPhosphophylliteFluidHandler, IOnAssemblyTile, IOnDisassemblyTile {
    
    public long lastCheckedTick;
    
    @RegisterTileEntity.Type
    public static TileEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final TileSupplier SUPPLIER = HeatExchangerCoolantPortTile::new;
    
    public HeatExchangerCoolantPortTile() {
        super(TYPE);
    }
    
    @CapabilityInject(IGasHandler.class)
    public static Capability<IGasHandler> GAS_HANDLER_CAPABILITY = null;
    
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return fluidHandlerCapability().cast();
        }
        if (cap == GAS_HANDLER_CAPABILITY) {
            return LazyOptional.of(() -> MekanismGasWrappers.wrap(this)).cast();
        }
        return super.getCapability(cap, side);
    }
    
    private IPhosphophylliteFluidHandler HETank;
    
    public void setHETank(IPhosphophylliteFluidHandler HETank) {
        this.HETank = HETank;
    }
    
    private boolean inlet = true;
    private boolean condenser = true;
    
    public void setInlet(boolean inlet) {
        this.inlet = inlet;
        world.setBlockState(this.getPos(), this.getBlockState().with(PORT_DIRECTION, inlet));
        markDirty();
    }
    
    public void setInletOtherOutlet(boolean inlet) {
        controller.setInletPort(this, inlet);
    }
    
    public boolean isInlet() {
        return inlet;
    }
    
    public void setCondenser(boolean condenser) {
        this.condenser = condenser;
        world.setBlockState(this.getPos(), this.getBlockState().with(CONDENSER, condenser));
    }
    
    public boolean isCondenser() {
        return condenser;
    }
    
    @Override
    public int tankCount() {
        if (HETank == null) {
            return 0;
        }
        return HETank.tankCount();
    }
    
    @Override
    public long tankCapacity(int tank) {
        if (HETank == null) {
            return 0;
        }
        return HETank.tankCapacity(tank);
    }
    
    @Nonnull
    @Override
    public Fluid fluidTypeInTank(int tank) {
        if (HETank == null) {
            return Fluids.EMPTY;
        }
        return HETank.fluidTypeInTank(tank);
    }
    
    @Nullable
    @Override
    public CompoundNBT fluidTagInTank(int tank) {
        if (HETank == null) {
            return null;
        }
        return HETank.fluidTagInTank(tank);
    }
    
    @Override
    public long fluidAmountInTank(int tank) {
        if (HETank == null) {
            return 0;
        }
        return HETank.fluidAmountInTank(tank);
    }
    
    @Override
    public boolean fluidValidForTank(int tank, @Nonnull Fluid fluid) {
        if (HETank == null) {
            return false;
        }
        return HETank.fluidValidForTank(tank, fluid);
    }
    
    @Override
    public long fill(@Nonnull Fluid fluid, @Nullable CompoundNBT tag, long amount, boolean simulate) {
        if (HETank == null || !inlet) {
            return 0;
        }
        return HETank.fill(fluid, null, amount, simulate);
    }
    
    @Override
    public long drain(@Nonnull Fluid fluid, @Nullable CompoundNBT tag, long amount, boolean simulate) {
        if (HETank == null || inlet) {
            return 0;
        }
        return HETank.drain(fluid, null, amount, simulate);
    }
    
    
    public long pushFluid() {
        if (!connected || inlet) {
            return 0;
        }
        if (handlerOptional.isPresent()) {
            Fluid fluid = HETank.fluidTypeInTank(1);
            long amount = HETank.fluidAmountInTank(1);
            amount = HETank.drain(fluid, null, amount, true);
            amount = handler.fill(fluid, null, amount, false);
            amount = HETank.drain(fluid, null, amount, false);
            return amount;
        } else {
            handlerOptional = LazyOptional.empty();
            handler = null;
            connected = false;
        }
        return 0;
    }
    
    private boolean connected = false;
    Direction outputDirection = null;
    LazyOptional<?> handlerOptional = LazyOptional.empty();
    IPhosphophylliteFluidHandler handler = null;
    FluidTank EMPTY_TANK = new FluidTank(0);
    
    @SuppressWarnings("DuplicatedCode")
    public void neighborChanged() {
        handlerOptional = LazyOptional.empty();
        handler = null;
        if (outputDirection == null) {
            connected = false;
            return;
        }
        assert world != null;
        TileEntity te = world.getTileEntity(pos.offset(outputDirection));
        if (te == null) {
            connected = false;
            return;
        }
        connected = false;
        LazyOptional<IFluidHandler> waterOutput = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, outputDirection.getOpposite());
        if (waterOutput.isPresent()) {
            connected = true;
            handlerOptional = waterOutput;
            handler = FluidHandlerWrapper.wrap(waterOutput.orElse(EMPTY_TANK));
        } else if (GAS_HANDLER_CAPABILITY != null) {
            LazyOptional<IGasHandler> gasOptional = te.getCapability(GAS_HANDLER_CAPABILITY, outputDirection.getOpposite());
            if (gasOptional.isPresent()) {
                IGasHandler gasHandler = gasOptional.orElse(MekanismGasWrappers.EMPTY_TANK);
                connected = true;
                handlerOptional = gasOptional;
                handler = MekanismGasWrappers.wrap(gasHandler);
            }
        }
    }
    
    @Override
    protected void readNBT(@Nonnull CompoundNBT compound) {
        super.readNBT(compound);
        inlet = compound.getBoolean("inlet");
    }
    
    @Nonnull
    @Override
    protected CompoundNBT writeNBT() {
        CompoundNBT nbt = super.writeNBT();
        nbt.putBoolean("inlet", inlet);
        return nbt;
    }
    
    @Override
    public void onAssembly() {
        outputDirection = getBlockState().get(BlockStates.FACING);
    }
    
    @Override
    public void onDisassembly() {
        outputDirection = null;
        HETank = null;
        neighborChanged();
    }
}
