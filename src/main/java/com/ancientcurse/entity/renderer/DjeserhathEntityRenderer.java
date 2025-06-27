package com.ancientcurse.entity.renderer;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.DjeserhathEntity;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.model.GeoModel;

/**
 * Renderer for the Djeserhath entity with dynamic jaw control
 */
public class DjeserhathEntityRenderer extends GeoEntityRenderer<DjeserhathEntity> {
    
    public DjeserhathEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new DjeserhathModel());
        this.shadowRadius = 0f; // No shadow for plant entity
    }
    
    @Override
    public void preRender(MatrixStack poseStack, DjeserhathEntity animatable, BakedGeoModel model, 
                         VertexConsumerProvider bufferSource, VertexConsumer buffer, 
                         boolean isReRender, float partialTick, int packedLight, 
                         int packedOverlay, float red, float green, float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, 
                       partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        
        // Handle jaw rotation based on JAW_OPENNESS
        float jawOpenness = animatable.getJawOpenness();
        
        // Apply rotation to jaw bones - FIXED: Set rotation instead of adding to it
        applyJawRotation(model, "jaw_bottom", jawOpenness, 20.0f); // Max 20 degrees
        applyJawRotation(model, "jaw_bottom2", jawOpenness, 15.0f); // Max 15 degrees
        applyJawRotation(model, "jaw_bottom3", jawOpenness, 10.0f); // Max 10 degrees
        applyJawRotation(model, "jaw_top", jawOpenness, -15.0f); // Negative for opposite direction
        
        // Handle hunting state visual effects
        handleHuntingStateEffects(animatable, model, partialTick);
    }
    
    private void applyJawRotation(BakedGeoModel model, String boneName, float openness, float maxRotation) {
        GeoBone bone = model.getBone(boneName).orElse(null);
        if (bone != null) {
            // Convert degrees to radians and SET rotation (not add)
            float rotation = (float) Math.toRadians(openness * maxRotation);
            bone.setRotX(rotation); // FIXED: Use setRotX instead of adding to getRotX
        }
    }
    
    private void handleHuntingStateEffects(DjeserhathEntity entity, BakedGeoModel model, float partialTick) {
        DjeserhathEntity.HuntingState state = entity.getHuntingState();
        
        // Get the root bone to apply global transformations
        GeoBone root = model.getBone("root").orElse(null);
        if (root == null) return;
        
        switch (state) {
            case DORMANT:
                // No special effects when dormant
                break;
                
            case SENSING:
                // Slight trembling effect - apply to root for cohesive movement
                float tremble = MathHelper.sin((entity.age + partialTick) * 0.3f) * 0.02f;
                root.setPosY(tremble);
                break;
                
            case LURING:
                // Subtle swaying motion - apply to root
                float sway = MathHelper.sin((entity.age + partialTick) * 0.05f) * 0.1f;
                root.setRotZ(sway);
                break;
                
            case STALKING:
                // Tense, focused state - slight forward lean on whole model
                root.setRotX(0.1f);
                break;
                
            case STRIKING:
                // Quick striking motion handled by animation
                break;
                
            case DIGESTING:
                // For digesting, we still want local scaling effects on body segments
                // These are local animations and should be fine to apply to specific bones
                GeoBone body2 = model.getBone("body2").orElse(null);
                GeoBone body3 = model.getBone("body3").orElse(null);
                if (body2 != null && body3 != null) {
                    float gulp = MathHelper.sin((entity.age + partialTick) * 0.1f) * 0.05f;
                    body2.setScaleX(1.0f + gulp);
                    body2.setScaleZ(1.0f + gulp);
                    body3.setScaleX(1.0f - gulp * 0.5f);
                    body3.setScaleZ(1.0f - gulp * 0.5f);
                }
                break;
                
            case SATISFIED:
                // Gentle, content swaying - apply to root for cohesive movement
                float contentSway = MathHelper.sin((entity.age + partialTick) * 0.03f) * 0.05f;
                root.setRotZ(contentSway);
                break;
        }
    }
    
    @Override
    public RenderLayer getRenderType(DjeserhathEntity animatable, Identifier texture,
                                    @Nullable VertexConsumerProvider bufferSource, float partialTick) {
        // Use cutout for better plant rendering
        return RenderLayer.getEntityCutoutNoCull(texture);
    }
    
    @Override
    public float getMotionAnimThreshold(DjeserhathEntity animatable) {
        // Since this is a rooted plant, we don't need motion-based animations
        return Float.MAX_VALUE;
    }
    
    @Override
    public Identifier getTextureLocation(DjeserhathEntity instance) {
        return DjeserhathModel.TEXTURE;
    }
    
    /**
     * Inner model class to handle the Djeserhath entity model
     */
    private static class DjeserhathModel extends GeoModel<DjeserhathEntity> {
        private static final Identifier MODEL = new Identifier(AncientCurse.MOD_ID, "geo/djeserhath.geo.json");
        private static final Identifier TEXTURE = new Identifier(AncientCurse.MOD_ID, "textures/entity/djeserhath.png");
        private static final Identifier ANIMATION = new Identifier(AncientCurse.MOD_ID, "animations/djeserhath.animation.json");
        
        @Override
        public Identifier getModelResource(DjeserhathEntity object) {
            return MODEL;
        }

        @Override
        public Identifier getTextureResource(DjeserhathEntity object) {
            return TEXTURE;
        }

        @Override
        public Identifier getAnimationResource(DjeserhathEntity animatable) {
            return ANIMATION;
        }
    }
}