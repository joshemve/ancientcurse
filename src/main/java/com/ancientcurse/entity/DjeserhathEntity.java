package com.ancientcurse.entity;

import net.minecraft.util.math.Vec3d;
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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import net.minecraft.world.World;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;

import java.util.*;

public class DjeserhathEntity extends HostileEntity implements GeoEntity {

    /* ------------------------------------------------------------------------
     *  STATIC CONSTANTS + DATA TRACKERS
     * --------------------------------------------------------------------- */

    // Core constants
    private static final float ACTIVATE_RADIUS = 5.0F;
    private static final float DEACTIVATE_RADIUS = 12.0F;  // Deactivate when player is this far
    private static final int SPIT_COOLDOWN_TICKS = 160;    // 8 seconds (much longer)
    private static final int SPIT_WARMUP_TICKS  = 20;      // 1 second
    private static final float SPIT_RANGE       = 10.0F;
    private static final int DEACTIVATION_DELAY_TICKS = 40; // 2 seconds (faster return to idle)
    
    // Enhanced behavior constants
    private static final float SNAP_ATTACK_RADIUS = 2.5F;
    private static final int SNAP_COOLDOWN_TICKS = 40;
    private static final int DIGESTION_TIME_TICKS = 600;
    private static final int VIBRATION_RANGE = 16;
    private static final int VIBRATION_DECAY_RATE = 2;
    private static final int MAX_VIBRATION_ENTRIES = 20;
    private static final int MAX_PREY_MEMORY_ENTRIES = 10;

    // Existing tracked data
    private static final TrackedData<Boolean> IS_ACTIVATED =
            DataTracker.registerData(DjeserhathEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IS_SPITTING  =
            DataTracker.registerData(DjeserhathEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IS_ATTACKING =
            DataTracker.registerData(DjeserhathEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IS_DYING     =
            DataTracker.registerData(DjeserhathEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    
    // Enhanced tracked data
    private static final TrackedData<Float> JAW_OPENNESS = 
            DataTracker.registerData(DjeserhathEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> HUNTING_STATE = 
            DataTracker.registerData(DjeserhathEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> IS_LURING = 
            DataTracker.registerData(DjeserhathEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IS_DIGESTING = 
            DataTracker.registerData(DjeserhathEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    // Hunting state enum
    public enum HuntingState {
        DORMANT(0),
        SENSING(1),
        LURING(2),
        STALKING(3),
        STRIKING(4),
        DIGESTING(5),
        SATISFIED(6);
        
        private final int id;
        HuntingState(int id) { this.id = id; }
        public int getId() { return id; }
        
        public static HuntingState fromId(int id) {
            for (HuntingState state : values()) {
                if (state.id == id) return state;
            }
            return DORMANT;
        }
    }

    /* -------------------------------------------------------------------- */
    /*  FIELDS                                                              */
    /* -------------------------------------------------------------------- */

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private final Cooldown spitCd = new Cooldown(SPIT_COOLDOWN_TICKS);
    private final Cooldown snapCd = new Cooldown(SNAP_COOLDOWN_TICKS);

    // These fields are now handled directly in the SpitAttackGoal

    private int deactivationTimer = 0;
    
    // Enhanced behavior fields
    private HuntingState currentHuntingState = HuntingState.DORMANT;
    private Map<BlockPos, Integer> vibrationMap = new LinkedHashMap<BlockPos, Integer>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<BlockPos, Integer> eldest) {
            return size() > MAX_VIBRATION_ENTRIES;
        }
    };
    private int lastFeedTime = 0;
    private int vibrationCheckTicker = 0;
    private Map<UUID, Integer> preyMemory = new LinkedHashMap<UUID, Integer>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, Integer> eldest) {
            return size() > MAX_PREY_MEMORY_ENTRIES;
        }
    };
    private BlockPos favoriteHuntingSpot = null;
    private int huntingSuccessCount = 0;
    
    // Animation tracking
    private String targetAnim = "animation.djeserhath.idle"; // Default base animation: idle or idle_activate
    private int spitAnimationCooldown = 0;
    private boolean playActivationAnimation = false;
    private BlockPos anchorPos = null;

    public DjeserhathEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 15;
    }

    /* -------------------------------------------------------------------- */
    /*  ATTRIBUTES                                                          */
    /* -------------------------------------------------------------------- */

    public static DefaultAttributeContainer.Builder createDjeserhathAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 4.0D);
    }

    /* -------------------------------------------------------------------- */
    /*  DATATRACKER + GOALS                                                 */
    /* -------------------------------------------------------------------- */

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        dataTracker.startTracking(IS_ACTIVATED, false);
        dataTracker.startTracking(IS_SPITTING, false);
        dataTracker.startTracking(IS_ATTACKING, false);
        dataTracker.startTracking(IS_DYING, false);
        dataTracker.startTracking(JAW_OPENNESS, 0.0f);
        dataTracker.startTracking(HUNTING_STATE, HuntingState.DORMANT.getId());
        dataTracker.startTracking(IS_LURING, false);
        dataTracker.startTracking(IS_DIGESTING, false);
    }

