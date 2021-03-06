package net.roguelogix.biggerreactors.multiblocks.turbine.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.roguelogix.biggerreactors.BiggerReactors;
import net.roguelogix.biggerreactors.multiblocks.reactor.containers.ReactorTerminalContainer;
import net.roguelogix.biggerreactors.multiblocks.reactor.state.ReactorState;
import net.roguelogix.biggerreactors.multiblocks.turbine.state.VentState;
import net.roguelogix.biggerreactors.multiblocks.turbine.containers.TurbineTerminalContainer;
import net.roguelogix.biggerreactors.multiblocks.turbine.state.TurbineActivity;
import net.roguelogix.biggerreactors.multiblocks.turbine.state.TurbineState;
import net.roguelogix.biggerreactors.client.Biselector;
import net.roguelogix.biggerreactors.client.CommonRender;
import net.roguelogix.biggerreactors.client.SelectorColors;
import net.roguelogix.biggerreactors.client.Triselector;
import net.roguelogix.phosphophyllite.gui.client.RenderHelper;
import net.roguelogix.phosphophyllite.gui.client.ScreenBase;
import net.roguelogix.phosphophyllite.gui.client.elements.Button;
import net.roguelogix.phosphophyllite.gui.client.elements.Symbol;
import net.roguelogix.phosphophyllite.gui.client.elements.Tooltip;

import javax.annotation.Nonnull;
import java.util.Objects;

public class TurbineTerminalScreen extends ScreenBase<TurbineTerminalContainer> {

    private static final ResourceLocation DEFAULT_TEXTURE = new ResourceLocation(BiggerReactors.modid, "textures/screen/turbine_terminal.png");

    private TurbineState turbineState;

    private Fluid intakeFluid = Fluids.EMPTY;
    private Fluid exhaustFluid = Fluids.EMPTY;

    public TurbineTerminalScreen(TurbineTerminalContainer container, PlayerInventory playerInventory, ITextComponent title) {
        super(container, playerInventory, new TranslationTextComponent("screen.biggerreactors.turbine_terminal"), DEFAULT_TEXTURE, 176, 152);

        // Initialize turbine state.
        turbineState = (TurbineState) this.getContainer().getGuiPacket();
        intakeFluid = Registry.FLUID.getOrDefault(new ResourceLocation(turbineState.intakeResourceLocation));
        exhaustFluid = Registry.FLUID.getOrDefault(new ResourceLocation(turbineState.exhaustResourceLocation));
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
        this.addElement(new Tooltip<>(this, 8, 19, 16, 16, new TranslationTextComponent("screen.biggerreactors.turbine_terminal.tachometer.tooltip")));

        // (Left) Tachometer readout tooltip:
        Tooltip<TurbineTerminalContainer> tachometerReadoutTooltip = new Tooltip<>(this, 26, 19, 53, 16, StringTextComponent.EMPTY);
        tachometerReadoutTooltip.onTick = () -> {
            tachometerReadoutTooltip.tooltip = new StringTextComponent(String.format("%.3f RPM", this.turbineState.currentRPM));
        };
        this.addElement(tachometerReadoutTooltip);

        // (Left) RF generation rate tooltip:
        this.addElement(new Tooltip<>(this, 8, 38, 16, 16, new TranslationTextComponent("screen.biggerreactors.turbine_terminal.energy_generation_rate.tooltip")));

        // (Left) RF generation readout tooltip:
        Tooltip<TurbineTerminalContainer> generationRateReadoutTooltip = new Tooltip<>(this, 26, 38, 53, 16, StringTextComponent.EMPTY);
        generationRateReadoutTooltip.onTick = () -> {
            generationRateReadoutTooltip.tooltip = new StringTextComponent(String.format("%.3f RF/t", this.turbineState.turbineOutputRate));
        };
        this.addElement(generationRateReadoutTooltip);

        // (Left) Flow rate governor tooltip:
        this.addElement(new Tooltip<>(this, 8, 57, 16, 16, new TranslationTextComponent("screen.biggerreactors.turbine_terminal.flow_rate_governor.tooltip")));

        // (Left) Flow rate governor readout tooltip:
        Tooltip<TurbineTerminalContainer> flowRateReadoutTooltip = new Tooltip<>(this, 26, 57, 53, 16, StringTextComponent.EMPTY);
        flowRateReadoutTooltip.onTick = () -> {
            flowRateReadoutTooltip.tooltip = new StringTextComponent(String.format("%d mB/t", this.turbineState.flowRate));
        };
        this.addElement(flowRateReadoutTooltip);

        // (Left) Rotor efficiency tooltip:
        this.addElement(new Tooltip<>(this, 8, 76, 16, 16, new TranslationTextComponent("screen.biggerreactors.turbine_terminal.rotor_efficiency.tooltip")));

        // (Left) Rotor efficiency readout tooltip:
        Tooltip<TurbineTerminalContainer> rotorEfficiencyReadoutTooltip = new Tooltip<>(this, 26, 76, 53, 16, StringTextComponent.EMPTY);
        rotorEfficiencyReadoutTooltip.onTick = () -> {
            rotorEfficiencyReadoutTooltip.tooltip = new StringTextComponent(String.format("%.1f%%", (turbineState.efficiencyRate * 100.0D)));
        };
        this.addElement(rotorEfficiencyReadoutTooltip);

        // (Top) Tachometer gauge tooltip:
        this.addElement(new Tooltip<>(this, 86, 6, 16, 16, new TranslationTextComponent("screen.biggerreactors.turbine_terminal.tachometer.tooltip")));

        // (Top) Internal battery tooltip:
        this.addElement(new Tooltip<>(this, 152, 6, 16, 16, new TranslationTextComponent("screen.biggerreactors.turbine_terminal.internal_battery.tooltip")));
    }

