package net.roguelogix.biggerreactors.multiblocks.reactor2.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.phosphophyllite.registry.RegisterTile;
import net.roguelogix.phosphophyllite.util.NonnullDefault;

@NonnullDefault
public class ReactorControlRodTile extends ReactorBaseTile {
    
    @RegisterTile("reactor2_control_rod")
    public static final BlockEntityType.BlockEntitySupplier<ReactorControlRodTile> CONTROL_ROD_SUPPLIER = new RegisterTile.Producer<>(ReactorControlRodTile::new);
    
    public ReactorControlRodTile(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
    }
    
    private double insertion = 0;
    
    @Override
    protected void readNBT(CompoundTag compound) {
        insertion = compound.getDouble("insertion");
    }
    
    @Override
    protected CompoundTag writeNBT() {
        final var tag = new CompoundTag();
        tag.putDouble("insertion", insertion);
        return tag;
    }
}
