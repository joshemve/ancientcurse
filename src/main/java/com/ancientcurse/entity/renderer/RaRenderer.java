package com.ancientcurse.entity.renderer;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.RaEntity;
import com.ancientcurse.entity.model.RaModel;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import org.joml.Matrix3f;

/**
 * Ra Entity Renderer - The Egyptian Sun God with fire and glow effects
 *
 * Features:
 * - Glowing sun orb that pulses with solar energy
 * - Glowing eyes with divine fire
 * - Fire particles emanating from wings
 * - Fire breath particles from beak
 * - Solar crown fire effects
 * - Chest glow effects
 */
public class RaRenderer extends GeoEntityRenderer<RaEntity> {

    // Particle spawn rate control
    private int particleTick = 0;

    public RaRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new RaModel());

        // Boss-appropriate shadow size
        this.shadowRadius = 1.2f;

        // Add glowing render layers for divine fire aura
        addRenderLayer(new DivineFireAuraLayer(this));
        addRenderLayer(new SolarCoronaLayer(this));

        // Add wing fire particle layer - spawns particles from animated bone positions
        addRenderLayer(new WingFireParticleLayer(this));

        // Add sun orb specific multi-layer glow with radial light rays
        addRenderLayer(new SunOrbGlowLayer(this));
    }

    @Override
    public void render(RaEntity entity, float entityYaw, float partialTick, MatrixStack poseStack,
            VertexConsumerProvider bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        // Spawn particles on client side
        if (entity.getWorld().isClient) {
            spawnParticles(entity, partialTick);
        }
    }

    /**
     * Spawn fire and flame particles from all locator points
     */
    private void spawnParticles(RaEntity entity, float partialTick) {
        particleTick++;

        // Get the model to access bone positions
        BakedGeoModel model = this.getGeoModel().getBakedModel(this.getGeoModel().getModelResource(entity));
        if (model == null)
            return;

        // Entity position and rotation (interpolated for smoothness)
        double entityX = MathHelper.lerp(partialTick, entity.prevX, entity.getX());
        double entityY = MathHelper.lerp(partialTick, entity.prevY, entity.getY());
        double entityZ = MathHelper.lerp(partialTick, entity.prevZ, entity.getZ());
        float bodyYaw = MathHelper.lerpAngleDegrees(partialTick, entity.prevBodyYaw, entity.bodyYaw);

        // Wing fire particles are now handled by WingFireParticleLayer for proper bone
        // tracking

        // Sun orb particles - constant solar energy
        if (particleTick % 3 == 0) {
            spawnSunOrbParticles(entity, model, entityX, entityY, entityZ, bodyYaw);
        }

        // Sun crown fire particles
        if (particleTick % 4 == 0) {
            spawnSunCrownParticles(entity, model, entityX, entityY, entityZ, bodyYaw);
        }

        // Eye glow particles - occasional divine sparks
        if (particleTick % 8 == 0) {
            spawnEyeParticles(entity, model, entityX, entityY, entityZ, bodyYaw);
        }

        // Chest glow particles
        if (particleTick % 6 == 0) {
            spawnChestGlowParticles(entity, model, entityX, entityY, entityZ, bodyYaw);
        }

        // Occasional fire breath
        if (particleTick % 10 == 0 && entity.getRandom().nextFloat() < 0.3f) {
            spawnFireBreathParticles(entity, model, entityX, entityY, entityZ, bodyYaw);
        }
    }

    /**
     * Spawn solar particles around the sun orb - enhanced with orbiting particles,
     * solar prominences, and radial sparks for realistic sun effect
     */
    private void spawnSunOrbParticles(RaEntity entity, BakedGeoModel model,
            double entityX, double entityY, double entityZ, float bodyYaw) {
        Vec3d worldPos = getBoneWorldPos(entity, "sun_orb", model, entityX, entityY, entityZ, bodyYaw);

        // === ORBITING PARTICLES ===
        // 6 particles in elliptical orbits around the sun orb
        for (int i = 0; i < 6; i++) {
            float orbitAngle = (entity.age * 0.3f) + (i * (360f / 6));
            double orbitRadius = 0.35 + Math.sin(entity.age * 0.1) * 0.1;
            double offsetX = Math.cos(Math.toRadians(orbitAngle)) * orbitRadius;
            double offsetZ = Math.sin(Math.toRadians(orbitAngle)) * orbitRadius;
            // Figure-8 orbit pattern on Y axis
            double offsetY = Math.sin(Math.toRadians(orbitAngle * 2)) * 0.15;

            entity.getWorld().addParticle(
                    ParticleTypes.FLAME,
                    worldPos.x + offsetX,
                    worldPos.y + offsetY,
                    worldPos.z + offsetZ,
                    offsetX * 0.02,
                    0.01,
                    offsetZ * 0.02);
        }

        // === RADIAL SPARKS (like reference image - shooting outward) ===
        // END_ROD particles for the bright white/yellow sparks
        if (entity.getRandom().nextFloat() < 0.4f) {
            double angle = entity.getRandom().nextDouble() * Math.PI * 2;
            double elevation = (entity.getRandom().nextDouble() - 0.5) * Math.PI * 0.8;
            double speed = 0.08 + entity.getRandom().nextDouble() * 0.12;

            double velX = Math.cos(angle) * Math.cos(elevation) * speed;
            double velY = Math.sin(elevation) * speed;
            double velZ = Math.sin(angle) * Math.cos(elevation) * speed;

            entity.getWorld().addParticle(
                    ParticleTypes.END_ROD,
                    worldPos.x,
                    worldPos.y,
                    worldPos.z,
                    velX, velY, velZ);
        }

        // Additional flame sparks shooting outward
        if (entity.getRandom().nextFloat() < 0.3f) {
            double angle = entity.getRandom().nextDouble() * Math.PI * 2;
            double elevation = (entity.getRandom().nextDouble() - 0.5) * Math.PI;
            double speed = 0.06 + entity.getRandom().nextDouble() * 0.08;

            entity.getWorld().addParticle(
                    ParticleTypes.SMALL_FLAME,
                    worldPos.x,
                    worldPos.y,
                    worldPos.z,
                    Math.cos(angle) * Math.cos(elevation) * speed,
                    Math.sin(elevation) * speed,
                    Math.sin(angle) * Math.cos(elevation) * speed);
        }

        // === SOLAR PROMINENCES (rare dramatic arcing bursts) ===
        if (entity.getRandom().nextFloat() < 0.02f) {
            spawnSolarProminence(entity, worldPos);
        }

        // Central lava drip effect for intense heat
        if (entity.getRandom().nextFloat() < 0.3f) {
            entity.getWorld().addParticle(
                    ParticleTypes.LAVA,
                    worldPos.x + (entity.getRandom().nextFloat() - 0.5) * 0.15,
                    worldPos.y + (entity.getRandom().nextFloat() - 0.5) * 0.15,
                    worldPos.z + (entity.getRandom().nextFloat() - 0.5) * 0.15,
                    0, 0, 0);
        }

        // Electric sparks for divine energy
        if (entity.getRandom().nextFloat() < 0.25f) {
            entity.getWorld().addParticle(
                    ParticleTypes.ELECTRIC_SPARK,
                    worldPos.x + (entity.getRandom().nextFloat() - 0.5) * 0.2,
                    worldPos.y + (entity.getRandom().nextFloat() - 0.5) * 0.2,
                    worldPos.z + (entity.getRandom().nextFloat() - 0.5) * 0.2,
                    (entity.getRandom().nextFloat() - 0.5) * 0.15,
                    entity.getRandom().nextFloat() * 0.08,
                    (entity.getRandom().nextFloat() - 0.5) * 0.15);
        }
    }

    /**
     * Spawn a solar prominence - dramatic arcing burst of particles
     */
    private void spawnSolarProminence(RaEntity entity, Vec3d worldPos) {
        // Random direction for the prominence
        double baseAngle = entity.getRandom().nextDouble() * Math.PI * 2;
        double arcHeight = 0.5 + entity.getRandom().nextDouble() * 0.5;

        // Spawn 15-20 particles in an arc
        int particleCount = 15 + entity.getRandom().nextInt(6);
        for (int i = 0; i < particleCount; i++) {
            double progress = (double) i / particleCount;
            // Parabolic arc
            double height = Math.sin(progress * Math.PI) * arcHeight;
            double distance = progress * 0.8;

            double offsetX = Math.cos(baseAngle) * distance;
            double offsetZ = Math.sin(baseAngle) * distance;

            // Velocity follows the arc tangent
            double velMult = 0.03;
            double velX = Math.cos(baseAngle) * velMult;
            double velY = (progress < 0.5 ? 1 : -1) * velMult * 1.5;
            double velZ = Math.sin(baseAngle) * velMult;

            entity.getWorld().addParticle(
                    ParticleTypes.FLAME,
                    worldPos.x + offsetX,
                    worldPos.y + height,
                    worldPos.z + offsetZ,
                    velX, velY, velZ);
        }
    }

    /**
     * Spawn fire particles from the sun crown
     */
    private void spawnSunCrownParticles(RaEntity entity, BakedGeoModel model,
            double entityX, double entityY, double entityZ, float bodyYaw) {
        Vec3d worldPos = getBoneWorldPos(entity, "head", model, entityX, entityY, entityZ, bodyYaw);
        // Adjustment to reach the crown area from the head pivot
        worldPos = worldPos.add(0, 1.2, 0);

        // Majestic upward flames from the crown
        for (int i = 0; i < 2; i++) {
            double offsetX = (entity.getRandom().nextFloat() - 0.5) * 0.4;
            double offsetZ = (entity.getRandom().nextFloat() - 0.5) * 0.4;

            entity.getWorld().addParticle(
                    ParticleTypes.FLAME,
                    worldPos.x + offsetX,
                    worldPos.y,
                    worldPos.z + offsetZ,
                    offsetX * 0.02,
                    0.08 + entity.getRandom().nextFloat() * 0.05,
                    offsetZ * 0.02);
        }
        // Add enchanting glow effect
        if (entity.getRandom().nextFloat() < 0.3f) {
            entity.getWorld().addParticle(
                    ParticleTypes.END_ROD,
                    worldPos.x + (entity.getRandom().nextFloat() - 0.5) * 0.3,
                    worldPos.y + entity.getRandom().nextFloat() * 0.2,
                    worldPos.z + (entity.getRandom().nextFloat() - 0.5) * 0.3,
                    0, 0.02, 0);
        }
    }

    /**
     * Spawn divine glow particles from eyes
     */
    private void spawnEyeParticles(RaEntity entity, BakedGeoModel model,
            double entityX, double entityY, double entityZ, float bodyYaw) {
        Vec3d worldPos = getBoneWorldPos(entity, "head", model, entityX, entityY, entityZ, bodyYaw);

        // Offset from head center to eyes (eyes are roughly forward and side-by-side)
        // We'll spawn around the head pivot for now, but tracking the head's rotation

        // Divine spark from eyes
        entity.getWorld().addParticle(
                ParticleTypes.FLAME,
                worldPos.x,
                worldPos.y,
                worldPos.z,
                (entity.getRandom().nextFloat() - 0.5) * 0.02,
                0.01,
                -0.03 // Slightly forward
        );

        // Occasional bright end rod for divine glow
        if (entity.getRandom().nextFloat() < 0.4f) {
            entity.getWorld().addParticle(
                    ParticleTypes.END_ROD,
                    worldPos.x,
                    worldPos.y,
                    worldPos.z,
                    0, 0.01, -0.02);
        }
    }

    /**
     * Spawn chest glow particles
     */
    private void spawnChestGlowParticles(RaEntity entity, BakedGeoModel model,
            double entityX, double entityY, double entityZ, float bodyYaw) {
        Vec3d worldPos = getBoneWorldPos(entity, "chest", model, entityX, entityY, entityZ, bodyYaw);
        // Chest locator adjustment
        worldPos = worldPos.add(0, -0.2, -0.6);

        // Warm glow from chest
        entity.getWorld().addParticle(
                ParticleTypes.FLAME,
                worldPos.x + (entity.getRandom().nextFloat() - 0.5) * 0.2,
                worldPos.y + (entity.getRandom().nextFloat() - 0.5) * 0.1,
                worldPos.z,
                0, 0.01, -0.01);

        // Heart-beat like pulse with lava
        if (entity.getRandom().nextFloat() < 0.2f) {
            entity.getWorld().addParticle(
                    ParticleTypes.LAVA,
                    worldPos.x,
                    worldPos.y,
                    worldPos.z,
                    0, 0, 0);
        }
    }

    /**
     * Spawn fire breath particles from beak
     */
    private void spawnFireBreathParticles(RaEntity entity, BakedGeoModel model,
            double entityX, double entityY, double entityZ, float bodyYaw) {
        Vec3d worldPos = getBoneWorldPos(entity, "head", model, entityX, entityY, entityZ, bodyYaw);
        // Beak adjustment
        worldPos = worldPos.add(0, 0.8, -1.0);

        // Calculate forward direction based on entity rotation
        double radians = Math.toRadians(bodyYaw);
        double forwardX = -Math.sin(radians);
        double forwardZ = Math.cos(radians);

        // Burst of fire breath
        for (int i = 0; i < 5; i++) {
            double speed = 0.1 + entity.getRandom().nextFloat() * 0.1;
            double spread = (entity.getRandom().nextFloat() - 0.5) * 0.1;

            entity.getWorld().addParticle(
                    ParticleTypes.FLAME,
                    worldPos.x,
                    worldPos.y + (entity.getRandom().nextFloat() - 0.5) * 0.1,
                    worldPos.z,
                    forwardX * speed + spread,
                    (entity.getRandom().nextFloat() - 0.5) * 0.02,
                    forwardZ * speed + spread);
        }

        // Add smoke trail
        entity.getWorld().addParticle(
                ParticleTypes.SMOKE,
                worldPos.x,
                worldPos.y,
                worldPos.z,
                forwardX * 0.05,
                0.01,
                forwardZ * 0.05);
    }

    /**
     * Get the world-space position of a bone, accounting for Geckolib animations
     * and entity state.
     */
    public Vec3d getBoneWorldPos(RaEntity entity, String boneName, BakedGeoModel model,
            double entityX, double entityY, double entityZ, float bodyYaw) {
        return model.getBone(boneName).map(bone -> {
            // Get the recursive transformations starting from the bone up to the root
            Matrix4f matrix = new Matrix4f();
            matrix.translation((float) entityX, (float) entityY, (float) entityZ);

            // Apply entity rotation (body yaw)
            matrix.rotateY((float) Math.toRadians(-bodyYaw));

            // Apply cumulative bone transformations
            Matrix4f boneMatrix = transformRecursively(bone);
            matrix.mul(boneMatrix);

            // Extract the resulting world position
            Vector4f worldPos = new Vector4f(0, 0, 0, 1).mul(matrix);
            return new Vec3d(worldPos.x(), worldPos.y(), worldPos.z());
        }).orElse(new Vec3d(entityX, entityY, entityZ));
    }

    /**
     * Recursively build the transformation matrix for a bone by traversing up to
     * the model root.
     */
    private Matrix4f transformRecursively(GeoBone bone) {
        Matrix4f matrix = new Matrix4f();

        if (bone.getParent() != null) {
            matrix.mul(transformRecursively(bone.getParent()));
        }

        // Translate to bone's pivot and animated position
        matrix.translate(bone.getPivotX() / 16f, bone.getPivotY() / 16f, bone.getPivotZ() / 16f);

        // Apply rotations
        matrix.rotateZ(bone.getRotZ());
        matrix.rotateY(bone.getRotY());
        matrix.rotateX(bone.getRotX());

        // Apply scaling
        matrix.scale(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ());

        // Translate back from pivot
        matrix.translate(-bone.getPivotX() / 16f, -bone.getPivotY() / 16f, -bone.getPivotZ() / 16f);

        // Apply the animated displacement (offset)
        matrix.translate(bone.getPosX() / 16f, bone.getPosY() / 16f, bone.getPosZ() / 16f);

        return matrix;
    }

    /**
     * Divine fire aura render layer - creates a pulsing emissive glow around Ra
     * Uses the main texture with additive blending for a sun god radiance effect
     */
    private class DivineFireAuraLayer extends GeoRenderLayer<RaEntity> {
        public DivineFireAuraLayer(GeoEntityRenderer<RaEntity> entityRenderer) {
            super(entityRenderer);
        }

        @Override
        public void render(MatrixStack poseStack, RaEntity entity, BakedGeoModel bakedModel,
                RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer,
                float partialTick, int packedLight, int packedOverlay) {

            // Calculate pulsing divine aura effect
            float time = entity.age + partialTick;
            float pulse = 0.15f + MathHelper.sin(time * 0.1f) * 0.05f;
            float warmth = 0.9f + MathHelper.sin(time * 0.08f) * 0.1f;

            // Use emissive render layer with the main texture for divine glow
            // RenderLayer.getEyes ignores lighting, creating true emissive effect
            VertexConsumer emissiveBuffer = bufferSource.getBuffer(
                    RenderLayer.getEyes(getTextureLocation(entity)));

            // Render with warm golden-orange tint at low alpha for subtle divine radiance
            // This creates a sun-like aura without overpowering the main texture
            getRenderer().reRender(bakedModel, poseStack, bufferSource, entity, renderType,
                    emissiveBuffer, partialTick, 15728880, OverlayTexture.DEFAULT_UV,
                    1.0f * warmth, 0.7f * warmth, 0.2f, pulse);
        }
    }

    /**
     * Solar corona render layer - secondary glow with different timing for depth
     */
    private class SolarCoronaLayer extends GeoRenderLayer<RaEntity> {
        public SolarCoronaLayer(GeoEntityRenderer<RaEntity> entityRenderer) {
            super(entityRenderer);
        }

        @Override
        public void render(MatrixStack poseStack, RaEntity entity, BakedGeoModel bakedModel,
                RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer,
                float partialTick, int packedLight, int packedOverlay) {

            // Calculate secondary pulse with offset timing for layered glow effect
            float time = entity.age + partialTick;
            float pulse = 0.08f + MathHelper.sin(time * 0.15f + 1.5f) * 0.04f;

            // Use translucent emissive for softer outer glow
            VertexConsumer emissiveBuffer = bufferSource.getBuffer(
                    RenderLayer.getEntityTranslucentEmissive(getTextureLocation(entity), true));

            // Render with slightly cooler orange for corona effect
            getRenderer().reRender(bakedModel, poseStack, bufferSource, entity, renderType,
                    emissiveBuffer, partialTick, 15728880, OverlayTexture.DEFAULT_UV,
                    1.0f, 0.5f, 0.15f, pulse);
        }
    }

    /**
     * Wing fire particle layer - spawns elegant trailing fire from wing positions
     * Uses model locator coordinates for accurate placement
     */
    private class WingFireParticleLayer extends GeoRenderLayer<RaEntity> {
        // Wing tip locator positions (from ra.geo.json, in pixels)
        // These are the actual fire_wing_particles locators at the wing tips
        private static final float[][] WING_TIP_LOCATORS = {
                { 77f, 52f, 10.75f }, // fire_wing_particles10 - left wing tip
                { -77f, 51f, 10.75f } // fire_wing_particles9 - right wing tip
        };

        // Mid-wing locator positions for subtle embers
        private static final float[][] WING_MID_LOCATORS = {
                { 51f, 33f, 10.75f }, // fire_wing_particles6 - left mid
                { -47f, 31f, 10.75f } // fire_wing_particles3 - right mid
        };

        private int tickCounter = 0;

        public WingFireParticleLayer(GeoEntityRenderer<RaEntity> entityRenderer) {
            super(entityRenderer);
        }

        @Override
        public void render(MatrixStack poseStack, RaEntity entity, BakedGeoModel bakedModel,
                RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer,
                float partialTick, int packedLight, int packedOverlay) {

            if (!entity.getWorld().isClient)
                return;

            tickCounter++;

            float bodyYaw = entity.bodyYaw;

            // Enhanced Wing Fire - Using all 10 locators for full coverage
            if (tickCounter % 2 == 0) { // Faster spawning for higher density
                String[] allWingBones = {
                        "fire_wing_particles", "fire_wing_particles2", "fire_wing_particles3",
                        "fire_wing_particles4", "fire_wing_particles5", "fire_wing_particles6",
                        "fire_wing_particles7", "fire_wing_particles8", "fire_wing_particles9",
                        "fire_wing_particles10"
                };

                for (String boneName : allWingBones) {
                    Vec3d worldPos = ((RaRenderer) getRenderer()).getBoneWorldPos(entity, boneName, bakedModel,
                            entity.getX(), entity.getY(), entity.getZ(), bodyYaw);

                    // Multiple particles per locator with jitter to fill the gaps
                    for (int i = 0; i < 2; i++) {
                        double jitterX = (entity.getRandom().nextFloat() - 0.5) * 0.6;
                        double jitterY = (entity.getRandom().nextFloat() - 0.5) * 0.4;
                        double jitterZ = (entity.getRandom().nextFloat() - 0.5) * 0.6;

                        Vec3d jitterPos = worldPos.add(jitterX, jitterY, jitterZ);
                        spawnTrailingFlame(entity, jitterPos, boneName.contains("10") || boneName.contains("9"));

                        // Occasional large smoke for intensity
                        if (entity.getRandom().nextFloat() < 0.15f) {
                            entity.getWorld().addParticle(
                                    ParticleTypes.LARGE_SMOKE,
                                    jitterPos.x, jitterPos.y, jitterPos.z,
                                    0, 0.02, 0);
                        }
                    }
                }
            }
        }

        // locatorToWorld removed as it's replaced by getBoneWorldPos in the parent
        // renderer

        /**
         * Spawn trailing flame - elegant fire streaming from wing tips
         */
        private void spawnTrailingFlame(RaEntity entity, Vec3d pos, boolean isTip) {
            double offsetX = (entity.getRandom().nextFloat() - 0.5) * 0.15;
            double offsetY = (entity.getRandom().nextFloat() - 0.5) * 0.1;
            double offsetZ = (entity.getRandom().nextFloat() - 0.5) * 0.15;

            // Main flame - gentle upward drift
            entity.getWorld().addParticle(
                    ParticleTypes.FLAME,
                    pos.x + offsetX,
                    pos.y + offsetY,
                    pos.z + offsetZ,
                    (entity.getRandom().nextFloat() - 0.5) * 0.01,
                    0.025 + entity.getRandom().nextFloat() * 0.02,
                    (entity.getRandom().nextFloat() - 0.5) * 0.01);

            // Rare lava drip for intensity
            if (isTip && entity.getRandom().nextFloat() < 0.1f) {
                entity.getWorld().addParticle(
                        ParticleTypes.LAVA,
                        pos.x, pos.y, pos.z,
                        0, 0, 0);
            }
        }

        /**
         * Spawn subtle ember
         */
        private void spawnEmber(RaEntity entity, Vec3d pos) {
            double offsetX = (entity.getRandom().nextFloat() - 0.5) * 0.25;
            double offsetY = (entity.getRandom().nextFloat() - 0.5) * 0.15;
            double offsetZ = (entity.getRandom().nextFloat() - 0.5) * 0.25;

            entity.getWorld().addParticle(
                    ParticleTypes.SMALL_FLAME,
                    pos.x + offsetX,
                    pos.y + offsetY,
                    pos.z + offsetZ,
                    (entity.getRandom().nextFloat() - 0.5) * 0.005,
                    0.015 + entity.getRandom().nextFloat() * 0.01,
                    (entity.getRandom().nextFloat() - 0.5) * 0.005);
        }
    }
}
