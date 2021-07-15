package net.roguelogix.biggerreactors;

import net.roguelogix.phosphophyllite.config.PhosphophylliteConfig;
import net.roguelogix.phosphophyllite.registry.RegisterConfig;

@SuppressWarnings({"FieldMayBeFinal", "unused"})
@RegisterConfig
@PhosphophylliteConfig
public class Config {
    
    @PhosphophylliteConfig.EnableAdvanced
    @PhosphophylliteConfig.Value(hidden = true)
    private static boolean EnableAdvancedConfig = false;
    
    public enum Modes {
        // classic BR, if available
        CLASSIC,
        // Modern BiR, familiar yet different
        MODERN,
        // Mechanics of modern, usually, may be unstable, may be more resource intensive, may not even run
        // if not applicable to multiblock, defaults to modern
        EXPERIMENTAL,
    }
    
    @PhosphophylliteConfig.Value(advanced = true)
    public static Modes mode = Modes.MODERN;
    
    @PhosphophylliteConfig
    public static class WorldGen {
        @PhosphophylliteConfig.Value(range = "[1,)")
        public static int YelloriteOreMaxClustersPerChunk = 5;
        @PhosphophylliteConfig.Value(range = "[1,)")
        public static int YelloriteMaxOrePerCluster = 10;
        @PhosphophylliteConfig.Value(range = "[5,)")
        public static int YelloriteOreMaxSpawnY = 50;
        @PhosphophylliteConfig.Value
        public static boolean EnableYelloriteGeneration = true;
    }
    
    @PhosphophylliteConfig
    public static class Reactor {
        //TODO: remove max, its only there because of the render system
        //      multiblock system can take *much* larger structures
        @PhosphophylliteConfig.Value(range = "[3,192]")
        public static int MaxLength = 128;
        @PhosphophylliteConfig.Value(range = "[3,192]")
        public static int MaxWidth = 128;
        @PhosphophylliteConfig.Value(range = "[3,256]")
        public static int MaxHeight = 192;
        
        @PhosphophylliteConfig.Value(range = "(0,)")
        public static double FuelUsageMultiplier = 1;
        @PhosphophylliteConfig.Value(range = "(0,)")
        public static double OutputMultiplier = 1.0f;
        @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
        public static double PassiveOutputMultiplier = 0.5f;
        @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
        public static double ActiveOutputMultiplier = 1.0f;
        @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
        public static long FuelMBPerIngot = 1000;
        
        @PhosphophylliteConfig
        public static class Classic {
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double AmbientTemperature = 20.0f;
            @PhosphophylliteConfig.Value(range = "[1,)", advanced = true)
            public static long PerFuelRodCapacity = 4000;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double FuelFertilityMinimumDecay = 0.1f;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double FuelFertilityDecayDenominator = 20;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double FuelFertilityDecayDenominatorInactiveMultiplier = 200;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double FuelReactivity = 1.05f;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double FissionEventsPerFuelUnit = 0.01f;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double FEPerRadiationUnit = 10f;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double FuelPerRadiationUnit = 0.0007f;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static long IrradiationDistance = 4;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double FuelHardnessDivisor = 1f;
            @PhosphophylliteConfig.Value(range = "[0,1]", advanced = true)
            public static double FuelAbsorptionCoefficient = 0.5f;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double FuelModerationFactor = 1.5f;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double FEPerCentigradePerUnitVolume = 10.0f;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double FuelToCasingTransferCoefficientMultiplier = 1.0f;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double CasingToCoolantSystemCoefficientMultiplier = 0.6f;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double HeatLossCoefficientMultiplier = 0.001f;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double PassiveCoolingTransferEfficiency = 0.2f;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double CasingHeatTransferCoefficient = 0.6f;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static long PassiveBatteryPerExternalBlock = 10_000;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static long CoolantTankAmountPerExternalBlock = 100;
            @PhosphophylliteConfig.Value(range = "(0,1]", advanced = true)
            public static double RadIntensityScalingMultiplier = 0.95f;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double RadIntensityScalingRateExponentMultiplier = 1.2f;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double RadIntensityScalingShiftMultiplier = 1f;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double RadPenaltyShiftMultiplier = 15f;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double RadPenaltyRateMultiplier = 2.5f;
            @PhosphophylliteConfig.Value(range = "(0,1]", advanced = true)
            public static double FuelAbsorptionScalingMultiplier = 0.95f;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double FuelAbsorptionScalingShiftMultiplier = 1f;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double FuelAbsorptionScalingRateExponentMultiplier = 2.2f;
        }
        
