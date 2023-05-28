package net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.blocks.HeatExchangerCondenserChannelBlock;
import net.roguelogix.phosphophyllite.registry.RegisterTile;
import net.roguelogix.phosphophyllite.util.VectorUtil;
import net.roguelogix.quartz.DrawBatch;
import net.roguelogix.quartz.Mesh;
import net.roguelogix.quartz.Quartz;
import net.roguelogix.quartz.QuartzEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import javax.annotation.ParametersAreNonnullByDefault;

import static net.roguelogix.phosphophyllite.modular.block.IConnectedTexture.Module.*;
import static net.roguelogix.phosphophyllite.modular.block.IConnectedTexture.Module.WEST_CONNECTED_PROPERTY;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class HeatExchangerChannelTile extends HeatExchangerBaseTile {
    
    public final boolean CONDENSER;
    
    public long lastCheckedTick;
    
    @RegisterTile("heat_exchanger_channel")
    public static final BlockEntityType.BlockEntitySupplier<HeatExchangerChannelTile> SUPPLIER = new RegisterTile.Producer<>(HeatExchangerChannelTile::new);
    
    public HeatExchangerChannelTile(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
        CONDENSER = state.getBlock() == HeatExchangerCondenserChannelBlock.INSTANCE;
    }
    
    
    static {
        Quartz.EVENT_BUS.addListener(HeatExchangerChannelTile::onQuartzStartup);
    }
    
    private static Mesh condenserConnectionErrorMesh;
    private static Mesh condenserStraightMesh;
    private static Mesh condenserCornerMesh;
    private static Mesh evaporatorConnectionErrorMesh;
    private static Mesh evaporatorStraightMesh;
    private static Mesh evaporatorCornerMesh;
    
    private static void onQuartzStartup(QuartzEvent.Startup quartzStartup) {
        condenserConnectionErrorMesh = Quartz.createStaticMesh(new ResourceLocation("biggerreactors:block/heat_exchanger/casing/corner"));
        condenserStraightMesh = Quartz.createStaticMesh(new ResourceLocation("biggerreactors:block/heat_exchanger/hot_channel/connected_tb"));
        condenserCornerMesh = Quartz.createStaticMesh(new ResourceLocation("biggerreactors:block/heat_exchanger/hot_channel/connected_bn"));
        
        evaporatorConnectionErrorMesh = Quartz.createStaticMesh(new ResourceLocation("biggerreactors:block/heat_exchanger/casing/corner"));
        evaporatorStraightMesh = Quartz.createStaticMesh(new ResourceLocation("biggerreactors:block/heat_exchanger/cold_channel/connected_tb"));
        evaporatorCornerMesh = Quartz.createStaticMesh(new ResourceLocation("biggerreactors:block/heat_exchanger/cold_channel/connected_bn"));
    }
    
    private static final ReferenceArrayList<Matrix4f> rotationMatrices = new ReferenceArrayList<>();
    
    private static final int TOP_CONNECTED_BIT = 1;
    private static final int BOTTOM_CONNECTED_BIT = 2;
    private static final int NORTH_CONNECTED_BIT = 4;
    private static final int SOUTH_CONNECTED_BIT = 8;
    private static final int EAST_CONNECTED_BIT = 16;
    private static final int WEST_CONNECTED_BIT = 32;
    
    
    static {
        rotationMatrices.clear();
        for (int i = 0; i < 64; i++) {
            rotationMatrices.add(new Matrix4f());
        }
        final var preTranslate = new Vector3f(0.5f);
        final var postTranslate = new Vector3f(-0.5f);
        rotationMatrices.get(NORTH_CONNECTED_BIT | SOUTH_CONNECTED_BIT).translate(preTranslate).rotate((float) Math.toRadians(90), 1, 0, 0).translate(postTranslate);
        rotationMatrices.get(NORTH_CONNECTED_BIT).set(rotationMatrices.get(NORTH_CONNECTED_BIT | SOUTH_CONNECTED_BIT));
        rotationMatrices.get(SOUTH_CONNECTED_BIT).set(rotationMatrices.get(NORTH_CONNECTED_BIT | SOUTH_CONNECTED_BIT));
        rotationMatrices.get(EAST_CONNECTED_BIT | WEST_CONNECTED_BIT).translate(preTranslate).rotate((float) Math.toRadians(90), 0, 0, 1).translate(postTranslate);
        rotationMatrices.get(EAST_CONNECTED_BIT).set(rotationMatrices.get(EAST_CONNECTED_BIT | WEST_CONNECTED_BIT));
        rotationMatrices.get(WEST_CONNECTED_BIT).set(rotationMatrices.get(EAST_CONNECTED_BIT | WEST_CONNECTED_BIT));
        
        rotationMatrices.get(BOTTOM_CONNECTED_BIT | NORTH_CONNECTED_BIT).identity();
        rotationMatrices.get(BOTTOM_CONNECTED_BIT | WEST_CONNECTED_BIT).translate(preTranslate).rotate((float) Math.toRadians(90), 0, 1, 0).translate(postTranslate);
        rotationMatrices.get(BOTTOM_CONNECTED_BIT | SOUTH_CONNECTED_BIT).translate(preTranslate).rotate((float) Math.toRadians(180), 0, 1, 0).translate(postTranslate);
        rotationMatrices.get(BOTTOM_CONNECTED_BIT | EAST_CONNECTED_BIT).translate(preTranslate).rotate((float) Math.toRadians(-90), 0, 1, 0).translate(postTranslate);
        
        rotationMatrices.get(TOP_CONNECTED_BIT | NORTH_CONNECTED_BIT).translate(preTranslate).rotate((float) Math.toRadians(90), 1, 0, 0).translate(postTranslate);
        rotationMatrices.get(TOP_CONNECTED_BIT | WEST_CONNECTED_BIT).translate(preTranslate).rotate((float) Math.toRadians(180), 1, 0, 0).rotate((float) Math.toRadians(90), 0, 1, 0).translate(postTranslate);
        rotationMatrices.get(TOP_CONNECTED_BIT | SOUTH_CONNECTED_BIT).translate(preTranslate).rotate((float) Math.toRadians(180), 1, 0, 0).translate(postTranslate);
        rotationMatrices.get(TOP_CONNECTED_BIT | EAST_CONNECTED_BIT).translate(preTranslate).rotate((float) Math.toRadians(180), 1, 0, 0).rotate((float) Math.toRadians(-90), 0, 1, 0).translate(postTranslate);
        
        rotationMatrices.get(SOUTH_CONNECTED_BIT | WEST_CONNECTED_BIT).translate(preTranslate).rotate((float) Math.toRadians(-90), 1, 0, 0).rotate((float) Math.toRadians(90), 0, 1, 0).translate(postTranslate);
        rotationMatrices.get(NORTH_CONNECTED_BIT | EAST_CONNECTED_BIT).translate(preTranslate).rotate((float) Math.toRadians(90), 1, 0, 0).rotate((float) Math.toRadians(-90), 0, 1, 0).translate(postTranslate);
        
        rotationMatrices.get(SOUTH_CONNECTED_BIT | EAST_CONNECTED_BIT).translate(preTranslate).rotate((float) Math.toRadians(-90), 1, 0, 0).rotate((float) Math.toRadians(-90), 0, 1, 0).translate(postTranslate);
        rotationMatrices.get(NORTH_CONNECTED_BIT | WEST_CONNECTED_BIT).translate(preTranslate).rotate((float) Math.toRadians(90), 1, 0, 0).rotate((float) Math.toRadians(90), 0, 1, 0).translate(postTranslate);
    }
    
    private DrawBatch.Instance instance;
    
    @Override
    public void onAdded() {
        assert level != null;
        if (!level.isClientSide()) {
            return;
        }
        createModel();
    }
    
    @Override
    public void onRemoved(boolean chunkUnload) {
        assert level != null;
        if (!level.isClientSide()) {
            return;
        }
        destroyModel();
    }
    
    @Override
    public void setBlockState(BlockState p_155251_) {
        super.setBlockState(p_155251_);
        rebuildModel();
    }
    
    public void rebuildModel() {
        assert level != null;
        if (!level.isClientSide()) {
            return;
        }
        destroyModel();
        createModel();
    }
    
    public void createModel() {
        final var position = VectorUtil.fromBlockPos(getBlockPos());
        final var batcher = Quartz.getDrawBatcherForBlock(position);
        
        var mesh = CONDENSER ? condenserConnectionErrorMesh : evaporatorConnectionErrorMesh;
        BlockState state = getBlockState();
        int connectedSides = 0;
        connectedSides += state.getValue(TOP_CONNECTED_PROPERTY) ? 1 : 0;
        connectedSides += state.getValue(BOTTOM_CONNECTED_PROPERTY) ? 1 : 0;
        connectedSides += state.getValue(NORTH_CONNECTED_PROPERTY) ? 1 : 0;
        connectedSides += state.getValue(SOUTH_CONNECTED_PROPERTY) ? 1 : 0;
        connectedSides += state.getValue(EAST_CONNECTED_PROPERTY) ? 1 : 0;
        connectedSides += state.getValue(WEST_CONNECTED_PROPERTY) ? 1 : 0;
        
        Matrix4f rotationMatrix = null;
        if (connectedSides <= 2) {
            int sideConnectionFlags = 0;
            sideConnectionFlags |= state.getValue(TOP_CONNECTED_PROPERTY) ? TOP_CONNECTED_BIT : 0;
            sideConnectionFlags |= state.getValue(BOTTOM_CONNECTED_PROPERTY) ? BOTTOM_CONNECTED_BIT : 0;
            sideConnectionFlags |= state.getValue(NORTH_CONNECTED_PROPERTY) ? NORTH_CONNECTED_BIT : 0;
            sideConnectionFlags |= state.getValue(SOUTH_CONNECTED_PROPERTY) ? SOUTH_CONNECTED_BIT : 0;
            sideConnectionFlags |= state.getValue(EAST_CONNECTED_PROPERTY) ? EAST_CONNECTED_BIT : 0;
            sideConnectionFlags |= state.getValue(WEST_CONNECTED_PROPERTY) ? WEST_CONNECTED_BIT : 0;
            rotationMatrix = rotationMatrices.get(sideConnectionFlags);
            if (connectedSides <= 1 || sideConnectionFlags == (TOP_CONNECTED_BIT | BOTTOM_CONNECTED_BIT) || sideConnectionFlags == (NORTH_CONNECTED_BIT | SOUTH_CONNECTED_BIT) || sideConnectionFlags == (EAST_CONNECTED_BIT | WEST_CONNECTED_BIT)) {
                mesh = CONDENSER ? condenserStraightMesh : evaporatorStraightMesh;
            } else {
                mesh = CONDENSER ? condenserCornerMesh : evaporatorCornerMesh;
            }
        }
        
        if (mesh == null) {
            return;
        }
        
        instance = batcher.createInstance(position, mesh, null, rotationMatrix, null);
    }
    
    public void destroyModel() {
        if (instance != null) {
            instance.delete();
            instance = null;
        }
    }
}
