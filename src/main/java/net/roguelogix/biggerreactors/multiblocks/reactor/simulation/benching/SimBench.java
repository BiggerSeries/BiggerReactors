package net.roguelogix.biggerreactors.multiblocks.reactor.simulation.benching;

import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.SimulationDescription;
import net.roguelogix.biggerreactors.registries.ReactorModeratorRegistry;
import net.roguelogix.phosphophyllite.threading.Queues;

import java.util.Objects;

public class SimBench {
    public static void main(String[] args) {
//        System.setProperty("org.lwjgl.util.Debug", String.valueOf(true));
//        System.setProperty("org.lwjgl.util.DebugAllocator", String.valueOf(true));
        int spaceBetweenRods = 1;
        int XZ = 128 * spaceBetweenRods, Y = 192;
        int rodOffset = 0;
        int ticks = 50;
        
        Queues.offThread.enqueue(() -> {
        });
        SimulationDescription simulationDescription = new SimulationDescription();
        simulationDescription.setSize(XZ, Y, XZ);
        for (int i = rodOffset; i < XZ; i += spaceBetweenRods) {
            for (int j = rodOffset; j < XZ; j += spaceBetweenRods) {
                simulationDescription.setControlRod(i, j, true);
            }
        }
        simulationDescription.setDefaultIModeratorProperties(new ReactorModeratorRegistry.ModeratorProperties(1, 1, 1.1, 1));
        simulationDescription.setPassivelyCooled(true);
        
        long start = System.nanoTime();
    
        final var simulationBuilder = new SimulationDescription.Builder(false, false, true, true, false);
        final var simulation = simulationBuilder.build(simulationDescription);
        simulation.fuelTank().insertFuel(Long.MAX_VALUE, false);
//        long JITStart = System.nanoTime();
//        for (int i = 0; i < 1000; i++) {
//            simulation.fuelTank().extractWaste(Long.MAX_VALUE, false);
//            simulation.fuelTank().insertFuel(Long.MAX_VALUE, false);
//            simulation.tick(true);
//        }
//        long tickStart = System.nanoTime();
        for (int i = 0; i < 50000; i++) {
            simulation.fuelTank().extractWaste(Long.MAX_VALUE, false);
            simulation.fuelTank().insertFuel(Long.MAX_VALUE, false);
            simulation.tick(true);
        }
//
//        long SerializeStart = System.nanoTime();
//        var nbt = simulation.save();
        
        long MStart = System.nanoTime();
        final var simulation1Builder = new SimulationDescription.Builder(false, true, true, true, false);
        final var simulation2Builder = new SimulationDescription.Builder(false, true, false, false, true);
        var simulation1 = simulation1Builder.build(simulationDescription);
        var simulation2 = simulation2Builder.build(simulationDescription);
        
        // accounts for async simulations being "tick behind"
        int sim1Ticks = ticks + (simulation1.isAsync() ? 1 : 0);
        int sim2Ticks = ticks + (simulation2.isAsync() ? 1 : 0);
//        if (simulation1 instanceof OpenCLReactorSimulation oclSim) {
//            oclSim.syncMode = true;
//        }
//        if (simulation2 instanceof OpenCLReactorSimulation oclSim) {
//            oclSim.syncMode = true;
//        }
        long deserializeStart = System.nanoTime();
//        assert nbt != null;
//        simulation.load(nbt);
        
        long MJITStart = System.nanoTime();
        for (int i = 0; i < 50; i++) {
            simulation1.fuelTank().extractWaste(Long.MAX_VALUE, false);
            simulation1.fuelTank().insertFuel(Long.MAX_VALUE, false);
            simulation1.tick(true);
            simulation2.fuelTank().extractWaste(Long.MAX_VALUE, false);
            simulation2.fuelTank().insertFuel(Long.MAX_VALUE, false);
            simulation2.tick(true);
        }
        long MtickStart1 = System.nanoTime();
        for (int i = 0; i < sim1Ticks; i++) {
            simulation1.fuelTank().extractWaste(Long.MAX_VALUE, false);
            simulation1.fuelTank().insertFuel(Long.MAX_VALUE, false);
            simulation1.tick(true);
        }
        long MtickStart2 = System.nanoTime();
        for (int i = 0; i < sim2Ticks; i++) {
            simulation2.fuelTank().extractWaste(Long.MAX_VALUE, false);
            simulation2.fuelTank().insertFuel(Long.MAX_VALUE, false);
            simulation2.tick(true);
        }
        
        long end = System.nanoTime();
        
        double totalRodsProcessed = (double) XZ / spaceBetweenRods;
        totalRodsProcessed *= totalRodsProcessed;
        totalRodsProcessed *= Y;
        totalRodsProcessed *= ticks;
        
        System.out.printf("total rods processed %.0f\n", totalRodsProcessed);

//        long setupTime = JITStart - start;
//        long JITTime = tickStart - JITStart;
//        long tickTime = SerializeStart - tickStart;
//        long serializationTime = MStart - SerializeStart;
        long mSetupTime = deserializeStart - MStart;
        long deserializeTime = MJITStart - deserializeStart;
        long MJITTime = MtickStart1 - MJITStart;
        long MTickTime1 = MtickStart2 - MtickStart1;
        long MTickTime2 = end - MtickStart2;
        long totalTime = end - start;
//        System.out.println(setupTime / 1_000_000);
//        System.out.println(JITTime / 1_000_000);
//        System.out.println(tickTime / 1_000_000);
//        System.out.println(serializationTime / 1_000_000);
        System.out.println();
        System.out.print("setup: ");
        System.out.println(mSetupTime / 1_000_000.0);
//        System.out.print("deserialize: ");
//        System.out.println(deserializeTime / 1_000_000);
        System.out.print("jit: ");
        System.out.println(MJITTime / 1_000_000.0);
        System.out.print("tick1 total: ");
        System.out.println(MTickTime1 / 1_000_000.0);
        System.out.print("per tick1: ");
        System.out.println(MTickTime1 / (1_000_000.0 * ticks));
        System.out.print("per rod tick1: ");
        System.out.println(MTickTime1 / totalRodsProcessed);
        System.out.print("tick2 total: ");
        System.out.println(MTickTime2 / 1_000_000.0);
        System.out.print("per tick2: ");
        System.out.println(MTickTime2 / (1_000_000.0 * ticks));
        System.out.print("per rod tick2: ");
        System.out.println(MTickTime2 / totalRodsProcessed);
        System.out.print("total: ");
        System.out.println(totalTime / 1_000_000.0);
        
        System.out.println();
        System.out.println(simulation1.getClass().getSimpleName());
        System.out.print("TEMP: ");
        System.out.print(simulation1.fuelHeat());
        System.out.print("/");
        System.out.println(simulation1.stackHeat());
        System.out.print("RAD: ");
        System.out.println(simulation1.fertility());
        System.out.print("RF: ");
        System.out.println(simulation1.battery().generatedLastTick());
        
        System.out.println();
        System.out.println(simulation2.getClass().getSimpleName());
        System.out.print("TEMP: ");
        System.out.print(simulation2.fuelHeat());
        System.out.print("/");
        System.out.println(simulation2.stackHeat());
        System.out.print("RAD: ");
        System.out.println(simulation2.fertility());
        System.out.print("RF: ");
        System.out.println(simulation2.battery().generatedLastTick());
        
        System.out.println();
        System.out.printf("Total time        (ms)    : %.2f\n", totalTime / 1_000_000.0);
        System.out.printf("Total time        (s)     : %.2f\n", totalTime / 1_000_000_000.0);
        System.out.printf("Total time        (m)     : %.2f\n", totalTime / 60_000_000_000.0);
        System.out.printf("Total rods processed      : %.0f\n", totalRodsProcessed);
        System.out.println();
        System.out.printf("Mode run                  : %s\n", simulation1.getClass().getSimpleName());
        System.out.printf("Time for sim      (ms)    : %.2f\n", MTickTime1 / 1_000_000.0);
        System.out.printf("Time per tick     (ms)    : %.2f\n", MTickTime1 / (1_000_000.0 * ticks));
        System.out.printf("Time per rod tick (ns)    : %.2f\n", MTickTime1 / totalRodsProcessed);
        System.out.printf("Tick time multiple (50ms) : %.2f\n", ((MTickTime1 / ticks) / 1_000_000.0) / 50.0);
        System.out.printf("Max acceleration factor   : %.2f\n", 50.0 / ((MTickTime1 / ticks) / 1_000_000.0));
        System.out.printf("Max TPS                   : %.2f\n", 20.0 * 50.0 / ((MTickTime1 / ticks) / 1_000_000.0));
        System.out.printf("Sim 1 temp                : %.2f\n", simulation1.fuelHeat());
        System.out.printf("Sim 1 rad                 : %.6f\n", simulation1.fertility());
        System.out.printf("Sim 1 RF/t                : %d\n", Objects.requireNonNull(simulation1.battery()).generatedLastTick());
        System.out.printf("Sim 1 mb/t                : %.3f\n", simulation1.fuelTank().burnedLastTick());
        System.out.println();
        System.out.printf("Mode run                  : %s\n", simulation2.getClass().getSimpleName());
        System.out.printf("Time for sim      (ms)    : %.2f\n", MTickTime2 / 1_000_000.0);
        System.out.printf("Time per tick     (ms)    : %.2f\n", MTickTime2 / (1_000_000.0 * ticks));
        System.out.printf("Time per rod tick (ns)    : %.2f\n", MTickTime2 / totalRodsProcessed);
        System.out.printf("Tick time multiple (50ms) : %.2f\n", ((MTickTime2 / ticks) / 1_000_000.0) / 50.0);
        System.out.printf("Max acceleration factor   : %.2f\n", 50.0 / ((MTickTime2 / ticks) / 1_000_000.0));
        System.out.printf("Max TPS                   : %.2f\n", 20.0 * 50.0 / ((MTickTime2 / ticks) / 1_000_000.0));
        System.out.printf("Sim 2 temp                : %.2f\n", simulation2.fuelHeat());
        System.out.printf("Sim 2 rad                 : %.6f\n", simulation2.fertility());
        System.out.printf("Sim 2 RF/t                : %d\n", Objects.requireNonNull(simulation2.battery()).generatedLastTick());
        System.out.printf("Sim 2 mb/t                : %.3f\n", simulation2.fuelTank().burnedLastTick());
        
        System.out.println();
        System.out.printf("Sim1Time/Sim2Time         : %.4f\n", ((double) MTickTime1) / ((double) MTickTime2));
        System.out.printf("Sim2Time/Sim1Time         : %.4f\n", ((double) MTickTime2) / ((double) MTickTime1));
        System.out.println();
        System.out.println("breakpoint");

//        OpenCLReactorSimulation.shutdown();
    }
}
