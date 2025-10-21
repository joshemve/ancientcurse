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
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.StringIdentifiable;
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
import net.minecraft.world.WorldAccess;
import net.minecraft.world.tick.TickPriority;
import net.minecraft.entity.FallingBlockEntity;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.item.TooltipContext;

import java.util.*;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

/**
 * Solar Spire that purifies ALL connected cursed earth when activated with an Eye of Apophis
 */
public class SolarSpireBlock extends BlockWithEntity implements LandingBlock {

    public static final BooleanProperty ACTIVATED = BooleanProperty.of("activated");
    public static final EnumProperty<ThirdPart> THIRD = EnumProperty.of("third", ThirdPart.class);

    // Shapes for each third of the 3-block tall structure
    private static final VoxelShape SHAPE = Block.createCuboidShape(3, 0, 3, 13, 16, 13);
    private static final int BLOCKS_PER_WAVE = 100; // Process 100 blocks per wave (balanced for performance)
    private static final int WAVE_TICK_DELAY = 5; // Process every 0.25 seconds (balanced)
    private static final boolean REDUCED_PARTICLES = true; // Reduce particles to prevent lag
    private static final float SPIKE_DAMAGE = 4.0F; // Damage dealt by energy spikes (2 hearts)
    private static final int SPIKE_RADIUS = 3; // Radius of spike damage area
    
    // Map to track active spires for entity targeting
    private static final Map<BlockPos, Boolean> activeSpires = new HashMap<>();
    
    // Track active cleansing operations
    private static final Map<BlockPos, CleansingOperation> activeOperations = new HashMap<>();
    private static final Map<BlockPos, UUID> poweringUpSpires = new HashMap<>(); // Track spires that are powering up
    private static final int CLEANSING_ZONE_RADIUS = 250; // Disable spreading in this radius
    
