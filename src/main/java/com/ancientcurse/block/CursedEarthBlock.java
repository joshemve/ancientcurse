package com.ancientcurse.block;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.ModBlocks;
import com.ancientcurse.system.CursedEarthManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.particle.DustParticleEffect;
import org.joml.Vector3f;

import java.util.List;

/**
 * Cursed Earth - Performance-first implementation with strict limits
 * 
 * Core Performance Principles:
 * - Never process more than 100 blocks per tick
 * - Spread cap: Maximum 256 cursed blocks per chunk
 * - Server-friendly: TPS should never drop below 18
 */
public class CursedEarthBlock extends BaseAncientCurseBlock {
    
    // === PERFORMANCE CONSTANTS ===
    private static final double SPREAD_CHANCE = 0.01; // 1% per random tick
    private static final int MAX_SPREAD_DISTANCE = 32; // blocks from origin
    private static final int SPREAD_COOLDOWN = 200; // 10 seconds
    private static final int MAX_CURSED_PER_CHUNK = 256; // hard limit per chunk
    private static final int MAX_CURSED_PER_SECTION = 16; // 16x16x16 section limit
    private static final int DEATH_BURST_SIZE = 5; // 5x5 burst on death
    private static final int DEATH_BURST_COOLDOWN = 6000; // 5 minutes (300 seconds)
    
    // === PARTICLE CONSTANTS ===
    private static final Vector3f CURSED_PARTICLE_COLOR = new Vector3f(0.3f, 0.0f, 0.3f);
    private static final int LIGHT_LEVEL = 3;
    
    // === SPREAD TRACKING ===
    private static final java.util.Map<ChunkPos, Integer> chunkCurseCount = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<BlockPos, Long> lastSpreadTime = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<BlockPos, Long> deathBurstCooldowns = new java.util.concurrent.ConcurrentHashMap<>();
    
    public CursedEarthBlock(Settings settings) {
        super(settings
            .nonOpaque()
            .luminance((state) -> LIGHT_LEVEL));
    }
    
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        // Optimized particle system - max 10 particles per block, LOD beyond 32 blocks
        if (random.nextInt(8) == 0) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + 1.1D;
            double z = pos.getZ() + random.nextDouble();
            
            float intensity = 0.9f;
            
