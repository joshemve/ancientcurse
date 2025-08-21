package com.ancientcurse.block;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.ModBlocks;
import com.ancientcurse.ModBlockEntities;
import com.ancientcurse.ModItems;
import com.ancientcurse.block.entity.SolarSpireBlockEntity;
import com.ancientcurse.system.OriginalBlockTracker;
import com.ancientcurse.system.CursedEarthManager;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.util.math.Box;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
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
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.UUID;
import java.util.List;

/**
 * Solar Spire that purifies ALL connected cursed earth when activated with an Eye of Apophis
 */
public class SolarSpireBlock extends BlockWithEntity {
    
    public static final BooleanProperty ACTIVATED = BooleanProperty.of("activated");
    private static final VoxelShape SHAPE = Block.createCuboidShape(3, 0, 3, 13, 48, 13);
    private static final int BLOCKS_PER_WAVE = 50; // Process 50 blocks per wave for gradual cleansing
    private static final int WAVE_TICK_DELAY = 10; // Process every 0.5 seconds for visible progression
    private static final boolean REDUCED_PARTICLES = true; // Reduce particles to prevent lag
    private static final float SPIKE_DAMAGE = 4.0F; // Damage dealt by energy spikes (2 hearts)
    private static final int SPIKE_RADIUS = 3; // Radius of spike damage area
    
    // Track active cleansing operations
    private static final Map<BlockPos, CleansingOperation> activeOperations = new HashMap<>();
    private static final Map<BlockPos, UUID> poweringUpSpires = new HashMap<>(); // Track spires that are powering up
    private static final int CLEANSING_ZONE_RADIUS = 250; // Disable spreading in this radius
    
    public SolarSpireBlock(Settings settings) {
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
        
        // Check if player is using Eye of Apophis
        if (stack.getItem() == ModItems.EYE_OF_APOPHIS) {
            if (!world.isClient) {
                // Check if already activated
                if (state.get(ACTIVATED)) {
                    player.sendMessage(Text.translatable("block.ancientcurse.solar_spire.already_active")
                        .formatted(Formatting.YELLOW), true);
                    return ActionResult.SUCCESS;
                }
                
                // Check for nearby cursed earth
                if (!hasNearbyCursedEarth(world, pos)) {
                    player.sendMessage(Text.translatable("block.ancientcurse.solar_spire.no_corruption")
                        .formatted(Formatting.RED), true);
                    return ActionResult.SUCCESS;
                }
                
                // Activate the solar spire
                activateSpire(world, pos, player, stack);
                
                // Consume the Eye of Apophis
                if (!player.isCreative()) {
                    stack.decrement(1);
                }
            }
            
            return ActionResult.SUCCESS;
        }
        
        return ActionResult.PASS;
    }
    
