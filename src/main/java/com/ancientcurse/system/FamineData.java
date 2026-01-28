package com.ancientcurse.system;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.network.FaminePackets;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

/**
 * Server-side persistent state for the Famine event.
 *
 * When active:
 * - Crops gradually wither and die across the world
 * - Food items in player inventories spoil over time
 * - Animals slowly starve unless fed
 * - Visual effects show the land dying
 *
 * Performance considerations:
 * - Crop processing is spread across ticks (batch processing)
 * - Uses chunk-based scanning with cooldowns
 * - Food spoilage checked only periodically per player
 * - Animal starvation processed in batches
 */
public class FamineData extends PersistentState {
    private static final String KEY = "ancient_curse_famine";

    // Event state
    private boolean active = false;
    private int duration = -1; // Ticks remaining, -1 for infinite
    private float intensity = 0.0f; // 0.0 to 1.0, affects speed of effects
    private long startTime = 0; // World time when famine started

    // Processing state (not persisted)
    private transient int tickCounter = 0;
    private transient int lastSyncTick = 0;

    public FamineData() {}

    public static FamineData getServerState(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
            FamineData::readNbt,
            FamineData::new,
            KEY
        );
    }

    public static FamineData readNbt(NbtCompound nbt) {
        FamineData data = new FamineData();
        data.active = nbt.getBoolean("active");
        data.duration = nbt.getInt("duration");
        data.intensity = nbt.getFloat("intensity");
        data.startTime = nbt.getLong("startTime");
        return data;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putBoolean("active", active);
        nbt.putInt("duration", duration);
        nbt.putFloat("intensity", intensity);
        nbt.putLong("startTime", startTime);
        return nbt;
    }

    /**
     * Called every server tick from main tick handler
     */
    public void tick(ServerWorld world) {
        tickCounter++;
        boolean dirty = false;
        FamineConfig config = FamineConfig.get();

        if (active) {
            // Handle duration countdown
            if (duration > 0) {
                duration--;
                if (duration <= 0) {
                    stop(world);
                    return;
                }
                dirty = true;
            }

            // Fade in intensity using config speed
            float maxIntensity = config.getMaxIntensity();
            float fadeInSpeed = 0.01f * config.getIntensityFadeInSpeed();
            if (intensity < maxIntensity) {
                intensity = Math.min(maxIntensity, intensity + fadeInSpeed);
                dirty = true;
            }

            // Process crop deaths (handled by FamineCropHandler)
            FamineCropHandler.tick(world, this, config);

            // Process food spoilage (handled by FamineFoodSpoilageHandler)
            int spoilageInterval = config.getSpoilageCheckInterval();
            if (tickCounter % spoilageInterval == 0) {
                FamineFoodSpoilageHandler.checkAllPlayers(world, this, config);
            }

            // Process animal starvation (handled by FamineAnimalHandler)
            if (config.isAnimalStarvationEnabled()) {
                FamineAnimalHandler.tick(world, this, config);
            }

        } else {
            // Fade out intensity when inactive using config speed
            float fadeOutSpeed = 0.02f * config.getIntensityFadeOutSpeed();
            if (intensity > 0) {
                intensity = Math.max(0, intensity - fadeOutSpeed);
                dirty = true;

                // When fully faded out, do final cleanup
                if (intensity <= 0) {
                    performFullCleanup(world);
                }
            }
        }

        if (dirty) {
            markDirty();
        }

        // Sync to clients every second
        if (tickCounter - lastSyncTick >= 20) {
            FaminePackets.sendSyncPacket(world, this);
            lastSyncTick = tickCounter;
        }
    }

    /**
     * Start the famine event
     * @param world The server world
     * @param durationTicks Duration in ticks, or -1 for infinite
     */
    public void start(ServerWorld world, int durationTicks) {
        if (active) {
            AncientCurse.LOGGER.info("Famine already active, extending duration");
            if (durationTicks > 0 && this.duration > 0) {
                this.duration = Math.max(this.duration, durationTicks);
            } else if (durationTicks < 0) {
                this.duration = -1; // Set to infinite
            }
        } else {
            this.active = true;
            this.duration = durationTicks;
            this.startTime = world.getTime();
            this.intensity = 0.1f; // Start with some intensity
            AncientCurse.LOGGER.info("Famine started" + (durationTicks > 0 ? " for " + (durationTicks / 20) + " seconds" : " indefinitely"));
        }
        markDirty();
        FaminePackets.sendSyncPacket(world, this);
    }

    /**
     * Stop the famine event (will fade out)
     */
    public void stop(ServerWorld world) {
        this.active = false;
        this.duration = 0;
        markDirty();
        FaminePackets.sendSyncPacket(world, this);
        AncientCurse.LOGGER.info("Famine stopped (fading out)");
    }

    /**
     * Force stop with immediate cleanup
     */
    public void forceStop(ServerWorld world) {
        this.active = false;
        this.duration = 0;
        this.intensity = 0;
        markDirty();

        // Perform full cleanup
        performFullCleanup(world);

        FaminePackets.sendSyncPacket(world, this);
        AncientCurse.LOGGER.info("Famine force stopped");
    }

    /**
     * Perform full cleanup of all famine effects
     */
    private void performFullCleanup(ServerWorld world) {
        AncientCurse.LOGGER.info("Performing famine cleanup...");

        // Reset all handlers
        FamineCropHandler.reset();
        FamineFoodSpoilageHandler.reset();
        FamineAnimalHandler.reset();

        // Clear animal starvation effects
        FamineAnimalHandler.clearAllStarvationEffects(world);

        // Clear spoilage timers from all players' food
        for (ServerPlayerEntity player : world.getPlayers()) {
            FamineFoodSpoilageHandler.clearSpoilageTimers(player);
        }

        AncientCurse.LOGGER.info("Famine cleanup complete");
    }

    // === Getters ===

    public boolean isActive() {
        return active;
    }

    public float getIntensity() {
        return intensity;
    }

    public int getRemainingDuration() {
        return duration;
    }

    public long getStartTime() {
        return startTime;
    }

    /**
     * Get crop death chance based on intensity and config
     */
    public float getCropDeathChance() {
        FamineConfig config = FamineConfig.get();
        return config.getCropDeathChanceBase() * intensity * config.getCropDeathSpeedMultiplier();
    }

    /**
     * Get spoilage chance based on intensity and config
     */
    public float getSpoilageChance() {
        FamineConfig config = FamineConfig.get();
        return config.getSpoilageChanceBase() * intensity * config.getSpoilageSpeedMultiplier();
    }

    /**
     * Check if the famine is having any effect (active or fading)
     */
    public boolean hasEffect() {
        return intensity > 0.01f;
    }
}
