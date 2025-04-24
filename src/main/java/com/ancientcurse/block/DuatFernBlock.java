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
 * A mystical fern from the Egyptian underworld (Duat)
 * Applies weakness and mining fatigue to entities that touch it
 */
public class DuatFernBlock extends CursedPlantBlock {
    
    public DuatFernBlock(Settings settings) {
        super(settings, false, false); // No wither or poison by default
    }
    
    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient && entity instanceof LivingEntity livingEntity) {
            // Apply weakness and mining fatigue
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 200, 0));
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 200, 0));
        }
    }
    
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        super.randomDisplayTick(state, world, pos, random);
        
        if (random.nextInt(15) == 0) {
            // Add mystical particles
            world.addParticle(
                ParticleTypes.SOUL,
                pos.getX() + random.nextFloat(),
                pos.getY() + 0.5,
                pos.getZ() + random.nextFloat(),
                0.0, 0.05, 0.0
            );
        }
    }
}
