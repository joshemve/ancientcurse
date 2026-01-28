package com.ancientcurse.system;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.GameRules;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.particle.ParticleTypes;
import com.ancientcurse.ModSounds;
import com.ancientcurse.ModEntities;
import com.ancientcurse.entity.LocusEntity;
import com.ancientcurse.network.SandstormPackets;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public class SandstormData extends PersistentState {
    private boolean active;
    private int duration;
    private float intensity;

    // Wind direction (in radians) - changes slowly during storm
    private float windDirection;
    private float targetWindDirection;
    private int windChangeTimer;

    // Locust spawning system - transient (not saved)
    private transient Set<UUID> sandstormLocusts = new HashSet<>();
    private transient int locustSpawnTimer = 0;
    private transient int locustCleanupTimer = 0;
    private transient int locustTargetingTimer = 0;

    // Targeting settings
    private static final int LOCUST_TARGETING_INTERVAL = 20; // Re-target every 1 second

    private static final String KEY = "ancient_curse_sandstorm";

    // Configurable values
    private static final int MAX_DURATION = 24000; // 1 day
    private static final int MIN_DURATION = 6000;
    private static final float MAX_INTENSITY = 1.0f;

    // Wind push settings - subtle nudges with occasional stronger bursts
    private static final float BASE_WIND_STRENGTH = 0.01f; // Gentle nudge
    private static final float GUST_STRENGTH = 0.035f; // Noticeable gust
    private static final float STRONG_BURST_STRENGTH = 0.07f; // Strong burst - really pushes you
    private static final int WIND_CHANGE_INTERVAL = 200; // Ticks between wind direction changes

    // Locust spawning settings
    private static final int MAX_SANDSTORM_LOCUSTS = 15; // Hard cap per world
    private static final int LOCUST_SPAWN_INTERVAL = 100; // 5 seconds between spawn attempts
    private static final int LOCUST_CLEANUP_INTERVAL = 60; // 3 seconds between cleanup checks
    private static final float MIN_INTENSITY_FOR_LOCUSTS = 0.5f; // Only spawn at 50%+ intensity
    private static final int SPAWN_RADIUS_MIN = 15; // Minimum distance from player
    private static final int SPAWN_RADIUS_MAX = 30; // Maximum distance from player

    public static SandstormData getServerState(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
            SandstormData::readNbt,
            SandstormData::new,
            KEY
        );
    }

    public static SandstormData readNbt(NbtCompound nbt) {
        SandstormData data = new SandstormData();
        data.active = nbt.getBoolean("active");
        data.duration = nbt.getInt("duration");
        data.intensity = nbt.getFloat("intensity");
        data.windDirection = nbt.getFloat("windDirection");
        data.targetWindDirection = nbt.getFloat("targetWindDirection");
        return data;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putBoolean("active", active);
        nbt.putInt("duration", duration);
        nbt.putFloat("intensity", intensity);
        nbt.putFloat("windDirection", windDirection);
        nbt.putFloat("targetWindDirection", targetWindDirection);
        return nbt;
    }
    
    public void tick(ServerWorld world) {
        boolean dirty = false;

        // Ensure transient fields are initialized (in case of deserialization)
        if (sandstormLocusts == null) {
            sandstormLocusts = new HashSet<>();
        }

        if (this.active) {
            this.duration--;

            // Fade in intensity
            if (this.intensity < MAX_INTENSITY) {
                this.intensity = Math.min(MAX_INTENSITY, this.intensity + 0.005f);
                dirty = true;
            }

            // Update wind direction - slowly shifts over time
            updateWindDirection(world);

            // Apply wind push to all players
            applyWindToPlayers(world);

            // Spawn locusts during intense sandstorms
            tickLocustSpawning(world);

            // Keep locusts aggressive toward players
            tickLocustTargeting(world);

            if (this.duration <= 0) {
                stop(world);
                dirty = false; // stop() marks dirty
            } else {
                dirty = true;
            }
        } else {
            // Fade out intensity
            if (this.intensity > 0) {
                this.intensity = Math.max(0, this.intensity - 0.005f);
                dirty = true;

                // Still apply reduced wind during fade out
                applyWindToPlayers(world);

                // Gradually despawn locusts as storm fades
                if (intensity < MIN_INTENSITY_FOR_LOCUSTS) {
                    tickLocustCleanup(world, true); // Aggressive cleanup during fade
                }
            } else {
                // Storm fully ended - ensure all locusts are cleaned up
                if (!sandstormLocusts.isEmpty()) {
                    cleanupAllLocusts(world);
                }
            }

            // Random start logic
            if (world.getGameRules().getBoolean(GameRules.DO_WEATHER_CYCLE)) {
                // Only run in ancient_egypt or overworld?
                // Assuming global for now, but chance should be low
                if (world.getRandom().nextInt(24000) == 0) { // Once every ~20 mins on avg
                    start(world, MIN_DURATION + world.getRandom().nextInt(MAX_DURATION - MIN_DURATION));
                    dirty = false; // start() marks dirty
                }
            }
        }

        // Periodic cleanup of dead/removed locusts from tracking set
        tickLocustCleanup(world, false);

        if (dirty) {
            this.markDirty();
        }

        // Sync periodically or on change
        // For smooth transitions, we assume client interpolates, so we don't need to sync every tick
        if (world.getTime() % 20 == 0) { // Sync every second
             SandstormPackets.sendSyncPacket(world, this);
        }
    }

    /**
     * Updates the wind direction, slowly shifting towards a target direction.
     * Occasionally picks a new target direction for variety.
     */
    private void updateWindDirection(ServerWorld world) {
        windChangeTimer--;

        // Pick a new target direction periodically
        if (windChangeTimer <= 0) {
            windChangeTimer = WIND_CHANGE_INTERVAL + world.getRandom().nextInt(WIND_CHANGE_INTERVAL);
            // New random target direction (full 360 degrees)
            targetWindDirection = (float) (world.getRandom().nextFloat() * Math.PI * 2);
        }

        // Smoothly interpolate current direction towards target
        float diff = targetWindDirection - windDirection;
        // Normalize to -PI to PI range
        while (diff > Math.PI) diff -= Math.PI * 2;
        while (diff < -Math.PI) diff += Math.PI * 2;

        // Slowly turn towards target (about 1 degree per tick max)
        windDirection += diff * 0.02f;

        // Keep in 0 to 2PI range
        while (windDirection < 0) windDirection += Math.PI * 2;
        while (windDirection >= Math.PI * 2) windDirection -= Math.PI * 2;
    }

    /**
     * Applies subtle wind nudge effect to players.
     * Mostly gentle with occasional stronger bursts.
     */
    private void applyWindToPlayers(ServerWorld world) {
        if (this.intensity <= 0.2f) return; // No effect at low intensity

        for (ServerPlayerEntity player : world.getPlayers()) {
            // Skip creative/spectator players
            if (player.isCreative() || player.isSpectator()) continue;

            // Skip players indoors (under a solid block)
            if (isPlayerIndoors(world, player)) continue;

            // Only apply wind occasionally - not every tick
            // This creates a "gust" feel rather than constant push
            float gustChance = 0.10f * this.intensity; // ~10% chance at full intensity
            if (world.getRandom().nextFloat() > gustChance) continue;

            // Determine gust strength tier
            float roll = world.getRandom().nextFloat();
            float windStrength;
            boolean isStrongBurst = false;
            boolean isMediumGust = false;

            if (roll < 0.05f) {
                // 5% chance: Strong burst - really noticeable
                windStrength = STRONG_BURST_STRENGTH;
                isStrongBurst = true;
            } else if (roll < 0.25f) {
                // 20% chance: Medium gust
                windStrength = GUST_STRENGTH;
                isMediumGust = true;
            } else {
                // 75% chance: Gentle nudge
                windStrength = BASE_WIND_STRENGTH;
            }
            windStrength *= this.intensity;

            // Add randomness for natural feel
            windStrength *= 0.8f + world.getRandom().nextFloat() * 0.4f;

            // Calculate push direction from wind angle
            double pushX = Math.cos(windDirection) * windStrength;
            double pushZ = Math.sin(windDirection) * windStrength;

            // Apply the velocity nudge
            player.addVelocity(pushX, 0, pushZ);
            player.velocityModified = true;

            // Play wind burst sound effects based on gust strength
            // This makes the wind feel like actual bursts pushing you around, not mockery
            if (isStrongBurst) {
                // Strong burst - phantom flap for powerful wind whoosh
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_PHANTOM_FLAP, SoundCategory.WEATHER,
                    1.0f, 0.6f + world.getRandom().nextFloat() * 0.2f);
            } else if (isMediumGust) {
                // Medium gust - elytra flying with lower pitch for wind rush
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_ELYTRA_FLYING, SoundCategory.WEATHER,
                    0.7f, 0.8f + world.getRandom().nextFloat() * 0.3f);
            } else {
                // Gentle nudge - subtle wind sound (only 30% of the time to avoid spam)
                if (world.getRandom().nextFloat() < 0.3f) {
                    world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_PHANTOM_AMBIENT, SoundCategory.WEATHER,
                        0.4f, 1.2f + world.getRandom().nextFloat() * 0.3f);
                }
            }
        }
    }

    /**
     * Checks if a player is indoors (has a solid block above them within a few blocks).
     * Players indoors are sheltered from the wind.
     */
    private boolean isPlayerIndoors(ServerWorld world, PlayerEntity player) {
        // Check for solid blocks above the player (within 4 blocks)
        for (int y = 1; y <= 4; y++) {
            var pos = player.getBlockPos().up(y);
            if (world.getBlockState(pos).isSolidBlock(world, pos)) {
                return true;
            }
        }
        return false;
    }
    
    public void start(ServerWorld world, int duration) {
        this.active = true;
        this.duration = duration;
        this.intensity = Math.max(0.1f, this.intensity);
        // Initialize wind direction randomly when storm starts
        this.windDirection = (float) (world.getRandom().nextFloat() * Math.PI * 2);
        this.targetWindDirection = this.windDirection;
        this.windChangeTimer = WIND_CHANGE_INTERVAL;
        this.markDirty();
        SandstormPackets.sendSyncPacket(world, this);
    }
    
    public void stop(ServerWorld world) {
        this.active = false;
        this.duration = 0;
        this.markDirty();
        SandstormPackets.sendSyncPacket(world, this);
        // Note: Locusts will be gradually cleaned up as intensity fades
    }

    /**
     * Force stop with immediate cleanup (no fade out)
     */
    public void forceStop(ServerWorld world) {
        this.active = false;
        this.duration = 0;
        this.intensity = 0;
        // Immediately clean up all sandstorm locusts
        cleanupAllLocusts(world);
        this.markDirty();
        SandstormPackets.sendSyncPacket(world, this);
    }

    public boolean isActive() { return active; }
    public float getIntensity() { return intensity; }

    /**
     * Check if the sandstorm is having any visible effect (active or fading)
     */
    public boolean hasEffect() {
        return intensity > 0.01f;
    }

    // ==================== LOCUST SPAWNING SYSTEM ====================

    /**
     * Handles locust spawning during sandstorms.
     * Only spawns when intensity is high enough and below max count.
     */
    private void tickLocustSpawning(ServerWorld world) {
        locustSpawnTimer++;

        // Only attempt spawning periodically
        if (locustSpawnTimer < LOCUST_SPAWN_INTERVAL) return;
        locustSpawnTimer = 0;

        // Don't spawn if intensity too low
        if (intensity < MIN_INTENSITY_FOR_LOCUSTS) return;

        // Don't spawn if at max capacity
        if (sandstormLocusts.size() >= MAX_SANDSTORM_LOCUSTS) return;

        // Don't spawn if no players to target
        var players = world.getPlayers();
        if (players.isEmpty()) return;

        // Calculate spawn chance based on intensity (higher intensity = more likely)
        float spawnChance = (intensity - MIN_INTENSITY_FOR_LOCUSTS) / (1.0f - MIN_INTENSITY_FOR_LOCUSTS);
        if (world.getRandom().nextFloat() > spawnChance) return;

        // Pick a random player who is outdoors
        ServerPlayerEntity targetPlayer = null;
        for (int attempts = 0; attempts < 3; attempts++) {
            ServerPlayerEntity candidate = players.get(world.getRandom().nextInt(players.size()));
            if (!candidate.isCreative() && !candidate.isSpectator() && !isPlayerIndoors(world, candidate)) {
                targetPlayer = candidate;
                break;
            }
        }

        if (targetPlayer == null) return;

        // Spawn locust at a distance from the player (blown in by the wind)
        spawnSandstormLocust(world, targetPlayer);
    }

    /**
     * Spawns a single locust near a player, appearing to come from the storm.
     */
    private void spawnSandstormLocust(ServerWorld world, ServerPlayerEntity player) {
        LocusEntity locus = ModEntities.LOCUS.create(world);
        if (locus == null) return;

        // Spawn from upwind direction (opposite of wind direction)
        float spawnAngle = windDirection + (float) Math.PI; // Opposite of wind
        // Add some randomness to angle
        spawnAngle += (world.getRandom().nextFloat() - 0.5f) * 1.0f;

        // Random distance within spawn radius
        float distance = SPAWN_RADIUS_MIN + world.getRandom().nextFloat() * (SPAWN_RADIUS_MAX - SPAWN_RADIUS_MIN);

        double spawnX = player.getX() + Math.cos(spawnAngle) * distance;
        double spawnZ = player.getZ() + Math.sin(spawnAngle) * distance;

        // Find valid Y position (above ground, not inside blocks)
        BlockPos groundPos = world.getTopPosition(
            net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
            new BlockPos((int) spawnX, 0, (int) spawnZ)
        );

        // Spawn at player height +/- some variation (locusts fly)
        double spawnY = Math.max(groundPos.getY() + 2, player.getY() + (world.getRandom().nextDouble() - 0.3) * 8);

        locus.setPosition(spawnX, spawnY, spawnZ);

        // Give it initial velocity toward the player (blown by wind)
        Vec3d toPlayer = player.getPos().subtract(spawnX, spawnY, spawnZ).normalize();
        double windSpeed = 0.3 + world.getRandom().nextDouble() * 0.2;
        locus.setVelocity(toPlayer.multiply(windSpeed));

        // Set target to the player
        locus.setTarget(player);

        // Sandstorm locusts give XP when killed (3-5 XP like small hostile mobs)
        locus.setExperiencePoints(3 + world.getRandom().nextInt(3));

        // Spawn the entity
        if (world.spawnEntity(locus)) {
            sandstormLocusts.add(locus.getUuid());

            // Spawn dust particles at spawn location for visual effect
            world.spawnParticles(
                ParticleTypes.CAMPFIRE_COSY_SMOKE,
                spawnX, spawnY, spawnZ,
                5, 0.5, 0.5, 0.5, 0.02
            );
        }
    }

    /**
     * Periodically re-targets sandstorm locusts to nearby players.
     * This ensures locusts remain aggressive and don't lose their targets.
     */
    private void tickLocustTargeting(ServerWorld world) {
        locustTargetingTimer++;

        if (locustTargetingTimer < LOCUST_TARGETING_INTERVAL) return;
        locustTargetingTimer = 0;

        if (sandstormLocusts.isEmpty()) return;

        var players = world.getPlayers();
        if (players.isEmpty()) return;

        for (UUID locustId : sandstormLocusts) {
            Entity entity = world.getEntity(locustId);
            if (!(entity instanceof LocusEntity locus) || entity.isRemoved() || !entity.isAlive()) {
                continue;
            }

            // Re-target if no current target or target is invalid
            if (locus.getTarget() == null || !locus.getTarget().isAlive()) {
                // Find closest non-creative, non-spectator player
                PlayerEntity closest = null;
                double closestDist = 32 * 32; // 32 block max range squared

                for (ServerPlayerEntity player : players) {
                    if (player.isCreative() || player.isSpectator()) continue;

                    double distSq = locus.squaredDistanceTo(player);
                    if (distSq < closestDist) {
                        closestDist = distSq;
                        closest = player;
                    }
                }

                if (closest != null) {
                    locus.setTarget(closest);
                }
            }
        }
    }

    /**
     * Periodic cleanup of dead/removed locusts and optional aggressive despawning.
     * @param aggressive If true, actively despawn some locusts (used during storm fade)
     */
    private void tickLocustCleanup(ServerWorld world, boolean aggressive) {
        locustCleanupTimer++;

        if (locustCleanupTimer < LOCUST_CLEANUP_INTERVAL) return;
        locustCleanupTimer = 0;

        if (sandstormLocusts.isEmpty()) return;

        Iterator<UUID> iterator = sandstormLocusts.iterator();
        int despawnedThisTick = 0;
        int maxDespawnPerTick = aggressive ? 3 : 0; // Despawn up to 3 per tick during fade

        while (iterator.hasNext()) {
            UUID locustId = iterator.next();
            Entity entity = world.getEntity(locustId);

            // Remove from tracking if entity no longer exists
            if (entity == null || entity.isRemoved() || !entity.isAlive()) {
                iterator.remove();
                continue;
            }

            // Aggressive cleanup: despawn locusts that are far from players or randomly
            if (aggressive && despawnedThisTick < maxDespawnPerTick) {
                LocusEntity locus = (LocusEntity) entity;

                // Check if far from all players (> 50 blocks)
                boolean farFromPlayers = true;
                for (ServerPlayerEntity player : world.getPlayers()) {
                    if (locus.squaredDistanceTo(player) < 2500) { // 50 blocks squared
                        farFromPlayers = false;
                        break;
                    }
                }

                // Despawn if far from players or random chance during fade
                if (farFromPlayers || world.getRandom().nextFloat() < 0.3f) {
                    despawnLocustWithEffect(world, locus);
                    iterator.remove();
                    despawnedThisTick++;
                }
            }
        }
    }

    /**
     * Immediately cleans up all sandstorm-spawned locusts.
     * Called when storm fully ends.
     */
    private void cleanupAllLocusts(ServerWorld world) {
        for (UUID locustId : sandstormLocusts) {
            Entity entity = world.getEntity(locustId);
            if (entity instanceof LocusEntity locus && !entity.isRemoved()) {
                despawnLocustWithEffect(world, locus);
            }
        }
        sandstormLocusts.clear();
    }

    /**
     * Despawns a locust with a visual effect (blown away by wind).
     */
    private void despawnLocustWithEffect(ServerWorld world, LocusEntity locus) {
        // Particles showing locust being blown away
        world.spawnParticles(
            ParticleTypes.CLOUD,
            locus.getX(), locus.getY() + 0.3, locus.getZ(),
            3, 0.2, 0.2, 0.2, 0.05
        );

        // Small poof sound
        world.playSound(null, locus.getX(), locus.getY(), locus.getZ(),
            SoundEvents.ENTITY_PHANTOM_FLAP, SoundCategory.HOSTILE,
            0.3f, 1.5f);

        locus.discard();
    }

    /**
     * Gets the current count of sandstorm-spawned locusts.
     */
    public int getSandstormLocustCount() {
        return sandstormLocusts != null ? sandstormLocusts.size() : 0;
    }
}
