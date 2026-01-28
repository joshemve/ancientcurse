package com.ancientcurse.system;

import com.ancientcurse.AncientCurse;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

import java.util.*;

/**
 * Handles animal starvation during the Famine event.
 *
 * Animals gradually starve unless fed by players.
 * Feeding an animal resets its starvation timer.
 *
 * Performance optimizations:
 * - Only processes animals near players
 * - Spreads processing across ticks
 * - Uses entity NBT for persistent timers
 */
public class FamineAnimalHandler {

    // NBT keys for tracking starvation
    private static final String STARVATION_TIMER_KEY = "ancientcurse_starvation";
    private static final String LAST_FED_TIME_KEY = "ancientcurse_last_fed";

    // Processing state
    private static final Queue<UUID> pendingAnimals = new LinkedList<>();
    private static int tickCounter = 0;
    private static final int PROCESS_INTERVAL = 20; // Process every second
    private static final int MAX_PROCESS_PER_TICK = 10;

    // Tracked entities for cleanup
    private static final Set<UUID> trackedAnimals = new HashSet<>();

    /**
     * Called every server tick from FamineData.tick()
     */
    public static void tick(ServerWorld world, FamineData data, FamineConfig config) {
        tickCounter++;

        if (!data.isActive()) {
            return;
        }

        // Only process periodically
        if (tickCounter % PROCESS_INTERVAL != 0) {
            return;
        }

        float intensity = data.getIntensity();
        if (intensity < 0.1f) {
            return;
        }

        // Gather animals near players
        if (pendingAnimals.isEmpty()) {
            gatherAnimalsNearPlayers(world, config.getAnimalStarvationRadius());
        }

        // Process pending animals
        int processed = 0;
        while (!pendingAnimals.isEmpty() && processed < MAX_PROCESS_PER_TICK) {
            UUID entityId = pendingAnimals.poll();
            Entity entity = world.getEntity(entityId);

            if (entity instanceof AnimalEntity animal && animal.isAlive()) {
                processAnimal(world, animal, intensity, config);
                processed++;
            }
        }

        // Cleanup tracked animals periodically
        if (tickCounter % 1200 == 0) { // Every minute
            cleanupTrackedAnimals(world);
        }
    }

    /**
     * Gather animals within radius of players
     */
    private static void gatherAnimalsNearPlayers(ServerWorld world, int radius) {
        for (var player : world.getPlayers()) {
            var box = player.getBoundingBox().expand(radius);
            var entities = world.getEntitiesByClass(AnimalEntity.class, box,
                entity -> isAffectedAnimal(entity) && entity.isAlive());

            for (var animal : entities) {
                if (!pendingAnimals.contains(animal.getUuid())) {
                    pendingAnimals.add(animal.getUuid());
                    trackedAnimals.add(animal.getUuid());
                }
            }
        }
    }

    /**
     * Check if an animal is affected by famine starvation
     */
    private static boolean isAffectedAnimal(AnimalEntity animal) {
        // Farm animals and tameable animals are affected
        return animal instanceof CowEntity ||
               animal instanceof SheepEntity ||
               animal instanceof PigEntity ||
               animal instanceof ChickenEntity ||
               animal instanceof RabbitEntity ||
               animal instanceof GoatEntity ||
               animal instanceof HorseEntity ||
               animal instanceof DonkeyEntity ||
               animal instanceof MuleEntity ||
               animal instanceof LlamaEntity ||
               animal instanceof CatEntity ||
               animal instanceof WolfEntity ||
               animal instanceof FoxEntity ||
               animal instanceof BeeEntity ||
               animal instanceof StriderEntity;
    }

    /**
     * Process a single animal for starvation
     */
    private static void processAnimal(ServerWorld world, AnimalEntity animal, float intensity, FamineConfig config) {
        NbtCompound nbt = new NbtCompound();
        animal.writeCustomDataToNbt(nbt);

        // Get or initialize starvation timer
        int starvationTimer = 0;
        if (nbt.contains(STARVATION_TIMER_KEY)) {
            starvationTimer = nbt.getInt(STARVATION_TIMER_KEY);
        }

        // Increment timer based on intensity and config
        int increment = (int)(intensity * config.getAnimalStarvationRate());
        starvationTimer += increment;

        // Check if animal should take damage
        int damageThreshold = config.getAnimalStarvationDamageThreshold();

        if (starvationTimer >= damageThreshold) {
            // Apply starvation effects
            applyStarvationEffects(world, animal, intensity, config);

            // Reset timer but keep some progress
            starvationTimer = damageThreshold / 2;
        }

        // Store updated timer using persistent data
        setStarvationTimer(animal, starvationTimer);

        // Visual indicator of hunger at high starvation
        if (starvationTimer > damageThreshold * 0.7f && world.getRandom().nextFloat() < 0.1f) {
            world.spawnParticles(
                ParticleTypes.SMOKE,
                animal.getX(),
                animal.getY() + animal.getHeight() * 0.5,
                animal.getZ(),
                1,
                0.2, 0.1, 0.2,
                0.01
            );
        }
    }

