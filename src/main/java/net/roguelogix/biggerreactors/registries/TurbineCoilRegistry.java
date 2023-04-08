package net.roguelogix.biggerreactors.registries;

import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.ForgeRegistries;
import net.roguelogix.biggerreactors.BiggerReactors;
import net.roguelogix.biggerreactors.Config;
import net.roguelogix.phosphophyllite.data.DataLoader;
import net.roguelogix.phosphophyllite.networking.SimplePhosChannel;
import net.roguelogix.phosphophyllite.registry.OnModLoad;
import net.roguelogix.phosphophyllite.serialization.PhosphophylliteCompound;

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
                    BiggerReactors.LOGGER.debug("Loaded coil " + ForgeRegistries.BLOCKS.getKey(element));
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
    
    public static class Client {
        
        private static final SimplePhosChannel CHANNEL = new SimplePhosChannel(new ResourceLocation(BiggerReactors.modid, "coil_sync_channel"), "0", Client::readSync);
        private static final ObjectOpenHashSet<Block> coilBlocks = new ObjectOpenHashSet<>();
        private static final Object2ObjectOpenHashMap<Block, CoilData> coilProperties = new Object2ObjectOpenHashMap<>();
        
        @OnModLoad(required = true)
        private static void onModLoad() {
            MinecraftForge.EVENT_BUS.addListener(Client::datapackEvent);
            if (FMLEnvironment.dist.isClient()) {
                MinecraftForge.EVENT_BUS.addListener(Client::toolTipEvent);
            }
        }
        
        public static void datapackEvent(OnDatapackSyncEvent e) {
            final var player = e.getPlayer();
            if (player == null) {
                return;
            }
            
            if (BiggerReactors.LOG_DEBUG) {
                BiggerReactors.LOGGER.debug("Sending coil list to player: " + player);
            }
            CHANNEL.sendToPlayer(player, writeSync());
        }
        
        private static PhosphophylliteCompound writeSync() {
            final var list = new ObjectArrayList<String>();
            final var propertiesList = new ObjectArrayList<DoubleArrayList>();
            for (final var value : registry.entrySet()) {
                final var location = ForgeRegistries.BLOCKS.getKey(value.getKey());
                if (location == null) {
                    continue;
                }
                list.add(location.toString());
                var properties = new DoubleArrayList();
                properties.add(value.getValue().efficiency);
                properties.add(value.getValue().extractionRate);
                properties.add(value.getValue().bonus);
                propertiesList.add(properties);
            }
            final var compound = new PhosphophylliteCompound();
            compound.put("list", list);
            compound.put("propertiesList", propertiesList);
            return compound;
        }
        
        private static void readSync(PhosphophylliteCompound compound) {
            coilBlocks.clear();
            //noinspection unchecked
            final var list = (List<String>) compound.getList("list");
            //noinspection unchecked
            final var propertiesList = (List<DoubleArrayList>) compound.getList("propertiesList");
            if (BiggerReactors.LOG_DEBUG) {
                BiggerReactors.LOGGER.debug("Received coil list from server with length of " + list.size());
            }
            for (int i = 0; i < list.size(); i++) {
                var value = list.get(i);
                var properties = propertiesList.get(i);
                final var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(value));
                if (block == null) {
                    return;
                }
                if (BiggerReactors.LOG_DEBUG) {
                    BiggerReactors.LOGGER.debug("Block " + value + " added as coil on client");
                }
                coilBlocks.add(block);
                coilProperties.put(block, new CoilData(properties.getDouble(0), properties.getDouble(1), properties.getDouble(2)));
            }
        }
        
        public static void toolTipEvent(ItemTooltipEvent event) {
            final var item = event.getItemStack().getItem();
            if (item instanceof BlockItem blockItem) {
                if (!coilBlocks.contains(blockItem.getBlock())) {
                    return;
                }
            } else {
                return;
            }
            if (Minecraft.getInstance().options.advancedItemTooltips || Config.CONFIG.AlwaysShowTooltips) {
                event.getToolTip().add(Component.translatable("tooltip.biggerreactors.is_a_coil"));
            }
        }
        
        public static Map<Block, CoilData> getImmutableRegistry() {
            return Collections.unmodifiableMap(coilProperties);
        }
    }
}
