package net.roguelogix.biggerreactors.multiblocks.reactor2.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.phosphophyllite.registry.RegisterTile;

public class ReactorFuelRodTile extends ReactorBaseTile {
    @RegisterTile("reactor2_fuel_rod")
    public static final BlockEntityType.BlockEntitySupplier<ReactorFuelRodTile> FUEL_ROD_SUPPLIER = new RegisterTile.Producer<>(ReactorFuelRodTile::new);
    
    public ReactorFuelRodTile(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
    }
    
    // TODO: this needs to handle multiple fuel types, at some point
    private long fuel = 0;
    private long waste = 0;
    
    @Override
    protected void readNBT(CompoundTag compound) {
        fuel = compound.getLong("fuel");
        waste = compound.getLong("waste");
    }
    
    @Override
    protected CompoundTag writeNBT() {
        final var tag = new CompoundTag();
        tag.putLong("fuel", fuel);
        tag.putLong("waste", waste);
        return tag;
    }
}
