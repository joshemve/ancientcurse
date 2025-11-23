package com.ancientcurse.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

/**
 * Sandstone Path Block - A path variant for sandstone blocks
 * Similar to vanilla dirt path, but for desert stone environments
 */
public class SandstonePathBlock extends Block {
    // Path blocks are 15/16 of a block high (15 pixels)
    protected static final VoxelShape SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 15.0, 16.0);

    public SandstonePathBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        // Only place if the block below is solid and not underwater
        BlockPos pos = ctx.getBlockPos();
        BlockState stateBelow = ctx.getWorld().getBlockState(pos.down());

        if (!stateBelow.isSideSolidFullSquare(ctx.getWorld(), pos.down(), Direction.UP)) {
            return null;
        }

        if (ctx.getWorld().getFluidState(pos).isIn(FluidTags.WATER)) {
            return null;
        }

        return super.getPlacementState(ctx);
    }

    @Override
    public BlockState getStateForNeighborUpdate(
        BlockState state,
        Direction direction,
        BlockState neighborState,
        WorldAccess world,
        BlockPos pos,
        BlockPos neighborPos
    ) {
        // If a solid block is placed on top, convert back to sandstone
        if (direction == Direction.UP && !state.canPlaceAt(world, pos)) {
            world.scheduleBlockTick(pos, this, 1);
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // Check if path should convert back to sandstone
        BlockState stateAbove = world.getBlockState(pos.up());
        if (stateAbove.isSolid() && stateAbove.isSideSolidFullSquare(world, pos.up(), Direction.DOWN)) {
            // Convert back to sandstone
            world.setBlockState(pos, Blocks.SANDSTONE.getDefaultState());
        }
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockState stateAbove = world.getBlockState(pos.up());
        // Can only exist if there's no solid block above
        return !stateAbove.isSolid() || !stateAbove.isSideSolidFullSquare(world, pos.up(), Direction.DOWN);
    }

    @Override
    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        return false;
    }
}
