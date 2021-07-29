package net.roguelogix.biggerreactors.multiblocks.reactor.tiles;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@RegisterTileEntity(name = "reactor_fuel_rod")
public class ReactorFuelRodTile extends ReactorBaseTile {
    
    @RegisterTileEntity.Type
    public static BlockEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final BlockEntityType.BlockEntitySupplier<ReactorFuelRodTile> SUPPLIER = ReactorFuelRodTile::new;
    
    public ReactorFuelRodTile(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
    
    public long lastCheckedTick = 0;
    
    public long fuel = 0;
    public long waste = 0;
    
    @Override
    protected void readNBT(CompoundTag compound) {
        super.readNBT(compound);
        fuel = compound.getLong("fuel");
        waste = compound.getLong("waste");
    }
    
    @Override
    
    protected CompoundTag writeNBT() {
        CompoundTag compound = super.writeNBT();
        compound.putLong("fuel", fuel);
        compound.putLong("waste", waste);
        return compound;
    }
}
