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
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import com.ancientcurse.ModItems;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.SpawnReason;
import net.minecraft.block.BlockState;
import java.util.List;
import java.util.EnumSet;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Scarab Beetle Entity - A ground-based, spider-like creature with pinchers and defensive behavior.
 * Features wall-climbing abilities, aggressive melee attacks, and smooth leg animations.
 */
public class ScarabBeetleEntity extends SpiderEntity implements GeoEntity {
    
    /* ---------- DATA TRACKERS ---------- */
    private static final TrackedData<Boolean> ATTACKING = 
            DataTracker.registerData(ScarabBeetleEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> ATTACK_COOLDOWN = 
            DataTracker.registerData(ScarabBeetleEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> AGGRESSION_LEVEL = 
            DataTracker.registerData(ScarabBeetleEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> DEFENSIVE_MODE = 
            DataTracker.registerData(ScarabBeetleEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IS_GOLDEN = 
            DataTracker.registerData(ScarabBeetleEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IS_BURROWING = 
            DataTracker.registerData(ScarabBeetleEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Float> LEG_SWING_AMOUNT = 
            DataTracker.registerData(ScarabBeetleEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> LEG_SWING_SPEED = 
            DataTracker.registerData(ScarabBeetleEntity.class, TrackedDataHandlerRegistry.FLOAT);
    
    /* ---------- CONSTANTS ---------- */
    private static final int MAX_ATTACK_COOLDOWN = 30;
    private static final int TERRITORY_RADIUS = 16;
    private static final int CALL_FOR_HELP_RADIUS = 12;
    private static final int MAX_AGGRESSION = 10;
    private static final int BURROWING_DEPTH = 3;
    
    /* ---------- FIELDS ---------- */
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int swingTicks;
    private int territoryCheckTicks = 0;
    private int lastCallForHelpTicks = 0;
    private int aggressionDecayTicks = 0;
    private int particleEffectTicks = 0;
    private Vec3d homePosition;
    
    // Burrowing mechanics
    private boolean isBurrowing = false;
    private int burrowTicks = 0;
    private BlockPos burrowStartPos;
    
    // Smooth leg movement - fixed synchronization issues
    private float legSwingProgress = 0.0f;
    private float prevLegSwingProgress = 0.0f;
    private float limbSwingAmount = 0.0f;
    private float prevLimbSwingAmount = 0.0f;
    private Vec3d prevPosition;
    private boolean wasMoving = false;
    
    // AI state tracking
    private int idleTicks = 0;
    private boolean isPatrolling = false;
    private LivingEntity previousTarget = null;
    
    public int lastDamageTime = 0;
    
    public ScarabBeetleEntity(EntityType<? extends SpiderEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 5;
        this.homePosition = this.getPos();
        this.prevPosition = this.getPos();
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
        
        // 5% chance to spawn as golden scarab
        if (this.random.nextFloat() < 0.05f) {
            this.dataTracker.set(IS_GOLDEN, true);
            this.experiencePoints = 20;
            this.setHealth(this.getMaxHealth() * 1.5f);
        }
        
        return entityData;
    }
    
    /* ---------- GOALS - SIMPLIFIED AND FIXED ---------- */
    @Override
    protected void initGoals() {
        this.goalSelector.clear(goal -> true);
        this.targetSelector.clear(goal -> true);
        
        // Simplified goal priority system
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new ScarabBurrowGoal(this));
        this.goalSelector.add(2, new ScarabMeleeAttackGoal(this, 1.0, false));
        this.goalSelector.add(3, new ScarabDefensiveGoal(this));
        this.goalSelector.add(4, new ScarabWanderGoal(this, 0.6));
        this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(6, new LookAroundGoal(this));
        
        // Simplified targeting
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ScarabHuntGoal(this, PlayerEntity.class, true));
    }
    
    /* ---------- TICK - OPTIMIZED ---------- */
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
        
        // Server-side behaviors
        if (!this.getWorld().isClient) {
            handleAggressionSystem();
            handleTerritorialBehavior();
            
            // Less frequent particle/sound updates
            if (++particleEffectTicks >= 40) { // Every 2 seconds instead of 1
                particleEffectTicks = 0;
                handleParticleEffects();
                handleSoundEffects();
            }
            
            handleBurrowing();
        }
        
        // Fixed leg movement system
        updateLegMovement();
    }
    
    /**
     * Fixed leg movement system with proper synchronization
     */
    private void updateLegMovement() {
        // Store previous values for interpolation
        prevLegSwingProgress = legSwingProgress;
        prevLimbSwingAmount = limbSwingAmount;
        
        Vec3d currentPos = this.getPos();
        Vec3d movement = currentPos.subtract(prevPosition);
        float speed = (float)movement.horizontalLength();
        
        // Determine if entity is actually moving
        boolean isMoving = speed > 0.02f;
        
        if (isMoving) {
            // Calculate swing progress based on distance traveled
            legSwingProgress += speed * 8.0f; // Adjust multiplier for desired leg speed
            
            // Smooth swing amount transition
            float targetSwingAmount = MathHelper.clamp(speed * 15.0f, 0.0f, 1.0f);
            limbSwingAmount = MathHelper.lerp(0.3f, limbSwingAmount, targetSwingAmount);
            
            idleTicks = 0;
        } else {
            // Gradual slowdown when stopping
            limbSwingAmount = MathHelper.lerp(0.1f, limbSwingAmount, 0.0f);
            idleTicks++;
            
            // Idle breathing effect
            if (idleTicks > 20) {
                legSwingProgress += 0.02f; // Very slow idle movement
            }
        }
        
        // Update data tracker for renderer
        dataTracker.set(LEG_SWING_AMOUNT, limbSwingAmount);
        dataTracker.set(LEG_SWING_SPEED, isMoving ? 1.0f : 0.2f);
        
        // Store position for next tick
        prevPosition = currentPos;
        wasMoving = isMoving;
    }
    
    /**
     * Simplified aggression system
     */
    private void handleAggressionSystem() {
        if (++aggressionDecayTicks >= 100) { // Every 5 seconds
            aggressionDecayTicks = 0;
            int currentAggression = dataTracker.get(AGGRESSION_LEVEL);
            if (currentAggression > 0) {
                dataTracker.set(AGGRESSION_LEVEL, currentAggression - 1);
            }
            
            // Exit defensive mode if aggression is low
            if (currentAggression <= 3) {
                dataTracker.set(DEFENSIVE_MODE, false);
            }
        }
    }
    
    /**
     * Simplified territorial behavior
     */
    private void handleTerritorialBehavior() {
        if (++territoryCheckTicks >= 200) { // Every 10 seconds
            territoryCheckTicks = 0;
            
            // Reset home position if too far away
            if (homePosition.distanceTo(this.getPos()) > TERRITORY_RADIUS * 2) {
                homePosition = this.getPos();
            }
        }
    }
    
    /**
     * Reduced particle effects
     */
    private void handleParticleEffects() {
        ServerWorld serverWorld = (ServerWorld) this.getWorld();
        
        // Only golden particles for golden scarabs
        if (dataTracker.get(IS_GOLDEN) && this.random.nextFloat() < 0.3f) {
            serverWorld.spawnParticles(ParticleTypes.WAX_OFF,
                this.getX(), this.getY() + 0.5, this.getZ(),
                1, 0.2, 0.1, 0.2, 0.0);
        }
        
        // Defensive particles only when actively defensive
        if (dataTracker.get(DEFENSIVE_MODE) && this.getTarget() != null) {
            serverWorld.spawnParticles(ParticleTypes.POOF,
                this.getX(), this.getY() + 0.1, this.getZ(),
                2, 0.3, 0.1, 0.3, 0.02);
        }
    }
    
    /**
     * Reduced sound effects
     */
    private void handleSoundEffects() {
        // Only play sounds occasionally
        if (this.random.nextInt(200) == 0) {
            if (dataTracker.get(IS_GOLDEN)) {
                this.playSound(SoundEvents.BLOCK_BELL_RESONATE, 0.2f, 2.0f);
            } else if (dataTracker.get(AGGRESSION_LEVEL) >= 6) {
                this.playSound(SoundEvents.ENTITY_SPIDER_HURT, 0.4f, 1.3f);
            }
        }
    }
    
    private void handleBurrowing() {
        if (isBurrowing) {
            burrowTicks++;
            
            // Dig particles less frequently
            if (burrowTicks % 10 == 0 && this.getWorld() instanceof ServerWorld serverWorld) {
                BlockPos below = this.getBlockPos().down();
                serverWorld.spawnParticles(
                    new BlockStateParticleEffect(ParticleTypes.BLOCK, 
                        getWorld().getBlockState(below)),
                    this.getX(), this.getY(), this.getZ(),
                    5, 0.2, 0.1, 0.2, 0.05
                );
            }
            
            // Gradual sinking
            if (burrowTicks < 30) {
                this.setPosition(getX(), getY() - 0.03, getZ());
            }
            
            // Emerge after burrowing
            if (burrowTicks > 80) {
                emerge();
            }
        }
    }
    
    /* ---------- COMBAT - SIMPLIFIED ---------- */
    public boolean tryAttack(LivingEntity target) {
        if (dataTracker.get(ATTACK_COOLDOWN) > 0) return false;
        
        dataTracker.set(ATTACKING, true);
        dataTracker.set(ATTACK_COOLDOWN, MAX_ATTACK_COOLDOWN);
        swingTicks = 15;
        
        // Simple aggression increase
        increaseAggression(1);
        
        boolean attackSuccess = super.tryAttack(target);
        
        if (attackSuccess) {
            // Simplified effects
            if (dataTracker.get(IS_GOLDEN) && target instanceof PlayerEntity player) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 60, 0));
            }
            
            this.playSound(SoundEvents.ENTITY_SPIDER_HURT, 1.0f, 0.8f + this.random.nextFloat() * 0.4f);
        }
        
        return attackSuccess;
    }
    
    @Override
    public boolean damage(DamageSource source, float amount) {
        this.lastDamageTime = this.age;
        
        // Golden scarabs have damage reduction
        if (dataTracker.get(IS_GOLDEN)) {
            amount *= 0.8f;
        }
        
        // Simple aggression increase
        increaseAggression(2);
        
        // Enter defensive mode if health is low
        if (this.getHealth() / this.getMaxHealth() < 0.4f) {
            dataTracker.set(DEFENSIVE_MODE, true);
            
            // Reduced burrow chance
            if (!isBurrowing && this.isOnGround() && this.random.nextFloat() < 0.15f) {
                startBurrowing();
            }
        }
        
        return super.damage(source, amount);
    }
    
    private void startBurrowing() {
        isBurrowing = true;
        burrowTicks = 0;
        burrowStartPos = this.getBlockPos();
        this.setNoGravity(true);
        dataTracker.set(IS_BURROWING, true);
        this.playSound(SoundEvents.BLOCK_GRAVEL_BREAK, 0.8f, 1.0f);
    }
    
    private void emerge() {
        isBurrowing = false;
        this.setNoGravity(false);
        dataTracker.set(IS_BURROWING, false);
        
        // Simple emergence positioning
        BlockPos emergePos = burrowStartPos.add(
            random.nextInt(3) - 1,
            0,
            random.nextInt(3) - 1
        );
        this.setPosition(emergePos.getX() + 0.5, emergePos.getY(), emergePos.getZ() + 0.5);
        
        // Emergence effects
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.POOF,
                this.getX(), this.getY(), this.getZ(),
                8, 0.4, 0.2, 0.4, 0.1);
        }
        
