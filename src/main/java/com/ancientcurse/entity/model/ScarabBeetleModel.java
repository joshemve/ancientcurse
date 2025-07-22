package com.ancientcurse.entity.model;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.ScarabBeetleEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

/**
 * Scarab Beetle GeckoLib model with proper 6-legged insect locomotion.
 * Uses 3-segment leg structure (top->middle->bottom) for realistic movement.
 */
public class ScarabBeetleModel extends GeoModel<ScarabBeetleEntity> {

    /* ------------------ RESOURCES ------------------ */
    private static final Identifier MODEL = new Identifier(AncientCurse.MOD_ID, "geo/scarab_beetle.geo.json");
    private static final Identifier TEXTURE_NORMAL = new Identifier(AncientCurse.MOD_ID, "textures/entity/scarab_beetle.png");
    private static final Identifier TEXTURE_GOLD = new Identifier(AncientCurse.MOD_ID, "textures/entity/scarab_beetle_golden.png");
    private static final Identifier ANIM = new Identifier(AncientCurse.MOD_ID, "animations/scarab_beetle.animation.json");

    /* ------------------ ANIMATION CONSTANTS ------------------ */
    private static final float MAX_HEAD_YAW = 60f;
    private static final float MAX_HEAD_PITCH = 15f;

    /* ------------------ CACHED BONES ------------------ */
    private CoreGeoBone head, body, root;
    private CoreGeoBone leftPincher, rightPincher;
    private CoreGeoBone antennas, antennaL, antennaR, eyes;
    
    // Removed leg segment bones - using animations instead

    @Override public Identifier getModelResource(ScarabBeetleEntity e) { return MODEL; }
    @Override public Identifier getTextureResource(ScarabBeetleEntity e) { return e.isGolden() ? TEXTURE_GOLD : TEXTURE_NORMAL; }
    @Override public Identifier getAnimationResource(ScarabBeetleEntity e) { return ANIM; }

    @Override
    public void setCustomAnimations(ScarabBeetleEntity beetle, long id, AnimationState<ScarabBeetleEntity> state) {
        super.setCustomAnimations(beetle, id, state);

        if (head == null) cacheBones(); // Lazy init once per model instance

        EntityModelData data = state.getData(DataTickets.ENTITY_MODEL_DATA);
        if (data == null) return;

        float partialTick = state.getPartialTick();
        float age = beetle.age + partialTick;

        // Apply animations - simplified to use GeckoLib animations instead of procedural
        applyHeadRotation(data.netHeadYaw(), data.headPitch());
        applyPinchers(beetle, age);
        applyAntennas(age, beetle.isMoving() ? 1.0f : 0.0f);
        
        if (beetle.isGolden()) {
            applyGoldenShimmer(age);
        }
    }

    private void applyHeadRotation(float yaw, float pitch) {
        if (head == null) return;
        head.setRotY((float) Math.toRadians(MathHelper.clamp(yaw, -MAX_HEAD_YAW, MAX_HEAD_YAW)));
        head.setRotX((float) Math.toRadians(MathHelper.clamp(pitch, -MAX_HEAD_PITCH, MAX_HEAD_PITCH)));
    }

    // Removed procedural leg movement methods - using animations instead

    /**
     * Smooth step function for more natural interpolation
     */
    private float smoothStep(float min, float max, float value) {
        float t = MathHelper.clamp((value - min) / (max - min), 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    // Simplified body movement - only subtle breathing animation
    private void applyBodyMovement(ScarabBeetleEntity beetle, float partialTick) {
        if (body == null) return;
        
        // Very subtle breathing animation
        float breathe = MathHelper.sin((beetle.age + partialTick) * 0.1f) * 0.005f;
        body.setScaleY(1.0f + breathe);
    }

    private void applyPinchers(ScarabBeetleEntity beetle, float age) {
        if (leftPincher == null || rightPincher == null) return;
        
        boolean attacking = beetle.isAttacking();
        float speed = attacking ? 2.0f : 0.5f;
        float amplitude = attacking ? 0.4f : 0.15f;
        
        float movement = MathHelper.sin(age * speed) * amplitude;
        
        leftPincher.setRotY(-movement);
        rightPincher.setRotY(movement);
        
        if (attacking) {
            float lift = Math.abs(movement) * 0.3f;
            leftPincher.setRotX(lift);
            rightPincher.setRotX(lift);
        } else {
            leftPincher.setRotX(0);
            rightPincher.setRotX(0);
        }
    }

    private void applyAntennas(float age, float moveAmount) {
        if (antennas != null) {
            float sway = MathHelper.sin(age * 0.12f) * 0.08f;
            float bounce = MathHelper.cos(age * 0.15f) * 0.05f;
            float moveInfluence = Math.min(moveAmount * 1.5f, 1.0f);
            
            antennas.setRotX(bounce - moveInfluence * 0.1f);
            antennas.setRotZ(sway);
        }
        
        if (antennaL != null) {
            antennaL.setRotZ(MathHelper.sin(age * 0.14f) * 0.06f - 0.03f);
        }
        
        if (antennaR != null) {
            antennaR.setRotZ(MathHelper.sin(age * 0.14f + (float)Math.PI) * 0.06f + 0.03f);
        }
    }

    private void applyGoldenShimmer(float age) {
        if (body == null || eyes == null) return;
        
        float shimmer = 1.0f + MathHelper.sin(age * 0.08f) * 0.02f;
        body.setScaleX(shimmer);
        body.setScaleY(shimmer);
        body.setScaleZ(shimmer);
        
        float eyeGlow = 1.0f + MathHelper.sin(age * 0.25f) * 0.08f;
        eyes.setScaleX(eyeGlow);
        eyes.setScaleY(eyeGlow);
        eyes.setScaleZ(eyeGlow);
    }

    private void cacheBones() {
        head = getModelBone("head");
        body = getModelBone("body");
        root = getModelBone("scarab_beetle");
        
        // Pinchers
        leftPincher = getModelBone("left_pincher");
        rightPincher = getModelBone("right_pincher");
        
        // Antennas
        antennas = getModelBone("antennas");
        antennaL = getModelBone("antenna_left");
        antennaR = getModelBone("antenna_right");
        eyes = getModelBone("eyes");
        
        // Removed leg bone caching - using animations instead
    }

    private CoreGeoBone getModelBone(String name) {
        try {
            return getAnimationProcessor().getBone(name);
        } catch (Exception e) {
            return null;
        }
    }
}
