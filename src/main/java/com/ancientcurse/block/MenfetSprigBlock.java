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
 * A small sprig plant that provides water breathing to entities that touch it
 */
public class MenfetSprigBlock extends EgyptianPlantBlock {
    
    public MenfetSprigBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient && entity instanceof LivingEntity livingEntity) {
            // Apply water breathing effect
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 300, 0, false, false));
        }
    }
    
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        super.randomDisplayTick(state, world, pos, random);
        
        if (random.nextInt(15) == 0) {
            // Add bubble-like particles
            world.addParticle(
                ParticleTypes.BUBBLE,
                pos.getX() + random.nextFloat(),
                pos.getY() + 0.3,
                pos.getZ() + random.nextFloat(),
                0.0, 0.02, 0.0
            );
        }
    }
}
