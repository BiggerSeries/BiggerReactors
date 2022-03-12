package net.roguelogix.biggerreactors.multiblocks.turbine.tiles;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.phosphophyllite.registry.RegisterTile;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TurbineRotorBladeTile extends TurbineBaseTile {
    
    @RegisterTile("turbine_rotor_blade")
    public static final BlockEntityType.BlockEntitySupplier<TurbineRotorBladeTile> SUPPLIER = new RegisterTile.Producer<>(TurbineRotorBladeTile::new);
    
    public TurbineRotorBladeTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
}
