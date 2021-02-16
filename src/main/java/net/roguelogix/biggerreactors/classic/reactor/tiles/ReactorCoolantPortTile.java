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
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.fml.network.NetworkHooks;
import net.roguelogix.biggerreactors.classic.reactor.blocks.ReactorAccessPort;
import net.roguelogix.biggerreactors.classic.reactor.blocks.ReactorCoolantPort;
import net.roguelogix.biggerreactors.classic.reactor.containers.ReactorCoolantPortContainer;
import net.roguelogix.biggerreactors.classic.reactor.deps.ReactorGasHandler;
import net.roguelogix.biggerreactors.classic.reactor.state.ReactorCoolantPortState;
import net.roguelogix.biggerreactors.registries.FluidTransitionRegistry;
import net.roguelogix.phosphophyllite.gui.client.api.IHasUpdatableState;
import net.roguelogix.phosphophyllite.multiblock.generic.IAssemblyAttemptedTile;
import net.roguelogix.phosphophyllite.multiblock.generic.MultiblockBlock;
import net.roguelogix.phosphophyllite.multiblock.generic.MultiblockController;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static net.roguelogix.biggerreactors.classic.reactor.blocks.ReactorAccessPort.PortDirection.*;

@RegisterTileEntity(name = "reactor_coolant_port")
public class ReactorCoolantPortTile extends ReactorBaseTile implements IFluidHandler, INamedContainerProvider, IHasUpdatableState<ReactorCoolantPortState>, IAssemblyAttemptedTile {
    
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
            return ReactorGasHandler.create(() -> controller).cast();
        }
        return super.getCapability(cap, side);
    }
    
    @Override
    public int getTanks() {
        return 2;
    }
    
    @Nonnull
    @Override
    public FluidStack getFluidInTank(int tank) {
        if (controller != null) {
            if (tank == 0) {
                return controller.getCurrentLiquidStack();
            }
            if (tank == 1) {
                return controller.getCurrentVaporStack();
            }
        }
        return FluidStack.EMPTY;
    }
    
    @Override
    public int getTankCapacity(int tank) {
        if (controller != null) {
            return (int) controller.getSteamCapacity();
        }
        return 0;
    }
    
    @Override
    public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
        if (tank == 0) {
            return FluidTransitionRegistry.liquidTransition(stack.getRawFluid()) != null;
        }
        if (tank == 1) {
            return FluidTransitionRegistry.gasTransition(stack.getRawFluid()) != null;
        }
        return false;
    }
    
    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (direction == OUTLET) {
            return 0;
        }
        if (controller != null) {
            return (int) controller.addCoolantLiquid(resource.getRawFluid(), resource.getAmount(), action.simulate());
        }
        return 0;
    }
    
    @Nonnull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (direction == INLET || controller == null) {
            return FluidStack.EMPTY;
        }
        resource = resource.copy(); // do i need to do this? the documentation doesnt say if i can modify this stack or not
        resource.setAmount((int) controller.extractCoolantVapor(resource.getFluid(), resource.getAmount(), action.simulate()));
        return resource;
    }
    
    @Nonnull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (direction == INLET || controller == null) {
            return FluidStack.EMPTY;
        }
        FluidStack vaporStack = controller.getCurrentVaporStack();
        vaporStack.setAmount((int) controller.extractCoolantVapor(vaporStack.getFluid(), vaporStack.getAmount(), action.simulate()));
        return vaporStack;
    }
    
    public long pushSteam(long amount) {
        if (!connected || direction == INLET || amount == 0) {
            return 0;
        }
        if (steamGasOutput != null && steamGasOutput.isPresent()) {
            if (!controller.getCurrentVapor().getTags().contains(new ResourceLocation("forge:steam"))) {
                return 0;
            }
            IGasHandler output = steamGasOutput.orElse(ReactorGasHandler.EMPTY_TANK);
            return ReactorGasHandler.pushSteamToHandler(output, amount);
        } else if (vaporOutput != null && vaporOutput.isPresent()) {
            IFluidHandler handler = vaporOutput.orElse(EMPTY_TANK);
            FluidStack vaporStack = controller.getCurrentVaporStack();
            vaporStack.setAmount((int) amount);
            if (handler.fill(vaporStack, FluidAction.SIMULATE) == 0) {
                // aight, so, whatever we have isn't gonna work, so, gonna try all of the possible vapors
                vaporStack = null;
                for (Fluid gas : controller.getActiveTransition().gases) {
                    if (gas != controller.getCurrentVapor()) {
                        FluidStack possibleStack = new FluidStack(gas, (int) amount);
                        if (handler.fill(possibleStack, FluidAction.SIMULATE) != 0) {
                            vaporStack = possibleStack;
                            break;
                        }
                    }
                }
                if (vaporStack == null) {
                    return 0;
                }
                // set the reactors vapor to whatever was successfully pushed
                controller.extractCoolantVapor(vaporStack.getRawFluid(), 0, true);
            }
            
            return vaporOutput.orElse(EMPTY_TANK).fill(vaporStack, FluidAction.EXECUTE);
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
    public void updateOutputDirection() {
        if (controller.assemblyState() == MultiblockController.AssemblyState.DISASSEMBLED) {
            steamOutputDirection = null;
        } else if (pos.getX() == controller.minCoord().x()) {
            steamOutputDirection = Direction.WEST;
        } else if (pos.getX() == controller.maxCoord().x()) {
            steamOutputDirection = Direction.EAST;
        } else if (pos.getY() == controller.minCoord().y()) {
            steamOutputDirection = Direction.DOWN;
        } else if (pos.getY() == controller.maxCoord().y()) {
            steamOutputDirection = Direction.UP;
        } else if (pos.getZ() == controller.minCoord().z()) {
            steamOutputDirection = Direction.NORTH;
        } else if (pos.getZ() == controller.maxCoord().z()) {
            steamOutputDirection = Direction.SOUTH;
        }
        neighborChanged();
    }
    
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
//            IFluidHandler handler = steamOutput.orElse(EMPTY_TANK);
//            for (int i = 0; i < handler.getTanks(); i++) {
//                if (handler.isFluidValid(i, steam)) {
//                    connected = true;
//                    break;
//                }
//            }
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
}
