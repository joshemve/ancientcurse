package com.ancientcurse.client.renderer.block;

import com.ancientcurse.ModItems;
import com.ancientcurse.block.entity.SolarSpireBlockEntity;
import com.ancientcurse.client.model.block.SolarSpireModel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class SolarSpireRenderer extends GeoBlockRenderer<SolarSpireBlockEntity> {
    
    public SolarSpireRenderer(BlockEntityRendererFactory.Context context) {
        super(new SolarSpireModel());
    }
    
    @Override
    public void postRender(MatrixStack poseStack, SolarSpireBlockEntity animatable, BakedGeoModel model, VertexConsumerProvider bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        // Render the Eye of Apophis if the spire has one
        if (animatable.hasEye()) {
            poseStack.push();
            
            // Use the locator anchor from the model - "top_particle_locator_anchor" at [0, 47, 0]
            // The model coordinates are in pixels, so we need to convert to blocks (divide by 16)
            poseStack.translate(0.5, 47.0 / 16.0, 0.5); // Center X/Z, and use the Y from locator
            
            // Apply bobbing effect
            float bobOffset = animatable.getEyeBobOffset();
            poseStack.translate(0, bobOffset, 0);
            
            // Apply rotation
            float rotation = animatable.getEyeRotation();
            poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));
            
            // Scale the eye
            float scale = 0.5f;
            poseStack.scale(scale, scale, scale);
            
            // Render the Eye of Apophis item
            ItemStack eyeStack = new ItemStack(ModItems.EYE_OF_APOPHIS);
            MinecraftClient.getInstance().getItemRenderer().renderItem(
                eyeStack,
                ModelTransformationMode.FIXED,
                packedLight,
                OverlayTexture.DEFAULT_UV,
                poseStack,
                bufferSource,
                animatable.getWorld(),
                0
            );
            
            poseStack.pop();
        }
        
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}