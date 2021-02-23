package net.roguelogix.biggerreactors.classic.reactor.simulation.modern;

import net.minecraft.nbt.CompoundNBT;
import net.roguelogix.biggerreactors.Config;
import net.roguelogix.biggerreactors.classic.reactor.ReactorModeratorRegistry;
import net.roguelogix.biggerreactors.classic.reactor.simulation.IReactorBattery;
import net.roguelogix.biggerreactors.classic.reactor.simulation.IReactorCoolantTank;
import net.roguelogix.biggerreactors.classic.reactor.simulation.IReactorFuelTank;
import net.roguelogix.biggerreactors.classic.reactor.simulation.IReactorSimulation;
import net.roguelogix.phosphophyllite.repack.org.joml.Random;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector2i;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3d;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3i;
import net.roguelogix.phosphophyllite.util.HeatBody;

import java.util.ArrayList;

public class ModernReactorSimulation implements IReactorSimulation {
    private int x, y, z;
    private ReactorModeratorRegistry.IModeratorProperties[][][] moderatorProperties;
    private ControlRod[][] controlRodsXZ;
    private final ArrayList<ControlRod> controlRods = new ArrayList<>();
    
    private double fuelToCasingRFKT;
    private double fuelToManifoldSurfaceArea;
    private double casingToCoolantSystemRFKT;
    private double casingToAmbientRFKT;
    
    private final HeatBody fuelHeat = new HeatBody();
    private final HeatBody caseHeat = new HeatBody();
    private final HeatBody ambientHeat = new HeatBody();
    
    private double fuelFertility = 1;
    
    private HeatBody output;
    private final Battery battery = new Battery();
    private final CoolantTank coolantTank = new CoolantTank();
    
    private final FuelTank fuelTank = new FuelTank();
    
    private boolean passivelyCooled = true;
    
    private boolean active = false;
    public double fuelConsumedLastTick = 0;
    
    private final Vector2i[] cardinalDirections = new Vector2i[]{
            new Vector2i(1, 0),
            new Vector2i(-1, 0),
            new Vector2i(0, 1),
            new Vector2i(0, -1),
    };
    
    private final Vector3i[] axisDirections = new Vector3i[]{
            new Vector3i(+1, +0, +0),
            new Vector3i(-1, +0, +0),
            new Vector3i(+0, +1, +0),
            new Vector3i(+0, -1, +0),
            new Vector3i(+0, +0, +1),
            new Vector3i(+0, +0, -1)
    };
    
    {
        ambientHeat.setInfinite(true);
        // its in K, not C, this is 20C, just a default
        ambientHeat.setTemperature(293.15);
        caseHeat.setTemperature(293.15);
        battery.setTemperature(293.15);
    }
    
    private static class ControlRod {
        final int x;
        final int z;
        double insertion = 0;
        
        private ControlRod(int x, int z) {
            this.x = x;
            this.z = z;
        }
    }
    
    @Override
    public void resize(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        moderatorProperties = new ReactorModeratorRegistry.IModeratorProperties[x][y][z];
        controlRodsXZ = new ControlRod[x][z];
        controlRods.clear();
    }
    
    @Override
    public void setModeratorProperties(int x, int y, int z, ReactorModeratorRegistry.IModeratorProperties properties) {
        moderatorProperties[x][y][z] = properties;
    }
    
    @Override
    public void setControlRod(int x, int z) {
        ControlRod rod = new ControlRod(x, z);
        controlRods.add(rod);
        controlRodsXZ[x][z] = rod;
    }
    
    @Override
    public void setManifold(int x, int y, int z) {
        moderatorProperties[x][y][z] = coolantTank;
    }
    
    @Override
    public void setControlRodInsertion(int x, int z, double insertion) {
        controlRodsXZ[x][z].insertion = insertion;
    }
    
    @Override
    public void setPassivelyCooled(boolean passivelyCooled) {
        this.passivelyCooled = passivelyCooled;
        output = passivelyCooled ? battery : coolantTank;
    }
    
    @Override
    public void setAmbientTemperature(double temperature) {
        ambientHeat.setTemperature(temperature + 273.15);
        battery.setTemperature(temperature + 273.15);
    }
    
    @Override
    public boolean isPassive() {
        return passivelyCooled;
    }
    
