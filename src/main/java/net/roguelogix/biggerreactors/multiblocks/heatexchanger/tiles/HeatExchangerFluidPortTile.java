package net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.blocks.HeatExchangerFluidPortBlock;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.containers.HeatExchangerFluidPortContainer;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.state.HeatExchangerFluidPortState;
import net.roguelogix.phosphophyllite.client.gui.api.IHasUpdatableState;
import net.roguelogix.phosphophyllite.fluids.FluidHandlerWrapper;
import net.roguelogix.phosphophyllite.fluids.IPhosphophylliteFluidHandler;
import net.roguelogix.phosphophyllite.multiblock.common.IEventMultiblock;
import net.roguelogix.phosphophyllite.multiblock.validated.IValidatedMultiblock;
import net.roguelogix.phosphophyllite.registry.CapabilityRegistration;
import net.roguelogix.phosphophyllite.registry.RegisterCapability;
import net.roguelogix.phosphophyllite.registry.RegisterTile;
import net.roguelogix.phosphophyllite.util.BlockStates;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static net.roguelogix.biggerreactors.multiblocks.heatexchanger.blocks.HeatExchangerFluidPortBlock.CONDENSER;
import static net.roguelogix.phosphophyllite.util.BlockStates.PORT_DIRECTION;


@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class HeatExchangerFluidPortTile extends HeatExchangerBaseTile implements IPhosphophylliteFluidHandler, IEventMultiblock.AssemblyStateTransition.OnAssembly, IEventMultiblock.AssemblyStateTransition.OnDisassembly, MenuProvider, IHasUpdatableState<HeatExchangerFluidPortState> {
    
    public long lastCheckedTick;
    
    @RegisterTile("heat_exchanger_fluid_port")
    public static final BlockEntityType.BlockEntitySupplier<HeatExchangerFluidPortTile> SUPPLIER = new RegisterTile.Producer<>(HeatExchangerFluidPortTile::new);
    
    @RegisterCapability
    private static final CapabilityRegistration FLUID_HANDLER_CAP_REGISTRATION = CapabilityRegistration.tileCap(Capabilities.FluidHandler.BLOCK, HeatExchangerFluidPortTile.class);
    
    public HeatExchangerFluidPortTile(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
    }
    
    
    // TODO: mek gas
//    private static final Capability<IGasHandler> GAS_HANDLER_CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});
//
//    @Override
//    public <T> LazyOptional<T> capability(Capability<T> cap, @Nullable Direction side) {
//        if (cap == ForgeCapabilities.FLUID_HANDLER) {
//            return fluidHandlerCapability().cast();
//        }
//        if (cap == GAS_HANDLER_CAPABILITY) {
//            return LazyOptional.of(() -> MekanismGasWrappers.wrap(this)).cast();
//        }
//        return super.capability(cap, side);
//    }
    
    private IPhosphophylliteFluidHandler HETank;
    
    public void setHETank(IPhosphophylliteFluidHandler HETank) {
        this.HETank = HETank;
    }
    
    private boolean inlet = true;
    private boolean condenser = true;
    
    public void setInlet(boolean inlet) {
        assert level != null;
        this.inlet = inlet;
        level.setBlock(this.getBlockPos(), this.getBlockState().setValue(PORT_DIRECTION, inlet), 3);
        setChanged();
    }
    
    public void setInletOtherOutlet(boolean inlet) {
        controller().setInletPort(this, inlet);
    }
    
    public boolean isInlet() {
        return inlet;
    }
    
    public void setCondenser(boolean condenser) {
        this.condenser = condenser;
        assert level != null;
        level.setBlock(this.getBlockPos(), this.getBlockState().setValue(CONDENSER, condenser), 3);
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
    
    
    @Override
    public Fluid fluidTypeInTank(int tank) {
        if (HETank == null) {
            return Fluids.EMPTY;
        }
        return HETank.fluidTypeInTank(tank);
    }
    
    @Nullable
    @Override
    public CompoundTag fluidTagInTank(int tank) {
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
    public boolean fluidValidForTank(int tank, Fluid fluid) {
        if (HETank == null) {
            return false;
        }
        return HETank.fluidValidForTank(tank, fluid);
    }
    
    @Override
    public long fill(Fluid fluid, @Nullable CompoundTag tag, long amount, boolean simulate) {
        if (HETank == null || !inlet) {
            return 0;
        }
        return HETank.fill(fluid, null, amount, simulate);
    }
    
    @Override
    public long drain(Fluid fluid, @Nullable CompoundTag tag, long amount, boolean simulate) {
        if (HETank == null || inlet) {
            return 0;
        }
        return HETank.drain(fluid, null, amount, simulate);
    }
    
    
    public long pushFluid() {
        if (!connected || inlet) {
            return 0;
        }
        if (handler != null) {
            Fluid fluid = HETank.fluidTypeInTank(1);
            long amount = HETank.fluidAmountInTank(1);
            amount = HETank.drain(fluid, null, amount, true);
            amount = handler.fill(fluid, null, amount, false);
            amount = HETank.drain(fluid, null, amount, false);
            return amount;
        } else {
            handler = null;
            connected = false;
        }
        return 0;
    }
    
    private boolean connected = false;
    Direction outputDirection = null;
    IPhosphophylliteFluidHandler handler = null;
    FluidTank EMPTY_TANK = new FluidTank(0);
    
    @SuppressWarnings("DuplicatedCode")
    public void neighborChanged() {
        handler = null;
        if (outputDirection == null) {
            connected = false;
            return;
        }
        assert level != null;
        final var outputCap = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(outputDirection), outputDirection.getOpposite());
        connected = false;
        if (outputCap != null) {
            connected = true;
            handler = FluidHandlerWrapper.wrap(outputCap);
//        } else if (GAS_HANDLER_CAPABILITY != null) {
//            LazyOptional<IGasHandler> gasOptional = te.getCapability(GAS_HANDLER_CAPABILITY, outputDirection.getOpposite());
//            if (gasOptional.isPresent()) {
//                IGasHandler gasHandler = gasOptional.orElse(MekanismGasWrappers.EMPTY_TANK);
//                connected = true;
//                handlerOptional = gasOptional;
//                handler = MekanismGasWrappers.wrap(gasHandler);
//            }
        }
    }
    
    @Override
    protected void readNBT(CompoundTag compound) {
        super.readNBT(compound);
        inlet = compound.getBoolean("inlet");
    }
    
    
    @Override
    protected CompoundTag writeNBT() {
        CompoundTag nbt = super.writeNBT();
        nbt.putBoolean("inlet", inlet);
        return nbt;
    }
    
    @Override
    public void onAssemblyStateTransition(IValidatedMultiblock.AssemblyState oldState, IValidatedMultiblock.AssemblyState newState) {
        OnAssembly.super.onAssemblyStateTransition(oldState, newState);
        OnDisassembly.super.onAssemblyStateTransition(oldState, newState);
    }
    
    @Override
    public void onAssembly() {
        outputDirection = getBlockState().getValue(BlockStates.FACING);
        neighborChanged();
    }
    
    @Override
    public void onDisassembly() {
        outputDirection = null;
        HETank = null;
        neighborChanged();
    }
    
    private final HeatExchangerFluidPortState state = new HeatExchangerFluidPortState(this);
    
    @Override
    public HeatExchangerFluidPortState getState() {
        return state;
    }
    
    @Override
    public void updateState() {
        state.direction = isInlet();
        state.condenser = isCondenser();
    }
    
    @Override
    public Component getDisplayName() {
        return Component.translatable(HeatExchangerFluidPortBlock.INSTANCE.getDescriptionId());
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new HeatExchangerFluidPortContainer(windowId, this.worldPosition, player);
    }

    @Override
    public void runRequest(String requestName, Object requestData) {
        if (requestName.equals("setDirection")) {
            int direction = (Integer) requestData;
            setInletOtherOutlet(direction == 0);
        }

        super.runRequest(requestName, requestData);
    }
}
