package com.ancientcurse.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class HeavyMarshBlock extends Block {
    
    public HeavyMarshBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        // Apply marshy slowdown effect and entanglement
        if (!entity.isSpectator()) {
            // Slow down horizontal movement significantly due to dense vegetation
            entity.slowMovement(state, new Vec3d(0.4D, 0.7D, 0.4D));
            
            // Apply entanglement effect - gradually slow entities down
            if (!entity.isOnGround()) {
                // Less severe than mud but still restrictive
                double sinkSpeed = entity.getVelocity().y > -0.08D ? -0.08D : -0.04D;
                entity.setVelocity(entity.getVelocity().multiply(0.6, 0.97, 0.6).add(0, sinkSpeed, 0));
            }
            
            // Apply slowness effect to living entities
            if (entity instanceof LivingEntity livingEntity && world.getTime() % 25 == 0) {
                livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 30, 0, false, false, true));
                
                // Play squelchy grass sound periodically when moving
                Vec3d velocity = entity.getVelocity();
                if ((Math.abs(velocity.x) > 0.05 || Math.abs(velocity.z) > 0.05) && world.getTime() % 14 == 0) {
                    world.playSound(null, pos, SoundEvents.BLOCK_WET_GRASS_STEP, SoundCategory.BLOCKS, 
                            0.4f, 0.9f + world.random.nextFloat() * 0.2f);
                    
                }
            }
            
            // Special player handling
            if (entity instanceof PlayerEntity player && world.isClient) {
                // Show more intense effects when player is moving vertically (likely struggling)
                Vec3d velocity = player.getVelocity();
                if (Math.abs(velocity.y) > 0.08 && world.getTime() % 6 == 0) {
                    world.playSound(player.getX(), player.getY(), player.getZ(), 
                                   SoundEvents.BLOCK_WET_GRASS_BREAK, 
                                   SoundCategory.BLOCKS, 0.3f, 0.7f, false);
                }
            }
        }
    }
    
    
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        // Particle effects removed to reduce lag
        // Only keep occasional ambient sound
        if (random.nextInt(15) == 0 && random.nextInt(12) == 0) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + 1.0;
            double z = pos.getZ() + random.nextDouble();
            
            float pitch = 0.8f + random.nextFloat() * 0.3f;
            world.playSound(x, y, z, SoundEvents.BLOCK_GRASS_HIT, SoundCategory.BLOCKS, 
                    0.15f, pitch, false);
        }
    }
} 