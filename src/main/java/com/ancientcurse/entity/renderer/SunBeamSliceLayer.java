package com.ancientcurse.entity.renderer;

import com.ancientcurse.client.render.CrescentRayRenderLayer;
import com.ancientcurse.entity.RaEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Sun Ray Slice Render Layer - Directional crescent beam shooting toward target
 *
 * Creates a divine light beam attack:
 * - Crescent ray shoots from Ra toward the target
 * - Orange outer glow with white hot core
 * - Grows as it travels with trailing afterimages
 * - Dramatic "lightbeam slash" visual
 */
public class SunBeamSliceLayer extends GeoRenderLayer<RaEntity> {

    // Textures for the crescent rays
    private static final Identifier SUN_RAY_ORANGE = new Identifier("ancientcurse",
            "textures/entity/sun_ray_orange.png");
    private static final Identifier SUN_RAY_WHITE = new Identifier("ancientcurse",
            "textures/entity/sun_ray_white.png");

    // Beam configuration
    private static final float MIN_SCALE = 4.0f; // Starting size
    private static final float MAX_SCALE = 8.0f; // Max size as beam travels
    private static final double MAX_DISTANCE = 20.0; // Travel distance
    private static final int TRAIL_COUNT = 4; // Afterimage trail count
    private static final float TRAIL_SPACING = 0.08f; // Spacing between trail images

    // Color definitions
    private static final Vector3f ORANGE_TINT = new Vector3f(1.0f, 0.85f, 0.5f); // Warm orange
    private static final Vector3f WHITE_TINT = new Vector3f(1.0f, 1.0f, 0.95f); // Hot white core

    // Ground Smack Timing (Matches RaEntity & RaGroundSmackGoal)
    private static final int GROUND_SMACK_DURATION = 60; // 3.0s total
    private static final int GROUND_SMACK_DELAY = 28; // Firing point at 1.4s (delayed to sync with visual slam)
    private static final int BEAM_DURATION = 30; // How long beam is visible (ticks)

    public SunBeamSliceLayer(GeoEntityRenderer<RaEntity> entityRenderer) {
        super(entityRenderer);
    }

    @Override
    public void render(MatrixStack poseStack, RaEntity entity, BakedGeoModel bakedModel,
            RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer,
            float partialTick, int packedLight, int packedOverlay) {

        // Only render during Ground Smack
        if (entity.getCombatState() != RaEntity.RaCombatState.GROUND_SMACK) {
            return;
        }

        int beamTicks = entity.getSunBeamSliceTicks();
        if (beamTicks <= 0)
            return;

        float smoothTicks = beamTicks - partialTick;
        float elapsed = GROUND_SMACK_DURATION - smoothTicks;

        // Don't render until the delayed firing point (1.4s / 28 ticks)
        if (elapsed < GROUND_SMACK_DELAY) {
            return;
        }

        // Calculate progress of the beam (0.0 to 1.0)
        float activeElapsed = elapsed - GROUND_SMACK_DELAY;
        if (activeElapsed > BEAM_DURATION) {
            return; // Beam has finished
        }

        float travelProgress = MathHelper.clamp(activeElapsed / BEAM_DURATION, 0.0f, 1.0f);

        // Intensity: Quick fade in, gradual fade out
        float intensity;
        if (activeElapsed < 3) {
            intensity = activeElapsed / 3.0f; // Fade in over 3 ticks
        } else if (activeElapsed > BEAM_DURATION - 10) {
            intensity = (BEAM_DURATION - activeElapsed) / 10.0f; // Fade out over last 10 ticks
        } else {
            intensity = 1.0f;
        }

        // Get the beam direction (toward target)
        Vec3d direction = entity.getSunBeamDirection();

        // Validate direction - if zero or pointing straight down, use entity facing
        if (direction.lengthSquared() < 0.01 || (direction.x == 0 && direction.z == 0)) {
            // Use entity's facing direction instead
            float yawRad = (float) Math.toRadians(-entity.getYaw());
            direction = new Vec3d(Math.sin(yawRad), -0.2, Math.cos(yawRad)).normalize();
        }

        // Get sun orb world position as start point
        Vec3d startPos = getOrbWorldPosition(entity, bakedModel, partialTick);

        // Entity position for offset calculation
        double entityX = MathHelper.lerp(partialTick, entity.prevX, entity.getX());
        double entityY = MathHelper.lerp(partialTick, entity.prevY, entity.getY());
        double entityZ = MathHelper.lerp(partialTick, entity.prevZ, entity.getZ());

        // Calculate current beam world position
        Vec3d currentPos = startPos.add(direction.multiply(MAX_DISTANCE * travelProgress));
        Vec3d localOffset = currentPos.subtract(entityX, entityY, entityZ);

        // Calculate yaw to face the direction
        float yaw = (float) Math.atan2(direction.x, direction.z);

        // === RENDER TRAILING AFTERIMAGES ===
        for (int i = TRAIL_COUNT - 1; i >= 0; i--) {
            float trailProgress = travelProgress - (i + 1) * TRAIL_SPACING;
            if (trailProgress <= 0.0f)
                continue;

            Vec3d trailPos = startPos.add(direction.multiply(MAX_DISTANCE * trailProgress));
            Vec3d trailOffset = trailPos.subtract(entityX, entityY, entityZ);

            // Trail scale matches position
            float trailScale = MIN_SCALE + (MAX_SCALE - MIN_SCALE) * trailProgress;

            // Trail fades with distance from main beam
            float trailAlpha = intensity * (0.5f - (i * 0.12f));

            // Render orange trail
            poseStack.push();
            poseStack.translate(trailOffset.x, trailOffset.y, trailOffset.z);
            poseStack.multiply(RotationAxis.POSITIVE_Y.rotation(yaw));
            poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90)); // Face perpendicular to travel
            poseStack.scale(trailScale * 1.1f, trailScale * 0.5f, trailScale);

