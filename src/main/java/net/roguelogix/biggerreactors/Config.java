package net.roguelogix.biggerreactors;

import net.roguelogix.phosphophyllite.config.ConfigFormat;
import net.roguelogix.phosphophyllite.config.ConfigType;
import net.roguelogix.phosphophyllite.config.ConfigValue;
import net.roguelogix.phosphophyllite.registry.RegisterConfig;

@SuppressWarnings("unused")
public class Config {
    
    @RegisterConfig(format = ConfigFormat.TOML, type = {ConfigType.CLIENT, ConfigType.SERVER}, rootLevelType = ConfigType.SERVER)
    public static final Config CONFIG = new Config();
    
    public enum Mode {
        MODERN,
        EXPERIMENTAL,
    }
    
    @ConfigValue(advanced = ConfigValue.BoolOption.True)
    public final Mode mode;
    
    @ConfigValue(configType = ConfigType.CLIENT, comment = "Always show moderator and coil tooltips regardless of advanced tooltip setting\nCan always be enabled with advanced tooltips", reloadable = ConfigValue.BoolOption.True)
    public final boolean AlwaysShowTooltips;
    
    {
        mode = Mode.MODERN;
        AlwaysShowTooltips = true;
    }
    
    public static final class WorldGen {
        @ConfigValue(range = "[1,)")
        public final int UraniumOreMaxClustersPerChunk;
        @ConfigValue(range = "[1,)")
        public final int UraniumMaxOrePerCluster;
        @ConfigValue(range = "[5,)")
        public final int UraniumOreMaxSpawnY;
        @ConfigValue
        public final boolean EnableUraniumGeneration;
        
        {
            UraniumOreMaxClustersPerChunk = 5;
            UraniumMaxOrePerCluster = 6;
            UraniumOreMaxSpawnY = 50;
            EnableUraniumGeneration = true;
        }
    }
    
    @ConfigValue
    public final WorldGen WorldGen = new WorldGen();
    
    public static final class Reactor {
        @ConfigValue(range = "[3,)")
        public final int MaxLength;
        @ConfigValue(range = "[3,)")
        public final int MaxWidth;
        @ConfigValue(range = "[3,)")
        public final int MaxHeight;
        
        {
            MaxLength = 128;
            MaxWidth = 128;
            MaxHeight = 192;
        }
        
        @ConfigValue(range = "(0,)")
        public final double FuelUsageMultiplier;
        @ConfigValue(range = "(0,)")
        public final double OutputMultiplier;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double PassiveOutputMultiplier;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double ActiveOutputMultiplier;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final long FuelMBPerIngot;
        
        {
            FuelUsageMultiplier = 1;
            OutputMultiplier = 1.0f;
            PassiveOutputMultiplier = 0.5f;
            ActiveOutputMultiplier = 1.0f;
            FuelMBPerIngot = 1000;
        }
        
        @ConfigValue(advanced = ConfigValue.BoolOption.True)
        public final boolean useFullPassSimulation;
        @ConfigValue(advanced = ConfigValue.BoolOption.True)
        public final boolean allowOffThreadSimulation;
        @ConfigValue(advanced = ConfigValue.BoolOption.True)
        public final boolean allowMultiThreadSimulation;
        @ConfigValue(advanced = ConfigValue.BoolOption.True)
        public final boolean allowAcceleratedSimulation;
        
        {
            useFullPassSimulation = false;
            allowOffThreadSimulation = true;
            allowMultiThreadSimulation = true;
            allowAcceleratedSimulation = true;
        }
        