    @Override
    protected void initGoals() {
        // Rooted plant goals - no movement!
        goalSelector.add(1, new RootedBiteGoal(this));
        goalSelector.add(2, new DjeserhathSpitAttackGoal(this));
        goalSelector.add(3, new LuringGoal(this));
        
        // Removed look goals - rotation is handled in the renderer for upper body only

        // Target selection
        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true) {
            @Override
            public boolean canStart() {
                // Only target players within activation range
                return super.canStart() && DjeserhathEntity.this.isActivated();
            }
        });
    }

    /* -------------------------------------------------------------------- */
    /*  MAIN TICK METHOD                                                    */
    /* -------------------------------------------------------------------- */

    @Override
    public void tick() {
        // Anchor the entity to its spawn position (server-side only to avoid client desync)
        if (!this.getWorld().isClient) {
            if (anchorPos == null) {
                anchorPos = this.getBlockPos();
            } else {
                // Teleport back only if we've drifted more than 0.1 blocks from the anchor
                double dx = this.getX() - (anchorPos.getX() + 0.5);
                double dz = this.getZ() - (anchorPos.getZ() + 0.5);
                double dy = this.getY() - anchorPos.getY();
                if ((dx * dx + dy * dy + dz * dz) > 0.01) {
                    this.teleport(anchorPos.getX() + 0.5, anchorPos.getY(), anchorPos.getZ() + 0.5);
                }
            }
            
            // Handle spit animation cooldown to allow smooth animation transitions
            if (spitAnimationCooldown > 0) {
                spitAnimationCooldown--;
                // When we reach halfway through the cooldown, reset the spitting state
                // This gives time for the animation to properly transition
                if (spitAnimationCooldown == 20 && isSpitting()) {
                    setSpitting(false);
                }
            }
            
            // IMPROVED: Better player-facing when activated
            if (isActivated()) {
                LivingEntity target = getTarget();
                if (target != null && target.isAlive()) {
                    // Calculate angle to target
                    double dx = target.getX() - this.getX();
                    double dz = target.getZ() - this.getZ();
                    float targetYaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
                    
                    // Don't rotate the entire entity - this keeps the bottom_plant stationary
                    // The renderer will handle rotating individual body parts to track the target
                    // Comment out the rotation code:
                    /*
                    float yawDiff = MathHelper.wrapDegrees(targetYaw - this.getYaw());
                    float rotationSpeed = 4.0F;
                    
                    if (Math.abs(yawDiff) > 5.0F) {
                        this.setYaw(this.getYaw() + MathHelper.clamp(yawDiff, -rotationSpeed, rotationSpeed));
                        this.bodyYaw = this.getYaw();
                        this.headYaw = this.getYaw();
                    }
                    */
                }
            }
        }

        if (isDying()) {
            super.tick();
            return;
        }

        super.tick();
        
        // Update cooldowns
        spitCd.tick();
        snapCd.tick();
        
        if (spitAnimationCooldown > 0) {
            spitAnimationCooldown--;
        }
        
        // Server-side logic
        if (!this.getWorld().isClient) {
            updateHuntingBehavior();
            updateEnvironmentalFactors();
            handleVibrationDecay();
            updateActivation();
        }
        
        // Particle effects based on state
        if (this.getWorld().isClient && age % 10 == 0) {
            spawnStateParticles();
        }
    }

    /* -------------------------------------------------------------------- */
    /*  ENHANCED BEHAVIOR METHODS                                           */
    /* -------------------------------------------------------------------- */

    private void updateHuntingBehavior() {
        // State transitions based on conditions
        if (currentHuntingState == HuntingState.DIGESTING) {
            if (getWorld().getTime() - lastFeedTime > DIGESTION_TIME_TICKS) {
                setHuntingState(HuntingState.SATISFIED);
            }
            return;
        }
        
        if (currentHuntingState == HuntingState.SATISFIED) {
            if (getWorld().getTime() - lastFeedTime > DIGESTION_TIME_TICKS * 2) {
                setHuntingState(HuntingState.DORMANT);
            }
        }
        
        // Check for vibrations when dormant
        if (currentHuntingState == HuntingState.DORMANT && !vibrationMap.isEmpty()) {
            int totalVibration = vibrationMap.values().stream().mapToInt(Integer::intValue).sum();
            if (totalVibration > 10) {
                setHuntingState(HuntingState.SENSING);
                dataTracker.set(JAW_OPENNESS, 0.2f);
            }
        }
        
        // Transition from sensing to luring/stalking
        if (currentHuntingState == HuntingState.SENSING && isActivated()) {
            LivingEntity target = getTarget();
            if (target != null) {
                double distance = squaredDistanceTo(target);
                if (distance > SNAP_ATTACK_RADIUS * SNAP_ATTACK_RADIUS * 4) {
                    setHuntingState(HuntingState.LURING);
                } else {
                    setHuntingState(HuntingState.STALKING);
                }
            }
        }
    }

    private void updateEnvironmentalFactors() {
        World world = getWorld();
        
        // More active at night
        boolean isNight = world.getTimeOfDay() % 24000 >= 13000 && world.getTimeOfDay() % 24000 <= 23000;
        if (isNight && currentHuntingState == HuntingState.DORMANT) {
            setHuntingState(HuntingState.SENSING);
        }
        
        // More aggressive in rain
        if (world.isRaining() && currentHuntingState != HuntingState.DORMANT) {
            spitCd.reduce(20);
        }
    }

    private void handleVibrationDecay() {
        if (++vibrationCheckTicker >= 20) { // Every second
            vibrationCheckTicker = 0;
            vibrationMap.entrySet().removeIf(entry -> {
                entry.setValue(entry.getValue() - VIBRATION_DECAY_RATE);
                return entry.getValue() <= 0;
            });
        }
    }

    private void spawnStateParticles() {
        switch (currentHuntingState) {
            case DORMANT:
                // No particles when dormant
                break;
                
            case SENSING:
                // Subtle alert particles
                if (age % 30 == 0) {
                    double x = getX() + (random.nextDouble() - 0.5) * 0.5;
                    double y = getY() + 0.8;
                    double z = getZ() + (random.nextDouble() - 0.5) * 0.5;
                    getWorld().addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.02, 0);
                }
                break;
                
            case LURING:
                // Sweet scent particles
                for (int i = 0; i < 2; i++) {
                    double x = getX() + (random.nextDouble() - 0.5) * 1.5;
                    double y = getY() + 1.0 + random.nextDouble();
                    double z = getZ() + (random.nextDouble() - 0.5) * 1.5;
                    getWorld().addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, x, y, z, 0, -0.02, 0);
                }
                break;
                
            case STALKING:
                // Occasional tense particles
                if (age % 15 == 0) {
                    double x = getX() + (random.nextDouble() - 0.5) * 0.3;
                    double y = getY() + 0.1;
                    double z = getZ() + (random.nextDouble() - 0.5) * 0.3;
                    getWorld().addParticle(ParticleTypes.ASH, x, y, z, 0, 0, 0);
                }
                break;
                
            case STRIKING:
                // Quick motion particles
                for (int i = 0; i < 3; i++) {
                    double x = getX() + (random.nextDouble() - 0.5);
                    double y = getY() + 0.5 + (random.nextDouble() - 0.5);
                    double z = getZ() + (random.nextDouble() - 0.5);
                    getWorld().addParticle(ParticleTypes.CRIT, x, y, z, 0, 0, 0);
                }
                break;
                
            case DIGESTING:
                // Digestive particles
                if (age % 20 == 0) {
                    double x = getX() + (random.nextDouble() - 0.5) * 0.5;
                    double y = getY() + 0.5;
                    double z = getZ() + (random.nextDouble() - 0.5) * 0.5;
                    getWorld().addParticle(ParticleTypes.HAPPY_VILLAGER, x, y, z, 0, 0.1, 0);
                }
                break;
                
            case SATISFIED:
                // Occasional contentment particles
                if (age % 40 == 0) {
                    double x = getX() + (random.nextDouble() - 0.5) * 0.8;
                    double y = getY() + 0.7;
                    double z = getZ() + (random.nextDouble() - 0.5) * 0.8;
                    getWorld().addParticle(ParticleTypes.HAPPY_VILLAGER, x, y, z, 0, 0.05, 0);
                }
                break;
        }
    }

    public void detectVibration(BlockPos source, int strength) {
        if (!isActivated() && currentHuntingState == HuntingState.DORMANT) {
            double dist = Math.sqrt(source.getSquaredDistance(getBlockPos()));
            if (dist <= VIBRATION_RANGE) {
                vibrationMap.merge(source, strength, Integer::sum);
            }
        }
    }

    private void updateActivation() {
        // Enhanced activation logic
        if (!isActivated() && !this.getWorld().isClient) {
            PlayerEntity nearest = this.getWorld().getClosestPlayer(this, ACTIVATE_RADIUS);
            if (nearest != null && canSee(nearest)) {
                setActivated(true);
                this.targetAnim = "animation.djeserhath.idle_activated";
                setTarget(nearest);
                deactivationTimer = 0;
                playActivationAnimation = true; // Trigger activation animation
                if (currentHuntingState == HuntingState.DORMANT) {
                    setHuntingState(HuntingState.SENSING);
                }
            }
        }
        
        // Handle deactivation when player gets too far
        if (isActivated() && !this.getWorld().isClient) {
            LivingEntity target = getTarget();
            boolean shouldRemainActive = false;
            
            // Check if any player is within deactivation range
            PlayerEntity nearestPlayer = this.getWorld().getClosestPlayer(this, DEACTIVATE_RADIUS);
            if (nearestPlayer != null) {
                double distance = this.squaredDistanceTo(nearestPlayer);
                if (distance <= DEACTIVATE_RADIUS * DEACTIVATE_RADIUS) {
                    shouldRemainActive = true;
                    
                    // If player is shooting us, stay active longer
                    if (nearestPlayer.isUsingItem() && distance <= SPIT_RANGE * SPIT_RANGE) {
                        deactivationTimer = 0;
                    }
                }
            }
            
            if (shouldRemainActive) {
                deactivationTimer = 0;
                
                // Update target if needed
                if (target == null || !target.isAlive() || squaredDistanceTo(target) > DEACTIVATE_RADIUS * DEACTIVATE_RADIUS) {
                    if (nearestPlayer != null) {
                        setTarget(nearestPlayer);
                    }
                }
            } else {
                // No players in range, start deactivation timer
                deactivationTimer++;
                if (deactivationTimer >= DEACTIVATION_DELAY_TICKS) {
                    // Quickly return to idle state like a venus fly trap
                    setActivated(false);
                    this.targetAnim = "animation.djeserhath.idle";
                    setAttacking(false);
                    setSpitting(false);
                    setHuntingState(HuntingState.DORMANT);
                    setTarget(null);
                    deactivationTimer = 0;
                    dataTracker.set(JAW_OPENNESS, 0.0f);
                }
            }
        }
    }

    /* -------------------------------------------------------------------- */
    /*  ATTACK METHODS                                                      */
    /* -------------------------------------------------------------------- */

    private void performSpitAttack() {
        LivingEntity target = this.getTarget();
        if (target == null) {
            return;
        }

        // Get target position for aiming
        Vec3d targetPos = target.getPos().add(0, target.getHeight() * 0.5, 0);
        Vec3d selfPos = this.getPos().add(0, this.getHeight() * 0.75, 0);
        Vec3d motion = targetPos.subtract(selfPos).normalize().multiply(0.8);

        // Spawn projectile
        SpitBallEntity projectile = new SpitBallEntity(this.getWorld(), this);
        projectile.setPosition(selfPos.x, selfPos.y, selfPos.z);
        projectile.setVelocity(motion.x, motion.y, motion.z);
        this.getWorld().spawnEntity(projectile);

        // Play spit attack sounds
        this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_LLAMA_SPIT, SoundCategory.HOSTILE,
                1.0F, 1.2F);

        this.spitCd.reset();
    }

    private void performSnapAttack(LivingEntity target) {
        if (target == null || !target.isAlive()) return;
        
        // Play snap sound
        this.getWorld().playSound(null, getX(), getY(), getZ(),
                SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.HOSTILE,
                1.5F, 0.5F);
        
        // Deal damage
        target.damage(this.getDamageSources().mobAttack(this), 12.0f);
        
        // Apply small pull-in effect
        double dx = (this.getX() - target.getX());
        double dz = (this.getZ() - target.getZ());
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > 0.1) {
            target.addVelocity(dx / dist * 0.3, 0.1, dz / dist * 0.3);
            target.velocityModified = true;
        }
        
        // "Swallow" small entities
        if (target.getHeight() < 1.0f && target.getHealth() <= 0) {
            setHuntingState(HuntingState.DIGESTING);
            lastFeedTime = (int)getWorld().getTime();
            dataTracker.set(IS_DIGESTING, true);
            
            // Learn from successful hunt
            if (target instanceof PlayerEntity) {
                preyMemory.merge(target.getUuid(), 1, Integer::sum);
            }
            
            // Remember successful hunting spot
            if (favoriteHuntingSpot == null || getBlockPos().equals(favoriteHuntingSpot)) {
                huntingSuccessCount++;
                if (huntingSuccessCount > 3) {
                    favoriteHuntingSpot = getBlockPos();
                }
            }
        }
        
        snapCd.reset();
    }

    /* -------------------------------------------------------------------- */
    /*  CUSTOM AI GOALS                                                     */
    /* -------------------------------------------------------------------- */

    private class RootedBiteGoal extends Goal {
        private final DjeserhathEntity host;
        private LivingEntity biteTarget;
        private int biteCooldown = 0;
        private int attackAnimationDuration = 0;

        RootedBiteGoal(DjeserhathEntity host) { 
            this.host = host;
            this.setControls(EnumSet.of(Control.LOOK));
        }

        @Override 
        public boolean canStart() {
            if (!host.snapCd.ready() || host.currentHuntingState == HuntingState.DIGESTING) {
                return false;
            }
            
            LivingEntity target = host.getTarget();
            if (target == null || !target.isAlive()) return false;
            
            // Check if target is in bite range
            double dist = target.squaredDistanceTo(host);
            return dist <= SNAP_ATTACK_RADIUS * SNAP_ATTACK_RADIUS && host.canSee(target);
        }

        @Override 
        public void start() {
            host.setHuntingState(HuntingState.STRIKING);
            biteTarget = host.getTarget();
            // Direct jaw control
            host.dataTracker.set(JAW_OPENNESS, 1.0f); // Fully open
            host.setAttacking(true); // This triggers the attack animation
            biteCooldown = 15; // Ticks until bite
            attackAnimationDuration = 30; // Total animation duration
        }

        @Override 
        public boolean shouldContinue() { 
            return (biteCooldown > 0 || attackAnimationDuration > 0) && 
                   biteTarget != null && biteTarget.isAlive() && 
                   host.squaredDistanceTo(biteTarget) <= SNAP_ATTACK_RADIUS * SNAP_ATTACK_RADIUS * 1.5;
        }

        @Override 
        public void tick() {
            if (biteTarget != null) {
                // Always look at the target
                host.getLookControl().lookAt(biteTarget);
                
                // Countdown until the bite attack
                if (biteCooldown > 0) {
                    biteCooldown--;
                    
                    // When ready, snap at the target
                    if (biteCooldown == 0 && host.getJawOpenness() > 0.8f) {
                        // SNAP!
                        host.dataTracker.set(JAW_OPENNESS, 0.0f); // Close jaw
                        host.performSnapAttack(biteTarget);
                        
                        // Play attack sound
                        host.getWorld().playSound(null, host.getX(), host.getY(), host.getZ(),
                            SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.HOSTILE,
                            1.0F, 0.8F);
                    }
                }
                
                // Count down the total animation duration
                if (attackAnimationDuration > 0) {
                    attackAnimationDuration--;
                }
            }
        }
        
        @Override
        public void stop() {
            // Only reset attacking state when the animation is finished
            host.setAttacking(false);
            if (host.currentHuntingState == HuntingState.STRIKING) {
                host.setHuntingState(HuntingState.STALKING);
            }
            biteTarget = null;
            biteCooldown = 0;
        }
    }

    private class LuringGoal extends Goal {
        private final DjeserhathEntity host;
        private int luringTicks = 0;
        
        LuringGoal(DjeserhathEntity host) { this.host = host; }

        @Override 
        public boolean canStart() {
            return host.currentHuntingState == HuntingState.LURING && 
                   host.getTarget() != null;
        }

        @Override 
        public void start() {
            host.dataTracker.set(IS_LURING, true);
            luringTicks = 0;
        }

        @Override 
        public boolean shouldContinue() { 
            return host.currentHuntingState == HuntingState.LURING && 
                   host.getTarget() != null && 
                   luringTicks < 200;
        }

        @Override 
        public void tick() {
            luringTicks++;
            LivingEntity target = host.getTarget();
            
            if (target != null) {
                double distance = host.squaredDistanceTo(target);
                
                // Transition to stalking when prey gets close
                if (distance <= SNAP_ATTACK_RADIUS * SNAP_ATTACK_RADIUS * 4) {
                    host.setHuntingState(HuntingState.STALKING);
                }
                
                // Gentle swaying motion
                if (luringTicks % 20 == 0) {
                    float jawOpen = 0.3f + random.nextFloat() * 0.2f;
                    host.dataTracker.set(JAW_OPENNESS, jawOpen);
                }
            }
        }
        
        @Override
        public void stop() {
            host.dataTracker.set(IS_LURING, false);
        }
    }

    private class DjeserhathSpitAttackGoal extends Goal {
        private final DjeserhathEntity host;
        private int spitWarmup;
        private LivingEntity spitTarget;
        
        DjeserhathSpitAttackGoal(DjeserhathEntity host) {
            this.host = host;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }
        
        @Override
        public boolean canStart() {
            if (!host.isActivated() || !host.spitCd.ready() || 
                host.currentHuntingState == HuntingState.DIGESTING || 
                host.spitAnimationCooldown > 0) {
                return false;
            }
            
            LivingEntity target = this.host.getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            }
            
            // Check distance to target - only use spit attack if target is outside melee range
            // This ensures we prefer melee attack when the player is close
            double dist = target.squaredDistanceTo(host);
            double meleeRangeSq = SNAP_ATTACK_RADIUS * SNAP_ATTACK_RADIUS * 1.2;
            
            // Only use spit attack if the target is beyond melee range but within spit range
            return dist > meleeRangeSq && dist <= SPIT_RANGE * SPIT_RANGE && host.canSee(target);
        }
        
        @Override
        public void start() {
            spitWarmup = SPIT_WARMUP_TICKS;
            spitTarget = host.getTarget();
            host.setSpitting(true);
        }
        
        @Override
        public boolean shouldContinue() {
            // Stop spitting if the target gets into melee range
            double dist = spitTarget != null ? spitTarget.squaredDistanceTo(host) : 0;
            double meleeRangeSq = SNAP_ATTACK_RADIUS * SNAP_ATTACK_RADIUS * 1.2;
            
            return spitWarmup > 0 && spitTarget != null && spitTarget.isAlive() && 
                   dist > meleeRangeSq && host.canSee(spitTarget);
        }
        
        @Override
        public void tick() {
            if (spitTarget != null) {
                // Look control disabled - rotation handled in renderer
                
                // Begin opening jaw as we prepare to spit
                host.dataTracker.set(JAW_OPENNESS, Math.min(1.0f, host.getJawOpenness() + 0.1f));
                
                spitWarmup--;
                if (spitWarmup == 0) {
                    host.performSpitAttack();
                    host.spitAnimationCooldown = 40; // Cooldown after spitting
                    stop();
                }
            }
        }
        
        @Override
        public void stop() {
            // Instead of immediately setting spitting to false, add a delay timer
            // This allows the animation to complete before resetting state
            host.spitAnimationCooldown = 40; // animation cooldown 
            
            // We'll handle resetting the spitting state in the tick method
            // Using a delay allows the client animation to finish properly
        }
    }

    /* -------------------------------------------------------------------- */
    /*  ANIMATION CONTROLLERS                                               */
    /* -------------------------------------------------------------------- */

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar r) {
        r.add(new AnimationController<>(this, "main", 0, this::mainPredicate));
    }

    private <T extends GeoAnimatable> PlayState mainPredicate(AnimationState<T> state) {
        // Make sure targetAnim is correctly set based on activation state
        // This ensures proper idle animation is always used after other animations end
        if (isActivated() && this.targetAnim.equals("animation.djeserhath.idle")) {
            this.targetAnim = "animation.djeserhath.idle_activated";
        } else if (!isActivated() && this.targetAnim.equals("animation.djeserhath.idle_activated")) {
            this.targetAnim = "animation.djeserhath.idle";
        }
        
        // One-shot animations have priority
        if (isDying()) {
            state.getController().setAnimation(RawAnimation.begin().thenPlay("animation.djeserhath.death"));
            return PlayState.CONTINUE;
        }

        if (isAttacking()) {
            // Chain the attack animation with the correct idle animation to avoid disappearing
            state.getController().setAnimation(RawAnimation.begin()
                .then("animation.djeserhath.attack", Animation.LoopType.PLAY_ONCE)
                .thenLoop(this.targetAnim));
            return PlayState.CONTINUE;
        }

        if (isSpitting()) {
            // For spit attack, we need a special animation sequence to avoid disappearing
            AnimationController<?> controller = state.getController();
            
            // If we're already playing a spit animation, keep playing it
            if (controller.getCurrentAnimation() != null && 
                controller.getCurrentAnimation().animation().name().equals("animation.djeserhath.spit_attack")) {
                return PlayState.CONTINUE;
            }
            
            // Otherwise, start a new spit animation that explicitly transitions to the activated idle
            // Ensure we use the correct targetAnim here to avoid animation glitches
            controller.setAnimation(RawAnimation.begin()
                .then("animation.djeserhath.spit_attack", Animation.LoopType.PLAY_ONCE)
                .thenLoop(isActivated() ? "animation.djeserhath.idle_activated" : "animation.djeserhath.idle"));
            return PlayState.CONTINUE;
        }

        // If we just became activated, play the activation animation
        if (playActivationAnimation) {
            playActivationAnimation = false; // Consume the flag
            state.getController().setAnimation(RawAnimation.begin()
                .thenPlay("animation.djeserhath.activate")
                .thenLoop("animation.djeserhath.idle_activated")); // Always transition to activated idle
            return PlayState.CONTINUE;
        }

        // If the controller is idle or animation needs changing, set the looping animation
        String currentAnimationName = state.getController().getCurrentAnimation() != null ? 
            state.getController().getCurrentAnimation().animation().name() : "";
        if (state.getController().getAnimationState() == AnimationController.State.STOPPED || 
            !this.targetAnim.equals(currentAnimationName)) {
             state.getController().setAnimation(RawAnimation.begin().thenLoop(this.targetAnim));
        }

        return PlayState.CONTINUE;
    }

    /* -------------------------------------------------------------------- */
    /*  SOUNDS & DAMAGE HANDLING                                            */
    /* -------------------------------------------------------------------- */

    @Override 
    protected SoundEvent getAmbientSound() { 
        switch (currentHuntingState) {
            case LURING:
                return SoundEvents.BLOCK_HONEY_BLOCK_STEP;
            case DIGESTING:
                return SoundEvents.ENTITY_GENERIC_EAT;
            default:
                return SoundEvents.ENTITY_SLIME_SQUISH_SMALL;
        }
    }
    
    @Override 
    protected SoundEvent getHurtSound(DamageSource src) { 
        return SoundEvents.BLOCK_FUNGUS_BREAK; 
    }
    
    @Override 
    protected SoundEvent getDeathSound() { 
        return SoundEvents.BLOCK_SLIME_BLOCK_BREAK; 
    }

    @Override
    public boolean damage(DamageSource src, float amt) {
        if (!isActivated()) {
            setActivated(true);
            setHuntingState(HuntingState.SENSING);
            playActivationAnimation = true; // Trigger activation animation when damaged
            if (src.getAttacker() instanceof LivingEntity lv) {
                setTarget(lv);
                
                // Remember aggressive prey
                if (lv instanceof PlayerEntity) {
                    preyMemory.merge(lv.getUuid(), -1, Integer::sum);
                }
            }
        }
        
        // Reset deactivation timer when damaged
        deactivationTimer = 0;
        
        // Interrupt digestion if hurt badly
        if (currentHuntingState == HuntingState.DIGESTING && amt > 5.0f) {
            setHuntingState(HuntingState.STALKING);
        }
        
        return super.damage(src, amt);
    }

    @Override
    public void onDeath(DamageSource src) {
        if (!isDying()) {
            setDying(true);
            playSound(getDeathSound(), getSoundVolume(), getSoundPitch());
            super.onDeath(src);
        }
    }

    /* -------------------------------------------------------------------- */
    /*  ACCESSORS & UTILITIES                                               */
    /* -------------------------------------------------------------------- */

    public boolean isActivated() { return dataTracker.get(IS_ACTIVATED); }
    public void setActivated(boolean v) { dataTracker.set(IS_ACTIVATED, v); }

    public boolean isSpitting() { return dataTracker.get(IS_SPITTING); }
    public void setSpitting(boolean v) { dataTracker.set(IS_SPITTING, v); }

    public boolean isAttacking() { return dataTracker.get(IS_ATTACKING); }
    public void setAttacking(boolean v) { dataTracker.set(IS_ATTACKING, v); }

    public boolean isDying() { return dataTracker.get(IS_DYING); }
    public void setDying(boolean v) { dataTracker.set(IS_DYING, v); }
    
    public float getJawOpenness() { return dataTracker.get(JAW_OPENNESS); }
    
    public HuntingState getHuntingState() { 
        return HuntingState.fromId(dataTracker.get(HUNTING_STATE)); 
    }
    
    public void setHuntingState(HuntingState state) { 
        this.currentHuntingState = state;
        dataTracker.set(HUNTING_STATE, state.getId()); 
    }

    @Override 
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    
    @Override 
    public double getTick(Object unused) { return this.getWorld().getTime(); }

    /* -------------------------------------------------------------------- */
    /*  HELPER CLASSES                                                      */
    /* -------------------------------------------------------------------- */

    private static final class Cooldown {
        private final int max;
        private int value = 0;
        
        Cooldown(int max) { this.max = max; }
        
        void tick() { if (value > 0) value--; }
        void reset() { value = max; }
        boolean ready() { return value == 0; }
        
        void reduce(int amount) { 
            value = Math.max(0, value - amount); 
        }
    }
}