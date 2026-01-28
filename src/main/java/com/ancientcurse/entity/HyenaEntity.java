package com.ancientcurse.entity;

import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.EntityView;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Hyena Entity - A tameable wild canine found in desert biomes.
 *
 * Features:
 * - Can be tamed with bones (like wolves)
 * - Sits when tamed and right-clicked
 * - Multiple texture variants (default, sandy, rusty, red, leucistic, black)
 * - Smooth animation transitions between states
 *
 * Animation timings (from hyena.animation.json):
 * - hyena.walk: 1.1667s (23 ticks) - looping
 * - hyena.run: 0.6667s (13 ticks) - looping
 * - hyena.attack: 0.375s (8 ticks) - play once
 * - hyena.idle: 10.75s (215 ticks) - looping
 * - hyena.sit: 0.5s (10 ticks) - hold on last frame (transition to sitting)
 * - hyena.idle_sitting: 8.0s (160 ticks) - looping (while sitting)
 */
public class HyenaEntity extends TameableEntity implements GeoEntity {

    // Animation timing constants (derived from animation JSON)
    private static final int ATTACK_DURATION_TICKS = 8; // 0.375s * 20
    private static final int ATTACK_DAMAGE_TICK = 4; // Deal damage at ~0.2s into animation
    private static final int ATTACK_COOLDOWN_EXTRA = 5; // Extra ticks after animation for recovery
    private static final int SIT_TRANSITION_TICKS = 10; // 0.5s * 20

    // Tracked data for client/server sync
    private static final TrackedData<Boolean> ATTACKING = DataTracker.registerData(HyenaEntity.class,
            TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> ATTACK_COOLDOWN = DataTracker.registerData(HyenaEntity.class,
            TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(HyenaEntity.class,
            TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SIT_TRANSITION_TIMER = DataTracker.registerData(HyenaEntity.class,
            TrackedDataHandlerRegistry.INTEGER);

    // GeckoLib animation cache
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Texture variants
    public static final int VARIANT_DEFAULT = 0;
    public static final int VARIANT_SANDY = 1;
    public static final int VARIANT_RUSTY = 2;
    public static final int VARIANT_RED = 3;
    public static final int VARIANT_LEUCISTIC = 4;
    public static final int VARIANT_BLACK = 5;
    public static final int VARIANT_COUNT = 6;

    // Cached animations to prevent recreation every frame
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("hyena.idle");
    private static final RawAnimation ANIM_WALK = RawAnimation.begin().thenLoop("hyena.walk");
    private static final RawAnimation ANIM_RUN = RawAnimation.begin().thenLoop("hyena.run");
    private static final RawAnimation ANIM_ATTACK = RawAnimation.begin().thenPlayAndHold("hyena.attack");
    private static final RawAnimation ANIM_SIT_THEN_IDLE = RawAnimation.begin()
            .thenPlay("hyena.sit")
            .thenLoop("hyena.idle_sitting");
    private static final RawAnimation ANIM_IDLE_SITTING = RawAnimation.begin().thenLoop("hyena.idle_sitting");

    public HyenaEntity(EntityType<? extends TameableEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 5;
    }

    /* ---------- ATTRIBUTES ---------- */
    public static DefaultAttributeContainer.Builder createHyenaAttributes() {
        return TameableEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 16.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.32)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0)
                .add(EntityAttributes.GENERIC_ARMOR, 0.0);
    }

    /* ---------- INITIALIZATION ---------- */
    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(ATTACKING, false);
        this.dataTracker.startTracking(ATTACK_COOLDOWN, 0);
        this.dataTracker.startTracking(VARIANT, VARIANT_DEFAULT);
        this.dataTracker.startTracking(SIT_TRANSITION_TIMER, 0);
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
            EntityData entityData, NbtCompound entityNbt) {
        entityData = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);

        // Only assign random variant if NOT spawning from breeding (parents pass it
        // down)
        if (spawnReason != SpawnReason.BREEDING) {
            this.setVariant(this.random.nextInt(VARIANT_COUNT));
        }

