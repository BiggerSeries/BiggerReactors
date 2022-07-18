package net.roguelogix.biggerreactors.multiblocks.reactor.simulation.benching;

import net.roguelogix.biggerreactors.Config;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.IReactorSimulation;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.SimulationDescription;
import net.roguelogix.biggerreactors.registries.ReactorModeratorRegistry;
import net.roguelogix.phosphophyllite.threading.Queues;

import java.util.Objects;

public class MultiSimBench {
    public static void main(String[] args) {
        Queues.offThread.enqueue(() -> {
        });
        
        final int spaceBetweenRods = 1;
        final int XZ = 64 * spaceBetweenRods, Y = 96;
        final int rodOffset = 0;
        final int simulationCount = 4;
        final var simulationBuilder = new SimulationDescription.Builder(false, true, false, false, true);
        final int warmupTicks = 50;
        final int ticks = 5000;
    
        System.out.println("Creating description");
        SimulationDescription simulationDescription = new SimulationDescription();
        simulationDescription.setSize(XZ, Y, XZ);
        for (int i = rodOffset; i < XZ; i += spaceBetweenRods) {
            for (int j = rodOffset; j < XZ; j += spaceBetweenRods) {
                simulationDescription.setControlRod(i, j, true);
            }
        }
        simulationDescription.setDefaultIModeratorProperties(new ReactorModeratorRegistry.ModeratorProperties(1, 1, 1, 1));
        simulationDescription.setPassivelyCooled(true);
    
        final IReactorSimulation[] simulations = new IReactorSimulation[simulationCount];
    
        System.out.println("Building simulations");
        for (int i = 0; i < simulations.length; i++) {
            simulations[i] = simulationBuilder.build(simulationDescription);
//            if(simulations[i] instanceof OpenCLReactorSimulation sim){
//                sim.syncMode = true;
//            }
        }
    
        System.out.println("JIT warmup");
        // JIT warmup
        for (int i = 0; i < warmupTicks; i++) {
            //noinspection ForLoopReplaceableByForEach
            for (int j = 0; j < simulations.length; j++) {
                final var sim = simulations[j];
                sim.fuelTank().extractWaste(Long.MAX_VALUE, false);
                sim.fuelTank().insertFuel(Long.MAX_VALUE, false);
                sim.tick(true);
            }
        }
    
        System.out.println("Running test");
        long start = System.nanoTime();
        for (int i = 0; i < ticks; i++) {
            //noinspection ForLoopReplaceableByForEach
            for (int j = 0; j < simulations.length; j++) {
                final var sim = simulations[j];
                sim.fuelTank().extractWaste(Long.MAX_VALUE, false);
                sim.fuelTank().insertFuel(Long.MAX_VALUE, false);
                sim.tick(true);
            }
        }
        long end = System.nanoTime();
        
        double totalRodsProcessed = (double) XZ / spaceBetweenRods;
        totalRodsProcessed *= totalRodsProcessed;
        totalRodsProcessed *= Y;
        totalRodsProcessed *= ticks;
        totalRodsProcessed *= simulationCount;
        
        double totalTime = end - start;
        double timePerSim = totalTime / simulationCount;
        double timePerTick = totalTime / ticks;
        double timePerSimTick = timePerTick / simulationCount;
        double timePerRodTick = totalTime / totalRodsProcessed;
    
        System.out.println();
        System.out.printf("Mode run                  : %s\n", simulations[0].getClass().getSimpleName());
        System.out.printf("Total time        (ms)    : %.2f\n", totalTime / 1_000_000.0);
        System.out.printf("Time per sim      (ms)    : %.2f\n", timePerSim / 1_000_000.0);
        System.out.printf("Time per tick     (ms)    : %.2f\n", timePerTick / 1_000_000.0);
        System.out.printf("Time per sim tick (ms)    : %.2f\n", timePerSimTick / 1_000_000.0);
        System.out.printf("Time per rod tick (ns)    : %.2f\n", timePerRodTick);
        System.out.printf("Total rods processed      : %.0f\n", totalRodsProcessed);
        System.out.printf("Tick time multiple (50ms) : %.2f\n", (timePerSimTick / 1_000_000.0) / 50.0);
        System.out.printf("Sim 1 temp                : %.2f\n", simulations[0].fuelHeat());
        System.out.printf("Sim 1 rad                 : %.2f\n", simulations[0].fertility());
        System.out.printf("Sim 1 RF/t                : %d\n", Objects.requireNonNull(simulations[0].battery()).generatedLastTick());
    }
}
