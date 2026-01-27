package com.ancientcurse.entity.ai;

import com.ancientcurse.entity.RaEntity;
import com.ancientcurse.network.CurseZonePackets;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.EnumSet;

/**
 * Ra Flying Staff Attack Goal
 *
 * The airborne equivalent of the Shard Attack - used when Ra is flying.
 * Ra hovers and fires a powerful staff beam at the target.
 *
 * This attack uses the EXACT same visual effects as the Staff of Ra item
 * that players can use, so when they defeat Ra and earn the staff,
 * they'll recognize they're wielding the same divine power.
 *
 * Animation timing - MUST match ra.animation.json "ra.flying_staff_attack"!
 * Source: src/main/resources/assets/ancientcurse/animations/ra.animation.json
 * Animation length: 3.0 seconds = 60 ticks
 */
public class RaFlyingStaffAttackGoal extends Goal {
    private final RaEntity ra;
    private int attackTime = 0;
    private int cooldown = 0;
    private int loopCount = 1;     // Random 1-3 loops per attack
    private int totalTicks = 60;   // Total duration based on loop count

    private static final int SINGLE_LOOP_TICKS = 60; // 3.0s per animation loop

    // Beam phases within each loop (matching animation timing)
    // Visual beam handled by FlyingStaffBeamLayer (client-side) from tick 20-45
    private static final int CHARGE_START = 0;
    private static final int BEAM_START = 20;    // Start firing beam (matches client layer)
    private static final int BEAM_END = 45;      // Stop firing beam (matches client layer)
    private static final int DAMAGE_TICK = 22;   // Deal damage early in beam phase

    // Cooldown between uses (in ticks)
    private static final int COOLDOWN_TICKS = 160; // 8 seconds

    // Beam configuration - matches StaffOfRaItem exactly
    private static final double BEAM_RANGE = 32.0D;

    // Sun beam colors - EXACT same as StaffOfRaItem
    private static final Vector3f CORE_COLOR = new Vector3f(1.0F, 1.0F, 0.9F);   // Bright white-yellow
    private static final Vector3f INNER_COLOR = new Vector3f(1.0F, 0.95F, 0.7F); // Warm yellow
    private static final Vector3f OUTER_COLOR = new Vector3f(1.0F, 0.85F, 0.4F); // Golden orange

    public RaFlyingStaffAttackGoal(RaEntity ra) {
        this.ra = ra;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        // Must be off cooldown
        if (cooldown > 0) {
            return false;
        }

        LivingEntity target = this.ra.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }

        // Don't start if already performing an action
        if (this.ra.isPerformingAction() || this.ra.isHibernating()) {
            return false;
        }

        // Flying Staff Attack is ONLY used while flying
        if (!this.ra.isFlying()) {
            return false;
        }

