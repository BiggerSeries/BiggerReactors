package net.roguelogix.biggerreactors.multiblocks.reactor.simulation.cpu;

import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.SimulationConfiguration;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.base.BaseReactorSimulation;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.base.SimUtil;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.SimulationDescription;
import net.roguelogix.biggerreactors.registries.ReactorModeratorRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

public class TimeSlicedReactorSimulation extends BaseReactorSimulation {
    
    private int currentRod = 0;
    private int rodOffset = 0;
    
    public TimeSlicedReactorSimulation(SimulationDescription simulationDescription, SimulationConfiguration configuration) {
        super(simulationDescription, configuration);
        Collections.shuffle(Arrays.asList(controlRods), new Random());
    }
    
    protected double radiate() {
        
        if (fuelTank.fuel() <= 0) {
            return 0;
        }
        
        // along with a shuffled array of control rods, this should be close enough to random, yet still cyclical
        currentRod++;
        if (currentRod >= controlRods.length) {
            currentRod = 0;
            rodOffset++;
        }
        int yLevel = currentRod % y;
        int currentRod = this.currentRod + rodOffset;
        currentRod %= controlRods.length;
        
        // Base value for radiation production penalties. 0-1, caps at about 3000C;
        final double radiationPenaltyBase = Math.exp(-configuration.radPenaltyShiftMultiplier() * Math.exp(-0.001 * configuration.radPenaltyRateMultiplier() * (fuelHeat.temperature() - 273.15)));
        
        // Raw amount - what's actually in the tanks
        // Effective amount - how
        final long baseFuelAmount = fuelTank.fuel() + (fuelTank.waste() / 100);
        
        // Intensity = how strong the radiation is, hardness = how energetic the radiation is (penetration)
        final double rawRadIntensity = (double) baseFuelAmount * configuration.fissionEventsPerFuelUnit();
        
        // Scale up the "effective" intensity of radiation, to provide an incentive for bigger reactors in general.
        // Scale up a second time based on scaled amount in each fuel rod. Provides an incentive for making reactors that aren't just pancakes.
        final double scaledRadIntensity = Math.pow((Math.pow((rawRadIntensity), configuration.fuelReactivity()) / controlRods.length), configuration.fuelReactivity()) * controlRods.length;
        
        // Radiation hardness starts at 20% and asymptotically approaches 100% as heat rises.
        // This will make radiation harder and harder to capture.
        final double initialHardness = Math.min(1.0, 0.2f + (0.8 * radiationPenaltyBase));
        
        final double rawIntensity = (1f + (-configuration.radIntensityScalingMultiplier() * Math.exp(-10f * configuration.radIntensityScalingShiftMultiplier() * Math.exp(-0.001f * configuration.radIntensityScalingRateExponentMultiplier() * (fuelHeat.temperature() - 273.15)))));
        final double fuelAbsorptionTemperatureCoefficient = (1.0 - (configuration.fuelAbsorptionScalingMultiplier() * Math.exp(-10 * configuration.fuelAbsorptionScalingShiftMultiplier() * Math.exp(-0.001 * configuration.fuelAbsorptionScalingRateExponentMultiplier() * (fuelHeat.temperature() - 273.15)))));
        final double fuelHardnessMultiplier = 1 / configuration.fuelHardnessDivisor();
        
        double rawFuelUsage = 0;
        
        double fuelRFAdded = 0;
        double fuelRadAdded = 0;
        double caseRFAdded = 0;
        
        final var FuelPerRadiationUnit = configuration.fuelPerRadiationUnit();
        final var FEPerRadiationUnit = configuration.RFPerRadiationUnit();
        final var FuelUsageMultiplier = configuration.fuelUsageMultiplier();
        final var FuelAbsorptionCoefficient = configuration.fuelAbsorptionCoefficient();
        final var FuelModerationFactor = configuration.fuelModerationFactor();
        
        SimUtil.ControlRod rod = controlRods[currentRod];
        
        // Apply control rod moderation of radiation to the quantity of produced radiation. 100% insertion = 100% reduction.
        double controlRodModifier = (100 - rod.insertion) / 100f;
        double effectiveRadIntensity = scaledRadIntensity * controlRodModifier;
        double effectiveRawRadIntensity = rawRadIntensity * controlRodModifier;
        
        // Now nerf actual radiation production based on heat.
        double initialIntensity = effectiveRadIntensity * rawIntensity;
        
        // Calculate based on propagation-to-self
        rawFuelUsage += (FuelPerRadiationUnit * effectiveRawRadIntensity / fertility()) * FuelUsageMultiplier; // Not a typo. Fuel usage is thus penalized at high heats.
        fuelRFAdded += FEPerRadiationUnit * initialIntensity;
        
        double rayMultiplier = 1.0 / (double) (SimUtil.rays.size());
        
        for (int j = 0; j < SimUtil.rays.size(); j++) {
            ArrayList<SimUtil.RayStep> raySteps = SimUtil.rays.get(j);
            double neutronHardness = initialHardness;
            double neutronIntensity = initialIntensity * rayMultiplier;
            //noinspection ForLoopReplaceableByForEach
            for (int k = 0; k < raySteps.size(); k++) {
                SimUtil.RayStep rayStep = raySteps.get(k);
                final int currentX = rod.x + rayStep.offset.x;
                final int currentY = yLevel + rayStep.offset.y;
                final int currentZ = rod.z + rayStep.offset.z;
                if (currentX < 0 || currentX >= this.x ||
                            currentY < 0 || currentY >= this.y ||
                            currentZ < 0 || currentZ >= this.z) {
                    break;
                }
                ReactorModeratorRegistry.IModeratorProperties properties = moderatorProperties[currentX][currentY][currentZ];
                if (properties != null) {
                    final double radiationAbsorbed = neutronIntensity * properties.absorption() * (1f - neutronHardness) * rayStep.length;
                    neutronIntensity = Math.max(0, neutronIntensity - radiationAbsorbed);
                    neutronHardness = neutronHardness / (((properties.moderation() - 1.0) * rayStep.length) + 1.0);
                    caseRFAdded += properties.heatEfficiency() * radiationAbsorbed * FEPerRadiationUnit;
                } else {
                    // its a fuel rod!
                    
                    // Scale control rod insertion 0..1
                    final double controlRodInsertion = controlRodsXZ[currentX][currentZ].insertion * .001;
                    
                    // Fuel absorptiveness is determined by control rod + a heat modifier.
                    // Starts at 1 and decays towards 0.05, reaching 0.6 at 1000 and just under 0.2 at 2000. Inflection point at about 500-600.
                    // Harder radiation makes absorption more difficult.
                    final double baseAbsorption = fuelAbsorptionTemperatureCoefficient * (1f - (neutronHardness * fuelHardnessMultiplier));
                    
                    // Some fuels are better at absorbing radiation than others
                    final double scaledAbsorption = baseAbsorption * FuelAbsorptionCoefficient * rayStep.length;
                    
                    // Control rods increase total neutron absorption, but decrease the total neutrons which fertilize the fuel
                    // Absorb up to 50% better with control rods inserted.
                    final double controlRodBonus = (1f - scaledAbsorption) * controlRodInsertion * 0.5f;
                    final double controlRodPenalty = scaledAbsorption * controlRodInsertion * 0.5f;
                    
                    final double radiationAbsorbed = (scaledAbsorption + controlRodBonus) * neutronIntensity;
                    final double fertilityAbsorbed = (scaledAbsorption - controlRodPenalty) * neutronIntensity;
                    
                    // Full insertion doubles the moderation factor of the fuel as well as adding its own level
                    final double fuelModerationFactor = FuelModerationFactor + (FuelModerationFactor * controlRodInsertion + controlRodInsertion);
                    
                    neutronIntensity = Math.max(0, neutronIntensity - (radiationAbsorbed));
                    neutronHardness = neutronHardness / (((fuelModerationFactor - 1.0) * rayStep.length) + 1.0);
                    
                    // Being irradiated both heats up the fuel and also enhances its fertility
                    fuelRFAdded += radiationAbsorbed * FEPerRadiationUnit;
                    fuelRadAdded += fertilityAbsorbed;
                }
            }
        }
        
        if (!Double.isNaN(fuelRadAdded)) {
            if (configuration.fuelRadScalingMultiplier() != 0) {
                fuelRadAdded *= configuration.fuelRadScalingMultiplier() * (configuration.fuelRodFuelCapacity() / Math.max(1.0, (double) fuelTank().totalStored()));
            }
            fuelFertility += fuelRadAdded;
        }
        if (!Double.isNaN(fuelRFAdded)) {
            fuelHeat.absorbRF(fuelRFAdded);
        }
        if (!Double.isNaN(caseRFAdded)) {
            stackHeat.absorbRF(caseRFAdded);
        }
        return rawFuelUsage;
    }
}
