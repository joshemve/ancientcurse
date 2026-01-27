package com.ancientcurse.block;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.ModBlocks;
import com.ancientcurse.ModEntities;
import com.ancientcurse.command.CursedEarthCommand;
import com.ancientcurse.system.CursedEarthManager;
import com.ancientcurse.block.registry.CursedPlantBlocks;
import com.ancientcurse.block.CursedPlantBlock;
import com.ancientcurse.block.SolarSpireBlock;
import com.ancientcurse.util.CurseZoneManager;
import com.ancientcurse.player.AnkhDataManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.registry.Registries;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.BlockView;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Cursed Earth - Performance-first implementation with strict limits
 * 
 * Core Performance Principles:
 * - Never process more than 100 blocks per tick
 * - Spread cap: Maximum 256 cursed blocks per chunk
 * - Server-friendly: TPS should never drop below 18
 * - Prioritizes surface and horizontal spread for natural-looking biome takeover
 */
public class CursedEarthBlock extends BaseAncientCurseBlock {
    // Constants for spread behavior
    private static final int MAX_BLOCKS_PER_CHUNK = 256;
    private static final int MAX_BLOCKS_PER_SECTION = 64;
    private static final int SURFACE_SECTION_BONUS = 32;
    private static final int SPREAD_COOLDOWN = 100; // 5 seconds - faster for sweeping effect
    private static final int DEATH_BURST_COOLDOWN = 6000; // 5 minutes
    private static final int DEATH_BURST_SIZE = 12; // Diameter of death burst
    private static final float HORIZONTAL_SPREAD_CHANCE = 0.85f; // Strong favor for horizontal spread
    private static final float SURFACE_SPREAD_BONUS = 0.4f; // High bonus for surface blocks
    private static final float BRANCH_CHANCE = 0.45f; // Increased chance to create branching patterns
    private static final float MOMENTUM_BONUS = 0.15f; // Reduced momentum for more variation
    private static final int MAX_BRANCH_LENGTH = 5; // Shorter branches for more spider-webbing
    private static final float MULTI_SPREAD_CHANCE = 0.25f; // Chance to spread to multiple blocks
    private static final float ROTATION_CHANCE = 0.35f; // Chance to rotate direction
    private static final Vector3f CURSED_PARTICLE_COLOR = new Vector3f(0.4f, 0.1f, 0.5f);

    // PERFORMANCE LIMITS for tracking maps
    private static final int MAX_TRACKED_COOLDOWNS = 2000; // Max entries in cooldown maps
    private static final int MAX_TRACKED_DIRECTIONS = 1000; // Max entries in spread direction maps
    private static final int CLEANUP_INTERVAL = 1200; // Clean up every 60 seconds instead of 10 minutes
    
    // Leaf decay constants
    private static final int LEAF_WITHER_TIME = 400; // 20 seconds to fully wither (faster to reduce tracking)
    private static final float LEAF_DECAY_CHANCE = 0.02f; // Even lower chance per tick
    private static final int MAX_WITHERING_LEAVES = 10; // Limit concurrent withering to prevent lag
    
    // Tracking maps
    private static final Map<ChunkPos, Integer> chunkCounts = new HashMap<>();
    private static final Map<Long, Integer> sectionCounts = new HashMap<>();
    private static final Map<BlockPos, Long> spreadCooldowns = new HashMap<>();
    private static final Map<BlockPos, Long> deathBurstCooldowns = new HashMap<>();
    private static final Map<PlayerEntity, Long> playerExposureTime = new HashMap<>();
    // New tracking for branching spread
    private static final Map<BlockPos, Direction> spreadDirections = new HashMap<>();
    private static final Map<BlockPos, Integer> branchLengths = new HashMap<>();
    private static final Map<BlockPos, BlockPos> spreadOrigins = new HashMap<>();
    // Cache for block spreadability to optimize performance
    private static final Map<Block, Boolean> spreadableBlockCache = new HashMap<>();
    // Tracking for leaf withering
    private static final Map<BlockPos, Long> leafWitherStartTimes = new HashMap<>();
    
