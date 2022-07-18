package net.roguelogix.biggerreactors.multiblocks.reactor.simulation;

import net.roguelogix.biggerreactors.Config;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.accellerated.ocl.SingleQueueOpenCL12Simulation;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.accellerated.ocl.CLUtil;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.cpu.FullPassReactorSimulation;
import net.roguelogix.biggerreactors.multiblocks.reactor.simulation.cpu.TimeSlicedReactorSimulation;
import net.roguelogix.biggerreactors.registries.ReactorModeratorRegistry;
import net.roguelogix.phosphophyllite.serialization.IPhosphophylliteSerializable;
import net.roguelogix.phosphophyllite.serialization.PhosphophylliteCompound;
import net.roguelogix.phosphophyllite.util.NonnullDefault;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@NonnullDefault
public class SimulationDescription implements IPhosphophylliteSerializable {
    
    public SimulationDescription() {
    }
    
    public SimulationDescription(@Nonnull PhosphophylliteCompound compound) {
        load(compound);
    }
    
    int x = 0, y = 0, z = 0;
    @Nonnull
    // accessing this constant may cause a classloading crash when loaded w/o forge, need to check that and move stuff as needed
    ReactorModeratorRegistry.IModeratorProperties defaultModeratorProperties = ReactorModeratorRegistry.ModeratorProperties.EMPTY_MODERATOR;
    @Nullable
    ReactorModeratorRegistry.IModeratorProperties[][][] moderatorProperties = null;
    @Nullable
    boolean[][][] manifoldLocations = null;
    int manifoldCount = 0;
    @Nullable
    boolean[][] controlRodLocations = null;
    int controlRodCount = 0;
    boolean passivelyCooled = false;
    double ambientTemperature = 273.15;
    
    public void setSize(int x, int y, int z) {
        if (x <= 0 || y <= 0 || z <= 0) {
            throw new IllegalArgumentException("all sizes must be greater than zero");
        }
        this.x = x;
        this.y = y;
        this.z = z;
        if (moderatorProperties == null || moderatorProperties.length < x || moderatorProperties[0].length < y || moderatorProperties[0][0].length < z) {
            moderatorProperties = new ReactorModeratorRegistry.IModeratorProperties[x][y][z];
        }
        if (manifoldLocations == null || manifoldLocations.length < x || manifoldLocations[0].length < y || manifoldLocations[0][0].length < z) {
            manifoldLocations = new boolean[x][y][z];
        }
        if (controlRodLocations == null || controlRodLocations.length < x || controlRodLocations[0].length < z) {
            controlRodLocations = new boolean[x][z];
        }
    }
    
    public void setDefaultIModeratorProperties(ReactorModeratorRegistry.IModeratorProperties properties) {
        defaultModeratorProperties = properties;
    }
    
    public void setModeratorProperties(int x, int y, int z, @Nullable ReactorModeratorRegistry.IModeratorProperties properties) {
        if (moderatorProperties == null) {
            if (properties == null) {
                return;
            }
            throw new IllegalStateException("Size must be set before adding moderators");
        }
        if (x < 0 || x >= moderatorProperties.length || y < 0 || y >= moderatorProperties[0].length || z < 0 || z >= moderatorProperties[0][0].length) {
            if (properties == null) {
                return;
            }
            throw new IndexOutOfBoundsException("Attempt to add moderator outside of reactor bounds");
        }
        moderatorProperties[x][y][z] = properties;
    }
    
    public void setControlRod(int x, int z, boolean isControlRod) {
        if (controlRodLocations == null) {
            if (!isControlRod) {
                return;
            }
            throw new IllegalStateException("Size must be set before adding control rods");
        }
        if (x < 0 || x >= controlRodLocations.length || z < 0 || z >= controlRodLocations[0].length) {
            if (!isControlRod) {
                return;
            }
            throw new IndexOutOfBoundsException("Attempt to add control rod outside of reactor bounds");
        }
        if (controlRodLocations[x][z] != isControlRod) {
            controlRodCount += isControlRod ? 1 : -1;
        }
        controlRodLocations[x][z] = isControlRod;
    }
    
    public void setManifold(int x, int y, int z, boolean manifold) {
        if (manifoldLocations == null) {
            if (!manifold) {
                return;
            }
            throw new IllegalStateException("Size must be set before adding manifolds");
        }
        if (x < 0 || x >= manifoldLocations.length || y < 0 || y >= manifoldLocations[0].length || z < 0 || z >= manifoldLocations[0][0].length) {
            if (!manifold) {
                return;
            }
            throw new IndexOutOfBoundsException("Attempt to add manifold outside of reactor bounds");
        }
        if (manifoldLocations[x][y][z] != manifold) {
            manifoldCount += manifold ? 1 : -1;
        }
        manifoldLocations[x][y][z] = manifold;
    }
    
    public void setPassivelyCooled(boolean passivelyCooled) {
        this.passivelyCooled = passivelyCooled;
    }
    
