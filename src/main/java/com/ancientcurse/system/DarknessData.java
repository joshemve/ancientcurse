package com.ancientcurse.system;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.network.DarknessPackets;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

/**
 * Server-side persistent state for the Darkness plague event (9th Plague of Egypt).
 *
 * "Then the LORD said to Moses, 'Stretch out your hand toward the sky so that
 * darkness spreads over Egypt—darkness that can be felt.' So Moses stretched
 * out his hand toward the sky, and total darkness covered all Egypt for three days."
 * - Exodus 10:21-22
 *
 * When active:
 * - Total darkness covers the land
 * - Sky turns black
 * - Light sources are dimmed
 * - Players receive the darkness effect
 * - Hostile mob spawning may increase
 *
 * The event respects configured duration (default 3 Minecraft days).
 */
public class DarknessData extends PersistentState {
    private static final String KEY = "ancient_curse_darkness";

    // Event state
    private boolean active = false;
    private int duration = -1; // Ticks remaining, -1 for infinite
    private float intensity = 0.0f; // 0.0 to 1.0, affects darkness level
    private long startTime = 0; // World time when darkness started

    // Lightning flash state
    private int flashTimer = 0; // Ticks remaining in current flash
    private float flashIntensity = 0.0f; // Current flash brightness

    // Processing state (not persisted)
    private transient int tickCounter = 0;
    private transient int lastSyncTick = 0;
    private transient int darknessEffectTimer = 0;

    public DarknessData() {}

