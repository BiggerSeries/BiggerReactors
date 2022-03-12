package net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.phosphophyllite.registry.RegisterTile;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class HeatExchangerCasingTile extends HeatExchangerBaseTile {
    
    @RegisterTile("heat_exchanger_casing")
    public static final BlockEntityType.BlockEntitySupplier<HeatExchangerCasingTile> SUPPLIER = new RegisterTile.Producer<>(HeatExchangerCasingTile::new);
    
    public HeatExchangerCasingTile(BlockEntityType<HeatExchangerCasingTile> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
}