        @ConfigValue(range = "[1,)", advanced = ConfigValue.BoolOption.True)
        public final long PerFuelRodCapacity;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double FuelFertilityMinimumDecay;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double FuelFertilityDecayDenominator;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double FuelFertilityDecayDenominatorInactiveMultiplier;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double CasingHeatTransferRFMKT;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double FuelToStackRFKTMultiplier;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double StackToCoolantRFMKT;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double StackToAmbientRFMKT;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final long PassiveBatteryPerExternalBlock;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double PassiveCoolingTransferEfficiency;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final long CoolantTankAmountPerFuelRod;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double CaseFEPerUnitVolumeKelvin;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double RodFEPerUnitVolumeKelvin;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double FuelReactivity;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double FissionEventsPerFuelUnit;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double FEPerRadiationUnit;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double FuelPerRadiationUnit;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final long IrradiationDistance;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double FuelHardnessDivisor;
        @ConfigValue(range = "[0,1]", advanced = ConfigValue.BoolOption.True)
        public final double FuelAbsorptionCoefficient;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double FuelModerationFactor;
        @ConfigValue(range = "(0,1]", advanced = ConfigValue.BoolOption.True)
        public final double RadIntensityScalingMultiplier;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double RadIntensityScalingRateExponentMultiplier;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double RadIntensityScalingShiftMultiplier;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double RadPenaltyShiftMultiplier;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double RadPenaltyRateMultiplier;
        @ConfigValue(range = "(0,1]", advanced = ConfigValue.BoolOption.True)
        public final double FuelAbsorptionScalingMultiplier;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double FuelAbsorptionScalingShiftMultiplier;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double FuelAbsorptionScalingRateExponentMultiplier;
    
        @ConfigValue(range = "(,)", advanced = ConfigValue.BoolOption.True)
        public final double fuelRadScalingMultiplier;
        
        {
            PerFuelRodCapacity = 4000;
            FuelFertilityMinimumDecay = 0.1f;
            FuelFertilityDecayDenominator = 20;
            FuelFertilityDecayDenominatorInactiveMultiplier = 200;
            CasingHeatTransferRFMKT = 0.6;
            FuelToStackRFKTMultiplier = 1.0;
            StackToCoolantRFMKT = 0.6;
            StackToAmbientRFMKT = 0.001;
            PassiveBatteryPerExternalBlock = 100_000;
            PassiveCoolingTransferEfficiency = 0.2f;
            CoolantTankAmountPerFuelRod = 10_000;
            CaseFEPerUnitVolumeKelvin = 10;
            RodFEPerUnitVolumeKelvin = 10;
            FuelReactivity = 1.05f;
            FissionEventsPerFuelUnit = 0.1f;
            FEPerRadiationUnit = 10f;
            FuelPerRadiationUnit = 0.0007f;
            IrradiationDistance = 4;
            FuelHardnessDivisor = 1f;
            FuelAbsorptionCoefficient = 0.5f;
            FuelModerationFactor = 1.5f;
            RadIntensityScalingMultiplier = 0.95f;
            RadIntensityScalingRateExponentMultiplier = 1.2f;
            RadIntensityScalingShiftMultiplier = 1f;
            RadPenaltyShiftMultiplier = 15f;
            RadPenaltyRateMultiplier = 2.5f;
            FuelAbsorptionScalingMultiplier = 0.95f;
            FuelAbsorptionScalingShiftMultiplier = 1f;
            FuelAbsorptionScalingRateExponentMultiplier = 2.2f;
            fuelRadScalingMultiplier = 1.0;
        }
        
        @ConfigValue(range = "[16,)", advanced = ConfigValue.BoolOption.True, comment = "Powers of two recommended")
        public final int SimulationRays;
        
        {
            SimulationRays = 64;
        }
        
        public static final class ModeSpecific {
            @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
            public final int ControlRodBatchSize;
            
            {
                ControlRodBatchSize = 32;
            }
        }
        
        @ConfigValue
        public final ModeSpecific ModeSpecific = new ModeSpecific();
        
        public static final class GUI {
            @ConfigValue
            public final long HeatDisplayMax;
            
            {
                HeatDisplayMax = 2000;
            }
        }
        
        @ConfigValue(configType = ConfigType.CLIENT)
        public final GUI GUI = new GUI();
    }
    
    @ConfigValue
    public final Reactor Reactor = new Reactor();
    
    public static final class Turbine {
        @ConfigValue(range = "[5,)")
        public final int MaxLength;
        @ConfigValue(range = "[5,)")
        public final int MaxWidth;
        @ConfigValue(range = "[4,)")
        public final int MaxHeight;
        
        {
            MaxLength = 32;
            MaxWidth = 32;
            MaxHeight = 192;
        }
        
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final long FlowRatePerBlock;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final long TankVolumePerBlock;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double FluidPerBladeLinerKilometre;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double RotorAxialMassPerShaft;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double RotorAxialMassPerBlade;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double FrictionDragMultiplier;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double AerodynamicDragMultiplier;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double CoilDragMultiplier;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final long BatterySizePerCoilBlock;
        
