package com.ancientcurse.entity.renderer;

import com.ancientcurse.entity.AnubisEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import software.bernie.geckolib.cache.object.GeoBone;

/**
 * Renderer for the Anubis boss entity
 * Handles model, texture, and animation rendering with special effects for boss phases
 */
public class AnubisEntityRenderer extends GeoEntityRenderer<AnubisEntity> {

    public AnubisEntityRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new AnubisModel());
        this.shadowRadius = 1.5F; // Boss shadow
    }

    @Override
    public void render(AnubisEntity entity, float entityYaw, float partialTick, MatrixStack poseStack,
                      VertexConsumerProvider bufferSource, int packedLight) {
        
        // Scale based on boss phase for dramatic effect
        float scale = switch (entity.getBossPhase()) {
            case AWAKENING -> 1.1F;
            case JUDGING -> 1.15F;
            case ENRAGED -> 1.2F;
            case MERCIFUL -> 1.05F;
            case COMBAT -> 1.1F;
            case DORMANT -> 1.0F;
            case DEAD -> 0.9F;
            default -> 1.0F;
        };

        poseStack.push();
        poseStack.scale(scale, scale, scale);
        
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        
        poseStack.pop();
    }

    @Override
    public void renderRecursively(MatrixStack poseStack, AnubisEntity animatable, GeoBone bone,
                                 net.minecraft.client.render.RenderLayer renderType, VertexConsumerProvider bufferSource,
                                 net.minecraft.client.render.VertexConsumer buffer, boolean isReRender,
                                 float partialTick, int packedLight, int packedOverlay,
                                 float red, float green, float blue, float alpha) {

        // Apply global transformations to the root bone to ensure the whole model moves together
        if (bone.getName().equals("root")) {
            // Subtle, constant floating and swaying
            float sway = (float) Math.sin(animatable.age * 0.05f) * 0.03f;
            float hover = (float) Math.sin(animatable.age * 0.07f) * 0.1f;
            bone.setPivotY(bone.getPivotY() + hover);
            bone.setRotZ(bone.getRotZ() + sway);

            // Handle the more pronounced hovering effect
            if (animatable.isHovering()) {
                bone.setPivotY(bone.getPivotY() +
                    (float) (Math.sin(animatable.age * 0.1f) * 1.0f)); // Reduced from 4.0f to be less extreme
            }
        }

        // Special bone manipulations for different phases
        switch (animatable.getBossPhase()) {
            case JUDGING:
                // Subtle floating animation during judgment, applied to root
                if (bone.getName().equals("root")) {
                    bone.setPivotY(bone.getPivotY() +
                        (float) (Math.sin(animatable.age * 0.05f) * 0.5f)); // Reduced from 2.0f
                }
                break;

            case AWAKENING:
                // Trembling effect during awakening - this is a local shake, so it's fine on head/body
                if (bone.getName().equals("head") || bone.getName().equals("body")) {
                    bone.setPivotX(bone.getPivotX() +
                        (float) (Math.sin(animatable.age * 0.3f) * 0.5f));
                }
                break;

            case ENRAGED:
                // Aggressive trembling when enraged - local shake on head
                if (bone.getName().equals("head")) {
                    bone.setPivotX(bone.getPivotX() +
                        (float) (Math.sin(animatable.age * 0.4f) * 1.0f));
                    bone.setPivotY(bone.getPivotY() +
                        (float) (Math.cos(animatable.age * 0.3f) * 0.8f));
                }
                break;

            case MERCIFUL:
                // Gentle swaying when merciful - this should also be on root
                if (bone.getName().equals("root")) {
                    bone.setPivotX(bone.getPivotX() +
                        (float) (Math.sin(animatable.age * 0.02f) * 0.2f)); // Reduced from 1.5f
                }
                break;

            case COMBAT:
                // Combat stance adjustments - this can be on body/arms if they are separate parts that need to move
                if (bone.getName().equals("arms") || bone.getName().equals("body")) {
                    bone.setPivotY(bone.getPivotY() - 1.0f); // Lower stance
                }
                break;

            case DORMANT:
                // Minimal movement when dormant - chest is fine if it's a subtle breathing animation
                if (bone.getName().equals("chest")) {
                    bone.setPivotY(bone.getPivotY() +
                        (float) (Math.sin(animatable.age * 0.01f) * 0.3f));
                }
                break;

            case DEAD:
                // Death pose - falling over should affect the whole model via root
                if (bone.getName().equals("root")) {
                    bone.setRotZ(bone.getRotZ() + (float) Math.toRadians(90)); // Fall over
                }
                break;
        }

        // Handle special sky yell animation - head-specific is fine
        if (animatable.isSkyYelling() && bone.getName().equals("head")) {
            bone.setRotX(bone.getRotX() - (float) Math.toRadians(45)); // Look up
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer,
                               isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public Identifier getTextureLocation(AnubisEntity animatable) {
        return new Identifier("ancientcurse", "textures/entity/anubis.png");
    }

    /**
     * Inner model class that provides the geo model, texture, and animation resources
     */
    private static class AnubisModel extends GeoModel<AnubisEntity> {
        
        @Override
        public Identifier getModelResource(AnubisEntity animatable) {
            return new Identifier("ancientcurse", "geo/anubis.geo.json");
        }

        @Override
        public Identifier getTextureResource(AnubisEntity animatable) {
            return new Identifier("ancientcurse", "textures/entity/anubis.png");
        }

        @Override
        public Identifier getAnimationResource(AnubisEntity animatable) {
            return new Identifier("ancientcurse", "animations/anubis.animation.json");
        }
    }
}
