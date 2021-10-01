package net.roguelogix.biggerreactors.multiblocks.reactor.simulation;

import net.roguelogix.biggerreactors.Config;
import net.roguelogix.biggerreactors.registries.ReactorModeratorRegistry;
import net.roguelogix.phosphophyllite.serialization.IPhosphophylliteSerializable;
import net.roguelogix.phosphophyllite.serialization.PhosphophylliteCompound;
import net.roguelogix.phosphophyllite.util.MethodsReturnNonnullByDefault;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
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
    
    public void addControlRod(int x, int z) {
        if (controlRodLocations == null) {
            throw new IllegalStateException("Size must be set before adding control rods");
        }
        if (x < 0 || x >= controlRodLocations.length || z < 0 || z >= controlRodLocations[0].length) {
            throw new IndexOutOfBoundsException("Attempt to add control rod outside of reactor bounds");
        }
        controlRodLocations[x][z] = true;
        controlRodCount++;
    }
    
    public void removeControlRod(int x, int z) {
        if (controlRodLocations == null) {
            return;
        }
        if (x < 0 || x >= controlRodLocations.length || y < 0 || y >= controlRodLocations[0].length) {
            return;
        }
        controlRodLocations[x][z] = false;
    }
    
    public void addManifold(int x, int y, int z) {
        if (manifoldLocations == null) {
            throw new IllegalStateException("Size must be set before adding manifolds");
        }
        if (x < 0 || x >= manifoldLocations.length || y < 0 || y >= manifoldLocations[0].length || z < 0 || z >= manifoldLocations[0][0].length) {
            throw new IndexOutOfBoundsException("Attempt to add manifold outside of reactor bounds");
        }
        manifoldLocations[x][y][z] = true;
        manifoldCount++;
    }
    
    public void removeManifold(int x, int y, int z) {
        if (manifoldLocations == null) {
            return;
        }
        if (x < 0 || x >= manifoldLocations.length || y < 0 || y >= manifoldLocations[0].length || z < 0 || z >= manifoldLocations[0][0].length) {
            return;
        }
        manifoldLocations[x][y][z] = false;
        manifoldCount--;
    }
    
    public void setPassivelyCooled(boolean passivelyCooled) {
        this.passivelyCooled = passivelyCooled;
    }
    
    public void setAmbientTemperature(double ambientTemperature) {
        this.ambientTemperature = ambientTemperature;
    }
    
    public IReactorSimulation build(Config.Mode mode) {
        return switch (mode){
            case MODERN -> new ModernReactorSimulation(this);
            case EXPERIMENTAL -> new ExperimentalReactorSimulation(this);
            case MULTITHREADED -> new MultithreadedReactorSimulation(this);
            //noinspection UnnecessaryDefault
            default -> throw new IllegalArgumentException();
        };
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
                    int index = moderatorProperties.indexOf(properties);
                    if (index == -1) {
                        index = moderatorProperties.size();
                        moderatorProperties.add(properties);
                    }
                    moderatorIndexesXY.add(index);
                    manifoldLocationsXY.add(this.manifoldLocations[x][y][z]);
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
        
        
        compound.put("moderatorProperties", moderatorProperties);
        compound.put("moderatorIndexes", moderatorIndexes);
        compound.put("manifoldLocations", manifoldLocations);
        compound.put("controlRodLocations", controlRodLocations);
        
        return compound;
    }
    
    @Override
    public void load(@Nonnull PhosphophylliteCompound compound) {
    
    }
}
