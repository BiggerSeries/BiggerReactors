package net.roguelogix.biggerreactors.multiblocks.turbine.containers;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.math.BlockPos;
import net.roguelogix.biggerreactors.multiblocks.turbine.blocks.TurbineTerminal;
import net.roguelogix.biggerreactors.multiblocks.turbine.tiles.TurbineTerminalTile;
import net.roguelogix.phosphophyllite.gui.GuiSync;
import net.roguelogix.phosphophyllite.registry.ContainerSupplier;
import net.roguelogix.phosphophyllite.registry.RegisterContainer;

import javax.annotation.Nullable;

@RegisterContainer(name = "turbine_terminal")
public class TurbineTerminalContainer extends Container implements GuiSync.IGUIPacketProvider {
    
    @RegisterContainer.Type
    public static ContainerType<TurbineTerminalContainer> INSTANCE;
    @RegisterContainer.Supplier
    public static final ContainerSupplier SUPPLIER = TurbineTerminalContainer::new;
    
    private PlayerEntity player;
    private TurbineTerminalTile tileEntity;
    
    public TurbineTerminalContainer(int windowId, BlockPos blockPos, PlayerEntity player) {
        super(INSTANCE, windowId);
        this.player = player;
        this.tileEntity = (TurbineTerminalTile) player.world.getTileEntity(blockPos);
        this.getGuiPacket();
    }
    
    /**
     * @return The current state of the machine.
     */
    @Nullable
    @Override
    public GuiSync.IGUIPacket getGuiPacket() {
        return this.tileEntity.turbineState;
    }
    
    @Override
    public boolean canInteractWith(PlayerEntity player) {
        assert tileEntity.getWorld() != null;
        return isWithinUsableDistance(IWorldPosCallable.of(tileEntity.getWorld(), tileEntity.getPos()),
                player, TurbineTerminal.INSTANCE);
    }
    
    @Override
    public void executeRequest(String requestName, Object requestData) {
        assert tileEntity.getWorld() != null;
        if (tileEntity.getWorld().isRemote) {
            runRequest(requestName, requestData);
            return;
        }
        
        tileEntity.runRequest(requestName, requestData);
    }
}
