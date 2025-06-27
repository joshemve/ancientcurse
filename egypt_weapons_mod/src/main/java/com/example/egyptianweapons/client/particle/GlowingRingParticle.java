package com.example.egyptianweapons.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
public class GlowingRingParticle extends SpriteBillboardParticle {
    private final SpriteProvider spriteProvider;

    protected GlowingRingParticle(ClientWorld world, double x, double y, double z, 
                               double velocityX, double velocityY, double velocityZ,
                               SpriteProvider spriteProvider) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.spriteProvider = spriteProvider;
        this.scale = 1.5F;
        this.maxAge = 20;
        this.collidesWithWorld = false;
        
        // Set particle color (golden glow)
        this.setColor(1.0F, 0.9F, 0.5F);
        
        // Set the sprite using a random sprite from the provider
        this.setSprite(spriteProvider.getSprite(world.getRandom()));
    }

    @Override
    public void tick() {
        super.tick();
        float lifeProgress = (float) this.age / this.maxAge;
        this.alpha = 1.0F - lifeProgress;  // Fade out effect
        
        // Add a slight pulsing effect to the scale
        float pulseScale = 1.0F + 0.2F * (float)Math.sin(lifeProgress * Math.PI * 2.0F);
        this.scale = 1.5F * pulseScale;

        // Update sprite for animation
        this.setSpriteForAge(this.spriteProvider);
        
        if (this.age >= this.maxAge) {
            this.markDead();
        }
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(DefaultParticleType parameters, ClientWorld world, 
                                   double x, double y, double z,
                                   double velocityX, double velocityY, double velocityZ) {
            return new GlowingRingParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider);
        }
    }
}
