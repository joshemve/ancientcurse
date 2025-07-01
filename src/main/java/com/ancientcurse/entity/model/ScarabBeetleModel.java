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
    // Animation constants for realistic beetle movement
    private static final float LEG_SWING_AMPLITUDE = 0.15f; // Reduced for smoother movement
    private static final float LEG_LIFT_HEIGHT = 0.1f;      // Reduced lift height  
    private static final float LEG_GROUND_EXTEND = 0.08f;   // Less extreme extension
    private static final float WALK_SPEED_MULTIPLIER = 0.4f; // Slower, more deliberate movement
    private static final float BODY_WOBBLE_AMOUNT = 0.02f;   // Reduced body movement

    /* ------------------ CACHED BONES ------------------ */
    private CoreGeoBone head, body, root;
    private CoreGeoBone leftPincher, rightPincher;
    private CoreGeoBone antennas, antennaL, antennaR, eyes;
    
    // Leg segments: Each leg has 3 parts (top, middle, bottom)
    // Front legs
    private CoreGeoBone frontRightTop, frontRightMiddle, frontRightBottom;
    private CoreGeoBone frontLeftTop, frontLeftMiddle, frontLeftBottom;
    // Middle legs  
    private CoreGeoBone middleRightTop, middleRightMiddle, middleRightBottom;
    private CoreGeoBone middleLeftTop, middleLeftMiddle, middleLeftBottom;
    // Back legs
    private CoreGeoBone backRightTop, backRightMiddle, backRightBottom;
    private CoreGeoBone backLeftTop, backLeftMiddle, backLeftBottom;

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
        float swing = beetle.getLegSwingProgress(partialTick);
        float swingAmount = beetle.getLimbSwingAmount(partialTick);
        float age = beetle.age + partialTick;

        // Apply animations
        applyHeadRotation(data.netHeadYaw(), data.headPitch());
        applyLegMovement(swing, swingAmount);
        applyBodyMovement(beetle, swing, swingAmount, partialTick);
        applyPinchers(beetle, age);
        applyAntennas(age, swingAmount);
        
        if (beetle.isGolden()) {
            applyGoldenShimmer(age);
        }
    }

    private void applyHeadRotation(float yaw, float pitch) {
        if (head == null) return;
        head.setRotY((float) Math.toRadians(MathHelper.clamp(yaw, -MAX_HEAD_YAW, MAX_HEAD_YAW)));
        head.setRotX((float) Math.toRadians(MathHelper.clamp(pitch, -MAX_HEAD_PITCH, MAX_HEAD_PITCH)));
    }

    private void applyLegMovement(float swing, float swingAmount) {
        // Cache bones if needed
        if (frontRightTop == null) cacheBones();
        
        // Set all legs to grounded stance first
        setGroundedStance();
        
        // Only apply movement if beetle is actually moving
        if (swingAmount > 0.01f) {
            float cycle = swing * WALK_SPEED_MULTIPLIER;
            float moveIntensity = Math.min(swingAmount * 1.2f, 1.0f);
            
            // Apply tripod gait - alternating leg groups
            applyTripodGait(cycle, moveIntensity);
            
            // Apply subtle body wobble
            applyBodyWobble(cycle, moveIntensity);
        }
    }

    /**
     * Implements proper insect tripod gait locomotion
     * Tripod A: Front-Right + Middle-Left + Back-Right
     * Tripod B: Front-Left + Middle-Right + Back-Left
     */
    private void applyTripodGait(float cycle, float intensity) {
        // Calculate phase for each tripod group
        float tripodAPhase = MathHelper.sin(cycle);
        float tripodBPhase = MathHelper.sin(cycle + (float)Math.PI); // 180 degrees out of phase
        
        // Apply movement to Tripod A (Front-Right, Middle-Left, Back-Right)
        applyLegTripodMovement(frontRightTop, frontRightMiddle, frontRightBottom, tripodAPhase, intensity);
        applyLegTripodMovement(middleLeftTop, middleLeftMiddle, middleLeftBottom, tripodAPhase, intensity);
        applyLegTripodMovement(backRightTop, backRightMiddle, backRightBottom, tripodAPhase, intensity);
        
        // Apply movement to Tripod B (Front-Left, Middle-Right, Back-Left)
        applyLegTripodMovement(frontLeftTop, frontLeftMiddle, frontLeftBottom, tripodBPhase, intensity);
        applyLegTripodMovement(middleRightTop, middleRightMiddle, middleRightBottom, tripodBPhase, intensity);
        applyLegTripodMovement(backLeftTop, backLeftMiddle, backLeftBottom, tripodBPhase, intensity);
    }

    /**
     * Applies subtle walking movement to legs - preserves geo file base rotations
     * The geo file already positions legs correctly to touch ground
     */
    private void applyLegTripodMovement(CoreGeoBone topBone, CoreGeoBone middleBone, CoreGeoBone bottomBone, 
                                        float phase, float intensity) {
        if (topBone == null || middleBone == null || bottomBone == null) return;
        
        // Smooth step function for more natural movement
        float smoothPhase = smoothStep(-1f, 1f, phase);
        
        // Apply VERY SUBTLE walking animation - additive to geo base rotations
        // The geo file already positions legs correctly to touch ground
        float subtleSwing = smoothPhase * LEG_SWING_AMPLITUDE * intensity * 0.3f; // Much smaller movement
        
        // Reset to base geo rotations (which properly position legs on ground)
        // Only add minimal walking movement for animation
        topBone.setRotX(subtleSwing * 0.2f); // Very small top movement
        topBone.setRotY(0); 
        topBone.setRotZ(0); 
        
        // Middle and bottom segments - trust the geo file positioning completely
        // The geo file base rotations handle proper ground contact
        middleBone.setRotX(0); // Reset to geo file base rotation
        middleBone.setRotY(0); 
        middleBone.setRotZ(0); 
        
        bottomBone.setRotX(0); // Reset to geo file base rotation
        bottomBone.setRotY(0);   
        bottomBone.setRotZ(0); 
    }

    /**
     * Applies subtle body movement during walking - NO ROTATION to prevent tilting
     */
    private void applyBodyWobble(float cycle, float intensity) {
        if (body == null) return;
        
        // Only very subtle vertical movement, no tilting or rotation
        // Use lowered positioning to ensure legs touch ground
        float bodyBob = Math.abs(MathHelper.sin(cycle * 0.5f)) * 0.002f * intensity;
        body.setPosY(-0.15f + bodyBob); // Lowered position + subtle wobble
        
        // ENSURE NO ROTATION - keep beetle completely level
        body.setRotX(0);
        body.setRotY(0);
        body.setRotZ(0);
        
        // Also ensure root is level if it exists
        if (root != null) {
            root.setRotX(0);
            root.setRotY(0);
            root.setRotZ(0);
        }
    }

    /**
     * Sets all legs to natural grounded stance - WORKS WITH EXISTING GEO ROTATIONS
     */
    private void setGroundedStance() {
        // DON'T override the base rotations from geo file - they position legs correctly
        // Only make minimal adjustments if needed
        
        // The geo file already has the correct base rotations:
        // Front legs: [21.17901, 34.76135, 34.19815] and [21.17901, -34.76135, -34.19815] 
        // Middle segments: [0, 0, 20] and [0, 0, -20]
        // Bottom segments: [0, 0, 10] and [0, 0, -10]
        
        // Reset any previous animation modifications to return to base geo rotations
        if (frontRightTop != null) {
            frontRightTop.setRotX(0);
            frontRightTop.setRotY(0);
            frontRightTop.setRotZ(0);
        }
        if (frontLeftTop != null) {
            frontLeftTop.setRotX(0);
            frontLeftTop.setRotY(0);
            frontLeftTop.setRotZ(0);
        }
        if (middleRightTop != null) {
            middleRightTop.setRotX(0);
            middleRightTop.setRotY(0);
            middleRightTop.setRotZ(0);
        }
        if (middleLeftTop != null) {
            middleLeftTop.setRotX(0);
            middleLeftTop.setRotY(0);
            middleLeftTop.setRotZ(0);
        }
        if (backRightTop != null) {
            backRightTop.setRotX(0);
            backRightTop.setRotY(0);
            backRightTop.setRotZ(0);
        }
        if (backLeftTop != null) {
            backLeftTop.setRotX(0);
            backLeftTop.setRotY(0);
            backLeftTop.setRotZ(0);
        }
        
        // Reset middle segments to their base rotations from geo file
        CoreGeoBone[] middles = {frontRightMiddle, frontLeftMiddle, middleRightMiddle, 
                               middleLeftMiddle, backRightMiddle, backLeftMiddle};
        for (CoreGeoBone middle : middles) {
            if (middle != null) {
                middle.setRotX(0);
                middle.setRotY(0);
                middle.setRotZ(0);
            }
        }
        
        // Reset bottom segments to their base rotations from geo file
        CoreGeoBone[] bottoms = {frontRightBottom, frontLeftBottom, middleRightBottom,
                               middleLeftBottom, backRightBottom, backLeftBottom};
        for (CoreGeoBone bottom : bottoms) {
            if (bottom != null) {
                bottom.setRotX(0);
                bottom.setRotY(0);
                bottom.setRotZ(0);
            }
        }
        
        // Lower the entire beetle to ensure all legs touch the ground
        if (body != null) {
            body.setPosY(-0.15f); // Lower the beetle to get legs on ground
            body.setRotX(0);
            body.setRotY(0);
            body.setRotZ(0);
        }
        
        // Also ensure root is positioned correctly and level
        if (root != null) {
            root.setPosY(0); // Keep root at normal level
            root.setRotX(0); // No tilt
            root.setRotY(0);
            root.setRotZ(0); // Completely level
        }
    }

    /**
     * Smooth step function for more natural interpolation
     */
    private float smoothStep(float min, float max, float value) {
        float t = MathHelper.clamp((value - min) / (max - min), 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    private void applyBodyMovement(ScarabBeetleEntity beetle, float swing, float swingAmount, float partialTick) {
        if (body == null) return;
        
        // ENSURE NO TILTING - reset all rotations first
        body.setRotX(0);
        body.setRotY(0);
        body.setRotZ(0);
        
        // Very minimal body bobbing while walking (lowered to get legs on ground)
        if (swingAmount > 0.1f) {
            float bob = MathHelper.abs(MathHelper.sin(swing * 0.6662f)) * swingAmount * 0.005f; // Reduced from 0.015f
            body.setPosY(-0.15f + bob); // Lowered positioning + walking bob
        } else {
            body.setPosY(-0.15f); // Lowered position when stationary
        }
        
        // Very subtle breathing animation (reduced to prevent scaling issues)
        float breathe = MathHelper.sin((beetle.age + partialTick) * 0.1f) * 0.005f; // Reduced from 0.01f
        body.setScaleY(1.0f + breathe);
        
        // Ensure root stays level too
        if (root != null) {
            root.setRotX(0);
            root.setRotY(0);
            root.setRotZ(0);
        }
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
        
        // Front legs (3 segments each)
        frontRightTop = getModelBone("top");           // right_leg -> top
        frontRightMiddle = getModelBone("middle");     // top -> middle
        frontRightBottom = getModelBone("bottom");     // middle -> bottom
        
        frontLeftTop = getModelBone("top5");           // left_leg -> top5
        frontLeftMiddle = getModelBone("middle7");     // top5 -> middle7
        frontLeftBottom = getModelBone("bottom6");     // middle7 -> bottom6
        
        // Back legs
        backRightTop = getModelBone("top1");           // right_leg1 -> top1
        backRightMiddle = getModelBone("middle2");     // top1 -> middle2
        backRightBottom = getModelBone("bottom2");     // middle2 -> bottom2
        
        backLeftTop = getModelBone("top3");            // right_leg3 -> top3 (actually left)
        backLeftMiddle = getModelBone("middle5");      // top3 -> middle5
        backLeftBottom = getModelBone("bottom4");      // middle5 -> bottom4
        
        // Middle legs
        middleRightTop = getModelBone("top2");         // right_leg2 -> top2
        middleRightMiddle = getModelBone("middle3");   // top2 -> middle3
        middleRightBottom = getModelBone("bottom3");   // middle3 -> bottom3
        
        middleLeftTop = getModelBone("top4");          // right_leg4 -> top4 (actually left)
        middleLeftMiddle = getModelBone("middle6");    // top4 -> middle6
        middleLeftBottom = getModelBone("bottom5");    // middle6 -> bottom5
    }

    private CoreGeoBone getModelBone(String name) {
        try {
            return getAnimationProcessor().getBone(name);
        } catch (Exception e) {
            return null;
        }
    }
}
