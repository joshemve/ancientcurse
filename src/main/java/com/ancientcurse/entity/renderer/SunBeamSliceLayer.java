package com.ancientcurse.entity.renderer;

import com.ancientcurse.client.render.SunBeamRenderLayer;
import com.ancientcurse.entity.RaEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.particle.DustParticleEffect;
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
 * Crescent Moon Slash Render Layer - Anime-style growing light slash for Ra
 *
 * Creates a stunning visual effect with:
 * - Growing crescent that expands as it travels
 * - Trailing afterimage slashes for motion blur effect
 * - Multi-layer glow (core, corona, bloom)
 * - Particle trail with sparkles
 * - Divine light emission
 */
public class SunBeamSliceLayer extends GeoRenderLayer<RaEntity> {

    // Textures for rendering
    private static final Identifier MIRAGE_SLICE_TEXTURE = new Identifier("ancientcurse",
            "textures/entity/mirage_slice.png");

    // Sun beam configuration - EPIC divine attack
    private static final float MIN_SCALE = 3.0f;      // Starting size (already impressive)
    private static final float MAX_SCALE = 7.0f;      // Grows to massive divine light
    private static final int TRAIL_COUNT = 3;         // Light trail for motion
    private static final float TRAIL_SPACING = 0.06f; // Tight for smooth light streak

    // Color definitions (sun/divine colors)
    private static final Vector3f CORE_COLOR = new Vector3f(1.0f, 1.0f, 0.95f);    // White core
    private static final Vector3f INNER_COLOR = new Vector3f(1.0f, 0.95f, 0.7f);   // Pale yellow
    private static final Vector3f OUTER_COLOR = new Vector3f(1.0f, 0.85f, 0.4f);   // Golden
    private static final Vector3f BLOOM_COLOR = new Vector3f(1.0f, 0.7f, 0.2f);    // Orange bloom

    // Ground Smack Timing (Matches RaEntity & Goal)
    private static final int GROUND_SMACK_DURATION = 60; // 3.0s total
    private static final int GROUND_SMACK_DELAY = 23;    // Firing point at 1.15s
    private static final int GROUND_SMACK_FADE = 10;     // Fade out at end

    // Particle colors
    private static final DustParticleEffect CORE_DUST = new DustParticleEffect(
            new Vector3f(1.0f, 1.0f, 0.9f), 1.2f);

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

        if (elapsed < GROUND_SMACK_DELAY) {
            return; // Don't render until 1s mark
        }

        // --- MIRAGE SLICE PROJECTILE (Triggered during Ground Smack) ---
        // Calculate progress (0.0 to 1.0) from DELAY (tick 20) to end (tick 60)
        float activeElapsed = elapsed - GROUND_SMACK_DELAY;
        float activeDuration = GROUND_SMACK_DURATION - GROUND_SMACK_DELAY;
        float travelProgress = MathHelper.clamp(activeElapsed / activeDuration, 0.0f, 1.0f);

        // Intensity: Fades in quickly, fades out at end
        float intensity;
        if (activeElapsed < 5) {
            intensity = activeElapsed / 5.0f;
        } else if (smoothTicks < GROUND_SMACK_FADE) {
            intensity = smoothTicks / (float) GROUND_SMACK_FADE;
        } else {
            intensity = 1.0f;
        }

        // Get sun orb world position
        Vec3d orbPos = getOrbWorldPosition(entity, bakedModel, partialTick);
        Vec3d startPos = orbPos;
        Vec3d direction = entity.getSunBeamDirection();
        double maxDist = 24.0;

        // Entity position for offset calculation
        double entityX = MathHelper.lerp(partialTick, entity.prevX, entity.getX());
        double entityY = MathHelper.lerp(partialTick, entity.prevY, entity.getY());
        double entityZ = MathHelper.lerp(partialTick, entity.prevZ, entity.getZ());

        // Calculate yaw once
        float yaw = (float) Math.atan2(direction.x, direction.z);

