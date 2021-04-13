package net.roguelogix.biggerreactors.multiblocks.turbine.simulation.modern;

import net.minecraft.nbt.CompoundNBT;
import net.roguelogix.biggerreactors.Config;
import net.roguelogix.biggerreactors.registries.TurbineCoilRegistry;
import net.roguelogix.biggerreactors.multiblocks.turbine.simulation.ITurbineBattery;
import net.roguelogix.biggerreactors.multiblocks.turbine.simulation.ITurbineFluidTank;
import net.roguelogix.biggerreactors.multiblocks.turbine.simulation.ITurbineSimulation;
import net.roguelogix.biggerreactors.multiblocks.turbine.state.VentState;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector4i;

import java.util.ArrayList;

public class ModernTurbineSimulation implements ITurbineSimulation {
    
    private int x, y, z;
    
    private boolean active = false;
    
    private long coilSize;
    private double inductionEfficiency;
    private double inductorDragCoefficient;
    private double inductionEnergyExponentBonus;
    
    private double rotorCapacityPerRPM;
    
    private long maxFlowRate = -1;
    private long maxMaxFlowRate = 0;
    
    private int rotorShafts;
    private double rotorAxialMass;
    private double rotorMass;
    private double linearBladeMetersPerRevolution = 0;
    
    private VentState ventState = VentState.OVERFLOW;
    private boolean coilEngaged = true;
    
    private double rotorEnergy = 0;
    private final FluidTank fluidTank = new FluidTank();
    private final Battery battery = new Battery();
    
    private double energyGeneratedLastTick;
    private double rotorEfficiencyLastTick;
    
    @Override
    public void reset() {
        rotorEnergy = 0;
    }
    
    @Override
    public void resize(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        coilSize = 0;
        inductionEfficiency = 0;
        inductorDragCoefficient = 0;
        inductionEnergyExponentBonus = 0;
        maxMaxFlowRate = (((long) x * z) - 1 /* bearing*/) * Config.Turbine.Modern.FlowRatePerBlock;
    }
    
    @Override
    public void setRotorConfiguration(ArrayList<Vector4i> rotorConfiguration) {
        rotorMass = 0;
        linearBladeMetersPerRevolution = 0;
        for (Vector4i vector4i : rotorConfiguration) {
            linearBladeMetersPerRevolution += rangeFromZeroSum(vector4i.x);
            linearBladeMetersPerRevolution += rangeFromZeroSum(vector4i.y);
            linearBladeMetersPerRevolution += rangeFromZeroSum(vector4i.z);
            linearBladeMetersPerRevolution += rangeFromZeroSum(vector4i.w);
            rotorMass += vector4i.x + vector4i.y + vector4i.z + vector4i.w;
        }
        
        rotorCapacityPerRPM = linearBladeMetersPerRevolution * Config.Turbine.Modern.FluidPerBladeLinerKilometre;
        rotorCapacityPerRPM /= 1000; // metre / kilometre
        
        rotorShafts = rotorConfiguration.size();
        
        rotorAxialMass = rotorShafts * Config.Turbine.Modern.RotorAxialMassPerShaft;
        rotorAxialMass += linearBladeMetersPerRevolution * Config.Turbine.Modern.RotorAxialMassPerBlade;
        
        rotorCapacityPerRPM *= 2 * Math.PI;
        
        rotorMass *= Config.Turbine.Modern.RotorAxialMassPerBlade;
        rotorMass += (double) rotorShafts * Config.Turbine.Modern.RotorAxialMassPerShaft;
        
        if (maxFlowRate == -1) {
            setNominalFlowRate((long) (rotorCapacityPerRPM * 1800));
        }
    }
    
    private long rangeFromZeroSum(long upper) {
        return ((1 + upper) * upper) / 2;
    }
    
    @Override
    public void setCoilData(int x, int y, TurbineCoilRegistry.CoilData coilData) {
        inductionEfficiency += coilData.efficiency;
        inductionEnergyExponentBonus += coilData.bonus;
        
        double distance = Math.max(Math.abs(x), Math.abs(y));
        
        inductorDragCoefficient += coilData.extractionRate * layerMultiplier(distance);
        
        coilSize++;
    }
    
    private double layerMultiplier(double distance) {
        if (distance < 1) {
            return 1;
        }
        return 2 / (distance + 1);
    }
    
    @Override
    public void updateInternalValues() {
        inductorDragCoefficient *= Config.Turbine.Modern.CoilDragMultiplier;
        
        battery.setCapacity((coilSize + 1) * Config.Turbine.Modern.BatterySizePerCoilBlock);
        
        if (coilSize <= 0) {
            inductionEfficiency = 0;
            inductorDragCoefficient = 0;
            inductionEnergyExponentBonus = 0;
        } else {
            // TODO: 8/8/20 config that 1b
            inductionEfficiency = (inductionEfficiency * 1d) / coilSize;
            inductionEnergyExponentBonus = Math.max(1f, (inductionEnergyExponentBonus / coilSize));
            inductorDragCoefficient = (inductorDragCoefficient / coilSize);
        }
        
        fluidTank.perSideCapacity = (((long) x * y * z) - ((long) rotorShafts + coilSize)) * Config.Turbine.Modern.TankVolumePerBlock;
    }
    
    @Override
    public void setVentState(VentState state) {
        ventState = state;
    }
    
    @Override
    public VentState ventState() {
        return ventState;
    }
    
