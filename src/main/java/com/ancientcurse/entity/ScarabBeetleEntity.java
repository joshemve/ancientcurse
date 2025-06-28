package com.ancientcurse.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Scarab Beetle Entity - A ground-based, spider-like creature with pinchers and defensive behavior.
 * Features wall-climbing abilities, aggressive melee attacks, and procedural leg animations.
 */
public class ScarabBeetleEntity extends HostileEntity implements GeoEntity {
    
    /* ---------- DATA TRACKERS ---------- */
    private static final TrackedData<Boolean> ATTACKING = 
            DataTracker.registerData(ScarabBeetleEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> ATTACK_COOLDOWN = 
            DataTracker.registerData(ScarabBeetleEntity.class, TrackedDataHandlerRegistry.INTEGER);
    
    /* ---------- CONSTANTS ---------- */
    private static final int MAX_ATTACK_COOLDOWN = 30; // 1.5 seconds between attacks
    
    /* ---------- FIELDS ---------- */
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int swingTicks;
    
    /* ---------- DAMAGE TRACKING ---------- */
    public int lastDamageTime = 0; // For renderer hurt flash effects
    
    public ScarabBeetleEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 5; // Moderate XP reward
    }
    
    /* ---------- ATTRIBUTES ---------- */
    public static DefaultAttributeContainer.Builder createScarabBeetleAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 18.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0)
                .add(EntityAttributes.GENERIC_ARMOR, 2.0); // Natural chitin armor
    }
    
    /* ---------- GOALS ---------- */
    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.add(3, new WanderAroundGoal(this, 0.8));
        this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(5, new LookAroundGoal(this));
        
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }
    
    /* ---------- WALL CLIMBING ---------- */
    public boolean isClimbing() {
        return this.horizontalCollision;
    }
    
    /* ---------- TICK ---------- */
    @Override
    public void tick() {
        super.tick();
        
        // Handle attack cooldown
        int cd = dataTracker.get(ATTACK_COOLDOWN);
        if (cd > 0) dataTracker.set(ATTACK_COOLDOWN, cd - 1);
        
        // Handle attack animation
        if (swingTicks > 0 && --swingTicks == 0) {
            dataTracker.set(ATTACKING, false);
        }
    }
    
    /* ---------- COMBAT ---------- */
    public boolean tryAttack(LivingEntity target) {
        if (dataTracker.get(ATTACK_COOLDOWN) > 0) return false;
        
        dataTracker.set(ATTACKING, true);
        dataTracker.set(ATTACK_COOLDOWN, MAX_ATTACK_COOLDOWN);
        swingTicks = 15; // Animation duration
        
        return super.tryAttack(target);
    }
    
    @Override
    public boolean damage(DamageSource source, float amount) {
        this.lastDamageTime = this.age;
        return super.damage(source, amount);
    }
    
    /* ---------- EXPERIENCE POINTS ---------- */
    /**
     * Get the experience points this entity drops when killed.
     */
    public int getExperiencePoints() {
        return this.experiencePoints;
    }
    
    /**
     * Set the experience points this entity drops when killed.
     */
    public void setExperiencePoints(int points) {
        this.experiencePoints = points;
    }
    
    /* ---------- SOUNDS ---------- */
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_SPIDER_AMBIENT;
    }
    
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.ENTITY_SPIDER_HURT;
    }
    
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_SPIDER_DEATH;
    }
    
    protected SoundEvent getStepSound() {
        return SoundEvents.ENTITY_SPIDER_STEP;
    }
    
    /* ---------- DATA TRACKING ---------- */
    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(ATTACKING, false);
        this.dataTracker.startTracking(ATTACK_COOLDOWN, 0);
    }
    
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("ExperiencePoints", this.experiencePoints);
    }
    
    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.experiencePoints = nbt.getInt("ExperiencePoints");
    }
    
    /* ---------- GECKOLIB ANIMATION ---------- */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }
    
    private <T extends GeoEntity> PlayState predicate(AnimationState<T> state) {
        if (dataTracker.get(ATTACKING)) {
            state.getController().setAnimation(RawAnimation.begin().then("animation.scarab_beetle.attack", Animation.LoopType.PLAY_ONCE));
        } else if (state.isMoving()) {
            state.getController().setAnimation(RawAnimation.begin().then("animation.scarab_beetle.walk", Animation.LoopType.LOOP));
        } else {
            state.getController().setAnimation(RawAnimation.begin().then("animation.scarab_beetle_idle", Animation.LoopType.LOOP));
        }
        return PlayState.CONTINUE;
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
    
    /* ---------- GETTERS FOR RENDERER ---------- */
    public boolean isAttacking() {
        return dataTracker.get(ATTACKING);
    }
    
    public int getAttackCooldown() {
        return dataTracker.get(ATTACK_COOLDOWN);
    }
} 