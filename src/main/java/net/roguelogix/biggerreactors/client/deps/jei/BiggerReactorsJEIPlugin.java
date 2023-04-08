package net.roguelogix.biggerreactors.client.deps.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraftforge.fluids.FluidStack;
import net.roguelogix.biggerreactors.BiggerReactors;
import net.roguelogix.biggerreactors.Config;
import net.roguelogix.biggerreactors.client.deps.jei.classic.reactor.BlockModeratorCategory;
import net.roguelogix.biggerreactors.client.deps.jei.classic.reactor.FluidModeratorCategory;
import net.roguelogix.biggerreactors.client.deps.jei.classic.turbine.CoilCategory;
import net.roguelogix.biggerreactors.multiblocks.reactor.blocks.ReactorTerminal;
import net.roguelogix.biggerreactors.multiblocks.turbine.blocks.TurbineTerminal;
import net.roguelogix.biggerreactors.registries.ReactorModeratorRegistry;
import net.roguelogix.biggerreactors.registries.TurbineCoilRegistry;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@JeiPlugin
public class BiggerReactorsJEIPlugin implements IModPlugin {

    @Nonnull
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(BiggerReactors.modid, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new BlockModeratorCategory(guiHelper));
        registration.addRecipeCategories(new FluidModeratorCategory(guiHelper));
        registration.addRecipeCategories(new CoilCategory(guiHelper));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ReactorTerminal.INSTANCE), BlockModeratorCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ReactorTerminal.INSTANCE), FluidModeratorCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(TurbineTerminal.INSTANCE), CoilCategory.RECIPE_TYPE);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (!Config.CONFIG.EnableJEIIntegration) {
            return;
        }
        
        List<CoilCategory.Recipe> recipes = TurbineCoilRegistry.Client.getImmutableRegistry().entrySet().stream()
                .map(e -> new CoilCategory.Recipe(new ItemStack(e.getKey().asItem()), e.getValue()))
                .collect(Collectors.toList());
        
        registration.addRecipes(CoilCategory.RECIPE_TYPE, recipes);
        
        
        List<FluidModeratorCategory.Recipe> fluidModeratorRecipes = new ArrayList<>();
        List<BlockModeratorCategory.Recipe> blockModeratorRecipes = new ArrayList<>();

        ReactorModeratorRegistry.Client.forEach((block, moderatorProperties) -> {
            if (block instanceof LiquidBlock) {
                LiquidBlock fluidBlock = (LiquidBlock) block;
                FluidStack stack = new FluidStack(fluidBlock.getFluid(), 1000);
                fluidModeratorRecipes.add(new FluidModeratorCategory.Recipe(stack, moderatorProperties));
            } else if (!(block instanceof AirBlock)) {
                ItemStack stack = new ItemStack(block.asItem());
                blockModeratorRecipes.add(new BlockModeratorCategory.Recipe(stack, moderatorProperties));
            }
        });

        registration.addRecipes(FluidModeratorCategory.RECIPE_TYPE, fluidModeratorRecipes);
        registration.addRecipes(BlockModeratorCategory.RECIPE_TYPE, blockModeratorRecipes);
    }
}
