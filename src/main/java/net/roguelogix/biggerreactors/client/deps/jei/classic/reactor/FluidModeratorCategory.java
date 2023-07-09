package net.roguelogix.biggerreactors.client.deps.jei.classic.reactor;

import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraftforge.fluids.FluidStack;
import net.roguelogix.biggerreactors.BiggerReactors;
import net.roguelogix.biggerreactors.multiblocks.reactor.blocks.ReactorTerminal;
import net.roguelogix.biggerreactors.registries.ReactorModeratorRegistry;

import java.awt.*;


public class FluidModeratorCategory implements IRecipeCategory<FluidModeratorCategory.Recipe> {
    private final IDrawable background;
    private final IDrawable icon;
    public static final ResourceLocation UID = new ResourceLocation(BiggerReactors.modid, "classic/reactor_moderator_fluid");
    public static final RecipeType<Recipe> RECIPE_TYPE = new RecipeType<>(UID, Recipe.class);

    
    public FluidModeratorCategory(IGuiHelper guiHelper) {
        icon = guiHelper.createDrawableItemStack(new ItemStack(ReactorTerminal.INSTANCE));
        background = guiHelper.createDrawable(new ResourceLocation(BiggerReactors.modid, "textures/jei/common.png"), 0, 0, 144, 46);
    }
    
    @Override
    public RecipeType<Recipe> getRecipeType() {
        return RECIPE_TYPE;
    }
    
    @Override
    public Component getTitle() {
        return Component.translatable("jei.biggerreactors.classic.reactor_moderator_fluid");
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
        var slot = builder.addSlot(RecipeIngredientRole.INPUT, 1, 15);
        slot.addFluidStack(recipe.getInput().getRawFluid(), 1000);
    }
    
    @Override
    public void draw(Recipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        Component[] info = {
                Component.translatable("jei.biggerreactors.classic.reactor_moderator_moderation", recipe.getModeratorProperties().moderation()),
                Component.translatable("jei.biggerreactors.classic.reactor_moderator_absorption", recipe.getModeratorProperties().absorption()),
                Component.translatable("jei.biggerreactors.classic.reactor_moderator_conductivity", recipe.getModeratorProperties().heatConductivity()),
                Component.translatable("jei.biggerreactors.classic.reactor_moderator_efficiency", recipe.getModeratorProperties().heatEfficiency())
        };
        guiGraphics.drawString(mc.font,  info[0], 80 - mc.font.width(info[0]) / 2, 0, Color.BLACK.getRGB(), false);
        guiGraphics.drawString(mc.font,  info[1], 80 - mc.font.width(info[1]) / 2, 12, Color.BLACK.getRGB(), false);
        guiGraphics.drawString(mc.font,  info[2], 80 - mc.font.width(info[2]) / 2, 24, Color.BLACK.getRGB(), false);
        guiGraphics.drawString(mc.font,  info[3], 80 - mc.font.width(info[3]) / 2, 36, Color.BLACK.getRGB(), false);
    }
    
    public static class Recipe {
        private final FluidStack input;
        private final ReactorModeratorRegistry.IModeratorProperties moderatorProperties;

        public Recipe(FluidStack input, ReactorModeratorRegistry.IModeratorProperties moderatorProperties) {
            this.input = input;
            this.moderatorProperties = moderatorProperties;
        }

        public FluidStack getInput() {
            return input;
        }

        public ReactorModeratorRegistry.IModeratorProperties getModeratorProperties() {
            return moderatorProperties;
        }
    }
}
