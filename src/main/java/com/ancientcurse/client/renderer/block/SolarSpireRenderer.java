package com.ancientcurse.client.renderer.block;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.ModItems;
import com.ancientcurse.block.entity.SolarSpireBlockEntity;
import com.ancientcurse.client.model.block.SolarSpireModel;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class SolarSpireRenderer extends GeoBlockRenderer<SolarSpireBlockEntity> {
    
    // Texture overlays for animations
    private static final Identifier[] LAVA_TEXTURES = new Identifier[] {
        new Identifier(AncientCurse.MOD_ID, "textures/block/solarspire_lava_1.png"),
        new Identifier(AncientCurse.MOD_ID, "textures/block/solarspire_lava_2.png"),
        new Identifier(AncientCurse.MOD_ID, "textures/block/solarspire_lava_3.png")
    };
    
    private static final Identifier[] POWER_UP_TEXTURES = new Identifier[] {
        new Identifier(AncientCurse.MOD_ID, "textures/block/solarspire_power_up_1.png"),
        new Identifier(AncientCurse.MOD_ID, "textures/block/solarspire_power_up_2.png"),
        new Identifier(AncientCurse.MOD_ID, "textures/block/solarspire_power_up_3.png"),
        new Identifier(AncientCurse.MOD_ID, "textures/block/solarspire_power_up_4.png"),
        new Identifier(AncientCurse.MOD_ID, "textures/block/solarspire_power_up_5.png"),
        new Identifier(AncientCurse.MOD_ID, "textures/block/solarspire_power_up_6.png"),
        new Identifier(AncientCurse.MOD_ID, "textures/block/solarspire_power_up_7.png")
    };
    
    public SolarSpireRenderer(BlockEntityRendererFactory.Context context) {
        super(new SolarSpireModel());
    }
    
    
    private void renderOverlays(MatrixStack poseStack, SolarSpireBlockEntity animatable, BakedGeoModel model, VertexConsumerProvider bufferSource, int packedLight, int packedOverlay, float partialTick) {
        // Always render lava animation overlay
        renderLavaOverlay(poseStack, animatable, model, bufferSource, packedLight, packedOverlay, partialTick);
        
        // Render power-up overlay if activating
        if (animatable.isPoweringUp()) {
            renderPowerUpOverlay(poseStack, animatable, model, bufferSource, packedLight, packedOverlay, partialTick);
        }
    }
    
    private void renderLavaOverlay(MatrixStack poseStack, SolarSpireBlockEntity animatable, BakedGeoModel model, VertexConsumerProvider bufferSource, int packedLight, int packedOverlay, float partialTick) {
        // Calculate which lava texture to use (cycles through 1-3)
        long time = animatable.getWorld() != null ? animatable.getWorld().getTime() : 0;
        int lavaFrame = (int)((time / 8) % 3); // Change every 8 ticks
        
        Identifier lavaTexture = LAVA_TEXTURES[lavaFrame];
        RenderLayer renderLayer = RenderLayer.getEntityTranslucent(lavaTexture);
        VertexConsumer lavaBuffer = bufferSource.getBuffer(renderLayer);
        
        // Render the overlay using actuallyRender to maintain positioning
        actuallyRender(poseStack, animatable, model, renderLayer, bufferSource, lavaBuffer,
                      true, partialTick, packedLight, packedOverlay, 1f, 1f, 1f, 0.7f);
    }
    
    private void renderPowerUpOverlay(MatrixStack poseStack, SolarSpireBlockEntity animatable, BakedGeoModel model, VertexConsumerProvider bufferSource, int packedLight, int packedOverlay, float partialTick) {
        int powerUpStage = animatable.getPowerUpStage();
        if (powerUpStage < 1 || powerUpStage > 7) return;
        
        Identifier powerUpTexture = POWER_UP_TEXTURES[powerUpStage - 1];
        
        // Calculate glow intensity based on stage
        float glowIntensity = (float)powerUpStage / 7f;
        
        // Make it glow brighter by increasing light level
        int glowLight = 0xF000F0; // Full bright for glow effect
        
        // Use additive blending for glow effect
        RenderLayer renderLayer = RenderLayer.getEntityTranslucentEmissive(powerUpTexture);
        VertexConsumer powerUpBuffer = bufferSource.getBuffer(renderLayer);
        
        // Render the overlay using actuallyRender to maintain positioning
        actuallyRender(poseStack, animatable, model, renderLayer, bufferSource, powerUpBuffer,
                      true, partialTick, glowLight, packedOverlay, 
                      1f, 1f, 0.8f + (glowIntensity * 0.2f), glowIntensity);
    }
    
    @Override
    public void actuallyRender(MatrixStack poseStack, SolarSpireBlockEntity animatable, BakedGeoModel model,
                              RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer,
                              boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                              float red, float green, float blue, float alpha) {
        // First render the base model
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer,
                           isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        
        // Then render overlays (only when rendering the base texture to avoid duplicates)
        if (!isReRender && renderType.toString().contains("solarspire.png")) {
            renderOverlays(poseStack, animatable, model, bufferSource, packedLight, packedOverlay, partialTick);
        }
    }
    
    @Override
    public void postRender(MatrixStack poseStack, SolarSpireBlockEntity animatable, BakedGeoModel model, VertexConsumerProvider bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        // Render the Eye of Apophis if the spire has one
        if (animatable.hasEye()) {
            poseStack.push();
            
            // Use the locator anchor from the model - "top_particle_locator_anchor" at [0, 47, 0]
            // The model coordinates are in pixels, so we need to convert to blocks (divide by 16)
            // Set to 3.5 blocks height as requested
            poseStack.translate(0, 56.0 / 16.0, 0); // 56 pixels = 3.5 blocks high
            
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