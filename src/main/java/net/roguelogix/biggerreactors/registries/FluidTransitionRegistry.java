package net.roguelogix.biggerreactors.registries;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;
import net.roguelogix.biggerreactors.BiggerReactors;
import net.roguelogix.phosphophyllite.config.ConfigValue;
import net.roguelogix.phosphophyllite.data.DatapackLoader;
import net.roguelogix.phosphophyllite.robn.ROBNObject;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class FluidTransitionRegistry {
    
    public interface ITransitionProperties extends ROBNObject {
        double latentHeat();
        
        double boilingPoint();
        
        double liquidRFMKT();
        
        double gasRFMKT();
        
        double turbineMultiplier();
    
        @Override
        default Map<String, Object> toROBNMap() {
            final Map<String, Object> map = new HashMap<>();
            map.put("latentHeat", latentHeat());
            map.put("boilingPoint", boilingPoint());
            map.put("liquidRFMKT", liquidRFMKT());
            map.put("gasRFMKT", gasRFMKT());
            map.put("turbineMultiplier", turbineMultiplier());
            return map;
        }
    
        @Override
        default void fromROBNMap(Map<String, Object> map) {
            throw new IllegalArgumentException("");
        }
    }
    
    
    public record TransitionProperties(double latentHeat, double boilingPoint, double liquidRFMKT, double gasRFMKT, double turbineMultiplier) implements ITransitionProperties {
        
        public TransitionProperties(Map<String, Object> map) {
            this(
                    ((Number) map.get("latentHeat")).doubleValue(),
                    ((Number) map.get("boilingPoint")).doubleValue(),
                    ((Number) map.get("liquidRFMKT")).doubleValue(),
                    ((Number) map.get("gasRFMKT")).doubleValue(),
                    ((Number) map.get("turbineMultiplier")).doubleValue()
            );
        }
    }
    
    public static class FluidTransition implements ITransitionProperties {
        public final List<Fluid> liquids;
        public final List<Fluid> gases;
        public final double latentHeat;
        public final double boilingPoint;
        public final double liquidRFMKT;
        public final double gasRFMKT;
        public final double turbineMultiplier;
        
        public FluidTransition(List<Fluid> liquids, List<Fluid> gases, double latentHeat, double boilingPoint, double liquidRFMKT, double gasRFMKT, double turbineMultiplier) {
            this.liquids = Collections.unmodifiableList(liquids);
            this.gases = Collections.unmodifiableList(gases);
            this.latentHeat = latentHeat;
            this.boilingPoint = boilingPoint;
            this.liquidRFMKT = liquidRFMKT;
            this.gasRFMKT = gasRFMKT;
            this.turbineMultiplier = turbineMultiplier;
        }
    
        @Override
        public double latentHeat() {
            return latentHeat;
        }
    
        @Override
        public double boilingPoint() {
            return boilingPoint;
        }
    
        @Override
        public double liquidRFMKT() {
            return liquidRFMKT;
        }
    
        @Override
        public double gasRFMKT() {
            return gasRFMKT;
        }
    
        @Override
        public double turbineMultiplier() {
            return turbineMultiplier;
        }
    }
    
    private static final Map<Fluid, FluidTransition> liquidTransitions = new HashMap<>();
    private static final Map<Fluid, FluidTransition> gasTransitions = new HashMap<>();
    
    @Nullable
    public static FluidTransition liquidTransition(Fluid liquid) {
        return liquidTransitions.get(liquid);
    }
    
    @Nullable
    public static FluidTransition gasTransition(Fluid gas) {
        return gasTransitions.get(gas);
    }
    
    // TODO: unify these names across all registries
    private enum RegistryType {
        tag,
        registry,
    }
    
    private static class FluidTransitionJsonData {
        @ConfigValue
        public RegistryType liquidType = RegistryType.tag;
        @ConfigValue
        public ResourceLocation liquid = new ResourceLocation("dirt");
        
        @ConfigValue
        public RegistryType gasType = RegistryType.tag;
        @ConfigValue
        public ResourceLocation gas = new ResourceLocation("dirt");
    
        @ConfigValue(range = "(0,)")
        public double latentHeat;
    
        @ConfigValue(range = "(0,)")
        public double boilingPoint;
    
        @ConfigValue(range = "(0,)")
        public double liquidThermalConductivity;
    
        @ConfigValue(range = "(0,)")
        public double gasThermalConductivity;
    
        @ConfigValue(range = "[0,)")
        public double turbineMultiplier;
    }
    
    private static final DatapackLoader<FluidTransitionJsonData> loader = new DatapackLoader<>(FluidTransitionJsonData::new);
    
    public static void loadRegistry() {
        BiggerReactors.LOGGER.info("Loading fluid transitions");
        liquidTransitions.clear();
        gasTransitions.clear();
        
        final List<FluidTransitionJsonData> data = loader.loadAll(new ResourceLocation("biggerreactors:transitions"));
        BiggerReactors.LOGGER.info("Loaded " + data.size() + " transitions data entries");
        
        for (FluidTransitionJsonData transitionData : data) {
            final List<Fluid> liquids = new ArrayList<>();
            
            if (transitionData.liquidType == RegistryType.tag) {
                var fluidTagOptional = BuiltInRegistries.FLUID.getTag(TagKey.create(BuiltInRegistries.FLUID.key(), transitionData.liquid));
                fluidTagOptional.ifPresent(holders -> holders.forEach(fluidHolder -> {
                    var fluid = fluidHolder.value();
                    if (fluid.isSource(fluid.defaultFluidState())) {
                        liquids.add(fluid);
                    }
                }));
            } else {
                if (ForgeRegistries.FLUIDS.containsKey(transitionData.liquid)) {
                    Fluid fluid = ForgeRegistries.FLUIDS.getValue(transitionData.liquid);
                    liquids.add(fluid);
                }
                
            }
            
            if (liquids.isEmpty()) {
                continue;
            }
            
            final List<Fluid> gases = new ArrayList<>();
            
            if (transitionData.gasType == RegistryType.tag) {
                var fluidTagOptional = BuiltInRegistries.FLUID.getTag(TagKey.create(BuiltInRegistries.FLUID.key(), transitionData.gas));
                fluidTagOptional.ifPresent(holders -> holders.forEach(fluidHolder -> {
                    var fluid = fluidHolder.value();
                    if (fluid.isSource(fluid.defaultFluidState())) {
                        gases.add(fluid);
                    }
                }));
            } else {
                if (ForgeRegistries.FLUIDS.containsKey(transitionData.gas)) {
                    Fluid fluid = ForgeRegistries.FLUIDS.getValue(transitionData.gas);
                    gases.add(fluid);
                }
                
            }
            
            if (gases.isEmpty()) {
                continue;
            }
            
            FluidTransition transition = new FluidTransition(liquids, gases, transitionData.latentHeat, transitionData.boilingPoint, transitionData.liquidThermalConductivity, transitionData.gasThermalConductivity, transitionData.turbineMultiplier);
            
            for (Fluid liquid : transition.liquids) {
                if (liquidTransitions.put(liquid, transition) != null) {
                    
                    BiggerReactors.LOGGER.error("Duplicate transitions given for liquid fluid " + ForgeRegistries.FLUIDS.getKey(liquid).toString());
                }
            }
            
            for (Fluid gas : transition.gases) {
                if (gasTransitions.put(gas, transition) != null) {
                    BiggerReactors.LOGGER.error("Duplicate transitions given for gas fluid " + ForgeRegistries.FLUIDS.getKey(gas).toString());
                }
            }
            
        }
        
        BiggerReactors.LOGGER.info("Loaded " + liquidTransitions.size() + " liquid transition entries");
        BiggerReactors.LOGGER.info("Loaded " + gasTransitions.size() + " gas transition entries");
    }
    
}
