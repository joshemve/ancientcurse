package com.ancientcurse.entity.renderer;

import com.ancientcurse.entity.RaEntity;
import com.ancientcurse.entity.model.RaModel;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

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

    public RaRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new RaModel());

        // Boss-appropriate shadow size
        this.shadowRadius = 1.2f;

        // Note: DivineFireAuraLayer, SolarCoronaLayer, and SunOrbGlowLayer removed -
        // they applied glow effects that caused unwanted layering

        // Add wing fire particle layer - spawns particles from animated bone positions
        addRenderLayer(new WingFireParticleLayer(this));

        // Add sun beam slice attack layer - divine vertical light beam
        addRenderLayer(new SunBeamSliceLayer(this));

        // Add sun shard attack layer - orbiting divine crystals
        // Add sun shard attack layer - orbiting divine crystals
        addRenderLayer(new SunShardLayer(this));

        // Add proper rope overlay for hibernation state
        addRenderLayer(new com.ancientcurse.entity.renderer.layer.RaRopeLayer(this));
    }

    @Override
    public void render(RaEntity entity, float entityYaw, float partialTick, MatrixStack poseStack,
            VertexConsumerProvider bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public void renderRecursively(MatrixStack poseStack, RaEntity animatable, GeoBone bone, RenderLayer renderType,
            VertexConsumerProvider bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
            int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {

        // Staff bones use staff_of_ra.png texture (imported with original UVs)
        // All other bones use ra.png
        boolean isStaffBone = isStaffOrDescendant(bone);

        if (isStaffBone) {
            net.minecraft.util.Identifier staffTexture = new net.minecraft.util.Identifier(
                    com.ancientcurse.AncientCurse.MOD_ID, "textures/item/staff_of_ra.png");
            renderType = RenderLayer.getEntityCutout(staffTexture);
            buffer = bufferSource.getBuffer(renderType);
        } else {
            // Ensure Ra's main texture for non-staff bones
            renderType = getRenderType(animatable, getTextureLocation(animatable), bufferSource, partialTick);
            buffer = bufferSource.getBuffer(renderType);
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha);
    }

    /**
     * Check if a bone is the staff_of_ra bone or a descendant of it.
     */
    private boolean isStaffOrDescendant(GeoBone bone) {
        GeoBone current = bone;
        while (current != null) {
            String name = current.getName().toLowerCase();
            // Match staff_of_ra or StaffofRa (the original bone name)
            if (name.equals("staff_of_ra") || name.equals("staffofra")) {
                return true;
            }
            current = current.getParent();
        }
        return false;
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
     * Wing fire particle layer - spawns a few flame particles at wing anchor points
     */
    private class WingFireParticleLayer extends GeoRenderLayer<RaEntity> {
        private int tickCounter = 0;

        // Only use wing tip anchors for minimal particles
        private static final String[] WING_ANCHORS = {
                "fire_wing_particles9", // right wing tip
                "fire_wing_particles10" // left wing tip
        };

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

            // Spawn flames every 4 ticks (reduced frequency)
            if (tickCounter % 4 == 0) {
                float bodyYaw = entity.bodyYaw;

                for (String boneName : WING_ANCHORS) {
                    Vec3d worldPos = ((RaRenderer) getRenderer()).getBoneWorldPos(entity, boneName, bakedModel,
                            entity.getX(), entity.getY(), entity.getZ(), bodyYaw);

                    // Single flame particle per anchor
                    entity.getWorld().addParticle(
                            ParticleTypes.FLAME,
                            worldPos.x,
                            worldPos.y,
                            worldPos.z,
                            (entity.getRandom().nextFloat() - 0.5) * 0.01,
                            0.02 + entity.getRandom().nextFloat() * 0.01,
                            (entity.getRandom().nextFloat() - 0.5) * 0.01);
                }
            }
        }
    }
}
