package net.roguelogix.biggerreactors.multiblocks.reactor.simulation;

import net.roguelogix.biggerreactors.Config;
import net.roguelogix.biggerreactors.registries.ReactorModeratorRegistry;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector2ic;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3ic;
import net.roguelogix.phosphophyllite.serialization.PhosphophylliteCompound;
import net.roguelogix.phosphophyllite.util.HeatBody;
import net.roguelogix.phosphophyllite.util.MethodsReturnNonnullByDefault;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ModernReactorSimulation implements IReactorSimulation {
    
    private final int x, y, z;
    private final ReactorModeratorRegistry.IModeratorProperties defaultModeratorProperties;
    private final ReactorModeratorRegistry.IModeratorProperties[][][] moderatorProperties;
    private final SimUtil.ControlRod[][] controlRodsXZ;
    private final SimUtil.ControlRod[] controlRods;
    
    private final double fuelToCasingRFKT;
    private final double fuelToManifoldSurfaceArea;
    private final double stackToCoolantSystemRFKT;
    private final double casingToAmbientRFKT;
    
    private final HeatBody fuelHeat = new HeatBody();
    private final HeatBody stackHeat = new HeatBody();
    private final HeatBody ambientHeat = new HeatBody();
    
    @Nullable
    private final Battery battery;
    @Nullable
    private final CoolantTank coolantTank;
    private final HeatBody output;
    
    private final FuelTank fuelTank = new FuelTank();
    
    private double fuelFertility = 1;
    private int currentRod = 0;
    private int currentY = 0;
    
    public ModernReactorSimulation(SimulationDescription simulationDescription) {
        if (simulationDescription.controlRodLocations == null) {
            throw new IllegalArgumentException();
        }
        if (simulationDescription.moderatorProperties == null) {
            throw new IllegalArgumentException();
        }
        if (simulationDescription.manifoldLocations == null) {
            throw new IllegalArgumentException();
        }
        
        this.x = simulationDescription.x;
        this.y = simulationDescription.y;
        this.z = simulationDescription.z;
        
        defaultModeratorProperties = simulationDescription.defaultModeratorProperties;
        
        moderatorProperties = new ReactorModeratorRegistry.IModeratorProperties[x][y][z];
        controlRodsXZ = new SimUtil.ControlRod[x][z];
        controlRods = new SimUtil.ControlRod[simulationDescription.controlRodCount];
        
        {
            int currentControlRodIndex = 0;
            for (int i = 0; i < x; i++) {
                for (int j = 0; j < z; j++) {
                    if (simulationDescription.controlRodLocations[i][j]) {
                        var rod = new SimUtil.ControlRod(i, j);
                        controlRodsXZ[i][j] = rod;
                        controlRods[currentControlRodIndex++] = rod;
                    }
                }
            }
        }
        
        if (simulationDescription.passivelyCooled) {
            output = battery = new Battery((((long) (x + 2) * (y + 2) * (z + 2)) - ((long) x * y * z)) * Config.CONFIG.Reactor.PassiveBatteryPerExternalBlock);
            coolantTank = null;
        } else {
            long perSideCapacity = controlRods.length * y * Config.CONFIG.Reactor.CoolantTankAmountPerFuelRod;
            perSideCapacity += simulationDescription.manifoldCount * Config.CONFIG.Reactor.CoolantTankAmountPerFuelRod;
            output = coolantTank = new CoolantTank(perSideCapacity, simulationDescription.defaultModeratorProperties);
            battery = null;
        }
        
        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                for (int k = 0; k < z; k++) {
                    var newProperties = simulationDescription.moderatorProperties[i][j][k];
                    if (simulationDescription.manifoldLocations[i][j][k]) {
                        newProperties = coolantTank;
                    }
                    if(newProperties == null){
                        newProperties = simulationDescription.defaultModeratorProperties;
                    }
                    if (controlRodsXZ[i][k] != null) {
                        newProperties = null;
                    }
                    moderatorProperties[i][j][k] = newProperties;
                }
            }
        }
        
        fuelTank.setCapacity(Config.CONFIG.Reactor.PerFuelRodCapacity * controlRods.length * y);
        
        double fuelToCasingRFKT = 0;
        int fuelToManifoldSurfaceArea = 0;
        for (SimUtil.ControlRod controlRod : controlRods) {
            for (int i = 0; i < y; i++) {
                for (Vector2ic direction : SimUtil.cardinalDirections) {
                    if (controlRod.x + direction.x() < 0 || controlRod.x + direction.x() >= x || controlRod.z + direction.y() < 0 || controlRod.z + direction.y() >= z) {
                        fuelToCasingRFKT += Config.CONFIG.Reactor.CasingHeatTransferRFMKT;
                        continue;
                    }
                    ReactorModeratorRegistry.IModeratorProperties properties = moderatorProperties[controlRod.x + direction.x()][i][controlRod.z + direction.y()];
                    if (properties != null) {
                        if (properties instanceof CoolantTank) {
                            // manifold, dynamic heat transfer rate
                            fuelToManifoldSurfaceArea++;
                        } else {
                            // normal block
                            fuelToCasingRFKT += properties.heatConductivity();
                        }
                    }
                }
            }
        }
        fuelToCasingRFKT *= Config.CONFIG.Reactor.FuelToStackRFKTMultiplier;
    
        double stackToCoolantSystemRFKT = 2 * (x * y + x * z + z * y);
        
        
        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                for (int k = 0; k < z; k++) {
                    ReactorModeratorRegistry.IModeratorProperties properties = moderatorProperties[i][j][k];
                    if (properties instanceof CoolantTank) {
                        // its a manifold here, need to consider its surface area
                        for (Vector3ic axisDirection : SimUtil.axisDirections) {
                            int neighborX = i + axisDirection.x();
                            int neighborY = j + axisDirection.y();
                            int neighborZ = k + axisDirection.z();
                            if (neighborX < 0 || neighborX >= this.x ||
                                    neighborY < 0 || neighborY >= this.y ||
                                    neighborZ < 0 || neighborZ >= this.z) {
                                // OOB, so its a casing we are against here, this counts against us
                                stackToCoolantSystemRFKT--;
                                continue;
                            }
                            ReactorModeratorRegistry.IModeratorProperties neighborProperties = moderatorProperties[neighborX][neighborY][neighborZ];
                            // should a fuel rod add to surface area? it does right now.
                            if (!(neighborProperties instanceof ICoolantTank)) {
                                stackToCoolantSystemRFKT++;
                            }
                        }
                    }
                }
            }
        }
        stackToCoolantSystemRFKT *= Config.CONFIG.Reactor.StackToCoolantRFMKT;
        
        if (simulationDescription.passivelyCooled) {
            stackToCoolantSystemRFKT *= Config.CONFIG.Reactor.PassiveCoolingTransferEfficiency;
        }
        
        this.casingToAmbientRFKT = 2 * ((x + 2) * (y + 2) + (x + 2) * (z + 2) + (z + 2) * (y + 2)) * Config.CONFIG.Reactor.StackToAmbientRFMKT;
        this.fuelToCasingRFKT = fuelToCasingRFKT;
        this.fuelToManifoldSurfaceArea = fuelToManifoldSurfaceArea;
        this.stackToCoolantSystemRFKT = stackToCoolantSystemRFKT;
        
        fuelHeat.setRfPerKelvin(controlRods.length * y * Config.CONFIG.Reactor.RodFEPerUnitVolumeKelvin);
        stackHeat.setRfPerKelvin(x * y * z * Config.CONFIG.Reactor.RodFEPerUnitVolumeKelvin);
        
        ambientHeat.setInfinite(true);
        ambientHeat.setTemperature(simulationDescription.ambientTemperature);
        stackHeat.setTemperature(simulationDescription.ambientTemperature);
        fuelHeat.setTemperature(simulationDescription.ambientTemperature);
        if (battery != null) {
            battery.setTemperature(simulationDescription.ambientTemperature);
        }
    }
    
    @Nullable
    @Override
    public PhosphophylliteCompound save() {
        var compound = new PhosphophylliteCompound();
        compound.put("fuelTank", fuelTank.save());
        if (coolantTank != null) {
            compound.put("coolantTank", coolantTank.save());
        }
        if (battery != null) {
            compound.put("battery", battery.save());
        }
        compound.put("fuelFertility", fuelFertility);
        compound.put("fuelHeat", fuelHeat.temperature());
        compound.put("reactorHeat", stackHeat.temperature());
        return compound;
    }
    
    @Override
    public void load(@Nonnull PhosphophylliteCompound compound) {
        fuelTank.load(compound.getCompound("fuelTank"));
        if (coolantTank != null) {
            coolantTank.load(compound.getCompound("coolantTank"));
        }
        if (battery != null) {
            battery.load(compound.getCompound("battery"));
        }
        fuelFertility = compound.getDouble("fuelFertility");
        fuelHeat.setTemperature(compound.getDouble("fuelHeat"));
        stackHeat.setTemperature(compound.getDouble("reactorHeat"));
    }
    
    @Override
    public void tick(boolean active) {
        if (active) {
            radiate();
        } else {
            fuelTank.burn(0);
        }
        
        {
            // decay fertility, RadiationHelper.tick in old BR, this is copied, mostly
            double denominator = Config.CONFIG.Reactor.FuelFertilityDecayDenominator;
            if (!active) {
                // Much slower decay when off
                denominator *= Config.CONFIG.Reactor.FuelFertilityDecayDenominatorInactiveMultiplier;
            }
            
            // Fertility decay, at least 0.1 rad/t, otherwise halve it every 10 ticks
            fuelFertility = Math.max(0f, fuelFertility - Math.max(Config.CONFIG.Reactor.FuelFertilityMinimumDecay, fuelFertility / denominator));
        }
        
        fuelHeat.transferWith(stackHeat, fuelToCasingRFKT + fuelToManifoldSurfaceArea * (coolantTank == null ? defaultModeratorProperties : coolantTank).heatConductivity());
        output.transferWith(stackHeat, stackToCoolantSystemRFKT);
        stackHeat.transferWith(ambientHeat, casingToAmbientRFKT);
    }
    
    private void radiate() {
        
        currentRod++;
        if(currentRod >= controlRods.length){
            currentRod = 0;
            currentY++;
            if(currentY >= y){
                currentY = 0;
            }
        }
        
        // Base value for radiation production penalties. 0-1, caps at about 3000C;
        final double radiationPenaltyBase = Math.exp(-Config.CONFIG.Reactor.RadPenaltyShiftMultiplier * Math.exp(-0.001 * Config.CONFIG.Reactor.RadPenaltyRateMultiplier * (fuelHeat.temperature() - 273.15)));
        
        // Raw amount - what's actually in the tanks
        // Effective amount - how
        final long baseFuelAmount = fuelTank.fuel() + (fuelTank.waste() / 100);
        
        // Intensity = how strong the radiation is, hardness = how energetic the radiation is (penetration)
        final double rawRadIntensity = (double) baseFuelAmount * Config.CONFIG.Reactor.FissionEventsPerFuelUnit;
        
        // Scale up the "effective" intensity of radiation, to provide an incentive for bigger reactors in general.
        // Scale up a second time based on scaled amount in each fuel rod. Provides an incentive for making reactors that aren't just pancakes.
        final double scaledRadIntensity = Math.pow((Math.pow((rawRadIntensity), Config.CONFIG.Reactor.FuelReactivity) / controlRods.length), Config.CONFIG.Reactor.FuelReactivity) * controlRods.length;
        
        // Radiation hardness starts at 20% and asymptotically approaches 100% as heat rises.
        // This will make radiation harder and harder to capture.
        final double initialHardness = Math.min(1.0, 0.2f + (0.8 * radiationPenaltyBase));
        
        final double rawIntensity = (1f + (-Config.CONFIG.Reactor.RadIntensityScalingMultiplier * Math.exp(-10f * Config.CONFIG.Reactor.RadIntensityScalingShiftMultiplier * Math.exp(-0.001f * Config.CONFIG.Reactor.RadIntensityScalingRateExponentMultiplier * (fuelHeat.temperature() - 273.15)))));
        final double fuelAbsorptionTemperatureCoefficient = (1.0 - (Config.CONFIG.Reactor.FuelAbsorptionScalingMultiplier * Math.exp(-10 * Config.CONFIG.Reactor.FuelAbsorptionScalingShiftMultiplier * Math.exp(-0.001 * Config.CONFIG.Reactor.FuelAbsorptionScalingRateExponentMultiplier * (fuelHeat.temperature() - 273.15)))));
        final double fuelHardnessMultiplier = 1 / Config.CONFIG.Reactor.FuelHardnessDivisor;
        
        double rawFuelUsage = 0;
        
        double fuelRFAdded = 0;
        double fuelRadAdded = 0;
        double caseRFAdded = 0;
        
        final var FuelPerRadiationUnit = Config.CONFIG.Reactor.FuelPerRadiationUnit;
        final var FEPerRadiationUnit = Config.CONFIG.Reactor.FEPerRadiationUnit;
        final var FuelUsageMultiplier = Config.CONFIG.Reactor.FuelUsageMultiplier;
        final var FuelAbsorptionCoefficient = Config.CONFIG.Reactor.FuelAbsorptionCoefficient;
        final var FuelModerationFactor = Config.CONFIG.Reactor.FuelModerationFactor;
        
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
                final int currentY = this.currentY + rayStep.offset.y;
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
            fuelFertility += fuelRadAdded;
        }
        if (!Double.isNaN(fuelRFAdded)) {
            fuelHeat.absorbRF(fuelRFAdded);
        }
        if (!Double.isNaN(caseRFAdded)) {
            stackHeat.absorbRF(caseRFAdded);
        }
        fuelTank.burn(rawFuelUsage);
    }
    
    @Override
    @Nullable
    public IBattery battery() {
        return battery;
    }
    
    @Override
    @Nullable
    public ICoolantTank coolantTank() {
        return coolantTank;
    }
    
    @Override
    public IFuelTank fuelTank() {
        return fuelTank;
    }
    
    @Nullable
    @Override
    public ControlRod controlRodAt(int x, int z) {
        if (x < 0 || x >= this.x || z < 0 || z >= this.z) {
            return null;
        }
        return controlRodsXZ[x][z];
    }
    
    @Override
    public double fertility() {
        if (fuelFertility <= 1f) {
            return 1f;
        } else {
            return Math.log10(fuelFertility) + 1;
        }
    }
    
    @Override
    public double fuelHeat() {
        return fuelHeat.temperature();
    }
    
    @Override
    public double stackHeat() {
        return stackHeat.temperature();
    }
    
    @Override
    public double ambientTemperature() {
        return ambientHeat.temperature();
    }
}