    /**
     * Initialize controls.
     */
    private void initControls() {
        // (Left) Activity toggle:
        Biselector<TurbineTerminalContainer> activityToggle = new Biselector<>(this, 8, 98, new TranslationTextComponent("screen.biggerreactors.turbine_terminal.activity_toggle.tooltip"),
                () -> turbineState.turbineActivity.toInt(), SelectorColors.RED, SelectorColors.GREEN);
        activityToggle.onMouseReleased = (mX, mY, btn) -> {
            // Click logic.
            this.getContainer().executeRequest("setActive", activityToggle.getState() == 0 ? 1 : 0);
            return true;
        };
        this.addElement(activityToggle);

        // (Left) Coil engage toggle:
        Biselector<TurbineTerminalContainer> coilEngageToggle = new Biselector<>(this, 8, 114, new TranslationTextComponent("screen.biggerreactors.turbine_terminal.coil_engage_toggle.tooltip"),
                () -> turbineState.coilStatus ? 1 : 0, SelectorColors.RED, SelectorColors.GREEN);
        coilEngageToggle.onMouseReleased = (mX, mY, btn) -> {
            // Click logic.
            this.getContainer().executeRequest("setCoilEngaged", coilEngageToggle.getState() == 0 ? 1 : 0);
            return true;
        };
        this.addElement(coilEngageToggle);

        // (Left) Vent state toggle:
        Triselector<TurbineTerminalContainer> ventStateToggle = new Triselector<>(this, 8, 130, new TranslationTextComponent("screen.biggerreactors.turbine_terminal.vent_state_toggle.tooltip"),
                () -> turbineState.ventState.toInt(), SelectorColors.YELLOW, SelectorColors.RED, SelectorColors.GREEN);
        ventStateToggle.onMouseReleased = (mX, mY, btn) -> {
            // Click logic.
            this.getContainer().executeRequest("setVentState", ventStateToggle.getState());
            return true;
        };
        this.addElement(ventStateToggle);

        // (Right) Flow rate increase button:
        Button<TurbineTerminalContainer> flowRateIncreaseButton = new Button<>(this, 153, 92, 14, 15, 226, 0, new TranslationTextComponent("screen.biggerreactors.turbine_terminal.flow_rate_increase.tooltip"));
        flowRateIncreaseButton.onMouseReleased = (mX, mY, btn) -> {
            // Click logic. Extra check necessary since this is an "in-class" button.
            if (flowRateIncreaseButton.isMouseOver(mX, mY)) {
                // Calculate amount of change:
                long delta;
                if (Screen.hasShiftDown() && Screen.hasControlDown()) delta = 100L;
                else if (Screen.hasControlDown()) delta = 50L;
                else if (Screen.hasShiftDown()) delta = 10L;
                else delta = 1L;
                // Mouse is hovering, do the thing.
                this.getContainer().executeRequest("changeFlowRate", delta);
                // Play the selection sound.
                flowRateIncreaseButton.playSound(SoundEvents.UI_BUTTON_CLICK);
                return true;
            } else {
                // It ain't hovered, don't do the thing.
                return false;
            }
        };
        flowRateIncreaseButton.onRender = ((mS, mX, mY) -> {
            // Custom rendering.
            if (flowRateIncreaseButton.isMouseOver(mX, mY)) {
                // Mouse is hovering, highlight it.
                flowRateIncreaseButton.blit(mS, 242, 0);
            } else {
                // It ain't hovered, don't highlight.
                flowRateIncreaseButton.blit(mS, 228, 0);
            }
        });
        this.addElement(flowRateIncreaseButton);

        // (Right) Flow rate decrease button:
        Button<TurbineTerminalContainer> flowRateDecreaseButton = new Button<>(this, 153, 108, 14, 15, 226, 0, new TranslationTextComponent("screen.biggerreactors.turbine_terminal.flow_rate_decrease.tooltip"));
        flowRateDecreaseButton.onMouseReleased = (mX, mY, btn) -> {
            // Click logic. Extra check necessary since this is an "in-class" button.
            if (flowRateDecreaseButton.isMouseOver(mX, mY)) {
                // Calculate amount of change:
                long delta;
                if (Screen.hasShiftDown() && Screen.hasControlDown()) delta = -100L;
                else if (Screen.hasControlDown()) delta = -50L;
                else if (Screen.hasShiftDown()) delta = -10L;
                else delta = -1L;
                // Mouse is hovering, do the thing.
                this.getContainer().executeRequest("changeFlowRate", delta);
                // Play the selection sound.
                flowRateDecreaseButton.playSound(SoundEvents.UI_BUTTON_CLICK);
                return true;
            } else {
                // It ain't hovered, don't do the thing.
                return false;
            }
        };
        flowRateDecreaseButton.onRender = (mS, mX, mY) -> {
            // Custom rendering.
            if (flowRateDecreaseButton.isMouseOver(mX, mY)) {
                // Mouse is hovering, highlight it.
                flowRateDecreaseButton.blit(mS, 242, 15);
            } else {
                // It ain't hovered, don't highlight.
                flowRateDecreaseButton.blit(mS, 228, 15);
            }
        };
        this.addElement(flowRateDecreaseButton);
    }

