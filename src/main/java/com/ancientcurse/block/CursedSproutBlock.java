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
 * A young cursed plant sprout that applies hunger and nausea effects
 */
public class CursedSproutBlock extends CursedPlantBlock {
    
    public CursedSproutBlock(Settings settings) {
        super(settings, false, false); // No wither or poison by default
    }
    
    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient && entity instanceof LivingEntity livingEntity) {
            // Apply hunger and nausea
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 200, 0));
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 160, 0));
        }
    }
    
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        super.randomDisplayTick(state, world, pos, random);
        
        if (random.nextInt(20) == 0) {
            // Add sickly particles
            world.addParticle(
                ParticleTypes.MYCELIUM,
                pos.getX() + random.nextFloat(),
                pos.getY() + 0.3,
                pos.getZ() + random.nextFloat(),
                0.0, 0.02, 0.0
            );
        }
    }
}
