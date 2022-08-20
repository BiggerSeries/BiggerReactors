package net.roguelogix.biggerreactors.multiblocks.reactor.simulation.cpu;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
//import jdk.incubator.vector.DoubleVector;
//import jdk.incubator.vector.VectorOperators;
import net.roguelogix.biggerreactors.Config;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.base.BaseReactorSimulation;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.base.ModeratorCache;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.base.SimUtil;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.SimulationDescription;
import net.roguelogix.biggerreactors.registries.ReactorModeratorRegistry;
import net.roguelogix.phosphophyllite.threading.Event;
import net.roguelogix.phosphophyllite.threading.Queues;

import javax.annotation.Nullable;

public class FullPassReactorSimulation extends BaseReactorSimulation {
    
    protected static final ReactorModeratorRegistry.IModeratorProperties CONTROL_ROD_MODERATOR = new ReactorModeratorRegistry.ModeratorProperties(-1, 0, 1, 0);
    protected final ObjectArrayList<ModeratorCache> moderatorCaches = new ObjectArrayList<>();
    protected final byte[] moderatorIndices;
    protected final double[] initialIntensties;
    
    public FullPassReactorSimulation(SimulationDescription simulationDescription) {
        super(simulationDescription);
        final ObjectArrayList<ReactorModeratorRegistry.IModeratorProperties> moderators = new ObjectArrayList<>();
        
        moderators.add(CONTROL_ROD_MODERATOR);
        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                for (int k = 0; k < z; k++) {
                    final var moderator = moderatorProperties[i][j][k];
                    if (moderator != null && !moderators.contains(moderator)) {
                        moderators.add(moderator);
                    }
                }
            }
        }
        if (moderators.size() > 127) {
            throw new IllegalArgumentException("Full pass reactor simulations only supports 127 moderator types, switch back to time sliced simulation to load world");
        }
        
        for (var moderator : moderators) {
            moderatorCaches.add(new ModeratorCache(moderator));
        }
        
        moderatorIndices = new byte[x * y * z];
        
        for (int i = 0; i < x; i++) {
            for (int j = 0; j < z; j++) {
                for (int k = 0; k < y; k++) {
                    var properties = moderatorProperties[i][k][j];
                    byte moderatorIndex = 0;
                    if (properties != null) {
                        moderatorIndex = (byte) moderators.indexOf(properties);
                    }
                    final int linearIndex = (((i * z) + j) * y) + k;
                    moderatorIndices[linearIndex] = moderatorIndex;
                }
            }
        }
        
        initialIntensties = new double[controlRods.length];
        
        fullPassIrradiationRequest = new IrradiationRequest(0, controlRods.length, this.moderatorCaches.toArray(new ModeratorCache[0]), y);
    }
    
    protected static class IrradiationRequest {
        public final int baseControlRod;
        public final int controlRodCount;
        public final IrradiationResult result = new IrradiationResult();
        public final ModeratorCache[] moderatorCache;
        public final double[] intensities;
        public final double[] hardnesses;
        
        public IrradiationRequest(int baseControlRod, int controlRodCount, ModeratorCache[] moderatorCache, int controlRodLength) {
            this.baseControlRod = baseControlRod;
            this.controlRodCount = controlRodCount;
            this.moderatorCache = new ModeratorCache[moderatorCache.length];
            for (int i = 0; i < moderatorCache.length; i++) {
                this.moderatorCache[i] = moderatorCache[i].duplicate();
            }
            intensities = new double[controlRodLength];
            hardnesses = new double[controlRodLength];
        }
        
        public void updateCache() {
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < moderatorCache.length; i++) {
                moderatorCache[i].update();
            }
        }
    }
    
    protected static class IrradiationResult {
        public double fuelRFAdded = 0;
        public double fuelRadAdded = 0;
        public double caseRFAdded = 0;
    }
    
    protected double fuelAbsorptionTemperatureCoefficient;
    protected double FuelAbsorptionCoefficient;
    protected double FuelModerationFactor;
    protected double fuelHardnessMultiplier;
    protected double rayMultiplier;
    protected double initialHardness;
    
    protected IrradiationRequest fullPassIrradiationRequest;
    
    protected double rawFuelUsage = 0;
    protected double fuelRFAdded = 0;
    protected double fuelRadAdded = 0;
    protected double caseRFAdded = 0;
    
    @Override
    protected double radiate() {
        if (fuelTank.fuel() <= 0) {
            return 0;
        }
        
        setupIrradiationTick();
        fullPassIrradiationRequest.updateCache();
        runIrradiationRequest(fullPassIrradiationRequest);
        collectIrradiationResult(fullPassIrradiationRequest.result);
        return realizeIrradiationTick();
    }
    
    protected void setupIrradiationTick() {
        moderatorCaches.forEach(ModeratorCache::update);
        
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
        initialHardness = Math.min(1.0, 0.2f + (0.8 * radiationPenaltyBase));
        
        final double rawIntensity = (1f + (-Config.CONFIG.Reactor.RadIntensityScalingMultiplier * Math.exp(-10f * Config.CONFIG.Reactor.RadIntensityScalingShiftMultiplier * Math.exp(-0.001f * Config.CONFIG.Reactor.RadIntensityScalingRateExponentMultiplier * (fuelHeat.temperature() - 273.15)))));
        fuelAbsorptionTemperatureCoefficient = (1.0 - (Config.CONFIG.Reactor.FuelAbsorptionScalingMultiplier * Math.exp(-10 * Config.CONFIG.Reactor.FuelAbsorptionScalingShiftMultiplier * Math.exp(-0.001 * Config.CONFIG.Reactor.FuelAbsorptionScalingRateExponentMultiplier * (fuelHeat.temperature() - 273.15)))));

//        final double controlRodModifier = 1.0 / controlRods.length;
        
        final double FuelUsageMultiplier = Config.CONFIG.Reactor.FuelUsageMultiplier;
        final double FuelPerRadiationUnit = Config.CONFIG.Reactor.FuelPerRadiationUnit;
        FuelAbsorptionCoefficient = Config.CONFIG.Reactor.FuelAbsorptionCoefficient;
        FuelModerationFactor = Config.CONFIG.Reactor.FuelModerationFactor;
        fuelHardnessMultiplier = 1 / Config.CONFIG.Reactor.FuelHardnessDivisor;
        rayMultiplier = 1.0 / (double) (SimUtil.rays.size() * y);
        
        double rawFuelUsage = 0;
        double fuelRFAdded = 0;
        
        for (int i = 0; i < controlRods.length; i++) {
            var rod = controlRods[i];
            
            // Apply control rod moderation of radiation to the quantity of produced radiation. 100% insertion = 100% reduction.
            final double controlRodModifier = (100 - rod.insertion) / 100f;
            final double effectiveRadIntensity = scaledRadIntensity * controlRodModifier;
            final double effectiveRawRadIntensity = rawRadIntensity * controlRodModifier;
            
            // Now nerf actual radiation production based on heat.
            final double initialIntensity = effectiveRadIntensity * rawIntensity;
            
            // Calculate based on propagation-to-self
            rawFuelUsage += (FuelPerRadiationUnit * effectiveRawRadIntensity / fertility()) * FuelUsageMultiplier; // Not a typo. Fuel usage is thus penalized at high heats.
            fuelRFAdded += initialIntensity;
            
            initialIntensties[i] = initialIntensity;
        }
        
        
        this.rawFuelUsage = rawFuelUsage / controlRods.length;
        this.fuelRFAdded = fuelRFAdded;
    }
    
    protected void collectIrradiationResult(IrradiationResult result) {
        fuelRFAdded += result.fuelRFAdded;
        fuelRadAdded += result.fuelRadAdded;
        caseRFAdded += result.caseRFAdded;
        result.fuelRFAdded = 0;
        result.fuelRadAdded = 0;
        result.caseRFAdded = 0;
    }
    
    protected double realizeIrradiationTick() {
        final double FEPerRadiationUnit = Config.CONFIG.Reactor.FEPerRadiationUnit;
        caseRFAdded *= FEPerRadiationUnit;
        fuelRFAdded *= FEPerRadiationUnit;
        
        fuelRFAdded /= controlRods.length;
        fuelRadAdded /= controlRods.length;
        caseRFAdded /= controlRods.length;
        
        if (!Double.isNaN(fuelRadAdded)) {
            if (Config.CONFIG.Reactor.fuelRadScalingMultiplier != 0) {
                fuelRadAdded *= Config.CONFIG.Reactor.fuelRadScalingMultiplier * (Config.CONFIG.Reactor.PerFuelRodCapacity / Math.max(1.0, (double) fuelTank().totalStored()));
            }
            fuelFertility += fuelRadAdded;
        }
        if (!Double.isNaN(fuelRFAdded)) {
            fuelHeat.absorbRF(fuelRFAdded);
        }
        if (!Double.isNaN(caseRFAdded)) {
            stackHeat.absorbRF(caseRFAdded);
        }
        
        fuelRFAdded = 0;
        fuelRadAdded = 0;
        caseRFAdded = 0;
        
        return rawFuelUsage;
    }
    
    protected void runIrradiationRequest(IrradiationRequest request) {
        final double FuelAbsorptionCoefficient = this.FuelAbsorptionCoefficient;
        final double FuelModerationFactor = this.FuelModerationFactor;
        final double fuelHardnessMultiplier = this.fuelHardnessMultiplier;
        final double rayMultiplier = this.rayMultiplier;
        final var moderatorCache = request.moderatorCache;
        double fuelRFAdded = 0;
        double fuelRadAdded = 0;
        double caseRFAdded = 0;
        double[] intensities = request.intensities;
        double[] hardnesses = request.hardnesses;
        int rods = 0;
        for (int cro = 0; cro < request.controlRodCount; cro++) {
            final int cri = cro + request.baseControlRod;
            final var controlRod = controlRods[cri];
            final var initialIntensity = initialIntensties[cri] * rayMultiplier;
            for (int i = 0; i < SimUtil.rays.size(); i++) {
                for (int j = 0; j < intensities.length; j++) {
                    intensities[j] = initialIntensity;
                    hardnesses[j] = initialHardness;
                }
                final var raySteps = SimUtil.rays.get(i);
                //noinspection ForLoopReplaceableByForEach
                for (int j = 0; j < raySteps.size(); j++) {
                    final var step = raySteps.get(j);
                    final int currentX = controlRod.x + step.offset.x;
                    final int offsetY = step.offset.y;
                    final int currentZ = controlRod.z + step.offset.z;
                    if (currentX < 0 || currentX >= this.x ||
                                currentZ < 0 || currentZ >= this.z) {
                        break;
                    }
                    int moderatorIndexIndex = ((((currentX * z) + currentZ)) * y);
                    final byte baseModeratorIndex = getModeratorIndex(moderatorIndexIndex);
                    if (baseModeratorIndex != 0) {
                        for (int k = 0; k < y; k++) {
                            final var currentY = k + offsetY;
                            if (currentY < 0) {
                                // skip until == 0
                                k -= currentY;
                                k--;
                                continue;
                            }
                            if (currentY >= y) {
                                break;
                            }
                            final double neutronIntensity = intensities[k];
                            final double neutronHardness = hardnesses[k];
                            
                            final byte moderatorIndex = moderatorIndices[moderatorIndexIndex + currentY];
                            final var properties = moderatorCache[moderatorIndex];
                            final double radiationAbsorbed = neutronIntensity * properties.absorption * (1.0 - neutronHardness) * step.length;
                            intensities[k] = Math.max(0, neutronIntensity - radiationAbsorbed);
                            hardnesses[k] = neutronHardness / ((properties.moderation * step.length) + 1.0);
                            caseRFAdded += properties.heatEfficiency * radiationAbsorbed;
                        }
                    } else {
                        // Scale control rod insertion 0..1
                        // TODO: race condition with computer craft is possible here
                        final double controlRodInsertion = controlRodsXZ[currentX][currentZ].insertion * .001;
                        final double halfRodInsertion = controlRodInsertion * 0.5;
                        // Full insertion doubles the moderation factor of the fuel as well as adding its own level
                        final double fuelModerationFactor = FuelModerationFactor + (FuelModerationFactor * controlRodInsertion + controlRodInsertion);
                        final double hardnessMultiplier = 1.0 / (((fuelModerationFactor - 1.0) * step.length) + 1.0);
                        final double stepFuelAbsorptionCoefficient = FuelAbsorptionCoefficient * step.length;
                        
                        for (int k = 0; k < y; k++) {
                            final var currentY = k + offsetY;
                            if (currentY < 0) {
                                // skip until == 0
                                k -= currentY;
                                k--;
                                continue;
                            }
                            if (currentY >= y) {
                                break;
                            }
                            rods++;
                            
                            final double neutronIntensity = intensities[k];
                            final double neutronHardness = hardnesses[k];
                            // Fuel absorptiveness is determined by control rod + a heat modifier.
                            // Starts at 1 and decays towards 0.05, reaching 0.6 at 1000 and just under 0.2 at 2000. Inflection point at about 500-600.
                            // Harder radiation makes absorption more difficult.
                            final double baseAbsorption = fuelAbsorptionTemperatureCoefficient * (1.0 - (neutronHardness * fuelHardnessMultiplier));
                            
                            // Some fuels are better at absorbing radiation than others
                            final double scaledAbsorption = baseAbsorption * stepFuelAbsorptionCoefficient;
                            
                            // Control rods increase total neutron absorption, but decrease the total neutrons which fertilize the fuel
                            // Absorb up to 50% better with control rods inserted.
                            final double controlRodBonus = (1.0 - scaledAbsorption) * halfRodInsertion;
                            final double controlRodPenalty = scaledAbsorption * halfRodInsertion;
                            
                            final double radiationAbsorbed = (scaledAbsorption + controlRodBonus) * neutronIntensity;
                            final double fertilityAbsorbed = (scaledAbsorption - controlRodPenalty) * neutronIntensity;
                            
                            intensities[k] = Math.max(0, neutronIntensity - (radiationAbsorbed));
                            hardnesses[k] = neutronHardness * hardnessMultiplier;
                            // Being irradiated both heats up the fuel and also enhances its fertility
                            fuelRFAdded += radiationAbsorbed;
                            fuelRadAdded += fertilityAbsorbed;
                        }
                    }
                }
            }
        }
        request.result.fuelRFAdded = fuelRFAdded;
        request.result.fuelRadAdded = fuelRadAdded;
        request.result.caseRFAdded = caseRFAdded;
    }
    
    protected byte getModeratorIndex(int moderatorIndexIndex) {
        return moderatorIndices[moderatorIndexIndex];
    }
    
    public static class MultiThreaded extends FullPassReactorSimulation {
        
        @Nullable
        protected final Runnable[] irradiationRequestRunnables;
        @Nullable
        protected final IrradiationRequest[] irradiationRequests;
        @Nullable
        protected final Event[] irradiationRequestEvents;
        @Nullable
        private Event doneEvent;
        private final Runnable mainRunnable = () -> runIrradiationRequest(fullPassIrradiationRequest);
        
        public MultiThreaded(SimulationDescription simulationDescription, boolean singleThread) {
            super(simulationDescription);
            
            if (!singleThread) {
                final var cacheArray = this.moderatorCaches.toArray(new ModeratorCache[0]);
                final int batchSize = Config.CONFIG.Reactor.ModeSpecific.ControlRodBatchSize;
                final int batches = controlRods.length / batchSize + ((controlRods.length % batchSize == 0) ? 0 : 1);
                irradiationRequestRunnables = new Runnable[batches];
                irradiationRequests = new IrradiationRequest[batches];
                irradiationRequestEvents = new Event[batches];
                for (int i = 0; i < batches; i++) {
                    int baseRod = i * batchSize;
                    int rodCount = Math.min(batchSize, controlRods.length - baseRod);
                    final var request = new IrradiationRequest(baseRod, rodCount, cacheArray, y);
                    irradiationRequestRunnables[i] = () -> runIrradiationRequest(request);
                    irradiationRequests[i] = request;
                }
            } else {
                irradiationRequestRunnables = null;
                irradiationRequests = null;
                irradiationRequestEvents = null;
            }
        }
        
        @Override
        protected double radiate() {
            if (irradiationRequests != null && irradiationRequestEvents != null && irradiationRequestRunnables != null) {
                for (int i = 0; i < irradiationRequests.length; i++) {
                    final var event = irradiationRequestEvents[i];
                    if (event != null) {
                        event.join();
                        irradiationRequestEvents[i] = null;
                        collectIrradiationResult(irradiationRequests[i].result);
                    }
                }
            } else {
                if (doneEvent != null) {
                    doneEvent.join();
                    doneEvent = null;
                    collectIrradiationResult(fullPassIrradiationRequest.result);
                }
            }
            return realizeIrradiationTick();
        }
        
        @Override
        protected void startNextRadiate() {
            if (fuelTank.fuel() <= 0) {
                return;
            }
            
            setupIrradiationTick();
            if (irradiationRequests != null && irradiationRequestEvents != null && irradiationRequestRunnables != null) {
                for (int i = 0; i < irradiationRequests.length; i++) {
                    irradiationRequests[i].updateCache();
                    irradiationRequestEvents[i] = Queues.offThread.enqueue(irradiationRequestRunnables[i]);
                }
            } else {
                fullPassIrradiationRequest.updateCache();
                doneEvent = Queues.offThread.enqueue(mainRunnable);
            }
        }
        
        @Override
        public boolean isAsync() {
            return true;
        }
    }
}
