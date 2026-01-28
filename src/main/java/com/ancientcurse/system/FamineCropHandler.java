package com.ancientcurse.system;

import com.ancientcurse.AncientCurse;
import net.minecraft.block.*;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.util.math.random.Random;

import java.util.*;

/**
 * Handles crop death mechanics during the Famine event.
 *
 * Performance optimizations:
 * - Processes chunks around players, not entire world
 * - Spreads work across multiple ticks
 * - Uses chunk-based cooldowns to avoid repeated scanning
 * - Limits blocks processed per tick
 */
public class FamineCropHandler {

    // Processing state
    private static final Map<ChunkPos, Long> chunkCooldowns = new HashMap<>();
    private static final Queue<BlockPos> pendingDeaths = new LinkedList<>();

    // Configuration
    private static final int CHUNK_COOLDOWN_TICKS = 200; // 10 seconds between chunk scans
    private static final int MAX_DEATHS_PER_TICK = 5; // Max crops to kill per tick
    private static final int MAX_PENDING_DEATHS = 100; // Cap pending queue
    private static final int SCAN_RADIUS_CHUNKS = 4; // Chunks around each player

    // Crop blocks that can be affected
    private static final Set<Block> AFFECTED_CROPS = new HashSet<>();

    static {
        // Vanilla crops
        AFFECTED_CROPS.add(Blocks.WHEAT);
        AFFECTED_CROPS.add(Blocks.CARROTS);
        AFFECTED_CROPS.add(Blocks.POTATOES);
        AFFECTED_CROPS.add(Blocks.BEETROOTS);
        AFFECTED_CROPS.add(Blocks.MELON_STEM);
        AFFECTED_CROPS.add(Blocks.PUMPKIN_STEM);
        AFFECTED_CROPS.add(Blocks.ATTACHED_MELON_STEM);
        AFFECTED_CROPS.add(Blocks.ATTACHED_PUMPKIN_STEM);
        AFFECTED_CROPS.add(Blocks.SWEET_BERRY_BUSH);
        AFFECTED_CROPS.add(Blocks.COCOA);
        AFFECTED_CROPS.add(Blocks.SUGAR_CANE);
        AFFECTED_CROPS.add(Blocks.BAMBOO);
        AFFECTED_CROPS.add(Blocks.BAMBOO_SAPLING);
        AFFECTED_CROPS.add(Blocks.KELP);
        AFFECTED_CROPS.add(Blocks.KELP_PLANT);
        AFFECTED_CROPS.add(Blocks.NETHER_WART);

        // Produce blocks
        AFFECTED_CROPS.add(Blocks.MELON);
        AFFECTED_CROPS.add(Blocks.PUMPKIN);
    }

    /**
     * Called every server tick from FamineData.tick()
     */
    public static void tick(ServerWorld world, FamineData data, FamineConfig config) {
        if (!data.isActive() && pendingDeaths.isEmpty()) {
            return;
        }

        long currentTime = world.getTime();
        float intensity = data.getIntensity();
        int radius = config.getCropDeathRadius();
        float deathChance = data.getCropDeathChance();
        int maxDeathsPerTick = config.getMaxCropDeathsPerTick();
        int chunkCooldown = config.getCropChunkScanCooldown();

        // Process pending deaths first (spread load)
        processPendingDeaths(world, intensity, maxDeathsPerTick);

        // Only scan for new deaths if famine is active
        if (data.isActive() && intensity > 0.1f) {
            // Scan chunks around players
            for (ServerPlayerEntity player : world.getPlayers()) {
                scanPlayerArea(world, player, currentTime, radius, deathChance, chunkCooldown);
            }
        }

        // Cleanup old cooldowns periodically
        if (currentTime % 1200 == 0) { // Every minute
            cleanupCooldowns(currentTime, chunkCooldown);
        }
    }

    /**
     * Process queued crop deaths - limited per tick for performance
     */
    private static void processPendingDeaths(ServerWorld world, float intensity, int maxPerTick) {
        int processed = 0;

        while (!pendingDeaths.isEmpty() && processed < maxPerTick) {
            BlockPos pos = pendingDeaths.poll();

            // Verify block is still a valid crop
            BlockState state = world.getBlockState(pos);
            if (isAffectedCrop(state)) {
                killCrop(world, pos, state, intensity);
                processed++;
            }
        }
    }

