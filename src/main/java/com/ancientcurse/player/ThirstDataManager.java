package com.ancientcurse.player;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.config.ModConfig;
import com.ancientcurse.network.ThirstPackets;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

/**
 * Manages thirst values for players in the Ancient Curse desert mod.
 * Thirst depletes over time, especially in hot biomes, and can be restored by drinking.
 */
public class ThirstDataManager {
    private static final String THIRST_KEY = "ThirstValue";
    private static final String THIRST_SATURATION_KEY = "ThirstSaturation";
    private static final String THIRST_EXHAUSTION_KEY = "ThirstExhaustion";

    // Thirst constants
    public static final int MAX_THIRST = 20; // Same as hunger (20 = 10 droplets)
    public static final int DEFAULT_THIRST = 20;
    private static final float DEFAULT_SATURATION = 5.0f;
    private static final float MAX_SATURATION = 20.0f;

    // Custom damage type for dehydration
    public static final RegistryKey<DamageType> DEHYDRATION_DAMAGE = RegistryKey.of(
        RegistryKeys.DAMAGE_TYPE,
        new Identifier(AncientCurse.MOD_ID, "dehydration")
    );

    // Depletion rates (exhaustion required to lose 1 thirst point)
    private static final float EXHAUSTION_THRESHOLD = 4.0f;

    // Exhaustion gained from various actions
    private static final float SPRINT_EXHAUSTION = 0.1f;  // Per tick while sprinting
    private static final float JUMP_EXHAUSTION = 0.2f;    // Per jump
    private static final float SWIM_EXHAUSTION = 0.02f;   // Per tick while swimming
    private static final float DESERT_EXHAUSTION = 0.05f; // Per tick in desert biomes (additional)
    private static final float BASE_EXHAUSTION = 0.005f;  // Base exhaustion per tick

    // Thresholds for effects
    private static final int THIRST_WARNING_THRESHOLD = 6;  // Start showing warning effects
    private static final int THIRST_DANGER_THRESHOLD = 2;   // Severe dehydration

    // Client-side thirst value for display
    private static int clientThirstValue = DEFAULT_THIRST;
    private static float clientThirstSaturation = DEFAULT_SATURATION;

    /**
     * Get the current thirst value for a player.
     */
    public static int getThirstValue(PlayerEntity player) {
        if (player == null) return DEFAULT_THIRST;

        PlayerDataStorage storage = (PlayerDataStorage) player;
        NbtCompound data = storage.ancientcurse$getPersistentData();

        if (data.contains(THIRST_KEY)) {
            return data.getInt(THIRST_KEY);
        }
        return DEFAULT_THIRST;
    }

    /**
     * Get the thirst saturation for a player.
     */
    public static float getThirstSaturation(PlayerEntity player) {
        if (player == null) return DEFAULT_SATURATION;

        PlayerDataStorage storage = (PlayerDataStorage) player;
        NbtCompound data = storage.ancientcurse$getPersistentData();

        if (data.contains(THIRST_SATURATION_KEY)) {
            return data.getFloat(THIRST_SATURATION_KEY);
        }
        return DEFAULT_SATURATION;
    }

    /**
     * Get the thirst exhaustion for a player.
     */
    public static float getThirstExhaustion(PlayerEntity player) {
        if (player == null) return 0.0f;

        PlayerDataStorage storage = (PlayerDataStorage) player;
        NbtCompound data = storage.ancientcurse$getPersistentData();

        if (data.contains(THIRST_EXHAUSTION_KEY)) {
            return data.getFloat(THIRST_EXHAUSTION_KEY);
        }
        return 0.0f;
    }

    /**
     * Set the thirst value for a player.
     */
    public static void setThirstValue(PlayerEntity player, int value) {
        if (player == null) return;

        int clampedValue = Math.max(0, Math.min(MAX_THIRST, value));

        PlayerDataStorage storage = (PlayerDataStorage) player;
        NbtCompound data = storage.ancientcurse$getPersistentData();
        data.putInt(THIRST_KEY, clampedValue);

        // Sync to client
        if (player instanceof ServerPlayerEntity serverPlayer) {
            ThirstPackets.sendThirstToClient(serverPlayer, clampedValue, getThirstSaturation(player));
        }
    }

