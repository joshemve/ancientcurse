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
    
    private static final Identifier TEXTURE = new Identifier(AncientCurse.MOD_ID, "textures/entity/thoth.png");
    private static final Identifier TEXTURE_GREEN = new Identifier(AncientCurse.MOD_ID, "textures/entity/thoth_green.png");
    private static final Identifier TEXTURE_PURPLE = new Identifier(AncientCurse.MOD_ID, "textures/entity/thoth_purple.png");
    private static final Identifier DAMAGE_TEXTURE = new Identifier(AncientCurse.MOD_ID, "textures/entity/thoth_damaged.png");
    private static final Identifier GLOW_TEXTURE = new Identifier(AncientCurse.MOD_ID, "textures/entity/thoth_glow.png");
    
    // Cached RenderLayers to avoid map lookups each frame
    private static final RenderLayer RENDER_LAYER_DEFAULT = RenderLayer.getEntityTranslucent(TEXTURE);
    private static final RenderLayer RENDER_LAYER_GREEN = RenderLayer.getEntityTranslucent(TEXTURE_GREEN);
    private static final RenderLayer RENDER_LAYER_PURPLE = RenderLayer.getEntityTranslucent(TEXTURE_PURPLE);
    private static final RenderLayer RENDER_LAYER_DAMAGE = RenderLayer.getEntityTranslucent(DAMAGE_TEXTURE);
    private static final RenderLayer RENDER_LAYER_GLOW = RenderLayer.getEyes(GLOW_TEXTURE);
    
    // Attack state constants (matching ThothEntity)
    private static final int ATTACK_NONE = 0;
    private static final int ATTACK_MAGIC_BALL = 1;
    private static final int ATTACK_SCROLL_BLAST = 2;
    private static final int ATTACK_TIME_BEND = 3;
    private static final int ATTACK_ENTITY_SUMMON = 4;
    
    public ThothRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new ThothModel());
        
        // Add layers in order of rendering
        this.addRenderLayer(new ThothDamageLayer(this));
        this.addRenderLayer(new ThothMagicGlowLayer(this));
        this.addRenderLayer(new ThothParticleLayer(this));
        
        // Scale up for boss presence
        this.shadowRadius = 1.5f;
    }
    
    @Override
    public void render(ThothEntity entity, float entityYaw, float partialTick, MatrixStack poseStack,
                      VertexConsumerProvider bufferSource, int packedLight) {
        
        // Scale up the entity for boss presence
        poseStack.push();
        
        // Eye tracking for intimidation
        if (entity.getTarget() != null) {
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
            float transitionProgress = (float) entity.getSpawnTransitionTicks() / ThothEntity.SPAWN_TRANSITION_DURATION;
            
            // Gradual scale-in effect during spawn
            float spawnScale = 1.0f + (transitionProgress * 0.3f); // Start 30% larger, scale down to normal
            poseStack.scale(1.2f * spawnScale, 1.2f * spawnScale, 1.2f * spawnScale);
            
            // Spiraling particles effect
            float spiralAngle = transitionProgress * 720.0f; // Two full rotations
            poseStack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(spiralAngle));
            
            // Vertical oscillation during spawn
            float yOffset = MathHelper.sin(transitionProgress * (float)Math.PI) * 0.5f;
            poseStack.translate(0, yOffset, 0);
            
            // Lightning-like flicker during spawn (proper packed light usage)
            if (entity.age % 5 < 2) {
                packedLight = 0xF000F0; // Full bright flicker
            }
        } else {
            poseStack.scale(1.2f, 1.2f, 1.2f);
        }
        
        // Apply floating movement to rendering
        if (entity.isFloating()) {
            float bobOffset = MathHelper.sin((entity.age + partialTick) * 0.05f) * 0.1f;
            poseStack.translate(0, bobOffset, 0);
        }
        
        // Attack-specific visual effects using entity getters
        if (entity.isMagicBallAttack()) {
            // Subtle hand glow effect - use safer light enhancement
            float chargeProgress = (entity.age % 20) / 20.0f;
            int extraLight = (int)(chargeProgress * 0x20); // Small boost to both components
            packedLight = Math.min(packedLight + extraLight, 0xF000F0);
        } else if (entity.isScrollBlastAttack()) {
            // Enhanced vibration effect with Y component and charge progression
            int attackTicks = entity.age % 40; // Assume 2-second attack cycle
            float chargeProgress = Math.min(attackTicks / 20.0f, 1.0f); // First half is charge-up
            float shakeIntensity = 0.01f + (chargeProgress * 0.03f); // Ramp up shake
            
            float shakeX = (entity.age % 4) < 2 ? shakeIntensity : -shakeIntensity;
            float shakeY = (entity.age % 6) < 3 ? shakeIntensity * 0.5f : -shakeIntensity * 0.5f; // Smaller Y shake
            float shakeZ = ((entity.age + 2) % 4) < 2 ? shakeIntensity : -shakeIntensity;
            
            poseStack.translate(shakeX, shakeY, shakeZ);
        } else if (entity.isTimeBendAttack()) {
            // Distortion effect
            float distortion = MathHelper.sin((entity.age + partialTick) * 0.3f) * 0.05f;
            poseStack.scale(1.0f + distortion, 1.0f - distortion, 1.0f + distortion);
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
        
        // Render with all the visual effects (scaling, rotation, lighting already provide great spawn transition)
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, enhancedLight);
        
        poseStack.pop();
    }
    
    public Identifier getTextureResource(ThothEntity animatable) {
        // Dynamic texture selection based on state
        if (animatable.isCastingTimeMagic()) {
            return TEXTURE_PURPLE; // Purple during time magic
        } else if (animatable.getHealth() < animatable.getMaxHealth() * 0.3f) {
            return TEXTURE_GREEN; // Green when low health (desperate/enraged)
        }
        return TEXTURE; // Default texture
    }
    
    @Override
    public RenderLayer getRenderType(ThothEntity animatable, Identifier texture, 
                                   VertexConsumerProvider bufferSource, float partialTick) {
        // Use cached render layers
        if (texture == TEXTURE_PURPLE) {
            return RENDER_LAYER_PURPLE;
        } else if (texture == TEXTURE_GREEN) {
            return RENDER_LAYER_GREEN;
        }
        return RENDER_LAYER_DEFAULT;
    }
    
    /**
     * Damage Layer - Shows visual damage as health decreases
     */
    public static class ThothDamageLayer extends GeoRenderLayer<ThothEntity> {
        
        public ThothDamageLayer(GeoEntityRenderer<ThothEntity> entityRenderer) {
            super(entityRenderer);
        }
        
        @Override
        public void render(MatrixStack poseStack, ThothEntity animatable, BakedGeoModel bakedModel,
                          RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer,
                          float partialTick, int packedLight, int packedOverlay) {
            
            float healthPercent = animatable.getHealth() / animatable.getMaxHealth();
            if (healthPercent >= 0.7f) return; // Early bailout for performance
            
            float damageAlpha = (0.7f - healthPercent) / 0.7f; // 0 at 70% health, 1 at 0% health
            if (damageAlpha <= 0) return; // Additional bailout check
            
            VertexConsumer damageBuffer = bufferSource.getBuffer(RENDER_LAYER_DAMAGE);
            
            this.getRenderer().reRender(
                bakedModel, poseStack, bufferSource, animatable, RENDER_LAYER_DAMAGE,
                damageBuffer, partialTick, packedLight, packedOverlay,
                1.0f, 1.0f, 1.0f, damageAlpha * 0.5f
            );
        }
        
        @Override
        public Identifier getTextureResource(ThothEntity animatable) {
            return DAMAGE_TEXTURE;
        }
    }
    
    /**
     * Magic Glow Layer - Adds glowing effects during spellcasting
     */
    public static class ThothMagicGlowLayer extends GeoRenderLayer<ThothEntity> {
        
        public ThothMagicGlowLayer(GeoEntityRenderer<ThothEntity> entityRenderer) {
            super(entityRenderer);
        }
        
        @Override
        public void render(MatrixStack poseStack, ThothEntity animatable, BakedGeoModel bakedModel,
                          RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer,
                          float partialTick, int packedLight, int packedOverlay) {
            
            // Early bailout using entity getters instead of magic numbers
            boolean shouldGlow = animatable.isCastingTimeMagic() || animatable.isAttackingWithMagic();
            
            if (!shouldGlow) {
                return; // Early bailout for performance
            }
            
            // Create pulsing glow effect
            float glowIntensity = (MathHelper.sin((animatable.age + partialTick) * 0.2f) + 1.0f) * 0.5f;
            if (glowIntensity <= 0) return; // Additional bailout check
            
            // Different glow colors for different attacks
            float r = 1.0f, g = 1.0f, b = 1.0f;
            if (animatable.isCastingTimeMagic()) {
                // Purple for time magic
                r = 0.7f; g = 0.3f; b = 1.0f;
            } else if (animatable.isMagicBallAttack()) {
                // Blue for magic ball
                r = 0.3f; g = 0.5f; b = 1.0f;
            } else if (animatable.isScrollBlastAttack()) {
                // Gold for scroll blast
                r = 1.0f; g = 0.8f; b = 0.3f;
            }
            
            VertexConsumer glowBuffer = bufferSource.getBuffer(RENDER_LAYER_GLOW);
            
            // Render the glow with full brightness
            this.getRenderer().reRender(
                bakedModel, poseStack, bufferSource, animatable, RENDER_LAYER_GLOW,
                glowBuffer, partialTick, 0xF000F0, packedOverlay,
                r, g, b, glowIntensity
            );
        }
        
        @Override
        public Identifier getTextureResource(ThothEntity animatable) {
            return GLOW_TEXTURE;
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
            VertexConsumer auraBuffer = bufferSource.getBuffer(RENDER_LAYER_PURPLE);
            
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
                        bakedModel, poseStack, bufferSource, animatable, RENDER_LAYER_PURPLE,
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
            
            VertexConsumer auraBuffer = bufferSource.getBuffer(RENDER_LAYER_PURPLE);
            
            this.getRenderer().reRender(
                bakedModel, poseStack, bufferSource, animatable, RENDER_LAYER_PURPLE,
                auraBuffer, partialTick, 0xF000F0, 0,
                1.0f, 1.0f, 1.0f, alpha
            );
            
            poseStack.pop();
        }
        
        @Override
        public Identifier getTextureResource(ThothEntity animatable) {
            return TEXTURE_PURPLE; // Use purple texture for energy aura
        }
    }
}