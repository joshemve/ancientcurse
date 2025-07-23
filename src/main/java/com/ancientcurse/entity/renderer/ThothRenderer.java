package com.ancientcurse.entity.renderer;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.ThothEntity;
import com.ancientcurse.entity.model.ThothModel;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Thoth Entity Renderer with magical effects and boss-appropriate visual enhancements
 * 
 * Performance optimizations:
 * - Cached RenderLayers to avoid map lookups each frame
 * - Early bailout conditions in render layers when effects not needed
 * - Proper packed light bit manipulation to avoid overflow
 * - VertexConsumer caching in multi-pass rendering
 * - Entity getter methods instead of magic number constants
 * - Enhanced visual effects with proper charge progression
 * 
 * Note: For better performance, consider implementing client-side 
 * particle generation in a client tick event handler rather than
 * sending particles from the server for ambient effects
 */
public class ThothRenderer extends GeoEntityRenderer<ThothEntity> {
    
    private static final Identifier TEXTURE_GREEN = new Identifier(AncientCurse.MOD_ID, "textures/entity/thoth_green.png");
    private static final Identifier EYES_TEXTURE = new Identifier(AncientCurse.MOD_ID, "textures/entity/thoth_eyes.png");
    
    // Cached RenderLayers to avoid map lookups each frame
    private static final RenderLayer RENDER_LAYER_GREEN = RenderLayer.getEntityTranslucent(TEXTURE_GREEN);
    // Do NOT cache the eyes RenderLayer statically because the underlying shader may not be loaded yet at
    // class-init time (causes NPE inside RenderPhase.startDrawing). Generate it lazily whenever it is needed.
    // private static final RenderLayer getEyesLayer() = RenderLayer.getEyes(EYES_TEXTURE);
    private static RenderLayer getEyesLayer() {
        return RenderLayer.getEyes(EYES_TEXTURE);
    }
    
    // Attack state constants (matching ThothEntity)
    private static final int ATTACK_NONE = 0;
    private static final int ATTACK_MAGIC_BALL = 1;
    private static final int ATTACK_SCROLL_BLAST = 2;
    private static final int ATTACK_TIME_BEND = 3;
    private static final int ATTACK_ENTITY_SUMMON = 4;
    
