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
 * A cursed vine named after the ancient Egyptian deity of chaos
 * Applies poison and slowness to entities that touch it
 */
public class VineOfApepBlock extends CursedPlantBlock {
    
    public VineOfApepBlock(Settings settings) {
        super(settings, false, true); // Apply poison effect but not wither
    }
    
    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient && entity instanceof LivingEntity livingEntity) {
            // Apply poison and slowness
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 120, 1));
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 0));
        }
    }
    
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        super.randomDisplayTick(state, world, pos, random);
        
        if (random.nextInt(10) == 0) {
            // Add poison-like particles
            world.addParticle(
                ParticleTypes.WARPED_SPORE,
                pos.getX() + random.nextFloat(),
                pos.getY() + 0.5,
                pos.getZ() + random.nextFloat(),
                0.0, 0.05, 0.0
            );
        }
    }
}
