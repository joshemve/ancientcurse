package com.ancientcurse.block;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.ModBlocks;
import com.ancientcurse.system.CursedEarthManager;
import com.ancientcurse.block.SolarSpireBlock;
import com.ancientcurse.player.AnkhDataManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.SandBlock;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.particle.DustParticleEffect;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cursed Sand - A corrupted version of sand that falls and spreads the curse
 * Inherits from FallingBlock to maintain sand physics
 */
public class CursedSandBlock extends FallingBlock {
    private static final Vector3f CURSED_PARTICLE_COLOR = new Vector3f(0.5f, 0.3f, 0.4f); // Sandy purple
    private static final Map<BlockPos, Long> spreadCooldowns = new HashMap<>();
    private static final int SPREAD_COOLDOWN = 140; // Slower than cursed earth
    
    public CursedSandBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, BlockView world, List<Text> tooltip, TooltipContext options) {
        // No tooltip needed - the mod name shows automatically
        super.appendTooltip(stack, world, tooltip, options);
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
    
    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // Check if we're in an active cleansing zone
        if (SolarSpireBlock.isInActiveCleansingZone(pos)) {
            return;
        }
        
        // Attempt to spread the curse to nearby sand blocks (increased chance)
        if (random.nextFloat() < 0.35f) { // Increased from 0.15f
            attemptSpread(world, pos, random);
        }
        
        // Apply effects to nearby players (similar to cursed earth but weaker)
        if (random.nextFloat() < 0.3f) {
            applyEffectsToNearbyPlayers(world, pos);
        }
    }
    
    /**
     * Attempts to spread the curse to nearby sand blocks
     */
    private void attemptSpread(ServerWorld world, BlockPos pos, Random random) {
        // Check cooldown
        long currentTime = world.getTime();
        if (currentTime - spreadCooldowns.getOrDefault(pos, 0L) < SPREAD_COOLDOWN) {
            return;
        }
        
        // Update cooldown
        spreadCooldowns.put(pos, currentTime);
        
        // Try to spread to adjacent sand blocks
        for (Direction dir : Direction.values()) {
            if (random.nextFloat() < 0.4f) { // Increased from 0.2f for faster spread
                BlockPos targetPos = pos.offset(dir);
                
                if (canCorrupt(world, targetPos)) {
                    // Check for cleansing protection
                    if (SolarSpireBlock.isInActiveCleansingZone(targetPos)) {
                        continue;
                    }
                    
                    // Queue the spread
                    CursedEarthManager.getInstance().queueSpread(
                        world, pos, targetPos, 
                        ModBlocks.CURSED_SAND.getDefaultState()
                    );
                    
                    // Create particle effect
                    createSpreadParticles(world, pos, targetPos, random);
                }
            }
        }
    }
    
    /**
     * Check if a block can be corrupted (is it sand?)
     */
    private boolean canCorrupt(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        
        // Don't corrupt already cursed sand
        if (block == ModBlocks.CURSED_SAND) {
            return false;
        }
        
        // Check if it's a sand-type block
        return isSandBlock(state);
    }
    
    /**
     * Detect if a block is any type of gravity-affected block (sand, gravel, etc)
     * Updated to include gravel and other falling blocks
     */
    public static boolean isSandBlock(BlockState state) {
        Block block = state.getBlock();
        
        // Check vanilla gravity blocks
        if (block == Blocks.SAND || block == Blocks.RED_SAND || 
            block == Blocks.GRAVEL || block == Blocks.SUSPICIOUS_SAND ||
            block == Blocks.SUSPICIOUS_GRAVEL) {
            return true;
        }
        
        // Check if it's a SandBlock subclass (catches all modded sands including ours)
        if (block instanceof SandBlock) {
            return block != ModBlocks.CURSED_SAND; // Exclude our cursed sand
        }
        
        // Check concrete powder (also affected by gravity)
        if (block instanceof FallingBlock) {
            String blockName = block.getTranslationKey().toLowerCase();
            
            // Exclude our cursed sand and blocks that shouldn't convert
            if (block == ModBlocks.CURSED_SAND || 
                blockName.contains("cursed") ||
                blockName.contains("anvil") || // Anvils fall but shouldn't become sand
                blockName.contains("dragon_egg")) { // Dragon egg shouldn't convert
                return false;
            }
            
            // Include any falling block that makes sense to convert
            // This includes sand, gravel, concrete powder, and modded falling blocks
            return true;
        }
        
        // Fallback name-based detection for mods
        String blockName = block.getTranslationKey().toLowerCase();
        
        // Check for gravity-related keywords
        if ((blockName.contains("sand") || blockName.contains("gravel") || 
             blockName.contains("dust") || blockName.contains("powder") ||
             blockName.contains("ash") || blockName.contains("silt")) && 
            !blockName.contains("sandstone") && 
            !blockName.contains("quicksand") &&
            !blockName.contains("cursed")) {
            
            // Additional check - gravity blocks usually have low hardness
            float hardness = block.getHardness();
            if (hardness >= 0 && hardness <= 0.7f) {
                return true;
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
            
            // Add some randomness
            x += (random.nextDouble() - 0.5) * 0.2;
            z += (random.nextDouble() - 0.5) * 0.2;
            
            world.addParticle(
                new DustParticleEffect(CURSED_PARTICLE_COLOR, 0.5f),
                x, y, z,
                0, 0.01, 0
            );
        }
    }
    
    /**
     * Apply effects to nearby players
     */
    private void applyEffectsToNearbyPlayers(ServerWorld world, BlockPos pos) {
        // Check for players standing directly on this block
        Box standingBox = new Box(pos.getX(), pos.getY() + 1, pos.getZ(), 
                                  pos.getX() + 1, pos.getY() + 1.1, pos.getZ() + 1);
        List<PlayerEntity> standingPlayers = world.getNonSpectatingEntities(PlayerEntity.class, standingBox);
        
        for (PlayerEntity player : standingPlayers) {
            if (player.isCreative() || player.isSpectator()) {
                continue;
            }
            
            // Check if player is actually standing on this block
            BlockPos playerBlockPos = player.getBlockPos().down();
            if (!playerBlockPos.equals(pos)) {
                continue;
            }
            
            // Decrease ankh value when standing on cursed sand (moderate rate)
            if (world.getTime() % 75 == 0) { // Every 3.75 seconds
                AnkhDataManager.decreaseAnkhValue(player, 1);
            }
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
        }
    }
}