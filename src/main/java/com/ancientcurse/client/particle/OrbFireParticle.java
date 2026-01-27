package com.ancientcurse.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

/**
 * Custom particle for Ra's sun orb base glow.
 * These particles swirl around a point and pulse in size.
 */
@Environment(EnvType.CLIENT)
public class OrbFireParticle extends SpriteBillboardParticle {

    private final SpriteProvider spriteProvider;
    private final float centerX, centerY, centerZ;
    private float orbitAngle;
    private final float orbitRadius;
    private final float orbitSpeed;

    protected OrbFireParticle(ClientWorld world, double x, double y, double z,
            double velocityX, double velocityY, double velocityZ,
            SpriteProvider spriteProvider) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.spriteProvider = spriteProvider;

        // Use initial position as center for orbit
        this.centerX = (float) x;
        this.centerY = (float) y;
        this.centerZ = (float) z;

        // Randomize orbit properties
        this.orbitAngle = random.nextFloat() * (float) Math.PI * 2;
        this.orbitRadius = 0.15f + random.nextFloat() * 0.25f;
        this.orbitSpeed = 0.1f + random.nextFloat() * 0.15f;

        // Particle properties
        this.maxAge = 15 + random.nextInt(15);
        this.scale = 0.2f + random.nextFloat() * 0.2f;
        this.collidesWithWorld = false;
        this.gravityStrength = 0.0f;
        this.alpha = 0.0f; // Start transparent and fade in

        // Golden colors
        this.red = 1.0f;
        this.green = 0.8f + random.nextFloat() * 0.2f;
        this.blue = 0.2f + random.nextFloat() * 0.3f;

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

        this.setSpriteForAge(spriteProvider);

        // Orbital motion
        this.orbitAngle += this.orbitSpeed;
        float targetX = centerX + (float) Math.cos(orbitAngle) * orbitRadius;
        float targetZ = centerZ + (float) Math.sin(orbitAngle) * orbitRadius;

        // Move towards orbital target
        this.velocityX = (targetX - this.x) * 0.2f;
        this.velocityZ = (targetZ - this.z) * 0.2f;
        this.velocityY += 0.01f; // Slight upward drift

        // Fade in and out
        if (this.age < this.maxAge * 0.2f) {
            this.alpha = (float) this.age / (this.maxAge * 0.2f);
        } else if (this.age > this.maxAge * 0.6f) {
            this.alpha = 1.0f - ((float) (this.age - this.maxAge * 0.6f) / (this.maxAge * 0.4f));
        } else {
            this.alpha = 1.0f;
        }

        this.move(this.velocityX, this.velocityY, this.velocityZ);
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getBrightness(float tint) {
        return 240; // Fullbright
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
            return new OrbFireParticle(world, x, y, z, velocityX, velocityY, velocityZ, spriteProvider);
        }
    }
}
