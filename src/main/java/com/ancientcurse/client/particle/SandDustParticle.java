package com.ancientcurse.client.particle;

import com.ancientcurse.client.SandstormClientHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

@Environment(EnvType.CLIENT)
public class SandDustParticle extends SpriteBillboardParticle {
    private final SpriteProvider spriteProvider;

    // Per-particle turbulence offsets for natural variation
    private final float turbulenceOffsetX;
    private final float turbulenceOffsetZ;
    private final float turbulenceSpeed;
    private final float baseVelocityX;
    private final float baseVelocityZ;

    protected SandDustParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, SpriteProvider spriteProvider) {
        super(world, x, y, z);
        this.spriteProvider = spriteProvider;
        this.velocityMultiplier = 0.98F; // Slightly less drag for longer travel
        this.velocityX = vx;
        this.velocityY = vy;
        this.velocityZ = vz;

        // Store base velocity for wind following
        this.baseVelocityX = (float)vx;
        this.baseVelocityZ = (float)vz;

        // Unique turbulence pattern per particle
        this.turbulenceOffsetX = random.nextFloat() * 1000f;
        this.turbulenceOffsetZ = random.nextFloat() * 1000f;
        this.turbulenceSpeed = 0.05f + random.nextFloat() * 0.05f;

        // Variable size - mix of small and large particles
        float sizeVariation = random.nextFloat();
        if (sizeVariation < 0.3f) {
            // Small particles (30%)
            this.scale *= 1.0F + random.nextFloat() * 1.0F;
        } else if (sizeVariation < 0.8f) {
            // Medium particles (50%)
            this.scale *= 2.0F + random.nextFloat() * 1.5F;
        } else {
            // Large dust clouds (20%)
            this.scale *= 3.5F + random.nextFloat() * 2.0F;
        }

        // Longer lifetime for more visible streams
        this.maxAge = (int)(80.0D / (random.nextDouble() * 0.4D + 0.6D));

        // Sand color with variation - Warm Tan to Orange
        float shade = random.nextFloat() * 0.2F + 0.8F;
        float hueShift = random.nextFloat() * 0.1F - 0.05F;
        this.red = Math.min(1.0F, (0.85F + hueShift) * shade);
        this.green = (0.68F - hueShift * 0.5F) * shade;
        this.blue = (0.48F - hueShift) * shade;

        this.collidesWithWorld = false; // Optimization
        this.setSpriteForAge(spriteProvider);
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
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

        // Apply dynamic wind influence from SandstormClientHandler
        if (SandstormClientHandler.isRenderingSandstorm()) {
            float windInfluence = 0.15f; // How much current wind affects existing particles
            float currentWindX = SandstormClientHandler.getWindX() * 0.6f;
            float currentWindZ = SandstormClientHandler.getWindZ() * 0.6f;

            // Blend toward current wind direction
            this.velocityX = this.velocityX * (1 - windInfluence) + currentWindX * windInfluence;
            this.velocityZ = this.velocityZ * (1 - windInfluence) + currentWindZ * windInfluence;

            // Add gust effect
            float gust = SandstormClientHandler.getGustStrength();
            if (gust > 0.1f) {
                this.velocityX += currentWindX * gust * 0.3f;
                this.velocityZ += currentWindZ * gust * 0.3f;
                this.velocityY += gust * 0.05f; // Gusts lift particles
            }
        }

        // Add per-particle turbulence (sine wave oscillation)
        float time = this.age * turbulenceSpeed;
        float turbX = (float)Math.sin(time + turbulenceOffsetX) * 0.02f;
        float turbZ = (float)Math.cos(time + turbulenceOffsetZ) * 0.02f;
        float turbY = (float)Math.sin(time * 0.7f) * 0.01f;

        this.velocityX += turbX;
        this.velocityY += turbY;
        this.velocityZ += turbZ;

        // Slight downward drift for realism
        this.velocityY -= 0.002f;

        // Apply velocity with multiplier
        this.velocityX *= this.velocityMultiplier;
        this.velocityY *= this.velocityMultiplier;
        this.velocityZ *= this.velocityMultiplier;

        this.move(this.velocityX, this.velocityY, this.velocityZ);

        this.setSpriteForAge(this.spriteProvider);

        // Smooth fade in and out
        float lifeRatio = (float)this.age / (float)this.maxAge;
        if (lifeRatio < 0.1f) {
            // Fade in over first 10%
            this.alpha = lifeRatio / 0.1f * 0.7f;
        } else if (lifeRatio > 0.7f) {
            // Fade out over last 30%
            this.alpha = (1.0f - lifeRatio) / 0.3f * 0.7f;
        } else {
            this.alpha = 0.7f; // Max alpha (semi-transparent for layering)
        }
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(DefaultParticleType type, ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
            return new SandDustParticle(world, x, y, z, vx, vy, vz, this.spriteProvider);
        }
    }
}
