package net.roguelogix.biggerreactors.multiblocks.turbine.tiles;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.roguelogix.biggerreactors.multiblocks.turbine.blocks.TurbineRotorBlade;
import net.roguelogix.biggerreactors.multiblocks.turbine.blocks.TurbineRotorShaft;
import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.multiblock.common.IEventMultiblock;
import net.roguelogix.phosphophyllite.multiblock.validated.IValidatedMultiblock;
import net.roguelogix.phosphophyllite.registry.RegisterTile;
import net.roguelogix.phosphophyllite.threading.Queues;
import net.roguelogix.quartz.DrawBatch;
import net.roguelogix.quartz.Mesh;
import net.roguelogix.quartz.Quartz;
import net.roguelogix.quartz.QuartzEvent;
import org.joml.Matrix4f;
import org.joml.Vector3i;
import org.joml.Vector4f;
import org.joml.Vector4i;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;

import static net.roguelogix.phosphophyllite.multiblock.IAssemblyStateBlock.ASSEMBLED;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TurbineRotorBearingTile extends TurbineBaseTile implements IEventMultiblock.AssemblyStateTransition {
    
    public static boolean APRIL_FOOLS_JOKE = false;
    
    @RegisterTile("turbine_rotor_bearing")
    public static final RegisterTile.Producer<TurbineRotorBearingTile> SUPPLIER = new RegisterTile.Producer<>(TurbineRotorBearingTile::new);
    
    public TurbineRotorBearingTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
    public boolean isRenderBearing = false;
    public double speed = 0;
    public Vector3i rotationAxis = null;
    public ArrayList<Vector4i> rotorConfiguration = null;
    public AABB AABB = null;
    private long sendFullUpdate = Long.MAX_VALUE;
    
    @Nullable
    @Override
    public CompoundTag getUpdateNBT() {
        CompoundTag nbt = new CompoundTag();
        if (nullableController() != null) {
            if (!getBlockState().getValue(ASSEMBLED)) {
                nbt.putBoolean("disassembled", true);
                return nbt;
            }
            nbt.putDouble("speed", controller().simulation().RPM());
            if (sendFullUpdate < Phosphophyllite.tickNumber()) {
                sendFullUpdate = Long.MAX_VALUE;
                nbt.put("config", getUpdateTag());
            }
        }
        return nbt;
    }
    
    @Override
    public void handleUpdateNBT(CompoundTag nbt) {
        if (getBlockState().getValue(ASSEMBLED) && nbt.contains("speed")) {
            speed = nbt.getDouble("speed");
            if (nbt.contains("config")) {
                handleUpdateTag(nbt.getCompound("config"));
            }
//            if (rotationAxis != null) {
//                teardownQuartzModel();
//                setupQuartzModel();
//            }
        } else {
            Queues.clientThread.enqueue(this::teardownQuartzModel);
            isRenderBearing = false;
        }
    }
    
    @Override
    public void handleDataNBT(CompoundTag nbt) {
        if (nbt.contains("rotx")) {
            isRenderBearing = true;
            if (rotationAxis == null) {
                rotationAxis = new Vector3i();
            }
            rotationAxis.set(nbt.getInt("rotx"), nbt.getInt("roty"), nbt.getInt("rotz"));
            if (rotorConfiguration == null) {
                rotorConfiguration = new ArrayList<>();
            }
            rotorConfiguration.clear();
            int rotorShafts = nbt.getInt("shafts");
            for (int i = 0; i < rotorShafts; i++) {
                Vector4i vec = new Vector4i();
                vec.x = nbt.getInt("shaft" + i + "0");
                vec.y = nbt.getInt("shaft" + i + "1");
                vec.z = nbt.getInt("shaft" + i + "2");
                vec.w = nbt.getInt("shaft" + i + "3");
                rotorConfiguration.add(vec);
            }
            AABB = new AABB(nbt.getInt("minx"), nbt.getInt("miny"), nbt.getInt("minz"), nbt.getInt("maxx"), nbt.getInt("maxy"), nbt.getInt("maxz"));
            Queues.clientThread.enqueue(this::setupQuartzModel);
        } else {
            Queues.clientThread.enqueue(this::teardownQuartzModel);
            isRenderBearing = false;
        }
    }
    
    @Override
    public CompoundTag getDataNBT() {
        CompoundTag nbt = super.getDataNBT();
        if (isRenderBearing && nullableController() != null) {
            nbt.putInt("rotx", controller().rotationAxis.getX());
            nbt.putInt("roty", controller().rotationAxis.getY());
            nbt.putInt("rotz", controller().rotationAxis.getZ());
            nbt.putInt("minx", controller().min().x());
            nbt.putInt("miny", controller().min().y());
            nbt.putInt("minz", controller().min().z());
            nbt.putInt("maxx", controller().max().x());
            nbt.putInt("maxy", controller().max().y());
            nbt.putInt("maxz", controller().max().z());
            nbt.putInt("shafts", controller().rotorConfiguration.size());
            ArrayList<Vector4i> config = controller().rotorConfiguration;
            for (int i = 0; i < config.size(); i++) {
                Vector4i vec = config.get(i);
                nbt.putInt("shaft" + i + "0", vec.x);
                nbt.putInt("shaft" + i + "1", vec.y);
                nbt.putInt("shaft" + i + "2", vec.z);
                nbt.putInt("shaft" + i + "3", vec.w);
            }
        }
        return nbt;
    }
    
    @Override
    public void onRemoved(boolean chunkUnload) {
        if (Queues.clientThread != null) {
            Queues.clientThread.enqueue(this::teardownQuartzModel);
        }
    }
    
    @Override
    public void onAssemblyStateTransition(IValidatedMultiblock.AssemblyState oldState, IValidatedMultiblock.AssemblyState newState) {
        sendFullUpdate = Phosphophyllite.tickNumber() + 10;
    }
    
    static {
        Quartz.EVENT_BUS.addListener(TurbineRotorBearingTile::onQuartzStartup);
    }
    
    private static Mesh shaftMesh;
    private static Mesh bladeMesh;
    
    private static void onQuartzStartup(QuartzEvent.Startup quartzStartup) {
        shaftMesh = Quartz.createStaticMesh(TurbineRotorShaft.INSTANCE.defaultBlockState());
        bladeMesh = Quartz.createStaticMesh(TurbineRotorBlade.INSTANCE.defaultBlockState());
    }
    
    private final ObjectArrayList<DrawBatch.Instance> instances = new ObjectArrayList<>();
    
    private void setupQuartzModel() {
        if (level == null || !level.isClientSide) {
            return;
        }
        teardownQuartzModel();
        if (level.getBlockEntity(getBlockPos()) != this) {
            return;
        }
        final var drawBatch = Quartz.getDrawBatcherForAABB(new net.roguelogix.quartz.AABB((int) AABB.minX, (int) AABB.minY, (int) AABB.minZ, (int) AABB.maxX, (int) AABB.maxY, (int) AABB.maxZ));
        final var tempMatrix = new Matrix4f();
        
        final int blade180RotationMultiplier = -rotationAxis.x() | -rotationAxis.y() | rotationAxis.z();
        double initialAngle = 0;
        if (blade180RotationMultiplier > 0) {
            initialAngle += 180;
        }
        if (rotationAxis.x() != 0) {
            initialAngle += 180;
        }
        
        tempMatrix.identity();
        tempMatrix.translate(0.5f, 0.5f, 0.5f);
        if (rotationAxis.x() != 0) {
            tempMatrix.rotate((float) Math.toRadians(-90.0f * rotationAxis.x()), 0, 0, 1);
            initialAngle -= 90;
        } else if (rotationAxis.z() != 0) {
            tempMatrix.rotate((float) Math.toRadians(90.0f * rotationAxis.z()), 1, 0, 0);
        } else if (rotationAxis.y() != 1) {
            tempMatrix.rotate((float) Math.toRadians(180), 1, 0, 0);
        }
        tempMatrix.rotate((float) Math.toRadians(initialAngle), 0, 1, 0);
        tempMatrix.translate(-0.5f, -0.5f, -0.5f);
        
        final var dynamicMatrix = drawBatch.createDynamicMatrix(tempMatrix, (matrix, nanoSinceLastFrame, partialTicks, playerBlock, playerPartialBlock) -> {
            double toRotate = 0;
            
            double speed = this.speed / 10f;
            if (speed > 0.001f) {
                double elapsedTimeMilis = ((double) nanoSinceLastFrame) / 1_000_000;
                toRotate += speed * ((float) elapsedTimeMilis / 60000f) * 360f; // RPM * time in minutes * 360 degrees per rotation
                toRotate = toRotate % 360f;
            }
            
            matrix.translate(0.5f, 0.5f, 0.5f);
            matrix.rotate((float) Math.toRadians(toRotate), 0, 1, 0);
            matrix.translate(-0.5f, -0.5f, -0.5f);
        });
        
        Vector3i worldPos = new Vector3i(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ());
        for (Vector4i vector4i : rotorConfiguration) {
            worldPos.add(rotationAxis);
            
            tempMatrix.identity();
            instances.add(drawBatch.createInstance(worldPos, shaftMesh, dynamicMatrix, tempMatrix, null));
            
            int i = 0;
            for (Direction direction : Direction.values()) {
                switch (direction) {
                    case UP, DOWN -> {
                        if (rotationAxis.y() != 0) {
                            continue;
                        }
                    }
                    case NORTH, SOUTH -> {
                        if (rotationAxis.z() != 0) {
                            continue;
                        }
                    }
                    case WEST, EAST -> {
                        if (rotationAxis.x() != 0) {
                            continue;
                        }
                    }
                }
                for (int j = 0; j < vector4i.get(i); j++) {
                    tempMatrix.identity();
                    tempMatrix.translate(0.5f, 0.5f, 0.5f);
                    tempMatrix.rotate((float) Math.toRadians(180 * (i & 1)), 0, 1, 0);
                    tempMatrix.rotate((float) Math.toRadians(blade180RotationMultiplier * 135 * (i & 2)), 0, 1, 0);
                    tempMatrix.translate(-0.5f, -0.5f, -0.5f);
                    tempMatrix.translate(0, 0, -(j + 1));
                    tempMatrix.translate(0.5f, 0.5f, 0.5f);
                    tempMatrix.rotate((float) Math.toRadians(180), 0, 0, 1);
                    tempMatrix.translate(-0.5f, -0.5f, -0.5f);
                    
                    var intPos = new Vector3i();
                    if (APRIL_FOOLS_JOKE) {
                        var pos = new Vector4f(0.5f, 0.5f, -(j + 1) + 0.5f, 1).mul(tempMatrix).sub(0.5f, 0.5f, 0.5f, 0);
                        intPos.add((int) pos.x, (int) pos.y, (int) pos.z);
                        if (rotationAxis.x() != 0) {
                            intPos.x ^= intPos.y;
                            intPos.y ^= intPos.x;
                            intPos.x ^= intPos.y;
                        } else if (rotationAxis.z() != 0) {
                            intPos.z ^= intPos.y;
                            intPos.y ^= intPos.z;
                            intPos.z ^= intPos.y;
                        } else if (rotationAxis.y() != 1) {
                            // no change needed
                        }
                    }
                    intPos.add(worldPos);
                    
                    instances.add(drawBatch.createInstance(intPos, bladeMesh, dynamicMatrix, tempMatrix, null));
                }
                i++;
            }
        }
    }
    
    private void teardownQuartzModel() {
        if (level == null || !level.isClientSide) {
            return;
        }
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < instances.size(); i++) {
            instances.get(i).delete();
        }
        instances.clear();
    }
}