    @Override
    public double RPM() {
        return rotorEnergy / rotorAxialMass;
    }
    
    @Override
    public double bladeEfficiencyLastTick() {
        return rotorEfficiencyLastTick;
    }
    
    @Override
    public long flowLastTick() {
        return fluidTank.transitionedLastTick();
    }
    
    @Override
    public long nominalFlowRate() {
        return maxFlowRate;
    }
    
    @Override
    public void setNominalFlowRate(long flowRate) {
        maxFlowRate = flowRate;
        maxFlowRate = Math.min(maxMaxFlowRate, Math.max(0, flowRate));
    }
    
    @Override
    public long flowRateLimit() {
        return maxMaxFlowRate;
    }
    
    @Override
    public ITurbineBattery battery() {
        return battery;
    }
    
    @Override
    public ITurbineFluidTank fluidTank() {
        return fluidTank;
    }
    
    @Override
    public void tick() {
        double rpm = RPM();
        
        if (active) {
            double flowRate = fluidTank.flow(maxFlowRate, ventState != VentState.CLOSED);
            double effectiveFlowRate = flowRate;
            
            // need something to get it started, also, divide by zero errors
            double rotorCapacity = rotorCapacityPerRPM * Math.max(100, rpm);
            
            if (flowRate > rotorCapacity) {
                double excessFLow = flowRate - rotorCapacity;
                double excessEfficiency = rotorCapacity / flowRate;
                effectiveFlowRate = rotorCapacity + excessFLow * excessEfficiency;
            }
            
            if (flowRate != 0) {
                rotorEfficiencyLastTick = effectiveFlowRate / flowRate;
            } else {
                rotorEfficiencyLastTick = 0;
            }
            
            if (effectiveFlowRate > 0) {
                rotorEnergy += fluidTank.activeTransition().latentHeat * effectiveFlowRate * fluidTank.activeTransition().turbineMultiplier;
            }
            
        } else {
            fluidTank.flow(0, ventState != VentState.CLOSED);
            rotorEfficiencyLastTick = 0;
        }
        
        if (ventState == VentState.ALL) {
            fluidTank.dumpLiquid();
        }
        
        if (coilEngaged) {
            double inductionTorque = rpm * inductorDragCoefficient * coilSize;
            double energyToGenerate = Math.pow(inductionTorque, inductionEnergyExponentBonus) * inductionEfficiency;
            
            // TODO: 8/7/20 make RPM range configurable, its not exactly the easiest thing to do
            double efficiency = 0.25 * Math.cos(rpm / (45.5 * Math.PI)) + 0.75;
            // yes this is slightly different, this matches what the equation actually looks like better
            // go on, graph it
            if (rpm < 450) {
                efficiency = Math.min(0.5, efficiency);
            }
            
            // oh noes, there is a cap now, *no over speeding your fucking turbines*
            if (rpm > 2245) {
                efficiency = -rpm / 4490;
                efficiency += 1;
            }
            if (efficiency < 0) {
                efficiency = 0;
            }
            
            energyToGenerate *= efficiency;
            
            energyGeneratedLastTick = energyToGenerate;
            
            if (energyToGenerate > 1) {
                battery.generate((long) energyToGenerate);
            }
            
            rotorEnergy -= inductionTorque;
        } else {
            energyGeneratedLastTick = 0;
        }
        
        rotorEnergy -= rotorMass * Math.pow(rpm * Config.Turbine.Modern.FrictionDragMultiplier, 2); // yes, rpm squared, thats how drag works, bitch ain't it?
        rotorEnergy -= linearBladeMetersPerRevolution * Math.pow(rpm * Config.Turbine.Modern.AerodynamicDragMultiplier, 2);
        if (rotorEnergy < 0) {
            rotorEnergy = 0;
        }
        
    }
    
    @Override
    public void setActive(boolean active) {
        this.active = active;
    }
    
    @Override
    public boolean active() {
        return active;
    }
    
    @Override
    public void setCoilEngaged(boolean engaged) {
        coilEngaged = engaged;
    }
    
    @Override
    public boolean coilEngaged() {
        return coilEngaged;
    }
    
    @Override
    public long FEGeneratedLastTick() {
        return (long) energyGeneratedLastTick;
    }
    
    @Override
    public long bladeSurfaceArea() {
        return 0;
    }
    
    @Override
    public double rotorMass() {
        return rotorAxialMass;
    }
    
    @Override
    public String debugString() {
        return "";
    }
    
    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.put("fluidTank", fluidTank.serializeNBT());
        nbt.put("battery", battery.serializeNBT());
        nbt.putInt("ventState", ventState.toInt());
        nbt.putDouble("rotorEnergy", rotorEnergy);
        nbt.putLong("maxFlowRate", maxFlowRate);
        nbt.putBoolean("coilEngaged", coilEngaged);
        nbt.putBoolean("active", active);
        return nbt;
    }
    
    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        fluidTank.deserializeNBT(nbt.getCompound("fluidTank"));
        battery.deserializeNBT(nbt.getCompound("battery"));
        ventState = VentState.fromInt(nbt.getInt("ventState"));
        rotorEnergy = nbt.getDouble("rotorEnergy");
        maxFlowRate = nbt.getLong("maxFlowRate");
        coilEngaged = nbt.getBoolean("coilEngaged");
        active = nbt.getBoolean("active");
    }
}
