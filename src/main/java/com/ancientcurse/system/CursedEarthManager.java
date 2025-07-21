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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    
    // === TRACKING MAPS ===
    private static final Map<ChunkPos, ChunkCurseTracker> chunkTrackers = new ConcurrentHashMap<>();
    private static final Queue<SpreadRequest> spreadQueue = new LinkedList<>();
    private static final Map<World, Set<ChunkPos>> loadedChunks = new ConcurrentHashMap<>();
    private static final Map<BlockPos, SaltCircle> saltCircles = new ConcurrentHashMap<>();
    private static final Map<BlockPos, CleansingProtection> cleansingProtections = new ConcurrentHashMap<>();
    
    // === PERFORMANCE MONITORING ===
    private static long lastPerformanceCheck = 0;
    private static double averageTPS = 20.0;
    private static boolean performanceMode = false;
    
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
        int maxSpreads = performanceMode ? MAX_SPREADS_PER_TICK / 2 : MAX_SPREADS_PER_TICK;
        
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
            ChunkPos chunkPos = new ChunkPos(request.toPos);
            
            // Check chunk limits
            ChunkCurseTracker tracker = getChunkTracker(chunkPos);
            if (tracker.getCurseCount() >= MAX_CURSED_PER_CHUNK) {
                return false;
            }
            
            // Check for cleansing protection
            if (isProtectedByCleansingStation(request.toPos)) {
                return false;
            }
            
            // ALSO check if we're in an active cleansing zone
            if (com.ancientcurse.block.CleansingStationBlock.isInActiveCleansingZone(request.toPos)) {
                return false; // Don't spread into active cleansing zones
            }
            
            // Attempt the spread
            if (CursedEarthBlock.canSpreadToStatic(world, request.toPos)) {
                // Track original block before converting
                BlockState originalState = world.getBlockState(request.toPos);
                
                // Debug logging
                if (processedSpreads % 50 == 0) {
                    AncientCurse.LOGGER.debug("Tracking original block at {}: {}", request.toPos, originalState.getBlock());
                }
                
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
        // Calculate average TPS over the last 5 seconds
        double currentTPS = Math.min(20.0, 1000.0 / Math.max(server.getTickTime(), 1.0));
        averageTPS = (averageTPS * 0.8) + (currentTPS * 0.2); // Smooth average
        
        // Enable performance mode if TPS drops below threshold
        boolean newPerformanceMode = averageTPS < MIN_TPS_THRESHOLD;
        if (newPerformanceMode != performanceMode) {
            performanceMode = newPerformanceMode;
            AncientCurse.LOGGER.info("Cursed Earth performance mode: {} (TPS: {:.1f})", 
                performanceMode ? "ENABLED" : "DISABLED", averageTPS);
        }
        
        lastPerformanceCheck = System.currentTimeMillis();
    }
    
    /**
     * Cleans up trackers for unloaded chunks
     */
    private void cleanupUnloadedChunks(MinecraftServer server) {
        Set<ChunkPos> chunksToRemove = new HashSet<>();
        
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
     * Cleans up old tracking data for blocks that no longer exist
     */
    private void cleanupOldTrackingData(MinecraftServer server) {
        // This cleanup is handled by the OriginalBlockTracker when chunks are unloaded
        // But we can log statistics here
        for (ServerWorld world : server.getWorlds()) {
            OriginalBlockTracker tracker = OriginalBlockTracker.get(world);
            OriginalBlockTracker.TrackerStats stats = tracker.getStats();
            
            if (stats.blocksTracked > 10000) {
                AncientCurse.LOGGER.info("Original block tracker for {} - Blocks: {}, Memory: {}", 
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
     * Gets the chunk tracker for a given chunk position
     */
    public ChunkCurseTracker getChunkTracker(ChunkPos chunkPos) {
        return chunkTrackers.computeIfAbsent(chunkPos, ChunkCurseTracker::new);
    }
    
    /**
     * Checks if a chunk can accept more cursed blocks
     */
    public boolean canChunkAcceptMore(ChunkPos chunkPos) {
        return getChunkTracker(chunkPos).getCurseCount() < MAX_CURSED_PER_CHUNK;
    }
    
    /**
     * Gets current performance metrics
     */
    public PerformanceMetrics getPerformanceMetrics() {
        return new PerformanceMetrics(averageTPS, performanceMode, spreadQueue.size(), 
            chunkTrackers.size());
    }
    
    /**
     * Creates a salt circle for protection against cursed earth
     */
    public void createSaltCircle(ServerWorld world, BlockPos center, int radius, int duration) {
        long expirationTime = world.getTime() + duration;
        SaltCircle saltCircle = new SaltCircle(center, radius, expirationTime);
        saltCircles.put(center, saltCircle);
        
        AncientCurse.LOGGER.info("Salt circle created at {} with radius {} for {} ticks", 
            center, radius, duration);
    }
    
    /**
     * Checks if there's a salt circle at the given position
     */
    public boolean hasSaltCircle(BlockPos pos) {
        return saltCircles.containsKey(pos);
    }
    
    /**
     * Checks if a position is protected by any salt circle
     */
    public boolean isProtectedBySaltCircle(BlockPos pos) {
        for (SaltCircle circle : saltCircles.values()) {
            if (circle.isActive() && circle.contains(pos)) {
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
        cleansingProtections.put(pos, protection);
        
    }
    
    /**
     * Checks if a position is protected by cleansing station
     */
    public boolean isProtectedByCleansingStation(BlockPos pos) {
        // Check the exact position
        CleansingProtection protection = cleansingProtections.get(pos);
        if (protection != null && protection.isActive()) {
            return true;
        }
        
        // Also check vertical column (5 blocks up and down) to prevent surface finding bypass
        for (int y = -5; y <= 5; y++) {
            BlockPos checkPos = pos.add(0, y, 0);
            protection = cleansingProtections.get(checkPos);
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
    public boolean clearProtectionAt(BlockPos pos) {
        CleansingProtection removed = cleansingProtections.remove(pos);
        return removed != null;
    }
    
    /**
     * Clears all connected protection starting from a given position
     * Uses flood fill to find all connected protected blocks
     * @return number of blocks cleared
     */
    public int clearAllConnectedProtection(BlockPos startPos) {
        if (!isProtectedByCleansingStation(startPos)) {
            return 0;
        }
        
        Set<BlockPos> toProcess = new HashSet<>();
        Set<BlockPos> processed = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(startPos);
        
        // Find all connected protected blocks
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            if (processed.contains(current)) continue;
            processed.add(current);
            
            if (isProtectedByCleansingStation(current)) {
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
        }
        
        // Clear all found protections
        int cleared = 0;
        for (BlockPos pos : toProcess) {
            if (clearProtectionAt(pos)) {
                cleared++;
            }
        }
        
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
        private final ChunkPos chunkPos;
        private int curseCount = 0;
        private long lastUpdate = 0;
        private boolean spreadingDisabled = false;
        
        public ChunkCurseTracker(ChunkPos chunkPos) {
            this.chunkPos = chunkPos;
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
        public ChunkPos getChunkPos() { return chunkPos; }
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