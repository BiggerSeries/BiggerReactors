package net.roguelogix.biggerreactors.multiblocks.reactor2.simulation;

public class SimulationFunctions {
    
    public record HeatTransfer(double baseBias) {
        public static final HeatTransfer DEFAULT = new HeatTransfer(0);
        
        double compute(double kelvin) {
            return baseBias;
        }
    }
    
    public record Moderator(HeatTransfer heatTransfer) {
        public static final Moderator DEFAULT = new Moderator(HeatTransfer.DEFAULT);
    }
}
