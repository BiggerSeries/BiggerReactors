package net.roguelogix.biggerreactors.multiblocks.reactor.tiles;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.EmptyHandler;
import net.roguelogix.biggerreactors.Config;
import net.roguelogix.biggerreactors.blocks.materials.MaterialBlock;
import net.roguelogix.biggerreactors.items.ingots.BlutoniumIngot;
import net.roguelogix.biggerreactors.items.ingots.CyaniteIngot;
import net.roguelogix.biggerreactors.items.ingots.UraniumIngot;
import net.roguelogix.biggerreactors.multiblocks.reactor.blocks.ReactorAccessPort;
import net.roguelogix.biggerreactors.multiblocks.reactor.containers.ReactorAccessPortContainer;
import net.roguelogix.biggerreactors.multiblocks.reactor.state.ReactorAccessPortState;
import net.roguelogix.phosphophyllite.client.gui.api.IHasUpdatableState;
import net.roguelogix.phosphophyllite.debug.DebugInfo;
import net.roguelogix.phosphophyllite.multiblock.common.IEventMultiblock;
import net.roguelogix.phosphophyllite.multiblock.validated.IValidatedMultiblock;
import net.roguelogix.phosphophyllite.registry.RegisterTile;
import net.roguelogix.phosphophyllite.util.BlockStates;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static net.roguelogix.biggerreactors.multiblocks.reactor.blocks.ReactorAccessPort.PortDirection.*;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ReactorAccessPortTile extends ReactorBaseTile implements IItemHandler, MenuProvider, IHasUpdatableState<ReactorAccessPortState>, IEventMultiblock.AssemblyStateTransition {
    
    @RegisterTile("reactor_access_port")
    public static final BlockEntityType.BlockEntitySupplier<ReactorAccessPortTile> SUPPLIER = new RegisterTile.Producer<>(ReactorAccessPortTile::new);
    
    private static final TagKey<Item> uraniumIngotTag = TagKey.create(BuiltInRegistries.ITEM.key(), new ResourceLocation("forge:ingots/uranium"));
    private static final TagKey<Item> uraniumBlockTag = TagKey.create(BuiltInRegistries.ITEM.key(), new ResourceLocation("forge:storage_blocks/uranium"));
    
    public static final int FUEL_SLOT = 0;
    public static final int WASTE_SLOT = 1;
    public static final int FUEL_INSERT_SLOT = 2;
    
    public ReactorAccessPortTile(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
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
    
    @Nonnull
    @Override
    public DebugInfo getDebugInfo() {
        return super.getDebugInfo().add("PortDirection: " + direction);
    }
    
    @Override
    public void onAssemblyStateTransition(IValidatedMultiblock.AssemblyState oldState, IValidatedMultiblock.AssemblyState newState) {
        if (newState != IValidatedMultiblock.AssemblyState.DISASSEMBLED) {
            assert level != null;
            level.setBlock(worldPosition, level.getBlockState(worldPosition).setValue(PORT_DIRECTION_ENUM_PROPERTY, direction), 3);
            itemOutputDirection = getBlockState().getValue(BlockStates.FACING);
        } else {
            itemOutputDirection = null;
        }
        neighborChanged();
    }
    
    LazyOptional<IItemHandler> itemStackHandler = LazyOptional.of(() -> this);
    
    @Override
    protected <T> LazyOptional<T> capability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemStackHandler.cast();
        }
        return super.capability(cap, side);
    }
    
    @Override
    public int getSlots() {
        return 3;
    }
    
    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        if (nullableController() == null) {
            return ItemStack.EMPTY;
        }
        var reactorSim = controller().simulation();
        if(reactorSim == null){
            return ItemStack.EMPTY;
        }
        if (slot == WASTE_SLOT) {
            long availableIngots = reactorSim.fuelTank().waste() / Config.CONFIG.Reactor.FuelMBPerIngot;
            return new ItemStack(CyaniteIngot.INSTANCE, (int) availableIngots);
        } else if (slot == FUEL_SLOT) {
            long availableIngots = reactorSim.fuelTank().fuel() / Config.CONFIG.Reactor.FuelMBPerIngot;
            return new ItemStack(UraniumIngot.INSTANCE, (int) availableIngots);
        } else {
            return ItemStack.EMPTY;
        }
    }
    
    @Nonnull
    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!isInlet() || nullableController() == null || slot != FUEL_INSERT_SLOT) {
            return stack;
        }
        stack = stack.copy();
        if (stack.getItem().builtInRegistryHolder().is(uraniumIngotTag) || stack.getItem().builtInRegistryHolder().is(uraniumIngotTag) || stack.getItem() == BlutoniumIngot.INSTANCE) {
            long maxAcceptable = controller().refuel(stack.getCount() * Config.CONFIG.Reactor.FuelMBPerIngot, true);
            long canAccept = maxAcceptable - (maxAcceptable % Config.CONFIG.Reactor.FuelMBPerIngot);
            controller().refuel(canAccept, simulate);
            if (canAccept > 0) {
                stack.setCount(stack.getCount() - (int) (canAccept / Config.CONFIG.Reactor.FuelMBPerIngot));
            }
        }
        if (stack.getItem().builtInRegistryHolder().is(uraniumBlockTag) || stack.getItem().builtInRegistryHolder().is(uraniumBlockTag) || stack.getItem() == MaterialBlock.BLUTONIUM.asItem()) {
            long maxAcceptable = controller().refuel(stack.getCount() * (Config.CONFIG.Reactor.FuelMBPerIngot * 9), true);
            long canAccept = maxAcceptable - (maxAcceptable % (Config.CONFIG.Reactor.FuelMBPerIngot * 9));
            controller().refuel(canAccept, simulate);
            if (canAccept > 0) {
                stack.setCount(stack.getCount() - (int) (canAccept / (Config.CONFIG.Reactor.FuelMBPerIngot * 9)));
            }
        }
        return stack;
    }
    
    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (isInlet() || nullableController() == null || slot == FUEL_INSERT_SLOT) {
            return ItemStack.EMPTY;
        }
        
        if (slot == WASTE_SLOT && !fuelMode) {
            long maxExtractable = controller().extractWaste(amount * Config.CONFIG.Reactor.FuelMBPerIngot, true);
            long toExtracted = maxExtractable - (maxExtractable % Config.CONFIG.Reactor.FuelMBPerIngot);
            long extracted = controller().extractWaste(toExtracted, simulate);
            
            return new ItemStack(CyaniteIngot.INSTANCE, (int) Math.min(amount, extracted / Config.CONFIG.Reactor.FuelMBPerIngot));
        } else if (slot == FUEL_SLOT && fuelMode) {
            long maxExtractable = controller().extractFuel(amount * Config.CONFIG.Reactor.FuelMBPerIngot, true);
            long toExtracted = maxExtractable - (maxExtractable % Config.CONFIG.Reactor.FuelMBPerIngot);
            long extracted = controller().extractFuel(toExtracted, simulate);
            
            return new ItemStack(UraniumIngot.INSTANCE, (int) Math.min(amount, extracted / Config.CONFIG.Reactor.FuelMBPerIngot));
        }
        
        return ItemStack.EMPTY;
    }
    
    @Override
    public int getSlotLimit(int slot) {
        if (nullableController() == null) {
            return 0;
        }
        var reactorSim = controller().simulation();
        if(reactorSim == null){
            return 0;
        }
        return (int) (reactorSim.fuelTank().capacity() / Config.CONFIG.Reactor.FuelMBPerIngot);
    }
    
    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (slot == FUEL_INSERT_SLOT) {
            return stack.getItem().builtInRegistryHolder().is(uraniumIngotTag) || stack.getItem() == BlutoniumIngot.INSTANCE
                    || stack.getItem().builtInRegistryHolder().is(uraniumBlockTag) || stack.getItem() == MaterialBlock.BLUTONIUM.asItem();
        } else if (slot == FUEL_SLOT) {
            return stack.getItem() == UraniumIngot.INSTANCE;
        } else {
            return stack.getItem() == CyaniteIngot.INSTANCE;
        }
    }
    
    public int pushWaste(int waste, boolean simulated) {
        if (itemOutput.isPresent()) {
            IItemHandler output = itemOutput.orElse(EmptyHandler.INSTANCE);
            waste /= Config.CONFIG.Reactor.FuelMBPerIngot;
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
            return (int) (wasteHandled * Config.CONFIG.Reactor.FuelMBPerIngot);
        }
        return 0;
    }
    
    public void ejectWaste() {
        controller().extractWaste(pushWaste((int) controller().extractWaste(Integer.MAX_VALUE, true), false), false);
    }
    
    public int pushFuel(int fuel, boolean simulated) {
        if (itemOutput.isPresent()) {
            IItemHandler output = itemOutput.orElse(EmptyHandler.INSTANCE);
            fuel /= Config.CONFIG.Reactor.FuelMBPerIngot;
            int fuelHandled = 0;
            for (int i = 0; i < output.getSlots(); i++) {
                if (fuel == 0) {
                    break;
                }
                ItemStack toInsertStack = new ItemStack(UraniumIngot.INSTANCE, fuel);
                ItemStack remainingStack = output.insertItem(i, toInsertStack, simulated);
                fuelHandled += toInsertStack.getCount() - remainingStack.getCount();
                fuel -= toInsertStack.getCount() - remainingStack.getCount();
            }
            return (int) (fuelHandled * Config.CONFIG.Reactor.FuelMBPerIngot);
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
        itemOutput = te.getCapability(ForgeCapabilities.ITEM_HANDLER, itemOutputDirection.getOpposite());
        connected = itemOutput.isPresent();
    }
    
    @Override
    public void runRequest(String requestName, Object requestData) {
        if (nullableController() == null) {
            return;
        }
        
        // Change IO direction.
        if (requestName.equals("setDirection")) {
            this.setDirection(((Integer) requestData != 0) ? OUTLET : INLET);
            assert level != null;
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
        return Component.translatable(ReactorAccessPort.INSTANCE.getDescriptionId());
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new ReactorAccessPortContainer(windowId, this.worldPosition, player);
    }
    
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
