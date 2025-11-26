package com.ancientcurse.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public class PistiaStratiotesBlock extends Block {
    protected static final VoxelShape SHAPE = Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, 1.5, 15.0);

    public PistiaStratiotesBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        return type == NavigationType.AIR || super.canPathfindThrough(state, world, pos, type);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockPos pos = ctx.getBlockPos();
        BlockState blockStateAtPos = ctx.getWorld().getBlockState(pos);
        FluidState fluidState = ctx.getWorld().getFluidState(pos);

        // Can be placed if:
        // 1. Position has full water (level 8) OR
        // 2. Position has a replaceable block (like seagrass, kelp, etc.) with water below
        if (fluidState.isIn(FluidTags.WATER) && fluidState.getLevel() == 8) {
            return super.getPlacementState(ctx);
        }

        // Check if we can replace the block at this position and there's water below
        if (blockStateAtPos.isReplaceable() && !blockStateAtPos.isAir()) {
            FluidState belowFluid = ctx.getWorld().getFluidState(pos.down());
            if (belowFluid.isIn(FluidTags.WATER) && belowFluid.getLevel() == 8) {
                return super.getPlacementState(ctx);
            }
        }

        return null;
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        // Check if there's water at this position (floating on water)
        FluidState fluidState = world.getFluidState(pos);
        if (fluidState.isIn(FluidTags.WATER) && fluidState.getLevel() == 8) {
            return true;
        }

        // Or if there's water directly below (placed on seagrass, etc.)
        FluidState belowFluid = world.getFluidState(pos.down());
        return belowFluid.isIn(FluidTags.WATER) && belowFluid.getLevel() == 8;
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        return !state.canPlaceAt(world, pos) ? Blocks.AIR.getDefaultState() : super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }
}