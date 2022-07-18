package net.roguelogix.biggerreactors.machine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluids;
import net.roguelogix.biggerreactors.BiggerReactors;
import net.roguelogix.biggerreactors.client.CommonRender;
import net.roguelogix.biggerreactors.machine.containers.CyaniteReprocessorContainer;
import net.roguelogix.biggerreactors.machine.state.CyaniteReprocessorState;
import net.roguelogix.phosphophyllite.client.gui.screens.PhosphophylliteScreen;
import net.roguelogix.phosphophyllite.client.gui.RenderHelper;
import net.roguelogix.phosphophyllite.client.gui.elements.RenderedElement;
import net.roguelogix.phosphophyllite.client.gui.elements.TooltipElement;

import javax.annotation.Nonnull;

public class CyaniteReprocessorScreen extends PhosphophylliteScreen<CyaniteReprocessorContainer> {

    private static final ResourceLocation DEFAULT_TEXTURE = new ResourceLocation(BiggerReactors.modid, "textures/screen/cyanite_reprocessor.png");

    private CyaniteReprocessorState cyaniteReprocessorState;

    public CyaniteReprocessorScreen(CyaniteReprocessorContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title, DEFAULT_TEXTURE, 176, 175);

        // Initialize reprocessor state.
        cyaniteReprocessorState = (CyaniteReprocessorState) this.getMenu().getGuiPacket();
    }

    /**
     * Initialize the screen.
     */
    @Override
    public void init() {
        super.init();

        // Set title to be drawn in the center.
        this.titleLabelX = (this.getXSize() / 2) - (this.getFont().width(this.getTitle()) / 2);

        // Initialize tooltips:
        this.initTooltips();

        // Initialize controls:

        // Initialize gauges:
        this.initGauges();

        // Initialize symbols:
        this.initSymbols();
    }

    /**
     * Initialize tooltips.
     */
    public void initTooltips() {
        // (Left) Internal battery:
        this.addScreenElement(new TooltipElement<>(this, 8, 6, 16, 16, Component.translatable("screen.biggerreactors.cyanite_reprocessor.internal_battery.tooltip")));
    }

    /**
     * Initialize gauges.
     */
    public void initGauges() {
        // (Top) Internal battery:
        RenderedElement<CyaniteReprocessorContainer> internalBattery = new RenderedElement<>(this, 7, 25, 18, 64, 0, 152, Component.empty());
        internalBattery.onRender = (@Nonnull PoseStack mS, int mX, int mY) -> CommonRender.renderEnergyGauge(mS,
                internalBattery, cyaniteReprocessorState.energyStored, cyaniteReprocessorState.energyCapacity);
        this.addScreenElement(internalBattery);

        // (Top) Water tank:
        RenderedElement<CyaniteReprocessorContainer> waterTank = new RenderedElement<>(this, 151, 25, 18, 64, 0, 152, Component.empty());
        waterTank.onRender = (@Nonnull PoseStack mS, int mX, int mY) -> CommonRender.renderFluidGauge(mS,
                waterTank, cyaniteReprocessorState.waterStored, cyaniteReprocessorState.waterCapacity,
                Fluids.WATER.getSource());
        this.addScreenElement(waterTank);

        // (Center) Progress bar:
        RenderedElement<CyaniteReprocessorContainer> progressBar = new RenderedElement<>(this, 75, 40, 24, 18, 0, 175, null);
        progressBar.onRender = (@Nonnull PoseStack mS, int mX, int mY) -> CyaniteReprocessorScreen.renderProgressBar(mS,
                progressBar, cyaniteReprocessorState.workTime, cyaniteReprocessorState.workTimeTotal);
        this.addScreenElement(progressBar);
    }

    /**
     * Initialize symbols.
     */
    public void initSymbols() {
        // (Right) Water tank symbol:
        RenderedElement<CyaniteReprocessorContainer> waterTankSymbol = new RenderedElement<>(this, 152, 6, 16, 16, 48, 175, Component.translatable("screen.biggerreactors.cyanite_reprocessor.water_tank.tooltip"));
        waterTankSymbol.onRender = (@Nonnull PoseStack mS, int mX, int mY) -> RenderHelper.drawMaskedFluid(mS,
                waterTankSymbol.x, waterTankSymbol.y, this.getBlitOffset(),
                waterTankSymbol.width, waterTankSymbol.height,
                waterTankSymbol.u, waterTankSymbol.v, Fluids.WATER.getSource());
        this.addScreenElement(waterTankSymbol);
    }

    /**
     * Render the progress bar.
     *
     * @param poseStack     The current pose stack.
     * @param symbol        The symbol to draw as.
     * @param workTime      The time the machine has been working.
     * @param workTimeTotal The total time needed for completion.
     */
    private static void renderProgressBar(@Nonnull PoseStack poseStack, @Nonnull RenderedElement<CyaniteReprocessorContainer> symbol, int workTime, int workTimeTotal) {
        // If there's no progress, there's no need to draw.
        if ((workTime > 0) && (workTimeTotal > 0)) {
            // Calculate how much needs to be rendered.
            int renderSize = (int) ((symbol.width * workTime) / workTimeTotal);
            // Render progress.
            symbol.blit(poseStack, renderSize, symbol.height, symbol.u + 24, symbol.v);
        }
    }
}
