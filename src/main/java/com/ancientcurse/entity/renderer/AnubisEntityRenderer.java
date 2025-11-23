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

        // FIXED: Removed MatrixStack transformations that interfere with GeckoLib animations
        // MatrixStack.translate() affects ALL rendering including bone animations, causing arms to "break"
        // Phase-specific scaling is kept minimal and only applied to root entity scale

        poseStack.push();

        // MINIMAL scaling only - removed phase-specific scales that were breaking animations
        // If you need phase effects, implement them in the animation files instead
        float scale = 1.0F; // Use consistent scale to prevent animation glitches
        poseStack.scale(scale, scale, scale);

        // REMOVED: All translate() calls - these were breaking howl animations
        // The constant adjustments to the MatrixStack were conflicting with GeckoLib's
        // bone position calculations, causing arms to move incorrectly

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
