package net.roguelogix.biggerreactors;

import net.roguelogix.phosphophyllite.config.ConfigFormat;
import net.roguelogix.phosphophyllite.config.ConfigValue;
import net.roguelogix.phosphophyllite.registry.RegisterConfig;

@SuppressWarnings("unused")
public class Config {
    
    @RegisterConfig(format = ConfigFormat.TOML)
    public static final Config CONFIG = new Config();
    
    @ConfigValue(hidden = true, enableAdvanced = true)
    private final boolean EnableAdvancedConfig = false;
    
    public enum Mode {
        // Modern BiR, familiar yet different
        MODERN,
        // Mechanics of modern, usually, may be unstable, may be more resource intensive, may not even run
        // if not applicable to multiblock, defaults to modern
        EXPERIMENTAL,
        // Experimental, but allowed to use multiple threads
        // don't blame me if you end up pissing off your server host
        MULTITHREADED,
    }
    
    @ConfigValue(advanced = true)
    public final Mode mode = Mode.MODERN;
    
    public static final class WorldGen {
        @ConfigValue(range = "[1,)")
        public final int YelloriteOreMaxClustersPerChunk = 5;
        @ConfigValue(range = "[1,)")
        public final int YelloriteMaxOrePerCluster = 10;
        @ConfigValue(range = "[5,)")
        public final int YelloriteOreMaxSpawnY = 50;
        @ConfigValue
        public final boolean EnableYelloriteGeneration = true;
    }
    
    @ConfigValue
    public final WorldGen WorldGen = new WorldGen();
    
    public static final class Reactor {
        //TODO: remove max, its only there because of the render system
        //      multiblock system can take *much* larger structures
        @ConfigValue(range = "[3,192]")
        public final int MaxLength = 128;
        @ConfigValue(range = "[3,192]")
        public final int MaxWidth = 128;
        @ConfigValue(range = "[3,256]")
        public final int MaxHeight = 192;
        
        @ConfigValue(range = "(0,)")
        public final double FuelUsageMultiplier = 1;
        @ConfigValue(range = "(0,)")
        public final double OutputMultiplier = 1.0f;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double PassiveOutputMultiplier = 0.5f;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double ActiveOutputMultiplier = 1.0f;
        @ConfigValue(range = "(0,)", advanced = true)
        public final long FuelMBPerIngot = 1000;
        
        @ConfigValue(range = "[1,)", advanced = true)
        public final long PerFuelRodCapacity = 4000;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double FuelFertilityMinimumDecay = 0.1f;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double FuelFertilityDecayDenominator = 20;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double FuelFertilityDecayDenominatorInactiveMultiplier = 200;
        @ConfigValue(range = "(0,)", advanced = true)
        public final int RayCount = 32;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double CasingHeatTransferRFMKT = 0.6;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double FuelToStackRFKTMultiplier = 1.0;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double StackToCoolantRFMKT = 0.6;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double StackToAmbientRFMKT = 0.001;
        @ConfigValue(range = "(0,)", advanced = true)
        public final long PassiveBatteryPerExternalBlock = 100_000;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double PassiveCoolingTransferEfficiency = 0.2f;
        @ConfigValue(range = "(0,)", advanced = true)
        public final long CoolantTankAmountPerFuelRod = 10_000;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double RadiationBlocksToLive = 4;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double CaseFEPerUnitVolumeKelvin = 10;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double RodFEPerUnitVolumeKelvin = 10;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double FuelReactivity = 1.05f;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double FissionEventsPerFuelUnit = 0.1f;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double FEPerRadiationUnit = 10f;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double FuelPerRadiationUnit = 0.0007f;
        @ConfigValue(range = "(0,)", advanced = true)
        public final long IrradiationDistance = 4;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double FuelHardnessDivisor = 1f;
        @ConfigValue(range = "[0,1]", advanced = true)
        public final double FuelAbsorptionCoefficient = 0.5f;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double FuelModerationFactor = 1.5f;
        @ConfigValue(range = "(0,1]", advanced = true)
        public final double RadIntensityScalingMultiplier = 0.95f;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double RadIntensityScalingRateExponentMultiplier = 1.2f;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double RadIntensityScalingShiftMultiplier = 1f;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double RadPenaltyShiftMultiplier = 15f;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double RadPenaltyRateMultiplier = 2.5f;
        @ConfigValue(range = "(0,1]", advanced = true)
        public final double FuelAbsorptionScalingMultiplier = 0.95f;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double FuelAbsorptionScalingShiftMultiplier = 1f;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double FuelAbsorptionScalingRateExponentMultiplier = 2.2f;
        
