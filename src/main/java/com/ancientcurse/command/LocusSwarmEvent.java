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
    private static final int DISCOVERY_ERUPTION_DELAY = 3 * TICKS_PER_SECOND;
    private static final int ERUPTION_DURATION = 5 * TICKS_PER_SECOND;
    private static final int BASE_SWARM_SIZE = 40;
    private static final int PARTICLE_THROTTLE_TICKS = 5;
    
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
    private final int maxSwarmSize;
    
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
        
        // Scale with player count and territory
        int playerCount = world.getPlayers().size();
        this.maxSwarmSize = Math.min(
            BASE_SWARM_SIZE + (playerCount * 3), 
            BASE_SWARM_SIZE + (territoryRadius / 2)
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
        // Check for nearby players
        for (BlockPos nest : nestCenters) {
            List<ServerPlayerEntity> nearbyPlayers = world.getEntitiesByClass(
                ServerPlayerEntity.class,
                new Box(Vec3d.of(nest).add(-5, -2, -5), Vec3d.of(nest).add(5, 5, 5)),
                player -> !player.isSpectator() && !player.isCreative()
            );
            
            if (!nearbyPlayers.isEmpty() && !scoutShown) {
                triggerPlayer = nearbyPlayers.get(0);
                triggerDiscovery(nest);
                return;
            }
        }
        
        // Subtle hints - throttled
        if (phaseTicks % (5 * TICKS_PER_SECOND) == 0) {
            BlockPos randomNest = nestCenters.get(random.nextInt(nestCenters.size()));
            
            world.spawnParticles(
                new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.SAND.getDefaultState()),
                randomNest.getX() + 0.5,
                randomNest.getY() + 1.1, // Fixed: above ground
                randomNest.getZ() + 0.5,
                5, 0.5, 0.1, 0.5, 0.01
            );
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
        
        // Break ground at nests
        for (BlockPos nest : nestCenters) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = nest.add(x, 0, z);
                    if (random.nextDouble() < 0.7) {
                        world.breakBlock(pos, false); // No drops
                    }
                }
            }
        }
    }
    
    private void updateEruptingPhase() {
        if (swarmMembers.size() < maxSwarmSize && phaseTicks < ERUPTION_DURATION) {
            // Dynamic spawn rate based on server load
            int baseSpawns = MathHelper.nextBetween(random, 2, 4);
            int spawnsThisTick = Math.min(baseSpawns, maxSwarmSize - swarmMembers.size());
            
            // Reduce spawns if many players for performance
            if (world.getPlayers().size() > 20) {
                spawnsThisTick = Math.max(1, spawnsThisTick / 2);
            }
            
            for (int i = 0; i < spawnsThisTick; i++) {
                if (swarmMembers.size() >= maxSwarmSize) break;
                
                // Use nest intensity for weighted selection
                BlockPos selectedNest = selectNestByIntensity();
                spawnEruptingLocus(selectedNest);
            }
        }
        
        if (phaseTicks >= ERUPTION_DURATION) {
            currentPhase = Phase.SWARMING;
            phaseTicks = 0;
            
            world.getPlayers().forEach(player -> {
                player.sendMessage(
                    Text.literal("§4☥ THE EIGHTH PLAGUE HAS RISEN ☥").formatted(Formatting.BOLD),
                    false
                );
            });
        }
    }
    
    private void spawnEruptingLocus(BlockPos nest) {
        // 30% chance to spawn babies in early eruption
        if (phaseTicks < 40 && random.nextFloat() < 0.3) {
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
                                    3, 0.2, 0.2, 0.2, 0.01
                                );
                            }
                        }, i * 10);
                    }
                }
            }, 0);
            
            // 10% chance for alpha variant
            if (random.nextFloat() < 0.1) {
                locus.setHealth(locus.getMaxHealth() * 1.5f);
                locus.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SPEED, Integer.MAX_VALUE, 0, false, false
                ));
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
        // Check if chunk is loaded first
        BlockPos centerPos = new BlockPos(
            (int)(originPoint.x + random.nextGaussian() * 20), // Reduced range
            (int)originPoint.y,
            (int)(originPoint.z + random.nextGaussian() * 20)
        );
        
        if (!world.isChunkLoaded(centerPos)) return;
        
        // Destroy vegetation in area
        boolean anythingDestroyed = false;
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = -3; y <= 3; y++) {
                    BlockPos checkPos = centerPos.add(x, y, z);
                    BlockState state = world.getBlockState(checkPos);
                    
                    if (state.isIn(BlockTags.LEAVES) || 
                        state.isOf(Blocks.WHEAT) || 
                        state.isOf(Blocks.CARROTS) || 
                        state.isOf(Blocks.GRASS)) {
                        world.breakBlock(checkPos, false); // No drops
                        anythingDestroyed = true;
                    }
                }
            }
        }
        
        // Visual feedback - dust cloud
        if (anythingDestroyed) {
            world.spawnParticles(
                new BlockStateParticleEffect(ParticleTypes.FALLING_DUST, Blocks.DIRT.getDefaultState()),
                centerPos.getX(),
                centerPos.getY() + 2,
                centerPos.getZ(),
                20, 3.0, 0.0, 3.0, 0.1
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
        
        Vec3d center = Vec3d.ZERO;
        Vec3d avoidance = Vec3d.ZERO;
        int neighbors = 0;
        
        for (LocusEntity other : swarmMembers.values()) {
            if (other != null && other != locus && !other.isRemoved() && other.squaredDistanceTo(locus) < 64) {
                center = center.add(other.getPos());
                neighbors++;
                
                if (other.squaredDistanceTo(locus) < 4) {
                    Vec3d away = locus.getPos().subtract(other.getPos()).normalize();
                    avoidance = avoidance.add(away);
                }
            }
        }
        
        if (neighbors > 0) {
            center = center.multiply(1.0 / neighbors);
            Vec3d toCenter = center.subtract(locus.getPos()).normalize().multiply(0.05);
            Vec3d velocity = locus.getVelocity().add(toCenter).add(avoidance.multiply(0.1));
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
        return !cancelled && (!swarmMembers.isEmpty() || currentPhase != Phase.DORMANT);
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
            return nestCenters.size() * 20;
        }
        return 0;
    }
    
    public Vec3d getSwarmCenter() {
        return originPoint;
    }
}