package net.roguelogix.biggerreactors.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.roguelogix.phosphophyllite.client.gui.RenderHelper;
import net.roguelogix.phosphophyllite.client.gui.screens.PhosphophylliteScreen;
import net.roguelogix.phosphophyllite.client.gui.elements.InteractiveElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.IntSupplier;

@OnlyIn(Dist.CLIENT)
public class Triselector<T extends AbstractContainerMenu> extends InteractiveElement<T> {
    
    private final IntSupplier renderState;
    
    /**
     * Selector state for the switch.
     */
    private int state;

    /**
     * The color of the left selector.
     */
    private final SelectorColors leftColor;

    /**
     * The color of the center selector.
     */
    private final SelectorColors centerColor;

    /**
     * The color of the right selector.
     */
    private final SelectorColors rightColor;

    /**
     * Default constructor.
     *
     * @param parent       The parent screen of this element.
     * @param x            The x position of this element.
     * @param y            The y position of this element.
     * @param tooltip      The tooltip for this element. If null, a tooltip will not render. If you set a tooltip later, use StringTextComponent.EMPTY.
     * @param initialState The initial switch state to use.
     */
    public Triselector(@Nonnull PhosphophylliteScreen<T> parent, int x, int y, @Nullable Component tooltip, IntSupplier initialState, SelectorColors leftColor, SelectorColors centerColor, SelectorColors rightColor) {
        super(parent, x, y, 46, 14, 31, 64, tooltip);
        this.renderState = initialState;
        this.state = initialState.getAsInt();
        this.leftColor = leftColor;
        this.centerColor = centerColor;
        this.rightColor = rightColor;
    }

    /**
     * Get the current switch state.
     *
     * @return The current switch state.
     */
    public int getState() {
        return this.state;
    }

    /**
     * Render element.
     *
     * @param poseStack The current pose stack.
     * @param mouseX The x position of the mouse.
     * @param mouseY The y position of the mouse.
     */
    @Override
    public void render(@Nonnull PoseStack poseStack, int mouseX, int mouseY) {
        // Check conditions.
        if (this.renderEnable) {
            // Preserve the previously selected texture and bind the common texture.
            ResourceLocation preservedResource = RenderHelper.getCurrentResource();
            RenderHelper.bindTexture(CommonRender.COMMON_RESOURCE_TEXTURE);
            // Draw the selector frame.
            if (this.renderState.getAsInt() == 0) {
                // Position is 0 (left), draw left frame.
                this.blit(poseStack, 31, 64);
                // Check where the mouse is.
                if (this.isMouseOver(mouseX, mouseY)) {
                    // Draw active/hovered button.
                    if(this.actionEnable) {
                        this.blit(poseStack, this.x + 1, this.y + 1, leftColor.uA, leftColor.vA, 14, 12);
                    } else {
                        this.blit(poseStack, this.x + 1, this.y + 1, SelectorColors.DISABLED.uA, SelectorColors.DISABLED.vA, 14, 12);
                    }
                } else {
                    // Draw inactive/non-hovered button.
                    if(this.actionEnable) {
                        this.blit(poseStack, this.x + 1, this.y + 1, leftColor.uI, leftColor.vI, 14, 12);
                    } else {
                        this.blit(poseStack, this.x + 1, this.y + 1, SelectorColors.DISABLED.uI, SelectorColors.DISABLED.vI, 14, 12);
                    }
                }
            } else if (this.renderState.getAsInt() == 1) {
                // Position is 1 (center), draw center frame.
                this.blit(poseStack, 31, 78);
                // Check where the mouse is.
                if (this.isMouseOver(mouseX, mouseY)) {
                    // Draw active/hovered button.
                    if(this.actionEnable) {
                        this.blit(poseStack, this.x + 16, this.y + 1, centerColor.uA, centerColor.vA, 14, 12);
                    } else {
                        this.blit(poseStack, this.x + 16, this.y + 1, SelectorColors.DISABLED.uA, SelectorColors.DISABLED.vA, 14, 12);
                    }
                } else {
                    // Draw inactive/non-hovered button.
                    if(this.actionEnable) {
                        this.blit(poseStack, this.x + 16, this.y + 1, centerColor.uI, centerColor.vI, 14, 12);
                    } else {
                        this.blit(poseStack, this.x + 16, this.y + 1, SelectorColors.DISABLED.uI, SelectorColors.DISABLED.vI, 14, 12);
                    }
                }
            } else {
                // Position is 2 (right), draw right frame.
                this.blit(poseStack, 31, 92);
                // Check if the selector is enabled.
                // Check where the mouse is.
                if (this.isMouseOver(mouseX, mouseY)) {
                    // Draw active/hovered button.
                    if(this.actionEnable) {
                        this.blit(poseStack, this.x + 31, this.y + 1, rightColor.uA, rightColor.vA, 14, 12);
                    } else {
                        this.blit(poseStack, this.x + 31, this.y + 1, SelectorColors.DISABLED.uA, SelectorColors.DISABLED.vA, 14, 12);
                    }
                } else {
                    // Draw inactive/non-hovered button.
                    if(this.actionEnable) {
                        this.blit(poseStack, this.x + 31, this.y + 1, rightColor.uI, rightColor.vI, 14, 12);
                    } else {
                        this.blit(poseStack, this.x + 31, this.y + 1, SelectorColors.DISABLED.uI, SelectorColors.DISABLED.vI, 14, 12);
                    }
                }
            }
            // Reset color and restore the previously bound texture.
            RenderHelper.clearRenderColor();
            RenderHelper.bindTexture(preservedResource);
            // Trigger user-defined render logic.
            if (this.onRender != null) {
                this.onRender.trigger(poseStack, mouseX, mouseY);
            }
        }
    }

    /**
     * Triggered when the mouse is released.
     *
     * @param mouseX The x position of the mouse.
     * @param mouseY The y position of the mouse.
     * @param button The button clicked.
     * @return Whether the event was consumed.
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // Check conditions.
        if (this.isMouseOver(mouseX, mouseY)) {
            // Get actual x position.
            int relativeX = this.parent.getGuiLeft() + this.x;
            // Check if the selector is enabled.
            if (this.actionEnable) {
                // Set side depending on position of click.
                if (mouseX > relativeX) {
                    if ((mouseX < relativeX + (int) (this.width / 3))) {
                        // Set to left position (0).
                        this.state = 0;
                    } else if ((mouseX < relativeX + ((int) (this.width / 3) * 2))) {
                        // Set to center position (1).
                        this.state = 1;
                    } else {
                        // Set to right position (2).
                        this.state = 2;
                    }
                    // Play the selection sound.
                    this.playSound(SoundEvents.UI_BUTTON_CLICK);
                    // Trigger user-defined selection logic.
                    if (this.onMouseReleased != null) {
                        this.onMouseReleased.trigger(mouseX, mouseY, button);
                    }
                }
            }
            // The event was consumed.
            return true;
        } else {
            // The event was not consumed.
            return false;
        }
    }
}

