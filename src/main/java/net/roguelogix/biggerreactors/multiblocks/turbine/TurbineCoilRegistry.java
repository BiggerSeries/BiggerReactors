package net.roguelogix.biggerreactors.multiblocks.turbine;

import net.minecraft.block.Block;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ITagCollection;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.roguelogix.biggerreactors.BiggerReactors;
import net.roguelogix.phosphophyllite.data.DataLoader;

import java.util.*;

public class TurbineCoilRegistry {
    
    
    public static class CoilData {
        public final double efficiency;
        public final double bonus;
        public final double extractionRate;
        
        public CoilData(double efficiency, double bonus, double extractionRate) {
            this.efficiency = efficiency;
            this.bonus = bonus;
            this.extractionRate = extractionRate;
        }
    }
    
    private static final HashMap<Block, CoilData> registry = new HashMap<>();
    
    public static Map<Block, CoilData> getImmutableRegistry() {
        return Collections.unmodifiableMap(registry);
    }
    
    public static synchronized boolean isBlockAllowed(Block block) {
        return registry.containsKey(block);
    }
    
    public static synchronized CoilData getCoilData(Block block) {
        return registry.get(block);
    }
    
    private static class TurbineCoilJsonData {
        @DataLoader.Values({"tag", "registry"})
        String type;
        
        ResourceLocation location;
        
        @DataLoader.Range("(0,)")
        double efficiency;
        
        @DataLoader.Range("(0,)")
        double extractionRate;
        
        @DataLoader.Range("[1,)")
        double bonus;
    }
    
    private static final DataLoader<TurbineCoilJsonData> dataLoader = new DataLoader<>(TurbineCoilJsonData.class);
    
    public static void loadRegistry(ITagCollection<Block> blockTags) {
        BiggerReactors.LOGGER.info("Loading turbine coils");
        registry.clear();
        
        List<TurbineCoilJsonData> data = dataLoader.loadAll(new ResourceLocation("biggerreactors:ebest/coils"));
        BiggerReactors.LOGGER.info("Loaded " + data.size() + " coil data entries");
        
        for (TurbineCoilJsonData coilData : data) {
            
            CoilData properties = new CoilData(coilData.efficiency, coilData.bonus, coilData.extractionRate);
            
            if (coilData.type.equals("tag")) {
                ITag<Block> blockTag = blockTags.get(coilData.location);
                if (blockTag == null) {
                    continue;
                }
                for (Block element : blockTag.getAllElements()) {
                    registry.put(element, properties);
                    BiggerReactors.LOGGER.debug("Loaded coil " + element.getRegistryName().toString());
                }
            } else {
                // cant check against air, because air is a valid thing to load
                if (ForgeRegistries.BLOCKS.containsKey(coilData.location)) {
                    registry.put(ForgeRegistries.BLOCKS.getValue(coilData.location), properties);
                    BiggerReactors.LOGGER.debug("Loaded coil " + coilData.location);
                }
            }
        }
        BiggerReactors.LOGGER.info("Loaded " + registry.size() + " coil entries");
    }
}