    /**
     * Initialize gauges.
     */
    private void initGauges() {
        // (Top) Tachometer gauge:
        Symbol<TurbineTerminalContainer> tachometerGauge = new Symbol<>(this, 85, 25, 18, 64, 0, 152, StringTextComponent.EMPTY);
        tachometerGauge.onRender = (@Nonnull MatrixStack mS, int mX, int mY) -> TurbineTerminalScreen.renderTachometerGauge(mS, tachometerGauge, turbineState.currentRPM, turbineState.maxRPM);
        this.addElement(tachometerGauge);

        // (Top) Hot intake tank:
        Symbol<TurbineTerminalContainer> intakeTank = new Symbol<>(this, 107, 25, 18, 64, 0, 152, StringTextComponent.EMPTY);
        intakeTank.onRender = (@Nonnull MatrixStack mS, int mX, int mY) -> CommonRender.renderFluidGauge(mS,
                intakeTank, turbineState.intakeStored, turbineState.intakeCapacity, intakeFluid);
        this.addElement(intakeTank);

        // (Top) Cold exhaust tank:
        Symbol<TurbineTerminalContainer> exhaustTank = new Symbol<>(this, 129, 25, 18, 64, 0, 152, StringTextComponent.EMPTY);
        exhaustTank.onRender = (@Nonnull MatrixStack mS, int mX, int mY) -> CommonRender.renderFluidGauge(mS,
                exhaustTank, turbineState.exhaustStored, turbineState.exhaustCapacity, exhaustFluid);
        this.addElement(exhaustTank);

        // (Top) Internal battery:
        Symbol<TurbineTerminalContainer> internalBattery = new Symbol<>(this, 151, 25, 18, 64, 0, 152, StringTextComponent.EMPTY);
        internalBattery.onRender = (@Nonnull MatrixStack mS, int mX, int mY) -> CommonRender.renderEnergyGauge(mS,
                internalBattery, turbineState.energyStored, turbineState.energyCapacity);
        this.addElement(internalBattery);
    }

