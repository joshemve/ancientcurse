package com.ancientcurse.entity.model;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.ThothEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

/**
 * Thoth GeckoLib model - Egyptian God of Wisdom with floating animations and magic effects
 */
public class ThothModel extends GeoModel<ThothEntity> {

    /* ------------------ RESOURCES ------------------ */
    private static final Identifier MODEL = new Identifier(AncientCurse.MOD_ID, "geo/thoth.geo.json");
    private static final Identifier TEXTURE = new Identifier(AncientCurse.MOD_ID, "textures/entity/thoth.png");
    private static final Identifier TEXTURE_GREEN = new Identifier(AncientCurse.MOD_ID, "textures/entity/thoth_green.png");
    private static final Identifier TEXTURE_PURPLE = new Identifier(AncientCurse.MOD_ID, "textures/entity/thoth_purple.png");
    private static final Identifier ANIM = new Identifier(AncientCurse.MOD_ID, "animations/thoth.animation.json");

    /* ------------------ ANIMATION CONSTANTS ------------------ */
    private static final float MAX_HEAD_YAW = 45f;
    private static final float MAX_HEAD_PITCH = 30f;
    private static final float FLOATING_BOB_AMPLITUDE = 0.1f;
    private static final float MAGIC_GLOW_INTENSITY = 0.3f;
    
    /* ------------------ CACHED BONES ------------------ */
    private CoreGeoBone head, body, root, thoth;
    private CoreGeoBone leftArm, rightArm, leftHand, rightHand;
    private CoreGeoBone staffOfThoth, scroll;
    private CoreGeoBone leftLeg, rightLeg;
    private CoreGeoBone eyes, ibisHead;

    @Override 
    public Identifier getModelResource(ThothEntity entity) { 
        return MODEL; 
    }
    
    @Override
    public Identifier getTextureResource(ThothEntity entity) {
        return TEXTURE_GREEN; // Always use the green texture
    }
    
    @Override 
    public Identifier getAnimationResource(ThothEntity entity) { 
        return ANIM; 
    }

    @Override
    public void setCustomAnimations(ThothEntity thoth, long id, AnimationState<ThothEntity> state) {
        super.setCustomAnimations(thoth, id, state);

        if (head == null) cacheBones(); // Lazy init once per model instance

        EntityModelData data = state.getData(DataTickets.ENTITY_MODEL_DATA);
        if (data == null) return;

        float partialTick = state.getPartialTick();
        float age = thoth.age + partialTick;

        // Apply custom animations
        applyHeadRotation(data.netHeadYaw(), data.headPitch());
        applyFloatingMovement(thoth, age, partialTick);
        applyMagicEffects(thoth, age);
        applyStaffAnimations(thoth, age);
        applyReadingAnimations(thoth, age);
        applyTimeMagicEffects(thoth, age);
    }

    /**
     * Apply head rotation with limits for more natural movement
     */
    private void applyHeadRotation(float yaw, float pitch) {
        if (head == null) return;
        
        // Clamp rotation values for more natural movement
        float clampedYaw = MathHelper.clamp(yaw, -MAX_HEAD_YAW, MAX_HEAD_YAW);
        float clampedPitch = MathHelper.clamp(pitch, -MAX_HEAD_PITCH, MAX_HEAD_PITCH);
        
        head.setRotY((float) Math.toRadians(clampedYaw));
        head.setRotX((float) Math.toRadians(clampedPitch));
    }

    /**
     * Apply visual floating effects (does not affect physics, just visual bobbing)
     */
    private void applyFloatingMovement(ThothEntity thoth, float age, float partialTick) {
        if (this.thoth == null || body == null) return;
        
        if (!thoth.isInCombat()) {
            // Gentle visual bobbing motion for peaceful floating look
            // REMOVED: Manual setPosY conflicts with animation file's idle floating
            // float bobOffset = MathHelper.sin(age * 0.05f) * FLOATING_BOB_AMPLITUDE;
            // this.thoth.setPosY(bobOffset);
            
            // Slight body rotation for mystical floating effect
            float bodyRotation = MathHelper.sin(age * 0.03f) * 0.02f;
            body.setRotZ(bodyRotation);
            
            // Floating robes effect
            float robeWave = MathHelper.sin(age * 0.04f) * 0.1f;
            if (leftLeg != null) leftLeg.setRotX(robeWave);
            if (rightLeg != null) rightLeg.setRotX(-robeWave);
        } else {
            // Combat mode - more stable stance but keep slight mystical effects
            // this.thoth.setPosY(0);
            
            // Very subtle body movement in combat for divine presence
            float combatSway = MathHelper.sin(age * 0.02f) * 0.01f;
            body.setRotZ(combatSway);
            
            // Reset legs to stable combat stance
            if (leftLeg != null) leftLeg.setRotX(0);
            if (rightLeg != null) rightLeg.setRotX(0);
        }
    }

