package com.ancientcurse.entity.renderer;

import com.ancientcurse.entity.RaEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Flying Staff Beam Render Layer - Client-side beam particles for Ra's flying staff attack
 *
 * Spawns the sun beam particles from the actual sun_orb bone position,
 * matching how the Staff of Ra item appears when held by players.
 *
 * Animation timing - MUST match ra.animation.json "ra.flying_staff_attack"!
 * Source: src/main/resources/assets/ancientcurse/animations/ra.animation.json
 * Animation length: 3.0 seconds = 60 ticks
 */
public class FlyingStaffBeamLayer extends GeoRenderLayer<RaEntity> {

    // Timing constants - match RaFlyingStaffAttackGoal
    private static final int SINGLE_LOOP_TICKS = 60;  // 3.0s per animation loop
    private static final int BEAM_START = 20;         // Start firing beam within each loop
    private static final int BEAM_END = 45;           // Stop firing beam within each loop

    // Beam configuration - matches StaffOfRaItem
    private static final double BEAM_RANGE = 32.0D;

    // Sun beam colors - EXACT same as StaffOfRaItem
    private static final Vector3f CORE_COLOR = new Vector3f(1.0F, 1.0F, 0.9F);   // Bright white-yellow
    private static final Vector3f INNER_COLOR = new Vector3f(1.0F, 0.95F, 0.7F); // Warm yellow
    private static final Vector3f OUTER_COLOR = new Vector3f(1.0F, 0.85F, 0.4F); // Golden orange

    public FlyingStaffBeamLayer(GeoEntityRenderer<RaEntity> entityRenderer) {
        super(entityRenderer);
    }

    @Override
    public void render(MatrixStack poseStack, RaEntity entity, BakedGeoModel bakedModel,
            RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer,
            float partialTick, int packedLight, int packedOverlay) {

        // Only render during Flying Staff Attack
        if (entity.getCombatState() != RaEntity.RaCombatState.FLYING_STAFF_ATTACK) {
            return;
        }

        // Get the action animation ticks to determine beam phase
        int actionTicks = entity.getActionAnimationTicks();
        if (actionTicks <= 0) {
            return;
        }

        // Get loop count to calculate total duration
        int loopCount = entity.getFlyingStaffLoopCount();
        int totalTicks = SINGLE_LOOP_TICKS * loopCount;

        // Calculate elapsed time in the animation
        int elapsed = totalTicks - actionTicks;

        // Calculate position within the current loop (0-59 for each loop)
        int localTick = elapsed % SINGLE_LOOP_TICKS;

        // Only render during beam phase of any loop
        if (localTick < BEAM_START || localTick > BEAM_END) {
            return;
        }

        // Calculate beam intensity (fade in/out within each loop)
        float intensity = 1.0f;
        if (localTick < BEAM_START + 5) {
            intensity = (localTick - BEAM_START) / 5.0f;
        } else if (localTick > BEAM_END - 5) {
            intensity = (BEAM_END - localTick) / 5.0f;
        }

        // Get the staff sun world position (where the beam originates)
        Vec3d startPos = getStaffSunWorldPosition(entity, bakedModel, partialTick);

        // Get the beam direction from the entity (set by server)
        Vec3d direction = entity.getSunBeamDirection();

        // Validate direction
        if (direction.lengthSquared() < 0.01) {
            // Use entity's facing direction as fallback
            float yawRad = (float) Math.toRadians(-entity.getYaw());
            direction = new Vec3d(Math.sin(yawRad), -0.1, Math.cos(yawRad)).normalize();
        }

        // Spawn beam particles along the path
        spawnBeamParticles(entity, startPos, direction, intensity);
    }

    // Bone name for the sun on Ra's staff (not the orb in the left hand)
    private static final String STAFF_SUN_BONE = "sun";

