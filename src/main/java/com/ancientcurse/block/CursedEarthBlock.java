package com.ancientcurse.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.particle.DustParticleEffect;
import org.joml.Vector3f;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

/**
 * Cursed Earth - a corrupted soil that slowly transitions between textures and affects nearby entities.
 * Emits a constant subtle glow to enhance the cursed atmosphere.
 */
public class CursedEarthBlock extends Block {
    
    // Particle colors for the cursed effect
    private static final Vector3f CURSED_PARTICLE_COLOR = new Vector3f(0.3f, 0.0f, 0.3f);
    
    // Constant light level for the cursed earth
    private static final int LIGHT_LEVEL = 3;
    
    public CursedEarthBlock(Settings settings) {
        // Add nonOpaque() and a static luminance value
        super(settings
            .nonOpaque()
            .luminance((state) -> LIGHT_LEVEL));
    }
    
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        // Add ambient particles for a cursed effect
        if (random.nextInt(8) == 0) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + 1.1D;
            double z = pos.getZ() + random.nextDouble();
            
            // Use a consistent particle intensity
            float intensity = 0.9f;
            
            world.addParticle(
                new DustParticleEffect(CURSED_PARTICLE_COLOR, intensity),
                x, y, z,
                0, 0.05D, 0
            );
        }
        

    }
    
    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // Apply effects to nearby entities
        if (!world.isClient && random.nextInt(20) == 0) {
            Box box = new Box(pos).expand(3.0);
            List<LivingEntity> entities = world.getNonSpectatingEntities(LivingEntity.class, box);
            
            for (LivingEntity entity : entities) {
                // Apply a brief wither effect to simulate the cursed nature
                if (!(entity instanceof PlayerEntity) || !((PlayerEntity) entity).isCreative()) {
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 60, 0, false, false));
                }
            }
        }
        

    }
}
