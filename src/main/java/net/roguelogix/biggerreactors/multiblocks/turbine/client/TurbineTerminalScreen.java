package net.roguelogix.biggerreactors.multiblocks.turbine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;
import net.roguelogix.biggerreactors.BiggerReactors;
import net.roguelogix.biggerreactors.Config;
import net.roguelogix.biggerreactors.client.Biselector;
import net.roguelogix.biggerreactors.client.CommonRender;
import net.roguelogix.biggerreactors.client.SelectorColors;
import net.roguelogix.biggerreactors.client.Triselector;
import net.roguelogix.biggerreactors.multiblocks.turbine.containers.TurbineTerminalContainer;
import net.roguelogix.biggerreactors.multiblocks.turbine.state.TurbineActivity;
import net.roguelogix.biggerreactors.multiblocks.turbine.state.TurbineState;
import net.roguelogix.biggerreactors.multiblocks.turbine.state.VentState;
import net.roguelogix.phosphophyllite.client.gui.screens.PhosphophylliteScreen;
import net.roguelogix.phosphophyllite.client.gui.RenderHelper;
import net.roguelogix.phosphophyllite.client.gui.elements.InteractiveElement;
import net.roguelogix.phosphophyllite.client.gui.elements.RenderedElement;
import net.roguelogix.phosphophyllite.client.gui.elements.TooltipElement;

import javax.annotation.Nonnull;
import java.util.Objects;

public class TurbineTerminalScreen extends PhosphophylliteScreen<TurbineTerminalContainer> {

    private static final ResourceLocation DEFAULT_TEXTURE = new ResourceLocation(BiggerReactors.modid, "textures/screen/turbine_terminal.png");

    private TurbineState turbineState;

    private Fluid intakeFluid = Fluids.EMPTY;
    private Fluid exhaustFluid = Fluids.EMPTY;

    public TurbineTerminalScreen(TurbineTerminalContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, Component.translatable("screen.biggerreactors.turbine_terminal"), DEFAULT_TEXTURE, 176, 152);

