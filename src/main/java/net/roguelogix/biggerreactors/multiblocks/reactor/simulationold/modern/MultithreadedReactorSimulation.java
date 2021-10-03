package net.roguelogix.biggerreactors.multiblocks.reactor.simulationold.modern;

import net.minecraft.nbt.CompoundTag;
import net.roguelogix.biggerreactors.Config;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulationold.IReactorBattery;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulationold.IReactorCoolantTank;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulationold.IReactorFuelTank;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulationold.IReactorSimulation;
import net.roguelogix.biggerreactors.registries.ReactorModeratorRegistry;
import net.roguelogix.phosphophyllite.registry.OnModLoad;
import net.roguelogix.phosphophyllite.repack.org.joml.*;
import net.roguelogix.phosphophyllite.threading.Event;
import net.roguelogix.phosphophyllite.threading.Queues;
import net.roguelogix.phosphophyllite.util.HeatBody;

import javax.annotation.Nonnull;
import java.lang.Math;
import java.util.ArrayList;

public class MultithreadedReactorSimulation implements IReactorSimulation {
    private static class ControlRod {
        final int x;
        final int z;
        double insertion = 0;
        
        double initialHardness = 0;
        double initialIntensity = 0;
        double fuelAbsorptionTemperatureCoefficient = 0;
        
        private ControlRod(int x, int z) {
            this.x = x;
            this.z = z;
        }
    }
    
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
    
    private static final Vector2ic[] cardinalDirections = new Vector2ic[]{
            new Vector2i(1, 0),
            new Vector2i(-1, 0),
            new Vector2i(0, 1),
            new Vector2i(0, -1),
    };
    
    private static final Vector3ic[] axisDirections = new Vector3ic[]{
            new Vector3i(+1, +0, +0),
            new Vector3i(-1, +0, +0),
            new Vector3i(+0, +1, +0),
            new Vector3i(+0, -1, +0),
            new Vector3i(+0, +0, +1),
            new Vector3i(+0, +0, -1)
    };
    
    private static class RayStep {
        final Vector3i offset;
        final double length;
        
        private RayStep(Vector3i offset, double length) {
            this.offset = offset;
            this.length = length;
        }
    }
    
    private static final ArrayList<ArrayList<RayStep>> rays = new ArrayList<>();
    
    private static final Vector3dc[] rayDirections = new Vector3dc[]{
            new Vector3d(+1, 0, 0),
            new Vector3d(-1, 0, 0),
            new Vector3d(0, +1, 0),
            new Vector3d(0, -1, 0),
            new Vector3d(0, 0, +1),
            new Vector3d(0, 0, -1),
            
            
            new Vector3d(+1, +1, 0),
            new Vector3d(+1, -1, 0),
            new Vector3d(-1, +1, 0),
            new Vector3d(-1, -1, 0),
            
            new Vector3d(0, +1, +1),
            new Vector3d(0, +1, -1),
            new Vector3d(0, -1, +1),
            new Vector3d(0, -1, -1),
            
            new Vector3d(+1, 0, +1),
            new Vector3d(-1, 0, +1),
            new Vector3d(+1, 0, -1),
            new Vector3d(-1, 0, -1),
    };
    
