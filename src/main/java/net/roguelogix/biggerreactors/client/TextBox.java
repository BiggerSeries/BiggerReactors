package net.roguelogix.biggerreactors.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.roguelogix.biggerreactors.BiggerReactors;
import net.roguelogix.phosphophyllite.client.gui.screens.PhosphophylliteScreen;
import net.roguelogix.phosphophyllite.client.gui.RenderHelper;
import net.roguelogix.phosphophyllite.client.gui.elements.InteractiveElement;

import javax.annotation.Nonnull;

import static org.lwjgl.glfw.GLFW.*;

@OnlyIn(Dist.CLIENT)
public class TextBox<T extends AbstractContainerMenu> extends InteractiveElement<T> {

    /**
     * Whether or not the text box is in focus.
     */
    private boolean focusEnable;

    /**
     * Position of the cursor or selection end.
     */
    private int cursorPos;

    /**
     * Position of the selection start.
     */
    private int selectionPos;

    /**
     * Cursor flash animation timer.
     */
    private int cursorAnimationTime;

    /**
     * Cursor flash animation timer total. If time is more than half of this, it will be lit.
     */
    private final int cursorAnimationTimeTotal;

    /**
     * The max number of characters this can store.
     */
    private final int charLimit;

    /**
     * Text stored in the buffer.
     */
    private final StringBuffer textBuffer;

    /**
     * Instance of the font renderer to use.
     */
    private final Font fontRenderer;

    /**
     * Default constructor.
     *
     * @param parent    The parent screen of this element.
     * @param x         The x position of this element.
     * @param y         The y position of this element.
     * @param width     How wide this element should be.
     * @param charLimit The max number of characters to allow in this box.
     */
    public TextBox(@Nonnull PhosphophylliteScreen<T> parent, @Nonnull Font fontRenderer, int x, int y, int width, int charLimit, String initialText) {
        super(parent, x, y, width, 16, 0, 172, null);
        this.textBuffer = new StringBuffer(initialText);
        this.fontRenderer = fontRenderer;
        this.cursorPos = textBuffer.length();
        this.selectionPos = this.cursorPos;
        this.charLimit = charLimit;
        this.cursorAnimationTime = 30;
        this.cursorAnimationTimeTotal = 60;
    }

    /**
     * Get the current text contents.
     *
     * @return The current text contents.
     */
    public String getContents() {
        if(this.textBuffer.length() > 0) {
            return this.textBuffer.toString();
        } else {
            return "";
        }
    }

