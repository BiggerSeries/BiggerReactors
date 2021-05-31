package net.roguelogix.biggerreactors.multiblocks.turbine.client;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.LightType;
import net.roguelogix.biggerreactors.BiggerReactors;
import net.roguelogix.biggerreactors.multiblocks.turbine.blocks.TurbineRotorBlade;
import net.roguelogix.biggerreactors.multiblocks.turbine.blocks.TurbineRotorShaft;
import net.roguelogix.biggerreactors.multiblocks.turbine.tiles.TurbineRotorBearingTile;
import net.roguelogix.phosphophyllite.gui.client.api.IRender;
import net.roguelogix.phosphophyllite.multiblock.generic.MultiblockBlock;
import net.roguelogix.phosphophyllite.quartz.QuartzMatrixStack;
import net.roguelogix.phosphophyllite.quartz.QuartzMesh;
import net.roguelogix.phosphophyllite.quartz.QuartzTESR;
import net.roguelogix.phosphophyllite.repack.org.joml.Matrix4f;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector4i;

import javax.annotation.ParametersAreNonnullByDefault;

import static net.roguelogix.phosphophyllite.repack.org.joml.Math.toRadians;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class QuartzBladeRenderer extends QuartzTESR.Renderer {
    
    private final TurbineRotorBearingTile bearing;
    private final Matrix4f rotationMatrix = new Matrix4f();
    
    public QuartzBladeRenderer(TurbineRotorBearingTile bearing) {
        this.bearing = bearing;
    }
    
    @Override
    public void build(QuartzMesh.Builder builder) {
        
        final QuartzMatrixStack matrixStack = builder.getMatrixStack();
    
        BlockState state = bearing.getBlockState();
        if (state.get(MultiblockBlock.ASSEMBLED)) {
            // it is notable that this is on the client, and as a result i do not have direct access to the multiblock controller
            // so, tile entity, do your thing and just magically be updated k thx
            
            // each turbine has two, one of them is chosen
            if (!bearing.isRenderBearing) {
                return;
            }
            
            // signal to not render
            // ie: no glass
            if (bearing.rotationAxis == null || bearing.rotorConfiguration == null || (bearing.rotationAxis.getX() == 0 && bearing.rotationAxis.getY() == 0 && bearing.rotationAxis.getZ() == 0)) {
                return;
            }
            
            matrixStack.push();
            matrixStack.translate(0.5, 0.5, 0.5);
            
            matrixStack.push(rotationMatrix); // dynamic matrix, calculated later, its the rotation angle
            int blade180RotationMultiplier = ((int) -bearing.rotationAxis.getX()) | ((int) -bearing.rotationAxis.getY()) | ((int) bearing.rotationAxis.getZ());
            
            matrixStack.push();
            matrixStack.translate(-0.5, -0.5, -0.5);
            
            for (Vector4i vector4i : bearing.rotorConfiguration) {
                matrixStack.translate(0, 1, 0);
                
                BlockPos shaftPos = bearing.getPos().add(bearing.rotationAxis.getX(), bearing.rotationAxis.getY(), bearing.rotationAxis.getZ());
                int skyLight = bearing.getWorld().getLightFor(LightType.SKY, shaftPos);
                int blockLight = bearing.getWorld().getLightFor(LightType.BLOCK, shaftPos);
                int combinedLight = (skyLight << 16) | blockLight;
                combinedLight *= 16;
                
                Minecraft.getInstance().getBlockRendererDispatcher().renderBlock(TurbineRotorShaft.INSTANCE.getDefaultState(), matrixStack, builder, combinedLight, 0xA0000, net.minecraftforge.client.model.data.EmptyModelData.INSTANCE);
                
                int i = 0;
                for (Direction direction : Direction.values()) {
                    switch (direction) {
                        case UP:
                        case DOWN: {
                            if (bearing.rotationAxis.getY() != 0) {
                                continue;
                            }
                            break;
                        }
                        case NORTH:
                        case SOUTH: {
                            if (bearing.rotationAxis.getZ() != 0) {
                                continue;
                            }
                            break;
                        }
                        case WEST:
                        case EAST: {
                            if (bearing.rotationAxis.getX() != 0) {
                                continue;
                            }
                            break;
                        }
                    }
                    for (int j = 0; j < vector4i.get(i); j++) {
                        matrixStack.push();
                        matrixStack.translate(0.5, 0.5, 0.5);
                        matrixStack.rotate(new Quaternion(Vector3f.YP, (float) (180 * (i & 1) + blade180RotationMultiplier * 135 * (i & 2)), true));
                        matrixStack.translate(-0.5, -0.5, -0.5);
                        matrixStack.translate(0, 0, -(j + 1));
                        matrixStack.translate(0.5, 0.5, 0.5);
                        matrixStack.rotate(new Quaternion(Vector3f.ZP, 180, true));
                        matrixStack.translate(-0.5, -0.5, -0.5);
                        Minecraft.getInstance().getBlockRendererDispatcher().renderBlock(TurbineRotorBlade.INSTANCE.getDefaultState(), matrixStack, builder, combinedLight, 0xA0000, net.minecraftforge.client.model.data.EmptyModelData.INSTANCE);
                        matrixStack.pop();
                    }
                    i++;
                }
            }
            
            matrixStack.pop();
            matrixStack.pop();
            matrixStack.pop();
        }
    }
    
    @Override
    public void updateMatrices(float partialTicks) {
        double angle = bearing.angle;
        long elapsedTimeNano = System.nanoTime() - BiggerReactors.lastRenderTime;
        
        double speed = bearing.speed / 10f;
        if (speed > 0.001f) {
            double elapsedTimeMilis = ((double) elapsedTimeNano) / 1_000_000;
            angle += speed * ((float) elapsedTimeMilis / 60000f) * 360f; // RPM * time in minutes * 360 degrees per rotation
            angle = angle % 360f;
            bearing.angle = angle;
        }
        
        int blade180RotationMultiplier = ((int) -bearing.rotationAxis.getX()) | ((int) -bearing.rotationAxis.getY()) | ((int) bearing.rotationAxis.getZ());
        if (blade180RotationMultiplier > 0) {
            angle += 180;
        }
        
        if (bearing.rotationAxis.getX() != 0) {
            rotationMatrix.rotate(toRadians(-90) * bearing.rotationAxis.getX(), 0, 0, 1);
            angle -= 90;
        } else if (bearing.rotationAxis.getZ() != 0) {
            rotationMatrix.rotate(toRadians(90) * bearing.rotationAxis.getZ(), 1, 0, 0);
        } else if (bearing.rotationAxis.getY() != 1) {
            rotationMatrix.rotate(toRadians(180), 1, 0, 0);
        }
        rotationMatrix.rotate((float) toRadians(angle), 0, 1, 0);
    }
}
