package com.ancientcurse.entity.ai;

import com.ancientcurse.entity.RaEntity;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.List;

/**
 * Ra Sun Beam Attack Goal
 *
 * Ra channels the power of the sun through his staff, firing a devastating
 * continuous beam that sets targets ablaze. Similar to the Staff of Ra item.
 *
 * This attack is used when flying and creates a visually impressive beam
 * with fire effects and burning damage.
 *
 * Animation: Uses ra.flying_staff_attack (3.0s = 60 ticks)
 * The beam fires during the middle portion of the animation.
 */
public class RaSunBeamAttackGoal extends Goal {
    private final RaEntity ra;
    private int attackTime = 0;
    private int cooldown = 0;
    private LivingEntity lockedTarget = null;

    // Timing constants
    private static final int TOTAL_TICKS = 60; // 3.0s animation
    private static final int BEAM_START = 15; // Start firing at 0.75s
    private static final int BEAM_END = 50; // Stop firing at 2.5s
    private static final int COOLDOWN_TICKS = 240; // 12 seconds (longer cooldown - powerful attack)

    // Beam configuration
    private static final double BEAM_RANGE = 24.0;
    private static final float BEAM_WIDTH = 1.5f;

    // Damage configuration (scales with phase)
    private static final float PHASE_1_DPS = 3.0f; // Damage per second
    private static final float PHASE_2_DPS = 5.0f;
    private static final float PHASE_3_DPS = 8.0f;

    // Sun beam colors (matching Staff of Ra)
    private static final Vector3f CORE_COLOR = new Vector3f(1.0F, 1.0F, 0.9F); // Bright white-yellow
    private static final Vector3f INNER_COLOR = new Vector3f(1.0F, 0.95F, 0.7F); // Warm yellow
    private static final Vector3f OUTER_COLOR = new Vector3f(1.0F, 0.85F, 0.4F); // Golden orange

    public RaSunBeamAttackGoal(RaEntity ra) {
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

        // Sun Beam Attack is ONLY used while flying (it's a divine aerial attack)
        if (!this.ra.isFlying()) {
            return false;
        }

        // Long range: 8 - 24 blocks (further than flying staff attack)
        double distSq = this.ra.squaredDistanceTo(target);
        return distSq >= 64.0 && distSq <= 576.0;
    }

    @Override
    public boolean shouldContinue() {
        return this.attackTime < TOTAL_TICKS
                && this.ra.getTarget() != null
                && this.ra.getTarget().isAlive()
                && !this.ra.isHibernating();
    }

    @Override
    public void start() {
        this.attackTime = 0;
        this.lockedTarget = this.ra.getTarget();

        // Use the flying staff attack animation/state
        this.ra.triggerAction(RaEntity.RaCombatState.FLYING_STAFF_ATTACK, TOTAL_TICKS);

        // Activation sound - divine power charging
        this.ra.playSound(SoundEvents.BLOCK_BEACON_POWER_SELECT, 1.5f, 0.8f);

    }

    @Override
    public void stop() {
        this.attackTime = 0;
        this.lockedTarget = null;

        if (this.ra.getCombatState() == RaEntity.RaCombatState.FLYING_STAFF_ATTACK) {
            this.ra.setCombatState(RaEntity.RaCombatState.FLYING);
        }
        this.cooldown = COOLDOWN_TICKS;

        // Deactivation sound
        this.ra.playSound(SoundEvents.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.0f);
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

        // Track target while charging and firing
        LivingEntity target = this.lockedTarget != null ? this.lockedTarget : this.ra.getTarget();
        if (target != null && target.isAlive()) {
            this.ra.getLookControl().lookAt(target, 30.0F, 30.0F);

            // Update beam direction for rendering
            Vec3d startPos = getBeamStartPosition();
            Vec3d targetPos = target.getPos().add(0, target.getHeight() * 0.5, 0);
            Vec3d direction = targetPos.subtract(startPos).normalize();
            this.ra.setSunBeamDirection(direction);
        }

        // Fire the beam during the active window
        if (this.attackTime >= BEAM_START && this.attackTime <= BEAM_END) {
            fireBeam(target);
        }

        // Charging sounds
        if (this.attackTime < BEAM_START && this.attackTime % 5 == 0) {
            this.ra.playSound(SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.5f, 0.5f + (this.attackTime / (float) BEAM_START));
        }

        // Ambient beam sound
        if (this.attackTime >= BEAM_START && this.attackTime <= BEAM_END && this.attackTime % 10 == 0) {
            this.ra.playSound(SoundEvents.BLOCK_BEACON_AMBIENT, 0.6f, 1.2f);
        }
    }

