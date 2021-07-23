package net.roguelogix.biggerreactors.multiblocks.reactor.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fmllegacy.network.NetworkHooks;
import net.roguelogix.biggerreactors.multiblocks.reactor.blocks.ReactorTerminal;
import net.roguelogix.biggerreactors.multiblocks.reactor.containers.ReactorTerminalContainer;
import net.roguelogix.biggerreactors.multiblocks.reactor.state.ReactorState;
import net.roguelogix.phosphophyllite.gui.client.api.IHasUpdatableState;
import net.roguelogix.phosphophyllite.items.DebugTool;
import net.roguelogix.phosphophyllite.multiblock.generic.MultiblockBlock;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;
import net.roguelogix.phosphophyllite.registry.TileSupplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@RegisterTileEntity(name = "reactor_terminal")
public class ReactorTerminalTile extends ReactorBaseTile implements MenuProvider, IHasUpdatableState<ReactorState> {
    
    @RegisterTileEntity.Type
    public static BlockEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final TileSupplier SUPPLIER = ReactorTerminalTile::new;
    
    public ReactorTerminalTile(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
    
    public final ReactorState reactorState = new ReactorState(this);
    
    @Override
    @Nonnull
    public ReactorState getState() {
        this.updateState();
        return this.reactorState;
    }
    
    @Override
    public void updateState() {
        if (controller != null) {
            controller.updateReactorState(reactorState);
        }
    }
    
    @SuppressWarnings("DuplicatedCode")
    @Override
    @Nonnull
    public InteractionResult onBlockActivated(@Nonnull Player player, @Nonnull InteractionHand handIn) {
        if (player.isCrouching() && handIn == InteractionHand.MAIN_HAND && player.getMainHandItem().getItem() == DebugTool.INSTANCE) {
            if (controller != null) {
                controller.toggleActive();
            }
            return InteractionResult.SUCCESS;
        }
        
        if (handIn == InteractionHand.MAIN_HAND) {
            assert level != null;
            if (level.getBlockState(worldPosition).getValue(MultiblockBlock.ASSEMBLED)) {
                if (!level.isClientSide) {
                    NetworkHooks.openGui((ServerPlayer) player, this, this.getBlockPos());
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.onBlockActivated(player, handIn);
    }
    
    @Override
    @Nonnull
    public Component getDisplayName() {
        return new TranslatableComponent(ReactorTerminal.INSTANCE.getDescriptionId());
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, @Nonnull Inventory playerInventory, @Nonnull Player player) {
        return new ReactorTerminalContainer(windowId, this.worldPosition, player);
    }
}
