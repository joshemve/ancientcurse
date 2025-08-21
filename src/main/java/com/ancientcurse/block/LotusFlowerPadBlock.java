package com.ancientcurse.block;

import com.ancientcurse.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

/**
 * Lotus Flower Pad block that opens during the day and closes at night.
 * Similar to lily pads but with day/night cycle behavior.
 */
public class LotusFlowerPadBlock extends Block {
    public static final BooleanProperty OPEN = BooleanProperty.of("open");
    // Shape similar to vanilla lily pad but slightly thicker to be walkable
    protected static final VoxelShape SHAPE = Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, 1.5, 15.0);
    // Collision shape that allows entities to stand on it
    protected static final VoxelShape COLLISION_SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);
    
    public LotusFlowerPadBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(OPEN, true));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
    
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        // Return collision shape that entities can walk on
        return COLLISION_SHAPE;
    }
    
    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        // Can only be placed on water
        FluidState fluidState = world.getFluidState(pos.down());
        return fluidState.isOf(Fluids.WATER) && fluidState.getLevel() == 8;
    }
    
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        World world = ctx.getWorld();
        // Set initial open/closed state based on time of day
        boolean isDay = world.getTimeOfDay() % 24000 < 13000;
        return this.getDefaultState().with(OPEN, isDay);
    }
    
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, 
                                               WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (!state.canPlaceAt(world, pos)) {
            return null;
        }
        return state;
    }
    
    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // Update open/closed state based on time of day
        boolean isDay = world.getTimeOfDay() % 24000 < 13000;
        boolean isCurrentlyOpen = state.get(OPEN);
        
        if (isDay != isCurrentlyOpen) {
            world.setBlockState(pos, state.with(OPEN, isDay), Block.NOTIFY_LISTENERS);
        }
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
    }
    
    @Override
    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        // Water mobs can swim through, but land mobs treat it as solid
        return type == NavigationType.WATER;
    }
    
    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        // When picked with creative mode, give the block item
        return new ItemStack(this);
    }
    
    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient && !player.isCreative()) {
            // Only drop the Lotus Flower item if the pad is open
            if (state.get(OPEN)) {
                // Drop the Lotus Flower item when the block is broken
                ItemStack itemStack = new ItemStack(ModItems.LOTUS_FLOWER);
                Block.dropStack(world, pos, itemStack);
            }
        }
        super.onBreak(world, pos, state, player);
    }
}