    /**
     * Apply general magic effects and ambient animations
     */
    private void applyMagicEffects(ThothEntity thoth, float age) {
        // Eye glow effect during magic casting
        if (eyes != null && (thoth.isCastingTimeMagic() || thoth.getAttackState() != 0)) {
            float glowPulse = (MathHelper.sin(age * 0.2f) + 1.0f) * 0.5f * MAGIC_GLOW_INTENSITY;
            eyes.setScaleX(1.0f + glowPulse);
            eyes.setScaleY(1.0f + glowPulse);
        } else if (eyes != null) {
            eyes.setScaleX(1.0f);
            eyes.setScaleY(1.0f);
        }
        
        // Ibis head subtle movement
        if (ibisHead != null) {
            float headTilt = MathHelper.sin(age * 0.08f) * 0.05f;
            ibisHead.setRotZ(headTilt);
        }
    }

    /**
     * Apply staff-specific animations based on Thoth's state
     * NOTE: Do NOT manipulate arm bones during attack animations - let GeckoLib handle them
     * IMPORTANT: Only apply visual effects to props (staff), never during active attack animations
     */
    private void applyStaffAnimations(ThothEntity thoth, float age) {
        if (staffOfThoth == null) return;

        int attackState = thoth.getAttackState();

        // FIXED: Only apply effects when NOT actively attacking to prevent animation conflicts
        // Let GeckoLib's animations handle everything during attacks
        if (attackState == 0 && !thoth.isInCombat()) {
            // Idle mystical glow effect only when peaceful
            float idleGlow = MathHelper.sin(age * 0.1f) * 0.05f + 1.0f;
            staffOfThoth.setScaleX(idleGlow);
            staffOfThoth.setScaleY(idleGlow);
        }
        // All attack animations are now 100% controlled by GeckoLib animation files
        // No manual bone manipulation during combat or attacks
    }

    /**
     * Apply reading animations when Thoth is reading scrolls
     * FIXED: Never manipulate bones during attacks - only apply subtle idle effects
     */
    private void applyReadingAnimations(ThothEntity thoth, float age) {
        if (scroll == null) return;

        // FIXED: Only apply reading effects during peaceful idle state
        // All attack animations (including scroll blast) are 100% handled by GeckoLib
        if (thoth.isReading() && thoth.getAttackState() == 0 && !thoth.isInCombat()) {
            // Very subtle page turning motion during peaceful reading only
            if (leftHand != null) {
                float pageFlip = MathHelper.sin(age * 0.08f) * 0.08f;
                leftHand.setRotX(pageFlip);
            }
        } else if (thoth.getAttackState() == 0) {
            // Only reset when not in attack animation
            if (leftHand != null) {
                leftHand.setRotX(0);
            }
        }
        // Scroll position is now 100% controlled by animation files during attacks
    }

    /**
     * Apply special time magic visual effects
     * FIXED: Only apply subtle scale distortion to body, never manipulate limbs
     */
    private void applyTimeMagicEffects(ThothEntity thoth, float age) {
        if (!thoth.isCastingTimeMagic()) {
            // Reset time distortion when not casting
            if (body != null) {
                body.setScaleX(1.0f);
                body.setScaleZ(1.0f);
            }
            return;
        }

        // Very subtle time distortion effect on body only during time magic
        // This adds visual flair without interfering with the time_bend animation
        float timeWarp = MathHelper.sin(age * 0.12f) * 0.02f;

        if (body != null) {
            body.setScaleX(1.0f + timeWarp);
            body.setScaleZ(1.0f - timeWarp);
        }

        // All limb positioning and rotations are 100% controlled by time_bend animation
    }

    /**
     * Cache all bone references for performance
     */
    private void cacheBones() {
        // Main structure bones
        root = getModelBone("root");
        thoth = getModelBone("thoth");
        body = getModelBone("body");
        head = getModelBone("head");
        
        // Limb bones
        leftArm = getModelBone("left_arm");
        rightArm = getModelBone("right_arm");
        leftHand = getModelBone("left_hand");
        rightHand = getModelBone("right_hand");
        leftLeg = getModelBone("left_leg");
        rightLeg = getModelBone("right_leg");
        
        // Equipment bones
        staffOfThoth = getModelBone("staff_of_thoth"); // Fixed bone name
        scroll = getModelBone("scroll");
        // tome = getModelBone("tome"); // Removed: Bone does not exist
        
        // Feature bones
        eyes = getModelBone("eyes");
        ibisHead = getModelBone("ibis_head");
    }

    /**
     * Safely get a bone by name, returning null if not found
     */
    private CoreGeoBone getModelBone(String name) {
        try {
            return getBone(name).orElse(null);
        } catch (Exception e) {
            // Bone doesn't exist in model, return null
            return null;
        }
    }
} 