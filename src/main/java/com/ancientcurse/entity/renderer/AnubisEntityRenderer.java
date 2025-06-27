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
        
        poseStack.push();
        
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
        poseStack.scale(scale, scale, scale);
        
        // Apply phase-specific visual effects using pose stack
        switch (entity.getBossPhase()) {
            case AWAKENING:
                // Trembling effect during awakening
                float shake = (float) (Math.sin(entity.age * 0.3f) * 0.02f);
                poseStack.translate(shake, 0, shake);
                break;
                
            case JUDGING:
                // Subtle floating effect during judgment
                float judgeFloat = (float) (Math.sin(entity.age * 0.05f) * 0.05f);
                poseStack.translate(0, judgeFloat, 0);
                break;
                
            case MERCIFUL:
                // Gentle swaying when merciful
                float sway = (float) (Math.sin(entity.age * 0.02f) * 0.03f);
                poseStack.translate(sway, 0, 0);
                break;
                
            case ENRAGED:
                // Aggressive trembling when enraged
                float rage = (float) (Math.sin(entity.age * 0.4f) * 0.01f);
                poseStack.translate(rage, rage * 0.5f, rage);
                break;
        }
        
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        
        poseStack.pop();
    }

    @Override
    public void renderRecursively(MatrixStack poseStack, AnubisEntity animatable, GeoBone bone,
                                 net.minecraft.client.render.RenderLayer renderType, VertexConsumerProvider bufferSource,
                                 net.minecraft.client.render.VertexConsumer buffer, boolean isReRender,
                                 float partialTick, int packedLight, int packedOverlay,
                                 float red, float green, float blue, float alpha) {

        // IMPORTANT: Direct bone manipulation causes drift over time!
        // The issue was that modifying bone positions directly (setPivotX/Y) causes cumulative changes
        // that make body parts separate. Instead, let GeckoLib handle animations through the
        // animation files, or use MatrixStack transformations that don't permanently modify bones.
        
        // For now, we'll rely on the animation system to handle all bone movements
        // If custom effects are needed, they should be done through:
        // 1. Animation files (recommended)
        // 2. MatrixStack transformations (for temporary effects)
        // 3. Storing original positions and resetting them each frame

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