        // Initialize turbine state.
        turbineState = (TurbineState) this.getMenu().getGuiPacket();
        intakeFluid = BuiltInRegistries.FLUID.get(new ResourceLocation(turbineState.intakeResourceLocation));
        exhaustFluid = BuiltInRegistries.FLUID.get(new ResourceLocation(turbineState.exhaustResourceLocation));
    }

    /**
     * Initialize the screen.
     */
    @Override
    public void init() {
        super.init();

        // Initialize tooltips:
        this.initTooltips();

        // Initialize controls:
        this.initControls();

        // Initialize gauges:
        this.initGauges();

        // Initialize symbols:
        this.initSymbols();
    }

    /**
     * Initialize tooltips.
     **/
    private void initTooltips() {
        // (Left) Tachometer tooltip:
        this.addScreenElement(new TooltipElement<>(this, 8, 19, 16, 16, Component.translatable("screen.biggerreactors.turbine_terminal.tachometer.tooltip")));

        // (Left) Tachometer readout tooltip:
        TooltipElement<TurbineTerminalContainer> tachometerReadoutTooltip = new TooltipElement<>(this, 26, 19, 53, 16, Component.empty());
        tachometerReadoutTooltip.onTick = () -> {
            tachometerReadoutTooltip.tooltip = Component.literal(String.format("%.3f RPM", this.turbineState.currentRPM));
        };
        this.addScreenElement(tachometerReadoutTooltip);

        // (Left) RF generation rate tooltip:
        this.addScreenElement(new TooltipElement<>(this, 8, 38, 16, 16, Component.translatable("screen.biggerreactors.turbine_terminal.energy_generation_rate.tooltip")));

        // (Left) RF generation readout tooltip:
        TooltipElement<TurbineTerminalContainer> generationRateReadoutTooltip = new TooltipElement<>(this, 26, 38, 53, 16, Component.empty());
        generationRateReadoutTooltip.onTick = () -> {
            generationRateReadoutTooltip.tooltip = Component.literal(String.format("%.3f RF/t", this.turbineState.turbineOutputRate));
        };
        this.addScreenElement(generationRateReadoutTooltip);

        // (Left) Flow rate governor tooltip:
        this.addScreenElement(new TooltipElement<>(this, 8, 57, 16, 16, Component.translatable("screen.biggerreactors.turbine_terminal.flow_rate_governor.tooltip")));

        // (Left) Flow rate governor readout tooltip:
        TooltipElement<TurbineTerminalContainer> flowRateReadoutTooltip = new TooltipElement<>(this, 26, 57, 53, 16, Component.empty());
        flowRateReadoutTooltip.onTick = () -> {
            flowRateReadoutTooltip.tooltip = Component.literal(String.format("%d mB/t", this.turbineState.flowRate));
        };
        this.addScreenElement(flowRateReadoutTooltip);

        // (Left) Rotor efficiency tooltip:
        this.addScreenElement(new TooltipElement<>(this, 8, 76, 16, 16, Component.translatable("screen.biggerreactors.turbine_terminal.rotor_efficiency.tooltip")));

        // (Left) Rotor efficiency readout tooltip:
        TooltipElement<TurbineTerminalContainer> rotorEfficiencyReadoutTooltip = new TooltipElement<>(this, 26, 76, 53, 16, Component.empty());
        rotorEfficiencyReadoutTooltip.onTick = () -> {
            rotorEfficiencyReadoutTooltip.tooltip = Component.literal(String.format("%.1f%%", (turbineState.efficiencyRate * 100.0D)));
        };
        this.addScreenElement(rotorEfficiencyReadoutTooltip);

        // (Top) Tachometer gauge tooltip:
        this.addScreenElement(new TooltipElement<>(this, 86, 6, 16, 16, Component.translatable("screen.biggerreactors.turbine_terminal.tachometer.tooltip")));

        // (Top) Internal battery tooltip:
        this.addScreenElement(new TooltipElement<>(this, 152, 6, 16, 16, Component.translatable("screen.biggerreactors.turbine_terminal.internal_battery.tooltip")));
    }

    /**
     * Initialize controls.
     */
    private void initControls() {
        // (Left) Activity toggle:
        Biselector<TurbineTerminalContainer> activityToggle = new Biselector<>(this, 8, 98, Component.translatable("screen.biggerreactors.turbine_terminal.activity_toggle.tooltip"),
                () -> turbineState.turbineActivity.toInt(), SelectorColors.RED, SelectorColors.GREEN);
        activityToggle.onMouseReleased = (mX, mY, btn) -> {
            // Click logic.
            this.getMenu().executeRequest("setActive", activityToggle.getState() == 0 ? 1 : 0);
            return true;
        };
        this.addScreenElement(activityToggle);

        // (Left) Coil engage toggle:
        Biselector<TurbineTerminalContainer> coilEngageToggle = new Biselector<>(this, 8, 114, Component.translatable("screen.biggerreactors.turbine_terminal.coil_engage_toggle.tooltip"),
                () -> turbineState.coilStatus ? 1 : 0, SelectorColors.RED, SelectorColors.GREEN);
        coilEngageToggle.onMouseReleased = (mX, mY, btn) -> {
            // Click logic.
            this.getMenu().executeRequest("setCoilEngaged", coilEngageToggle.getState() == 0 ? 1 : 0);
            return true;
        };
        this.addScreenElement(coilEngageToggle);

        // (Left) Vent state toggle:
        Triselector<TurbineTerminalContainer> ventStateToggle = new Triselector<>(this, 8, 130, Component.translatable("screen.biggerreactors.turbine_terminal.vent_state_toggle.tooltip"),
                () -> turbineState.ventState.toInt(), SelectorColors.YELLOW, SelectorColors.RED, SelectorColors.GREEN);
        ventStateToggle.onMouseReleased = (mX, mY, btn) -> {
            // Click logic.
            this.getMenu().executeRequest("setVentState", ventStateToggle.getState());
            return true;
        };
        this.addScreenElement(ventStateToggle);

        // (Right) Flow rate increase button:
        InteractiveElement<TurbineTerminalContainer> flowRateIncreaseButton = new InteractiveElement<>(this, 153, 92, 14, 15, 226, 0, Component.translatable("screen.biggerreactors.turbine_terminal.flow_rate_increase.tooltip"));
        flowRateIncreaseButton.onMouseReleased = (mX, mY, btn) -> {
            // Click logic. Extra check necessary since this is an "in-class" button.
            if (flowRateIncreaseButton.isMouseOver(mX, mY)) {
                // Calculate amount of change:
                long delta;
                if (Screen.hasShiftDown() && Screen.hasControlDown()) delta = Config.CONFIG.Turbine.GUI.DeltaMBHCtrlShift;
                else if (Screen.hasControlDown()) delta = Config.CONFIG.Turbine.GUI.DeltaMBCtrl;
                else if (Screen.hasShiftDown()) delta = Config.CONFIG.Turbine.GUI.DeltaMBShift;
                else delta = Config.CONFIG.Turbine.GUI.DeltaMB;
                // Mouse is hovering, do the thing.
                this.getMenu().executeRequest("changeFlowRate", delta);
                // Play the selection sound.
                flowRateIncreaseButton.playSound(SoundEvents.UI_BUTTON_CLICK);
                return true;
            } else {
                // It ain't hovered, don't do the thing.
                return false;
            }
        };
        flowRateIncreaseButton.onRender = ((graphics, mX, mY) -> {
            // Custom rendering.
            if (flowRateIncreaseButton.isMouseOver(mX, mY)) {
                // Mouse is hovering, highlight it.
                flowRateIncreaseButton.blit(graphics, 242, 0);
            } else {
                // It ain't hovered, don't highlight.
                flowRateIncreaseButton.blit(graphics, 228, 0);
            }
        });
        this.addScreenElement(flowRateIncreaseButton);

        // (Right) Flow rate decrease button:
        InteractiveElement<TurbineTerminalContainer> flowRateDecreaseButton = new InteractiveElement<>(this, 153, 108, 14, 15, 226, 0, Component.translatable("screen.biggerreactors.turbine_terminal.flow_rate_decrease.tooltip"));
        flowRateDecreaseButton.onMouseReleased = (mX, mY, btn) -> {
            // Click logic. Extra check necessary since this is an "in-class" button.
            if (flowRateDecreaseButton.isMouseOver(mX, mY)) {
                // Calculate amount of change:
                long delta;
                if (Screen.hasShiftDown() && Screen.hasControlDown()) delta = -Config.CONFIG.Turbine.GUI.DeltaMBHCtrlShift;
                else if (Screen.hasControlDown()) delta = -Config.CONFIG.Turbine.GUI.DeltaMBCtrl;
                else if (Screen.hasShiftDown()) delta = -Config.CONFIG.Turbine.GUI.DeltaMBShift;
                else delta = -Config.CONFIG.Turbine.GUI.DeltaMB;
                // Mouse is hovering, do the thing.
                this.getMenu().executeRequest("changeFlowRate", delta);
                // Play the selection sound.
                flowRateDecreaseButton.playSound(SoundEvents.UI_BUTTON_CLICK);
                return true;
            } else {
                // It ain't hovered, don't do the thing.
                return false;
            }
        };
        flowRateDecreaseButton.onRender = (graphics, mX, mY) -> {
            // Custom rendering.
            if (flowRateDecreaseButton.isMouseOver(mX, mY)) {
                // Mouse is hovering, highlight it.
                flowRateDecreaseButton.blit(graphics, 242, 15);
            } else {
                // It ain't hovered, don't highlight.
                flowRateDecreaseButton.blit(graphics, 228, 15);
            }
        };
        this.addScreenElement(flowRateDecreaseButton);
    }

    /**
     * Initialize gauges.
     */
    private void initGauges() {
        // (Top) Tachometer gauge:
        RenderedElement<TurbineTerminalContainer> tachometerGauge = new RenderedElement<>(this, 85, 25, 18, 64, 0, 152, Component.empty());
        tachometerGauge.onRender = (@Nonnull GuiGraphics graphics, int mX, int mY) -> TurbineTerminalScreen.renderTachometerGauge(graphics, tachometerGauge, turbineState.currentRPM, turbineState.maxRPM);
        this.addScreenElement(tachometerGauge);

        // (Top) Hot intake tank:
        RenderedElement<TurbineTerminalContainer> intakeTank = new RenderedElement<>(this, 107, 25, 18, 64, 0, 152, Component.empty());
        intakeTank.onRender = (@Nonnull GuiGraphics graphics, int mX, int mY) -> CommonRender.renderFluidGauge(graphics,
                intakeTank, turbineState.intakeStored, turbineState.intakeCapacity, intakeFluid);
        this.addScreenElement(intakeTank);

        // (Top) Cold exhaust tank:
        RenderedElement<TurbineTerminalContainer> exhaustTank = new RenderedElement<>(this, 129, 25, 18, 64, 0, 152, Component.empty());
        exhaustTank.onRender = (@Nonnull GuiGraphics graphics, int mX, int mY) -> CommonRender.renderFluidGauge(graphics,
                exhaustTank, turbineState.exhaustStored, turbineState.exhaustCapacity, exhaustFluid);
        this.addScreenElement(exhaustTank);

        // (Top) Internal battery:
        RenderedElement<TurbineTerminalContainer> internalBattery = new RenderedElement<>(this, 151, 25, 18, 64, 0, 152, Component.empty());
        internalBattery.onRender = (@Nonnull GuiGraphics graphics, int mX, int mY) -> CommonRender.renderEnergyGauge(graphics,
                internalBattery, turbineState.energyStored, turbineState.energyCapacity);
        this.addScreenElement(internalBattery);
    }

    /**
     * Initialize symbols.
     */
    private void initSymbols() {
        // (Top) Intake tank symbol:
        RenderedElement<TurbineTerminalContainer> intakeTankSymbol = new RenderedElement<>(this, 108, 6, 16, 16, 54, 152, Component.translatable("screen.biggerreactors.turbine_terminal.intake_tank.tooltip"));
        intakeTankSymbol.onRender = (@Nonnull GuiGraphics graphics, int mX, int mY) -> RenderHelper.drawMaskedFluid(graphics,
                intakeTankSymbol.x, intakeTankSymbol.y, 0,
                intakeTankSymbol.width, intakeTankSymbol.height,
                intakeTankSymbol.u, intakeTankSymbol.v, intakeFluid);
        this.addScreenElement(intakeTankSymbol);

        // (Top) Exhaust tank symbol:
        RenderedElement<TurbineTerminalContainer> exhaustTankSymbol = new RenderedElement<>(this, 130, 6, 16, 16, 70, 152, Component.translatable("screen.biggerreactors.turbine_terminal.exhaust_tank.tooltip"));
        exhaustTankSymbol.onRender = (@Nonnull GuiGraphics graphics, int mX, int mY) -> RenderHelper.drawMaskedFluid(graphics,
                exhaustTankSymbol.x, exhaustTankSymbol.y, 0,
                exhaustTankSymbol.width, exhaustTankSymbol.height,
                exhaustTankSymbol.u, exhaustTankSymbol.v, exhaustFluid);
        this.addScreenElement(exhaustTankSymbol);
    }


    /**
     * Tick/update this screen.
     */
    @Override
    public void containerTick() {
        // Update turbine state and tick.
        turbineState = (TurbineState) this.getMenu().getGuiPacket();
        super.containerTick();
        // Check if intake type changed.
        if (!turbineState.intakeResourceLocation.equals(Objects.requireNonNull(ForgeRegistries.FLUIDS.getKey(intakeFluid)).toString())) {
            intakeFluid = BuiltInRegistries.FLUID.get(new ResourceLocation(turbineState.intakeResourceLocation));
        }
        // Check if exhaust type changed.
        if (!turbineState.exhaustResourceLocation.equals(Objects.requireNonNull(ForgeRegistries.FLUIDS.getKey(exhaustFluid)).toString())) {
            exhaustFluid = BuiltInRegistries.FLUID.get(new ResourceLocation(turbineState.exhaustResourceLocation));
        }
    }

    /**
     * Render a turbine tachometer gauge.
     *
     * @param poseStack  The current pose stack.
     * @param symbol     The symbol to draw as.
     * @param currentRPM The rpm value to draw.
     * @param maxRPM     The max rpm capacity this gauge can display.
     */
    public static void renderTachometerGauge(@Nonnull GuiGraphics graphics, @Nonnull RenderedElement<TurbineTerminalContainer> symbol, double currentRPM, double maxRPM) {
        // If there's no heat, there's no need to draw.
        if ((currentRPM > 0) && (maxRPM > 0)) {
            // Calculate how much needs to be rendered.
            int renderSize = (int) ((symbol.height * currentRPM) / maxRPM);
            // Render heat.
            symbol.blit(graphics, symbol.u + 36, symbol.v);
            // Render backdrop/mask away extra heat.
            symbol.blit(graphics, symbol.width, symbol.height - renderSize, symbol.u + 18, symbol.v);
        }
        // Draw frame.
        symbol.blit(graphics);
        // Update tooltip.
        symbol.tooltip = Component.literal(String.format("%.1f/%.1f RPM", currentRPM, maxRPM));
    }

    /**
     * Draw the status text for this screen.
     *
     * @param poseStack    The current pose stack.
     * @param mouseX       The x position of the mouse.
     * @param mouseY       The y position of the mouse.
     * @param partialTicks Partial ticks.
     */
    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);

        // Render text for turbine tachometer:
        graphics.drawString(this.getFont(), RenderHelper.formatValue((this.turbineState.currentRPM), 1, "RPM", false), this.getGuiLeft() + 27, this.getGuiTop() + 23, 4210752, false);

        // Render text for output rate:
        graphics.drawString(this.getFont(), RenderHelper.formatValue(this.turbineState.turbineOutputRate, "RF/t"), this.getGuiLeft() + 27, this.getGuiTop() + 42, 4210752, false);

        // Render text for flow rate:
        graphics.drawString(this.getFont(), RenderHelper.formatValue((this.turbineState.flowRate / 1000.0), 1, "B/t", true), this.getGuiLeft() + 27, this.getGuiTop() + 61, 4210752, false);

        // Render text for reactivity rate (no fancy suffix for percentages):
        graphics.drawString(this.getFont(), String.format("%.1f%%", (this.turbineState.efficiencyRate * 100.0)), this.getGuiLeft() + 27, this.getGuiTop() + 80, 4210752, false);

        // Render text for online/offline status:
        if (this.turbineState.turbineActivity == TurbineActivity.ACTIVE) {
            // Text for an online turbine:
            graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactors.turbine_terminal.activity_toggle.online").getString(), this.getGuiLeft() + 42, this.getGuiTop() + 102, 4210752, false);

        } else {
            // Text for an offline turbine:
            graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactors.turbine_terminal.activity_toggle.offline").getString(), this.getGuiLeft() + 42, this.getGuiTop() + 102, 4210752, false);
        }

        // Render text for coil engage status:
        if (this.turbineState.coilStatus) {
            // Text for engaged coils:
            graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactors.turbine_terminal.coil_engage_toggle.engaged").getString(), this.getGuiLeft() + 42, this.getGuiTop() + 118, 4210752, false);
        } else {
            // Text for disengaged coils:
            graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactors.turbine_terminal.coil_engage_toggle.disengaged").getString(), this.getGuiLeft() + 42, this.getGuiTop() + 118, 4210752, false);
        }

        // Render text for vent state:
        if (this.turbineState.ventState == VentState.OVERFLOW) {
            // Text for venting overflow exhaust:
            graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactors.turbine_terminal.vent_state_toggle.overflow").getString(), this.getGuiLeft() + 58, this.getGuiTop() + 134, 4210752, false);
        } else if (this.turbineState.ventState == VentState.ALL) {
            // Text for venting all exhaust:
            graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactors.turbine_terminal.vent_state_toggle.all").getString(), this.getGuiLeft() + 58, this.getGuiTop() + 134, 4210752, false);
        } else {
            // Text for venting no exhaust:
            graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactors.turbine_terminal.vent_state_toggle.closed").getString(), this.getGuiLeft() + 58, this.getGuiTop() + 134, 4210752, false);
        }
    }
}
