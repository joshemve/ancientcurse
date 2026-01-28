package com.ancientcurse.system;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.network.FaminePackets;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import net.minecraft.util.math.random.Random;
import java.util.*;

/**
 * Handles food spoilage in player inventories during the Famine event.
 *
 * Food items gradually spoil based on:
 * - Famine intensity
 * - Time in inventory (tracked via NBT)
 * - Food type (some foods spoil faster)
 *
 * Performance optimizations:
 * - Only checks periodically (configured in FamineData)
 * - Processes one player at a time spread across ticks
 * - Uses efficient item checks
 */
public class FamineFoodSpoilageHandler {

    // NBT key for tracking spoilage timer
    private static final String SPOILAGE_TIMER_KEY = "ancientcurse_spoilage";
    private static final String SPOILAGE_START_KEY = "ancientcurse_spoilage_start";

    // Spoilage ticks before food is destroyed (base value, modified by intensity)
    private static final int BASE_SPOILAGE_TICKS = 6000; // 5 minutes at full intensity

    // Foods that spoil quickly
    private static final Set<Item> FAST_SPOILING = new HashSet<>();
    // Foods that spoil slowly
    private static final Set<Item> SLOW_SPOILING = new HashSet<>();
    // Foods immune to spoilage
    private static final Set<Item> IMMUNE_FOODS = new HashSet<>();

    // Player processing queue
    private static final Queue<UUID> playerQueue = new LinkedList<>();
    private static int tickCounter = 0;

    static {
        // Fast spoiling foods (meat, fish, etc.)
        FAST_SPOILING.add(Items.BEEF);
        FAST_SPOILING.add(Items.PORKCHOP);
        FAST_SPOILING.add(Items.CHICKEN);
        FAST_SPOILING.add(Items.MUTTON);
        FAST_SPOILING.add(Items.RABBIT);
        FAST_SPOILING.add(Items.COD);
        FAST_SPOILING.add(Items.SALMON);
        FAST_SPOILING.add(Items.TROPICAL_FISH);
        FAST_SPOILING.add(Items.PUFFERFISH);
        FAST_SPOILING.add(Items.RABBIT_STEW);
        FAST_SPOILING.add(Items.BEETROOT_SOUP);
        FAST_SPOILING.add(Items.MUSHROOM_STEW);
        FAST_SPOILING.add(Items.SUSPICIOUS_STEW);
        FAST_SPOILING.add(Items.MILK_BUCKET);

        // Slow spoiling foods (dried, preserved, etc.)
        SLOW_SPOILING.add(Items.BREAD);
        SLOW_SPOILING.add(Items.COOKIE);
        SLOW_SPOILING.add(Items.DRIED_KELP);
        SLOW_SPOILING.add(Items.COOKED_BEEF);
        SLOW_SPOILING.add(Items.COOKED_PORKCHOP);
        SLOW_SPOILING.add(Items.COOKED_CHICKEN);
        SLOW_SPOILING.add(Items.COOKED_MUTTON);
        SLOW_SPOILING.add(Items.COOKED_RABBIT);
        SLOW_SPOILING.add(Items.COOKED_COD);
        SLOW_SPOILING.add(Items.COOKED_SALMON);
        SLOW_SPOILING.add(Items.BAKED_POTATO);

        // Immune foods (magical, non-perishable)
        IMMUNE_FOODS.add(Items.GOLDEN_APPLE);
        IMMUNE_FOODS.add(Items.ENCHANTED_GOLDEN_APPLE);
        IMMUNE_FOODS.add(Items.GOLDEN_CARROT);
        IMMUNE_FOODS.add(Items.HONEY_BOTTLE);
        IMMUNE_FOODS.add(Items.ROTTEN_FLESH); // Already rotten
        IMMUNE_FOODS.add(Items.SPIDER_EYE); // Not really food
        IMMUNE_FOODS.add(Items.CHORUS_FRUIT); // Magical
    }

