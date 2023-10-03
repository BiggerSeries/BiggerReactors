package net.roguelogix.biggerreactors.multiblocks.reactor.simulation;

import net.roguelogix.biggerreactors.Config;

public record SimulationConfiguration(
        double ambientTemperature,
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
    
    public SimulationConfiguration(Config.Reactor reactorConfig, double ambientTemperature) {
        this(ambientTemperature,
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
}