    private boolean hasNearbyCursedEarth(World world, BlockPos center) {
        // Check in a small radius for any cursed earth or cursed stone
        int checkRadius = 10;
        for (int x = -checkRadius; x <= checkRadius; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -checkRadius; z <= checkRadius; z++) {
                    BlockPos checkPos = center.add(x, y, z);
                    BlockState state = world.getBlockState(checkPos);
                    if (state.getBlock() == ModBlocks.CURSED_EARTH || 
                        state.getBlock() == ModBlocks.CURSED_STONE) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private void activateSpire(World world, BlockPos pos, PlayerEntity player, ItemStack eyeStack) {
        // Set the block to activated state
        world.setBlockState(pos, world.getBlockState(pos).with(ACTIVATED, true));
        
        // Trigger the activate animation on the block entity
        if (world.getBlockEntity(pos) instanceof SolarSpireBlockEntity blockEntity) {
            blockEntity.activate(eyeStack);
        }
        
        // Store the player UUID for later use but DON'T start cleansing yet
        // We'll wait for the power-up animation to complete
        poweringUpSpires.put(pos, player.getUuid());
        
        // Create protection zone around the station (10 days = 10 * 24000 ticks)
        CursedEarthManager.getInstance().createCleansingProtection((ServerWorld)world, pos, 10 * 24000);
        
        // Schedule a check for power-up completion (will check every second)
        ((ServerWorld)world).scheduleBlockTick(pos, this, 20, TickPriority.HIGH);
        
        // Play activation sound
        world.playSound(null, pos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.0F, 1.0F);
        
        // Send initial message
        player.sendMessage(Text.translatable("block.ancientcurse.solar_spire.activating")
            .formatted(Formatting.GOLD), true);
        
        // Visual effects
        for (int i = 0; i < 20; i++) {
            double d = world.random.nextGaussian() * 0.02D;
            double e = world.random.nextGaussian() * 0.02D;
            double f = world.random.nextGaussian() * 0.02D;
            ((ServerWorld)world).spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                1, d, e, f, 0.1);
        }
    }
    
    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, net.minecraft.util.math.random.Random random) {
        if (!state.get(ACTIVATED)) {
            return;
        }
        
        // Check if this spire is still powering up
        if (poweringUpSpires.containsKey(pos)) {
            // Check if power-up is complete
            if (world.getBlockEntity(pos) instanceof SolarSpireBlockEntity blockEntity) {
                if (blockEntity.isPowerUpComplete()) {
                    // Power-up is complete! Start the cleansing
                    UUID playerUuid = poweringUpSpires.remove(pos);
                    
                    // Create cleansing operation
                    CleansingOperation operation = new CleansingOperation(pos, playerUuid);
                    activeOperations.put(pos, operation);
                    
                    // Play a special sound when cleansing begins
                    world.playSound(null, pos, SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    
                    // Send message to player
                    PlayerEntity player = world.getPlayerByUuid(playerUuid);
                    if (player != null) {
                        player.sendMessage(Text.literal("§6The Eye of Apophis manifests! Solar cleansing begins!"), true);
                        player.sendMessage(Text.literal("§cDefend the Solar Spire from cursed creatures!"), true);
                    }
                    
                    // Transition block entity to working state
                    blockEntity.setWorking(true);
                    
                    // Start the actual cleansing process
                    world.scheduleBlockTick(pos, this, WAVE_TICK_DELAY, TickPriority.HIGH);
                } else {
                    // Still powering up, spawn hieroglyph particles
                    spawnHieroglyphParticles(world, pos);
                    
                    // Check again in 1 second
                    world.scheduleBlockTick(pos, this, 20, TickPriority.HIGH);
                }
            }
            return;
        }
        
        // Normal cleansing operation
        CleansingOperation operation = activeOperations.get(pos);
        if (operation == null) {
            // Operation was removed, deactivate
            deactivateSpire(world, pos);
            return;
        }
        
        // Apply spike damage to nearby entities
        applySpikesDamage(world, pos);
        
        // Process the wave
        boolean hasMore = operation.processWave(world, pos);
        
        if (hasMore) {
            // Schedule next wave
            world.scheduleBlockTick(pos, this, WAVE_TICK_DELAY, TickPriority.HIGH);
        } else {
            // Cleansing complete
            completeCleansingAtSpire(world, pos, operation);
        }
    }
    
    private void completeCleansingAtSpire(ServerWorld world, BlockPos pos, CleansingOperation operation) {
        // Send completion message
        PlayerEntity player = world.getPlayerByUuid(operation.playerUuid);
        if (player != null) {
            player.sendMessage(Text.translatable("block.ancientcurse.solar_spire.complete",
                    operation.totalCleansed, operation.blocksRestored)
                .formatted(Formatting.GREEN), false);
        }
        
        // Log the cleansing
        AncientCurse.LOGGER.info("Solar Spire at {} completed cleansing: {} blocks cleansed, {} blocks restored",
            pos, operation.totalCleansed, operation.blocksRestored);
        
        // Log tracker stats for debugging
        OriginalBlockTracker tracker = OriginalBlockTracker.get(world);
        OriginalBlockTracker.TrackerStats stats = tracker.getStats();
        AncientCurse.LOGGER.info("Tracker stats: {} blocks tracked in {} chunks, {} unique block types",
            stats.blocksTracked, stats.chunksTracked, stats.uniqueBlockTypes);
        
        // Play completion sound
        world.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.BLOCKS, 1.0F, 1.0F);
        
        // Big particle burst - golden sun energy
        for (int i = 0; i < 50; i++) {
            double angle = (Math.PI * 2) * i / 50;
            double radius = 2.0;
            double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;
            world.spawnParticles(ParticleTypes.FLAME,
                x, pos.getY() + 1.0, z,
                1, 0, 0.1, 0, 0.05);
        }
        
        // Deactivate after a delay
        world.scheduleBlockTick(pos, this, 100, TickPriority.NORMAL);
        
        // Remove from active operations
        activeOperations.remove(pos);
    }
    