    private Vec3d getBeamStartPosition() {
        // Start from Ra's staff position (approximated as chest height + forward offset)
        Vec3d pos = this.ra.getPos();
        float yaw = (float) Math.toRadians(-this.ra.getYaw());
        double forwardOffset = 1.5;
        return new Vec3d(
                pos.x + Math.sin(yaw) * forwardOffset,
                pos.y + this.ra.getHeight() * 0.7,
                pos.z + Math.cos(yaw) * forwardOffset
        );
    }

    private void fireBeam(LivingEntity target) {
        if (this.ra.getWorld().isClient || !(this.ra.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        Vec3d startPos = getBeamStartPosition();
        Vec3d direction;

        if (target != null && target.isAlive()) {
            Vec3d targetPos = target.getPos().add(0, target.getHeight() * 0.5, 0);
            direction = targetPos.subtract(startPos).normalize();
        } else {
            // Fallback to facing direction
            float yaw = (float) Math.toRadians(-this.ra.getYaw());
            float pitch = (float) Math.toRadians(-this.ra.getPitch());
            direction = new Vec3d(
                    Math.sin(yaw) * Math.cos(pitch),
                    Math.sin(pitch),
                    Math.cos(yaw) * Math.cos(pitch)
            ).normalize();
        }

        // Create visual beam particles
        createSunBeamParticles(serverWorld, startPos, direction);

        // Deal damage to entities in beam path
        damageEntitiesInBeam(serverWorld, startPos, direction);

        // Set fires along beam path (occasionally)
        if (this.attackTime % 10 == 0) {
            setFiresAlongBeam(serverWorld, startPos, direction);
        }
    }

    private void createSunBeamParticles(ServerWorld world, Vec3d start, Vec3d direction) {
        // Beam gets more intense during the attack
        float progress = (this.attackTime - BEAM_START) / (float) (BEAM_END - BEAM_START);
        float intensity = 0.5f + 0.5f * Math.min(1.0f, progress * 2); // Ramp up quickly

        // Main beam path
        for (double distance = 0; distance < BEAM_RANGE; distance += 0.4) {
            Vec3d pos = start.add(direction.multiply(distance));

            // Calculate beam width (slightly widens with distance)
            double beamWidth = 0.2 + (distance * 0.02);

            // Core beam - bright white center
            world.spawnParticles(
                    new DustParticleEffect(CORE_COLOR, 1.2f * intensity),
                    pos.x, pos.y, pos.z,
                    1, 0.05, 0.05, 0.05, 0
            );

            // Inner glow - warm yellow (spiral pattern)
            double innerAngle = (distance * 2.0 + this.attackTime * 0.3) % (Math.PI * 2);
            double innerOffset = beamWidth * 0.4;
            world.spawnParticles(
                    new DustParticleEffect(INNER_COLOR, 0.9f * intensity),
                    pos.x + Math.cos(innerAngle) * innerOffset,
                    pos.y + Math.sin(innerAngle) * innerOffset * 0.5,
                    pos.z + Math.sin(innerAngle) * innerOffset,
                    1, 0.02, 0.02, 0.02, 0
            );

            // Outer halo - golden particles
            if (distance % 0.8 < 0.4) {
                double outerAngle = (distance * 1.5 - this.attackTime * 0.2) % (Math.PI * 2);
                double outerOffset = beamWidth * 0.8;
                world.spawnParticles(
                        new DustParticleEffect(OUTER_COLOR, 0.7f * intensity),
                        pos.x + Math.cos(outerAngle) * outerOffset,
                        pos.y,
                        pos.z + Math.sin(outerAngle) * outerOffset,
                        1, 0.03, 0.03, 0.03, 0
                );
            }

            // Flame particles along beam
            if (world.getRandom().nextFloat() < 0.15f * intensity) {
                world.spawnParticles(
                        ParticleTypes.FLAME,
                        pos.x + (world.getRandom().nextDouble() - 0.5) * beamWidth,
                        pos.y + (world.getRandom().nextDouble() - 0.5) * beamWidth,
                        pos.z + (world.getRandom().nextDouble() - 0.5) * beamWidth,
                        1, 0, 0, 0, 0.02
                );
            }

            // Sparkles
            if (world.getRandom().nextFloat() < 0.08f * intensity) {
                world.spawnParticles(
                        ParticleTypes.END_ROD,
                        pos.x, pos.y, pos.z,
                        1, beamWidth * 0.5, beamWidth * 0.5, beamWidth * 0.5, 0.01
                );
            }
        }

        // Source glow at staff
        world.spawnParticles(
                ParticleTypes.FLAME,
                start.x, start.y, start.z,
                3, 0.2, 0.2, 0.2, 0.02
        );
        world.spawnParticles(
                new DustParticleEffect(INNER_COLOR, 2.0f),
                start.x, start.y, start.z,
                2, 0.1, 0.1, 0.1, 0
        );
    }

    private void damageEntitiesInBeam(ServerWorld world, Vec3d start, Vec3d direction) {
        // Get damage per tick based on phase
        float damagePerTick = switch (this.ra.getCurrentPhase()) {
            case PHASE_1_AWAKENED -> PHASE_1_DPS / 20.0f;
            case PHASE_2_SOLAR_WRATH -> PHASE_2_DPS / 20.0f;
            case PHASE_3_DIVINE_FURY -> PHASE_3_DPS / 20.0f;
        };

        // Check for entities along the beam path
        Vec3d endPos = start.add(direction.multiply(BEAM_RANGE));

        // Create a box that encompasses the beam
        Box beamBox = new Box(start, endPos).expand(BEAM_WIDTH);

        List<LivingEntity> entities = world.getEntitiesByClass(
                LivingEntity.class,
                beamBox,
                entity -> entity != this.ra && entity.isAlive() && !entity.isSpectator()
        );

        for (LivingEntity entity : entities) {
            // Check if entity is actually in the beam path
            Vec3d entityPos = entity.getPos().add(0, entity.getHeight() * 0.5, 0);
            Vec3d toEntity = entityPos.subtract(start);
            double projectionLength = toEntity.dotProduct(direction);

            if (projectionLength < 0 || projectionLength > BEAM_RANGE) {
                continue; // Entity is behind or beyond beam
            }

            Vec3d closestPointOnBeam = start.add(direction.multiply(projectionLength));
            double distanceToBeam = entityPos.distanceTo(closestPointOnBeam);

            if (distanceToBeam <= BEAM_WIDTH + entity.getWidth() * 0.5) {
                // Entity is in the beam! Deal damage
                entity.damage(world.getDamageSources().mobAttack(this.ra), damagePerTick);

                // Set on fire
                if (this.attackTime % 10 == 0) {
                    entity.setOnFireFor(3);
                }

                // Apply effects periodically
                if (this.attackTime % 20 == 0) {
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1));

                    // Bonus damage to undead (Ra's divine power)
                    if (entity.isUndead()) {
                        entity.damage(world.getDamageSources().mobAttack(this.ra), damagePerTick * 2);
                    }
                }

                // Impact particles on hit
                if (this.attackTime % 5 == 0) {
                    world.spawnParticles(
                            ParticleTypes.LAVA,
                            entityPos.x, entityPos.y, entityPos.z,
                            2, 0.3, 0.3, 0.3, 0
                    );
                }
            }
        }
    }

