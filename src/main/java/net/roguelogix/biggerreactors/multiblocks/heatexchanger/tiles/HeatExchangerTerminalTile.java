package net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.blocks.HeatExchangerTerminalBlock;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.gui.container.HeatExchangerTerminalContainer;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.state.HeatExchangerState;
import net.roguelogix.phosphophyllite.client.gui.api.IHasUpdatableState;
import net.roguelogix.phosphophyllite.registry.RegisterTile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class HeatExchangerTerminalTile extends HeatExchangerBaseTile implements MenuProvider, IHasUpdatableState<HeatExchangerState> {
    
    @RegisterTile("heat_exchanger_terminal")
    public static final BlockEntityType.BlockEntitySupplier<HeatExchangerTerminalTile> SUPPLIER = new RegisterTile.Producer<>(HeatExchangerTerminalTile::new);
    
    public HeatExchangerTerminalTile(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
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
        if (nullableController() == null) {
            return;
        }
        
        state.condenserTankSize = controller().condenserTank.perSideCapacity;
        
        state.condenserIntakeFluid = ForgeRegistries.FLUIDS.getKey(controller().condenserTank.fluidTypeInTank(0)).toString();
        state.condenserIntakeFluidAmount = controller().condenserTank.fluidAmountInTank(0);
        
        state.condenserExhaustFluid = ForgeRegistries.FLUIDS.getKey(controller().condenserTank.fluidTypeInTank(1)).toString();
        state.condenserExhaustFluidAmount = controller().condenserTank.fluidAmountInTank(1);
        
        
        state.evaporatorTankSize = controller().evaporatorTank.perSideCapacity;
        
        state.evaporatorIntakeFluid = ForgeRegistries.FLUIDS.getKey(controller().evaporatorTank.fluidTypeInTank(0)).toString();
        state.evaporatorIntakeFluidAmount = controller().evaporatorTank.fluidAmountInTank(0);
        
        state.evaporatorExhaustFluid = ForgeRegistries.FLUIDS.getKey(controller().evaporatorTank.fluidTypeInTank(1)).toString();
        state.evaporatorExhaustFluidAmount = controller().evaporatorTank.fluidAmountInTank(1);
        
        
        state.condenserChannelTemperature = controller().condenserHeatBody.temperature();
        state.condenserChannelFlowRate = controller().condenserTank.transitionedLastTick();
        
        state.evaporatorChannelTemperature = controller().evaporatorHeatBody.temperature();
        state.evaporatorChannelFlowRate = controller().evaporatorTank.transitionedLastTick();
    }
    
    @Override
    public Component getDisplayName() {
        return Component.translatable(HeatExchangerTerminalBlock.INSTANCE.getDescriptionId());
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, @Nonnull Inventory playerInventory, @Nonnull Player player) {
        return new HeatExchangerTerminalContainer(windowId, this.worldPosition, player);
    }
}
