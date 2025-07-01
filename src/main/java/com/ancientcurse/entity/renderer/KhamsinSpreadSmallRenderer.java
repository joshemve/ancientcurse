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
import org.jetbrains.annotations.Nullable;

import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class KhamsinSpreadSmallRenderer extends GeoEntityRenderer<KhamsinSpreadSmallEntity> {
    private static final Identifier TEXTURE = new Identifier(AncientCurse.MOD_ID, "textures/entity/khamsin_spread_small.png");
    
    public KhamsinSpreadSmallRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new KhamsinSpreadSmallModel());
    }
    
    @Override
    public Identifier getTextureLocation(KhamsinSpreadSmallEntity entity) {
        // Use only the normal texture - we'll darken it with color tinting
        return TEXTURE;
    }
    
    @Override
    public void render(KhamsinSpreadSmallEntity entity, float entityYaw, float partialTick, MatrixStack poseStack,
                      VertexConsumerProvider bufferSource, int packedLight) {
        // Apply floating effect
        poseStack.push();
        float bobOffset = MathHelper.sin((entity.age + partialTick) * 0.08f) * 0.15f;
        poseStack.translate(0, bobOffset, 0);
        
        // Enhanced lighting for better visibility
        int enhancedLight = Math.max(packedLight + 0x20, 0xF000F0);
        
        // Render the entity with custom color tinting
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, enhancedLight);
        
        poseStack.pop();
    }
    
    @Override
    public void preRender(MatrixStack poseStack, KhamsinSpreadSmallEntity entity, BakedGeoModel model,
                         VertexConsumerProvider bufferSource, VertexConsumer buffer, boolean isReRender,
                         float partialTick, int packedLight, int packedOverlay, float red, float green,
                         float blue, float alpha) {
        // Calculate transition value (0.0 = dark, 1.0 = normal)
        float transitionValue = entity.getActivationProgress(partialTick);
        
        // Apply darkening effect (0.3 = dark multiplier, 1.0 = normal)
        float darkness = 0.3f + (0.7f * transitionValue);
        
        // Add pulse effect when fully activated
        if (entity.isActivated() && transitionValue >= 1.0f) {
            float pulseIntensity = entity.getPulseIntensity();
            darkness += pulseIntensity * 0.2f;
            darkness = Math.min(darkness, 1.2f); // Allow slight over-brightening for pulse
        }
        
        // Apply the color tinting
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender,
                       partialTick, packedLight, packedOverlay,
                       darkness, darkness, darkness, alpha);
    }

    @Override
    public RenderLayer getRenderType(KhamsinSpreadSmallEntity entity, Identifier texture,
                                   @Nullable VertexConsumerProvider bufferSource,
                                   float partialTick) {
        // Use standard entity cutout for better texture quality
        return RenderLayer.getEntityCutoutNoCull(texture);
    }
}