    public SolarSpireBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
            .with(ACTIVATED, false)
            .with(THIRD, ThirdPart.LOWER));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ACTIVATED, THIRD);
    }
    
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
    
    @Override
    public void appendTooltip(ItemStack stack, @Nullable BlockView world, List<Text> tooltip, TooltipContext options) {
        tooltip.add(Text.literal("§6Ancient monument of solar purification").formatted(Formatting.GOLD));
        tooltip.add(Text.literal("§7Requires the Eye of Apophis to awaken").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("§7Cleanses corruption from the land").formatted(Formatting.GRAY));
        super.appendTooltip(stack, world, tooltip, options);
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        // Get the lower block position for interaction
        BlockPos lowerPos = getLowerBlockPos(state, pos);
        BlockState lowerState = world.getBlockState(lowerPos);

        ItemStack stack = player.getStackInHand(hand);

        // Check if player is using Eye of Apophis
        if (stack.getItem() == ModItems.EYE_OF_APOPHIS) {
            if (!world.isClient) {
                // Check if already activated
                if (lowerState.get(ACTIVATED)) {
                    player.sendMessage(Text.translatable("block.ancientcurse.solar_spire.already_active")
                        .formatted(Formatting.YELLOW), true);
                    return ActionResult.SUCCESS;
                }

                // Check for nearby cursed earth
                if (!hasNearbyCursedEarth(world, lowerPos)) {
                    player.sendMessage(Text.translatable("block.ancientcurse.solar_spire.no_corruption")
                        .formatted(Formatting.RED), true);
                    return ActionResult.SUCCESS;
                }

                // Activate the solar spire at the lower block position
                activateSpire(world, lowerPos, player, stack);

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
        // Set all three blocks to activated state
        BlockState lowerState = world.getBlockState(pos);
        world.setBlockState(pos, lowerState.with(ACTIVATED, true));
        world.setBlockState(pos.up(), world.getBlockState(pos.up()).with(ACTIVATED, true));
        world.setBlockState(pos.up(2), world.getBlockState(pos.up(2)).with(ACTIVATED, true));
        
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
    
    private void completeCleansingAtSpire(ServerWorld world, BlockPos pos, CleansingOperation operation) {
        // Log the cleansing for debugging only
        AncientCurse.LOGGER.info("Solar Spire at {} completed cleansing: {} blocks cleansed, {} blocks restored",
            pos, operation.totalCleansed, operation.blocksRestored);
        
        // Log tracker stats for debugging
        OriginalBlockTracker tracker = OriginalBlockTracker.get(world);
        OriginalBlockTracker.TrackerStats stats = tracker.getStats();
        AncientCurse.LOGGER.info("Tracker stats: {} blocks tracked in {} chunks, {} unique block types",
            stats.blocksTracked, stats.chunksTracked, stats.uniqueBlockTypes);
        
        // Remove from active operations immediately
        activeOperations.remove(pos);
        
        // Start the dramatic finale sequence
        startDramaticFinale(world, pos);
    }
    
    /**
     * Creates an immersive, dramatic explosion and ascension effect when the Solar Spire completes its mission
     */
    private void startDramaticFinale(ServerWorld world, BlockPos pos) {
        // Mark this position as undergoing finale
        finalizingSpires.put(pos, 0);
        
        // Drop the Eye of Apophis first (it survives the explosion)
        if (world.getBlockEntity(pos) instanceof SolarSpireBlockEntity blockEntity) {
            blockEntity.dropStoredEye();
        }
        
        // Schedule rapid ticks for the finale animation
        world.scheduleBlockTick(pos, this, 1, TickPriority.HIGH);
    }
    
    // Track spires undergoing finale animation
    private static final Map<BlockPos, Integer> finalizingSpires = new HashMap<>();
    
    /**
     * Execute the dramatic finale phases
     */
    private void executeFinalePhase(ServerWorld world, BlockPos pos, int tick) {
        // Phase 1: Building energy (0-60 ticks / 3 seconds)
        if (tick < 60) {
            // Intensifying particle spiral
            double intensity = tick / 60.0;
            int particleCount = (int)(5 + intensity * 20);
            
            for (int i = 0; i < particleCount; i++) {
                double angle = (tick * 0.2) + (i * Math.PI * 2 / particleCount);
                double radius = 2.0 * (1.0 - intensity * 0.5); // Spiral inward
                double height = tick * 0.1; // Rise up
                
                double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
                double y = pos.getY() + height;
                double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;
                
                // Golden energy particles
                world.spawnParticles(ParticleTypes.WAX_ON, x, y, z, 1, 0, 0.05, 0, 0.02);
                world.spawnParticles(ParticleTypes.END_ROD, x, y + 0.5, z, 1, 0, 0.1, 0, 0.05);
            }
            
            // Pulsing sound every 10 ticks
            if (tick % 10 == 0) {
                float pitch = 0.5f + (float)(intensity * 1.0);
                world.playSound(null, pos, SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.BLOCKS, 1.0f, pitch);
            }
        }
        // Phase 2: Ascension beam (60-80 ticks)
        else if (tick < 80) {
            // Massive vertical beam of light
            for (int y = 0; y < 50; y++) {
                world.spawnParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + y, pos.getZ() + 0.5,
                    2, 0.1, 0, 0.1, 0.05);
            }
            
            // Ring explosions expanding outward
            double ringRadius = (tick - 60) * 0.5;
            for (int i = 0; i < 32; i++) {
                double angle = Math.PI * 2 * i / 32;
                double x = pos.getX() + 0.5 + Math.cos(angle) * ringRadius;
                double z = pos.getZ() + 0.5 + Math.sin(angle) * ringRadius;
                
                world.spawnParticles(ParticleTypes.FLAME, x, pos.getY() + 1, z, 1, 0, 0.2, 0, 0.1);
                world.spawnParticles(ParticleTypes.WAX_ON, x, pos.getY() + 2, z, 1, 0, 0.3, 0, 0.05);
            }
            
            // Thunder sound AND block removal at tick 60
            if (tick == 60) {
                // Play the dramatic sounds
                world.playSound(null, pos, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.BLOCKS, 2.0f, 1.5f);
                world.playSound(null, pos, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.BLOCKS, 1.0f, 1.0f);
                
                // Remove the Solar Spire block RIGHT when the sound plays
                world.removeBlock(pos, false);
                
                // Clean up
                finalizingSpires.remove(pos);
                activeSpires.remove(pos);
            }
        }
        // Phase 3: Final particles and effects (80 ticks)
        else if (tick == 80) {
            // Just subtle completion sounds (block already gone)
            world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.BLOCKS, 1.5f, 1.2f);
            world.playSound(null, pos, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1.0f, 1.5f);
            
            // Golden ascension particles - more subtle and elegant
            world.spawnParticles(ParticleTypes.WAX_ON,
                pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5,
                20, 0.5, 1, 0.5, 0.02); // Reduced from 50 to 20 particles
            
            // Single flash for transition
            world.spawnParticles(ParticleTypes.FLASH,
                pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5,
                1, 0, 0, 0, 0);
            
            // Elegant ring of light dissipating outward
            for (int i = 0; i < 24; i++) {
                double angle = Math.PI * 2 * i / 24;
                double radius = 3;
                double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
                double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;
                
                // Soft white particles floating upward
                world.spawnParticles(ParticleTypes.END_ROD, x, pos.getY() + 1, z, 
                    1, 0, 0.2, 0, 0.05);
            }
            
            // Mystical enchantment particles spiraling up
            for (int h = 0; h < 10; h++) {
                double spiralAngle = h * 0.5;
                double spiralX = pos.getX() + 0.5 + Math.cos(spiralAngle) * 0.5;
                double spiralZ = pos.getZ() + 0.5 + Math.sin(spiralAngle) * 0.5;
                double spiralY = pos.getY() + h * 0.5;
                
                world.spawnParticles(ParticleTypes.ENCHANT, spiralX, spiralY, spiralZ, 
                    2, 0.1, 0.1, 0.1, 0.5);
            }
            
            // Final ascending particles
            for (int i = 0; i < 20; i++) {
                world.spawnParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + i, pos.getZ() + 0.5,
                    3, 0.2, 0, 0.2, 0.1);
            }
            
            // Leave behind a golden memory
            world.spawnParticles(ParticleTypes.WAX_ON,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                50, 0.5, 0.5, 0.5, 0.02);
            
            return; // Stop scheduling ticks - finale complete
        }
        
        // Continue the finale animation until tick 80
        if (tick < 80) {
            finalizingSpires.put(pos, tick + 1);
            world.scheduleBlockTick(pos, this, 1, TickPriority.HIGH);
        }
    }
    
    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, net.minecraft.util.math.random.Random random) {
        // Check if this spire is in finale animation
        if (finalizingSpires.containsKey(pos)) {
            int tick = finalizingSpires.get(pos);
            executeFinalePhase(world, pos, tick);
            return;
        }
        
        // Continue with regular scheduled tick logic from original implementation
        // First check if this is an activated spire
        if (state.get(ACTIVATED)) {
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
                        
                        // MASSIVE HIEROGLYPH EXPLOSION - Ra's power connecting from the heavens!
                        spawnPowerConnectionExplosion(world, pos);
                    
                        // Send message to player
                        PlayerEntity player = world.getPlayerByUuid(playerUuid);
                        if (player != null) {
                            player.sendMessage(Text.literal("§6The Eye of Apophis manifests! Solar cleansing begins!"), true);
                        }
                        
                        // Transition block entity to working state
                        blockEntity.setWorking(true);
                        
                        // Mark this spire as active for entity targeting
                        activeSpires.put(pos, true);
                        
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
            
            // Spawn hieroglyph particles during cleansing
            spawnWorkingHieroglyphParticles(world, pos);
            
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
        
        // Only check falling for non-activated spires
        if (!state.get(ACTIVATED) && !canPlaceAt(state, world, pos)) {
            // Make the Solar Spire fall
            FallingBlockEntity fallingBlockEntity = FallingBlockEntity.spawnFromBlock(world, pos, state);
            if (fallingBlockEntity != null) {
                // Preserve the block entity data
                if (world.getBlockEntity(pos) instanceof SolarSpireBlockEntity blockEntity) {
                    // Save important data to the falling entity
                    fallingBlockEntity.blockEntityData = blockEntity.createNbt();
                }
            }
        }
    }
    
    private void deactivateSpire(World world, BlockPos pos) {
        // Deactivate all three parts - check if they're still Solar Spire blocks first
        BlockState baseState = world.getBlockState(pos);
        BlockState middleState = world.getBlockState(pos.up());
        BlockState topState = world.getBlockState(pos.up(2));

        if (baseState.contains(ACTIVATED)) {
            world.setBlockState(pos, baseState.with(ACTIVATED, false));
        }
        if (middleState.contains(ACTIVATED)) {
            world.setBlockState(pos.up(), middleState.with(ACTIVATED, false));
        }
        if (topState.contains(ACTIVATED)) {
            world.setBlockState(pos.up(2), topState.with(ACTIVATED, false));
        }

        // Tell block entity to stop working animation
        if (world.getBlockEntity(pos) instanceof SolarSpireBlockEntity blockEntity) {
            blockEntity.deactivate();
        }

        // Remove from active spires
        activeSpires.remove(pos);

        world.playSound(null, pos, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1.0F, 1.0F);
    }
    
    /**
     * Called when an entity attacks the spire
     */
    public static void damageSpire(World world, BlockPos pos, int damage) {
        if (world.getBlockEntity(pos) instanceof SolarSpireBlockEntity blockEntity) {
            blockEntity.damage(damage);
        }
    }
    
    /**
     * Get the nearest active Solar Spire within range
     */
    public static BlockPos getNearestActiveSpire(World world, BlockPos from, int maxRange) {
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (BlockPos spirePos : activeSpires.keySet()) {
            if (spirePos.isWithinDistance(from, maxRange)) {
                double distance = spirePos.getSquaredDistance(from);
                if (distance < nearestDistance) {
                    // Verify the spire still exists and is active
                    BlockState state = world.getBlockState(spirePos);
                    if (state.getBlock() instanceof SolarSpireBlock && state.get(ACTIVATED)) {
                        nearest = spirePos;
                        nearestDistance = distance;
                    }
                }
            }
        }
        
        return nearest;
    }
    
    /**
     * Called when the spire is destroyed during defense
     */
    public void onSpireDestroyed(World world, BlockPos pos) {
        if (!world.isClient) {
            // Clean up operations
            CleansingOperation operation = activeOperations.remove(pos);
            poweringUpSpires.remove(pos);
            activeSpires.remove(pos);
            
            // Notify players of failure
            if (operation != null) {
                PlayerEntity player = ((ServerWorld)world).getPlayerByUuid(operation.playerUuid);
                if (player != null) {
                    player.sendMessage(Text.literal("§cThe Solar Spire has been destroyed! Cleansing failed!"), false);
                }
            }
            
            // Remove protection zone
            CursedEarthManager.getInstance().clearProtectionAt((ServerWorld)world, pos);
            
            // Break the block
            world.breakBlock(pos, false);
            
            // Play destruction sound
            world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 1.0F, 1.0F);
            
            // Explosion particles
            ((ServerWorld)world).spawnParticles(ParticleTypes.EXPLOSION_EMITTER,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                1, 0, 0, 0, 0);
        }
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
            // Get the lower block position for cleanup
            BlockPos lowerPos = getLowerBlockPos(state, pos);

            // Clean up active operations and powering up tracking if block is broken
            activeOperations.remove(lowerPos);
            poweringUpSpires.remove(lowerPos);
            activeSpires.remove(lowerPos);

            // Return Eye of Apophis if it was active (only from lower block)
            if (state.get(ACTIVATED) && world.getBlockEntity(lowerPos) instanceof SolarSpireBlockEntity blockEntity) {
                blockEntity.dropStoredEye();
            }

            // Remove the other two parts of the structure
            if (!world.isClient) {
                ThirdPart third = state.get(THIRD);
                if (third == ThirdPart.LOWER) {
                    // Remove middle and upper
                    world.removeBlock(pos.up(), false);
                    world.removeBlock(pos.up(2), false);
                } else if (third == ThirdPart.MIDDLE) {
                    // Remove lower and upper
                    world.removeBlock(pos.down(), false);
                    world.removeBlock(pos.up(), false);
                } else if (third == ThirdPart.UPPER) {
                    // Remove lower and middle
                    world.removeBlock(pos.down(2), false);
                    world.removeBlock(pos.down(), false);
                }
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    /**
     * Get the position of the lower block regardless of which part we're called from
     */
    private BlockPos getLowerBlockPos(BlockState state, BlockPos pos) {
        ThirdPart third = state.get(THIRD);
        return switch (third) {
            case MIDDLE -> pos.down();
            case UPPER -> pos.down(2);
            default -> pos; // LOWER
        };
    }
    
    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        // Only create block entity for the lower part
        if (state.get(THIRD) == ThirdPart.LOWER) {
            return new SolarSpireBlockEntity(pos, state);
        }
        return null;
    }
    
    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);

        // If this is the lower part being placed, create the middle and upper parts
        if (!world.isClient && state.get(THIRD) == ThirdPart.LOWER) {
            BlockPos middlePos = pos.up();
            BlockPos upperPos = pos.up(2);

            // Check if there's space for the full structure
            if (world.getBlockState(middlePos).isReplaceable() && world.getBlockState(upperPos).isReplaceable()) {
                // Place middle part
                world.setBlockState(middlePos, state.with(THIRD, ThirdPart.MIDDLE), Block.NOTIFY_ALL);
                // Place upper part
                world.setBlockState(upperPos, state.with(THIRD, ThirdPart.UPPER), Block.NOTIFY_ALL);
            }
        }

        // Schedule a tick to check if we should fall
        world.scheduleBlockTick(pos, this, 2);
    }
    
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        // Check if support was removed from below
        if (direction == Direction.DOWN && !canPlaceAt(state, world, pos)) {
            // Schedule a tick to make it fall
            world.scheduleBlockTick(pos, this, 2);
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
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
        private final BlockPos centerPos; // The Solar Spire position
        private final Set<BlockPos> processed = new HashSet<>();
        private final Map<BlockPos, Float> corruptionFrontier = new HashMap<>(); // Tracks the cleansing frontier with priorities
        private final UUID playerUuid;
        private int totalCleansed = 0;
        private int blocksRestored = 0;
        private float currentRadius = 1.0f; // Start at radius 1
        private int emptyWaveCount = 0; // Track waves with no corruption found
        private static final float RADIUS_INCREMENT = 6.0f; // Base expansion rate (reduced for thoroughness)
        private static final int JUMP_SEARCH_RADIUS = 35; // Search radius for corruption jumps
        private static final float ORGANIC_VARIATION = 0.3f; // Adds natural variation to the wave
        private final Random waveRandom = new Random(); // For organic patterns
        
        public CleansingOperation(BlockPos start, UUID playerUuid) {
            this.centerPos = start;
            this.playerUuid = playerUuid;
            // Initialize with starting positions
            for (Direction dir : Direction.values()) {
                BlockPos adj = centerPos.offset(dir);
                corruptionFrontier.put(adj, 1.0f);
            }
        }
        
        public boolean processWave(ServerWorld world, BlockPos stationPos) {
            int processedThisTick = 0;
            boolean foundAnyCorruption = false;
            
            // Always scan the current radius thoroughly
            scanCurrentRadius(world);
            
            // Add all corrupted blocks from adjacent to already processed blocks
            expandFromProcessedBlocks(world);
            
            // Sort frontier by pure distance (closest first)
            List<Map.Entry<BlockPos, Float>> sortedFrontier = new ArrayList<>(corruptionFrontier.entrySet());
            sortedFrontier.sort((a, b) -> Float.compare(a.getValue(), b.getValue()));
            
            // Process blocks in order of distance
            for (Map.Entry<BlockPos, Float> entry : sortedFrontier) {
                if (processedThisTick >= BLOCKS_PER_WAVE) {
                    break;
                }
                
                BlockPos pos = entry.getKey();
                
                if (processed.contains(pos)) {
                    continue;
                }
                
                processed.add(pos);
                corruptionFrontier.remove(pos);
                
                BlockState state = world.getBlockState(pos);
                
                if (isCorruptedBlock(state)) {
                    // Always cleanse corrupted blocks we find
                    cleanseBlock(world, pos);
                    totalCleansed++;
                    foundAnyCorruption = true;
                    processedThisTick++;
                    
                    // Add all adjacent blocks for checking
                    float distance = (float)Math.sqrt(pos.getSquaredDistance(centerPos));
                    addAdjacentBlocks(world, pos, distance);
                } else {
                    // Even if no corrupted block, check for Khamsin entities to cleanse
                    cleanseKhamsinEntitiesAt(world, pos);
                    // Even if not corrupted, we processed this position
                    processedThisTick++;
                }
            }
            
            // Search for distant patches if we're running low on targets
            if (corruptionFrontier.size() < 100 && currentRadius > 20) {
                searchForDistantCorruption(world);
            }
            
            // Expand radius for next wave
            currentRadius += RADIUS_INCREMENT;
            
            // Track if we've scanned enough without finding corruption
            if (!foundAnyCorruption) {
                emptyWaveCount++;
            } else {
                emptyWaveCount = 0; // Reset if we found corruption
            }
            
            // Complete if we've had multiple waves with no corruption found
            if (emptyWaveCount >= 3 && corruptionFrontier.isEmpty()) {
                return false; // Cleansing complete!
            }
            
            // Continue if we have more to process or haven't reached max radius
            return !corruptionFrontier.isEmpty() || currentRadius < 300;
        }
        
        private void scanCurrentRadius(ServerWorld world) {
            int radiusInt = (int)Math.ceil(currentRadius);
            
            // Scan everything within current radius thoroughly
            for (int x = -radiusInt; x <= radiusInt; x++) {
                for (int z = -radiusInt; z <= radiusInt; z++) {
                    double horizontalDistance = Math.sqrt(x * x + z * z);
                    
                    // Check if within current scanning radius
                    if (horizontalDistance <= currentRadius) {
                        // Slightly reduce Y range at very long distances for performance
                        int yRange = currentRadius > 150 ? 7 : 10;
                        for (int y = -yRange; y <= yRange; y++) {
                            BlockPos checkPos = centerPos.add(x, y, z);
                            
                            if (!processed.contains(checkPos) && !corruptionFrontier.containsKey(checkPos)) {
                                BlockState state = world.getBlockState(checkPos);
                                if (isCorruptedBlock(state)) {
                                    // Simple distance priority - closer blocks get processed first
                                    float priority = (float)Math.sqrt(checkPos.getSquaredDistance(centerPos));
                                    corruptionFrontier.put(checkPos, priority);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        private void expandFromProcessedBlocks(ServerWorld world) {
            // Check around all recently processed blocks for missed corruption
            Set<BlockPos> recentlyProcessed = new HashSet<>();
            for (BlockPos processedPos : processed) {
                // Only check recently processed blocks (within current radius + a bit)
                if (processedPos.isWithinDistance(centerPos, currentRadius + 10)) {
                    recentlyProcessed.add(processedPos);
                }
            }
            
            // Check all adjacent positions to recently processed blocks
            for (BlockPos processedPos : recentlyProcessed) {
                // Check all 26 adjacent positions (including diagonals)
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dy == 0 && dz == 0) continue;
                            
                            BlockPos checkPos = processedPos.add(dx, dy, dz);
                            if (!processed.contains(checkPos) && !corruptionFrontier.containsKey(checkPos)) {
                                BlockState state = world.getBlockState(checkPos);
                                if (isCorruptedBlock(state)) {
                                    float priority = (float)Math.sqrt(checkPos.getSquaredDistance(centerPos));
                                    corruptionFrontier.put(checkPos, priority);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        private void addAdjacentBlocks(ServerWorld world, BlockPos cleansedPos, float distance) {
            // Check all 26 adjacent blocks (6 faces + 12 edges + 8 corners)
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        
                        BlockPos adjacent = cleansedPos.add(dx, dy, dz);
                        
                        if (!processed.contains(adjacent) && !corruptionFrontier.containsKey(adjacent)) {
                            // Always add to frontier to check, even if not corrupted
                            // This ensures we don't miss any blocks
                            float priority = (float)Math.sqrt(adjacent.getSquaredDistance(centerPos));
                            corruptionFrontier.put(adjacent, priority);
                        }
                    }
                }
            }
        }
        
        private void cleanseKhamsinEntitiesAt(ServerWorld world, BlockPos pos) {
            // Find and remove any Khamsin or Djeserhath entities at this position
            Box searchBox = new Box(pos).expand(0.5); // Check exact block position
            List<Entity> entities = world.getOtherEntities(null, searchBox);
            
            for (Entity entity : entities) {
                String entityName = Registries.ENTITY_TYPE.getId(entity.getType()).getPath();
                if (entityName.contains("khamsin") || entityName.contains("djeserhath")) {
                    // Play cleansing effect (reduced particles)
                    world.spawnParticles(ParticleTypes.WAX_ON,
                        entity.getX(), entity.getY() + 0.5, entity.getZ(),
                        3, 0.2, 0.2, 0.2, 0.05); // Reduced from 10 to 3 particles
                    
                    // Play a cleansing sound (more dramatic for Djeserhath)
                    if (entityName.contains("djeserhath")) {
                        world.playSound(null, entity.getBlockPos(), SoundEvents.ENTITY_BLAZE_DEATH, 
                            SoundCategory.HOSTILE, 1.0F, 1.5F);
                        // Extra particles for boss removal
                        world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING,
                            entity.getX(), entity.getY() + 1.0, entity.getZ(),
                            20, 0.5, 0.5, 0.5, 0.1);
                    } else {
                        world.playSound(null, entity.getBlockPos(), SoundEvents.BLOCK_FIRE_EXTINGUISH, 
                            SoundCategory.HOSTILE, 0.5F, 2.0F);
                    }
                    
                    // Remove the entity
                    entity.discard();
                    
                    totalCleansed++; // Count as cleansed
                    AncientCurse.LOGGER.debug("Cleansed {} entity at {}", entityName, pos);
                }
            }
        }
        
        private void searchForDistantCorruption(ServerWorld world) {
            // Pick random frontier positions to search from
            List<BlockPos> searchOrigins = new ArrayList<>(corruptionFrontier.keySet());
            if (searchOrigins.isEmpty()) {
                searchOrigins.add(centerPos);
            }
            
            for (BlockPos origin : searchOrigins) {
                // Search in a large radius for disconnected corruption
                for (int attempts = 0; attempts < 10; attempts++) {
                    int x = origin.getX() + waveRandom.nextInt(JUMP_SEARCH_RADIUS * 2) - JUMP_SEARCH_RADIUS;
                    int z = origin.getZ() + waveRandom.nextInt(JUMP_SEARCH_RADIUS * 2) - JUMP_SEARCH_RADIUS;
                    int y = origin.getY() + waveRandom.nextInt(11) - 5;
                    
                    BlockPos checkPos = new BlockPos(x, y, z);
                    if (!processed.contains(checkPos) && !corruptionFrontier.containsKey(checkPos)) {
                        BlockState state = world.getBlockState(checkPos);
                        if (isCorruptedBlock(state)) {
                            // Found distant corruption! Add it with priority based on distance
                            float priority = (float)Math.sqrt(checkPos.getSquaredDistance(centerPos));
                            corruptionFrontier.put(checkPos, priority);
                            
                            // Add surrounding blocks too for better coverage
                            for (Direction dir : Direction.values()) {
                                BlockPos adj = checkPos.offset(dir);
                                if (!processed.contains(adj) && !corruptionFrontier.containsKey(adj)) {
                                    if (isCorruptedBlock(world.getBlockState(adj))) {
                                        corruptionFrontier.put(adj, priority + 1);
                                    }
                                }
                            }
                            break; // Found corruption, move to next search origin
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
            
            // Check if it's from the CursedPlantBlocks registry, Small Khamsin Spread, or Reed of Sekhem
            String blockName = Registries.BLOCK.getId(block).getPath();
            if (blockName.contains("cursed") || blockName.contains("withered") || 
                blockName.contains("isfet") || blockName.contains("duat") ||
                blockName.contains("khemnu") || blockName.contains("kheru") ||
                blockName.contains("menfet") || blockName.contains("sutekh") ||
                blockName.contains("bloodshade") || blockName.contains("duamutef") ||
                blockName.contains("khamsin") || blockName.contains("reed_of_sekhem") ||
                blockName.contains("sekhem")) {
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
            // IMPORTANT: Exclude cursed_earth and cursed_stone - they should be restored, not removed!
            String blockName = Registries.BLOCK.getId(currentBlock).getPath();
            boolean isCorruptedPlant = false;
            
            // Only check for plant names if it's NOT cursed earth or cursed stone
            if (currentBlock != ModBlocks.CURSED_EARTH && currentBlock != ModBlocks.CURSED_STONE) {
                isCorruptedPlant = blockName.contains("cursed") || blockName.contains("withered") || 
                    blockName.contains("isfet") || blockName.contains("duat") ||
                    blockName.contains("khemnu") || blockName.contains("kheru") ||
                    blockName.contains("menfet") || blockName.contains("sutekh") ||
                    blockName.contains("bloodshade") || blockName.contains("duamutef") ||
                    blockName.contains("khamsin") || blockName.contains("reed_of_sekhem") ||
                    blockName.contains("sekhem");
            }
            
            if (isCorruptedPlant) {
                // Check if Reed of Sekhem has a tracked original (was grass)
                if (blockName.contains("reed_of_sekhem") || blockName.contains("sekhem")) {
                    BlockState originalState = tracker.getOriginalBlock(pos);
                    if (originalState != null) {
                        // Restore to original grass/fern
                        AncientCurse.LOGGER.debug("Restoring Reed of Sekhem to original {} at {}", 
                            originalState.getBlock().getTranslationKey(), pos);
                        world.setBlockState(pos, originalState);
                        tracker.clearTracking(pos);
                        blocksRestored++;
                    } else {
                        // No tracked original, just remove it
                        world.setBlockState(pos, Blocks.AIR.getDefaultState());
                    }
                } else {
                    // Other corrupted plants are just removed (replaced with air)
                    AncientCurse.LOGGER.debug("Removing corrupted plant at {}: {}", pos, blockName);
                    world.setBlockState(pos, Blocks.AIR.getDefaultState());
                }
                
                // Also remove any Khamsin entities at this position
                cleanseKhamsinEntities(world, pos);
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
            
            // Visual effect (greatly reduced) - golden cleansing energy
            if (!REDUCED_PARTICLES || world.random.nextFloat() < 0.01f) { // Reduced from 0.05f to 0.01f
                world.spawnParticles(ParticleTypes.WAX_ON,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    1, 0, 0, 0, 0.02);
            }
            
            // Sound effect (very rare to prevent spam)
            if (world.random.nextFloat() < 0.001f) {
                world.playSound(null, pos, SoundEvents.BLOCK_GRASS_BREAK, SoundCategory.BLOCKS, 0.5F, 1.5F);
            }
        }
        
        private void cleanseKhamsinEntities(ServerWorld world, BlockPos pos) {
            // Find and remove any Khamsin or Djeserhath entities at or near this position
            Box searchBox = new Box(pos).expand(1.0); // Check a small area around the block
            List<Entity> entities = world.getOtherEntities(null, searchBox);
            
            for (Entity entity : entities) {
                String entityName = Registries.ENTITY_TYPE.getId(entity.getType()).getPath();
                if (entityName.contains("khamsin") || entityName.contains("djeserhath")) {
                    // Play cleansing effect (reduced particles)
                    world.spawnParticles(ParticleTypes.WAX_ON,
                        entity.getX(), entity.getY() + 0.5, entity.getZ(),
                        3, 0.2, 0.2, 0.2, 0.05); // Reduced from 10 to 3 particles
                    
                    // Play sound for Djeserhath removal (more dramatic)
                    if (entityName.contains("djeserhath")) {
                        world.playSound(null, entity.getBlockPos(), SoundEvents.ENTITY_BLAZE_DEATH, 
                            SoundCategory.HOSTILE, 1.0F, 1.5F);
                    }
                    
                    // Remove the entity
                    entity.discard();
                    
                    AncientCurse.LOGGER.debug("Cleansed {} entity at {}", entityName, pos);
                }
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
    
    /**
     * Spawn more intense hieroglyph particles during working/cleansing state
     */
    private void spawnWorkingHieroglyphParticles(ServerWorld world, BlockPos pos) {
        double time = world.getTime() * 0.08; // Faster rotation during cleansing
        
        // Inner ring of hieroglyphs
        int innerGlyphs = 6;
        double innerRadius = 1.2;
        for (int i = 0; i < innerGlyphs; i++) {
            double angle = (Math.PI * 2 * i / innerGlyphs) + time;
            double x = pos.getX() + 0.5 + Math.cos(angle) * innerRadius;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * innerRadius;
            double y = pos.getY() + 2.0 + Math.sin(time * 2 + i) * 0.3;
            
            world.spawnParticles(
                ParticleTypes.ENCHANT,
                x, y, z,
                1, 0, 0, 0, 0.8
            );
        }
        
        // Outer ring of hieroglyphs (counter-rotating)
        int outerGlyphs = 10;
        double outerRadius = 2.5;
        for (int i = 0; i < outerGlyphs; i++) {
            double angle = (Math.PI * 2 * i / outerGlyphs) - time * 0.5; // Counter-rotate
            double x = pos.getX() + 0.5 + Math.cos(angle) * outerRadius;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * outerRadius;
            double y = pos.getY() + 1.5 + Math.cos(time * 1.5 + i * 0.3) * 0.4;
            
            world.spawnParticles(
                ParticleTypes.ENCHANT,
                x, y, z,
                1, 0, 0, 0, 0.6
            );
        }
        
        // Vertical column of rising hieroglyphs
        if (world.random.nextFloat() < 0.3f) {
            double riseHeight = (world.getTime() % 60) / 10.0; // Rise from 0 to 6 blocks
            world.spawnParticles(
                ParticleTypes.ENCHANT,
                pos.getX() + 0.5 + (world.random.nextDouble() - 0.5) * 0.5,
                pos.getY() + riseHeight,
                pos.getZ() + 0.5 + (world.random.nextDouble() - 0.5) * 0.5,
                1, 0, 0.1, 0, 0.3
            );
        }
        
        // Intense golden energy pulses
        if (world.getTime() % 20 == 0) { // Every second
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI * 2 * i / 8;
                double x = pos.getX() + 0.5 + Math.cos(angle) * 1.8;
                double z = pos.getZ() + 0.5 + Math.sin(angle) * 1.8;
                
                world.spawnParticles(
                    ParticleTypes.WAX_ON,
                    x, pos.getY() + 3.0, z,
                    2, 0.1, 0.2, 0.1, 0.05
                );
            }
        }
        
        // Occasional mystical explosion
        if (world.random.nextFloat() < 0.05f) {
            world.spawnParticles(
                ParticleTypes.ENCHANTED_HIT,
                pos.getX() + 0.5, pos.getY() + 3.5, pos.getZ() + 0.5,
                10, 0.5, 0.5, 0.5, 0.2
            );
        }
    }
    
    public boolean canPlaceAt(BlockState state, BlockView world, BlockPos pos) {
        ThirdPart third = state.get(THIRD);

        if (third == ThirdPart.LOWER) {
            // Lower part needs solid ground beneath it
            BlockPos below = pos.down();
            BlockState belowState = world.getBlockState(below);
            return belowState.isSideSolidFullSquare(world, below, Direction.UP);
        } else if (third == ThirdPart.MIDDLE) {
            // Middle part needs lower part below
            BlockState below = world.getBlockState(pos.down());
            return below.isOf(this) && below.get(THIRD) == ThirdPart.LOWER;
        } else { // UPPER
            // Upper part needs middle part below
            BlockState below = world.getBlockState(pos.down());
            return below.isOf(this) && below.get(THIRD) == ThirdPart.MIDDLE;
        }
    }
    
    @Override
    public void onLanding(World world, BlockPos pos, BlockState fallingBlockState, BlockState currentStateInPos, FallingBlockEntity fallingBlockEntity) {
        // When the Solar Spire lands after falling, restore its block entity if needed
        if (!world.isClient && fallingBlockEntity.blockEntityData != null) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof SolarSpireBlockEntity) {
                blockEntity.readNbt(fallingBlockEntity.blockEntityData);
            }
        }
    }
    
    /**
     * Create a cleaner, more elegant power connection effect when Ra's power connects
     */
    private void spawnPowerConnectionExplosion(ServerWorld world, BlockPos pos) {
        // Thunder sound for dramatic effect
        world.playSound(null, pos, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.BLOCKS, 0.5F, 1.5F);
        
        // Single elegant ring of hieroglyphs expanding outward
        int numGlyphs = 12;
        for (int i = 0; i < numGlyphs; i++) {
            double angle = (Math.PI * 2 * i / numGlyphs);
            double x = pos.getX() + 0.5 + Math.cos(angle) * 2.5;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * 2.5;
            
            // Spawn a few enchantment particles per position
            world.spawnParticles(
                ParticleTypes.ENCHANT,
                x, pos.getY() + 3.5, z,
                2, 0.1, 0.1, 0.1, 0.5
            );
        }
        
        // Simple vertical beam of light
        for (int height = 0; height < 10; height++) {
            double y = pos.getY() + 3.5 + height;
            
            // Minimal white particles for the beam
            world.spawnParticles(
                ParticleTypes.END_ROD,
                pos.getX() + 0.5, y, pos.getZ() + 0.5,
                1, 0.05, 0, 0.05, 0.02
            );
        }
        
        // Small ground impact - just a hint of energy
        for (int i = 0; i < 8; i++) {
            double angle = (Math.PI * 2 * i / 8);
            double x = pos.getX() + 0.5 + Math.cos(angle) * 1.5;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * 1.5;
            
            world.spawnParticles(
                ParticleTypes.ENCHANT,
                x, pos.getY() + 0.1, z,
                1, 0, 0, 0, 0.01
            );
        }
        
        // Single flash effect for impact
        world.spawnParticles(
            ParticleTypes.FLASH,
            pos.getX() + 0.5, pos.getY() + 3.5, pos.getZ() + 0.5,
            1, 0, 0, 0, 0
        );
    }

    /**
     * Enum for the three parts of the Solar Spire (3 blocks tall)
     */
    public enum ThirdPart implements StringIdentifiable {
        LOWER("lower"),
        MIDDLE("middle"),
        UPPER("upper");

        private final String name;

        ThirdPart(String name) {
            this.name = name;
        }

        @Override
        public String asString() {
            return this.name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }
}