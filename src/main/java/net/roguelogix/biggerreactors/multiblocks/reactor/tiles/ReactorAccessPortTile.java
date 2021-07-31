package net.roguelogix.biggerreactors.multiblocks.reactor.tiles;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.EmptyHandler;
import net.roguelogix.biggerreactors.Config;
import net.roguelogix.biggerreactors.blocks.materials.BlutoniumBlock;
import net.roguelogix.biggerreactors.items.ingots.BlutoniumIngot;
import net.roguelogix.biggerreactors.items.ingots.CyaniteIngot;
import net.roguelogix.biggerreactors.items.ingots.YelloriumIngot;
import net.roguelogix.biggerreactors.multiblocks.reactor.blocks.ReactorAccessPort;
import net.roguelogix.biggerreactors.multiblocks.reactor.containers.ReactorAccessPortContainer;
import net.roguelogix.biggerreactors.multiblocks.reactor.state.ReactorAccessPortState;
import net.roguelogix.phosphophyllite.gui.client.api.IHasUpdatableState;
import net.roguelogix.phosphophyllite.multiblock.IOnAssemblyTile;
import net.roguelogix.phosphophyllite.multiblock.IOnDisassemblyTile;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;
import net.roguelogix.phosphophyllite.util.BlockStates;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static net.roguelogix.biggerreactors.multiblocks.reactor.blocks.ReactorAccessPort.PortDirection.*;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@RegisterTileEntity(name = "reactor_access_port")
public class ReactorAccessPortTile extends ReactorBaseTile implements IItemHandler, MenuProvider, IHasUpdatableState<ReactorAccessPortState>, IOnAssemblyTile, IOnDisassemblyTile {
    
    @RegisterTileEntity.Type
    public static BlockEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final BlockEntityType.BlockEntitySupplier<ReactorAccessPortTile> SUPPLIER = ReactorAccessPortTile::new;
    
    private static final ResourceLocation uraniumIngotTag = new ResourceLocation("forge:ingots/uranium");
    private static final ResourceLocation uraniumBlockTag = new ResourceLocation("forge:storage_blocks/uranium");
    private static final ResourceLocation yelloriumIngotTag = new ResourceLocation("forge:ingots/yellorium");
    private static final ResourceLocation yelloriumBlockTag = new ResourceLocation("forge:storage_blocks/yellorium");
    
    public static final int FUEL_SLOT = 0;
    public static final int WASTE_SLOT = 1;
    public static final int FUEL_INSERT_SLOT = 2;
    
    public ReactorAccessPortTile(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
    
    private ReactorAccessPort.PortDirection direction = INLET;
    private boolean fuelMode = false;
    
    public boolean isInlet() {
        return direction == INLET;
    }
    
    public void setDirection(ReactorAccessPort.PortDirection direction) {
        this.direction = direction;
        this.setChanged();
    }
    
    @Override
    protected void readNBT(CompoundTag compound) {
        if (compound.contains("direction")) {
            direction = ReactorAccessPort.PortDirection.valueOf(compound.getString("direction"));
        }
        if (compound.contains("fuelMode")) {
            fuelMode = compound.getBoolean("fuelMode");
        }
    }
    
    @Override
    protected CompoundTag writeNBT() {
        CompoundTag NBT = new CompoundTag();
        NBT.putString("direction", String.valueOf(direction));
        NBT.putBoolean("fuelMode", fuelMode);
        return NBT;
    }
    
    @Override
    public String getDebugString() {
        return direction.toString();
    }
    
    @Override
    public void onAssembly() {
        assert level != null;
        level.setBlock(worldPosition, getBlockState().setValue(PORT_DIRECTION_ENUM_PROPERTY, direction), 3);
        itemOutputDirection = getBlockState().getValue(BlockStates.FACING);
        neighborChanged();
    }
    
    @Override
    public void onDisassembly() {
        itemOutputDirection = null;
        neighborChanged();
    }
    
    LazyOptional<IItemHandler> itemStackHandler = LazyOptional.of(() -> this);
    
    @Override
    protected <T> LazyOptional<T> capability(Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return itemStackHandler.cast();
        }
        return super.getCapability(cap, side);
    }
    
    @Override
    public int getSlots() {
        return 3;
    }
    
