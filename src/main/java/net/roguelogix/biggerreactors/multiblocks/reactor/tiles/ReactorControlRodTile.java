package net.roguelogix.biggerreactors.multiblocks.reactor.tiles;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fmllegacy.network.NetworkHooks;
import net.roguelogix.biggerreactors.multiblocks.reactor.blocks.ReactorControlRod;
import net.roguelogix.biggerreactors.multiblocks.reactor.containers.ReactorControlRodContainer;
import net.roguelogix.biggerreactors.multiblocks.reactor.state.ReactorControlRodState;
import net.roguelogix.phosphophyllite.gui.client.api.IHasUpdatableState;
import net.roguelogix.phosphophyllite.multiblock.generic.MultiblockBlock;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;
import net.roguelogix.phosphophyllite.registry.TileSupplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@RegisterTileEntity(name = "reactor_control_rod")
public class ReactorControlRodTile extends ReactorBaseTile implements MenuProvider, IHasUpdatableState<ReactorControlRodState> {
    
    @RegisterTileEntity.Type
    public static BlockEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final TileSupplier SUPPLIER = ReactorControlRodTile::new;
    
    public ReactorControlRodTile(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
    
    public final ReactorControlRodState reactorControlRodState = new ReactorControlRodState(this);
    
    @Override
    @Nonnull
    public ReactorControlRodState getState() {
        this.updateState();
        return this.reactorControlRodState;
    }
    
    @Override
    public void updateState() {
        reactorControlRodState.name = name;
        reactorControlRodState.insertionLevel = insertion;
    }
    
    @Override
    @Nonnull
    public InteractionResult onBlockActivated(@Nonnull Player player, @Nonnull InteractionHand handIn) {
        assert level != null;
        if (level.getBlockState(worldPosition).getValue(MultiblockBlock.ASSEMBLED)) {
            if (!level.isClientSide) {
                NetworkHooks.openGui((ServerPlayer) player, this, this.getBlockPos());
            }
            return InteractionResult.SUCCESS;
        }
        return super.onBlockActivated(player, handIn);
    }
    
    @Override
    @Nonnull
    public Component getDisplayName() {
        return new TranslatableComponent(ReactorControlRod.INSTANCE.getDescriptionId());
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, @Nonnull Inventory playerInventory, @Nonnull Player player) {
        return new ReactorControlRodContainer(windowId, this.worldPosition, player);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void runRequest(String requestName, Object requestData) {
        if (controller == null) {
            return;
        }
        
        // Change the insertion level of the rod.
        if (requestName.equals("changeInsertionLevel")) {
            Pair<Double, Boolean> dataPair = (Pair<Double, Boolean>) requestData;
            double newLevel = this.insertion + dataPair.getFirst();
            newLevel = Math.max(0, Math.min(100, newLevel));
            if (dataPair.getSecond()) {
                controller.setAllControlRodLevels(newLevel);
            } else {
                this.insertion = newLevel;
                controller.updateControlRodLevels();
            }
        }
        
        // Set the name for the control rod.
        if (requestName.equals("setName")) {
            this.setName((String) requestData);
        }
        
        super.runRequest(requestName, requestData);
    }
    
    private double insertion = 0;
    
    public void setInsertion(double newLevel) {
        if(Double.isNaN(newLevel)){
            return;
        }
        if (newLevel < 0) {
            newLevel = 0;
        }
        if (newLevel > 100) {
            newLevel = 100;
        }
        insertion = newLevel;
    }
    
    public double getInsertion() {
        return insertion;
    }
    
    // TODO: What should the default control rod name be? I think it should be Chris Houlihan...
    private String name = "";
    
    public void setName(@Nonnull String name) {
        this.name = name;
    }
    
    @Nonnull
    public String getName() {
        return name;
    }
    
    @Override
    @Nonnull
    protected CompoundTag writeNBT() {
        CompoundTag compound = super.writeNBT();
        compound.putDouble("insertion", insertion);
        compound.putString("name", name);
        return compound;
    }
    
    @Override
    protected void readNBT(@Nonnull CompoundTag compound) {
        super.readNBT(compound);
        if (compound.contains("insertion")) {
            insertion = compound.getDouble("insertion");
        }
        if (compound.contains("name")) {
            name = compound.getString("name");
        }
    }
}