    /**
     * Set the thirst saturation for a player.
     */
    public static void setThirstSaturation(PlayerEntity player, float saturation) {
        if (player == null) return;

        float clampedValue = Math.max(0.0f, Math.min(MAX_SATURATION, saturation));

        PlayerDataStorage storage = (PlayerDataStorage) player;
        NbtCompound data = storage.ancientcurse$getPersistentData();
        data.putFloat(THIRST_SATURATION_KEY, clampedValue);
    }

    /**
     * Set the thirst exhaustion for a player.
     */
    private static void setThirstExhaustion(PlayerEntity player, float exhaustion) {
        if (player == null) return;

        PlayerDataStorage storage = (PlayerDataStorage) player;
        NbtCompound data = storage.ancientcurse$getPersistentData();
        data.putFloat(THIRST_EXHAUSTION_KEY, Math.max(0.0f, exhaustion));
    }

    /**
     * Add exhaustion to the player's thirst system.
     */
    public static void addExhaustion(PlayerEntity player, float amount) {
        if (player == null || player.isCreative() || player.isSpectator()) return;

        // Prevent negative exhaustion (which could be exploited)
        if (amount < 0) return;

        // Prevent overflow by capping at a reasonable max
        float currentExhaustion = getThirstExhaustion(player);
        float newExhaustion = Math.min(currentExhaustion + amount, EXHAUSTION_THRESHOLD * 100);

        // When exhaustion exceeds threshold, reduce saturation or thirst
        while (newExhaustion >= EXHAUSTION_THRESHOLD) {
            newExhaustion -= EXHAUSTION_THRESHOLD;

            float saturation = getThirstSaturation(player);
            if (saturation > 0) {
                // Reduce saturation first
                setThirstSaturation(player, Math.max(0, saturation - 1.0f));
            } else {
                // Then reduce thirst
                int thirst = getThirstValue(player);
                if (thirst > 0) {
                    setThirstValue(player, thirst - 1);
                }
            }
        }

        setThirstExhaustion(player, newExhaustion);
    }

    /**
     * Restore thirst for a player (when drinking).
     *
     * @param player The player
     * @param thirstRestored Amount of thirst to restore
     * @param saturationRestored Amount of saturation to restore
     */
    public static void restoreThirst(PlayerEntity player, int thirstRestored, float saturationRestored) {
        if (player == null) return;

        int currentThirst = getThirstValue(player);
        float currentSaturation = getThirstSaturation(player);

        // Restore thirst (capped at max)
        int newThirst = Math.min(MAX_THIRST, currentThirst + thirstRestored);
        setThirstValue(player, newThirst);

        // Restore saturation (capped at current thirst level, like hunger)
        float newSaturation = Math.min(newThirst, currentSaturation + saturationRestored);
        setThirstSaturation(player, newSaturation);

        // Sync to client
        if (player instanceof ServerPlayerEntity serverPlayer) {
            ThirstPackets.sendThirstToClient(serverPlayer, newThirst, newSaturation);
        }
    }

    /**
     * Called every tick to update thirst for a player.
     * Should be called from a server tick event.
     */
    public static void tickThirst(ServerPlayerEntity player) {
        if (player == null || player.isCreative() || player.isSpectator()) return;

        // Check if thirst system is enabled
        ModConfig config = ModConfig.get();
        if (!config.isThirstEnabled()) return;

        // Get drain speed multiplier from config
        float drainMultiplier = config.getThirstDrainMultiplier();
        if (drainMultiplier <= 0) return; // Drain disabled

        // Base exhaustion (scaled by config multiplier)
        addExhaustion(player, BASE_EXHAUSTION * drainMultiplier);

        // Additional exhaustion from sprinting
        if (player.isSprinting()) {
            addExhaustion(player, SPRINT_EXHAUSTION * drainMultiplier);
        }

        // Additional exhaustion from swimming
        if (player.isSwimming()) {
            addExhaustion(player, SWIM_EXHAUSTION * drainMultiplier);
        }

        // Additional exhaustion in hot/desert biomes
        if (isInHotBiome(player)) {
            float desertMultiplier = config.getDesertDrainMultiplier();
            addExhaustion(player, DESERT_EXHAUSTION * drainMultiplier * desertMultiplier);
        }

        // Apply dehydration effects
        applyDehydrationEffects(player);
    }

    /**
     * Called when player jumps to add exhaustion.
     */
    public static void onPlayerJump(ServerPlayerEntity player) {
        ModConfig config = ModConfig.get();
        if (!config.isThirstEnabled()) return;

        float drainMultiplier = config.getThirstDrainMultiplier();
        if (drainMultiplier <= 0) return;

        addExhaustion(player, JUMP_EXHAUSTION * drainMultiplier);
    }

