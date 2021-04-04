package net.roguelogix.biggerreactors.multiblocks.heatexchanger.gui.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.roguelogix.biggerreactors.BiggerReactors;
import net.roguelogix.biggerreactors.Config;
import net.roguelogix.biggerreactors.client.CommonRender;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.gui.container.HeatExchangerTerminalContainer;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.state.HeatExchangerState;
import net.roguelogix.phosphophyllite.gui.client.RenderHelper;
import net.roguelogix.phosphophyllite.gui.client.ScreenBase;
import net.roguelogix.phosphophyllite.gui.client.elements.Symbol;
import net.roguelogix.phosphophyllite.gui.client.elements.Tooltip;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class HeatExchangerTerminalScreen extends ScreenBase<HeatExchangerTerminalContainer> {

    private static final ResourceLocation DEFAULT_TEXTURE = new ResourceLocation(BiggerReactors.modid, "textures/screen/heat_exchanger_terminal.png");

    private HeatExchangerState heatExchangerState;

    private Fluid condenserIntakeFluid = Fluids.EMPTY;
    private Fluid condenserExhaustFluid = Fluids.EMPTY;
    private Fluid evaporatorIntakeFluid = Fluids.EMPTY;
    private Fluid evaporatorExhaustFluid = Fluids.EMPTY;

    public HeatExchangerTerminalScreen(HeatExchangerTerminalContainer container, PlayerInventory playerInventory, ITextComponent title) {
        super(container, playerInventory, new TranslationTextComponent("screen.biggerreactors.heat_exchanger_terminal"), DEFAULT_TEXTURE, 198, 106);

        // Initialize heat exchanger state.
        this.heatExchangerState = (HeatExchangerState) this.getContainer().getGuiPacket();
        this.condenserIntakeFluid = Registry.FLUID.getOrDefault(new ResourceLocation(this.heatExchangerState.condenserIntakeFluid));
        this.condenserExhaustFluid = Registry.FLUID.getOrDefault(new ResourceLocation(this.heatExchangerState.condenserExhaustFluid));
        this.evaporatorIntakeFluid = Registry.FLUID.getOrDefault(new ResourceLocation(this.heatExchangerState.evaporatorIntakeFluid));
        this.evaporatorExhaustFluid = Registry.FLUID.getOrDefault(new ResourceLocation(this.heatExchangerState.evaporatorExhaustFluid));
    }

    /**
     * Initialize the screen.
     */
    @Override
    public void init() {
        super.init();

        // Set title to be drawn in the center.
        this.titleX = (this.getWidth() / 2) - (this.getFont().getStringPropertyWidth(this.getTitle()) / 2);

        // Initialize tooltips:
        this.initTooltips();

        // Initialize gauges:
        this.initGauges();
    }

    /**
     * Initialize tooltips.
     **/
    private void initTooltips() {
        // (Left) Condenser temperature tooltip:
        this.addElement(new Tooltip<>(this, 8, 17, 16, 16, new TranslationTextComponent("screen.biggerreactors.heat_exchanger_terminal.temperature.condenser.tooltip")));

        // (Left) Evaporator temperature tooltip:
        this.addElement(new Tooltip<>(this, 8, 36, 16, 16, new TranslationTextComponent("screen.biggerreactors.heat_exchanger_terminal.temperature.evaporator.tooltip")));

        // (Left) Condenser flow rate tooltip:
        this.addElement(new Tooltip<>(this, 8, 55, 16, 16, new TranslationTextComponent("screen.biggerreactors.heat_exchanger_terminal.flow_rate.condenser.tooltip")));

        // (Left) Evaporator flow rate tooltip:
        this.addElement(new Tooltip<>(this, 8, 74, 16, 16, new TranslationTextComponent("screen.biggerreactors.heat_exchanger_terminal.flow_rate.evaporator.tooltip")));

        // (Top) Condenser intake gauge tooltip:
        this.addElement(new Tooltip<>(this, 86, 17, 16, 16, new TranslationTextComponent("screen.biggerreactors.heat_exchanger_terminal.intake_gauge.condenser.tooltip")));

        // (Top) Evaporator intake gauge tooltip:
        this.addElement(new Tooltip<>(this, 108, 17, 16, 16, new TranslationTextComponent("screen.biggerreactors.heat_exchanger_terminal.exhaust_gauge.condenser.tooltip")));

        // (Top) Heat exchanger temperature tooltip:
        this.addElement(new Tooltip<>(this, 130, 17, 16, 16, new TranslationTextComponent("screen.biggerreactors.heat_exchanger_terminal.heat_gauge.tooltip")));

        // (Top) Condenser exhaust gauge tooltip:
        this.addElement(new Tooltip<>(this, 152, 17, 16, 16, new TranslationTextComponent("screen.biggerreactors.heat_exchanger_terminal.intake_gauge.evaporator.tooltip")));

        // (Top) Evaporator exhaust gauge tooltip:
        this.addElement(new Tooltip<>(this, 174, 17, 16, 16, new TranslationTextComponent("screen.biggerreactors.heat_exchanger_terminal.exhaust_gauge.evaporator.tooltip")));
    }

    /**
     * Initialize gauges.
     */
    private void initGauges() {
        // (Top) Condenser intake tank:
        Symbol<HeatExchangerTerminalContainer> condenserIntakeTank = new Symbol<>(this, 85, 36, 18, 64, 0, 152, StringTextComponent.EMPTY);
        condenserIntakeTank.onRender = (@Nonnull MatrixStack mS, int mX, int mY) -> CommonRender.renderFluidGauge(mS,
                condenserIntakeTank, heatExchangerState.condenserIntakeFluidAmount, Config.HeatExchanger.FluidTankCapacity, this.condenserIntakeFluid);
        this.addElement(condenserIntakeTank);

        // (Top) Condenser exhaust tank:
        Symbol<HeatExchangerTerminalContainer> condenserExhaustTank = new Symbol<>(this, 107, 36, 18, 64, 0, 152, StringTextComponent.EMPTY);
        condenserExhaustTank.onRender = (@Nonnull MatrixStack mS, int mX, int mY) -> CommonRender.renderFluidGauge(mS,
                condenserExhaustTank, heatExchangerState.condenserExhaustFluidAmount, Config.HeatExchanger.FluidTankCapacity, this.condenserExhaustFluid);
        this.addElement(condenserExhaustTank);

        // (Top) Heat Evaporator heat gauge:
        Symbol<HeatExchangerTerminalContainer> caseHeatGauge = new Symbol<>(this, 129, 36, 18, 64, 0, 106, StringTextComponent.EMPTY);
        caseHeatGauge.onRender = (@Nonnull MatrixStack mS, int mX, int mY) -> HeatExchangerTerminalScreen.renderHeatGauge(mS, caseHeatGauge, heatExchangerState.heatStored, Config.HeatExchanger.GUI.HeatDisplayMax);
        this.addElement(caseHeatGauge);

        // (Top) Evaporator intake tank:
        Symbol<HeatExchangerTerminalContainer> evaporatorIntakeTank = new Symbol<>(this, 151, 36, 18, 64, 0, 152, StringTextComponent.EMPTY);
        evaporatorIntakeTank.onRender = (@Nonnull MatrixStack mS, int mX, int mY) -> CommonRender.renderFluidGauge(mS,
                evaporatorIntakeTank, heatExchangerState.evaporatorIntakeFluidAmount, Config.HeatExchanger.FluidTankCapacity, this.evaporatorIntakeFluid);
        this.addElement(evaporatorIntakeTank);

        // (Top) Evaporator exhaust tank:
        Symbol<HeatExchangerTerminalContainer> evaporatorExhaustTank = new Symbol<>(this, 173, 36, 18, 64, 0, 152, StringTextComponent.EMPTY);
        evaporatorExhaustTank.onRender = (@Nonnull MatrixStack mS, int mX, int mY) -> CommonRender.renderFluidGauge(mS,
                evaporatorExhaustTank, heatExchangerState.evaporatorExhaustFluidAmount, Config.HeatExchanger.FluidTankCapacity, this.evaporatorExhaustFluid);
        this.addElement(evaporatorExhaustTank);
    }

    /**
     * Render a heat exchanger heat gauge.
     *
     * @param mStack       The current matrix stack.
     * @param symbol       The symbol to draw as.
     * @param heatStored   The heat value to draw.
     * @param heatCapacity The max heat capacity this gauge can display.
     */
    public static void renderHeatGauge(@Nonnull MatrixStack mStack, @Nonnull Symbol<HeatExchangerTerminalContainer> symbol, double heatStored, double heatCapacity) {
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
        symbol.tooltip = new StringTextComponent(String.format("%.1f/%.1f \u00B0C", heatStored, heatCapacity));
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
    public void render(@Nonnull MatrixStack mStack, int mouseX, int mouseY, float partialTicks) {
        super.render(mStack, mouseX, mouseY, partialTicks);

        // Render text for condenser channel temperature:
        this.getFont().drawString(mStack, String.format("%.0f \u00B0C", this.heatExchangerState.condenserChannelTemperature), this.getGuiLeft() + 27, this.getGuiTop() + 22, 4210752);

        // Render text for evaporator channel temperature:
        this.getFont().drawString(mStack, String.format("%.0f \u00B0C", this.heatExchangerState.evaporatorChannelTemperature), this.getGuiLeft() + 27, this.getGuiTop() + 41, 4210752);

        // Render text for condenser channel flow rate:
        this.getFont().drawString(mStack, RenderHelper.formatValue((this.heatExchangerState.condenserChannelFlowRate / 1000.0), 1, "B/t", true), this.getGuiLeft() + 27, this.getGuiTop() + 60, 4210752);

        // Render text for evaporator channel flow rate:
        this.getFont().drawString(mStack, RenderHelper.formatValue((this.heatExchangerState.evaporatorChannelFlowRate / 1000.0), 1, "B/t", true), this.getGuiLeft() + 27, this.getGuiTop() + 79, 4210752);
    }
}
