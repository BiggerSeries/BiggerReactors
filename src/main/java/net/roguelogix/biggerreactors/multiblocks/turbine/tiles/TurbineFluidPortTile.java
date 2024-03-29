package net.roguelogix.biggerreactors.multiblocks.turbine.tiles;

import mekanism.api.chemical.gas.IGasHandler;
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
import net.roguelogix.biggerreactors.multiblocks.turbine.blocks.TurbineFluidPort;
import net.roguelogix.biggerreactors.multiblocks.turbine.containers.TurbineFluidPortContainer;
import net.roguelogix.biggerreactors.multiblocks.turbine.simulation.ITurbineFluidTank;
import net.roguelogix.biggerreactors.multiblocks.turbine.state.TurbineFluidPortState;
import net.roguelogix.phosphophyllite.fluids.FluidHandlerWrapper;
import net.roguelogix.phosphophyllite.fluids.IPhosphophylliteFluidHandler;
import net.roguelogix.phosphophyllite.client.gui.api.IHasUpdatableState;
import net.roguelogix.phosphophyllite.fluids.MekanismGasWrappers;
import net.roguelogix.phosphophyllite.multiblock.common.IEventMultiblock;
import net.roguelogix.phosphophyllite.multiblock.validated.IValidatedMultiblock;
import net.roguelogix.phosphophyllite.registry.CapabilityRegistration;
import net.roguelogix.phosphophyllite.registry.RegisterCapability;
import net.roguelogix.phosphophyllite.registry.RegisterTile;
import net.roguelogix.phosphophyllite.util.BlockStates;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static net.roguelogix.biggerreactors.multiblocks.turbine.blocks.TurbineFluidPort.PortDirection.*;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TurbineFluidPortTile extends TurbineBaseTile implements IPhosphophylliteFluidHandler, MenuProvider, IHasUpdatableState<TurbineFluidPortState>, IEventMultiblock.AssemblyStateTransition.OnAssembly, IEventMultiblock.AssemblyStateTransition.OnDisassembly {
    
    @RegisterTile("turbine_fluid_port")
    public static final BlockEntityType.BlockEntitySupplier<TurbineFluidPortTile> SUPPLIER = new RegisterTile.Producer<>(TurbineFluidPortTile::new);
    
    @RegisterCapability
    private static final CapabilityRegistration FLUID_HANDLER_CAP_REGISTRATION = CapabilityRegistration.tileCap(Capabilities.FluidHandler.BLOCK, TurbineFluidPortTile.class);
    
    public TurbineFluidPortTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    // TODO: mek gas
//    private static final Capability<IGasHandler> GAS_HANDLER_CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});
//
//    @Override
//    public <T> LazyOptional<T> capability(Capability<T> cap, @Nullable Direction side) {
//        if (cap == ForgeCapabilities.FLUID_HANDLER) {
//            return LazyOptional.of(() -> this).cast();
//        }
//        if (cap == GAS_HANDLER_CAPABILITY) {
//            return LazyOptional.of(() -> MekanismGasWrappers.wrap(this)).cast();
//        }
//        return super.capability(cap, side);
//    }
    
    private ITurbineFluidTank transitionTank;
    
    @Override
    public int tankCount() {
        if (transitionTank == null) {
            return 0;
        }
        return transitionTank.tankCount();
    }
    
    @Override
    public long tankCapacity(int tank) {
        if (transitionTank == null) {
            return 0;
        }
        return transitionTank.tankCapacity(tank);
    }
    
    @Override
    public Fluid fluidTypeInTank(int tank) {
        if (transitionTank == null) {
            return Fluids.EMPTY;
        }
        return transitionTank.fluidTypeInTank(tank);
    }
    
    @Nullable
    @Override
    public CompoundTag fluidTagInTank(int tank) {
        if (transitionTank == null) {
            return null;
        }
        return transitionTank.fluidTagInTank(tank);
    }
    
    @Override
    public long fluidAmountInTank(int tank) {
        if (transitionTank == null) {
            return 0;
        }
        return transitionTank.fluidAmountInTank(tank);
    }
    
    @Override
    public boolean fluidValidForTank(int tank, Fluid fluid) {
        if (transitionTank == null) {
            return false;
        }
        return transitionTank.fluidValidForTank(tank, fluid);
    }
    
    @Override
    public long fill(Fluid fluid, @Nullable CompoundTag tag, long amount, boolean simulate) {
        if (transitionTank == null || direction != INLET) {
            return 0;
        }
        return transitionTank.fill(fluid, null, amount, simulate);
    }
    
    @Override
    public long drain(Fluid fluid, @Nullable CompoundTag tag, long amount, boolean simulate) {
        if (transitionTank == null || direction == INLET) {
            return 0;
        }
        return transitionTank.drain(fluid, null, amount, simulate);
    }
    
    public long pushFluid() {
        if (!connected || direction == INLET) {
            return 0;
        }
        if (handler != null) {
            Fluid fluid = transitionTank.liquidType();
            long amount = transitionTank.liquidAmount();
            amount = transitionTank.drain(fluid, null, amount, true);
            amount = handler.fill(fluid, null, amount, false);
            amount = transitionTank.drain(fluid, null, amount, false);
            return amount;
        } else {
            handler = null;
            connected = false;
        }
        return 0;
    }
    
    private boolean connected = false;
    Direction waterOutputDirection = null;
    
    IPhosphophylliteFluidHandler handler = null;
    private TurbineFluidPort.PortDirection direction = INLET;
    public final TurbineFluidPortState fluidPortState = new TurbineFluidPortState(this);
    
    @SuppressWarnings("DuplicatedCode")
    public void neighborChanged() {
        handler = null;
        if (waterOutputDirection == null) {
            connected = false;
            return;
        }
        assert level != null;
        final var outputCap = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(waterOutputDirection), waterOutputDirection.getOpposite());
        if (outputCap == null) {
            connected = false;
            return;
        }
        connected = false;
        if (outputCap != null) {
            connected = true;
            handler = FluidHandlerWrapper.wrap(outputCap);
            // todo: mek gas
//        } else if (GAS_HANDLER_CAPABILITY != null) {
//            LazyOptional<IGasHandler> gasOptional = te.getCapability(GAS_HANDLER_CAPABILITY, waterOutputDirection.getOpposite());
//            if (gasOptional.isPresent()) {
//                IGasHandler gasHandler = gasOptional.orElse(MekanismGasWrappers.EMPTY_TANK);
//                connected = true;
//                handlerOptional = gasOptional;
//                handler = MekanismGasWrappers.wrap(gasHandler);
//            }
        }
    }
    
    public void setDirection(TurbineFluidPort.PortDirection direction) {
        this.direction = direction;
        this.setChanged();
    }
    
    @Override
    protected void readNBT(CompoundTag compound) {
        if (compound.contains("direction")) {
            direction = TurbineFluidPort.PortDirection.valueOf(compound.getString("direction"));
        }
    }
    
    @Override
    protected CompoundTag writeNBT() {
        CompoundTag NBT = new CompoundTag();
        NBT.putString("direction", String.valueOf(direction));
        return NBT;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void runRequest(String requestName, Object requestData) {
        // Change IO direction.
        if (requestName.equals("setDirection")) {
            this.setDirection(((Integer) requestData != 0) ? OUTLET : INLET);
            level.setBlockAndUpdate(this.worldPosition, this.getBlockState().setValue(PORT_DIRECTION_ENUM_PROPERTY, direction));
        }
        super.runRequest(requestName, requestData);
    }
    
    @Override
    public Component getDisplayName() {
        return Component.translatable(TurbineFluidPort.INSTANCE.getDescriptionId());
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new TurbineFluidPortContainer(windowId, this.worldPosition, player);
    }
    
    @Nullable
    @Override
    public TurbineFluidPortState getState() {
        this.updateState();
        return this.fluidPortState;
    }
    
    @Override
    public void updateState() {
        fluidPortState.direction = (this.direction == INLET);
    }
    
    
    @Override
    public void onAssemblyStateTransition(IValidatedMultiblock.AssemblyState oldState, IValidatedMultiblock.AssemblyState newState) {
        OnAssembly.super.onAssemblyStateTransition(oldState, newState);
        OnDisassembly.super.onAssemblyStateTransition(oldState, newState);
    }
    
    @Override
    public void onAssembly() {
        this.transitionTank = controller().simulation().fluidTank();
        waterOutputDirection = getBlockState().getValue(BlockStates.FACING);
        level.setBlockAndUpdate(worldPosition, level.getBlockState(worldPosition).setValue(PORT_DIRECTION_ENUM_PROPERTY, direction));
        neighborChanged();
    }
    
    @Override
    public void onDisassembly() {
        waterOutputDirection = null;
        transitionTank = null;
        neighborChanged();
    }
}
