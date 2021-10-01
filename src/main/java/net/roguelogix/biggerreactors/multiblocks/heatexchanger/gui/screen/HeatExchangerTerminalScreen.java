package net.roguelogix.biggerreactors.multiblocks.heatexchanger.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.roguelogix.biggerreactors.BiggerReactors;
import net.roguelogix.biggerreactors.client.CommonRender;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.gui.container.HeatExchangerTerminalContainer;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.state.HeatExchangerState;
import net.roguelogix.phosphophyllite.gui.client.RenderHelper;
import net.roguelogix.phosphophyllite.gui.client.ScreenBase;
import net.roguelogix.phosphophyllite.gui.client.elements.Symbol;
import net.roguelogix.phosphophyllite.gui.client.elements.Tooltip;

import javax.annotation.Nonnull;
import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public class HeatExchangerTerminalScreen extends ScreenBase<HeatExchangerTerminalContainer> {

    private static final ResourceLocation DEFAULT_TEXTURE = new ResourceLocation(BiggerReactors.modid, "textures/screen/heat_exchanger_terminal.png");

    private HeatExchangerState heatExchangerState;

    private Fluid condenserIntakeFluid = Fluids.EMPTY;
    private Fluid condenserExhaustFluid = Fluids.EMPTY;
    private Fluid evaporatorIntakeFluid = Fluids.EMPTY;
    private Fluid evaporatorExhaustFluid = Fluids.EMPTY;

    public HeatExchangerTerminalScreen(HeatExchangerTerminalContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, new TranslatableComponent("screen.biggerreactors.heat_exchanger_terminal"), DEFAULT_TEXTURE, 144, 144);

        // Initialize heat exchanger state.
        this.heatExchangerState = (HeatExchangerState) this.getMenu().getGuiPacket();
        this.condenserIntakeFluid = Registry.FLUID.get(new ResourceLocation(this.heatExchangerState.condenserIntakeFluid));
        this.condenserExhaustFluid = Registry.FLUID.get(new ResourceLocation(this.heatExchangerState.condenserExhaustFluid));
        this.evaporatorIntakeFluid = Registry.FLUID.get(new ResourceLocation(this.heatExchangerState.evaporatorIntakeFluid));
        this.evaporatorExhaustFluid = Registry.FLUID.get(new ResourceLocation(this.heatExchangerState.evaporatorExhaustFluid));
    }

    /**
     * Initialize the screen.
     */
    @Override
    public void init() {
        super.init();

        // Set title to be drawn in the center.
        this.titleLabelX = (this.getWidth() / 2) - (this.getFont().width(this.getTitle()) / 2);

        // Initialize tooltips:
        this.initTooltips();

        // Initialize gauges:
        this.initGauges();
    }

    /**
     * Initialize tooltips.
     **/
    private void initTooltips() {
        // (Bottom) Condenser temperature tooltip:
        this.addElement(new Tooltip<>(this, 9, 103, 16, 16, new TranslatableComponent("screen.biggerreactors.heat_exchanger_terminal.temperature.condenser.tooltip")));

        // (Bottom) Evaporator temperature tooltip:
        this.addElement(new Tooltip<>(this, 9, 122, 16, 16, new TranslatableComponent("screen.biggerreactors.heat_exchanger_terminal.temperature.evaporator.tooltip")));

        // (Bottom) Condenser flow rate tooltip:
        this.addElement(new Tooltip<>(this, 75, 103, 16, 16, new TranslatableComponent("screen.biggerreactors.heat_exchanger_terminal.flow_rate.condenser.tooltip")));

        // (Bottom) Evaporator flow rate tooltip:
        this.addElement(new Tooltip<>(this, 75, 122, 16, 16, new TranslatableComponent("screen.biggerreactors.heat_exchanger_terminal.flow_rate.evaporator.tooltip")));

        // (Bottom) Condenser intake gauge tooltip:
        this.addElement(new Tooltip<>(this, 9, 17, 16, 16, new TranslatableComponent("screen.biggerreactors.heat_exchanger_terminal.intake_gauge.condenser.tooltip")));

        // (Top) Evaporator intake gauge tooltip:
        this.addElement(new Tooltip<>(this, 31, 17, 16, 16, new TranslatableComponent("screen.biggerreactors.heat_exchanger_terminal.intake_gauge.evaporator.tooltip")));

        // (Top) Condenser temperature gauge tooltip:
        this.addElement(new Tooltip<>(this, 53, 17, 16, 16, new TranslatableComponent("screen.biggerreactors.heat_exchanger_terminal.temperature.condenser.tooltip")));

        // (Top) Evaporator temperature gauge tooltip:
        this.addElement(new Tooltip<>(this, 75, 17, 16, 16, new TranslatableComponent("screen.biggerreactors.heat_exchanger_terminal.temperature.evaporator.tooltip")));

        // (Top) Condenser exhaust gauge tooltip:
        this.addElement(new Tooltip<>(this, 97, 17, 16, 16, new TranslatableComponent("screen.biggerreactors.heat_exchanger_terminal.exhaust_gauge.condenser.tooltip")));

        // (Top) Evaporator exhaust gauge tooltip:
        this.addElement(new Tooltip<>(this, 119, 17, 16, 16, new TranslatableComponent("screen.biggerreactors.heat_exchanger_terminal.exhaust_gauge.evaporator.tooltip")));
    }

    /**
     * Initialize gauges.
     */
    private void initGauges() {
        // (Top) Condenser intake tank:
        Symbol<HeatExchangerTerminalContainer> condenserIntakeTank = new Symbol<>(this, 8, 36, 18, 64, 0, 144, TextComponent.EMPTY);
        condenserIntakeTank.onRender = (@Nonnull PoseStack mS, int mX, int mY) -> CommonRender.renderFluidGauge(mS,
                condenserIntakeTank, heatExchangerState.condenserIntakeFluidAmount, heatExchangerState.condenserTankSize, this.condenserIntakeFluid);
        this.addElement(condenserIntakeTank);

        // (Top) Evaporator intake tank:
        Symbol<HeatExchangerTerminalContainer> evaporatorIntakeTank = new Symbol<>(this, 30, 36, 18, 64, 0, 144, TextComponent.EMPTY);
        evaporatorIntakeTank.onRender = (@Nonnull PoseStack mS, int mX, int mY) -> CommonRender.renderFluidGauge(mS,
                evaporatorIntakeTank, heatExchangerState.evaporatorIntakeFluidAmount, heatExchangerState.evaporatorTankSize, this.evaporatorIntakeFluid);
        this.addElement(evaporatorIntakeTank);

        // (Top) Condenser heat gauge:
        Symbol<HeatExchangerTerminalContainer> condenserHeatGauge = new Symbol<>(this, 52, 36, 18, 64, 0, 144, TextComponent.EMPTY);
        condenserHeatGauge.onRender = (@Nonnull PoseStack mS, int mX, int mY) -> HeatExchangerTerminalScreen.renderHeatGauge(mS, condenserHeatGauge, heatExchangerState.condenserChannelTemperature, BiggerReactors.CONFIG.HeatExchanger.gui.HeatDisplayMax);
        this.addElement(condenserHeatGauge);

        // (Top) Evaporator heat gauge:
        Symbol<HeatExchangerTerminalContainer> evaporatorHeatGauge = new Symbol<>(this, 74, 36, 18, 64, 0, 144, TextComponent.EMPTY);
        evaporatorHeatGauge.onRender = (@Nonnull PoseStack mS, int mX, int mY) -> HeatExchangerTerminalScreen.renderHeatGauge(mS, evaporatorHeatGauge, heatExchangerState.evaporatorChannelTemperature, BiggerReactors.CONFIG.HeatExchanger.gui.HeatDisplayMax);
        this.addElement(evaporatorHeatGauge);

        // (Top) Condenser exhaust tank:
        Symbol<HeatExchangerTerminalContainer> condenserExhaustTank = new Symbol<>(this, 96, 36, 18, 64, 0, 144, TextComponent.EMPTY);
        condenserExhaustTank.onRender = (@Nonnull PoseStack mS, int mX, int mY) -> CommonRender.renderFluidGauge(mS,
                condenserExhaustTank, heatExchangerState.condenserExhaustFluidAmount, heatExchangerState.condenserTankSize, this.condenserExhaustFluid);
        this.addElement(condenserExhaustTank);

        // (Top) Evaporator exhaust tank:
        Symbol<HeatExchangerTerminalContainer> evaporatorExhaustTank = new Symbol<>(this, 118, 36, 18, 64, 0, 144, TextComponent.EMPTY);
        evaporatorExhaustTank.onRender = (@Nonnull PoseStack mS, int mX, int mY) -> CommonRender.renderFluidGauge(mS,
                evaporatorExhaustTank, heatExchangerState.evaporatorExhaustFluidAmount, heatExchangerState.evaporatorTankSize, this.evaporatorExhaustFluid);
        this.addElement(evaporatorExhaustTank);
    }

    /**
     * Tick/update this screen.
     */
    @Override
    public void containerTick() {
        // Check if condenser intake fluid changed.
        if(!heatExchangerState.condenserIntakeFluid.equals(Objects.requireNonNull(condenserIntakeFluid.getRegistryName()).toString())) {
            condenserIntakeFluid = Registry.FLUID.get(new ResourceLocation(heatExchangerState.condenserIntakeFluid));
        }

        // Check if evaporator intake fluid changed.
        if(!heatExchangerState.evaporatorIntakeFluid.equals(Objects.requireNonNull(evaporatorIntakeFluid.getRegistryName()).toString())) {
            evaporatorIntakeFluid = Registry.FLUID.get(new ResourceLocation(heatExchangerState.evaporatorIntakeFluid));
        }

        // Check if condenser exhaust fluid changed.
        if(!heatExchangerState.condenserExhaustFluid.equals(Objects.requireNonNull(condenserExhaustFluid.getRegistryName()).toString())) {
            condenserExhaustFluid = Registry.FLUID.get(new ResourceLocation(heatExchangerState.condenserExhaustFluid));
        }

        // Check if evaporator exhaust fluid changed.
        if(!heatExchangerState.evaporatorExhaustFluid.equals(Objects.requireNonNull(evaporatorExhaustFluid.getRegistryName()).toString())) {
            evaporatorExhaustFluid = Registry.FLUID.get(new ResourceLocation(heatExchangerState.evaporatorExhaustFluid));
        }
    }

    /**
     * Render a heat exchanger heat gauge.
     *
     * @param mStack       The current matrix stack.
     * @param symbol       The symbol to draw as.
     * @param heatStored   The heat value to draw.
     * @param heatCapacity The max heat capacity this gauge can display.
     */
    public static void renderHeatGauge(@Nonnull PoseStack mStack, @Nonnull Symbol<HeatExchangerTerminalContainer> symbol, double heatStored, double heatCapacity) {
        // If there's no heat, there's no need to draw.
        if ((heatStored > 0) && (heatCapacity > 0)) {
            // Calculate how much needs to be rendered.
            int renderSize = (int) ((symbol.height * heatStored) / heatCapacity);
            // Render heat.
            symbol.blit(mStack, symbol.u + 36, symbol.v);
            // Render backdrop/mask away extra heat.
            symbol.blit(mStack, symbol.width, symbol.height - renderSize, symbol.u + 18, symbol.v);
        }
        // Draw frame.
        symbol.blit(mStack);
        // Update tooltip.
        symbol.tooltip = new TextComponent(String.format("%.1f/%.1f \u00B0C", heatStored, heatCapacity));
    }

    /**
     * Draw the status text for this screen.
     *
     * @param mStack       The current matrix stack.
     * @param mouseX       The x position of the mouse.
     * @param mouseY       The y position of the mouse.
     * @param partialTicks Partial ticks.
     */
    @Override
    public void render(@Nonnull PoseStack mStack, int mouseX, int mouseY, float partialTicks) {
        super.render(mStack, mouseX, mouseY, partialTicks);

        // Render text for condenser channel temperature:
        this.getFont().draw(mStack, String.format("%.0f \u00B0K", this.heatExchangerState.condenserChannelTemperature), this.getGuiLeft() + 27, this.getGuiTop() + 107, 4210752);

        // Render text for evaporator channel temperature:
        this.getFont().draw(mStack, String.format("%.0f \u00B0K", this.heatExchangerState.evaporatorChannelTemperature), this.getGuiLeft() + 27, this.getGuiTop() + 127, 4210752);

        // Render text for condenser channel flow rate:
        this.getFont().draw(mStack, RenderHelper.formatValue((this.heatExchangerState.condenserChannelFlowRate / 1000.0), 1, "B/t", true), this.getGuiLeft() + 93, this.getGuiTop() + 107, 4210752);

        // Render text for evaporator channel flow rate:
        this.getFont().draw(mStack, RenderHelper.formatValue((this.heatExchangerState.evaporatorChannelFlowRate / 1000.0), 1, "B/t", true), this.getGuiLeft() + 93, this.getGuiTop() + 127, 4210752);
    }
}
