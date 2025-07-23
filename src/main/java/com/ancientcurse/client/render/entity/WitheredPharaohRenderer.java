package com.ancientcurse.client.render.entity;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.WitheredPharaohEntity;
import com.ancientcurse.client.model.WitheredPharaohModel;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Renderer for the Withered Pharaoh entity with glowing overlay effects
 */
public class WitheredPharaohRenderer extends GeoEntityRenderer<WitheredPharaohEntity> {
    // Overlay texture identifiers
    private static final Identifier EYES_TEXTURE = new Identifier(AncientCurse.MOD_ID, "textures/entity/the_withered_pharoh_eyes.png");
    private static final Identifier RUBY_TEXTURE = new Identifier(AncientCurse.MOD_ID, "textures/entity/the_withered_pharoh_ruby.png");
    
    public WitheredPharaohRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new WitheredPharaohModel());
        
        // Adjust shadow size
        this.shadowRadius = 0.5f;
        
        // Add overlay render layers
        addRenderLayer(new GlowingEyesLayer(this));
        addRenderLayer(new GlowingRubyLayer(this));
    }

    @Override
    public Identifier getTextureLocation(WitheredPharaohEntity entity) {
        return new Identifier(AncientCurse.MOD_ID, "textures/entity/the_withered_pharaoh.png");
    }
    
    /**
     * Glowing eyes overlay render layer
     */
    private class GlowingEyesLayer extends GeoRenderLayer<WitheredPharaohEntity> {
        public GlowingEyesLayer(GeoEntityRenderer<WitheredPharaohEntity> entityRenderer) {
            super(entityRenderer);
        }

        @Override
        public void render(MatrixStack matrixStack, WitheredPharaohEntity entity, BakedGeoModel bakedModel,
                          RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer,
                          float partialTick, int packedLight, int packedOverlay) {
            // Calculate pulsing glow effect
            float alpha = 0.7f + MathHelper.sin(entity.age * 0.1f) * 0.3f;
            
            // Use entity translucent emissive layer for proper transparency
            VertexConsumer emissiveBuffer = bufferSource.getBuffer(
                    RenderLayer.getEntityTranslucentEmissive(EYES_TEXTURE, true));
            
            // Render the glowing eyes overlay
            getRenderer().reRender(bakedModel, matrixStack, bufferSource, entity, renderType,
                    emissiveBuffer, partialTick, 15728640, OverlayTexture.DEFAULT_UV,
                    1.0f, 1.0f, 1.0f, alpha);
        }
    }
    
    /**
     * Glowing ruby overlay render layer
     */
    private class GlowingRubyLayer extends GeoRenderLayer<WitheredPharaohEntity> {
        public GlowingRubyLayer(GeoEntityRenderer<WitheredPharaohEntity> entityRenderer) {
            super(entityRenderer);
        }

        @Override
        public void render(MatrixStack matrixStack, WitheredPharaohEntity entity, BakedGeoModel bakedModel,
                          RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer,
                          float partialTick, int packedLight, int packedOverlay) {
            // Calculate pulsing glow effect with different timing than eyes
            float alpha = 0.6f + MathHelper.sin(entity.age * 0.07f) * 0.4f;
            
            // Use entity translucent emissive layer for proper transparency
            VertexConsumer emissiveBuffer = bufferSource.getBuffer(
                    RenderLayer.getEntityTranslucentEmissive(RUBY_TEXTURE, true));
            
            // Render the glowing ruby overlay
            getRenderer().reRender(bakedModel, matrixStack, bufferSource, entity, renderType,
                    emissiveBuffer, partialTick, 15728640, OverlayTexture.DEFAULT_UV,
                    1.0f, 0.3f, 0.3f, alpha); // Reddish tint
        }
    }
    

}
