package com.ancientcurse.entity.renderer;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.HyenaEntity;
import com.ancientcurse.entity.model.HyenaModel;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * GeckoLib renderer for the Hyena entity.
 * Handles rendering with proper scaling and tamed visual effects.
 */
public class HyenaRenderer extends GeoEntityRenderer<HyenaEntity> {

    private static final float SHADOW_RADIUS = 0.5f;
    private static final Identifier EYES_TEXTURE = new Identifier(AncientCurse.MOD_ID, "textures/entity/hyena_eyes_overlay.png");

    public HyenaRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new HyenaModel());
        this.shadowRadius = SHADOW_RADIUS;

        // Add emissive eyes layer
        this.addRenderLayer(new HyenaGlowLayer(this));
    }

    @Override
    public RenderLayer getRenderType(HyenaEntity entity, Identifier texture,
                                     VertexConsumerProvider bufferSource, float partialTick) {
        // Use translucent to properly render eyes and any alpha in the texture
        return RenderLayer.getEntityTranslucent(texture);
    }

    @Override
    public void preRender(MatrixStack poseStack, HyenaEntity entity, BakedGeoModel model,
                          VertexConsumerProvider bufferSource, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha) {

        // Only apply scale/transforms on initial render, not re-renders (like glow layer)
        // This prevents double-scaling when reRender calls preRender again
        if (!isReRender) {
            // Apply scale adjustments
            float scale = 1.0f;

            // Tamed hyenas are slightly larger (well-fed)
            if (entity.isTamed()) {
                scale = 1.1f;
            }

            // Baby hyenas are smaller
            if (entity.isBaby()) {
                scale *= 0.5f;
            }

            poseStack.scale(scale, scale, scale);

            // Adjust shadow based on size
            this.shadowRadius = SHADOW_RADIUS * scale;
        }

        // Apply color tints (these are fine on re-renders)
        float r = red, g = green, b = blue;

        // Tamed hyenas have a subtle warm tint
        if (entity.isTamed()) {
            r = Math.min(1.0f, r + 0.05f);
            g = Math.min(1.0f, g + 0.03f);
        }

        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, r, g, b, alpha);
    }

    /**
     * Emissive eyes layer - renders eye overlay with full brightness
     */
    public static class HyenaGlowLayer extends GeoRenderLayer<HyenaEntity> {

        public HyenaGlowLayer(GeoEntityRenderer<HyenaEntity> entityRenderer) {
            super(entityRenderer);
        }

        @Override
        public void render(MatrixStack poseStack, HyenaEntity entity, BakedGeoModel bakedModel,
                          RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer,
                          float partialTick, int packedLight, int packedOverlay) {

            // Get the emissive eyes render layer (lazily to avoid shader NPE at class init)
            RenderLayer eyesLayer = RenderLayer.getEyes(EYES_TEXTURE);
            VertexConsumer eyesBuffer = bufferSource.getBuffer(eyesLayer);

            // Render eyes at full brightness (0xF000F0 = max light)
            this.getRenderer().reRender(
                bakedModel, poseStack, bufferSource, entity, eyesLayer,
                eyesBuffer, partialTick, 0xF000F0, packedOverlay,
                1.0f, 1.0f, 1.0f, 1.0f
            );
        }

        @Override
        public Identifier getTextureResource(HyenaEntity entity) {
            return EYES_TEXTURE;
        }
    }
}
