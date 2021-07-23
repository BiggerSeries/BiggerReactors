package net.roguelogix.biggerreactors.multiblocks.heatexchanger.blocks;


import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.roguelogix.biggerreactors.multiblocks.heatexchanger.tiles.HeatExchangerCoolantPortTile;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

import static net.roguelogix.phosphophyllite.util.BlockStates.PORT_DIRECTION;

@RegisterBlock(name = "heat_exchanger_coolant_port", tileEntityClass = HeatExchangerCoolantPortTile.class)
public class HeatExchangerCoolantPortBlock extends HeatExchangerBaseBlock {
    
    public static final BooleanProperty CONDENSER = BooleanProperty.create("condenser");
    
    
    public HeatExchangerCoolantPortBlock(){
        super();
        registerDefaultState(defaultBlockState().setValue(PORT_DIRECTION, false));
        registerDefaultState(defaultBlockState().setValue(CONDENSER, false));
    }
    
    @RegisterBlock.Instance
    public static HeatExchangerCoolantPortBlock INSTANCE;
    
    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PORT_DIRECTION);
        builder.add(CONDENSER);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HeatExchangerCoolantPortTile(pos, state);
    }
    
    @Nonnull
    @Override
    public InteractionResult use(@Nonnull BlockState state, Level worldIn, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand handIn, @Nonnull BlockHitResult hit) {
        if (handIn == InteractionHand.MAIN_HAND) {
            Set<ResourceLocation> tags = player.getMainHandItem().getItem().getTags();
            if (tags.contains(new ResourceLocation("forge:tools/wrench")) || tags.contains(new ResourceLocation("forge:wrenches"))) {
                boolean direction = !state.getValue(PORT_DIRECTION);
                state = state.setValue(PORT_DIRECTION, direction);
                worldIn.setBlock(pos, state, 3);
                if (!worldIn.isClientSide()) {
                    BlockEntity te = worldIn.getBlockEntity(pos);
                    if (te instanceof HeatExchangerCoolantPortTile) {
                        ((HeatExchangerCoolantPortTile) te).setInletOtherOutlet(direction);
                    }
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.use(state, worldIn, pos, player, handIn, hit);
    }
    
    @Override
    public void neighborChanged(@Nonnull BlockState state, @Nonnull Level worldIn, @Nonnull BlockPos pos, @Nonnull Block blockIn, @Nonnull BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, worldIn, pos, blockIn, fromPos, isMoving);
        BlockEntity te = worldIn.getBlockEntity(pos);
        if (te instanceof HeatExchangerCoolantPortTile) {
            ((HeatExchangerCoolantPortTile) te).neighborChanged();
        }
    }
    
    @Override
    public boolean usesFaceDirection() {
        return true;
    }
}
