package net.roguelogix.biggerreactors.multiblocks.reactor.simulation.cpu;

import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.SimulationConfiguration;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.SimulationDescription;

public class AsyncReactorSimulation extends FullPassReactorSimulation {
    
    private final Thread asyncThread = new Thread(this::asyncThreadFunc);
    
    private boolean requestRunning;
    private double runningResultAverageIntensity = 0;
    private double currentResultAverageIntensity = 0;
    private final ReactorStateData asyncReactorStateData = new ReactorStateData();
    private final IrradiationResult currentResult = new IrradiationResult();
    private final IrradiationResult scaledResult = new IrradiationResult();
    public int tickNumber = 0;
    public int realTicks = 0;
    
    public AsyncReactorSimulation(SimulationDescription simulationDescription, SimulationConfiguration configuration) {
        super(simulationDescription, configuration);
        asyncThread.setDaemon(true);
        asyncThread.setName(this.getClass().getSimpleName());
        asyncThread.start();
    }
    
    private void asyncThreadFunc() {
        while (true) {
            if (!requestRunning) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
                continue;
            }
            runIrradiationRequest(fullPassIrradiationRequest, asyncReactorStateData);
            requestRunning = false;
        }
    }
    
    @Override
    protected double radiate() {
        setupIrradiationTick();
        if (!requestRunning) {
            realTicks++;
            currentResult.fuelRFAdded = fullPassIrradiationRequest.result.fuelRFAdded;
            currentResult.fuelRadAdded = fullPassIrradiationRequest.result.fuelRadAdded;
            currentResult.caseRFAdded = fullPassIrradiationRequest.result.caseRFAdded;
            
            fullPassIrradiationRequest.updateCache();
            asyncReactorStateData.update(this);
            currentResultAverageIntensity = runningResultAverageIntensity;
            runningResultAverageIntensity = 0;
            for (int i = 0; i < initialIntensties.length; i++) {
                runningResultAverageIntensity += initialIntensties[i];
            }
            runningResultAverageIntensity /= initialIntensties.length;
            if (realTicks < 2) {
                runIrradiationRequest(fullPassIrradiationRequest, asyncReactorStateData);
            } else {
                requestRunning = true;
                asyncThread.interrupt();
            }
        }
        
        scaledResult.fuelRFAdded = currentResult.fuelRFAdded;
        scaledResult.fuelRadAdded = currentResult.fuelRadAdded;
        scaledResult.caseRFAdded = currentResult.caseRFAdded;
        
        double averageIntensity = 0;
        for (int i = 0; i < initialIntensties.length; i++) {
            averageIntensity += initialIntensties[i];
        }
        averageIntensity /= initialIntensties.length;
        
        double scalingFactor = averageIntensity / currentResultAverageIntensity;
        
        scaledResult.fuelRFAdded *= scalingFactor;
        scaledResult.fuelRadAdded *= scalingFactor;
        scaledResult.caseRFAdded *= scalingFactor;
        
        collectIrradiationResult(scaledResult);
        tickNumber++;
        return realizeIrradiationTick();
    }
}
