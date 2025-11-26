package com.ancientcurse.entity.renderer;

import com.ancientcurse.entity.ZulmakEntity;
import com.ancientcurse.entity.model.ZulmakModel;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Zulmak Entity Renderer
 *
 * Simple GeckoLib renderer - expand with render layers for:
 * - Glowing effects
 * - Particle effects
 * - Armor overlays
 * - Ghostly downed phase effect
 */
public class ZulmakRenderer extends GeoEntityRenderer<ZulmakEntity> {

    public ZulmakRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new ZulmakModel());

        // Shadow size - adjust based on entity size
        this.shadowRadius = 0.6f;

        // Add render layers here for special effects:
        // this.addRenderLayer(new ZulmakGlowLayer(this));
    }

    @Override
    public void preRender(MatrixStack poseStack, ZulmakEntity animatable, BakedGeoModel model,
                          VertexConsumerProvider bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        // Apply ghostly transparency during downed phase
        if (animatable.isDowned()) {
            // Pulse the alpha based on downed ticks for a mystical effect
            float progress = (float) animatable.getDownedTicks() * 0.05f;
            float pulseAlpha = 0.4f + 0.2f * (float) Math.sin(progress);
            // Shift color toward cyan/teal for mystical look
            super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, 0.6f, 0.9f, 1.0f, pulseAlpha);
        } else {
            super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        }
    }

    @Override
    public RenderLayer getRenderType(ZulmakEntity animatable, Identifier texture,
                                     VertexConsumerProvider bufferSource, float partialTick) {
        // Use translucent render layer during downed phase for ghostly effect
        if (animatable.isDowned()) {
            return RenderLayer.getEntityTranslucent(texture);
        }
        return super.getRenderType(animatable, texture, bufferSource, partialTick);
    }
}
