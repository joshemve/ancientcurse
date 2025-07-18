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
    private static final Identifier TEXTURE_LIGHT = new Identifier(AncientCurse.MOD_ID, "textures/entity/khamsin_spread_small.png");
    private static final Identifier TEXTURE_DARK = new Identifier(AncientCurse.MOD_ID, "textures/entity/khamsin_spread_small_dark.png");
    
    public KhamsinSpreadSmallRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new KhamsinSpreadSmallModel());
    }
    
    @Override
    public Identifier getTextureLocation(KhamsinSpreadSmallEntity entity) {
        // Use dark texture when not activated
        return entity.isActivated() ? TEXTURE_LIGHT : TEXTURE_DARK;
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
        // Check if entity is hurt for damage flash
        if (entity.hurtTime > 0) {
            // Apply red tint for damage
            float hurtIntensity = (float)entity.hurtTime / (float)entity.maxHurtTime;
            super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender,
                           partialTick, packedLight, packedOverlay,
                           1.0f, 1.0f - hurtIntensity * 0.5f, 1.0f - hurtIntensity * 0.5f, alpha);
        } else if (entity.isActivated()) {
            // Add subtle pulse effect when activated
            float pulseIntensity = entity.getPulseIntensity();
            float brightness = 1.0f + pulseIntensity * 0.2f;
            super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender,
                           partialTick, packedLight, packedOverlay,
                           brightness, brightness, brightness, alpha);
        } else {
            // Normal rendering
            super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender,
                           partialTick, packedLight, packedOverlay,
                           red, green, blue, alpha);
        }
    }

    @Override
    public RenderLayer getRenderType(KhamsinSpreadSmallEntity entity, Identifier texture,
                                   @Nullable VertexConsumerProvider bufferSource,
                                   float partialTick) {
        // Use standard entity cutout for better texture quality
        return RenderLayer.getEntityCutoutNoCull(texture);
    }
}