package com.ancientcurse.entity.ai;

import com.ancientcurse.entity.RaEntity;
import com.ancientcurse.entity.RaEntity.RaPhase;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.EnumSet;

/**
 * AI Goal that controls Ra's flight behavior based on combat phase.
 *
 * Server-side only - flight state is synced to clients via DataTracker.
 *
 * Phase 1: Rarely flies (10% chance after ground combat timer)
 * Phase 2: Moderate flying (30% chance, longer durations)
 * Phase 3: Frequent flying (50% chance, aggressive)
 */
public class RaFlightGoal extends Goal {
    private final RaEntity ra;

    // Timing (in ticks)
    private int groundCombatTimer = 0;
    private int flightTimer = 0;

    // Phase-based timing configuration
    private static final int PHASE_1_GROUND_TIME = 200; // 10 seconds on ground
    private static final int PHASE_2_GROUND_TIME = 120; // 6 seconds on ground
    private static final int PHASE_3_GROUND_TIME = 60; // 3 seconds on ground

    private static final int PHASE_1_FLIGHT_TIME = 60; // 3 seconds flying
    private static final int PHASE_2_FLIGHT_TIME = 100; // 5 seconds flying
    private static final int PHASE_3_FLIGHT_TIME = 80; // 4 seconds flying (aggressive, lands to attack)

    private static final float PHASE_1_FLIGHT_CHANCE = 0.1f;
    private static final float PHASE_2_FLIGHT_CHANCE = 0.3f;
    private static final float PHASE_3_FLIGHT_CHANCE = 0.5f;

    public RaFlightGoal(RaEntity ra) {
        this.ra = ra;
        this.setControls(EnumSet.of(Control.MOVE, Control.JUMP, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        // Don't start if hibernating or performing action
        if (ra.isHibernating() || ra.isPerformingAction()) {
            return false;
        }

        // Don't start if already flying
        if (ra.isFlying()) {
            return false;
        }

        // Must have a target to fly
        if (ra.getTarget() == null) {
            return false;
        }

        // Check if ground combat timer expired
        return groundCombatTimer <= 0;
    }

    @Override
    public boolean shouldContinue() {
        // Stop if hibernating
        if (ra.isHibernating()) {
            return false;
        }

        // Continue while flying and timer not expired
        return ra.isFlying() && flightTimer > 0;
    }

    @Override
    public void start() {
        RaPhase phase = ra.getCurrentPhase();

        // Roll for flight chance based on phase
        float flightChance = switch (phase) {
            case PHASE_1_AWAKENED -> PHASE_1_FLIGHT_CHANCE;
            case PHASE_2_SOLAR_WRATH -> PHASE_2_FLIGHT_CHANCE;
            case PHASE_3_DIVINE_FURY -> PHASE_3_FLIGHT_CHANCE;
        };

        if (ra.getRandom().nextFloat() < flightChance) {
            // Take off to random height between 3-6 blocks
            float targetHeight = 3.0f + ra.getRandom().nextFloat() * 3.0f;
            ra.takeOff(targetHeight);

            // Set flight duration based on phase
            flightTimer = switch (phase) {
                case PHASE_1_AWAKENED -> PHASE_1_FLIGHT_TIME;
                case PHASE_2_SOLAR_WRATH -> PHASE_2_FLIGHT_TIME;
                case PHASE_3_DIVINE_FURY -> PHASE_3_FLIGHT_TIME;
            };
        } else {
            // Reset ground timer without flying
            resetGroundTimer();
        }
    }

    @Override
    public void stop() {
        if (ra.isFlying()) {
            ra.land();
        }
        resetGroundTimer();
    }

    @Override
    public void tick() {
        // Decrement flight timer
        if (flightTimer > 0) {
            flightTimer--;
        }

        // While flying, look at and hover toward target
        if (ra.isFlying() && ra.getTarget() != null) {
            LivingEntity target = ra.getTarget();
            ra.getLookControl().lookAt(target, 30.0f, 30.0f);

            // Move horizontally toward target while maintaining height
            double dx = target.getX() - ra.getX();
            double dz = target.getZ() - ra.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);

            if (distance > 2.0) {
                // Normalize and apply hover speed
                double speed = 0.15;
                ra.setVelocity(
                        dx / distance * speed,
                        ra.getVelocity().y,
                        dz / distance * speed);
            }
        }
    }

    /**
     * Called from RaEntity.tick() to update ground combat timer.
     */
    public void tickGroundTimer() {
        if (!ra.isFlying() && !ra.isHibernating()) {
            if (groundCombatTimer > 0) {
                groundCombatTimer--;
            }
        }
    }

    /**
     * Reset ground combat timer based on current phase.
     */
    private void resetGroundTimer() {
        RaPhase phase = ra.getCurrentPhase();
        groundCombatTimer = switch (phase) {
            case PHASE_1_AWAKENED -> PHASE_1_GROUND_TIME;
            case PHASE_2_SOLAR_WRATH -> PHASE_2_GROUND_TIME;
            case PHASE_3_DIVINE_FURY -> PHASE_3_GROUND_TIME;
        };
    }

    /**
     * Force reset the ground timer (e.g., after landing from hibernation).
     */
    public void forceResetTimer() {
        resetGroundTimer();
        flightTimer = 0;
    }
}