        // === RENDER LIGHT TRAIL (subtle afterglow) ===
        for (int i = TRAIL_COUNT - 1; i >= 0; i--) {
            float trailProgress = travelProgress - (i + 1) * TRAIL_SPACING;
            if (trailProgress <= 0.0f) continue;

            Vec3d trailPos = startPos.add(direction.multiply(maxDist * trailProgress));
            Vec3d trailOffset = trailPos.subtract(entityX, entityY, entityZ);

            // Trail matches main scale at its position
            float trailScale = MIN_SCALE + (MAX_SCALE - MIN_SCALE) * trailProgress;

            // Trail fades with distance from main beam
            float trailAlpha = intensity * (0.4f - (i * 0.15f));

            poseStack.push();
            poseStack.translate(trailOffset.x, trailOffset.y, trailOffset.z);
            poseStack.multiply(RotationAxis.POSITIVE_Y.rotation(yaw));
            poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));
            poseStack.scale(trailScale, trailScale, trailScale);

            renderSunBeam(poseStack, bufferSource, trailAlpha, false);
            poseStack.pop();
        }

        // === RENDER MAIN SUN BEAM ===
        Vec3d currentPos = startPos.add(direction.multiply(maxDist * travelProgress));
        Vec3d localOffset = currentPos.subtract(entityX, entityY, entityZ);

        // Subtle growth as it travels
        float scale = MIN_SCALE + (MAX_SCALE - MIN_SCALE) * travelProgress;
        // Gentle pulse
        scale += MathHelper.sin(entity.age * 0.4f) * 0.2f;

        poseStack.push();
        poseStack.translate(localOffset.x, localOffset.y, localOffset.z);
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotation(yaw));
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));
        poseStack.scale(scale, scale, scale);

        renderSunBeam(poseStack, bufferSource, intensity, true);
        poseStack.pop();

        // === SIMPLE PARTICLE TRAIL ===
        if (entity.getWorld().isClient && travelProgress > 0.05f) {
            spawnSunBeamParticles(entity, currentPos, direction, intensity, scale);
        }
    }

    /**
     * Spawn epic sun beam particles - divine light radiating outward
     */
    private void spawnSunBeamParticles(RaEntity entity, Vec3d pos, Vec3d direction,
            float intensity, float scale) {

        // Radiant sparkles shooting outward from the beam
        for (int i = 0; i < 2; i++) {
            if (entity.getRandom().nextFloat() > 0.6f) continue;

            double angle = entity.getRandom().nextDouble() * Math.PI * 2;
            double radius = scale * 0.4;
            double offsetX = Math.cos(angle) * radius;
            double offsetY = (entity.getRandom().nextDouble() - 0.5) * scale * 0.5;
            double offsetZ = Math.sin(angle) * radius;

            // END_ROD for bright divine sparkles
            entity.getWorld().addParticle(
                    ParticleTypes.END_ROD,
                    pos.x + offsetX * 0.5,
                    pos.y + offsetY,
                    pos.z + offsetZ * 0.5,
                    offsetX * 0.08,
                    entity.getRandom().nextFloat() * 0.03,
                    offsetZ * 0.08);
        }

        // Core golden dust (leaving a trail)
        if (entity.getRandom().nextFloat() < 0.5f) {
            entity.getWorld().addParticle(
                    CORE_DUST,
                    pos.x + (entity.getRandom().nextDouble() - 0.5) * 0.3,
                    pos.y + (entity.getRandom().nextDouble() - 0.5) * 0.3,
                    pos.z + (entity.getRandom().nextDouble() - 0.5) * 0.3,
                    -direction.x * 0.02,
                    0.01,
                    -direction.z * 0.02);
        }

        // Flame particles for heat/power
        if (entity.getRandom().nextFloat() < 0.35f * intensity) {
            entity.getWorld().addParticle(
                    ParticleTypes.FLAME,
                    pos.x + (entity.getRandom().nextDouble() - 0.5) * scale * 0.3,
                    pos.y + (entity.getRandom().nextDouble() - 0.5) * scale * 0.3,
                    pos.z + (entity.getRandom().nextDouble() - 0.5) * scale * 0.3,
                    (entity.getRandom().nextFloat() - 0.5f) * 0.03,
                    0.02,
                    (entity.getRandom().nextFloat() - 0.5f) * 0.03);
        }

        // Electric divine energy
        if (entity.getRandom().nextFloat() < 0.2f) {
            entity.getWorld().addParticle(
                    ParticleTypes.ELECTRIC_SPARK,
                    pos.x, pos.y, pos.z,
                    (entity.getRandom().nextFloat() - 0.5f) * 0.2,
                    entity.getRandom().nextFloat() * 0.1,
                    (entity.getRandom().nextFloat() - 0.5f) * 0.2);
        }
    }

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
     * Render the divine sun beam with epic multi-layer glow effect
     *
     * Uses 6 additive layers to create intense bloom without shaders:
     * - Outer bloom (massive, faint orange)
     * - Corona (golden halo)
     * - Inner glow (bright yellow)
     * - Core (near-white, texture)
     * - Hot center (pure white highlight)
     * - Divine spark (intense white point)
     */
    private void renderSunBeam(MatrixStack poseStack, VertexConsumerProvider bufferSource,
            float intensity, boolean isMainBeam) {

        // Use custom sun beam shader for animated energy flow and divine sparkles
        VertexConsumer vertices = bufferSource.getBuffer(SunBeamRenderLayer.getSunBeamLayer(MIRAGE_SLICE_TEXTURE));
        MatrixStack.Entry entry = poseStack.peek();
        Matrix4f posMatrix = entry.getPositionMatrix();
        Matrix3f normMatrix = entry.getNormalMatrix();

        // Base dimensions (crescent shape: wide, not too tall)
        float baseW = 1.0f;
        float baseH = 0.55f;

        if (isMainBeam) {
            // === LAYER 1: Massive outer bloom ===
            drawQuad(posMatrix, normMatrix, vertices,
                    baseW * 2.0f, baseH * 2.0f, -0.04f,
                    1.0f, 0.5f, 0.1f, intensity * 0.1f);  // Deep orange, very faint

            // === LAYER 2: Outer bloom ===
            drawQuad(posMatrix, normMatrix, vertices,
                    baseW * 1.7f, baseH * 1.7f, -0.03f,
                    BLOOM_COLOR.x, BLOOM_COLOR.y, BLOOM_COLOR.z, intensity * 0.15f);

            // === LAYER 3: Golden corona ===
            drawQuad(posMatrix, normMatrix, vertices,
                    baseW * 1.4f, baseH * 1.4f, -0.02f,
                    OUTER_COLOR.x, OUTER_COLOR.y, OUTER_COLOR.z, intensity * 0.25f);

            // === LAYER 4: Inner glow (bright yellow) ===
            drawQuad(posMatrix, normMatrix, vertices,
                    baseW * 1.2f, baseH * 1.2f, -0.01f,
                    INNER_COLOR.x, INNER_COLOR.y, INNER_COLOR.z, intensity * 0.4f);

            // === LAYER 5: Core (near-white, main texture) ===
            drawQuad(posMatrix, normMatrix, vertices,
                    baseW, baseH, 0f,
                    CORE_COLOR.x, CORE_COLOR.y, CORE_COLOR.z, intensity * 0.85f);

            // === LAYER 6: Hot center (bright white) ===
            drawQuad(posMatrix, normMatrix, vertices,
                    baseW * 0.75f, baseH * 0.75f, 0.01f,
                    1.0f, 1.0f, 1.0f, intensity * 0.5f);

            // === LAYER 7: Divine spark (intense white point) ===
            drawQuad(posMatrix, normMatrix, vertices,
                    baseW * 0.4f, baseH * 0.4f, 0.02f,
                    1.0f, 1.0f, 1.0f, intensity * 0.7f);
        } else {
            // Trail rendering (3 layers for smooth fade)
            // Outer glow
            drawQuad(posMatrix, normMatrix, vertices,
                    baseW * 1.3f, baseH * 1.3f, -0.01f,
                    OUTER_COLOR.x, OUTER_COLOR.y, OUTER_COLOR.z, intensity * 0.25f);

            // Inner
            drawQuad(posMatrix, normMatrix, vertices,
                    baseW * 1.1f, baseH * 1.1f, 0f,
                    INNER_COLOR.x, INNER_COLOR.y, INNER_COLOR.z, intensity * 0.4f);

            // Core
            drawQuad(posMatrix, normMatrix, vertices,
                    baseW, baseH, 0.01f,
                    CORE_COLOR.x, CORE_COLOR.y, CORE_COLOR.z, intensity * 0.5f);
        }
    }

    /**
     * Draw a double-sided quad for the crescent texture
     */
    private void drawQuad(Matrix4f posMatrix, Matrix3f normMatrix, VertexConsumer vertices,
            float w, float h, float z, float r, float g, float b, float a) {
        // Front face
        addVertex(posMatrix, normMatrix, vertices, -w, -h, z, r, g, b, a, 0, 1);
        addVertex(posMatrix, normMatrix, vertices,  w, -h, z, r, g, b, a, 1, 1);
        addVertex(posMatrix, normMatrix, vertices,  w,  h, z, r, g, b, a, 1, 0);
        addVertex(posMatrix, normMatrix, vertices, -w,  h, z, r, g, b, a, 0, 0);

        // Back face
        addVertex(posMatrix, normMatrix, vertices,  w, -h, z, r, g, b, a, 1, 1);
        addVertex(posMatrix, normMatrix, vertices, -w, -h, z, r, g, b, a, 0, 1);
        addVertex(posMatrix, normMatrix, vertices, -w,  h, z, r, g, b, a, 0, 0);
        addVertex(posMatrix, normMatrix, vertices,  w,  h, z, r, g, b, a, 1, 0);
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
