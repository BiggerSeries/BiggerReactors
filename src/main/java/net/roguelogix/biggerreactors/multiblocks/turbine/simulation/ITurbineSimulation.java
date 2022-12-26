package net.roguelogix.biggerreactors.multiblocks.turbine.simulation;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;
import net.roguelogix.biggerreactors.multiblocks.turbine.state.VentState;
import net.roguelogix.biggerreactors.registries.TurbineCoilRegistry;
import org.joml.Vector4i;

import java.util.ArrayList;

public interface ITurbineSimulation extends INBTSerializable<CompoundTag> {
    
    void reset();
    
    void resize(int x, int y, int z);
    
    void setRotorConfiguration(ArrayList<Vector4i> rotorConfiguration);
    
    void setCoilData(int x, int y, TurbineCoilRegistry.CoilData coilData);
    
    void updateInternalValues();
    
    void setVentState(VentState state);
    
    VentState ventState();
    
    double RPM();
    
    double bladeEfficiencyLastTick();
    
    long flowLastTick();
    
    long nominalFlowRate();
    
    void setNominalFlowRate(long flowRate);
    
    long flowRateLimit();
    
    ITurbineBattery battery();
    
    ITurbineFluidTank fluidTank();
    
    void tick();
    
    void setActive(boolean active);
    boolean active();
    
    void setCoilEngaged(boolean engaged);
    
    boolean coilEngaged();
    
    long FEGeneratedLastTick();
    
    @Deprecated
    long bladeSurfaceArea();
    
    @Deprecated
    double rotorMass();
    
    String debugString();
}
