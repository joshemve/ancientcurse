package com.ancientcurse.entity.ai;

import com.ancientcurse.entity.RaEntity;
import com.ancientcurse.entity.RaEntity.RaPhase;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;

import java.util.EnumSet;
import java.util.List;

/**
 * AI Goal for Ra's ground smack attack (jump + twist + slam).
 *
 * Server-side only - animation state synced via DataTracker.
 *
 * This is a ground-based attack where Ra jumps, twists, and slams down.
 * Deals AoE damage on landing.
 *
 * Phase 1: Long cooldown, small AoE
 * Phase 2: Medium cooldown, medium AoE
 * Phase 3: Short cooldown, large AoE + more damage
 */
public class RaGroundSmackGoal extends Goal {
    private final RaEntity ra;

    // Timing
    private int cooldown = 0;
    private int animationTicks = 0;
    private boolean hasDealtDamage = false;

    // Phase-based configuration
    private static final int PHASE_1_COOLDOWN = 200; // 10 seconds
    private static final int PHASE_2_COOLDOWN = 120; // 6 seconds
    private static final int PHASE_3_COOLDOWN = 60; // 3 seconds

    private static final float PHASE_1_DAMAGE = 6.0f;
    private static final float PHASE_2_DAMAGE = 10.0f;
    private static final float PHASE_3_DAMAGE = 14.0f;

    private static final double PHASE_1_RADIUS = 3.0;
    private static final double PHASE_2_RADIUS = 4.0;
    private static final double PHASE_3_RADIUS = 5.0;

    /*
     * Animation timing - MUST match ra.animation.json "ra.flying_ground_smack"!
     * Source: src/main/resources/assets/ancientcurse/animations/ra.animation.json
     * Animation length: 3.0 seconds = 60 ticks
     *
     * DAMAGE_FRAME should match the keyframe where the slam visually impacts
     * ground.
     * To find this: Open ra.animation.json in Blockbench, scrub to the impact
     * frame,
     * and note the timestamp. Multiply by 20 to get ticks.
     */
    private static final int GROUND_SMACK_DURATION = 60; // 3 seconds - matches animation_length
    private static final int DAMAGE_FRAME = 20; // 1.0s - when slam impacts (per user feedback)

    public RaGroundSmackGoal(RaEntity ra) {
        this.ra = ra;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        // Don't start if hibernating, flying, or already performing action
        if (ra.isHibernating() || ra.isFlying() || ra.isPerformingAction()) {
            return false;
        }

        // Must have a target
        LivingEntity target = ra.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }

        // Must be off cooldown
        if (cooldown > 0) {
            return false;
        }

