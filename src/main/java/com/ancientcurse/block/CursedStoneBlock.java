package com.ancientcurse.block;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.ModBlocks;
import com.ancientcurse.command.CursedEarthCommand;
import com.ancientcurse.system.CursedEarthManager;
import com.ancientcurse.block.SolarSpireBlock;
import com.ancientcurse.player.AnkhDataManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.BlockView;
import net.minecraft.particle.DustParticleEffect;
import org.joml.Vector3f;
import net.minecraft.registry.Registries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cursed Stone - Stone variant of cursed earth with light-based animated texture
 * Spreads to stone-type blocks and creates cave/underground corruption patterns
 */
public class CursedStoneBlock extends BaseAncientCurseBlock {
    // Constants for spread behavior
    private static final int MAX_BLOCKS_PER_CHUNK = 256;
    private static final int SPREAD_COOLDOWN = 120; // Slightly slower than cursed earth
    private static final float UNDERGROUND_SPREAD_CHANCE = 0.75f; // Favors underground spreading
    private static final float VEIN_SPREAD_BONUS = 0.3f; // Bonus for following ore-vein patterns
    private static final Vector3f CURSED_PARTICLE_COLOR = new Vector3f(0.3f, 0.1f, 0.4f); // Darker purple
    
    // Tracking maps
    private static final Map<ChunkPos, Integer> chunkCounts = new HashMap<>();
    private static final Map<BlockPos, Long> spreadCooldowns = new HashMap<>();
    private static final Map<Block, Boolean> spreadableStoneCache = new HashMap<>();
    
