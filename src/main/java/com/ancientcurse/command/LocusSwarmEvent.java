package com.ancientcurse.command;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.ModEntities;
import com.ancientcurse.entity.LocusEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LocusSwarmEvent {
    private final ServerWorld world;
    private final Vec3d originPoint;
    private final int territoryRadius;
    private final Map<UUID, LocusEntity> swarmMembers;
    private final Random random;
    
    // Natural swarm behavior parameters
    private static final int MAX_SWARM_SIZE = 60;
    private static final int NATURAL_SPAWN_RADIUS = 150;
    private static final double SWARM_COHESION = 0.15;
    private static final double PREDATOR_INSTINCT = 0.8;
    
    // Swarm lifecycle (more organic timing)
    private int swarmAge = 0;
    private int lastMigrationCheck = 0;
    private Vec3d swarmCenter;
    private Vec3d migrationTarget;
    private boolean isFeeding = false;
    private int hungerLevel = 0;
    private float swarmAgitation = 0.0f;
    
    // Environmental factors
    private float windStrength = 0.0f;
    private Vec3d windDirection;
    private long lastWeatherUpdate = 0;
    
    // Natural spawn patterns
    private final List<Vec3d> nestingGrounds;
    private int dormantEggs = 0;
    
    public LocusSwarmEvent(ServerWorld world, Vec3d origin, int radius) {
        this.world = world;
        this.originPoint = origin;
        this.territoryRadius = radius;
        this.swarmMembers = new ConcurrentHashMap<>();
        this.random = new Random();
        this.swarmCenter = origin;
        this.migrationTarget = origin;
        this.nestingGrounds = new ArrayList<>();
        this.windDirection = new Vec3d(random.nextGaussian(), 0, random.nextGaussian()).normalize();
        
        // Find natural nesting spots
        findNestingGrounds();
    }
    
    public void beginInfestation() {
        // Subtle environmental changes first
        createInitialNests();
        
        // Start with dormant eggs that will hatch over time
        dormantEggs = 30 + random.nextInt(40);
        
        AncientCurse.LOGGER.info("Locus infestation beginning at " + originPoint);
    }
    
    public void updateSwarmBehavior() {
        swarmAge++;
        
        // Natural lifecycle progression
        updateEnvironmentalFactors();
        processEggHatching();
        updateSwarmMovement();
        regulateSwarmSize();
        
        // Individual behavior updates
        for (LocusEntity locus : swarmMembers.values()) {
            if (!locus.isAlive()) {
                swarmMembers.remove(locus.getUuid());
                continue;
            }
            
            applySwarmIntelligence(locus);
            applyEnvironmentalForces(locus);
        }
        
        // Migration patterns
        if (swarmAge - lastMigrationCheck > 600) { // Every 30 seconds
            considerMigration();
            lastMigrationCheck = swarmAge;
        }
    }
    
    private void findNestingGrounds() {
        // Look for dark, sheltered areas
        for (int i = 0; i < 5; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = 20 + random.nextDouble() * 40;
            
            int x = (int)(originPoint.x + Math.cos(angle) * distance);
            int z = (int)(originPoint.z + Math.sin(angle) * distance);
            
            BlockPos ground = world.getTopPosition(
                net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                new BlockPos(x, 0, z)
            );
            
            // Check if it's a good nesting spot (under trees, in caves, etc)
            if (world.getLightLevel(ground) < 7) {
                nestingGrounds.add(Vec3d.ofCenter(ground));
            }
        }
    }
    
    private void createInitialNests() {
        for (Vec3d nestPos : nestingGrounds) {
            // Subtle particle effects suggesting eggs/nests
            for (int i = 0; i < 30; i++) {
                world.spawnParticles(
                    ParticleTypes.MYCELIUM,
                    nestPos.x + random.nextGaussian() * 2,
                    nestPos.y,
                    nestPos.z + random.nextGaussian() * 2,
                    1, 0, 0, 0, 0
                );
            }
        }
    }
    
    private void processEggHatching() {
        if (dormantEggs <= 0 || swarmMembers.size() >= MAX_SWARM_SIZE) return;
        
        // Natural hatching rate influenced by swarm size and time
        double hatchChance = 0.02 + (swarmAge / 6000.0) * 0.03;
        
        if (random.nextDouble() < hatchChance) {
            Vec3d hatchLocation = nestingGrounds.isEmpty() ? 
                originPoint : nestingGrounds.get(random.nextInt(nestingGrounds.size()));
            
            emergeFromNest(hatchLocation);
            dormantEggs--;
        }
    }
    
    private void emergeFromNest(Vec3d nestLocation) {
        // Multiple locusts emerge together
        int emergeCount = 1 + random.nextInt(3);
        
        for (int i = 0; i < emergeCount && swarmMembers.size() < MAX_SWARM_SIZE; i++) {
            Vec3d emergePos = nestLocation.add(
                random.nextGaussian() * 2,
                0.5 + random.nextDouble(),
                random.nextGaussian() * 2
            );
            
            LocusEntity locus = ModEntities.LOCUS.create(world);
            if (locus != null) {
                locus.setPosition(emergePos);
                
                // Newly hatched are smaller/weaker
                if (swarmAge < 1200) { // First minute
                    locus.setBaby(true);
                    locus.setHealth(locus.getMaxHealth() * 0.6f);
                }
                
                if (world.spawnEntity(locus)) {
                    swarmMembers.put(locus.getUuid(), locus);
                    
                    // Emergence particles
                    for (int j = 0; j < 10; j++) {
                        world.spawnParticles(
                            ParticleTypes.BLOCK,
                            emergePos.x, emergePos.y, emergePos.z,
                            1, 0.2, 0.2, 0.2, 0.05,
                            world.getBlockState(new BlockPos(emergePos).down())
                        );
                    }
                }
            }
        }
    }
    
    private void updateSwarmMovement() {
        if (swarmMembers.isEmpty()) return;
        
        // Calculate swarm center
        Vec3d centerSum = Vec3d.ZERO;
        int activeCount = 0;
        
        for (LocusEntity locus : swarmMembers.values()) {
            if (locus.isAlive()) {
                centerSum = centerSum.add(locus.getPos());
                activeCount++;
            }
        }
        
        if (activeCount > 0) {
            swarmCenter = centerSum.multiply(1.0 / activeCount);
        }
        
        // Update swarm agitation based on threats
        updateSwarmAgitation();
        
        // Natural foraging behavior
        if (!isFeeding && hungerLevel > 50) {
            seekFoodSource();
        }
    }
    
    private void updateSwarmAgitation() {
        // Check for threats near swarm
        List<PlayerEntity> nearbyThreats = world.getEntitiesByClass(
            PlayerEntity.class,
            new Box(swarmCenter.add(-20, -10, -20), swarmCenter.add(20, 10, 20)),
            player -> !player.isSpectator() && !player.isCreative()
        );
        
        // Gradual agitation increase/decrease
        if (!nearbyThreats.isEmpty()) {
            swarmAgitation = Math.min(1.0f, swarmAgitation + 0.02f);
        } else {
            swarmAgitation = Math.max(0.0f, swarmAgitation - 0.01f);
        }
        
        // Hunger increases agitation
        swarmAgitation += (hungerLevel / 1000.0f);
        swarmAgitation = Math.min(1.0f, swarmAgitation);
    }
    
    private void applySwarmIntelligence(LocusEntity locus) {
        Vec3d velocity = locus.getVelocity();
        
        // 1. Cohesion - move toward swarm center
        Vec3d toCenter = swarmCenter.subtract(locus.getPos());
        if (toCenter.length() > 10) {
            velocity = velocity.add(toCenter.normalize().multiply(SWARM_COHESION * 0.5));
        }
        
        // 2. Separation - avoid crowding
        for (LocusEntity other : swarmMembers.values()) {
            if (other != locus && other.squaredDistanceTo(locus) < 4) {
                Vec3d away = locus.getPos().subtract(other.getPos());
                velocity = velocity.add(away.normalize().multiply(0.1));
            }
        }
        
        // 3. Alignment - match nearby velocities
        Vec3d avgVelocity = Vec3d.ZERO;
        int neighbors = 0;
        
        for (LocusEntity other : swarmMembers.values()) {
            if (other != locus && other.squaredDistanceTo(locus) < 64) {
                avgVelocity = avgVelocity.add(other.getVelocity());
                neighbors++;
            }
        }
        
        if (neighbors > 0) {
            avgVelocity = avgVelocity.multiply(1.0 / neighbors);
            velocity = velocity.add(avgVelocity.subtract(velocity).multiply(0.05));
        }
        
        // 4. Predator response based on agitation
        if (swarmAgitation > 0.3 && random.nextDouble() < swarmAgitation) {
            PlayerEntity nearestThreat = world.getClosestPlayer(locus, 32);
            if (nearestThreat != null && !nearestThreat.isCreative()) {
                locus.setTarget(nearestThreat);
                
                // Aggressive pursuit when highly agitated
                if (swarmAgitation > 0.7) {
                    Vec3d toTarget = nearestThreat.getPos().subtract(locus.getPos());
                    velocity = velocity.add(toTarget.normalize().multiply(PREDATOR_INSTINCT * swarmAgitation));
                }
            }
        }
        
        // Apply final velocity with limits
        velocity = new Vec3d(
            Math.max(-0.5, Math.min(0.5, velocity.x)),
            Math.max(-0.3, Math.min(0.3, velocity.y)),
            Math.max(-0.5, Math.min(0.5, velocity.z))
        );
        
        locus.setVelocity(velocity);
    }
    
    private void applyEnvironmentalForces(LocusEntity locus) {
        // Wind affects flying locusts
        if (!locus.isOnGround()) {
            Vec3d windForce = windDirection.multiply(windStrength * 0.02);
            locus.addVelocity(windForce.x, windForce.y * 0.5, windForce.z);
        }
        
        // Rain makes them seek shelter
        if (world.isRaining() && !locus.isOnGround()) {
            locus.addVelocity(0, -0.02, 0);
        }
    }
    
    private void updateEnvironmentalFactors() {
        if (System.currentTimeMillis() - lastWeatherUpdate > 5000) {
            // Gradual wind changes
            windStrength = Math.max(0, Math.min(1, windStrength + (random.nextFloat() - 0.5f) * 0.2f));
            
            // Slight wind direction drift
            windDirection = windDirection.add(
                (random.nextDouble() - 0.5) * 0.1,
                0,
                (random.nextDouble() - 0.5) * 0.1
            ).normalize();
            
            lastWeatherUpdate = System.currentTimeMillis();
        }
        
        // Hunger increases over time
        hungerLevel = Math.min(100, hungerLevel + 1);
    }
    
    private void seekFoodSource() {
        // Look for players or animals to feed on
        List<LivingEntity> potentialFood = world.getEntitiesByClass(
            LivingEntity.class,
            new Box(swarmCenter.add(-50, -20, -50), swarmCenter.add(50, 20, 50)),
            entity -> entity instanceof PlayerEntity || entity instanceof AnimalEntity
        );
        
        if (!potentialFood.isEmpty()) {
            LivingEntity target = potentialFood.get(random.nextInt(potentialFood.size()));
            migrationTarget = target.getPos();
            isFeeding = true;
            
            // Alert nearby swarm members
            for (LocusEntity locus : swarmMembers.values()) {
                if (locus.squaredDistanceTo(swarmCenter) < 400 && random.nextDouble() < 0.3) {
                    locus.setTarget(target);
                }
            }
        }
    }
    
    private void considerMigration() {
        // Natural migration based on food scarcity, threats, or random wandering
        if (hungerLevel > 70 || swarmAgitation > 0.8 || random.nextDouble() < 0.2) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = 50 + random.nextDouble() * 100;
            
            migrationTarget = new Vec3d(
                swarmCenter.x + Math.cos(angle) * distance,
                swarmCenter.y,
                swarmCenter.z + Math.sin(angle) * distance
            );
            
            isFeeding = false;
            hungerLevel = Math.max(0, hungerLevel - 30);
        }
    }
    
    private void regulateSwarmSize() {
        // Natural death and reproduction
        if (swarmMembers.size() > MAX_SWARM_SIZE * 0.8) {
            // Overcrowding causes some to leave or die
            List<LocusEntity> weakest = new ArrayList<>(swarmMembers.values());
            weakest.sort(Comparator.comparing(LocusEntity::getHealth));
            
            for (int i = 0; i < weakest.size() / 10; i++) {
                LocusEntity locus = weakest.get(i);
                if (locus.getHealth() < locus.getMaxHealth() * 0.3) {
                    locus.damage(world.getDamageSources().starve(), 1.0f);
                }
            }
        }
        
        // Lay new eggs when conditions are good
        if (swarmMembers.size() < MAX_SWARM_SIZE * 0.5 && hungerLevel < 30 && !isFeeding) {
            dormantEggs += random.nextInt(10);
        }
    }
    
    public void naturalDispersion() {
        // Swarm naturally breaks apart over time
        for (LocusEntity locus : swarmMembers.values()) {
            if (locus.isAlive()) {
                // Some fly away
                if (random.nextDouble() < 0.1) {
                    Vec3d escapeDirection = new Vec3d(
                        random.nextGaussian(),
                        0.5 + random.nextDouble(),
                        random.nextGaussian()
                    ).normalize();
                    
                    locus.setVelocity(escapeDirection.multiply(0.5));
                    locus.setDespawnCounter(100); // Will naturally despawn soon
                }
                
                // Others simply die
                else if (random.nextDouble() < 0.05) {
                    locus.damage(world.getDamageSources().starve(), locus.getHealth());
                }
            }
        }
        
        swarmMembers.clear();
    }
    
    public boolean isActive() {
        return !swarmMembers.isEmpty() || dormantEggs > 0;
    }
    
    public int getSwarmSize() {
        return swarmMembers.size();
    }
    
    public float getSwarmAgitation() {
        return swarmAgitation;
    }
}