package com.ancientcurse.client.renderer.armor;

import com.ancientcurse.client.model.armor.ScarabMaskModel;
import com.ancientcurse.item.armor.ScarabMaskItem;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * Renderer for Scarab Mask armor piece.
 * Scales down the model when worn on player's head.
 */
public class ScarabMaskRenderer extends GeoArmorRenderer<ScarabMaskItem> {
    public ScarabMaskRenderer() {
        super(new ScarabMaskModel());
    }

    @Override
    public void actuallyRender(MatrixStack poseStack, ScarabMaskItem animatable, BakedGeoModel model,
                               net.minecraft.client.render.RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer,
                               boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        // Scale down the mask before rendering (65% size to fit better on player head)
        poseStack.push();
        poseStack.scale(0.65f, 0.65f, 0.65f);

        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        poseStack.pop();
    }
}
