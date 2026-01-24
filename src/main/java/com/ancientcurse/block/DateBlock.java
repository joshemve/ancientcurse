package com.ancientcurse.block;

import com.ancientcurse.ModBlocks;
import com.ancientcurse.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public class DateBlock extends Block {
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
    public static final IntProperty AGE = Properties.AGE_2;
    
    // Define the different shapes for each direction - these are adjusted to connect to the log
    // For cocoa beans, FACING points TOWARD the log, so the shapes need to be positioned accordingly
    // Updated shapes to match the visual model (main part is from [4, 2, 7] to [12, 12, 14])
    private static final VoxelShape NORTH_SHAPE = Block.createCuboidShape(4.0, 2.0, 0.0, 12.0, 12.0, 7.0);  // Faces NORTH, connects to block on SOUTH
    private static final VoxelShape SOUTH_SHAPE = Block.createCuboidShape(4.0, 2.0, 9.0, 12.0, 12.0, 16.0); // Faces SOUTH, connects to block on NORTH
    private static final VoxelShape WEST_SHAPE = Block.createCuboidShape(0.0, 2.0, 4.0, 7.0, 12.0, 12.0);   // Faces WEST, connects to block on EAST
    private static final VoxelShape EAST_SHAPE = Block.createCuboidShape(9.0, 2.0, 4.0, 16.0, 12.0, 12.0);  // Faces EAST, connects to block on WEST

    public DateBlock(Settings settings) {
        super(settings.nonOpaque().strength(0.2f, 3.0f).ticksRandomly()); // Added ticksRandomly() to enable growth
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(AGE, 0));
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        // The FACING direction points TOWARD the supporting block, so we need to check in that direction
        BlockState blockState = world.getBlockState(pos.offset(state.get(FACING)));
        return blockState.isOf(ModBlocks.DATE_PALM_LOG) || blockState.isOf(ModBlocks.SEKHEM_CACTUS);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        // Set the facing direction based on the player's placement
        BlockState blockState = this.getDefaultState();
        WorldView worldView = ctx.getWorld();
        BlockPos blockPos = ctx.getBlockPos();
        Direction[] directions = ctx.getPlacementDirections();
        
        // Try each direction to find a valid placement
        for (Direction direction : directions) {
            if (direction.getAxis().isHorizontal()) {
                // For cocoa beans, FACING points TOWARD the log
                blockState = blockState.with(FACING, direction);
                if (blockState.canPlaceAt(worldView, blockPos)) {
                    return blockState;
                }
            }
        }
        return null;
    }
    
    // Using the non-deprecated method for neighbor updates in 1.20.1
    @Override
    public BlockState getStateForNeighborUpdate(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            WorldAccess world,
            BlockPos pos,
            BlockPos neighborPos) {
        // If the supporting block is removed, break this block
        // FACING points TOWARD the log, so we check if that direction is the one that changed
        if (direction == state.get(FACING) && !state.canPlaceAt(world, pos)) {
            // Drop the item before breaking the block to avoid NPE
            if (!world.isClient()) {
                ItemStack itemStack = new ItemStack(ModItems.SEKHEM_DATE);
                Block.dropStack((World) world, pos, itemStack);
            }
            return Blocks.AIR.getDefaultState(); // Return AIR instead of null to avoid NPE
        }
        return state;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        // Return the appropriate shape based on the facing direction
        switch (state.get(FACING)) {
            case SOUTH:
                return SOUTH_SHAPE;
            case WEST:
                return WEST_SHAPE;
            case EAST:
                return EAST_SHAPE;
            case NORTH:
            default:
                return NORTH_SHAPE;
        }
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, AGE);
    }
    
    @Override
    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        return false;
    }
    
    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        return new ItemStack(ModItems.SEKHEM_DATE);
    }
    
    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient && !player.isCreative()) {
            // Drop the Sekhem Date item when the block is broken
            ItemStack itemStack = new ItemStack(ModItems.SEKHEM_DATE);
            Block.dropStack(world, pos, itemStack);
        }
        super.onBreak(world, pos, state, player);
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        // If the date is fully grown (age 2), harvest it
        if (state.get(AGE) == 2) {
            // Drop the Sekhem Date item
            ItemStack itemStack = new ItemStack(ModItems.SEKHEM_DATE);
            Block.dropStack(world, pos, itemStack);
            
            // Reset the date block to age 0
            world.setBlockState(pos, state.with(AGE, 0), Block.NOTIFY_LISTENERS);
            return ActionResult.success(world.isClient);
        }
        
        return ActionResult.PASS;
    }
    
    // Add random tick method for growth
    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        int age = state.get(AGE);
        // Only grow if not fully grown
        if (age < 2) {
            // 25% chance to grow each random tick
            if (random.nextInt(4) == 0) {
                // Increment the age
                world.setBlockState(pos, state.with(AGE, age + 1), Block.NOTIFY_LISTENERS);
            }
        }
    }
}
