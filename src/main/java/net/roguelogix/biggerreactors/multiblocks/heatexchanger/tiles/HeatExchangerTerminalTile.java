package net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles;

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
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.blocks.HeatExchangerTerminalBlock;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.gui.container.HeatExchangerTerminalContainer;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.state.HeatExchangerState;
import net.roguelogix.phosphophyllite.gui.client.api.IHasUpdatableState;
import net.roguelogix.phosphophyllite.multiblock.generic.MultiblockBlock;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@RegisterTileEntity(name = "heat_exchanger_terminal")
public class HeatExchangerTerminalTile extends HeatExchangerBaseTile implements MenuProvider, IHasUpdatableState<HeatExchangerState> {
    
    @RegisterTileEntity.Type
    public static BlockEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final BlockEntityType.BlockEntitySupplier<HeatExchangerTerminalTile> SUPPLIER = HeatExchangerTerminalTile::new;
    
    public HeatExchangerTerminalTile(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
    
    @Override
    @Nonnull
    public InteractionResult onBlockActivated(@Nonnull Player player, @Nonnull InteractionHand handIn) {
        assert level != null;
        if (level.getBlockState(worldPosition).getValue(MultiblockBlock.ASSEMBLED)) {
            if (!level.isClientSide) {
                NetworkHooks.openGui((ServerPlayer) player, this, this.getBlockPos());
            }
            return InteractionResult.SUCCESS;
        }
        return super.onBlockActivated(player, handIn);
    }
    
    HeatExchangerState state = new HeatExchangerState(this);
    
    @Nonnull
    @Override
    public HeatExchangerState getState() {
        return state;
    }
    
    @Override
    public void updateState() {
        // TODO: trigger an actual update
        if (controller == null) {
            return;
        }
        
        state.condenserTankSize = controller.condenserTank.perSideCapacity;
        
        state.condenserIntakeFluid = controller.condenserTank.fluidTypeInTank(0).getRegistryName().toString();
        state.condenserIntakeFluidAmount = controller.condenserTank.fluidAmountInTank(0);

        state.condenserExhaustFluid = controller.condenserTank.fluidTypeInTank(1).getRegistryName().toString();
        state.condenserExhaustFluidAmount = controller.condenserTank.fluidAmountInTank(1);
    
        
        state.evaporatorTankSize = controller.evaporatorTank.perSideCapacity;
    
        state.evaporatorIntakeFluid = controller.evaporatorTank.fluidTypeInTank(0).getRegistryName().toString();
        state.evaporatorIntakeFluidAmount = controller.evaporatorTank.fluidAmountInTank(0);

        state.evaporatorExhaustFluid = controller.evaporatorTank.fluidTypeInTank(1).getRegistryName().toString();
        state.evaporatorExhaustFluidAmount = controller.evaporatorTank.fluidAmountInTank(1);

        
        state.condenserChannelTemperature = controller.condenserHeatBody.temperature();
        state.condenserChannelFlowRate = controller.condenserTank.transitionedLastTick();

        state.evaporatorChannelTemperature = controller.evaporatorHeatBody.temperature();
        state.evaporatorChannelFlowRate = controller.evaporatorTank.transitionedLastTick();
    }
    
    @Override
    public Component getDisplayName() {
        return new TranslatableComponent(HeatExchangerTerminalBlock.INSTANCE.getDescriptionId());
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, @Nonnull Inventory playerInventory, @Nonnull Player player) {
        return new HeatExchangerTerminalContainer(windowId, this.worldPosition, player);
    }
}
