package com.ancientcurse.block;

import com.ancientcurse.ModBlocks;
import net.minecraft.block.*;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

/**
 * Nile River Grass block that can be placed on land or underwater.
 * Supports waterlogging and bone meal growth.
 */
public class NileRiverGrassBlock extends PlantBlock implements Fertilizable, Waterloggable {
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
    protected static final VoxelShape SHAPE = Block.createCuboidShape(2.0, 0.0, 2.0, 14.0, 12.0, 14.0);
    
    public NileRiverGrassBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(WATERLOGGED, false));
    }
    
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
    
    @Override
    protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        return floor.isIn(BlockTags.DIRT) || 
               floor.isOf(Blocks.SAND) || 
               floor.isOf(Blocks.RED_SAND) ||
               floor.isOf(Blocks.GRAVEL) ||
               floor.isOf(Blocks.CLAY) ||
               floor.isOf(ModBlocks.FERTILE_NILE_SILT) || 
               floor.isOf(ModBlocks.DRY_NILE_SILT) ||
               floor.isOf(ModBlocks.NILE_MUD) ||
               floor.isOf(ModBlocks.RIVERBED) ||
               floor.isOf(ModBlocks.RIVERBED_CLAY) ||
               floor.isOf(ModBlocks.NILE_RIVER_SAND) ||
               floor.isOf(ModBlocks.SMOOTH_SAND) ||
               floor.isOf(ModBlocks.DESHRET_SAND) ||
               floor.isOf(ModBlocks.DESHRET_WAVY_SAND) ||
               floor.isOf(ModBlocks.BLACK_SAND) ||
               floor.isOf(ModBlocks.ARID_NILE_TURF);
    }
    
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
        BlockState state = super.getPlacementState(ctx);
        
        if (state != null) {
            return state.with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
        }
        return null;
    }
    
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, 
                                               WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }
    
    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }
    
    @Override
    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        return type == NavigationType.AIR && !this.collidable || super.canPathfindThrough(state, world, pos, type);
    }
    
    // Fertilizable implementation for bone meal
    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state, boolean isClient) {
        // Can convert to tall grass if there's space above
        return world.getBlockState(pos.up()).isAir() || world.getBlockState(pos.up()).getFluidState().getFluid() == Fluids.WATER;
    }
    
    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return true;
    }
    
    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        // Convert to tall grass when bone meal is used
        if (ModBlocks.NILE_RIVER_TALL_GRASS != null) {
            BlockState tallGrassState = ModBlocks.NILE_RIVER_TALL_GRASS.getDefaultState();
            
            if (tallGrassState.canPlaceAt(world, pos) && world.isAir(pos.up())) {
                // Place tall grass
                TallPlantBlock tallPlantBlock = (TallPlantBlock) ModBlocks.NILE_RIVER_TALL_GRASS;
                tallPlantBlock.placeAt(world, tallGrassState, pos, 2);
            }
        }
    }
}