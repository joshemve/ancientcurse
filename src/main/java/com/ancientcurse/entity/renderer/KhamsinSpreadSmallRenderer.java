package com.ancientcurse.entity.renderer;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.KhamsinSpreadSmallEntity;
import com.ancientcurse.entity.model.KhamsinSpreadSmallModel;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class KhamsinSpreadSmallRenderer extends GeoEntityRenderer<KhamsinSpreadSmallEntity> {
    private float lastPulseIntensity = 0f;
    private long lastRenderTime = 0;
    
    public KhamsinSpreadSmallRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new KhamsinSpreadSmallModel());
    }
    
    @Override
    public Identifier getTextureLocation(KhamsinSpreadSmallEntity entity) {
        // Switch between dark and normal texture based on activation state
        if (entity.isActivated()) {
            return new Identifier(AncientCurse.MOD_ID, "textures/entity/khamsin_spread_small.png");
        } else {
            return new Identifier(AncientCurse.MOD_ID, "textures/entity/khamsin_spread_small_dark.png");
        }
    }
    
    @Override
    public void render(KhamsinSpreadSmallEntity entity, float entityYaw, float partialTick, MatrixStack poseStack,
                      VertexConsumerProvider bufferSource, int packedLight) {
        // Apply floating effect
        poseStack.push();
        float bobOffset = MathHelper.sin((entity.age + partialTick) * 0.08f) * 0.15f;
        poseStack.translate(0, bobOffset, 0);
        
        // Enhanced lighting for better visibility
        int enhancedLight = Math.max(packedLight + 0x20, 0x80F0);
        
        // Get current time for smooth interpolation
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min(0.1f, (currentTime - lastRenderTime) / 1000f);
        lastRenderTime = currentTime;
        
        // Get pulse intensity and smooth it
        float targetPulseIntensity = entity.getPulseIntensity();
        float pulseIntensity = MathHelper.lerp(deltaTime * 10f, lastPulseIntensity, targetPulseIntensity);
        lastPulseIntensity = pulseIntensity;
        
        // Calculate color and alpha based on activation and pulse intensity
        float alpha = entity.isActivated() ? 0.8f + (pulseIntensity * 0.2f) : 0.6f;
        
        // Use normal brightness for activated state, darker for dormant state
        float brightness = entity.isActivated() ? 1.0f : 0.4f;
        
        // Add pulse effect to brightness when activated
        if (entity.isActivated()) {
            brightness += pulseIntensity * 0.3f;
        }
        
        // Get the model and render type
        BakedGeoModel model = this.model.getBakedModel(this.getGeoModel().getModelResource(entity));
        RenderLayer renderType = this.getRenderType(entity, this.getTextureLocation(entity), bufferSource, partialTick);
        
        // Apply color and render
        this.actuallyRender(
            poseStack, entity, model, renderType,
            bufferSource, bufferSource.getBuffer(renderType),
            false, partialTick, enhancedLight, 0,
            brightness, brightness, brightness, alpha
        );
        
        poseStack.pop();
    }
    
    @Override
    public void actuallyRender(MatrixStack poseStack, KhamsinSpreadSmallEntity animatable, BakedGeoModel model, 
                              RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer,
                              boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                              float red, float green, float blue, float alpha) {
        // Apply the color and alpha to the model
        poseStack.push();
        poseStack.translate(0, 0.01f, 0); // Slight offset to prevent z-fighting
        
        // Ensure we use neutral colors (no red tinting) and let the texture do the work
        float neutralRed = red;
        float neutralGreen = green;
        float neutralBlue = blue;
        
        // Let the parent class handle the actual rendering with our modified colors
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, 
                           isReRender, partialTick, packedLight, packedOverlay, 
                           neutralRed, neutralGreen, neutralBlue, alpha);
        
        poseStack.pop();
    }

    @Override
    public RenderLayer getRenderType(KhamsinSpreadSmallEntity entity, Identifier texture, 
                                   @org.jetbrains.annotations.Nullable VertexConsumerProvider bufferSource, 
                                   float partialTick) {
        // Use translucent render layer for smooth blending
        return RenderLayer.getEntityTranslucent(texture);
    }
}