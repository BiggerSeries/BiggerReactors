package net.roguelogix.biggerreactors.multiblocks.heatexchanger.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.roguelogix.biggerreactors.BiggerReactors;
import net.roguelogix.biggerreactors.client.Biselector;
import net.roguelogix.biggerreactors.client.SelectorColors;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.gui.container.HeatExchangerCoolantPortContainer;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.state.HeatExchangerCoolantPortState;
import net.roguelogix.phosphophyllite.client.gui.screens.PhosphophylliteScreen;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class HeatExchangerCoolantPortScreen extends PhosphophylliteScreen<HeatExchangerCoolantPortContainer> {

    private static final ResourceLocation DEFAULT_TEXTURE = new ResourceLocation(BiggerReactors.modid, "textures/screen/heat_exchanger_coolant_port.png");

    private HeatExchangerCoolantPortState heatExchangerCoolantPortState;

    public HeatExchangerCoolantPortScreen(HeatExchangerCoolantPortContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title, DEFAULT_TEXTURE, 176, 52);

        // Initialize access port state.
        heatExchangerCoolantPortState = (HeatExchangerCoolantPortState) this.getMenu().getGuiPacket();
    }

    /**
     * Initialize the screen.
     */
    @Override
    public void init() {
        super.init();

        // Set title to be drawn in the center.
        this.titleLabelX = (this.getWidth() / 2) - (this.getFont().width(this.getTitle()) / 2);

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
        Biselector<HeatExchangerCoolantPortContainer> directionToggle = new Biselector<>(this, 8, 18, new TranslatableComponent("screen.biggerreactors.heat_exchanger_coolant_port.direction_toggle.tooltip"),
                () -> heatExchangerCoolantPortState.direction ? 0 : 1, SelectorColors.CYAN, SelectorColors.RED);
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
    public void render(@Nonnull PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        super.render(poseStack, mouseX, mouseY, partialTicks);

        // Render text to show what channel this is connected to.
        if (heatExchangerCoolantPortState.condenser) {
            // Text for when connected to a condenser:
            this.getFont().draw(poseStack, new TranslatableComponent("screen.biggerreactors.heat_exchanger_coolant_port.channel_type.condenser").getString(), this.getGuiLeft() + 8, this.getGuiTop() + 37, 4210752);
        } else {
            // Text for when connected to an evaporator.
            this.getFont().draw(poseStack, new TranslatableComponent("screen.biggerreactors.heat_exchanger_coolant_port.channel_type.evaporator").getString(), this.getGuiLeft() + 8, this.getGuiTop() + 37, 4210752);
        }

        // Render text for input/output direction:
        if (heatExchangerCoolantPortState.direction) {
            // Text for an inlet:
            this.getFont().draw(poseStack, new TranslatableComponent("screen.biggerreactors.heat_exchanger_coolant_port.direction_toggle.input").getString(), this.getGuiLeft() + 42, this.getGuiTop() + 22, 4210752);
        } else {
            // Text for an outlet:
            this.getFont().draw(poseStack, new TranslatableComponent("screen.biggerreactors.heat_exchanger_coolant_port.direction_toggle.output").getString(), this.getGuiLeft() + 42, this.getGuiTop() + 22, 4210752);
        }
    }
}