    /**
     * Check if player is in a hot biome.
     */
    private static boolean isInHotBiome(PlayerEntity player) {
        World world = player.getWorld();
        var biome = world.getBiome(player.getBlockPos());

        // Check for desert-type biomes by temperature
        float temperature = biome.value().getTemperature();
        if (temperature >= 1.0f) {
            return true;
        }

        // Check for Ancient Curse desert biomes by name
        String biomeName = biome.getKey().map(key -> key.getValue().toString()).orElse("");
        return biomeName.contains("desert") ||
               biomeName.contains("ancient_curse") ||
               biomeName.contains("deshret") ||
               biomeName.contains("badlands");
    }

    /**
     * Apply effects based on dehydration level.
     */
    private static void applyDehydrationEffects(ServerPlayerEntity player) {
        int thirst = getThirstValue(player);

        if (thirst <= 0) {
            // Severe dehydration - take damage (like starvation)
            if (player.getWorld().getTime() % 80 == 0) { // Every 4 seconds
                // Only damage if health is above 1 (half heart) on easy,
                // above 0.5 on normal, can kill on hard
                float minHealth = switch (player.getWorld().getDifficulty()) {
                    case PEACEFUL -> 20.0f; // No damage in peaceful
                    case EASY -> 10.0f;
                    case NORMAL -> 1.0f;
                    case HARD -> 0.0f;
                };

                if (player.getHealth() > minHealth) {
                    // Use custom dehydration damage instead of vanilla starvation
                    DamageSource dehydrationSource = new DamageSource(
                        player.getWorld().getRegistryManager()
                            .get(RegistryKeys.DAMAGE_TYPE)
                            .entryOf(DEHYDRATION_DAMAGE)
                    );
                    player.damage(dehydrationSource, 1.0f);
                }
            }
        }

        if (thirst <= THIRST_DANGER_THRESHOLD) {
            // Severe dehydration effects - apply every 2 seconds to avoid spam
            if (player.getWorld().getTime() % 40 == 0) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 1, false, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 0, false, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 100, 0, false, false, true));
            }
        } else if (thirst <= THIRST_WARNING_THRESHOLD) {
            // Mild dehydration effects - apply every 3 seconds
            if (player.getWorld().getTime() % 60 == 0) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 0, false, false, true));
            }
        }
    }

    /**
     * Initialize thirst data for a player joining the server.
     */
    public static void initializePlayer(ServerPlayerEntity player) {
        PlayerDataStorage storage = (PlayerDataStorage) player;
        NbtCompound data = storage.ancientcurse$getPersistentData();

        if (!data.contains(THIRST_KEY)) {
            // New player, set default values
            data.putInt(THIRST_KEY, DEFAULT_THIRST);
            data.putFloat(THIRST_SATURATION_KEY, DEFAULT_SATURATION);
            data.putFloat(THIRST_EXHAUSTION_KEY, 0.0f);
        }

        // Sync current values to client
        int thirst = data.contains(THIRST_KEY) ? data.getInt(THIRST_KEY) : DEFAULT_THIRST;
        float saturation = data.contains(THIRST_SATURATION_KEY) ? data.getFloat(THIRST_SATURATION_KEY) : DEFAULT_SATURATION;
        ThirstPackets.sendThirstToClient(player, thirst, saturation);
    }

    /**
     * Reset thirst on respawn.
     */
    public static void onPlayerRespawn(ServerPlayerEntity player) {
        setThirstValue(player, DEFAULT_THIRST);
        setThirstSaturation(player, DEFAULT_SATURATION);
        setThirstExhaustion(player, 0.0f);
    }

    // Client-side methods

    /**
     * Set the client-side thirst value for HUD display.
     */
    public static void setClientThirstValue(int thirst, float saturation) {
        clientThirstValue = Math.max(0, Math.min(MAX_THIRST, thirst));
        clientThirstSaturation = Math.max(0, Math.min(MAX_SATURATION, saturation));
    }

    /**
     * Get the client-side thirst value for HUD display.
     */
    public static int getClientThirstValue() {
        return clientThirstValue;
    }

    /**
     * Get the client-side thirst saturation for HUD display.
     */
    public static float getClientThirstSaturation() {
        return clientThirstSaturation;
    }
}
