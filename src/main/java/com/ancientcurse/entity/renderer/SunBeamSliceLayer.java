package com.ancientcurse.entity.renderer;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.RaEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
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
 * Sun Beam Slice Render Layer - Divine vertical beam attack for Ra
 *
 * Creates a stunning visual effect with:
 * - Multi-layer beam geometry (core, inner corona, outer halo)
 * - Additive bloom glow effect
 * - Horizontal particle bursts
 * - Enchantment glyph swirls
 * - Dynamic light emission simulation
 */
public class SunBeamSliceLayer extends GeoRenderLayer<RaEntity> {

    // Textures for beam rendering
    private static final Identifier BEACON_TEXTURE = new Identifier("textures/entity/beacon_beam.png");
    private static final Identifier MIRAGE_SLICE_TEXTURE = new Identifier("ancientcurse",
            "textures/entity/mirage_slice.png");

    // Beam configuration
    private static final float BEAM_MAX_LENGTH = 16.0f;
    private static final float BEAM_CORE_WIDTH = 0.15f;
    private static final float BEAM_INNER_WIDTH = 0.4f;
    private static final float BEAM_OUTER_WIDTH = 0.8f;
    private static final float BEAM_BLOOM_WIDTH = 1.5f;

    // Color definitions (white-yellow sun colors)
    private static final Vector3f CORE_COLOR = new Vector3f(1.0f, 1.0f, 0.95f);
    private static final Vector3f INNER_COLOR = new Vector3f(1.0f, 0.95f, 0.7f);
    private static final Vector3f OUTER_COLOR = new Vector3f(1.0f, 0.85f, 0.4f);
    private static final Vector3f BLOOM_COLOR = new Vector3f(1.0f, 0.7f, 0.2f);
    private static final Vector3f EDGE_COLOR = new Vector3f(1.0f, 0.5f, 0.1f);

    // Ground Smack Timing (Matches RaEntity & Goal)
    private static final int GROUND_SMACK_DURATION = 60; // 3.0s total
    private static final int GROUND_SMACK_DELAY = 20; // Firing point at 1.0s
    private static final int GROUND_SMACK_FADE = 10; // Fade out at end

    // Particle configuration
    private static final int HORIZONTAL_BURST_COUNT = 24;
    private static final int ENCHANT_GLYPH_COUNT = 16;

    // Particle colors
    private static final DustParticleEffect CORE_DUST = new DustParticleEffect(
            new Vector3f(1.0f, 1.0f, 0.9f), 1.2f);
    private static final DustParticleEffect INNER_DUST = new DustParticleEffect(
            new Vector3f(1.0f, 0.9f, 0.6f), 1.0f);
    private static final DustParticleEffect OUTER_DUST = new DustParticleEffect(
            new Vector3f(1.0f, 0.75f, 0.3f), 0.8f);

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
        Vec3d currentPos = startPos.add(direction.multiply(maxDist * travelProgress));

        // Render relative to entity
        Vec3d localOffset = currentPos.subtract(MathHelper.lerp(partialTick, entity.prevX, entity.getX()),
                MathHelper.lerp(partialTick, entity.prevY, entity.getY()),
                MathHelper.lerp(partialTick, entity.prevZ, entity.getZ()));

        poseStack.push();
        poseStack.translate(localOffset.x, localOffset.y, localOffset.z);