    /**
     * Called periodically from FamineData.tick() to check all players
     */
    public static void checkAllPlayers(ServerWorld world, FamineData data, FamineConfig config) {
        if (!data.isActive()) {
            return;
        }

        // Add all current players to queue if empty
        if (playerQueue.isEmpty()) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                playerQueue.add(player.getUuid());
            }
        }

        // Process one player per call (spread load)
        if (!playerQueue.isEmpty()) {
            UUID playerId = playerQueue.poll();
            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(playerId);

            if (player != null && player.getWorld() == world) {
                processPlayerInventory(player, data, config);
            }
        }
    }

    /**
     * Process a single player's inventory for spoilage
     */
    private static void processPlayerInventory(ServerPlayerEntity player, FamineData data, FamineConfig config) {
        PlayerInventory inventory = player.getInventory();
        float intensity = data.getIntensity();
        float spoilageChance = data.getSpoilageChance();
        float spoilageSpeedMultiplier = config.getSpoilageSpeedMultiplier();
        int baseSpoilageTicks = config.getBaseSpoilageTicks();
        Random random = player.getRandom();

        List<Integer> spoiledSlots = new ArrayList<>();
        int totalSpoiled = 0;

        // Check main inventory
        for (int i = 0; i < inventory.main.size(); i++) {
            ItemStack stack = inventory.main.get(i);
            int spoiled = processStack(stack, intensity, spoilageChance, spoilageSpeedMultiplier, baseSpoilageTicks, random);
            if (spoiled > 0) {
                spoiledSlots.add(i);
                totalSpoiled += spoiled;
            }
        }

        // Check offhand
        ItemStack offhand = inventory.offHand.get(0);
        int spoiled = processStack(offhand, intensity, spoilageChance, spoilageSpeedMultiplier, baseSpoilageTicks, random);
        if (spoiled > 0) {
            spoiledSlots.add(-1); // Special marker for offhand
            totalSpoiled += spoiled;
        }

        // Notify client if food spoiled
        if (totalSpoiled > 0 && !spoiledSlots.isEmpty()) {
            // Send packet for the first spoiled slot
            int firstSlot = spoiledSlots.get(0);
            FaminePackets.sendFoodSpoiledPacket(player, firstSlot, totalSpoiled);

            AncientCurse.LOGGER.debug("Player {} had {} food items spoil",
                player.getName().getString(), totalSpoiled);
        }
    }

    /**
     * Process a single item stack for spoilage
     * @return Number of items that spoiled
     */
    private static int processStack(ItemStack stack, float intensity, float spoilageChance,
                                     float spoilageSpeedMultiplier, int baseSpoilageTicks, Random random) {
        if (stack.isEmpty()) {
            return 0;
        }

        Item item = stack.getItem();

        // Check if this is a food item
        if (!isFoodItem(item)) {
            return 0;
        }

        // Check immunity
        if (IMMUNE_FOODS.contains(item)) {
            return 0;
        }

        // Get or initialize spoilage timer
        NbtCompound nbt = stack.getOrCreateNbt();
        int spoilageTimer = nbt.getInt(SPOILAGE_TIMER_KEY);

        // Calculate spoilage rate multiplier
        float foodTypeMultiplier = 1.0f;
        if (FAST_SPOILING.contains(item)) {
            foodTypeMultiplier = 2.0f;
        } else if (SLOW_SPOILING.contains(item)) {
            foodTypeMultiplier = 0.5f;
        }

        // Increment spoilage timer based on intensity and config multipliers
        int increment = (int)(intensity * spoilageSpeedMultiplier * foodTypeMultiplier * 20); // Base increment per check
        spoilageTimer += increment;
        nbt.putInt(SPOILAGE_TIMER_KEY, spoilageTimer);

        // Calculate threshold for spoilage using config value
        int spoilageThreshold = (int)(baseSpoilageTicks / Math.max(0.1f, intensity));

        // Check if food should spoil
        if (spoilageTimer >= spoilageThreshold) {
            // Reset timer
            nbt.putInt(SPOILAGE_TIMER_KEY, 0);

            // Determine how many items spoil (at least 1)
            int maxSpoil = Math.max(1, (int)(stack.getCount() * spoilageChance));
            int spoilCount = 1 + random.nextInt(Math.max(1, maxSpoil));
            spoilCount = Math.min(spoilCount, stack.getCount());

            // Spoil the items
            int originalCount = stack.getCount();
            stack.decrement(spoilCount);

            // If stack is now empty, remove NBT (handled by Minecraft)
            // Otherwise update count

            return spoilCount;
        }

        // Random chance for immediate spoilage (simulates varying food quality)
        if (random.nextFloat() < spoilageChance * 0.1f * intensity) {
            int spoilCount = 1;
            stack.decrement(spoilCount);
            return spoilCount;
        }

        return 0;
    }

    /**
     * Check if an item is a food item (has food component)
     */
    private static boolean isFoodItem(Item item) {
        return item.getFoodComponent() != null;
    }

    /**
     * Get the spoilage progress for an item stack (0.0 to 1.0)
     * Used for rendering spoilage indicators
     */
    public static float getSpoilageProgress(ItemStack stack, float intensity) {
        if (stack.isEmpty() || !isFoodItem(stack.getItem())) {
            return 0.0f;
        }

        if (IMMUNE_FOODS.contains(stack.getItem())) {
            return 0.0f;
        }

        NbtCompound nbt = stack.getNbt();
        if (nbt == null) {
            return 0.0f;
        }

        int spoilageTimer = nbt.getInt(SPOILAGE_TIMER_KEY);
        int spoilageThreshold = (int)(BASE_SPOILAGE_TICKS / Math.max(0.1f, intensity));

        return Math.min(1.0f, (float)spoilageTimer / spoilageThreshold);
    }

    /**
     * Reset all spoilage timers on a player's food (when famine ends)
     */
    public static void clearSpoilageTimers(ServerPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();

        // Clear main inventory
        for (ItemStack stack : inventory.main) {
            clearSpoilageNbt(stack);
        }

        // Clear offhand
        clearSpoilageNbt(inventory.offHand.get(0));
    }

    /**
     * Clear spoilage NBT from a stack
     */
    private static void clearSpoilageNbt(ItemStack stack) {
        if (stack.isEmpty()) return;

        NbtCompound nbt = stack.getNbt();
        if (nbt != null) {
            nbt.remove(SPOILAGE_TIMER_KEY);
            nbt.remove(SPOILAGE_START_KEY);

            // Remove NBT entirely if empty
            if (nbt.isEmpty()) {
                stack.setNbt(null);
            }
        }
    }

    /**
     * Reset handler state
     */
    public static void reset() {
        playerQueue.clear();
        tickCounter = 0;
    }

    /**
     * Add an item to the immune foods set (for mod integration)
     */
    public static void addImmunFood(Item item) {
        IMMUNE_FOODS.add(item);
    }

    /**
     * Add an item to fast spoiling set
     */
    public static void addFastSpoilingFood(Item item) {
        FAST_SPOILING.add(item);
    }

    /**
     * Add an item to slow spoiling set
     */
    public static void addSlowSpoilingFood(Item item) {
        SLOW_SPOILING.add(item);
    }
}
