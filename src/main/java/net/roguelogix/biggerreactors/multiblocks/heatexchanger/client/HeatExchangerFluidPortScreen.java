package net.roguelogix.biggerreactors.multiblocks.heatexchanger.client;

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
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.containers.HeatExchangerFluidPortContainer;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.state.HeatExchangerFluidPortState;
import net.roguelogix.biggerreactors.multiblocks.reactor.containers.ReactorCoolantPortContainer;
import net.roguelogix.phosphophyllite.client.gui.elements.InteractiveElement;
import net.roguelogix.phosphophyllite.client.gui.screens.PhosphophylliteScreen;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class HeatExchangerFluidPortScreen extends PhosphophylliteScreen<HeatExchangerFluidPortContainer> {

    private static final ResourceLocation DEFAULT_TEXTURE = new ResourceLocation(BiggerReactors.modid, "textures/screen/heat_exchanger_fluid_port.png");

    private HeatExchangerFluidPortState heatExchangerFluidPortState;

    public HeatExchangerFluidPortScreen(HeatExchangerFluidPortContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title, DEFAULT_TEXTURE, 176, 72);

        // Initialize access port state.
        heatExchangerFluidPortState = (HeatExchangerFluidPortState) this.getMenu().getGuiPacket();
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
        Biselector<HeatExchangerFluidPortContainer> directionToggle = new Biselector<>(this, 8, 33, Component.translatable("screen.biggerreactors.heat_exchanger_fluid_port.direction_toggle.tooltip"),
                () -> heatExchangerFluidPortState.direction ? 0 : 1, SelectorColors.RED, SelectorColors.BLUE);
        directionToggle.onMouseReleased = (mX, mY, btn) -> {
            // Click logic.
            this.getMenu().executeRequest("setDirection", directionToggle.getState() == 0 ? 1 : 0);
            return true;
        };
        this.addScreenElement(directionToggle);

        // (Left) Manual dump button:
        InteractiveElement<HeatExchangerFluidPortContainer> manualEjectButton = new InteractiveElement<>(this, 8, 49, 15, 15, 226, 0, Component.translatable("screen.biggerreactors.heat_exchanger_fluid_port.manual_dump.tooltip"));
        manualEjectButton.onMouseReleased = (mX, mY, btn) -> {
            // Click logic. Extra check necessary since this is an "in-class" button.
            if (manualEjectButton.isMouseOver(mX, mY)) {
                // Mouse is hovering, do the thing.
                this.getMenu().executeRequest("dumpTanks", this.heatExchangerFluidPortState.condenser);
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

        // Render text to show what channel this is connected to.
        if (heatExchangerFluidPortState.condenser) {
            // Text for when connected to a condenser:
            graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactors.heat_exchanger_fluid_port.channel_type.condenser").getString(), this.getGuiLeft() + 8, this.getGuiTop() + 22, 4210752);
        } else {
            // Text for when connected to an evaporator.
            graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactors.heat_exchanger_fluid_port.channel_type.evaporator").getString(), this.getGuiLeft() + 8, this.getGuiTop() + 22, 4210752);
        }

        // Render text for input/output direction:
        if (heatExchangerFluidPortState.direction) {
            // Text for an inlet:
            graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactors.heat_exchanger_fluid_port.direction_toggle.input").getString(), this.getGuiLeft() + 42, this.getGuiTop() + 37, 4210752);
        } else {
            // Text for an outlet:
            graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactors.heat_exchanger_fluid_port.direction_toggle.output").getString(), this.getGuiLeft() + 42, this.getGuiTop() + 37, 4210752);
        }

        // Render text for manual tank eject:
        graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactors.heat_exchanger_fluid_port.manual_dump").getString(), this.getGuiLeft() + 26, this.getGuiTop() + 53, 4210752);
    }
}
