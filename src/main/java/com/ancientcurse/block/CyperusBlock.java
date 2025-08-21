package com.ancientcurse.block;

import com.ancientcurse.ModBlocks;
import net.minecraft.block.*;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

/**
 * Cyperus - An ancient Egyptian water plant (papyrus sedge)
 * Can grow in shallow water or on sand near water
 */
public class CyperusBlock extends PlantBlock implements Waterloggable {
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
    protected static final VoxelShape SHAPE = Block.createCuboidShape(2.0, 0.0, 2.0, 14.0, 12.0, 14.0);
    
    public CyperusBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(WATERLOGGED, false));
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }
    
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
    
    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos blockPos = pos.down();
        BlockState groundState = world.getBlockState(blockPos);
        Block groundBlock = groundState.getBlock();
        
        // Can be placed on various sand types and mud/silt blocks
        if (canPlantOnTop(groundState, world, blockPos)) {
            // Check if we're in water or adjacent to water
            FluidState fluidState = world.getFluidState(pos);
            boolean inWater = fluidState.isIn(FluidTags.WATER);
            
            // Can be placed in shallow water
            if (inWater) {
                return true;
            }
            
            // Or on sand/mud that's adjacent to water
            for (Direction direction : Direction.Type.HORIZONTAL) {
                BlockPos adjacentPos = pos.offset(direction);
                if (world.getFluidState(adjacentPos).isIn(FluidTags.WATER) ||
                    world.getFluidState(adjacentPos.down()).isIn(FluidTags.WATER)) {
                    return true;
                }
            }
            
            // Allow placement on any valid ground even without water nearby
            // (for creative building purposes)
            return true;
        }
        
        return false;
    }
    
    @Override
    protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        Block block = floor.getBlock();
        
        // Can grow on all sand types
        return block == Blocks.SAND || 
               block == Blocks.RED_SAND ||
               block == ModBlocks.SMOOTH_SAND ||
               block == ModBlocks.NILE_RIVER_SAND ||
               block == ModBlocks.DESHRET_SAND ||
               block == ModBlocks.DESHRET_WAVY_SAND ||
               // Can also grow on mud and silt
               block == Blocks.MUD ||
               block == ModBlocks.FERTILE_NILE_SILT ||
               block == ModBlocks.DRY_NILE_SILT ||
               block == ModBlocks.ARID_NILE_TURF ||
               // And dirt/grass for flexibility
               block == Blocks.DIRT ||
               block == Blocks.GRASS_BLOCK ||
               block == Blocks.COARSE_DIRT ||
               block == Blocks.ROOTED_DIRT;
    }
    
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
        return this.getDefaultState().with(WATERLOGGED, fluidState.isIn(FluidTags.WATER) && fluidState.getLevel() == 8);
    }
    
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, 
                                                WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        
        return !state.canPlaceAt(world, pos) ? Blocks.AIR.getDefaultState() : 
               super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }
    
    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }
    
    @Override
    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        return type == NavigationType.AIR && !this.collidable || super.canPathfindThrough(state, world, pos, type);
    }
}