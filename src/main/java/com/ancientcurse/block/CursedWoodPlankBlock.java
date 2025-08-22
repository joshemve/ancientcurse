package com.ancientcurse.block;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.ModBlocks;
import com.ancientcurse.system.CursedEarthManager;
import com.ancientcurse.block.SolarSpireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.List;

/**
 * Cursed Wood Plank - A corrupted version of wooden planks
 * Spreads to other wood blocks at a slow rate
 */
public class CursedWoodPlankBlock extends BaseAncientCurseBlock {
    
    public CursedWoodPlankBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, BlockView world, List<Text> tooltip, TooltipContext options) {
        // No tooltip needed - the mod name shows automatically
    }
    
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        // Only handle removal if the block is actually being replaced with a different type
        if (!state.isOf(newState.getBlock()) && !world.isClient) {
            // Notify CursedEarthManager that a cursed block was removed
            CursedEarthManager.getInstance().onCursedBlockRemoved((ServerWorld) world, pos);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
    
    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // Check if we're in an active cleansing zone
        if (SolarSpireBlock.isInActiveCleansingZone(pos)) {
            return;
        }
        
        // Similar spread rate to cursed earth for consistent corruption
        if (random.nextFloat() < 0.3f) {
            attemptSpread(world, pos, random);
        }
    }
    
    /**
     * Attempts to spread the curse to nearby wood blocks
     */
    private void attemptSpread(ServerWorld world, BlockPos pos, Random random) {
        // Try to spread to adjacent wood blocks
        for (Direction dir : Direction.values()) {
            if (random.nextFloat() < 0.35f) { // Higher chance per direction for faster spread
                BlockPos targetPos = pos.offset(dir);
                BlockState targetState = world.getBlockState(targetPos);
                
                if (canCorrupt(targetState)) {
                    // Check for cleansing protection
                    if (SolarSpireBlock.isInActiveCleansingZone(targetPos)) {
                        continue;
                    }
                    
                    // Determine what to convert to
                    BlockState newState;
                    if (CursedLogBlock.isLogBlock(targetState)) {
                        // Convert logs to cursed logs, preserving axis
                        newState = ModBlocks.CURSED_LOG.getDefaultState();
                        if (targetState.contains(net.minecraft.block.PillarBlock.AXIS)) {
                            newState = newState.with(net.minecraft.block.PillarBlock.AXIS, 
                                                    targetState.get(net.minecraft.block.PillarBlock.AXIS));
                        }
                    } else if (CursedLogBlock.isPlankBlock(targetState)) {
                        // Convert planks to cursed planks
                        newState = ModBlocks.CURSED_WOOD_PLANK.getDefaultState();
                    } else {
                        continue; // Skip non-wood blocks
                    }
                    
                    // Queue the spread
                    CursedEarthManager.getInstance().queueSpread(world, pos, targetPos, newState);
                }
            }
        }
    }
    
    /**
     * Check if a block can be corrupted
     */
    private boolean canCorrupt(BlockState state) {
        Block block = state.getBlock();
        
        // Don't corrupt already cursed blocks
        if (block == ModBlocks.CURSED_LOG || block == ModBlocks.CURSED_WOOD_PLANK) {
            return false;
        }
        
        // Check if it's a log or plank
        return CursedLogBlock.isLogBlock(state) || CursedLogBlock.isPlankBlock(state);
    }
}