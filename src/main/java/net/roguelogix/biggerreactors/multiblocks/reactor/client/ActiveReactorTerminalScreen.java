package net.roguelogix.biggerreactors.multiblocks.reactor.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import net.roguelogix.biggerreactors.BiggerReactors;
import net.roguelogix.biggerreactors.client.CommonRender;
import net.roguelogix.biggerreactors.multiblocks.reactor.containers.ReactorTerminalContainer;
import net.roguelogix.biggerreactors.multiblocks.reactor.state.ReactorActivity;
import net.roguelogix.biggerreactors.multiblocks.reactor.state.ReactorState;
import net.roguelogix.biggerreactors.multiblocks.reactor.state.ReactorType;
import net.roguelogix.phosphophyllite.client.gui.screens.PhosphophylliteScreen;
import net.roguelogix.phosphophyllite.client.gui.RenderHelper;
import net.roguelogix.phosphophyllite.client.gui.elements.RenderedElement;
import net.roguelogix.phosphophyllite.client.gui.elements.TooltipElement;

import javax.annotation.Nonnull;
import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public class ActiveReactorTerminalScreen extends PhosphophylliteScreen<ReactorTerminalContainer> {

    private static final ResourceLocation DEFAULT_TEXTURE = new ResourceLocation(BiggerReactors.modid, "textures/screen/reactor_terminal_active.png");

    private ReactorState reactorState;

    private final int screenWorkTimeTotal = 400;
    private int screenWorkTime = 0;

    private Fluid coolantFluid = Fluids.EMPTY;
    private Fluid exhaustFluid = Fluids.EMPTY;

    public ActiveReactorTerminalScreen(ReactorTerminalContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title, DEFAULT_TEXTURE, 198, 152);

        // Initialize reactor state.
        reactorState = (ReactorState) this.getMenu().getGuiPacket();
        coolantFluid = Registry.FLUID.get(new ResourceLocation(reactorState.coolantResourceLocation));
        exhaustFluid = Registry.FLUID.get(new ResourceLocation(reactorState.exhaustResourceLocation));
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
        this.initSymbols();
    }

    /**
     * Initialize tooltips.
     */
    private void initTooltips() {
        // (Left) Exhaust generation readout tooltip:
        TooltipElement<ReactorTerminalContainer> generationRateReadoutTooltip = new TooltipElement<>(this, 26, 38, 53, 16, Component.empty());
        generationRateReadoutTooltip.onTick = () -> {
            generationRateReadoutTooltip.tooltip = Component.literal(String.format("%.3f mB/t", this.reactorState.reactorOutputRate));
        };
        this.addScreenElement(generationRateReadoutTooltip);
    }

    /**
     * Initialize gauges.
     */
    private void initGauges() {
        // (Top) Coolant intake tank:
        RenderedElement<ReactorTerminalContainer> coolantIntakeTank = new RenderedElement<>(this, 151, 25, 18, 64, 0, 152, Component.empty());
        coolantIntakeTank.onRender = (@Nonnull PoseStack mS, int mX, int mY) -> CommonRender.renderFluidGauge(mS,
                coolantIntakeTank, reactorState.coolantStored, reactorState.coolantCapacity, coolantFluid);
        this.addScreenElement(coolantIntakeTank);

        // (Top) Hot exhaust tank:
        RenderedElement<ReactorTerminalContainer> hotExhaustTank = new RenderedElement<>(this, 173, 25, 18, 64, 0, 152, Component.empty());
        hotExhaustTank.onRender = (@Nonnull PoseStack mS, int mX, int mY) -> CommonRender.renderFluidGauge(mS,
                hotExhaustTank, reactorState.exhaustStored, reactorState.exhaustCapacity, exhaustFluid);
        this.addScreenElement(hotExhaustTank);

        // (Bottom) Progress bar:
        RenderedElement<ReactorTerminalContainer> progressBar = new RenderedElement<>(this, 173, 90, 18, 26, 90, 152, null);
        progressBar.onRender = (@Nonnull PoseStack mS, int mX, int mY) -> {
            // Custom rendering.
            if (reactorState.coolantStored > 0) {
                ActiveReactorTerminalScreen.renderProgressBar(mS, progressBar, reactorState.reactorActivity, screenWorkTime++, screenWorkTimeTotal, coolantFluid);
                if (screenWorkTime >= screenWorkTimeTotal) {
                    screenWorkTime = 0;
                }
            }
        };
        this.addScreenElement(progressBar);
    }

    /**
     * Initialize symbols.
     */
    private void initSymbols() {
        // (Top) Coolant intake tank symbol:
        RenderedElement<ReactorTerminalContainer> coolantIntakeTankSymbol = new RenderedElement<>(this, 152, 6, 16, 16, 174, 152, Component.translatable("screen.biggerreactors.reactor_terminal.coolant_intake_tank.tooltip"));
        coolantIntakeTankSymbol.onRender = (@Nonnull PoseStack mS, int mX, int mY) -> RenderHelper.drawMaskedFluid(mS,
                coolantIntakeTankSymbol.x, coolantIntakeTankSymbol.y, this.getBlitOffset(),
                coolantIntakeTankSymbol.width, coolantIntakeTankSymbol.height,
                coolantIntakeTankSymbol.u, coolantIntakeTankSymbol.v, coolantFluid);
        this.addScreenElement(coolantIntakeTankSymbol);

        // (Top) Hot exhaust tank symbol:
        RenderedElement<ReactorTerminalContainer> hotExhaustTankSymbol = new RenderedElement<>(this, 174, 6, 16, 16, 158, 152, Component.translatable("screen.biggerreactors.reactor_terminal.exhaust_tank.tooltip"));
        hotExhaustTankSymbol.onRender = (@Nonnull PoseStack mS, int mX, int mY) -> RenderHelper.drawMaskedFluid(mS,
                hotExhaustTankSymbol.x, hotExhaustTankSymbol.y, this.getBlitOffset(),
                hotExhaustTankSymbol.width, hotExhaustTankSymbol.height,
                hotExhaustTankSymbol.u, hotExhaustTankSymbol.v, exhaustFluid);
        this.addScreenElement(hotExhaustTankSymbol);

        // (Left) Exhaust generation rate symbol:
        RenderedElement<ReactorTerminalContainer> exhaustGenerationRateSymbol = new RenderedElement<>(this, 8, 38, 16, 16, 142, 152, Component.translatable("screen.biggerreactors.reactor_terminal.exhaust_generation_rate.tooltip"));
        exhaustGenerationRateSymbol.onRender = (@Nonnull PoseStack mS, int mX, int mY) -> RenderHelper.drawMaskedFluid(mS,
                exhaustGenerationRateSymbol.x, exhaustGenerationRateSymbol.y, this.getBlitOffset(),
                exhaustGenerationRateSymbol.width, exhaustGenerationRateSymbol.height,
                exhaustGenerationRateSymbol.u, exhaustGenerationRateSymbol.v, exhaustFluid);
        this.addScreenElement(exhaustGenerationRateSymbol);
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
        if (reactorState.reactorType != ReactorType.ACTIVE) {
            this.getMinecraft().setScreen(new PassiveReactorTerminalScreen(this.menu, this.inventory, this.title));
        }
        // Check if coolant type changed.
        if (!reactorState.coolantResourceLocation.equals(Objects.requireNonNull(ForgeRegistries.FLUIDS.getKey(coolantFluid)).toString())) {
            coolantFluid = Registry.FLUID.get(new ResourceLocation(reactorState.coolantResourceLocation));
        }
        // Check if exhaust type changed.
        if (!reactorState.exhaustResourceLocation.equals(Objects.requireNonNull(ForgeRegistries.FLUIDS.getKey(exhaustFluid)).toString())) {
            exhaustFluid = Registry.FLUID.get(new ResourceLocation(reactorState.exhaustResourceLocation));
        }
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
    public void render(@Nonnull PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        super.render(poseStack, mouseX, mouseY, partialTicks);

        // Render the other text:
        CommonReactorTerminalScreen.renderStatusText(poseStack, this, reactorState.reactorActivity, reactorState.doAutoEject,
                reactorState.fuelHeatStored, reactorState.fuelUsageRate, reactorState.reactivityRate);

        // Render text for output rate:
        this.getFont().draw(poseStack, RenderHelper.formatValue((reactorState.reactorOutputRate / 1000.0), "B/t"), this.getGuiLeft() + 27, this.getGuiTop() + 42, 4210752);
    }

    /**
     * Render the progress bar.
     *
     * @param poseStack       The current pose stack.
     * @param symbol          The symbol to draw as.
     * @param reactorActivity Current status of the reactor.
     * @param workTime        The time the machine has been working.
     * @param workTimeTotal   The total time needed for completion.
     */
    private static void renderProgressBar(@Nonnull PoseStack poseStack, @Nonnull RenderedElement<ReactorTerminalContainer> symbol, ReactorActivity reactorActivity, int workTime, int workTimeTotal, Fluid coolant) {
        // Check that the reactor is active. If not, reset work time.
        if (reactorActivity != ReactorActivity.ACTIVE) {
            workTime = 0;
        }
        // If there's no progress, there's no need to draw.
        // This isn't the way I wanted to render this bar, but it's vertical, so funky is as funky does.
        if ((workTime > 0) && (workTimeTotal > 0)) {
            // Calculate how much needs to be rendered.
            int renderSize = (int) ((symbol.height * workTime) / workTimeTotal);
            // Render progress.
            symbol.blit(poseStack, symbol.u + 18, symbol.v);
            // Render backdrop/mask away "not progress."
            symbol.blit(poseStack, symbol.width, symbol.height - renderSize, symbol.u, symbol.v);
        }
        // If work time exceeds work time total, then reset to 0.

        // Render the fluid "undertank."
        RenderHelper.drawMaskedFluid(poseStack, symbol.x + 1, symbol.y + 26, 0, 16, 16, symbol.u + 36, symbol.v, coolant);
    }
}
