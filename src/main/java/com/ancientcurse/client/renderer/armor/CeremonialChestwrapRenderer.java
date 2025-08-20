package com.ancientcurse.client.renderer.armor;

import com.ancientcurse.client.model.armor.CeremonialChestwrapModel;
import com.ancientcurse.item.CeremonialChestwrapItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

public class CeremonialChestwrapRenderer extends GeoArmorRenderer<CeremonialChestwrapItem> {
    public CeremonialChestwrapRenderer() {
        super(new CeremonialChestwrapModel());
    }
    
    @Override
    public void preRender(MatrixStack poseStack, CeremonialChestwrapItem animatable, BakedGeoModel model, VertexConsumerProvider bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        // Scale the model slightly larger to prevent clipping with the player model
        poseStack.scale(1.02f, 1.01f, 1.02f);
        
        // Also translate very slightly outward on the Z axis to prevent z-fighting
        poseStack.translate(0.0f, 0.0f, -0.005f);
        
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}