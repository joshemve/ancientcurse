package com.ancientcurse.block;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.ModBlocks;
import com.ancientcurse.ModEntities;
import com.ancientcurse.system.CursedEarthManager;
import com.ancientcurse.block.registry.CursedPlantBlocks;
import com.ancientcurse.block.CursedPlantBlock;
import com.ancientcurse.effect.ModStatusEffects;
import com.ancientcurse.util.CurseZoneManager;
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
    
    // === KHAMSIN CURSE TRACKING ===
    private static final java.util.Map<java.util.UUID, Long> playerExposureTime = new java.util.concurrent.ConcurrentHashMap<>();
    private static final int EXPOSURE_CHECK_INTERVAL = 20; // Check every second
    private static final int INITIAL_CURSE_THRESHOLD = 100; // 5 seconds for first chance
    private static final float BASE_CURSE_CHANCE = 0.1f; // 10% base chance
    
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
        
        // === CURSE ZONE BOOST ===
        CurseZoneManager zoneManager = CurseZoneManager.get(world);
        float khamsinLevel = zoneManager.getInterpolatedKhamsinLevel(pos);
        double boostedSpreadChance = SPREAD_CHANCE * (1 + khamsinLevel / 10.0); // Up to 2x spread in max zones
        
        // === SPREAD MECHANICS ===
        if (random.nextDouble() < boostedSpreadChance) {
            attemptSpread(world, pos, random);
        }
        
        // === ENTITY EFFECTS ===
        if (random.nextInt(20) == 0) {
            applyEntityEffects(world, pos);
        }
        
        // === CURSED PLANT/ENTITY SPAWNING ===
        if (random.nextInt(100) == 0) { // 1% chance per random tick
            // Very rare chance to spawn special entities instead of plants
            if (random.nextFloat() < 0.05f) { // 5% of the 1% = 0.05% total chance
                attemptEntitySpawn(world, pos.up(), random);
            } else {
                attemptPlantSpawn(world, pos.up(), random);
            }
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
     * Applies Khamsin Curse to players standing on cursed earth
     */
    private void applyEntityEffects(ServerWorld world, BlockPos pos) {
        Box box = new Box(pos).expand(3.0, 1.0, 3.0); // Check 3 blocks around, 1 block up
        List<PlayerEntity> players = world.getNonSpectatingEntities(PlayerEntity.class, box);
        
        for (PlayerEntity player : players) {
            if (player.isCreative() || player.isSpectator()) {
                continue;
            }
            
            // Check if player is actually standing on cursed earth
            BlockPos playerPos = player.getBlockPos().down();
            if (!world.getBlockState(playerPos).isOf(this)) {
                continue;
            }
            
            // Track exposure time
            java.util.UUID playerId = player.getUuid();
            long currentTime = world.getTime();
            long exposureStart = playerExposureTime.getOrDefault(playerId, currentTime);
            long exposureDuration = currentTime - exposureStart;
            
            // Update exposure time
            playerExposureTime.put(playerId, exposureStart);
            
            // Only check for curse application every second
            if (currentTime % EXPOSURE_CHECK_INTERVAL != 0) {
                continue;
            }
            
            // Check if player already has curse
            boolean hasCurse = false;
            int currentStage = 0;
            for (int i = 1; i <= 5; i++) {
                if (player.hasStatusEffect(ModStatusEffects.getCurseStage(i))) {
                    hasCurse = true;
                    currentStage = i;
                    break;
                }
            }
            
            // Calculate curse chance based on exposure time
            if (exposureDuration >= INITIAL_CURSE_THRESHOLD) {
                float timeMultiplier = Math.min(3.0f, exposureDuration / (float)INITIAL_CURSE_THRESHOLD);
                float curseChance = BASE_CURSE_CHANCE * timeMultiplier;
                
                // Higher chance if already cursed (progression)
                if (hasCurse && currentStage < 5) {
                    curseChance *= 1.5f;
                }
                
                if (world.random.nextFloat() < curseChance) {
                    if (!hasCurse) {
                        // Apply initial curse
                        player.addStatusEffect(new StatusEffectInstance(
                            ModStatusEffects.KHAMSIN_CURSE_STAGE_1, 
                            600, // 30 seconds
                            0, 
                            false, 
                            true
                        ));
                        
                        // Visual feedback - dark curse particles
                        for (int i = 0; i < 5; i++) {
                            world.addParticle(
                                new DustParticleEffect(new Vector3f(0.5f, 0.2f, 0.6f), 1.0f), // Purple curse particles
                                player.getX() + (world.random.nextDouble() - 0.5),
                                player.getY() + 1,
                                player.getZ() + (world.random.nextDouble() - 0.5),
                                0, 0.1, 0
                            );
                        }
                        
                        AncientCurse.LOGGER.debug("Player {} contracted Khamsin Curse from cursed earth exposure", player.getName().getString());
                    }
                }
            }
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
        
        // Select a random cursed plant
        Block[] cursedPlants = {
            CursedPlantBlocks.CURSED_SPRIG,
            CursedPlantBlocks.CURSED_SPROUT,
            CursedPlantBlocks.BLOODSHADE_THICKET,
            CursedPlantBlocks.DUAT_FERN,
            CursedPlantBlocks.ISFET_FROND,
            CursedPlantBlocks.ISFET_SHRUB,
            CursedPlantBlocks.KHEMNU_POD,
            CursedPlantBlocks.MENFET_SPRIG,
            CursedPlantBlocks.REED_OF_SEKHEM,
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
            
            AncientCurse.LOGGER.debug("Spawned {} at {}", selectedPlant.getTranslationKey(), pos);
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
        boolean spawnDjeserhath = random.nextBoolean();
        
        if (spawnDjeserhath) {
            // Spawn Djeserhath (cactus eye entity)
            com.ancientcurse.entity.DjeserhathEntity djeserhath = ModEntities.DJESERHATH.create(world);
            if (djeserhath != null) {
                djeserhath.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                
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
                    
                    AncientCurse.LOGGER.info("Rare spawn: Djeserhath emerged from cursed earth at {}", pos);
                }
            }
        } else {
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
                    
                    AncientCurse.LOGGER.info("Rare spawn: Khamsin Spread Small manifested from cursed earth at {}", pos);
                }
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
