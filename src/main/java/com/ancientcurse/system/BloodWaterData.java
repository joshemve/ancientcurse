package com.ancientcurse.system;

import com.ancientcurse.AncientCurse;
import net.minecraft.block.Blocks;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import com.ancientcurse.network.BloodWaterPackets;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side persistent state for the Blood Water plague event (1st Plague of
 * Egypt).
 *
 * "The LORD said to Moses, 'Tell Aaron, Take your staff and stretch out your
 * hand
 * over the waters of Egypt... and they will turn to blood. Blood will be
 * everywhere
 * in Egypt, even in vessels of wood and stone.'" - Exodus 7:19-20
 *
 * When active:
 * - All water appears blood-red
 * - Players in water take damage over time
 * - Curse lingers after leaving water
 * - Status effects applied (nausea, hunger, optionally poison)
 */
public class BloodWaterData extends PersistentState {
    private boolean active;
    private int duration; // Duration in ticks, -1 for infinite
    private float intensity; // 0.0 to 1.0 for fade in/out

    private static final String KEY = "ancient_curse_blood_water";
    private static final float MAX_INTENSITY = 1.0f;
    private static final float FADE_SPEED = 0.02f; // Fade in/out speed per tick

    // Custom damage type for blood water
    public static final RegistryKey<DamageType> BLOOD_WATER_DAMAGE = RegistryKey.of(
            RegistryKeys.DAMAGE_TYPE,
            new Identifier(AncientCurse.MOD_ID, "blood_water"));

    // Track cursed players and their linger timers
    private Map<UUID, Integer> cursedPlayers = new HashMap<>();
    private Map<UUID, Integer> playerDamageTimers = new HashMap<>();
    private Map<UUID, Integer> playerLingerTimers = new HashMap<>();
    private Map<UUID, Integer> playerImmunityTimers = new HashMap<>();
    private int tickCounter = 0;

    public BloodWaterData() {
        this.cursedPlayers = new HashMap<>();
        this.playerDamageTimers = new HashMap<>();
        this.playerLingerTimers = new HashMap<>();
        this.playerImmunityTimers = new HashMap<>();
    }

