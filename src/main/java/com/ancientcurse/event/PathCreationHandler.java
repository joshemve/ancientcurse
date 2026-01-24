package com.ancientcurse.event;

import com.ancientcurse.block.registry.ConstructionBlocks;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Handles the creation of sand and sandstone paths when right-clicking with a
 * shovel
 */
public class PathCreationHandler {

    /**
     * Registers the path creation event handler
     */
    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            // Only handle main hand interactions
            if (hand != Hand.MAIN_HAND) {
                return ActionResult.PASS;
            }

            ItemStack itemStack = player.getStackInHand(hand);

            // Check if the player is holding a shovel
            if (!itemStack.isIn(ItemTags.SHOVELS)) {
                return ActionResult.PASS;
            }

            BlockPos pos = hitResult.getBlockPos();
            BlockState state = world.getBlockState(pos);
            Block block = state.getBlock();

            // Check if we can create a path from this block
            BlockState pathState = getPathState(block);
            if (pathState == null) {
                return ActionResult.PASS;
            }

            // Don't convert if there's a solid block above
            BlockState stateAbove = world.getBlockState(pos.up());
            if (stateAbove.isSolid()
                    && stateAbove.isSideSolidFullSquare(world, pos.up(), net.minecraft.util.math.Direction.DOWN)) {
                return ActionResult.PASS;
            }

            // Server-side conversion
            if (!world.isClient) {
                // Convert the block to a path
                world.setBlockState(pos, pathState, Block.NOTIFY_ALL | Block.REDRAW_ON_MAIN_THREAD);

                // Play sound
                world.playSound(
                        null,
                        pos,
                        SoundEvents.ITEM_SHOVEL_FLATTEN,
                        SoundCategory.BLOCKS,
                        1.0f,
                        1.0f);

                // Damage the shovel
                itemStack.damage(1, player, (p) -> p.sendToolBreakStatus(hand));
            }

            return ActionResult.SUCCESS;
        });
    }

    /**
     * Returns the appropriate path state for a given block, or null if no path
     * variant exists
     */
    private static BlockState getPathState(Block block) {
        // Sand -> Desert Path
        if (block == Blocks.SAND || block == com.ancientcurse.ModBlocks.SMOOTH_SAND) {
            return ConstructionBlocks.DESERT_PATH.getDefaultState();
        }

        // Red Sand -> Sand Path (using same sand path for red sand too)
        if (block == Blocks.RED_SAND) {
            return ConstructionBlocks.SAND_PATH.getDefaultState();
        }

        // Black Sand -> Black Sand Path
        if (block == com.ancientcurse.ModBlocks.BLACK_SAND) {
            return ConstructionBlocks.BLACK_SAND_PATH.getDefaultState();
        }

        // Sandstone -> Sandstone Path
        if (block == Blocks.SANDSTONE ||
                block == Blocks.SMOOTH_SANDSTONE ||
                block == Blocks.CUT_SANDSTONE ||
                block == Blocks.CHISELED_SANDSTONE) {
            return ConstructionBlocks.SANDSTONE_PATH.getDefaultState();
        }

        // Red Sandstone -> Sandstone Path (using same sandstone path)
        if (block == Blocks.RED_SANDSTONE ||
                block == Blocks.SMOOTH_RED_SANDSTONE ||
                block == Blocks.CUT_RED_SANDSTONE ||
                block == Blocks.CHISELED_RED_SANDSTONE) {
            return ConstructionBlocks.SANDSTONE_PATH.getDefaultState();
        }

        // Deshret Sand -> Deshret Path
        if (block == com.ancientcurse.ModBlocks.DESHRET_SAND ||
                block == com.ancientcurse.ModBlocks.DESHRET_WAVY_SAND) {
            return ConstructionBlocks.DESHRET_PATH.getDefaultState();
        }

        return null;
    }
}
