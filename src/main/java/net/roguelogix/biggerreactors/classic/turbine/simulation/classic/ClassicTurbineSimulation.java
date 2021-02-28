package net.roguelogix.biggerreactors.classic.turbine.simulation.classic;

import net.minecraft.nbt.CompoundNBT;
import net.roguelogix.biggerreactors.Config;
import net.roguelogix.biggerreactors.classic.turbine.TurbineCoilRegistry;
import net.roguelogix.biggerreactors.classic.turbine.simulation.ITurbineBattery;
import net.roguelogix.biggerreactors.classic.turbine.simulation.ITurbineFluidTank;
import net.roguelogix.biggerreactors.classic.turbine.simulation.ITurbineSimulation;
import net.roguelogix.biggerreactors.classic.turbine.state.TurbineActivity;
import net.roguelogix.biggerreactors.classic.turbine.state.VentState;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector4i;

import java.util.ArrayList;

public class ClassicTurbineSimulation implements ITurbineSimulation {
    private int x, y, z;
    
    private boolean active = false;
    
    private int rotorShafts;
    private long rotorMass;
    private long bladeSurfaceArea;
    private long coilSize;
    private double inductionEfficiency;
    private double inductorDragCoefficient;
    private double inductionEnergyExponentBonus;
    private double frictionDrag;
    private double bladeDrag;
    
    private double energyGeneratedLastTick;
    private double rotorEfficiencyLastTick;
    
    private double rotorEnergy = 0;
    
    private long maxFlowRate = 0;
    private long maxMaxFlowRate = 0;
    
    private VentState ventState = VentState.OVERFLOW;
    private boolean coilEngaged = true;
    
    private final Battery battery = new Battery();
    private final FluidTank fluidTank = new FluidTank();
    
    @Override
    public void reset() {
        rotorEnergy = 0;
        maxFlowRate = Config.Turbine.FluidPerBlade * bladeSurfaceArea;
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
        maxMaxFlowRate = (((long) x * z) - 1 /* bearing*/) * Config.Turbine.FlowRatePerBlock;
    }
    
    @Override
    public void setRotorConfiguration(ArrayList<Vector4i> rotorConfiguration) {
        rotorMass = 0;
        bladeSurfaceArea = 0;
        
        rotorMass += rotorConfiguration.size();
        
        for (Vector4i vector4i : rotorConfiguration) {
            bladeSurfaceArea += vector4i.x + vector4i.y + vector4i.z + vector4i.w;
        }
        
        rotorMass += bladeSurfaceArea;
        rotorMass *= Config.Turbine.RotorMassPerPart;
        
        rotorShafts = rotorConfiguration.size();
    }
    
    @Override
    public void setCoilData(int x, int y, TurbineCoilRegistry.CoilData coilData) {
        
        inductionEfficiency += coilData.efficiency;
        inductorDragCoefficient += coilData.extractionRate;
        inductionEnergyExponentBonus += coilData.bonus;
        
        coilSize++;
    }
    
    @Override
    public void updateInternalValues() {
        
        inductorDragCoefficient *= Config.Turbine.CoilDragMultiplier;
        
        frictionDrag = rotorMass * Config.Turbine.MassDragMultiplier;
        bladeDrag = Config.Turbine.BladeDragMultiplier * bladeSurfaceArea;
        
        
        battery.setCapacity((coilSize + 1) * Config.Turbine.BatterySizePerCoilBlock);
        
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
        
        fluidTank.perSideCapacity = (((long) x * y * z) - ((long) rotorShafts + coilSize)) * Config.Turbine.TankVolumePerBlock;
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
        return (bladeSurfaceArea > 0 && rotorMass > 0 ? rotorEnergy / (double) (bladeSurfaceArea * rotorMass) : 0);
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
        energyGeneratedLastTick = 0;
        rotorEfficiencyLastTick = 0;
        
        long steamIn = 0;
        
        if (active) {
            steamIn = Math.min(maxFlowRate, fluidTank.vaporAmount());
            
            if (ventState == VentState.CLOSED) {
                long availableSpace = fluidTank.perSideCapacity - fluidTank.liquidAmount();
                steamIn = Math.min(steamIn, availableSpace);
            }
        }
        
        if (steamIn > 0 || rotorEnergy > 0) {
            
            double rotorSpeed = 0;
            if (bladeSurfaceArea > 0 && rotorMass > 0) {
                rotorSpeed = rotorEnergy / (double) (bladeSurfaceArea * rotorMass);
            }
            
            double aeroDragTorque = rotorSpeed * bladeDrag;
            
            double liftTorque = 0;
            if (steamIn > 0) {
                long steamToProcess = bladeSurfaceArea * Config.Turbine.FluidPerBlade;
                steamToProcess = Math.min(steamToProcess, steamIn);
                liftTorque = steamToProcess * Config.Turbine.SteamCondensationEnergy;
                
                if (steamToProcess < steamIn) {
                    steamToProcess = steamIn - steamToProcess;
                    double neededBlades = steamIn / (double) Config.Turbine.FluidPerBlade;
                    double missingBlades = neededBlades - bladeSurfaceArea;
                    double bladeEfficiency = 1.0 - missingBlades / neededBlades;
                    liftTorque += steamToProcess * bladeEfficiency;
                    
                }
                rotorEfficiencyLastTick = liftTorque / (steamIn * Config.Turbine.SteamCondensationEnergy);
            }
            
            double inductionTorque = coilEngaged ? rotorSpeed * inductorDragCoefficient * coilSize : 0f;
            double energyToGenerate = Math.pow(inductionTorque, inductionEnergyExponentBonus) * inductionEfficiency;
            if (energyToGenerate > 0) {
                // TODO: 8/7/20 make RPM range configurable, its not exactly the easiest thing to do
                double efficiency = 0.25 * Math.cos(rotorSpeed / (45.5 * Math.PI)) + 0.75;
                // yes this is slightly different, this matches what the equation actually looks like better
                // go on, graph it
                if (rotorSpeed < 450) {
                    efficiency = Math.min(0.5, efficiency);
                }
                
                // oh noes, there is a cap now, *no over speeding your fucking turbines*
                if (rotorSpeed > 2245) {
                    efficiency = -rotorSpeed / 4490;
                    efficiency += 1;
                }
                if (efficiency < 0) {
                    efficiency = 0;
                }
                
                energyToGenerate *= efficiency;
                
                energyGeneratedLastTick = energyToGenerate;
                
                battery.generate((long) energyToGenerate);
            }
            
            rotorEnergy += liftTorque;
            rotorEnergy -= inductionTorque;
            rotorEnergy -= aeroDragTorque;
            rotorEnergy -= frictionDrag;
            if (rotorEnergy < 0) {
                rotorEnergy = 0;
            }
            
            fluidTank.flow(steamIn, ventState != VentState.CLOSED);
            
            if (ventState == VentState.ALL) {
                fluidTank.dumpLiquid();
            }
        }
    }
    
    @Override
    public void setActive(boolean active) {
        this.active = active;
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
        return bladeSurfaceArea;
    }
    
    @Override
    public long rotorMass() {
        return rotorMass;
    }
    
    @Override
    public boolean active() {
        return active;
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
