package net.roguelogix.biggerreactors.multiblocks.turbine.tiles;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.roguelogix.biggerreactors.multiblocks.turbine.blocks.TurbineRotorBlade;
import net.roguelogix.biggerreactors.multiblocks.turbine.blocks.TurbineRotorShaft;
import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.multiblock.IAssemblyAttemptedTile;
import net.roguelogix.phosphophyllite.quartz.*;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;
import net.roguelogix.phosphophyllite.repack.org.joml.*;
import net.roguelogix.phosphophyllite.threading.Queues;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.Math;
import java.util.ArrayList;

import static net.roguelogix.phosphophyllite.multiblock.IAssemblyStateBlock.ASSEMBLED;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@RegisterTileEntity(name = "turbine_rotor_bearing")
public class TurbineRotorBearingTile extends TurbineBaseTile implements IAssemblyAttemptedTile {
    
    public static boolean USE_QUARTZ = true;
    
    @RegisterTileEntity.Type
    public static BlockEntityType<TurbineRotorBearingTile> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final BlockEntityType.BlockEntitySupplier<TurbineRotorBearingTile> SUPPLIER = TurbineRotorBearingTile::new;
    
    public TurbineRotorBearingTile(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
    
    public double angle = 0;
    
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
            nbt.putInt("minx", controller().minCoord().x());
            nbt.putInt("miny", controller().minCoord().y());
            nbt.putInt("minz", controller().minCoord().z());
            nbt.putInt("maxx", controller().maxCoord().x());
            nbt.putInt("maxy", controller().maxCoord().y());
            nbt.putInt("maxz", controller().maxCoord().z());
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
    public void onAssemblyAttempted() {
        sendFullUpdate = Phosphophyllite.tickNumber() + 10;
    }
    
    @OnlyIn(Dist.CLIENT)
    @Override
    public AABB getRenderBoundingBox() {
        if (AABB == null) {
            return INFINITE_EXTENT_AABB;
        }
        return AABB;
    }
    
    static {
        Quartz.EVENT_BUS.addListener(TurbineRotorBearingTile::onQuartzStartup);
    }
    
    private static StaticMesh shaftMesh;
    private static StaticMesh bladeMesh;
    
    private static void onQuartzStartup(QuartzEvent.Startup quartzStartup) {
        shaftMesh = Quartz.createStaticMesh(TurbineRotorShaft.INSTANCE.defaultBlockState());
        bladeMesh = Quartz.createStaticMesh(TurbineRotorBlade.INSTANCE.defaultBlockState());
    }
    
    private final ObjectArrayList<DrawBatch.Instance> instances = new ObjectArrayList();
    DynamicMatrix matrix;
    DrawBatch drawBatch;
    
    private void setupQuartzModel() {
        if (level == null || !level.isClientSide) {
            return;
        }
        teardownQuartzModel();
        if (!USE_QUARTZ) {
            return;
        }
        if (level.getBlockEntity(getBlockPos()) != this) {
            return;
        }
        final Matrix4f jomlMatrix = new Matrix4f();
        final int blade180RotationMultiplier = -rotationAxis.x() | -rotationAxis.y() | rotationAxis.z();
        drawBatch = Quartz.getDrawBatcherForAABB(new AABBi((int) AABB.minX, (int) AABB.minY, (int) AABB.minZ, (int) AABB.maxX, (int) AABB.maxY, (int) AABB.maxZ));
        matrix = drawBatch.createDynamicMatrix((matrix, nanoSinceLastFrame, partialTicks, playerBlock, playerPartialBlock) -> {
            double angle = this.angle;
            
            double speed = this.speed / 10f;
            if (speed > 0.001f) {
                double elapsedTimeMilis = ((double) nanoSinceLastFrame) / 1_000_000;
                angle += speed * ((float) elapsedTimeMilis / 60000f) * 360f; // RPM * time in minutes * 360 degrees per rotation
                angle = angle % 360f;
                this.angle = angle;
            }
            
            if (blade180RotationMultiplier > 0) {
                angle += 180;
            }
            if(rotationAxis.x() != 0){
                angle += 180;
            }
            
            jomlMatrix.identity();
            jomlMatrix.translate(0.5f, 0.5f, 0.5f);
            if (rotationAxis.x() != 0) {
                jomlMatrix.rotate((float) Math.toRadians(-90.0f * rotationAxis.x()), 0, 0, 1);
                angle -= 90;
            } else if (rotationAxis.z() != 0) {
                jomlMatrix.rotate((float) Math.toRadians(90.0f * rotationAxis.z()), 1, 0, 0);
            } else if (rotationAxis.y() != 1) {
                jomlMatrix.rotate((float) Math.toRadians(180), 1, 0, 0);
            }
            jomlMatrix.rotate((float) Math.toRadians(-angle), 0, 1, 0);
            jomlMatrix.translate(-0.5f, -0.5f, -0.5f);
            matrix.write(jomlMatrix);
        });
        Vector3i worldPos = new Vector3i(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ());
        for (Vector4i vector4i : rotorConfiguration) {
            worldPos.add(rotationAxis);
            var light = drawBatch.createLight(worldPos, DynamicLight.Type.INTERNAL);
            
            jomlMatrix.identity();
            instances.add(drawBatch.createInstance(worldPos, shaftMesh, matrix, jomlMatrix, light, null));
            
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
                    jomlMatrix.identity();
                    jomlMatrix.translate(0.5f, 0.5f, 0.5f);
                    jomlMatrix.rotate((float) Math.toRadians(180 * (i & 1)), 0, 1, 0);
                    jomlMatrix.rotate((float) Math.toRadians(blade180RotationMultiplier * 135 * (i & 2)), 0, 1, 0);
                    jomlMatrix.translate(-0.5f, -0.5f, -0.5f);
                    jomlMatrix.translate(0, 0, -(j + 1));
                    jomlMatrix.translate(0.5f, 0.5f, 0.5f);
                    jomlMatrix.rotate((float) Math.toRadians(180), 0, 0, 1);
                    jomlMatrix.translate(-0.5f, -0.5f, -0.5f);
                    
                    instances.add(drawBatch.createInstance(worldPos, bladeMesh, matrix, jomlMatrix, light, null));
                }
                i++;
            }
        }
    }
    
    private void teardownQuartzModel() {
        if (level == null || !level.isClientSide) {
            return;
        }
        if (drawBatch == null) {
            return;
        }
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < instances.size(); i++) {
            instances.get(i).delete();
        }
        instances.clear();
    }
}