    /**
     * Scan area around a player for crops to potentially kill
     */
    private static void scanPlayerArea(ServerWorld world, ServerPlayerEntity player,
                                        long currentTime, int radius, float deathChance, int chunkCooldown) {
        BlockPos playerPos = player.getBlockPos();
        ChunkPos playerChunk = new ChunkPos(playerPos);
        int scanRadiusChunks = radius / 16; // Convert block radius to chunks

        // Scan chunks in radius
        for (int cx = -scanRadiusChunks; cx <= scanRadiusChunks; cx++) {
            for (int cz = -scanRadiusChunks; cz <= scanRadiusChunks; cz++) {
                ChunkPos chunkPos = new ChunkPos(playerChunk.x + cx, playerChunk.z + cz);

                // Check cooldown
                Long lastScan = chunkCooldowns.get(chunkPos);
                if (lastScan != null && currentTime - lastScan < chunkCooldown) {
                    continue;
                }

                // Don't overflow pending queue
                if (pendingDeaths.size() >= MAX_PENDING_DEATHS) {
                    return;
                }

                // Scan this chunk
                scanChunkForCrops(world, chunkPos, deathChance);
                chunkCooldowns.put(chunkPos, currentTime);
            }
        }
    }

    /**
     * Scan a single chunk for crops and queue deaths
     */
    private static void scanChunkForCrops(ServerWorld world, ChunkPos chunkPos, float deathChance) {
        if (!world.isChunkLoaded(chunkPos.x, chunkPos.z)) {
            return;
        }

        WorldChunk chunk = world.getChunk(chunkPos.x, chunkPos.z);
        Random random = world.getRandom();

        // Sample positions in the chunk (not every block for performance)
        int minX = chunkPos.getStartX();
        int minZ = chunkPos.getStartZ();
        int minY = world.getBottomY();
        int maxY = world.getTopY();

        // Sample 64 random positions per chunk scan
        for (int i = 0; i < 64; i++) {
            int x = minX + random.nextInt(16);
            int z = minZ + random.nextInt(16);
            int y = minY + random.nextInt(maxY - minY);

            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = chunk.getBlockState(pos);

            if (isAffectedCrop(state)) {
                // Apply death chance
                if (random.nextFloat() < deathChance) {
                    if (pendingDeaths.size() < MAX_PENDING_DEATHS) {
                        pendingDeaths.add(pos);
                    }
                }
            }
        }
    }

    /**
     * Check if a block state is an affected crop
     */
    private static boolean isAffectedCrop(BlockState state) {
        Block block = state.getBlock();

        // Direct check against our set
        if (AFFECTED_CROPS.contains(block)) {
            return true;
        }

        // Check for mod crops using instanceof
        if (block instanceof CropBlock) {
            return true;
        }

        // Check for saplings (trees won't grow)
        if (state.isIn(BlockTags.SAPLINGS)) {
            return true;
        }

        return false;
    }

    /**
     * Kill a crop at the given position
     */
    private static void killCrop(ServerWorld world, BlockPos pos, BlockState state, float intensity) {
        Block block = state.getBlock();

        // Determine replacement block
        BlockState replacement;

        if (block instanceof CropBlock) {
            // Replace with dead bush or air
            if (world.getRandom().nextFloat() < 0.3f) {
                replacement = Blocks.DEAD_BUSH.getDefaultState();
                // Check if dead bush can be placed
                if (!replacement.canPlaceAt(world, pos)) {
                    replacement = Blocks.AIR.getDefaultState();
                }
            } else {
                replacement = Blocks.AIR.getDefaultState();
            }
        } else if (block == Blocks.MELON || block == Blocks.PUMPKIN) {
            // Produce rots away
            replacement = Blocks.AIR.getDefaultState();
        } else if (block == Blocks.SUGAR_CANE || block == Blocks.BAMBOO ||
                   block == Blocks.BAMBOO_SAPLING || block == Blocks.KELP ||
                   block == Blocks.KELP_PLANT) {
            // Plants wither
            replacement = Blocks.AIR.getDefaultState();
        } else if (state.isIn(BlockTags.SAPLINGS)) {
            // Saplings die
            replacement = Blocks.DEAD_BUSH.getDefaultState();
            if (!replacement.canPlaceAt(world, pos)) {
                replacement = Blocks.AIR.getDefaultState();
            }
        } else {
            replacement = Blocks.AIR.getDefaultState();
        }

        // Apply the change
        world.setBlockState(pos, replacement, Block.NOTIFY_ALL);

        // Spawn particles
        if (intensity > 0.3f) {
            world.spawnParticles(
                ParticleTypes.SMOKE,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                3,
                0.2, 0.2, 0.2,
                0.01
            );
        }
    }

    /**
     * Clean up old chunk cooldowns to prevent memory growth
     */
    private static void cleanupCooldowns(long currentTime, int chunkCooldown) {
        chunkCooldowns.entrySet().removeIf(entry ->
            currentTime - entry.getValue() > chunkCooldown * 2);
    }

    /**
     * Reset handler state (called when famine ends)
     */
    public static void reset() {
        pendingDeaths.clear();
        chunkCooldowns.clear();
    }

    /**
     * Add a block to the affected crops set (for mod integration)
     */
    public static void addAffectedCrop(Block block) {
        AFFECTED_CROPS.add(block);
    }

    /**
     * Get count of pending deaths (for debug)
     */
    public static int getPendingDeathCount() {
        return pendingDeaths.size();
    }
}
