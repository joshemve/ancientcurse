package com.ancientcurse.block;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.ModBlocks;
import com.ancientcurse.ModItems;
import com.ancientcurse.block.CursedEarthBlock;
import com.ancientcurse.system.OriginalBlockTracker;
import com.ancientcurse.system.CursedEarthManager;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.tick.TickPriority;

import java.util.*;

/**
 * Cleansing Station that purifies ALL connected cursed earth when activated with an Eternal Sigil
 */
public class CleansingStationBlock extends Block {
    
    public static final BooleanProperty ACTIVATED = BooleanProperty.of("activated");
    private static final VoxelShape SHAPE = Block.createCuboidShape(0, 0, 0, 16, 12, 16);
    private static final int BLOCKS_PER_TICK = 10000; // Process massive amounts for near-instant cleansing
    private static final int WAVE_TICK_DELAY = 1; // Process every single tick for rapid expansion
    private static final boolean REDUCED_PARTICLES = true; // Reduce particles to prevent lag
    
    // Track active cleansing operations
    private static final Map<BlockPos, CleansingOperation> activeOperations = new HashMap<>();
    private static final int CLEANSING_ZONE_RADIUS = 250; // Disable spreading in this radius
    
    public CleansingStationBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(ACTIVATED, false));
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ACTIVATED);
    }
    
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ItemStack stack = player.getStackInHand(hand);
        
        // Check if player is using Eternal Sigil
        if (stack.getItem() == ModItems.ETERNAL_SIGIL) {
            if (!world.isClient) {
                // Check if already activated
                if (state.get(ACTIVATED)) {
                    player.sendMessage(Text.translatable("block.ancientcurse.cleansing_station.already_active")
                        .formatted(Formatting.YELLOW), true);
                    return ActionResult.SUCCESS;
                }
                
                // Check for nearby cursed earth
                if (!hasNearbyCursedEarth(world, pos)) {
                    player.sendMessage(Text.translatable("block.ancientcurse.cleansing_station.no_corruption")
                        .formatted(Formatting.RED), true);
                    return ActionResult.SUCCESS;
                }
                
                // Activate the cleansing station
                activateStation(world, pos, player);
                
                // Consume one use of the Eternal Sigil (if it has durability)
                if (stack.isDamageable()) {
                    stack.damage(1, player, p -> p.sendToolBreakStatus(hand));
                }
            }
            
            return ActionResult.SUCCESS;
        }
        
        return ActionResult.PASS;
    }
    
    private boolean hasNearbyCursedEarth(World world, BlockPos center) {
        // Check in a small radius for any cursed earth
        for (int x = -5; x <= 5; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -5; z <= 5; z++) {
                    BlockPos checkPos = center.add(x, y, z);
                    if (world.getBlockState(checkPos).getBlock() == ModBlocks.CURSED_EARTH) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private void activateStation(World world, BlockPos pos, PlayerEntity player) {
        // Set altar to activated state
        world.setBlockState(pos, world.getBlockState(pos).with(ACTIVATED, true));
        
        // Clear any pending spread requests in the cleansing zone
        if (world instanceof ServerWorld serverWorld) {
            CursedEarthManager manager = com.ancientcurse.system.CursedEarthManager.getInstance();
            manager.clearSpreadRequestsInRadius(pos, CLEANSING_ZONE_RADIUS);
            
            // IMMEDIATELY create a protection bubble around the entire area
            // This prevents any spreading while we're cleansing
            createProtectionBubble(serverWorld, pos, CLEANSING_ZONE_RADIUS);
        }
        
        // Start the cleansing operation - no radius limit
        CleansingOperation operation = new CleansingOperation(world, pos);
        activeOperations.put(pos, operation);
        
        // Schedule first tick IMMEDIATELY with highest priority
        world.scheduleBlockTick(pos, this, WAVE_TICK_DELAY, TickPriority.HIGH);
        
        // DRAMATIC activation effects
        world.playSound(null, pos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 2.0f, 0.8f);
        world.playSound(null, pos, SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.BLOCKS, 1.5f, 1.0f);
        world.playSound(null, pos, SoundEvents.BLOCK_CONDUIT_ACTIVATE, SoundCategory.BLOCKS, 2.0f, 1.2f);
        
        // Spawn initial particle burst (reduced for performance)
        if (world instanceof ServerWorld serverWorld) {
            // Simple pillar of light
            serverWorld.spawnParticles(
                ParticleTypes.END_ROD,
                pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                20, 0.2, 1.0, 0.2, 0.1
            );
            
            // Small burst effect
            serverWorld.spawnParticles(
                ParticleTypes.FIREWORK,
                pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5,
                10, 0.5, 0.5, 0.5, 0.1
            );
        }
        
        player.sendMessage(Text.translatable("block.ancientcurse.cleansing_station.activated")
            .formatted(Formatting.GREEN), true);
    }
    
    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, net.minecraft.util.math.random.Random random) {
        if (!state.get(ACTIVATED)) return;
        
        CleansingOperation operation = activeOperations.get(pos);
        if (operation == null) return;
        
        // Process the cleansing
        boolean hasMore = operation.tick();
        
        // Visual effects every few ticks
        if (world.getTime() % 5 == 0) {
            world.spawnParticles(
                ParticleTypes.END_ROD,
                pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                5, 0.25, 0.25, 0.25, 0.05
            );
        }
        
        if (hasMore) {
            // Schedule next tick IMMEDIATELY with highest priority for rapid cleansing
            world.scheduleBlockTick(pos, this, WAVE_TICK_DELAY, TickPriority.HIGH);
        } else {
            // Cleansing complete
            completeCleansingOperation(world, pos, operation);
        }
    }
    
    private void completeCleansingOperation(World world, BlockPos pos, CleansingOperation operation) {
        // Remove from active operations
        activeOperations.remove(pos);
        
        // Ensure ALL cleansed positions have 10-day protection
        if (world instanceof ServerWorld serverWorld) {
            CursedEarthManager manager = CursedEarthManager.getInstance();
            for (BlockPos cleansedPos : operation.getCleansedPositions()) {
                manager.createCleansingProtection(serverWorld, cleansedPos, 240000); // Re-ensure 10 days
            }
            AncientCurse.LOGGER.info("Cleansing complete! Protected {} blocks for 10 days", operation.getCleansedPositions().size());
        }
        
        // Deactivate altar
        world.setBlockState(pos, world.getBlockState(pos).with(ACTIVATED, false));
        
        // EPIC completion effects - the world is saved!
        world.playSound(null, pos, SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.BLOCKS, 2.0f, 1.0f);
        world.playSound(null, pos, SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.BLOCKS, 1.5f, 1.2f);
        world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.BLOCKS, 1.0f, 1.5f);
        
        if (world instanceof ServerWorld serverWorld) {
            // Simple completion effects (reduced for performance)
            // Flash effect
            serverWorld.spawnParticles(
                ParticleTypes.FLASH,
                pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                1, 0, 0, 0, 0
            );
            
            // Victory particles
            serverWorld.spawnParticles(
                ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                20, 1.0, 1.0, 1.0, 0
            );
            
            // Protection aura
            serverWorld.spawnParticles(
                ParticleTypes.ENCHANT,
                pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5,
                30, 2.0, 1.0, 2.0, 0.1
            );
        }
        
        // Notify nearby players
        world.getPlayers().forEach(player -> {
            if (player.getBlockPos().isWithinDistance(pos, 64)) {
                player.sendMessage(Text.translatable("block.ancientcurse.cleansing_station.complete", 
                    operation.getBlocksCleansed()).formatted(Formatting.GREEN), true);
                player.sendMessage(Text.translatable("block.ancientcurse.cleansing_station.protection_active")
                    .formatted(Formatting.AQUA), false);
            }
        });
    }
    
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            // Clean up any active operation
            activeOperations.remove(pos);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
    
    /**
     * Check if a position is within any active cleansing zone
     */
    public static boolean isInActiveCleansingZone(BlockPos pos) {
        for (Map.Entry<BlockPos, CleansingOperation> entry : activeOperations.entrySet()) {
            BlockPos stationPos = entry.getKey();
            if (pos.isWithinDistance(stationPos, CLEANSING_ZONE_RADIUS)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Creates immediate protection bubble to prevent spreading during cleansing
     */
    private void createProtectionBubble(ServerWorld world, BlockPos center, int radius) {
        // The isInActiveCleansingZone check should be sufficient for preventing spread
        // We don't need to create thousands of protection entries
        AncientCurse.LOGGER.info("Cleansing zone active within {} blocks - spreading disabled", radius);
    }
    
    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }
    
    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return state.get(ACTIVATED) ? 15 : 0;
    }
    
    /**
     * Handles the cleansing operation for the station
     */
    private static class CleansingOperation {
        private final World world;
        private final BlockPos origin;
        private Queue<BlockPos> currentWave = new LinkedList<>();
        private Queue<BlockPos> nextWave = new LinkedList<>();
        private Queue<BlockPos> cursedEarthQueue = new LinkedList<>(); // Priority queue for cursed earth
        private Set<BlockPos> processed = new HashSet<>();
        private Set<BlockPos> cleansedPositions = new HashSet<>(); // Track all cleansed positions
        private int blocksCleansed = 0;
        private static final int MAX_SEARCH_RADIUS = 250; // Match the cleansing zone radius
        private static final int RING_THICKNESS = 3; // Process blocks in thick rings for faster spread
        private int emptyWaveCount = 0; // Track waves with no cursed earth found
        
        public CleansingOperation(World world, BlockPos origin) {
            this.world = world;
            this.origin = origin;
            
            AncientCurse.LOGGER.info("Starting cleansing operation - scanning for all cursed earth within {} blocks", MAX_SEARCH_RADIUS);
            
            // More efficient scanning approach
            int foundCount = scanForCursedEarth();
            
            AncientCurse.LOGGER.info("Initial scan found {} cursed earth blocks", foundCount);
            
            // Start with a small wave around the origin
            for (int x = -3; x <= 3; x++) {
                for (int y = -2; y <= 2; y++) {
                    for (int z = -3; z <= 3; z++) {
                        currentWave.add(origin.add(x, y, z));
                    }
                }
            }
        }
        
        /**
         * Efficient scanning for cursed earth - flood fill from nearby cursed earth
         */
        private int scanForCursedEarth() {
            int foundCount = 0;
            Set<BlockPos> visited = new HashSet<>();
            Queue<BlockPos> toCheck = new LinkedList<>();
            
            AncientCurse.LOGGER.info("Starting efficient cursed earth scan");
            
            // First, find any cursed earth nearby to start the flood fill
            boolean foundStart = false;
            for (int radius = 1; radius <= 20 && !foundStart; radius++) {
                for (int x = -radius; x <= radius; x++) {
                    for (int y = -5; y <= 5; y++) {
                        for (int z = -radius; z <= radius; z++) {
                            if (Math.abs(x) != radius && Math.abs(z) != radius) continue; // Only check perimeter
                            
                            BlockPos pos = origin.add(x, y, z);
                            if (world.getBlockState(pos).getBlock() == ModBlocks.CURSED_EARTH) {
                                toCheck.add(pos);
                                visited.add(pos);
                                foundStart = true;
                                foundCount++;
                            }
                        }
                    }
                }
            }
            
            if (!foundStart) {
                AncientCurse.LOGGER.info("No cursed earth found near cleansing station");
                return 0;
            }
            
            // Flood fill to find all connected cursed earth
            while (!toCheck.isEmpty() && foundCount < 100000) { // Safety limit
                BlockPos current = toCheck.poll();
                
                if (current.isWithinDistance(origin, MAX_SEARCH_RADIUS)) {
                    cursedEarthQueue.add(current);
                    processed.add(current);
                    
                    // Check all 26 neighbors
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            for (int dz = -1; dz <= 1; dz++) {
                                if (dx == 0 && dy == 0 && dz == 0) continue;
                                
                                BlockPos neighbor = current.add(dx, dy, dz);
                                if (!visited.contains(neighbor) && neighbor.isWithinDistance(origin, MAX_SEARCH_RADIUS)) {
                                    visited.add(neighbor);
                                    
                                    if (world.getBlockState(neighbor).getBlock() == ModBlocks.CURSED_EARTH) {
                                        toCheck.add(neighbor);
                                        foundCount++;
                                        
                                        if (foundCount % 1000 == 0) {
                                            AncientCurse.LOGGER.info("Found {} cursed earth blocks so far", foundCount);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            AncientCurse.LOGGER.info("Scan complete: found {} cursed earth blocks", foundCount);
            return foundCount;
        }
        
        
        private int currentWaveNumber = 0;
        
        public boolean tick() {
            List<BlockPos> cleansedThisTick = new ArrayList<>();
            
            // FIRST PRIORITY: Process all known cursed earth
            int processedFromQueue = 0;
            while (!cursedEarthQueue.isEmpty() && cleansedThisTick.size() < BLOCKS_PER_TICK && processedFromQueue < 5000) {
                BlockPos cursedPos = cursedEarthQueue.poll();
                processedFromQueue++;
                
                // Skip if already processed in this tick
                if (cleansedThisTick.contains(cursedPos)) continue;
                
                BlockState state = world.getBlockState(cursedPos);
                
                // Double-check it's still cursed earth (might have been cleansed already)
                if (state.getBlock() == ModBlocks.CURSED_EARTH) {
                    if (cleanseBlock(cursedPos)) {
                        blocksCleansed++;
                        cleansedThisTick.add(cursedPos);
                        cleansedPositions.add(cursedPos);
                    } else {
                        // If cleansing failed, try again next tick
                        cursedEarthQueue.offer(cursedPos);
                    }
                }
            }
            
            // SECOND: Process wave to find more cursed earth
            int waveProcessed = 0;
            while (!currentWave.isEmpty() && cleansedThisTick.size() < BLOCKS_PER_TICK && waveProcessed < 1000) {
                BlockPos pos = currentWave.poll();
                waveProcessed++;
                
                if (processed.contains(pos)) continue;
                processed.add(pos);
                
                // Check if it's cursed earth
                BlockState state = world.getBlockState(pos);
                boolean isCursedEarth = state.getBlock() == ModBlocks.CURSED_EARTH;
                
                // Try to cleanse if it's cursed earth
                if (isCursedEarth) {
                    // Add to priority queue for immediate processing
                    cursedEarthQueue.add(pos);
                    
                    if (cleanseBlock(pos)) {
                        blocksCleansed++;
                        cleansedThisTick.add(pos);
                        cleansedPositions.add(pos); // Track for protection
                    }
                }
                
                // Always check all neighbors within radius
                if (pos.isWithinDistance(origin, MAX_SEARCH_RADIUS - 5)) { // Leave margin for neighbors
                    // Check all 26 neighbors regardless of current block type
                    for (int x = -1; x <= 1; x++) {
                        for (int y = -1; y <= 1; y++) {
                            for (int z = -1; z <= 1; z++) {
                                if (x != 0 || y != 0 || z != 0) {
                                    BlockPos neighbor = pos.add(x, y, z);
                                    if (!processed.contains(neighbor) && neighbor.isWithinDistance(origin, MAX_SEARCH_RADIUS)) {
                                        // Quick check if it's cursed earth
                                        BlockState neighborState = world.getBlockState(neighbor);
                                        if (neighborState.getBlock() == ModBlocks.CURSED_EARTH) {
                                            cursedEarthQueue.add(neighbor); // Immediate priority!
                                            processed.add(neighbor);
                                        } else {
                                            nextWave.add(neighbor);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Create wave effect for all blocks cleansed this tick
            if (!cleansedThisTick.isEmpty() && world instanceof ServerWorld serverWorld) {
                createWaveEffect(serverWorld, cleansedThisTick);
                
                // Log progress occasionally
                if (currentWaveNumber % 20 == 0 && !cleansedThisTick.isEmpty()) {
                    AncientCurse.LOGGER.info("Cleansing progress: {} blocks cleansed, {} in queue", 
                        blocksCleansed, cursedEarthQueue.size());
                }
            }
            
            // This is now handled at the start of tick()
            
            // Move to next wave if current is empty
            if (currentWave.isEmpty() && !nextWave.isEmpty()) {
                currentWave = nextWave;
                nextWave = new LinkedList<>();
                currentWaveNumber++;
                
                // Track empty waves
                if (cleansedThisTick.isEmpty()) {
                    emptyWaveCount++;
                } else {
                    emptyWaveCount = 0;
                }
            }
            
            // Continue if:
            // 1. We have known cursed earth to process
            // 2. We have waves to explore
            // 3. We haven't exhausted our search
            boolean shouldContinue = !cursedEarthQueue.isEmpty() || 
                                   !currentWave.isEmpty() || 
                                   (currentWaveNumber < 200 && emptyWaveCount < 20);
            
            if (!shouldContinue) {
                AncientCurse.LOGGER.info("Cleansing operation complete. Cleansed {} blocks in {} waves.", 
                    blocksCleansed, currentWaveNumber);
            }
            
            return shouldContinue;
        }
        
        private void createWaveEffect(ServerWorld world, List<BlockPos> positions) {
            if (!REDUCED_PARTICLES) return; // Skip most particles to prevent lag
            
            // Calculate average distance for this wave
            double avgDistance = positions.stream()
                .mapToDouble(pos -> Math.sqrt(pos.getSquaredDistance(origin)))
                .average()
                .orElse(0);
            
            // Only show particles for every 10th block to reduce lag
            int particleCount = 0;
            for (BlockPos pos : positions) {
                if (particleCount++ % 10 != 0) continue;
                
                double distance = Math.sqrt(pos.getSquaredDistance(origin));
                
                // Simple wave indicator
                if (Math.abs(distance - avgDistance) < 3.0) {
                    world.spawnParticles(
                        ParticleTypes.END_ROD,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        1, 0.1, 0.1, 0.1, 0.05
                    );
                }
            }
            
            // Occasional sound effect
            if (!positions.isEmpty() && currentWaveNumber % 5 == 0) {
                BlockPos waveCenter = positions.get(positions.size() / 2);
                world.playSound(null, waveCenter, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 
                    SoundCategory.BLOCKS, 0.2f, 1.5f + (currentWaveNumber * 0.02f));
            }
        }
        
        private boolean cleanseBlock(BlockPos pos) {
            BlockState state = world.getBlockState(pos);
            
            // Check if it's cursed earth
            if (state.getBlock() == ModBlocks.CURSED_EARTH) {
                // Get the original block from the tracker
                BlockState replacement = null;
                boolean hadTrackedData = false;
                
                if (world instanceof ServerWorld serverWorld) {
                    OriginalBlockTracker tracker = OriginalBlockTracker.get(serverWorld);
                    replacement = tracker.getOriginalBlock(pos);
                    
                    if (replacement != null) {
                        hadTrackedData = true;
                        
                        // CRITICAL: If the tracked block is cursed earth, it's invalid tracking data
                        if (replacement.getBlock() == ModBlocks.CURSED_EARTH) {
                            AncientCurse.LOGGER.warn("Invalid tracking data at {} - tracked block is cursed earth!", pos);
                            replacement = null;
                            hadTrackedData = false;
                        } else {
                            // Successfully found original block
                            AncientCurse.LOGGER.debug("Found original block for {}: {}", pos, replacement.getBlock());
                            // Clear tracking AFTER we've successfully used it
                            tracker.clearTracking(pos);
                        }
                    } else {
                        // Check tracker statistics
                        if (blocksCleansed == 0) { // Log once at start
                            OriginalBlockTracker.TrackerStats stats = tracker.getStats();
                            AncientCurse.LOGGER.info("OriginalBlockTracker stats: {} blocks tracked in {} chunks", 
                                stats.blocksTracked, stats.chunksTracked);
                        }
                        
                        // Log when we don't have tracking data
                        if (blocksCleansed % 100 == 0) { // Log every 100 blocks to avoid spam
                            AncientCurse.LOGGER.warn("No tracking data for cursed earth at {}, using fallback", pos);
                        }
                    }
                }
                
                // Fallback if no tracked block or invalid tracking
                if (replacement == null) {
                    replacement = getDefaultReplacementBlock(pos);
                }
                
                // Actually set the block with proper flags
                // Flag 3 = notify neighbors + send to clients
                boolean success = world.setBlockState(pos, replacement, 3);
                
                if (!success) {
                    AncientCurse.LOGGER.error("setBlockState returned false for cursed earth at {}", pos);
                    
                    // Try a more forceful approach
                    world.removeBlock(pos, false);
                    world.setBlockState(pos, replacement, 3);
                }
                
                // Verify the block was changed
                BlockState newState = world.getBlockState(pos);
                if (newState.getBlock() == ModBlocks.CURSED_EARTH) {
                    AncientCurse.LOGGER.error("Failed to cleanse cursed earth at {} - block is still cursed earth!", pos);
                    
                    // Log more details for debugging
                    AncientCurse.LOGGER.error("Attempted to set: {}, Current: {}", replacement, newState);
                    AncientCurse.LOGGER.error("Had tracking data: {}", hadTrackedData);
                    
                    return false;
                }
                
                // Decrement the chunk curse count
                CursedEarthBlock.decrementChunkCurseCount(pos);
                
                // IMMEDIATELY protect this position to prevent re-infection
                if (world instanceof ServerWorld serverWorld) {
                    com.ancientcurse.system.CursedEarthManager.getInstance()
                        .createCleansingProtection(serverWorld, pos, 240000); // 10 days
                }
                
                return true;
            }
            
            return false;
        }
        
        private BlockState getDefaultReplacementBlock(BlockPos pos) {
            // Smart fallback based on surrounding blocks
            for (Direction dir : Direction.values()) {
                BlockPos checkPos = pos.offset(dir);
                BlockState checkState = world.getBlockState(checkPos);
                Block checkBlock = checkState.getBlock();
                
                // Skip cursed earth and air
                if (checkBlock != ModBlocks.CURSED_EARTH && !checkState.isAir()) {
                    // Check if it's a suitable ground block
                    if (checkState.isSolidBlock(world, checkPos) && 
                        !checkState.hasBlockEntity() &&
                        checkBlock != Blocks.BEDROCK) {
                        return checkState;
                    }
                }
            }
            
            // Ultimate fallback based on biome/height
            if (pos.getY() < 63) {
                return Blocks.STONE.getDefaultState();
            } else if (world.getBiome(pos).value().getTemperature() > 0.8f) {
                return ModBlocks.SMOOTH_SAND.getDefaultState(); // Use smooth sand for desert biomes
            } else {
                return Blocks.GRASS_BLOCK.getDefaultState();
            }
        }
        
        public int getBlocksCleansed() {
            return blocksCleansed;
        }
        
        public Set<BlockPos> getCleansedPositions() {
            return new HashSet<>(cleansedPositions);
        }
    }
}