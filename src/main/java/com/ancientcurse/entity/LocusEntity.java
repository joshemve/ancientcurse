package com.ancientcurse.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.FlyingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import java.util.EnumSet;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class LocusEntity extends HostileEntity implements GeoEntity {
    private static final TrackedData<Boolean> ATTACKING = DataTracker.registerData(LocusEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> ATTACK_COOLDOWN = DataTracker.registerData(LocusEntity.class, TrackedDataHandlerRegistry.INTEGER);
    
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final int MAX_ATTACK_COOLDOWN = 20;
    private float targetAltitude;
    private int stuckCounter = 0;
    private Vec3d lastPosition = Vec3d.ZERO;

    public LocusEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(true);
        this.moveControl = new FlightMoveControl(this, 20, true);
        this.experiencePoints = 10;
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(ATTACKING, false);
        this.dataTracker.startTracking(ATTACK_COOLDOWN, 0);
    }

    @Override
    protected void initGoals() {
        // Priority goals
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new LocusMeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.add(2, new LocusFlyToTargetGoal(this, 1.0D));
        this.goalSelector.add(3, new LocusWanderGoal(this, 0.8D));
        this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 16.0F));
        this.goalSelector.add(5, new LookAroundGoal(this));
        
        // Target selectors
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    public static DefaultAttributeContainer.Builder createLocusAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.45D)
                .add(EntityAttributes.GENERIC_ARMOR, 2.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.5D);
    }

    @Override
    protected EntityNavigation createNavigation(World world) {
        BirdNavigation navigation = new BirdNavigation(this, world);
        navigation.setCanPathThroughDoors(false);
        navigation.setCanSwim(false);
        navigation.setCanEnterOpenDoors(true);
        return navigation;
    }

    @Override
    public void tick() {
        super.tick();
        
        // Update attack cooldown
        int cooldown = this.dataTracker.get(ATTACK_COOLDOWN);
        if (cooldown > 0) {
            this.dataTracker.set(ATTACK_COOLDOWN, cooldown - 1);
        }
        
        // Anti-stuck mechanism
        if (!this.getWorld().isClient()) {
            if (this.age % 20 == 0) {
                if (this.getPos().distanceTo(lastPosition) < 0.5) {
                    stuckCounter++;
                    if (stuckCounter > 3) {
                        this.addVelocity(
                            (random.nextDouble() - 0.5) * 0.2,
                            (random.nextDouble() - 0.5) * 0.2,
                            (random.nextDouble() - 0.5) * 0.2
                        );
                        stuckCounter = 0;
                    }
                } else {
                    stuckCounter = 0;
                }
                lastPosition = this.getPos();
            }
        }
        
        // Maintain flight height
        if (!this.isOnGround() && this.getVelocity().y < 0 && this.getBlockY() < targetAltitude - 2) {
            this.setVelocity(this.getVelocity().add(0, 0.05, 0));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, this::predicate));
    }

    private <T extends GeoEntity> PlayState predicate(AnimationState<T> state) {
        if (this.isDead()) {
            state.getController().setAnimation(RawAnimation.begin()
                .then("animation.locus.death", Animation.LoopType.HOLD_ON_LAST_FRAME));
            return PlayState.CONTINUE;
        }
        
        if (this.dataTracker.get(ATTACKING)) {
            state.getController().setAnimation(RawAnimation.begin()
                .then("animation.locus.attack", Animation.LoopType.PLAY_ONCE));
            return PlayState.CONTINUE;
        }
        
        if (state.isMoving() || !this.isOnGround()) {
            state.getController().setAnimation(RawAnimation.begin()
                .then("animation.locus.fly", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }
        
        // Idle animation
        state.getController().setAnimation(RawAnimation.begin()
            .then("animation.locus.idle", Animation.LoopType.LOOP));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public boolean tryAttack(LivingEntity target) {
        if (this.dataTracker.get(ATTACK_COOLDOWN) == 0) {
            this.dataTracker.set(ATTACKING, true);
            this.dataTracker.set(ATTACK_COOLDOWN, MAX_ATTACK_COOLDOWN);
            boolean result = super.tryAttack(target);
            
            // Schedule animation reset
            if (!this.getWorld().isClient()) {
                this.getWorld().getServer().execute(() -> {
                    try {
                        Thread.sleep(500); // Attack animation duration
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                    this.dataTracker.set(ATTACKING, false);
                });
            }
            
            return result;
        }
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_BEE_LOOP_AGGRESSIVE; // Replace with custom sound
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_BEE_HURT; // Replace with custom sound
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_BEE_DEATH; // Replace with custom sound
    }

    @Override
    protected float getSoundVolume() {
        return 0.7F;
    }

    @Override
    public boolean canSpawn(net.minecraft.world.WorldView world) {
        BlockPos pos = this.getBlockPos();
        return world.isAir(pos) && world.isAir(pos.up()) && 
               this.getWorld().getBaseLightLevel(pos, 0) <= 7;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceToClosestPlayer) {
        return distanceToClosestPlayer > 128.0D;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putFloat("TargetAltitude", this.targetAltitude);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.targetAltitude = nbt.getFloat("TargetAltitude");
    }

    // Custom goals
    static class LocusMeleeAttackGoal extends MeleeAttackGoal {
        private final LocusEntity locus;

        public LocusMeleeAttackGoal(LocusEntity locus, double speed, boolean pauseWhenMobIdle) {
            super(locus, speed, pauseWhenMobIdle);
            this.locus = locus;
        }

        @Override
        public boolean canStart() {
            return super.canStart() && this.locus.dataTracker.get(ATTACK_COOLDOWN) == 0;
        }

        @Override
        protected double getSquaredMaxAttackDistance(LivingEntity entity) {
            return 4.0D + entity.getWidth();
        }
    }

    static class LocusFlyToTargetGoal extends Goal {
        private final LocusEntity locus;
        private final double speed;

        public LocusFlyToTargetGoal(LocusEntity locus, double speed) {
            this.locus = locus;
            this.speed = speed;
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            return this.locus.getTarget() != null && !this.locus.getMoveControl().isMoving();
        }

        @Override
        public boolean shouldContinue() {
            return this.locus.getTarget() != null && this.locus.getTarget().isAlive();
        }

        @Override
        public void start() {
            LivingEntity target = this.locus.getTarget();
            if (target != null) {
                Vec3d targetPos = target.getEyePos();
                this.locus.getMoveControl().moveTo(targetPos.x, targetPos.y, targetPos.z, this.speed);
            }
        }

        @Override
        public void tick() {
            LivingEntity target = this.locus.getTarget();
            if (target != null) {
                double distance = this.locus.squaredDistanceTo(target);
                if (distance > 100) { // If far away, update path
                    Vec3d targetPos = target.getEyePos();
                    this.locus.getMoveControl().moveTo(targetPos.x, targetPos.y, targetPos.z, this.speed);
                }
            }
        }
    }

    static class LocusWanderGoal extends Goal {
        private final LocusEntity locus;
        private final double speed;

        public LocusWanderGoal(LocusEntity locus, double speed) {
            this.locus = locus;
            this.speed = speed;
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            return this.locus.getTarget() == null && !this.locus.getMoveControl().isMoving() && 
                   this.locus.getRandom().nextInt(10) == 0;
        }

        @Override
        public boolean shouldContinue() {
            return this.locus.getMoveControl().isMoving();
        }

        @Override
        public void start() {
            double x = this.locus.getX() + (this.locus.getRandom().nextDouble() * 2 - 1) * 16;
            double y = this.locus.getY() + (this.locus.getRandom().nextDouble() * 2 - 1) * 8;
            double z = this.locus.getZ() + (this.locus.getRandom().nextDouble() * 2 - 1) * 16;
            
            // Ensure minimum height
            y = Math.max(y, this.locus.getWorld().getBottomY() + 5);
            
            this.locus.getMoveControl().moveTo(x, y, z, this.speed);
            this.locus.targetAltitude = (float) y;
        }
    }
}