    public void setAmbientTemperature(double ambientTemperature) {
        this.ambientTemperature = ambientTemperature;
    }
    
    public record Builder(boolean experimental, boolean fullPass, boolean allowOffThread, boolean allowMultiThread, boolean allowAccelerated) {
        
        public IReactorSimulation build(SimulationDescription description) {
            description.ensureValid();
    
            if (experimental) {
                return new SingleQueueOpenCL12Simulation(description);
            }
            if (!fullPass) {
                return new TimeSlicedReactorSimulation(description);
            }
            
            var rodMultiple = description.controlRodCount / Config.CONFIG.Reactor.ModeSpecific.ControlRodBatchSize;
            
            if (allowAccelerated && rodMultiple >= 16) {
                if (CLUtil.available) {
                    return new SingleQueueOpenCL12Simulation(description);
                }
            }
            if (allowMultiThread && rodMultiple >= 2) {
                return new FullPassReactorSimulation.MultiThreaded(description, false);
            }
            if (allowOffThread) {
                return new FullPassReactorSimulation.MultiThreaded(description, true);
            }
            return new FullPassReactorSimulation(description);
        }
    }
    
    public void ensureValid() {
        if (controlRodLocations == null) {
            throw new IllegalArgumentException();
        }
        if (moderatorProperties == null) {
            throw new IllegalArgumentException();
        }
        if (manifoldLocations == null) {
            throw new IllegalArgumentException();
        }
    }
    
    public int x() {
        return x;
    }
    
    public int y() {
        return y;
    }
    
    public int z() {
        return z;
    }
    
    public ReactorModeratorRegistry.IModeratorProperties defaultModeratorProperties() {
        return defaultModeratorProperties;
    }
    
    public int controlRodCount() {
        return controlRodCount;
    }
    
    public boolean isControlRodAt(int x, int z) {
        assert controlRodLocations != null;
        return controlRodLocations[x][z];
    }
    
    public boolean passivelyCooled() {
        return passivelyCooled;
    }
    
    public int manifoldCount() {
        return manifoldCount;
    }
    
    public ReactorModeratorRegistry.IModeratorProperties moderatorPropertiesAt(int x, int y, int z) {
        assert moderatorProperties != null;
        return moderatorProperties[x][y][z];
    }
    
    public boolean isManifoldAt(int x, int y, int z) {
        assert manifoldLocations != null;
        return manifoldLocations[x][y][z];
    }
    
    public double ambientTemperature(){
        return ambientTemperature;
    }
    
    @Override
    @Nullable
    public PhosphophylliteCompound save() {
        final var compound = new PhosphophylliteCompound();
        if (moderatorProperties == null || manifoldLocations == null || controlRodLocations == null) {
            return null;
        }
        ArrayList<ReactorModeratorRegistry.IModeratorProperties> moderatorProperties = new ArrayList<>();
        ArrayList<ArrayList<ArrayList<Integer>>> moderatorIndexes = new ArrayList<>();
        ArrayList<ArrayList<ArrayList<Boolean>>> manifoldLocations = new ArrayList<>();
        ArrayList<ArrayList<Boolean>> controlRodLocations = new ArrayList<>();
        for (int i = 0; i < x; i++) {
            ArrayList<ArrayList<Integer>> moderatorIndexesX = new ArrayList<>();
            ArrayList<ArrayList<Boolean>> manifoldLocationsX = new ArrayList<>();
            ArrayList<Boolean> controlRodLocationsX = new ArrayList<>();
            for (int j = 0; j < y; j++) {
                ArrayList<Integer> moderatorIndexesXY = new ArrayList<>();
                ArrayList<Boolean> manifoldLocationsXY = new ArrayList<>();
                for (int k = 0; k < z; k++) {
                    ReactorModeratorRegistry.IModeratorProperties properties = this.moderatorProperties[i][j][k];
                    if (properties == null) {
                        moderatorIndexesXY.add(-1);
                    } else {
                        int index = moderatorProperties.indexOf(properties);
                        if (index == -1) {
                            index = moderatorProperties.size();
                            moderatorProperties.add(properties);
                        }
                        moderatorIndexesXY.add(index);
                    }
                    manifoldLocationsXY.add(this.manifoldLocations[i][j][k]);
                }
                moderatorIndexesX.add(moderatorIndexesXY);
                manifoldLocationsX.add(manifoldLocationsXY);
            }
            for (int j = 0; j < z; j++) {
                controlRodLocationsX.add(this.controlRodLocations[i][j]);
            }
            moderatorIndexes.add(moderatorIndexesX);
            manifoldLocations.add(manifoldLocationsX);
            controlRodLocations.add(controlRodLocationsX);
        }
        
        
        compound.put("x", x);
        compound.put("y", y);
        compound.put("z", z);
        compound.put("moderatorProperties", moderatorProperties);
        compound.put("moderatorIndices", moderatorIndexes);
        compound.put("manifoldLocations", manifoldLocations);
        compound.put("controlRodLocations", controlRodLocations);
        compound.put("defaultModeratorProperties", defaultModeratorProperties.toROBNMap());
        compound.put("passivelyCooled", passivelyCooled);
        compound.put("ambientTemperature", ambientTemperature);
        
        return compound;
    }
    
