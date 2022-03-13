package net.roguelogix.biggerreactors.registries;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;
import net.roguelogix.biggerreactors.BiggerReactors;
import net.roguelogix.phosphophyllite.data.DataLoader;
import net.roguelogix.phosphophyllite.robn.ROBNObject;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReactorModeratorRegistry {
    
    public interface IModeratorProperties extends ROBNObject {
        double absorption();
        
        double heatEfficiency();
        
        double moderation();
        
        double heatConductivity();
        
        @Override
        default Map<String, Object> toROBNMap() {
            final Map<String, Object> map = new HashMap<>();
            map.put("absorption", absorption());
            map.put("heatEfficiency", heatEfficiency());
            map.put("moderation", moderation());
            map.put("heatConductivity", heatConductivity());
            return map;
        }
        
        @Override
        default void fromROBNMap(Map<String, Object> map) {
            throw new NotImplementedException("");
        }
    }
    
    public static class ModeratorProperties implements IModeratorProperties, ROBNObject {
        
        public static final ModeratorProperties EMPTY_MODERATOR = new ModeratorProperties(0, 0, 1, 0);
        
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
    
    public static void loadRegistry() {
        BiggerReactors.LOGGER.info("Loading reactor moderators");
        registry.clear();
        
        List<ReactorModeratorJsonData> data = dataLoader.loadAll(new ResourceLocation("biggerreactors:ebcr/moderators"));
        BiggerReactors.LOGGER.info("Loaded " + data.size() + " moderator data entries");
        
        for (ReactorModeratorJsonData moderatorData : data) {
            
            ModeratorProperties properties = new ModeratorProperties(moderatorData.absorption, moderatorData.efficiency, moderatorData.moderation, moderatorData.conductivity);
            
            switch (moderatorData.type) {
                case "tag": {
                    var blockTagOptional = Registry.BLOCK.getTag(TagKey.create(Registry.BLOCK_REGISTRY, moderatorData.location));
                    blockTagOptional.ifPresent(holders -> holders.forEach(blockHolder -> {
                        var element = blockHolder.value();
                        registry.put(element, properties);
                        BiggerReactors.LOGGER.debug("Loaded moderator " + element.getRegistryName());
                    }));
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
                    var fluidTagOptional = Registry.FLUID.getTag(TagKey.create(Registry.FLUID_REGISTRY, moderatorData.location));
                    fluidTagOptional.ifPresent(holders -> holders.forEach(fluidHolder -> {
                        var element = fluidHolder.value();
                        Block elementBlock = element.defaultFluidState().createLegacyBlock().getBlock();
                        registry.put(elementBlock, properties);
                        BiggerReactors.LOGGER.debug("Loaded moderator " + element.getRegistryName());
                    }));
                    break;
                }
                case "fluid":
                    // cant check against air, because air is a valid thing to load
                    if (ForgeRegistries.FLUIDS.containsKey(moderatorData.location)) {
                        Fluid fluid = ForgeRegistries.FLUIDS.getValue(moderatorData.location);
                        assert fluid != null;
                        Block block = fluid.defaultFluidState().createLegacyBlock().getBlock();
                        registry.put(block, properties);
                        BiggerReactors.LOGGER.debug("Loaded moderator " + moderatorData.location);
                    }
                    break;
            }
        }
        BiggerReactors.LOGGER.info("Loaded " + registry.size() + " moderator entries");
    }
}