    private void setFiresAlongBeam(ServerWorld world, Vec3d start, Vec3d direction) {
        // Raycast to find where beam hits blocks
        Vec3d endPos = start.add(direction.multiply(BEAM_RANGE));

        BlockHitResult hitResult = world.raycast(new RaycastContext(
                start, endPos,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                this.ra
        ));

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos hitPos = hitResult.getBlockPos();
            BlockPos abovePos = hitPos.offset(hitResult.getSide());

            // Set fire at impact point
            if (world.getBlockState(abovePos).isAir() && world.getRandom().nextFloat() < 0.4f) {
                world.setBlockState(abovePos, Blocks.FIRE.getDefaultState());
            }

            // Spread fire around impact (small radius)
            int radius = switch (this.ra.getCurrentPhase()) {
                case PHASE_1_AWAKENED -> 1;
                case PHASE_2_SOLAR_WRATH -> 2;
                case PHASE_3_DIVINE_FURY -> 3;
            };

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + z * z <= radius * radius) {
                        BlockPos firePos = abovePos.add(x, 0, z);
                        if (world.getBlockState(firePos).isAir() && world.getRandom().nextFloat() < 0.2f) {
                            world.setBlockState(firePos, Blocks.FIRE.getDefaultState());
                        }
                    }
                }
            }

            // Impact effect
            world.spawnParticles(
                    ParticleTypes.FLAME,
                    hitResult.getPos().x, hitResult.getPos().y, hitResult.getPos().z,
                    10, 0.3, 0.3, 0.3, 0.05
            );
            world.spawnParticles(
                    ParticleTypes.LAVA,
                    hitResult.getPos().x, hitResult.getPos().y, hitResult.getPos().z,
                    3, 0.2, 0.2, 0.2, 0
            );
        }
    }

    public void forceStart() {
        this.cooldown = 0;
        this.start();
    }
}