        // Align with the travel direction (Vertical Blade)
        float yaw = (float) Math.atan2(direction.x, direction.z);
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotation(yaw));
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));

        // Scale pulse
        float scale = 5.0f + MathHelper.sin(entity.age * 0.4f) * 0.5f;
        poseStack.scale(scale, scale, scale);

        renderMirageSlice(poseStack, bufferSource, intensity, partialTick);
        poseStack.pop();

        // Trail particles
        if (entity.getWorld().isClient && travelProgress > 0.05f && travelProgress < 0.95f) {
            spawnBeamParticles(entity, currentPos, direction, 0.5f, intensity, (int) elapsed, true);
        }
    }

    private float calculateBeamProgress(int beamTicks, float partialTick) {
        return 1.0f;
    }

    private float calculateIntensity(int beamTicks) {
        return 1.0f;
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

    private float calculateBeamLength(RaEntity entity, Vec3d orbPos, Vec3d direction, float progress) {
        return 24.0f * progress;
    }

    private void applyBeamRotation(MatrixStack poseStack, Vec3d direction) {
        double yaw = Math.atan2(direction.x, direction.z);
        double horizontalDist = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        double pitch = Math.atan2(-direction.y, horizontalDist);

        poseStack.multiply(RotationAxis.POSITIVE_Y.rotation((float) yaw));
        poseStack.multiply(RotationAxis.POSITIVE_X.rotation((float) pitch + MathHelper.HALF_PI));
    }

    private void renderBeamLayers(MatrixStack poseStack, VertexConsumerProvider bufferSource,
            RaEntity entity, float beamLength, float intensity, float progress, float partialTick) {

        float time = entity.age + partialTick;

        // Layer 1: Outer bloom
        renderBeamLayer(poseStack, bufferSource, BEACON_TEXTURE,
                beamLength, BEAM_BLOOM_WIDTH * intensity,
                BLOOM_COLOR.x, BLOOM_COLOR.y, BLOOM_COLOR.z,
                0.15f * intensity, time, 0);

        // Layer 2: Outer halo
        renderBeamLayer(poseStack, bufferSource, BEACON_TEXTURE,
                beamLength, BEAM_OUTER_WIDTH * intensity,
                OUTER_COLOR.x, OUTER_COLOR.y, OUTER_COLOR.z,
                0.3f * intensity, time, 1);

        // Layer 3: Inner corona
        float innerPulse = 1.0f + MathHelper.sin(time * 0.5f) * 0.2f;
        renderBeamLayer(poseStack, bufferSource, BEACON_TEXTURE,
                beamLength, BEAM_INNER_WIDTH * intensity * innerPulse,
                INNER_COLOR.x, INNER_COLOR.y, INNER_COLOR.z,
                0.6f * intensity, time, 2);

        // Layer 4: Core beam
        renderBeamLayer(poseStack, bufferSource, BEACON_TEXTURE,
                beamLength, BEAM_CORE_WIDTH * intensity,
                CORE_COLOR.x, CORE_COLOR.y, CORE_COLOR.z,
                0.95f * intensity, time, 3);

        // Layer 5: Super-bright center
        renderBeamLayer(poseStack, bufferSource, BEACON_TEXTURE,
                beamLength, BEAM_CORE_WIDTH * 0.3f * intensity,
                1.0f, 1.0f, 1.0f,
                1.0f * intensity, time, 4);

        // Edge glow layers
        for (int i = 0; i < 4; i++) {
            float angle = i * MathHelper.HALF_PI + time * 0.1f;
            float edgeOffset = BEAM_INNER_WIDTH * 0.8f * intensity;

            poseStack.push();
            poseStack.multiply(RotationAxis.POSITIVE_Y.rotation(angle));
            poseStack.translate(edgeOffset, 0, 0);

            renderBeamLayer(poseStack, bufferSource, BEACON_TEXTURE,
                    beamLength, BEAM_CORE_WIDTH * 0.5f,
                    EDGE_COLOR.x, EDGE_COLOR.y, EDGE_COLOR.z,
                    0.4f * intensity, time, 5 + i);

            poseStack.pop();
        }
    }

    private void renderBeamLayer(MatrixStack poseStack, VertexConsumerProvider bufferSource,
            Identifier texture, float length, float width, float r, float g, float b, float alpha,
            float time, int layerIndex) {

        VertexConsumer vertices = bufferSource.getBuffer(RenderLayer.getBeaconBeam(texture, true));

        MatrixStack.Entry entry = poseStack.peek();
        Matrix4f posMatrix = entry.getPositionMatrix();
        Matrix3f normMatrix = entry.getNormalMatrix();

        float halfWidth = width * 0.5f;

        for (int face = 0; face < 4; face++) {
            float faceAngle = face * MathHelper.HALF_PI;
            float faceCos = MathHelper.cos(faceAngle);
            float faceSin = MathHelper.sin(faceAngle);

            float x1 = halfWidth * faceCos;
            float z1 = halfWidth * faceSin;
            float x2 = halfWidth * MathHelper.cos(faceAngle + MathHelper.HALF_PI);
            float z2 = halfWidth * MathHelper.sin(faceAngle + MathHelper.HALF_PI);

            float uvOffset = (time * 0.05f + layerIndex * 0.2f) % 1.0f;

            addVertex(posMatrix, normMatrix, vertices, x1, 0, z1, r, g, b, alpha, 0, uvOffset);
            addVertex(posMatrix, normMatrix, vertices, x2, 0, z2, r, g, b, alpha, 1, uvOffset);
            addVertex(posMatrix, normMatrix, vertices, x2, length, z2, r, g, b, alpha, 1, uvOffset + length * 0.5f);
            addVertex(posMatrix, normMatrix, vertices, x1, length, z1, r, g, b, alpha, 0, uvOffset + length * 0.5f);
        }
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

    private void spawnBeamParticles(RaEntity entity, Vec3d orbPos, Vec3d beamDirection,
            float beamLength, float intensity, int elapsed, boolean isGroundSmack) {

        float time = entity.age;

        spawnOriginParticles(entity, orbPos, intensity, time);
        spawnBeamTrailParticles(entity, orbPos, beamDirection, beamLength, intensity, time);

        // Burst trigger logic: Slam impacts at frame 20 (~1.0s)
        boolean triggerBurst = elapsed >= 19 && elapsed <= 23;

        if (triggerBurst) {
            spawnHorizontalBurst(entity, orbPos, beamDirection, beamLength, intensity);
        }

        spawnEnchantmentGlyphs(entity, orbPos, beamDirection, beamLength, intensity, time);

        Vec3d impactPos = orbPos.add(beamDirection.multiply(beamLength));
        spawnImpactParticles(entity, impactPos, intensity, time);
    }

    private void spawnOriginParticles(RaEntity entity, Vec3d orbPos, float intensity, float time) {
        if (entity.getRandom().nextFloat() > 0.7f)
            return;

        for (int i = 0; i < 3; i++) {
            double angle = entity.getRandom().nextDouble() * Math.PI * 2;
            double radius = 0.2 + entity.getRandom().nextDouble() * 0.3;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double offsetY = (entity.getRandom().nextDouble() - 0.5) * 0.4;

            entity.getWorld().addParticle(
                    CORE_DUST,
                    orbPos.x + offsetX, orbPos.y + offsetY, orbPos.z + offsetZ,
                    offsetX * 0.05, 0.02, offsetZ * 0.05);
        }

        if (entity.getRandom().nextFloat() < 0.3f * intensity) {
            entity.getWorld().addParticle(
                    ParticleTypes.END_ROD,
                    orbPos.x, orbPos.y, orbPos.z,
                    (entity.getRandom().nextFloat() - 0.5f) * 0.1,
                    entity.getRandom().nextFloat() * 0.05,
                    (entity.getRandom().nextFloat() - 0.5f) * 0.1);
        }
    }

    private void spawnBeamTrailParticles(RaEntity entity, Vec3d orbPos, Vec3d direction,
            float beamLength, float intensity, float time) {

        int particleCount = (int) (beamLength * 2 * intensity);

        for (int i = 0; i < particleCount; i++) {
            if (entity.getRandom().nextFloat() > 0.4f)
                continue;

            float progress = entity.getRandom().nextFloat();
            Vec3d particlePos = orbPos.add(direction.multiply(beamLength * progress));

            double perpX = (entity.getRandom().nextDouble() - 0.5) * 0.3 * (1 - progress);
            double perpZ = (entity.getRandom().nextDouble() - 0.5) * 0.3 * (1 - progress);

            entity.getWorld().addParticle(
                    progress < 0.3f ? CORE_DUST : (progress < 0.7f ? INNER_DUST : OUTER_DUST),
                    particlePos.x + perpX, particlePos.y, particlePos.z + perpZ,
                    perpX * 0.02, -0.02, perpZ * 0.02);

            if (entity.getRandom().nextFloat() < 0.2f) {
                entity.getWorld().addParticle(
                        ParticleTypes.FLAME,
                        particlePos.x + perpX, particlePos.y, particlePos.z + perpZ,
                        perpX * 0.03, 0.01, perpZ * 0.03);
            }
        }
    }

    private void spawnHorizontalBurst(RaEntity entity, Vec3d orbPos, Vec3d beamDirection,
            float beamLength, float intensity) {

        Vec3d perpendicular1 = beamDirection.crossProduct(new Vec3d(0, 1, 0)).normalize();
        if (perpendicular1.lengthSquared() < 0.01) {
            perpendicular1 = new Vec3d(1, 0, 0);
        }
        Vec3d perpendicular2 = beamDirection.crossProduct(perpendicular1).normalize();

        for (float height = 0; height < beamLength; height += 0.5f) {
            Vec3d burstOrigin = orbPos.add(beamDirection.multiply(height));

            for (int i = 0; i < HORIZONTAL_BURST_COUNT; i++) {
                double angle = (i / (double) HORIZONTAL_BURST_COUNT) * Math.PI * 2;

                Vec3d burstDir = perpendicular1.multiply(Math.cos(angle))
                        .add(perpendicular2.multiply(Math.sin(angle)));

                double speed = 0.15 + entity.getRandom().nextDouble() * 0.1;

                entity.getWorld().addParticle(
                        INNER_DUST,
                        burstOrigin.x, burstOrigin.y, burstOrigin.z,
                        burstDir.x * speed,
                        (entity.getRandom().nextDouble() - 0.5) * 0.02,
                        burstDir.z * speed);

                if (entity.getRandom().nextFloat() < 0.5f) {
                    entity.getWorld().addParticle(
                            ParticleTypes.END_ROD,
                            burstOrigin.x, burstOrigin.y, burstOrigin.z,
                            burstDir.x * speed * 1.5,
                            entity.getRandom().nextDouble() * 0.05,
                            burstDir.z * speed * 1.5);
                }

                if (entity.getRandom().nextFloat() < 0.3f) {
                    entity.getWorld().addParticle(
                            ParticleTypes.ELECTRIC_SPARK,
                            burstOrigin.x, burstOrigin.y, burstOrigin.z,
                            burstDir.x * speed * 0.8,
                            (entity.getRandom().nextDouble() - 0.5) * 0.1,
                            burstDir.z * speed * 0.8);
                }
            }
        }
    }

    private void spawnEnchantmentGlyphs(RaEntity entity, Vec3d orbPos, Vec3d beamDirection,
            float beamLength, float intensity, float time) {

        if (entity.getRandom().nextFloat() > 0.6f)
            return;

        for (int i = 0; i < ENCHANT_GLYPH_COUNT / 4; i++) {
            float heightProgress = entity.getRandom().nextFloat();
            float spiralAngle = time * 0.3f + heightProgress * MathHelper.PI * 4 + i * MathHelper.PI * 0.5f;
            float spiralRadius = 0.5f + heightProgress * 0.5f;

            Vec3d beamPoint = orbPos.add(beamDirection.multiply(beamLength * heightProgress));

            Vec3d up = new Vec3d(0, 1, 0);
            Vec3d perp1 = beamDirection.crossProduct(up).normalize();
            if (perp1.lengthSquared() < 0.01)
                perp1 = new Vec3d(1, 0, 0);
            Vec3d perp2 = beamDirection.crossProduct(perp1).normalize();

            double offsetX = Math.cos(spiralAngle) * spiralRadius;
            double offsetZ = Math.sin(spiralAngle) * spiralRadius;
            Vec3d spiralOffset = perp1.multiply(offsetX).add(perp2.multiply(offsetZ));

            entity.getWorld().addParticle(
                    ParticleTypes.ENCHANT,
                    beamPoint.x + spiralOffset.x,
                    beamPoint.y + spiralOffset.y,
                    beamPoint.z + spiralOffset.z,
                    -spiralOffset.x * 0.1,
                    -0.02,
                    -spiralOffset.z * 0.1);

            if (entity.getRandom().nextFloat() < 0.2f) {
                entity.getWorld().addParticle(
                        ParticleTypes.TOTEM_OF_UNDYING,
                        beamPoint.x + spiralOffset.x,
                        beamPoint.y + spiralOffset.y,
                        beamPoint.z + spiralOffset.z,
                        -spiralOffset.x * 0.05,
                        0.05,
                        -spiralOffset.z * 0.05);
            }
        }
    }

    private void spawnImpactParticles(RaEntity entity, Vec3d impactPos, float intensity, float time) {
        if (entity.getRandom().nextFloat() > 0.5f)
            return;

        for (int i = 0; i < 5; i++) {
            double angle = entity.getRandom().nextDouble() * Math.PI * 2;
            double speed = 0.05 + entity.getRandom().nextDouble() * 0.05;

            entity.getWorld().addParticle(
                    OUTER_DUST,
                    impactPos.x, impactPos.y + 0.1, impactPos.z,
                    Math.cos(angle) * speed, 0.02, Math.sin(angle) * speed);
        }

        if (entity.getRandom().nextFloat() < 0.3f * intensity) {
            entity.getWorld().addParticle(
                    ParticleTypes.FLAME,
                    impactPos.x + (entity.getRandom().nextDouble() - 0.5) * 0.5,
                    impactPos.y + 0.1,
                    impactPos.z + (entity.getRandom().nextDouble() - 0.5) * 0.5,
                    0, 0.05, 0);
        }

        if (entity.getRandom().nextFloat() < 0.2f) {
            entity.getWorld().addParticle(
                    ParticleTypes.LAVA,
                    impactPos.x + (entity.getRandom().nextDouble() - 0.5) * 0.3,
                    impactPos.y + 0.1,
                    impactPos.z + (entity.getRandom().nextDouble() - 0.5) * 0.3,
                    0, 0, 0);
        }

        if (entity.getRandom().nextFloat() < 0.4f) {
            entity.getWorld().addParticle(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    impactPos.x + (entity.getRandom().nextDouble() - 0.5) * 0.5,
                    impactPos.y + 0.1,
                    impactPos.z + (entity.getRandom().nextDouble() - 0.5) * 0.5,
                    0, 0.02, 0);
        }
    }

    private float calculateTravelProgress(int beamTicks, float partialTick) {
        return 1.0f;
    }

    private void renderMirageSlice(MatrixStack poseStack, VertexConsumerProvider bufferSource, float intensity,
            float partialTick) {
        // Use getBeaconBeam for ADDITIVE blending: Black background becomes
        // transparent.
        VertexConsumer vertices = bufferSource.getBuffer(RenderLayer.getBeaconBeam(MIRAGE_SLICE_TEXTURE, true));
        MatrixStack.Entry entry = poseStack.peek();
        Matrix4f posMatrix = entry.getPositionMatrix();
        Matrix3f normMatrix = entry.getNormalMatrix();

        // Color: Golden Orange (1.0, 0.6, 0.1)
        float r = 1.0f;
        float g = 0.6f;
        float b = 0.1f;
        float a = Math.min(1.0f, intensity);

        // Vertical Quad centered
        // Width is oriented along X, Height along Y in local space.
        // We want a large horizontal-ish crescent, so we use w > h.
        float w = 2.0f;
        float h = 1.2f;

        // Single flat vertical slice (no cross-quads)
        drawQuad(posMatrix, normMatrix, vertices, w, h, 0, r, g, b, a);
    }

    private void drawQuad(Matrix4f posMatrix, Matrix3f normMatrix, VertexConsumer vertices, float w, float h, float z,
            float r, float g, float b, float a) {
        addVertex(posMatrix, normMatrix, vertices, -w, -h, z, r, g, b, a, 0, 1);
        addVertex(posMatrix, normMatrix, vertices, w, -h, z, r, g, b, a, 1, 1);
        addVertex(posMatrix, normMatrix, vertices, w, h, z, r, g, b, a, 1, 0);
        addVertex(posMatrix, normMatrix, vertices, -w, h, z, r, g, b, a, 0, 0);

        addVertex(posMatrix, normMatrix, vertices, w, -h, z, r, g, b, a, 1, 1);
        addVertex(posMatrix, normMatrix, vertices, -w, -h, z, r, g, b, a, 0, 1);
        addVertex(posMatrix, normMatrix, vertices, -w, h, z, r, g, b, a, 0, 0);
        addVertex(posMatrix, normMatrix, vertices, w, h, z, r, g, b, a, 1, 0);
    }
}
