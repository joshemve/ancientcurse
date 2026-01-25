package com.ancientcurse.entity.ai;

import com.ancientcurse.entity.RaEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

/**
 * Ra Shard Attack Goal
 *
 * Phase 1: Buildup (30 ticks) - Shards materialize and orbit Ra
 * Phase 2: Fire (30 ticks) - Shards launch at the target sequentially
 *
 * Animation timing - MUST match ra.animation.json "ra.shard_attack"!
 * Source: src/main/resources/assets/ancientcurse/animations/ra.animation.json
 * Animation length: 3.0 seconds = 60 ticks
 */
public class RaShardAttackGoal extends Goal {
    private final RaEntity ra;
    private int attackTime = 0;
    private int cooldown = 0;

    private static final int BUILDUP_TICKS = 30;
    private static final int TOTAL_TICKS = 60; // 3.0s animation
    private static final int SHARD_COUNT = 5;

    // Cooldown between uses (in ticks)
    private static final int COOLDOWN_TICKS = 200; // 10 seconds

    public RaShardAttackGoal(RaEntity ra) {
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

        // Only use while on ground (as requested)
        if (this.ra.isFlying()) {
            return false;
        }

        // Medium range: 5 - 24 blocks
        double distSq = this.ra.squaredDistanceTo(target);
        return distSq >= 25.0 && distSq <= 576.0;
    }

    @Override
    public boolean shouldContinue() {
        return this.attackTime < TOTAL_TICKS && this.ra.getTarget() != null && this.ra.getTarget().isAlive();
    }

    @Override
    public void start() {
        this.attackTime = 0;
        this.ra.setCombatState(RaEntity.RaCombatState.SHARD_ATTACK);
        this.ra.setShardAttackTicks(TOTAL_TICKS);
    }

    @Override
    public void stop() {
        this.attackTime = 0;
        if (this.ra.getCombatState() == RaEntity.RaCombatState.SHARD_ATTACK) {
            this.ra.setCombatState(RaEntity.RaCombatState.IDLE);
        }
        this.ra.setShardAttackTicks(0);
        this.cooldown = COOLDOWN_TICKS;
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
        this.ra.setShardAttackTicks(TOTAL_TICKS - this.attackTime);

        LivingEntity target = this.ra.getTarget();
        if (target != null) {
            this.ra.getLookControl().lookAt(target, 30.0F, 30.0F);
        }

        // Phase 2: Firing sequence
        if (this.attackTime > BUILDUP_TICKS && this.attackTime % 6 == 0 && target != null) {
            fireShard(target);
        }
    }

    private void fireShard(LivingEntity target) {
        if (this.ra.getWorld().isClient)
            return;

        Vec3d startPos = this.ra.getPos().add(0, this.ra.getHeight() * 0.8, 0);
        Vec3d targetPos = target.getPos().add(0, target.getHeight() * 0.5, 0);

        com.ancientcurse.entity.SunShardProjectileEntity shard = new com.ancientcurse.entity.SunShardProjectileEntity(
                this.ra.getWorld(), this.ra);
        shard.setPosition(startPos.x, startPos.y, startPos.z);

        Vec3d velocity = targetPos.subtract(startPos).normalize().multiply(1.5);
        shard.setVelocity(velocity);

        this.ra.getWorld().spawnEntity(shard);

        // Play firing sound
        this.ra.playSound(net.minecraft.sound.SoundEvents.ENTITY_FIREWORK_ROCKET_SHOOT, 1.0f, 1.2f);
    }

    public void forceStart() {
        this.start();
    }
}
