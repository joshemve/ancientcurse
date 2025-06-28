package com.ancientcurse.entity.renderer;

import com.ancientcurse.entity.ScarabBeetleEntity;
import com.ancientcurse.entity.model.ScarabBeetleModel;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * GeckoLib renderer for the Scarab Beetle entity.
 * Features hurt flash effects and proper scaling for the ground-based creature.
 */
public class ScarabBeetleRenderer extends GeoEntityRenderer<ScarabBeetleEntity> {
    
    public ScarabBeetleRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new ScarabBeetleModel());
        this.shadowRadius = 0.7f; // Moderate shadow for ground creature
    }
    
    @Override
    public RenderLayer getRenderType(ScarabBeetleEntity animatable, Identifier texture, 
                                   VertexConsumerProvider bufferSource, float partialTick) {
        // Use cutout for any translucent parts like antennas
        return RenderLayer.getEntityCutout(texture);
    }
    
    @Override
    public void preRender(MatrixStack poseStack, ScarabBeetleEntity animatable, BakedGeoModel model,
                         VertexConsumerProvider bufferSource, VertexConsumer buffer, boolean isReRender,
                         float partialTick, int packedLight, int packedOverlay, float red, float green,
                         float blue, float alpha) {
        
        // Hurt flash effect - red tint when recently damaged
        if (animatable.lastDamageTime > 0 && animatable.age - animatable.lastDamageTime < 10) {
            red = 1.0f;
            green = 0.4f;
            blue = 0.4f;
        }
        
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, 
                       partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
    
    @Override
    public void render(ScarabBeetleEntity entity, float entityYaw, float partialTick, 
                      MatrixStack poseStack, VertexConsumerProvider bufferSource, int packedLight) {
        
        // Scale down slightly for a more realistic beetle size
        poseStack.push();
        poseStack.scale(0.8f, 0.8f, 0.8f);
        
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        
        poseStack.pop();
    }
} 