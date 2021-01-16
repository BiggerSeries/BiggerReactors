package net.roguelogix.biggerreactors.classic.reactor;

import net.minecraft.block.Block;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ITagCollection;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ResourceLocationException;
import net.minecraftforge.registries.ForgeRegistries;
import net.roguelogix.biggerreactors.BiggerReactors;
import net.roguelogix.biggerreactors.Config;
import net.roguelogix.phosphophyllite.data.DataLoader;
import net.roguelogix.phosphophyllite.repack.tnjson.ParseException;
import net.roguelogix.phosphophyllite.repack.tnjson.TnJson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class ReactorModeratorRegistry {
    
    public static class ModeratorProperties {
        
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
        
        @DataLoader.Values({"tag", "registry"})
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
    
    public static void loadRegistry(ITagCollection<Block> blockTags) {
        
        BiggerReactors.LOGGER.info("Loading reactor moderators");
        registry.clear();
        
        List<ReactorModeratorJsonData> data = dataLoader.loadAll(new ResourceLocation("biggerreactors:ebcr/moderators"));
        BiggerReactors.LOGGER.info("Loaded " + data.size() + " moderator data entries");
        
        for (ReactorModeratorJsonData moderatorData : data) {
            
            ModeratorProperties properties = new ModeratorProperties(moderatorData.absorption, moderatorData.efficiency, moderatorData.moderation, moderatorData.conductivity);
            
            if (moderatorData.type.equals("tag")) {
                ITag<Block> blockTag = blockTags.get(moderatorData.location);
                if (blockTag == null) {
                    continue;
                }
                for (Block element : blockTag.getAllElements()) {
                    registry.put(element, properties);
                    BiggerReactors.LOGGER.debug("Loaded moderator " + element.getRegistryName().toString());
                }
            } else {
                // cant check against air, because air is a valid thing to load
                if (ForgeRegistries.BLOCKS.containsKey(moderatorData.location)) {
                    registry.put(ForgeRegistries.BLOCKS.getValue(moderatorData.location), properties);
                    BiggerReactors.LOGGER.debug("Loaded moderator " + moderatorData.location);
                }
            }
        }
        BiggerReactors.LOGGER.info("Loaded " + registry.size() + " moderator entries");
    }
}