        return entityData;
    }

    /* ---------- AI GOALS ---------- */
    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new SitGoal(this));
        this.goalSelector.add(2, new PounceAtTargetGoal(this, 0.4f));
        this.goalSelector.add(3, new HyenaAttackGoal(this, 1.2, false));
        this.goalSelector.add(4, new FollowOwnerGoal(this, 1.0, 10.0f, 2.0f, false));
        this.goalSelector.add(5, new AnimalMateGoal(this, 1.0));
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));

        this.targetSelector.add(1, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(2, new AttackWithOwnerGoal(this));
        this.targetSelector.add(3, new RevengeGoal(this).setGroupRevenge());
        // Wild hyenas attack players
        this.targetSelector.add(4, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false,
                entity -> !this.isTamed()));
    }

    /* ---------- TICK ---------- */
    @Override
    public void tick() {
        super.tick();

        // Server-side only: handle attack cooldown, damage timing, and state changes
        if (!this.getWorld().isClient) {
            // Handle attack cooldown and damage timing
            int cooldown = this.dataTracker.get(ATTACK_COOLDOWN);
            if (cooldown > 0) {
                // Apply damage at the correct animation frame
                // Cooldown starts at (ATTACK_DURATION_TICKS + ATTACK_COOLDOWN_EXTRA) = 13
                // Damage should be dealt ATTACK_DAMAGE_TICK (4) ticks into the animation
                // So damage is dealt when cooldown == (13 - 4) = 9
                int damageAtCooldown = ATTACK_DURATION_TICKS + ATTACK_COOLDOWN_EXTRA - ATTACK_DAMAGE_TICK;
                if (cooldown == damageAtCooldown && this.getTarget() != null) {
                    if (this.squaredDistanceTo(this.getTarget()) <= 6.25) { // 2.5 blocks squared
                        super.tryAttack(this.getTarget());
                    }
                }

                cooldown--;
                this.dataTracker.set(ATTACK_COOLDOWN, cooldown);
                if (cooldown == 0) {
                    this.dataTracker.set(ATTACKING, false);
                }
            }

            // Handle sit transition timer countdown
            int sitTimer = this.dataTracker.get(SIT_TRANSITION_TIMER);
            if (sitTimer > 0) {
                this.dataTracker.set(SIT_TRANSITION_TIMER, sitTimer - 1);
            }
        }
    }

    /* ---------- COMBAT ---------- */
    @Override
    public boolean tryAttack(net.minecraft.entity.Entity target) {
        if (this.dataTracker.get(ATTACK_COOLDOWN) > 0)
            return false;

        // Start attack animation
        this.dataTracker.set(ATTACK_COOLDOWN, ATTACK_DURATION_TICKS + ATTACK_COOLDOWN_EXTRA);
        this.dataTracker.set(ATTACKING, true);

        // Play attack sound
        this.playSound(SoundEvents.ENTITY_WOLF_GROWL, 1.0f, 0.9f + this.random.nextFloat() * 0.2f);

        return true;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }

        // Make sitting hyenas stand up when damaged
        if (this.isSitting()) {
            this.setSitting(false);
            this.dataTracker.set(SIT_TRANSITION_TIMER, 0); // Reset transition timer
        }

        return super.damage(source, amount);
    }

    /* ---------- TAMING & INTERACTION ---------- */
    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        Item item = itemStack.getItem();

        if (this.getWorld().isClient) {
            boolean canInteract = this.isOwner(player) || this.isTamed() || isBreedingItem(itemStack);
            return canInteract ? ActionResult.CONSUME : ActionResult.PASS;
        }

        // Taming with bones (like wolves)
        if (!this.isTamed() && item == Items.BONE) {
            if (!player.getAbilities().creativeMode) {
                itemStack.decrement(1);
            }

            // 33% chance to tame
            if (this.random.nextInt(3) == 0) {
                this.setOwner(player);
                this.navigation.stop();
                this.setTarget(null);
                this.setSitting(true);
                startSitTransition();
                this.getWorld().sendEntityStatus(this, (byte) 7); // Hearts particles
                this.playSound(SoundEvents.ENTITY_WOLF_WHINE, 1.0f, 1.0f);
            } else {
                this.getWorld().sendEntityStatus(this, (byte) 6); // Smoke particles
                this.playSound(SoundEvents.ENTITY_WOLF_GROWL, 0.8f, 0.8f);
            }

            return ActionResult.SUCCESS;
        }

        // Owner interactions
        if (this.isTamed() && this.isOwner(player)) {
            // Healing with meat (only if damaged)
            if (item == Items.COOKED_BEEF || item == Items.COOKED_PORKCHOP || item == Items.COOKED_MUTTON) {
                if (this.getHealth() < this.getMaxHealth()) {
                    if (!player.getAbilities().creativeMode) {
                        itemStack.decrement(1);
                    }
                    this.heal(6.0f);
                    this.playSound(SoundEvents.ENTITY_GENERIC_EAT, 1.0f, 1.0f);
                    return ActionResult.SUCCESS;
                }
            }

            // Toggle sitting (standard wolf behavior)
            // Works with empty hand or non-food item
            boolean newSitting = !this.isSitting();
            this.setSitting(newSitting);
            if (newSitting) {
                startSitTransition();
            } else {
                this.dataTracker.set(SIT_TRANSITION_TIMER, 0); // Reset transition timer when standing
            }
            this.jumping = false;
            this.navigation.stop();
            this.setTarget(null);
            return ActionResult.SUCCESS;
        }

        return super.interactMob(player, hand);
    }

    private void startSitTransition() {
        this.dataTracker.set(SIT_TRANSITION_TIMER, SIT_TRANSITION_TICKS);
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isOf(Items.COOKED_BEEF) || stack.isOf(Items.COOKED_PORKCHOP) || stack.isOf(Items.COOKED_MUTTON);
    }

    /* ---------- TAMED ATTRIBUTES ---------- */
    @Override
    public void setTamed(boolean tamed) {
        super.setTamed(tamed);
        if (tamed) {
            this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(24.0);
            this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(6.0);
            this.setHealth(24.0f);
        } else {
            this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(16.0);
            this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(4.0);
        }
    }

    /* ---------- NBT PERSISTENCE ---------- */
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Variant", this.getVariant());
        // Persist attack state for proper save/load handling
        nbt.putBoolean("IsAttacking", this.dataTracker.get(ATTACKING));
        nbt.putInt("AttackCooldown", this.dataTracker.get(ATTACK_COOLDOWN));
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.setVariant(nbt.getInt("Variant"));
        // Restore attack state
        this.dataTracker.set(ATTACKING, nbt.getBoolean("IsAttacking"));
        this.dataTracker.set(ATTACK_COOLDOWN, nbt.getInt("AttackCooldown"));
    }

    /* ---------- VARIANT GETTERS/SETTERS ---------- */
    public int getVariant() {
        return this.dataTracker.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.dataTracker.set(VARIANT, Math.max(0, Math.min(variant, VARIANT_COUNT - 1)));
    }

    public boolean isAttacking() {
        return this.dataTracker.get(ATTACKING);
    }

    /* ---------- GECKOLIB ANIMATION ---------- */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Use 5 tick transition time for smooth animation blending
        controllers.add(new AnimationController<>(this, "main_controller", 5, this::animationPredicate));
    }

    private <T extends GeoAnimatable> PlayState animationPredicate(AnimationState<T> state) {
        // Priority 1: Death animation
        if (this.isDead()) {
            return PlayState.STOP;
        }

        // Priority 2: Attack animation - check both flag AND cooldown for reliability
        boolean isAttacking = this.dataTracker.get(ATTACKING);
        int attackCooldown = this.dataTracker.get(ATTACK_COOLDOWN);
        if (isAttacking && attackCooldown > 0) {
            state.getController().setAnimation(ANIM_ATTACK);
            return PlayState.CONTINUE;
        }

        // Priority 3: Sitting animations (tamed only)
        // Always use ANIM_SIT_THEN_IDLE - GeckoLib will play sit once then loop
        // idle_sitting
        if (this.isTamed() && this.isSitting()) {
            state.getController().setAnimation(ANIM_SIT_THEN_IDLE);
            return PlayState.CONTINUE;
        }

        // Priority 4: Movement animations
        if (state.isMoving()) {
            double horizontalSpeed = Math
                    .sqrt(this.getVelocity().x * this.getVelocity().x + this.getVelocity().z * this.getVelocity().z);
            if (horizontalSpeed > 0.1) {
                state.getController().setAnimation(ANIM_RUN);
            } else {
                state.getController().setAnimation(ANIM_WALK);
            }
            return PlayState.CONTINUE;
        }

        // Priority 5: Default idle
        state.getController().setAnimation(ANIM_IDLE);
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    /* ---------- BREEDING ---------- */
    @Override
    public HyenaEntity createChild(ServerWorld world, PassiveEntity mate) {
        HyenaEntity baby = com.ancientcurse.ModEntities.HYENA.create(world);
        if (baby != null) {
            baby.setBaby(true); // Ensure they are born as babies

            // Inherit variant from one parent randomly
            if (mate instanceof HyenaEntity hyenaMate) {
                baby.setVariant(this.random.nextBoolean() ? this.getVariant() : hyenaMate.getVariant());
            }

            // Inherit owner if parents are tamed
            if (this.isTamed()) {
                baby.setOwnerUuid(this.getOwnerUuid());
                baby.setTamed(true);
            }
        }
        return baby;
    }

    /* ---------- SOUNDS ---------- */
    @Override
    protected SoundEvent getAmbientSound() {
        if (this.isTamed()) {
            return this.random.nextFloat() < 0.5f ? SoundEvents.ENTITY_WOLF_PANT : SoundEvents.ENTITY_WOLF_WHINE;
        }
        return SoundEvents.ENTITY_WOLF_GROWL;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.ENTITY_WOLF_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_WOLF_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.8f;
    }

    @Override
    public EntityView method_48926() {
        return this.getWorld();
    }

    /* ---------- CUSTOM ATTACK GOAL ---------- */
    public static class HyenaAttackGoal extends MeleeAttackGoal {
        private final HyenaEntity hyena;

        public HyenaAttackGoal(HyenaEntity hyena, double speed, boolean pauseWhenMobIdle) {
            super(hyena, speed, pauseWhenMobIdle);
            this.hyena = hyena;
        }

        @Override
        public boolean canStart() {
            return !hyena.isSitting() && super.canStart();
        }

        @Override
        public boolean shouldContinue() {
            return !hyena.isSitting() && super.shouldContinue();
        }
    }
}
