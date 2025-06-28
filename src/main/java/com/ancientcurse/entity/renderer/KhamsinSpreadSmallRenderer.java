package com.ancientcurse.entity.renderer;

import com.ancientcurse.entity.KhamsinSpreadSmallEntity;
import com.ancientcurse.entity.model.KhamsinSpreadSmallModel;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class KhamsinSpreadSmallRenderer extends GeoEntityRenderer<KhamsinSpreadSmallEntity> {
    
    public KhamsinSpreadSmallRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new KhamsinSpreadSmallModel());
    }
    
    @Override
    public void render(KhamsinSpreadSmallEntity entity, float entityYaw, float partialTick, MatrixStack poseStack,
                      VertexConsumerProvider bufferSource, int packedLight) {
        poseStack.push();
        
        // Floating effect - gentle bobbing
        float bobOffset = MathHelper.sin((entity.age + partialTick) * 0.08f) * 0.15f;
        poseStack.translate(0, bobOffset, 0);
        
        // No special lighting effects - just natural lighting
        
        // Render the entity
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        
        poseStack.pop();
    }

    @Override
    public RenderLayer getRenderType(KhamsinSpreadSmallEntity entity, Identifier texture, VertexConsumerProvider bufferSource, float partialTick) {
        // Always use translucent render layer for smooth blending, no outline
        return RenderLayer.getEntityTranslucent(texture);
    }
} 