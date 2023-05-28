package net.roguelogix.biggerreactors.multiblocks.reactor2.simulation.java;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.roguelogix.biggerreactors.multiblocks.reactor2.simulation.SimulationDescription;
import net.roguelogix.phosphophyllite.util.NonnullDefault;

import java.util.Collections;
import java.util.List;

@NonnullDefault
public class ReferenceSimulation {
    
    private static class Moderator {
        double[][] inputMultipliers = new double[8][];
    }
    
    private static class ControlRod {
        final double speed = 0.1;
        final double resolution = 0.01;
        double requested = 1;
        double actual = 1;
        
        double fertility = 1;
        double temperature = 0;
        
        void request(double newPosition) {
            double requestedChange = newPosition - requested;
            requestedChange /= resolution;
            
            requestedChange *= resolution;
            requested += requestedChange;
        }
        
        void tick() {
            double toMove = requested - actual;
            toMove = Math.max(Math.min(toMove, speed), -speed);
            actual = toMove;
        }
    }

//    private static class FuelRod {
//        ControlRod controlRod;
//    }
    
    public record FuelAmount(long whole, long fractional) {
        public static final int FRACTIONAL_BITS = 30;
        public static final int FP_FRACTIONAL_BITS = 8;
        public static final long FRACTIONAL_ONE = 1 << FRACTIONAL_BITS;
        public static final double FRACTIONAL_TO_FP = 1.0 / FRACTIONAL_ONE;
        
        public FuelAmount(double amount) {
            this((long) Math.floor(amount), (((long) Math.floor((amount - Math.floor(amount)) * FRACTIONAL_ONE)) >> (FRACTIONAL_BITS - FP_FRACTIONAL_BITS)) << (FRACTIONAL_BITS - FP_FRACTIONAL_BITS));
        }
        
        double fpAmount() {
            return whole + fractional * FRACTIONAL_TO_FP;
        }
        
        FuelAmount multiply(FuelAmount other) {
            long newWhole = whole * other.whole;
            long newFractional = (fractional * other.fractional) >> FRACTIONAL_BITS;
            newFractional += whole * other.fractional;
            newFractional += other.whole * fractional;
            return new FuelAmount(newWhole, newFractional);
        }
        
        static FuelAmount multiply(FuelAmount a, FuelAmount b) {
            return a.multiply(b);
        }
    }
    
    private static class Fuel {
        Moderator fuelModerationProperties;
        boolean burnable;
        double rfPerBurn;
        List<Pair<Fuel, FuelAmount>> burnResults = new ObjectArrayList<>();
        List<Pair<Fuel, FuelAmount>> decayResult = new ObjectArrayList<>();
        double[][] fertilizationProperties = new double[8][4];
        double[][] reactionProperties = new double[48][4];
    }
    
    // liquid fueled, which is *bleh* but whatever, unified fuel pool is fine
    private static class FuelTank {
        
        private record BurnResult(double heatGenerated, double neutronIntensity, double neutronHardness, double neutronSpread, List<Pair<Fuel, FuelAmount>> byproducts, FuelAmount burnt) {
        }
        
        long totalCapacity;
        long totalAmount;
        
        private class Container {
            Fuel fuel;
            FuelAmount fuelAmount = new FuelAmount(0, 0);
            
