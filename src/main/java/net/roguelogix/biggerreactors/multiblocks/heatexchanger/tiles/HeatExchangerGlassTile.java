package net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.phosphophyllite.registry.RegisterTile;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class HeatExchangerGlassTile extends HeatExchangerBaseTile {
    
    @RegisterTile("heat_exchanger_glass")
    public static final BlockEntityType.BlockEntitySupplier<HeatExchangerGlassTile> SUPPLIER = new RegisterTile.Producer<>(HeatExchangerGlassTile::new);
    
    public HeatExchangerGlassTile(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
    }
}