    public CursedStoneBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, BlockView world, List<Text> tooltip, TooltipContext options) {
        // Don't add the base Ancient Curse tooltip - Minecraft already shows the mod name
    }
    
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        // Only handle removal if the block is actually being replaced with a different type
        if (!state.isOf(newState.getBlock()) && !world.isClient) {
            // Notify CursedEarthManager that a cursed block was removed
            CursedEarthManager.getInstance().onCursedBlockRemoved((ServerWorld) world, pos);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
    
    /**
     * Get the texture variant based on light level (0-3)
     * The texture is 16x64 with 4 variants from top to bottom
     */
    public static int getTextureVariant(BlockView world, BlockPos pos) {
        if (world == null || pos == null) {
            return 0; // Darkest variant for null cases
        }
        
        // For now, use random variant since getting actual light level from BlockView is complex
        // The variants will still show the animation effect
        return (pos.getX() + pos.getY() + pos.getZ()) % 4;
    }
    
    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // Check if we're in an active cleansing zone
        if (SolarSpireBlock.isInActiveCleansingZone(pos)) {
            return;
        }
        
        // Attempt to spread the curse to nearby stone blocks
        if (CursedEarthCommand.isEnabled() && random.nextFloat() < 0.25f) {
            attemptSpread(world, pos, random);
        }
        
        // Apply effects to nearby players (weaker than cursed earth)
        if (random.nextFloat() < 0.5f) {
            applyEffectsToNearbyPlayers(world, pos);
        }
    }
    
    /**
     * Attempts to spread the curse to nearby stone blocks
     * Uses vein-like patterns for more natural underground spread
     */
    private void attemptSpread(ServerWorld world, BlockPos pos, Random random) {
        // Check cooldown
        long currentTime = world.getTime();
        if (currentTime - spreadCooldowns.getOrDefault(pos, 0L) < SPREAD_COOLDOWN) {
            return;
        }
        
        // Update cooldown
        spreadCooldowns.put(pos, currentTime);
        
        // Check chunk limit
        ChunkPos chunkPos = new ChunkPos(pos);
        int currentCount = chunkCounts.getOrDefault(chunkPos, 0);
        if (currentCount >= MAX_BLOCKS_PER_CHUNK) {
            return;
        }
        
        // Create list of potential spread targets
        List<SpreadCandidate> candidates = new ArrayList<>();
        
        // Check all 6 directions for stone blocks
        for (Direction dir : Direction.values()) {
            BlockPos targetPos = pos.offset(dir);
            
            if (canSpreadToStone(world, targetPos)) {
                float weight = calculateSpreadWeight(world, pos, targetPos, dir, random);
                candidates.add(new SpreadCandidate(targetPos, dir, weight));
            }
            
            // Also check 2 blocks away for vein-like spreading
            if (random.nextFloat() < 0.3f) {
                BlockPos farPos = pos.offset(dir, 2);
                if (canSpreadToStone(world, farPos)) {
                    float weight = calculateSpreadWeight(world, pos, farPos, dir, random) * 0.7f;
                    candidates.add(new SpreadCandidate(farPos, dir, weight));
                }
            }
        }
        
        // Sort candidates by weight
        candidates.sort((a, b) -> Float.compare(b.weight, a.weight));
        
        // Spread to best candidate(s)
        int spreadsThisTick = 0;
        int maxSpreads = random.nextFloat() < 0.2f ? 2 : 1; // Sometimes spread to 2 blocks
        
        for (int i = 0; i < candidates.size() && spreadsThisTick < maxSpreads; i++) {
            SpreadCandidate candidate = candidates.get(i);
            
            if (random.nextFloat() < candidate.weight) {
                // Check for cleansing protection
                if (SolarSpireBlock.isInActiveCleansingZone(candidate.pos)) {
                    continue;
                }
                
                // Queue the spread
                CursedEarthManager.getInstance().queueSpread(
                    world, pos, candidate.pos, 
                    ModBlocks.CURSED_STONE.getDefaultState()
                );
                
                // Update tracking
                chunkCounts.put(chunkPos, currentCount + 1);
                
                // Create particle effect
                createSpreadParticles(world, pos, candidate.pos, random);
                
                spreadsThisTick++;
            }
        }
    }
    
    /**
     * Calculate weight for spreading to a target position
     */
    private float calculateSpreadWeight(ServerWorld world, BlockPos from, BlockPos to, 
                                       Direction dir, Random random) {
        float weight = 0.5f + random.nextFloat() * 0.2f;
        
        // Underground bonus - prefer spreading in caves and underground
        if (!world.isSkyVisible(to)) {
            weight += UNDERGROUND_SPREAD_CHANCE * 0.3f;
        }
        
        // Vein pattern bonus - check for nearby stone blocks
        int nearbyStone = 0;
        for (Direction checkDir : Direction.values()) {
            BlockPos checkPos = to.offset(checkDir);
            if (isStoneType(world.getBlockState(checkPos).getBlock())) {
                nearbyStone++;
            }
        }
        weight += nearbyStone * 0.05f; // Bonus for being surrounded by stone
        
        // Prefer horizontal spreading underground
        if (dir.getAxis() != Direction.Axis.Y) {
            weight += 0.1f;
        }
        
        // Add vein spreading pattern
        if (nearbyStone >= 4) {
            weight += VEIN_SPREAD_BONUS;
        }
        
        return Math.max(0.1f, Math.min(1.0f, weight));
    }
    
    /**
     * Checks if the curse can spread to the target position
     */
    private boolean canSpreadToStone(ServerWorld world, BlockPos pos) {
        // Check for cleansing protection
        if (CursedEarthManager.getInstance().isProtectedByCleansingStation(world, pos)) {
            return false;
        }
        
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        
        // Don't spread to air or liquids
        if (state.isAir() || state.isLiquid()) {
            return false;
        }
        
        // Don't spread to already cursed blocks
        if (block == ModBlocks.CURSED_STONE || block == ModBlocks.CURSED_EARTH) {
            return false;
        }
        
        // Check cache
        Boolean cached = spreadableStoneCache.get(block);
        if (cached != null) {
            return cached;
        }
        
        // Check if it's a stone-type block
        boolean canSpread = isStoneType(block);
        
        // Cache the result
        spreadableStoneCache.put(block, canSpread);
        
        return canSpread;
    }
    
    /**
     * Determines if a block is a stone type that can be corrupted
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
        String namespace = Registries.BLOCK.getId(block).getNamespace();
        
        // Common stone-related keywords
        String[] stoneKeywords = {
            "stone", "cobble", "brick", "deepslate", "granite", "diorite", 
            "andesite", "basalt", "limestone", "marble", "slate"
        };
        
        // Don't corrupt functional stone blocks
        String[] exclusions = {
            "furnace", "chest", "spawner", "infested", "silverfish",
            "command", "barrier", "bedrock", "ore"
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
        
        // For modded blocks, also check hardness similarity to stone
        if (!namespace.equals("minecraft")) {
            float hardness = block.getHardness();
            // Stone-like hardness range (stone is 1.5, cobblestone is 2.0)
            if (hardness >= 1.0f && hardness <= 3.0f) {
                // Additional checks for modded blocks
                if (blockName.contains("rock") || blockName.contains("mineral")) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Create particle effects showing spread
     */
    private void createSpreadParticles(ServerWorld world, BlockPos from, BlockPos to, Random random) {
        double steps = 3;
        for (int i = 0; i <= steps; i++) {
            double progress = i / steps;
            double x = from.getX() + 0.5 + (to.getX() - from.getX()) * progress;
            double y = from.getY() + 0.5 + (to.getY() - from.getY()) * progress;
            double z = from.getZ() + 0.5 + (to.getZ() - from.getZ()) * progress;
            
            world.addParticle(
                new DustParticleEffect(CURSED_PARTICLE_COLOR, 0.5f),
                x, y, z,
                0, 0, 0
            );
        }
    }
    
    /**
     * Apply effects to nearby players
     */
    private void applyEffectsToNearbyPlayers(ServerWorld world, BlockPos pos) {
        Box effectBox = new Box(pos).expand(1, 1, 1);
        List<PlayerEntity> nearbyPlayers = world.getNonSpectatingEntities(PlayerEntity.class, effectBox);
        
        for (PlayerEntity player : nearbyPlayers) {
            if (player.isCreative() || player.isSpectator()) {
                continue;
            }
            
            // Decrease ankh value when near cursed stone (less than cursed earth)
            if (world.getTime() % 200 == 0) { // Every 10 seconds
                AnkhDataManager.decreaseAnkhValue(player, 1);
            }
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
     * Clean up stale data
     */
    public static void cleanupStaleData(ServerWorld world) {
        if (world.getTime() % 12000 == 0) {
            long currentTime = world.getTime();
            spreadCooldowns.entrySet().removeIf(entry -> 
                currentTime - entry.getValue() > SPREAD_COOLDOWN * 2);
            spreadableStoneCache.clear();
        }
    }
    
    /**
     * Clears the cache
     */
    public static void clearSpreadableCache() {
        spreadableStoneCache.clear();
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