        this.playSound(SoundEvents.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.6f, 1.4f);
    }
    
    private void increaseAggression(int amount) {
        int currentAggression = dataTracker.get(AGGRESSION_LEVEL);
        int newAggression = Math.min(currentAggression + amount, MAX_AGGRESSION);
        dataTracker.set(AGGRESSION_LEVEL, newAggression);
        
        // Enter defensive mode at high aggression
        if (newAggression >= 7) {
            dataTracker.set(DEFENSIVE_MODE, true);
        }
    }
    
    /* ---------- SIMPLIFIED AI GOALS ---------- */
    
    public static class ScarabBurrowGoal extends Goal {
        private final ScarabBeetleEntity beetle;
        
        public ScarabBurrowGoal(ScarabBeetleEntity beetle) {
            this.beetle = beetle;
            this.setControls(EnumSet.of(Control.MOVE, Control.JUMP));
        }
        
        @Override
        public boolean canStart() {
            return beetle.getHealth() < 6 && beetle.isOnGround() && 
                   !beetle.isBurrowing && beetle.random.nextInt(60) == 0;
        }
        
        @Override
        public void start() {
            beetle.startBurrowing();
        }
        
        @Override
        public boolean shouldContinue() {
            return beetle.isBurrowing;
        }
    }
    
    public static class ScarabMeleeAttackGoal extends MeleeAttackGoal {
        private final ScarabBeetleEntity beetle;
        
        public ScarabMeleeAttackGoal(ScarabBeetleEntity beetle, double speed, boolean pauseWhenMobIdle) {
            super(beetle, speed, pauseWhenMobIdle);
            this.beetle = beetle;
        }
        
        @Override
        protected double getSquaredMaxAttackDistance(LivingEntity entity) {
            return super.getSquaredMaxAttackDistance(entity) * 1.2;
        }
    }
    
    public static class ScarabDefensiveGoal extends Goal {
        private final ScarabBeetleEntity beetle;
        private int defensiveTicks = 0;
        
        public ScarabDefensiveGoal(ScarabBeetleEntity beetle) {
            this.beetle = beetle;
            this.setControls(EnumSet.of(Control.MOVE));
        }
        
        @Override
        public boolean canStart() {
            return beetle.isDefensive() && beetle.getTarget() != null;
        }
        
        @Override
        public void start() {
            defensiveTicks = 40; // Shorter defensive period
        }
        
        @Override
        public boolean shouldContinue() {
            return defensiveTicks > 0 && beetle.isDefensive() && beetle.getTarget() != null;
        }
        
        @Override
        public void tick() {
            defensiveTicks--;
            
            // Simple backing away behavior
            if (beetle.getTarget() != null && beetle.squaredDistanceTo(beetle.getTarget()) < 9) {
                Vec3d awayVector = beetle.getPos().subtract(beetle.getTarget().getPos()).normalize();
                Vec3d targetPos = beetle.getPos().add(awayVector.multiply(2));
                beetle.getNavigation().startMovingTo(targetPos.x, targetPos.y, targetPos.z, 0.8);
            }
        }
    }
    
    public static class ScarabWanderGoal extends WanderAroundGoal {
        private final ScarabBeetleEntity beetle;
        
        public ScarabWanderGoal(ScarabBeetleEntity beetle, double speed) {
            super(beetle, speed);
            this.beetle = beetle;
        }
        
        @Override
        public boolean canStart() {
            return super.canStart() && beetle.getTarget() == null && !beetle.isDefensive();
        }
        
        @Override
        protected Vec3d getWanderTarget() {
            Vec3d target = super.getWanderTarget();
            
            // Stay near home territory
            if (target != null && beetle.getHomePosition().distanceTo(target) > TERRITORY_RADIUS) {
                Vec3d home = beetle.getHomePosition();
                return new Vec3d(
                    home.x + (beetle.random.nextDouble() - 0.5) * TERRITORY_RADIUS,
                    home.y,
                    home.z + (beetle.random.nextDouble() - 0.5) * TERRITORY_RADIUS
                );
            }
            
            return target;
        }
    }
    
    public static class ScarabHuntGoal extends ActiveTargetGoal<PlayerEntity> {
        private final ScarabBeetleEntity beetle;
        
        public ScarabHuntGoal(ScarabBeetleEntity beetle, Class<PlayerEntity> targetClass, boolean checkVisibility) {
            super(beetle, targetClass, checkVisibility);
            this.beetle = beetle;
        }
        
        @Override
        public boolean canStart() {
            // Only target players if moderately aggressive or provoked
            return super.canStart() && (beetle.getAggressionLevel() >= 4 || beetle.getLastAttacker() != null);
        }
        
        @Override
        public void start() {
            super.start();
            beetle.increaseAggression(1);
        }
    }
    
    /* ---------- GECKOLIB ANIMATION - SIMPLIFIED ---------- */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main_controller", 2, this::predicate));
    }
    
    private <T extends GeoEntity> PlayState predicate(AnimationState<T> state) {
        if (dataTracker.get(IS_BURROWING)) {
            state.getController().setAnimation(RawAnimation.begin()
                .then("animation.scarab_beetle.burrow", Animation.LoopType.PLAY_ONCE));
        } else if (dataTracker.get(ATTACKING)) {
            state.getController().setAnimation(RawAnimation.begin()
                .then("animation.scarab_beetle.attack", Animation.LoopType.PLAY_ONCE));
        } else if (dataTracker.get(DEFENSIVE_MODE)) {
            state.getController().setAnimation(RawAnimation.begin()
                .then("animation.scarab_beetle.defensive", Animation.LoopType.LOOP));
        } else if (state.isMoving() && dataTracker.get(LEG_SWING_AMOUNT) > 0.1f) {
            // Smooth animation speed based on movement
            float speed = 0.8f + dataTracker.get(LEG_SWING_AMOUNT) * 0.8f;
            state.getController().setAnimationSpeed(speed);
            state.getController().setAnimation(RawAnimation.begin()
                .then("animation.scarab_beetle.walk", Animation.LoopType.LOOP));
        } else {
            state.getController().setAnimation(RawAnimation.begin()
                .then("animation.scarab_beetle.idle", Animation.LoopType.LOOP));
        }
        return PlayState.CONTINUE;
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
    
    /* ---------- DATA TRACKING ---------- */
    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(ATTACKING, false);
        this.dataTracker.startTracking(ATTACK_COOLDOWN, 0);
        this.dataTracker.startTracking(AGGRESSION_LEVEL, 0);
        this.dataTracker.startTracking(DEFENSIVE_MODE, false);
        this.dataTracker.startTracking(IS_GOLDEN, false);
        this.dataTracker.startTracking(IS_BURROWING, false);
        this.dataTracker.startTracking(LEG_SWING_AMOUNT, 0.0f);
        this.dataTracker.startTracking(LEG_SWING_SPEED, 1.0f);
    }
    
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("AggressionLevel", this.dataTracker.get(AGGRESSION_LEVEL));
        nbt.putBoolean("DefensiveMode", this.dataTracker.get(DEFENSIVE_MODE));
        nbt.putBoolean("IsGolden", this.dataTracker.get(IS_GOLDEN));
        nbt.putBoolean("IsBurrowing", this.isBurrowing);
        nbt.putInt("BurrowTicks", this.burrowTicks);
        if (homePosition != null) {
            nbt.putDouble("HomeX", homePosition.x);
            nbt.putDouble("HomeY", homePosition.y);
            nbt.putDouble("HomeZ", homePosition.z);
        }
    }
    
    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.dataTracker.set(AGGRESSION_LEVEL, nbt.getInt("AggressionLevel"));
        this.dataTracker.set(DEFENSIVE_MODE, nbt.getBoolean("DefensiveMode"));
        this.dataTracker.set(IS_GOLDEN, nbt.getBoolean("IsGolden"));
        this.isBurrowing = nbt.getBoolean("IsBurrowing");
        this.burrowTicks = nbt.getInt("BurrowTicks");
        if (nbt.contains("HomeX")) {
            this.homePosition = new Vec3d(
                nbt.getDouble("HomeX"),
                nbt.getDouble("HomeY"),
                nbt.getDouble("HomeZ")
            );
        }
    }
    
    /* ---------- GETTERS ---------- */
    public boolean isGolden() { return dataTracker.get(IS_GOLDEN); }
    public boolean isBurrowing() { return dataTracker.get(IS_BURROWING); }
    public boolean isAttacking() { return dataTracker.get(ATTACKING); }
    public boolean isDefensive() { return dataTracker.get(DEFENSIVE_MODE); }
    public float getLegSwingAmount() { return dataTracker.get(LEG_SWING_AMOUNT); }
    public float getLegSwingSpeed() { return dataTracker.get(LEG_SWING_SPEED); }
    public int getAggressionLevel() { return dataTracker.get(AGGRESSION_LEVEL); }
    public boolean isHighlyAggressive() { return getAggressionLevel() >= 7; }
    public Vec3d getHomePosition() { return homePosition != null ? homePosition : this.getPos(); }
    public boolean isInTerritory() { return getHomePosition().distanceTo(this.getPos()) <= TERRITORY_RADIUS; }
    
    // For smooth leg interpolation in renderer
    public float getLegSwingProgress(float tickDelta) {
        return MathHelper.lerp(tickDelta, prevLegSwingProgress, legSwingProgress);
    }
    
    public float getLimbSwingAmount(float tickDelta) {
        return MathHelper.lerp(tickDelta, prevLimbSwingAmount, limbSwingAmount);
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
        
        // Always drop scarab shell components when killed
        if (causedByPlayer) {
            // 75% chance to drop a complete scarab shell
            if (this.random.nextFloat() < 0.75f) {
                this.dropItem(ModItems.SCARAB_SHELL);
            } else {
                // Otherwise drop 1-3 shell fragments
                int fragmentCount = 1 + this.random.nextInt(3);
                for (int i = 0; i < fragmentCount; i++) {
                    this.dropItem(ModItems.SCARAB_SHELL_FRAGMENT);
                }
            }
        }
        
        // Golden scarabs drop additional valuable items
        if (dataTracker.get(IS_GOLDEN) && causedByPlayer) {
            int goldCount = 1 + this.random.nextInt(2);
            for (int i = 0; i < goldCount; i++) {
                this.dropItem(Items.GOLD_NUGGET);
            }
            
            if (this.random.nextFloat() < 0.15f) {
                this.dropItem(Items.GOLD_INGOT);
            }
            
            // Golden scarabs have a higher chance of dropping pristine shells
            if (this.random.nextFloat() < 0.4f) {
                this.dropItem(ModItems.SCARAB_SHELL); // Extra shell for golden variants
            }
        }
    }
}