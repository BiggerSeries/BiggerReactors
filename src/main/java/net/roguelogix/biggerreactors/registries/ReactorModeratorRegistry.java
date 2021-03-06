package net.roguelogix.biggerreactors.registries;

import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ITagCollection;
import net.minecraft.tags.ITagCollectionSupplier;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.roguelogix.biggerreactors.BiggerReactors;
import net.roguelogix.phosphophyllite.data.DataLoader;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReactorModeratorRegistry {
    
    public interface IModeratorProperties {
        double absorption();
        
        double heatEfficiency();
        
        double moderation();
        
        double heatConductivity();
    }
    
    public static final IModeratorProperties EMPTY_MODERATOR = new IModeratorProperties() {
        @Override
        public double absorption() {
            return 0;
        }
    
        @Override
        public double heatEfficiency() {
            return 0;
        }
    
        @Override
        public double moderation() {
            return 1;
        }
    
        @Override
        public double heatConductivity() {
            return 0;
        }
    };
    
    public static class ModeratorProperties implements IModeratorProperties {
        
        public final double absorption;
        public final double heatEfficiency;
        public final double moderation;
        public final double heatConductivity;
        
        public ModeratorProperties(double absorption, double heatEfficiency, double moderation, double heatConductivity) {
            this.absorption = absorption;
            this.heatEfficiency = heatEfficiency;
            this.moderation = moderation;
            this.heatConductivity = heatConductivity;
        }
    
        @Override
        public double absorption() {
            return absorption;
        }
    
        @Override
        public double heatEfficiency() {
            return heatEfficiency;
        }
    
        @Override
        public double moderation() {
            return moderation;
        }
    
        @Override
        public double heatConductivity() {
            return heatConductivity;
        }
    }
    
    private final static HashMap<Block, ModeratorProperties> registry = new HashMap<>();
    
    public static Map<Block, ModeratorProperties> getImmutableRegistry() {
        return Collections.unmodifiableMap(registry);
    }
    
    public static boolean isBlockAllowed(Block block) {
        return registry.containsKey(block);
    }
    
    public static ModeratorProperties blockModeratorProperties(Block block) {
        return registry.get(block);
    }
    
    private static class ReactorModeratorJsonData {
        
        @DataLoader.Values({"tag", "registry", "fluidtag", "fluid"})
        String type;
        
        ResourceLocation location;
        
        @DataLoader.Range("[0,1]")
        double absorption;
        
        @DataLoader.Range("[0,1]")
        double efficiency;
        
        @DataLoader.Range("[1,)")
        double moderation;
        
        @DataLoader.Range("[0,)")
        double conductivity;
    }
    
    private static final DataLoader<ReactorModeratorJsonData> dataLoader = new DataLoader<>(ReactorModeratorJsonData.class);
    
    public static void loadRegistry(ITagCollectionSupplier tags) {
        
        ITagCollection<Block> blockTags = tags.getBlockTags();
        ITagCollection<Fluid> fluidTags = tags.getFluidTags();
        
        BiggerReactors.LOGGER.info("Loading reactor moderators");
        registry.clear();
        
        List<ReactorModeratorJsonData> data = dataLoader.loadAll(new ResourceLocation("biggerreactors:ebcr/moderators"));
        BiggerReactors.LOGGER.info("Loaded " + data.size() + " moderator data entries");
        
        for (ReactorModeratorJsonData moderatorData : data) {
            
            ModeratorProperties properties = new ModeratorProperties(moderatorData.absorption, moderatorData.efficiency, moderatorData.moderation, moderatorData.conductivity);
    
            switch (moderatorData.type) {
                case "tag": {
                    ITag<Block> blockTag = blockTags.get(moderatorData.location);
                    if (blockTag == null) {
                        continue;
                    }
                    for (Block element : blockTag.getAllElements()) {
                        registry.put(element, properties);
                        BiggerReactors.LOGGER.debug("Loaded moderator " + element.getRegistryName().toString());
                    }
                    break;
                }
                case "registry":
                    // cant check against air, because air is a valid thing to load
                    if (ForgeRegistries.BLOCKS.containsKey(moderatorData.location)) {
                        registry.put(ForgeRegistries.BLOCKS.getValue(moderatorData.location), properties);
                        BiggerReactors.LOGGER.debug("Loaded moderator " + moderatorData.location);
                    }
                    break;
                case "fluidtag": {
                    ITag<Fluid> blockTag = fluidTags.get(moderatorData.location);
                    if (blockTag == null) {
                        continue;
                    }
                    for (Fluid element : blockTag.getAllElements()) {
                        Block elementBlock = element.getDefaultState().getBlockState().getBlock();
                        registry.put(elementBlock, properties);
                        BiggerReactors.LOGGER.debug("Loaded moderator " + element.getRegistryName().toString());
                    }
                    break;
                }
                case "fluid":
                    // cant check against air, because air is a valid thing to load
                    if (ForgeRegistries.FLUIDS.containsKey(moderatorData.location)) {
                        Fluid fluid = ForgeRegistries.FLUIDS.getValue(moderatorData.location);
                        assert fluid != null;
                        Block block = fluid.getDefaultState().getBlockState().getBlock();
                        registry.put(block, properties);
                        BiggerReactors.LOGGER.debug("Loaded moderator " + moderatorData.location);
                    }
                    break;
            }
        }
        BiggerReactors.LOGGER.info("Loaded " + registry.size() + " moderator entries");
    }
}
