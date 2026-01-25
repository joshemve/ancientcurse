package com.ancientcurse.system;

import com.ancientcurse.AncientCurse;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Efficiently tracks original block states before cursed earth conversion
 * Optimized for 100+ player servers with memory-conscious design
 *
 * Performance features:
 * - Global cap of 50,000 blocks to prevent unbounded growth
 * - LRU-style eviction of oldest chunks when cap is reached
 * - Lazy loading of chunk data to reduce startup lag
 */
public class OriginalBlockTracker extends PersistentState {
    private static final String DATA_NAME = "ancient_curse_original_blocks";

    // === PERFORMANCE LIMITS ===
    private static final int MAX_TOTAL_BLOCKS = 20000; // Global cap to prevent lag (reduced from 50k)
    private static final int BLOCKS_PER_TICK = 500; // Load this many blocks per server tick (no lag)
    private static final int CLEANUP_THRESHOLD = 15000; // Start cleanup when approaching cap

    // Store full positions for accuracy - memory usage is acceptable for tracking cursed earth
    // Key: ChunkPos, Value: Map of BlockPos to block ID
    private final Map<ChunkPos, Map<BlockPos, Short>> chunkData = new ConcurrentHashMap<>();

    // Track chunk access times for LRU eviction
    private final Map<ChunkPos, Long> chunkAccessTimes = new ConcurrentHashMap<>();

    // Incremental loading state
    private NbtCompound pendingNbtData = null;
    private List<String> pendingChunkKeys = null;
    private int pendingChunkIndex = 0;
    private boolean incrementalLoadComplete = true;

    // Cache for block ID lookups to avoid repeated registry lookups
    private static final Map<Block, Short> blockToId = new ConcurrentHashMap<>();
    private static final Map<Short, Block> idToBlock = new ConcurrentHashMap<>();
    private static volatile short nextBlockId = 0;

    // Track total blocks for cap enforcement
    private volatile int totalBlockCount = 0;
    
    public OriginalBlockTracker() {
        super();
        initializeCommonBlocks();
    }
    
