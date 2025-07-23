package com.ancientcurse.entity;

import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;

import java.util.EnumSet;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Scarab Beetle Entity - A ground-based, spider-like creature.
 * Features wall-climbing abilities, melee attacks, and burrowing behavior.
 */
public class ScarabBeetleEntity extends SpiderEntity implements GeoEntity {
    
    /* ------------------ ENTITY CONSTANTS ------------------ */
    
    /* ---------- CONSTANTS ---------- */
    private static final int MAX_ATTACK_COOLDOWN = 30;
    private static final int BURROW_TIME = 40; // 2 seconds (matches dig_down animation)
    private static final int EMERGE_TIME = 43; // ~2.16 seconds (matches dig_up animation)
    
    // Animation state flags
    public boolean isAttacking = false;
    public boolean isBurrowing = false;
    public boolean isEmerging = false;
    public boolean isUnderground = false;
    
    // Timers
    private int burrowTicks = 0;
    private int emergeTicks = 0;
    private int attackCooldown = 0;
    
    // GeckoLib animation
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    public ScarabBeetleEntity(EntityType<? extends SpiderEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 5;
    }
    
    /* ---------- ATTRIBUTES ---------- */
    public static DefaultAttributeContainer.Builder createScarabBeetleAttributes() {
        return SpiderEntity.createSpiderAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 18.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28) // Slightly slower for better control
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 20.0) // Reduced for less aggressive behavior
                .add(EntityAttributes.GENERIC_ARMOR, 2.0);
    }
    
    /* ---------- INITIALIZATION ---------- */
    public EntityData initialize(ServerWorld world, LocalDifficulty difficulty, SpawnReason spawnReason, 
                                 EntityData entityData, NbtCompound entityNbt) {
        entityData = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
        return entityData;
    }
    
    /* ---------- GOALS - SIMPLIFIED ---------- */
    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new BurrowGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.add(3, new WanderAroundGoal(this, 0.6));
        this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(5, new LookAroundGoal(this));
        
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }
    
    /* ---------- TICK - OPTIMIZED ---------- */
    @Override
    public void tick() {
        super.tick();
        
        age++;
        
        // Manage attack cooldown
        if (attackCooldown > 0) {
            attackCooldown--;
            if (attackCooldown == 0) {
                isAttacking = false;
            }
        }
        
        // Manage burrowing animation
        if (burrowTicks > 0) {
            burrowTicks--;
            if (burrowTicks == 0) {
                isBurrowing = false;
                isUnderground = true;
            }
        }
        
        // Manage emerging animation
        if (emergeTicks > 0) {
            emergeTicks--;
            if (emergeTicks == 0) {
                isEmerging = false;
                isUnderground = false;
            }
        }
    }
    
    /* ---------- COMBAT - SIMPLIFIED ---------- */
    public boolean tryAttack(LivingEntity target) {
        if (attackCooldown > 0) return false;
        
        attackCooldown = MAX_ATTACK_COOLDOWN;
        isAttacking = true;
        
        boolean attackSuccess = super.tryAttack(target);
        
        if (attackSuccess) {
            this.playSound(SoundEvents.ENTITY_SPIDER_HURT, 1.0f, 0.8f + this.random.nextFloat() * 0.4f);
        }
        
        return attackSuccess;
    }
    
    @Override
    public boolean damage(DamageSource source, float amount) {
        // Chance to burrow when health is low
        if (this.getHealth() / this.getMaxHealth() < 0.3f && !isBurrowing && !isUnderground && this.isOnGround() && this.random.nextFloat() < 0.25f) {
            startBurrowing();
        }
        
        return super.damage(source, amount);
    }
    
    private void startBurrowing() {
        isBurrowing = true;
        burrowTicks = BURROW_TIME;
        this.setNoGravity(true);
    }
    
    private void emerge() {
        isEmerging = true;
        emergeTicks = EMERGE_TIME;
        this.setNoGravity(false);
        
        // Emergence effects
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.POOF,
                this.getX(), this.getY(), this.getZ(),
                8, 0.4, 0.2, 0.4, 0.1);
        }
        
        this.playSound(SoundEvents.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.6f, 1.4f);
    }
    
    /* ---------- SIMPLIFIED AI GOALS ---------- */
    
    public static class BurrowGoal extends Goal {
        private final ScarabBeetleEntity beetle;
        private int undergroundTicks = 0;
        
        public BurrowGoal(ScarabBeetleEntity beetle) {
            this.beetle = beetle;
            this.setControls(EnumSet.of(Control.MOVE, Control.JUMP));
        }
        
        @Override
        public boolean canStart() {
            return beetle.getHealth() < 6 && beetle.isOnGround() && 
                   !beetle.isBurrowing && !beetle.isUnderground && beetle.random.nextInt(100) == 0;
        }
        
        @Override
        public void start() {
            beetle.startBurrowing();
            undergroundTicks = 60; // Stay underground for 3 seconds
        }
        
        @Override
        public boolean shouldContinue() {
            return beetle.isBurrowing || beetle.isUnderground || beetle.isEmerging;
        }
        
        @Override
        public void tick() {
            if (beetle.isUnderground && undergroundTicks > 0) {
                undergroundTicks--;
                if (undergroundTicks == 0) {
                    beetle.emerge();
                }
            }
        }
    }
    
    /* ---------- GECKOLIB ANIMATION - UPDATED WITH NEW ANIMATIONS ---------- */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main_controller", 2, this::predicate));
    }
    
    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> state) {
        // Animation controller with all animations
        
        if (this.isDead()) {
            state.getController().setAnimation(RawAnimation.begin()
                .then("animation.scarab_beetle.death", Animation.LoopType.HOLD_ON_LAST_FRAME));
            return PlayState.CONTINUE;
        }
        
        // Burrowing animations (dig down)
        if (isBurrowing) {
            state.getController().setAnimation(RawAnimation.begin()
                .then("animation.scarab_beetle.dig_down", Animation.LoopType.PLAY_ONCE));
            return PlayState.CONTINUE;
        }
        
        // Emerging animations (dig up)
        if (isEmerging) {
            state.getController().setAnimation(RawAnimation.begin()
                .then("animation.scarab_beetle.dig_up", Animation.LoopType.PLAY_ONCE));
            return PlayState.CONTINUE;
        }
        
        // Attack animations
        if (isAttacking && attackCooldown > 20) {
            // Use attack2 for alternate attacks
            if (this.getRandom().nextBoolean()) {
                state.getController().setAnimation(RawAnimation.begin()
                    .then("animation.scarab_beetle.attack2", Animation.LoopType.PLAY_ONCE));
            } else {
                state.getController().setAnimation(RawAnimation.begin()
                    .then("animation.scarab_beetle.attack", Animation.LoopType.PLAY_ONCE));
            }
            return PlayState.CONTINUE;
        }
        
        // Check for movement
        if (this.getVelocity().horizontalLengthSquared() > 0.01D) {
            state.getController().setAnimation(RawAnimation.begin()
                .then("animation.scarab_beetle.walking", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }
        
        // Default to idle
        state.getController().setAnimation(RawAnimation.begin()
            .then("animation.scarab_beetle.idle", Animation.LoopType.LOOP));
        return PlayState.CONTINUE;
    }
    
    /* ---------- DATA TRACKING ---------- */
    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        // No custom data tracking needed - using fields instead
    }
    
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        
        // Save animation state
        nbt.putBoolean("IsAttacking", isAttacking);
        nbt.putBoolean("IsBurrowing", isBurrowing);
        nbt.putBoolean("IsEmerging", isEmerging);
        nbt.putBoolean("IsUnderground", isUnderground);
        
        // Save timers
        nbt.putInt("BurrowTicks", burrowTicks);
        nbt.putInt("EmergeTicks", emergeTicks);
        nbt.putInt("AttackCooldown", attackCooldown);
    }
    
    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        
        // Load animation state
        isAttacking = nbt.getBoolean("IsAttacking");
        isBurrowing = nbt.getBoolean("IsBurrowing");
        isEmerging = nbt.getBoolean("IsEmerging");
        isUnderground = nbt.getBoolean("IsUnderground");
        
        // Load timers
        burrowTicks = nbt.getInt("BurrowTicks");
        emergeTicks = nbt.getInt("EmergeTicks");
        attackCooldown = nbt.getInt("AttackCooldown");
    }
    
    /* ---------- GETTERS ---------- */
    public boolean isBurrowing() { return isBurrowing; }
    public boolean isAttacking() { return isAttacking; }
    
    /**
     * Used by the model to determine if walking animation should play
     * Uses same threshold as animation controller
     */
    public boolean isMoving() {
        // Match animation controller threshold
        return this.getVelocity().horizontalLengthSquared() > 0.01D;
    }
    
    /**
     * Required by GeoEntity interface
     */
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
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
    
    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.ENTITY_SPIDER_STEP, 0.12f, 1.0f);
    }
    
    /* ---------- DROPS ---------- */
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        super.dropLoot(source, causedByPlayer);
    }
}