        {
            FlowRatePerBlock = 5000;
            TankVolumePerBlock = 10000;
            FluidPerBladeLinerKilometre = 20;
            RotorAxialMassPerShaft = 100;
            RotorAxialMassPerBlade = 100;
            FrictionDragMultiplier = 0.0001;
            AerodynamicDragMultiplier = 0.0001;
            CoilDragMultiplier = 10;
            BatterySizePerCoilBlock = 300_000;
        }
        
        public static final class GUI {
            @ConfigValue
            public final long DeltaMB;
            @ConfigValue
            public final long DeltaMBShift;
            @ConfigValue
            public final long DeltaMBCtrl;
            @ConfigValue
            public final long DeltaMBHCtrlShift;
            
            {
                DeltaMB = 10;
                DeltaMBShift = 100;
                DeltaMBCtrl = 500;
                DeltaMBHCtrlShift = 1000;
            }
        }
        
        @ConfigValue(configType = ConfigType.CLIENT)
        public final GUI GUI = new GUI();
    }
    
    @ConfigValue
    public final Turbine Turbine = new Turbine();
    
    public static final class HeatExchanger {
        @ConfigValue(range = "[3,)")
        public final int MaxLength;
        @ConfigValue(range = "[3,)")
        public final int MaxWidth;
        @ConfigValue(range = "[4,)")
        public final int MaxHeight;
        
        {
            MaxLength = 64;
            MaxWidth = 64;
            MaxHeight = 96;
        }
        
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final long ChannelTankVolumePerBlock;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double ChannelToChannelHeatConductivityMultiplier;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double ChannelFEPerKelvinUnitVolume;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double ChannelFEPerKelvinMetreSquared;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double ChannelInternalSurfaceArea;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double AirFEPerKelvinUnitVolume;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double AirFEPerKelvinMetreSquared;
        @ConfigValue(range = "(0,)", advanced = ConfigValue.BoolOption.True)
        public final double AmbientFEPerKelvinMetreSquared;
        
        {
            ChannelTankVolumePerBlock = 10000;
            ChannelToChannelHeatConductivityMultiplier = 0.5;
            ChannelFEPerKelvinUnitVolume = 1000.0f;
            ChannelFEPerKelvinMetreSquared = 50.0f;
            ChannelInternalSurfaceArea = 80;
            AirFEPerKelvinUnitVolume = 10.0f;
            AirFEPerKelvinMetreSquared = 0.5f;
            AmbientFEPerKelvinMetreSquared = 0.2f;
        }
        
        public static final class GUI {
            @ConfigValue
            public final long HeatDisplayMax;
            
            {
                HeatDisplayMax = 2000;
            }
        }
        
        @ConfigValue(configType = ConfigType.CLIENT)
        public final GUI gui = new GUI();
    }
    
    @ConfigValue
    public final HeatExchanger HeatExchanger = new HeatExchanger();
    
    public static final class CyaniteReprocessor {
        @ConfigValue(range = "(0,)", comment = "Max transfer rate of fluids and energy.")
        public final int TransferRate;
        @ConfigValue(range = "(0,)", comment = "Max energy capacity.")
        public final int EnergyTankCapacity;
        @ConfigValue(range = "(0,)", comment = "Max water capacity")
        public final int WaterTankCapacity;
        @ConfigValue(range = "(0,)", comment = "Power usage per tick of work.")
        public final int EnergyConsumptionPerTick;
        @ConfigValue(range = "(0,)", comment = "Water usage per tick of work.")
        public final int WaterConsumptionPerTick;
        @ConfigValue(range = "(0,)", comment = "Time (in ticks) it takes to complete a job.")
        public final int TotalWorkTime;
        
        {
            TransferRate = 500;
            EnergyTankCapacity = 5000;
            WaterTankCapacity = 5000;
            EnergyConsumptionPerTick = 1;
            WaterConsumptionPerTick = 1;
            TotalWorkTime = 200;
        }
    }
    
    @ConfigValue
    public final CyaniteReprocessor CyaniteReprocessor = new CyaniteReprocessor();
}
