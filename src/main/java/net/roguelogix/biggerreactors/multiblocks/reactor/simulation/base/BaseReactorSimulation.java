package net.roguelogix.biggerreactors.multiblocks.reactor.simulation.base;

import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.SimulationConfiguration;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.IReactorSimulation;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.SimulationDescription;
import net.roguelogix.biggerreactors.registries.ReactorModeratorRegistry;
import net.roguelogix.phosphophyllite.debug.DebugInfo;
import net.roguelogix.phosphophyllite.serialization.PhosphophylliteCompound;
import net.roguelogix.phosphophyllite.util.HeatBody;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2ic;
import org.joml.Vector3ic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class BaseReactorSimulation implements IReactorSimulation {
    
    protected final int x, y, z;
    protected final ReactorModeratorRegistry.IModeratorProperties defaultModeratorProperties;
    protected final ReactorModeratorRegistry.IModeratorProperties[][][] moderatorProperties;
    protected final SimUtil.ControlRod[][] controlRodsXZ;
    protected final SimUtil.ControlRod[] controlRods;
    
    protected final double fuelToCasingRFKT;
    protected final double fuelToManifoldSurfaceArea;
    protected final double stackToCoolantSystemRFKT;
    protected final double casingToAmbientRFKT;
    
    protected final HeatBody fuelHeat = new HeatBody();
    protected final HeatBody stackHeat = new HeatBody();
    protected final HeatBody ambientHeat = new HeatBody();
    
    @Nullable
    protected final Battery battery;
    @Nullable
    protected final CoolantTank coolantTank;
    protected final HeatBody output;
    
    protected final FuelTank fuelTank;
    
    protected double fuelFertility = 1;
    
    protected final SimulationConfiguration configuration;
    
    protected BaseReactorSimulation(SimulationDescription simulationDescription, SimulationConfiguration configuration) {
        this.configuration = configuration;
        x = simulationDescription.x();
        y = simulationDescription.y();
        z = simulationDescription.z();
        defaultModeratorProperties = simulationDescription.defaultModeratorProperties();
        
        // yes this gets trashed immediately, oh well
        moderatorProperties = new ReactorModeratorRegistry.IModeratorProperties[x][y][z];
        controlRodsXZ = new SimUtil.ControlRod[x][z];
        controlRods = new SimUtil.ControlRod[simulationDescription.controlRodCount()];
        
        {
            int currentControlRodIndex = 0;
            for (int i = 0; i < x; i++) {
                for (int j = 0; j < z; j++) {
                    if (simulationDescription.isControlRodAt(i, j)) {
                        var rod = new SimUtil.ControlRod(i, j);
                        controlRodsXZ[i][j] = rod;
                        controlRods[currentControlRodIndex++] = rod;
                    }
                }
            }
        }
        
        final ReactorModeratorRegistry.IModeratorProperties manifoldSignalingProperties;
        if (configuration.passivelyCooled()) {
            output = battery = new Battery((((long) (x + 2) * (y + 2) * (z + 2)) - ((long) x * y * z)) * configuration.passiveBatteryPerExternalBlock(), configuration);
            coolantTank = null;
            manifoldSignalingProperties = new ReactorModeratorRegistry.ModeratorProperties(defaultModeratorProperties);
        } else {
            long perSideCapacity = controlRods.length * y * configuration.coolantTankCapacityPerFuelRod();
            perSideCapacity += simulationDescription.manifoldCount() * configuration.coolantTankCapacityPerFuelRod();
            output = coolantTank = new CoolantTank(perSideCapacity, simulationDescription.defaultModeratorProperties(), configuration);
            battery = null;
            manifoldSignalingProperties = coolantTank;
        }
        
        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                for (int k = 0; k < z; k++) {
                    var newProperties = simulationDescription.moderatorPropertiesAt(i, j, k);
                    if (simulationDescription.isManifoldAt(i, j, k)) {
                        newProperties = manifoldSignalingProperties;
                    }
                    if (newProperties == null) {
                        newProperties = simulationDescription.defaultModeratorProperties();
                    }
                    if (controlRodsXZ[i][k] != null) {
                        newProperties = null;
                    }
                    moderatorProperties[i][j][k] = newProperties;
                }
            }
        }
    
        fuelTank = new FuelTank(configuration.fuelRodFuelCapacity() * controlRods.length * y);
        
        double fuelToCasingRFKT = 0;
        int fuelToManifoldSurfaceArea = 0;
        for (SimUtil.ControlRod controlRod : controlRods) {
            for (int i = 0; i < y; i++) {
                for (Vector2ic direction : SimUtil.cardinalDirections) {
                    if (controlRod.x + direction.x() < 0 || controlRod.x + direction.x() >= x || controlRod.z + direction.y() < 0 || controlRod.z + direction.y() >= z) {
                        fuelToCasingRFKT += configuration.casingHeatTransferRFMKT();
                        continue;
                    }
                    ReactorModeratorRegistry.IModeratorProperties properties = moderatorProperties[controlRod.x + direction.x()][i][controlRod.z + direction.y()];
                    if (properties != null) {
                        if (properties == manifoldSignalingProperties) {
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
        fuelToCasingRFKT *= configuration.fuelToStackRFKTMultiplier();
        
        double stackToCoolantSystemRFKT = 2 * (x * y + x * z + z * y);
        
        
        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                for (int k = 0; k < z; k++) {
                    ReactorModeratorRegistry.IModeratorProperties properties = moderatorProperties[i][j][k];
                    if (properties == manifoldSignalingProperties) {
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
                            if (neighborProperties != manifoldSignalingProperties) {
                                stackToCoolantSystemRFKT++;
                            }
                        }
                    }
                }
            }
        }
        stackToCoolantSystemRFKT *= configuration.stackToCoolantRFMKT();
        
        if (configuration.passivelyCooled()) {
            stackToCoolantSystemRFKT *= configuration.passiveCoolingTransferEfficiency();
        }
        
        this.casingToAmbientRFKT = 2 * ((x + 2) * (y + 2) + (x + 2) * (z + 2) + (z + 2) * (y + 2)) * configuration.stackToAmbientRFMKT();
        this.fuelToCasingRFKT = fuelToCasingRFKT;
        this.fuelToManifoldSurfaceArea = fuelToManifoldSurfaceArea;
        this.stackToCoolantSystemRFKT = stackToCoolantSystemRFKT;
        
        fuelHeat.setRfPerKelvin(controlRods.length * y * configuration.rodRFM3K());
        stackHeat.setRfPerKelvin(x * y * z * configuration.stackRFM3K());
        
        ambientHeat.setInfinite(true);
        ambientHeat.setTemperature(configuration.ambientTemperature());
        stackHeat.setTemperature(configuration.ambientTemperature());
        fuelHeat.setTemperature(configuration.ambientTemperature());
        if (battery != null) {
            battery.setTemperature(configuration.ambientTemperature());
        }
    }
    
    @Override
    public void tick(boolean active) {
        double toBurn = 0;
        if (active) {
            toBurn = radiate();
        } else {
            fuelTank.burn(0);
        }
        
        {
            // decay fertility, RadiationHelper.tick in old BR, this is copied, mostly
            double denominator = configuration.fuelFertilityDecayDenominator();
            if (!active) {
                // Much slower decay when off
                denominator *= configuration.fuelFertilityDecayDenominatorInactiveMultiplier();
            }
            
            // Fertility decay, at least 0.1 rad/t, otherwise halve it every 10 ticks
            fuelFertility = Math.max(0f, fuelFertility - Math.max(configuration.fuelFertilityMinimumDecay(), fuelFertility / denominator));
        }
        
        fuelHeat.transferWith(stackHeat, fuelToCasingRFKT + fuelToManifoldSurfaceArea * (coolantTank == null ? defaultModeratorProperties : coolantTank).heatConductivity());
        output.transferWith(stackHeat, stackToCoolantSystemRFKT);
        stackHeat.transferWith(ambientHeat, casingToAmbientRFKT);
        
        if(active){
            startNextRadiate();
            fuelTank.burn(toBurn);
        }
    }
    
    protected abstract double radiate();
    
    protected void startNextRadiate() {
    
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
    
    @NotNull
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
    public DebugInfo getDebugInfo() {
        final var simInfo = new DebugInfo("Simulation");
        simInfo.add("SimClass: " + getClass().getSimpleName());
        simInfo.add("FuelUsage: " + fuelTank().burnedLastTick());
        simInfo.add("ReactantCapacity: " + fuelTank().capacity());
        simInfo.add("TotalReactant: " + fuelTank().totalStored());
        simInfo.add("PercentFull: " + (float) fuelTank().totalStored() * 100 / fuelTank().capacity());
        simInfo.add("Fuel: " + fuelTank().fuel());
        simInfo.add("Waste: " + fuelTank().waste());
        simInfo.add("Fertility: " + fertility());
        simInfo.add("FuelHeat: " + fuelHeat());
        simInfo.add("ReactorHeat: " + stackHeat());
        if (battery != null) {
            final var batteryInfo = new DebugInfo("Battery");
            batteryInfo.add("StoredPower: " + battery.stored());
            batteryInfo.add("PowerProduction: " + battery.generatedLastTick());
            simInfo.add(batteryInfo);
        }
        if (coolantTank != null) {
            final var coolantTankInfo = new DebugInfo("CoolantTank");
            coolantTankInfo.add("MBProduction: " + coolantTank.transitionedLastTick());
            coolantTankInfo.add("CoolantTankSize: " + coolantTank.perSideCapacity());
            coolantTankInfo.add("Liquid: " + coolantTank.liquidAmount());
            coolantTankInfo.add("Vapor: " + coolantTank.vaporAmount());
            simInfo.add(coolantTankInfo);
        }
        final var buildInfo = new DebugInfo("Build info");
        buildInfo.add("Size: (" + x + ", " + y + ", " + z + ")");
        buildInfo.add("Control rod count: " + controlRods.length);
        buildInfo.add("fuelToCasingRFKT: " + fuelToCasingRFKT);
        buildInfo.add("fuelToManifoldSurfaceArea: " + fuelToManifoldSurfaceArea);
        buildInfo.add("stackToCoolantSystemRFKT: " + stackToCoolantSystemRFKT);
        buildInfo.add("casingToAmbientRFKT: " + casingToAmbientRFKT);
        simInfo.add(buildInfo);
        return simInfo;
    }
}
