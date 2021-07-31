package net.roguelogix.biggerreactors.multiblocks.turbine.tiles;

import com.mojang.math.Vector3f;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.multiblock.IAssemblyAttemptedTile;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector4i;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@RegisterTileEntity(name = "turbine_rotor_bearing")
public class TurbineRotorBearingTile extends TurbineBaseTile implements IAssemblyAttemptedTile {
    
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
    public Vector3f rotationAxis = null;
    public ArrayList<Vector4i> rotorConfiguration = null;
    public AABB AABB = null;
    private long sendFullUpdate = Long.MAX_VALUE;
    
    @Nullable
    @Override
    public CompoundTag getUpdateNBT() {
        CompoundTag nbt = new CompoundTag();
        if (nullableController() != null) {
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
        if (nbt.contains("speed")) {
            speed = nbt.getDouble("speed");
            if (nbt.contains("config")) {
                handleUpdateTag(nbt.getCompound("config"));
            }
        }
    }
    
    @Override
    public void handleDataNBT(CompoundTag nbt) {
        if (nbt.contains("rotx")) {
            isRenderBearing = true;
            if (rotationAxis == null) {
                rotationAxis = new Vector3f();
            }
            rotationAxis.set(nbt.getFloat("rotx"), nbt.getFloat("roty"), nbt.getFloat("rotz"));
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
        } else {
            isRenderBearing = false;
        }
    }
    
    @Override
    public CompoundTag getDataNBT() {
        CompoundTag nbt = super.getDataNBT();
        if (isRenderBearing && nullableController() != null) {
            nbt.putFloat("rotx", controller().rotationAxis.getX());
            nbt.putFloat("roty", controller().rotationAxis.getY());
            nbt.putFloat("rotz", controller().rotationAxis.getZ());
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
}
