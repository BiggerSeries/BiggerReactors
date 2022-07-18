package net.roguelogix.biggerreactors.multiblocks.turbine.tiles;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.biggerreactors.multiblocks.turbine.blocks.TurbineTerminal;
import net.roguelogix.biggerreactors.multiblocks.turbine.containers.TurbineTerminalContainer;
import net.roguelogix.biggerreactors.multiblocks.turbine.state.TurbineState;
import net.roguelogix.phosphophyllite.client.gui.api.IHasUpdatableState;
import net.roguelogix.phosphophyllite.registry.RegisterTile;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TurbineTerminalTile extends TurbineBaseTile implements MenuProvider, IHasUpdatableState<TurbineState> {
    
    @RegisterTile("turbine_terminal")
    public static final BlockEntityType.BlockEntitySupplier<TurbineTerminalTile> SUPPLIER = new RegisterTile.Producer<>(TurbineTerminalTile::new);
    
    public TurbineTerminalTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
    
    public final TurbineState turbineState = new TurbineState(this);
    
    @Override
    public TurbineState getState() {
        this.updateState();
        return new TurbineState(this);
    }
    
    @Override
    public void updateState() {
        if (nullableController() != null) {
            controller().updateDataPacket(turbineState);
        }
    }
    
    @Override
    public Component getDisplayName() {
        return Component.translatable(TurbineTerminal.INSTANCE.getDescriptionId());
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new TurbineTerminalContainer(windowId, this.worldPosition, playerInventory.player);
    }
}