    /**
     * Render element.
     *
     * @param graphics The current pose stack.
     * @param mouseX The x position of the mouse.
     * @param mouseY The y position of the mouse.
     */
    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY) {
        // Check conditions.
        if (this.renderEnable) {
            // Preserve the previously selected texture and bind the common texture.
            ResourceLocation preservedResource = RenderHelper.getCurrentResource();
            RenderHelper.bindTexture(CommonRender.COMMON_RESOURCE_TEXTURE);

            // Draw the left side of the text box frame.
            this.blit(graphics, this.x, this.y, 0, 172, 3, 16);

            // Draw the center part of the text box.
            for (int i = 0; i <= this.width; i += 6) {
                this.blit(graphics, (this.x + 3) + i, this.y, 6, 172, 6, 16);
            }

            // Draw the right side of the text box frame.
            this.blit(graphics, (this.x + 6) + (6 * this.charLimit), this.y, 3, 172, 3, 16);

            // Draw the text.
            // TODO: Allow for larger text entry by allowing text scrolling.
            graphics.drawString(this.fontRenderer, this.textBuffer.toString(), (this.x + 3), (this.y + 4), 16777215, false);

            // Trigger user-defined render logic.
            if (this.onRender != null) {
                this.onRender.trigger(graphics, mouseX, mouseY);
            }

            // Draw cursor and selection box.
            renderCursor(this.parent.getGuiLeft() + this.x, this.parent.getGuiTop() + this.y);
            renderSelection(this.parent.getGuiLeft() + this.x, this.parent.getGuiTop() + this.y);

            // Reset color and restore the previously bound texture.
            RenderHelper.clearRenderColor();
            RenderHelper.bindTexture(preservedResource);
        }
    }

    /**
     * Draw the cursor.
     */
    private void renderCursor(int x, int y) {
        // Increment animation timer and reset if necessary.
        this.cursorAnimationTime++;
        if (cursorAnimationTime > cursorAnimationTimeTotal) {
            cursorAnimationTime = 0;
        }

        // Check conditions.
        if (!this.focusEnable || !this.renderEnable || (this.cursorAnimationTime < (this.cursorAnimationTimeTotal / 2))) {
            return;
        }

        // Render position for the cursor.
        int cursorRenderPos = (x + 2);
        if(this.textBuffer.length() >= this.cursorPos) {
            cursorRenderPos = (this.fontRenderer.width(this.textBuffer.substring(0, this.cursorPos)) + (x + 2));
        }

        // Set up tessellator and buffer.
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder renderBuffer = tessellator.getBuilder();
        RenderHelper.setRenderColor(255.0F, 255.0F, 255.0F, 255.0F);
        // TODO: does this need replaced?
//        RenderSystem.disableTexture();

        // Set positions.
        renderBuffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        renderBuffer.vertex((cursorRenderPos + 1), (y + 3), 0.0D).endVertex();
        renderBuffer.vertex(cursorRenderPos, (y + 3), 0.0D).endVertex();
        renderBuffer.vertex(cursorRenderPos, (y + 13), 0.0D).endVertex();
        renderBuffer.vertex((cursorRenderPos + 1), (y + 13), 0.0D).endVertex();

        // Draw and reset.
        tessellator.end();
//        RenderSystem.enableTexture();
        RenderHelper.clearRenderColor();
    }

    /**
     * Draw the highlight box on the selected text.
     */
    private void renderSelection(int x, int y) {
        // Check conditions.
        if (!this.renderEnable || this.cursorPos == this.selectionPos) {
            return;
        }

        // Render position for the cursor.
        int cursorRenderPos = (x + 2);
        if(this.textBuffer.length() >= this.cursorPos) {
            cursorRenderPos = (this.fontRenderer.width(this.textBuffer.substring(0, this.cursorPos)) + (x + 2));
        }

        // Render position for the selection.
        int selectionRenderPos = (x + 2);
        if(this.textBuffer.length() >= this.selectionPos) {
            selectionRenderPos = (this.fontRenderer.width(this.textBuffer.substring(0, this.selectionPos)) + (x + 2));
        }

        // Left edge of the box.
        int leftRenderPos = (this.selectionPos > this.cursorPos) ? cursorRenderPos : selectionRenderPos;
        // Right edge of the box.
        int rightRenderPos = (this.selectionPos > this.cursorPos) ? selectionRenderPos : cursorRenderPos;

        // Set up tessellator and buffer.
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder renderBuffer = tessellator.getBuilder();
        RenderHelper.setRenderColor(0.0F, 0.0F, 255.0F, 255.0F);
//        RenderSystem.disableTexture();
        RenderSystem.enableColorLogicOp();
        RenderSystem.logicOp(GlStateManager.LogicOp.OR_REVERSE);

        // Set positions.
        renderBuffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        renderBuffer.vertex(rightRenderPos, (y + 2), 0.0D).endVertex();
        renderBuffer.vertex(leftRenderPos, (y + 2), 0.0D).endVertex();
        renderBuffer.vertex(leftRenderPos, (y + 14), 0.0D).endVertex();
        renderBuffer.vertex(rightRenderPos, (y + 14), 0.0D).endVertex();

        // Draw and reset.
        tessellator.end();
        RenderSystem.disableColorLogicOp();
//        RenderSystem.enableTexture();
        RenderHelper.clearRenderColor();
    }

    /**
     * Returns whether the mouse is over the current element or not.
     *
     * @param mouseX The x position of the mouse.
     * @param mouseY The y position of the mouse.
     * @return True if the mouse is over this element, false otherwise.
     */
    public boolean isMouseOver(double mouseX, double mouseY) {
        // Get actual x and y positions.
        int relativeX = this.parent.getGuiLeft() + this.x;
        int relativeY = this.parent.getGuiTop() + this.y;
        // Check the mouse.
        return ((mouseX > relativeX) && (mouseX < relativeX + (this.width + 6))
                && (mouseY > relativeY) && (mouseY < relativeY + this.height));
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
        // Reset cursor and selection positions.
        this.cursorPos = this.selectionPos;
        // Change the focus (if needed).
        this.focusEnable = this.isMouseOver(mouseX, mouseY) && this.actionEnable;
        return this.focusEnable;
    }

    /**
     * Triggered when a key is pressed.
     *
     * @param keyCode   The key code pressed.
     * @param scanCode  The scan code pressed.
     * @param modifiers Any modifiers pressed.
     * @return Whether the event was consumed.
     * @implNote onKeyPressed is disabled.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Check conditions.
        if (this.actionEnable && this.focusEnable) {
            // Check for Ctrl+A (select all text).
            if (Screen.isSelectAll(keyCode)) {
                this.cursorPos = textBuffer.length();
                this.selectionPos = 0;
                return true;
            }
            // Check for Ctrl-C (copy text in selection).
            if (Screen.isCopy(keyCode)) {
                this.parent.getMinecraft().keyboardHandler.setClipboard(this.getSelection());
                return true;
            }
            // Check for Ctrl-X (cut text in selection).
            if (Screen.isCut(keyCode)) {
                this.parent.getMinecraft().keyboardHandler.setClipboard(this.getSelection());
                this.deleteSelection();
                return true;
            }
            // Check for Ctrl-V (paste text in selection).
            if (Screen.isPaste(keyCode)) {
                this.write(this.parent.getMinecraft().keyboardHandler.getClipboard());
                return true;
            }
            // Handle other characters:
            switch (keyCode) {
                case GLFW_KEY_ESCAPE: {
                    // Return false, so that the parent may close the screen.
                    return false;
                }
                case GLFW_KEY_BACKSPACE: {
                    // Delete the character.
                    this.deleteCharacter();
                    // Check for shift (delete word).
                    if ((Screen.hasControlDown())) {
                        // Continue to delete until the next break character.
                        this.deleteWord();
                    }
                    break;
                }
                case GLFW_KEY_LEFT: {
                    // Move cursor left.
                    this.shiftCursorLeft();
                    break;
                }
                case GLFW_KEY_RIGHT: {
                    // Move cursor right.
                    this.shiftCursorRight();
                    break;
                }
                case GLFW_KEY_HOME: {
                    // Move cursor to beginning.
                    this.cursorPos = 0;
                    break;
                }
                case GLFW_KEY_END: {
                    // Move cursor to end.
                    this.cursorPos = this.textBuffer.length();
                    break;
                }
            }
            // Consume the event.
            return true;
        }
        // Condition checks have failed, do not consume and let the parent handle it.
        return false;
    }

    /**
     * Triggered when a character is typed.
     *
     * @param codePoint The character typed.
     * @param modifiers Any modifiers released.
     * @return Whether the event was consumed.
     * @implNote onCharTyped is disabled.
     */
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        // Check conditions.
        if (this.actionEnable && this.focusEnable) {
            // Write this character.
            this.write(Character.toString(codePoint));
            return true;
        }
        // Do not write this character.
        return false;
    }

    /**
     * Shift the cursor one to the left.
     */
    public void shiftCursorLeft() {
        // Check if we can shift left.
        if (this.cursorPos > 0) {
            // Check which method to shift with.
            if (Screen.hasShiftDown() && Screen.hasControlDown()) {
                // Select and move by word (cursor position moves out of sync with selection position).
                this.cursorPos = this.findBreakCharacter(this.cursorPos, true);
            } else if (Screen.hasControlDown()) {
                // Move by word (cursor position moves in sync with selection position).
                this.cursorPos = this.findBreakCharacter(this.cursorPos, true);
                this.selectionPos = this.cursorPos;
            } else if (Screen.hasShiftDown()) {
                // Select and move by character (cursor position moves out of sync with selection position).
                this.cursorPos--;
            } else {
                // Move by character (cursor position moves in sync with selection position).
                this.cursorPos--;
                this.selectionPos = this.cursorPos;
            }
        }
    }

    /**
     * Shift the cursor one to the right.
     */
    public void shiftCursorRight() {
        // Check if we can shift right.
        if (this.cursorPos < this.textBuffer.length()) {
            // Check which method to shift with.
            if (Screen.hasShiftDown() && Screen.hasControlDown()) {
                // Select and move by word (cursor position moves out of sync with selection position).
                this.cursorPos = this.findBreakCharacter(this.cursorPos, false);
            } else if (Screen.hasControlDown()) {
                // Move by word (cursor position moves in sync with selection position).
                this.cursorPos = this.findBreakCharacter(this.cursorPos, false);
                this.selectionPos = this.cursorPos;
            } else if (Screen.hasShiftDown()) {
                // Select and move by character (cursor position moves out of sync with selection position).
                this.cursorPos++;
            } else {
                // Move by character (cursor position moves in sync with selection position).
                this.cursorPos++;
                this.selectionPos = this.cursorPos;
            }
        }
    }

    /**
     * Find the first "break" character when iterating from the start point.
     *
     * @param start   The index to start at.
     * @param reverse Which direction to search in.
     * @return The index of the break.
     */
    public int findBreakCharacter(int start, boolean reverse) {
        // If the index is below 0, it's invalid.
        if (start < 0) {
            return 0;
        }
        // If the index is larger than the buffer, it's invalid.
        if (start > this.textBuffer.length()) {
            return this.textBuffer.length();
        }
        // What direction we going?
        if (reverse) {
            // Go backwards.
            for (int i = (start - 1); i >= 0; i--) {
                // Check if we've reached the beginning.
                if (i == 0) {
                    return 0;
                }
                // Check if the character is a "break" character.
                if (this.isBreakCharacter(this.textBuffer.codePointAt(i))) {
                    return i;
                }
            }
            return this.textBuffer.length();
        } else {
            // Go forward.
            for (int i = (start + 1); i <= this.textBuffer.length(); i++) {
                // Check if we've reached the end.
                if (i == this.textBuffer.length()) {
                    return this.textBuffer.length();
                }
                // Check if the character is a "break" character.
                if (this.isBreakCharacter(this.textBuffer.codePointAt(i))) {
                    return i;
                }
            }
            return 0;
        }
    }

    /**
     * Check if the character is a "break" character.
     *
     * @param codePoint The character to check.
     * @return Whether or not it's a word break.
     */
    public boolean isBreakCharacter(int codePoint) {
        switch (codePoint) {
            case GLFW_KEY_SPACE:
            case GLFW_KEY_PERIOD:
            case GLFW_KEY_COMMA:
                //case GLFW_KEY_COLON:
            case GLFW_KEY_SEMICOLON:
            case GLFW_KEY_SLASH:
            case GLFW_KEY_BACKSLASH: {
                return true;
            }
            default:
                return false;
        }
    }

    /**
     * Get the currently-selected text.
     *
     * @return The text that is selected.
     */
    public String getSelection() {
        // Overarching check for OOB exceptions.
        try {
            // Return the selected text (must read from smallest to largest index).
            if (this.selectionPos > this.cursorPos) {
                return this.textBuffer.substring(this.cursorPos, this.selectionPos);
            } else {
                return this.textBuffer.substring(this.selectionPos, this.cursorPos);
            }
        } catch (StringIndexOutOfBoundsException err) {
            // Cursor out of bounds!
            BiggerReactors.LOGGER.error("Failed to write text: ", err);
            return "";
        }
    }

    /**
     * Delete the currently-selected text.
     */
    public void deleteSelection() {
        // Overarching check for OOB exceptions.
        try {
            int charsDeleted = 0;
            // Check which way around we must delete (must delete from smallest to largest index).
            if (this.selectionPos > this.cursorPos) {
                this.textBuffer.delete(this.cursorPos, this.selectionPos);
                charsDeleted = this.selectionPos - this.cursorPos;
            } else {
                this.textBuffer.delete(this.selectionPos, this.cursorPos);
                charsDeleted = this.cursorPos - this.selectionPos;
            }
            // Update cursor and selection position.
            this.cursorPos -= charsDeleted;
            this.selectionPos = this.cursorPos;
        } catch (StringIndexOutOfBoundsException err) {
            // Cursor out of bounds!
            BiggerReactors.LOGGER.error("Failed to write text: ", err);
        }
    }

    /**
     * Delete text between the cursor and the previous whitespace/break character.
     */
    public void deleteWord() {
        // Overarching check for OOB exceptions.
        try {
            // Loop until we find a whitespace.
            for (int i = this.cursorPos; i > 0; --i) {
                // Is this a space?
                if (this.textBuffer.codePointAt(i - 1) == GLFW_KEY_SPACE) {
                    // Found the space.
                    return;
                }
                // Not a space, nuke.
                this.textBuffer.deleteCharAt(i - 1);
                // Update cursor and selection position.
                this.cursorPos--;
                this.selectionPos = this.cursorPos;
            }
        } catch (StringIndexOutOfBoundsException err) {
            // Cursor out of bounds!
            BiggerReactors.LOGGER.error("Failed to delete word: ", err);
        }
    }

    /**
     * Delete whichever character is immediately behind the cursor.
     */
    public void deleteCharacter() {
        // Overarching check for OOB exceptions.
        try {
            // Check if we can remove a character at this position.
            if (this.cursorPos > 0) {
                // Delete the character.
                this.textBuffer.deleteCharAt(this.cursorPos - 1);
                // Update cursor and selection position.
                this.cursorPos--;
                this.selectionPos = this.cursorPos;
            }
        } catch (StringIndexOutOfBoundsException err) {
            // Cursor out of bounds!
            BiggerReactors.LOGGER.error("Failed to write text: ", err);
        }
    }

    /**
     * Clear the contents of the buffer.
     */
    public void clear() {
        this.cursorPos = 0;
        this.selectionPos = 0;
        if(this.textBuffer.length() > 0) {
            this.textBuffer.delete(0, this.textBuffer.length());
        }
    }

    /**
     * Set the provided text into the buffer, returning what was already existing.
     *
     * @param text The text to set.
     * @return The existing/overwritten text.
     */
    public String set(String text) {
        String buffer = this.textBuffer.toString();
        this.clear();
        this.write(text);
        return buffer;
    }

    /**
     * Insert or append text at the cursor's position.
     *
     * @param text The text to write.
     */
    public void write(String text) {
        // Overarching check for OOB exceptions.
        try {
            // Check for character limit.
            if (this.textBuffer.length() >= this.charLimit) {
                this.playSound(SoundEvents.VILLAGER_NO);
                return;
            }
            // Filter out invalid characters.
            String filteredText = SharedConstants.filterText(text);
            // Append or insert the text (either at the end or in the middle).
            if (this.cursorPos >= this.textBuffer.length() || this.cursorPos < 0) {
                this.textBuffer.append(filteredText);
            } else {
                this.textBuffer.insert(this.cursorPos, filteredText);
            }
            // Update cursor and selection position.
            this.cursorPos += text.length();
            this.selectionPos = this.cursorPos;
        } catch (StringIndexOutOfBoundsException err) {
            // Cursor out of bounds!
            BiggerReactors.LOGGER.error("Failed to write text: ", err);
        }
    }
}
