package net.roguelogix.biggerreactors.multiblocks.reactor2.simulation;

import net.roguelogix.phosphophyllite.serialization.IPhosphophylliteSerializable;
import net.roguelogix.phosphophyllite.util.HeatBody;
import net.roguelogix.phosphophyllite.util.NonnullDefault;

import javax.annotation.Nullable;
import java.util.List;

/**
 * While it doesnt follow normal math layout, the indexing for the simulation matches the minecraft world
 */
@NonnullDefault
public interface ReactorSimulation extends IPhosphophylliteSerializable {
    
    void tick();
    
    ReactantSystem reactantSystem();
    
    @Nullable
    ControlRod controlRodAt(int x, int z);
    
    double thermalPower();
    
    double fuelTemperature();
    
    double stackTemperature();
    
    double reactivity();
    
    default boolean isAsync() {
        return false;
    }
    
    interface Moderator {
        
        double thermalMass();
        
        double heatConductivity();
    
        /**
         * Fission ignored
         * @return
         */
        CrossSection.Function crossSectionFunction();
    }
    
    @Nullable
    Moderator moderatorAt(int x, int y, int z);
    
    
    double fuelRodArea();
    
    double stackCaseArea();
    
    /**
     * Physical inaccuracy, these act as if they are telescoping, its easier to implement and easier to use, so, that will stay
     */
    interface ControlRod {
        double insertion();
        
        void setInsertion(double insertion);
        
        Properties properties();
        
        interface Properties {
        
        }
    }
    
    interface ReactantSystem {
        long capacity();
        
        long totalStored();
        
        double reactedLastTick();
        
        ReactantTank tankForReactant(IReactant reactant);
        
        List<ReactantTank> allReactantTanks();
        
        interface ReactantTank {
            long stored();
            
            IReactant reactant();
        }
        
        interface IReactant {
            double thermalMass();
            
            CrossSection.Function crossSectionFunction();
        }
    }
    
    record CrossSection(double scattering, double capture, double fission) {
        interface Function {
            CrossSection generate(double temperature, double energy);
        }
    }
    
    interface ThermalOutput {
        void accept(HeatBody fuel, HeatBody stack);
    }
}
