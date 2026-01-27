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

        // Add flying staff beam layer - sun beam from orb during flying attack
        addRenderLayer(new FlyingStaffBeamLayer(this));

        // Add sun shard attack layer - orbiting divine crystals
        addRenderLayer(new SunShardLayer(this));

        // Add proper rope overlay for hibernation state
        addRenderLayer(new com.ancientcurse.entity.renderer.layer.RaRopeLayer(this));

        // Add emissive sun orb overlay - glows like a real sun
        addRenderLayer(new SunOrbGlowLayer(this));

        // Add sun orb fire particles - follows the animated sun orb position
        addRenderLayer(new SunOrbFireParticleLayer(this));
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

        // Only override textures/layers during the main render pass
        // This allows GeoRenderLayers to use their own layers during re-render passes
        // (isReRender = true)
        if (!isReRender) {
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
     * Apply the full bone transformation hierarchy to the poseStack
     * This includes all parent bone transforms so effects follow the bone exactly
     */
    protected void applyBoneTransform(MatrixStack poseStack, GeoBone bone) {
        if (bone.getParent() != null) {
            applyBoneTransform(poseStack, bone.getParent());
        }

        poseStack.translate(bone.getPivotX() / 16f, bone.getPivotY() / 16f, bone.getPivotZ() / 16f);

        poseStack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotation(bone.getRotZ()));
        poseStack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotation(bone.getRotY()));
        poseStack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotation(bone.getRotX()));

        poseStack.scale(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ());

        poseStack.translate(-bone.getPivotX() / 16f, -bone.getPivotY() / 16f, -bone.getPivotZ() / 16f);

        poseStack.translate(bone.getPosX() / 16f, bone.getPosY() / 16f, bone.getPosZ() / 16f);
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
     * Sun orb glow layer with additive blending
     * Creates a glowing sun effect on the sun_orb bone
     */
    private class SunOrbGlowLayer extends GeoRenderLayer<RaEntity> {
        private static final net.minecraft.util.Identifier SUN_ORB_TEXTURE = new net.minecraft.util.Identifier(
                com.ancientcurse.AncientCurse.MOD_ID, "textures/entity/sun_orb.png");

        // Fullbright light value
        private static final int FULLBRIGHT = 15728880;

        public SunOrbGlowLayer(GeoEntityRenderer<RaEntity> entityRenderer) {
            super(entityRenderer);
        }

        @Override
        public void render(MatrixStack poseStack, RaEntity entity, BakedGeoModel bakedModel,
                RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer,
                float partialTick, int packedLight, int packedOverlay) {

            // Render additive glow overlay on sun_orb bone
            RenderLayer glowLayer = RenderLayer.getEyes(SUN_ORB_TEXTURE); // Eyes layer = additive-like
            VertexConsumer glowBuffer = bufferSource.getBuffer(glowLayer);

            bakedModel.getBone("sun_orb").ifPresent(sunOrbBone -> {
                // Store and hide all bones
                java.util.Map<GeoBone, Boolean> originalVisibility = new java.util.HashMap<>();
                for (GeoBone bone : bakedModel.topLevelBones()) {
                    originalVisibility.put(bone, bone.isHidden());
                    bone.setHidden(true);
                }

                // Show only sun_orb
                setVisibilityRecursive(sunOrbBone, false);

                // Render with additive glow
                getRenderer().reRender(bakedModel, poseStack, bufferSource, entity, glowLayer, glowBuffer,
                        partialTick, FULLBRIGHT, packedOverlay, 1.0f, 1.0f, 1.0f, 1.0f);

                // Restore visibility
                for (java.util.Map.Entry<GeoBone, Boolean> entry : originalVisibility.entrySet()) {
                    entry.getKey().setHidden(entry.getValue());
                }
            });
        }

        private void setVisibilityRecursive(GeoBone bone, boolean hidden) {
            bone.setHidden(hidden);
            for (GeoBone child : bone.getChildBones()) {
                setVisibilityRecursive(child, hidden);
            }
        }
    }

    /**
     * Wing fire particle layer - spawns flame particles at wing anchor points
     * Uses multiple anchors along the wings for a dramatic fire trail effect
     */
    private class WingFireParticleLayer extends GeoRenderLayer<RaEntity> {
        private int tickCounter = 0;

        // Wing particle anchors - spread along both wings
        // These should match locator bones in the Ra model
        private static final String[] WING_ANCHORS = {
                "fire_wing_particles1",
                "fire_wing_particles2",
                "fire_wing_particles3",
                "fire_wing_particles4",
                "fire_wing_particles5",
                "fire_wing_particles6",
                "fire_wing_particles7",
                "fire_wing_particles8",
                "fire_wing_particles9",
                "fire_wing_particles10"
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

            // Spawn flames every 3 ticks for visible but not overwhelming particles
            if (tickCounter % 3 == 0) {
                float bodyYaw = entity.bodyYaw;

                for (String boneName : WING_ANCHORS) {
                    Vec3d worldPos = ((RaRenderer) getRenderer()).getBoneWorldPos(entity, boneName, bakedModel,
                            entity.getX(), entity.getY(), entity.getZ(), bodyYaw);

                    // Check if bone was found (position won't equal entity base if bone exists)
                    // Bones that don't exist return entity position, so skip those
                    double distFromBase = worldPos.distanceTo(new Vec3d(entity.getX(), entity.getY(), entity.getZ()));
                    if (distFromBase < 0.1) {
                        continue; // Bone not found, skip
                    }

                    // Flame particle
                    entity.getWorld().addParticle(
                            ParticleTypes.FLAME,
                            worldPos.x,
                            worldPos.y,
                            worldPos.z,
                            (entity.getRandom().nextFloat() - 0.5) * 0.02,
                            0.03 + entity.getRandom().nextFloat() * 0.02,
                            (entity.getRandom().nextFloat() - 0.5) * 0.02);

                    // Occasional small flame for variety
                    if (entity.getRandom().nextFloat() < 0.3f) {
                        entity.getWorld().addParticle(
                                ParticleTypes.SMALL_FLAME,
                                worldPos.x + (entity.getRandom().nextFloat() - 0.5) * 0.2,
                                worldPos.y + (entity.getRandom().nextFloat() - 0.5) * 0.2,
                                worldPos.z + (entity.getRandom().nextFloat() - 0.5) * 0.2,
                                (entity.getRandom().nextFloat() - 0.5) * 0.01,
                                0.02,
                                (entity.getRandom().nextFloat() - 0.5) * 0.01);
                    }
                }
            }
        }
    }

    /**
     * Sun orb fire particle layer - spawns fire particles from the sun_orb_particles_anchor bone
     * Particles follow the animated sun orb position during all animations
     */
    private class SunOrbFireParticleLayer extends GeoRenderLayer<RaEntity> {
        private int tickCounter = 0;

        // The bone/locator name for sun orb particles
        private static final String SUN_ORB_ANCHOR = "sun_orb_particles_anchor";

        public SunOrbFireParticleLayer(GeoEntityRenderer<RaEntity> entityRenderer) {
            super(entityRenderer);
        }

        @Override
        public void render(MatrixStack poseStack, RaEntity entity, BakedGeoModel bakedModel,
                RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer,
                float partialTick, int packedLight, int packedOverlay) {

            if (!entity.getWorld().isClient)
                return;

            tickCounter++;

            // Spawn fire particles every 2 ticks for a nice continuous effect
            if (tickCounter % 2 == 0) {
                float bodyYaw = entity.bodyYaw;

                // Get the world position of the sun orb anchor bone
                Vec3d worldPos = ((RaRenderer) getRenderer()).getBoneWorldPos(entity, SUN_ORB_ANCHOR, bakedModel,
                        entity.getX(), entity.getY(), entity.getZ(), bodyYaw);

                // Spawn multiple fire particles around the orb for a sun-like effect
                for (int i = 0; i < 3; i++) {
                    // Small random offset for particle spread
                    double offsetX = (entity.getRandom().nextFloat() - 0.5) * 0.3;
                    double offsetY = (entity.getRandom().nextFloat() - 0.5) * 0.3;
                    double offsetZ = (entity.getRandom().nextFloat() - 0.5) * 0.3;

                    // Flame particles
                    entity.getWorld().addParticle(
                            ParticleTypes.FLAME,
                            worldPos.x + offsetX,
                            worldPos.y + offsetY,
                            worldPos.z + offsetZ,
                            (entity.getRandom().nextFloat() - 0.5) * 0.02,
                            0.03 + entity.getRandom().nextFloat() * 0.02,
                            (entity.getRandom().nextFloat() - 0.5) * 0.02);
                }

                // Occasionally spawn a soul fire particle for variety (golden/divine look)
                if (tickCounter % 6 == 0) {
                    entity.getWorld().addParticle(
                            ParticleTypes.SOUL_FIRE_FLAME,
                            worldPos.x,
                            worldPos.y,
                            worldPos.z,
                            (entity.getRandom().nextFloat() - 0.5) * 0.01,
                            0.04 + entity.getRandom().nextFloat() * 0.02,
                            (entity.getRandom().nextFloat() - 0.5) * 0.01);
                }
            }
        }
    }
}
