package com.ancientcurse.entity.renderer;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.model.BabyLocusModel;
import com.ancientcurse.entity.BabyLocusEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer for the Baby Locus Entity
 * 
 * Features:
 * - Smaller size (0.5x scale)
 * - Same basic animations as adult
 * - Uses baby_locus texture
 */
public class BabyLocusRenderer extends GeoEntityRenderer<BabyLocusEntity> {
    
    public BabyLocusRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new BabyLocusModel());
        this.shadowRadius = 0.3F; // Smaller shadow
    }

    @Override
    public Identifier getTextureLocation(BabyLocusEntity entity) {
        return new Identifier(AncientCurse.MOD_ID, "textures/entity/baby_locus.png");
    }

    @Override
    public RenderLayer getRenderType(BabyLocusEntity animatable, Identifier texture, 
                                   VertexConsumerProvider bufferSource, float partialTick) {
        return RenderLayer.getEntityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public void preRender(MatrixStack poseStack, BabyLocusEntity animatable, BakedGeoModel model, 
                         VertexConsumerProvider bufferSource, VertexConsumer buffer, 
                         boolean isReRender, float partialTick, int packedLight, 
                         int packedOverlay, float red, float green, float blue, float alpha) {
        
        // Scale down to 50% size for babies
        poseStack.push();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        
        super.preRender(poseStack, animatable, model, bufferSource, buffer, 
                       isReRender, partialTick, packedLight, packedOverlay, 
                       red, green, blue, alpha);
        
        poseStack.pop();
    }

    @Override
    public void actuallyRender(MatrixStack poseStack, BabyLocusEntity animatable, 
                              BakedGeoModel model, RenderLayer renderType, 
                              VertexConsumerProvider bufferSource, VertexConsumer buffer, 
                              boolean isReRender, float partialTick, int packedLight, 
                              int packedOverlay, float red, float green, float blue, float alpha) {
        
        // Hurt flash effect
        if (animatable.lastDamageTime > 0 && 
            animatable.age - animatable.lastDamageTime < 10) {
            red = 1.0f;
            green = 0.3f;
            blue = 0.3f;
        }
        
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, 
                           isReRender, partialTick, packedLight, packedOverlay, 
                           red, green, blue, alpha);
    }
}