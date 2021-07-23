package net.roguelogix.biggerreactors.multiblocks.reactor.simulation;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;
import net.roguelogix.biggerreactors.registries.ReactorModeratorRegistry;

public interface IReactorSimulation extends INBTSerializable<CompoundTag> {
    
    void resize(int x, int y, int z);
    
    void setModeratorProperties(int x, int y, int z, ReactorModeratorRegistry.IModeratorProperties properties);
    
    void setControlRod(int x, int z);
    
    void setManifold(int x, int y, int z);
    
    void setControlRodInsertion(int x, int z, double insertion);
    
    void setPassivelyCooled(boolean passivelyCooled);
    
    boolean isPassive();
    
    void updateInternalValues();
    
    void setActive(boolean active);
    
    /**
     * tick the reactor simulation
     * call this even if inactive, as there can be some inactive decay
     */
    void tick();
    
    IReactorBattery battery();
    
    IReactorCoolantTank coolantTank();
    
    /**
     * @return simulation's fuel tank
     */
    IReactorFuelTank fuelTank();
    
    /**
     * FE produced if passive
     * MB * latent heat if active
     *
     * @return FE or FE equivlent produced last tick
     */
    long FEProducedLastTick();
    
    /**
     * 0 if passive
     * @return MB of coolant vaporized last ticks
     */
    long MBProducedLastTick();
    
    /**
     * limit of the coolant system, assuming it had infinite coolant to work with
     * @return MB of coolant that could have been vaporized last tick
     */
    long maxMBProductionLastTick();
    
    /**
     * FE in passive mode, MB in active mode
     * always matches at least one of of the two above functions
     * @return last tick's output rate, depending on mode
     */
    long outputLastTick();
    
    double fuelConsumptionLastTick();
    
    double fertility();
    
    double fuelHeat();
    
    double caseHeat();
    
    double ambientTemperature();
}