    @Override
    public ItemStack getStackInSlot(int slot) {
        if (nullableController() == null) {
            return ItemStack.EMPTY;
        } else if (slot == WASTE_SLOT) {
            long availableIngots = controller().simulation().fuelTank().waste() / Config.Reactor.FuelMBPerIngot;
            return new ItemStack(CyaniteIngot.INSTANCE, (int) availableIngots);
        } else if (slot == FUEL_SLOT) {
            long availableIngots = controller().simulation().fuelTank().fuel() / Config.Reactor.FuelMBPerIngot;
            return new ItemStack(YelloriumIngot.INSTANCE, (int) availableIngots);
        } else {
            return ItemStack.EMPTY;
        }
    }
    
    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!isInlet() || nullableController() == null || slot != FUEL_INSERT_SLOT) {
            return stack;
        }
        stack = stack.copy();
        if (stack.getItem().getTags().contains(uraniumIngotTag) || stack.getItem().getTags().contains(yelloriumIngotTag) || stack.getItem() == BlutoniumIngot.INSTANCE) {
            long maxAcceptable = controller().refuel(stack.getCount() * Config.Reactor.FuelMBPerIngot, true);
            long canAccept = maxAcceptable - (maxAcceptable % Config.Reactor.FuelMBPerIngot);
            controller().refuel(canAccept, simulate);
            if (canAccept > 0) {
                stack.setCount(stack.getCount() - (int) (canAccept / Config.Reactor.FuelMBPerIngot));
            }
        }
        if (stack.getItem().getTags().contains(uraniumBlockTag) || stack.getItem().getTags().contains(yelloriumBlockTag) || stack.getItem() == BlutoniumBlock.INSTANCE.asItem()) {
            long maxAcceptable = controller().refuel(stack.getCount() * (Config.Reactor.FuelMBPerIngot * 9), true);
            long canAccept = maxAcceptable - (maxAcceptable % (Config.Reactor.FuelMBPerIngot * 9));
            controller().refuel(canAccept, simulate);
            if (canAccept > 0) {
                stack.setCount(stack.getCount() - (int) (canAccept / (Config.Reactor.FuelMBPerIngot * 9)));
            }
        }
        return stack;
    }
    
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (isInlet() || nullableController() == null || slot == FUEL_INSERT_SLOT) {
            return ItemStack.EMPTY;
        }
        
        if (slot == WASTE_SLOT && !fuelMode) {
            long maxExtractable = controller().extractWaste(amount * Config.Reactor.FuelMBPerIngot, true);
            long toExtracted = maxExtractable - (maxExtractable % Config.Reactor.FuelMBPerIngot);
            long extracted = controller().extractWaste(toExtracted, simulate);
            
            return new ItemStack(CyaniteIngot.INSTANCE, (int) Math.min(amount, extracted / Config.Reactor.FuelMBPerIngot));
        } else if (slot == FUEL_SLOT && fuelMode) {
            long maxExtractable = controller().extractFuel(amount * Config.Reactor.FuelMBPerIngot, true);
            long toExtracted = maxExtractable - (maxExtractable % Config.Reactor.FuelMBPerIngot);
            long extracted = controller().extractFuel(toExtracted, simulate);
            
            return new ItemStack(YelloriumIngot.INSTANCE, (int) Math.min(amount, extracted / Config.Reactor.FuelMBPerIngot));
        }
        
        return ItemStack.EMPTY;
    }
    
    @Override
    public int getSlotLimit(int slot) {
        if (nullableController() == null) {
            return 0;
        }
        return (int) (controller().simulation().fuelTank().capacity() / Config.Reactor.FuelMBPerIngot);
    }
    
    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (slot == FUEL_INSERT_SLOT) {
            return stack.getItem().getTags().contains(uraniumIngotTag) || stack.getItem().getTags().contains(yelloriumIngotTag) || stack.getItem() == BlutoniumIngot.INSTANCE
                    || stack.getItem().getTags().contains(uraniumBlockTag) || stack.getItem().getTags().contains(yelloriumBlockTag) || stack.getItem() == BlutoniumBlock.INSTANCE.asItem();
        } else if (slot == FUEL_SLOT) {
            return stack.getItem() == YelloriumIngot.INSTANCE;
        } else {
            return stack.getItem() == CyaniteIngot.INSTANCE;
        }
    }
    
    public int pushWaste(int waste, boolean simulated) {
        if (itemOutput.isPresent()) {
            IItemHandler output = itemOutput.orElse(EmptyHandler.INSTANCE);
            waste /= Config.Reactor.FuelMBPerIngot;
            int wasteHandled = 0;
            for (int i = 0; i < output.getSlots(); i++) {
                if (waste == 0) {
                    break;
                }
                ItemStack toInsertStack = new ItemStack(CyaniteIngot.INSTANCE, waste);
                ItemStack remainingStack = output.insertItem(i, toInsertStack, simulated);
                wasteHandled += toInsertStack.getCount() - remainingStack.getCount();
                waste -= toInsertStack.getCount() - remainingStack.getCount();
            }
            return (int) (wasteHandled * Config.Reactor.FuelMBPerIngot);
        }
        return 0;
    }
    
    public void ejectWaste() {
        controller().extractWaste(pushWaste((int) controller().extractWaste(Integer.MAX_VALUE, true), false), false);
    }
    
    public int pushFuel(int fuel, boolean simulated) {
        if (itemOutput.isPresent()) {
            IItemHandler output = itemOutput.orElse(EmptyHandler.INSTANCE);
            fuel /= Config.Reactor.FuelMBPerIngot;
            int fuelHandled = 0;
            for (int i = 0; i < output.getSlots(); i++) {
                if (fuel == 0) {
                    break;
                }
                ItemStack toInsertStack = new ItemStack(YelloriumIngot.INSTANCE, fuel);
                ItemStack remainingStack = output.insertItem(i, toInsertStack, simulated);
                fuelHandled += toInsertStack.getCount() - remainingStack.getCount();
                fuel -= toInsertStack.getCount() - remainingStack.getCount();
            }
            return (int) (fuelHandled * Config.Reactor.FuelMBPerIngot);
        }
        return 0;
    }
    
    public void ejectFuel() {
        controller().extractFuel(pushFuel((int) controller().extractFuel(Integer.MAX_VALUE, true), false), false);
    }
    
    Direction itemOutputDirection;
    boolean connected;
    LazyOptional<IItemHandler> itemOutput = LazyOptional.empty();
    public final ReactorAccessPortState reactorAccessPortState = new ReactorAccessPortState(this);
    
    @SuppressWarnings("DuplicatedCode")
    public void neighborChanged() {
        itemOutput = LazyOptional.empty();
        if (itemOutputDirection == null) {
            connected = false;
            return;
        }
        assert level != null;
        BlockEntity te = level.getBlockEntity(worldPosition.relative(itemOutputDirection));
        if (te == null) {
            connected = false;
            return;
        }
        itemOutput = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, itemOutputDirection.getOpposite());
        connected = itemOutput.isPresent();
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void runRequest(String requestName, Object requestData) {
        if (nullableController() == null) {
            return;
        }
        
        // Change IO direction.
        if (requestName.equals("setDirection")) {
            this.setDirection(((Integer) requestData != 0) ? OUTLET : INLET);
            level.setBlockAndUpdate(this.worldPosition, this.getBlockState().setValue(PORT_DIRECTION_ENUM_PROPERTY, direction));
            return;
        }
        
        // Change fuel/waste ejection.
        if (requestName.equals("setFuelMode")) {
            this.fuelMode = ((Integer) requestData != 0);
            return;
        }
        
        if (requestName.equals("ejectWaste")) {
            if (fuelMode) {
                ejectFuel();
            } else {
                ejectWaste();
            }
            return;
        }
        
        super.runRequest(requestName, requestData);
    }
    
    @Override
    public Component getDisplayName() {
        return new TranslatableComponent(ReactorAccessPort.INSTANCE.getDescriptionId());
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new ReactorAccessPortContainer(windowId, this.worldPosition, player);
    }
    
    @Nullable
    @Override
    public ReactorAccessPortState getState() {
        this.updateState();
        return this.reactorAccessPortState;
    }
    
    @Override
    public void updateState() {
        reactorAccessPortState.direction = (this.direction == INLET);
        reactorAccessPortState.fuelMode = this.fuelMode;
    }
}
