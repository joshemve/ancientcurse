package com.ancientcurse.event;

import com.ancientcurse.ModItems;
import com.ancientcurse.item.WaterSkinItem;
import com.ancientcurse.player.ThirstDataManager;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

/**
 * Handles water-related interactions for the thirst system.
 * - Refilling water skins at water sources
 * - Drinking from water bottles restores thirst
 */
public class WaterInteractionHandler {

    public static void register() {
        // Handle right-clicking on/near water blocks with water skin
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            ItemStack stack = player.getStackInHand(hand);

            // Check if holding a water skin
            if (stack.isOf(ModItems.WATER_SKIN)) {
                // Check the block we hit and the block at the hit position (for water)
                BlockPos hitPos = hitResult.getBlockPos();
                BlockPos offsetPos = hitResult.getBlockPos().offset(hitResult.getSide());

                boolean isWaterAtHit = isWaterSource(world, hitPos);
                boolean isWaterAtOffset = isWaterSource(world, offsetPos);

                if (isWaterAtHit || isWaterAtOffset) {
                    // Fill the water skin
                    if (!WaterSkinItem.isFull(stack)) {
                        // Only modify on server side
                        if (!world.isClient) {
                            WaterSkinItem.fill(stack);
                        }

                        // Play fill sound on both sides
                        world.playSound(player, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ITEM_BUCKET_FILL, SoundCategory.PLAYERS,
                                1.0f, 1.0f);

                        if (world.isClient) {
                            player.sendMessage(Text.literal("Water skin filled!")
                                    .formatted(Formatting.AQUA), true);
                        }

                        // Swing arm
                        player.swingHand(hand);
                        return ActionResult.SUCCESS;
                    } else {
                        if (world.isClient) {
                            player.sendMessage(Text.literal("Water skin is already full.")
                                    .formatted(Formatting.YELLOW), true);
                        }
                        return ActionResult.FAIL;
                    }
                }
            }

            return ActionResult.PASS;
        });

        // Also handle UseItemCallback for when player right-clicks while looking at water
        // This catches cases where UseBlockCallback doesn't fire (like clicking through water)
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);

            // Check if holding a water skin
            if (stack.isOf(ModItems.WATER_SKIN)) {
                // Check if player is standing in water or looking at water nearby
                BlockPos playerPos = player.getBlockPos();

                // Check blocks around and below player
                boolean nearWater = isWaterSource(world, playerPos) ||
                        isWaterSource(world, playerPos.down()) ||
                        isWaterSource(world, playerPos.north()) ||
                        isWaterSource(world, playerPos.south()) ||
                        isWaterSource(world, playerPos.east()) ||
                        isWaterSource(world, playerPos.west());

                if (nearWater && !WaterSkinItem.isFull(stack)) {
                    // Only modify on server side
                    if (!world.isClient) {
                        WaterSkinItem.fill(stack);
                    }

                    // Play fill sound
                    world.playSound(player, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ITEM_BUCKET_FILL, SoundCategory.PLAYERS,
                            1.0f, 1.0f);

                    if (world.isClient) {
                        player.sendMessage(Text.literal("Water skin filled!")
                                .formatted(Formatting.AQUA), true);
                    }

                    player.swingHand(hand);
                    return TypedActionResult.success(stack);
                }
            }

            return TypedActionResult.pass(stack);
        });
    }

    /**
     * Check if a position contains a water source.
     */
    private static boolean isWaterSource(net.minecraft.world.World world, BlockPos pos) {
        var blockState = world.getBlockState(pos);
        var fluidState = world.getFluidState(pos);

        // Check for water fluid (still or flowing)
        if (fluidState.getFluid() == Fluids.WATER || fluidState.getFluid() == Fluids.FLOWING_WATER) {
            return true;
        }

        // Check for water cauldron
        if (blockState.isOf(Blocks.WATER_CAULDRON)) {
            return true;
        }

        // Check for waterlogged blocks
        if (fluidState.isOf(Fluids.WATER)) {
            return true;
        }

        return false;
    }

    /**
     * Called when a player drinks a water bottle.
     * This should be called from a mixin on PotionItem.
     */
    public static void onDrinkWaterBottle(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            // Water bottles restore moderate thirst
            ThirstDataManager.restoreThirst(serverPlayer, 4, 2.0f);
        }
    }

    /**
     * Called when a player drinks any potion.
     * Restores a small amount of thirst since you're drinking liquid.
     */
    public static void onDrinkPotion(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            // Potions restore small thirst
            ThirstDataManager.restoreThirst(serverPlayer, 2, 1.0f);
        }
    }
}
