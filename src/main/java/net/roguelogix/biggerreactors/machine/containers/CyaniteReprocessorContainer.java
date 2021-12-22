package net.roguelogix.biggerreactors.machine.containers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.roguelogix.biggerreactors.machine.blocks.CyaniteReprocessor;
import net.roguelogix.biggerreactors.machine.tiles.CyaniteReprocessorTile;
import net.roguelogix.biggerreactors.machine.tiles.impl.CyaniteReprocessorItemHandler;
import net.roguelogix.phosphophyllite.client.gui.GuiSync;
import net.roguelogix.phosphophyllite.registry.ContainerSupplier;
import net.roguelogix.phosphophyllite.registry.RegisterContainer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@RegisterContainer(name = "cyanite_reprocessor")
public class CyaniteReprocessorContainer extends AbstractContainerMenu implements GuiSync.IGUIPacketProvider {
    
    @RegisterContainer.Type
    public static MenuType<CyaniteReprocessorContainer> INSTANCE;
    @RegisterContainer.Supplier
    public static final ContainerSupplier SUPPLIER = CyaniteReprocessorContainer::new;
    
    private Player player;
    private CyaniteReprocessorTile tileEntity;
    
    public CyaniteReprocessorContainer(int windowId, BlockPos blockPos, Player player) {
        super(CyaniteReprocessorContainer.INSTANCE, windowId);
        this.player = player;
        this.tileEntity = (CyaniteReprocessorTile) player.level.getBlockEntity(blockPos);
        this.getGuiPacket();
        
        // Populate machine slots.
        if (this.tileEntity != null) {
            IItemHandler handler = tileEntity.getItemHandler();
            // Add input slot.
            this.addSlot(new SlotItemHandler(handler, CyaniteReprocessorItemHandler.INPUT_SLOT_INDEX, 44, 41));
            // Add output slot.
            this.addSlot(new SlotItemHandler(handler, CyaniteReprocessorItemHandler.OUTPUT_SLOT_INDEX, 116, 41));
        }
        
        // Populate player inventory.
        this.populatePlayerInventory();
    }
    
    /**
     * @return The current state of the machine.
     */
    @Nullable
    @Override
    public GuiSync.IGUIPacket getGuiPacket() {
        return this.tileEntity.cyaniteReprocessorState;
    }
    
    @Override
    public boolean stillValid(@Nonnull Player player) {
        assert this.tileEntity.getLevel() != null;
        return stillValid(ContainerLevelAccess.create(tileEntity.getLevel(), tileEntity.getBlockPos()),
                player, CyaniteReprocessor.INSTANCE);
    }
    
    @Nonnull
    @Override
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        ItemStack itemStackA = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        int inventorySize = this.tileEntity.getContainerSize();
        
        if (slot.hasItem()) {
            ItemStack itemStackB = slot.getItem();
            itemStackA = itemStackB.copy();
            
            if (index < inventorySize) {
                if (!this.moveItemStackTo(itemStackB, inventorySize, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemStackB, 0, inventorySize, false)) {
                return ItemStack.EMPTY;
            }
            
            if (itemStackB.getCount() == 0) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        
        return itemStackA;
    }
    
    /**
     * Draw and initialize the player's inventory.
     */
    private void populatePlayerInventory() {
        int guiOffset = 93;
        
        // Add player inventory;
        for (int rowIndex = 0; rowIndex < 3; rowIndex++) {
            for (int columnIndex = 0; columnIndex < 9; columnIndex++) {
                this.addSlot(new Slot(player.getInventory(), (columnIndex + rowIndex * 9 + 9),
                        (8 + columnIndex * 18), (guiOffset + rowIndex * 18)));
            }
        }
        // Add player hotbar.
        for (int columnIndex = 0; columnIndex < 9; columnIndex++) {
            this.addSlot(
                    new Slot(player.getInventory(), columnIndex, (8 + columnIndex * 18), (guiOffset + 58)));
        }
    }
}
