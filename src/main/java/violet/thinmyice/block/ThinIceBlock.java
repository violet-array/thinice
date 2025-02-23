package violet.thinmyice.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.*;
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


public class ThinIceBlock extends Block implements SimpleWaterloggedBlock {
    public static final MapCodec<ThinIceBlock> CODEC = simpleCodec(ThinIceBlock::new);
    public static final int MAX_HEIGHT = 8;
    public static final IntegerProperty LAYERS = BlockStateProperties.LAYERS;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    protected static final VoxelShape[] SHAPE_BY_LAYER = new VoxelShape[]{
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

    public ThinIceBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any().setValue(LAYERS, 1).setValue(WATERLOGGED, false));
    }

//    @Override
//    protected boolean isPathfindable(@NotNull BlockState state, @NotNull PathComputationType pathComputationType) {
//        if (Objects.requireNonNull(pathComputationType) == PathComputationType.LAND) {
//            return state.getValue(LAYERS) < 5;
//        }
//        return false;
//    }


    // Waterlogging
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
        return SHAPE_BY_LAYER[state.getValue(LAYERS)];
    }

    @Override
    protected @NotNull VoxelShape getCollisionShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPE_BY_LAYER[state.getValue(LAYERS)];
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter reader, BlockPos pos) {
        return SHAPE_BY_LAYER[state.getValue(LAYERS)];
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter reader, BlockPos pos, CollisionContext context) {
        return SHAPE_BY_LAYER[state.getValue(LAYERS)];
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

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos blockpos = context.getClickedPos();
        BlockState blockstate = context.getLevel().getBlockState(blockpos);
        if (blockstate.is(this)) {
            int i = blockstate.getValue(LAYERS);
            if (i >= MAX_HEIGHT-1) {
                return blockstate.setValue(LAYERS, Math.min(MAX_HEIGHT, i + 1)).setValue(WATERLOGGED, false);
            }
            else {
                return blockstate.setValue(LAYERS, Math.min(MAX_HEIGHT, i + 1));
            }
        } else {
            //Waterlogging logic
            FluidState fluidState = context.getLevel().getFluidState(blockpos);
            BlockState defaultBlockState = this.defaultBlockState();
            if (fluidState.getType() == Fluids.WATER)
                return defaultBlockState.setValue(WATERLOGGED, true);

            return super.getStateForPlacement(context);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LAYERS, WATERLOGGED);

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



        //TODO: this could cull a lot more faces with lots of logic
        if(adjacentBlockState.is(this) && side != Direction.DOWN) {

            return state.getValue(LAYERS).equals(adjacentBlockState.getValue(LAYERS));
        }
        else {
            return super.skipRendering(state, adjacentBlockState, side);
        }


//        return adjacentBlockState.is(this) ? true : super.skipRendering(state, adjacentBlockState, side);
    }


    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext useContext) {
        int i = state.getValue(LAYERS);
        if (!useContext.getItemInHand().is(this.asItem()) || i >= 8) {
            return i == 1;
        } else {
            return !useContext.replacingClickedOnBlock() || useContext.getClickedFace() == Direction.UP;
        }
    }
}
