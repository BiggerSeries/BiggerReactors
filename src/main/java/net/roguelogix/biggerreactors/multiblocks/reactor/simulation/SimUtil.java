package net.roguelogix.biggerreactors.multiblocks.reactor.simulation;

import net.roguelogix.biggerreactors.Config;
import net.roguelogix.phosphophyllite.registry.OnModLoad;
import net.roguelogix.phosphophyllite.repack.org.joml.*;

import java.lang.Math;
import java.util.ArrayList;

public class SimUtil {
    
    public static class ControlRod implements IReactorSimulation.ControlRod{
        final int x;
        final int z;
        double insertion = 0;
        
        public ControlRod(int x, int z) {
            this.x = x;
            this.z = z;
        }
    
        @Override
        public double insertion() {
            return insertion;
        }
    
        @Override
        public void setInsertion(double insertion) {
            this.insertion = insertion;
        }
    }
    
    public static final Vector2ic[] cardinalDirections = new Vector2ic[]{
            new Vector2i(1, 0),
            new Vector2i(-1, 0),
            new Vector2i(0, 1),
            new Vector2i(0, -1),
    };
    
    public static final Vector3ic[] axisDirections = new Vector3ic[]{
            new Vector3i(+1, +0, +0),
            new Vector3i(-1, +0, +0),
            new Vector3i(+0, +1, +0),
            new Vector3i(+0, -1, +0),
            new Vector3i(+0, +0, +1),
            new Vector3i(+0, +0, -1)
    };
    
    public static class RayStep {
        final Vector3i offset;
        final double length;
        
        private RayStep(Vector3i offset, double length) {
            this.offset = offset;
            this.length = length;
        }
    }
    
    public static final ArrayList<ArrayList<RayStep>> rays = new ArrayList<>();
    
    public static final Vector3dc[] rayDirections = new Vector3dc[]{
            new Vector3d(+1, 0, 0),
            new Vector3d(-1, 0, 0),
            new Vector3d(0, +1, 0),
            new Vector3d(0, -1, 0),
            new Vector3d(0, 0, +1),
            new Vector3d(0, 0, -1),
            
            
            new Vector3d(+1, +1, 0),
            new Vector3d(+1, -1, 0),
            new Vector3d(-1, +1, 0),
            new Vector3d(-1, -1, 0),
            
            new Vector3d(0, +1, +1),
            new Vector3d(0, +1, -1),
            new Vector3d(0, -1, +1),
            new Vector3d(0, -1, -1),
            
            new Vector3d(+1, 0, +1),
            new Vector3d(-1, 0, +1),
            new Vector3d(+1, 0, -1),
            new Vector3d(-1, 0, -1),
    };
    
    @OnModLoad
    private static void onModLoad() {
        // trigger classload on mod load, so this doesnt happen at runtime
    }
    
    static {
        // Config is registered before any @OnModLoad classes are loaded/called, so, the the config is loaded first
        final double TTL = Config.CONFIG.Reactor.IrradiationDistance;
        
        final Vector3d radiationDirection = new Vector3d();
        final Vector3d currentSegment = new Vector3d();
        final Vector3d currentSegmentStart = new Vector3d();
        final Vector3d currentSegmentEnd = new Vector3d();
        final Vector3d currentSectionBlock = new Vector3d();
        final Vector3d planes = new Vector3d();
        double processedLength;
        
        final Vector3d[] intersections = new Vector3d[]{
                new Vector3d(),
                new Vector3d(),
                new Vector3d()
        };
        
        // ray tracing, because cardinal directions isn't good enough for me
        // also keeps you from building a skeleton reactor
        
        for (Vector3dc rayDirection : rayDirections) {
            final ArrayList<RayStep> raySteps = new ArrayList<>();
            
            radiationDirection.set(rayDirection);
            radiationDirection.normalize();
            
            // radiation extends for RadiationBlocksToLive from the outside of the fuel rod
            // but i rotate about the center of the fuel rod, so, i need to add the length of the inside
            currentSegmentStart.set(radiationDirection);
            currentSegmentStart.mul(1 / Math.abs(currentSegmentStart.get(currentSegmentStart.maxComponent())));
            currentSegmentStart.mul(0.5);
            radiationDirection.mul(TTL + currentSegmentStart.length());
            
            processedLength = 0;
            double totalLength = radiationDirection.length();
            
            currentSegmentStart.set(0);
            
            // +0.5 or -0.5 for each of them, tells me which way i need to be looking for the intersections
            planes.set(radiationDirection);
            planes.absolute();
            planes.div(radiationDirection);
            planes.mul(0.5);
            
            boolean firstIteration = true;
            while (true) {
                for (int i = 0; i < 3; i++) {
                    final Vector3d intersection = intersections[i];
                    intersection.set(radiationDirection);
                    double component = intersection.get(i);
                    double plane = planes.get(i);
                    intersection.mul(plane / component);
                }
                
                int minVec = 0;
                double minLength = Double.POSITIVE_INFINITY;
                for (int i = 0; i < 3; i++) {
                    double length = intersections[i].lengthSquared();
                    if (length < minLength) {
                        minVec = i;
                        minLength = length;
                    }
                }
                
                // move the plane we just intersected back one
                planes.setComponent(minVec, planes.get(minVec) + (planes.get(minVec) / Math.abs(planes.get(minVec))));
                
                currentSegmentEnd.set(intersections[minVec]);
                currentSegment.set(currentSegmentEnd).sub(currentSegmentStart);
                currentSectionBlock.set(currentSegmentEnd).sub(currentSegmentStart).mul(0.5).add(0.5, 0.5, 0.5).add(currentSegmentStart).floor();
                
                double segmentLength = currentSegment.length();
                boolean breakAfterLoop = processedLength + segmentLength >= totalLength;
                
                segmentLength = Math.min(totalLength - processedLength, segmentLength);
                
                if (!firstIteration && segmentLength != 0) {
                    raySteps.add(new RayStep(new Vector3i(currentSectionBlock, 0), segmentLength));
                }
                firstIteration = false;
                
                
                processedLength += segmentLength;
                if (breakAfterLoop) {
                    break;
                }
                
                currentSegmentStart.set(currentSegmentEnd);
            }
            rays.add(raySteps);
        }
    }
}
