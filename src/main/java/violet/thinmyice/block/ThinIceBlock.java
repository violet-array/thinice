package violet.thinmyice.block;

import com.ibm.icu.text.RelativeDateTimeFormatter;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
//import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

import net.minecraft.core.Direction;
import violet.thinmyice.ThinMyIce;


public class ThinIceBlock extends IceBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<ThinIceBlock> CODEC = simpleCodec(ThinIceBlock::new);
    public static final int MAX_HEIGHT = 8;
    public static final BooleanProperty BOTTOM = BlockStateProperties.BOTTOM;
    public static final IntegerProperty LAYERS = BlockStateProperties.LAYERS;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    protected static final VoxelShape[] TOP_SHAPE_BY_LAYER = new VoxelShape[]{
            Shapes.empty(),
            Block.box(0.0, 14.0, 0.0, 16.0, 16.0, 16.0),
            Block.box(0.0, 12.0, 0.0, 16.0, 16.0, 16.0),
            Block.box(0.0, 10.0, 0.0, 16.0, 16.0, 16.0),
            Block.box(0.0, 8.0, 0.0, 16.0, 16.0, 16.0),
            Block.box(0.0, 6.0, 0.0, 16.0, 16.0, 16.0),
            Block.box(0.0, 4.0, 0.0, 16.0, 16.0, 16.0),
            Block.box(0.0, 2.0, 0.0, 16.0, 16.0, 16.0),
            Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0)
    };

    protected static final VoxelShape[] BOTTOM_SHAPE_BY_LAYER = new VoxelShape[]{
            Shapes.empty(),
            Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0),
            Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0),
            Block.box(0.0, 0.0, 0.0, 16.0, 6.0, 16.0),
            Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0),
            Block.box(0.0, 0.0, 0.0, 16.0, 10.0, 16.0),
            Block.box(0.0, 0.0, 0.0, 16.0, 12.0, 16.0),
            Block.box(0.0, 0.0, 0.0, 16.0, 14.0, 16.0),
            Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0)
    };


    public ThinIceBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any().setValue(LAYERS, 1).setValue(WATERLOGGED, false).setValue(BOTTOM, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BOTTOM, LAYERS, WATERLOGGED);

    }

//    @Override
//    protected boolean isPathfindable(@NotNull BlockState state, @NotNull PathComputationType pathComputationType) {
//        if (Objects.requireNonNull(pathComputationType) == PathComputationType.LAND) {
//            return state.getValue(LAYERS) < 5;
//        }
//        return false;
//    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos blockpos = context.getClickedPos();
        BlockState blockstate = context.getLevel().getBlockState(blockpos);
        if (blockstate.is(this)) {
            int i = blockstate.getValue(LAYERS);
            if (i >= MAX_HEIGHT-1) {
                return blockstate.setValue(LAYERS, Math.min(MAX_HEIGHT, i + 1)).setValue(WATERLOGGED, false).setValue(BOTTOM, false);
            }
            else {
                return blockstate.setValue(LAYERS, Math.min(MAX_HEIGHT, i + 1));
            }
        } else {
            //Waterlogging logic
            FluidState fluidState = context.getLevel().getFluidState(blockpos);
            BlockState defaultBlockState = this.defaultBlockState()
                    .setValue(BOTTOM, false)
                    .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);

            Direction direction = context.getClickedFace();
            return direction != Direction.DOWN && (direction == Direction.UP || !(context.getClickLocation().y - (double)blockpos.getY() > 0.5))
                    ? defaultBlockState.setValue(BOTTOM, true)
                    : defaultBlockState;
        }
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext useContext) {
        if (!useContext.getItemInHand().is(this.asItem())) {
            return false;
        } else {
            if (state.getValue(BOTTOM)) {
                return !useContext.replacingClickedOnBlock() || useContext.getClickedFace() == Direction.UP;
            }
            else {
                return state.getValue(LAYERS) != MAX_HEIGHT && useContext.getClickedFace() == Direction.DOWN;
            }
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBrightness(LightLayer.BLOCK, pos) > 11) {
            dropResources(state, level, pos);
            level.removeBlock(pos, false);
        }
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacentBlockState, Direction side) {

        //TODO: Bottom Thin Ice Blocks below Ice Blocks are rendering a top face
        // Thin Ice Blocks below Full Thin Ice Blocks also render it

        int layers = state.getValue(LAYERS);
        boolean isBottom = state.getValue(BOTTOM);

        if(adjacentBlockState.is(this)) {

            int adjacentLayers = adjacentBlockState.getValue(LAYERS);
            boolean adjacentIsBottom = adjacentBlockState.getValue(BOTTOM);

            if (side == Direction.UP) {

                return (layers == MAX_HEIGHT && adjacentIsBottom) || (!isBottom && adjacentIsBottom) || (layers == MAX_HEIGHT && adjacentLayers == MAX_HEIGHT);
            }
            else if (side == Direction.DOWN) {

                return (layers == MAX_HEIGHT && !adjacentIsBottom) || (isBottom && !adjacentIsBottom) || (layers == MAX_HEIGHT && adjacentLayers == MAX_HEIGHT);
            }
            else {
                if (layers <= adjacentLayers) {
                    return adjacentLayers == MAX_HEIGHT || isBottom == adjacentIsBottom;
                }
                else return false;
            }
        }
        else if(adjacentBlockState.is(Blocks.ICE)) {
            if (side == Direction.UP && !isBottom) {
                return true;
            }
            else return side == Direction.DOWN && isBottom;
        }
        else {
            return super.skipRendering(state, adjacentBlockState, side);
        }
    }





    /* SHAPE AND WATER */
    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        return state.getValue(LAYERS) != MAX_HEIGHT ? SimpleWaterloggedBlock.super.placeLiquid(level, pos, state, fluidState) : false;
    }
    @Override
    public boolean canPlaceLiquid(@Nullable Player player, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return state.getValue(LAYERS) != MAX_HEIGHT
                ? SimpleWaterloggedBlock.super.canPlaceLiquid(player, level, pos, state, fluid)
                : false;
    }

    @Override
    protected @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return state.getValue(BOTTOM) ? BOTTOM_SHAPE_BY_LAYER[state.getValue(LAYERS)] : TOP_SHAPE_BY_LAYER[state.getValue(LAYERS)];
    }

    @Override
    protected @NotNull VoxelShape getCollisionShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return state.getValue(BOTTOM) ? BOTTOM_SHAPE_BY_LAYER[state.getValue(LAYERS)] : TOP_SHAPE_BY_LAYER[state.getValue(LAYERS)];
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter reader, BlockPos pos) {
        return state.getValue(BOTTOM) ? BOTTOM_SHAPE_BY_LAYER[state.getValue(LAYERS)] : TOP_SHAPE_BY_LAYER[state.getValue(LAYERS)];
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter reader, BlockPos pos, CollisionContext context) {
        return state.getValue(BOTTOM) ? BOTTOM_SHAPE_BY_LAYER[state.getValue(LAYERS)] : TOP_SHAPE_BY_LAYER[state.getValue(LAYERS)];
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(LAYERS) == 8 ? 0.2F : 1.0F;
    }


    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity te, ItemStack stack) {
        player.awardStat(Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);
        dropResources(state, level, pos, te, player, stack);
    }

}
