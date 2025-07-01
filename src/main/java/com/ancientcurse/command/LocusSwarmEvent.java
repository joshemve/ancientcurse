package com.ancientcurse.command;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.ModEntities;
import com.ancientcurse.entity.LocusEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.registry.tag.BlockTags;

import java.util.*;

public class LocusSwarmEvent {
    // Constants (move to config later)
    private static final int TICKS_PER_SECOND = 20;
    private static final int DISCOVERY_ERUPTION_DELAY = 5 * TICKS_PER_SECOND; // More buildup
    private static final int ERUPTION_DURATION = 8 * TICKS_PER_SECOND; // Longer eruption
    private static final int BASE_SWARM_SIZE = 30; // Start smaller for performance
    private static final int PARTICLE_THROTTLE_TICKS = 10; // Less frequent particles
    private static final int MAX_SWARM_ABSOLUTE = 100; // Hard cap for server performance
    private static final int CHUNK_CHECK_RADIUS = 3; // Only spawn in loaded chunks
    
    // Event phases
    private enum Phase { 
        DORMANT, 
        DISCOVERED, 
        ERUPTING, 
        SWARMING 
    }
    
    private final ServerWorld world;
    private final Vec3d originPoint;
    private final int territoryRadius;
    private final Map<UUID, LocusEntity> swarmMembers;
    private final net.minecraft.util.math.random.Random random;
    
    private Phase currentPhase = Phase.DORMANT;
    private int phaseTicks = 0;
    private int tickCounter = 0;
    private boolean scoutShown = false;
    private PlayerEntity triggerPlayer = null;
    private boolean cancelled = false;
    
    // Nest locations
    private final List<BlockPos> nestCenters;
    private final Map<BlockPos, Integer> nestIntensity;
    
    // Task scheduling
    private final Map<Integer, List<Runnable>> scheduledTasks = new HashMap<>();
    
    // Dynamic difficulty
    private int maxSwarmSize;
    private float intensity = 1.0f;
    private boolean cinematicMode = false;
    
    // Reusable vectors for performance
    private final Vec3d tmpVec1 = new Vec3d(0, 0, 0);
    private final Vec3d tmpVec2 = new Vec3d(0, 0, 0);
    
    public LocusSwarmEvent(ServerWorld world, Vec3d origin, int radius) {
        this.world = world;
        this.originPoint = origin;
        this.territoryRadius = radius;
        this.swarmMembers = new HashMap<>(); // Single-threaded, no need for concurrent
        this.random = world.random; // Use world's random for consistency
        this.nestCenters = new ArrayList<>();
        this.nestIntensity = new HashMap<>();
        
        // Scale with player count and territory, but cap for performance
        int playerCount = world.getPlayers().size();
        this.maxSwarmSize = Math.min(
            BASE_SWARM_SIZE + (playerCount * 2), 
            MAX_SWARM_ABSOLUTE
        );
    }
    
    public void beginInfestation() {
        currentPhase = Phase.DORMANT;
        createNestingSites();
        
        // Schedule subtle pre-eruption hints
        scheduleSubtleHints();
        
        AncientCurse.LOGGER.info("Locust infestation begun at " + originPoint + " (max size: " + maxSwarmSize + ")");
    }
    
    private void createNestingSites() {
        int nestCount = MathHelper.nextBetween(random, 3, 5);
        
        for (int i = 0; i < nestCount; i++) {
            double angle = (2 * Math.PI * i) / nestCount + random.nextDouble() * 0.5;
            double distance = MathHelper.nextBetween(random, 5, 20);
            
            int x = (int)(originPoint.x + Math.cos(angle) * distance);
            int z = (int)(originPoint.z + Math.sin(angle) * distance);
            
            BlockPos ground = world.getTopPosition(
                net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                new BlockPos(x, 0, z)
            );
            
            nestCenters.add(ground);
            nestIntensity.put(ground, MathHelper.nextBetween(random, 10, 30));
            
            createDisturbedGround(ground);
        }
    }
    
