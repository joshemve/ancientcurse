package com.ancientcurse.entity;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.EntityView;
import java.util.UUID;

import java.util.EnumSet;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Scarab Beetle Entity - A tameable beetle companion.
 * Features wall-climbing abilities, melee attacks, burrowing behavior, and can be tamed with Golden Sycamore Figs.
 */
public class ScarabBeetleEntity extends TameableEntity implements GeoEntity {
    
    /* ------------------ ENTITY CONSTANTS ------------------ */
    
    /* ---------- CONSTANTS ---------- */
    private static final int MAX_ATTACK_COOLDOWN = 30;
    private static final int BURROW_TIME = 40; // 2 seconds (matches dig_down animation)
    private static final int EMERGE_TIME = 43; // ~2.16 seconds (matches dig_up animation)
    
    // Animation state flags
    public boolean isBurrowing = false;
    public boolean isEmerging = false;
    public boolean isUnderground = false;

    // Timers
    private int burrowTicks = 0;
    private int emergeTicks = 0;
    
    // Summoner tracking - UUID of the entity that summoned this beetle (e.g., Thoth)
    private UUID summonerUUID = null;

    // Tracked data for client/server sync
    private static final TrackedData<Boolean> SITTING = DataTracker.registerData(ScarabBeetleEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> CLIMBING = DataTracker.registerData(ScarabBeetleEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> ATTACKING = DataTracker.registerData(ScarabBeetleEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> ATTACK_COOLDOWN = DataTracker.registerData(ScarabBeetleEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> ATTACK_VARIANT = DataTracker.registerData(ScarabBeetleEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> SUMMONED = DataTracker.registerData(ScarabBeetleEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> LIFESPAN_TICKS = DataTracker.registerData(ScarabBeetleEntity.class, TrackedDataHandlerRegistry.INTEGER);
    
    // GeckoLib animation
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    public ScarabBeetleEntity(EntityType<? extends TameableEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 5;
        this.setTamed(false);
    }
    
    /* ---------- ATTRIBUTES ---------- */
    public static DefaultAttributeContainer.Builder createScarabBeetleAttributes() {
        return TameableEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 18.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 12.0) // Reduced from 20 to prevent long-range aggro
                .add(EntityAttributes.GENERIC_ARMOR, 2.0);
    }
    
    /* ---------- INITIALIZATION ---------- */
    public EntityData initialize(ServerWorld world, LocalDifficulty difficulty, SpawnReason spawnReason, 
                                 EntityData entityData, NbtCompound entityNbt) {
        entityData = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
        return entityData;
    }
    
    /* ---------- GOALS ---------- */
    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new SitGoal(this));
        this.goalSelector.add(2, new BurrowGoal(this));
        this.goalSelector.add(3, new FollowOwnerGoal(this, 1.0, 10.0f, 2.0f, false));
        this.goalSelector.add(4, new ScarabAttackGoal(this, 1.0, false));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.6));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(7, new LookAroundGoal(this));
        
        this.targetSelector.add(1, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(2, new AttackWithOwnerGoal(this));
        this.targetSelector.add(3, new RevengeGoal(this).setGroupRevenge());
        this.targetSelector.add(4, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false, entity -> !this.isTamed()));
    }
    
    /* ---------- TICK ---------- */
    @Override
    public void tick() {
        super.tick();

        age++;

        // Update climbing state for animations
        this.setClimbing(this.horizontalCollision && !this.isOnGround());

        // CRITICAL: Ensure gravity is always enabled unless actively underground
        // This prevents the beetle from accidentally flying if burrow sequence is interrupted
        if (!isUnderground && this.hasNoGravity()) {
            this.setNoGravity(false);
        }

        // Manage attack cooldown and apply damage at correct animation frame
        int cooldown = this.dataTracker.get(ATTACK_COOLDOWN);
        if (cooldown > 0) {
            // Apply damage at frame 10 (when animation shows bite/strike)
            if (cooldown == MAX_ATTACK_COOLDOWN - 10 && this.getTarget() != null) {
                LivingEntity target = this.getTarget();
                // Verify target is still in range before applying damage
                if (this.squaredDistanceTo(target) <= 4.0) {
                    super.tryAttack(target);
                }
            }

            cooldown--;
            this.dataTracker.set(ATTACK_COOLDOWN, cooldown);
            if (cooldown == 0) {
                this.dataTracker.set(ATTACKING, false);
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
        
        // Handle summoned beetle lifespan (3 minutes = 3600 ticks)
        if (this.dataTracker.get(SUMMONED)) {
            // Despawn immediately if difficulty is set to peaceful
            if (this.getWorld().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
                if (this.getWorld() instanceof ServerWorld serverWorld) {
                    serverWorld.spawnParticles(
                        ParticleTypes.POOF,
                        this.getX(), this.getY() + 0.5, this.getZ(),
                        10, 0.3, 0.3, 0.3, 0.1
                    );
                }
                this.discard();
                return;
            }
            
            // Despawn if summoner (Thoth) is dead or removed
            if (summonerUUID != null && !this.getWorld().isClient) {
                Entity summoner = ((ServerWorld)this.getWorld()).getEntity(summonerUUID);
                if (summoner == null || !summoner.isAlive()) {
                    // Summoner is dead/removed, despawn this beetle
                    if (this.getWorld() instanceof ServerWorld serverWorld) {
                        serverWorld.spawnParticles(
                            ParticleTypes.POOF,
                            this.getX(), this.getY() + 0.5, this.getZ(),
                            10, 0.3, 0.3, 0.3, 0.1
                        );
                    }
                    this.discard();
                    return;
                }
            }
            
            int lifespan = this.dataTracker.get(LIFESPAN_TICKS);
            if (lifespan > 0) {
                lifespan--;
                this.dataTracker.set(LIFESPAN_TICKS, lifespan);
                
                // Despawn when lifespan expires
                if (lifespan == 0) {
                    // Spawn poof particles
                    if (this.getWorld() instanceof ServerWorld serverWorld) {
                        serverWorld.spawnParticles(
                            ParticleTypes.POOF,
                            this.getX(), this.getY() + 0.5, this.getZ(),
                            10, 0.3, 0.3, 0.3, 0.1
                        );
                    }
                    this.discard(); // Remove the entity
                }
            }
        }
    }
    
    /* ---------- COMBAT - ANIMATION TRIGGERS ATTACK ---------- */
    @Override
    public boolean tryAttack(Entity target) {
        if (this.dataTracker.get(ATTACK_COOLDOWN) > 0) return false;

        // Set attack state using tracked data for client/server sync
        this.dataTracker.set(ATTACK_COOLDOWN, MAX_ATTACK_COOLDOWN);
        this.dataTracker.set(ATTACKING, true);

        // Choose attack animation variant ONCE at attack start (not every frame!)
        int variant = this.random.nextBoolean() ? 1 : 0;
        this.dataTracker.set(ATTACK_VARIANT, variant);

        // Play attack sound
        this.playSound(SoundEvents.ENTITY_SPIDER_HURT, 1.0f, 0.8f + this.random.nextFloat() * 0.4f);

        // NOTE: Damage will be applied after 10 ticks (0.5 seconds) in tick() method
        // This syncs damage with the animation bite/strike frame
        // We return true to tell MeleeAttackGoal the attack started successfully
        return true;
    }
    
    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }

        // Make sitting beetles stand up when damaged
        if (this.isSitting()) {
            this.setSitting(false);
        }

        // If beetle is damaged while underground or emerging, force it back to normal state
        if (isUnderground || isEmerging || isBurrowing) {
            isUnderground = false;
            isEmerging = false;
            isBurrowing = false;
            emergeTicks = 0;
            burrowTicks = 0;
            this.setNoGravity(false); // Critical: restore gravity immediately
        }

        // Chance to burrow when health is low (only if not tamed or owner is far away)
        if (this.getHealth() / this.getMaxHealth() < 0.3f && !isBurrowing && !isUnderground && this.isOnGround()) {
            if (!this.isTamed() || (this.getOwner() != null && this.squaredDistanceTo(this.getOwner()) > 144)) {
                if (this.random.nextFloat() < 0.25f) {
                    startBurrowing();
                }
            }
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
    
    /* ---------- CUSTOM GOALS ---------- */
    
    // Custom attack goal that respects sitting state
    public static class ScarabAttackGoal extends MeleeAttackGoal {
        private final ScarabBeetleEntity beetle;
        
        public ScarabAttackGoal(ScarabBeetleEntity beetle, double speed, boolean pauseWhenMobIdle) {
            super(beetle, speed, pauseWhenMobIdle);
            this.beetle = beetle;
        }
        
        @Override
        public boolean canStart() {
            return !beetle.isSitting() && super.canStart();
        }
        
        @Override
        public boolean shouldContinue() {
            return !beetle.isSitting() && super.shouldContinue();
        }
    }
    
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
        // Use 5 tick transition time for smoother animation changes
        controllers.add(new AnimationController<>(this, "main_controller", 5, this::predicate));
    }
    
    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> state) {
        // Animation controller with all animations

        if (this.isDead()) {
            // Ensure gravity is restored when dead to prevent floating corpses
            if (this.hasNoGravity()) {
                this.setNoGravity(false);
            }
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
        
        // Attack animations - Uses tracked data for client/server sync
        boolean isAttacking = this.dataTracker.get(ATTACKING);
        int attackCooldown = this.dataTracker.get(ATTACK_COOLDOWN);
        if (isAttacking && attackCooldown > 0) {
            // Use the variant chosen at attack start (prevents animation glitching)
            int variant = this.dataTracker.get(ATTACK_VARIANT);
            if (variant == 1) {
                state.getController().setAnimation(RawAnimation.begin()
                    .then("animation.scarab_beetle.attack2", Animation.LoopType.PLAY_ONCE));
            } else {
                state.getController().setAnimation(RawAnimation.begin()
                    .then("animation.scarab_beetle.attack", Animation.LoopType.PLAY_ONCE));
            }
            return PlayState.CONTINUE;
        }
        
        // Check for movement - use state.isMoving() which is more reliable
        if (state.isMoving()) {
            // Debug: log when walking animation should play
            if (this.age % 20 == 0 && !this.getWorld().isClient) {
                AncientCurse.LOGGER.debug("Scarab beetle walking - velocity: " + this.getVelocity().horizontalLengthSquared());
            }
            state.getController().setAnimation(RawAnimation.begin()
                .then("animation.scarab_beetle.walking", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }
        
        // Sitting animation (uses idle)
        if (this.isSitting()) {
            state.getController().setAnimation(RawAnimation.begin()
                .then("animation.scarab_beetle.idle", Animation.LoopType.LOOP));
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
        this.dataTracker.startTracking(SITTING, false);
        this.dataTracker.startTracking(CLIMBING, false);
        this.dataTracker.startTracking(ATTACKING, false);
        this.dataTracker.startTracking(ATTACK_COOLDOWN, 0);
        this.dataTracker.startTracking(ATTACK_VARIANT, 0);
        this.dataTracker.startTracking(SUMMONED, false);
        this.dataTracker.startTracking(LIFESPAN_TICKS, 0);
    }
    
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);

        // Save animation state
        nbt.putBoolean("IsAttacking", this.dataTracker.get(ATTACKING));
        nbt.putBoolean("IsBurrowing", isBurrowing);
        nbt.putBoolean("IsEmerging", isEmerging);
        nbt.putBoolean("IsUnderground", isUnderground);
        nbt.putBoolean("IsSitting", this.isSitting());

        // Save timers
        nbt.putInt("BurrowTicks", burrowTicks);
        nbt.putInt("EmergeTicks", emergeTicks);
        nbt.putInt("AttackCooldown", this.dataTracker.get(ATTACK_COOLDOWN));
        nbt.putInt("AttackAnimationVariant", this.dataTracker.get(ATTACK_VARIANT));
        
        // Save summoned state
        nbt.putBoolean("IsSummoned", this.dataTracker.get(SUMMONED));
        nbt.putInt("LifespanTicks", this.dataTracker.get(LIFESPAN_TICKS));
        if (summonerUUID != null) {
            nbt.putUuid("SummonerUUID", summonerUUID);
        }
    }
    
    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);

        // Load animation state
        this.dataTracker.set(ATTACKING, nbt.getBoolean("IsAttacking"));
        isBurrowing = nbt.getBoolean("IsBurrowing");
        isEmerging = nbt.getBoolean("IsEmerging");
        isUnderground = nbt.getBoolean("IsUnderground");
        this.setSitting(nbt.getBoolean("IsSitting"));

        // Load timers
        burrowTicks = nbt.getInt("BurrowTicks");
        emergeTicks = nbt.getInt("EmergeTicks");
        this.dataTracker.set(ATTACK_COOLDOWN, nbt.getInt("AttackCooldown"));
        this.dataTracker.set(ATTACK_VARIANT, nbt.getInt("AttackAnimationVariant"));
        
        // Load summoned state
        this.dataTracker.set(SUMMONED, nbt.getBoolean("IsSummoned"));
        this.dataTracker.set(LIFESPAN_TICKS, nbt.getInt("LifespanTicks"));
        if (nbt.containsUuid("SummonerUUID")) {
            summonerUUID = nbt.getUuid("SummonerUUID");
        }
    }
    
    /* ---------- INTERACTION ---------- */
    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        Item item = itemStack.getItem();
        
        if (this.getWorld().isClient) {
            boolean canInteract = this.isOwner(player) || this.isTamed() || isBreedingItem(itemStack);
            return canInteract ? ActionResult.CONSUME : ActionResult.PASS;
        }
        
        // Taming with Golden Sycamore Fig
        if (!this.isTamed() && item == ModItems.GOLDEN_SYCAMORE_FIG) {
            if (!player.getAbilities().creativeMode) {
                itemStack.decrement(1);
            }
            
            // 33% chance to tame
            if (this.random.nextInt(3) == 0) {
                this.setOwner(player);
                this.navigation.stop();
                this.setTarget(null);
                this.setSitting(true);
                this.getWorld().sendEntityStatus(this, (byte)7); // Hearts particles
                
                // Play taming sound
                this.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            } else {
                this.getWorld().sendEntityStatus(this, (byte)6); // Smoke particles
                this.playSound(SoundEvents.ENTITY_SPIDER_AMBIENT, 1.0f, 0.8f);
            }
            
            return ActionResult.SUCCESS;
        }
        
        // Owner interactions
        if (this.isTamed() && this.isOwner(player)) {
            // Healing with regular Sycamore Figs
            if (item == ModItems.SYCAMORE_FIG && this.getHealth() < this.getMaxHealth()) {
                if (!player.getAbilities().creativeMode) {
                    itemStack.decrement(1);
                }
                this.heal(4.0f);
                this.playSound(SoundEvents.ENTITY_GENERIC_EAT, 1.0f, 1.0f);
                return ActionResult.SUCCESS;
            }
            
            // Toggle sitting
            if (!this.isBreedingItem(itemStack)) {
                this.setSitting(!this.isSitting());
                this.jumping = false;
                this.navigation.stop();
                this.setTarget(null);
                return ActionResult.SUCCESS;
            }
        }
        
        return super.interactMob(player, hand);
    }
    
    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isOf(ModItems.SYCAMORE_FIG);
    }
    
    /* ---------- PET FEATURES ---------- */
    public boolean isSitting() {
        return this.dataTracker.get(SITTING);
    }
    
    public void setSitting(boolean sitting) {
        this.dataTracker.set(SITTING, sitting);
    }
    
    public boolean isClimbing() {
        return this.dataTracker.get(CLIMBING);
    }
    
    public void setClimbing(boolean climbing) {
        this.dataTracker.set(CLIMBING, climbing);
    }
    
    @Override
    public boolean canBeLeashedBy(PlayerEntity player) {
        return this.isTamed() && super.canBeLeashedBy(player);
    }
    
    @Override
    public boolean canAttackWithOwner(LivingEntity target, LivingEntity owner) {
        return !(target instanceof TameableEntity) || !((TameableEntity)target).isTamed();
    }
    
    @Override
    public void setTamed(boolean tamed) {
        super.setTamed(tamed);
        if (tamed) {
            this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(30.0);
            this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(6.0);
            this.setHealth(30.0f);
        } else {
            this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(18.0);
            this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(4.0);
        }
    }
    
    /* ---------- GETTERS ---------- */
    public boolean isBurrowing() { return isBurrowing; }
    public boolean isAttacking() { return this.dataTracker.get(ATTACKING); }
    
    /**
     * Mark this beetle as summoned by Thoth with a 3-minute lifespan
     * @param summonerUUID The UUID of the entity that summoned this beetle (e.g., Thoth)
     */
    public void setSummoned(UUID summonerUUID) {
        this.dataTracker.set(SUMMONED, true);
        this.dataTracker.set(LIFESPAN_TICKS, 3600); // 3 minutes = 180 seconds = 3600 ticks
        this.summonerUUID = summonerUUID;
    }
    
    @Override
    public ScarabBeetleEntity createChild(ServerWorld world, PassiveEntity mate) {
        return null; // Scarab beetles don't breed for now
    }
    
    /**
     * Used by the model to determine if walking animation should play
     * Uses same threshold as animation controller
     */
    public boolean isMoving() {
        // Match animation controller threshold
        return this.getVelocity().horizontalLengthSquared() > 0.01D;
    }
    
    @Override
    public EntityView method_48926() {
        return this.getWorld();
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