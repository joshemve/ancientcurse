package com.ancientcurse.item.model;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.item.PurificationStaffItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

/**
 * Model for the Purification Staff with procedural physics for the dangling
 * piece.
 * Includes ultra-smooth interpolation and rhythmic walking bob.
 */
public class PurificationStaffModel extends GeoModel<PurificationStaffItem> {
    @Override
    public Identifier getModelResource(PurificationStaffItem animatable) {
        return new Identifier(AncientCurse.MOD_ID, "geo/purification_staff.geo.json");
    }

    @Override
    public Identifier getTextureResource(PurificationStaffItem animatable) {
        return new Identifier(AncientCurse.MOD_ID, "textures/item/purification_staff.png");
    }

    @Override
    public Identifier getAnimationResource(PurificationStaffItem animatable) {
        return null; // Using procedural animations
    }

    @Override
    public void setCustomAnimations(PurificationStaffItem animatable, long instanceId,
            AnimationState<PurificationStaffItem> state) {
        super.setCustomAnimations(animatable, instanceId, state);

        var proc = this.getAnimationProcessor();
        var base = proc.getBone("dangle_base");
        var tip = proc.getBone("dangle_tip");
        if (base == null || tip == null)
            return;

        // Get per-instance state
        PurificationStaffItem.DangleState s = animatable.getOrCreateDangleState(instanceId);
        float partialTick = state.getPartialTick();

        // Driver input
        float yaw, pitch;
        double x, y, z;

        var entity = state.getData(DataTickets.ENTITY);
        if (entity instanceof LivingEntity living) {
            // Interpolate position and rotation
            yaw = MathHelper.lerpAngleDegrees(partialTick, living.prevBodyYaw, living.bodyYaw);

            // responsiveness: Mix in head yaw so the staff reacts to camera turns quickly
            float headYaw = MathHelper.lerpAngleDegrees(partialTick, living.prevHeadYaw, living.headYaw);
            yaw = MathHelper.lerpAngleDegrees(0.25f, yaw, headYaw); // 25% head influence for "snappier" feel

            pitch = MathHelper.lerp(partialTick, living.prevPitch, living.getPitch());
            x = MathHelper.lerp(partialTick, living.prevX, living.getX());
            y = MathHelper.lerp(partialTick, living.prevY, living.getY());
            z = MathHelper.lerp(partialTick, living.prevZ, living.getZ());
        } else {
            yaw = 0;
            pitch = 0;
            x = 0;
            y = 0;
            z = 0;
        }

        // Detect GUI context
        EntityModelData emd = state.getData(DataTickets.ENTITY_MODEL_DATA);
        boolean inGui = emd != null;

        // Initialize smooth state if needed or reset on context switch
        if (!s.initialized || s.wasInGui != inGui) {
            s.lastSmoothX = x;
            s.lastSmoothY = y;
            s.lastSmoothZ = z;
            s.lastSmoothYaw = yaw;
            s.lastSmoothPitch = pitch;
            s.vel = 0;
            s.velX = 0;
            s.initialized = true;
            s.wasInGui = inGui;
        }

        // --- Calculate Smooth Deltas ---
        float yawDelta = MathHelper.wrapDegrees(yaw - s.lastSmoothYaw);
        float pitchDelta = pitch - s.lastSmoothPitch;

        double vx = x - s.lastSmoothX;
        double vy = y - s.lastSmoothY;
        double vz = z - s.lastSmoothZ;

        // Anti-snap/teleport protection
        if (Math.abs(yawDelta) > 60)
            yawDelta = 0;
        if (Math.abs(vx) > 1 || Math.abs(vy) > 1 || Math.abs(vz) > 1) {
            vx = 0;
            vy = 0;
            vz = 0;
        }

        s.lastSmoothYaw = yaw;
        s.lastSmoothPitch = pitch;
        s.lastSmoothX = x;
        s.lastSmoothY = y;
        s.lastSmoothZ = z;

        // --- Walking Bob ---
        // Calculate horizontal speed for bob frequency
        float horizontalSpeed = (float) Math.sqrt(vx * vx + vz * vz);
        if (horizontalSpeed > 0.0001f) {
            // Rhythmic timer that continues even if speed is variable
            s.bobTimer += horizontalSpeed * 10.0f;
        } else {
            // Slowly settle the bob timer when stopped
            s.bobTimer *= 0.8f;
        }

        // Bob offsets
        // sin for forward/back sway, cos for side sway (creates a figure-8 like bob)
        float bobXOffset = MathHelper.sin(s.bobTimer) * horizontalSpeed * 0.4f;
        float bobZOffset = MathHelper.cos(s.bobTimer * 0.5f) * horizontalSpeed * 0.3f;

        // --- Project Velocity into Staff Local Space ---
        double radYaw = Math.toRadians(yaw);
        float cos = (float) Math.cos(radYaw);
        float sin = (float) Math.sin(radYaw);

        // Map world velocity to player-relative velocity
        // localVelX is strafe speed, localVelZ is forward/back speed
        float localVelX = (float) (vx * cos + vz * sin);
        float localVelZ = (float) (vz * cos - vx * sin);

        // --- Drive Targets ---
        // Side-to-side swing (Z axis)
        float targetZ = (-yawDelta * 0.04f) - (localVelX * 1.2f) + bobZOffset;

        // Forward/Backward swing (X axis)
        float targetX = (pitchDelta * 0.03f) - (localVelZ * 1.5f) + bobXOffset;

        // --- Ultra-Soft Physics Simulation ---
        // Very low stiffness and high damping for "creamy", heavy movement
        float stiffness = 0.015f;
        float damping = 0.96f;

        // Z-Axis (Sideways Swing)
        float accelZ = (targetZ - s.angle) * stiffness;
        s.vel += accelZ;
        s.vel *= damping;
        s.angle += s.vel;

        // X-Axis (Forward/Back Swing)
        float accelX = (targetX - s.angleX) * stiffness;
        s.velX += accelX;
        s.velX *= damping;
        s.angleX += s.velX;

        // --- Clamp & Apply ---
        float maxRad = (float) Math.toRadians(20); // Subtle 20-degree range
        s.angle = MathHelper.clamp(s.angle, -maxRad, maxRad);
        s.angleX = MathHelper.clamp(s.angleX, -maxRad, maxRad);

        // Apply with a slight multiplier for the tip to make it feel flexible
        base.setRotZ(s.angle);
        tip.setRotZ(s.angle * 1.25f);

        base.setRotX(s.angleX);
        tip.setRotX(s.angleX * 1.25f);
    }
}
