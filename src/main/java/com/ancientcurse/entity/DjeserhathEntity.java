package com.ancientcurse.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.PathNodeType;
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
    private static final float ACTIVATE_RADIUS_SQ = 25.0F;     // 5-block radius ^2
    private static final int SPIT_COOLDOWN_TICKS = 80;         // 4 s
    private static final int SPIT_WARMUP_TICKS  = 15;          // 0.75 s
    private static final float SPIT_RANGE       = 10.0F;
    private static final int DEACTIVATION_DELAY_TICKS = 60;    // 3 seconds
    
    // New constants for enhanced behaviors
    private static final float SNAP_ATTACK_RADIUS = 2.5F;
    private static final int SNAP_COOLDOWN_TICKS = 40;
    private static final int DIGESTION_TIME_TICKS = 600;       // 30 seconds
    private static final int VIBRATION_RANGE = 16;
    private static final int VIBRATION_DECAY_RATE = 2;
    private static final float JAW_SMOOTH_SPEED = 0.08f;

    // Existing tracked data
    private static final TrackedData<Boolean> IS_ACTIVATED =
            DataTracker.registerData(DjeserhathEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IS_SPITTING  =
            DataTracker.registerData(DjeserhathEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IS_ATTACKING =
            DataTracker.registerData(DjeserhathEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IS_DYING     =
            DataTracker.registerData(DjeserhathEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    
    // New tracked data for enhanced features
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
        DORMANT(0),      // Completely still, conserving energy
        SENSING(1),      // Detected vibrations, slowly awakening
        LURING(2),       // Using scent/visual lures to attract prey
        STALKING(3),     // Tracking prey movement, predicting path
        STRIKING(4),     // Fast snap attack
        DIGESTING(5),    // Processing captured prey
        SATISFIED(6);    // Recently fed, less aggressive
        
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

    private int spitWarmup = 0;
    private LivingEntity spitTarget;
    private int lastSeenTargetTicks = 0;
    
    // New fields for enhanced behaviors
    private float jawTarget = 0.0f;
    private float jawCurrent = 0.0f;
    private HuntingState currentHuntingState = HuntingState.DORMANT;
    private Map<BlockPos, Integer> vibrationMap = new HashMap<>();
    private int lastFeedTime = 0;
    private int vibrationCheckTicker = 0;
    private Map<UUID, Integer> preyMemory = new HashMap<>();
    private BlockPos favoriteHuntingSpot = null;
    private int huntingSuccessCount = 0;
    
    // Animation tracking
    private String currentMainAnimation = "";
    private boolean currentlyPlayingAttack = false;
    private boolean currentlyPlayingSpitAttack = false;

    public DjeserhathEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.setPathfindingPenalty(PathNodeType.WATER, -1.0F);
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
        // Priority order is important!
        goalSelector.add(1, new SnapTrapAttackGoal(this));
        goalSelector.add(2, new DjeserhathSpitAttackGoal(this));
        goalSelector.add(3, new DjeserhathMeleeAttackGoal(this, 1.0D, false));
        goalSelector.add(4, new LuringGoal(this));
        
        // Conditional look goals
        goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 12.0F) {
            @Override
            public boolean canStart() {
                return DjeserhathEntity.this.isActivated() && super.canStart();
            }
            
            @Override
            public boolean shouldContinue() {
                return DjeserhathEntity.this.isActivated() && super.shouldContinue();
            }
        });
        
        goalSelector.add(6, new LookAroundGoal(this) {
            @Override
            public boolean canStart() {
                return DjeserhathEntity.this.isActivated() && 
                       currentHuntingState != HuntingState.DORMANT && 
                       super.canStart();
            }
        });

        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    /* -------------------------------------------------------------------- */
    /*  MAIN TICK METHOD                                                    */
    /* -------------------------------------------------------------------- */

    @Override
    public void tick() {
        if (isDying()) {
            super.tick();
            return;
        }

        super.tick();
        
        // Update cooldowns
        spitCd.tick();
        snapCd.tick();
        
        // Server-side logic
        if (!this.getWorld().isClient) {
            updateHuntingBehavior();
            updateEnvironmentalFactors();
            handleVibrationDecay();
            updateActivation();
        }
        
        // Client and server
        updateJawMovement();
        
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
            return; // Skip other updates while digesting
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
                jawTarget = 0.2f;
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
        
        // Update jaw based on hunting state
        updateJawForHuntingState();
    }

    private void updateJawForHuntingState() {
        switch (currentHuntingState) {
            case DORMANT:
                jawTarget = 0.0f;
                break;
            case SENSING:
                // Subtle movements
                if (age % 60 == 0) {
                    jawTarget = 0.1f + random.nextFloat() * 0.1f;
                }
                break;
            case LURING:
                // Inviting open jaw
                jawTarget = 0.4f + MathHelper.sin(age * 0.05f) * 0.1f;
                break;
            case STALKING:
                // Tense, ready to strike
                jawTarget = 0.6f;
                break;
            case STRIKING:
                // Handled by snap attack goal
                break;
            case DIGESTING:
                // Chewing motion
                jawTarget = 0.1f + MathHelper.abs(MathHelper.sin(age * 0.1f)) * 0.2f;
                break;
            case SATISFIED:
                jawTarget = 0.05f;
                break;
        }
    }

    private void updateJawMovement() {
        float currentJaw = dataTracker.get(JAW_OPENNESS);
        if (Math.abs(currentJaw - jawTarget) > 0.01f) {
            float smoothSpeed = currentHuntingState == HuntingState.STRIKING ? 0.3f : JAW_SMOOTH_SPEED;
            jawCurrent = currentJaw + (jawTarget - currentJaw) * smoothSpeed;
            dataTracker.set(JAW_OPENNESS, jawCurrent);
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
            if (spitCd.value > 20) spitCd.value -= 20;
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
                // Occasional dust particles as it moves
                if (age % 10 == 0 && (this.getVelocity().lengthSquared() > 0.001)) {
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
            PlayerEntity nearest = this.getWorld().getClosestPlayer(this, 5.0);
            if (nearest != null && canSee(nearest)) {
                setActivated(true);
                setTarget(nearest);
                lastSeenTargetTicks = 0;
                if (currentHuntingState == HuntingState.DORMANT) {
                    setHuntingState(HuntingState.SENSING);
                }
            }
        }
        
        // Handle delayed deactivation
        if (isActivated() && !this.getWorld().isClient) {
            LivingEntity target = getTarget();
            if (target != null && target.isAlive() && canSee(target) && 
                this.squaredDistanceTo(target) <= 20.0 * 20.0) {
                lastSeenTargetTicks = 0;
            } else {
                PlayerEntity player = this.getWorld().getClosestPlayer(this, 20.0);
                if (player != null && canSee(player)) {
                    setTarget(player);
                    lastSeenTargetTicks = 0;
                } else {
                    lastSeenTargetTicks++;
                    if (lastSeenTargetTicks >= DEACTIVATION_DELAY_TICKS) {
                        setActivated(false);
                        setAttacking(false);
                        setSpitting(false);
                        setHuntingState(HuntingState.DORMANT);
                        lastSeenTargetTicks = 0;
                    }
                }
            }
        }
    }

    /* -------------------------------------------------------------------- */
    /*  ATTACK METHODS                                                      */
    /* -------------------------------------------------------------------- */

    private void performSpitAttack() {
        LivingEntity target = this.getTarget();
        if (target == null) return;

        if (!this.getWorld().isClient) {
            SpitBallEntity projectile = new SpitBallEntity(this.getWorld(), this);
            projectile.setPosition(this.getX(), this.getY() + 1.8, this.getZ());
            
            double d = target.getEyeY() - 0.5;
            double e = target.getX() - projectile.getX();
            double f = d - projectile.getY();
            double g = target.getZ() - projectile.getZ();
            float drag = MathHelper.sqrt((float)(e*e + g*g)) * 0.2F;

            projectile.setVelocity(e, f + drag, g, 1.6F, 4.0F);
            this.getWorld().spawnEntity(projectile);
        }

        this.getWorld().playSound(null, getX(), getY(), getZ(),
                SoundEvents.ENTITY_LLAMA_SPIT, SoundCategory.HOSTILE,
                1.0F, 0.8F + random.nextFloat() * 0.4F);

        spitCd.reset();
    }

    private void performSnapAttack(LivingEntity target) {
        if (target == null || !target.isAlive()) return;
        
        // Play snap sound
        this.getWorld().playSound(null, getX(), getY(), getZ(),
                SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.HOSTILE,
                1.5F, 0.5F);
        
        // Deal damage
        target.damage(this.getDamageSources().mobAttack(this), 12.0f);
        
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

    private class SnapTrapAttackGoal extends Goal {
        private final DjeserhathEntity host;
        private LivingEntity snapTarget;
        
        SnapTrapAttackGoal(DjeserhathEntity host) { 
            this.host = host;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override 
        public boolean canStart() {
            if (!host.snapCd.ready() || host.currentHuntingState != HuntingState.STALKING) {
                return false;
            }
            
            LivingEntity target = host.getTarget();
            if (target == null || !target.isAlive()) return false;
            
            double dist = target.squaredDistanceTo(host);
            return dist <= SNAP_ATTACK_RADIUS * SNAP_ATTACK_RADIUS && host.canSee(target);
        }

        @Override 
        public void start() {
            host.setHuntingState(HuntingState.STRIKING);
            snapTarget = host.getTarget();
            host.jawTarget = 1.0f; // Fully open
            host.setAttacking(true);
        }

        @Override 
        public boolean shouldContinue() { 
            return snapTarget != null && snapTarget.isAlive() && 
                   host.squaredDistanceTo(snapTarget) <= SNAP_ATTACK_RADIUS * SNAP_ATTACK_RADIUS * 1.5;
        }

        @Override 
        public void tick() {
            if (snapTarget != null) {
                host.getLookControl().lookAt(snapTarget);
                
                // Wait for jaw to open fully
                if (host.dataTracker.get(JAW_OPENNESS) > 0.8f) {
                    // SNAP!
                    host.jawTarget = 0.0f;
                    host.performSnapAttack(snapTarget);
                    stop();
                }
            }
        }
        
        @Override
        public void stop() {
            host.setAttacking(false);
            if (host.currentHuntingState == HuntingState.STRIKING) {
                host.setHuntingState(HuntingState.STALKING);
            }
            snapTarget = null;
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
                    host.jawTarget = 0.3f + random.nextFloat() * 0.2f;
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

        DjeserhathSpitAttackGoal(DjeserhathEntity host) { this.host = host; }

        @Override 
        public boolean canStart() {
            if (!host.isActivated() || !host.spitCd.ready() || 
                host.currentHuntingState == HuntingState.DIGESTING) {
                return false;
            }
            
            LivingEntity tgt = host.getTarget();
            return tgt != null && tgt.isAlive() &&
                   host.squaredDistanceTo(tgt) <= SPIT_RANGE * SPIT_RANGE &&
                   host.squaredDistanceTo(tgt) > SNAP_ATTACK_RADIUS * SNAP_ATTACK_RADIUS;
        }

        @Override 
        public void start() {
            host.spitWarmup = SPIT_WARMUP_TICKS;
            host.spitTarget = host.getTarget();
            host.setSpitting(true);
        }

        @Override 
        public boolean shouldContinue() { 
            return host.spitWarmup > 0; 
        }

        @Override 
        public void tick() {
            if (--host.spitWarmup <= 0 && host.spitTarget != null && host.spitTarget.isAlive()) {
                host.performSpitAttack();
                host.setSpitting(false);
            } else if (host.spitTarget != null) {
                host.getLookControl().lookAt(host.spitTarget);
                
                // Open jaw slightly for spit
                host.jawTarget = 0.5f;
            }
        }
        
        @Override
        public void stop() {
            host.setSpitting(false);
            host.spitTarget = null;
        }
    }

    private class DjeserhathMeleeAttackGoal extends MeleeAttackGoal {
        private final DjeserhathEntity host;
        private int attackTimer = 0;
        
        public DjeserhathMeleeAttackGoal(DjeserhathEntity entity, double speed, boolean pauseWhenMobIdle) {
            super(entity, speed, pauseWhenMobIdle);
            this.host = entity;
        }
        
        @Override
        public boolean canStart() {
            return host.isActivated() && 
                   host.currentHuntingState != HuntingState.DIGESTING &&
                   super.canStart();
        }
        
        @Override
        public void start() {
            super.start();
            host.setAttacking(true);
        }
        
        @Override
        public void stop() {
            super.stop();
            host.setAttacking(false);
            attackTimer = 0;
        }
        
        @Override
        protected void resetCooldown() {
            super.resetCooldown();
            attackTimer = 20;
            host.setAttacking(true);
        }
        
        @Override
        public void tick() {
            super.tick();
            
            if (attackTimer > 0) {
                attackTimer--;
            } else if (host.isAttacking()) {
                host.setAttacking(false);
            }
        }
    }

    /* -------------------------------------------------------------------- */
    /*  ANIMATION CONTROLLERS                                               */
    /* -------------------------------------------------------------------- */

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar r) {
        r.add(new AnimationController<>(this, "death", -1, this::deathPredicate));
        r.add(new AnimationController<>(this, "main", 0, this::mainPredicate));
        r.add(new AnimationController<>(this, "attack", 0, this::attackPredicate));
        r.add(new AnimationController<>(this, "spit", 0, this::spitPredicate));
        r.add(new AnimationController<>(this, "jaw", 0, this::jawPredicate));
    }

    private <T extends GeoAnimatable> PlayState mainPredicate(AnimationState<T> s) {
        if (isDying()) return PlayState.STOP;
        
        String targetAnim;
        
        // Choose animation based on hunting state
        switch (currentHuntingState) {
            case DORMANT:
                targetAnim = "animation.djeserhath.idle";
                break;
            case SENSING:
            case LURING:
            case STALKING:
            case SATISFIED:
                targetAnim = "animation.djeserhath.activated";
                break;
            case STRIKING:
                targetAnim = "animation.djeserhath.activated"; // Attack handled separately
                break;
            case DIGESTING:
                targetAnim = "animation.djeserhath.activated"; // Could add digest anim
                break;
            default:
                targetAnim = isActivated() ? "animation.djeserhath.activated" : "animation.djeserhath.idle";
        }

        if (!targetAnim.equals(currentMainAnimation)) {
            s.getController().setAnimation(RawAnimation.begin()
                    .then(targetAnim, Animation.LoopType.LOOP));
            currentMainAnimation = targetAnim;
        }
        return PlayState.CONTINUE;
    }

    private <T extends GeoAnimatable> PlayState attackPredicate(AnimationState<T> s) {
        if (isDying() || !isActivated()) return PlayState.STOP;
        
        if (isAttacking() && !currentlyPlayingAttack) {
            currentlyPlayingAttack = true;
            s.getController().setAnimation(RawAnimation.begin()
                    .then("animation.djeserhath.attack", Animation.LoopType.PLAY_ONCE));
            return PlayState.CONTINUE;
        }
        
        if (!isAttacking()) {
            currentlyPlayingAttack = false;
            return PlayState.STOP;
        }
        
        return PlayState.CONTINUE;
    }

    private <T extends GeoAnimatable> PlayState spitPredicate(AnimationState<T> s) {
        if (isDying() || !isActivated()) return PlayState.STOP;
        
        if (isSpitting() && !currentlyPlayingSpitAttack) {
            currentlyPlayingSpitAttack = true;
            s.getController().setAnimation(RawAnimation.begin()
                    .then("animation.djeserhath.spit_attack", Animation.LoopType.PLAY_ONCE));
            return PlayState.CONTINUE;
        }
        
        if (!isSpitting()) {
            currentlyPlayingSpitAttack = false;
            return PlayState.STOP;
        }
        
        return PlayState.CONTINUE;
    }

    private <T extends GeoAnimatable> PlayState jawPredicate(AnimationState<T> s) {
        // This could control jaw-specific animations based on JAW_OPENNESS
        // For now, we'll let the model renderer handle jaw rotation based on the data tracker
        return PlayState.STOP;
    }

    private <T extends GeoAnimatable> PlayState deathPredicate(AnimationState<T> s) {
        if (!isDying()) return PlayState.STOP;
        s.getController().forceAnimationReset();
        s.getController().setAnimation(RawAnimation.begin()
                .then("animation.djeserhath.death", Animation.LoopType.PLAY_ONCE));
        return PlayState.CONTINUE;
    }

    /* -------------------------------------------------------------------- */
    /*  SOUNDS & DAMAGE HANDLING                                            */
    /* -------------------------------------------------------------------- */

    @Override 
    protected SoundEvent getAmbientSound() { 
        switch (currentHuntingState) {
            case LURING:
                return SoundEvents.BLOCK_HONEY_BLOCK_STEP; // Sweet sound
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
            if (src.getAttacker() instanceof LivingEntity lv) {
                setTarget(lv);
                
                // Remember aggressive prey
                if (lv instanceof PlayerEntity) {
                    preyMemory.merge(lv.getUuid(), -1, Integer::sum);
                }
            }
        }
        
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
    }
}