        // Target must be within range (but not too close - melee handles that)
        double distance = ra.squaredDistanceTo(target);
        return distance >= 4.0 && distance <= 36.0; // 2-6 blocks
    }

    @Override
    public boolean shouldContinue() {
        // Continue until animation completes
        return animationTicks > 0 && !ra.isHibernating();
    }

    @Override
    public void start() {
        // Trigger the animation
        ra.triggerGroundSmack();
        animationTicks = GROUND_SMACK_DURATION;
        hasDealtDamage = false;

        // Look at target
        if (ra.getTarget() != null) {
            ra.getLookControl().lookAt(ra.getTarget(), 30.0f, 30.0f);
        }
    }

    @Override
    public void stop() {
        animationTicks = 0;
        hasDealtDamage = false;
        resetCooldown();
    }

    /**
     * Force start the ground smack attack for debugging.
     * Bypasses all checks (cooldown, distance, etc.)
     */
    public void forceStart() {
        // Trigger the animation and state
        ra.triggerGroundSmack();
        animationTicks = GROUND_SMACK_DURATION;
        hasDealtDamage = false;
        cooldown = 0; // Reset cooldown so it can trigger

        // Look at target if available
        if (ra.getTarget() != null) {
            ra.getLookControl().lookAt(ra.getTarget(), 30.0f, 30.0f);
        }
    }

    @Override
    public void tick() {
        animationTicks--;

        // Deal damage at the slam frame
        int frameInAnimation = GROUND_SMACK_DURATION - animationTicks;

        // Track target and update beam direction during buildup phase (first 1s)
        LivingEntity target = ra.getTarget();
        if (target != null && target.isAlive()) {
            ra.getLookControl().lookAt(target, 30.0f, 30.0f);

            // Sync beam direction until the impact point (DAMAGE_FRAME)
            if (frameInAnimation < DAMAGE_FRAME) {
                net.minecraft.util.math.Vec3d startPos = ra.getPos().add(0, 3.0, 0);
                net.minecraft.util.math.Vec3d targetPos = target.getPos().add(0, target.getHeight() * 0.5, 0);
                net.minecraft.util.math.Vec3d direction = targetPos.subtract(startPos).normalize();

                // Keep it slightly downward if needed
                if (direction.y > -0.1) {
                    direction = new net.minecraft.util.math.Vec3d(direction.x, -0.1, direction.z).normalize();
                }
                ra.setSunBeamDirection(direction);
            }
        }

        if (frameInAnimation == DAMAGE_FRAME && !hasDealtDamage) {
            dealAreaDamage();
            hasDealtDamage = true;
        }
    }

    /**
     * Deal AoE damage around Ra's position.
     */
    private void dealAreaDamage() {
        if (ra.getWorld().isClient)
            return;

        RaPhase phase = ra.getCurrentPhase();

        float damage = switch (phase) {
            case PHASE_1_AWAKENED -> PHASE_1_DAMAGE;
            case PHASE_2_SOLAR_WRATH -> PHASE_2_DAMAGE;
            case PHASE_3_DIVINE_FURY -> PHASE_3_DAMAGE;
        };

        double radius = switch (phase) {
            case PHASE_1_AWAKENED -> PHASE_1_RADIUS;
            case PHASE_2_SOLAR_WRATH -> PHASE_2_RADIUS;
            case PHASE_3_DIVINE_FURY -> PHASE_3_RADIUS;
        };

        // Create damage area
        Box damageBox = new Box(
                ra.getX() - radius, ra.getY() - 1, ra.getZ() - radius,
                ra.getX() + radius, ra.getY() + 2, ra.getZ() + radius);

        // Get all players in range
        List<PlayerEntity> targets = ra.getWorld().getEntitiesByClass(
                PlayerEntity.class,
                damageBox,
                player -> player.isAlive() && !player.isSpectator());

        // Deal damage to all
        DamageSource damageSource = ra.getWorld().getDamageSources().mobAttack(ra);
        for (PlayerEntity player : targets) {
            player.damage(damageSource, damage);

            // Knockback away from Ra
            double dx = player.getX() - ra.getX();
            double dz = player.getZ() - ra.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > 0) {
                double knockback = 1.5;
                player.addVelocity(dx / dist * knockback, 0.4, dz / dist * knockback);
            }
        }

        // Trigger sun beam visual on impact (longer duration for ground smack)
        ra.setSunBeamTicks(40);

        // Play solar impact sound (beacon + blaze for divine feel)
        ra.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE, 1.2f, 1.5f);
        ra.playSound(SoundEvents.ENTITY_BLAZE_SHOOT, 0.8f, 0.8f);

        // Spawn solar ground impact particles (flame ring only, no explosion)
        spawnSolarImpactParticles(radius);
    }

    /**
     * Spawn solar-themed particles for the ground impact effect.
     * Uses golden/yellow particles to match the sun beam visual.
     */
    private void spawnSolarImpactParticles(double radius) {
        if (!(ra.getWorld() instanceof ServerWorld serverWorld))
            return;

        // Ring of golden flame particles
        for (int i = 0; i < 36; i++) {
            double angle = Math.toRadians(i * 10);
            double x = ra.getX() + Math.cos(angle) * radius;
            double z = ra.getZ() + Math.sin(angle) * radius;

            serverWorld.spawnParticles(
                    ParticleTypes.FLAME,
                    x, ra.getY() + 0.1, z,
                    3, // count
                    0.2, 0.1, 0.2, // spread
                    0.05 // speed
            );

            // Add end rod particles for golden shimmer
            serverWorld.spawnParticles(
                    ParticleTypes.END_ROD,
                    x, ra.getY() + 0.3, z,
                    1,
                    0.1, 0.2, 0.1,
                    0.02);
        }

        // Central solar burst (lava + end rods instead of explosion)
        serverWorld.spawnParticles(
                ParticleTypes.LAVA,
                ra.getX(), ra.getY() + 0.3, ra.getZ(),
                8, 0.3, 0.1, 0.3, 0);

        serverWorld.spawnParticles(
                ParticleTypes.END_ROD,
                ra.getX(), ra.getY() + 0.5, ra.getZ(),
                12, 0.5, 0.3, 0.5, 0.05);

        // Warm dust cloud
        serverWorld.spawnParticles(
                ParticleTypes.CAMPFIRE_COSY_SMOKE,
                ra.getX(), ra.getY() + 0.2, ra.getZ(),
                15,
                radius * 0.4, 0.2, radius * 0.4,
                0.01);
    }

    /**
     * Called from RaEntity.tick() to update cooldown.
     */
    public void tickCooldown() {
        if (cooldown > 0) {
            cooldown--;
        }
    }

    /**
     * Reset cooldown based on current phase.
     */
    private void resetCooldown() {
        RaPhase phase = ra.getCurrentPhase();
        cooldown = switch (phase) {
            case PHASE_1_AWAKENED -> PHASE_1_COOLDOWN;
            case PHASE_2_SOLAR_WRATH -> PHASE_2_COOLDOWN;
            case PHASE_3_DIVINE_FURY -> PHASE_3_COOLDOWN;
        };
    }
}