            BurnResult burn(ControlRod controlRod) {
                // TODO: range safeties, holy shit does this need them
                
                if (!fuel.burnable) {
                    return new BurnResult(0, 0, 0, 0, new ObjectArrayList<>(), new FuelAmount(0));
                }
                
                final double fertility = controlRod.fertility / (double) totalAmount;
                final double temp = controlRod.temperature;
                final double insertion = controlRod.actual;
                final double fuelAmount = this.fuelAmount.fpAmount() / (double) totalCapacity;
                
                // JIT should immediately yeet these allocs
                final double[] exp = new double[4];
                final double[] curve = new double[4];
                final double[] linear = new double[4];
                final double[][] output = new double[4][4];
                
                for (int i = 0; i < 4; i++) {
                    exp[i] = fertility * fuel.reactionProperties[0][0] + temp * fuel.reactionProperties[1][0] * insertion * fuel.reactionProperties[2][0] + fuelAmount * fuel.reactionProperties[3][0];
                }
                for (int i = 0; i < 4; i++) {
                    exp[i] = fuel.reactionProperties[4][0] * Math.exp(fuel.reactionProperties[5][0] * (exp[i] - fuel.reactionProperties[6][0])) + fuel.reactionProperties[7][0];
                }
                
                for (int i = 0; i < 4; i++) {
                    curve[i] = fertility * fuel.reactionProperties[8][i] + temp * fuel.reactionProperties[9][i] * insertion * fuel.reactionProperties[10][i] + fuelAmount * fuel.reactionProperties[11][i];
                }
                for (int i = 0; i < 4; i++) {
                    curve[i] = fuel.reactionProperties[12][i] * Math.exp(-1.0 * Math.pow((curve[i] * fuel.reactionProperties[13][i]) - fuel.reactionProperties[14][i], 2)) + fuel.reactionProperties[15][i];
                }
                
                for (int i = 0; i < 4; i++) {
                    linear[i] = fertility * fuel.reactionProperties[16][i] + temp * fuel.reactionProperties[17][i] * insertion * fuel.reactionProperties[18][i] + fuelAmount * fuel.reactionProperties[19][i];
                }
                for (int i = 0; i < 4; i++) {
                    linear[i] = linear[i] + fuel.reactionProperties[20][i];
                }
                
                for (int i = 0; i < 4; i++) {
                    output[0][i] = exp[0] * fuel.reactionProperties[21][i] + exp[1] * fuel.reactionProperties[22][i] * exp[2] * fuel.reactionProperties[23][i] + exp[3] * fuel.reactionProperties[24][i];
                    output[1][i] = curve[0] * fuel.reactionProperties[25][i] + curve[1] * fuel.reactionProperties[26][i] * curve[2] * fuel.reactionProperties[27][i] + curve[3] * fuel.reactionProperties[28][i];
                    output[2][i] = linear[0] * fuel.reactionProperties[29][i] + linear[1] * fuel.reactionProperties[30][i] * linear[2] * fuel.reactionProperties[31][i] + linear[3] * fuel.reactionProperties[24][i];
                }
                
                for (int i = 0; i < 4; i++) {
                    output[3][i] = output[0][i] * fuel.reactionProperties[33][i] + output[1][i] * fuel.reactionProperties[34][i] * output[2][i] * fuel.reactionProperties[35][i] + fuel.reactionProperties[36][i];
                }
                
                final double burnPercent = output[3][0];
                final double intensityPerBurn = output[3][1];
                final double hardness = output[3][2];
                final double spread = output[3][3];
                
                final double toBurn = burnPercent * this.fuelAmount.fpAmount();
                final double heat = toBurn * fuel.rfPerBurn;
                final double intensity = intensityPerBurn * toBurn;
                
                final var burnt = new FuelAmount(toBurn);
                final var byproducts = new ObjectArrayList<Pair<Fuel, FuelAmount>>();
                for (final var value : fuel.burnResults) {
                    byproducts.add(Pair.of(value.first(), burnt.multiply(value.second())));
                }
    
                return new BurnResult(heat, intensity, hardness, spread, Collections.unmodifiableList(byproducts), burnt);
            }
        }
        
        Object2ObjectOpenHashMap<Fuel, Container> fuels = new Object2ObjectOpenHashMap<>();
    }
    
    private record RadiationPacket(double neutronIntensity, double neutronHardness, double neutronSpread) {
    }
    
    public ReferenceSimulation(SimulationDescription description) {
    
    }
    
    void tick() {
    
    }
}