    public ThothRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new ThothModel());
        
        // Add layers in order of rendering
        this.addRenderLayer(new ThothGlowingEyesLayer(this));
        this.addRenderLayer(new ThothParticleLayer(this));
        
        // Scale up for boss presence
        this.shadowRadius = 1.5f;
    }
    
    @Override
    public void render(ThothEntity entity, float entityYaw, float partialTick, MatrixStack poseStack,
                      VertexConsumerProvider bufferSource, int packedLight) {
        
        // Scale up the entity for boss presence
        poseStack.push();
        
        // Base scale for boss presence
        float baseScale = 1.2f;
        
        // Eye tracking for intimidation - only subtle when in combat
        if (entity.getTarget() != null && entity.isInCombat()) {
            Vec3d targetPos = entity.getTarget().getEyePos();
            Vec3d entityPos = entity.getEyePos();
            Vec3d lookVec = targetPos.subtract(entityPos).normalize();
            
            // Apply subtle head turn toward target
            float targetYaw = (float)(MathHelper.atan2(lookVec.z, lookVec.x) * (180F / Math.PI)) - 90.0F;
            float yawDiff = MathHelper.wrapDegrees(targetYaw - entity.getYaw());
            yawDiff = MathHelper.clamp(yawDiff, -30.0F, 30.0F); // Limit head turn
            
            poseStack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(yawDiff * 0.5f));
        }
        
        // Special effects during spawn transition
        if (entity.isInSpawnTransition()) {
            // No flickering, just ensure base scale is applied
        }
        // Apply base scale regardless of spawn transition
        poseStack.scale(baseScale, baseScale, baseScale);
        
        // Visual effects based on combat state
        if (!entity.isInCombat() && !entity.isOnGround()) {
            // Only apply floating effect if actually floating (not on ground)
            float bobOffset = MathHelper.sin((entity.age + partialTick) * 0.05f) * 0.1f;
            poseStack.translate(0, bobOffset, 0);
        } else if (entity.isInCombat()) {
            // Combat mode - minimal sway to show divine presence without disrupting movement
            float groundedSway = MathHelper.sin((entity.age + partialTick) * 0.02f) * 0.02f;
            poseStack.translate(0, groundedSway, 0);
        }
        
        // Attack-specific visual effects using entity getters
        if (entity.isScrollBlastAttack()) {
            // Enhanced vibration effect with Y component and charge progression
            int attackTicks = entity.age % 40; // Assume 2-second attack cycle
            float chargeProgress = Math.min(attackTicks / 20.0f, 1.0f); // First half is charge-up
            float shakeIntensity = 0.01f + (chargeProgress * 0.03f); // Ramp up shake
            
            float shakeX = (entity.age % 4) < 2 ? shakeIntensity : -shakeIntensity;
            float shakeY = (entity.age % 6) < 3 ? shakeIntensity * 0.5f : -shakeIntensity * 0.5f; // Smaller Y shake
            float shakeZ = ((entity.age + 2) % 4) < 2 ? shakeIntensity : -shakeIntensity;
            
            poseStack.translate(shakeX, shakeY, shakeZ);
        } else if (entity.isTimeBendAttack() || entity.isCastingTimeMagic()) {
            // Enhanced distortion effect for time magic - the cool red trippy effect!
            float timeWarp = entity.age + partialTick;
            float distortionX = MathHelper.sin(timeWarp * 0.3f) * 0.08f;
            float distortionY = MathHelper.cos(timeWarp * 0.25f) * 0.06f;
            float distortionZ = MathHelper.sin(timeWarp * 0.35f) * 0.07f;
            
            poseStack.scale(1.0f + distortionX, 1.0f + distortionY, 1.0f + distortionZ);
            
            // Additional reality-bending effect
            float warpRotation = MathHelper.sin(timeWarp * 0.1f) * 0.05f;
            poseStack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotation(warpRotation));
        }
        
        // Enhanced lighting - ensure minimum brightness for boss visibility
        int enhancedLight;
        if (entity.isInSpawnTransition()) {
            // Extra bright during spawn for dramatic effect
            float transitionProgress = (float) entity.getSpawnTransitionTicks() / ThothEntity.SPAWN_TRANSITION_DURATION;
            int spawnBoost = (int)(transitionProgress * 0x40); // Bright spawn effect
            enhancedLight = Math.min(packedLight + 0x40 + spawnBoost, 0xF000F0);
        } else {
            // Ensure boss is always well-lit for visibility
            enhancedLight = Math.max(packedLight + 0x30, 0x80F0); // Minimum light level with boost
            enhancedLight = Math.min(enhancedLight, 0xF000F0); // Cap at maximum
        }
        
        // Render with all the visual effects
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, enhancedLight);
        
        poseStack.pop();
    }
    
    public Identifier getTextureResource(ThothEntity animatable) {
        // Always use green texture
        return TEXTURE_GREEN;
    }
    
    @Override
    public RenderLayer getRenderType(ThothEntity animatable, Identifier texture, 
                                   VertexConsumerProvider bufferSource, float partialTick) {
        // Always use green render layer
        return RENDER_LAYER_GREEN;
    }
    
    /**
     * Glowing Eyes Layer - Adds emissive eyes to Thoth with soft glow effects
     */
    public static class ThothGlowingEyesLayer extends GeoRenderLayer<ThothEntity> {
        
        public ThothGlowingEyesLayer(GeoEntityRenderer<ThothEntity> entityRenderer) {
            super(entityRenderer);
        }
        
        @Override
        public void render(MatrixStack poseStack, ThothEntity animatable, BakedGeoModel bakedModel,
                          RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer,
                          float partialTick, int packedLight, int packedOverlay) {
            
            // Always render eyes, but with different intensities based on state
            boolean isCasting = animatable.isCastingTimeMagic();
            boolean isAttacking = animatable.isAttackingWithMagic();
            boolean isInCombat = animatable.isInCombat();
            
            // Base glow intensity - always present but subtle
            float baseGlow = 0.3f;
            
            // Enhanced glow during magical activities
            if (isCasting || isAttacking) {
                baseGlow = 0.8f;
            } else if (isInCombat) {
                baseGlow = 0.5f;
            }
            
            // Create pulsing glow effect
            float pulseIntensity = (MathHelper.sin((animatable.age + partialTick) * 0.15f) + 1.0f) * 0.5f;
            float finalGlow = baseGlow + (pulseIntensity * 0.2f);
            
            if (finalGlow <= 0.1f) return; // Early bailout if too dim
            
            // Different glow colors for different states
            float r = 1.0f, g = 1.0f, b = 1.0f;
            if (isCasting) {
                // Purple for time magic
                r = 0.7f; g = 0.3f; b = 1.0f;
            } else if (animatable.isScrollBlastAttack()) {
                // Gold for scroll blast
                r = 1.0f; g = 0.8f; b = 0.3f;
            } else if (animatable.isMagicBallAttack()) {
                // Blue for magic ball
                r = 0.3f; g = 0.7f; b = 1.0f;
            } else if (isInCombat) {
                // Orange-red for combat
                r = 1.0f; g = 0.5f; b = 0.2f;
            }
            
            // Render multiple layers for soft glow effect
            VertexConsumer eyesBuffer = bufferSource.getBuffer(getEyesLayer());
            
            // Layer 1: Core eye glow (brightest)
            this.getRenderer().reRender(
                bakedModel, poseStack, bufferSource, animatable, getEyesLayer(),
                eyesBuffer, partialTick, 0xF000F0, packedOverlay,
                r, g, b, finalGlow
            );
            
            // Layer 2: Soft outer glow (slightly larger, more transparent)
            poseStack.push();
            poseStack.scale(1.05f, 1.05f, 1.05f);
            this.getRenderer().reRender(
                bakedModel, poseStack, bufferSource, animatable, getEyesLayer(),
                eyesBuffer, partialTick, 0xF000F0, packedOverlay,
                r, g, b, finalGlow * 0.6f
            );
            poseStack.pop();
            
            // Layer 3: Ambient glow (largest, most transparent)
            poseStack.push();
            poseStack.scale(1.1f, 1.1f, 1.1f);
            this.getRenderer().reRender(
                bakedModel, poseStack, bufferSource, animatable, getEyesLayer(),
                eyesBuffer, partialTick, 0xF000F0, packedOverlay,
                r, g, b, finalGlow * 0.3f
            );
            poseStack.pop();
        }
        
        @Override
        public Identifier getTextureResource(ThothEntity animatable) {
            return EYES_TEXTURE;
        }
    }
    
    /**
     * Particle Layer - Adds ambient magical particles around Thoth
     */
    public static class ThothParticleLayer extends GeoRenderLayer<ThothEntity> {
        
        public ThothParticleLayer(GeoEntityRenderer<ThothEntity> entityRenderer) {
            super(entityRenderer);
        }
        
        @Override
        public void render(MatrixStack poseStack, ThothEntity animatable, BakedGeoModel bakedModel,
                          RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer,
                          float partialTick, int packedLight, int packedOverlay) {
            
            // Early bailout for performance
            if (!animatable.isCastingTimeMagic() && !animatable.isInSpawnTransition()) {
                return;
            }
            
            // Render energy aura during time magic
            if (animatable.isCastingTimeMagic()) {
                renderEnergyAura(poseStack, animatable, bakedModel, bufferSource, partialTick);
            }
            
            // Render spawn aura during spawn transition
            if (animatable.isInSpawnTransition()) {
                renderSpawnAura(poseStack, animatable, bakedModel, bufferSource, partialTick);
            }
        }
        
        private void renderEnergyAura(MatrixStack poseStack, ThothEntity animatable, BakedGeoModel bakedModel,
                                     VertexConsumerProvider bufferSource, float partialTick) {
            // Create a subtle energy aura around Thoth during time magic
            poseStack.push();
            
            // Cache VertexConsumer once per aura render instead of getting it in loop
            VertexConsumer auraBuffer = bufferSource.getBuffer(RENDER_LAYER_GREEN);
            
            // Multiple layers for depth
            for (int i = 0; i < 3; i++) {
                poseStack.push();
                
                float layerOffset = i * 0.03f;
                float auraScale = 1.1f + layerOffset + MathHelper.sin((animatable.age + partialTick) * 0.1f + i) * 0.05f;
                poseStack.scale(auraScale, auraScale, auraScale);
                
                // Rotate each layer differently
                float rotation = (animatable.age + partialTick) * (2.0f - i * 0.5f);
                poseStack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(rotation));
                
                float alpha = 0.1f - (i * 0.03f);
                if (alpha > 0) { // Only render if alpha is positive
                    this.getRenderer().reRender(
                        bakedModel, poseStack, bufferSource, animatable, RENDER_LAYER_GREEN,
                        auraBuffer, partialTick, 0xF000F0, 0,
                        0.5f, 0.2f, 0.8f, alpha // Purple tint with low alpha
                    );
                }
                
                poseStack.pop();
            }
            
            poseStack.pop();
        }
        
        private void renderSpawnAura(MatrixStack poseStack, ThothEntity animatable, BakedGeoModel bakedModel,
                                    VertexConsumerProvider bufferSource, float partialTick) {
            float transitionProgress = (float) animatable.getSpawnTransitionTicks() / ThothEntity.SPAWN_TRANSITION_DURATION;
            float alpha = transitionProgress * 0.2f;
            
            if (alpha <= 0) return; // Early bailout if no alpha
            
            poseStack.push();
            
            // Expanding aura effect
            float auraScale = 1.0f + (1.0f - transitionProgress) * 2.0f;
            poseStack.scale(auraScale, auraScale, auraScale);
            
            VertexConsumer auraBuffer = bufferSource.getBuffer(RENDER_LAYER_GREEN);
            
            this.getRenderer().reRender(
                bakedModel, poseStack, bufferSource, animatable, RENDER_LAYER_GREEN,
                auraBuffer, partialTick, 0xF000F0, 0,
                1.0f, 1.0f, 1.0f, alpha
            );
            
            poseStack.pop();
        }
        
        @Override
        public Identifier getTextureResource(ThothEntity animatable) {
            return TEXTURE_GREEN; // Use green texture for energy aura
        }
    }
}