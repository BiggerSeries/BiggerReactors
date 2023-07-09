package net.roguelogix.biggerreactors.client.deps.jei.classic.turbine;

import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.roguelogix.biggerreactors.BiggerReactors;
import net.roguelogix.biggerreactors.multiblocks.turbine.blocks.TurbineTerminal;
import net.roguelogix.biggerreactors.registries.TurbineCoilRegistry;

import java.awt.*;

public class CoilCategory implements IRecipeCategory<CoilCategory.Recipe> {

    private final IDrawable background;
    private final IDrawable icon;
    public static final ResourceLocation UID = new ResourceLocation(BiggerReactors.modid, "classic/turbine_coil");
    public static final RecipeType<Recipe> RECIPE_TYPE = new RecipeType<>(UID, Recipe.class);

    public CoilCategory(IGuiHelper guiHelper) {
        icon = guiHelper.createDrawableItemStack(new ItemStack(TurbineTerminal.INSTANCE));
        background = guiHelper.createDrawable(new ResourceLocation(BiggerReactors.modid, "textures/jei/common.png"), 0, 6, 144, 34);
    }
    
    @Override
    public RecipeType<Recipe> getRecipeType() {
        return RECIPE_TYPE;
    }
    
    @Override
    public Component getTitle() {
        return Component.translatable("jei.biggerreactors.classic.turbine_coil_block");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }
    
    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        var slot = builder.addSlot(RecipeIngredientRole.INPUT, 1, 9);
        slot.addItemStack(recipe.getInput());
    }
    
    @Override
    public void draw(Recipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        Component[] info = {
                Component.translatable("jei.biggerreactors.classic.turbine_coil_bonus", recipe.getCoilData().bonus),
                Component.translatable("jei.biggerreactors.classic.turbine_coil_efficiency", recipe.getCoilData().efficiency),
                Component.translatable("jei.biggerreactors.classic.turbine_coil_extraction", recipe.getCoilData().extractionRate)
        };
        guiGraphics.drawString(mc.font,  info[0], 80 - mc.font.width(info[0]) / 2, 0, Color.BLACK.getRGB(), false);
        guiGraphics.drawString(mc.font,  info[1], 80 - mc.font.width(info[1]) / 2, 12, Color.BLACK.getRGB(), false);
        guiGraphics.drawString(mc.font,  info[2], 80 - mc.font.width(info[2]) / 2, 24, Color.BLACK.getRGB(), false);
    }
    
    public static class Recipe {
        private final ItemStack input;
        private final TurbineCoilRegistry.CoilData coilData;

        public Recipe(ItemStack input, TurbineCoilRegistry.CoilData coilData) {
            this.input = input;
            this.coilData = coilData;
        }

        public ItemStack getInput() {
            return input;
        }

        public TurbineCoilRegistry.CoilData getCoilData() {
            return coilData;
        }
    }
}
