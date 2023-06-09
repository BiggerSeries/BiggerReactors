package net.roguelogix.biggerreactors.multiblocks.reactor.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.roguelogix.biggerreactors.BiggerReactors;
import net.roguelogix.biggerreactors.client.Biselector;
import net.roguelogix.biggerreactors.client.SelectorColors;
import net.roguelogix.biggerreactors.multiblocks.reactor.containers.ReactorCoolantPortContainer;
import net.roguelogix.biggerreactors.multiblocks.reactor.state.ReactorCoolantPortState;
import net.roguelogix.phosphophyllite.client.gui.elements.InteractiveElement;
import net.roguelogix.phosphophyllite.client.gui.screens.PhosphophylliteScreen;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class ReactorCoolantPortScreen extends PhosphophylliteScreen<ReactorCoolantPortContainer> {

    private static final ResourceLocation DEFAULT_TEXTURE = new ResourceLocation(BiggerReactors.modid, "textures/screen/reactor_coolant_port.png");

    private ReactorCoolantPortState reactorCoolantPortState;

    public ReactorCoolantPortScreen(ReactorCoolantPortContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title, DEFAULT_TEXTURE, 142, 56);

        // Initialize access port state.
        reactorCoolantPortState = (ReactorCoolantPortState) this.getMenu().getGuiPacket();
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
        Biselector<ReactorCoolantPortContainer> directionToggle = new Biselector<>(this, 8, 18, Component.translatable("screen.biggerreactors.reactor_coolant_port.direction_toggle.tooltip"),
                () -> reactorCoolantPortState.direction ? 0 : 1, SelectorColors.CYAN, SelectorColors.RED);
        directionToggle.onMouseReleased = (mX, mY, btn) -> {
            // Click logic.
            this.getMenu().executeRequest("setDirection", directionToggle.getState() == 0 ? 1 : 0);
            return true;
        };
        this.addScreenElement(directionToggle);

        // (Left) Manual dump button:
        InteractiveElement<ReactorCoolantPortContainer> manualEjectButton = new InteractiveElement<>(this, 8, 34, 15, 15, 226, 0, Component.translatable("screen.biggerreactors.reactor_coolant_port.manual_dump.tooltip"));
        manualEjectButton.onMouseReleased = (mX, mY, btn) -> {
            // Click logic. Extra check necessary since this is an "in-class" button.
            if (manualEjectButton.isMouseOver(mX, mY)) {
                // Mouse is hovering, do the thing.
                this.getMenu().executeRequest("dumpTanks", true);
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
        this.addScreenElement(manualEjectButton);
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
        if (reactorCoolantPortState.direction) {
            // Text for an inlet:
            graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactors.reactor_coolant_port.direction_toggle.input").getString(), this.getGuiLeft() + 42, this.getGuiTop() + 22, 4210752, false);

        } else {
            // Text for an outlet:
            graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactors.reactor_coolant_port.direction_toggle.output").getString(), this.getGuiLeft() + 42, this.getGuiTop() + 22, 4210752, false);
        }

        // Render text for manual tank eject:
        graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactors.reactor_coolant_port.manual_dump").getString(), this.getGuiLeft() + 26, this.getGuiTop() + 38, 4210752, false);
    }
}
