package com.ancientcurse.command;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.ModEntities;
import com.ancientcurse.entity.LotusEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LotusSwarmEvent {
    private final World world;
    private final Vec3d centerPos;
    private final int totalCount;
    private final int durationSeconds;
    private final int radius;
    private final List<LotusEntity> spawnedEntities;
    private final Random random;
    
    private int spawnedCount = 0;
    private int ticksRemaining;
    private boolean isActive = false;

    public LotusSwarmEvent(World world, Vec3d centerPos, int count, int durationSeconds, int radius) {
        this.world = world;
        this.centerPos = centerPos;
        this.totalCount = count;
        this.durationSeconds = durationSeconds;
        this.radius = radius;
        this.spawnedEntities = new ArrayList<>();
        this.random = new Random();
        this.ticksRemaining = durationSeconds * 20; // Convert seconds to ticks
    }

    public void start() {
        if (this.isActive) return;
        
        this.isActive = true;
        this.spawnedCount = 0;
        this.ticksRemaining = this.durationSeconds * 20;
        
        AncientCurse.LOGGER.info("Starting Lotus swarm event: " + this.totalCount + " entities over " + this.durationSeconds + " seconds");
        
        // Schedule the first spawn
        scheduleNextSpawn();
    }

    private void scheduleNextSpawn() {
        if (!this.isActive || this.spawnedCount >= this.totalCount) {
            return;
        }

        // Calculate spawn delay based on remaining time and entities
        int remainingEntities = this.totalCount - this.spawnedCount;
        int spawnDelay = Math.max(1, this.ticksRemaining / remainingEntities);
        
        // Schedule spawn with some randomization
        spawnDelay += this.random.nextInt(10) - 5; // ±5 ticks variation
        spawnDelay = Math.max(1, spawnDelay);
        
        // Schedule the spawn
        if (this.world instanceof ServerWorld serverWorld) {
            serverWorld.getServer().execute(() -> {
                if (this.isActive) {
                    spawnEntity();
                    scheduleNextSpawn();
                }
            });
        }
    }

    private void spawnEntity() {
        if (this.spawnedCount >= this.totalCount) return;
        
        // Find a valid spawn position
        Vec3d spawnPos = findValidSpawnPosition();
        if (spawnPos == null) {
            AncientCurse.LOGGER.warn("Could not find valid spawn position for Lotus swarm");
            return;
        }
        
        // Create and spawn the entity
        LotusEntity lotus = ModEntities.LOTUS.create(this.world);
        if (lotus != null) {
            lotus.setPosition(spawnPos);
            lotus.setFlying(true);
            
            // Add some randomization to movement
            Vec3d randomVelocity = new Vec3d(
                (this.random.nextDouble() - 0.5) * 0.2,
                (this.random.nextDouble() - 0.5) * 0.1,
                (this.random.nextDouble() - 0.5) * 0.2
            );
            lotus.setVelocity(randomVelocity);
            
            // Spawn the entity
            if (this.world.spawnEntity(lotus)) {
                this.spawnedEntities.add(lotus);
                this.spawnedCount++;
                
                AncientCurse.LOGGER.debug("Spawned Lotus entity " + this.spawnedCount + "/" + this.totalCount);
            }
        }
    }

    private Vec3d findValidSpawnPosition() {
        int maxAttempts = 50;
        
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // Generate random position within radius
            double angle = this.random.nextDouble() * 2 * Math.PI;
            double distance = this.random.nextDouble() * this.radius;
            
            double x = this.centerPos.x + Math.cos(angle) * distance;
            double z = this.centerPos.z + Math.sin(angle) * distance;
            
            // Find a good Y position (air block)
            int y = (int) this.centerPos.y + this.random.nextInt(10) - 5; // ±5 blocks from player height
            
            // Check if position is valid (air block)
            BlockPos pos = new BlockPos((int) x, y, (int) z);
            if (this.world.getBlockState(pos).isAir()) {
                return new Vec3d(x, y, z);
            }
        }
        
        return null;
    }

    public void tick() {
        if (!this.isActive) return;
        
        this.ticksRemaining--;
        
        // Clean up dead entities
        this.spawnedEntities.removeIf(entity -> !entity.isAlive());
        
        // Check if event should end
        if (this.ticksRemaining <= 0 || this.spawnedCount >= this.totalCount) {
            end();
        }
    }

    public void end() {
        if (!this.isActive) return;
        
        this.isActive = false;
        
        // Remove remaining entities
        for (LotusEntity entity : this.spawnedEntities) {
            if (entity.isAlive()) {
                entity.discard();
            }
        }
        
        this.spawnedEntities.clear();
        
        AncientCurse.LOGGER.info("Lotus swarm event ended. Spawned " + this.spawnedCount + " entities");
    }

    public boolean isActive() {
        return this.isActive;
    }

    public int getSpawnedCount() {
        return this.spawnedCount;
    }

    public int getRemainingTicks() {
        return this.ticksRemaining;
    }
} 