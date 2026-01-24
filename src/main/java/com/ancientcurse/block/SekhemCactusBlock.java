package com.ancientcurse.block;

import com.ancientcurse.ModBlockEntities;
import com.ancientcurse.ModBlocks;
import com.ancientcurse.block.entity.SekhemCactusBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

import com.ancientcurse.ModItems;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;

public class SekhemCactusBlock extends BlockWithEntity {
    public static final IntProperty AGE = Properties.AGE_15;
    public static final IntProperty MAX_HEIGHT = IntProperty.of("max_height", 2, 4);
    public static final EnumProperty<SekhemCactusPosition> POSITION = EnumProperty.of("position",
            SekhemCactusPosition.class);
    public static final EnumProperty<SekhemCactusVariant> VARIANT = EnumProperty.of("variant",
            SekhemCactusVariant.class);
    public static final EnumProperty<SekhemCactusModelVariant> MODEL_VARIANT = EnumProperty.of("model_variant",
            SekhemCactusModelVariant.class);

    protected static final VoxelShape COLLISION_SHAPE = Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, 15.0, 15.0);
    protected static final VoxelShape OUTLINE_SHAPE = Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);

    public SekhemCactusBlock(Settings settings) {
        super(settings);
        this.setDefaultState(
                this.stateManager.getDefaultState().with(AGE, 0).with(POSITION, SekhemCactusPosition.BOTTOM)
                        .with(VARIANT, SekhemCactusVariant.DEFAULT)
                        .with(MODEL_VARIANT, SekhemCactusModelVariant.MODEL_1)
                        .with(MAX_HEIGHT, 3));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return OUTLINE_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return COLLISION_SHAPE;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        BlockState stateBelow = world.getBlockState(pos.down());

        if (stateBelow.isOf(this)) {
            // Count the height of the cactus stack below this position
            int currentHeight = 1;
            BlockPos checkPos = pos.down();
            while (world.getBlockState(checkPos.down()).isOf(this)) {
                currentHeight++;
                checkPos = checkPos.down();
            }

            // Check if placing this block would exceed the max height
            int maxHeight = stateBelow.get(MAX_HEIGHT);
            if (currentHeight >= maxHeight) {
                return null; // Prevent placement if it would exceed max height
            }

            return this.getDefaultState()
                    .with(VARIANT, stateBelow.get(VARIANT))
                    .with(MODEL_VARIANT, stateBelow.get(MODEL_VARIANT))
                    .with(MAX_HEIGHT, stateBelow.get(MAX_HEIGHT));
        } else {
            SekhemCactusModelVariant modelVariant = world.random.nextFloat() < 0.3f ? SekhemCactusModelVariant.MODEL_2
                    : SekhemCactusModelVariant.MODEL_1;

            SekhemCactusVariant variant;
            if (modelVariant == SekhemCactusModelVariant.MODEL_2) {
                variant = SekhemCactusVariant.HEALTHY; // Model 2 is forced to Healthy
            } else {
                float r = world.random.nextFloat();
                if (r < 0.4f)
                    variant = SekhemCactusVariant.DEFAULT; // sekhem_cactus_block.png
                else if (r < 0.7f)
                    variant = SekhemCactusVariant.DRY; // sekhem_cactus_dry.png
                else
                    variant = SekhemCactusVariant.HEALTHY; // sekhem_cactus_healthy.png
            }

            int maxHeight = 2 + world.random.nextInt(3); // 2, 3, or 4

            return this.getDefaultState()
                    .with(VARIANT, variant)
                    .with(MODEL_VARIANT, modelVariant)
                    .with(MAX_HEIGHT, maxHeight);
        }
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        BlockPos blockPos = pos.up();
        if (world.isAir(blockPos)) {
            int i = 1;
            while (world.getBlockState(pos.down(i)).isOf(this)) {
                i++;
            }

            int cactusMaxHeight = state.get(MAX_HEIGHT);
            if (i < cactusMaxHeight) { // respects randomized max height
                int j = state.get(AGE);
                if (j == 15) {
                    SekhemCactusVariant variant = state.get(VARIANT);
                    SekhemCactusModelVariant modelVariant = state.get(MODEL_VARIANT);
                    int maxHeight = state.get(MAX_HEIGHT);
                    world.setBlockState(blockPos, this.getDefaultState()
                            .with(VARIANT, variant)
                            .with(MODEL_VARIANT, modelVariant)
                            .with(MAX_HEIGHT, maxHeight));
                    BlockState blockState = state.with(AGE, 0);
                    world.setBlockState(pos, blockState, 4);
                    blockState.neighborUpdate(world, blockPos, this, pos, false);
                } else {
                    world.setBlockState(pos, state.with(AGE, j + 1), 4);
                }
            }
        }

        // Date growth logic: Optimized for server performance
        // Only the bottom block handles growth for the whole stack to prevent redundant
        // scans.
        if (state.get(POSITION) == SekhemCactusPosition.BOTTOM && random.nextInt(5) == 0) {
            growDate(world, pos, random);
        }
    }

    private void growDate(ServerWorld world, BlockPos pos, Random random) {
        // Use mutable pos to avoid allocations
        BlockPos.Mutable current = pos.mutableCopy();

        // Go down to the bottom of the cactus stack
        while (world.getBlockState(current.down()).isOf(this)) {
            current.move(Direction.DOWN);
        }
        BlockPos bottomPos = current.toImmutable();

        // Count dates and empty slots in a single pass (no list allocation)
        int totalDates = 0;
        int emptySlotCount = 0;
        int segmentIndex = 0;

        current.set(bottomPos);
        while (world.getBlockState(current).isOf(this)) {
            BlockEntity be = world.getBlockEntity(current);
            if (be instanceof SekhemCactusBlockEntity cactusBe) {
                if (cactusBe.isDateGrown(1)) {
                    totalDates++;
                } else if (segmentIndex > 0) {
                    // Only count empty slots on non-bottom segments
                    emptySlotCount++;
                }
                if (cactusBe.isDateGrown(2)) {
                    totalDates++;
                } else if (segmentIndex > 0) {
                    emptySlotCount++;
                }
            }
            current.move(Direction.UP);
            segmentIndex++;
        }

        // Limit to 2 dates per entire cactus stack
        if (totalDates >= 2 || emptySlotCount == 0) {
            return;
        }

        // Pick a random empty slot and find it in a second pass
        int targetSlot = random.nextInt(emptySlotCount);
        int currentSlot = 0;

        current.set(bottomPos).move(Direction.UP); // Start from first non-bottom segment
        while (world.getBlockState(current).isOf(this)) {
            BlockEntity be = world.getBlockEntity(current);
            if (be instanceof SekhemCactusBlockEntity cactusBe) {
                if (!cactusBe.isDateGrown(1)) {
                    if (currentSlot == targetSlot) {
                        cactusBe.setDateGrown(1, true);
                        return;
                    }
                    currentSlot++;
                }
                if (!cactusBe.isDateGrown(2)) {
                    if (currentSlot == targetSlot) {
                        cactusBe.setDateGrown(2, true);
                        return;
                    }
                    currentSlot++;
                }
            }
            current.move(Direction.UP);
        }
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
            WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (!state.canPlaceAt(world, pos)) {
            world.scheduleBlockTick(pos, this, 1);
        }

        // Update position based on neighbors
        return updatePositionState(state, world, pos);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!state.canPlaceAt(world, pos)) {
            world.breakBlock(pos, true);
        }
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockState blockState = world.getBlockState(pos.offset(direction));
            if (blockState.isSolid() || world.getFluidState(pos.offset(direction)).isStill()) {
                return false;
            }
        }

        BlockState blockState2 = world.getBlockState(pos.down());
        return (blockState2.isOf(this) || blockState2.isOf(Blocks.SAND) || blockState2.isOf(Blocks.RED_SAND) ||
                blockState2.isOf(ModBlocks.SMOOTH_SAND) || blockState2.isOf(ModBlocks.NILE_RIVER_SAND) ||
                blockState2.isOf(ModBlocks.DESHRET_SAND) || blockState2.isOf(ModBlocks.DESHRET_WAVY_SAND) ||
                blockState2.isOf(ModBlocks.BLACK_SAND) || blockState2.isOf(ModBlocks.CURSED_SAND))
                && !world.getBlockState(pos.up()).isLiquid();
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        entity.damage(world.getDamageSources().cactus(), 1.0f);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.isOf(newState.getBlock())) {
            return;
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        return false;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AGE, POSITION, VARIANT, MODEL_VARIANT, MAX_HEIGHT);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SekhemCactusBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    private BlockState updatePositionState(BlockState state, BlockView world, BlockPos pos) {
        boolean hasCactusBelow = world.getBlockState(pos.down()).isOf(this);
        boolean hasCactusAbove = world.getBlockState(pos.up()).isOf(this);

        if (!hasCactusBelow) {
            return state.with(POSITION, SekhemCactusPosition.BOTTOM);
        }

        if (!hasCactusAbove) {
            return state.with(POSITION, SekhemCactusPosition.TOP);
        }

        // It's a middle piece. Determine if it's MIDDLE or MIDDLE2
        int countBelow = 0;
        BlockPos current = pos.down();
        while (world.getBlockState(current).isOf(this)) {
            countBelow++;
            current = current.down();
        }

        if (countBelow == 1) {
            return state.with(POSITION, SekhemCactusPosition.MIDDLE);
        } else {
            return state.with(POSITION, SekhemCactusPosition.MIDDLE2);
        }
    }

    public enum SekhemCactusPosition implements StringIdentifiable {
        BOTTOM("bottom"),
        MIDDLE("middle"),
        MIDDLE2("middle2"),
        TOP("top");

        private final String name;

        SekhemCactusPosition(String name) {
            this.name = name;
        }

        @Override
        public String asString() {
            return this.name;
        }
    }

    public enum SekhemCactusVariant implements StringIdentifiable {
        DEFAULT("default"),
        HEALTHY("healthy"),
        DRY("dry");

        private final String name;

        SekhemCactusVariant(String name) {
            this.name = name;
        }

        @Override
        public String asString() {
            return this.name;
        }
    }

    public enum SekhemCactusModelVariant implements StringIdentifiable {
        MODEL_1("model_1"),
        MODEL_2("model_2");

        private final String name;

        SekhemCactusModelVariant(String name) {
            this.name = name;
        }

        @Override
        public String asString() {
            return this.name;
        }
    }
}
