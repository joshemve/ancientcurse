package com.ancientcurse.command;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.ModEntities;
import com.ancientcurse.entity.LocusEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class LocusSwarmEvent {
    private final World world;
    private final Vec3d centerPos;
    private final int totalCount;
    private final int durationSeconds;
    private final int radius;
    private final List<LocusEntity> spawnedEntities;
    private final Random random;
    
    private int spawnedCount = 0;
    private boolean isActive = false;
    private Timer timer;
    
    public LocusSwarmEvent(World world, Vec3d centerPos, int totalCount, int durationSeconds, int radius) {
        this.world = world;
        this.centerPos = centerPos;
        this.totalCount = totalCount;
        this.durationSeconds = durationSeconds;
        this.radius = radius;
        this.spawnedEntities = new ArrayList<>();
        this.random = new Random();
    }
    
    public void start() {
        if (this.isActive || !(this.world instanceof ServerWorld)) {
            return;
        }
        
        this.isActive = true;
        this.timer = new Timer();
        
        AncientCurse.LOGGER.info("Starting Locus swarm event: " + this.totalCount + " entities over " + this.durationSeconds + " seconds");
        
        // Calculate spawn interval (spawn entities gradually over time)
        long spawnInterval = (this.durationSeconds * 1000L) / this.totalCount; // Convert to milliseconds
        spawnInterval = Math.max(spawnInterval, 100); // Minimum 100ms between spawns
        
        // Schedule spawning
        this.timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (spawnedCount < totalCount && isActive) {
                    spawnLocus();
                } else {
                    scheduleCleanup();
                }
            }
        }, 0, spawnInterval);
    }
    
    private void spawnLocus() {
        if (!(this.world instanceof ServerWorld serverWorld)) {
            return;
        }
        
        // Find a random position within radius
        Vec3d spawnPos = findSpawnPosition();
        if (spawnPos == null) {
            AncientCurse.LOGGER.warn("Could not find valid spawn position for Locus swarm");
            return;
        }
        
        // Create and spawn the entity (locus = bug, not lotus = plant)
        LocusEntity locus = ModEntities.LOCUS.create(this.world);
        if (locus != null) {
            locus.setPosition(spawnPos);
            
            // Add some randomization to initial positioning
            Vec3d randomOffset = new Vec3d(
                (this.random.nextDouble() - 0.5) * 2,
                (this.random.nextDouble() - 0.5) * 1,
                (this.random.nextDouble() - 0.5) * 2
            );
            locus.setPosition(spawnPos.add(randomOffset));
            
            // Spawn the entity
            if (this.world.spawnEntity(locus)) {
                this.spawnedEntities.add(locus);
                this.spawnedCount++;
                
                AncientCurse.LOGGER.debug("Spawned Locus entity " + this.spawnedCount + "/" + this.totalCount);
            }
        }
    }
    
    private Vec3d findSpawnPosition() {
        // Try multiple times to find a valid spawn position
        for (int attempts = 0; attempts < 10; attempts++) {
            double angle = this.random.nextDouble() * 2 * Math.PI;
            double distance = this.random.nextDouble() * this.radius;
            
            double x = this.centerPos.x + Math.cos(angle) * distance;
            double z = this.centerPos.z + Math.sin(angle) * distance;
            
            // Find surface height
            BlockPos surfacePos = this.world.getTopPosition(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, new BlockPos((int)x, 0, (int)z));
            
            // Spawn slightly above surface
            double y = surfacePos.getY() + 2;
            
            Vec3d spawnPos = new Vec3d(x, y, z);
            
            // Check if position is valid (not in solid blocks)
            if (this.world.isSpaceEmpty(null, net.minecraft.entity.EntityDimensions.fixed(1.0f, 1.5f).getBoxAt(spawnPos))) {
                return spawnPos;
            }
        }
        
        return null; // Could not find valid position
    }
    
    private void scheduleCleanup() {
        if (this.timer != null) {
            this.timer.cancel();
        }
        
        // Schedule cleanup after duration
        this.timer = new Timer();
        this.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                cleanup();
            }
        }, Math.max(this.durationSeconds * 1000L - (this.totalCount * 100), 5000)); // Ensure minimum cleanup delay
    }
    
    private void cleanup() {
        // Remove remaining entities
        for (LocusEntity entity : this.spawnedEntities) {
            if (entity.isAlive()) {
                entity.discard();
            }
        }
        
        this.spawnedEntities.clear();
        this.isActive = false;
        
        if (this.timer != null) {
            this.timer.cancel();
        }
        
        AncientCurse.LOGGER.info("Locus swarm event ended. Spawned " + this.spawnedCount + " entities");
    }
    
    public boolean isActive() {
        return this.isActive;
    }
    
    public int getSpawnedCount() {
        return this.spawnedCount;
    }
    
    public int getTotalCount() {
        return this.totalCount;
    }
} 