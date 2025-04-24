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
 * A cap-shaped plant named after the Egyptian funerary god Duamutef
 * Provides resistance to entities that touch it
 */
public class DuamutefCapBlock extends EgyptianPlantBlock {
    
    public DuamutefCapBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient && entity instanceof LivingEntity livingEntity) {
            // Apply resistance effect
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 0, false, false));
        }
    }
    
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        super.randomDisplayTick(state, world, pos, random);
        
        if (random.nextInt(20) == 0) {
            // Add dust particles
            world.addParticle(
                ParticleTypes.ENCHANT,
                pos.getX() + random.nextFloat(),
                pos.getY() + 0.5,
                pos.getZ() + random.nextFloat(),
                0.0, 0.02, 0.0
            );
        }
    }
}
