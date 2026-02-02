package com.ancientcurse.item;

import com.ancientcurse.player.ThirstDataManager;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

import net.minecraft.client.item.TooltipContext;
import java.util.List;

/**
 * A refillable water container for the Ancient Curse desert mod.
 * Can hold multiple uses of water and be refilled at water sources.
 *
 * Right-click to drink when thirsty.
 * Right-click on water source while sneaking to refill.
 */
public class WaterSkinItem extends Item {

    private static final int MAX_USES = 5;
    private static final int THIRST_RESTORED_PER_USE = 6; // 3 droplets per drink
    private static final float SATURATION_RESTORED_PER_USE = 4.0f;
    private static final String USES_KEY = "WaterUses";

    public WaterSkinItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        int uses = getWaterUses(stack);

        // Check if we have water and player needs it
        if (uses > 0) {
            // Check thirst level - use appropriate method for client/server
            int currentThirst;
            if (world.isClient) {
                // Client-side: use cached value for immediate feedback
                currentThirst = ThirstDataManager.getClientThirstValue();
            } else {
                // Server-side: use actual player data (authoritative)
                currentThirst = ThirstDataManager.getThirstValue(user);
            }

            // Don't drink if already at max thirst
            if (currentThirst >= ThirstDataManager.MAX_THIRST) {
                return TypedActionResult.pass(stack);
            }

            // Start drinking animation
            // UseAction.DRINK + ItemUsage.consumeHeldItem automatically syncs the third-person animation
            // Other players will see: head tilted back, arm raised, drinking motion for 32 ticks
            return ItemUsage.consumeHeldItem(world, user, hand);
        }

        // No water - play empty container sound to signal player should refill
        world.playSound(user, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ITEM_BUNDLE_DROP_CONTENTS, SoundCategory.PLAYERS,
                0.8f, 0.5f); // Low pitch indicates empty/failure
        return TypedActionResult.fail(stack);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (user instanceof PlayerEntity player) {
            int uses = getWaterUses(stack);

            if (uses > 0) {
                // Server-side only for the actual effect
                if (!world.isClient) {
                    // Restore thirst
                    ThirstDataManager.restoreThirst(player, THIRST_RESTORED_PER_USE, SATURATION_RESTORED_PER_USE);

                    // Decrease uses
                    setWaterUses(stack, uses - 1);

                    // Play drinking sound
                    world.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS,
                            0.5f, world.random.nextFloat() * 0.1f + 0.9f);

                    // Award stat
                    player.incrementStat(Stats.USED.getOrCreateStat(this));

                    // Criteria for advancements
                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        Criteria.CONSUME_ITEM.trigger(serverPlayer, stack);
                    }
                }
            }
        }
        return stack;
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 32; // Same as eating
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    /**
     * Get the number of water uses remaining in this water skin.
     */
    public static int getWaterUses(ItemStack stack) {
        if (stack.getNbt() != null && stack.getNbt().contains(USES_KEY)) {
            return stack.getNbt().getInt(USES_KEY);
        }
        return 0; // Empty by default
    }

    /**
     * Set the number of water uses in this water skin.
     */
    public static void setWaterUses(ItemStack stack, int uses) {
        stack.getOrCreateNbt().putInt(USES_KEY, Math.max(0, Math.min(MAX_USES, uses)));
    }

    /**
     * Fill the water skin completely.
     */
    public static void fill(ItemStack stack) {
        setWaterUses(stack, MAX_USES);
    }

    /**
     * Check if the water skin is full.
     */
    public static boolean isFull(ItemStack stack) {
        return getWaterUses(stack) >= MAX_USES;
    }

    /**
     * Check if the water skin is empty.
     */
    public static boolean isEmpty(ItemStack stack) {
        return getWaterUses(stack) <= 0;
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        // Always show the bar so players can see water level
        return true;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        // Scale to 13 (max bar size)
        int uses = getWaterUses(stack);
        return Math.round(13.0f * uses / MAX_USES);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        int uses = getWaterUses(stack);
        if (uses == 0) {
            // Red when empty
            return 0xFF5555;
        } else if (uses <= 2) {
            // Orange/yellow when low
            return 0xFFAA00;
        } else {
            // Blue when has water
            return 0x3399FF;
        }
    }

    /**
     * Get the maximum number of uses.
     */
    public static int getMaxUses() {
        return MAX_USES;
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        int uses = getWaterUses(stack);
        if (uses == 0) {
            tooltip.add(Text.literal("Empty - refill at water source").formatted(Formatting.RED));
        } else {
            tooltip.add(Text.literal("Water: " + uses + "/" + MAX_USES).formatted(Formatting.AQUA));
        }
        super.appendTooltip(stack, world, tooltip, context);
    }
}
