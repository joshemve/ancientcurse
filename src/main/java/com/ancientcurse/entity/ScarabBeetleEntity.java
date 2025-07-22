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
 * Scarab Beetle Entity - A ground-based, spider-like creature with pinchers and defensive behavior.
 * Features wall-climbing abilities, aggressive melee attacks, and smooth leg animations.
 */
public class ScarabBeetleEntity extends SpiderEntity implements GeoEntity {
    
    /* ------------------ ENTITY CONSTANTS ------------------ */
    
    /* ---------- CONSTANTS ---------- */
    private static final int MAX_ATTACK_COOLDOWN = 30;
    private static final int TERRITORY_RADIUS = 16; // Entity territory radius
    // Animation and behavior constants // 10 seconds
    private static final int BURROW_TIME = 40; // 2 seconds
    private static final int EMERGE_TIME = 40; // 2 seconds
    private static final int MAX_AGGRESSION = 10; // Maximum aggression level
    
    // Animation state flags
    public boolean isAttacking = false;
    public boolean isDefensive = false;
    public boolean isBurrowing = false;
    public boolean isEmerging = false;
    public boolean isDigging = false;
    public boolean isUnderground = false;
    private boolean isGolden = false;
    
    // Timers
    private int burrowTicks = 0;
    private int emergeTicks = 0;
    private int diggingTicks = 0;
    private int attackCooldown = 0;
    private int defensiveStanceTicks = 0;
    private BlockPos burrowStartPos;
    
    // AI state tracking
    // Aggression tracking
    private int aggressionLevel = 0;
    private Vec3d homePosition;
    
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
        
        // 5% chance to spawn as golden scarab
        if (this.getRandom().nextFloat() < 0.05f) {
            this.isGolden = true;
            this.experiencePoints = 20;
            this.setHealth(this.getMaxHealth() * 1.5f);
        }
        
        // Set home position to spawn location
        this.homePosition = this.getPos();
        