    @Override
    public void load(@Nonnull PhosphophylliteCompound compound) {
        setSize(compound.getInt("x"), compound.getInt("y"), compound.getInt("z"));
        final ArrayList<ReactorModeratorRegistry.ModeratorProperties> moderatorProperties = new ArrayList<>();
        {
            final List<?> moderatorROBN = compound.getList("moderatorProperties");
            for (Object o : moderatorROBN) {
                if (!(o instanceof Map<?, ?> map)) {
                    throw new IllegalArgumentException("Malformed Binary");
                }
                double absorption = 0;
                if (map.get("absorption") instanceof Number num) {
                    absorption = num.doubleValue();
                }
                double heatEfficiency = 0;
                if (map.get("heatEfficiency") instanceof Number num) {
                    heatEfficiency = num.doubleValue();
                }
                double moderation = 1;
                if (map.get("moderation") instanceof Number num) {
                    moderation = num.doubleValue();
                }
                double heatConductivity = 0;
                if (map.get("heatConductivity") instanceof Number num) {
                    heatConductivity = num.doubleValue();
                }
                moderatorProperties.add(new ReactorModeratorRegistry.ModeratorProperties(absorption, heatEfficiency, moderation, heatConductivity));
            }
        }
        List<?> moderatorIndices = compound.getList("moderatorIndices");
        List<?> manifoldLocations = compound.getList("manifoldLocations");
        List<?> controlRodLocations = compound.getList("controlRodLocations");
        if (moderatorIndices.size() != x || manifoldLocations.size() != x || controlRodLocations.size() != x) {
            throw new IllegalArgumentException("Malformed Binary");
        }
        for (int i = 0; i < x; i++) {
            if (!(moderatorIndices.get(i) instanceof List<?> moderatorIndicesX)) {
                throw new IllegalArgumentException("Malformed Binary");
            }
            if (!(manifoldLocations.get(i) instanceof List<?> manifoldLocationsX)) {
                throw new IllegalArgumentException("Malformed Binary");
            }
            if (!(controlRodLocations.get(i) instanceof List<?> controlRodLocationsX)) {
                throw new IllegalArgumentException("Malformed Binary");
            }
            if (moderatorIndicesX.size() != y || manifoldLocationsX.size() != y || controlRodLocations.size() != z) {
                throw new IllegalArgumentException("Malformed Binary");
            }
            for (int j = 0; j < y; j++) {
                if (!(moderatorIndicesX.get(j) instanceof List<?> moderatorIndicesXY)) {
                    throw new IllegalArgumentException("Malformed Binary");
                }
                if (!(manifoldLocationsX.get(j) instanceof List<?> manifoldLocationsXY)) {
                    throw new IllegalArgumentException("Malformed Binary");
                }
                if (moderatorIndicesXY.size() != z || manifoldLocationsXY.size() != z) {
                    throw new IllegalArgumentException("Malformed Binary");
                }
                for (int k = 0; k < z; k++) {
                    if (!(moderatorIndicesXY.get(k) instanceof Number moderatorIndex)) {
                        throw new IllegalArgumentException("Malformed Binary");
                    }
                    if (!(manifoldLocationsXY.get(k) instanceof Boolean isManifold)) {
                        throw new IllegalArgumentException("Malformed Binary");
                    }
                    int index = moderatorIndex.intValue();
                    setModeratorProperties(i, j, k, index != -1 ? moderatorProperties.get(index) : null);
                    setManifold(i, j, k, isManifold);
                }
            }
            for (int j = 0; j < z; j++) {
                if (!(controlRodLocationsX.get(j) instanceof Boolean isControlRod)) {
                    throw new IllegalArgumentException("Malformed Binary");
                }
                setControlRod(i, j, isControlRod);
            }
        }
        
        {
            Map<?, ?> map = compound.getMap("defaultModeratorProperties");
            if (map.isEmpty()) {
                throw new IllegalArgumentException("Malformed Binary");
            }
            double absorption = 0;
            if (map.get("absorption") instanceof Number num) {
                absorption = num.doubleValue();
            }
            double heatEfficiency = 0;
            if (map.get("heatEfficiency") instanceof Number num) {
                heatEfficiency = num.doubleValue();
            }
            double moderation = 1;
            if (map.get("moderation") instanceof Number num) {
                moderation = num.doubleValue();
            }
            double heatConductivity = 0;
            if (map.get("heatConductivity") instanceof Number num) {
                heatConductivity = num.doubleValue();
            }
            setDefaultIModeratorProperties(new ReactorModeratorRegistry.ModeratorProperties(absorption, heatEfficiency, moderation, heatConductivity));
        }
        setPassivelyCooled(compound.getBoolean("passivelyCooled"));
        setAmbientTemperature(compound.getDouble("ambientTemperature"));
    }
}
