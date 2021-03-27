package net.roguelogix.biggerreactors.registries;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.fluid.Fluid;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ITagCollection;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.roguelogix.biggerreactors.BiggerReactors;
import net.roguelogix.phosphophyllite.data.DataLoader;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class FluidTransitionRegistry {
    
    public static class FluidTransition {
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
    }
    
    private static final Map<Fluid, FluidTransition> liquidTransitions = new HashMap<>();
    private static final Map<Fluid, FluidTransition> gasTransitions = new HashMap<>();
    
    @Nullable
    public static FluidTransition liquidTransition(Fluid liquid){
        return liquidTransitions.get(liquid);
    }
    
    @Nullable
    public static FluidTransition gasTransition(Fluid gas){
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
    
    public static void loadRegistry(ITagCollection<Fluid> fluidTags) {
        BiggerReactors.LOGGER.info("Loading fluid transitions");
        liquidTransitions.clear();
        gasTransitions.clear();
        
        List<FluidTransitionJsonData> data = loader.loadAll(new ResourceLocation("biggerreactors:transitions"));
        BiggerReactors.LOGGER.info("Loaded " + data.size() + " transitions data entries");
        
        for (FluidTransitionJsonData transitionData : data) {
            List<Fluid> liquids = new ArrayList<>();
            
            if (transitionData.liquidType.equals("tag")) {
                ITag<Fluid> tag = fluidTags.get(transitionData.liquid);
                
                if (tag == null) {
                    continue;
                }
                
                for (Fluid fluid : tag.getAllElements()) {
                    if (fluid.isSource(fluid.getDefaultState())) {
                        liquids.add(fluid);
                    }
                }
            } else {
                if (ForgeRegistries.FLUIDS.containsKey(transitionData.liquid)) {
                    Fluid fluid = ForgeRegistries.FLUIDS.getValue(transitionData.liquid);
                    liquids.add(fluid);
                }
                
            }
            
            if (liquids.isEmpty()) {
                continue;
            }
            
            List<Fluid> gases = new ArrayList<>();
            
            if (transitionData.gasType.equals("tag")) {
                ITag<Fluid> tag = fluidTags.get(transitionData.gas);
                
                if (tag == null) {
                    continue;
                }
                
                for (Fluid fluid : tag.getAllElements()) {
                    if (fluid.isSource(fluid.getDefaultState())) {
                        gases.add(fluid);
                    }
                }
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
                    BiggerReactors.LOGGER.error("Duplicate transitions given for liquid fluid " + liquid.getRegistryName().toString());
                }
            }
            
            for (Fluid gas : transition.gases) {
                if (gasTransitions.put(gas, transition) != null) {
                    BiggerReactors.LOGGER.error("Duplicate transitions given for gas fluid " + gas.getRegistryName().toString());
                }
            }
            
        }
        
        BiggerReactors.LOGGER.info("Loaded " + liquidTransitions.size() + " liquid transition entries");
        BiggerReactors.LOGGER.info("Loaded " + gasTransitions.size() + " gas transition entries");
    }
    
}
