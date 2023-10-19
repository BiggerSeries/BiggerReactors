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
                ((Number)jsonMap.get("ambientTemperature")).doubleValue(),
                (boolean) jsonMap.get("passivelyCooled"),
                ((Number)jsonMap.get("fuelUsageMultiplier")).doubleValue(),
                ((Number)jsonMap.get("outputMultiplier")).doubleValue(),
                ((Number)jsonMap.get("passiveOutputMultiplier")).doubleValue(),
                ((Number)jsonMap.get("activeOutputMultiplier")).doubleValue(),
                ((Number)jsonMap.get("fuelRodFuelCapacity")).longValue(),
                ((Number)jsonMap.get("fuelFertilityMinimumDecay")).doubleValue(),
                ((Number)jsonMap.get("fuelFertilityDecayDenominator")).doubleValue(),
                ((Number)jsonMap.get("fuelFertilityDecayDenominatorInactiveMultiplier")).doubleValue(),
                ((Number)jsonMap.get("casingHeatTransferRFMKT")).doubleValue(),
                ((Number)jsonMap.get("fuelToStackRFKTMultiplier")).doubleValue(),
                ((Number)jsonMap.get("stackToCoolantRFMKT")).doubleValue(),
                ((Number)jsonMap.get("stackToAmbientRFMKT")).doubleValue(),
                ((Number)jsonMap.get("passiveBatteryPerExternalBlock")).longValue(),
                ((Number)jsonMap.get("passiveCoolingTransferEfficiency")).doubleValue(),
                ((Number)jsonMap.get("coolantTankCapacityPerFuelRod")).longValue(),
                ((Number)jsonMap.get("stackRFM3K")).doubleValue(),
                ((Number)jsonMap.get("rodRFM3K")).doubleValue(),
                ((Number)jsonMap.get("fuelReactivity")).doubleValue(),
                ((Number)jsonMap.get("fissionEventsPerFuelUnit")).doubleValue(),
                ((Number)jsonMap.get("RFPerRadiationUnit")).doubleValue(),
                ((Number)jsonMap.get("fuelPerRadiationUnit")).doubleValue(),
                ((Number)jsonMap.get("fuelHardnessDivisor")).doubleValue(),
                ((Number)jsonMap.get("fuelAbsorptionCoefficient")).doubleValue(),
                ((Number)jsonMap.get("fuelModerationFactor")).doubleValue(),
                ((Number)jsonMap.get("radIntensityScalingMultiplier")).doubleValue(),
                ((Number)jsonMap.get("radIntensityScalingRateExponentMultiplier")).doubleValue(),
                ((Number)jsonMap.get("radIntensityScalingShiftMultiplier")).doubleValue(),
                ((Number)jsonMap.get("radPenaltyShiftMultiplier")).doubleValue(),
                ((Number)jsonMap.get("radPenaltyRateMultiplier")).doubleValue(),
                ((Number)jsonMap.get("fuelAbsorptionScalingMultiplier")).doubleValue(),
                ((Number)jsonMap.get("fuelAbsorptionScalingShiftMultiplier")).doubleValue(),
                ((Number)jsonMap.get("fuelAbsorptionScalingRateExponentMultiplier")).doubleValue(),
                ((Number)jsonMap.get("fuelRadScalingMultiplier")).doubleValue()
        );
    }
}
