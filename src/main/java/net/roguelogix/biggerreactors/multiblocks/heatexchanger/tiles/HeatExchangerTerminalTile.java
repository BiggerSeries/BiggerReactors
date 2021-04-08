package net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.network.NetworkHooks;
import net.roguelogix.biggerreactors.fluids.LiquidObsidian;
import net.roguelogix.biggerreactors.fluids.Steam;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.blocks.HeatExchangerCoolantPortBlock;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.blocks.HeatExchangerTerminalBlock;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.gui.container.HeatExchangerCoolantPortContainer;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.gui.container.HeatExchangerTerminalContainer;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.state.HeatExchangerState;
import net.roguelogix.phosphophyllite.gui.client.api.IHasUpdatableState;
import net.roguelogix.phosphophyllite.multiblock.generic.MultiblockBlock;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;
import net.roguelogix.phosphophyllite.registry.TileSupplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@RegisterTileEntity(name = "heat_exchanger_terminal")
public class HeatExchangerTerminalTile extends HeatExchangerBaseTile implements INamedContainerProvider, IHasUpdatableState<HeatExchangerState> {
    
    @RegisterTileEntity.Type
    public static TileEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final TileSupplier SUPPLIER = HeatExchangerTerminalTile::new;
    
    public HeatExchangerTerminalTile() {
        super(TYPE);
    }
    
    @Override
    @Nonnull
    public ActionResultType onBlockActivated(@Nonnull PlayerEntity player, @Nonnull Hand handIn) {
        assert world != null;
        if (world.getBlockState(pos).get(MultiblockBlock.ASSEMBLED)) {
            if (!world.isRemote) {
                NetworkHooks.openGui((ServerPlayerEntity) player, this, this.getPos());
            }
            return ActionResultType.SUCCESS;
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
        state.condenserIntakeFluid = controller.condenserTank.fluidTypeInTank(0).getRegistryName().toString();
        state.condenserIntakeFluidAmount = controller.condenserTank.fluidAmountInTank(0);

        state.condenserExhaustFluid = controller.condenserTank.fluidTypeInTank(1).getRegistryName().toString();
        state.condenserExhaustFluidAmount = controller.condenserTank.fluidAmountInTank(1);

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
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent(HeatExchangerTerminalBlock.INSTANCE.getTranslationKey());
    }
    
    @Nullable
    @Override
    public Container createMenu(int windowId, @Nonnull PlayerInventory playerInventory, @Nonnull PlayerEntity player) {
        return new HeatExchangerTerminalContainer(windowId, this.pos, player);
    }
}
