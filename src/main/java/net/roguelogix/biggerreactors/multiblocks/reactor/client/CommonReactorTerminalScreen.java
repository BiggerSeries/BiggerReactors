package net.roguelogix.biggerreactors.multiblocks.reactor.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.roguelogix.biggerreactors.Config;
import net.roguelogix.biggerreactors.client.Biselector;
import net.roguelogix.biggerreactors.client.SelectorColors;
import net.roguelogix.biggerreactors.multiblocks.reactor.containers.ReactorTerminalContainer;
import net.roguelogix.biggerreactors.multiblocks.reactor.state.ReactorActivity;
import net.roguelogix.biggerreactors.multiblocks.reactor.state.ReactorState;
import net.roguelogix.biggerreactors.multiblocks.reactor.state.ReactorType;
import net.roguelogix.phosphophyllite.client.gui.screens.PhosphophylliteScreen;
import net.roguelogix.phosphophyllite.client.gui.RenderHelper;
import net.roguelogix.phosphophyllite.client.gui.elements.InteractiveElement;
import net.roguelogix.phosphophyllite.client.gui.elements.RenderedElement;
import net.roguelogix.phosphophyllite.client.gui.elements.TooltipElement;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class CommonReactorTerminalScreen extends PhosphophylliteScreen<ReactorTerminalContainer> {

    // This state is used once, and as such can be final. Most other states should NOT be final.
    private final ReactorState initialState;

    public CommonReactorTerminalScreen(ReactorTerminalContainer container, Inventory playerInventory, Component title) {
        // We override whatever Minecraft wants to set the title to. It wants "Reactor Terminal," but that's too long.
        super(container, playerInventory, Component.translatable("screen.biggerreactors.reactor_terminal"), RenderHelper.getBlankTextureResource(), 176, 152);

        // Initialize reactor terminal state.
        initialState = (ReactorState) this.getMenu().getGuiPacket();
    }

    /**
     * Initialize whichever subscreen we need.
     */
    @Override
    public void init() {
        if (initialState.reactorType == ReactorType.ACTIVE) {
            // Initialize an actively-cooled reactor screen.
            this.getMinecraft().setScreen(new ActiveReactorTerminalScreen(this.menu, this.inventory, this.title));
        } else {
            // Initialize a passively-cooled reactor screen.
            this.getMinecraft().setScreen(new PassiveReactorTerminalScreen(this.menu, this.inventory, this.title));
        }
    }

    /**
     * Initialize common/shared tooltips.
     *
     * @param screen The screen to initialize.
     */
    public static void initTooltips(@Nonnull PhosphophylliteScreen<ReactorTerminalContainer> screen, ReactorState reactorState) {
        // (Left) Temperature tooltip:
        screen.addScreenElement(new TooltipElement<>(screen, 8, 19, 16, 16, Component.translatable("screen.biggerreactors.reactor_terminal.temperature.tooltip")));

        // (Left) Temperature readout tooltip:
        TooltipElement<ReactorTerminalContainer> temperatureReadoutTooltip = new TooltipElement<>(screen, 26, 19, 53, 16, Component.empty());
        temperatureReadoutTooltip.onTick = () -> {
            temperatureReadoutTooltip.tooltip = Component.literal(String.format("%.3f K", reactorState.fuelHeatStored));
        };
        screen.addScreenElement(temperatureReadoutTooltip);

        // (Left) Fuel consumption rate tooltip:
        screen.addScreenElement(new TooltipElement<>(screen, 8, 57, 16, 16, Component.translatable("screen.biggerreactors.reactor_terminal.fuel_usage_rate.tooltip")));

        // (Left) Fuel consumption readout tooltip:
        TooltipElement<ReactorTerminalContainer> fuelConsumptionReadoutTooltip = new TooltipElement<>(screen, 26, 57, 53, 16, Component.empty());
        fuelConsumptionReadoutTooltip.onTick = () -> {
            fuelConsumptionReadoutTooltip.tooltip = Component.literal(String.format("%.3f mB/t", reactorState.fuelUsageRate));
        };
        screen.addScreenElement(fuelConsumptionReadoutTooltip);

        // (Left) Reactivity rate tooltip:
        screen.addScreenElement(new TooltipElement<>(screen, 8, 76, 16, 16, Component.translatable("screen.biggerreactors.reactor_terminal.reactivity_rate.tooltip")));

        // (Left) Reactivity rate readout tooltip:
        TooltipElement<ReactorTerminalContainer> reactivityRateTooltip = new TooltipElement<>(screen, 26, 76, 53, 16, Component.empty());
        reactivityRateTooltip.onTick = () -> {
            reactivityRateTooltip.tooltip = Component.literal(String.format("%.1f%%", (reactorState.reactivityRate * 100.0)));
        };
        screen.addScreenElement(reactivityRateTooltip);

        // (Top) Fuel mix gauge tooltip:
        screen.addScreenElement(new TooltipElement<>(screen, 86, 6, 16, 16, Component.translatable("screen.biggerreactors.reactor_terminal.fuel_mix.tooltip")));

        // (Top) Case heat gauge tooltip:
        screen.addScreenElement(new TooltipElement<>(screen, 108, 6, 16, 16, Component.translatable("screen.biggerreactors.reactor_terminal.case_heat.tooltip")));

        // (Top) Fuel heat gauge tooltip:
        screen.addScreenElement(new TooltipElement<>(screen, 130, 6, 16, 16, Component.translatable("screen.biggerreactors.reactor_terminal.fuel_heat.tooltip")));
    }

    /**
     * Initialize common/shared controls.
     *
     * @param screen The screen to initialize.
     */
    public static void initControls(@Nonnull PhosphophylliteScreen<ReactorTerminalContainer> screen, ReactorState reactorState) {
        // (Left) Activity toggle:
        Biselector<ReactorTerminalContainer> activityToggle = new Biselector<>(screen, 8, 98, Component.translatable("screen.biggerreactors.reactor_terminal.activity_toggle.tooltip"),
                () -> reactorState.reactorActivity.toInt(), SelectorColors.RED, SelectorColors.GREEN);
        activityToggle.onMouseReleased = (mX, mY, btn) -> {
            // Click logic.
            screen.getMenu().executeRequest("setActive", activityToggle.getState() == 0 ? 1 : 0);
            return true;
        };
        screen.addScreenElement(activityToggle);

        // (Left) Auto-eject toggle:
        Biselector<ReactorTerminalContainer> autoEjectToggle = new Biselector<>(screen, 8, 114, Component.translatable("screen.biggerreactors.reactor_terminal.auto_eject_toggle.tooltip"),
                () -> reactorState.doAutoEject ? 1 : 0, SelectorColors.RED, SelectorColors.CYAN);
        autoEjectToggle.onMouseReleased = (mX, mY, btn) -> {
            // Click logic.
            screen.getMenu().executeRequest("setAutoEject", autoEjectToggle.getState() == 0 ? 1 : 0);
            return true;
        };
        screen.addScreenElement(autoEjectToggle);

        // (Left) Manual eject button:
        InteractiveElement<ReactorTerminalContainer> manualEjectButton = new InteractiveElement<>(screen, 8, 130, 15, 15, 226, 0, Component.translatable("screen.biggerreactors.reactor_terminal.manual_eject.tooltip"));
        manualEjectButton.onMouseReleased = (mX, mY, btn) -> {
            // Click logic. Extra check necessary since this is an "in-class" button.
            if (manualEjectButton.isMouseOver(mX, mY)) {
                // Mouse is hovering, do the thing.
                //screen.getContainer().executeRequest("ejectWaste", true);
                Minecraft.getInstance().player.sendSystemMessage(Component.literal("No effect. This button will be removed in the future."));
                Minecraft.getInstance().player.sendSystemMessage(Component.literal("Use the access ports to eject waste!"));
                // Play the selection sound.
                manualEjectButton.playSound(SoundEvents.UI_BUTTON_CLICK);
                return true;
            } else {
                // It ain't hovered, don't do the thing.
                return false;
            }
        };
        manualEjectButton.onRender = ((mS, mX, mY) -> {
            // Custom rendering.
            if (manualEjectButton.isMouseOver(mX, mY)) {
                // Mouse is hovering, highlight it.
                manualEjectButton.blit(mS, 241, 0);
            } else {
                // It ain't hovered, don't highlight.
                manualEjectButton.blit(mS, 226, 0);
            }
        });
        // TODO: Remove with reactor manual eject.
        // This element will be removed soon, in favor of having the eject button be in the access ports.
        //screen.addElement(manualEjectButton);
    }

    /**
     * Initialize common/shared gauges.
     *
     * @param screen The screen to initialize.
     */
    public static void initGauges(@Nonnull PhosphophylliteScreen<ReactorTerminalContainer> screen, ReactorState reactorState) {
        // (Top) Fuel mix gauge:
        RenderedElement<ReactorTerminalContainer> fuelMixGauge = new RenderedElement<>(screen, 85, 25, 18, 64, 0, 152, Component.empty());
        fuelMixGauge.onRender = (@Nonnull PoseStack mS, int mX, int mY) -> CommonReactorTerminalScreen.renderFuelMixGauge(mS, fuelMixGauge, reactorState.wasteStored, reactorState.fuelStored, reactorState.fuelCapacity);
        screen.addScreenElement(fuelMixGauge);

        // (Top) Case heat gauge:
        RenderedElement<ReactorTerminalContainer> caseHeatGauge = new RenderedElement<>(screen, 107, 25, 18, 64, 0, 152, Component.empty());
        caseHeatGauge.onRender = (@Nonnull PoseStack mS, int mX, int mY) -> CommonReactorTerminalScreen.renderHeatGauge(mS, caseHeatGauge, reactorState.caseHeatStored, Config.CONFIG.Reactor.GUI.HeatDisplayMax);
        screen.addScreenElement(caseHeatGauge);

        // (Top) Fuel heat gauge:
        RenderedElement<ReactorTerminalContainer> fuelHeatGauge = new RenderedElement<>(screen, 129, 25, 18, 64, 0, 152, Component.empty());
        fuelHeatGauge.onRender = (@Nonnull PoseStack mS, int mX, int mY) -> CommonReactorTerminalScreen.renderHeatGauge(mS, fuelHeatGauge, reactorState.fuelHeatStored, Config.CONFIG.Reactor.GUI.HeatDisplayMax);
        screen.addScreenElement(fuelHeatGauge);
    }

    /**
     * Initialize common/shared symbols.
     *
     * @param screen The screen to initialize.
     */
    public static void initSymbols(@Nonnull PhosphophylliteScreen<ReactorTerminalContainer> screen, ReactorState reactorState) {
        // None (yet).
    }

    /**
     * Render a reactor heat gauge.
     *
     * @param poseStack    The current pose stack.
     * @param symbol       The symbol to draw as.
     * @param heatStored   The heat value to draw.
     * @param heatCapacity The max heat capacity this gauge can display.
     */
    public static void renderHeatGauge(@Nonnull PoseStack poseStack, @Nonnull RenderedElement<ReactorTerminalContainer> symbol, double heatStored, double heatCapacity) {
        // If there's no heat, there's no need to draw.
        if ((heatStored > 0) && (heatCapacity > 0)) {
            // Calculate how much needs to be rendered.
            int renderSize = (int) ((symbol.height * heatStored) / heatCapacity);
            // Render heat.
            symbol.blit(poseStack, symbol.u + 72, symbol.v);
            // Render backdrop/mask away extra heat.
            symbol.blit(poseStack, symbol.width, symbol.height - renderSize, symbol.u + 18, symbol.v);
        }
        // Draw frame.
        symbol.blit(poseStack);
        // Update tooltip.
        symbol.tooltip = Component.literal(String.format("%.1f/%.1f K", heatStored, heatCapacity));
    }

    /**
     * Render a reactor fuel mix gauge.
     *
     * @param poseStack    The current pose stack.
     * @param symbol       The symbol to draw as.
     * @param wasteStored  The waste value to draw.
     * @param fuelStored   The fuel value to draw.
     * @param fuelCapacity The max fuel capacity this gauge can display.
     */
    public static void renderFuelMixGauge(@Nonnull PoseStack poseStack, @Nonnull RenderedElement<ReactorTerminalContainer> symbol, double wasteStored, double fuelStored, double fuelCapacity) {
        // If there's no fuel or waste, there's no need to draw.
        if ((wasteStored > 0 || fuelStored > 0) && (fuelCapacity > 0)) {
            // Calculate how much needs to be rendered.
            int wasteRenderSize = (int) ((symbol.height * wasteStored) / fuelCapacity);
            int fuelRenderSize = (int) ((symbol.height * fuelStored) / fuelCapacity);
            // Render waste.
            symbol.blit(poseStack, symbol.u + 54, symbol.v);
            // Render fuel on top of waste.
            symbol.blit(poseStack, symbol.width, symbol.height - (wasteRenderSize), symbol.u + 36, symbol.v);
            // Render backdrop/mask away extra waste and fuel.
            symbol.blit(poseStack, symbol.width, symbol.height - (wasteRenderSize + fuelRenderSize), symbol.u + 18, symbol.v);
        }
        // Draw frame.
        symbol.blit(poseStack);
        // Update tooltip.
        symbol.tooltip = Component.literal(String.format(
                Component.translatable("screen.biggerreactors.reactor_terminal.fuel_mix_gauge.tooltip").getString(),
                RenderHelper.formatValue((fuelStored + wasteStored) / 1000.0, "B", true),
                RenderHelper.formatValue(fuelCapacity / 1000.0, "B", true),
                RenderHelper.formatValue(fuelStored / 1000.0, "B", true),
                RenderHelper.formatValue(wasteStored / 1000.0, "B", true)));
    }

    /**
     * Render status text.
     *
     * @param poseStack       The current pose stack.
     * @param reactorActivity The reactor status to draw.
     * @param doAutoEject     The auto-eject status to draw.
     * @param heatStored      The heat value to draw.
     * @param fuelUsageRate   The fuel usage rate to draw.
     * @param reactivityRate  The reactivity rate to draw.
     * @implNote Output rate is not rendered by this function, since it changes depending on reactor type. Do that yourself.
     */
    public static void renderStatusText(@Nonnull PoseStack poseStack, @Nonnull PhosphophylliteScreen<ReactorTerminalContainer> screen, ReactorActivity reactorActivity, boolean doAutoEject, double heatStored, double fuelUsageRate, double reactivityRate) {
        // Render text for reactor temperature (no fancy suffix for Celsius):
        screen.getFont().draw(poseStack, String.format("%.0f K", heatStored), screen.getGuiLeft() + 27, screen.getGuiTop() + 23, 4210752);

        // Render text for fuel consumption rate:
        screen.getFont().draw(poseStack, RenderHelper.formatValue((fuelUsageRate / 1000.0), 3, "B/t", true), screen.getGuiLeft() + 27, screen.getGuiTop() + 61, 4210752);

        // Render text for reactivity rate (no fancy suffix for percentages):
        screen.getFont().draw(poseStack, String.format("%.1f%%", (reactivityRate * 100.0)), screen.getGuiLeft() + 27, screen.getGuiTop() + 80, 4210752);

        // Render text for online/offline status:
        if (reactorActivity == ReactorActivity.ACTIVE) {
            // Text for an online reactor:
            screen.getFont().draw(poseStack, Component.translatable("screen.biggerreactors.reactor_terminal.activity_toggle.online").getString(), screen.getGuiLeft() + 42, screen.getGuiTop() + 102, 4210752);

        } else {
            // Text for an offline reactor:
            screen.getFont().draw(poseStack, Component.translatable("screen.biggerreactors.reactor_terminal.activity_toggle.offline").getString(), screen.getGuiLeft() + 42, screen.getGuiTop() + 102, 4210752);
        }

        // Render text for auto-eject status:
        if (doAutoEject) {
            // Text for enabled auto-ejection:
            screen.getFont().draw(poseStack, Component.translatable("screen.biggerreactors.reactor_terminal.auto_eject_toggle.enabled").getString(), screen.getGuiLeft() + 42, screen.getGuiTop() + 118, 4210752);
        } else {
            // Text for disabled auto-ejection:
            screen.getFont().draw(poseStack, Component.translatable("screen.biggerreactors.reactor_terminal.auto_eject_toggle.disabled").getString(), screen.getGuiLeft() + 42, screen.getGuiTop() + 118, 4210752);
        }

        // Render text for manual eject button:
        // TODO: Remove with reactor manual eject.
        //screen.getFont().drawString(mStack, Component.translatable("screen.biggerreactors.reactor_terminal.manual_eject").getString(), screen.getGuiLeft() + 26, screen.getGuiTop() + 134, 4210752);
    }
}
