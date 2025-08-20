package com.ancientcurse.client.renderer.armor;

import com.ancientcurse.client.model.armor.ScarabMaskModel;
import com.ancientcurse.item.armor.ScarabMaskItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

public class ScarabMaskRenderer extends GeoArmorRenderer<ScarabMaskItem> {
    public ScarabMaskRenderer() {
        super(new ScarabMaskModel());
    }
    
    @Override
    public void preRender(MatrixStack poseStack, ScarabMaskItem animatable, BakedGeoModel model, VertexConsumerProvider bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        // Scale down the entire model
        float scale = 0.5f; // 50% of original size for very noticeable change
        poseStack.scale(scale, scale, scale);
        
        // Translate to keep centered after scaling
        // Adjust these values as needed to maintain position on face
        // Z-axis: negative values move forward (away from face)
        poseStack.translate(0.0f, -0.25f, -0.2f);
        
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}