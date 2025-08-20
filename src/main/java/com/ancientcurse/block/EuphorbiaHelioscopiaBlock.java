package com.ancientcurse.block;

import com.ancientcurse.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public class EuphorbiaHelioscopiaBlock extends Block {
    protected static final VoxelShape SHAPE = Block.createCuboidShape(2.0, 0.0, 2.0, 14.0, 14.0, 14.0);

    public EuphorbiaHelioscopiaBlock(Settings settings) {
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
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos blockPos = pos.down();
        return canPlantOnTop(world.getBlockState(blockPos), world, blockPos);
    }

    protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        return floor.isOf(Blocks.SAND) || 
               floor.isOf(Blocks.RED_SAND) || 
               floor.isOf(Blocks.DIRT) || 
               floor.isOf(Blocks.GRASS_BLOCK) ||
               floor.isOf(ModBlocks.SMOOTH_SAND) ||
               floor.isOf(ModBlocks.NILE_RIVER_SAND) ||
               floor.isOf(ModBlocks.DESHRET_SAND) ||
               floor.isOf(ModBlocks.DESHRET_WAVY_SAND);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        return !state.canPlaceAt(world, pos) ? Blocks.AIR.getDefaultState() : super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }
} 