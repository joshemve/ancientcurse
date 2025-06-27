package com.ancientcurse.entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;

/**
 * The Withered Pharaoh entity - an ancient undead ruler of the desert
 */
public class WitheredPharaohEntity extends HostileEntity implements GeoEntity {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    
    private int magicAttackCooldown = 0;
    private int magicAttackWarmup = 0;
    private LivingEntity magicAttackTarget;
    private static final int MAGIC_COOLDOWN = 100; // 5 seconds
    private static final int MAGIC_WARMUP = 30; // 1.5 seconds
    private static final float MAGIC_ATTACK_RANGE = 16.0F;
    
    // Track if the entity is dead for animation purposes
    private static final TrackedData<Boolean> IS_DYING = DataTracker.registerData(WitheredPharaohEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private int deathTime = 0;
    private boolean playingDeathAnimation = false;
    
    public WitheredPharaohEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.setPathfindingPenalty(PathNodeType.WATER, -1.0F);
        this.experiencePoints = 20; // More XP than regular mobs
    }
    
    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(IS_DYING, false);
    }
    
    @Override
    protected void initGoals() {
        // Add basic goals
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new PharaohMagicAttackGoal(this)); // Custom magic attack goal
        this.goalSelector.add(3, new MeleeAttackGoal(this, 1.2D, false)); // Faster attack speed
        this.goalSelector.add(4, new WanderAroundFarGoal(this, 1.0D));
        this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 16.0F)); // Increased awareness range
        this.goalSelector.add(6, new LookAroundGoal(this));
        
        // Add targeting goals - higher priority for revenge
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }
    
    // Custom goal for magic attack
    private class PharaohMagicAttackGoal extends Goal {
        private final WitheredPharaohEntity pharaoh;
        
        public PharaohMagicAttackGoal(WitheredPharaohEntity entity) {
            this.pharaoh = entity;
        }
        
        @Override
        public boolean canStart() {
            if (pharaoh.magicAttackCooldown > 0) {
                return false;
            }
            
            LivingEntity target = pharaoh.getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            }
            
            double distSq = pharaoh.squaredDistanceTo(target);
            return distSq > 4.0 && distSq < MAGIC_ATTACK_RANGE * MAGIC_ATTACK_RANGE;
        }
        
        @Override
        public void start() {
            pharaoh.magicAttackWarmup = MAGIC_WARMUP;
            pharaoh.magicAttackTarget = pharaoh.getTarget();
            pharaoh.handSwinging = true; // Trigger attack animation
        }
        
        @Override
        public boolean shouldContinue() {
            return pharaoh.magicAttackWarmup > 0;
        }
        
        @Override
        public void tick() {
            pharaoh.magicAttackWarmup--;
            
            // Look at target
            if (pharaoh.magicAttackTarget != null) {
                pharaoh.getLookControl().lookAt(pharaoh.magicAttackTarget);
            }
            
            // When warmup is done, perform the attack
            if (pharaoh.magicAttackWarmup == 0 && pharaoh.magicAttackTarget != null && pharaoh.magicAttackTarget.isAlive()) {
                performMagicAttack();
                pharaoh.magicAttackCooldown = MAGIC_COOLDOWN;
            }
        }
        
        private void performMagicAttack() {
            if (!(pharaoh.getWorld() instanceof ServerWorld)) return;
            
            // Play sound
            pharaoh.playSound(SoundEvents.ENTITY_WITHER_SHOOT, 1.0F, 1.0F);
            
            // Create wither skull projectile
            double d = pharaoh.magicAttackTarget.getX() - pharaoh.getX();
            double e = pharaoh.magicAttackTarget.getBodyY(0.5D) - pharaoh.getBodyY(0.5D);
            double f = pharaoh.magicAttackTarget.getZ() - pharaoh.getZ();
            
            WitherSkullEntity witherSkull = new WitherSkullEntity(pharaoh.getWorld(), pharaoh, d, e, f);
            witherSkull.setPos(pharaoh.getX(), pharaoh.getBodyY(0.5D), pharaoh.getZ());
            pharaoh.getWorld().spawnEntity(witherSkull);
            
            // Apply wither effect to nearby players
            for (PlayerEntity player : pharaoh.getWorld().getPlayers()) {
                if (pharaoh.squaredDistanceTo(player) < 25.0 && pharaoh.canSee(player)) {
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 100, 0));
                }
            }
        }
    }
    
    /**
     * Set up entity attributes like health, movement speed, attack damage
     */
    public static DefaultAttributeContainer.Builder createWitheredPharaohAttributes() {
        return HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 60.0D) // Increased health
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25D)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0D) // Increased damage
            .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 1.0D)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D) // Increased awareness range
            .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.6D);
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        // Death controller needs highest priority (lowest number)
        controllerRegistrar.add(new AnimationController<>(this, "deathController", -1, this::deathPredicate));
        controllerRegistrar.add(new AnimationController<>(this, "controller", 0, this::predicate));
        controllerRegistrar.add(new AnimationController<>(this, "attackController", 0, this::attackPredicate));
    }
    
    @Override
    public void tick() {
        // If we're playing the death animation, handle it specially
        if (this.playingDeathAnimation) {
            this.deathTime++;
            
            // Prevent movement and other behaviors during death animation
            this.setVelocity(0, this.getVelocity().y * 0.98, 0);
            this.setTarget(null);
            
            // Log every second during death animation (for debugging)
            if (!this.getWorld().isClient && this.deathTime % 20 == 0) {
                System.out.println("Death animation progress: " + this.deathTime + "/193 ticks");
            }
            
            // After death animation finishes (9.625 seconds = ~193 ticks), actually remove entity
            if (this.deathTime >= 193) {
                if (!this.getWorld().isClient) {
                    System.out.println("Death animation complete, removing entity");
                }
                this.playingDeathAnimation = false;
                this.remove(RemovalReason.KILLED); // Force removal after animation
                return;
            }
            
            // Skip normal tick logic during death animation
            return;
        }
        
        super.tick();
        
        // Decrease cooldown timer
        if (this.magicAttackCooldown > 0) {
            this.magicAttackCooldown--;
        }
        
        // Dynamic movement speed - increase speed when chasing a player
        LivingEntity target = this.getTarget();
        if (target instanceof PlayerEntity && target.isAlive() && this.canSee(target)) {
            // Calculate distance to target
            double distSq = this.squaredDistanceTo(target);
            
            // Speed boost when chasing but not too close
            if (distSq > 16.0 && distSq < 100.0) { // Between 4-10 blocks away
                // Apply speed boost (30% faster)
                this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.325D);
            } else {
                // Reset to normal speed
                this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.25D);
            }
        } else {
            // Reset to normal speed when not chasing
            this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.25D);
        }
    }
    
    /**
     * Main animation controller for idle/walk animations
     */
    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> state) {
        // Don't play movement animations if dying
        if (this.isDying()) {
            return PlayState.STOP;
        }
        
        if (state.isMoving()) {
            state.getController().setAnimation(RawAnimation.begin().then("animation.the_withered_pharaoh.walk", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }
        
        state.getController().setAnimation(RawAnimation.begin().then("animation.the_withered_pharaoh.idle", Animation.LoopType.LOOP));
        return PlayState.CONTINUE;
    }
    
    /**
     * Magic attack animation controller
     */
    private <T extends GeoAnimatable> PlayState attackPredicate(AnimationState<T> state) {
        // Don't play attack animations if dying
        if (this.isDying()) {
            return PlayState.STOP;
        }
        
        if (this.handSwinging) {
            state.getController().setAnimation(RawAnimation.begin().then("animation.the_withered_pharaoh.magic_attack1", Animation.LoopType.PLAY_ONCE));
            this.handSwinging = false;
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }
    
    /**
     * Death animation controller
     */
    private <T extends GeoAnimatable> PlayState deathPredicate(AnimationState<T> state) {
        if (this.isDying()) {
            // Force animation reset to ensure it plays from the beginning
            state.getController().forceAnimationReset();
            // Set animation with explicit name matching the animation file
            state.getController().setAnimation(RawAnimation.begin().then("animation.the_withered_pharaoh.death", Animation.LoopType.PLAY_ONCE));
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }
    
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_WITHER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_WITHER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_WITHER_DEATH;
    }
    
    @Override
    public boolean damage(DamageSource source, float amount) {
        // Make the pharaoh immediately target players who attack it
        if (source.getAttacker() instanceof PlayerEntity) {
            this.setTarget((LivingEntity)source.getAttacker());
            this.magicAttackCooldown = 20; // Short cooldown before potential magic attack
        }
        
        return super.damage(source, amount);
    }
    
    /**
     * Override onDeath to handle death animation
     */
    @Override
    public void onDeath(DamageSource source) {
        // Start death animation instead of immediately dying
        if (!this.isDying() && !this.isDead()) {
            // Set dying state
            this.setDying(true);
            this.playingDeathAnimation = true;
            this.deathTime = 0;
            
            // Play death sound
            this.playSound(this.getDeathSound(), this.getSoundVolume(), this.getSoundPitch());
            
            // Log that death animation is starting (for debugging)
            if (!this.getWorld().isClient) {
                System.out.println("Withered Pharaoh death animation starting");
            }
            
            // Call the parent onDeath to trigger loot table drops
            // This ensures items are dropped according to the loot table
            super.onDeath(source);
        }
    }
    
    /**
     * Check if entity is in dying state
     */
    public boolean isDying() {
        return this.dataTracker.get(IS_DYING);
    }
    
    /**
     * Set entity dying state
     */
    public void setDying(boolean dying) {
        this.dataTracker.set(IS_DYING, dying);
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
    
    @Override
    public double getTick(Object entity) {
        return getWorld().getTime();
    }
    
    /**
     * Override isAlive to prevent entity from being removed before death animation completes
     */
    @Override
    public boolean isAlive() {
        return super.isAlive() || this.playingDeathAnimation;
    }
}