    private void createDisturbedGround(BlockPos center) {
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                if (x*x + z*z <= 9) {
                    BlockPos pos = center.add(x, 0, z);
                    BlockState current = world.getBlockState(pos);
                    
                    if (current.isOf(Blocks.GRASS_BLOCK) || current.isOf(Blocks.DIRT)) {
                        world.setBlockState(pos, Blocks.COARSE_DIRT.getDefaultState());
                    } else if (current.isOf(Blocks.SAND)) {
                        if (random.nextDouble() < 0.3) {
                            world.setBlockState(pos.down(), Blocks.SAND.getDefaultState());
                        }
                    }
                }
            }
        }
    }
    
    private void scheduleSubtleHints() {
        // Occasional skittering sounds before discovery
        for (int i = 0; i < 10; i++) {
            int delay = MathHelper.nextBetween(random, 5 * TICKS_PER_SECOND, 30 * TICKS_PER_SECOND);
            scheduleTask(() -> {
                if (currentPhase == Phase.DORMANT && !cancelled) {
                    BlockPos nest = nestCenters.get(random.nextInt(nestCenters.size()));
                    world.playSound(null, nest, SoundEvents.BLOCK_GRAVEL_BREAK, 
                        SoundCategory.AMBIENT, 0.1f, 0.3f);
                }
            }, delay);
        }
    }
    
    public void updateSwarmBehavior() {
        if (cancelled) return;
        
        tickCounter++;
        phaseTicks++;
        
        // Process scheduled tasks
        runScheduledTasks();
        
        switch (currentPhase) {
            case DORMANT -> updateDormantPhase();
            case DISCOVERED -> updateDiscoveredPhase();
            case ERUPTING -> updateEruptingPhase();
            case SWARMING -> updateSwarmingPhase();
        }
        
        updateActiveLocusts();
    }
    
    private void updateDormantPhase() {
        // Check for nearby players - throttled to every 10 ticks
        if (phaseTicks % 10 == 0) {
            for (BlockPos nest : nestCenters) {
                // Only check if chunk is loaded
                if (!world.isChunkLoaded(nest)) continue;
                
                List<ServerPlayerEntity> nearbyPlayers = world.getEntitiesByClass(
                    ServerPlayerEntity.class,
                    new Box(Vec3d.of(nest).add(-8, -2, -8), Vec3d.of(nest).add(8, 5, 8)),
                    player -> !player.isSpectator() && !player.isCreative()
                );
                
                if (!nearbyPlayers.isEmpty() && !scoutShown) {
                    triggerPlayer = nearbyPlayers.get(0);
                    triggerDiscovery(nest);
                    return;
                }
            }
        }
        
        // Subtle hints - heavily throttled
        if (phaseTicks % (10 * TICKS_PER_SECOND) == 0 && random.nextFloat() < 0.3) {
            BlockPos randomNest = nestCenters.get(random.nextInt(nestCenters.size()));
            
            if (world.isChunkLoaded(randomNest)) {
                world.spawnParticles(
                    new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.SAND.getDefaultState()),
                    randomNest.getX() + 0.5,
                    randomNest.getY() + 1.1,
                    randomNest.getZ() + 0.5,
                    3, 0.5, 0.1, 0.5, 0.01
                );
            }
        }
    }
    
    private void triggerDiscovery(BlockPos discoveredNest) {
        currentPhase = Phase.DISCOVERED;
        phaseTicks = 0;
        scoutShown = true;
        
        LocusEntity scout = ModEntities.LOCUS.create(world);
        if (scout == null) return; // Null safety
        
        Vec3d emergePos = Vec3d.ofCenter(discoveredNest).add(0, 0.5, 0);
        scout.setPosition(emergePos);
        
        if (world.spawnEntity(scout)) {
            // Emergence particles
            for (int i = 0; i < 20; i++) {
                world.spawnParticles(
                    new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.COARSE_DIRT.getDefaultState()),
                    emergePos.x, emergePos.y, emergePos.z,
                    10, 0.3, 0.3, 0.3, 0.1
                );
            }
            
            if (triggerPlayer != null) {
                // Scout behavior: circle then burrow
                scout.getLookControl().lookAt(triggerPlayer);
                scout.setAiDisabled(true);
                
                triggerPlayer.sendMessage(
                    Text.literal("§oThe creature's compound eyes fix upon you...").formatted(Formatting.GRAY, Formatting.ITALIC),
                    true
                );
                
                // Make scout circle player before burrowing
                scheduleTask(() -> circleAndBurrow(scout), TICKS_PER_SECOND);
            }
        }
    }
    
    private void circleAndBurrow(LocusEntity scout) {
        if (cancelled || scout.isRemoved()) return;
        
        // Simple circle motion
        scout.setAiDisabled(false);
        if (triggerPlayer != null) {
            scout.setTarget(triggerPlayer);
        }
        
        // Burrow after another second
        scheduleTask(() -> {
            if (!cancelled && !scout.isRemoved()) {
                for (int i = 0; i < 20; i++) {
                    world.spawnParticles(
                        new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.COARSE_DIRT.getDefaultState()),
                        scout.getX(), scout.getY(), scout.getZ(),
                        10, 0.3, 0.3, 0.3, 0.1
                    );
                }
                scout.discard();
                startGroundRumbling();
            }
        }, TICKS_PER_SECOND);
    }
    
    private void startGroundRumbling() {
        // Throttled particle effects with staggered timing
        int delay = 0;
        for (BlockPos nest : nestCenters) {
            scheduleTask(() -> {
                if (!cancelled && currentPhase == Phase.DISCOVERED) {
                    world.spawnParticles(
                        ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        nest.getX() + 0.5,
                        nest.getY() + 1.1,
                        nest.getZ() + 0.5,
                        3, 1.5, 0.1, 1.5, 0.01
                    );
                }
            }, delay);
            delay += 2; // Stagger by 2 ticks
        }
        
        // Heartbeat with distance-based volume
        for (int i = 0; i < 3; i++) {
            int beatDelay = (3 - i) * 15;
            final int beatIndex = i; // Create final copy for lambda
            scheduleTask(() -> {
                if (!cancelled) {
                    List<ServerPlayerEntity> nearbyPlayers = getNearbyPlayers();
                    for (ServerPlayerEntity player : nearbyPlayers) {
                        // Calculate distance-based volume
                        double minDist = Double.MAX_VALUE;
                        for (BlockPos nest : nestCenters) {
                            double dist = player.squaredDistanceTo(Vec3d.ofCenter(nest));
                            minDist = Math.min(minDist, dist);
                        }
                        
                        float baseVolume = 0.5f + (beatIndex * 0.2f);
                        float distanceFactor = (float) Math.max(0.1, 1.0 - (minDist / 1024)); // Falloff over 32 blocks
                        float volume = baseVolume * distanceFactor;
                        
                        if (volume > 0.1f) {
                            player.playSound(SoundEvents.ENTITY_WARDEN_HEARTBEAT, 
                                SoundCategory.HOSTILE, volume, 0.5f);
                        }
                    }
                }
            }, beatDelay);
        }
        
        // Warning message
        getNearbyPlayers().forEach(player -> 
            player.sendMessage(
                Text.literal("§cThe ground begins to tremble...").formatted(Formatting.ITALIC),
                false
            )
        );
    }
    
    private void updateDiscoveredPhase() {
        if (phaseTicks >= DISCOVERY_ERUPTION_DELAY) {
            triggerEruption();
        } else if (phaseTicks % PARTICLE_THROTTLE_TICKS == 0) {
            // Throttled rumbling effects
            for (BlockPos nest : nestCenters) {
                world.spawnParticles(
                    new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.COARSE_DIRT.getDefaultState()),
                    nest.getX() + 0.5,
                    nest.getY() + 1.5,
                    nest.getZ() + 0.5,
                    Math.min(phaseTicks / 2, 20), 1.0, 0.2, 1.0, 0.05
                );
                
                if (phaseTicks % TICKS_PER_SECOND == 0) {
                    world.playSound(null, nest, SoundEvents.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 
                        SoundCategory.HOSTILE, 0.3f, 0.5f);
                }
            }
        }
    }
    
    private void triggerEruption() {
        currentPhase = Phase.ERUPTING;
        phaseTicks = 0;
        
        List<ServerPlayerEntity> nearbyPlayers = getNearbyPlayers();
        
        for (ServerPlayerEntity player : nearbyPlayers) {
            player.sendMessage(
                Text.literal("§4§lTHEY'RE BREAKING THROUGH!").formatted(Formatting.BOLD),
                true
            );
            player.playSound(SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 1.0f, 0.7f);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 20, 0));
        }
        
        // Break ground at nests - more dramatic in cinematic mode
        for (BlockPos nest : nestCenters) {
            int radius = cinematicMode ? 2 : 1;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = nest.add(x, 0, z);
                    if (random.nextDouble() < (cinematicMode ? 0.9 : 0.7)) {
                        world.breakBlock(pos, false); // No drops
                        
                        // Extra particles in cinematic mode
                        if (cinematicMode && random.nextFloat() < 0.5) {
                            world.spawnParticles(
                                ParticleTypes.EXPLOSION,
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                1, 0, 0, 0, 0
                            );
                        }
                    }
                }
            }
        }
    }
    
    private void updateEruptingPhase() {
        if (swarmMembers.size() < maxSwarmSize && phaseTicks < ERUPTION_DURATION) {
            // Staggered spawning for better visuals and performance
            if (phaseTicks % 5 == 0) { // Spawn every 5 ticks instead of every tick
                // Dynamic spawn rate based on phase progress
                int phaseProgress = phaseTicks * 100 / ERUPTION_DURATION;
                int baseSpawns = phaseProgress < 30 ? 1 : phaseProgress < 70 ? 2 : 3;
                int spawnsThisTick = Math.min(baseSpawns, maxSwarmSize - swarmMembers.size());
                
                // Reduce spawns if TPS is low (simplified check)
                if (world.getPlayers().size() > 10) {
                    spawnsThisTick = Math.max(1, spawnsThisTick / 2);
                }
                
                for (int i = 0; i < spawnsThisTick; i++) {
                    if (swarmMembers.size() >= maxSwarmSize) break;
                    
                    // Use nest intensity for weighted selection
                    BlockPos selectedNest = selectNestByIntensity();
                    if (world.isChunkLoaded(selectedNest)) {
                        spawnEruptingLocus(selectedNest);
                    }
                }
            }
        }
        
        if (phaseTicks >= ERUPTION_DURATION) {
            currentPhase = Phase.SWARMING;
            phaseTicks = 0;
            
            world.getPlayers().forEach(player -> {
                // Epic announcement with cinematic flair
                if (cinematicMode) {
                    player.sendMessage(
                        Text.literal("§4§l☥ THE EIGHTH PLAGUE HAS RISEN ☥").formatted(Formatting.BOLD),
                        false
                    );
                    player.sendMessage(
                        Text.literal("§c§oThe sky darkens with a living cloud of destruction...").formatted(Formatting.ITALIC),
                        false
                    );
                    // Add darkness effect for atmosphere
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 100, 0));
                } else {
                    player.sendMessage(
                        Text.literal("§4☥ THE EIGHTH PLAGUE HAS RISEN ☥").formatted(Formatting.BOLD),
                        false
                    );
                }
            });
        }
    }
    
    private void spawnEruptingLocus(BlockPos nest) {
        // 30% chance to spawn babies in early eruption (affected by intensity)
        if (phaseTicks < 40 && random.nextFloat() < 0.3 * intensity) {
            com.ancientcurse.entity.BabyLocusEntity baby = ModEntities.BABY_LOCUS.create(world);
            if (baby == null) return;
            
            Vec3d spawnPos = Vec3d.ofCenter(nest).add(
                random.nextGaussian() * 0.5,
                0.1, // Start at ground level
                random.nextGaussian() * 0.5
            );
            
            baby.setPosition(spawnPos);
            baby.setEmerging(true); // Use emergence animation
            baby.setExperiencePoints(0); // No XP for event mobs
            
            if (world.spawnEntity(baby)) {
                // Don't track babies in swarmMembers - they'll grow up
            }
            
            return;
        }
        
        // Regular adult spawning continues...
        LocusEntity locus = ModEntities.LOCUS.create(world);
        if (locus == null) return; // Null safety
        
        Vec3d spawnPos = Vec3d.ofCenter(nest).add(
            random.nextGaussian() * 0.5,
            1.0, // Fixed: proper height above ground
            random.nextGaussian() * 0.5
        );
        
        locus.setPosition(spawnPos);
        locus.setVelocity(new Vec3d(
            random.nextGaussian() * 0.2,
            MathHelper.nextBetween(random, 0.5f, 0.8f),
            random.nextGaussian() * 0.2
        ));
        
        // Mark as event entity - no XP
        locus.setExperiencePoints(0);
        
        if (world.spawnEntity(locus)) {
            swarmMembers.put(locus.getUuid(), locus);
            
            // Dust particles as they emerge
            scheduleTask(() -> {
                if (!locus.isRemoved()) {
                    for (int i = 0; i < 3; i++) {
                        scheduleTask(() -> {
                            if (!locus.isRemoved()) {
                                world.spawnParticles(
                                    new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.COARSE_DIRT.getDefaultState()),
                                    locus.getX(), locus.getY() + 0.5, locus.getZ(),
                                    cinematicMode ? 5 : 3, 0.2, 0.2, 0.2, 0.01
                                );
                            }
                        }, i * 10);
                    }
                }
            }, 0);
            
            // Alpha variant chance based on intensity and cinematic mode
            float alphaChance = cinematicMode ? 0.15f : 0.1f;
            if (random.nextFloat() < alphaChance * intensity) {
                locus.setHealth(locus.getMaxHealth() * 1.5f);
                locus.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SPEED, Integer.MAX_VALUE, 0, false, false
                ));
                
                // Visual indicator for alpha variants in cinematic mode
                if (cinematicMode) {
                    locus.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.GLOWING, Integer.MAX_VALUE, 0, false, false
                    ));
                }
            }
        }
    }
    
    private void updateSwarmingPhase() {
        // Aggressive targeting
        for (LocusEntity locus : swarmMembers.values()) {
            if (locus != null && !locus.isRemoved() && locus.getTarget() == null && random.nextFloat() < 0.1) {
                PlayerEntity target = world.getClosestPlayer(locus, 32);
                if (target != null && !target.isCreative()) {
                    locus.setTarget(target);
                }
            }
        }
        
        // Environmental destruction (throttled)
        if (phaseTicks % (5 * TICKS_PER_SECOND) == 0) {
            destroyVegetation();
        }
    }
    
    private BlockPos selectNestByIntensity() {
        // Weight nest selection by intensity
        int totalWeight = nestIntensity.values().stream().mapToInt(Integer::intValue).sum();
        int selection = random.nextInt(totalWeight);
        
        int current = 0;
        for (Map.Entry<BlockPos, Integer> entry : nestIntensity.entrySet()) {
            current += entry.getValue();
            if (selection < current) {
                return entry.getKey();
            }
        }
        
        // Fallback
        return nestCenters.get(0);
    }
    private void destroyVegetation() {
        // Only process if there are active locusts nearby
        if (swarmMembers.isEmpty()) return;
        
        // Pick a random active locust to center destruction around
        List<LocusEntity> activeLocusts = swarmMembers.values().stream()
            .filter(l -> l != null && !l.isRemoved())
            .limit(5) // Only check first 5 for performance
            .toList();
        
        if (activeLocusts.isEmpty()) return;
        
        LocusEntity centerLocust = activeLocusts.get(random.nextInt(activeLocusts.size()));
        BlockPos centerPos = centerLocust.getBlockPos();
        
        if (!world.isChunkLoaded(centerPos)) return;
        
        // Smaller, more focused destruction
        boolean anythingDestroyed = false;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = -1; y <= 1; y++) {
                    if (random.nextFloat() > 0.3) continue; // 30% chance per block
                    
                    BlockPos checkPos = centerPos.add(x, y, z);
                    BlockState state = world.getBlockState(checkPos);
                    
                    // Focus on crops and grass for biblical accuracy
                    if (state.isOf(Blocks.WHEAT) || 
                        state.isOf(Blocks.CARROTS) || 
                        state.isOf(Blocks.POTATOES) ||
                        state.isOf(Blocks.BEETROOTS) ||
                        state.isOf(Blocks.GRASS) ||
                        state.isOf(Blocks.TALL_GRASS)) {
                        world.breakBlock(checkPos, false); // No drops
                        anythingDestroyed = true;
                        
                        // Small chance to damage farmland too
                        if (random.nextFloat() < 0.1) {
                            BlockPos below = checkPos.down();
                            if (world.getBlockState(below).isOf(Blocks.FARMLAND)) {
                                world.setBlockState(below, Blocks.DIRT.getDefaultState());
                            }
                        }
                    }
                }
            }
        }
        
        // More subtle visual feedback
        if (anythingDestroyed && random.nextFloat() < 0.5) {
            world.spawnParticles(
                ParticleTypes.SMOKE,
                centerPos.getX() + 0.5,
                centerPos.getY() + 0.5,
                centerPos.getZ() + 0.5,
                5, 1.0, 0.5, 1.0, 0.01
            );
        }
    }
    
    private void updateActiveLocusts() {
        Iterator<Map.Entry<UUID, LocusEntity>> iterator = swarmMembers.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<UUID, LocusEntity> entry = iterator.next();
            LocusEntity locus = entry.getValue();
            
            if (locus == null || !locus.isAlive() || locus.isRemoved()) {
                iterator.remove();
                continue;
            }
            
            if (currentPhase == Phase.SWARMING) {
                applySwarmBehavior(locus);
            }
        }
    }
    
    private void applySwarmBehavior(LocusEntity locus) {
        if (locus == null || locus.isRemoved()) return;
        
        // Skip every other tick for performance
        if (locus.age % 2 != 0) return;
        
        Vec3d center = Vec3d.ZERO;
        Vec3d avoidance = Vec3d.ZERO;
        Vec3d alignment = Vec3d.ZERO;
        int neighbors = 0;
        
        // Limit neighbor checks for performance
        int checked = 0;
        for (LocusEntity other : swarmMembers.values()) {
            if (checked++ > 10) break; // Check max 10 neighbors
            
            if (other != null && other != locus && !other.isRemoved()) {
                double distSq = other.squaredDistanceTo(locus);
                if (distSq < 100) { // 10 block radius
                    center = center.add(other.getPos());
                    alignment = alignment.add(other.getVelocity());
                    neighbors++;
                    
                    if (distSq < 6) { // Separation distance
                        Vec3d away = locus.getPos().subtract(other.getPos()).normalize();
                        avoidance = avoidance.add(away.multiply(6 / distSq)); // Stronger avoidance when closer
                    }
                }
            }
        }
        
        if (neighbors > 0) {
            // Classic boid behavior: cohesion, alignment, separation
            center = center.multiply(1.0 / neighbors);
            alignment = alignment.multiply(1.0 / neighbors);
            
            Vec3d cohesion = center.subtract(locus.getPos()).normalize().multiply(0.02);
            Vec3d alignVec = alignment.normalize().multiply(0.03);
            Vec3d separation = avoidance.multiply(0.15);
            
            // Add some vertical variation for more realistic movement
            double verticalNoise = (random.nextDouble() - 0.5) * 0.02;
            
            Vec3d velocity = locus.getVelocity()
                .add(cohesion)
                .add(alignVec)
                .add(separation)
                .add(0, verticalNoise, 0);
            
            // Limit speed
            double speed = velocity.length();
            if (speed > 0.5) {
                velocity = velocity.normalize().multiply(0.5);
            }
            
            locus.setVelocity(velocity);
        }
    }
    
    private List<ServerPlayerEntity> getNearbyPlayers() {
        return world.getEntitiesByClass(
            ServerPlayerEntity.class,
            new Box(originPoint.add(-territoryRadius, -50, -territoryRadius), 
                    originPoint.add(territoryRadius, 50, territoryRadius)),
            player -> !player.isSpectator() && !player.isCreative()
        );
    }
    
    private void scheduleTask(Runnable task, int delayTicks) {
        // Note: Tasks scheduled within the same tick will execute next tick
        int executeTick = tickCounter + delayTicks;
        scheduledTasks.computeIfAbsent(executeTick, k -> new ArrayList<>()).add(task);
    }
    
    private void runScheduledTasks() {
        List<Runnable> tasks = scheduledTasks.remove(tickCounter);
        if (tasks != null && !cancelled) {
            tasks.forEach(Runnable::run);
        }
    }
    
    public void naturalDispersion() {
        cancelled = true;
        currentPhase = Phase.DORMANT;
        swarmMembers.values().forEach(LocusEntity::discard);
        swarmMembers.clear();
        scheduledTasks.clear();
        triggerPlayer = null; // Clear stale reference
    }
    
    public void reset() {
        naturalDispersion();
        tickCounter = 0;
        phaseTicks = 0;
        scoutShown = false;
        triggerPlayer = null;
        nestCenters.clear();
        nestIntensity.clear();
        cancelled = false;
    }
    
    // Getters for command info
    public boolean isActive() {
        return !cancelled && (currentPhase != Phase.DORMANT || !nestCenters.isEmpty());
    }
    
    public int getSwarmSize() {
        return swarmMembers.size();
    }
    
    public float getSwarmAgitation() {
        return currentPhase == Phase.SWARMING ? 1.0f : 0.0f;
    }
    
    public String getPhaseDescription() {
        return switch(currentPhase) {
            case DORMANT -> "Eggs dormant underground";
            case DISCOVERED -> "Nest disturbed!";
            case ERUPTING -> "SWARM ERUPTING!";
            case SWARMING -> "Full plague active";
        };
    }
    
    public int getDormantEggs() {
        if (currentPhase == Phase.DORMANT) {
            return nestCenters.size() * 20; // Estimate based on nest count
        } else if (currentPhase == Phase.DISCOVERED) {
            return nestCenters.size() * 15; // Some are preparing to emerge
        }
        return 0;
    }
    
    public Vec3d getSwarmCenter() {
        return originPoint;
    }
    
    public void setCinematicMode(boolean cinematic) {
        this.cinematicMode = cinematic;
    }
    
    public void setIntensity(float intensity) {
        this.intensity = MathHelper.clamp(intensity, 0.1f, 2.0f);
        // Adjust max swarm size based on intensity
        this.maxSwarmSize = (int)(BASE_SWARM_SIZE * intensity);
        this.maxSwarmSize = Math.min(this.maxSwarmSize, MAX_SWARM_ABSOLUTE);
    }
}