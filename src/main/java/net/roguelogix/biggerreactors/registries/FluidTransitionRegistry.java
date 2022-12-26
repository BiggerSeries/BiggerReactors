package net.roguelogix.biggerreactors.registries;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;
import net.roguelogix.biggerreactors.BiggerReactors;
import net.roguelogix.phosphophyllite.data.DataLoader;
import net.roguelogix.phosphophyllite.robn.ROBNObject;
import org.apache.commons.lang3.NotImplementedException;

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
            throw new NotImplementedException("");
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
    
    private static class FluidTransitionJsonData {
        @DataLoader.Values({"tag", "registry"})
        public String liquidType;
        public ResourceLocation liquid;
        
        @DataLoader.Values({"tag", "registry"})
        public String gasType;
        public ResourceLocation gas;
        
        @DataLoader.Range("(0,)")
        public double latentHeat;
        
        @DataLoader.Range("(20,)")
        public double boilingPoint;
        
        @DataLoader.Range("(0,)")
        public double liquidThermalConductivity;
        
        @DataLoader.Range("(0,)")
        public double gasThermalConductivity;
        
        @DataLoader.Range("[0,)")
        public double turbineMultiplier;
    }
    
    private static final DataLoader<FluidTransitionJsonData> loader = new DataLoader<>(FluidTransitionJsonData.class);
    
    public static void loadRegistry() {
        BiggerReactors.LOGGER.info("Loading fluid transitions");
        liquidTransitions.clear();
        gasTransitions.clear();
        
        final List<FluidTransitionJsonData> data = loader.loadAll(new ResourceLocation("biggerreactors:transitions"));
        BiggerReactors.LOGGER.info("Loaded " + data.size() + " transitions data entries");
        
        for (FluidTransitionJsonData transitionData : data) {
            final List<Fluid> liquids = new ArrayList<>();
            
            if (transitionData.liquidType.equals("tag")) {
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
            
            if (transitionData.gasType.equals("tag")) {
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