        @PhosphophylliteConfig
        public static class Modern {
            @PhosphophylliteConfig.Value(range = "[1,)", advanced = true)
            public static long PerFuelRodCapacity = 4000;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double FuelFertilityMinimumDecay = 0.1f;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double FuelFertilityDecayDenominator = 20;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double FuelFertilityDecayDenominatorInactiveMultiplier = 200;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static int RayCount = 32;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double CasingHeatTransferRFMKT = 0.6;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double FuelToCasingRFKTMultiplier = 1.0;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double CasingToCoolantRFMKT = 0.6;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double CasingToAmbientRFMKT = 0.001;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static long PassiveBatteryPerExternalBlock = 10_000;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double PassiveCoolingTransferEfficiency = 0.2f;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static long CoolantTankAmountPerFuelRod = 10_000;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double RadiationBlocksToLive = 4;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double CaseFEPerUnitVolumeKelvin = 10;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double RodFEPerUnitVolumeKelvin = 10;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double FuelReactivity = 1.05f;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double FissionEventsPerFuelUnit = 0.1f;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double FEPerRadiationUnit = 10f;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double FuelPerRadiationUnit = 0.0007f;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static long IrradiationDistance = 4;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double FuelHardnessDivisor = 1f;
            @PhosphophylliteConfig.Value(range = "[0,1]", advanced = true)
            public static double FuelAbsorptionCoefficient = 0.5f;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double FuelModerationFactor = 1.5f;
            @PhosphophylliteConfig.Value(range = "(0,1]", advanced = true)
            public static double RadIntensityScalingMultiplier = 0.95f;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double RadIntensityScalingRateExponentMultiplier = 1.2f;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double RadIntensityScalingShiftMultiplier = 1f;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double RadPenaltyShiftMultiplier = 15f;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double RadPenaltyRateMultiplier = 2.5f;
            @PhosphophylliteConfig.Value(range = "(0,1]", advanced = true)
            public static double FuelAbsorptionScalingMultiplier = 0.95f;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double FuelAbsorptionScalingShiftMultiplier = 1f;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double FuelAbsorptionScalingRateExponentMultiplier = 2.2f;
        }
        
        @PhosphophylliteConfig
        public static class GUI {
            @PhosphophylliteConfig.Value
            public static long HeatDisplayMax = 2000;
        }
    }
    
    @PhosphophylliteConfig
    public static class Turbine {
        @PhosphophylliteConfig.Value(range = "[5,192]")
        public static int MaxLength = 32;
        @PhosphophylliteConfig.Value(range = "[5,192]")
        public static int MaxWidth = 32;
        @PhosphophylliteConfig.Value(range = "[4,256]")
        public static int MaxHeight = 192;
        
        @PhosphophylliteConfig
        public static class Classic {
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static long FluidPerBlade = 25;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static long FlowRatePerBlock = 500;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static long TankVolumePerBlock = 1000;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double LatentHeatMultiplier = 2.5;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static long RotorMassPerPart = 10;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double MassDragMultiplier = 0.01;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double BladeDragMultiplier = 0.000025;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double CoilDragMultiplier = 1;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static long BatterySizePerCoilBlock = 30_000;
        }
        
        @PhosphophylliteConfig
        public static class Modern {
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static long FlowRatePerBlock = 5000;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static long TankVolumePerBlock = 10000;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double FluidPerBladeLinerKilometre = 20;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double RotorAxialMassPerShaft = 100;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double RotorAxialMassPerBlade = 100;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double FrictionDragMultiplier = 0.0001;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double AerodynamicDragMultiplier = 0.0001;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static double CoilDragMultiplier = 10;
            @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
            public static long BatterySizePerCoilBlock = 300_000;
        }
    }
    
    @PhosphophylliteConfig
    public static class HeatExchanger {
        @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
        public static long ChannelTankVolumePerBlock = 10000;
        @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
        public static double ChannelToChannelHeatConductivityMultiplier = 0.5;
        @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
        public static double ChannelFEPerKelvinUnitVolume = 1000.0f;
        @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
        public static double ChannelFEPerKelvinMetreSquared = 50.0f;
        @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
        public static double ChannelInternalSurfaceArea = 80;
        @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
        public static double AirFEPerKelvinUnitVolume = 10.0f;
        @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
        public static double AirFEPerKelvinMetreSquared = 0.5f;
        @PhosphophylliteConfig.Value(range = "(0,)", advanced = true)
        public static double AmbientFEPerKelvinMetreSquared = 0.2f;

        @PhosphophylliteConfig
        public static class GUI {
            @PhosphophylliteConfig.Value
            public static long HeatDisplayMax = 2000;
        }
    }
    
    
    @PhosphophylliteConfig
    public static class CyaniteReprocessor {
        @PhosphophylliteConfig.Value(range = "(0,)", comment = "Max transfer rate of fluids and energy.")
        public static int TransferRate = 500;
        @PhosphophylliteConfig.Value(range = "(0,)", comment = "Max energy capacity.")
        public static int EnergyTankCapacity = 5000;
        @PhosphophylliteConfig.Value(range = "(0,)", comment = "Max water capacity")
        public static int WaterTankCapacity = 5000;
        @PhosphophylliteConfig.Value(range = "(0,)", comment = "Power usage per tick of work.")
        public static int EnergyConsumptionPerTick = 1;
        @PhosphophylliteConfig.Value(range = "(0,)", comment = "Water usage per tick of work.")
        public static int WaterConsumptionPerTick = 1;
        @PhosphophylliteConfig.Value(range = "(0,)", comment = "Time (in ticks) it takes to complete a job.")
        public static int TotalWorkTime = 200;
    }
}
