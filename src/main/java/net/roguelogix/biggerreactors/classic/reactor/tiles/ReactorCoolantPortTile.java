package net.roguelogix.biggerreactors.classic.reactor.tiles;

import mekanism.api.chemical.gas.IGasHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.fml.network.NetworkHooks;
import net.roguelogix.biggerreactors.classic.reactor.blocks.ReactorAccessPort;
import net.roguelogix.biggerreactors.classic.reactor.blocks.ReactorCoolantPort;
import net.roguelogix.biggerreactors.classic.reactor.containers.ReactorCoolantPortContainer;
import net.roguelogix.biggerreactors.classic.reactor.deps.ReactorGasHandler;
import net.roguelogix.biggerreactors.classic.reactor.simulation.IReactorCoolantTank;
import net.roguelogix.biggerreactors.classic.reactor.state.ReactorCoolantPortState;
import net.roguelogix.phosphophyllite.fluids.IPhosphophylliteFluidHandler;
import net.roguelogix.phosphophyllite.fluids.PhosphophylliteFluidStack;
import net.roguelogix.phosphophyllite.gui.client.api.IHasUpdatableState;
import net.roguelogix.phosphophyllite.multiblock.generic.*;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;
import net.roguelogix.phosphophyllite.util.BlockStates;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static net.roguelogix.biggerreactors.classic.reactor.blocks.ReactorAccessPort.PortDirection.*;

@RegisterTileEntity(name = "reactor_coolant_port")
public class ReactorCoolantPortTile extends ReactorBaseTile implements IPhosphophylliteFluidHandler, INamedContainerProvider, IHasUpdatableState<ReactorCoolantPortState>, IAssemblyAttemptedTile, IOnAssemblyTile, IOnDisassemblyTile {
    
    @RegisterTileEntity.Type
    public static TileEntityType<?> TYPE;
    
    public ReactorCoolantPortTile() {
        super(TYPE);
    }
    
