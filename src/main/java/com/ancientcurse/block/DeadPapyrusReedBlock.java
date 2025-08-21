package com.ancientcurse.block;

import com.ancientcurse.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

/**
 * Implements a dead papyrus reed block that can only be placed
 * on specific blocks and has a non-full collision box.
 */
public class DeadPapyrusReedBlock extends PlantBlock {
    protected static final VoxelShape SHAPE = Block.createCuboidShape(2.0, 0.0, 2.0, 14.0, 14.0, 14.0);
    
    public DeadPapyrusReedBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
    
    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos.down());
        return isSupportingBlock(blockState);
    }
    
    private boolean isSupportingBlock(BlockState state) {
        // Dead papyrus can be placed on sand, dirt-like blocks, or Nile silt variants
        Block block = state.getBlock();
        return block == Blocks.SAND || 
               block == Blocks.RED_SAND || 
               block == ModBlocks.FERTILE_NILE_SILT || 
               block == ModBlocks.DRY_NILE_SILT || 
               block == Blocks.DIRT || 
               block == Blocks.GRASS_BLOCK || 
               block == ModBlocks.ARID_NILE_TURF ||
               block == ModBlocks.SMOOTH_SAND ||
               block == ModBlocks.NILE_RIVER_SAND ||
               block == ModBlocks.DESHRET_SAND ||
               block == ModBlocks.DESHRET_WAVY_SAND ||
               block == ModBlocks.BLACK_SAND ||
               block == ModBlocks.NILE_MUD ||
               block == ModBlocks.RIVERBED ||
               block == ModBlocks.RIVERBED_CLAY;
    }
    
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        // If support block is removed, break this block
        if (direction == Direction.DOWN && !state.canPlaceAt(world, pos)) {
            return null;
        }
        
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }
    
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        // Check if can be placed during placement
        if (!this.getDefaultState().canPlaceAt(ctx.getWorld(), ctx.getBlockPos())) {
            return null;
        }
        return super.getPlacementState(ctx);
    }
    
    @Override
    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        return type == NavigationType.AIR && !this.collidable || super.canPathfindThrough(state, world, pos, type);
    }
    
    @Override
    protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        return isSupportingBlock(floor);
    }
}