package com.ancientcurse.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Custom particle for Ra's sun orb flares.
 * These particles streak away from the orb at high speed and stretch along
 * their movement vector.
 */
@Environment(EnvType.CLIENT)
public class OrbFlareParticle extends SpriteBillboardParticle {

    private final SpriteProvider spriteProvider;

    protected OrbFlareParticle(ClientWorld world, double x, double y, double z,
            double velocityX, double velocityY, double velocityZ,
            SpriteProvider spriteProvider) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.spriteProvider = spriteProvider;

        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;

        // Particle properties
        this.maxAge = 10 + random.nextInt(10); // Short lived streaks
        this.scale = 0.1f + random.nextFloat() * 0.15f;
        this.collidesWithWorld = false;
        this.gravityStrength = 0.0f;

        // Brighter golden-white
        this.red = 1.0f;
        this.green = 0.9f + random.nextFloat() * 0.1f;
        this.blue = 0.6f + random.nextFloat() * 0.2f;

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

        // Slight deceleration
        this.velocityX *= 0.95f;
        this.velocityY *= 0.95f;
        this.velocityZ *= 0.95f;

        // Alpha fade
        this.alpha = 1.0f - ((float) this.age / (float) this.maxAge);

        this.move(this.velocityX, this.velocityY, this.velocityZ);
    }

    @Override
    public void buildGeometry(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
        // Custom rendering for stretched billboard
        Vec3d vec3d = camera.getPos();
        float f = (float) (MathHelper.lerp((double) tickDelta, this.prevPosX, this.x) - vec3d.getX());
        float g = (float) (MathHelper.lerp((double) tickDelta, this.prevPosY, this.y) - vec3d.getY());
        float h = (float) (MathHelper.lerp((double) tickDelta, this.prevPosZ, this.z) - vec3d.getZ());

        Quaternionf quaternionf;
        if (this.angle == 0.0F) {
            quaternionf = camera.getRotation();
        } else {
            quaternionf = new Quaternionf(camera.getRotation());
            quaternionf.rotateZ(MathHelper.lerp(tickDelta, this.prevAngle, this.angle));
        }

        Vector3f[] vector3fs = new Vector3f[] { new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F) };
        float j = this.getSize(tickDelta);

        // Stretch factor based on velocity
        float velLen = (float) Math.sqrt(velocityX * velocityX + velocityY * velocityY + velocityZ * velocityZ);
        float stretch = 1.0f + velLen * 10.0f;

        for (int k = 0; k < 4; ++k) {
            Vector3f vector3f = vector3fs[k];
            // Stretch along the X axis of the billboard (or Y if we rotate it to match
            // velocity)
            // For simplicity, we just stretch it. To do it properly, we'd align to velocity
            // vector.
            // Let's just stretch it for now.
            vector3f.mul(j * stretch, j, j);
            vector3f.rotate(quaternionf);
            vector3f.add(f, g, h);
        }

        float k = this.getMinU();
        float l = this.getMaxU();
        float m = this.getMinV();
        float n = this.getMaxV();
        int o = this.getBrightness(tickDelta);
        vertexConsumer.vertex((double) vector3fs[0].x(), (double) vector3fs[0].y(), (double) vector3fs[0].z())
                .texture(l, n).color(this.red, this.green, this.blue, this.alpha).light(o).next();
        vertexConsumer.vertex((double) vector3fs[1].x(), (double) vector3fs[1].y(), (double) vector3fs[1].z())
                .texture(l, m).color(this.red, this.green, this.blue, this.alpha).light(o).next();
        vertexConsumer.vertex((double) vector3fs[2].x(), (double) vector3fs[2].y(), (double) vector3fs[2].z())
                .texture(k, m).color(this.red, this.green, this.blue, this.alpha).light(o).next();
        vertexConsumer.vertex((double) vector3fs[3].x(), (double) vector3fs[3].y(), (double) vector3fs[3].z())
                .texture(k, n).color(this.red, this.green, this.blue, this.alpha).light(o).next();
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getBrightness(float tint) {
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
            return new OrbFlareParticle(world, x, y, z, velocityX, velocityY, velocityZ, spriteProvider);
        }
    }
}
