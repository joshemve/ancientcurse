package com.ancientcurse.entity.renderer;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.model.LocusModel;
import com.ancientcurse.entity.LocusEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Enhanced Locus Renderer with Visual Improvements:
 * 
 * - Alpha Variant Visuals: Different texture and 20% size increase for alphas
 * - Dynamic Movement: Wing buzz vibration when flying, forward tilt based on velocity
 * - Better Rendering: Translucent render layer for wings, proper shadow radius
 * - Death Animation: 180° spin on death (dramatic!)
 * - Hurt Flash: Smooth red flash animation when taking damage
 */
public class LocusRenderer extends GeoEntityRenderer<LocusEntity> {
    private static final Identifier TEXTURE = new Identifier(AncientCurse.MOD_ID, "textures/entity/locus.png");
    private static final Identifier TEXTURE_ALPHA = new Identifier(AncientCurse.MOD_ID, "textures/entity/locus_alpha.png");
    
    public LocusRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new LocusModel());
        this.shadowRadius = 0.4f; // Subtle shadow for realism
    }

    @Override
    public Identifier getTextureLocation(LocusEntity animatable) {
        // Use different texture for alpha variants (stronger locusts)
        if (animatable.getMaxHealth() > 30) { // Alphas have more health
            return TEXTURE_ALPHA;
        }
        return TEXTURE;
    }
    
    @Override
    public void preRender(MatrixStack poseStack, LocusEntity animatable, BakedGeoModel model, 
                         VertexConsumerProvider bufferSource, VertexConsumer buffer, 
                         boolean isReRender, float partialTick, int packedLight, 
                         int packedOverlay, float red, float green, float blue, float alpha) {
        
        // ===== ALPHA VARIANT VISUALS =====
        // Scale alpha locusts 20% larger to make them more threatening
        if (animatable.getMaxHealth() > 30) {
            poseStack.scale(1.2f, 1.2f, 1.2f);
        }
        
        // ===== DYNAMIC MOVEMENT EFFECTS =====
        
        // Wing buzz vibration when flying - rapid micro-movements
        if (!animatable.isOnGround()) {
            float buzzIntensity = 0.015f; // Subtle but noticeable
            float buzzSpeed = 1.2f; // Rapid wing beats
            float buzzTime = animatable.age + partialTick;
            
            // Multi-axis vibration for realistic wing buzz
            float buzzX = MathHelper.sin(buzzTime * buzzSpeed) * buzzIntensity;
            float buzzY = MathHelper.sin(buzzTime * buzzSpeed * 1.3f) * buzzIntensity * 0.5f;
            float buzzZ = MathHelper.cos(buzzTime * buzzSpeed * 0.9f) * buzzIntensity;
            
            poseStack.translate(buzzX, buzzY, buzzZ);
        }
        
        // Forward tilt based on velocity - more realistic flight appearance
        if (!animatable.isOnGround() && animatable.getVelocity().length() > 0.1) {
            double velocityLength = Math.sqrt(
                animatable.getVelocity().x * animatable.getVelocity().x + 
                animatable.getVelocity().z * animatable.getVelocity().z
            );
            
            // Calculate pitch based on vertical vs horizontal movement
            float pitch = (float) Math.toDegrees(Math.atan2(
                animatable.getVelocity().y, velocityLength
            ));
            
            // Apply forward tilt (but limit it for visual appeal)
            float tiltAmount = MathHelper.clamp(pitch * 0.6f, -30.0f, 20.0f);
            poseStack.multiply(new org.joml.Quaternionf().rotateX((float) Math.toRadians(-tiltAmount)));
            
            // Subtle roll based on horizontal turning
            if (velocityLength > 0.2) {
                float yaw = animatable.getYaw();
                float prevYaw = animatable.prevYaw;
                float yawDelta = MathHelper.wrapDegrees(yaw - prevYaw);
                float rollAmount = MathHelper.clamp(yawDelta * 2.0f, -15.0f, 15.0f);
                poseStack.multiply(new org.joml.Quaternionf().rotateZ((float) Math.toRadians(rollAmount)));
            }
        }
        
        // Enhanced color tinting for special variants
        float finalRed = red;
        float finalGreen = green;
        float finalBlue = blue;
        
        // Golden tint for scout locusts (if they have special names)
        if (animatable.hasCustomName() && 
            animatable.getName().getString().toLowerCase().contains("scout")) {
            finalRed = Math.min(1.0f, red + 0.3f);
            finalGreen = Math.min(1.0f, green + 0.2f);
            finalBlue = Math.max(0.4f, blue - 0.2f);
        }
        
        super.preRender(poseStack, animatable, model, bufferSource, buffer, 
                       isReRender, partialTick, packedLight, packedOverlay, 
                       finalRed, finalGreen, finalBlue, alpha);
    }
    
    @Override
    public RenderLayer getRenderType(LocusEntity animatable, Identifier texture, 
                                   VertexConsumerProvider bufferSource, float partialTick) {
        // Use translucent render layer for better wing rendering
        // This allows wing membranes to look more realistic
        return RenderLayer.getEntityTranslucent(texture);
    }
    
    @Override
    protected float getDeathMaxRotation(LocusEntity entityLivingBaseIn) {
        // Dramatic 180° death spin - makes death more visually impactful
        return 180.0F;
    }
    
    @Override
    public int getPackedOverlay(LocusEntity animatable, float uIn) {
        // Enhanced hurt flash animation with smoother transitions
        float hurtIntensity = getHurtIntensity(animatable);
        
        if (hurtIntensity > 0.0F) {
            // Create a pulsing red effect instead of solid red
            float pulseIntensity = MathHelper.sin(animatable.age * 0.8f) * 0.3f + 0.7f;
            hurtIntensity *= pulseIntensity;
        }
        
        return super.getPackedOverlay(animatable, hurtIntensity);
    }
    
    /**
     * Calculate smooth hurt animation intensity
     */
    private float getHurtIntensity(LocusEntity locus) {
        // Smooth exponential decay for hurt flash
        float timeSinceHurt = (float)(locus.age - locus.lastDamageTime);
        float decayRate = 8.0f; // How quickly the flash fades
        
        return MathHelper.clamp(
            (float) Math.exp(-timeSinceHurt / decayRate), 
            0.0F, 
            1.0F
        );
    }
}