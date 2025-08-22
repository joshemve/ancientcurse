package com.ancientcurse.entity.ai;

import com.ancientcurse.block.SolarSpireBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.EnumSet;

/**
 * AI Goal that makes cursed entities attack active Solar Spires
 */
public class AttackSolarSpireGoal extends Goal {
    private final MobEntity mob;
    private final double speed;
    private final int searchRange;
    private final int attackDamage;
    private BlockPos targetSpirePos;
    private int attackCooldown;
    private int pathRecalculateTimer;
    
    public AttackSolarSpireGoal(MobEntity mob, double speed, int searchRange, int attackDamage) {
        this.mob = mob;
        this.speed = speed;
        this.searchRange = searchRange;
        this.attackDamage = attackDamage;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }
    
    @Override
    public boolean canStart() {
        // Don't start if mob has a player target
        if (mob.getTarget() != null && mob.getTarget() instanceof net.minecraft.entity.player.PlayerEntity) {
            return false;
        }
        
        // Look for nearby active Solar Spire
        targetSpirePos = SolarSpireBlock.getNearestActiveSpire(mob.getWorld(), mob.getBlockPos(), searchRange);
        return targetSpirePos != null;
    }
    
    @Override
    public boolean shouldContinue() {
        // Stop if we have a player target now
        if (mob.getTarget() != null && mob.getTarget() instanceof net.minecraft.entity.player.PlayerEntity) {
            return false;
        }
        
        // Continue if spire still exists and is active
        if (targetSpirePos == null) {
            return false;
        }
        
        World world = mob.getWorld();
        return world.getBlockState(targetSpirePos).getBlock() instanceof SolarSpireBlock &&
               world.getBlockState(targetSpirePos).get(SolarSpireBlock.ACTIVATED);
    }
    
    @Override
    public void start() {
        this.pathRecalculateTimer = 0;
        this.attackCooldown = 0;
    }
    
    @Override
    public void stop() {
        this.targetSpirePos = null;
        this.mob.getNavigation().stop();
    }
    
    @Override
    public void tick() {
        if (targetSpirePos == null) {
            return;
        }
        
        // Look at the spire
        mob.getLookControl().lookAt(
            targetSpirePos.getX() + 0.5,
            targetSpirePos.getY() + 1.0,
            targetSpirePos.getZ() + 0.5
        );
        
        double distanceSquared = mob.squaredDistanceTo(
            targetSpirePos.getX() + 0.5,
            targetSpirePos.getY(),
            targetSpirePos.getZ() + 0.5
        );
        
        // Attack if within range (3 blocks)
        if (distanceSquared < 9.0) {
            if (attackCooldown <= 0) {
                // Damage the spire
                SolarSpireBlock.damageSpire(mob.getWorld(), targetSpirePos, attackDamage);
                
                // Play attack animation
                mob.swingHand(mob.getActiveHand());
                
                // Reset cooldown (20 ticks = 1 second)
                attackCooldown = 20;
            }
        } else {
            // Move towards the spire
            if (--pathRecalculateTimer <= 0) {
                pathRecalculateTimer = 10;
                mob.getNavigation().startMovingTo(
                    targetSpirePos.getX() + 0.5,
                    targetSpirePos.getY(),
                    targetSpirePos.getZ() + 0.5,
                    speed
                );
            }
        }
        
        // Decrease attack cooldown
        if (attackCooldown > 0) {
            attackCooldown--;
        }
    }
}