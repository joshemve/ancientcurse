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
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
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
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Solar Spire that purifies ALL connected cursed earth when activated with an Eye of Apophis
 */
public class SolarSpireBlock extends BlockWithEntity {
    
    public static final BooleanProperty ACTIVATED = BooleanProperty.of("activated");
    private static final VoxelShape SHAPE = Block.createCuboidShape(2, 0, 2, 14, 24, 14);
    private static final int BLOCKS_PER_TICK = 10000; // Process massive amounts for near-instant cleansing
    private static final int WAVE_TICK_DELAY = 1; // Process every single tick for rapid expansion
    private static final boolean REDUCED_PARTICLES = true; // Reduce particles to prevent lag
    
    // Track active cleansing operations
    private static final Map<BlockPos, CleansingOperation> activeOperations = new HashMap<>();
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
        // Check in a small radius for any cursed earth
        int checkRadius = 10;
        for (int x = -checkRadius; x <= checkRadius; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -checkRadius; z <= checkRadius; z++) {
                    BlockPos checkPos = center.add(x, y, z);
                    BlockState state = world.getBlockState(checkPos);
                    if (state.getBlock() == ModBlocks.CURSED_EARTH) {
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
        
        // Create cleansing operation
        CleansingOperation operation = new CleansingOperation(pos, player.getUuid());
        activeOperations.put(pos, operation);
        
        // Create protection zone around the station (10 days = 10 * 24000 ticks)
        CursedEarthManager.getInstance().createCleansingProtection((ServerWorld)world, pos, 10 * 24000);
        
        // Start the cleansing process
        ((ServerWorld)world).scheduleBlockTick(pos, this, WAVE_TICK_DELAY, TickPriority.HIGH);
        
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
        
        CleansingOperation operation = activeOperations.get(pos);
        if (operation == null) {
            // Operation was removed, deactivate
            deactivateSpire(world, pos);
            return;
        }
        
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
        
        // Play completion sound
        world.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.BLOCKS, 1.0F, 1.0F);
        
        // Big particle burst
        for (int i = 0; i < 50; i++) {
            double angle = (Math.PI * 2) * i / 50;
            double radius = 2.0;
            double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;
            world.spawnParticles(ParticleTypes.END_ROD,
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
    
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            // Clean up active operations if block is broken
            activeOperations.remove(pos);
            
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
            
            // Process current wave
            Iterator<BlockPos> iterator = toProcess.iterator();
            while (iterator.hasNext() && processedThisTick < BLOCKS_PER_TICK) {
                BlockPos pos = iterator.next();
                iterator.remove();
                
                if (processed.contains(pos)) {
                    continue;
                }
                
                processed.add(pos);
                processedThisTick++;
                
                BlockState state = world.getBlockState(pos);
                if (state.getBlock() == ModBlocks.CURSED_EARTH) {
                    // Cleanse this block
                    cleanseBlock(world, pos);
                    totalCleansed++;
                    
                    // Add adjacent blocks to next wave
                    for (Direction dir : Direction.values()) {
                        BlockPos adjacent = pos.offset(dir);
                        if (!processed.contains(adjacent)) {
                            nextWave.add(adjacent);
                        }
                    }
                }
            }
            
            // Add remaining blocks from current wave to next wave
            nextWave.addAll(toProcess);
            toProcess.clear();
            toProcess.addAll(nextWave);
            
            return !toProcess.isEmpty();
        }
        
        private void cleanseBlock(ServerWorld world, BlockPos pos) {
            // Get the original block tracker instance
            OriginalBlockTracker tracker = OriginalBlockTracker.get(world);
            
            // Get the original block to restore
            BlockState originalState = tracker.getOriginalBlock(pos);
            
            if (originalState != null && originalState.getBlock() != ModBlocks.CURSED_EARTH) {
                // Restore to original state
                world.setBlockState(pos, originalState);
                tracker.clearTracking(pos);
                blocksRestored++;
            } else {
                // Default to grass block
                world.setBlockState(pos, Blocks.GRASS_BLOCK.getDefaultState());
            }
            
            // Visual effect (reduced)
            if (!REDUCED_PARTICLES || world.random.nextFloat() < 0.05f) {
                world.spawnParticles(ParticleTypes.END_ROD,
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
}