    @Override
    public void updateInternalValues() {
        fuelTank.setCapacity(Config.Reactor.Modern.PerFuelRodCapacity * controlRods.size() * y);
        
        fuelToCasingRFKT = 0;
        fuelToManifoldSurfaceArea = 0;
        for (ControlRod controlRod : controlRods) {
            for (int i = 0; i < y; i++) {
                for (Vector2i direction : cardinalDirections) {
                    if (controlRod.x + direction.x < 0 || controlRod.x + direction.x >= x || controlRod.z + direction.y < 0 || controlRod.z + direction.y >= z) {
                        fuelToCasingRFKT += Config.Reactor.Modern.CasingHeatTransferRFMKT;
                        continue;
                    }
                    ReactorModeratorRegistry.IModeratorProperties properties = moderatorProperties[controlRod.x + direction.x][i][controlRod.z + direction.y];
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
        fuelToCasingRFKT *= Config.Reactor.Modern.FuelToCasingRFKTMultiplier;
        
        casingToCoolantSystemRFKT = 2 * (x * y + x * z + z * y);
        
        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                for (int k = 0; k < z; k++) {
                    ReactorModeratorRegistry.IModeratorProperties properties = moderatorProperties[i][j][k];
                    if (properties instanceof CoolantTank) {
                        // its a manifold here, need to consider its surface area
                        for (Vector3i axisDirection : axisDirections) {
                            int neighborX = i + axisDirection.x;
                            int neighborY = j + axisDirection.y;
                            int neighborZ = k + axisDirection.z;
                            if (neighborX < 0 || neighborX >= this.x ||
                                    neighborY < 0 || neighborY >= this.y ||
                                    neighborZ < 0 || neighborZ >= this.z) {
                                // OOB, so its a casing we are against here, this counts against us
                                casingToCoolantSystemRFKT--;
                                continue;
                            }
                            ReactorModeratorRegistry.IModeratorProperties neighborProperties = moderatorProperties[neighborX][neighborY][neighborZ];
                            // should a fuel rod add to surface area? it does right now.
                            if (!(neighborProperties instanceof CoolantTank)) {
                                casingToCoolantSystemRFKT++;
                            }
                        }
                    }
                }
            }
        }
        casingToCoolantSystemRFKT *= Config.Reactor.Modern.CasingToCoolantRFMKT;
        
        casingToAmbientRFKT = 2 * ((x + 2) * (y + 2) + (x + 2) * (z + 2) + (z + 2) * (y + 2)) * Config.Reactor.Modern.CasingToAmbientRFMKT;
        
        if (passivelyCooled) {
            casingToCoolantSystemRFKT *= Config.Reactor.Modern.PassiveCoolingTransferEfficiency;
            coolantTank.perSideCapacity = 0;
            battery.setCapacity((((long) (x + 2) * (y + 2) * (z + 2)) - ((long) x * y * z)) * Config.Reactor.Modern.PassiveBatteryPerExternalBlock);
        } else {
            coolantTank.perSideCapacity = controlRods.size() * y * Config.Reactor.Modern.CoolantTankAmountPerFuelRod;
        }
        
        fuelHeat.setRfPerKelvin(controlRods.size() * y * Config.Reactor.Modern.RodFEPerUnitVolumeKelvin);
        caseHeat.setRfPerKelvin(x * y * z * Config.Reactor.Modern.RodFEPerUnitVolumeKelvin);
    }
    
    @Override
    public void setActive(boolean active) {
        this.active = active;
    }
    
    @Override
    public void tick() {
        if (active) {
            radiate();
        } else {
            fuelConsumedLastTick = 0;
        }
        
        {
            // decay fertility, RadiationHelper.tick in old BR, this is copied, mostly
            double denominator = Config.Reactor.Modern.FuelFertilityDecayDenominator;
            if (!active) {
                // Much slower decay when off
                denominator *= Config.Reactor.Modern.FuelFertilityDecayDenominatorInactiveMultiplier;
            }
            
            // Fertility decay, at least 0.1 rad/t, otherwise halve it every 10 ticks
            fuelFertility = Math.max(0f, fuelFertility - Math.max(Config.Reactor.Modern.FuelFertilityMinimumDecay, fuelFertility / denominator));
        }
        
        fuelHeat.transferWith(caseHeat, fuelToCasingRFKT + fuelToManifoldSurfaceArea * coolantTank.heatConductivity());
        output.transferWith(caseHeat, casingToCoolantSystemRFKT);
        caseHeat.transferWith(ambientHeat, casingToAmbientRFKT);
    }
    
    
    private int rodToIrradiate = 0;
    private int yLevelToIrradiate = 0;
    
    private double neutronIntensity;
    private double neutronHardness;
    private double fuelRFAdded;
    private double fuelRadAdded;
    private double caseRFAdded;
    
    void radiate() {
        
        rodToIrradiate++;
        //  MultiblockReactor.updateServer
        // this is a different method, but im just picking a fuel rod to radiate from
        if (rodToIrradiate >= controlRods.size()) {
            rodToIrradiate = 0;
            yLevelToIrradiate++;
        }
        
        if (yLevelToIrradiate >= y) {
            yLevelToIrradiate = 0;
        }
        
        ControlRod rod = controlRods.get(rodToIrradiate);
        
        // Base value for radiation production penalties. 0-1, caps at about 3000C;
        double radiationPenaltyBase = Math.exp(-Config.Reactor.Modern.RadPenaltyShiftMultiplier * Math.exp(-0.001 * Config.Reactor.Modern.RadPenaltyRateMultiplier * (fuelHeat.temperature() - 273.15)));
        
        // Raw amount - what's actually in the tanks
        // Effective amount - how
        long baseFuelAmount = fuelTank.fuel() + (fuelTank.waste() / 100);
        
        // Intensity = how strong the radiation is, hardness = how energetic the radiation is (penetration)
        double rawRadIntensity = (double) baseFuelAmount * Config.Reactor.Modern.FissionEventsPerFuelUnit;
        
        // Scale up the "effective" intensity of radiation, to provide an incentive for bigger reactors in general.
        double scaledRadIntensity = Math.pow((rawRadIntensity), Config.Reactor.Modern.FuelReactivity);
        
        // Scale up a second time based on scaled amount in each fuel rod. Provides an incentive for making reactors that aren't just pancakes.
        scaledRadIntensity = Math.pow((scaledRadIntensity / controlRods.size()), Config.Reactor.Modern.FuelReactivity) * controlRods.size();
        
        // Apply control rod moderation of radiation to the quantity of produced radiation. 100% insertion = 100% reduction.
        double controlRodModifier = (100 - rod.insertion) / 100f;
        scaledRadIntensity = scaledRadIntensity * controlRodModifier;
        rawRadIntensity = rawRadIntensity * controlRodModifier;
        
        // Now nerf actual radiation production based on heat.
        double initialIntensity = scaledRadIntensity * (1f + (-Config.Reactor.Modern.RadIntensityScalingMultiplier * Math.exp(-10f * Config.Reactor.Modern.RadIntensityScalingShiftMultiplier * Math.exp(-0.001f * Config.Reactor.Modern.RadIntensityScalingRateExponentMultiplier * (fuelHeat.temperature() - 273.15)))));
        
        
        // Radiation hardness starts at 20% and asymptotically approaches 100% as heat rises.
        // This will make radiation harder and harder to capture.
        double initialHardness = 0.2f + (0.8 * radiationPenaltyBase);
        
        // Calculate based on propagation-to-self
        double rawFuelUsage = (Config.Reactor.Modern.FuelPerRadiationUnit * rawRadIntensity / fertility()) * Config.Reactor.FuelUsageMultiplier; // Not a typo. Fuel usage is thus penalized at high heats.
        fuelRFAdded = Config.Reactor.Modern.FEPerRadiationUnit * initialIntensity;
        
        double rayMultiplier = 1.0 / (double) Config.Reactor.Modern.RayCount;
        
        fuelRadAdded = 0;
        caseRFAdded = 0;
        
        for (int i = 0; i < Config.Reactor.Modern.RayCount; i++) {
            neutronHardness = initialHardness;
            neutronIntensity = initialIntensity * rayMultiplier;
            radiateFrom(rod.x, yLevelToIrradiate, rod.z);
        }
        
        
        if (!Double.isNaN(fuelRadAdded)) {
            fuelFertility += fuelRadAdded;
        }
        if (!Double.isNaN(fuelRFAdded)) {
            fuelHeat.absorbRF(fuelRFAdded);
        }
        if (!Double.isNaN(caseRFAdded)) {
            caseHeat.absorbRF(caseRFAdded);
        }
        fuelConsumedLastTick = fuelTank.burn(rawFuelUsage);
    }
    
    final Vector3d radiationDirection = new Vector3d();
    final Random random = new Random();
    
    final Vector3d currentSegment = new Vector3d();
    final Vector3d currentSegmentStart = new Vector3d();
    final Vector3d currentSegmentEnd = new Vector3d();
    final Vector3d currentSectionBlock = new Vector3d();
    final Vector3d planes = new Vector3d();
    double processedLength;
    
    final Vector3d[] intersections = new Vector3d[]{
            new Vector3d(),
            new Vector3d(),
            new Vector3d()
    };
    
    void radiateFrom(int x, int y, int z) {
        // ray tracing, because cardinal directions isn't good enough for me
        // also keeps you from building a skeleton reactor
        
        // pick a random ass direction for me to march in
        radiationDirection.set(random.nextFloat(), random.nextFloat(), random.nextFloat());
        radiationDirection.sub(0.5, 0.5, 0.5);
        radiationDirection.normalize();
        
        // radiation extends for RadiationBlocksToLive from the outside of the fuel rod
        // but i rotate about the center of the fuel rod, so, i need to add the length of the inside
        currentSegmentStart.set(radiationDirection);
        currentSegmentStart.mul(1 / Math.abs(currentSegmentStart.get(currentSegmentStart.maxComponent())));
        currentSegmentStart.mul(0.5);
        radiationDirection.mul(Config.Reactor.Modern.RadiationBlocksToLive + currentSegmentStart.length());
        
        processedLength = 0;
        double totalLength = radiationDirection.length();
        
        currentSegmentStart.set(0);
        
        // +0.5 or -0.5 for each of them, tells me which way i need to be looking for the intersections
        planes.set(radiationDirection);
        planes.absolute();
        planes.div(radiationDirection);
        planes.mul(0.5);
        
        boolean firstIteration = true;
        while (true) {
            for (int i = 0; i < 3; i++) {
                final Vector3d intersection = intersections[i];
                intersection.set(radiationDirection);
                double component = intersection.get(i);
                double plane = planes.get(i);
                intersection.mul(plane / component);
            }
            
            int minVec = 0;
            double minLength = Double.POSITIVE_INFINITY;
            for (int i = 0; i < 3; i++) {
                double length = intersections[i].lengthSquared();
                if (length < minLength) {
                    minVec = i;
                    minLength = length;
                }
            }
            
            // move the plane we just intersected back one
            planes.setComponent(minVec, planes.get(minVec) + (planes.get(minVec) / Math.abs(planes.get(minVec))));
            
            currentSegmentEnd.set(intersections[minVec]);
            currentSegment.set(currentSegmentEnd).sub(currentSegmentStart);
            currentSectionBlock.set(currentSegmentEnd).sub(currentSegmentStart).mul(0.5).add(0.5, 0.5, 0.5).add(currentSegmentStart).floor().add(x, y, z);
            
            if (currentSectionBlock.x < 0 || currentSectionBlock.x >= this.x ||
                    currentSectionBlock.y < 0 || currentSectionBlock.y >= this.y ||
                    currentSectionBlock.z < 0 || currentSectionBlock.z >= this.z) {
                break;
            }
            
            double segmentLength = currentSegment.length();
            boolean breakAfterLoop = processedLength + segmentLength >= totalLength;
            
            segmentLength = Math.min(totalLength - processedLength, segmentLength);
            ReactorModeratorRegistry.IModeratorProperties properties = moderatorProperties[(int) currentSectionBlock.x][(int) currentSectionBlock.y][(int) currentSectionBlock.z];
            
            if (!firstIteration && segmentLength != 0) {
                performIrradiation((int) currentSectionBlock.x, (int) currentSectionBlock.z, properties, segmentLength);
            }
            firstIteration = false;
            
            
            processedLength += segmentLength;
            if (breakAfterLoop || neutronIntensity < 0.0001f) {
                break;
            }
            
            currentSegmentStart.set(currentSegmentEnd);
        }
    }
    
    void performIrradiation(int x, int z, ReactorModeratorRegistry.IModeratorProperties properties, double effectMultiplier) {
        // TODO, use exponentials for the effect multiplier, linear doesnt describe it perfectly
        if (properties != null) {
            double radiationAbsorbed = neutronIntensity * properties.absorption() * (1f - neutronHardness) * effectMultiplier;
            neutronIntensity = Math.max(0f, neutronIntensity - radiationAbsorbed);
            neutronHardness = neutronHardness / (((properties.moderation() - 1.0) * effectMultiplier) + 1.0);
            caseRFAdded += properties.heatEfficiency() * radiationAbsorbed * Config.Reactor.Modern.FEPerRadiationUnit;
        } else {
            // its a fuel rod!
            
            // Scale control rod insertion 0..1
            double controlRodInsertion = Math.min(1f, Math.max(0f, controlRodsXZ[x][z].insertion / 100f));
            
            // Fuel absorptiveness is determined by control rod + a heat modifier.
            // Starts at 1 and decays towards 0.05, reaching 0.6 at 1000 and just under 0.2 at 2000. Inflection point at about 500-600.
            // Harder radiation makes absorption more difficult.
            double baseAbsorption = (1.0 - (Config.Reactor.Modern.FuelAbsorptionScalingMultiplier * Math.exp(-10 * Config.Reactor.Modern.FuelAbsorptionScalingShiftMultiplier * Math.exp(-0.001 * Config.Reactor.Modern.FuelAbsorptionScalingRateExponentMultiplier * (fuelHeat.temperature() - 273.15))))) * (1f - (neutronHardness / Config.Reactor.Modern.FuelHardnessDivisor));
            
            // Some fuels are better at absorbing radiation than others
            double scaledAbsorption = Math.min(1f, baseAbsorption * Config.Reactor.Modern.FuelAbsorptionCoefficient) * effectMultiplier;
            
            // Control rods increase total neutron absorption, but decrease the total neutrons which fertilize the fuel
            // Absorb up to 50% better with control rods inserted.
            double controlRodBonus = (1f - scaledAbsorption) * controlRodInsertion * 0.5f;
            double controlRodPenalty = scaledAbsorption * controlRodInsertion * 0.5f;
            
            double radiationAbsorbed = (scaledAbsorption + controlRodBonus) * neutronIntensity;
            double fertilityAbsorbed = (scaledAbsorption - controlRodPenalty) * neutronIntensity;
            
            double fuelModerationFactor = Config.Reactor.Modern.FuelModerationFactor;
            fuelModerationFactor += fuelModerationFactor * controlRodInsertion + controlRodInsertion; // Full insertion doubles the moderation factor of the fuel as well as adding its own level
            
            neutronIntensity = Math.max(0f, neutronIntensity - (radiationAbsorbed));
            neutronHardness = neutronHardness / (((fuelModerationFactor - 1.0) * effectMultiplier) + 1.0);
            
            // Being irradiated both heats up the fuel and also enhances its fertility
            fuelRFAdded += radiationAbsorbed * Config.Reactor.Modern.FEPerRadiationUnit;
            fuelRadAdded += fertilityAbsorbed;
        }
    }
    
    @Override
    public IReactorBattery battery() {
        return battery;
    }
    
    @Override
    public IReactorCoolantTank coolantTank() {
        return coolantTank;
    }
    
    @Override
    public IReactorFuelTank fuelTank() {
        return fuelTank;
    }
    
    @Override
    public long FEProducedLastTick() {
        return passivelyCooled ? battery.generatedLastTick() : coolantTank.rfTransferredLastTick();
    }
    
    @Override
    public long MBProducedLastTick() {
        return coolantTank.transitionedLastTick();
    }
    
    @Override
    public long maxMBProductionLastTick() {
        return coolantTank.maxTransitionedLastTick();
    }
    
    @Override
    public long outputLastTick() {
        return passivelyCooled ? battery.generatedLastTick() : coolantTank.transitionedLastTick();
    }
    
    @Override
    public double fuelConsumptionLastTick() {
        return fuelConsumedLastTick;
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
        return fuelHeat.temperature() - 273.15;
    }
    
    @Override
    public double caseHeat() {
        return caseHeat.temperature() - 273.15;
    }
    
    @Override
    public double ambientTemperature() {
        return ambientHeat.temperature() - 273.15;
    }
    
    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.put("fuelTank", fuelTank.serializeNBT());
        nbt.put("coolantTank", coolantTank.serializeNBT());
        nbt.put("battery", battery.serializeNBT());
        nbt.putDouble("fuelFertility", fuelFertility);
        nbt.putDouble("fuelHeat", fuelHeat.temperature() - 273.15);
        nbt.putDouble("reactorHeat", caseHeat.temperature() - 273.15);
        return nbt;
    }
    
    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        fuelTank.deserializeNBT(nbt.getCompound("fuelTank"));
        coolantTank.deserializeNBT(nbt.getCompound("coolantTank"));
        battery.deserializeNBT(nbt.getCompound("battery"));
        fuelFertility = nbt.getDouble("fuelFertility");
        fuelHeat.setTemperature(nbt.getDouble("fuelHeat") + 273.15);
        caseHeat.setTemperature(nbt.getDouble("reactorHeat") + 273.15);
    }
}
