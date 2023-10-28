package net.roguelogix.biggerreactors.multiblocks.reactor.simulation;

import net.roguelogix.biggerreactors.registries.FluidTransitionRegistry;
import net.roguelogix.biggerreactors.registries.ReactorModeratorRegistry;
import net.roguelogix.phosphophyllite.debug.IDebuggable;
import net.roguelogix.phosphophyllite.serialization.IPhosphophylliteSerializable;
import net.roguelogix.phosphophyllite.serialization.PhosphophylliteCompound;
import net.roguelogix.phosphophyllite.util.NonnullDefault;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@NonnullDefault
public interface IReactorSimulation extends IPhosphophylliteSerializable, IDebuggable {
    
    /**
     * tick the reactor simulation
     * call this even if inactive, as there can be some inactive decay
     */
    void tick(boolean active);
    
    @Nullable
    IBattery battery();
    
    @Nullable
    ICoolantTank coolantTank();
    
    IFuelTank fuelTank();
    
    @Nullable
    ControlRod controlRodAt(int x, int z);
    
    void setAllControlRodInsertions(double insertion);
    
    double fertility();
    
    double fuelHeat();
    
    double stackHeat();
    
    double ambientTemperature();
    
    default boolean isAsync() {
        return false;
    }
    
    default boolean readyToTick() {
        return true;
    }
    
    @NotNull
    @Override
    PhosphophylliteCompound save();
    
    interface ControlRod {
        double insertion();
        
        void setInsertion(double insertion);
    }
    
    interface IBattery {
        long extract(long toExtract);
        
        long stored();
        
        long capacity();
        
        long generatedLastTick();
    }
    
    interface ICoolantTank {
        void dumpLiquid();
        
        void dumpVapor();
        
        long insertLiquid(long amount);
        
        long extractLiquid(long amount);
        
        long insertVapor(long amount);
        
        long extractVapor(long amount);
        
        long liquidAmount();
        
        long vaporAmount();
    
        long perSideCapacity();
    
        void setModeratorProperties(ReactorModeratorRegistry.IModeratorProperties moderatorProperties);
    
        void setTransitionProperties(FluidTransitionRegistry.ITransitionProperties transitionProperties);
    
        long transitionedLastTick();
    
        long maxTransitionedLastTick();
    
        long rfTransferredLastTick();
    }
    
    interface IFuelTank {
        long capacity();
        
        long totalStored();
        
        long fuel();
        
        long waste();
        
        long insertFuel(long amount, boolean simulated);
        
        long insertWaste(long amount, boolean simulated);
        
        long extractFuel(long amount, boolean simulated);
        
        long extractWaste(long amount, boolean simulated);
        
        double burnedLastTick();
    }
}
