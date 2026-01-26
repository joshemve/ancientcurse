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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import com.ancientcurse.network.CurseZonePackets;

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
    private static final int DAMAGE_FRAME = 23; // 1.15s - when slam impacts (per user feedback)

    public RaGroundSmackGoal(RaEntity ra) {
        this.ra = ra;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        // Don't start if hibernating, flying, or already performing action
        // This check is critical: it prevents this goal from overriding an active Shard
        // Attack
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
        // Ground smack triggers at 3-6 blocks, leaving 0-3 blocks for melee attacks
        double distance = ra.squaredDistanceTo(target);
        return distance >= 9.0 && distance <= 36.0; // 3-6 blocks (squared)
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

        // Stop all movement - Ra should be stationary during ground smack
        ra.setVelocity(0, 0, 0);
        ra.getNavigation().stop();

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

        // Ensure Ra stays still during animation
        ra.setVelocity(0, ra.getVelocity().y, 0); // Preserve gravity, stop horizontal movement

        // Deal damage at the slam frame
        int frameInAnimation = GROUND_SMACK_DURATION - animationTicks;

        // Track target and update beam direction ONLY during buildup phase (before
        // impact)
        // After DAMAGE_FRAME, Ra locks facing direction and beam direction
        if (frameInAnimation < DAMAGE_FRAME) {
            LivingEntity target = ra.getTarget();
            if (target != null && target.isAlive()) {
                // Track target with look control
                ra.getLookControl().lookAt(target, 30.0f, 30.0f);

                // Sync beam direction toward target
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
        // After DAMAGE_FRAME: Ra is locked in position and facing - no more tracking

        if (frameInAnimation == DAMAGE_FRAME && !hasDealtDamage) {
            System.out.println("[Ra DEBUG] DAMAGE_FRAME hit! Frame=" + frameInAnimation +
                    " (tick " + (animationTicks) + " remaining), time=" + (frameInAnimation / 20.0f) + "s");
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

        // Sun beam visual disabled - using new rope/shockwave effect instead
        // ra.setSunBeamTicks(40);

        // Play solar impact sound (beacon + blaze for divine feel)
        ra.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE, 1.2f, 1.5f);
        ra.playSound(SoundEvents.ENTITY_BLAZE_SHOOT, 0.8f, 0.8f);

        // Spawn solar ground impact particles (flame ring only, no explosion)
        spawnSolarImpactParticles(radius);

        // Screen shake for nearby players - intensity scales with phase
        // CRAZY Testing Intensity: 5.0 -> 15.0+ to ensure visibility
        float shakeIntensity = switch (phase) {
            case PHASE_1_AWAKENED -> 15.0f;
            case PHASE_2_SOLAR_WRATH -> 22.0f;
            case PHASE_3_DIVINE_FURY -> 30.0f;
        };

        // Fix: Sample ground position for shake origin to ensure nearby players on
        // ground feel it
        // even if Ra is slamming from mid-air.
        BlockPos shakeOrigin = ra.getBlockPos();
        for (int y = shakeOrigin.getY(); y > ra.getWorld().getBottomY(); y--) {
            BlockPos check = new BlockPos(shakeOrigin.getX(), y, shakeOrigin.getZ());
            if (!ra.getWorld().getBlockState(check).isAir()) {
                shakeOrigin = check.up();
                break;
            }
        }

        CurseZonePackets.sendScreenShake(
                (ServerWorld) ra.getWorld(),
                shakeOrigin,
                shakeIntensity,
                30, // Increased duration to 1.5 seconds (30 ticks)
                64 // Increased range to 64 blocks
        );
    }

    /**
     * Spawn solar-themed particles for the ground impact effect.
     * Creates radiating light rays shooting outward for a "woosh" effect.
     */
    private void spawnSolarImpactParticles(double radius) {
        if (!(ra.getWorld() instanceof ServerWorld serverWorld))
            return;

        // === RADIATING LIGHT RAYS (the "woosh" effect) ===
        // Increased to 32 rays for density
        System.out.println("[RaDebug] Spawning shockwave particles at " + ra.getPos());
        for (int i = 0; i < 32; i++) {
            double angle = Math.toRadians(i * 11.25); // 360/32 = 11.25 degrees apart
            double dirX = Math.cos(angle);
            double dirZ = Math.sin(angle);

            // Each ray is a stream of fast-moving flame particles
            for (int j = 0; j < 8; j++) {
                double distance = 0.3 + j * 0.5; // Staggered along the ray
                double speed = 0.2 + j * 0.04; // Faster particles further out

                // Use the server-side broadcast method that can force particles
                serverWorld.spawnParticles(
                        ParticleTypes.FLAME,
                        ra.getX() + dirX * distance,
                        ra.getY() + 0.2 + j * 0.05,
                        ra.getZ() + dirZ * distance,
                        3, // Increased count
                        0.1, 0.05, 0.1, // Increased delta
                        speed);
            }

            // Small flame trail along each ray for extra "woosh"
            serverWorld.spawnParticles(
                    ParticleTypes.SMALL_FLAME,
                    ra.getX() + dirX * radius * 0.5,
                    ra.getY() + 0.3,
                    ra.getZ() + dirZ * radius * 0.5,
                    4,
                    0.1, 0.05, 0.1,
                    0.12);
        }

        // === RISING LIGHT PILLAR (vertical beam at impact) ===
        for (int i = 0; i < 12; i++) {
            double height = 0.5 + i * 0.3;
            serverWorld.spawnParticles(
                    ParticleTypes.FLAME,
                    ra.getX(), ra.getY() + height, ra.getZ(),
                    2,
                    0.15, 0.1, 0.15,
                    0.02);
        }

        // Electric sparks shooting upward (divine energy)
        serverWorld.spawnParticles(
                ParticleTypes.ELECTRIC_SPARK,
                ra.getX(), ra.getY() + 0.5, ra.getZ(),
                20,
                0.3, 0.5, 0.3,
                0.15);

        // === EXPANDING RING of flames (ground shockwave) ===
        for (int i = 0; i < 24; i++) {
            double angle = Math.toRadians(i * 15);
            double x = ra.getX() + Math.cos(angle) * radius;
            double z = ra.getZ() + Math.sin(angle) * radius;

            // Fast outward-moving flames
            serverWorld.spawnParticles(
                    ParticleTypes.FLAME,
                    x, ra.getY() + 0.15, z,
                    2,
                    0.1, 0.05, 0.1,
                    0.08);
        }

        // Central lava burst (intense heat core)
        serverWorld.spawnParticles(
                ParticleTypes.LAVA,
                ra.getX(), ra.getY() + 0.2, ra.getZ(),
                6, 0.2, 0.1, 0.2, 0);

        // Light dust cloud (warm golden feel)
        serverWorld.spawnParticles(
                ParticleTypes.CAMPFIRE_COSY_SMOKE,
                ra.getX(), ra.getY() + 0.1, ra.getZ(),
                8,
                radius * 0.3, 0.1, radius * 0.3,
                0.005);
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