    @OnModLoad
    private static void onModLoad() {
        // i cannot rely on the config being loaded yet, so, im just going to make the assumption that its TTL of 4, its probably not been changed
        // once i update the registry with more strict ordering, then i can rely on the config being loaded, but until then, *it might not be*
        
        final Vector3d radiationDirection = new Vector3d();
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
        
        // ray tracing, because cardinal directions isn't good enough for me
        // also keeps you from building a skeleton reactor
        
        for (Vector3dc rayDirection : rayDirections) {
            final ArrayList<RayStep> raySteps = new ArrayList<>();
            
            radiationDirection.set(rayDirection);
            radiationDirection.sub(0.5, 0.5, 0.5);
            radiationDirection.normalize();
            
            // radiation extends for RadiationBlocksToLive from the outside of the fuel rod
            // but i rotate about the center of the fuel rod, so, i need to add the length of the inside
            currentSegmentStart.set(radiationDirection);
            currentSegmentStart.mul(1 / Math.abs(currentSegmentStart.get(currentSegmentStart.maxComponent())));
            currentSegmentStart.mul(0.5);
            radiationDirection.mul(4 + currentSegmentStart.length());
            
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
                currentSectionBlock.set(currentSegmentEnd).sub(currentSegmentStart).mul(0.5).add(0.5, 0.5, 0.5).add(currentSegmentStart).floor();
                
                double segmentLength = currentSegment.length();
                boolean breakAfterLoop = processedLength + segmentLength >= totalLength;
                
                segmentLength = Math.min(totalLength - processedLength, segmentLength);
                
                if (!firstIteration && segmentLength != 0) {
                    raySteps.add(new RayStep(new Vector3i(currentSectionBlock, 0), segmentLength));
                }
                firstIteration = false;
                
                
                processedLength += segmentLength;
                if (breakAfterLoop) {
                    break;
                }
                
                currentSegmentStart.set(currentSegmentEnd);
            }
            rays.add(raySteps);
        }
    }
    
    public MultithreadedReactorSimulation(double ambientTemperature) {
        ambientHeat.setInfinite(true);
        ambientHeat.setTemperature(ambientTemperature + 273.15);
        caseHeat.setTemperature(ambientTemperature + 273.15);
        fuelHeat.setTemperature(ambientTemperature + 273.15);
        battery.setTemperature(ambientTemperature + 273.15);
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
    public boolean isPassive() {
        return passivelyCooled;
    }
    
    @Override
    public void updateInternalValues() {
        fuelTank.setCapacity(Config.CONFIG.Reactor.PerFuelRodCapacity * controlRods.size() * y);
        
        fuelToCasingRFKT = 0;
        fuelToManifoldSurfaceArea = 0;
        for (ControlRod controlRod : controlRods) {
            for (int i = 0; i < y; i++) {
                for (Vector2ic direction : cardinalDirections) {
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
        
        casingToCoolantSystemRFKT = 2 * (x * y + x * z + z * y);
        
        int manifoldCount = 0;
        
        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                for (int k = 0; k < z; k++) {
                    ReactorModeratorRegistry.IModeratorProperties properties = moderatorProperties[i][j][k];
                    if (properties instanceof CoolantTank) {
                        manifoldCount++;
                        // its a manifold here, need to consider its surface area
                        for (Vector3ic axisDirection : axisDirections) {
                            int neighborX = i + axisDirection.x();
                            int neighborY = j + axisDirection.y();
                            int neighborZ = k + axisDirection.z();
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
        casingToCoolantSystemRFKT *= Config.CONFIG.Reactor.StackToCoolantRFMKT;
        
        casingToAmbientRFKT = 2 * ((x + 2) * (y + 2) + (x + 2) * (z + 2) + (z + 2) * (y + 2)) * Config.CONFIG.Reactor.StackToAmbientRFMKT;
        
        if (passivelyCooled) {
            casingToCoolantSystemRFKT *= Config.CONFIG.Reactor.PassiveCoolingTransferEfficiency;
            coolantTank.perSideCapacity = 0;
            battery.setCapacity((((long) (x + 2) * (y + 2) * (z + 2)) - ((long) x * y * z)) * Config.CONFIG.Reactor.PassiveBatteryPerExternalBlock);
        } else {
            coolantTank.perSideCapacity = controlRods.size() * y * Config.CONFIG.Reactor.CoolantTankAmountPerFuelRod;
            coolantTank.perSideCapacity += manifoldCount * Config.CONFIG.Reactor.CoolantTankAmountPerFuelRod;
        }
        
        fuelHeat.setRfPerKelvin(controlRods.size() * y * Config.CONFIG.Reactor.RodFEPerUnitVolumeKelvin);
        caseHeat.setRfPerKelvin(x * y * z * Config.CONFIG.Reactor.RodFEPerUnitVolumeKelvin);
        
        fuelRods.clear();
        fuelRods.ensureCapacity(controlRods.size() * y);
        for (ControlRod rod : controlRods) {
            for (int j = 0; j < y; j++) {
                fuelRods.add(new FuelRod(rod, j));
            }
        }
        
        final int BATCH_SIZE = Config.CONFIG.Reactor.Experimental.RodBatchSize;
        
        batches.clear();
        int handledRods = 0;
        int rodsToHandle = fuelRods.size();
        while (rodsToHandle >= BATCH_SIZE) {
            batches.add(new RodBatch(handledRods, BATCH_SIZE));
            handledRods += BATCH_SIZE;
            rodsToHandle -= BATCH_SIZE;
        }
        batches.add(new RodBatch(handledRods, rodsToHandle));
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
            double denominator = Config.CONFIG.Reactor.FuelFertilityDecayDenominator;
            if (!active) {
                // Much slower decay when off
                denominator *= Config.CONFIG.Reactor.FuelFertilityDecayDenominatorInactiveMultiplier;
            }
            
            // Fertility decay, at least 0.1 rad/t, otherwise halve it every 10 ticks
            fuelFertility = Math.max(0f, fuelFertility - Math.max(Config.CONFIG.Reactor.FuelFertilityMinimumDecay, fuelFertility / denominator));
        }
        
        fuelHeat.transferWith(caseHeat, fuelToCasingRFKT + fuelToManifoldSurfaceArea * coolantTank.heatConductivity());
        output.transferWith(caseHeat, casingToCoolantSystemRFKT);
        caseHeat.transferWith(ambientHeat, casingToAmbientRFKT);
    }
    
    private final ArrayList<FuelRod> fuelRods = new ArrayList<>();
    private final ArrayList<RodBatch> batches = new ArrayList<>();
    
    private static class FuelRod {
        final ControlRod controlRod;
        final int y;
        
        private FuelRod(ControlRod controlRod, int y) {
            this.controlRod = controlRod;
            this.y = y;
        }
    }
    
    private class RodBatch {
        final int startingRod;
        final int size;
        
        private RodBatch(int startingRod, int size) {
            this.startingRod = startingRod;
            this.size = size;
            lastTickDone.trigger();
        }
        
        class Results {
            double caseRFAdded = 0;
            double fuelRFAdded = 0;
            double fuelRadAdded = 0;
        }
        
        private final Results results = new Results();
        
        @Nonnull
        private Event lastTickDone = new Event();
        
        Results results() {
            lastTickDone.join();
            return results;
        }
        
        void run() {
            results.caseRFAdded = 0;
            results.fuelRFAdded = 0;
            results.fuelRadAdded = 0;
            lastTickDone = Queues.offThread.enqueue(this::doRun, lastTickDone);
        }
        
        private void doRun() {
            for (int i = startingRod; i < fuelRods.size() && i < startingRod + size; i++) {
                FuelRod rod = fuelRods.get(i);
                runFuelRodSim(rod.controlRod.x, rod.y, rod.controlRod.z, rod.controlRod.initialHardness, rod.controlRod.initialIntensity, rod.controlRod.fuelAbsorptionTemperatureCoefficient, results);
            }
        }
    }
    
    private void runFuelRodSim(int X, int Y, int Z, double initialHardness, double initialIntensity, double fuelAbsorptionTemperatureCoefficient, RodBatch.Results results) {
        for (int j = 0; j < rays.size(); j++) {
            ArrayList<RayStep> raySteps = rays.get(j);
            double neutronHardness = initialHardness;
            double neutronIntensity = initialIntensity;
            for (int k = 0; k < raySteps.size(); k++) {
                RayStep rayStep = raySteps.get(k);
                int currentX = X + rayStep.offset.x;
                int currentY = Y + rayStep.offset.y;
                int currentZ = Z + rayStep.offset.z;
                if (currentX < 0 || currentX >= this.x ||
                        currentY < 0 || currentY >= this.y ||
                        currentZ < 0 || currentZ >= this.z) {
                    break;
                }
                ReactorModeratorRegistry.IModeratorProperties properties = moderatorProperties[currentX][currentY][currentZ];
                if (properties != null) {
                    double radiationAbsorbed = neutronIntensity * properties.absorption() * (1f - neutronHardness) * rayStep.length;
                    neutronIntensity = Math.max(0, neutronIntensity - radiationAbsorbed);
                    neutronHardness = neutronHardness / (((properties.moderation() - 1.0) * rayStep.length) + 1.0);
                    results.caseRFAdded += properties.heatEfficiency() * radiationAbsorbed * Config.CONFIG.Reactor.FEPerRadiationUnit;
                } else {
                    // its a fuel rod!
                    
                    // Scale control rod insertion 0..1
                    double controlRodInsertion = controlRodsXZ[currentX][currentZ].insertion * .001;
                    
                    // Fuel absorptiveness is determined by control rod + a heat modifier.
                    // Starts at 1 and decays towards 0.05, reaching 0.6 at 1000 and just under 0.2 at 2000. Inflection point at about 500-600.
                    // Harder radiation makes absorption more difficult.
                    double baseAbsorption = fuelAbsorptionTemperatureCoefficient * (1f - (neutronHardness / Config.CONFIG.Reactor.FuelHardnessDivisor));
                    
                    // Some fuels are better at absorbing radiation than others
                    double scaledAbsorption = baseAbsorption * Config.CONFIG.Reactor.FuelAbsorptionCoefficient * rayStep.length;
                    
                    // Control rods increase total neutron absorption, but decrease the total neutrons which fertilize the fuel
                    // Absorb up to 50% better with control rods inserted.
                    double controlRodBonus = (1f - scaledAbsorption) * controlRodInsertion * 0.5f;
                    double controlRodPenalty = scaledAbsorption * controlRodInsertion * 0.5f;
                    
                    double radiationAbsorbed = (scaledAbsorption + controlRodBonus) * neutronIntensity;
                    double fertilityAbsorbed = (scaledAbsorption - controlRodPenalty) * neutronIntensity;
                    
                    double fuelModerationFactor = Config.CONFIG.Reactor.FuelModerationFactor;
                    fuelModerationFactor += fuelModerationFactor * controlRodInsertion + controlRodInsertion; // Full insertion doubles the moderation factor of the fuel as well as adding its own level
                    
                    neutronIntensity = Math.max(0, neutronIntensity - (radiationAbsorbed));
                    neutronHardness = neutronHardness / (((fuelModerationFactor - 1.0) * rayStep.length) + 1.0);
                    
                    // Being irradiated both heats up the fuel and also enhances its fertility
                    results.fuelRFAdded += radiationAbsorbed * Config.CONFIG.Reactor.FEPerRadiationUnit;
                    results.fuelRadAdded += fertilityAbsorbed;
                }
            }
        }
    }
    
    private void radiate() {
        
        double fuelRFAdded = 0;
        double fuelRadAdded = 0;
        double caseRFAdded = 0;
        
        for (RodBatch batch : batches) {
            RodBatch.Results results = batch.results();
            
            fuelRFAdded += results.fuelRFAdded;
            fuelRadAdded += results.fuelRadAdded;
            caseRFAdded += results.caseRFAdded;
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
        final double scaledRadIntensity = Math.pow((Math.pow((rawRadIntensity), Config.CONFIG.Reactor.FuelReactivity) / controlRods.size()), Config.CONFIG.Reactor.FuelReactivity) * controlRods.size();
        
        // Radiation hardness starts at 20% and asymptotically approaches 100% as heat rises.
        // This will make radiation harder and harder to capture.
        final double initialHardness = Math.min(1.0, 0.2f + (0.8 * radiationPenaltyBase));
        
        final double rawIntensity = (1f + (-Config.CONFIG.Reactor.RadIntensityScalingMultiplier * Math.exp(-10f * Config.CONFIG.Reactor.RadIntensityScalingShiftMultiplier * Math.exp(-0.001f * Config.CONFIG.Reactor.RadIntensityScalingRateExponentMultiplier * (fuelHeat.temperature() - 273.15)))));
        final double fuelAbsorptionTemperatureCoefficient = (1.0 - (Config.CONFIG.Reactor.FuelAbsorptionScalingMultiplier * Math.exp(-10 * Config.CONFIG.Reactor.FuelAbsorptionScalingShiftMultiplier * Math.exp(-0.001 * Config.CONFIG.Reactor.FuelAbsorptionScalingRateExponentMultiplier * (fuelHeat.temperature() - 273.15)))));
        
        double rawFuelUsage = 0;
        
        final double rayMultiplier = 1.0 / (double) (rays.size() * y);
        
        for (int r = 0; r < controlRods.size(); r++) {
            ControlRod rod = controlRods.get(r);
            
            // Apply control rod moderation of radiation to the quantity of produced radiation. 100% insertion = 100% reduction.
            double controlRodModifier = (100 - rod.insertion) / 100f;
            double effectiveRadIntensity = scaledRadIntensity * controlRodModifier;
            double effectiveRawRadIntensity = rawRadIntensity * controlRodModifier;
            
            // Now nerf actual radiation production based on heat.
            double initialIntensity = effectiveRadIntensity * rawIntensity;
            
            // Calculate based on propagation-to-self
            rawFuelUsage += (Config.CONFIG.Reactor.FuelPerRadiationUnit * effectiveRawRadIntensity / fertility()) * Config.CONFIG.Reactor.FuelUsageMultiplier; // Not a typo. Fuel usage is thus penalized at high heats.
            fuelRFAdded += Config.CONFIG.Reactor.FEPerRadiationUnit * initialIntensity;
            
            rod.initialHardness = initialHardness;
            rod.initialIntensity = initialIntensity * rayMultiplier;
            rod.fuelAbsorptionTemperatureCoefficient = fuelAbsorptionTemperatureCoefficient;
        }
        
        rawFuelUsage /= controlRods.size();
        fuelRFAdded /= controlRods.size();
        fuelRadAdded /= controlRods.size();
        caseRFAdded /= controlRods.size();
        
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
        
        batches.forEach(RodBatch::run);
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
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.put("fuelTank", fuelTank.serializeNBT());
        nbt.put("coolantTank", coolantTank.serializeNBT());
        nbt.put("battery", battery.serializeNBT());
        nbt.putDouble("fuelFertility", fuelFertility);
        nbt.putDouble("fuelHeat", fuelHeat.temperature() - 273.15);
        nbt.putDouble("reactorHeat", caseHeat.temperature() - 273.15);
        return nbt;
    }
    
    @Override
    public void deserializeNBT(CompoundTag nbt) {
        fuelTank.deserializeNBT(nbt.getCompound("fuelTank"));
        coolantTank.deserializeNBT(nbt.getCompound("coolantTank"));
        battery.deserializeNBT(nbt.getCompound("battery"));
        fuelFertility = nbt.getDouble("fuelFertility");
        fuelHeat.setTemperature(nbt.getDouble("fuelHeat") + 273.15);
        caseHeat.setTemperature(nbt.getDouble("reactorHeat") + 273.15);
    }
}
