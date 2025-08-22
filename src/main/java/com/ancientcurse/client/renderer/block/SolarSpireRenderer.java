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
import org.joml.Matrix3f;
import org.joml.Matrix4f;
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
        
        // Render power-up overlay if powering up OR at stage 7 (working state)
        if (animatable.isPoweringUp() || animatable.getPowerUpStage() == 7) {
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
        
        // Show overlay if we're in any power-up stage (1-7)
        // Stage 7 stays active during working state until deactivated
        if (powerUpStage < 1) return;
        
        // Always use power_up_7 texture when at stage 7 (it stays at 7 during working state)
        int textureIndex = Math.min(powerUpStage - 1, 6); // Clamp to max index 6 (power_up_7)
        Identifier powerUpTexture = POWER_UP_TEXTURES[textureIndex];
        
        // Full intensity when at stage 7 (which includes working state)
        float glowIntensity = (powerUpStage == 7) ? 1.0f : (float)powerUpStage / 7f;
        
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
        // Render sun beacon during working state
        if (animatable.isWorking() && animatable.hasEye()) {
            renderSunBeacon(poseStack, bufferSource, animatable);
        }
        
        // Render the Eye of Apophis if the spire has one
        if (animatable.hasEye()) {
            poseStack.push();
            
            // Calculate the eye position with sliding animation
            float appearProgress = animatable.getEyeAppearProgress();
            float startY = (56.0f - 16.0f) / 16.0f; // Start 1 block lower (2.5 blocks high)
            float endY = 56.0f / 16.0f; // End at 3.5 blocks high
            float currentY = startY + (endY - startY) * appearProgress;
            
            poseStack.translate(0, currentY, 0);
            
            // Apply bobbing effect (only after fully appeared)
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
    
    private void renderSunBeacon(MatrixStack poseStack, VertexConsumerProvider bufferSource, SolarSpireBlockEntity animatable) {
        poseStack.push();
        
        // Position at 2 blocks high
        poseStack.translate(0, 2.0, 0); // Start at 2 blocks high
        
        // Get the beacon texture
        VertexConsumer beaconBuffer = bufferSource.getBuffer(RenderLayer.getBeaconBeam(
            new Identifier("textures/entity/beacon_beam.png"), true));
        
        // Calculate animation values
        long time = animatable.getWorld().getTime();
        float pulseAlpha = 0.8f + (float)Math.sin(time * 0.1) * 0.2f; // Subtle pulsing
        
        // Render a vertical beam going upward only
        float beamHeight = 256.0f; // Extend to build height
        float beamRadius = 0.1f; // Thin beam
        
        // Orange-yellow sun energy color (more orange)
        float red = 1.0f;
        float green = 0.7f;  // Less green for more orange
        float blue = 0.1f;   // Much less blue for warmer color
        
        // Draw single beam segment going up
        renderBeamSegment(poseStack, beaconBuffer, red, green, blue, pulseAlpha, 
                         0.0f, beamHeight,          // Start at 0, go up
                         -beamRadius, -beamRadius,  // x1, z1
                         beamRadius, -beamRadius,   // x2, z2
                         beamRadius, beamRadius,    // x3, z3
                         -beamRadius, beamRadius,   // x4, z4
                         0.0f, 1.0f,               // u1, u2
                         0.0f, 1.0f,               // v1, v2 - fixed texture coordinates
                         beamHeight);
        
        poseStack.pop();
    }
    
    private static void renderBeamSegment(MatrixStack matrices, VertexConsumer vertices, float red, float green,
                                          float blue, float alpha, float yOffset, float height, float x1, float z1,
                                          float x2, float z2, float x3, float z3, float x4, float z4, float u1,
                                          float u2, float v1, float v2, float beamHeight) {
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f positionMatrix = entry.getPositionMatrix();
        Matrix3f normalMatrix = entry.getNormalMatrix();
        
        // Draw the four sides of the beam
        renderBeamFace(positionMatrix, normalMatrix, vertices, red, green, blue, alpha, yOffset, height,
                      x1, z1, x2, z2, u1, u2, v1, v2);
        renderBeamFace(positionMatrix, normalMatrix, vertices, red, green, blue, alpha, yOffset, height,
                      x4, z4, x3, z3, u1, u2, v1, v2);
        renderBeamFace(positionMatrix, normalMatrix, vertices, red, green, blue, alpha, yOffset, height,
                      x2, z2, x4, z4, u1, u2, v1, v2);
        renderBeamFace(positionMatrix, normalMatrix, vertices, red, green, blue, alpha, yOffset, height,
                      x3, z3, x1, z1, u1, u2, v1, v2);
    }
    
    private static void renderBeamFace(Matrix4f positionMatrix, Matrix3f normalMatrix, VertexConsumer vertices,
                                       float red, float green, float blue, float alpha, float yOffset, float height,
                                       float x1, float z1, float x2, float z2, float u1, float u2, float v1, float v2) {
        addBeamVertex(positionMatrix, normalMatrix, vertices, red, green, blue, alpha, x1, yOffset, z1, u2, v1);
        addBeamVertex(positionMatrix, normalMatrix, vertices, red, green, blue, alpha, x1, yOffset + height, z1, u2, v2);
        addBeamVertex(positionMatrix, normalMatrix, vertices, red, green, blue, alpha, x2, yOffset + height, z2, u1, v2);
        addBeamVertex(positionMatrix, normalMatrix, vertices, red, green, blue, alpha, x2, yOffset, z2, u1, v1);
    }
    
    private static void addBeamVertex(Matrix4f positionMatrix, Matrix3f normalMatrix, VertexConsumer vertices,
                                      float red, float green, float blue, float alpha, float x, float y, float z,
                                      float u, float v) {
        vertices.vertex(positionMatrix, x, y, z)
                .color(red, green, blue, alpha)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(15728880) // Full bright
                .normal(normalMatrix, 0.0F, 1.0F, 0.0F)
                .next();
    }
}