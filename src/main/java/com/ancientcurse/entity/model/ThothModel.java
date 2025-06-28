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
    private CoreGeoBone staff, scroll, tome;
    private CoreGeoBone leftLeg, rightLeg;
    private CoreGeoBone eyes, ibisHead;

    @Override 
    public Identifier getModelResource(ThothEntity entity) { 
        return MODEL; 
    }
    
    @Override 
    public Identifier getTextureResource(ThothEntity entity) { 
        // Different texture variants based on magic state
        if (entity.isCastingTimeMagic()) {
            return TEXTURE_PURPLE; // Purple when casting time magic
        } else if (entity.isReading()) {
            return TEXTURE_GREEN; // Green when reading scrolls
        }
        return TEXTURE; // Default texture
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
            float bobOffset = MathHelper.sin(age * 0.05f) * FLOATING_BOB_AMPLITUDE;
            this.thoth.setPosY(bobOffset);
            
            // Slight body rotation for mystical floating effect
            float bodyRotation = MathHelper.sin(age * 0.03f) * 0.02f;
            body.setRotZ(bodyRotation);
            
            // Floating robes effect
            float robeWave = MathHelper.sin(age * 0.04f) * 0.1f;
            if (leftLeg != null) leftLeg.setRotX(robeWave);
            if (rightLeg != null) rightLeg.setRotX(-robeWave);
        } else {
            // Combat mode - more stable stance but keep slight mystical effects
            this.thoth.setPosY(0);
            
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
     */
    private void applyStaffAnimations(ThothEntity thoth, float age) {
        if (staff == null) return;
        
        int attackState = thoth.getAttackState();
        
        // Only apply staff effects, never manipulate arms during attacks
        if (attackState == 1) { // Magic ball attack
            // Staff glowing effect only
            float glowPulse = MathHelper.sin(age * 0.5f) * 0.2f;
            staff.setScaleX(1.0f + glowPulse);
            staff.setScaleY(1.0f + glowPulse);
            
        } else if (attackState == 3) { // Time bend attack
            // Staff spinning motion only
            float spinRotation = age * 0.3f;
            staff.setRotZ(spinRotation);
            
        } else {
            // Reset staff effects only
            staff.setRotZ(0);
            staff.setScaleX(1.0f);
            staff.setScaleY(1.0f);
        }
        
        // NEVER manipulate arm bones during attacks - GeckoLib animations handle this
    }

    /**
     * Apply reading animations when Thoth is reading scrolls or tomes
     * NOTE: Only manipulate props (scroll/tome), not hands/arms during attacks
     */
    private void applyReadingAnimations(ThothEntity thoth, float age) {
        if (scroll == null || tome == null) return;
        
        if (thoth.isReading()) {
            // Only manipulate hand during non-attack reading (idle reading)
            if (leftHand != null && thoth.getAttackState() == 0) {
                // Gentle page turning motion only during idle reading
                float pageFlip = MathHelper.sin(age * 0.1f) * 0.1f;
                leftHand.setRotX(pageFlip);
            }
            
            // Scroll/tome positioning (props only, not hands)
            if (thoth.getAttackState() == 2) { // Scroll blast attack
                scroll.setRotX((float) Math.toRadians(-30));
                scroll.setPosY(0.2f);
            } else {
                // Reading tome in idle
                tome.setRotX((float) Math.toRadians(-15));
                tome.setPosY(0.1f);
            }
        } else {
            // Reset reading poses
            if (leftHand != null && thoth.getAttackState() == 0) {
                leftHand.setRotX(0); // Only reset hand when not attacking
            }
            scroll.setRotX(0);
            scroll.setPosY(0);
            tome.setRotX(0);
            tome.setPosY(0);
        }
    }

    /**
     * Apply special time magic visual effects
     * NOTE: Do NOT manipulate arm bones during attack animations - let GeckoLib handle them
     */
    private void applyTimeMagicEffects(ThothEntity thoth, float age) {
        if (!thoth.isCastingTimeMagic()) return;
        
        // Time distortion effect on the entire model (body only, not arms)
        float timeWarp = MathHelper.sin(age * 0.15f) * 0.03f;
        
        if (body != null) {
            body.setScaleX(1.0f + timeWarp);
            body.setScaleZ(1.0f - timeWarp);
        }
        
        // REMOVED: Do not manipulate arms during time magic - GeckoLib animations handle this
        // The time_bend animation in the JSON file will control arm positioning
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
        staff = getModelBone("staff");
        scroll = getModelBone("scroll");
        tome = getModelBone("tome");
        
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