    /**
     * Get the world position of Ra's staff sun bone (where the beam originates)
     */
    private Vec3d getStaffSunWorldPosition(RaEntity entity, BakedGeoModel model, float partialTick) {
        double entityX = MathHelper.lerp(partialTick, entity.prevX, entity.getX());
        double entityY = MathHelper.lerp(partialTick, entity.prevY, entity.getY());
        double entityZ = MathHelper.lerp(partialTick, entity.prevZ, entity.getZ());
        float bodyYaw = MathHelper.lerpAngleDegrees(partialTick, entity.prevBodyYaw, entity.bodyYaw);

        return model.getBone(STAFF_SUN_BONE).map(bone -> {
            Matrix4f matrix = new Matrix4f();
            matrix.translation((float) entityX, (float) entityY, (float) entityZ);
            matrix.rotateY((float) Math.toRadians(-bodyYaw));

            Matrix4f boneMatrix = transformRecursively(bone);
            matrix.mul(boneMatrix);

            Vector4f worldPos = new Vector4f(0, 0, 0, 1).mul(matrix);
            return new Vec3d(worldPos.x(), worldPos.y(), worldPos.z());
        }).orElseGet(() -> {
            // Fallback: estimate staff position based on entity facing
            float yawRad = (float) Math.toRadians(-bodyYaw);
            double offsetX = Math.sin(yawRad) * 1.5;
            double offsetZ = Math.cos(yawRad) * 1.5;
            return new Vec3d(entityX + offsetX, entityY + entity.getHeight() * 0.8, entityZ + offsetZ);
        });
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
     * Spawn sun beam particles - matches StaffOfRaItem.createSunBeam() visually
     */
    private void spawnBeamParticles(RaEntity entity, Vec3d start, Vec3d direction, float intensity) {
        if (!entity.getWorld().isClient) return;

        // Main beam path - spawn particles along the beam
        for (double distance = 0; distance < BEAM_RANGE; distance += 0.4D) {
            Vec3d pos = start.add(direction.multiply(distance));

            // Calculate beam width based on distance
            double beamWidth = 0.1D + (distance * 0.01D);

            // Core beam - bright white center (every other segment)
            if ((int)(distance * 10) % 4 < 2) {
                entity.getWorld().addParticle(
                    new DustParticleEffect(CORE_COLOR, 1.0F * intensity),
                    pos.x, pos.y, pos.z,
                    0, 0, 0
                );
            }

            // Inner glow - warm yellow (3 points ring)
            double innerOffset = beamWidth * 0.3D;
            for (int i = 0; i < 3; i++) {
                double angle = (Math.PI * 2) * i / 3;
                double offsetX = Math.cos(angle) * innerOffset;
                double offsetZ = Math.sin(angle) * innerOffset;

                entity.getWorld().addParticle(
                    new DustParticleEffect(INNER_COLOR, 0.8F * intensity),
                    pos.x + offsetX, pos.y, pos.z + offsetZ,
                    0, 0, 0
                );
            }

            // Outer halo - golden particles (6 points rotating ring, less frequent)
            if ((int)(distance * 10) % 6 < 2) {
                double haloOffset = beamWidth * 0.6D;
                for (int i = 0; i < 6; i++) {
                    double angle = (Math.PI * 2) * i / 6 + (distance * 0.1);
                    double offsetX = Math.cos(angle) * haloOffset;
                    double offsetZ = Math.sin(angle) * haloOffset;

                    entity.getWorld().addParticle(
                        new DustParticleEffect(OUTER_COLOR, 0.6F * intensity),
                        pos.x + offsetX, pos.y, pos.z + offsetZ,
                        0, 0, 0
                    );
                }
            }

            // END_ROD sparkles along the beam (random)
            if (entity.getRandom().nextFloat() < 0.08F * intensity) {
                double sparkleOffset = entity.getRandom().nextDouble() * beamWidth;
                double sparkleAngle = entity.getRandom().nextDouble() * Math.PI * 2;

                entity.getWorld().addParticle(
                    ParticleTypes.END_ROD,
                    pos.x + Math.cos(sparkleAngle) * sparkleOffset,
                    pos.y + (entity.getRandom().nextDouble() - 0.5) * beamWidth,
                    pos.z + Math.sin(sparkleAngle) * sparkleOffset,
                    0, 0, 0.01
                );
            }

            // God rays effect - occasional bright flashes
            if ((int)(distance * 10) % 20 < 2 && entity.getRandom().nextFloat() < 0.25F * intensity) {
                entity.getWorld().addParticle(
                    ParticleTypes.INSTANT_EFFECT,
                    pos.x, pos.y, pos.z,
                    beamWidth, beamWidth, beamWidth
                );
            }
        }

        // Source glow effect around the orb
        for (int i = 0; i < 3; i++) {
            double angle = (Math.PI * 2) * i / 3 + (entity.age * 0.1);
            double radius = 0.4D;

            entity.getWorld().addParticle(
                new DustParticleEffect(INNER_COLOR, 1.5F),
                start.x + Math.cos(angle) * radius,
                start.y,
                start.z + Math.sin(angle) * radius,
                0, 0, 0
            );
        }
    }
}
