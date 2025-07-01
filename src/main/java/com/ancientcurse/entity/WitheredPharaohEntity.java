package com.ancientcurse.entity;
import net.minecraft.entity.Entity;
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
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

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
    
    // Animation state tracking
    private boolean isScreaming = false;
    private int screamTime = 0;
    private static final int SCREAM_DURATION = 172; // 8.5833 seconds * 20 ticks
    
    private boolean isFlying = false;
    private int flyingTime = 0;
    
    // Animation state tracking for attacks
    private boolean isPerformingMagicAttack = false;
    private int magicAttackAnimationTime = 0;
    private static final int MAGIC_ATTACK_ANIMATION_DURATION = 30; // 1.5 seconds for magic_attack2
    
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
        this.goalSelector.add(5, new PharaohLookAtTargetGoal(this)); // Custom aggressive look-at goal
        this.goalSelector.add(6, new LookAroundGoal(this));
        
        // Add targeting goals - higher priority for revenge
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }
    
    // Custom aggressive look-at goal for better targeting
    private class PharaohLookAtTargetGoal extends Goal {
        private final WitheredPharaohEntity pharaoh;
        private LivingEntity target;
        private int lookTime;
        private final float maxLookDistance;
        private final float lookChance;
        
        public PharaohLookAtTargetGoal(WitheredPharaohEntity entity) {
            this.pharaoh = entity;
            this.maxLookDistance = 32.0F; // Increased range
            this.lookChance = 0.02F; // More frequent looking
        }
        
        @Override
        public boolean canStart() {
            if (this.pharaoh.getRandom().nextFloat() >= this.lookChance) {
                return false;
            }
            
            this.target = this.pharaoh.getTarget();
            if (this.target == null) {
                // If no target, look for nearby players
                this.target = this.pharaoh.getWorld().getClosestPlayer(this.pharaoh, this.maxLookDistance);
            }
            
            return this.target != null && this.target.isAlive() && 
                   this.pharaoh.squaredDistanceTo(this.target) <= (double)(this.maxLookDistance * this.maxLookDistance) &&
                   this.pharaoh.canSee(this.target);
        }
        
        @Override
        public boolean shouldContinue() {
            if (!this.target.isAlive()) {
                return false;
            }
            
            if (this.pharaoh.squaredDistanceTo(this.target) > (double)(this.maxLookDistance * this.maxLookDistance)) {
                return false;
            }
            
            return this.lookTime > 0;
        }
        
        @Override
        public void start() {
            this.lookTime = 40 + this.pharaoh.getRandom().nextInt(40); // Look for 2-4 seconds
        }
        
        @Override
        public void tick() {
            this.pharaoh.getLookControl().lookAt(this.target, 30.0F, 30.0F); // Faster head turning
            --this.lookTime;
        }
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
            // Don't trigger animation here, wait for actual attack
        }
        
        @Override
        public boolean shouldContinue() {
            return pharaoh.magicAttackWarmup > 0;
        }
        
        @Override
        public void tick() {
            pharaoh.magicAttackWarmup--;
            
            // Enhanced targeting during magic attack - look more aggressively at target
            if (pharaoh.magicAttackTarget != null) {
                pharaoh.getLookControl().lookAt(pharaoh.magicAttackTarget, 45.0F, 45.0F); // More aggressive looking
                
                // If target is moving, predict their position
                if (pharaoh.magicAttackTarget instanceof PlayerEntity) {
                    PlayerEntity player = (PlayerEntity) pharaoh.magicAttackTarget;
                    double predictedX = player.getX() + player.getVelocity().x * 0.5;
                    double predictedZ = player.getZ() + player.getVelocity().z * 0.5;
                    pharaoh.getLookControl().lookAt(predictedX, player.getY() + player.getHeight() * 0.5, predictedZ, 45.0F, 45.0F);
                }
            }
            
            // When warmup is done, trigger the animation
            if (pharaoh.magicAttackWarmup == 0 && pharaoh.magicAttackTarget != null && pharaoh.magicAttackTarget.isAlive()) {
                pharaoh.triggerMagicAttack(); // Trigger animation
                pharaoh.magicAttackCooldown = MAGIC_COOLDOWN;
                // Don't perform attack yet, wait for animation
            }
        }
        
        private void performMagicAttack() {
            // Attack logic moved to tick() method to sync with animation timing
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
        
        // Scream controller (high priority for intimidation)
        controllerRegistrar.add(new AnimationController<>(this, "screamController", 1, this::screamPredicate));
        
        // Flying controller (medium priority)
        controllerRegistrar.add(new AnimationController<>(this, "flyingController", 2, this::flyingPredicate));
        
        // Attack controllers (medium priority)
        controllerRegistrar.add(new AnimationController<>(this, "magicAttackController", 3, this::magicAttackPredicate));
        controllerRegistrar.add(new AnimationController<>(this, "staffAttackController", 3, this::staffAttackPredicate));
        
        // Main movement controller (lowest priority)
        controllerRegistrar.add(new AnimationController<>(this, "movementController", 4, this::movementPredicate));
    }
    
    @Override
    public void tick() {
        // If we're playing the death animation, handle it specially
        if (this.playingDeathAnimation) {
            this.deathTime++;
            
            // Prevent movement and other behaviors during death animation
            this.setVelocity(0, this.getVelocity().y * 0.98, 0);
            this.setTarget(null);
            
            // Death particles
            if (this.getWorld() instanceof ServerWorld && this.deathTime % 4 == 0) {
                ServerWorld serverWorld = (ServerWorld) this.getWorld();
                
                // Soul particles escaping
                for (int i = 0; i < 3; i++) {
                    double offsetX = (this.random.nextDouble() - 0.5) * 0.5;
                    double offsetZ = (this.random.nextDouble() - 0.5) * 0.5;
                    double yVelocity = 0.1 + this.random.nextDouble() * 0.1;
                    
                    serverWorld.spawnParticles(
                        ParticleTypes.SOUL,
                        this.getX() + offsetX,
                        this.getY() + this.getHeight() * 0.5,
                        this.getZ() + offsetZ,
                        1, 0, yVelocity, 0, 0.02
                    );
                }
                
                // Dust particles as body crumbles
                if (this.deathTime > 60) {
                    serverWorld.spawnParticles(
                        new DustParticleEffect(new Vector3f(0.4f, 0.3f, 0.2f), 1.5f),
                        this.getX(), this.getY() + 0.5, this.getZ(),
                        5, 0.3, 0.3, 0.3, 0
                    );
                }
            }
            
            // Log every second during death animation (for debugging)
            if (!this.getWorld().isClient && this.deathTime % 20 == 0) {
                System.out.println("Death animation progress: " + this.deathTime + "/193 ticks");
            }
            
            // After death animation finishes (9.625 seconds = ~193 ticks), actually remove entity
            if (this.deathTime >= 193) {
                if (!this.getWorld().isClient) {
                    System.out.println("Death animation complete, removing entity");
                    
                    // Final explosion of particles
                    if (this.getWorld() instanceof ServerWorld) {
                        ServerWorld serverWorld = (ServerWorld) this.getWorld();
                        serverWorld.spawnParticles(
                            ParticleTypes.POOF,
                            this.getX(), this.getY() + this.getHeight() * 0.5, this.getZ(),
                            20, 0.5, 0.5, 0.5, 0.05
                        );
                    }
                }
                this.playingDeathAnimation = false;
                this.remove(RemovalReason.KILLED); // Force removal after animation
                return;
            }
            
            // Skip normal tick logic during death animation
            return;
        }
        
        super.tick();
        
        // Update animation timers
        if (this.isPerformingMagicAttack) {
            this.magicAttackAnimationTime++;
            
            // Prevent movement during magic attack
            this.setVelocity(0, this.getVelocity().y, 0);
            this.getNavigation().stop();
            
            // Shoot the wither skull 0.5 seconds (10 ticks) into the animation
            if (this.magicAttackAnimationTime == 10 && this.magicAttackTarget != null && this.magicAttackTarget.isAlive()) {
                // Perform the actual attack
                if (this.getWorld() instanceof ServerWorld) {
                    ServerWorld serverWorld = (ServerWorld) this.getWorld();
                    
                    // Play sound
                    this.playSound(SoundEvents.ENTITY_WITHER_SHOOT, 1.0F, 1.0F);
                    
                    // Calculate staff tip position
                    Vec3d lookVec = this.getRotationVector();
                    double staffLength = 1.5;
                    double staffTipX = this.getX() + lookVec.x * staffLength;
                    double staffTipY = this.getY() + this.getHeight() * 0.7;
                    double staffTipZ = this.getZ() + lookVec.z * staffLength;
                    
                    // Create wither skull projectile
                    double d = this.magicAttackTarget.getX() - this.getX();
                    double e = this.magicAttackTarget.getBodyY(0.5D) - this.getBodyY(0.5D);
                    double f = this.magicAttackTarget.getZ() - this.getZ();
                    
                    WitherSkullEntity witherSkull = new WitherSkullEntity(this.getWorld(), this, d, e, f);
                    witherSkull.setPos(staffTipX, staffTipY, staffTipZ);
                    this.getWorld().spawnEntity(witherSkull);
                    
                    // Burst of particles when firing
                    for (int i = 0; i < 10; i++) {
                        serverWorld.spawnParticles(
                            ParticleTypes.SOUL_FIRE_FLAME,
                            staffTipX + (this.random.nextDouble() - 0.5) * 0.3,
                            staffTipY + (this.random.nextDouble() - 0.5) * 0.3,
                            staffTipZ + (this.random.nextDouble() - 0.5) * 0.3,
                            1, 0, 0, 0, 0.05
                        );
                    }
                }
            }
            
            // Add particle effects during magic attack from staff
            if (this.getWorld() instanceof ServerWorld && this.magicAttackAnimationTime % 3 == 0) {
                ServerWorld serverWorld = (ServerWorld) this.getWorld();
                Vec3d lookVec = this.getRotationVector();
                double staffLength = 1.5;
                double staffTipX = this.getX() + lookVec.x * staffLength;
                double staffTipY = this.getY() + this.getHeight() * 0.7;
                double staffTipZ = this.getZ() + lookVec.z * staffLength;
                
                // Magic buildup at staff tip
                serverWorld.spawnParticles(
                    ParticleTypes.WITCH,
                    staffTipX,
                    staffTipY,
                    staffTipZ,
                    2, 0.1, 0.1, 0.1, 0.01
                );
                
                // Energy swirls
                serverWorld.spawnParticles(
                    new DustParticleEffect(new Vector3f(0.7f, 0.0f, 1.0f), 0.8f),
                    staffTipX, staffTipY, staffTipZ,
                    3, 0.2, 0.2, 0.2, 0
                );
            }
            
            if (this.magicAttackAnimationTime >= MAGIC_ATTACK_ANIMATION_DURATION) {
                this.isPerformingMagicAttack = false;
                this.magicAttackAnimationTime = 0;
            }
        }
        
        // Staff attack disabled for now
        
        // Update scream timer
        if (this.isScreaming) {
            this.screamTime++;
            
            // Prevent movement during scream
            this.setVelocity(0, this.getVelocity().y, 0);
            this.getNavigation().stop();
            
            // Scream particles - dark energy emanating from mouth
            if (this.getWorld() instanceof ServerWorld && this.screamTime % 3 == 0) {
                ServerWorld serverWorld = (ServerWorld) this.getWorld();
                double mouthHeight = this.getY() + this.getHeight() * 0.85;
                Vec3d lookVec = this.getRotationVector();
                
                serverWorld.spawnParticles(
                    ParticleTypes.SCULK_SOUL,
                    this.getX() + lookVec.x * 0.5,
                    mouthHeight,
                    this.getZ() + lookVec.z * 0.5,
                    2, 0.1, 0.1, 0.1, 0.05
                );
                
                // Fear particles around the pharaoh
                serverWorld.spawnParticles(
                    ParticleTypes.SMOKE,
                    this.getX(), mouthHeight, this.getZ(),
                    5, 0.5, 0.5, 0.5, 0.02
                );
            }
            
            if (this.screamTime >= SCREAM_DURATION) {
                this.isScreaming = false;
                this.screamTime = 0;
            }
        }
        
        // Update flying timer
        if (this.isFlying) {
            this.flyingTime++;
            
            // Flying particles - sand swirling beneath
            if (this.getWorld() instanceof ServerWorld && this.flyingTime % 2 == 0) {
                ServerWorld serverWorld = (ServerWorld) this.getWorld();
                
                // Swirling sand effect
                double angle = this.flyingTime * 0.1;
                for (int i = 0; i < 3; i++) {
                    double offsetAngle = angle + (Math.PI * 2 * i / 3);
                    serverWorld.spawnParticles(
                        new DustParticleEffect(new Vector3f(0.86f, 0.8f, 0.6f), 1.0f),
                        this.getX() + Math.cos(offsetAngle) * 1.5,
                        this.getY() - 0.5,
                        this.getZ() + Math.sin(offsetAngle) * 1.5,
                        1, 0, 0, 0, 0
                    );
                }
            }
        }
        
        // Update cooldowns
        if (this.magicAttackCooldown > 0) {
            this.magicAttackCooldown--;
        }
        
        // Prevent movement during magic attack warmup
        if (this.magicAttackWarmup > 0) {
            this.setVelocity(0, this.getVelocity().y, 0);
            this.getNavigation().stop();
        }
        
        // Random chance to start screaming (intimidation)
        if (!this.isScreaming && !this.isDying() && this.getTarget() != null && this.random.nextInt(400) == 0) {
            this.startScream();
        }
        
        // Check if should be flying (when target is far away or entity is elevated)
        LivingEntity target = this.getTarget();
        if (target != null && !this.isDying()) {
            double distance = this.squaredDistanceTo(target);
            boolean shouldFly = distance > 64.0 || this.getY() > target.getY() + 2.0;
            
            if (shouldFly && !this.isFlying) {
                this.startFlying();
            } else if (!shouldFly && this.isFlying) {
                this.stopFlying();
            }
        } else if (this.isFlying) {
            this.stopFlying();
        }
        
        // Dynamic movement speed - increase speed when chasing a player
        if (target instanceof PlayerEntity && target.isAlive() && this.canSee(target)) {
            // Calculate distance to target
            double distSq = this.squaredDistanceTo(target);
            
            // Enhanced targeting - always look at player when in range
            if (distSq <= 64.0) { // Within 8 blocks
                this.getLookControl().lookAt(target, 30.0F, 30.0F);
                
                // If very close, be more aggressive about looking
                if (distSq <= 16.0) {
                    this.getLookControl().lookAt(target, 45.0F, 45.0F);
                }
            }
            
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
        
        // Additional targeting enhancement - if we have a target but can't see them, try to look in their direction
        if (target != null && target.isAlive() && !this.canSee(target)) {
            double distSq = this.squaredDistanceTo(target);
            if (distSq <= 100.0) { // Within 10 blocks
                // Look in the general direction of the target
                this.getLookControl().lookAt(target.getX(), target.getY() + target.getHeight() * 0.5, target.getZ(), 20.0F, 20.0F);
            }
        }
    }
    
    /**
     * Main movement animation controller for idle/walk animations
     */
    private <T extends GeoAnimatable> PlayState movementPredicate(AnimationState<T> state) {
        // Don't play movement animations if dying, screaming, flying, or performing attacks
        if (this.isDying() || this.isScreaming || this.isFlying || this.isPerformingMagicAttack) {
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
    private <T extends GeoAnimatable> PlayState magicAttackPredicate(AnimationState<T> state) {
        // Don't play attack animations if dying
        if (this.isDying()) {
            return PlayState.STOP;
        }
        
        if (this.isPerformingMagicAttack) {
            // Use magic_attack2 for wither skull projectile attacks
            state.getController().setAnimation(RawAnimation.begin().then("animation.the_withered_pharaoh.magic_attack2", Animation.LoopType.PLAY_ONCE));
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }
    
    /**
     * Staff attack animation controller (disabled)
     */
    private <T extends GeoAnimatable> PlayState staffAttackPredicate(AnimationState<T> state) {
        // Staff attack disabled for now
        return PlayState.STOP;
    }
    
    /**
     * Scream animation controller
     */
    private <T extends GeoAnimatable> PlayState screamPredicate(AnimationState<T> state) {
        if (this.isScreaming) {
            state.getController().setAnimation(RawAnimation.begin().then("animation.the_withered_pharaoh.scream", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }
    
    /**
     * Flying animation controller
     */
    private <T extends GeoAnimatable> PlayState flyingPredicate(AnimationState<T> state) {
        if (this.isFlying) {
            state.getController().setAnimation(RawAnimation.begin().then("animation.the_withered_pharaoh.flying", Animation.LoopType.LOOP));
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
            
            // Immediately look at the attacker
            this.getLookControl().lookAt(source.getAttacker(), 45.0F, 45.0F);
            
            // Staff attack disabled for now
        }
        
        // Chance to scream when taking significant damage
        if (amount > 4.0f && !this.isScreaming && this.random.nextInt(3) == 0) {
            this.startScream();
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
    
    /**
     * Start the scream animation
     */
    public void startScream() {
        if (!this.isScreaming && !this.isDying()) {
            this.isScreaming = true;
            this.screamTime = 0;
            // Play a scary sound
            this.playSound(SoundEvents.ENTITY_WITHER_AMBIENT, 1.5f, 0.5f);
        }
    }
    
    /**
     * Start the flying animation
     */
    public void startFlying() {
        if (!this.isFlying && !this.isDying()) {
            this.isFlying = true;
            this.flyingTime = 0;
        }
    }
    
    /**
     * Stop the flying animation
     */
    public void stopFlying() {
        this.isFlying = false;
        this.flyingTime = 0;
    }
    
    /**
     * Check if entity is screaming
     */
    public boolean isScreaming() {
        return this.isScreaming;
    }
    
    /**
     * Check if entity is flying
     */
    public boolean isFlying() {
        return this.isFlying;
    }
    
    /**
     * Trigger a magic attack animation
     */
    public void triggerMagicAttack() {
        if (!this.isPerformingMagicAttack) {
            this.isPerformingMagicAttack = true;
            this.magicAttackAnimationTime = 0;
            if (!this.getWorld().isClient) {
                System.out.println("Withered Pharaoh triggered magic attack animation (magic_attack2)");
            }
        }
    }
    
    /**
     * Trigger a staff attack animation (disabled)
     */
    public void triggerStaffAttack() {
        // Staff attack disabled for now
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
    
    /**
     * Override tryAttack to trigger staff attack animation
     */
    @Override
    public boolean tryAttack(Entity target) {
        // Use normal melee attack for now
        return super.tryAttack(target);
    }
    
    /**
     * Add ambient particle effects
     */
    @Override
    public void tickMovement() {
        super.tickMovement();
        
        // Ambient particles when not in special animations
        if (!this.isDying() && !this.isScreaming && this.age % 20 == 0) {
            if (this.getWorld() instanceof ServerWorld) {
                ServerWorld serverWorld = (ServerWorld) this.getWorld();
                
                // Occasional dark wisps
                if (this.random.nextInt(3) == 0) {
                    serverWorld.spawnParticles(
                        ParticleTypes.SMOKE,
                        this.getX() + (this.random.nextDouble() - 0.5) * 0.5,
                        this.getY() + this.getHeight() * 0.8,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 0.5,
                        1, 0, 0.05, 0, 0.01
                    );
                }
                
                // Glowing eyes particle effect
                Vec3d lookVec = this.getRotationVector();
                double eyeHeight = this.getY() + this.getHeight() * 0.9;
                
                serverWorld.spawnParticles(
                    new DustParticleEffect(new Vector3f(0.8f, 0.2f, 0.2f), 0.5f),
                    this.getX() + lookVec.x * 0.3,
                    eyeHeight,
                    this.getZ() + lookVec.z * 0.3,
                    1, 0.05, 0, 0.05, 0
                );
            }
        }
    }
}