    public static DarknessData getServerState(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
            DarknessData::readNbt,
            DarknessData::new,
            KEY
        );
    }

    public static DarknessData readNbt(NbtCompound nbt) {
        DarknessData data = new DarknessData();
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
        DarknessConfig config = DarknessConfig.get();

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
            float fadeInSpeed = 0.005f * config.getIntensityFadeInSpeed();
            if (intensity < maxIntensity) {
                intensity = Math.min(maxIntensity, intensity + fadeInSpeed);
                dirty = true;
            }

            // Handle lightning flashes if enabled
            if (config.enableLightningFlashes() && flashTimer <= 0) {
                if (world.random.nextFloat() < config.getLightningFlashChance()) {
                    triggerLightningFlash(world);
                }
            }

            // Update flash timer
            if (flashTimer > 0) {
                flashTimer--;
                // Flash fades out
                flashIntensity = Math.max(0, flashIntensity - 0.1f);
                if (flashTimer <= 0) {
                    flashIntensity = 0.0f;
                }
                dirty = true;
            }

            // Apply darkness effect to players periodically
            if (config.applyDarknessEffect()) {
                darknessEffectTimer++;
                if (darknessEffectTimer >= 40) { // Every 2 seconds
                    applyDarknessEffectToPlayers(world, config);
                    darknessEffectTimer = 0;
                }
            }

        } else {
            // Fade out intensity when inactive using config speed
            float fadeOutSpeed = 0.01f * config.getIntensityFadeOutSpeed();
            if (intensity > 0) {
                intensity = Math.max(0, intensity - fadeOutSpeed);
                dirty = true;

                // When fully faded out, do final cleanup
                if (intensity <= 0) {
                    performFullCleanup(world);
                }
            }

            // Flash also fades when inactive
            if (flashIntensity > 0) {
                flashIntensity = Math.max(0, flashIntensity - 0.1f);
                dirty = true;
            }
        }

        if (dirty) {
            markDirty();
        }

        // Sync to clients every second
        if (tickCounter - lastSyncTick >= 20) {
            DarknessPackets.sendSyncPacket(world, this);
            lastSyncTick = tickCounter;
        }
    }

    /**
     * Start the darkness event
     * @param world The server world
     * @param durationTicks Duration in ticks, or -1 for infinite
     */
    public void start(ServerWorld world, int durationTicks) {
        if (active) {
            AncientCurse.LOGGER.info("Darkness already active, extending duration");
            if (durationTicks > 0 && this.duration > 0) {
                this.duration = Math.max(this.duration, durationTicks);
            } else if (durationTicks < 0) {
                this.duration = -1; // Set to infinite
            }
        } else {
            this.active = true;
            this.duration = durationTicks;
            this.startTime = world.getTime();
            this.intensity = 0.05f; // Start with slight darkness
            this.flashTimer = 0;
            this.flashIntensity = 0.0f;

            String durationStr;
            if (durationTicks > 0) {
                int days = durationTicks / 24000;
                int remainingTicks = durationTicks % 24000;
                if (remainingTicks == 0) {
                    durationStr = days + " day" + (days != 1 ? "s" : "");
                } else {
                    durationStr = (durationTicks / 20) + " seconds";
                }
            } else {
                durationStr = "indefinitely";
            }
            AncientCurse.LOGGER.info("Darkness plague started " + durationStr);
        }
        markDirty();
        DarknessPackets.sendSyncPacket(world, this);
    }

    /**
     * Start darkness for a number of days
     */
    public void startDays(ServerWorld world, int days) {
        DarknessConfig config = DarknessConfig.get();
        int clampedDays = Math.max(config.getMinDurationDays(),
                         Math.min(config.getMaxDurationDays(), days));
        start(world, clampedDays * 24000);
    }

    /**
     * Stop the darkness event (will fade out)
     */
    public void stop(ServerWorld world) {
        this.active = false;
        this.duration = 0;
        markDirty();
        DarknessPackets.sendSyncPacket(world, this);
        AncientCurse.LOGGER.info("Darkness plague stopped (fading out)");
    }

    /**
     * Force stop with immediate cleanup
     */
    public void forceStop(ServerWorld world) {
        this.active = false;
        this.duration = 0;
        this.intensity = 0;
        this.flashTimer = 0;
        this.flashIntensity = 0.0f;
        markDirty();

        // Perform full cleanup
        performFullCleanup(world);

        DarknessPackets.sendSyncPacket(world, this);
        AncientCurse.LOGGER.info("Darkness plague force stopped");
    }

    /**
     * Perform full cleanup of all darkness effects
     */
    private void performFullCleanup(ServerWorld world) {
        AncientCurse.LOGGER.info("Performing darkness cleanup...");

        // Remove darkness effect from all players
        for (ServerPlayerEntity player : world.getPlayers()) {
            player.removeStatusEffect(StatusEffects.DARKNESS);
        }

        AncientCurse.LOGGER.info("Darkness cleanup complete");
    }

    /**
     * Apply darkness status effect to all players in the world
     */
    private void applyDarknessEffectToPlayers(ServerWorld world, DarknessConfig config) {
        if (intensity < 0.5f) return; // Only apply when darkness is significant

        int effectLevel = config.getDarknessEffectLevel();
        int duration = 100; // 5 seconds, will be refreshed

        for (ServerPlayerEntity player : world.getPlayers()) {
            // Apply darkness effect
            player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.DARKNESS,
                duration,
                effectLevel,
                false, // ambient
                false, // show particles
                true   // show icon
            ));
        }
    }

    /**
     * Trigger a brief lightning flash
     */
    private void triggerLightningFlash(ServerWorld world) {
        flashTimer = 10; // Brief flash (0.5 seconds)
        flashIntensity = 0.8f + world.random.nextFloat() * 0.2f; // 80-100% brightness

        // Sync immediately for dramatic effect
        DarknessPackets.sendSyncPacket(world, this);
    }

    // === Getters ===

    public boolean isActive() {
        return active;
    }

    public float getIntensity() {
        return intensity;
    }

    /**
     * Get the effective darkness level (accounting for flashes)
     */
    public float getEffectiveDarkness() {
        if (flashIntensity > 0) {
            // During flash, reduce darkness
            return Math.max(0, intensity - flashIntensity);
        }
        return intensity;
    }

    public float getFlashIntensity() {
        return flashIntensity;
    }

    public int getRemainingDuration() {
        return duration;
    }

    public int getRemainingDays() {
        if (duration < 0) return -1;
        return duration / 24000;
    }

    public long getStartTime() {
        return startTime;
    }

    /**
     * Get elapsed time in ticks since darkness started
     */
    public long getElapsedTime(ServerWorld world) {
        if (!active || startTime == 0) return 0;
        return world.getTime() - startTime;
    }

    /**
     * Check if the darkness is having any effect (active or fading)
     */
    public boolean hasEffect() {
        return intensity > 0.01f;
    }

    /**
     * Check if it's a lightning flash moment
     */
    public boolean isFlashing() {
        return flashTimer > 0 && flashIntensity > 0;
    }

    /**
     * Get the ambient light level based on darkness intensity
     */
    public float getAmbientLightLevel() {
        DarknessConfig config = DarknessConfig.get();
        float minLight = config.getMinimumAmbientLight();

        // During flash, more light
        if (isFlashing()) {
            return Math.min(1.0f, minLight + flashIntensity * (1.0f - minLight));
        }

        // Interpolate between full light (1.0) and minimum ambient based on intensity
        return 1.0f - (intensity * (1.0f - minLight));
    }
}
