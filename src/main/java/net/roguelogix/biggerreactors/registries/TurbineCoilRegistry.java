package net.roguelogix.biggerreactors.registries;

import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.ForgeRegistries;
import net.roguelogix.biggerreactors.BiggerReactors;
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
                var blockTagOptional = BuiltInRegistries.BLOCK.getTag(TagKey.create(BuiltInRegistries.BLOCK.key(), coilData.location));
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
            for (final var value : registry.keySet()) {
                final var location = ForgeRegistries.BLOCKS.getKey(value);
                if (location == null) {
                    continue;
                }
                list.add(location.toString());
            }
            final var compound = new PhosphophylliteCompound();
            compound.put("list", list);
            return compound;
        }
        
        private static void readSync(PhosphophylliteCompound compound) {
            coilBlocks.clear();
            //noinspection unchecked
            final var list = (List<String>) compound.getList("list");
            if (BiggerReactors.LOG_DEBUG) {
                BiggerReactors.LOGGER.debug("Received coil list from server with length of " + list.size());
            }
            for (final var value : list) {
                final var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(value));
                if (block == null) {
                    return;
                }
                if (BiggerReactors.LOG_DEBUG) {
                    BiggerReactors.LOGGER.debug("Block " + value + " added as coil on client");
                }
                coilBlocks.add(block);
            }
        }
        
        public static void toolTipEvent(RenderTooltipEvent.GatherComponents event) {
            // TODO: sync this, currently reaching across sides
            //       there is an event for doing that
            final var item = event.getItemStack().getItem();
            if (item instanceof BlockItem blockItem) {
                if (!coilBlocks.contains(blockItem.getBlock())) {
                    return;
                }
            } else {
                return;
            }
            event.getTooltipElements().add(Either.left(Component.translatable("tooltip.biggerreactors.is_a_coil")));
        }
    }
}
