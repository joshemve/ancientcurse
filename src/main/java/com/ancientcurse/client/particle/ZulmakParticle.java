package com.ancientcurse.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

/**
 * Custom particle for Zulmak's spinning blocks effect.
 * These particles orbit around Zulmak and fade over time.
 */
@Environment(EnvType.CLIENT)
public class ZulmakParticle extends SpriteBillboardParticle {

    private final SpriteProvider spriteProvider;

    protected ZulmakParticle(ClientWorld world, double x, double y, double z,
                             double velocityX, double velocityY, double velocityZ,
                             SpriteProvider spriteProvider) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.spriteProvider = spriteProvider;

        // Set velocity for orbital motion
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;

        // Particle properties
        this.maxAge = 20 + random.nextInt(10); // 1-1.5 seconds lifetime
        this.scale = 0.15f + random.nextFloat() * 0.1f; // Slightly varied size
        this.collidesWithWorld = false;

        // No gravity - particles float
        this.gravityStrength = 0.0f;

        // Start with full alpha
        this.alpha = 1.0f;

        // Set sprite
        this.setSpriteForAge(spriteProvider);
    }

    @Override
    public void tick() {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;

        if (this.age++ >= this.maxAge) {
            this.markDead();
            return;
        }

        // Update sprite for animation
        this.setSpriteForAge(spriteProvider);

        // Fade out in last quarter of life
        if (this.age > this.maxAge * 0.75f) {
            float fadeProgress = (this.age - this.maxAge * 0.75f) / (this.maxAge * 0.25f);
            this.alpha = 1.0f - fadeProgress;
        }

        // Move particle (orbital velocity is set when spawning)
        this.move(this.velocityX, this.velocityY, this.velocityZ);
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getBrightness(float tint) {
        // Make particles glow slightly (higher brightness)
        return 240;
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(DefaultParticleType type, ClientWorld world,
                                       double x, double y, double z,
                                       double velocityX, double velocityY, double velocityZ) {
            return new ZulmakParticle(world, x, y, z, velocityX, velocityY, velocityZ, spriteProvider);
        }
    }
}
