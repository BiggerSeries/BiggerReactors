package net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles;

import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.roguelogix.phosphophyllite.fluids.IPhosphophylliteFluidHandler;
import net.roguelogix.phosphophyllite.fluids.PhosphophylliteFluidStack;
import net.roguelogix.phosphophyllite.multiblock.generic.IAssemblyAttemptedTile;
import net.roguelogix.phosphophyllite.multiblock.generic.IOnAssemblyTile;
import net.roguelogix.phosphophyllite.multiblock.generic.IOnDisassemblyTile;
import net.roguelogix.phosphophyllite.multiblock.generic.MultiblockController;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;
import net.roguelogix.phosphophyllite.registry.TileSupplier;
import net.roguelogix.phosphophyllite.util.BlockStates;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static net.roguelogix.biggerreactors.multiblocks.heatexchanger.blocks.HeatExchangerCoolantPortBlock.CONDENSER;
import static net.roguelogix.phosphophyllite.util.BlockStates.PORT_DIRECTION;

@RegisterTileEntity(name = "heat_exchanger_coolant_port")
public class HeatExchangerCoolantPortTile extends HeatExchangerBaseTile implements IPhosphophylliteFluidHandler, IOnAssemblyTile, IOnDisassemblyTile {
    
    @RegisterTileEntity.Type
    public static TileEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final TileSupplier SUPPLIER = HeatExchangerCoolantPortTile::new;
    
    public HeatExchangerCoolantPortTile() {
        super(TYPE);
    }
    
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return fluidHandlerCapability().cast();
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
    
    
    private static final PhosphophylliteFluidStack fluidStack = new PhosphophylliteFluidStack();
    
    public long pushFluid() {
        if (fluidOutputCapability.isPresent() && !inlet) {
            IFluidHandler handler = fluidOutputCapability.orElse(EMPTY_TANK);
            fluidStack.setFluid(HETank.fluidTypeInTank(1));
            fluidStack.setAmount(HETank.drain(fluidStack.getRawFluid(), null, HETank.fluidAmountInTank(1), true));
            int filled = handler.fill(fluidStack, FluidAction.EXECUTE);
            return HETank.drain(fluidStack.getRawFluid(), null, filled, false);
        }
        return 0;
    }
    
    private boolean connected = false;
    Direction outputDirection = null;
    LazyOptional<IFluidHandler> fluidOutputCapability = LazyOptional.empty();
    FluidTank EMPTY_TANK = new FluidTank(0);
    
    @SuppressWarnings("DuplicatedCode")
    public void neighborChanged() {
        fluidOutputCapability = LazyOptional.empty();
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
        fluidOutputCapability = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, outputDirection.getOpposite());
        if (fluidOutputCapability.isPresent()) {
            // just gonna assume its fine
            connected = true;
//            IFluidHandler handler = steamOutput.orElse(EMPTY_TANK);
//            for (int i = 0; i < handler.getTanks(); i++) {
//                if (handler.isFluidValid(i, steam)) {
//                    connected = true;
//                    break;
//                }
//            }
        }
//        if (GAS_HANDLER_CAPABILITY != null) {
//            steamGasOutput = te.getCapability(GAS_HANDLER_CAPABILITY, steamOutputDirection.getOpposite());
//            if (steamGasOutput.isPresent()) {
//                IGasHandler handler = steamGasOutput.orElse(ReactorGasHandler.EMPTY_TANK);
//                if (ReactorGasHandler.isValidHandler(handler)) {
//                    connected = true;
//                }
//            }
//        }
        
        connected = connected && ((fluidOutputCapability != null && fluidOutputCapability.isPresent()) /*|| (steamGasOutput != null && steamGasOutput.isPresent())*/);
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
        neighborChanged();
    }
}
