package net.roguelogix.biggerreactors.multiblocks.turbine.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;
import net.roguelogix.phosphophyllite.registry.TileSupplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@RegisterTileEntity(name = "turbine_computer_port")
public class TurbineComputerPortTile extends TurbineBaseTile {
    
    @RegisterTileEntity.Type
    public static BlockEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final TileSupplier SUPPLIER = TurbineComputerPortTile::new;
    
    public TurbineComputerPortTile(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
    
//    @CapabilityInject(IPeripheral.class)
//    public static Capability<IPeripheral> CAPABILITY_PERIPHERAL = null;

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, final @Nullable Direction side) {
//        if (cap == CAPABILITY_PERIPHERAL) {
//            return TurbinePeripheral.create(() -> controller).cast();
//        }
        return LazyOptional.empty();
    }
    
}
