package net.roguelogix.biggerreactors.multiblocks.reactor.simulation;

import net.roguelogix.biggerreactors.Config;

import java.util.Map;

public record SimulationConfiguration(
        double ambientTemperature,
        boolean passivelyCooled,
        double fuelUsageMultiplier,
        double outputMultiplier,
        double passiveOutputMultiplier,
        double activeOutputMultiplier,
        long fuelRodFuelCapacity,
        double fuelFertilityMinimumDecay,
        double fuelFertilityDecayDenominator,
        double fuelFertilityDecayDenominatorInactiveMultiplier,
        double casingHeatTransferRFMKT,
        double fuelToStackRFKTMultiplier,
        double stackToCoolantRFMKT,
        double stackToAmbientRFMKT,
        long passiveBatteryPerExternalBlock,
        double passiveCoolingTransferEfficiency,
        long coolantTankCapacityPerFuelRod,
        double stackRFM3K,
        double rodRFM3K,
        double fuelReactivity,
        double fissionEventsPerFuelUnit,
        double RFPerRadiationUnit,
        double fuelPerRadiationUnit,
        double fuelHardnessDivisor,
        double fuelAbsorptionCoefficient,
        double fuelModerationFactor,
        double radIntensityScalingMultiplier,
        double radIntensityScalingRateExponentMultiplier,
        double radIntensityScalingShiftMultiplier,
        double radPenaltyShiftMultiplier,
        double radPenaltyRateMultiplier,
        double fuelAbsorptionScalingMultiplier,
        double fuelAbsorptionScalingShiftMultiplier,
        double fuelAbsorptionScalingRateExponentMultiplier,
        double fuelRadScalingMultiplier
        // TODO: find a way to do this too, maybe
        //       these aren't reloadable values at all
        //       SimUtil actually uses these very early in startup
//        double irradiationDistance
//        double simulationRays
) {
    
    public SimulationConfiguration(double ambientTemperature, boolean passivelyCooled) {
        this(Config.CONFIG.Reactor, ambientTemperature, passivelyCooled);
    }
    
    public SimulationConfiguration(Config.Reactor reactorConfig, double ambientTemperature, boolean passivelyCooled) {
        this(ambientTemperature, passivelyCooled,
                reactorConfig.FuelUsageMultiplier,
                reactorConfig.OutputMultiplier,
                reactorConfig.PassiveOutputMultiplier,
                reactorConfig.ActiveOutputMultiplier,
                reactorConfig.PerFuelRodCapacity,
                reactorConfig.FuelFertilityMinimumDecay,
                reactorConfig.FuelFertilityDecayDenominator,
                reactorConfig.FuelFertilityDecayDenominatorInactiveMultiplier,
                reactorConfig.CasingHeatTransferRFMKT,
                reactorConfig.FuelToStackRFKTMultiplier,
                reactorConfig.StackToCoolantRFMKT,
                reactorConfig.StackToAmbientRFMKT,
                reactorConfig.PassiveBatteryPerExternalBlock,
                reactorConfig.PassiveCoolingTransferEfficiency,
                reactorConfig.CoolantTankAmountPerFuelRod,
                reactorConfig.CaseFEPerUnitVolumeKelvin,
                reactorConfig.RodFEPerUnitVolumeKelvin,
                reactorConfig.FuelReactivity,
                reactorConfig.FissionEventsPerFuelUnit,
                reactorConfig.FEPerRadiationUnit,
                reactorConfig.FuelPerRadiationUnit,
                reactorConfig.FuelHardnessDivisor,
                reactorConfig.FuelAbsorptionCoefficient,
                reactorConfig.FuelModerationFactor,
                reactorConfig.RadIntensityScalingMultiplier,
                reactorConfig.RadIntensityScalingRateExponentMultiplier,
                reactorConfig.RadIntensityScalingShiftMultiplier,
                reactorConfig.RadPenaltyShiftMultiplier,
                reactorConfig.RadPenaltyRateMultiplier,
                reactorConfig.FuelAbsorptionScalingMultiplier,
                reactorConfig.FuelAbsorptionScalingShiftMultiplier,
                reactorConfig.FuelAbsorptionScalingRateExponentMultiplier,
                reactorConfig.fuelRadScalingMultiplier
        );
    }
    
    public SimulationConfiguration(Map<String, Object> jsonMap) {
        this(
                (double) jsonMap.get("ambientTemperature"),
                (boolean) jsonMap.get("passivelyCooled"),
                (double) jsonMap.get("fuelUsageMultiplier"),
                (double) jsonMap.get("outputMultiplier"),
                (double) jsonMap.get("passiveOutputMultiplier"),
                (double) jsonMap.get("activeOutputMultiplier"),
                (long) jsonMap.get("fuelRodFuelCapacity"),
                (double) jsonMap.get("fuelFertilityMinimumDecay"),
                (double) jsonMap.get("fuelFertilityDecayDenominator"),
                (double) jsonMap.get("fuelFertilityDecayDenominatorInactiveMultiplier"),
                (double) jsonMap.get("casingHeatTransferRFMKT"),
                (double) jsonMap.get("fuelToStackRFKTMultiplier"),
                (double) jsonMap.get("stackToCoolantRFMKT"),
                (double) jsonMap.get("stackToAmbientRFMKT"),
                (long) jsonMap.get("passiveBatteryPerExternalBlock"),
                (double) jsonMap.get("passiveCoolingTransferEfficiency"),
                (long) jsonMap.get("coolantTankCapacityPerFuelRod"),
                (double) jsonMap.get("stackRFM3K"),
                (double) jsonMap.get("rodRFM3K"),
                (double) jsonMap.get("fuelReactivity"),
                (double) jsonMap.get("fissionEventsPerFuelUnit"),
                (double) jsonMap.get("RFPerRadiationUnit"),
                (double) jsonMap.get("fuelPerRadiationUnit"),
                (double) jsonMap.get("fuelHardnessDivisor"),
                (double) jsonMap.get("fuelAbsorptionCoefficient"),
                (double) jsonMap.get("fuelModerationFactor"),
                (double) jsonMap.get("radIntensityScalingMultiplier"),
                (double) jsonMap.get("radIntensityScalingRateExponentMultiplier"),
                (double) jsonMap.get("radIntensityScalingShiftMultiplier"),
                (double) jsonMap.get("radPenaltyShiftMultiplier"),
                (double) jsonMap.get("radPenaltyRateMultiplier"),
                (double) jsonMap.get("fuelAbsorptionScalingMultiplier"),
                (double) jsonMap.get("fuelAbsorptionScalingShiftMultiplier"),
                (double) jsonMap.get("fuelAbsorptionScalingRateExponentMultiplier"),
                (double) jsonMap.get("fuelRadScalingMultiplier")
        );
    }
}