        return entityData;
    }
    
    /* ---------- GOALS - SIMPLIFIED AND FIXED ---------- */
    @Override
    protected void initGoals() {
        this.goalSelector.clear(goal -> true);
        this.targetSelector.clear(goal -> true);
        
        // Simplified goal priority system
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new BurrowGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.add(3, new DefensiveGoal(this));
        this.goalSelector.add(4, new ScarabPatrolGoal(this, 0.6));
        this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(6, new LookAroundGoal(this));
        
        // Simplified targeting
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<PlayerEntity>(this, PlayerEntity.class, true));
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
        
        // Manage defensive stance
        if (defensiveStanceTicks > 0) {
            defensiveStanceTicks--;
            if (defensiveStanceTicks == 0) {
                isDefensive = false;
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
        
        // Manage digging animation
        if (diggingTicks > 0) {
            diggingTicks--;
            if (diggingTicks == 0) {
                isDigging = false;
            }
        }
    }
    
    /* ---------- COMBAT - SIMPLIFIED ---------- */
    public boolean tryAttack(LivingEntity target) {
        if (attackCooldown > 0) return false;
        
        attackCooldown = MAX_ATTACK_COOLDOWN;
        isAttacking = true;
        
        // Simple aggression increase
        increaseAggression(1);
        
        boolean attackSuccess = super.tryAttack(target);
        
        if (attackSuccess) {
            // Simplified effects
            if (this.random.nextFloat() < 0.3f) {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 60, 0));
            }
            
            this.playSound(SoundEvents.ENTITY_SPIDER_HURT, 1.0f, 0.8f + this.random.nextFloat() * 0.4f);
        }
        
        return attackSuccess;
    }
    
    @Override
    public boolean damage(DamageSource source, float amount) {
        // Simple aggression increase
        increaseAggression(2);
        
        // Enter defensive mode if health is low
        if (this.getHealth() / this.getMaxHealth() < 0.4f) {
            isDefensive = true;
            
            // Reduced burrow chance
            if (!isBurrowing && this.isOnGround() && this.random.nextFloat() < 0.15f) {
                startBurrowing();
            }
        }
        
        return super.damage(source, amount);
    }
    
    private void startBurrowing() {
        isBurrowing = true;
        burrowTicks = BURROW_TIME;
        burrowStartPos = this.getBlockPos();
        this.setNoGravity(true);
    }
    
    private void emerge() {
        isEmerging = true;
        emergeTicks = EMERGE_TIME;
        this.setNoGravity(false);
        
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
        int newAggression = Math.min(aggressionLevel + amount, MAX_AGGRESSION);
        aggressionLevel = newAggression;
        
        // Enter defensive mode at high aggression
        if (newAggression >= 7) {
            isDefensive = true;
        }
    }
    
    /* ---------- SIMPLIFIED AI GOALS ---------- */
    
    public static class BurrowGoal extends Goal {
        private final ScarabBeetleEntity beetle;
        
        public BurrowGoal(ScarabBeetleEntity beetle) {
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
    
    public static class DefensiveGoal extends Goal {
        private final ScarabBeetleEntity beetle;
        private int defensiveTicks = 0;
        
        public DefensiveGoal(ScarabBeetleEntity beetle) {
            this.beetle = beetle;
            this.setControls(EnumSet.of(Control.MOVE));
        }
        
        @Override
        public boolean canStart() {
            return beetle.isDefensive && beetle.getTarget() != null;
        }
        
        @Override
        public void start() {
            defensiveTicks = 40; // Shorter defensive period
        }
        
        @Override
        public boolean shouldContinue() {
            return defensiveTicks > 0 && beetle.isDefensive && beetle.getTarget() != null;
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
    
    public static class ScarabPatrolGoal extends WanderAroundGoal {
        private final ScarabBeetleEntity beetle;
        
        public ScarabPatrolGoal(ScarabBeetleEntity beetle, double speed) {
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
    
    /* ---------- GECKOLIB ANIMATION - UPDATED WITH NEW ANIMATIONS ---------- */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main_controller", 2, this::predicate));
    }
    
    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> state) {
        // Clean, simple animation controller
        
        if (this.isDead()) {
            state.getController().setAnimation(RawAnimation.begin()
                .then("animation.scarab_beetle.death", Animation.LoopType.HOLD_ON_LAST_FRAME));
            return PlayState.CONTINUE;
        }
        
        if (isAttacking) {
            state.getController().setAnimation(RawAnimation.begin()
                .then("animation.scarab_beetle.attack", Animation.LoopType.PLAY_ONCE));
            return PlayState.CONTINUE;
        }
        
        // Check for movement - use simple velocity check
        if (this.getVelocity().horizontalLengthSquared() > 0.01D) {
            state.getController().setAnimation(RawAnimation.begin()
                .then("animation.scarab_beetle.walk", Animation.LoopType.LOOP));
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
        nbt.putBoolean("IsDefensive", isDefensive);
        nbt.putBoolean("IsAttacking", isAttacking);
        nbt.putBoolean("IsBurrowing", isBurrowing);
        nbt.putBoolean("IsEmerging", isEmerging);
        nbt.putBoolean("IsDigging", isDigging);
        nbt.putBoolean("IsUnderground", isUnderground);
        nbt.putBoolean("IsGolden", isGolden);
        
        // Save timers
        nbt.putInt("BurrowTicks", burrowTicks);
        nbt.putInt("EmergeTicks", emergeTicks);
        nbt.putInt("DiggingTicks", diggingTicks);
        nbt.putInt("AttackCooldown", attackCooldown);
        nbt.putInt("DefensiveStanceTicks", defensiveStanceTicks);
        nbt.putInt("AggressionLevel", aggressionLevel);
        
        // Save home position if it exists
        if (homePosition != null) {
            nbt.putDouble("HomeX", homePosition.x);
            nbt.putDouble("HomeY", homePosition.y);
            nbt.putDouble("HomeZ", homePosition.z);
        }
    }
    
    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        
        // Load animation state
        isDefensive = nbt.getBoolean("IsDefensive");
        isAttacking = nbt.getBoolean("IsAttacking");
        isBurrowing = nbt.getBoolean("IsBurrowing");
        isEmerging = nbt.getBoolean("IsEmerging");
        isDigging = nbt.getBoolean("IsDigging");
        isUnderground = nbt.getBoolean("IsUnderground");
        isGolden = nbt.getBoolean("IsGolden");
        
        // Load timers
        burrowTicks = nbt.getInt("BurrowTicks");
        emergeTicks = nbt.getInt("EmergeTicks");
        diggingTicks = nbt.getInt("DiggingTicks");
        attackCooldown = nbt.getInt("AttackCooldown");
        defensiveStanceTicks = nbt.getInt("DefensiveStanceTicks");
        aggressionLevel = nbt.getInt("AggressionLevel");
        
        // Load home position if it exists
        if (nbt.contains("HomeX")) {
            homePosition = new Vec3d(
                nbt.getDouble("HomeX"),
                nbt.getDouble("HomeY"),
                nbt.getDouble("HomeZ")
            );
        }
    }
    
    /* ---------- GETTERS ---------- */
    public boolean isGolden() { return isGolden; }
    public boolean isBurrowing() { return isBurrowing; }
    public boolean isAttacking() { return isAttacking; }
    public boolean isDefensive() { return isDefensive; }
    public int getAggressionLevel() { return aggressionLevel; }
    public boolean isHighlyAggressive() { return getAggressionLevel() >= 7; }
    public Vec3d getHomePosition() { return homePosition != null ? homePosition : this.getPos(); }
    public boolean isInTerritory() { return getHomePosition().distanceTo(this.getPos()) <= TERRITORY_RADIUS; }
    
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
        
        // Always drop scarab shell components when killed
        if (causedByPlayer) {
            // 75% chance to drop a complete scarab shell
            if (this.getRandom().nextFloat() < 0.75f) {
                this.dropItem(Items.SCUTE); // Placeholder for SCARAB_SHELL
            } else {
                // Otherwise drop 1-3 shell fragments
                int fragmentCount = 1 + this.getRandom().nextInt(3);
                for (int i = 0; i < fragmentCount; i++) {
                    this.dropItem(Items.PRISMARINE_SHARD); // Placeholder for SCARAB_SHELL_FRAGMENT
                }
            }
        }
        
        // Golden scarabs drop additional valuable items
        if (isGolden && causedByPlayer) {
            int goldCount = 1 + this.getRandom().nextInt(2);
            for (int i = 0; i < goldCount; i++) {
                this.dropItem(Items.GOLD_NUGGET);
            }
            
            // Small chance to drop a gold ingot
            if (this.getRandom().nextFloat() < 0.15f) {
                this.dropItem(Items.GOLD_INGOT);
            }
            
            // Rare chance to drop a special item
            if (this.getRandom().nextFloat() < 0.05f) {
                this.dropItem(Items.GOLDEN_APPLE); // Placeholder for SCARAB_AMULET
            }
        }
    }
}