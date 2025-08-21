package com.ancientcurse.block;

import com.ancientcurse.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.FluidTags;

/**
 * Papyrus reed block that grows similar to sugar cane but with an Egyptian theme.
 * Can grow up to 3 blocks tall and requires water or fertile Nile silt nearby.
 * Supports bone meal growth and can be waterlogged.
 */
public class PapyrusReedBlock extends Block implements Fertilizable, Waterloggable {
    public static final IntProperty AGE = Properties.AGE_15;
    public static final BooleanProperty TOP = BooleanProperty.of("top");
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
    protected static final VoxelShape SHAPE = Block.createCuboidShape(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);
    
    public PapyrusReedBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(AGE, 0)
                .with(TOP, true)
                .with(WATERLOGGED, false));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
    
    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!state.canPlaceAt(world, pos)) {
            world.breakBlock(pos, true);
            return;
        }
        
        // Only grow if this is the top block in the stack
        if (state.get(TOP)) {
            int height = 1;
            // Check how tall the current reed stack is
            while (world.getBlockState(pos.down(height)).isOf(this) && height < 3) {
                height++;
            }
            
            // If not at max height and there's space above, grow a new reed
            if (height < 3 && world.isAir(pos.up()) && random.nextInt(10) == 0) {
                // Create a new reed on top and mark this one as not the top
                world.setBlockState(pos.up(), this.getDefaultState());
                world.setBlockState(pos, state.with(TOP, false));
            }
        }
    }
    
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, 
                                               WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        // Handle waterlogging
        if (state.get(WATERLOGGED)) {
            world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        
        if (!state.canPlaceAt(world, pos)) {
            // Return AIR state to break the block immediately
            return Blocks.AIR.getDefaultState();
        }
        
        // Update TOP property based on block above
        if (direction == Direction.UP) {
            boolean isTop = !neighborState.isOf(this);
            return state.with(TOP, isTop);
        }
        
        return state;
    }
    
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState state = super.getPlacementState(ctx);
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        FluidState fluidState = world.getFluidState(pos);
        
        // Check if there's another reed block above
        boolean isTop = !world.getBlockState(pos.up()).isOf(this);
        
        if (state != null) {
            return state
                .with(TOP, isTop)
                .with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
        }
        return null;
    }
    
    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos.down());
        FluidState currentFluid = world.getFluidState(pos);
        
        // Check if block below is a valid soil for papyrus reed
        if (blockState.isOf(Blocks.GRASS_BLOCK) || 
            blockState.isOf(Blocks.DIRT) || 
            blockState.isOf(Blocks.SAND) || 
            blockState.isOf(ModBlocks.FERTILE_NILE_SILT) || 
            blockState.isOf(ModBlocks.DRY_NILE_SILT) || 
            blockState.isOf(ModBlocks.NILE_MUD) || 
            blockState.isOf(ModBlocks.RIVERBED) || 
            blockState.isOf(ModBlocks.RIVERBED_CLAY) ||
            blockState.isOf(ModBlocks.NILE_RIVER_SAND) ||
            blockState.isOf(ModBlocks.SMOOTH_SAND) ||
            blockState.isOf(ModBlocks.DESHRET_SAND) ||
            blockState.isOf(ModBlocks.DESHRET_WAVY_SAND) ||
            blockState.isOf(ModBlocks.BLACK_SAND)) {
            
            // Allow placement directly in water or check for water nearby
            if (currentFluid.isIn(FluidTags.WATER)) {
                return true;
            }
            
            // Check if there's water nearby
            BlockPos blockPos = pos.down();
            
            // Check all adjacent blocks for water
            for (Direction direction : Direction.Type.HORIZONTAL) {
                BlockState adjacentState = world.getBlockState(blockPos.offset(direction));
                FluidState fluidState = world.getFluidState(blockPos.offset(direction));
                
                // Valid if adjacent block is water or has water (waterlogged)
                if (fluidState.isIn(FluidTags.WATER) || 
                    adjacentState.isOf(Blocks.FROSTED_ICE) || 
                    adjacentState.isOf(ModBlocks.RIVERBED_MOSS)) {
                    return true;
                }
            }
        }

        // Allow papyrus to stack on top of itself
        return blockState.isOf(this);
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AGE, TOP, WATERLOGGED);
    }
    
    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }
    
    // Bone meal support methods
    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state, boolean isClient) {
        // Can only use bone meal if this is the top block and not at max height
        if (!state.get(TOP)) {
            return false;
        }
        
        // Check current height
        int height = 1;
        while (world.getBlockState(pos.down(height)).isOf(this) && height < 3) {
            height++;
        }
        
        // Can grow if less than max height and there's space above
        return height < 3 && world.getBlockState(pos.up()).isAir();
    }
    
    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return true;
    }
    
    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        // Find the top block of this reed stack
        BlockPos topPos = pos;
        while (world.getBlockState(topPos.up()).isOf(this)) {
            topPos = topPos.up();
        }
        
        // Check current height
        int height = 1;
        BlockPos checkPos = topPos;
        while (world.getBlockState(checkPos.down()).isOf(this) && height < 3) {
            height++;
            checkPos = checkPos.down();
        }
        
        // Grow if not at max height
        if (height < 3 && world.isAir(topPos.up())) {
            // Add a new reed on top
            world.setBlockState(topPos.up(), this.getDefaultState().with(TOP, true).with(WATERLOGGED, false));
            // Update the current top block to not be top anymore
            world.setBlockState(topPos, world.getBlockState(topPos).with(TOP, false));
        }
    }
} 