    /**
     * Initialize symbols.
     */
    private void initSymbols() {
        // (Top) Intake tank symbol:
        Symbol<TurbineTerminalContainer> intakeTankSymbol = new Symbol<>(this, 108, 6, 16, 16, 54, 152, new TranslationTextComponent("screen.biggerreactors.turbine_terminal.intake_tank.tooltip"));
        intakeTankSymbol.onRender = (@Nonnull MatrixStack mS, int mX, int mY) -> RenderHelper.drawMaskedFluid(mS,
                intakeTankSymbol.x, intakeTankSymbol.y, this.getBlitOffset(),
                intakeTankSymbol.width, intakeTankSymbol.height,
                intakeTankSymbol.u, intakeTankSymbol.v, intakeFluid);
        this.addElement(intakeTankSymbol);

        // (Top) Exhaust tank symbol:
        Symbol<TurbineTerminalContainer> exhaustTankSymbol = new Symbol<>(this, 130, 6, 16, 16, 70, 152, new TranslationTextComponent("screen.biggerreactors.turbine_terminal.exhaust_tank.tooltip"));
        exhaustTankSymbol.onRender = (@Nonnull MatrixStack mS, int mX, int mY) -> RenderHelper.drawMaskedFluid(mS,
                exhaustTankSymbol.x, exhaustTankSymbol.y, this.getBlitOffset(),
                exhaustTankSymbol.width, exhaustTankSymbol.height,
                exhaustTankSymbol.u, exhaustTankSymbol.v, exhaustFluid);
        this.addElement(exhaustTankSymbol);
    }


    /**
     * Tick/update this screen.
     */
    @Override
    public void tick() {
        // Update turbine state and tick.
        turbineState = (TurbineState) this.getContainer().getGuiPacket();
        super.tick();
        // Check if intake type changed.
        if(!turbineState.intakeResourceLocation.equals(Objects.requireNonNull(intakeFluid.getRegistryName()).toString())) {
            intakeFluid = Registry.FLUID.getOrDefault(new ResourceLocation(turbineState.intakeResourceLocation));
        }
        // Check if exhaust type changed.
        if(!turbineState.exhaustResourceLocation.equals(Objects.requireNonNull(exhaustFluid.getRegistryName()).toString())) {
            exhaustFluid = Registry.FLUID.getOrDefault(new ResourceLocation(turbineState.exhaustResourceLocation));
        }
    }

