package com.ancientcurse.entity;

import com.ancientcurse.ModEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Random;

public class LotusEntity extends PathAwareEntity implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    // Flying and AI properties
    private int flightHeight = 0;
    private boolean isFlying = true;
    private int babySpawnTimer = 0;
    private boolean hasSpawnedBabies = false;
    private Vec3d lastTargetPos = null;
    private int hoverTimer = 0;
    
    // Ancient Egypt themed properties
    private boolean isGuardian = false;
    private BlockPos sacredArea = null;
    private int sunWorshipTimer = 0;

    public LotusEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(true); // Enable flying
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new LotusFlyGoal(this));
        this.goalSelector.add(1, new LotusHoverGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.add(3, new LotusWanderGoal(this, 0.8D));
        this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(5, new LookAroundGoal(this));
        this.goalSelector.add(6, new LotusSunWorshipGoal(this));
        this.goalSelector.add(7, new LotusGuardianGoal(this));
        
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    public static DefaultAttributeContainer.Builder createLotusAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.4D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0D)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.6D);
    }

    @Override
    public void tick() {
        super.tick();
        
        // Handle baby spawning
        if (!this.hasSpawnedBabies && this.babySpawnTimer++ > 60) {
            this.trySpawnBabies();
        }
        
        // Update flight height
        this.updateFlightHeight();
        
        // Sun worship behavior
        this.updateSunWorship();
        
        // Guardian behavior
        this.updateGuardianBehavior();
    }

    private void trySpawnBabies() {
        if (this.hasSpawnedBabies || this.getWorld().isClient) return;
        
        net.minecraft.util.math.random.Random random = this.getRandom();
        if (random.nextFloat() < 0.2f) { // 20% chance
            int babyCount = random.nextInt(3) + 1; // 1-3 babies
            
            for (int i = 0; i < babyCount; i++) {
                BabyLotusEntity baby = ModEntities.BABY_LOTUS.create(this.getWorld());
                if (baby != null) {
                    baby.setPosition(this.getPos());
                    baby.setParent(this);
                    this.getWorld().spawnEntity(baby);
                }
            }
            this.hasSpawnedBabies = true;
        }
    }

    private void updateFlightHeight() {
        if (this.isFlying) {
            // Gradually adjust flight height
            int targetHeight = this.getTargetHeight();
            if (this.flightHeight < targetHeight) {
                this.flightHeight++;
            } else if (this.flightHeight > targetHeight) {
                this.flightHeight--;
            }
            
            // Apply height to position
            Vec3d pos = this.getPos();
            this.setPosition(pos.x, pos.y + (this.flightHeight - pos.y) * 0.1, pos.z);
        }
    }

    private int getTargetHeight() {
        // Vary height based on time and environment
        int baseHeight = 3;
        if (this.getWorld().isDay()) {
            baseHeight += 2; // Fly higher during day
        }
        if (this.isNearWater()) {
            baseHeight += 1; // Fly higher near water
        }
        return baseHeight + this.getRandom().nextInt(4);
    }

    private boolean isNearWater() {
        BlockPos pos = this.getBlockPos();
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                BlockPos checkPos = pos.add(x, 0, z);
                if (this.getWorld().getBlockState(checkPos).getFluidState().isStill()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void updateSunWorship() {
        if (this.getWorld().isDay()) {
            this.sunWorshipTimer++;
            if (this.sunWorshipTimer > 200) {
                // Move towards sunlight (simplified)
                Vec3d sunDirection = new Vec3d(0, 1, 0); // Simplified sun direction
                this.setVelocity(this.getVelocity().add(sunDirection.multiply(0.01)));
                this.sunWorshipTimer = 0;
            }
        }
    }

    private void updateGuardianBehavior() {
        if (this.isGuardian && this.sacredArea != null) {
            // Stay near sacred area
            double distance = this.getPos().distanceTo(Vec3d.ofCenter(this.sacredArea));
            if (distance > 20) {
                Vec3d direction = Vec3d.ofCenter(this.sacredArea).subtract(this.getPos()).normalize();
                this.setVelocity(this.getVelocity().add(direction.multiply(0.02)));
            }
        }
    }

    public void setGuardian(boolean guardian, BlockPos sacredArea) {
        this.isGuardian = guardian;
        this.sacredArea = sacredArea;
    }

    public boolean isFlying() {
        return this.isFlying;
    }

    public void setFlying(boolean flying) {
        this.isFlying = flying;
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
        
        if (this.isAttacking()) {
            tAnimationState.getController().setAnimation(RawAnimation.begin().then("animation.locus.attack", Animation.LoopType.PLAY_ONCE));
            return PlayState.CONTINUE;
        }
        
        // Default to fly animation when idle
        tAnimationState.getController().setAnimation(RawAnimation.begin().then("animation.locus.fly", Animation.LoopType.LOOP));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // Custom AI Goals
    private static class LotusFlyGoal extends Goal {
        private final LotusEntity lotus;
        private int cooldown = 0;

        public LotusFlyGoal(LotusEntity lotus) {
            this.lotus = lotus;
        }

        @Override
        public boolean canStart() {
            return this.lotus.isFlying() && this.cooldown-- <= 0;
        }

        @Override
        public void tick() {
            // Smooth flying movement
            Vec3d velocity = this.lotus.getVelocity();
            // Use a simple forward movement instead of movement direction
            Vec3d targetVelocity = this.lotus.getRotationVec(1.0f).multiply(0.3);
            this.lotus.setVelocity(velocity.lerp(targetVelocity, 0.1));
            
            this.cooldown = 20;
        }
    }

    private static class LotusHoverGoal extends Goal {
        private final LotusEntity lotus;
        private int hoverTime = 0;

        public LotusHoverGoal(LotusEntity lotus) {
            this.lotus = lotus;
        }

        @Override
        public boolean canStart() {
            return this.lotus.isFlying() && this.lotus.getRandom().nextFloat() < 0.1f;
        }

        @Override
        public void tick() {
            this.hoverTime++;
            if (this.hoverTime > 60) {
                this.stop();
            }
            
            // Gentle hovering movement
            Vec3d pos = this.lotus.getPos();
            double hoverOffset = Math.sin(this.hoverTime * 0.1) * 0.1;
            this.lotus.setPosition(pos.x, pos.y + hoverOffset, pos.z);
        }
    }

    private static class LotusWanderGoal extends WanderAroundFarGoal {
        private final LotusEntity lotus;

        public LotusWanderGoal(LotusEntity lotus, double speed) {
            super(lotus, speed);
            this.lotus = lotus;
        }

        @Override
        protected Vec3d getWanderTarget() {
            // Prefer flying to higher areas
            Vec3d baseTarget = super.getWanderTarget();
            if (baseTarget != null) {
                return new Vec3d(baseTarget.x, baseTarget.y + 3, baseTarget.z);
            }
            return baseTarget;
        }
    }

    private static class LotusSunWorshipGoal extends Goal {
        private final LotusEntity lotus;

        public LotusSunWorshipGoal(LotusEntity lotus) {
            this.lotus = lotus;
        }

        @Override
        public boolean canStart() {
            return this.lotus.getWorld().isDay() && this.lotus.getRandom().nextFloat() < 0.05f;
        }

        @Override
        public void tick() {
            // Move towards the sun (simplified)
            Vec3d sunPos = new Vec3d(0, 100, 0); // Simplified sun position
            Vec3d direction = sunPos.subtract(this.lotus.getPos()).normalize();
            this.lotus.setVelocity(this.lotus.getVelocity().add(direction.multiply(0.02)));
        }
    }

    private static class LotusGuardianGoal extends Goal {
        private final LotusEntity lotus;

        public LotusGuardianGoal(LotusEntity lotus) {
            this.lotus = lotus;
        }

        @Override
        public boolean canStart() {
            return this.lotus.isGuardian && this.lotus.sacredArea != null;
        }

        @Override
        public void tick() {
            // Patrol around sacred area
            if (this.lotus.getRandom().nextFloat() < 0.02f) {
                Vec3d center = Vec3d.ofCenter(this.lotus.sacredArea);
                double angle = this.lotus.getRandom().nextDouble() * Math.PI * 2;
                double radius = 10 + this.lotus.getRandom().nextDouble() * 10;
                Vec3d patrolPos = center.add(Math.cos(angle) * radius, 3, Math.sin(angle) * radius);
                this.lotus.getMoveControl().moveTo(patrolPos.x, patrolPos.y, patrolPos.z, 0.3);
            }
        }
    }
} 