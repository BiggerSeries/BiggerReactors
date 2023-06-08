package net.roguelogix.biggerreactors.multiblocks.heatexchanger.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import net.roguelogix.biggerreactors.BiggerReactors;
import net.roguelogix.biggerreactors.Config;
import net.roguelogix.biggerreactors.client.CommonRender;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.containers.HeatExchangerTerminalContainer;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.state.HeatExchangerState;
import net.roguelogix.phosphophyllite.client.gui.screens.PhosphophylliteScreen;
import net.roguelogix.phosphophyllite.client.gui.RenderHelper;
import net.roguelogix.phosphophyllite.client.gui.elements.RenderedElement;
import net.roguelogix.phosphophyllite.client.gui.elements.TooltipElement;

import javax.annotation.Nonnull;
import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public class HeatExchangerTerminalScreen extends PhosphophylliteScreen<HeatExchangerTerminalContainer> {

    private static final ResourceLocation DEFAULT_TEXTURE = new ResourceLocation(BiggerReactors.modid, "textures/screen/heat_exchanger_terminal.png");

    private HeatExchangerState heatExchangerState;

    private Fluid condenserIntakeFluid = Fluids.EMPTY;
    private Fluid condenserExhaustFluid = Fluids.EMPTY;
    private Fluid evaporatorIntakeFluid = Fluids.EMPTY;
    private Fluid evaporatorExhaustFluid = Fluids.EMPTY;

    public HeatExchangerTerminalScreen(HeatExchangerTerminalContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, Component.translatable("screen.biggerreactors.heat_exchanger_terminal"), DEFAULT_TEXTURE, 144, 144);

        // Initialize heat exchanger state.
        this.heatExchangerState = (HeatExchangerState) this.getMenu().getGuiPacket();
        this.condenserIntakeFluid = BuiltInRegistries.FLUID.get(new ResourceLocation(this.heatExchangerState.condenserIntakeFluid));
        this.condenserExhaustFluid = BuiltInRegistries.FLUID.get(new ResourceLocation(this.heatExchangerState.condenserExhaustFluid));
        this.evaporatorIntakeFluid = BuiltInRegistries.FLUID.get(new ResourceLocation(this.heatExchangerState.evaporatorIntakeFluid));
        this.evaporatorExhaustFluid = BuiltInRegistries.FLUID.get(new ResourceLocation(this.heatExchangerState.evaporatorExhaustFluid));
    }

    /**
     * Initialize the screen.
     */
    @Override
    public void init() {
        super.init();

        // Set title to be drawn in the center.
        this.titleLabelX = (this.imageWidth - this.font.width(this.getTitle())) / 2;

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
        this.addScreenElement(new TooltipElement<>(this, 9, 103, 16, 16, Component.translatable("screen.biggerreactors.heat_exchanger_terminal.temperature.condenser.tooltip")));

        // (Bottom) Evaporator temperature tooltip:
        this.addScreenElement(new TooltipElement<>(this, 9, 122, 16, 16, Component.translatable("screen.biggerreactors.heat_exchanger_terminal.temperature.evaporator.tooltip")));

        // (Bottom) Condenser flow rate tooltip:
        this.addScreenElement(new TooltipElement<>(this, 75, 103, 16, 16, Component.translatable("screen.biggerreactors.heat_exchanger_terminal.flow_rate.condenser.tooltip")));

        // (Bottom) Evaporator flow rate tooltip:
        this.addScreenElement(new TooltipElement<>(this, 75, 122, 16, 16, Component.translatable("screen.biggerreactors.heat_exchanger_terminal.flow_rate.evaporator.tooltip")));

        // (Bottom) Condenser intake gauge tooltip:
        this.addScreenElement(new TooltipElement<>(this, 9, 17, 16, 16, Component.translatable("screen.biggerreactors.heat_exchanger_terminal.intake_gauge.condenser.tooltip")));

        // (Top) Evaporator intake gauge tooltip:
        this.addScreenElement(new TooltipElement<>(this, 31, 17, 16, 16, Component.translatable("screen.biggerreactors.heat_exchanger_terminal.intake_gauge.evaporator.tooltip")));

        // (Top) Condenser temperature gauge tooltip:
        this.addScreenElement(new TooltipElement<>(this, 53, 17, 16, 16, Component.translatable("screen.biggerreactors.heat_exchanger_terminal.temperature.condenser.tooltip")));

        // (Top) Evaporator temperature gauge tooltip:
        this.addScreenElement(new TooltipElement<>(this, 75, 17, 16, 16, Component.translatable("screen.biggerreactors.heat_exchanger_terminal.temperature.evaporator.tooltip")));

        // (Top) Condenser exhaust gauge tooltip:
        this.addScreenElement(new TooltipElement<>(this, 97, 17, 16, 16, Component.translatable("screen.biggerreactors.heat_exchanger_terminal.exhaust_gauge.condenser.tooltip")));

        // (Top) Evaporator exhaust gauge tooltip:
        this.addScreenElement(new TooltipElement<>(this, 119, 17, 16, 16, Component.translatable("screen.biggerreactors.heat_exchanger_terminal.exhaust_gauge.evaporator.tooltip")));
    }

    /**
     * Initialize gauges.
     */
    private void initGauges() {
        // (Top) Condenser intake tank:
        RenderedElement<HeatExchangerTerminalContainer> condenserIntakeTank = new RenderedElement<>(this, 8, 36, 18, 64, 0, 144, Component.empty());
        condenserIntakeTank.onRender = (@Nonnull GuiGraphics graphics, int mX, int mY) -> CommonRender.renderFluidGauge(graphics,
                condenserIntakeTank, heatExchangerState.condenserIntakeFluidAmount, heatExchangerState.condenserTankSize, this.condenserIntakeFluid);
        this.addScreenElement(condenserIntakeTank);

        // (Top) Evaporator intake tank:
        RenderedElement<HeatExchangerTerminalContainer> evaporatorIntakeTank = new RenderedElement<>(this, 30, 36, 18, 64, 0, 144, Component.empty());
        evaporatorIntakeTank.onRender = (@Nonnull GuiGraphics graphics, int mX, int mY) -> CommonRender.renderFluidGauge(graphics,
                evaporatorIntakeTank, heatExchangerState.evaporatorIntakeFluidAmount, heatExchangerState.evaporatorTankSize, this.evaporatorIntakeFluid);
        this.addScreenElement(evaporatorIntakeTank);

        // (Top) Condenser heat gauge:
        RenderedElement<HeatExchangerTerminalContainer> condenserHeatGauge = new RenderedElement<>(this, 52, 36, 18, 64, 0, 144, Component.empty());
        condenserHeatGauge.onRender = (@Nonnull GuiGraphics graphics, int mX, int mY) -> HeatExchangerTerminalScreen.renderHeatGauge(graphics, condenserHeatGauge, heatExchangerState.condenserChannelTemperature, Config.CONFIG.HeatExchanger.gui.HeatDisplayMax);
        this.addScreenElement(condenserHeatGauge);

        // (Top) Evaporator heat gauge:
        RenderedElement<HeatExchangerTerminalContainer> evaporatorHeatGauge = new RenderedElement<>(this, 74, 36, 18, 64, 0, 144, Component.empty());
        evaporatorHeatGauge.onRender = (@Nonnull GuiGraphics graphics, int mX, int mY) -> HeatExchangerTerminalScreen.renderHeatGauge(graphics, evaporatorHeatGauge, heatExchangerState.evaporatorChannelTemperature, Config.CONFIG.HeatExchanger.gui.HeatDisplayMax);
        this.addScreenElement(evaporatorHeatGauge);

        // (Top) Condenser exhaust tank:
        RenderedElement<HeatExchangerTerminalContainer> condenserExhaustTank = new RenderedElement<>(this, 96, 36, 18, 64, 0, 144, Component.empty());
        condenserExhaustTank.onRender = (@Nonnull GuiGraphics graphics, int mX, int mY) -> CommonRender.renderFluidGauge(graphics,
                condenserExhaustTank, heatExchangerState.condenserExhaustFluidAmount, heatExchangerState.condenserTankSize, this.condenserExhaustFluid);
        this.addScreenElement(condenserExhaustTank);

        // (Top) Evaporator exhaust tank:
        RenderedElement<HeatExchangerTerminalContainer> evaporatorExhaustTank = new RenderedElement<>(this, 118, 36, 18, 64, 0, 144, Component.empty());
        evaporatorExhaustTank.onRender = (@Nonnull GuiGraphics graphics, int mX, int mY) -> CommonRender.renderFluidGauge(graphics,
                evaporatorExhaustTank, heatExchangerState.evaporatorExhaustFluidAmount, heatExchangerState.evaporatorTankSize, this.evaporatorExhaustFluid);
        this.addScreenElement(evaporatorExhaustTank);
    }

    /**
     * Tick/update this screen.
     */
    @Override
    public void containerTick() {
        // Check if condenser intake fluid changed.
        if (!heatExchangerState.condenserIntakeFluid.equals(Objects.requireNonNull(ForgeRegistries.FLUIDS.getKey(condenserIntakeFluid)).toString())) {
            condenserIntakeFluid = BuiltInRegistries.FLUID.get(new ResourceLocation(heatExchangerState.condenserIntakeFluid));
        }

        // Check if evaporator intake fluid changed.
        if (!heatExchangerState.evaporatorIntakeFluid.equals(Objects.requireNonNull(ForgeRegistries.FLUIDS.getKey(evaporatorIntakeFluid)).toString())) {
            evaporatorIntakeFluid = BuiltInRegistries.FLUID.get(new ResourceLocation(heatExchangerState.evaporatorIntakeFluid));
        }

        // Check if condenser exhaust fluid changed.
        if (!heatExchangerState.condenserExhaustFluid.equals(Objects.requireNonNull(ForgeRegistries.FLUIDS.getKey(condenserExhaustFluid)).toString())) {
            condenserExhaustFluid = BuiltInRegistries.FLUID.get(new ResourceLocation(heatExchangerState.condenserExhaustFluid));
        }

        // Check if evaporator exhaust fluid changed.
        if (!heatExchangerState.evaporatorExhaustFluid.equals(Objects.requireNonNull(ForgeRegistries.FLUIDS.getKey(evaporatorExhaustFluid)).toString())) {
            evaporatorExhaustFluid = BuiltInRegistries.FLUID.get(new ResourceLocation(heatExchangerState.evaporatorExhaustFluid));
        }
    }

    /**
     * Render a heat exchanger heat gauge.
     *
     * @param graphics    The current pose stack.
     * @param symbol       The symbol to draw as.
     * @param heatStored   The heat value to draw.
     * @param heatCapacity The max heat capacity this gauge can display.
     */
    public static void renderHeatGauge(@Nonnull GuiGraphics graphics, @Nonnull RenderedElement<HeatExchangerTerminalContainer> symbol, double heatStored, double heatCapacity) {
        // If there's no heat, there's no need to draw.
        if ((heatStored > 0) && (heatCapacity > 0)) {
            // Calculate how much needs to be rendered.
            int renderSize = (int) ((symbol.height * heatStored) / heatCapacity);
            // Render heat.
            symbol.blit(graphics, symbol.u + 36, symbol.v);
            // Render backdrop/mask away extra heat.
            symbol.blit(graphics, symbol.width, symbol.height - renderSize, symbol.u + 18, symbol.v);
        }
        // Draw frame.
        symbol.blit(graphics);
        // Update tooltip.
        symbol.tooltip = Component.literal(String.format("%.1f/%.1f K", heatStored, heatCapacity));
    }

    /**
     * Draw the status text for this screen.
     *
     * @param graphics    The current pose stack.
     * @param mouseX       The x position of the mouse.
     * @param mouseY       The y position of the mouse.
     * @param partialTicks Partial ticks.
     */
    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);

        // Render text for condenser channel temperature:
        graphics.drawString(this.getFont(), String.format("%.0f K", this.heatExchangerState.condenserChannelTemperature), this.getGuiLeft() + 27, this.getGuiTop() + 107, 4210752);

        // Render text for evaporator channel temperature:
        graphics.drawString(this.getFont(), String.format("%.0f K", this.heatExchangerState.evaporatorChannelTemperature), this.getGuiLeft() + 27, this.getGuiTop() + 127, 4210752);

        // Render text for condenser channel flow rate:
        graphics.drawString(this.getFont(), RenderHelper.formatValue((this.heatExchangerState.condenserChannelFlowRate / 1000.0), 1, "B/t", true), this.getGuiLeft() + 93, this.getGuiTop() + 107, 4210752);

        // Render text for evaporator channel flow rate:
        graphics.drawString(this.getFont(), RenderHelper.formatValue((this.heatExchangerState.evaporatorChannelFlowRate / 1000.0), 1, "B/t", true), this.getGuiLeft() + 93, this.getGuiTop() + 127, 4210752);
    }
}
