package net.roguelogix.biggerreactors.multiblocks.reactor.tiles;

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
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.fml.network.NetworkHooks;
import net.roguelogix.biggerreactors.multiblocks.reactor.blocks.ReactorAccessPort;
import net.roguelogix.biggerreactors.multiblocks.reactor.blocks.ReactorCoolantPort;
import net.roguelogix.biggerreactors.multiblocks.reactor.containers.ReactorCoolantPortContainer;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.IReactorCoolantTank;
import net.roguelogix.biggerreactors.multiblocks.reactor.state.ReactorCoolantPortState;
import net.roguelogix.phosphophyllite.fluids.FluidHandlerWrapper;
import net.roguelogix.phosphophyllite.fluids.IPhosphophylliteFluidHandler;
import net.roguelogix.phosphophyllite.fluids.MekanismGasWrappers;
import net.roguelogix.phosphophyllite.gui.client.api.IHasUpdatableState;
import net.roguelogix.phosphophyllite.multiblock.generic.IAssemblyAttemptedTile;
import net.roguelogix.phosphophyllite.multiblock.generic.IOnAssemblyTile;
import net.roguelogix.phosphophyllite.multiblock.generic.IOnDisassemblyTile;
import net.roguelogix.phosphophyllite.multiblock.generic.MultiblockBlock;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;
import net.roguelogix.phosphophyllite.registry.TileSupplier;
import net.roguelogix.phosphophyllite.util.BlockStates;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static net.roguelogix.biggerreactors.multiblocks.reactor.blocks.ReactorAccessPort.PortDirection.*;

@RegisterTileEntity(name = "reactor_coolant_port")
public class ReactorCoolantPortTile extends ReactorBaseTile implements IPhosphophylliteFluidHandler, INamedContainerProvider, IHasUpdatableState<ReactorCoolantPortState>, IAssemblyAttemptedTile, IOnAssemblyTile, IOnDisassemblyTile {
    
    @RegisterTileEntity.Type
    public static TileEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final TileSupplier SUPPLIER = ReactorCoolantPortTile::new;
    
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
            return LazyOptional.of(() -> MekanismGasWrappers.wrap(this)).cast();
        }
        return super.getCapability(cap, side);
    }
    
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
    
    @Nullable
    @Override
    public CompoundNBT fluidTagInTank(int tank) {
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
    public boolean fluidValidForTank(int tank, @Nonnull Fluid fluid) {
        if (transitionTank == null) {
            return false;
        }
        return transitionTank.fluidValidForTank(tank, fluid);
    }
    
    @Override
    public long fill(@Nonnull Fluid fluid, @Nullable CompoundNBT tag, long amount, boolean simulate) {
        if (transitionTank == null || direction != INLET) {
            return 0;
        }
        return transitionTank.fill(fluid, tag, amount, simulate);
    }
    
    @Override
    public long drain(@Nonnull Fluid fluid, @Nullable CompoundNBT tag, long amount, boolean simulate) {
        if (transitionTank == null || direction == INLET) {
            return 0;
        }
        return transitionTank.drain(fluid, tag, amount, simulate);
    }
    
    
    public long pushFluid() {
        if (!connected || direction == INLET) {
            return 0;
        }
        if (handlerOptional.isPresent()) {
            Fluid fluid = transitionTank.vaporType();
            long amount = transitionTank.vaporAmount();
            amount = transitionTank.drain(fluid, null, amount, true);
            amount = handler.fill(fluid, null, amount, false);
            amount = transitionTank.drain(fluid, null, amount, false);
            return amount;
        } else {
            handlerOptional = LazyOptional.empty();
            handler = null;
            connected = false;
        }
        return 0;
    }
    
    private boolean connected = false;
    Direction steamOutputDirection = null;
    @Nonnull
    LazyOptional<?> handlerOptional = LazyOptional.empty();
    IPhosphophylliteFluidHandler handler = null;
    FluidTank EMPTY_TANK = new FluidTank(0);
    private ReactorAccessPort.PortDirection direction = INLET;
    public final ReactorCoolantPortState reactorCoolantPortState = new ReactorCoolantPortState(this);
    
    @SuppressWarnings("DuplicatedCode")
    public void neighborChanged() {
        handlerOptional = LazyOptional.empty();
        handler = null;
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
        LazyOptional<IFluidHandler> fluidOptional = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, steamOutputDirection.getOpposite());
        if (fluidOptional.isPresent()) {
            connected = true;
            handlerOptional = fluidOptional;
            handler = FluidHandlerWrapper.wrap(fluidOptional.orElse(EMPTY_TANK));
        } else if (GAS_HANDLER_CAPABILITY != null) {
            LazyOptional<IGasHandler> gasOptional = te.getCapability(GAS_HANDLER_CAPABILITY, steamOutputDirection.getOpposite());
            if (gasOptional.isPresent()) {
                IGasHandler gasHandler = gasOptional.orElse(MekanismGasWrappers.EMPTY_TANK);
                connected = true;
                handlerOptional = gasOptional;
                handler = MekanismGasWrappers.wrap(gasHandler);
            }
        }
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
        transitionTank = null;
        neighborChanged();
    }
}
