package net.roguelogix.biggerreactors.multiblocks.reactor.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.roguelogix.biggerreactors.BiggerReactors;
import net.roguelogix.biggerreactors.multiblocks.reactor.containers.ReactorTerminalContainer;
import net.roguelogix.biggerreactors.multiblocks.reactor.state.ReactorState;
import net.roguelogix.biggerreactors.multiblocks.reactor.state.ReactorType;
import net.roguelogix.biggerreactors.client.CommonRender;
import net.roguelogix.phosphophyllite.gui.client.RenderHelper;
import net.roguelogix.phosphophyllite.gui.client.ScreenBase;
import net.roguelogix.phosphophyllite.gui.client.elements.Symbol;
import net.roguelogix.phosphophyllite.gui.client.elements.Tooltip;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class PassiveReactorTerminalScreen extends ScreenBase<ReactorTerminalContainer> {

    private static final ResourceLocation DEFAULT_TEXTURE = new ResourceLocation(BiggerReactors.modid, "textures/screen/reactor_terminal_passive.png");

    private ReactorState reactorState;

    public PassiveReactorTerminalScreen(ReactorTerminalContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title, DEFAULT_TEXTURE, 176, 152);

        // Initialize reactor state.
        reactorState = (ReactorState) this.getMenu().getGuiPacket();
    }

    /**
     * Initialize the screen.
     */
    @Override
    public void init() {
        super.init();

        // Initialize tooltips:
        CommonReactorTerminalScreen.initTooltips(this, reactorState);
        this.initTooltips();

        // Initialize controls:
        CommonReactorTerminalScreen.initControls(this, reactorState);

        // Initialize gauges:
        CommonReactorTerminalScreen.initGauges(this, reactorState);
        this.initGauges();

        // Initialize symbols:
        CommonReactorTerminalScreen.initSymbols(this, reactorState);
    }

    /**
     * Initialize tooltips.
     */
    private void initTooltips() {
        // (Left) RF generation rate tooltip:
        this.addElement(new Tooltip<>(this, 8, 38, 16, 16, new TranslatableComponent("screen.biggerreactors.reactor_terminal.energy_generation_rate.tooltip")));

        // (Left) RF generation readout tooltip:
        Tooltip<ReactorTerminalContainer> generationRateReadoutTooltip = new Tooltip<>(this, 26, 38, 53, 16, TextComponent.EMPTY);
        generationRateReadoutTooltip.onTick = () -> {
            generationRateReadoutTooltip.tooltip = new TextComponent(String.format("%.3f RF/t", this.reactorState.reactorOutputRate));
        };
        this.addElement(generationRateReadoutTooltip);

        // (Top) Internal battery tooltip:
        this.addElement(new Tooltip<>(this, 152, 6, 16, 16, new TranslatableComponent("screen.biggerreactors.reactor_terminal.internal_battery.tooltip")));
    }

    /**
     * Initialize gauges.
     */
    private void initGauges() {
        // (Top) Internal battery:
        Symbol<ReactorTerminalContainer> internalBattery = new Symbol<>(this, 151, 25, 18, 64, 0, 152, TextComponent.EMPTY);
        internalBattery.onRender = (@Nonnull PoseStack mS, int mX, int mY) -> CommonRender.renderEnergyGauge(mS,
                internalBattery, reactorState.energyStored, reactorState.energyCapacity);
        this.addElement(internalBattery);
    }

    /**
     * Tick/update this screen.
     */
    @Override
    public void containerTick() {
        // Update reactor state and tick.
        reactorState = (ReactorState) this.getMenu().getGuiPacket();
        super.containerTick();
        // Check if reactor type changed.
        if(reactorState.reactorType != ReactorType.PASSIVE) {
            this.getMinecraft().setScreen(new ActiveReactorTerminalScreen(this.menu, this.inventory, this.title));
        }
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

        // Render the other text:
        CommonReactorTerminalScreen.renderStatusText(mStack, this, reactorState.reactorActivity, reactorState.doAutoEject,
                reactorState.fuelHeatStored, reactorState.fuelUsageRate, reactorState.reactivityRate);

        // Render text for output rate:
        this.getFont().draw(mStack, RenderHelper.formatValue(reactorState.reactorOutputRate, "RF/t"), this.getGuiLeft() + 27, this.getGuiTop() + 42, 4210752);
    }
}