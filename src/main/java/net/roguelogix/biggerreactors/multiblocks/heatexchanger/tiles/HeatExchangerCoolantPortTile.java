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
import net.roguelogix.phosphophyllite.multiblock.generic.MultiblockController;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static net.roguelogix.biggerreactors.multiblocks.heatexchanger.blocks.HeatExchangerCoolantPortBlock.CONDENSER;
import static net.roguelogix.phosphophyllite.util.BlockStates.PORT_DIRECTION;

@RegisterTileEntity(name = "heat_exchanger_coolant_port")
public class HeatExchangerCoolantPortTile extends HeatExchangerBaseTile implements IPhosphophylliteFluidHandler, IAssemblyAttemptedTile {

    @RegisterTileEntity.Type
    public static TileEntityType<?> TYPE;

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
    
    public void setInletOtherOutlet(boolean inlet){
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
    public long fill(@Nonnull Fluid fluid, long amount, boolean simulate) {
        if (HETank == null || !inlet) {
            return 0;
        }
        return HETank.fill(fluid, amount, simulate);
    }

    @Override
    public long drain(@Nonnull Fluid fluid, long amount, boolean simulate) {
        if (HETank == null || inlet) {
            return 0;
        }
        return HETank.drain(fluid, amount, simulate);
    }
    
    
    private static PhosphophylliteFluidStack fluidStack = new PhosphophylliteFluidStack();
    
    public long pushFluid(){
        if(fluidOutputCapability.isPresent() && !inlet){
            IFluidHandler handler = fluidOutputCapability.orElse(EMPTY_TANK);
            fluidStack.setFluid(HETank.fluidTypeInTank(1));
            fluidStack.setAmount(HETank.drain(fluidStack.getRawFluid(), HETank.fluidAmountInTank(1), true));
            int filled = handler.fill(fluidStack, FluidAction.EXECUTE);
            return HETank.drain(fluidStack.getRawFluid(), filled, false);
        }
        return 0;
    }
    
    private boolean connected = false;
    Direction outputDirection = null;
    LazyOptional<IFluidHandler> fluidOutputCapability = LazyOptional.empty();
    FluidTank EMPTY_TANK = new FluidTank(0);
    
    @SuppressWarnings("DuplicatedCode")
    public void updateOutputDirection() {
        if (controller.assemblyState() == MultiblockController.AssemblyState.DISASSEMBLED) {
            outputDirection = null;
        } else if (pos.getX() == controller.minCoord().x()) {
            outputDirection = Direction.WEST;
        } else if (pos.getX() == controller.maxCoord().x()) {
            outputDirection = Direction.EAST;
        } else if (pos.getY() == controller.minCoord().y()) {
            outputDirection = Direction.DOWN;
        } else if (pos.getY() == controller.maxCoord().y()) {
            outputDirection = Direction.UP;
        } else if (pos.getZ() == controller.minCoord().z()) {
            outputDirection = Direction.NORTH;
        } else if (pos.getZ() == controller.maxCoord().z()) {
            outputDirection = Direction.SOUTH;
        }
        neighborChanged();
    }
    
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
    public void onAssemblyAttempted() {
        updateOutputDirection();
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
}
