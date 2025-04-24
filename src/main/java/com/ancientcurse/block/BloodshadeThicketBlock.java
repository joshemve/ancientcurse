package com.ancientcurse.block;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * A dense, bloodred thicket that damages entities and applies wither effect
 */
public class BloodshadeThicketBlock extends CursedPlantBlock {
    
    public BloodshadeThicketBlock(Settings settings) {
        super(settings, true, false); // Apply wither effect but not poison
    }
    
    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient && entity instanceof LivingEntity livingEntity) {
            // Apply damage and wither effect
            livingEntity.damage(world.getDamageSources().magic(), 1.0f);
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 100, 0));
            
            // Apply slowness to simulate getting caught in the thicket
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1));
        }
    }
    
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        super.randomDisplayTick(state, world, pos, random);
        
        if (random.nextInt(10) == 0) {
            // Add crimson-like particles
            world.addParticle(
                ParticleTypes.CRIMSON_SPORE,
                pos.getX() + random.nextFloat(),
                pos.getY() + 0.5,
                pos.getZ() + random.nextFloat(),
                0.0, 0.05, 0.0
            );
        }
    }
}
