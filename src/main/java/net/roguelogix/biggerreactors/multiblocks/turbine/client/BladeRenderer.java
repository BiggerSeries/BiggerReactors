package net.roguelogix.biggerreactors.multiblocks.turbine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.roguelogix.biggerreactors.BiggerReactors;
import net.roguelogix.biggerreactors.multiblocks.turbine.blocks.TurbineRotorBlade;
import net.roguelogix.biggerreactors.multiblocks.turbine.blocks.TurbineRotorShaft;
import net.roguelogix.biggerreactors.multiblocks.turbine.tiles.TurbineRotorBearingTile;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector4i;

import javax.annotation.ParametersAreNonnullByDefault;

import static net.roguelogix.biggerreactors.multiblocks.turbine.tiles.TurbineRotorBearingTile.USE_QUARTZ;
import static net.roguelogix.phosphophyllite.multiblock.IAssemblyStateBlock.ASSEMBLED;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BladeRenderer implements BlockEntityRenderer<TurbineRotorBearingTile> {
    
    public BladeRenderer(BlockEntityRendererProvider.Context rendererDispatcherIn) {
    }
    
    @Override
    public void render(TurbineRotorBearingTile bearing, float partialTicks, PoseStack matrixStackIn, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        if(USE_QUARTZ){
            return;
        }
        // fuck using MC's render engine, its a slow pile of garbage
        // if they cant do it properly, ill just have to do it myself
        // later, im lazy right now, still, fuck this shit
        BlockState state = bearing.getBlockState();
        if (state.getValue(ASSEMBLED)) {
            // it is notable that this is on the client, and as a result i do not have direct access to the multiblock controller
            // so, tile entity, do your thing and just magically be updated k thx
            
            // each turbine has two, one of them is chosen
            if (!bearing.isRenderBearing) {
                return;
            }
            
            // signal to not render
            // ie: no glass
            if (bearing.rotationAxis == null || bearing.rotorConfiguration == null || (bearing.rotationAxis.x() == 0 && bearing.rotationAxis.y() == 0 && bearing.rotationAxis.z() == 0)) {
                return;
            }
            
            double angle = bearing.angle;
            long elapsedTimeNano = System.nanoTime() - BiggerReactors.lastRenderTime;
            
            double speed = bearing.speed / 10f;
            if (speed > 0.001f) {
                double elapsedTimeMilis = ((double) elapsedTimeNano) / 1_000_000;
                angle += speed * ((float) elapsedTimeMilis / 60000f) * 360f; // RPM * time in minutes * 360 degrees per rotation
                angle = angle % 360f;
                bearing.angle = angle;
            }
            
            int blade180RotationMultiplier = -bearing.rotationAxis.x() | -bearing.rotationAxis.y() | bearing.rotationAxis.z();
            if (blade180RotationMultiplier > 0) {
                angle += 180;
            }
            //            blade180RotationMultiplier *= -1;
            
            matrixStackIn.pushPose();
            
            matrixStackIn.translate(0.5, 0.5, 0.5);
            
            Quaternion axisRotation = null;
            
            if (bearing.rotationAxis.x() != 0) {
                axisRotation = new Quaternion(Vector3f.ZP, -90 * bearing.rotationAxis.x(), true);
                angle -= 90;
            } else if (bearing.rotationAxis.z() != 0) {
                axisRotation = new Quaternion(Vector3f.XP, 90 * bearing.rotationAxis.z(), true);
            } else if (bearing.rotationAxis.y() != 1) {
                axisRotation = new Quaternion(Vector3f.XP, 180, true);
            }
            if (axisRotation != null) {
                matrixStackIn.mulPose(axisRotation);
            }
            matrixStackIn.mulPose(new Quaternion(Vector3f.YP, (float) angle, true));
            
            matrixStackIn.translate(-0.5, -0.5, -0.5);
            
            int bearingNum = 0;
            for (Vector4i vector4i : bearing.rotorConfiguration) {
                matrixStackIn.translate(0, 1, 0);
                
                BlockPos shaftPos = bearing.getBlockPos().offset(bearing.rotationAxis.x(), bearing.rotationAxis.y(), bearing.rotationAxis.z());
                int skyLight = bearing.getLevel().getBrightness(LightLayer.SKY, shaftPos);
                int blockLight = bearing.getLevel().getBrightness(LightLayer.BLOCK, shaftPos);
                int combinedLight = (skyLight << 16) | blockLight;
                combinedLight *= 16;
                bearingNum++;
                
                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(TurbineRotorShaft.INSTANCE.defaultBlockState(), matrixStackIn, bufferIn, combinedLight, 0xA0000, ModelData.EMPTY, null);
                
                int i = 0;
                for (Direction direction : Direction.values()) {
                    switch (direction) {
                        case UP:
                        case DOWN: {
                            if (bearing.rotationAxis.y() != 0) {
                                continue;
                            }
                            break;
                        }
                        case NORTH:
                        case SOUTH: {
                            if (bearing.rotationAxis.z() != 0) {
                                continue;
                            }
                            break;
                        }
                        case WEST:
                        case EAST: {
                            if (bearing.rotationAxis.x() != 0) {
                                continue;
                            }
                            break;
                        }
                    }
                    for (int j = 0; j < vector4i.get(i); j++) {
                        matrixStackIn.pushPose();
                        matrixStackIn.translate(0.5, 0.5, 0.5);
                        matrixStackIn.mulPose(new Quaternion(Vector3f.YP, (float) (180 * (i & 1) + blade180RotationMultiplier * 135 * (i & 2)), true));
                        matrixStackIn.translate(-0.5, -0.5, -0.5);
                        matrixStackIn.translate(0, 0, -(j + 1));
                        matrixStackIn.translate(0.5, 0.5, 0.5);
                        matrixStackIn.mulPose(new Quaternion(Vector3f.ZP, 180, true));
                        matrixStackIn.translate(-0.5, -0.5, -0.5);
                        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(TurbineRotorBlade.INSTANCE.defaultBlockState(), matrixStackIn, bufferIn, combinedLight, 0xA0000, ModelData.EMPTY, null);
                        matrixStackIn.popPose();
                    }
                    i++;
                }
            }
            
            
            matrixStackIn.popPose();
        }
    }
}
