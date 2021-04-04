package net.roguelogix.biggerreactors.multiblocks.heatexchanger.gui.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.roguelogix.biggerreactors.BiggerReactors;
import net.roguelogix.biggerreactors.client.Biselector;
import net.roguelogix.biggerreactors.client.SelectorColors;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.gui.container.HeatExchangerCoolantPortContainer;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.state.HeatExchangerCoolantPortState;
import net.roguelogix.phosphophyllite.gui.client.ScreenBase;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class HeatExchangerCoolantPortScreen extends ScreenBase<HeatExchangerCoolantPortContainer> {

    private static final ResourceLocation DEFAULT_TEXTURE = new ResourceLocation(BiggerReactors.modid, "textures/screen/heat_exchanger_coolant_port.png");

    private HeatExchangerCoolantPortState heatExchangerCoolantPortState;

    public HeatExchangerCoolantPortScreen(HeatExchangerCoolantPortContainer container, PlayerInventory playerInventory, ITextComponent title) {
        super(container, playerInventory, title, DEFAULT_TEXTURE, 176, 52);

        // Initialize access port state.
        heatExchangerCoolantPortState = (HeatExchangerCoolantPortState) this.getContainer().getGuiPacket();
    }

    /**
     * Initialize the screen.
     */
    @Override
    public void init() {
        super.init();

        // Set title to be drawn in the center.
        this.titleX = (this.getWidth() / 2) - (this.getFont().getStringPropertyWidth(this.getTitle()) / 2);

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
        Biselector<HeatExchangerCoolantPortContainer> directionToggle = new Biselector<>(this, 8, 18, new TranslationTextComponent("screen.biggerreactors.heat_exchanger_coolant_port.direction_toggle.tooltip"),
                () -> heatExchangerCoolantPortState.direction ? 0 : 1, SelectorColors.CYAN, SelectorColors.RED);
        directionToggle.onMouseReleased = (mX, mY, btn) -> {
            // Click logic.
            this.getContainer().executeRequest("setDirection", directionToggle.getState() == 0 ? 1 : 0);
            return true;
        };
        this.addElement(directionToggle);
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

        // Render text to show what channel this is connected to.
        if (heatExchangerCoolantPortState.condenser) {
            // Text for when connected to a condenser:
            this.getFont().drawString(mStack, new TranslationTextComponent("screen.biggerreactors.heat_exchanger_coolant_port.channel_type.condenser").getString(), this.getGuiLeft() + 8, this.getGuiTop() + 37, 4210752);
        } else {
            // Text for when connected to an evaporator.
            this.getFont().drawString(mStack, new TranslationTextComponent("screen.biggerreactors.heat_exchanger_coolant_port.channel_type.evaporator").getString(), this.getGuiLeft() + 8, this.getGuiTop() + 37, 4210752);
        }

        // Render text for input/output direction:
        if (heatExchangerCoolantPortState.direction) {
            // Text for an inlet:
            this.getFont().drawString(mStack, new TranslationTextComponent("screen.biggerreactors.heat_exchanger_coolant_port.direction_toggle.input").getString(), this.getGuiLeft() + 42, this.getGuiTop() + 22, 4210752);
        } else {
            // Text for an outlet:
            this.getFont().drawString(mStack, new TranslationTextComponent("screen.biggerreactors.heat_exchanger_coolant_port.direction_toggle.output").getString(), this.getGuiLeft() + 42, this.getGuiTop() + 22, 4210752);
        }
    }
}
