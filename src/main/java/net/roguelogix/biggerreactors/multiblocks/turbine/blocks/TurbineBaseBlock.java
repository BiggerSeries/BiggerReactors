package net.roguelogix.biggerreactors.multiblocks.turbine.blocks;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import net.roguelogix.biggerreactors.multiblocks.turbine.state.TurbineActivity;
import net.roguelogix.phosphophyllite.modular.block.PhosphophylliteBlock;
import net.roguelogix.phosphophyllite.multiblock2.IMultiblockBlock;
import net.roguelogix.phosphophyllite.multiblock2.rectangular.IRectangularMultiblockBlock;

import javax.annotation.ParametersAreNonnullByDefault;

import static net.roguelogix.phosphophyllite.multiblock2.IAssemblyStateBlock.ASSEMBLED;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class TurbineBaseBlock extends PhosphophylliteBlock implements IMultiblockBlock, EntityBlock, IRectangularMultiblockBlock {
    public static final Block.Properties PROPERTIES_SOLID = Block.Properties.of().sound(SoundType.METAL).destroyTime(2).explosionResistance(10).isValidSpawn((a, b, c, d) -> false).requiresCorrectToolForDrops();
    public static final Block.Properties PROPERTIES_GLASS = Block.Properties.of().sound(SoundType.METAL).noOcclusion().destroyTime(2).explosionResistance(2).isValidSpawn((a, b, c, d) -> false).requiresCorrectToolForDrops();
    
    public TurbineBaseBlock() {
        this(true);
    }
    
    public TurbineBaseBlock(boolean solid) {
        super(solid ? PROPERTIES_SOLID : PROPERTIES_GLASS);
        if (usesTurbineState()) {
            registerDefaultState(defaultBlockState().setValue(TurbineActivity.TURBINE_STATE_ENUM_PROPERTY, TurbineActivity.INACTIVE));
        }
    }
    
    public boolean usesTurbineState() {
        return false;
    }
    
    @Override
    protected void buildStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        if (usesTurbineState()) {
            builder.add(TurbineActivity.TURBINE_STATE_ENUM_PROPERTY);
        }
    }
    
    @Override
    public boolean isGoodForInterior() {
        return false;
    }
    
    @Override
    public boolean isGoodForExterior() {
        return true;
    }
    
    @Override
    public boolean isGoodForFrame() {
        return false;
    }
    
    @Override
    public InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (hand == InteractionHand.MAIN_HAND && state.hasProperty(ASSEMBLED) && state.getValue(ASSEMBLED)) {
            if (level.getBlockEntity(pos) instanceof MenuProvider menuProvider) {
                if (!level.isClientSide) {
                    NetworkHooks.openScreen((ServerPlayer) player, menuProvider, pos);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.onUse(state, level, pos, player, hand, hitResult);
    }
}
