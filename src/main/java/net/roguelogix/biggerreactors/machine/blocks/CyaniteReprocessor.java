package net.roguelogix.biggerreactors.machine.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.ToolAction;
import net.roguelogix.biggerreactors.machine.tiles.CyaniteReprocessorTile;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CyaniteReprocessor extends BaseEntityBlock implements EntityBlock{
    
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
    
    @RegisterBlock(name = "cyanite_reprocessor", tileEntityClass = CyaniteReprocessorTile.class)
    public static final CyaniteReprocessor INSTANCE = new CyaniteReprocessor();
    
    public CyaniteReprocessor() {
        super(Properties.of()
                .sound(SoundType.STONE)
                .destroyTime(1.0F)
                .explosionResistance(1.0F)
                .requiresCorrectToolForDrops());
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ENABLED, Boolean.FALSE));
    }
    
    public static Direction getFacingFromEntity(BlockPos clickedBlockPos, LivingEntity entity) {
        return Direction.getNearest((float) (entity.getX() - clickedBlockPos.getX()), 0.0F, (float) (entity.getZ() - clickedBlockPos.getZ()));
    }
    
    @Nonnull
    @Override
    public RenderShape getRenderShape(@Nonnull BlockState blockState) {
        return RenderShape.MODEL;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return CyaniteReprocessorTile.SUPPLIER.create(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (a, b, c, tile) -> {
            assert tile instanceof CyaniteReprocessorTile;
            ((CyaniteReprocessorTile) tile).tick();
        };
    }
    
    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING).add(ENABLED);
    }
    
    @Nonnull
    @Override
    public InteractionResult use(@Nonnull BlockState blockState, Level world, @Nonnull BlockPos blockPos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult trace) {
        BlockEntity tile = world.getBlockEntity(blockPos);
        if (tile instanceof CyaniteReprocessorTile) {
            return ((CyaniteReprocessorTile) tile).onBlockActivated(blockState, world, blockPos, player, hand, trace);
        }
        return InteractionResult.FAIL;
    }
    
    
    
    @Override
    public void onRemove(BlockState blockState, Level world, BlockPos blockPos, BlockState newBlockState, boolean isMoving) {
        if (blockState.getBlock() != newBlockState.getBlock()) {
            BlockEntity tile = world.getBlockEntity(blockPos);
            if (tile instanceof CyaniteReprocessorTile) {
                ((CyaniteReprocessorTile) tile).onReplaced(blockState, world, blockPos, newBlockState, isMoving);
            }
            super.onRemove(blockState, world, blockPos, newBlockState, isMoving);
        }
    }
    
    @Override
    public void setPlacedBy(@Nonnull Level world, @Nonnull BlockPos blockPos, @Nonnull BlockState blockState, @Nullable LivingEntity entity, @Nonnull ItemStack stack) {
        if (entity != null) {
            world.setBlock(blockPos, blockState
                    .setValue(FACING, getFacingFromEntity(blockPos, entity))
                    .setValue(ENABLED, false), 2);
        }
    }
}
