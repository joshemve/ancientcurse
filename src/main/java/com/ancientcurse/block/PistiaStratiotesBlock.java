package com.ancientcurse.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

/**
 * Pistia Stratiotes (Water Lettuce) - A floating aquatic plant.
 * Floats on the surface of water, similar to lily pads.
 * Can be placed on water, ice, frosted ice, or waterlogged blocks.
 */
public class PistiaStratiotesBlock extends Block {
    // Visual shape - slightly raised off the water surface
    protected static final VoxelShape SHAPE = Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, 1.5, 15.0);
    // Collision shape - flat to allow entities to walk on it
    protected static final VoxelShape COLLISION_SHAPE = Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, 1.0, 15.0);

    public PistiaStratiotesBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return COLLISION_SHAPE;
    }

    @Override
    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        // Allow water pathfinding through, treat as obstacle for land/air
        return type == NavigationType.WATER;
    }

    /**
     * Check if the block below can support this floating plant.
     * Valid supports: water source, ice, frosted ice, or waterlogged blocks.
     */
    private boolean isValidWaterBelow(WorldView world, BlockPos pos) {
        BlockPos belowPos = pos.down();
        BlockState belowState = world.getBlockState(belowPos);

        // Check for ice or frosted ice (like lily pads)
        if (belowState.isOf(Blocks.ICE) || belowState.isOf(Blocks.FROSTED_ICE) || belowState.isOf(Blocks.PACKED_ICE) || belowState.isOf(Blocks.BLUE_ICE)) {
            return true;
        }

        // Check for full water source block
        FluidState belowFluid = world.getFluidState(belowPos);
        if (belowFluid.isOf(Fluids.WATER) && belowFluid.isStill()) {
            return true;
        }

        // Check for waterlogged blocks (slabs, stairs, etc. with water)
        if (belowState.getFluidState().isOf(Fluids.WATER) && belowState.getFluidState().isStill()) {
            return true;
        }

        return false;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockPos pos = ctx.getBlockPos();
        World world = ctx.getWorld();
        BlockState stateAtPos = world.getBlockState(pos);
        FluidState fluidAtPos = world.getFluidState(pos);

        // Case 1: Placing in air above water/ice
        if (stateAtPos.isAir() && isValidWaterBelow(world, pos)) {
            return this.getDefaultState();
        }

        // Case 2: Placing in water (the water becomes the support, place on top)
        if (fluidAtPos.isOf(Fluids.WATER) && fluidAtPos.isStill()) {
            // Check if the position above is air (where we'll actually place)
            BlockPos abovePos = pos.up();
            BlockState aboveState = world.getBlockState(abovePos);
            if (aboveState.isAir()) {
                // Return null here - the item's use behavior should handle placing above
                // For now, allow placing at this position since it replaces water
                return this.getDefaultState();
            }
            return this.getDefaultState();
        }

        // Case 3: Replacing seagrass, kelp, or other water plants
        if (stateAtPos.isReplaceable() && !stateAtPos.isAir()) {
            // Check if there's still water support (the plant was in water)
            if (isValidWaterBelow(world, pos) || fluidAtPos.isOf(Fluids.WATER)) {
                return this.getDefaultState();
            }
        }

        return null;
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        // The block below must be water, ice, or a waterlogged block
        return isValidWaterBelow(world, pos);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        // Only care about changes below us
        if (direction == Direction.DOWN && !state.canPlaceAt(world, pos)) {
            return Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        super.onEntityCollision(state, world, pos, entity);
        // Boats destroy water lettuce on collision (like lily pads)
        if (entity instanceof BoatEntity && !world.isClient) {
            world.breakBlock(pos, true, entity);
        }
    }

    /**
     * Not a full cube - allows proper rendering
     */
    @Override
    public boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
        return true;
    }
}