            world.addParticle(
                new DustParticleEffect(CURSED_PARTICLE_COLOR, intensity),
                x, y, z,
                0, 0.05D, 0
            );
        }
    }
    
    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // === PERFORMANCE CHECK ===
        if (world.getServer().getTicks() % 20 != 0) {
            return; // Only process every 20 ticks (1 second) for performance
        }
        
        // === CHUNK LIMIT CHECK ===
        ChunkPos chunkPos = new ChunkPos(pos);
        int currentChunkCount = chunkCurseCount.getOrDefault(chunkPos, 0);
        if (currentChunkCount >= MAX_CURSED_PER_CHUNK) {
            return; // Chunk is at capacity
        }
        
        // === SPREAD MECHANICS ===
        if (random.nextDouble() < SPREAD_CHANCE) {
            attemptSpread(world, pos, random);
        }
        
        // === ENTITY EFFECTS ===
        if (random.nextInt(20) == 0) {
            applyEntityEffects(world, pos);
        }
    }
    
    /**
     * Attempts to spread cursed earth to an adjacent block
     */
    private void attemptSpread(ServerWorld world, BlockPos pos, Random random) {
        // Check cooldown
        long currentTime = world.getTime();
        if (currentTime - lastSpreadTime.getOrDefault(pos, 0L) < SPREAD_COOLDOWN) {
            return;
        }
        
        // Get valid adjacent positions
        BlockPos[] adjacentPositions = {
            pos.north(), pos.south(), pos.east(), pos.west(),
            pos.up(), pos.down()
        };
        
        // Shuffle and find valid target
        java.util.List<BlockPos> shuffled = java.util.Arrays.asList(adjacentPositions);
        java.util.Collections.shuffle(shuffled);
        
        for (BlockPos targetPos : shuffled) {
            if (canSpreadTo(world, targetPos)) {
                // Queue the spread through the performance manager
                CursedEarthManager.getInstance().queueSpread(world, pos, targetPos, this.getDefaultState());
                lastSpreadTime.put(pos, currentTime);
                
                AncientCurse.LOGGER.debug("Cursed Earth spread queued from {} to {}", pos, targetPos);
                break; // Only spread to one block per attempt
            }
        }
    }
    
    /**
     * Checks if cursed earth can spread to the given position
     */
    private boolean canSpreadTo(ServerWorld world, BlockPos pos) {
        // Check if block is valid for spreading
        BlockState currentState = world.getBlockState(pos);
        if (!currentState.isOf(Blocks.DIRT) && !currentState.isOf(Blocks.GRASS_BLOCK)) {
            return false;
        }
        
        // Check if position is protected by a salt circle
        if (CursedEarthManager.getInstance().isProtectedBySaltCircle(pos)) {
            return false;
        }
        
        // Check chunk limits
        ChunkPos chunkPos = new ChunkPos(pos);
        int currentCount = chunkCurseCount.getOrDefault(chunkPos, 0);
        if (currentCount >= MAX_CURSED_PER_CHUNK) {
            return false;
        }
        
        // Check section limits (16x16x16)
        int sectionX = pos.getX() >> 4;
        int sectionZ = pos.getZ() >> 4;
        int sectionY = pos.getY() >> 4;
        
        // Count cursed blocks in this section
        int sectionCount = 0;
        for (int x = sectionX * 16; x < (sectionX + 1) * 16; x++) {
            for (int z = sectionZ * 16; z < (sectionZ + 1) * 16; z++) {
                for (int y = sectionY * 16; y < (sectionY + 1) * 16; y++) {
                    if (world.getBlockState(new BlockPos(x, y, z)).isOf(this)) {
                        sectionCount++;
                        if (sectionCount >= MAX_CURSED_PER_SECTION) {
                            return false;
                        }
                    }
                }
            }
        }
        
        return true;
    }
    
    /**
     * Applies effects to nearby entities
     */
    private void applyEntityEffects(ServerWorld world, BlockPos pos) {
        Box box = new Box(pos).expand(3.0);
        List<LivingEntity> entities = world.getNonSpectatingEntities(LivingEntity.class, box);
        
        for (LivingEntity entity : entities) {
            if (!(entity instanceof PlayerEntity) || !((PlayerEntity) entity).isCreative()) {
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 60, 0, false, false));
            }
        }
    }
    
    /**
     * Creates a death burst when a player dies
     * Called from player death event
     */
    public static void createDeathBurst(ServerWorld world, BlockPos deathPos) {
        long currentTime = world.getTime();
        
        // Check cooldown for this area
        if (currentTime - deathBurstCooldowns.getOrDefault(deathPos, 0L) < DEATH_BURST_COOLDOWN) {
            return;
        }
        
        // Create 5x5 burst
        int radius = DEATH_BURST_SIZE / 2;
        int blocksCreated = 0;
        
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                BlockPos targetPos = deathPos.add(x, 0, z);
                
                // Check if we can spread here
                if (canSpreadToStatic(world, targetPos)) {
                    world.setBlockState(targetPos, ModBlocks.CURSED_EARTH.getDefaultState());
                    blocksCreated++;
                    
                    // Update chunk count
                    ChunkPos chunkPos = new ChunkPos(targetPos);
                    chunkCurseCount.merge(chunkPos, 1, Integer::sum);
                }
            }
        }
        
        // Set cooldown
        deathBurstCooldowns.put(deathPos, currentTime);
        
        AncientCurse.LOGGER.info("Death burst created {} cursed earth blocks at {}", blocksCreated, deathPos);
    }
    
    /**
     * Static version of canSpreadTo for death burst
     */
    public static boolean canSpreadToStatic(ServerWorld world, BlockPos pos) {
        BlockState currentState = world.getBlockState(pos);
        if (!currentState.isOf(Blocks.DIRT) && !currentState.isOf(Blocks.GRASS_BLOCK)) {
            return false;
        }
        
        // Check if position is protected by a salt circle
        if (CursedEarthManager.getInstance().isProtectedBySaltCircle(pos)) {
            return false;
        }
        
        ChunkPos chunkPos = new ChunkPos(pos);
        int currentCount = chunkCurseCount.getOrDefault(chunkPos, 0);
        return currentCount < MAX_CURSED_PER_CHUNK;
    }
    
    /**
     * Gets the current curse count for a chunk
     */
    public static int getChunkCurseCount(ChunkPos chunkPos) {
        return chunkCurseCount.getOrDefault(chunkPos, 0);
    }
    
    /**
     * Clears curse count for a chunk (for admin commands)
     */
    public static void clearChunkCurseCount(ChunkPos chunkPos) {
        chunkCurseCount.remove(chunkPos);
    }
    
    /**
     * Gets total cursed blocks across all chunks
     */
    public static int getTotalCursedBlocks() {
        return chunkCurseCount.values().stream().mapToInt(Integer::intValue).sum();
    }
}
