package com.ancientcurse.block;

import com.ancientcurse.ModBlocks;
import net.minecraft.block.*;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

/**
 * Represents tall grass that appears near the Nile River.
 * This is a double-height plant block similar to vanilla tall grass.
 * Supports waterlogging for both upper and lower halves.
 */
public class NileRiverTallGrassBlock extends TallPlantBlock implements Waterloggable {
    public static final EnumProperty<DoubleBlockHalf> HALF = TallPlantBlock.HALF;
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    public NileRiverTallGrassBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
            .with(HALF, DoubleBlockHalf.LOWER)
            .with(WATERLOGGED, false));
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(WATERLOGGED);
    }
    
    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        if (state.get(HALF) != DoubleBlockHalf.UPPER) {
            BlockPos blockPos = pos.down();
            BlockState blockState = world.getBlockState(blockPos);
            
            // Can be placed on various soil types including our custom blocks
            return blockState.isIn(BlockTags.DIRT) || 
                   blockState.isOf(Blocks.GRASS_BLOCK) ||
                   blockState.isOf(Blocks.DIRT) ||
                   blockState.isOf(Blocks.COARSE_DIRT) ||
                   blockState.isOf(Blocks.PODZOL) || 
                   blockState.isOf(Blocks.FARMLAND) ||
                   blockState.isOf(Blocks.SAND) ||
                   blockState.isOf(Blocks.RED_SAND) ||
                   blockState.isOf(Blocks.GRAVEL) ||
                   blockState.isOf(Blocks.CLAY) ||
                   blockState.isOf(ModBlocks.FERTILE_NILE_SILT) ||
                   blockState.isOf(ModBlocks.DRY_NILE_SILT) ||
                   blockState.isOf(ModBlocks.NILE_MUD) ||
                   blockState.isOf(ModBlocks.RIVERBED) ||
                   blockState.isOf(ModBlocks.RIVERBED_CLAY) ||
                   blockState.isOf(ModBlocks.NILE_RIVER_SAND) ||
                   blockState.isOf(ModBlocks.SMOOTH_SAND) ||
                   blockState.isOf(ModBlocks.DESHRET_SAND) ||
                   blockState.isOf(ModBlocks.DESHRET_WAVY_SAND) ||
                   blockState.isOf(ModBlocks.BLACK_SAND) ||
                   blockState.isOf(ModBlocks.ARID_NILE_TURF);
        } else {
            BlockState blockState = world.getBlockState(pos.down());
            return blockState.isOf(this) && blockState.get(HALF) == DoubleBlockHalf.LOWER;
        }
    }
    
    public void placeAt(World world, BlockState state, BlockPos pos, int flags) {
        FluidState fluidStateLower = world.getFluidState(pos);
        FluidState fluidStateUpper = world.getFluidState(pos.up());
        
        world.setBlockState(pos, state.with(HALF, DoubleBlockHalf.LOWER)
            .with(WATERLOGGED, fluidStateLower.getFluid() == Fluids.WATER), flags);
        world.setBlockState(pos.up(), state.with(HALF, DoubleBlockHalf.UPPER)
            .with(WATERLOGGED, fluidStateUpper.getFluid() == Fluids.WATER), flags);
    }
    
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        FluidState fluidStateUpper = world.getFluidState(pos.up());
        world.setBlockState(pos.up(), state.with(HALF, DoubleBlockHalf.UPPER)
            .with(WATERLOGGED, fluidStateUpper.getFluid() == Fluids.WATER), 3);
    }
    
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockPos blockPos = ctx.getBlockPos();
        World world = ctx.getWorld();
        FluidState fluidState = world.getFluidState(blockPos);
        
        if (blockPos.getY() < world.getTopY() - 1 && world.getBlockState(blockPos.up()).canReplace(ctx)) {
            return super.getPlacementState(ctx)
                .with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
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
    public boolean canReplace(BlockState state, ItemPlacementContext context) {
        return false;
    }
    
    @Override
    public float getMaxHorizontalModelOffset() {
        return 0.1f;
    }
    
    @Override
    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        return type == NavigationType.AIR && !this.collidable || super.canPathfindThrough(state, world, pos, type);
    }
}