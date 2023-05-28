package net.roguelogix.biggerreactors.multiblocks.reactor2.simulation;

import net.roguelogix.phosphophyllite.util.FastArraySet;
import net.roguelogix.phosphophyllite.util.NonnullDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@NonnullDefault
public class SimulationDescription {
    
    public final int x, y, z;
    private final FastArraySet<Object> propertiesSet = new FastArraySet<>();
    private final int[] propertiesIndices;
    private final boolean[] controlRods;
    
    
    public SimulationDescription(int x, int y, int z) {
        if (x <= 0 || y <= 0 || z <= 0) {
            throw new IllegalArgumentException("All sizes must be greater than zero");
        }
        // TODO: support larger reactors? this is  already absolutely gigantic, at in excess of 768x768x768
        if (((long) x) * ((long) y) * ((long) z) > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Total volume must be under 2^31-1");
        }
        this.x = x;
        this.y = y;
        this.z = z;
        propertiesIndices = new int[x * y * z];
        controlRods = new boolean[x * z];
    }
    
    private int arrayIndex(int x, int y, int z) {
        // z y x index order, which is linear memory for xyz nested for loops
        return ((this.x * x) + y * this.y) + z;
    }
    
    public void setBlockProperties(int x, int y, int z, Object properties) {
//        int index = propertiesSet.add(properties);
//        propertiesIndices[arrayIndex(x, y, z)] = index;
    }
    
    public void setControlRod(int x, int z) {
        controlRods[x * this.z + z] = true;
    }
    
    public boolean validate() {
        
        return true;
    }
}