        public static final class Experimental {
            @ConfigValue(range = "(0,)", advanced = true)
            public final int RodBatchSize = 4096;
        }
        
        @ConfigValue
        public final Experimental Experimental = new Experimental();
        
        public static final class GUI {
            @ConfigValue
            public final long HeatDisplayMax = 2000;
        }
        
        @ConfigValue
        public final GUI GUI = new GUI();
    }
    
    @ConfigValue
    public final Reactor Reactor = new Reactor();
    
    public static final class Turbine {
        @ConfigValue(range = "[5,192]")
        public final int MaxLength = 32;
        @ConfigValue(range = "[5,192]")
        public final int MaxWidth = 32;
        @ConfigValue(range = "[4,256]")
        public final int MaxHeight = 192;
        
        public static final class Classic {
            @ConfigValue(range = "(0,)", advanced = true)
            public final long FluidPerBlade = 25;
            @ConfigValue(range = "(0,)", advanced = true)
            public final long FlowRatePerBlock = 500;
            @ConfigValue(range = "(0,)", advanced = true)
            public final long TankVolumePerBlock = 1000;
            @ConfigValue(range = "(0,)", advanced = true)
            public final double LatentHeatMultiplier = 2.5;
            @ConfigValue(range = "(0,)", advanced = true)
            public final long RotorMassPerPart = 10;
            @ConfigValue(range = "(0,)", advanced = true)
            public final double MassDragMultiplier = 0.01;
            @ConfigValue(range = "(0,)", advanced = true)
            public final double BladeDragMultiplier = 0.000025;
            @ConfigValue(range = "(0,)", advanced = true)
            public final double CoilDragMultiplier = 1;
            @ConfigValue(range = "(0,)", advanced = true)
            public final long BatterySizePerCoilBlock = 30_000;
        }
        
        @ConfigValue(range = "(0,)", advanced = true)
        public final long FlowRatePerBlock = 5000;
        @ConfigValue(range = "(0,)", advanced = true)
        public final long TankVolumePerBlock = 10000;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double FluidPerBladeLinerKilometre = 20;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double RotorAxialMassPerShaft = 100;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double RotorAxialMassPerBlade = 100;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double FrictionDragMultiplier = 0.0001;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double AerodynamicDragMultiplier = 0.0001;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double CoilDragMultiplier = 10;
        @ConfigValue(range = "(0,)", advanced = true)
        public final long BatterySizePerCoilBlock = 300_000;
    }
    
    @ConfigValue
    public final Turbine Turbine = new Turbine();
    
    public static final class HeatExchanger {
        @ConfigValue(range = "(0,)", advanced = true)
        public final long ChannelTankVolumePerBlock = 10000;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double ChannelToChannelHeatConductivityMultiplier = 0.5;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double ChannelFEPerKelvinUnitVolume = 1000.0f;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double ChannelFEPerKelvinMetreSquared = 50.0f;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double ChannelInternalSurfaceArea = 80;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double AirFEPerKelvinUnitVolume = 10.0f;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double AirFEPerKelvinMetreSquared = 0.5f;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double AmbientFEPerKelvinMetreSquared = 0.2f;
        
        public static final class GUI {
            @ConfigValue
            public final long HeatDisplayMax = 2000;
        }
    
        @ConfigValue
        public final Reactor.GUI gui = new Reactor.GUI();
    }
    
    @ConfigValue
    public final HeatExchanger HeatExchanger = new HeatExchanger();
    
    public static final class CyaniteReprocessor {
        @ConfigValue(range = "(0,)", comment = "Max transfer rate of fluids and energy.")
        public final int TransferRate = 500;
        @ConfigValue(range = "(0,)", comment = "Max energy capacity.")
        public final int EnergyTankCapacity = 5000;
        @ConfigValue(range = "(0,)", comment = "Max water capacity")
        public final int WaterTankCapacity = 5000;
        @ConfigValue(range = "(0,)", comment = "Power usage per tick of work.")
        public final int EnergyConsumptionPerTick = 1;
        @ConfigValue(range = "(0,)", comment = "Water usage per tick of work.")
        public final int WaterConsumptionPerTick = 1;
        @ConfigValue(range = "(0,)", comment = "Time (in ticks) it takes to complete a job.")
        public final int TotalWorkTime = 200;
    }
    
    @ConfigValue
    public final CyaniteReprocessor CyaniteReprocessor = new CyaniteReprocessor();
}
