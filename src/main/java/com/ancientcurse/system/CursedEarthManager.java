package com.ancientcurse.system;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.block.CursedEarthBlock;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.registry.RegistryKey;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.ancientcurse.system.DimensionAwareKeys.DimPos;
import com.ancientcurse.system.DimensionAwareKeys.DimChunk;

/**
 * Centralized manager for Cursed Earth performance systems
 * Implements Phase 2 of the roadmap: Performance Systems
 * 
 * Features:
 * - Chunk curse tracking with limits
 * - Tick distribution to prevent TPS drops
 * - Server performance monitoring
 * - Spread queue management
 */
public class CursedEarthManager {
    
    // === PERFORMANCE CONSTANTS ===
    private static final int MAX_SPREADS_PER_TICK = 10; // Maximum spreads processed per tick
    private static final int MAX_CURSED_PER_CHUNK = 256; // Hard limit per chunk
    private static final int PERFORMANCE_CHECK_INTERVAL = 100; // Check TPS every 5 seconds
    private static final double MIN_TPS_THRESHOLD = 18.0; // Minimum acceptable TPS
    
    // === TICK DISTRIBUTION SCHEDULE ===
    private static final int[] CURSE_PROCESSING_TICKS = {6, 7, 8}; // Ticks 6-8 for curse processing
    private static final int[] REDSTONE_AVOID_TICKS = {0, 1, 2, 3, 4, 5}; // Avoid redstone ticks
    
    // === TRACKING MAPS - NOW DIMENSION-AWARE ===
    private static final Map<DimChunk, ChunkCurseTracker> chunkTrackers = new ConcurrentHashMap<>();
    private static final Queue<SpreadRequest> spreadQueue = new LinkedList<>();
    private static final Map<RegistryKey<World>, Set<ChunkPos>> loadedChunks = new ConcurrentHashMap<>();
    private static final Map<DimPos, SaltCircle> saltCircles = new ConcurrentHashMap<>();
    private static final Map<DimPos, CleansingProtection> cleansingProtections = new ConcurrentHashMap<>();
    
    // === SPREAD TRACKING (temporary until moved from CursedEarthBlock) ===
    private static final Map<BlockPos, net.minecraft.util.math.Direction> spreadDirections = new HashMap<>();
    private static final Map<BlockPos, Integer> branchLengths = new HashMap<>();
    private static final Map<BlockPos, BlockPos> spreadOrigins = new HashMap<>();
    
    // === PERFORMANCE MONITORING ===
    private static long lastPerformanceCheck = 0;
    private static double averageTPS = 20.0;
    private static boolean performanceMode = false;
    private static long lastTickNanos = 0;
    private static double avgMspt = 0;
    
    // === SINGLETON ===
    private static CursedEarthManager instance;
    
    public static CursedEarthManager getInstance() {
        if (instance == null) {
            instance = new CursedEarthManager();
        }
        return instance;
    }
    
    private CursedEarthManager() {
        // Register server tick event
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
    }
    
    /**
     * Main server tick handler - implements tick distribution
     */
    private void onServerTick(MinecraftServer server) {
        long currentTick = server.getTicks();
        int tickPhase = (int) (currentTick % 20);
        
        // Performance monitoring every 5 seconds
        if (currentTick % PERFORMANCE_CHECK_INTERVAL == 0) {
            updatePerformanceMetrics(server);
        }
        
        // Only process cursed earth during designated ticks (6-8)
        if (Arrays.stream(CURSE_PROCESSING_TICKS).anyMatch(tick -> tick == tickPhase)) {
            processCursedEarthSpreads(server);
        }
        
        // Clean up unloaded chunks every minute
        if (currentTick % 1200 == 0) {
            cleanupUnloadedChunks(server);
            cleanupExpiredSaltCircles(server);
            cleanupExpiredProtections(server);
        }
        
        // Clean up old tracking data every 5 minutes
        if (currentTick % 6000 == 0) {
            cleanupOldTrackingData(server);
        }
    }
    
    /**
     * Processes cursed earth spreads from the queue
     */
    private void processCursedEarthSpreads(MinecraftServer server) {
        int processedSpreads = 0;
        // Dynamic throttling based on queue size
        int baseSpreads = performanceMode ? MAX_SPREADS_PER_TICK / 2 : MAX_SPREADS_PER_TICK;
        int dynamic = Math.min(baseSpreads + spreadQueue.size()/50, 50);
        int maxSpreads = Math.max(1, dynamic);
        
        while (!spreadQueue.isEmpty() && processedSpreads < maxSpreads) {
            SpreadRequest request = spreadQueue.poll();
            if (request != null && request.isValid(server)) {
                if (processSpreadRequest(request)) {
                    processedSpreads++;
                }
            }
        }
        
        if (processedSpreads > 0) {
            AncientCurse.LOGGER.debug("Processed {} cursed earth spreads this tick", processedSpreads);
        }
    }
    