    public CursedEarthBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, BlockView world, List<Text> tooltip, TooltipContext options) {
        // Don't add the base Ancient Curse tooltip for cursed earth
        // If you want a specific tooltip, add it here instead
    }

    /**
     * Called when an entity steps on this block.
     * This method triggers immediately and reliably when a player steps on cursed earth,
     * unlike randomTick which is called infrequently.
     */
    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        super.onSteppedOn(world, pos, state, entity);

        if (!world.isClient && entity instanceof PlayerEntity player) {
            // Skip creative/spectator players
            if (player.isCreative() || player.isSpectator()) {
                return;
            }

            // Check if we're in an active cleansing zone - if so, do nothing
            if (SolarSpireBlock.isInActiveCleansingZone(pos)) {
                return;
            }

            // Decrease ankh value when standing on cursed earth
            // Using world time modulo to rate-limit the effect (every 2.5 seconds = 50 ticks)
            if (world.getTime() % 50 == 0) {
                int newAnkh = AnkhDataManager.decreaseAnkhValue(player, 1);

                // Handle Ankh depletion when standing on cursed blocks
                if (newAnkh <= 0) {
                    handleAnkhDepletionOnCursedBlock(player, (ServerWorld) world, pos);
                }
            }
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        // Only handle removal if the block is actually being replaced with a different type
        if (!state.isOf(newState.getBlock()) && !world.isClient) {
            // Notify CursedEarthManager that a cursed block was removed
            CursedEarthManager.getInstance().onCursedBlockRemoved((ServerWorld) world, pos);

            // PERFORMANCE: Also clean up local tracking data immediately
            spreadCooldowns.remove(pos);
            spreadDirections.remove(pos);
            branchLengths.remove(pos);
            spreadOrigins.remove(pos);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
    
    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // PERFORMANCE: Run cleanup on first random tick of each cleanup interval
        // This ensures cleanup runs regularly even with many cursed blocks
        cleanupStaleData(world);

        // FIRST check if we're in an active cleansing zone - if so, do NOTHING
        if (SolarSpireBlock.isInActiveCleansingZone(pos)) {
            return; // No spreading, no effects, nothing while cleansing is active
        }

        // Attempt to spread the curse to nearby blocks
        if (CursedEarthCommand.isEnabled() && random.nextFloat() < 0.3f) {
            attemptSpread(world, pos, random);
        } else if (!CursedEarthCommand.isEnabled() && world.getTime() % 100 == 0) {
            // Log once every 5 seconds if spreading is disabled
            // Debug: Cursed earth spreading is disabled
        }
        
        // Attempt to spawn cursed plants on top (rare)
        if (random.nextFloat() < 0.01f && world.getBlockState(pos.up()).isAir()) {
            attemptPlantSpawn(world, pos.up(), random);
        }
        
        // Very rare chance to spawn cursed entities (reduced from 0.001f to 0.0002f - 80% reduction)
        if (random.nextFloat() < 0.0002f) {
            attemptEntitySpawn(world, pos.up(), random);
        }
        
        // Decay nearby leaves (much less frequently)
        if (random.nextFloat() < LEAF_DECAY_CHANCE) {
            decayNearbyLeaves(world, pos, random);
        }
        
        // Replace nearby grass with Reed of Sekhem
        if (random.nextFloat() < 0.05f) { // 5% chance per tick
            replaceNearbyGrass(world, pos, random);
        }
        
        // Apply effects to nearby players
        applyEffectsToNearbyPlayers(world, pos);
    }
    
    /**
     * Attempts to spread the curse to nearby blocks
     * Uses tree-branching patterns and directional momentum for organic spread
     */
    private void attemptSpread(ServerWorld world, BlockPos pos, Random random) {
        // Early optimization: verify we're still cursed earth
        if (world.getBlockState(pos).getBlock() != ModBlocks.CURSED_EARTH) {
            return;
        }
        
        // Check cooldown
        long currentTime = world.getTime();
        if (currentTime - spreadCooldowns.getOrDefault(pos, 0L) < SPREAD_COOLDOWN) {
            return;
        }
        
        // Update cooldown with variation for organic feel
        spreadCooldowns.put(pos, currentTime - (random.nextInt(30))); 
        
        // Check if we've hit the chunk limit
        ChunkPos chunkPos = new ChunkPos(pos);
        int currentCount = chunkCounts.getOrDefault(chunkPos, 0);
        if (currentCount >= MAX_BLOCKS_PER_CHUNK) {
            return;
        }
        
        // Get or determine spread direction for this block
        Direction currentDirection = spreadDirections.get(pos);
        BlockPos origin = spreadOrigins.getOrDefault(pos, pos);
        int branchLength = branchLengths.getOrDefault(pos, 0);
        
        // Check if we're at the surface
        boolean isSurface = world.isSkyVisible(pos.up());
        
        // Create list of potential spread targets
        List<SpreadCandidate> candidates = new ArrayList<>();
        
        // Determine if we should rotate direction (for more organic patterns)
        boolean shouldRotate = random.nextFloat() < ROTATION_CHANCE || branchLength > MAX_BRANCH_LENGTH;
        
        // Create more dynamic direction list with weighted randomness
        List<Direction> primaryDirs = new ArrayList<>();
        List<Direction> secondaryDirs = new ArrayList<>();
        
        // If we have momentum, favor directions based on current direction
        if (currentDirection != null && !shouldRotate) {
            // Primary direction (forward)
            primaryDirs.add(currentDirection);
            
            // Adjacent directions (45 degrees)
            if (currentDirection.getAxis() != Direction.Axis.Y) {
                Direction left = currentDirection.rotateYCounterclockwise();
                Direction right = currentDirection.rotateYClockwise();
                if (random.nextFloat() < 0.7f) primaryDirs.add(left);
                if (random.nextFloat() < 0.7f) primaryDirs.add(right);
                secondaryDirs.add(currentDirection.getOpposite());
            }
        }
        
        // Add remaining cardinal directions
        for (Direction dir : Direction.Type.HORIZONTAL) {
            if (!primaryDirs.contains(dir) && !secondaryDirs.contains(dir)) {
                secondaryDirs.add(dir);
            }
        }
        
        // Shuffle secondary directions for randomness
        Collections.shuffle(secondaryDirs, new java.util.Random(pos.hashCode() + world.getTime()));
        
        // Process primary directions first
        for (Direction dir : primaryDirs) {
            BlockPos targetPos = pos.offset(dir);
            BlockPos surfacePos = findSurfaceBlock(world, targetPos, 3, 5);
            if (surfacePos != null && canSpreadTo(world, surfacePos)) {
                float weight = calculateSpreadWeight(world, pos, surfacePos, dir, currentDirection, isSurface, random);
                weight *= 1.2f; // Bonus for primary directions
                candidates.add(new SpreadCandidate(surfacePos, dir, weight));
            }
        }
        
        // Process secondary directions
        for (Direction dir : secondaryDirs) {
            BlockPos targetPos = pos.offset(dir);
            BlockPos surfacePos = findSurfaceBlock(world, targetPos, 3, 5);
            if (surfacePos != null && canSpreadTo(world, surfacePos)) {
                float weight = calculateSpreadWeight(world, pos, surfacePos, dir, currentDirection, isSurface, random);
                candidates.add(new SpreadCandidate(surfacePos, dir, weight));
            }
        }
        
        // Add diagonal positions with varied distances for organic shape
        int diagonalDistance = random.nextFloat() < 0.3f ? 2 : 1; // Sometimes reach further
        List<BlockPos> diagonalPositions = new ArrayList<>();
        
        // Close diagonals
        diagonalPositions.add(pos.north().east());
        diagonalPositions.add(pos.north().west());
        diagonalPositions.add(pos.south().east());
        diagonalPositions.add(pos.south().west());
        
        // Far diagonals for more variation
        if (diagonalDistance > 1) {
            diagonalPositions.add(pos.north(2).east());
            diagonalPositions.add(pos.north().east(2));
            diagonalPositions.add(pos.south(2).west());
            diagonalPositions.add(pos.south().west(2));
        }
        
        for (BlockPos targetPos : diagonalPositions) {
            BlockPos surfacePos = findSurfaceBlock(world, targetPos, 3, 5);
            if (surfacePos != null && canSpreadTo(world, surfacePos)) {
                Direction primaryDir = Direction.fromVector(targetPos.getX() - pos.getX(), 0, targetPos.getZ() - pos.getZ());
                float weight = calculateSpreadWeight(world, pos, surfacePos, primaryDir, currentDirection, isSurface, random);
                weight *= (0.7f + random.nextFloat() * 0.3f); // More variation for diagonals
                candidates.add(new SpreadCandidate(surfacePos, primaryDir, weight));
            }
        }
        
        // Sort candidates by weight but add some randomness to avoid too predictable patterns
        if (!candidates.isEmpty() && random.nextFloat() < 0.3f) {
            // Sometimes shuffle top candidates for more organic feel
            int topCount = Math.min(3, candidates.size());
            List<SpreadCandidate> topCandidates = new ArrayList<>(candidates.subList(0, topCount));
            Collections.shuffle(topCandidates);
            for (int i = 0; i < topCount; i++) {
                candidates.set(i, topCandidates.get(i));
            }
        } else {
            candidates.sort((a, b) -> Float.compare(b.weight, a.weight));
        }
        
        // Determine number of spreads this tick
        int spreadsThisTick = 0;
        int maxSpreads = 1;
        
        // Multi-spread chances for more organic growth
        if (random.nextFloat() < MULTI_SPREAD_CHANCE) {
            if (isSurface && candidates.size() >= 3) {
                maxSpreads = 2 + (random.nextFloat() < 0.1f ? 1 : 0); // Rarely 3
            } else if (candidates.size() >= 2) {
                maxSpreads = 2;
            }
        }
        
        // Special burst spreading for very organic patterns
        boolean burstSpread = random.nextFloat() < 0.05f && candidates.size() >= 4;
        if (burstSpread) {
            maxSpreads = Math.min(4, candidates.size());
        }
        
        // Track if we're creating new branches
        boolean createdNewBranch = false;
        
        for (int i = 0; i < candidates.size() && spreadsThisTick < maxSpreads; i++) {
            SpreadCandidate candidate = candidates.get(i);
            
            // Use weighted random selection with decay
            float selectionChance = candidate.weight * (1.0f - (i * 0.15f));
            if (random.nextFloat() < selectionChance) {
                // FINAL CHECK: Don't queue spread if target is in active cleansing zone
                if (SolarSpireBlock.isInActiveCleansingZone(candidate.pos)) {
                    continue; // Skip this candidate
                }
                
                // Determine which cursed block type to spread based on target
                BlockState targetBlockState = world.getBlockState(candidate.pos);
                BlockState targetState;
                
                // Check what type of block we're spreading to
                if (isStoneType(targetBlockState.getBlock())) {
                    targetState = ModBlocks.CURSED_STONE.getDefaultState();
                } else if (isLeafBlock(targetBlockState)) {
                    // Skip leaves - they decay naturally, don't convert them
                    continue;
                } else if (CursedLogBlock.isLogBlock(targetBlockState)) {
                    // Preserve the axis orientation for logs
                    targetState = ModBlocks.CURSED_LOG.getDefaultState();
                    if (targetBlockState.contains(net.minecraft.block.PillarBlock.AXIS)) {
                        targetState = targetState.with(net.minecraft.block.PillarBlock.AXIS, 
                                                      targetBlockState.get(net.minecraft.block.PillarBlock.AXIS));
                    }
                } else if (CursedLogBlock.isPlankBlock(targetBlockState)) {
                    targetState = ModBlocks.CURSED_WOOD_PLANK.getDefaultState();
                } else if (CursedSandBlock.isSandBlock(targetBlockState)) {
                    targetState = ModBlocks.CURSED_SAND.getDefaultState();
                } else {
                    targetState = ModBlocks.CURSED_EARTH.getDefaultState();
                }
                
                // Queue the spread
                CursedEarthManager.getInstance().queueSpread(world, pos, candidate.pos, targetState);
                
                // Update tracking
                chunkCounts.put(chunkPos, currentCount + spreadsThisTick + 1);
                
                // Determine if this is a new branch
                boolean isNewBranch = createdNewBranch || random.nextFloat() < BRANCH_CHANCE || 
                                     (candidate.direction != currentDirection && random.nextFloat() < 0.5f);
                
                // Set direction for the new block
                if (candidate.direction != null) {
                    // Add slight direction variation for more organic growth
                    Direction finalDirection = candidate.direction;
                    if (random.nextFloat() < 0.15f && candidate.direction.getAxis() != Direction.Axis.Y) {
                        // Occasionally shift direction slightly
                        finalDirection = random.nextBoolean() ? 
                            candidate.direction.rotateYClockwise() : 
                            candidate.direction.rotateYCounterclockwise();
                    }
                    spreadDirections.put(candidate.pos, finalDirection);
                    
                    // Track branch length
                    if (candidate.direction == currentDirection && !isNewBranch) {
                        branchLengths.put(candidate.pos, branchLength + 1);
                    } else {
                        branchLengths.put(candidate.pos, 1);
                        createdNewBranch = true;
                    }
                    
                    // Set origin
                    if (isNewBranch) {
                        spreadOrigins.put(candidate.pos, candidate.pos); // Start new branch
                    } else {
                        spreadOrigins.put(candidate.pos, origin); // Continue current branch
                    }
                }
                
                // Create sweeping particle effect
                createSpreadParticles(world, pos, candidate.pos, random);
                
                spreadsThisTick++;
            }
        }
    }
    
    /**
     * Calculate weight for spreading to a target position
     */
    private float calculateSpreadWeight(ServerWorld world, BlockPos from, BlockPos to, Direction dir, 
                                       Direction currentDir, boolean isSurface, Random random) {
        float weight = 0.4f + random.nextFloat() * 0.2f; // More variable base weight
        
        // Surface bonus - strongly favor surface spreading
        if (isSurface && world.isSkyVisible(to.up())) {
            weight += SURFACE_SPREAD_BONUS;
            
            // Extra bonus for connecting surface patches
            int nearbySurfaceCount = 0;
            for (Direction d : Direction.Type.HORIZONTAL) {
                BlockPos check = to.offset(d);
                if (world.isSkyVisible(check.up())) {
                    nearbySurfaceCount++;
                }
            }
            weight += nearbySurfaceCount * 0.03f;
        }
        
        // Reduced momentum bonus with variation
        if (dir != null && dir == currentDir) {
            weight += MOMENTUM_BONUS * (0.5f + random.nextFloat() * 0.5f);
        }
        
        // Smart height handling for hills and slopes
        int heightDiff = to.getY() - from.getY();
        
        // Check if we're following a slope
        boolean isFollowingSlope = isPartOfSlope(world, from, to);
        
        if (heightDiff == 0) {
            weight += 0.15f * (0.8f + random.nextFloat() * 0.4f); // Variable preference
        } else if (Math.abs(heightDiff) == 1) {
            // Single block height difference is natural for terrain
            if (isFollowingSlope) {
                weight += 0.12f + random.nextFloat() * 0.05f; // Bonus for following natural slopes
            } else {
                weight += 0.08f;
            }
        } else if (Math.abs(heightDiff) == 2) {
            // Two block difference can happen on steep hills
            if (isFollowingSlope) {
                weight += 0.05f;
            } else {
                weight -= 0.1f * random.nextFloat(); // Variable penalty
            }
        } else {
            // Large height differences are penalized less if following terrain
            weight -= isFollowingSlope ? 0.05f * Math.abs(heightDiff) : 0.15f * Math.abs(heightDiff);
        }
        
        // Prefer spreading downhill slightly (water-like flow) with variation
        if (heightDiff < 0 && heightDiff >= -2) {
            weight += 0.05f + random.nextFloat() * 0.03f;
        }
        
        // Check if target leads to more spreadable blocks (branching potential)
        int spreadableSurrounding = 0;
        int cursedSurrounding = 0;
        for (Direction checkDir : Direction.Type.HORIZONTAL) {
            BlockPos checkPos = to.offset(checkDir);
            if (canSpreadTo(world, checkPos)) {
                spreadableSurrounding++;
            } else if (world.getBlockState(checkPos).getBlock() == ModBlocks.CURSED_EARTH) {
                cursedSurrounding++;
            }
        }
        
        // Favor spreading to areas with potential but not already surrounded
        weight += spreadableSurrounding * 0.05f;
        weight -= cursedSurrounding * 0.08f; // Avoid areas already heavily corrupted
        
        // Add clustering tendency - sometimes prefer to fill gaps
        if (random.nextFloat() < 0.3f && cursedSurrounding >= 2) {
            weight += 0.15f; // Fill in gaps between corrupted areas
        }
        
        // Distance-based variation for more organic shapes
        double distance = Math.sqrt(Math.pow(to.getX() - from.getX(), 2) + Math.pow(to.getZ() - from.getZ(), 2));
        if (distance > 1.5) { // Diagonal or far spread
            weight *= 0.85f + random.nextFloat() * 0.15f;
        }
        
        // Add final randomness layer for organic feel
        weight *= 0.8f + random.nextFloat() * 0.4f;
        
        return Math.max(0.1f, Math.min(1.0f, weight));
    }
    
    /**
     * Create particle effects showing spread direction
     */
    private void createSpreadParticles(ServerWorld world, BlockPos from, BlockPos to, Random random) {
        // Create a line of particles from source to target
        double steps = 5;
        for (int i = 0; i <= steps; i++) {
            double progress = i / steps;
            double x = from.getX() + 0.5 + (to.getX() - from.getX()) * progress;
            double y = from.getY() + 0.2 + (to.getY() - from.getY()) * progress;
            double z = from.getZ() + 0.5 + (to.getZ() - from.getZ()) * progress;
            
            // Add some randomness
            x += (random.nextDouble() - 0.5) * 0.2;
            z += (random.nextDouble() - 0.5) * 0.2;
            
            world.addParticle(
                new DustParticleEffect(CURSED_PARTICLE_COLOR, 0.6f),
                x, y, z,
                0, 0.02, 0
            );
        }
    }
    
    // Helper class for spread candidates
    private static class SpreadCandidate {
        final BlockPos pos;
        final Direction direction;
        final float weight;
        
        SpreadCandidate(BlockPos pos, Direction direction, float weight) {
            this.pos = pos;
            this.direction = direction;
            this.weight = weight;
        }
    }
    
    /**
     * Find the surface block at a given position, checking up and down
     */
    private BlockPos findSurfaceBlock(ServerWorld world, BlockPos pos, int maxUp, int maxDown) {
        // IMPORTANT: We want to REPLACE blocks, not place on top of them
        // Skip if this position is already cursed earth
        if (world.getBlockState(pos).getBlock() == ModBlocks.CURSED_EARTH) {
            return null;
        }
        
        // First check the exact position - can we replace this block?
        if (canSpreadTo(world, pos)) {
            return pos;
        }
        
        // Check upward for blocks to replace (like when going up hills)
        for (int i = 1; i <= maxUp; i++) {
            BlockPos checkPos = pos.up(i);
            BlockState state = world.getBlockState(checkPos);
            
            // Skip if already cursed earth
            if (state.getBlock() == ModBlocks.CURSED_EARTH) {
                continue;
            }
            
            if (canSpreadTo(world, checkPos)) {
                return checkPos;
            }
        }
        
        // Check downward for blocks to replace (like when going down into valleys)
        for (int i = 1; i <= maxDown; i++) {
            BlockPos checkPos = pos.down(i);
            BlockState state = world.getBlockState(checkPos);
            
            // Skip if already cursed earth
            if (state.getBlock() == ModBlocks.CURSED_EARTH) {
                continue;
            }
            
            if (canSpreadTo(world, checkPos)) {
                return checkPos;
            }
            
            // Stop searching down if we hit something we can't replace
            if (!state.isAir() && !canSpreadTo(world, checkPos)) {
                break;
            }
        }
        
        return null;
    }
    
    /**
     * Check if movement from one position to another follows natural terrain slope
     */
    private boolean isPartOfSlope(ServerWorld world, BlockPos from, BlockPos to) {
        int heightDiff = to.getY() - from.getY();
        if (heightDiff == 0) return true;
        
        // Check if there's a gradual slope in the direction we're moving
        Direction moveDir = Direction.fromVector(to.getX() - from.getX(), 0, to.getZ() - from.getZ());
        if (moveDir == null) return false;
        
        // Check the block in the opposite direction to see if we're on a slope
        BlockPos behindPos = from.offset(moveDir.getOpposite());
        BlockPos behindSurface = findSurfaceBlock(world, behindPos, 2, 2);
        
        if (behindSurface != null) {
            int behindHeightDiff = from.getY() - behindSurface.getY();
            // We're on a slope if the height changes consistently
            return (heightDiff > 0 && behindHeightDiff < 0) || (heightDiff < 0 && behindHeightDiff > 0);
        }
        
        return false;
    }
    
    /**
     * Find surface for tendril spreading - handles overhangs and cliffs better
     */
    private static BlockPos findSurfaceForTendril(ServerWorld world, BlockPos pos) {
        // Start from a reasonable height
        int startY = Math.min(world.getTopY() - 1, pos.getY() + 10);
        BlockPos.Mutable mutable = new BlockPos.Mutable(pos.getX(), startY, pos.getZ());
        
        // Search downward for the first SOLID block we can replace
        for (int y = startY; y >= world.getBottomY(); y--) {
            mutable.setY(y);
            BlockState state = world.getBlockState(mutable);
            
            // Skip air - we want to find solid blocks to replace
            if (state.isAir()) {
                continue;
            }
            
            // Skip if already cursed earth
            if (state.getBlock() == ModBlocks.CURSED_EARTH) {
                continue;
            }
            
            // Found a solid block - can we replace it?
            if (canSpreadToStatic(world, mutable)) {
                return mutable.toImmutable();
            }
            
            // If we can't spread to this solid block, stop searching
            break;
        }
        
        return null;
    }
    
    /**
     * Checks if the curse can spread to the target position
     * Uses smart detection for any modded blocks
     */
    private boolean canSpreadTo(ServerWorld world, BlockPos pos) {
        // Check for cleansing protection FIRST
        if (CursedEarthManager.getInstance().isProtectedByCleansingStation(world, pos)) {
            return false;
        }
        
        // Check for salt circle protection
        // TODO: Implement salt circle protection
        // if (SaltCircleBlock.isProtected(world, pos)) {
        //     return false;
        // }
        
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        
        // Don't spread to air or liquids
        if (state.isAir() || state.isLiquid()) {
            return false;
        }
        
        // Check cache first for performance
        Boolean cached = spreadableBlockCache.get(block);
        if (cached != null) {
            return cached;
        }
        
        // Smart detection system
        boolean canSpread = isBlockSpreadable(state, block);
        
        // Cache the result
        spreadableBlockCache.put(block, canSpread);
        
        // Debug logging for first time block checks
        if (canSpread) {
            // Debug: Block is spreadable
        }
        
        return canSpread;
    }
    
    /**
     * Determines if a block is a leaf block that should become blackened leaves
     */
    private boolean isLeafBlock(BlockState state) {
        Block block = state.getBlock();
        
        // Check if it's any type of leaves block
        if (block instanceof LeavesBlock) {
            return true;
        }
        
        // Check if block is tagged as leaves (for modded compatibility)
        if (state.isIn(BlockTags.LEAVES)) {
            return true;
        }
        
        // Check for modded leaves by name
        String blockName = block.getTranslationKey().toLowerCase();
        if (blockName.contains("leaves") || blockName.contains("leaf") || 
            blockName.contains("foliage")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Determines if a block is a stone type that should become cursed stone
     */
    private boolean isStoneType(Block block) {
        // Vanilla stone types
        if (block == Blocks.STONE || block == Blocks.COBBLESTONE || 
            block == Blocks.MOSSY_COBBLESTONE || block == Blocks.STONE_BRICKS ||
            block == Blocks.MOSSY_STONE_BRICKS || block == Blocks.CRACKED_STONE_BRICKS ||
            block == Blocks.CHISELED_STONE_BRICKS || block == Blocks.SMOOTH_STONE ||
            block == Blocks.GRANITE || block == Blocks.POLISHED_GRANITE ||
            block == Blocks.DIORITE || block == Blocks.POLISHED_DIORITE ||
            block == Blocks.ANDESITE || block == Blocks.POLISHED_ANDESITE ||
            block == Blocks.DEEPSLATE || block == Blocks.COBBLED_DEEPSLATE ||
            block == Blocks.POLISHED_DEEPSLATE || block == Blocks.DEEPSLATE_BRICKS ||
            block == Blocks.DEEPSLATE_TILES || block == Blocks.CHISELED_DEEPSLATE ||
            block == Blocks.CRACKED_DEEPSLATE_BRICKS || block == Blocks.CRACKED_DEEPSLATE_TILES ||
            block == Blocks.TUFF || block == Blocks.CALCITE || block == Blocks.DRIPSTONE_BLOCK) {
            return true;
        }
        
        // Check for modded stone blocks by name patterns
        String blockName = block.getTranslationKey().toLowerCase();
        
        // Common stone-related keywords
        String[] stoneKeywords = {
            "stone", "cobble", "brick", "deepslate", "granite", "diorite", 
            "andesite", "basalt", "limestone", "marble", "slate"
        };
        
        // Don't count functional stone blocks or ores
        String[] exclusions = {
            "furnace", "chest", "spawner", "infested", "silverfish",
            "command", "barrier", "bedrock", "ore", "cursed"
        };
        
        // Check exclusions first
        for (String excluded : exclusions) {
            if (blockName.contains(excluded)) {
                return false;
            }
        }
        
        // Check if it contains stone keywords
        for (String keyword : stoneKeywords) {
            if (blockName.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Smart block detection that works with any mod
     * Goal: Corrupt almost everything except functional/special blocks
     */
    private boolean isBlockSpreadable(BlockState state, Block block) {
        // BLACKLIST approach - explicitly exclude what we DON'T want to corrupt
        
        // Never spread to cursed blocks
        if (block == ModBlocks.CURSED_EARTH || block == ModBlocks.CURSED_STONE || 
            block == ModBlocks.CURSED_LOG || block == ModBlocks.CURSED_WOOD_PLANK ||
            block == ModBlocks.CURSED_SAND) {
            return false;
        }
        
        // Never spread to air or liquids
        if (state.isAir() || state.isLiquid()) {
            return false;
        }
        
        // Check if it has a block entity (usually means it's functional)
        if (state.hasBlockEntity()) {
            // Some block entities are OK to corrupt (like signs, banners)
            String blockName = block.getTranslationKey().toLowerCase();
            if (!blockName.contains("sign") && !blockName.contains("banner") && 
                !blockName.contains("skull") && !blockName.contains("head")) {
                return false; // Don't corrupt chests, furnaces, etc.
            }
        }
        
        // Check hardness - bedrock and similar are indestructible
        float hardness = block.getHardness();
        if (hardness < 0 || hardness > 50.0f) {
            return false; // Bedrock, barriers, etc.
        }
        
        // Explicit blacklist of functional blocks that might not have block entities
        String blockName = block.getTranslationKey().toLowerCase();
        String[] blacklist = {
            "enchant", "anvil", "beacon", "spawner", "conduit", "lodestone",
            "respawn", "crying", "portal", "gateway", "frame", "chest", "barrel",
            "hopper", "dropper", "dispenser", "furnace", "blast", "smoker",
            "brewing", "cauldron", "composter", "lectern", "cartography", "loom",
            "stonecutter", "grindstone", "smithing", "fletching", "crafting",
            "shulker", "ender", "command", "structure", "jigsaw", "daylight",
            "comparator", "repeater", "lever", "button", "pressure", "detector",
            "piston", "observer", "note", "jukebox", "target", "tnt", "rail",
            "powered", "activator", "minecart", "boat", "redstone", "torch",
            "machine", "generator", "engine", "dynamo", "reactor", "turbine",
            "pipe", "tube", "duct", "cable", "wire", "energy", "power", "rf",
            "eu", "fe", "mana", "flux", "quantum", "nuclear", "solar", "thermal",
            "void", "storage", "tank", "cell", "battery", "capacitor", "upgrade"
        };
        
        for (String excluded : blacklist) {
            if (blockName.contains(excluded)) {
                return false;
            }
        }
        
        // Special check for modded functional blocks
        String namespace = Registries.BLOCK.getId(block).getNamespace();
        if (!namespace.equals("minecraft") && !namespace.equals(AncientCurse.MOD_ID)) {
            // For modded blocks, be more careful about technical blocks
            if (blockName.contains("_on") || blockName.contains("_off") ||
                blockName.contains("_active") || blockName.contains("_inactive") ||
                blockName.contains("tier") || blockName.contains("mk")) {
                return false; // Likely a machine with states
            }
        }
        
        // Everything else can be corrupted!
        // This includes:
        // - All stone types (including obsidian)
        // - All wood types (logs, planks, etc.)
        // - All concrete, terracotta, wool
        // - All glass types
        // - All metal blocks (iron, gold, etc.)
        // - All decorative blocks
        // - All building blocks from any mod
        return true;
    }
    
    /**
     * Static version of canSpreadTo for death burst
     */
    public static boolean canSpreadToStatic(ServerWorld world, BlockPos pos) {
        // Check for cleansing protection FIRST
        if (CursedEarthManager.getInstance().isProtectedByCleansingStation(world, pos)) {
            return false;
        }
        
        // Create a temporary instance to use the smart detection
        CursedEarthBlock temp = (CursedEarthBlock) ModBlocks.CURSED_EARTH;
        return temp.canSpreadTo(world, pos);
    }
    
    /**
     * Static version of isLeafBlock for death burst and tendril spreading
     */
    private static boolean isLeafBlockStatic(BlockState state) {
        CursedEarthBlock temp = (CursedEarthBlock) ModBlocks.CURSED_EARTH;
        return temp.isLeafBlock(state);
    }
    
    /**
     * Static version of isStoneType for death burst
     */
    private static boolean isStoneTypeStatic(Block block) {
        CursedEarthBlock temp = (CursedEarthBlock) ModBlocks.CURSED_EARTH;
        return temp.isStoneType(block);
    }
    
    /**
     * Apply effects to nearby players
     */
    private void applyEffectsToNearbyPlayers(ServerWorld world, BlockPos pos) {
        // Check for players standing directly on this block
        BlockPos abovePos = pos.up();
        Box standingBox = new Box(pos.getX(), pos.getY() + 1, pos.getZ(), 
                                  pos.getX() + 1, pos.getY() + 1.1, pos.getZ() + 1);
        List<PlayerEntity> standingPlayers = world.getNonSpectatingEntities(PlayerEntity.class, standingBox);
        
        for (PlayerEntity player : standingPlayers) {
            // Skip creative/spectator players
            if (player.isCreative() || player.isSpectator()) {
                continue;
            }
            
            // Check if player is actually standing on this block
            BlockPos playerBlockPos = player.getBlockPos().down();
            if (!playerBlockPos.equals(pos)) {
                continue;
            }
            
            // Only decrease ankh value when standing on cursed earth (no negative status effects)
            long currentTime = world.getTime();
            if (currentTime % 50 == 0) { // Every 2.5 seconds (twice as fast as before)
                int newAnkh = AnkhDataManager.decreaseAnkhValue(player, 1);

                // Handle Ankh depletion when standing on cursed blocks
                if (newAnkh <= 0) {
                    handleAnkhDepletionOnCursedBlock(player, world, pos);
                }
            }
            
            // Apply additional effects based on exposure time
            // TODO: Implement curse zone checking
            // if (CurseZoneManager.getInstance().isInCurseZone(player)) {
            //     // In curse zone, apply stronger effects
            //     player.addStatusEffect(new StatusEffectInstance(
            //         StatusEffects.WEAKNESS,
            //         100, // 5 seconds
            //         1,
            //         false,
            //         true
            //     ));
            // }
        }
        
        // Clean up old exposure times
        if (world.getTime() % 200 == 0) { // Every 10 seconds
            long currentTime = world.getTime();
            playerExposureTime.entrySet().removeIf(entry -> {
                // Remove if player hasn't been exposed for 10 seconds
                return currentTime - entry.getValue() > 200;
            });
        }
    }
    
    /**
     * Handles what happens when a player's Ankh meter depletes while standing on cursed earth blocks.
     * Applies damage and debuff effects to punish the player for staying on cursed ground.
     */
    private void handleAnkhDepletionOnCursedBlock(PlayerEntity player, ServerWorld world, BlockPos pos) {
        // Apply magic damage when Ankh is depleted on cursed blocks
        player.damage(world.getDamageSources().magic(), 2.0f);

        // Apply wither effect to show the curse is harming the player
        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
            net.minecraft.entity.effect.StatusEffects.WITHER,
            120, // 6 seconds (longer than other cursed blocks)
            0,
            false,
            true,
            true
        ));

        // Apply slowness to make it harder to escape
        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
            net.minecraft.entity.effect.StatusEffects.SLOWNESS,
            200, // 10 seconds
            1,
            false,
            true,
            true
        ));

        // Apply weakness
        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
            net.minecraft.entity.effect.StatusEffects.WEAKNESS,
            200, // 10 seconds
            0,
            false,
            true,
            true
        ));

        // Spawn ominous particles around the player (more particles than other cursed blocks)
        for (int i = 0; i < 20; i++) {
            double offsetX = (world.random.nextDouble() - 0.5) * 2.0;
            double offsetY = world.random.nextDouble() * 2.5;
            double offsetZ = (world.random.nextDouble() - 0.5) * 2.0;

            world.addParticle(
                new net.minecraft.particle.DustParticleEffect(CURSED_PARTICLE_COLOR, 1.5f),
                player.getX() + offsetX,
                player.getY() + offsetY,
                player.getZ() + offsetZ,
                0, 0.08, 0
            );
        }

        // Play warning sound (deeper pitch for earth)
        world.playSound(
            null,
            player.getX(), player.getY(), player.getZ(),
            net.minecraft.sound.SoundEvents.ENTITY_WARDEN_AMBIENT,
            player.getSoundCategory(),
            0.6f,
            0.8f
        );

        // Send warning message
        player.sendMessage(
            net.minecraft.text.Text.literal("The cursed earth consumes your soul!")
                .formatted(net.minecraft.util.Formatting.DARK_GREEN, net.minecraft.util.Formatting.BOLD),
            true // Action bar
        );
    }

    /**
     * Decays nearby leaf blocks with gradual withering effect
     */
    private void decayNearbyLeaves(ServerWorld world, BlockPos pos, Random random) {
        // Skip if already at max withering leaves
        if (leafWitherStartTimes.size() >= MAX_WITHERING_LEAVES) {
            return;
        }
        
        // Smaller search radius for more controlled decay
        int radius = 2;
        int verticalRadius = 2; // Reduced vertical range
        long currentTime = world.getTime();
        
        // Process withering leaves less frequently to reduce lag
        if (currentTime % 10 == 0) { // Only process every 10 ticks
            leafWitherStartTimes.entrySet().removeIf(entry -> {
                BlockPos leafPos = entry.getKey();
                long startTime = entry.getValue();
                
                // Quick distance check - don't process far leaves
                double distance = Math.sqrt(leafPos.getSquaredDistance(pos));
                if (distance > 10) {
                    return true; // Too far, remove from tracking
                }
                
                // Check if leaf still exists
                if (!isLeafBlock(world.getBlockState(leafPos))) {
                    return true; // Remove from tracking
                }
                
                // Calculate wither progress (0.0 to 1.0)
                float progress = (float)(currentTime - startTime) / LEAF_WITHER_TIME;
                
                if (progress >= 1.0f) {
                    // Time to break the leaf silently
                    breakLeafSilently(world, leafPos);
                    return true; // Remove from tracking
                } else if (currentTime % 20 == 0) { // Particles only every second
                    // Spawn subtle withering particles based on progress
                    if (random.nextFloat() < progress * 0.3f) {
                        spawnWitherParticles(world, leafPos, progress, random);
                    }
                }
                return false; // Keep tracking
            });
        }
        
        // Don't add new withering if we're at the limit
        if (leafWitherStartTimes.size() >= MAX_WITHERING_LEAVES) {
            return;
        }
        
        // Find the closest non-withering leaf to start withering
        BlockPos closestLeaf = null;
        double closestDistance = Double.MAX_VALUE;
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -1; y <= verticalRadius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos leafPos = pos.add(x, y, z);
                    
                    // Skip if already withering
                    if (leafWitherStartTimes.containsKey(leafPos)) {
                        continue;
                    }
                    
                    BlockState leafState = world.getBlockState(leafPos);
                    if (isLeafBlock(leafState)) {
                        double distance = Math.sqrt(x*x + y*y + z*z);
                        
                        // Prefer leaves that are:
                        // 1. Closer to cursed earth
                        // 2. Have withering neighbors (creates spreading effect)
                        double effectiveDistance = distance;
                        
                        // Check for withering neighbors
                        int witheringNeighbors = 0;
                        for (Direction dir : Direction.values()) {
                            if (leafWitherStartTimes.containsKey(leafPos.offset(dir))) {
                                witheringNeighbors++;
                            }
                        }
                        
                        // Reduce effective distance if has withering neighbors
                        effectiveDistance -= witheringNeighbors * 0.5;
                        
                        if (effectiveDistance < closestDistance) {
                            closestDistance = effectiveDistance;
                            closestLeaf = leafPos;
                        }
                    }
                }
            }
        }
        
        // Start withering the closest leaf
        if (closestLeaf != null && closestDistance <= radius) {
            leafWitherStartTimes.put(closestLeaf, currentTime);
            
            // Spawn initial corruption particles
            spawnInitialCorruptionParticles(world, closestLeaf, random);
        }
    }
    
    /**
     * Breaks a leaf block silently without sound or drops
     */
    private void breakLeafSilently(ServerWorld world, BlockPos pos) {
        // Set to air without playing break sound
        world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2); // Flag 2 = no sound
        
        // Spawn minimal ash particles (reduced from 5 to 2)
        for (int i = 0; i < 2; i++) {
            double x = pos.getX() + 0.5 + (world.random.nextDouble() - 0.5) * 0.3;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5 + (world.random.nextDouble() - 0.5) * 0.3;
            
            world.spawnParticles(
                ParticleTypes.ASH,
                x, y, z,
                1,
                0.02, -0.01, 0.02,
                0.005
            );
        }
    }
    
    /**
     * Spawn subtle particles showing the withering progress
     */
    private void spawnWitherParticles(ServerWorld world, BlockPos pos, float progress, Random random) {
        // Single particle only
        if (random.nextFloat() < 0.5f) { // 50% chance for any particle at all
            // Dark particles that get darker as withering progresses
            float darkness = 0.25f - (progress * 0.2f);
            Vector3f color = new Vector3f(darkness, darkness * 0.8f, darkness);
            
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.3;
            double z = pos.getZ() + 0.5;
            
            world.spawnParticles(
                new DustParticleEffect(color, 0.4f),
                x, y, z,
                1,
                0, -0.005, 0,
                0
            );
        }
    }
    
    /**
     * Spawn initial particles when a leaf starts withering
     */
    private void spawnInitialCorruptionParticles(ServerWorld world, BlockPos pos, Random random) {
        // Single subtle particle
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        
        world.spawnParticles(
            new DustParticleEffect(new Vector3f(0.15f, 0.1f, 0.2f), 0.3f),
            x, y, z,
            1,
            0.01, 0.01, 0.01,
            0
        );
    }
    
    /**
     * Replace nearby grass with Reed of Sekhem
     */
    private void replaceNearbyGrass(ServerWorld world, BlockPos pos, Random random) {
        // Small radius to keep it controlled
        int radius = 3;
        
        // Look for grass in the area
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = 0; y <= 2; y++) {
                    BlockPos grassPos = pos.add(x, y, z);
                    BlockState state = world.getBlockState(grassPos);
                    
                    // Check if it's grass or tall grass
                    if (isGrassPlant(state)) {
                        // Distance-based chance
                        double distance = Math.sqrt(x*x + y*y + z*z);
                        float replaceChance = 0.3f * (1.0f - (float)(distance / (radius + 1)));
                        
                        if (random.nextFloat() < replaceChance) {
                            // Store original grass type for restoration
                            CursedEarthManager.getInstance().storeOriginalBlock(world, grassPos, state);
                            
                            // Replace with Reed of Sekhem
                            BlockState reedState = CursedPlantBlocks.REED_OF_SEKHEM.getDefaultState();
                            if (reedState.canPlaceAt(world, grassPos)) {
                                world.setBlockState(grassPos, reedState);
                                
                                // Spawn corruption particles
                                for (int i = 0; i < 3; i++) {
                                    double px = grassPos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.3;
                                    double py = grassPos.getY() + 0.5;
                                    double pz = grassPos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.3;
                                    
                                    world.spawnParticles(
                                        new DustParticleEffect(CURSED_PARTICLE_COLOR, 0.5f),
                                        px, py, pz,
                                        1,
                                        0, 0.05, 0,
                                        0
                                    );
                                }
                                
                                return; // Only replace one grass per tick
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Check if a block is a grass plant (not grass block)
     */
    private boolean isGrassPlant(BlockState state) {
        Block block = state.getBlock();
        
        // Check for vanilla grass plants
        if (block == Blocks.GRASS || // Short grass in 1.20.1
            block == Blocks.TALL_GRASS ||
            block == Blocks.FERN ||
            block == Blocks.LARGE_FERN) {
            return true;
        }
        
        // Check for modded grass by name
        String blockName = block.getTranslationKey().toLowerCase();
        if ((blockName.contains("grass") && !blockName.contains("block")) || // Grass but not grass block
            blockName.contains("fern") ||
            blockName.contains("tallgrass")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Attempts to spawn a cursed plant on top of cursed earth
     */
    private void attemptPlantSpawn(ServerWorld world, BlockPos pos, Random random) {
        // Check if position is air and can support a plant
        if (!world.getBlockState(pos).isAir()) {
            return;
        }
        
        // Check if there's already a plant nearby (prevent overcrowding)
        Box searchBox = new Box(pos).expand(2, 1, 2);
        for (int x = (int)searchBox.minX; x <= (int)searchBox.maxX; x++) {
            for (int z = (int)searchBox.minZ; z <= (int)searchBox.maxZ; z++) {
                for (int y = (int)searchBox.minY; y <= (int)searchBox.maxY; y++) {
                    BlockPos checkPos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(checkPos);
                    if (state.getBlock() instanceof CursedPlantBlock) {
                        return; // Too crowded
                    }
                }
            }
        }
        
        // Select a random cursed plant - EXCLUDE Reed of Sekhem (it replaces grass instead)
        Block[] cursedPlants = {
            CursedPlantBlocks.CURSED_SPRIG,
            CursedPlantBlocks.CURSED_SPROUT,
            CursedPlantBlocks.BLOODSHADE_THICKET,
            CursedPlantBlocks.DUAT_FERN,
            CursedPlantBlocks.VINE_OF_APEP,
            CursedPlantBlocks.DUAMUTEF_CAP,
            CursedPlantBlocks.ISFET_FROND,
            CursedPlantBlocks.ISFET_SHRUB,
            CursedPlantBlocks.KHEMNU_POD,
            CursedPlantBlocks.KHERU_MOSS,
            CursedPlantBlocks.MENFET_SPRIG,
            // CursedPlantBlocks.REED_OF_SEKHEM, // Removed - only spawns by replacing grass
            CursedPlantBlocks.SUTEKH_COIL
        };
        
        // Weight towards common plants
        Block selectedPlant;
        if (random.nextFloat() < 0.7f) {
            // 70% chance for common plants
            selectedPlant = random.nextBoolean() ? CursedPlantBlocks.CURSED_SPRIG : CursedPlantBlocks.CURSED_SPROUT;
        } else {
            // 30% chance for rarer plants
            selectedPlant = cursedPlants[random.nextInt(cursedPlants.length)];
        }
        
        // Try to place the plant
        BlockState plantState = selectedPlant.getDefaultState();
        if (plantState.canPlaceAt(world, pos)) {
            world.setBlockState(pos, plantState);
            
            // Spawn particles for visual effect
            for (int i = 0; i < 5; i++) {
                double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.3;
                double y = pos.getY() + 0.5;
                double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.3;
                world.addParticle(
                    new DustParticleEffect(CURSED_PARTICLE_COLOR, 0.5f),
                    x, y, z,
                    0, 0.05D, 0
                );
            }
            
            // Debug: Spawned plant
        }
    }
    
    /**
     * Attempts to spawn a rare cursed entity on top of cursed earth
     */
    private void attemptEntitySpawn(ServerWorld world, BlockPos pos, Random random) {
        // Check if position is suitable for entity spawn
        if (!world.getBlockState(pos).isAir() || !world.getBlockState(pos.up()).isAir()) {
            return;
        }
        
        // Check for nearby entities to prevent overcrowding
        Box searchBox = new Box(pos).expand(5, 3, 5);
        List<LivingEntity> nearbyEntities = world.getNonSpectatingEntities(LivingEntity.class, searchBox);
        if (nearbyEntities.size() > 2) {
            return; // Too crowded
        }
        
        // Select which entity to spawn
        // 90% chance for Djeserhath, 10% for Khamsin Spread Small (reduced by 80%)
        boolean spawnDjeserhath = random.nextFloat() < 0.9f;
        
        if (spawnDjeserhath) {
            // Spawn Djeserhath (cactus eye entity)
            com.ancientcurse.entity.DjeserhathEntity djeserhath = ModEntities.DJESERHATH.create(world);
            if (djeserhath != null) {
                djeserhath.setPosition(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5);
                
                // Make it start in dormant state
                djeserhath.setHealth(djeserhath.getMaxHealth());
                
                if (world.spawnEntity(djeserhath)) {
                    // Spawn emergence particles
                    for (int i = 0; i < 10; i++) {
                        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.5;
                        double y = pos.getY() + 0.5;
                        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.5;
                        world.addParticle(
                            new DustParticleEffect(new Vector3f(0.5f, 0.1f, 0.5f), 1.0f),
                            x, y, z,
                            0, 0.1D, 0
                        );
                    }
                    
                    // Rare spawn: Djeserhath emerged from cursed earth
                }
            }
        } else {
            // Check for nearby Khamsin Spread Small entities (40 block radius)
            Box khamsinSearchBox = new Box(pos).expand(40, 20, 40);
            List<com.ancientcurse.entity.KhamsinSpreadSmallEntity> nearbyKhamsins = 
                world.getNonSpectatingEntities(com.ancientcurse.entity.KhamsinSpreadSmallEntity.class, khamsinSearchBox);
            
            // Don't spawn if there's already one within 40 blocks
            if (!nearbyKhamsins.isEmpty()) {
                return; // Too close to existing Khamsin Spread Small
            }
            
            // Spawn Khamsin Spread Small (floating mystical rock)
            com.ancientcurse.entity.KhamsinSpreadSmallEntity khamsin = ModEntities.KHAMSIN_SPREAD_SMALL.create(world);
            if (khamsin != null) {
                khamsin.setPosition(pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5); // Float above ground
                
                if (world.spawnEntity(khamsin)) {
                    // Spawn dark mystical particles for the obsidian entity
                    for (int i = 0; i < 15; i++) {
                        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.8;
                        double y = pos.getY() + 1.5 + (random.nextDouble() - 0.5) * 0.8;
                        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.8;
                        world.addParticle(
                            new DustParticleEffect(new Vector3f(0.4f, 0.1f, 0.5f), 0.8f), // Dark purple for obsidian
                            x, y, z,
                            0, 0.02D, 0
                        );
                    }
                    
                    // Rare spawn: Khamsin Spread Small manifested from cursed earth
                }
            }
        }
    }
    
    /**
     * Creates a death burst when a player dies
     * Creates an organic, sweeping corruption pattern that branches outward
     */
    public static void createDeathBurst(ServerWorld world, BlockPos deathPos) {
        // Check cooldown
        long currentTime = world.getTime();
        if (currentTime - deathBurstCooldowns.getOrDefault(deathPos, 0L) < DEATH_BURST_COOLDOWN) {
            return;
        }
        
        // Update cooldown
        deathBurstCooldowns.put(deathPos, currentTime);
        
        // Find the ground level
        BlockPos groundPos = deathPos;
        while (world.getBlockState(groundPos).isAir() && groundPos.getY() > world.getBottomY()) {
            groundPos = groundPos.down();
        }
        
        // Create multiple tendrils that branch out from death point
        int numTendrils = 5 + world.random.nextInt(3); // 5-7 tendrils
        int totalConverted = 0;
        
        for (int i = 0; i < numTendrils; i++) {
            // Random initial direction for each tendril
            double angle = (Math.PI * 2 * i / numTendrils) + (world.random.nextDouble() - 0.5) * 0.5;
            Direction primaryDir = getDirectionFromAngle(angle);
            
            // Create a tendril
            totalConverted += createTendril(world, groundPos, primaryDir, DEATH_BURST_SIZE, world.random);
        }
        
        // Create an initial burst at center
        totalConverted += createCenterBurst(world, groundPos, 3, world.random);
        
        // Created death burst with totalConverted blocks
    }
    
    /**
     * Creates a single tendril of corruption
     */
    private static int createTendril(ServerWorld world, BlockPos start, Direction primaryDir, int maxLength, Random random) {
        int converted = 0;
        BlockPos current = start;
        Direction currentDir = primaryDir;
        
        for (int length = 0; length < maxLength; length++) {
            // Smart surface finding that handles cliffs and overhangs
            BlockPos surfacePos = findSurfaceForTendril(world, current);
            if (surfacePos == null) {
                // If no surface found, try to continue in the direction
                current = current.offset(currentDir);
                continue;
            }
            
            // Try to convert current position
            if (canSpreadToStatic(world, surfacePos)) {
                // Determine which cursed block type based on target
                BlockState targetBlockState = world.getBlockState(surfacePos);
                BlockState newState;
                
                if (isStoneTypeStatic(targetBlockState.getBlock())) {
                    newState = ModBlocks.CURSED_STONE.getDefaultState();
                } else if (isLeafBlockStatic(targetBlockState)) {
                    // Skip leaves - they decay naturally
                    continue;
                } else if (CursedLogBlock.isLogBlock(targetBlockState)) {
                    newState = ModBlocks.CURSED_LOG.getDefaultState();
                    if (targetBlockState.contains(net.minecraft.block.PillarBlock.AXIS)) {
                        newState = newState.with(net.minecraft.block.PillarBlock.AXIS, 
                                                targetBlockState.get(net.minecraft.block.PillarBlock.AXIS));
                    }
                } else if (CursedLogBlock.isPlankBlock(targetBlockState)) {
                    newState = ModBlocks.CURSED_WOOD_PLANK.getDefaultState();
                } else if (CursedSandBlock.isSandBlock(targetBlockState)) {
                    newState = ModBlocks.CURSED_SAND.getDefaultState();
                } else {
                    newState = ModBlocks.CURSED_EARTH.getDefaultState();
                }
                
                CursedEarthManager.getInstance().queueSpread(
                    world, start, surfacePos, newState);
                converted++;
                
                // Set spread direction for continued growth
                spreadDirections.put(surfacePos, currentDir);
                
                // Sometimes widen the tendril
                if (random.nextFloat() < 0.4f) {
                    // Convert adjacent blocks
                    for (Direction side : Direction.Type.HORIZONTAL) {
                        if (side == currentDir || side == currentDir.getOpposite()) continue;
                        
                        BlockPos sidePos = surfacePos.offset(side);
                        BlockPos sideSurface = findSurfaceForTendril(world, sidePos);
                        
                        if (sideSurface != null && canSpreadToStatic(world, sideSurface) && random.nextFloat() < 0.6f) {
                            BlockState sideBlockState = world.getBlockState(sideSurface);
                            BlockState sideNewState;
                            
                            if (isStoneTypeStatic(sideBlockState.getBlock())) {
                                sideNewState = ModBlocks.CURSED_STONE.getDefaultState();
                            } else if (isLeafBlockStatic(sideBlockState)) {
                                // Skip leaves - they decay naturally
                                continue;
                            } else if (CursedLogBlock.isLogBlock(sideBlockState)) {
                                sideNewState = ModBlocks.CURSED_LOG.getDefaultState();
                                if (sideBlockState.contains(net.minecraft.block.PillarBlock.AXIS)) {
                                    sideNewState = sideNewState.with(net.minecraft.block.PillarBlock.AXIS, 
                                                                    sideBlockState.get(net.minecraft.block.PillarBlock.AXIS));
                                }
                            } else if (CursedLogBlock.isPlankBlock(sideBlockState)) {
                                sideNewState = ModBlocks.CURSED_WOOD_PLANK.getDefaultState();
                            } else if (CursedSandBlock.isSandBlock(sideBlockState)) {
                                sideNewState = ModBlocks.CURSED_SAND.getDefaultState();
                            } else {
                                sideNewState = ModBlocks.CURSED_EARTH.getDefaultState();
                            }
                            
                            CursedEarthManager.getInstance().queueSpread(
                                world, surfacePos, sideSurface, sideNewState);
                            converted++;
                        }
                    }
                }
                
                // Sometimes branch off
                if (length > 3 && random.nextFloat() < 0.3f) {
                    Direction branchDir = random.nextBoolean() ? currentDir.rotateYClockwise() : currentDir.rotateYCounterclockwise();
                    converted += createTendril(world, current, branchDir, maxLength - length - 2, random);
                }
            }
            
            // Move to next position
            current = current.offset(currentDir);
            
            // Sometimes change direction slightly
            if (random.nextFloat() < 0.3f) {
                currentDir = random.nextBoolean() ? currentDir.rotateYClockwise() : currentDir.rotateYCounterclockwise();
            }
            
            // Sometimes move diagonally
            if (random.nextFloat() < 0.2f) {
                Direction secondDir = random.nextBoolean() ? currentDir.rotateYClockwise() : currentDir.rotateYCounterclockwise();
                current = current.offset(secondDir);
            }
        }
        
        return converted;
    }
    
    /**
     * Creates the initial burst at death location
     */
    private static int createCenterBurst(ServerWorld world, BlockPos center, int radius, Random random) {
        int converted = 0;
        
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double distance = Math.sqrt(x*x + z*z);
                if (distance > radius) continue;
                
                // Higher chance near center
                if (random.nextFloat() < (1.0 - distance / radius)) {
                    BlockPos checkPos = center.add(x, 0, z);
                    BlockPos surfacePos = world.getTopPosition(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, checkPos);
                    
                    if (canSpreadToStatic(world, surfacePos)) {
                        BlockState centerBlockState = world.getBlockState(surfacePos);
                        BlockState centerNewState;
                        
                        if (isStoneTypeStatic(centerBlockState.getBlock())) {
                            centerNewState = ModBlocks.CURSED_STONE.getDefaultState();
                        } else if (isLeafBlockStatic(centerBlockState)) {
                            // Skip leaves - they decay naturally
                            continue;
                        } else if (CursedLogBlock.isLogBlock(centerBlockState)) {
                            centerNewState = ModBlocks.CURSED_LOG.getDefaultState();
                            if (centerBlockState.contains(net.minecraft.block.PillarBlock.AXIS)) {
                                centerNewState = centerNewState.with(net.minecraft.block.PillarBlock.AXIS, 
                                                                    centerBlockState.get(net.minecraft.block.PillarBlock.AXIS));
                            }
                        } else if (CursedLogBlock.isPlankBlock(centerBlockState)) {
                            centerNewState = ModBlocks.CURSED_WOOD_PLANK.getDefaultState();
                        } else if (CursedSandBlock.isSandBlock(centerBlockState)) {
                            centerNewState = ModBlocks.CURSED_SAND.getDefaultState();
                        } else {
                            centerNewState = ModBlocks.CURSED_EARTH.getDefaultState();
                        }
                        
                        CursedEarthManager.getInstance().queueSpread(
                            world, center, surfacePos, centerNewState);
                        converted++;
                    }
                }
            }
        }
        
        return converted;
    }
    
    /**
     * Convert angle to closest Direction
     */
    private static Direction getDirectionFromAngle(double angle) {
        // Normalize angle to 0-2π
        while (angle < 0) angle += Math.PI * 2;
        while (angle > Math.PI * 2) angle -= Math.PI * 2;
        
        // Convert to 8 directions
        int octant = (int)((angle + Math.PI / 8) / (Math.PI / 4)) % 8;
        
        return switch (octant) {
            case 0, 7 -> Direction.EAST;
            case 1, 2 -> Direction.SOUTH;
            case 3, 4 -> Direction.WEST;
            case 5, 6 -> Direction.NORTH;
            default -> Direction.NORTH;
        };
    }
    
    /**
     * Clean up stale cooldowns and counts - PERFORMANCE CRITICAL
     * Called frequently to prevent memory buildup
     */
    public static void cleanupStaleData(ServerWorld world) {
        long currentTime = world.getTime();

        // Run cleanup more frequently (every 60 seconds instead of 10 minutes)
        if (currentTime % CLEANUP_INTERVAL == 0) {
            int beforeCooldowns = spreadCooldowns.size();
            int beforeDirections = spreadDirections.size();

            // Clean up spread cooldowns
            spreadCooldowns.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > SPREAD_COOLDOWN * 2);

            // Clean up death burst cooldowns
            deathBurstCooldowns.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > DEATH_BURST_COOLDOWN * 2);

            // Clean up withering leaves that are too old or no longer exist
            leafWitherStartTimes.entrySet().removeIf(entry -> {
                long startTime = entry.getValue();
                return currentTime - startTime > LEAF_WITHER_TIME * 2;
            });

            // Clean up spread tracking data (these never got cleaned before!)
            // Only keep entries for positions that are still cursed earth
            spreadDirections.entrySet().removeIf(entry -> {
                BlockPos pos = entry.getKey();
                return world.getBlockState(pos).getBlock() != ModBlocks.CURSED_EARTH;
            });

            branchLengths.entrySet().removeIf(entry -> {
                BlockPos pos = entry.getKey();
                return world.getBlockState(pos).getBlock() != ModBlocks.CURSED_EARTH;
            });

            spreadOrigins.entrySet().removeIf(entry -> {
                BlockPos pos = entry.getKey();
                return world.getBlockState(pos).getBlock() != ModBlocks.CURSED_EARTH;
            });

            // Clear spread cache periodically to handle block changes
            spreadableBlockCache.clear();

            // Log cleanup stats
            int afterCooldowns = spreadCooldowns.size();
            int afterDirections = spreadDirections.size();
            if (beforeCooldowns - afterCooldowns > 100 || beforeDirections - afterDirections > 100) {
                AncientCurse.LOGGER.debug("Cursed earth cleanup: cooldowns {} -> {}, directions {} -> {}",
                    beforeCooldowns, afterCooldowns, beforeDirections, afterDirections);
            }
        }

        // Force trim maps if they get too large (check every 100 ticks for responsiveness)
        if (currentTime % 100 == 0) {
            trimTrackingMapsIfNeeded();
        }
    }

    /**
     * Trim tracking maps if they exceed size limits - prevents memory bloat
     */
    private static void trimTrackingMapsIfNeeded() {
        // Trim cooldowns map
        if (spreadCooldowns.size() > MAX_TRACKED_COOLDOWNS) {
            // Remove oldest 25% of entries
            int toRemove = spreadCooldowns.size() / 4;
            List<Map.Entry<BlockPos, Long>> sorted = new ArrayList<>(spreadCooldowns.entrySet());
            sorted.sort(Map.Entry.comparingByValue());
            for (int i = 0; i < toRemove && i < sorted.size(); i++) {
                spreadCooldowns.remove(sorted.get(i).getKey());
            }
            AncientCurse.LOGGER.info("Trimmed spreadCooldowns map from {} to {} entries",
                spreadCooldowns.size() + toRemove, spreadCooldowns.size());
        }

        // Trim direction tracking maps
        if (spreadDirections.size() > MAX_TRACKED_DIRECTIONS) {
            // Just clear the oldest entries - spread directions are less critical
            int toRemove = spreadDirections.size() / 4;
            int removed = 0;
            Iterator<BlockPos> iter = spreadDirections.keySet().iterator();
            while (iter.hasNext() && removed < toRemove) {
                BlockPos pos = iter.next();
                iter.remove();
                branchLengths.remove(pos);
                spreadOrigins.remove(pos);
                removed++;
            }
            AncientCurse.LOGGER.info("Trimmed spread direction maps, removed {} entries", removed);
        }

        // Trim death burst cooldowns
        if (deathBurstCooldowns.size() > 500) {
            deathBurstCooldowns.clear();
            AncientCurse.LOGGER.debug("Cleared death burst cooldowns map");
        }

        // Trim player exposure map
        if (playerExposureTime.size() > 100) {
            playerExposureTime.clear();
        }
    }
    
    /**
     * Clears the spreadable block cache
     * Should be called when blocks are added/removed by mods
     */
    public static void clearSpreadableCache() {
        spreadableBlockCache.clear();
    }
    
    /**
     * Gets the total number of cursed blocks across all chunks
     */
    public static int getTotalCursedBlocks() {
        return chunkCounts.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    /**
     * Clears the curse count for a specific chunk
     */
    public static void clearChunkCurseCount(ChunkPos chunkPos) {
        chunkCounts.remove(chunkPos);
    }
    
    /**
     * Decrements the curse count for a chunk when a block is cleansed
     */
    public static void decrementChunkCurseCount(BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        int currentCount = chunkCounts.getOrDefault(chunkPos, 0);
        if (currentCount > 0) {
            chunkCounts.put(chunkPos, currentCount - 1);
        }
    }
}