    public static BloodWaterData getServerState(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
                BloodWaterData::readNbt,
                BloodWaterData::new,
                KEY);
    }

    public static BloodWaterData readNbt(NbtCompound nbt) {
        BloodWaterData data = new BloodWaterData();
        data.active = nbt.getBoolean("active");
        data.duration = nbt.getInt("duration");
        data.intensity = nbt.getFloat("intensity");

        // Read maps
        if (nbt.contains("cursedPlayers")) {
            readMapFromNbt(nbt.getCompound("cursedPlayers"), data.cursedPlayers);
        }
        if (nbt.contains("playerDamageTimers")) {
            readMapFromNbt(nbt.getCompound("playerDamageTimers"), data.playerDamageTimers);
        }
        if (nbt.contains("playerLingerTimers")) {
            readMapFromNbt(nbt.getCompound("playerLingerTimers"), data.playerLingerTimers);
        }
        if (nbt.contains("playerImmunityTimers")) {
            readMapFromNbt(nbt.getCompound("playerImmunityTimers"), data.playerImmunityTimers);
        }

        return data;
    }

    private static void readMapFromNbt(NbtCompound compound, Map<UUID, Integer> map) {
        for (String key : compound.getKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                int value = compound.getInt(key);
                map.put(uuid, value);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putBoolean("active", active);
        nbt.putInt("duration", duration);
        nbt.putFloat("intensity", intensity);

        // Write maps
        nbt.put("cursedPlayers", writeMapToNbt(cursedPlayers));
        nbt.put("playerDamageTimers", writeMapToNbt(playerDamageTimers));
        nbt.put("playerLingerTimers", writeMapToNbt(playerLingerTimers));
        nbt.put("playerImmunityTimers", writeMapToNbt(playerImmunityTimers));

        return nbt;
    }

    private NbtCompound writeMapToNbt(Map<UUID, Integer> map) {
        NbtCompound compound = new NbtCompound();
        map.forEach((uuid, value) -> compound.putInt(uuid.toString(), value));
        return compound;
    }

    /**
     * Called every tick from the server tick event
     */
    public void tick(ServerWorld world) {
        tickCounter++;
        boolean dirty = false;
        BloodWaterConfig config = BloodWaterConfig.get();

        if (this.active) {
            // Handle duration countdown (-1 means infinite)
            if (this.duration > 0) {
                this.duration--;
                if (this.duration <= 0) {
                    stop(world);
                    return;
                }
            }

            // Fade in intensity
            if (this.intensity < MAX_INTENSITY) {
                this.intensity = Math.min(MAX_INTENSITY, this.intensity + FADE_SPEED);
                dirty = true;
            }

            // Process curse effects on players
            if (config.isCurseEnabled() && intensity >= config.getMinimumIntensityForCurse()) {
                processPlayerCurses(world, config);
            }
        } else {
            // Fade out intensity when inactive
            if (this.intensity > 0) {
                this.intensity = Math.max(0, this.intensity - FADE_SPEED);
                dirty = true;
            }

            // Still process lingering curses even when event is inactive
            if (!cursedPlayers.isEmpty()) {
                processLingeringCurses(world, config);
            }
        }

        // Update immunity timers
        playerImmunityTimers.entrySet().removeIf(entry -> {
            entry.setValue(entry.getValue() - 1);
            return entry.getValue() <= 0;
        });

        if (dirty) {
            this.markDirty();
        }

        // Sync to clients every second (20 ticks) for smooth interpolation
        if (tickCounter % 20 == 0) {
            BloodWaterPackets.sendSyncPacket(world, this);
        }
    }

    /**
     * Process curse effects on all players in the world
     */
    private void processPlayerCurses(ServerWorld world, BloodWaterConfig config) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            UUID playerId = player.getUuid();

            // Skip if player has immunity
            if (playerImmunityTimers.containsKey(playerId)) {
                continue;
            }

            // Skip creative/spectator mode
            if (player.isCreative() || player.isSpectator()) {
                // Clear curse if they switch to creative
                cursedPlayers.remove(playerId);
                playerLingerTimers.remove(playerId);
                continue;
            }

            boolean inWater = isPlayerInWater(player);

            if (inWater) {
                // Player is in blood water - apply direct curse effects
                cursedPlayers.put(playerId, config.getCurseLingerDuration());

                // Apply damage on interval
                int damageTimer = playerDamageTimers.getOrDefault(playerId, 0);
                damageTimer++;

                if (damageTimer >= config.getDamageInterval()) {
                    applyBloodWaterDamage(player, config, true);
                    damageTimer = 0;
                }
                playerDamageTimers.put(playerId, damageTimer);

                // Apply status effects
                applyStatusEffects(player, config);

                // Notify client of curse state
                BloodWaterPackets.sendCursePacket(player, true, config.getCurseLingerDuration());

            } else {
                // Player left water - start or continue linger timer
                Integer lingerTime = cursedPlayers.get(playerId);

                if (lingerTime != null && lingerTime > 0) {
                    // Decrease linger time
                    lingerTime--;
                    cursedPlayers.put(playerId, lingerTime);

                    // Apply linger damage on interval
                    int lingerTimer = playerLingerTimers.getOrDefault(playerId, 0);
                    lingerTimer++;

                    if (lingerTimer >= config.getLingerDamageInterval()) {
                        applyBloodWaterDamage(player, config, false);
                        lingerTimer = 0;
                    }
                    playerLingerTimers.put(playerId, lingerTimer);

                    // Notify client of remaining curse time
                    BloodWaterPackets.sendCursePacket(player, true, lingerTime);

                    if (lingerTime <= 0) {
                        // Curse expired
                        cursedPlayers.remove(playerId);
                        playerLingerTimers.remove(playerId);
                        playerDamageTimers.remove(playerId);
                        BloodWaterPackets.sendCursePacket(player, false, 0);
                    }
                }
            }
        }
    }

    /**
     * Process lingering curses when the event is inactive
     */
    private void processLingeringCurses(ServerWorld world, BloodWaterConfig config) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            UUID playerId = player.getUuid();
            Integer lingerTime = cursedPlayers.get(playerId);

            if (lingerTime != null && lingerTime > 0) {
                lingerTime--;
                cursedPlayers.put(playerId, lingerTime);

                // Apply linger damage
                int lingerTimer = playerLingerTimers.getOrDefault(playerId, 0);
                lingerTimer++;

                if (lingerTimer >= config.getLingerDamageInterval()) {
                    applyBloodWaterDamage(player, config, false);
                    lingerTimer = 0;
                }
                playerLingerTimers.put(playerId, lingerTimer);

                BloodWaterPackets.sendCursePacket(player, true, lingerTime);

                if (lingerTime <= 0) {
                    cursedPlayers.remove(playerId);
                    playerLingerTimers.remove(playerId);
                    playerDamageTimers.remove(playerId);
                    BloodWaterPackets.sendCursePacket(player, false, 0);
                }
            }
        }
    }

    /**
     * Check if a player is currently in water
     */
    private boolean isPlayerInWater(ServerPlayerEntity player) {
        // Check if player is submerged or touching water
        if (player.isTouchingWater() || player.isSubmergedInWater()) {
            return true;
        }

        // Also check the block at player's feet
        BlockPos feetPos = player.getBlockPos();
        if (player.getWorld().getBlockState(feetPos).getBlock() == Blocks.WATER) {
            return true;
        }

        return false;
    }

    /**
     * Apply blood water damage to a player
     */
    private void applyBloodWaterDamage(ServerPlayerEntity player, BloodWaterConfig config, boolean directContact) {
        float baseDamage = directContact ? config.getDamagePerTick() : config.getLingerDamagePerTick();

        // Scale damage with intensity if enabled
        float damage = baseDamage;
        if (config.isDamageScalesWithIntensity()) {
            damage *= intensity;
        }

        if (damage > 0) {
            // Use custom blood water damage type
            DamageSource source = new DamageSource(
                    player.getWorld().getRegistryManager()
                            .get(RegistryKeys.DAMAGE_TYPE)
                            .entryOf(BLOOD_WATER_DAMAGE));
            player.damage(source, damage);
        }
    }

    /**
     * Apply status effects to a player in blood water
     */
    private void applyStatusEffects(ServerPlayerEntity player, BloodWaterConfig config) {
        // Apply nausea
        if (config.isApplyNausea()) {
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.NAUSEA,
                    config.getNauseaDuration(),
                    config.getNauseaAmplifier(),
                    false, // ambient
                    false, // show particles
                    true // show icon
            ));
        }

        // Apply poison
        if (config.isApplyPoison()) {
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.POISON,
                    config.getPoisonDuration(),
                    config.getPoisonAmplifier(),
                    false,
                    true,
                    true));
        }

        // Apply hunger
        if (config.isApplyHunger()) {
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.HUNGER,
                    config.getHungerDuration(),
                    config.getHungerAmplifier(),
                    false,
                    false,
                    true));
        }
    }

    /**
     * Grant immunity to a player (e.g., after respawn)
     */
    public void grantImmunity(ServerPlayerEntity player, int ticks) {
        playerImmunityTimers.put(player.getUuid(), ticks);
        // Clear existing curse
        cursedPlayers.remove(player.getUuid());
        playerDamageTimers.remove(player.getUuid());
        playerLingerTimers.remove(player.getUuid());
    }

    /**
     * Check if a player is currently cursed
     */
    public boolean isPlayerCursed(UUID playerId) {
        Integer lingerTime = cursedPlayers.get(playerId);
        return lingerTime != null && lingerTime > 0;
    }

    /**
     * Get remaining curse time for a player
     */
    public int getPlayerCurseTime(UUID playerId) {
        return cursedPlayers.getOrDefault(playerId, 0);
    }

    /**
     * Start the blood water event
     * 
     * @param world         The server world
     * @param durationTicks Duration in ticks, or -1 for infinite
     */
    public void start(ServerWorld world, int durationTicks) {
        this.active = true;
        this.duration = durationTicks;
        // Start with some intensity for immediate visual feedback
        this.intensity = Math.max(0.1f, this.intensity);
        this.markDirty();
        BloodWaterPackets.sendSyncPacket(world, this);

        AncientCurse.LOGGER.info("Blood Water plague started" +
                (durationTicks > 0 ? " for " + (durationTicks / 20) + " seconds" : " indefinitely"));
    }

    /**
     * Stop the blood water event (will fade out)
     */
    public void stop(ServerWorld world) {
        this.active = false;
        this.duration = 0;
        this.markDirty();
        BloodWaterPackets.sendSyncPacket(world, this);
        AncientCurse.LOGGER.info("Blood Water plague stopped (fading out)");
    }

    /**
     * Force stop with immediate cleanup (no fade out)
     */
    public void forceStop(ServerWorld world) {
        this.active = false;
        this.duration = 0;
        this.intensity = 0;

        // Clear all curses
        for (ServerPlayerEntity player : world.getPlayers()) {
            BloodWaterPackets.sendCursePacket(player, false, 0);
        }
        cursedPlayers.clear();
        playerDamageTimers.clear();
        playerLingerTimers.clear();

        this.markDirty();
        BloodWaterPackets.sendSyncPacket(world, this);
        AncientCurse.LOGGER.info("Blood Water plague force stopped");
    }

    public boolean isActive() {
        return active;
    }

    public float getIntensity() {
        return intensity;
    }

    public int getRemainingDuration() {
        return duration;
    }

    /**
     * Check if the blood water effect is having any visible effect (active or
     * fading)
     */
    public boolean hasEffect() {
        return intensity > 0.01f;
    }

    /**
     * Get the number of currently cursed players
     */
    public int getCursedPlayerCount() {
        return (int) cursedPlayers.values().stream().filter(v -> v > 0).count();
    }
}