    private void deactivateSpire(World world, BlockPos pos) {
        world.setBlockState(pos, world.getBlockState(pos).with(ACTIVATED, false));
        
        // Tell block entity to stop working animation
        if (world.getBlockEntity(pos) instanceof SolarSpireBlockEntity blockEntity) {
            blockEntity.deactivate();
        }
        
        world.playSound(null, pos, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1.0F, 1.0F);
    }
    
    /**
     * Apply spike damage to hostile entities near the spire
     */
    private void applySpikesDamage(ServerWorld world, BlockPos pos) {
        // Create damage box around the spire
        Box damageBox = new Box(
            pos.getX() - SPIKE_RADIUS, pos.getY() - 1, pos.getZ() - SPIKE_RADIUS,
            pos.getX() + SPIKE_RADIUS + 1, pos.getY() + 3, pos.getZ() + SPIKE_RADIUS + 1
        );
        
        // Find all entities in the damage zone
        List<Entity> nearbyEntities = world.getOtherEntities(null, damageBox);
        
        for (Entity entity : nearbyEntities) {
            if (entity instanceof HostileEntity || 
                (entity instanceof LivingEntity && !(entity instanceof PlayerEntity))) {
                // Check if entity is corrupted/cursed
                String entityName = Registries.ENTITY_TYPE.getId(entity.getType()).getPath();
                if (entityName.contains("cursed") || entityName.contains("withered") || 
                    entityName.contains("scarab") || entityName.contains("locus") ||
                    entityName.contains("djeserhath") || entityName.contains("pharaoh")) {
                    
                    // Deal damage and knock back
                    entity.damage(world.getDamageSources().magic(), SPIKE_DAMAGE);
                    
                    // Knockback effect
                    double dx = entity.getX() - (pos.getX() + 0.5);
                    double dz = entity.getZ() - (pos.getZ() + 0.5);
                    double distance = Math.sqrt(dx * dx + dz * dz);
                    if (distance > 0) {
                        entity.addVelocity(dx / distance * 0.5, 0.2, dz / distance * 0.5);
                    }
                    
                    // Particle effect on hit
                    world.spawnParticles(ParticleTypes.CRIT,
                        entity.getX(), entity.getY() + 1, entity.getZ(),
                        5, 0.2, 0.2, 0.2, 0.1);
                }
            }
        }
    }
    
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            // Clean up active operations and powering up tracking if block is broken
            activeOperations.remove(pos);
            poweringUpSpires.remove(pos);
            
            // Return Eye of Apophis if it was active
            if (state.get(ACTIVATED) && world.getBlockEntity(pos) instanceof SolarSpireBlockEntity blockEntity) {
                blockEntity.dropStoredEye();
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
    
    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SolarSpireBlockEntity(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, ModBlockEntities.SOLAR_SPIRE, SolarSpireBlockEntity::tick);
    }
    
    /**
     * Handles the wave-based cleansing operation
     */
    private static class CleansingOperation {
        private final Set<BlockPos> toProcess = new HashSet<>();
        private final Set<BlockPos> processed = new HashSet<>();
        private final UUID playerUuid;
        private int totalCleansed = 0;
        private int blocksRestored = 0;
        private static final int JUMP_RADIUS = 25; // Radius to search when reaching end of connected blocks
        
        public CleansingOperation(BlockPos start, UUID playerUuid) {
            this.playerUuid = playerUuid;
            // Initialize with blocks adjacent to the spire
            for (Direction dir : Direction.values()) {
                toProcess.add(start.offset(dir));
            }
        }
        