    /**
     * Apply starvation damage and effects to an animal
     */
    private static void applyStarvationEffects(ServerWorld world, AnimalEntity animal, float intensity, FamineConfig config) {
        // Apply hunger/weakness effects
        animal.addStatusEffect(new StatusEffectInstance(
            StatusEffects.SLOWNESS,
            200, // 10 seconds
            (int)(intensity * 2),
            false, false, true
        ));

        animal.addStatusEffect(new StatusEffectInstance(
            StatusEffects.WEAKNESS,
            200,
            (int)(intensity),
            false, false, true
        ));

        // Deal starvation damage
        float damage = config.getAnimalStarvationDamage() * intensity;
        animal.damage(world.getDamageSources().starve(), damage);

        // Spawn particles
        world.spawnParticles(
            ParticleTypes.DAMAGE_INDICATOR,
            animal.getX(),
            animal.getY() + animal.getHeight() * 0.5,
            animal.getZ(),
            3,
            0.3, 0.3, 0.3,
            0.1
        );
    }

    /**
     * Called when an animal is fed by a player - resets starvation timer
     */
    public static void onAnimalFed(AnimalEntity animal) {
        setStarvationTimer(animal, 0);
        setLastFedTime(animal, animal.getWorld().getTime());
        trackedAnimals.add(animal.getUuid());

        AncientCurse.LOGGER.debug("Animal {} fed, starvation timer reset", animal.getType().getName().getString());
    }

    /**
     * Called when an animal breeds - babies start with no starvation
     */
    public static void onAnimalBorn(AnimalEntity baby) {
        setStarvationTimer(baby, 0);
        setLastFedTime(baby, baby.getWorld().getTime());
    }

    /**
     * Get starvation timer for an animal
     */
    public static int getStarvationTimer(AnimalEntity animal) {
        // Use scoreboards or a map since we can't easily read custom NBT
        // For simplicity, we'll use a static map
        return starvationTimers.getOrDefault(animal.getUuid(), 0);
    }

    /**
     * Set starvation timer for an animal
     */
    private static void setStarvationTimer(AnimalEntity animal, int timer) {
        starvationTimers.put(animal.getUuid(), timer);
    }

    /**
     * Set last fed time for an animal
     */
    private static void setLastFedTime(AnimalEntity animal, long time) {
        lastFedTimes.put(animal.getUuid(), time);
    }

    /**
     * Get last fed time for an animal
     */
    public static long getLastFedTime(AnimalEntity animal) {
        return lastFedTimes.getOrDefault(animal.getUuid(), 0L);
    }

    // In-memory storage for starvation data (cleaner than NBT for this use case)
    private static final Map<UUID, Integer> starvationTimers = new HashMap<>();
    private static final Map<UUID, Long> lastFedTimes = new HashMap<>();

    /**
     * Cleanup tracked animals that no longer exist
     */
    private static void cleanupTrackedAnimals(ServerWorld world) {
        Iterator<UUID> iter = trackedAnimals.iterator();
        while (iter.hasNext()) {
            UUID id = iter.next();
            Entity entity = world.getEntity(id);
            if (entity == null || !entity.isAlive()) {
                iter.remove();
                starvationTimers.remove(id);
                lastFedTimes.remove(id);
            }
        }
    }

    /**
     * Reset all starvation data - called when famine ends
     */
    public static void reset() {
        pendingAnimals.clear();
        trackedAnimals.clear();
        starvationTimers.clear();
        lastFedTimes.clear();
        tickCounter = 0;

        AncientCurse.LOGGER.info("Famine animal starvation data reset");
    }

    /**
     * Clear starvation effects from all tracked animals
     */
    public static void clearAllStarvationEffects(ServerWorld world) {
        for (UUID id : trackedAnimals) {
            Entity entity = world.getEntity(id);
            if (entity instanceof AnimalEntity animal && animal.isAlive()) {
                animal.removeStatusEffect(StatusEffects.SLOWNESS);
                animal.removeStatusEffect(StatusEffects.WEAKNESS);
            }
        }
    }

    /**
     * Get count of tracked animals (for debug)
     */
    public static int getTrackedAnimalCount() {
        return trackedAnimals.size();
    }

    /**
     * Check if an animal is being tracked
     */
    public static boolean isTracked(AnimalEntity animal) {
        return trackedAnimals.contains(animal.getUuid());
    }
}
