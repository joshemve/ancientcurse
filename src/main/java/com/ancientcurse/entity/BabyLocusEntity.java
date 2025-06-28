package com.ancientcurse.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class BabyLocusEntity extends PathAwareEntity implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    private LocusEntity parent;
    private int followTimer = 0;
    private boolean isFlying = true;

    public BabyLocusEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(true);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new BabyLocusFollowParentGoal(this));
        this.goalSelector.add(1, new BabyLocusFlyGoal(this));
        this.goalSelector.add(2, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(3, new LookAroundGoal(this));
        
        // Babies don't attack players
        // this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    public static DefaultAttributeContainer.Builder createBabyLocusAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0D) // Half health
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.5D) // Slightly faster
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0D) // Much less damage
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0D)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.7D);
    }

    @Override
    public void tick() {
        super.tick();
        
        // Update follow behavior
        this.updateFollowBehavior();
        
        // Update flight height
        this.updateFlightHeight();
    }

    private void updateFollowBehavior() {
        if (this.parent != null && this.parent.isAlive()) {
            this.followTimer++;
            if (this.followTimer > 20) {
                Vec3d parentPos = this.parent.getPos();
                double distance = this.getPos().distanceTo(parentPos);
                
                if (distance > 8) {
                    // Move towards parent
                    Vec3d direction = parentPos.subtract(this.getPos()).normalize();
                    this.setVelocity(this.getVelocity().add(direction.multiply(0.05)));
                }
                
                this.followTimer = 0;
            }
        }
    }

    private void updateFlightHeight() {
        if (this.isFlying && this.parent != null) {
            // Stay near parent's height
            double parentHeight = this.parent.getY();
            double targetHeight = parentHeight - 1; // Slightly below parent
            
            Vec3d pos = this.getPos();
            if (Math.abs(pos.y - targetHeight) > 0.5) {
                double heightDiff = targetHeight - pos.y;
                this.setPosition(pos.x, pos.y + heightDiff * 0.1, pos.z);
            }
        }
    }

    public void setParent(LocusEntity parent) {
        this.parent = parent;
    }

    public LocusEntity getParent() {
        return this.parent;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, this::predicate));
    }

    private <T extends GeoEntity> PlayState predicate(AnimationState<T> tAnimationState) {
        if (tAnimationState.isMoving()) {
            tAnimationState.getController().setAnimation(RawAnimation.begin().then("animation.locus.fly", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }
        
        // Default to fly animation when idle (babies are always flying)
        tAnimationState.getController().setAnimation(RawAnimation.begin().then("animation.locus.fly", Animation.LoopType.LOOP));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // Custom AI Goals for Baby Locus
    private static class BabyLocusFollowParentGoal extends Goal {
        private final BabyLocusEntity baby;

        public BabyLocusFollowParentGoal(BabyLocusEntity baby) {
            this.baby = baby;
        }

        @Override
        public boolean canStart() {
            return this.baby.parent != null && this.baby.parent.isAlive();
        }

        @Override
        public void tick() {
            if (this.baby.parent != null) {
                Vec3d parentPos = this.baby.parent.getPos();
                double distance = this.baby.getPos().distanceTo(parentPos);
                
                if (distance > 3) {
                    Vec3d direction = parentPos.subtract(this.baby.getPos()).normalize();
                    this.baby.getMoveControl().moveTo(parentPos.x, parentPos.y, parentPos.z, 0.6);
                }
            }
        }
    }

    private static class BabyLocusFlyGoal extends Goal {
        private final BabyLocusEntity baby;

        public BabyLocusFlyGoal(BabyLocusEntity baby) {
            this.baby = baby;
        }

        @Override
        public boolean canStart() {
            return this.baby.isFlying;
        }

        @Override
        public void tick() {
            // Simple flying movement for babies
            if (this.baby.parent == null) {
                // If no parent, just hover gently
                Vec3d pos = this.baby.getPos();
                double hoverOffset = Math.sin(this.baby.age * 0.2) * 0.05;
                this.baby.setPosition(pos.x, pos.y + hoverOffset, pos.z);
            }
        }
    }
} 