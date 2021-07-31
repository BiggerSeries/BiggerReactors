package net.roguelogix.biggerreactors.multiblocks.turbine.tiles;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.biggerreactors.multiblocks.turbine.blocks.TurbineRotorBlade;
import net.roguelogix.biggerreactors.multiblocks.turbine.blocks.TurbineRotorShaft;
import net.roguelogix.biggerreactors.multiblocks.turbine.state.TurbineShaftRotationState;
import net.roguelogix.phosphophyllite.multiblock.IAssemblyAttemptedTile;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;

import javax.annotation.ParametersAreNonnullByDefault;

import static net.roguelogix.biggerreactors.multiblocks.turbine.blocks.TurbineRotorBlade.BLADE_POSITION;
import static net.roguelogix.biggerreactors.multiblocks.turbine.state.TurbineShaftRotationState.*;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@RegisterTileEntity(name = "turbine_rotor_shaft")
public class TurbineRotorShaftTile extends TurbineBaseTile implements IAssemblyAttemptedTile {
    
    @RegisterTileEntity.Type
    public static BlockEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final BlockEntityType.BlockEntitySupplier<TurbineRotorShaftTile> SUPPLIER = TurbineRotorShaftTile::new;
    
    public TurbineRotorShaftTile(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
    
    @Override
    public void onAssemblyAttempted() {
        TurbineShaftRotationState newRotation = getBlockState().getValue(TurbineShaftRotationState.TURBINE_SHAFT_ROTATION_STATE_ENUM_PROPERTY);
        for (Direction value : Direction.values()) {
            BlockPos neighbor = getBlockPos().relative(value);
            assert level != null;
            BlockState state = level.getBlockState(neighbor);
            if (state.getBlock() != TurbineRotorShaft.INSTANCE) {
                continue;
            }
            if (neighbor.getX() != worldPosition.getX() && neighbor.getY() == worldPosition.getY() && neighbor.getZ() == worldPosition.getZ()) {
                newRotation = X;
            } else if (neighbor.getX() == worldPosition.getX() && neighbor.getY() != worldPosition.getY() && neighbor.getZ() == worldPosition.getZ()) {
                newRotation = Y;
            } else if (neighbor.getX() == worldPosition.getX() && neighbor.getY() == worldPosition.getY() && neighbor.getZ() != worldPosition.getZ()) {
                newRotation = Z;
            }
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(TURBINE_SHAFT_ROTATION_STATE_ENUM_PROPERTY, newRotation));
        }
        
        // propagate it out to the blades
        int i = 0;
        for (Direction value : Direction.values()) {
            if (value.getAxis().ordinal() == newRotation.ordinal()) {
                continue;
            }
            
            BlockPos pos = getBlockPos();
            while (true) {
                pos = pos.relative(value);
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() != TurbineRotorBlade.INSTANCE) {
                    break;
                }
                level.setBlockAndUpdate(pos, state.setValue(TURBINE_SHAFT_ROTATION_STATE_ENUM_PROPERTY, newRotation).setValue(BLADE_POSITION, i));
            }
            i++;
        }
    }
}
