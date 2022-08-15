package net.roguelogix.biggerreactors.multiblocks.reactor.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.roguelogix.biggerreactors.BiggerReactors;
import net.roguelogix.biggerreactors.client.CommonButton;
import net.roguelogix.biggerreactors.client.TextBox;
import net.roguelogix.biggerreactors.fluids.LiquidUranium;
import net.roguelogix.biggerreactors.multiblocks.reactor.containers.ReactorControlRodContainer;
import net.roguelogix.biggerreactors.multiblocks.reactor.state.ReactorControlRodState;
import net.roguelogix.phosphophyllite.client.gui.screens.PhosphophylliteScreen;
import net.roguelogix.phosphophyllite.client.gui.RenderHelper;
import net.roguelogix.phosphophyllite.client.gui.elements.InteractiveElement;
import net.roguelogix.phosphophyllite.client.gui.elements.RenderedElement;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class ReactorControlRodScreen extends PhosphophylliteScreen<ReactorControlRodContainer> {

    private static final ResourceLocation DEFAULT_TEXTURE = new ResourceLocation(BiggerReactors.modid, "textures/screen/reactor_control_rod.png");

    private ReactorControlRodState reactorControlRodState;

    public ReactorControlRodScreen(ReactorControlRodContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title, DEFAULT_TEXTURE, 136, 126);

        // Initialize control rod state.
        reactorControlRodState = (ReactorControlRodState) this.getMenu().getGuiPacket();
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
        this.initGauges();

        // Initialize symbols:
    }

    /**
     * Initialize controls.
     */
    public void initControls() {
        // (Top) Name text box:
        TextBox<ReactorControlRodContainer> textBox = new TextBox<>(this, this.font, 6, 26, 96, 16, reactorControlRodState.name);
        this.addScreenElement(textBox);

        // (Top) Name text box enter button:
        InteractiveElement<ReactorControlRodContainer> textBoxEnterButton = new InteractiveElement<>(this, 114, 27, 17, 14, 194, 0, new TranslatableComponent("screen.biggerreactors.reactor_control_rod.apply.tooltip"));
        textBoxEnterButton.onMouseReleased = (mX, mY, btn) -> {
            // Click logic. Extra check necessary since this is an "in-class" button.
            if (textBoxEnterButton.isMouseOver(mX, mY)) {
                // Mouse is hovering, do the thing.
                this.getMenu().executeRequest("setName", textBox.getContents());
                // Play the selection sound.
                textBoxEnterButton.playSound(SoundEvents.UI_BUTTON_CLICK);
                return true;
            } else {
                // It ain't hovered, don't do the thing.
                return false;
            }
        };
        textBoxEnterButton.onRender = ((mS, mX, mY) -> {
            // Custom rendering.
            if (textBoxEnterButton.isMouseOver(mX, mY)) {
                // Mouse is hovering, highlight it.
                textBoxEnterButton.blit(mS, 211, 0);
            } else {
                // It ain't hovered, don't highlight.
                textBoxEnterButton.blit(mS, 194, 0);
            }
        });
        //this.addElement(textBoxEnterButton);

        // (Top) Name text box enter button:
        CommonButton<ReactorControlRodContainer> textEnterButton = new CommonButton<>(this, 114, 27, 17, 14, 61, 130, new TranslatableComponent("screen.biggerreactors.reactor_redstone_port.apply.tooltip"));
        textEnterButton.onMouseReleased = (mX, mY, btn) -> {
            // Click logic.
            this.getMenu().executeRequest("setName", textBox.getContents());
            return true;
        };
        this.addScreenElement(textEnterButton);

        // (Center) Rod retract button:
        InteractiveElement<ReactorControlRodContainer> rodRetractButton = new InteractiveElement<>(this, 58, 82, 14, 15, 226, 0, new TranslatableComponent("screen.biggerreactors.reactor_control_rod.retract_rod.tooltip"));
        rodRetractButton.onMouseReleased = (mX, mY, btn) -> {
            // Click logic. Extra check necessary since this is an "in-class" button.
            if (rodRetractButton.isMouseOver(mX, mY)) {
                // Calculate amount of change:
                double delta;
                if (Screen.hasShiftDown() && Screen.hasControlDown()) delta = -100D;
                else if (Screen.hasControlDown()) delta = -50D;
                else if (Screen.hasShiftDown()) delta = -10D;
                else delta = -1D;
                // Mouse is hovering, do the thing.
                this.getMenu().executeRequest("changeInsertionLevel", new Pair<>(delta, Screen.hasAltDown()));
                // Play the selection sound.
                rodRetractButton.playSound(SoundEvents.UI_BUTTON_CLICK);
                return true;
            } else {
                // It ain't hovered, don't do the thing.
                return false;
            }
        };
        rodRetractButton.onRender = ((mS, mX, mY) -> {
            // Custom rendering.
            if (rodRetractButton.isMouseOver(mX, mY)) {
                // Mouse is hovering, highlight it.
                rodRetractButton.blit(mS, 242, 0);
            } else {
                // It ain't hovered, don't highlight.
                rodRetractButton.blit(mS, 228, 0);
            }
        });
        this.addScreenElement(rodRetractButton);

        // (Center) Rod insert button:
        InteractiveElement<ReactorControlRodContainer> rodInsertButton = new InteractiveElement<>(this, 58, 64, 14, 15, 226, 0, new TranslatableComponent("screen.biggerreactors.reactor_control_rod.insert_rod.tooltip"));
        rodInsertButton.onMouseReleased = (mX, mY, btn) -> {
            // Click logic. Extra check necessary since this is an "in-class" button.
            if (rodInsertButton.isMouseOver(mX, mY)) {
                // Calculate amount of change:
                double delta;
                if (Screen.hasShiftDown() && Screen.hasControlDown()) delta = 100D;
                else if (Screen.hasControlDown()) delta = 50D;
                else if (Screen.hasShiftDown()) delta = 10D;
                else delta = 1D;
                // Mouse is hovering, do the thing.
                this.getMenu().executeRequest("changeInsertionLevel", new Pair<>(delta, Screen.hasAltDown()));
                // Play the selection sound.
                rodInsertButton.playSound(SoundEvents.UI_BUTTON_CLICK);
                return true;
            } else {
                // It ain't hovered, don't do the thing.
                return false;
            }
        };
        rodInsertButton.onRender = (mS, mX, mY) -> {
            // Custom rendering.
            if (rodInsertButton.isMouseOver(mX, mY)) {
                // Mouse is hovering, highlight it.
                rodInsertButton.blit(mS, 242, 15);
            } else {
                // It ain't hovered, don't highlight.
                rodInsertButton.blit(mS, 228, 15);
            }
        };
        this.addScreenElement(rodInsertButton);
    }

    /**
     * Initialize gauges.
     */
    public void initGauges() {
        // (Center) Control rod insertion gauge:
        RenderedElement<ReactorControlRodContainer> rodInsertionGauge = new RenderedElement<>(this, 36, 50, 18, 64, 0, 126, TextComponent.EMPTY);
        rodInsertionGauge.onRender = (@Nonnull PoseStack mS, int mX, int mY) -> ReactorControlRodScreen.renderInsertionLevel(mS, rodInsertionGauge, this.reactorControlRodState.insertionLevel);
        this.addScreenElement(rodInsertionGauge);
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

        // Render text for text box:
        this.font.draw(poseStack, new TranslatableComponent("screen.biggerreactors.reactor_control_rod.name").getString(), this.getGuiLeft() + 8, this.getGuiTop() + 17, 4210752);

        // Render text for insertion level:
        this.font.draw(poseStack, String.format("%.1f%%", reactorControlRodState.insertionLevel), this.getGuiLeft() + 76, this.getGuiTop() + 77, 4210752);
    }

    /**
     * Render a reactor fuel mix gauge.
     *
     * @param poseStack      The current pose stack.
     * @param symbol         The symbol to draw as.
     * @param insertionLevel How far the control rod is inserted. 0 is no insertion, 100 is full insertion.
     */
    public static void renderInsertionLevel(@Nonnull PoseStack poseStack, @Nonnull RenderedElement<ReactorControlRodContainer> symbol, double insertionLevel) {
        // Render fuel background. Offset by 1, otherwise it doesn't align with the frame.
        RenderHelper.drawFluidGrid(poseStack, symbol.x + 1, symbol.y, symbol.getBlitOffset(), 16, 16, LiquidUranium.INSTANCE.getSource(), 1, 4);

        // If there's nothing inserted, there's no need to draw.
        if (insertionLevel > 0) {
            // Calculate how much needs to be rendered.
            int renderSize = (int) ((symbol.height * insertionLevel) / 100.0D);
            // Render rod. This is done differently than other bars since this renders top-down, rather than bottom-up.
            symbol.blit(poseStack, symbol.width, renderSize, symbol.u + 18, symbol.v);
        }
        // Draw frame.
        symbol.blit(poseStack);
        // Update tooltip.
        symbol.tooltip = new TextComponent(String.format("%.1f%%", insertionLevel));
    }
}
