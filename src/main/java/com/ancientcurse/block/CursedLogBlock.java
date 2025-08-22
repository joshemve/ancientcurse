package com.ancientcurse.block;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.ModBlocks;
import com.ancientcurse.system.CursedEarthManager;
import com.ancientcurse.block.SolarSpireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PillarBlock;
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
 * Cursed Log - A corrupted version of wood logs that spreads the curse
 * Uses pillar block for proper rotation support with side and top textures
 */
public class CursedLogBlock extends PillarBlock {
    
    public CursedLogBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, BlockView world, List<Text> tooltip, TooltipContext options) {
        // No tooltip needed - the mod name shows automatically
        super.appendTooltip(stack, world, tooltip, options);
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
        if (random.nextFloat() < 0.25f) {
            attemptSpread(world, pos, state, random);
        }
    }
    
    /**
     * Attempts to spread the curse to nearby wood blocks
     */
    private void attemptSpread(ServerWorld world, BlockPos pos, BlockState state, Random random) {
        // Try to spread to adjacent logs or planks
        for (Direction dir : Direction.values()) {
            if (random.nextFloat() < 0.3f) { // Moderate chance per direction
                BlockPos targetPos = pos.offset(dir);
                BlockState targetState = world.getBlockState(targetPos);
                
                if (canCorrupt(targetState)) {
                    // Check for cleansing protection
                    if (SolarSpireBlock.isInActiveCleansingZone(targetPos)) {
                        continue;
                    }
                    
                    // Determine what to convert to
                    BlockState newState;
                    if (isLogBlock(targetState)) {
                        // Preserve the axis orientation if it's a log
                        newState = ModBlocks.CURSED_LOG.getDefaultState();
                        if (targetState.contains(AXIS)) {
                            newState = newState.with(AXIS, targetState.get(AXIS));
                        }
                    } else if (isPlankBlock(targetState)) {
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
        return isLogBlock(state) || isPlankBlock(state);
    }
    
    /**
     * Detect if a block is any type of log (vanilla or modded)
     */
    public static boolean isLogBlock(BlockState state) {
        Block block = state.getBlock();
        String blockName = block.getTranslationKey().toLowerCase();
        
        // Check if it's a PillarBlock (most logs extend this)
        if (block instanceof PillarBlock) {
            // Additional checks to ensure it's actually a log
            if (blockName.contains("log") || blockName.contains("stem") || 
                blockName.contains("wood") || blockName.contains("hyphae")) {
                // Exclude our cursed log
                return block != ModBlocks.CURSED_LOG;
            }
        }
        
        // Fallback name-based detection for mods that don't use PillarBlock
        if (blockName.contains("log") || blockName.contains("trunk") || 
            blockName.contains("stem") || blockName.contains("wood")) {
            // Make sure it's not a crafted item or machine
            if (!blockName.contains("logged") && !blockName.contains("catalog") &&
                !blockName.contains("dialogue") && !blockName.contains("backlog")) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Detect if a block is any type of plank (vanilla or modded)
     */
    public static boolean isPlankBlock(BlockState state) {
        Block block = state.getBlock();
        
        // Exclude our cursed plank first
        if (block == ModBlocks.CURSED_WOOD_PLANK) {
            return false;
        }
        
        // Check vanilla planks directly
        if (block == net.minecraft.block.Blocks.OAK_PLANKS ||
            block == net.minecraft.block.Blocks.SPRUCE_PLANKS ||
            block == net.minecraft.block.Blocks.BIRCH_PLANKS ||
            block == net.minecraft.block.Blocks.JUNGLE_PLANKS ||
            block == net.minecraft.block.Blocks.ACACIA_PLANKS ||
            block == net.minecraft.block.Blocks.DARK_OAK_PLANKS ||
            block == net.minecraft.block.Blocks.MANGROVE_PLANKS ||
            block == net.minecraft.block.Blocks.CHERRY_PLANKS ||
            block == net.minecraft.block.Blocks.BAMBOO_PLANKS ||
            block == net.minecraft.block.Blocks.CRIMSON_PLANKS ||
            block == net.minecraft.block.Blocks.WARPED_PLANKS) {
            return true;
        }
        
        // Check for modded planks by translation key
        String blockName = block.getTranslationKey().toLowerCase();
        
        // Most planks have "plank" in their name
        if (blockName.contains("plank")) {
            return true;
        }
        
        // Some mods might use "wood" instead of "planks"
        if (blockName.contains("_wood") && !blockName.contains("log") && 
            !blockName.contains("stem") && !blockName.contains("stripped")) {
            return true;
        }
        
        return false;
    }
}