    public static OriginalBlockTracker get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
            OriginalBlockTracker::fromNbt,
            OriginalBlockTracker::new,
            DATA_NAME
        );
    }
    
    /**
     * Pre-cache common blocks to save memory
     */
    private void initializeCommonBlocks() {
        cacheBlock(Blocks.DIRT);
        cacheBlock(Blocks.GRASS_BLOCK);
        cacheBlock(Blocks.SAND);
        cacheBlock(Blocks.RED_SAND);
        cacheBlock(Blocks.GRAVEL);
        cacheBlock(Blocks.COARSE_DIRT);
        cacheBlock(Blocks.PODZOL);
        cacheBlock(Blocks.MYCELIUM);
        cacheBlock(Blocks.ROOTED_DIRT);
        cacheBlock(Blocks.SOUL_SAND);
        cacheBlock(Blocks.SOUL_SOIL);
    }
    
    /**
     * Track the original block state at a position
     */
    public void trackOriginalBlock(BlockPos pos, BlockState originalState) {
        // NEVER track cursed earth as an original block
        if (originalState.getBlock() == com.ancientcurse.ModBlocks.CURSED_EARTH) {
            AncientCurse.LOGGER.warn("Attempted to track cursed earth as original block at {} - ignoring", pos);
            return;
        }

        // Enforce global cap - evict old data if approaching limit
        if (totalBlockCount >= CLEANUP_THRESHOLD) {
            evictOldestChunks();
        }

        // Hard cap - don't track if at maximum
        if (totalBlockCount >= MAX_TOTAL_BLOCKS) {
            AncientCurse.LOGGER.warn("Original block tracker at capacity ({} blocks), skipping tracking at {}",
                MAX_TOTAL_BLOCKS, pos);
            return;
        }

        Block block = originalState.getBlock();
        short blockId = getOrCreateBlockId(block);

        ChunkPos chunkPos = new ChunkPos(pos);
        Map<BlockPos, Short> chunk = chunkData.computeIfAbsent(chunkPos, k -> new HashMap<>());

        // Only track if not already tracked (prevent overwriting valid data)
        if (!chunk.containsKey(pos)) {
            chunk.put(pos, blockId);
            totalBlockCount++;
            chunkAccessTimes.put(chunkPos, System.currentTimeMillis());
            markDirty();

            // Debug logging (less frequent to reduce spam)
            if (totalBlockCount % 1000 == 0) {
                AncientCurse.LOGGER.debug("Tracking progress: {} blocks in {} chunks",
                    totalBlockCount, chunkData.size());
            }
        }
    }

    /**
     * Evict oldest chunks to make room for new data
     */
    private void evictOldestChunks() {
        if (chunkAccessTimes.isEmpty()) return;

        // Find chunks not accessed in the last 10 minutes
        long cutoffTime = System.currentTimeMillis() - (10 * 60 * 1000);
        List<ChunkPos> toEvict = new ArrayList<>();

        for (Map.Entry<ChunkPos, Long> entry : chunkAccessTimes.entrySet()) {
            if (entry.getValue() < cutoffTime) {
                toEvict.add(entry.getKey());
            }
        }

        // If not enough old chunks, evict the oldest ones
        if (toEvict.isEmpty() && totalBlockCount >= MAX_TOTAL_BLOCKS) {
            // Sort by access time and evict oldest 10%
            List<Map.Entry<ChunkPos, Long>> sorted = new ArrayList<>(chunkAccessTimes.entrySet());
            sorted.sort(Map.Entry.comparingByValue());
            int toRemove = Math.max(1, sorted.size() / 10);
            for (int i = 0; i < toRemove && i < sorted.size(); i++) {
                toEvict.add(sorted.get(i).getKey());
            }
        }

        // Evict the chunks
        int blocksRemoved = 0;
        for (ChunkPos chunk : toEvict) {
            Map<BlockPos, Short> removed = chunkData.remove(chunk);
            chunkAccessTimes.remove(chunk);
            if (removed != null) {
                blocksRemoved += removed.size();
            }
        }

        totalBlockCount -= blocksRemoved;
        if (blocksRemoved > 0) {
            AncientCurse.LOGGER.info("Evicted {} blocks from {} old chunks (total now: {})",
                blocksRemoved, toEvict.size(), totalBlockCount);
            markDirty();
        }
    }

    /**
     * Check if incremental loading is still in progress
     */
    public boolean isLoadingInProgress() {
        return !incrementalLoadComplete && pendingNbtData != null;
    }

    /**
     * Called every server tick to incrementally load data without causing lag.
     * Should be called from CursedEarthManager's server tick handler.
     */
    public void tickIncrementalLoad() {
        if (incrementalLoadComplete || pendingNbtData == null) return;

        NbtCompound chunks = pendingNbtData.getCompound("chunks");
        int blocksLoadedThisTick = 0;

        while (pendingChunkIndex < pendingChunkKeys.size() && blocksLoadedThisTick < BLOCKS_PER_TICK) {
            // Stop if we hit the global cap
            if (totalBlockCount >= MAX_TOTAL_BLOCKS) {
                AncientCurse.LOGGER.warn("Truncating loaded data at {} blocks (cap reached)", MAX_TOTAL_BLOCKS);
                finishIncrementalLoad();
                return;
            }

            String chunkKey = pendingChunkKeys.get(pendingChunkIndex);
            String[] parts = chunkKey.split(",");
            ChunkPos chunkPos = new ChunkPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));

            NbtList blockList = chunks.getList(chunkKey, 10);
            Map<BlockPos, Short> chunkDataMap = new HashMap<>();

            for (int i = 0; i < blockList.size() && totalBlockCount < MAX_TOTAL_BLOCKS; i++) {
                NbtCompound blockData = blockList.getCompound(i);
                BlockPos pos = new BlockPos(
                    blockData.getInt("x"),
                    blockData.getInt("y"),
                    blockData.getInt("z")
                );
                chunkDataMap.put(pos, blockData.getShort("id"));
                blocksLoadedThisTick++;
                totalBlockCount++;
            }

            if (!chunkDataMap.isEmpty()) {
                this.chunkData.put(chunkPos, chunkDataMap);
                this.chunkAccessTimes.put(chunkPos, System.currentTimeMillis());
            }

            pendingChunkIndex++;
        }

        // Check if we're done
        if (pendingChunkIndex >= pendingChunkKeys.size()) {
            finishIncrementalLoad();
        }
    }

    /**
     * Complete the incremental loading process
     */
    private void finishIncrementalLoad() {
        AncientCurse.LOGGER.info("Incremental load complete: {} blocks in {} chunks",
            totalBlockCount, chunkData.size());
        pendingNbtData = null;
        pendingChunkKeys = null;
        pendingChunkIndex = 0;
        incrementalLoadComplete = true;
        markDirty();
    }

    /**
     * Start incremental loading from NBT data
     */
    private void startIncrementalLoad(NbtCompound nbt) {
        pendingNbtData = nbt.copy();
        NbtCompound chunks = nbt.getCompound("chunks");
        pendingChunkKeys = new ArrayList<>(chunks.getKeys());
        pendingChunkIndex = 0;
        incrementalLoadComplete = false;

        int totalBlocksInNbt = 0;
        for (String chunkKey : pendingChunkKeys) {
            totalBlocksInNbt += chunks.getList(chunkKey, 10).size();
        }

        AncientCurse.LOGGER.info("Starting incremental load of {} blocks across {} chunks ({} blocks/tick)",
            totalBlocksInNbt, pendingChunkKeys.size(), BLOCKS_PER_TICK);
    }
    
    /**
     * Get the original block state at a position.
     * Returns null if position is not tracked or if data is still loading.
     */
    public BlockState getOriginalBlock(BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        Map<BlockPos, Short> chunk = chunkData.get(chunkPos);

        if (chunk == null) {
            return null; // Not tracked yet, or chunk not loaded yet
        }

        // Update access time for LRU tracking
        chunkAccessTimes.put(chunkPos, System.currentTimeMillis());

        Short blockId = chunk.get(pos);

        if (blockId == null) {
            return null;
        }

        Block block = idToBlock.get(blockId);
        if (block == null) {
            return null; // Block mapping not loaded yet
        }

        return block.getDefaultState();
    }
    
    /**
     * Clear tracked data for a position
     */
    public void clearTracking(BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        Map<BlockPos, Short> chunk = chunkData.get(chunkPos);

        if (chunk != null) {
            if (chunk.remove(pos) != null) {
                totalBlockCount--;
            }

            // Remove empty chunks to save memory
            if (chunk.isEmpty()) {
                chunkData.remove(chunkPos);
                chunkAccessTimes.remove(chunkPos);
            }

            markDirty();
        }
    }
    
    /**
     * Clear all tracking data for a chunk
     */
    public void clearChunk(ChunkPos chunkPos) {
        Map<BlockPos, Short> removed = chunkData.remove(chunkPos);
        chunkAccessTimes.remove(chunkPos);
        if (removed != null) {
            totalBlockCount -= removed.size();
        }
        markDirty();
    }

    /**
     * Get statistics for monitoring
     */
    public TrackerStats getStats() {
        return new TrackerStats(
            chunkData.size(),
            totalBlockCount,
            blockToId.size(),
            getEstimatedMemoryUsage()
        );
    }

    /**
     * Force clear all tracking data (for manual cleanup)
     */
    public void clearAll() {
        chunkData.clear();
        chunkAccessTimes.clear();
        totalBlockCount = 0;
        pendingNbtData = null;
        pendingChunkKeys = null;
        pendingChunkIndex = 0;
        incrementalLoadComplete = true;
        markDirty();
        AncientCurse.LOGGER.info("Cleared all original block tracking data");
    }
    
    
    /**
     * Get or create a block ID
     */
    private static synchronized short getOrCreateBlockId(Block block) {
        return blockToId.computeIfAbsent(block, b -> {
            short id = nextBlockId++;
            idToBlock.put(id, b);
            return id;
        });
    }
    
    /**
     * Cache a block for efficient lookup
     */
    private static void cacheBlock(Block block) {
        getOrCreateBlockId(block);
    }
    
    /**
     * Estimate memory usage in bytes
     */
    private long getEstimatedMemoryUsage() {
        // Rough estimation
        long chunkMapOverhead = chunkData.size() * 64L; // HashMap overhead per chunk
        long blockDataSize = 0;
        
        for (Map<BlockPos, Short> chunk : chunkData.values()) {
            blockDataSize += chunk.size() * 16L; // BlockPos (12 bytes) + Short (2 bytes) + overhead
        }
        
        long blockCacheSize = blockToId.size() * 32L; // Estimated cache overhead
        
        return chunkMapOverhead + blockDataSize + blockCacheSize;
    }
    
    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        // Save block ID mappings
        NbtCompound blockMappings = new NbtCompound();
        for (Map.Entry<Block, Short> entry : blockToId.entrySet()) {
            String blockId = Registries.BLOCK.getId(entry.getKey()).toString();
            blockMappings.putShort(blockId, entry.getValue());
        }
        nbt.put("blockMappings", blockMappings);
        
        // Save chunk data
        NbtCompound chunks = new NbtCompound();
        for (Map.Entry<ChunkPos, Map<BlockPos, Short>> chunkEntry : chunkData.entrySet()) {
            ChunkPos chunkPos = chunkEntry.getKey();
            String chunkKey = chunkPos.x + "," + chunkPos.z;
            
            NbtList blockList = new NbtList();
            for (Map.Entry<BlockPos, Short> blockEntry : chunkEntry.getValue().entrySet()) {
                NbtCompound blockData = new NbtCompound();
                BlockPos pos = blockEntry.getKey();
                blockData.putInt("x", pos.getX());
                blockData.putInt("y", pos.getY());
                blockData.putInt("z", pos.getZ());
                blockData.putShort("id", blockEntry.getValue());
                blockList.add(blockData);
            }
            
            chunks.put(chunkKey, blockList);
        }
        nbt.put("chunks", chunks);
        
        return nbt;
    }
    
    public static OriginalBlockTracker fromNbt(NbtCompound nbt) {
        OriginalBlockTracker tracker = new OriginalBlockTracker();

        // Note: Static maps are shared across all worlds/dimensions
        // We need to be careful about clearing them

        // Only clear if we're loading fresh data
        if (nbt.contains("blockMappings")) {
            blockToId.clear();
            idToBlock.clear();
            nextBlockId = 0;

            // Re-initialize common blocks
            tracker.initializeCommonBlocks();
        }

        // Load block mappings immediately (they're small)
        NbtCompound blockMappings = nbt.getCompound("blockMappings");
        for (String blockIdStr : blockMappings.getKeys()) {
            short id = blockMappings.getShort(blockIdStr);
            Block block = Registries.BLOCK.get(new Identifier(blockIdStr));
            if (block != Blocks.AIR) {
                blockToId.put(block, id);
                idToBlock.put(id, block);
                nextBlockId = (short) Math.max(nextBlockId, id + 1);
            }
        }

        AncientCurse.LOGGER.info("Loaded {} block mappings from NBT", blockToId.size());

        // Count how many blocks we would load
        NbtCompound chunks = nbt.getCompound("chunks");
        int totalBlocksInNbt = 0;
        for (String chunkKey : chunks.getKeys()) {
            NbtList blockList = chunks.getList(chunkKey, 10);
            totalBlocksInNbt += blockList.size();
        }

        // Always use incremental loading to prevent lag spikes
        if (totalBlocksInNbt > 0) {
            tracker.startIncrementalLoad(nbt);
        } else {
            tracker.incrementalLoadComplete = true;
        }

        return tracker;
    }
    
    /**
     * Statistics class for monitoring
     */
    public static class TrackerStats {
        public final int chunksTracked;
        public final int blocksTracked;
        public final int uniqueBlockTypes;
        public final long estimatedMemoryBytes;
        
        public TrackerStats(int chunksTracked, int blocksTracked, int uniqueBlockTypes, long estimatedMemoryBytes) {
            this.chunksTracked = chunksTracked;
            this.blocksTracked = blocksTracked;
            this.uniqueBlockTypes = uniqueBlockTypes;
            this.estimatedMemoryBytes = estimatedMemoryBytes;
        }
        
        public String getMemoryUsageString() {
            if (estimatedMemoryBytes < 1024) {
                return estimatedMemoryBytes + " bytes";
            } else if (estimatedMemoryBytes < 1024 * 1024) {
                return String.format("%.2f KB", estimatedMemoryBytes / 1024.0);
            } else {
                return String.format("%.2f MB", estimatedMemoryBytes / (1024.0 * 1024.0));
            }
        }
    }
}