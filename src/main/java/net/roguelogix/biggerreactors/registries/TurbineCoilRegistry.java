package net.roguelogix.biggerreactors.registries;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import net.roguelogix.biggerreactors.BiggerReactors;
import net.roguelogix.phosphophyllite.data.DataLoader;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    
    public static void loadRegistry() {
        BiggerReactors.LOGGER.info("Loading turbine coils");
        registry.clear();
        
        List<TurbineCoilJsonData> data = dataLoader.loadAll(new ResourceLocation("biggerreactors:ebest/coils"));
        BiggerReactors.LOGGER.info("Loaded " + data.size() + " coil data entries");
        
        for (TurbineCoilJsonData coilData : data) {
            
            CoilData properties = new CoilData(coilData.efficiency, coilData.bonus, coilData.extractionRate);
            
            if (coilData.type.equals("tag")) {
                var blockTagOptional = Registry.BLOCK.getTag(TagKey.create(Registry.BLOCK_REGISTRY, coilData.location));
                blockTagOptional.ifPresent(holders -> holders.forEach(blockHolder -> {
                    var element = blockHolder.value();
                    registry.put(element, properties);
                    BiggerReactors.LOGGER.debug("Loaded moderator " + element.getRegistryName());
                }));
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
