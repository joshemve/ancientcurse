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
 * A glowing pod plant that provides night vision to entities that touch it
 */
public class KhemnuPodBlock extends EgyptianPlantBlock {
    
    public KhemnuPodBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient && entity instanceof LivingEntity livingEntity) {
            // Apply night vision effect
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 300, 0, false, false));
        }
    }
    
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        super.randomDisplayTick(state, world, pos, random);
        
        if (random.nextInt(10) == 0) {
            // Add glowing particles
            world.addParticle(
                ParticleTypes.END_ROD,
                pos.getX() + random.nextFloat(),
                pos.getY() + 0.5,
                pos.getZ() + random.nextFloat(),
                0.0, 0.05, 0.0
            );
        }
    }
}
