package net.roguelogix.biggerreactors.multiblocks.heatexchanger.gui.container;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.math.BlockPos;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.blocks.HeatExchangerCoolantPortBlock;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles.HeatExchangerCoolantPortTile;
import net.roguelogix.phosphophyllite.gui.GuiSync;
import net.roguelogix.phosphophyllite.registry.ContainerSupplier;
import net.roguelogix.phosphophyllite.registry.RegisterContainer;

import javax.annotation.Nonnull;

@RegisterContainer(name = "heat_exchanger_coolant_port")
public class HeatExchangerCoolantPortContainer extends Container implements GuiSync.IGUIPacketProvider {

    @RegisterContainer.Type
    public static ContainerType<HeatExchangerCoolantPortContainer> INSTANCE;
    @RegisterContainer.Supplier
    public static final ContainerSupplier SUPPLIER = HeatExchangerCoolantPortContainer::new;

    private PlayerEntity player;
    private HeatExchangerCoolantPortTile tileEntity;

    public HeatExchangerCoolantPortContainer(int windowId, BlockPos blockPos, PlayerEntity player) {
        super(INSTANCE, windowId);
        this.player = player;
        this.tileEntity = (HeatExchangerCoolantPortTile) player.world.getTileEntity(blockPos);
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
                player, HeatExchangerCoolantPortBlock.INSTANCE);
    }

    @Override
    public void executeRequest(String requestName, Object requestData) {
        assert tileEntity.getWorld() != null;
        if (tileEntity.getWorld().isRemote) {
            runRequest(requestName, requestData);
            return;
        }

        // TODO: rogue, uncomment this line when HeatExchangerBaseTile is able to handle requests from the GUI
        //tileEntity.runRequest(requestName, requestData);
    }
}