        // Medium to long range: 5 - 30 blocks (wider range than ground shard attack)
        double distSq = this.ra.squaredDistanceTo(target);
        return distSq >= 25.0 && distSq <= 900.0;
    }

    @Override
    public boolean shouldContinue() {
        return this.attackTime < this.totalTicks && this.ra.getTarget() != null && this.ra.getTarget().isAlive();
    }

    @Override
    public void start() {
        this.attackTime = 0;

        // Randomly select 1-3 loops for this attack
        this.loopCount = 1 + this.ra.getRandom().nextInt(3); // 1, 2, or 3
        this.totalTicks = SINGLE_LOOP_TICKS * this.loopCount;

        // Sync loop count to client for beam rendering
        this.ra.setFlyingStaffLoopCount(this.loopCount);

        // Use triggerAction to properly set both combatState AND actionAnimationTicks
        this.ra.triggerAction(RaEntity.RaCombatState.FLYING_STAFF_ATTACK, this.totalTicks);

        // Initial power-up sound
        if (!this.ra.getWorld().isClient) {
            this.ra.getWorld().playSound(null, this.ra.getX(), this.ra.getY(), this.ra.getZ(),
                SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 1.5F, 1.5F);
        }
    }

    @Override
    public void stop() {
        this.attackTime = 0;
        if (this.ra.getCombatState() == RaEntity.RaCombatState.FLYING_STAFF_ATTACK) {
            this.ra.setCombatState(RaEntity.RaCombatState.FLYING);
        }
        // Explicitly reset action ticks to stop beam particles immediately
        // This ensures the client-side beam layer stops rendering even with DataTracker sync delay
        this.ra.setActionTicks(0);
        // Clear beam direction to prevent stale direction being used
        this.ra.setSunBeamDirection(Vec3d.ZERO);
        this.cooldown = COOLDOWN_TICKS;

        // Deactivation sound
        if (!this.ra.getWorld().isClient) {
            this.ra.getWorld().playSound(null, this.ra.getX(), this.ra.getY(), this.ra.getZ(),
                SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.HOSTILE, 0.8F, 1.0F);
        }
    }

    /**
     * Called from RaEntity.tick() to update cooldown.
     */
    public void tickCooldown() {
        if (cooldown > 0) {
            cooldown--;
        }
    }

    @Override
    public void tick() {
        this.attackTime++;

        LivingEntity target = this.ra.getTarget();
        if (target == null) return;

        // Track target throughout
        this.ra.getLookControl().lookAt(target, 30.0F, 30.0F);

        // Calculate beam vectors
        Vec3d startPos = getStaffTipPosition();
        Vec3d targetPos = target.getPos().add(0, target.getHeight() * 0.5, 0);
        Vec3d direction = targetPos.subtract(startPos).normalize();
        this.ra.setSunBeamDirection(direction);

        if (this.ra.getWorld().isClient) return;
        ServerWorld world = (ServerWorld) this.ra.getWorld();

        // Calculate position within the current loop (0-59 for each loop)
        int localTick = this.attackTime % SINGLE_LOOP_TICKS;
        // Handle edge case where attackTime is exactly on loop boundary
        if (localTick == 0 && this.attackTime > 0) {
            localTick = SINGLE_LOOP_TICKS; // Last tick of previous loop
        }

        // Charging phase - building energy particles around Ra (only first loop)
        if (this.attackTime >= CHARGE_START && this.attackTime < BEAM_START) {
            float chargeProgress = (float)(this.attackTime - CHARGE_START) / (BEAM_START - CHARGE_START);
            spawnChargingParticles(world, startPos, chargeProgress);

            // Charging sounds
            if (this.attackTime % 5 == 0) {
                world.playSound(null, this.ra.getX(), this.ra.getY(), this.ra.getZ(),
                    SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.HOSTILE, 0.5F, 0.5F + chargeProgress);
            }
        }

        // Beam firing phase - check if we're in the beam window of ANY loop
        // Visual particles handled by FlyingStaffBeamLayer (client-side)
        // Server only handles sounds during beam phase
        if (localTick >= BEAM_START && localTick <= BEAM_END) {
            // Continuous beam sound
            if (this.attackTime % 10 == 0) {
                world.playSound(null, this.ra.getX(), this.ra.getY(), this.ra.getZ(),
                    SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.HOSTILE, 0.6F, 1.2F);
            }
        }

        // Deal damage at DAMAGE_TICK of each loop iteration
        if (localTick == DAMAGE_TICK) {
            fireStaffBeam(target, startPos, direction);
        }

        // Continuous damage while beam is active (every 10 ticks after initial hit within each loop)
        if (localTick > DAMAGE_TICK && localTick <= BEAM_END && this.attackTime % 10 == 0) {
            // Check if target is still in beam path
            double distToTarget = startPos.distanceTo(target.getPos().add(0, target.getHeight() * 0.5, 0));
            if (distToTarget <= BEAM_RANGE) {
                applyBeamDamage(target, 0.3F); // Reduced continuous damage
            }
        }
    }

    /**
     * Get the position of Ra's staff tip (where the beam originates)
     */
    private Vec3d getStaffTipPosition() {
        // Staff is held in right hand, beam comes from above Ra's position
        double yOffset = this.ra.getHeight() * 0.85;

        // Offset slightly forward based on Ra's facing direction
        float yaw = this.ra.getYaw();
        double xOffset = -Math.sin(Math.toRadians(yaw)) * 0.5;
        double zOffset = Math.cos(Math.toRadians(yaw)) * 0.5;

        return this.ra.getPos().add(xOffset, yOffset, zOffset);
    }

    /**
     * Spawn charging particles around the staff tip
     */
    private void spawnChargingParticles(ServerWorld world, Vec3d staffPos, float progress) {
        // Orbiting energy particles that converge as charge builds
        int particleCount = (int)(3 + 5 * progress);
        double radius = 1.5 - (progress * 1.0); // Shrinks as it charges

        for (int i = 0; i < particleCount; i++) {
            double angle = (Math.PI * 2) * i / particleCount + (this.ra.age * 0.2);
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double offsetY = Math.sin(angle * 2 + this.ra.age * 0.1) * 0.3;

            world.spawnParticles(
                new DustParticleEffect(INNER_COLOR, 1.0F + progress),
                staffPos.x + offsetX, staffPos.y + offsetY, staffPos.z + offsetZ,
                1, 0, 0, 0, 0
            );
        }

        // Central glow intensifies
        if (progress > 0.5F) {
            world.spawnParticles(
                new DustParticleEffect(CORE_COLOR, 1.5F * progress),
                staffPos.x, staffPos.y, staffPos.z,
                2, 0.1, 0.1, 0.1, 0
            );
        }
    }

    // NOTE: Sun beam visual particles are now rendered client-side by FlyingStaffBeamLayer
    // This allows the beam to originate from the actual sun_orb bone position

    private void fireStaffBeam(LivingEntity target, Vec3d startPos, Vec3d direction) {
        if (this.ra.getWorld().isClient) return;
        ServerWorld world = (ServerWorld) this.ra.getWorld();

        // Deal damage to target - scales with phase like original
        float damage = switch (this.ra.getCurrentPhase()) {
            case PHASE_1_AWAKENED -> 8.0f;
            case PHASE_2_SOLAR_WRATH -> 12.0f;
            case PHASE_3_DIVINE_FURY -> 16.0f;
        };

        target.damage(this.ra.getWorld().getDamageSources().mobAttack(this.ra), damage);

        // Apply status effects - same as StaffOfRaItem
        target.setOnFireFor(6); // Set on fire
        // Sun flash effect - blinding light of the sun god (replaces blindness)
        CurseZonePackets.sendSunFlash(world, target.getBlockPos(), 1.0f, 60, 32.0);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 1)); // 4 seconds slowness

        // Bonus damage to undead (just like the player's staff)
        if (target.isUndead()) {
            target.damage(this.ra.getWorld().getDamageSources().mobAttack(this.ra), damage * 0.5F);
        }

        // Create impact effect at target - matches StaffOfRaItem.createImpactEffect()
        Vec3d impactPos = target.getPos().add(0, target.getHeight() * 0.5, 0);
        createImpactEffect(world, impactPos);

        // Play firing sounds
        this.ra.playSound(SoundEvents.ENTITY_BLAZE_SHOOT, 1.5f, 0.8f);
        this.ra.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
    }

    /**
     * Apply reduced damage during continuous beam contact
     */
    private void applyBeamDamage(LivingEntity target, float damageMultiplier) {
        float baseDamage = switch (this.ra.getCurrentPhase()) {
            case PHASE_1_AWAKENED -> 8.0f;
            case PHASE_2_SOLAR_WRATH -> 12.0f;
            case PHASE_3_DIVINE_FURY -> 16.0f;
        };

        target.damage(this.ra.getWorld().getDamageSources().mobAttack(this.ra), baseDamage * damageMultiplier);
        target.setOnFireFor(2); // Keep them burning
    }

    /**
     * Create impact burst effect at hit location - matches StaffOfRaItem exactly
     */
    private void createImpactEffect(ServerWorld world, Vec3d pos) {
        int particleCount = 25;

        // Central flash
        world.spawnParticles(
            ParticleTypes.INSTANT_EFFECT,
            pos.x, pos.y, pos.z,
            5, 0.1, 0.1, 0.1, 0
        );

        // Radial burst
        for (int i = 0; i < particleCount; i++) {
            double angle = (Math.PI * 2) * i / particleCount;
            double speed = 0.4D;

            world.spawnParticles(
                new DustParticleEffect(OUTER_COLOR, 1.5F),
                pos.x, pos.y, pos.z,
                0,
                Math.cos(angle) * speed,
                0.1,
                Math.sin(angle) * speed,
                1.0
            );
        }

        // Upward burst
        world.spawnParticles(
            ParticleTypes.END_ROD,
            pos.x, pos.y, pos.z,
            8, 0.2, 0.3, 0.2, 0.05
        );

        // Impact sound
        world.playSound(null, pos.x, pos.y, pos.z,
            SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.HOSTILE, 1.0F, 1.2F);
    }

    public void forceStart() {
        this.cooldown = 0;
        this.start();
    }
}