    @CapabilityInject(IGasHandler.class)
    public static Capability<IGasHandler> GAS_HANDLER_CAPABILITY = null;
    
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return LazyOptional.of(() -> this).cast();
        }
        if (cap == GAS_HANDLER_CAPABILITY) {
            return ReactorGasHandler.create(() -> transitionTank).cast();
        }
        return super.getCapability(cap, side);
    }
    
    private static final PhosphophylliteFluidStack fluidStack = new PhosphophylliteFluidStack();
    
    private IReactorCoolantTank transitionTank;
    
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
    
    @Nonnull
    @Override
    public Fluid fluidTypeInTank(int tank) {
        if (transitionTank == null) {
            return Fluids.EMPTY;
        }
        return transitionTank.fluidTypeInTank(tank);
    }
    
    @Override
    public long fluidAmountInTank(int tank) {
        if (transitionTank == null) {
            return 0;
        }
        return transitionTank.fluidAmountInTank(tank);
    }
    
    @Override
    public boolean fluidValidForTank(int tank, @Nonnull Fluid fluid) {
        if (transitionTank == null) {
            return false;
        }
        return transitionTank.fluidValidForTank(tank, fluid);
    }
    
    @Override
    public long fill(@Nonnull Fluid fluid, long amount, boolean simulate) {
        if (transitionTank == null || direction != INLET) {
            return 0;
        }
        return transitionTank.fill(fluid, amount, simulate);
    }
    
    @Override
    public long drain(@Nonnull Fluid fluid, long amount, boolean simulate) {
        if (transitionTank == null || direction == INLET) {
            return 0;
        }
        return transitionTank.drain(fluid, amount, simulate);
    }
    
    
    public long pushFluid() {
        if (!connected || direction == INLET) {
            return 0;
        }
        if (vaporOutput.isPresent()) {
            IFluidHandler handler = vaporOutput.orElse(EMPTY_TANK);
            fluidStack.setFluid(transitionTank.vaporType());
            fluidStack.setAmount(transitionTank.drain(fluidStack.getRawFluid(), transitionTank.vaporAmount(), true));
            int filled = handler.fill(fluidStack, FluidAction.EXECUTE);
            return transitionTank.drain(fluidStack.getRawFluid(), filled, false);
        } else if (steamGasOutput != null && steamGasOutput.isPresent()) {
            if (!transitionTank.vaporType().getTags().contains(new ResourceLocation("forge:steam"))) {
                return 0;
            }
            IGasHandler output = steamGasOutput.orElse(ReactorGasHandler.EMPTY_TANK);
            return ReactorGasHandler.pushSteamToHandler(output, transitionTank.vaporAmount());
        }
        return 0;
    }
    
    private boolean connected = false;
    Direction steamOutputDirection = null;
    LazyOptional<IFluidHandler> vaporOutput = null;
    LazyOptional<IGasHandler> steamGasOutput = null;
    FluidTank EMPTY_TANK = new FluidTank(0);
    private ReactorAccessPort.PortDirection direction = INLET;
    public final ReactorCoolantPortState reactorCoolantPortState = new ReactorCoolantPortState(this);
    
    @SuppressWarnings("DuplicatedCode")
    public void neighborChanged() {
        vaporOutput = LazyOptional.empty();
        if (steamOutputDirection == null) {
            connected = false;
            return;
        }
        assert world != null;
        TileEntity te = world.getTileEntity(pos.offset(steamOutputDirection));
        if (te == null) {
            connected = false;
            return;
        }
        connected = false;
        vaporOutput = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, steamOutputDirection.getOpposite());
        if (vaporOutput.isPresent()) {
            // just gonna assume its fine
            connected = true;
        }
        if (GAS_HANDLER_CAPABILITY != null) {
            steamGasOutput = te.getCapability(GAS_HANDLER_CAPABILITY, steamOutputDirection.getOpposite());
            if (steamGasOutput.isPresent()) {
                IGasHandler handler = steamGasOutput.orElse(ReactorGasHandler.EMPTY_TANK);
                if (ReactorGasHandler.isValidHandler(handler)) {
                    connected = true;
                }
            }
        }
        
        connected = connected && ((vaporOutput != null && vaporOutput.isPresent()) || (steamGasOutput != null && steamGasOutput.isPresent()));
    }
    
    public void setDirection(ReactorAccessPort.PortDirection direction) {
        this.direction = direction;
        this.markDirty();
    }
    
    @Override
    protected void readNBT(@Nonnull CompoundNBT compound) {
        if (compound.contains("direction")) {
            direction = ReactorAccessPort.PortDirection.valueOf(compound.getString("direction"));
        }
    }
    
    @Override
    @Nonnull
    protected CompoundNBT writeNBT() {
        CompoundNBT NBT = new CompoundNBT();
        NBT.putString("direction", String.valueOf(direction));
        return NBT;
    }
    
    @Override
    public void onAssemblyAttempted() {
        assert world != null;
        world.setBlockState(pos, world.getBlockState(pos).with(PORT_DIRECTION_ENUM_PROPERTY, direction));
    }
    
    @Override
    @Nonnull
    public ActionResultType onBlockActivated(@Nonnull PlayerEntity player, @Nonnull Hand handIn) {
        assert world != null;
        if (world.getBlockState(pos).get(MultiblockBlock.ASSEMBLED)) {
            if (!world.isRemote) {
                NetworkHooks.openGui((ServerPlayerEntity) player, this, this.getPos());
            }
            return ActionResultType.SUCCESS;
        }
        return super.onBlockActivated(player, handIn);
    }
    
    @Override
    public void runRequest(String requestName, Object requestData) {
        // Change IO direction.
        if (requestName.equals("setDirection")) {
            this.setDirection(((Integer) requestData != 0) ? OUTLET : INLET);
            world.setBlockState(this.pos, this.getBlockState().with(PORT_DIRECTION_ENUM_PROPERTY, direction));
        }
        
        super.runRequest(requestName, requestData);
    }
    
    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent(ReactorCoolantPort.INSTANCE.getTranslationKey());
    }
    
    @Nullable
    @Override
    public Container createMenu(int windowId, @Nonnull PlayerInventory playerInventory, @Nonnull PlayerEntity player) {
        return new ReactorCoolantPortContainer(windowId, this.pos, player);
    }
    
    @Override
    @Nonnull
    public ReactorCoolantPortState getState() {
        this.updateState();
        return this.reactorCoolantPortState;
    }
    
    @Override
    public void updateState() {
        reactorCoolantPortState.direction = (this.direction == INLET);
    }
    
    @Override
    public void onAssembly() {
        this.transitionTank = controller.simulation().coolantTank();
        steamOutputDirection = getBlockState().get(BlockStates.FACING);
        neighborChanged();
    }
    
    @Override
    public void onDisassembly() {
        steamOutputDirection = null;
        neighborChanged();
    }
}