    /**
     * Render a turbine tachometer gauge.
     *
     * @param mStack     The current matrix stack.
     * @param symbol     The symbol to draw as.
     * @param currentRPM The rpm value to draw.
     * @param maxRPM     The max rpm capacity this gauge can display.
     */
    public static void renderTachometerGauge(@Nonnull MatrixStack mStack, @Nonnull Symbol<TurbineTerminalContainer> symbol, double currentRPM, double maxRPM) {
        // If there's no heat, there's no need to draw.
        if ((currentRPM > 0) && (maxRPM > 0)) {
            // Calculate how much needs to be rendered.
            int renderSize = (int) ((symbol.height * currentRPM) / maxRPM);
            // Render heat.
            symbol.blit(mStack, symbol.u + 36, symbol.v);
            // Render backdrop/mask away extra heat.
            symbol.blit(mStack, symbol.width, symbol.height - renderSize, symbol.u + 18, symbol.v);
        }
        // Draw frame.
        symbol.blit(mStack);
        // Update tooltip.
        symbol.tooltip = new StringTextComponent(String.format("%.1f/%.1f RPM", currentRPM, maxRPM));
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

        // Render text for turbine tachometer:
        this.getFont().drawString(mStack, RenderHelper.formatValue((this.turbineState.currentRPM), 1, "RPM", false), this.getGuiLeft() + 27, this.getGuiTop() + 23, 4210752);

        // Render text for output rate:
        this.getFont().drawString(mStack, RenderHelper.formatValue(this.turbineState.turbineOutputRate, "RF/t"), this.getGuiLeft() + 27, this.getGuiTop() + 42, 4210752);

        // Render text for flow rate:
        this.getFont().drawString(mStack, RenderHelper.formatValue((this.turbineState.flowRate / 1000.0), 1, "B/t", true), this.getGuiLeft() + 27, this.getGuiTop() + 61, 4210752);

        // Render text for reactivity rate (no fancy suffix for percentages):
        this.getFont().drawString(mStack, String.format("%.1f%%", (this.turbineState.efficiencyRate * 100.0)), this.getGuiLeft() + 27, this.getGuiTop() + 80, 4210752);

        // Render text for online/offline status:
        if (this.turbineState.turbineActivity == TurbineActivity.ACTIVE) {
            // Text for an online turbine:
            this.getFont().drawString(mStack, new TranslationTextComponent("screen.biggerreactors.turbine_terminal.activity_toggle.online").getString(), this.getGuiLeft() + 42, this.getGuiTop() + 102, 4210752);

        } else {
            // Text for an offline turbine:
            this.getFont().drawString(mStack, new TranslationTextComponent("screen.biggerreactors.turbine_terminal.activity_toggle.offline").getString(), this.getGuiLeft() + 42, this.getGuiTop() + 102, 4210752);
        }

        // Render text for coil engage status:
        if (this.turbineState.coilStatus) {
            // Text for engaged coils:
            this.getFont().drawString(mStack, new TranslationTextComponent("screen.biggerreactors.turbine_terminal.coil_engage_toggle.engaged").getString(), this.getGuiLeft() + 42, this.getGuiTop() + 118, 4210752);
        } else {
            // Text for disengaged coils:
            this.getFont().drawString(mStack, new TranslationTextComponent("screen.biggerreactors.turbine_terminal.coil_engage_toggle.disengaged").getString(), this.getGuiLeft() + 42, this.getGuiTop() + 118, 4210752);
        }

        // Render text for vent state:
        if (this.turbineState.ventState == VentState.OVERFLOW) {
            // Text for venting overflow exhaust:
            this.getFont().drawString(mStack, new TranslationTextComponent("screen.biggerreactors.turbine_terminal.vent_state_toggle.overflow").getString(), this.getGuiLeft() + 58, this.getGuiTop() + 134, 4210752);
        } else if (this.turbineState.ventState == VentState.ALL) {
            // Text for venting all exhaust:
            this.getFont().drawString(mStack, new TranslationTextComponent("screen.biggerreactors.turbine_terminal.vent_state_toggle.all").getString(), this.getGuiLeft() + 58, this.getGuiTop() + 134, 4210752);
        } else {
            // Text for venting no exhaust:
            this.getFont().drawString(mStack, new TranslationTextComponent("screen.biggerreactors.turbine_terminal.vent_state_toggle.closed").getString(), this.getGuiLeft() + 58, this.getGuiTop() + 134, 4210752);
        }
    }
}