        public boolean processWave(ServerWorld world, BlockPos stationPos) {
            if (toProcess.isEmpty()) {
                return false;
            }
            
            Set<BlockPos> nextWave = new HashSet<>();
            int processedThisTick = 0;
            boolean foundAnyCorruption = false;
            
            // Process current wave
            Iterator<BlockPos> iterator = toProcess.iterator();
            while (iterator.hasNext() && processedThisTick < BLOCKS_PER_WAVE) {
                BlockPos pos = iterator.next();
                iterator.remove();
                
                if (processed.contains(pos)) {
                    continue;
                }
                
                processed.add(pos);
                processedThisTick++;
                
                BlockState state = world.getBlockState(pos);
                
                // Check if it's any corrupted block (cursed earth, cursed stone, or cursed plants)
                if (isCorruptedBlock(state)) {
                    // Cleanse this block
                    cleanseBlock(world, pos);
                    totalCleansed++;
                    foundAnyCorruption = true;
                    
                    // Add adjacent blocks to next wave
                    for (Direction dir : Direction.values()) {
                        BlockPos adjacent = pos.offset(dir);
                        if (!processed.contains(adjacent)) {
                            nextWave.add(adjacent);
                        }
                    }
                }
            }
            
            // If we didn't find any corruption in this wave, search in a radius for more
            if (!foundAnyCorruption && toProcess.isEmpty() && !nextWave.isEmpty()) {
                // Get the last processed positions and search around them
                Set<BlockPos> searchCenters = new HashSet<>(nextWave);
                for (BlockPos center : searchCenters) {
                    searchForCorruptionInRadius(world, center, nextWave);
                }
            }
            
            // Add remaining blocks from current wave to next wave
            nextWave.addAll(toProcess);
            toProcess.clear();
            toProcess.addAll(nextWave);
            
            return !toProcess.isEmpty();
        }
        
