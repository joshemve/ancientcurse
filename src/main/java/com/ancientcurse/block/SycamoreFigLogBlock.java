package com.ancientcurse.block;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PillarBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * A special log block that grows figs over time and can be harvested.
 * Extends PillarBlock so it supports the log AXIS property for proper tree generation.
 */
public class SycamoreFigLogBlock extends PillarBlock {
    // Define the growth stage property (0-3)
    public static final IntProperty STAGE = IntProperty.of("stage", 0, 3);
    
    // Chance to spread figs to adjacent logs
    private static final float SPREAD_CHANCE = 0.05f; // 5% chance per random tick
    
    // Maximum distance to check for spreading
    private static final int MAX_SPREAD_DISTANCE = 3;

    public SycamoreFigLogBlock(Settings settings) {
        super(settings);
        // Default state: axis Y and stage 0
        this.setDefaultState(this.getStateManager().getDefaultState()
            .with(AXIS, Direction.Axis.Y)
            .with(STAGE, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        // Add AXIS (from PillarBlock) and STAGE properties
        builder.add(AXIS, STAGE);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(AXIS, ctx.getSide().getAxis());
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        int currentStage = state.get(STAGE);
        
        // Only grow if not fully ripe (stage < 3)
        if (currentStage < 3 && random.nextFloat() < 0.1f) { // 10% chance to grow
            // Increment the stage
            world.setBlockState(pos, state.with(STAGE, currentStage + 1), Block.NOTIFY_LISTENERS);
            AncientCurse.LOGGER.debug("Sycamore fig log at " + pos + " grew to stage " + (currentStage + 1));
        }
        
        // Try to spread figs to adjacent logs if this log has figs (stage > 0)
        if (currentStage > 0 && random.nextFloat() < SPREAD_CHANCE) {
            spreadFigsToAdjacentLogs(world, pos, random);
        }
    }
    
    /**
     * Attempts to spread figs to adjacent sycamore logs
     */
    private void spreadFigsToAdjacentLogs(ServerWorld world, BlockPos sourcePos, Random random) {
        // Get all potential target logs within spread distance
        List<BlockPos> targetLogs = findAdjacentLogs(world, sourcePos, MAX_SPREAD_DISTANCE);
        
        // If no valid targets found, return
        if (targetLogs.isEmpty()) {
            return;
        }
        
        // Select a random log to spread to
        BlockPos targetPos = targetLogs.get(random.nextInt(targetLogs.size()));
        BlockState targetState = world.getBlockState(targetPos);
        
        // Only spread to logs with a lower stage
        if (targetState.getBlock() instanceof SycamoreFigLogBlock) {
            int targetStage = targetState.get(STAGE);
            
            // Only spread if the target log has a lower stage
            if (targetStage < 3) {
                // Increment the stage of the target log
                world.setBlockState(targetPos, targetState.with(STAGE, targetStage + 1), Block.NOTIFY_LISTENERS);
                AncientCurse.LOGGER.debug("Figs spread from " + sourcePos + " to " + targetPos + ", new stage: " + (targetStage + 1));
            }
        }
    }
    
    /**
     * Finds adjacent sycamore logs within the specified distance
     */
    private List<BlockPos> findAdjacentLogs(ServerWorld world, BlockPos sourcePos, int maxDistance) {
        List<BlockPos> adjacentLogs = new ArrayList<>();
        
        // Check all directions
        for (Direction direction : Direction.values()) {
            // Check up to maxDistance blocks in each direction
            for (int distance = 1; distance <= maxDistance; distance++) {
                BlockPos checkPos = sourcePos.offset(direction, distance);
                BlockState checkState = world.getBlockState(checkPos);
                
                // If we found a sycamore log with a lower stage, add it to the list
                if (checkState.getBlock() instanceof SycamoreFigLogBlock) {
                    int checkStage = checkState.get(STAGE);
                    if (checkStage < 3) {
                        adjacentLogs.add(checkPos);
                    }
                } else {
                    // If we hit a non-log block, stop checking in this direction
                    break;
                }
            }
        }
        
        return adjacentLogs;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        int currentStage = state.get(STAGE);

        // Only allow harvesting at stage 3 (fully ripe)
        if (!world.isClient() && currentStage == 3) {
            // Create a sycamore fig item
            ItemStack figStack = new ItemStack(ModItems.SYCAMORE_FIG, 1);
            
            // Spawn the fig item in the world
            ItemEntity figEntity = new ItemEntity(
                world, 
                pos.getX() + 0.5, 
                pos.getY() + 0.5, 
                pos.getZ() + 0.5, 
                figStack
            );
            world.spawnEntity(figEntity);

            // Reset the block to stage 0 after harvesting
            world.setBlockState(pos, state.with(STAGE, 0), Block.NOTIFY_LISTENERS);
            AncientCurse.LOGGER.debug("Player harvested sycamore fig at " + pos);

            return ActionResult.SUCCESS;
        }

        // If not ripe or on client side, do nothing
        return ActionResult.PASS;
    }
}