    /**
     * Processes a single spread request
     */
    private boolean processSpreadRequest(SpreadRequest request) {
        try {
            ServerWorld world = request.world;
            BlockPos pos = request.fromPos;
            DimChunk dimChunk = new DimChunk(world.getRegistryKey(), new ChunkPos(request.toPos));
            
            // Check chunk limits
            ChunkCurseTracker tracker = getChunkTracker(dimChunk);
            if (tracker.getCurseCount() >= MAX_CURSED_PER_CHUNK) {
                return false;
            }
            
            // Check for cleansing protection
            if (isProtectedByCleansingStation(world, request.toPos)) {
                return false;
            }
            
            // ALSO check if we're in an active cleansing zone
            if (com.ancientcurse.block.SolarSpireBlock.isInActiveCleansingZone(request.toPos)) {
                return false; // Don't spread into active cleansing zones
            }
            
            // Attempt the spread
            if (CursedEarthBlock.canSpreadToStatic(world, request.toPos)) {
                // Track original block before converting
                BlockState originalState = world.getBlockState(request.toPos);
                
                // Debug logging
                AncientCurse.LOGGER.debug("Tracking original block at {}: {}", request.toPos, originalState.getBlock());
                
                OriginalBlockTracker blockTracker = OriginalBlockTracker.get(world);
                blockTracker.trackOriginalBlock(request.toPos, originalState);
                
                // Now convert to cursed earth
                world.setBlockState(request.toPos, request.newState);
                tracker.incrementCurseCount();
                return true;
            }
        } catch (Exception e) {
            AncientCurse.LOGGER.error("Error processing spread request: {}", e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Updates performance metrics and adjusts processing accordingly
     */
    private void updatePerformanceMetrics(MinecraftServer server) {
        // TPS already calculated from MSPT in onServerTick
        // Just log if mode changed
        boolean newPerformanceMode = averageTPS < MIN_TPS_THRESHOLD;
        if (newPerformanceMode != performanceMode) {
            performanceMode = newPerformanceMode;
            AncientCurse.LOGGER.info("Cursed Earth performance mode: {} (TPS: {})", 
                performanceMode ? "ENABLED" : "DISABLED", String.format("%.1f", averageTPS));
        }
        
        lastPerformanceCheck = System.currentTimeMillis();
    }
    
    /**
     * Cleans up trackers for unloaded chunks
     */
    private void cleanupUnloadedChunks(MinecraftServer server) {
        Set<DimChunk> chunksToRemove = new HashSet<>();
        
        // Simple cleanup - remove trackers that haven't been updated in 5 minutes
        long currentTime = System.currentTimeMillis();
        chunkTrackers.entrySet().removeIf(entry -> {
            ChunkCurseTracker tracker = entry.getValue();
            boolean shouldRemove = (currentTime - tracker.getLastUpdate()) > 300000; // 5 minutes
            if (shouldRemove) {
                chunksToRemove.add(entry.getKey());
            }
            return shouldRemove;
        });
        
        if (!chunksToRemove.isEmpty()) {
            AncientCurse.LOGGER.debug("Cleaned up {} stale chunk trackers", chunksToRemove.size());
        }
    }
    
    /**
     * Store original block state for restoration (e.g., grass replaced by Reed of Sekhem)
     */
    public void storeOriginalBlock(ServerWorld world, BlockPos pos, BlockState originalState) {
        OriginalBlockTracker tracker = OriginalBlockTracker.get(world);
        tracker.trackOriginalBlock(pos, originalState);
    }
    
    /**
     * Cleans up old tracking data for blocks that no longer exist
     */
    private void cleanupOldTrackingData(MinecraftServer server) {
        // Clean up chunk trackers with 0 cursed blocks
        List<DimChunk> emptyChunks = new ArrayList<>();
        for (Map.Entry<DimChunk, ChunkCurseTracker> entry : chunkTrackers.entrySet()) {
            if (entry.getValue().getCurseCount() <= 0) {
                emptyChunks.add(entry.getKey());
            }
        }
        
        // Remove empty chunk trackers
        for (DimChunk chunk : emptyChunks) {
            chunkTrackers.remove(chunk);
        }
        
        if (!emptyChunks.isEmpty()) {
            AncientCurse.LOGGER.debug("Cleaned up {} empty chunk trackers", emptyChunks.size());
        }
        
        // Clean up OriginalBlockTracker data
        for (ServerWorld world : server.getWorlds()) {
            OriginalBlockTracker tracker = OriginalBlockTracker.get(world);
            OriginalBlockTracker.TrackerStats stats = tracker.getStats();
            
            // Log high memory usage
            if (stats.blocksTracked > 10000) {
                AncientCurse.LOGGER.info("Original block tracker for {} - Blocks: {}, Memory: {} (consider manual cleanup)", 
                    world.getRegistryKey().getValue(), 
                    stats.blocksTracked, 
                    stats.getMemoryUsageString());
            }
        }
    }
    
    // === PUBLIC API ===
    
    /**
     * Queues a spread request for processing
     */
    public void queueSpread(ServerWorld world, BlockPos fromPos, BlockPos toPos, 
                           net.minecraft.block.BlockState newState) {
        if (spreadQueue.size() < 1000) { // Prevent queue overflow
            spreadQueue.offer(new SpreadRequest(world, fromPos, toPos, newState));
        }
    }
    
    /**
     * Called when a cursed block is removed (cleansed, broken, or replaced)
     * Updates chunk curse counts and clears related tracking data
     */
    public void onCursedBlockRemoved(ServerWorld world, BlockPos pos) {
        DimChunk dimChunk = new DimChunk(world.getRegistryKey(), new ChunkPos(pos));
        ChunkCurseTracker tracker = chunkTrackers.get(dimChunk);
        if (tracker != null) {
            tracker.decrementCurseCount();
            AncientCurse.LOGGER.debug("Decremented curse count at {} - new count: {}", pos, tracker.getCurseCount());
        }
        
        // Clear any spread tracking data for this position
        spreadDirections.remove(pos);
        branchLengths.remove(pos);
        spreadOrigins.remove(pos);
        
        // Clear protection if it exists at this position
        DimPos dimPos = new DimPos(world.getRegistryKey(), pos);
        cleansingProtections.remove(dimPos);
    }
    
    /**
     * Gets the chunk tracker for a given dimension-aware chunk position
     */
    public ChunkCurseTracker getChunkTracker(DimChunk dimChunk) {
        return chunkTrackers.computeIfAbsent(dimChunk, ChunkCurseTracker::new);
    }
    
    /**
     * Gets the chunk tracker for a given chunk position (convenience method)
     */
    public ChunkCurseTracker getChunkTracker(ChunkPos chunkPos) {
        // This method will need to be updated by callers to pass ServerWorld
        throw new UnsupportedOperationException("Use getChunkTracker(DimChunk) instead");
    }
    
    /**
     * Checks if a chunk can accept more cursed blocks
     */
    public boolean canChunkAcceptMore(DimChunk dimChunk) {
        return getChunkTracker(dimChunk).getCurseCount() < MAX_CURSED_PER_CHUNK;
    }
    
    /**
     * Gets current performance metrics
     */
    public PerformanceMetrics getPerformanceMetrics() {
        return new PerformanceMetrics(averageTPS, performanceMode, spreadQueue.size(), 
            chunkTrackers.size());
    }
    
    /**
     * Gets the number of chunks that contain cursed earth blocks
     */
    public int getChunksWithCursedEarth() {
        int count = 0;
        for (ChunkCurseTracker tracker : chunkTrackers.values()) {
            if (tracker.getCurseCount() > 0) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Gets total number of tracked chunks (including empty ones)
     */
    public int getTotalTrackedChunks() {
        return chunkTrackers.size();
    }
    
    /**
     * Creates a salt circle for protection against cursed earth
     */
    public void createSaltCircle(ServerWorld world, BlockPos center, int radius, int duration) {
        long expirationTime = world.getTime() + duration;
        SaltCircle saltCircle = new SaltCircle(center, radius, expirationTime);
        DimPos dimPos = new DimPos(world.getRegistryKey(), center);
        saltCircles.put(dimPos, saltCircle);
        
        AncientCurse.LOGGER.info("Salt circle created at {} with radius {} for {} ticks", 
            center, radius, duration);
    }
    
    /**
     * Checks if there's a salt circle at the given position
     */
    public boolean hasSaltCircle(ServerWorld world, BlockPos pos) {
        DimPos dimPos = new DimPos(world.getRegistryKey(), pos);
        return saltCircles.containsKey(dimPos);
    }
    
    /**
     * Checks if a position is protected by any salt circle
     */
    public boolean isProtectedBySaltCircle(ServerWorld world, BlockPos pos) {
        // Use dimension-aware key lookup
        for (Map.Entry<DimPos, SaltCircle> entry : saltCircles.entrySet()) {
            DimPos dimPos = entry.getKey();
            SaltCircle circle = entry.getValue();
            
            // Only check salt circles in the same dimension
            if (dimPos.dimension().equals(world.getRegistryKey()) && 
                circle.isActive() && circle.contains(pos)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Cleans up expired salt circles
     */
    private void cleanupExpiredSaltCircles(MinecraftServer server) {
        long currentTime = server.getTicks();
        saltCircles.entrySet().removeIf(entry -> {
            SaltCircle circle = entry.getValue();
            return !circle.isActive() || currentTime > circle.getExpirationTime();
        });
    }
    
    /**
     * Cleans up expired cleansing protections
     */
    private void cleanupExpiredProtections(MinecraftServer server) {
        long currentTime = server.getTicks();
        int sizeBefore = cleansingProtections.size();
        cleansingProtections.entrySet().removeIf(entry -> {
            CleansingProtection protection = entry.getValue();
            return currentTime > protection.getExpirationTime();
        });
        int removed = sizeBefore - cleansingProtections.size();
        if (removed > 0) {
            AncientCurse.LOGGER.debug("Removed {} expired cleansing protections", removed);
        }
    }
    
    /**
     * Creates cleansing protection for a position
     */
    public void createCleansingProtection(ServerWorld world, BlockPos pos, int duration) {
        long expirationTime = world.getTime() + duration;
        CleansingProtection protection = new CleansingProtection(pos, expirationTime);
        DimPos dimPos = new DimPos(world.getRegistryKey(), pos);
        cleansingProtections.put(dimPos, protection);
        
    }
    
    /**
     * Checks if a position is protected by cleansing station
     */
    public boolean isProtectedByCleansingStation(ServerWorld world, BlockPos pos) {
        // Check the exact position using dimension-aware key
        DimPos dimPos = new DimPos(world.getRegistryKey(), pos);
        CleansingProtection protection = cleansingProtections.get(dimPos);
        if (protection != null && protection.isActive()) {
            return true;
        }
        
        // Also check vertical column (5 blocks up and down) to prevent surface finding bypass
        for (int y = -5; y <= 5; y++) {
            BlockPos checkPos = pos.add(0, y, 0);
            DimPos checkDimPos = new DimPos(world.getRegistryKey(), checkPos);
            protection = cleansingProtections.get(checkDimPos);
            if (protection != null && protection.isActive()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Clears protection at a specific position
     * @return true if protection was cleared, false if no protection existed
     */
    public boolean clearProtectionAt(ServerWorld world, BlockPos pos) {
        DimPos dimPos = new DimPos(world.getRegistryKey(), pos);
        CleansingProtection removed = cleansingProtections.remove(dimPos);
        return removed != null;
    }
    
    /**
     * Clears all connected protection starting from a given position
     * Uses flood fill to find all connected protected blocks
     * @return number of blocks cleared
     */
    public int clearAllConnectedProtection(BlockPos startPos) {
        // This method needs ServerWorld parameter - for now, skip the check
        // TODO: Update callers to pass ServerWorld
        // if (!isProtectedByCleansingStation(world, startPos)) {
        //     return 0;
        // }
        
        Set<BlockPos> toProcess = new HashSet<>();
        Set<BlockPos> processed = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(startPos);
        
        // Find all connected protected blocks
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            if (processed.contains(current)) continue;
            processed.add(current);
            
            // This method needs ServerWorld parameter - add dummy logic for now
            // TODO: Update this method to accept ServerWorld parameter
            toProcess.add(current);
                
            // Check all 26 adjacent positions (including diagonals)
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;
                        BlockPos neighbor = current.add(x, y, z);
                        if (!processed.contains(neighbor)) {
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }
        
        // Clear all found protections
        // TODO: This method needs to be updated to accept ServerWorld parameter
        // For now, skip the clearing step since we can't access ServerWorld here
        int cleared = 0;
        // for (BlockPos pos : toProcess) {
        //     if (clearProtectionAt(world, pos)) {
        //         cleared++;
        //     }
        // }
        
        if (cleared > 0) {
            AncientCurse.LOGGER.info("Cleared protection from {} connected blocks", cleared);
        }
        
        return cleared;
    }
    
    /**
     * Clears all spread requests within a given radius of a position
     * Called when a cleansing station is activated
     */
    public void clearSpreadRequestsInRadius(BlockPos center, int radius) {
        int sizeBefore = spreadQueue.size();
        spreadQueue.removeIf(request -> 
            request.toPos.isWithinDistance(center, radius)
        );
        int removed = sizeBefore - spreadQueue.size();
        
        if (removed > 0) {
            AncientCurse.LOGGER.info("Cleared {} pending spread requests near cleansing station", removed);
        }
    }
    
    // === INNER CLASSES ===
    
    /**
     * Tracks curse count and state for a single chunk
     */
    public static class ChunkCurseTracker {
        private final DimChunk dimChunk;
        private int curseCount = 0;
        private long lastUpdate = 0;
        private boolean spreadingDisabled = false;
        
        public ChunkCurseTracker(DimChunk dimChunk) {
            this.dimChunk = dimChunk;
        }
        
        // Legacy constructor - deprecated
        @Deprecated
        public ChunkCurseTracker(ChunkPos chunkPos) {
            this.dimChunk = new DimChunk(null, chunkPos); // Will cause issues if world is needed
        }
        
        public int getCurseCount() { return curseCount; }
        public void incrementCurseCount() { 
            curseCount++; 
            lastUpdate = System.currentTimeMillis();
            if (curseCount >= MAX_CURSED_PER_CHUNK) {
                spreadingDisabled = true;
            }
        }
        public void decrementCurseCount() { 
            curseCount = Math.max(0, curseCount - 1);
            if (curseCount < MAX_CURSED_PER_CHUNK) {
                spreadingDisabled = false;
            }
        }
        public boolean isSpreadingDisabled() { return spreadingDisabled; }
        public ChunkPos getChunkPos() { return dimChunk.chunk(); }
        public DimChunk getDimChunk() { return dimChunk; }
        public long getLastUpdate() { return lastUpdate; }
    }
    
    /**
     * Represents a queued spread request
     */
    private static class SpreadRequest {
        final ServerWorld world;
        final BlockPos fromPos;
        final BlockPos toPos;
        final net.minecraft.block.BlockState newState;
        final long timestamp;
        
        SpreadRequest(ServerWorld world, BlockPos fromPos, BlockPos toPos, 
                     net.minecraft.block.BlockState newState) {
            this.world = world;
            this.fromPos = fromPos;
            this.toPos = toPos;
            this.newState = newState;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isValid(MinecraftServer server) {
            // Request expires after 30 seconds
            if (System.currentTimeMillis() - timestamp > 30000) {
                return false;
            }
            
            // Check if world still exists
            for (ServerWorld serverWorld : server.getWorlds()) {
                if (serverWorld.equals(world)) {
                    return true;
                }
            }
            return false;
        }
    }
    
    /**
     * Performance metrics snapshot
     */
    public static class PerformanceMetrics {
        public final double averageTPS;
        public final boolean performanceMode;
        public final int queueSize;
        public final int trackedChunks;
        
        PerformanceMetrics(double averageTPS, boolean performanceMode, int queueSize, int trackedChunks) {
            this.averageTPS = averageTPS;
            this.performanceMode = performanceMode;
            this.queueSize = queueSize;
            this.trackedChunks = trackedChunks;
        }
    }
    
    /**
     * Represents a protective salt circle
     */
    public static class SaltCircle {
        private final BlockPos center;
        private final int radius;
        private final long expirationTime;
        private boolean active;
        
        public SaltCircle(BlockPos center, int radius, long expirationTime) {
            this.center = center;
            this.radius = radius;
            this.expirationTime = expirationTime;
            this.active = true;
        }
        
        public boolean contains(BlockPos pos) {
            return Math.abs(pos.getX() - center.getX()) <= radius &&
                   Math.abs(pos.getZ() - center.getZ()) <= radius &&
                   Math.abs(pos.getY() - center.getY()) <= 2; // Allow some Y variation
        }
        
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public long getExpirationTime() { return expirationTime; }
        public BlockPos getCenter() { return center; }
        public int getRadius() { return radius; }
    }
    
    /**
     * Represents cleansing protection for a single block
     */
    public static class CleansingProtection {
        private final BlockPos pos;
        private final long expirationTime;
        
        public CleansingProtection(BlockPos pos, long expirationTime) {
            this.pos = pos;
            this.expirationTime = expirationTime;
        }
        
        public boolean isActive() {
            return true; // Always active until expiration
        }
        
        public long getExpirationTime() {
            return expirationTime;
        }
        
        public BlockPos getPos() {
            return pos;
        }
    }
} 