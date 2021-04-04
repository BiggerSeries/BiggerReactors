package net.roguelogix.biggerreactors.multiblocks.heatexchanger.gui.container;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.math.BlockPos;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.blocks.HeatExchangerTerminalBlock;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles.HeatExchangerTerminalTile;
import net.roguelogix.phosphophyllite.gui.GuiSync;
import net.roguelogix.phosphophyllite.registry.ContainerSupplier;
import net.roguelogix.phosphophyllite.registry.RegisterContainer;

import javax.annotation.Nonnull;

@RegisterContainer(name = "heat_exchanger_terminal")
public class HeatExchangerTerminalContainer extends Container implements GuiSync.IGUIPacketProvider {

    @RegisterContainer.Type
    public static ContainerType<HeatExchangerTerminalContainer> INSTANCE;
    @RegisterContainer.Supplier
    public static final ContainerSupplier SUPPLIER = HeatExchangerTerminalContainer::new;

    private PlayerEntity player;
    private HeatExchangerTerminalTile tileEntity;

    public HeatExchangerTerminalContainer(int windowId, BlockPos blockPos, PlayerEntity player) {
        super(INSTANCE, windowId);
        this.player = player;
        this.tileEntity = (HeatExchangerTerminalTile) player.world.getTileEntity(blockPos);
        this.getGuiPacket();
    }

    /**
     * @return The current state of the machine.
     */
    @Override
    public GuiSync.IGUIPacket getGuiPacket() {
        return this.tileEntity.getState();
    }

    @Override
    public boolean canInteractWith(@Nonnull PlayerEntity player) {
        assert tileEntity.getWorld() != null;
        return isWithinUsableDistance(IWorldPosCallable.of(tileEntity.getWorld(), tileEntity.getPos()),
                player, HeatExchangerTerminalBlock.INSTANCE);
    }
}
