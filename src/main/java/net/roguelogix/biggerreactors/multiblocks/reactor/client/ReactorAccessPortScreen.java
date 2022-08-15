package net.roguelogix.biggerreactors.multiblocks.reactor.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.roguelogix.biggerreactors.BiggerReactors;
import net.roguelogix.biggerreactors.client.Biselector;
import net.roguelogix.biggerreactors.client.SelectorColors;
import net.roguelogix.biggerreactors.multiblocks.reactor.containers.ReactorAccessPortContainer;
import net.roguelogix.biggerreactors.multiblocks.reactor.state.ReactorAccessPortState;
import net.roguelogix.phosphophyllite.client.gui.screens.PhosphophylliteScreen;
import net.roguelogix.phosphophyllite.client.gui.elements.InteractiveElement;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class ReactorAccessPortScreen extends PhosphophylliteScreen<ReactorAccessPortContainer> {

    private static final ResourceLocation DEFAULT_TEXTURE = new ResourceLocation(BiggerReactors.modid, "textures/screen/reactor_access_port.png");

    private ReactorAccessPortState reactorAccessPortState;

    public ReactorAccessPortScreen(ReactorAccessPortContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title, DEFAULT_TEXTURE, 142, 72);

        // Initialize access port state.
        reactorAccessPortState = (ReactorAccessPortState) this.getMenu().getGuiPacket();
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
        Biselector<ReactorAccessPortContainer> directionToggle = new Biselector<>(this, 8, 18, new TranslatableComponent("screen.biggerreactors.reactor_access_port.direction_toggle.tooltip"),
                () -> reactorAccessPortState.direction ? 0 : 1, SelectorColors.YELLOW, SelectorColors.BLUE);
        directionToggle.onMouseReleased = (mX, mY, btn) -> {
            // Click logic.
            this.getMenu().executeRequest("setDirection", directionToggle.getState() == 0 ? 1 : 0);
            return true;
        };
        this.addScreenElement(directionToggle);

        // (Left) Fuel mode toggle:
        Biselector<ReactorAccessPortContainer> fuelModeToggle = new Biselector<>(this, 8, 34, new TranslatableComponent("screen.biggerreactors.reactor_access_port.fuel_mode_toggle.tooltip"),
                () -> reactorAccessPortState.fuelMode ? 1 : 0, SelectorColors.CYAN, SelectorColors.YELLOW);
        fuelModeToggle.onMouseReleased = (mX, mY, btn) -> {
            // Click logic.
            this.getMenu().executeRequest("setFuelMode", fuelModeToggle.getState() == 0 ? 1 : 0);
            return true;
        };
        fuelModeToggle.onTick = () -> {
            // Check if the element should be enabled.
            fuelModeToggle.actionEnable = (directionToggle.getState() != 0);
        };
        this.addScreenElement(fuelModeToggle);

        // (Left) Manual eject button:
        InteractiveElement<ReactorAccessPortContainer> manualEjectButton = new InteractiveElement<>(this, 8, 50, 15, 15, 226, 0, new TranslatableComponent("screen.biggerreactors.reactor_access_port.manual_eject.tooltip"));
        manualEjectButton.onMouseReleased = (mX, mY, btn) -> {
            // Click logic. Extra check necessary since this is an "in-class" button.
            if (manualEjectButton.isMouseOver(mX, mY)) {
                // Mouse is hovering, do the thing.
                this.getMenu().executeRequest("ejectWaste", true);
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
    public void render(@Nonnull PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        super.render(poseStack, mouseX, mouseY, partialTicks);

        // Render text for input/output direction:
        if (reactorAccessPortState.direction) {
            // Text for an inlet:
            this.getFont().draw(poseStack, new TranslatableComponent("screen.biggerreactors.reactor_access_port.direction_toggle.input").getString(), this.getGuiLeft() + 42, this.getGuiTop() + 22, 4210752);

        } else {
            // Text for an outlet:
            this.getFont().draw(poseStack, new TranslatableComponent("screen.biggerreactors.reactor_access_port.direction_toggle.output").getString(), this.getGuiLeft() + 42, this.getGuiTop() + 22, 4210752);
        }

        // Check if we render output type:
        if (!reactorAccessPortState.direction) {
            // Render text for fuel/waste mode:
            if (reactorAccessPortState.fuelMode) {
                // Text for an inlet:
                this.getFont().draw(poseStack, new TranslatableComponent("screen.biggerreactors.reactor_access_port.fuel_mode_toggle.fuel").getString(), this.getGuiLeft() + 42, this.getGuiTop() + 38, 4210752);

            } else {
                // Text for an outlet:
                this.getFont().draw(poseStack, new TranslatableComponent("screen.biggerreactors.reactor_access_port.fuel_mode_toggle.waste").getString(), this.getGuiLeft() + 42, this.getGuiTop() + 38, 4210752);
            }
        } else {
            // Text for no output:
            this.getFont().draw(poseStack, new TranslatableComponent("screen.biggerreactors.reactor_access_port.fuel_mode_toggle.nope").getString(), this.getGuiLeft() + 42, this.getGuiTop() + 38, 4210752);
        }

        // Render text for manual waste eject:
        this.getFont().draw(poseStack, new TranslatableComponent("screen.biggerreactors.reactor_access_port.manual_eject").getString(), this.getGuiLeft() + 26, this.getGuiTop() + 54, 4210752);
    }
}