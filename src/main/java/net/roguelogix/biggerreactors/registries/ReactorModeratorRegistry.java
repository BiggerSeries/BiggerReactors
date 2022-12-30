package net.roguelogix.biggerreactors.registries;

import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.ForgeRegistries;
import net.roguelogix.biggerreactors.BiggerReactors;
import net.roguelogix.phosphophyllite.data.DataLoader;
import net.roguelogix.phosphophyllite.networking.SimplePhosChannel;
import net.roguelogix.phosphophyllite.registry.OnModLoad;
import net.roguelogix.phosphophyllite.robn.ROBNObject;
import net.roguelogix.phosphophyllite.serialization.PhosphophylliteCompound;
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
                    var blockTagOptional = BuiltInRegistries.BLOCK.getTag(TagKey.create(BuiltInRegistries.BLOCK.key(), moderatorData.location));
                    blockTagOptional.ifPresent(holders -> holders.forEach(blockHolder -> {
                        var element = blockHolder.value();
                        registry.put(element, properties);
                        BiggerReactors.LOGGER.debug("Loaded moderator " + ForgeRegistries.BLOCKS.getKey(element));
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
                    var fluidTagOptional = BuiltInRegistries.FLUID.getTag(TagKey.create(BuiltInRegistries.FLUID.key(), moderatorData.location));
                    fluidTagOptional.ifPresent(holders -> holders.forEach(fluidHolder -> {
                        var element = fluidHolder.value();
                        Block elementBlock = element.defaultFluidState().createLegacyBlock().getBlock();
                        registry.put(elementBlock, properties);
                        BiggerReactors.LOGGER.debug("Loaded moderator " + ForgeRegistries.FLUIDS.getKey(element));
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
    
    public static class Client {
        
        private static final SimplePhosChannel CHANNEL = new SimplePhosChannel(new ResourceLocation(BiggerReactors.modid, "moderator_sync_channel"), "0", Client::readSync);
        private static final ObjectOpenHashSet<Block> moderatorBlocks = new ObjectOpenHashSet<>();
        
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
                BiggerReactors.LOGGER.debug("Sending moderator list to player: " + player);
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
            moderatorBlocks.clear();
            //noinspection unchecked
            final var list = (List<String>) compound.getList("list");
            if (BiggerReactors.LOG_DEBUG) {
                BiggerReactors.LOGGER.debug("Received moderator list from server with length of " + list.size());
            }
            for (final var value : list) {
                final var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(value));
                if (block == null) {
                    return;
                }
                if (BiggerReactors.LOG_DEBUG) {
                    BiggerReactors.LOGGER.debug("Block " + value + " added as moderator on client");
                }
                moderatorBlocks.add(block);
            }
        }
        
        public static void toolTipEvent(RenderTooltipEvent.GatherComponents event) {
            // TODO: sync this, currently reaching across sides
            //       there is an event for doing that
            final var item = event.getItemStack().getItem();
            if (item instanceof BlockItem blockItem) {
                if (!moderatorBlocks.contains(blockItem.getBlock())) {
                    return;
                }
            } else if (item instanceof BucketItem bucketItem) {
                final var fluidBlock = bucketItem.getFluid().defaultFluidState().createLegacyBlock().getBlock();
                if (fluidBlock.defaultBlockState().isAir() || !moderatorBlocks.contains(fluidBlock)) {
                    return;
                }
            } else {
                return;
            }
            event.getTooltipElements().add(Either.left(Component.translatable("tooltip.biggerreactors.is_a_moderator")));
        }
    }
}
