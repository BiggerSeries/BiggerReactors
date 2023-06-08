package net.roguelogix.biggerreactors.multiblocks.turbine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.roguelogix.biggerreactors.BiggerReactors;
import net.roguelogix.biggerreactors.client.Biselector;
import net.roguelogix.biggerreactors.client.SelectorColors;
import net.roguelogix.biggerreactors.multiblocks.turbine.containers.TurbineFluidPortContainer;
import net.roguelogix.biggerreactors.multiblocks.turbine.state.TurbineFluidPortState;
import net.roguelogix.phosphophyllite.client.gui.screens.PhosphophylliteScreen;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class TurbineFluidPortScreen extends PhosphophylliteScreen<TurbineFluidPortContainer> {

    private static final ResourceLocation DEFAULT_TEXTURE = new ResourceLocation(BiggerReactors.modid, "textures/screen/turbine_fluid_port.png");

    private TurbineFluidPortState turbineFluidPortState;

    public TurbineFluidPortScreen(TurbineFluidPortContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title, DEFAULT_TEXTURE, 142, 40);

        // Initialize access port state.
        turbineFluidPortState = (TurbineFluidPortState) this.getMenu().getGuiPacket();
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

        // Initialize controls:
        this.initControls();

        // Initialize gauges:

        // Initialize symbols:
    }

    /**
     * Initialize controls.
     */
    public void initControls() {
        // (Left) Direction toggle:
        Biselector<TurbineFluidPortContainer> directionToggle = new Biselector<>(this, 8, 18, Component.translatable("screen.biggerreactors.turbine_fluid_port.direction_toggle.tooltip"),
                () -> turbineFluidPortState.direction ? 0 : 1, SelectorColors.RED, SelectorColors.BLUE);
        directionToggle.onMouseReleased = (mX, mY, btn) -> {
            // Click logic.
            this.getMenu().executeRequest("setDirection", directionToggle.getState() == 0 ? 1 : 0);
            return true;
        };
        this.addScreenElement(directionToggle);
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

        // Render text for input/output direction:
        if (turbineFluidPortState.direction) {
            // Text for an inlet:
            graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactors.turbine_fluid_port.direction_toggle.input").getString(), this.getGuiLeft() + 42, this.getGuiTop() + 22, 4210752);

        } else {
            // Text for an outlet:
            graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactors.turbine_fluid_port.direction_toggle.output").getString(), this.getGuiLeft() + 42, this.getGuiTop() + 22, 4210752);
        }
    }
}
