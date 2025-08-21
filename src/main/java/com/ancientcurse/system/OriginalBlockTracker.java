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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Efficiently tracks original block states before cursed earth conversion
 * Optimized for 100+ player servers with memory-conscious design
 */
public class OriginalBlockTracker extends PersistentState {
    private static final String DATA_NAME = "ancient_curse_original_blocks";
    
    // Store full positions for accuracy - memory usage is acceptable for tracking cursed earth
    // Key: ChunkPos, Value: Map of BlockPos to block ID
    private final Map<ChunkPos, Map<BlockPos, Short>> chunkData = new ConcurrentHashMap<>();
    
    // Cache for block ID lookups to avoid repeated registry lookups
    private static final Map<Block, Short> blockToId = new ConcurrentHashMap<>();
    private static final Map<Short, Block> idToBlock = new ConcurrentHashMap<>();
    private static volatile short nextBlockId = 0;
    
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
        
        Block block = originalState.getBlock();
        short blockId = getOrCreateBlockId(block);
        
        ChunkPos chunkPos = new ChunkPos(pos);
        Map<BlockPos, Short> chunk = chunkData.computeIfAbsent(chunkPos, k -> new HashMap<>());
        
        // Only track if not already tracked (prevent overwriting valid data)
        if (!chunk.containsKey(pos)) {
            chunk.put(pos, blockId);
            markDirty();
            
            // Debug logging
            TrackerStats stats = getStats();
            if (stats.blocksTracked % 100 == 0) {
                AncientCurse.LOGGER.debug("Tracked block {} at {} (total tracked: {})", 
                    block.getTranslationKey(), pos, stats.blocksTracked);
            }
        } else {
            AncientCurse.LOGGER.debug("Position {} already tracked, not overwriting", pos);
        }
    }
    
    /**
     * Get the original block state at a position
     */
    public BlockState getOriginalBlock(BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        Map<BlockPos, Short> chunk = chunkData.get(chunkPos);
        
        if (chunk == null) {
            AncientCurse.LOGGER.debug("No chunk data found for chunk {} when looking up {}", chunkPos, pos);
            return null;
        }
        
        Short blockId = chunk.get(pos);
        
        if (blockId == null) {
            AncientCurse.LOGGER.debug("No tracking data found for position {} in chunk {} (chunk has {} entries)", 
                pos, chunkPos, chunk.size());
            return null;
        }
        
        Block block = idToBlock.get(blockId);
        if (block == null) {
            AncientCurse.LOGGER.error("Block ID {} not found in idToBlock map for position {}", blockId, pos);
            return null;
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
            chunk.remove(pos);
            
            // Remove empty chunks to save memory
            if (chunk.isEmpty()) {
                chunkData.remove(chunkPos);
            }
            
            markDirty();
        }
    }
    
    /**
     * Clear all tracking data for a chunk
     */
    public void clearChunk(ChunkPos chunkPos) {
        chunkData.remove(chunkPos);
        markDirty();
    }
    
    /**
     * Get statistics for monitoring
     */
    public TrackerStats getStats() {
        int totalBlocks = 0;
        for (Map<BlockPos, Short> chunk : chunkData.values()) {
            totalBlocks += chunk.size();
        }
        
        return new TrackerStats(
            chunkData.size(),
            totalBlocks,
            blockToId.size(),
            getEstimatedMemoryUsage()
        );
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
        
        // Load block mappings
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
        
        // Load chunk data
        NbtCompound chunks = nbt.getCompound("chunks");
        int totalBlocksLoaded = 0;
        for (String chunkKey : chunks.getKeys()) {
            String[] parts = chunkKey.split(",");
            ChunkPos chunkPos = new ChunkPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
            
            Map<BlockPos, Short> chunkData = new HashMap<>();
            NbtList blockList = chunks.getList(chunkKey, 10); // 10 = Compound tag
            
            for (int i = 0; i < blockList.size(); i++) {
                NbtCompound blockData = blockList.getCompound(i);
                BlockPos pos = new BlockPos(
                    blockData.getInt("x"),
                    blockData.getInt("y"),
                    blockData.getInt("z")
                );
                chunkData.put(pos, blockData.getShort("id"));
                totalBlocksLoaded++;
            }
            
            tracker.chunkData.put(chunkPos, chunkData);
        }
        
        AncientCurse.LOGGER.info("Loaded {} blocks in {} chunks from NBT", totalBlocksLoaded, tracker.chunkData.size());
        
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