        private void searchForCorruptionInRadius(ServerWorld world, BlockPos center, Set<BlockPos> nextWave) {
            // Search in a 25-block radius for any corrupted blocks
            for (int x = -JUMP_RADIUS; x <= JUMP_RADIUS; x++) {
                for (int y = -10; y <= 10; y++) { // Limit Y range for performance
                    for (int z = -JUMP_RADIUS; z <= JUMP_RADIUS; z++) {
                        BlockPos checkPos = center.add(x, y, z);
                        
                        // Skip if already processed
                        if (processed.contains(checkPos)) {
                            continue;
                        }
                        
                        // Check if it's within the radius
                        if (checkPos.isWithinDistance(center, JUMP_RADIUS)) {
                            BlockState state = world.getBlockState(checkPos);
                            if (isCorruptedBlock(state)) {
                                // Found corruption! Add it to be processed
                                nextWave.add(checkPos);
                                // Only add a few to prevent massive expansion
                                if (nextWave.size() > 100) {
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        private boolean isCorruptedBlock(BlockState state) {
            Block block = state.getBlock();
            
            // Check for cursed earth and cursed stone
            if (block == ModBlocks.CURSED_EARTH || block == ModBlocks.CURSED_STONE) {
                return true;
            }
            
            // Check if it's from the CursedPlantBlocks registry
            String blockName = Registries.BLOCK.getId(block).getPath();
            if (blockName.contains("cursed") || blockName.contains("withered") || 
                blockName.contains("isfet") || blockName.contains("duat") ||
                blockName.contains("khemnu") || blockName.contains("kheru") ||
                blockName.contains("menfet") || blockName.contains("sutekh") ||
                blockName.contains("bloodshade") || blockName.contains("duamutef")) {
                return true;
            }
            
            return false;
        }
        
        private void cleanseBlock(ServerWorld world, BlockPos pos) {
            BlockState currentState = world.getBlockState(pos);
            Block currentBlock = currentState.getBlock();
            
            // Get the original block tracker instance
            OriginalBlockTracker tracker = OriginalBlockTracker.get(world);
            
            // Check if it's a corrupted plant (these should just be removed, not restored)
            String blockName = Registries.BLOCK.getId(currentBlock).getPath();
            boolean isCorruptedPlant = blockName.contains("cursed") || blockName.contains("withered") || 
                blockName.contains("isfet") || blockName.contains("duat") ||
                blockName.contains("khemnu") || blockName.contains("kheru") ||
                blockName.contains("menfet") || blockName.contains("sutekh") ||
                blockName.contains("bloodshade") || blockName.contains("duamutef");
            
            if (isCorruptedPlant) {
                // Corrupted plants are just removed (replaced with air)
                AncientCurse.LOGGER.debug("Removing corrupted plant at {}: {}", pos, blockName);
                world.setBlockState(pos, Blocks.AIR.getDefaultState());
            } else {
                // For cursed earth/stone, try to restore original
                BlockState originalState = tracker.getOriginalBlock(pos);
                
                AncientCurse.LOGGER.debug("Cleansing {} at {} - Original tracked: {}", 
                    currentBlock.getTranslationKey(), pos, 
                    originalState != null ? originalState.getBlock().getTranslationKey() : "null");
                
                if (originalState != null && originalState.getBlock() != ModBlocks.CURSED_EARTH && 
                    originalState.getBlock() != ModBlocks.CURSED_STONE) {
                    // Restore to original state
                    AncientCurse.LOGGER.info("Restoring {} to original {} at {}", 
                        currentBlock.getTranslationKey(), originalState.getBlock().getTranslationKey(), pos);
                    world.setBlockState(pos, originalState);
                    tracker.clearTracking(pos);
                    blocksRestored++;
                } else if (currentBlock == ModBlocks.CURSED_EARTH) {
                    // No tracked original - default cursed earth to grass block
                    AncientCurse.LOGGER.debug("No original tracked for cursed earth at {}, defaulting to grass", pos);
                    world.setBlockState(pos, Blocks.GRASS_BLOCK.getDefaultState());
                    tracker.clearTracking(pos); // Clear any stale tracking
                } else if (currentBlock == ModBlocks.CURSED_STONE) {
                    // No tracked original - default cursed stone to regular stone
                    AncientCurse.LOGGER.debug("No original tracked for cursed stone at {}, defaulting to stone", pos);
                    world.setBlockState(pos, Blocks.STONE.getDefaultState());
                    tracker.clearTracking(pos); // Clear any stale tracking
                } else {
                    // Some other corrupted block with no original tracked - remove it
                    AncientCurse.LOGGER.debug("No original tracked for {} at {}, removing", blockName, pos);
                    world.setBlockState(pos, Blocks.AIR.getDefaultState());
                }
            }
            
            // Notify CursedEarthManager that a cursed block was removed
            CursedEarthManager.getInstance().onCursedBlockRemoved(world, pos);
            
            // Visual effect (reduced) - golden cleansing energy
            if (!REDUCED_PARTICLES || world.random.nextFloat() < 0.05f) {
                world.spawnParticles(ParticleTypes.WAX_ON,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    1, 0, 0, 0, 0.02);
            }
            
            // Sound effect (very rare to prevent spam)
            if (world.random.nextFloat() < 0.001f) {
                world.playSound(null, pos, SoundEvents.BLOCK_GRASS_BREAK, SoundCategory.BLOCKS, 0.5F, 1.5F);
            }
        }
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
     * Spawn mystical hieroglyph particles during power-up sequence
     */
    private void spawnHieroglyphParticles(ServerWorld world, BlockPos pos) {
        // Create a spiral of enchantment glyphs around the spire
        double time = world.getTime() * 0.05; // Slow rotation
        int numGlyphs = 8; // Number of glyphs in the circle
        double radius = 1.5; // Distance from spire center
        
        for (int i = 0; i < numGlyphs; i++) {
            double angle = (Math.PI * 2 * i / numGlyphs) + time;
            
            // Calculate position in a circle
            double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;
            
            // Vary the height for a spiral effect
            double y = pos.getY() + 1.0 + Math.sin(time + i * 0.5) * 0.5 + i * 0.2;
            
            // Spawn enchantment glyph particles (hieroglyphs)
            world.spawnParticles(
                ParticleTypes.ENCHANT,
                x, y, z,
                1, 0, 0, 0, 0.5
            );
        }
        
        // Add some golden dust particles for extra mysticism
        for (int i = 0; i < 3; i++) {
            double angle = time * 2 + i * (Math.PI * 2 / 3);
            double x = pos.getX() + 0.5 + Math.cos(angle) * radius * 0.8;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * radius * 0.8;
            double y = pos.getY() + 2.0;
            
            world.spawnParticles(
                ParticleTypes.WAX_ON,
                x, y, z,
                1, 0.1, 0.1, 0.1, 0.02
            );
        }
        
        // Occasional mystical burst
        if (world.random.nextFloat() < 0.1f) {
            world.spawnParticles(
                ParticleTypes.ENCHANTED_HIT,
                pos.getX() + 0.5, pos.getY() + 3.0, pos.getZ() + 0.5,
                5, 0.3, 0.3, 0.3, 0.1
            );
        }
    }
}