package com.ancientcurse.block;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.ModBlocks;
import com.ancientcurse.block.entity.SolarSpireBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/**
 * Solar Spire Pyramidion - The golden capstone that completes the Solar Spire
 * When placed on top of a crucible and plinth, it triggers the assembly of the complete Solar Spire
 */
public class SolarSpirePyramidionBlock extends BaseAncientCurseBlock {
    
    public SolarSpirePyramidionBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, BlockView world, List<Text> tooltip, TooltipContext options) {
        tooltip.add(Text.literal("§6Golden capstone of the Solar Spire").formatted(net.minecraft.util.Formatting.GOLD));
        tooltip.add(Text.literal("§7Completes the spire when placed last").formatted(net.minecraft.util.Formatting.GRAY));
        tooltip.add(Text.literal("§7Transforms all three blocks into one").formatted(net.minecraft.util.Formatting.GRAY));
        super.appendTooltip(stack, world, tooltip, options);
    }
    
    // No custom shape - uses default full block (16x16x16)
    
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        
        if (!world.isClient) {
            // Check if we have a complete structure
            if (checkAndAssembleSpire(world, pos)) {
                AncientCurse.LOGGER.info("Solar Spire assembled at {}", pos.down(2));
            }
        }
    }
    
    /**
     * Checks if all three components are in place and assembles them into a Solar Spire
     */
    private boolean checkAndAssembleSpire(World world, BlockPos pyramidionPos) {
        try {
            // Ensure world is not null and is server-side
            if (world == null || world.isClient) {
                return false;
            }
            
            BlockPos cruciblePos = pyramidionPos.down();
            BlockPos plinthPos = pyramidionPos.down(2);
            
            // Ensure positions are within world bounds
            if (!world.isInBuildLimit(cruciblePos) || !world.isInBuildLimit(plinthPos)) {
                return false;
            }
            
            // Check if chunks are loaded
            if (!world.isChunkLoaded(cruciblePos) || !world.isChunkLoaded(plinthPos)) {
                return false;
            }
            
            // Check for crucible below with null safety
            BlockState crucibleState = world.getBlockState(cruciblePos);
            if (crucibleState == null || !(crucibleState.getBlock() instanceof SolarSpireCrucibleBlock)) {
                return false;
            }
            
            // Check for plinth below crucible with null safety
            BlockState plinthState = world.getBlockState(plinthPos);
            if (plinthState == null || !(plinthState.getBlock() instanceof SolarSpirePlinthBlock)) {
                return false;
            }
            
            // We have all three components! Time to assemble
            assembleIntoSolarSpire(world, plinthPos);
            return true;
        } catch (Exception e) {
            // Log the error for debugging
            AncientCurse.LOGGER.error("Error checking Solar Spire assembly at {}: {}", pyramidionPos, e.getMessage());
            return false;
        }
    }
    
    /**
     * Replaces the three component blocks with a complete Solar Spire
     */
    private void assembleIntoSolarSpire(World world, BlockPos plinthPos) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }
        
        // Create dramatic assembly effects
        createAssemblyEffects(serverWorld, plinthPos);
        
        // Remove the three component blocks
        world.removeBlock(plinthPos.up(2), false); // Remove pyramidion
        world.removeBlock(plinthPos.up(), false);   // Remove crucible
        world.removeBlock(plinthPos, false);        // Remove plinth
        
        // Place the Solar Spire at the plinth position
        BlockState spireState = ModBlocks.SOLAR_SPIRE.getDefaultState();
        world.setBlockState(plinthPos, spireState);
        
        // Get the block entity and trigger spawn animation
        if (world.getBlockEntity(plinthPos) instanceof SolarSpireBlockEntity spireEntity) {
            spireEntity.playSpawnAnimation();
        }
        
        // Play assembly sound
        world.playSound(null, plinthPos, SoundEvents.BLOCK_BEACON_POWER_SELECT, 
                       SoundCategory.BLOCKS, 1.5F, 1.0F);
        world.playSound(null, plinthPos, SoundEvents.BLOCK_CONDUIT_ACTIVATE, 
                       SoundCategory.BLOCKS, 1.0F, 1.2F);
    }
    
    /**
     * Creates visual effects for the assembly process
     */
    private void createAssemblyEffects(ServerWorld world, BlockPos centerPos) {
        // Create a spiral of particles rising from the base
        for (int i = 0; i < 100; i++) {
            double angle = i * 0.2;
            double radius = 1.5 - (i * 0.01);
            double height = i * 0.05;
            
            double x = centerPos.getX() + 0.5 + Math.cos(angle) * radius;
            double y = centerPos.getY() + height;
            double z = centerPos.getZ() + 0.5 + Math.sin(angle) * radius;
            
            // Mix of golden and mystical particles for dramatic effect
            if (i % 3 == 0) {
                world.spawnParticles(ParticleTypes.WAX_ON, x, y, z, 1, 0, 0.1, 0, 0.02);
            } else if (i % 3 == 1) {
                world.spawnParticles(ParticleTypes.FLAME, x, y, z, 1, 0, 0.05, 0, 0.02);
            } else {
                world.spawnParticles(ParticleTypes.ENCHANT, x, y, z, 1, 0, 0, 0, 0.5);
            }
        }
        
        // Create a burst of particles at the assembly point
        world.spawnParticles(ParticleTypes.FLASH, 
                           centerPos.getX() + 0.5, centerPos.getY() + 1.5, centerPos.getZ() + 0.5,
                           3, 0, 0, 0, 0);
        
        // Ring of golden energy particles
        for (int angle = 0; angle < 360; angle += 15) {
            double rad = Math.toRadians(angle);
            double x = centerPos.getX() + 0.5 + Math.cos(rad) * 2;
            double z = centerPos.getZ() + 0.5 + Math.sin(rad) * 2;
            world.spawnParticles(ParticleTypes.FLAME, x, centerPos.getY() + 0.5, z, 
                               2, 0.1, 0.5, 0.1, 0.05);
        }
    }
}