            renderRay(poseStack, bufferSource, SUN_RAY_ORANGE, ORANGE_TINT, trailAlpha * 0.6f);
            poseStack.pop();
        }

        // === RENDER MAIN BEAM ===
        // Scale grows as beam travels
        float scale = MIN_SCALE + (MAX_SCALE - MIN_SCALE) * travelProgress;
        // Add subtle pulse
        scale += MathHelper.sin(entity.age * 0.5f + activeElapsed * 0.4f) * 0.2f;

        // Render the orange outer beam
        poseStack.push();
        poseStack.translate(localOffset.x, localOffset.y, localOffset.z);
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotation(yaw));
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));
        poseStack.scale(scale * 1.3f, scale * 0.6f, scale);

        renderRay(poseStack, bufferSource, SUN_RAY_ORANGE, ORANGE_TINT, intensity * 0.9f);
        poseStack.pop();

        // Render the white hot core (smaller, brighter, slightly ahead)
        poseStack.push();
        poseStack.translate(localOffset.x, localOffset.y, localOffset.z);
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotation(yaw));
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));
        poseStack.scale(scale * 0.9f, scale * 0.4f, scale * 0.8f);

        renderRay(poseStack, bufferSource, SUN_RAY_WHITE, WHITE_TINT, intensity);
        poseStack.pop();

        // === SPAWN PARTICLES ===
        if (entity.getWorld().isClient && travelProgress > 0.05f && travelProgress < 0.95f) {
            spawnBeamParticles(entity, currentPos, direction, intensity, scale);
        }
    }

    /**
     * Get the world position of Ra's sun orb bone
     */
    private Vec3d getOrbWorldPosition(RaEntity entity, BakedGeoModel model, float partialTick) {
        double entityX = MathHelper.lerp(partialTick, entity.prevX, entity.getX());
        double entityY = MathHelper.lerp(partialTick, entity.prevY, entity.getY());
        double entityZ = MathHelper.lerp(partialTick, entity.prevZ, entity.getZ());
        float bodyYaw = MathHelper.lerpAngleDegrees(partialTick, entity.prevBodyYaw, entity.bodyYaw);

        return model.getBone("sun_orb").map(bone -> {
            Matrix4f matrix = new Matrix4f();
            matrix.translation((float) entityX, (float) entityY, (float) entityZ);
            matrix.rotateY((float) Math.toRadians(-bodyYaw));

            Matrix4f boneMatrix = transformRecursively(bone);
            matrix.mul(boneMatrix);

            Vector4f worldPos = new Vector4f(0, 0, 0, 1).mul(matrix);
            return new Vec3d(worldPos.x(), worldPos.y(), worldPos.z());
        }).orElse(new Vec3d(entityX, entityY + 3.0, entityZ));
    }

    private Matrix4f transformRecursively(GeoBone bone) {
        Matrix4f matrix = new Matrix4f();

        if (bone.getParent() != null) {
            matrix.mul(transformRecursively(bone.getParent()));
        }

        matrix.translate(bone.getPivotX() / 16f, bone.getPivotY() / 16f, bone.getPivotZ() / 16f);
        matrix.rotateZ(bone.getRotZ());
        matrix.rotateY(bone.getRotY());
        matrix.rotateX(bone.getRotX());
        matrix.scale(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ());
        matrix.translate(-bone.getPivotX() / 16f, -bone.getPivotY() / 16f, -bone.getPivotZ() / 16f);
        matrix.translate(bone.getPosX() / 16f, bone.getPosY() / 16f, bone.getPosZ() / 16f);

        return matrix;
    }

    /**
     * Render a single crescent ray with glow layers
     */
    private void renderRay(MatrixStack poseStack, VertexConsumerProvider bufferSource,
            Identifier texture, Vector3f tint, float intensity) {

        VertexConsumer vertices = bufferSource.getBuffer(RenderLayer.getEntityTranslucent(texture));
        MatrixStack.Entry entry = poseStack.peek();
        Matrix4f posMatrix = entry.getPositionMatrix();
        Matrix3f normMatrix = entry.getNormalMatrix();

        // Base dimensions
        float baseW = 1.5f;
        float baseH = 0.8f;

        // Outer glow layer
        drawQuad(posMatrix, normMatrix, vertices,
                baseW * 1.5f, baseH * 1.5f, -0.03f,
                tint.x * 0.7f, tint.y * 0.5f, tint.z * 0.2f, intensity * 0.25f);

        // Mid glow layer
        drawQuad(posMatrix, normMatrix, vertices,
                baseW * 1.2f, baseH * 1.2f, -0.015f,
                tint.x * 0.9f, tint.y * 0.7f, tint.z * 0.4f, intensity * 0.4f);

        // Main ray layer
        drawQuad(posMatrix, normMatrix, vertices,
                baseW, baseH, 0f,
                tint.x, tint.y, tint.z, intensity * 0.9f);

        // Bright core layer
        drawQuad(posMatrix, normMatrix, vertices,
                baseW * 0.65f, baseH * 0.65f, 0.015f,
                1.0f, 1.0f, 1.0f, intensity * 0.6f);

        // Hot center
        drawQuad(posMatrix, normMatrix, vertices,
                baseW * 0.35f, baseH * 0.35f, 0.03f,
                1.0f, 1.0f, 1.0f, intensity * 0.8f);
    }

    /**
     * Spawn particles along the beam path
     */
    private void spawnBeamParticles(RaEntity entity, Vec3d pos, Vec3d direction,
            float intensity, float scale) {

        if (entity.getRandom().nextFloat() > 0.7f * intensity) {
            return;
        }

        for (int i = 0; i < 4; i++) {
            double angle = entity.getRandom().nextDouble() * Math.PI * 2;
            double radius = scale * 0.4;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;

            entity.getWorld().addParticle(
                    ParticleTypes.FLAME,
                    pos.x + offsetX * 0.5,
                    pos.y + (entity.getRandom().nextDouble() - 0.5) * scale * 0.3,
                    pos.z + offsetZ * 0.5,
                    offsetX * 0.12 + direction.x * 0.03,
                    entity.getRandom().nextFloat() * 0.03,
                    offsetZ * 0.12 + direction.z * 0.03);
        }

        for (int i = 0; i < 2; i++) {
            if (entity.getRandom().nextFloat() < 0.6f) {
                double spread = (entity.getRandom().nextDouble() - 0.5) * 0.5;
                entity.getWorld().addParticle(
                        ParticleTypes.SMALL_FLAME,
                        pos.x - direction.x * 0.8 + spread,
                        pos.y + spread * 0.5,
                        pos.z - direction.z * 0.8 + spread,
                        -direction.x * 0.05,
                        0.02,
                        -direction.z * 0.05);
            }
        }

        if (entity.getRandom().nextFloat() < 0.35f) {
            entity.getWorld().addParticle(
                    ParticleTypes.ELECTRIC_SPARK,
                    pos.x, pos.y, pos.z,
                    (entity.getRandom().nextFloat() - 0.5f) * 0.18,
                    entity.getRandom().nextFloat() * 0.1,
                    (entity.getRandom().nextFloat() - 0.5f) * 0.18);
        }

        if (entity.getRandom().nextFloat() < 0.15f) {
            entity.getWorld().addParticle(
                    ParticleTypes.LAVA,
                    pos.x, pos.y, pos.z,
                    0, 0, 0);
        }
    }

    /**
     * Draw a double-sided quad for the crescent texture
     */
    private void drawQuad(Matrix4f posMatrix, Matrix3f normMatrix, VertexConsumer vertices,
            float w, float h, float z, float r, float g, float b, float a) {
        // Front face
        addVertex(posMatrix, normMatrix, vertices, -w, -h, z, r, g, b, a, 0, 1);
        addVertex(posMatrix, normMatrix, vertices, w, -h, z, r, g, b, a, 1, 1);
        addVertex(posMatrix, normMatrix, vertices, w, h, z, r, g, b, a, 1, 0);
        addVertex(posMatrix, normMatrix, vertices, -w, h, z, r, g, b, a, 0, 0);

        // Back face
        addVertex(posMatrix, normMatrix, vertices, w, -h, z, r, g, b, a, 1, 1);
        addVertex(posMatrix, normMatrix, vertices, -w, -h, z, r, g, b, a, 0, 1);
        addVertex(posMatrix, normMatrix, vertices, -w, h, z, r, g, b, a, 0, 0);
        addVertex(posMatrix, normMatrix, vertices, w, h, z, r, g, b, a, 1, 0);
    }

    private void addVertex(Matrix4f posMatrix, Matrix3f normMatrix, VertexConsumer vertices,
            float x, float y, float z, float r, float g, float b, float a, float u, float v) {
        vertices.vertex(posMatrix, x, y, z)
                .color(r, g, b, a)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(15728880)
                .normal(normMatrix, 0, 1, 0)
                .next();
    }
}
