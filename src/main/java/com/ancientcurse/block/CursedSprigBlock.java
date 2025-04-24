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
 * A small cursed plant sprig that applies minor negative effects
 */
public class CursedSprigBlock extends CursedPlantBlock {
    
    public CursedSprigBlock(Settings settings) {
        super(settings, true, false); // Apply wither effect but not poison
    }
    
    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient && entity instanceof LivingEntity livingEntity) {
            // Apply a mild wither effect
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 60, 0));
        }
    }
    
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        super.randomDisplayTick(state, world, pos, random);
        
        if (random.nextInt(20) == 0) {
            // Add dark particles
            world.addParticle(
                ParticleTypes.ASH,
                pos.getX() + random.nextFloat(),
                pos.getY() + 0.3,
                pos.getZ() + random.nextFloat(),
                0.0, 0.02, 0.0
            );
        }
    }
}
