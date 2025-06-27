package com.example.egyptianweapons.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

@Environment(EnvType.CLIENT)
public class SweepingSlashParticle extends SpriteBillboardParticle {
    private final SpriteProvider spriteProvider;
    private final double startAngle;
    private final double arcRadius;

    protected SweepingSlashParticle(ClientWorld world, double x, double y, double z, 
                                   double velocityX, double velocityY, double velocityZ,
                                   SpriteProvider spriteProvider) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.spriteProvider = spriteProvider;
        this.scale = 1.0F;
        this.maxAge = 12;
        this.collidesWithWorld = false;
        this.startAngle = Math.atan2(velocityZ, velocityX);
        this.arcRadius = Math.sqrt(velocityX * velocityX + velocityZ * velocityZ);
        
        // Set to full brightness and white color to show texture properly
        this.setColor(1.0F, 1.0F, 1.0F);
        
        // Set sprite with animation
        this.setSpriteForAge(spriteProvider);
        
        // Set alpha to fully visible
        this.setAlpha(0.9f);
    }

    @Override
    public void tick() {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;
        
        float lifeProgress = (float) this.age / this.maxAge;
        
        // Fade out towards the end
        if (lifeProgress > 0.7f) {
            this.alpha = 0.9F - ((lifeProgress - 0.7f) / 0.3f);
        }
        
        // Move in an arc pattern
        double angle = this.startAngle + (lifeProgress * Math.PI);
        this.x += Math.cos(angle) * this.arcRadius * 0.1;
        this.z += Math.sin(angle) * this.arcRadius * 0.1;
        this.y += velocityY * 0.1;
        
        // Update sprite for animation
        this.setSpriteForAge(this.spriteProvider);
        
        if (this.age++ >= this.maxAge) {
            this.markDead();
        }
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getBrightness(float tint) {
        return 15728880; // Maximum brightness (15 << 20 | 15 << 4)
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(DefaultParticleType defaultParticleType, ClientWorld clientWorld,
                                     double x, double y, double z,
                                     double velocityX, double velocityY, double velocityZ) {
            return new SweepingSlashParticle(clientWorld, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider);
        }
    }
}
