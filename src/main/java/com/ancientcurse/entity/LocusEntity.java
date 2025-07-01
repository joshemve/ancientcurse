package com.ancientcurse.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

/**
 * Full drop‑in replacement for LocusEntity.
 *
 * Key behavioural fixes:
 * 1. **Constant hover ring** around target at configurable radius – keeps bugs in camera view.
 * 2. **Dive‑bomb attack** instead of plain MeleeAttackGoal – improves hit reliability in 3‑D.
 * 3. Replaced per‑tick velocity jitter with goal‑driven erratic darts – no path‑finding thrash.
 * 4. Navigation uses BirdNavigation with "no walls" to avoid ceiling bumps.
 */
public class LocusEntity extends HostileEntity implements GeoEntity {
    /* ---------- DATA TRACKERS ---------- */
    private static final TrackedData<Boolean> ATTACKING =
            DataTracker.registerData(LocusEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> ATTACK_COOLDOWN =
            DataTracker.registerData(LocusEntity.class, TrackedDataHandlerRegistry.INTEGER);

    /* ---------- CONSTANTS ---------- */
    private static final int MAX_ATTACK_COOLDOWN = 20;          // 1 s between dives
    private static final float ORBIT_RADIUS = 4.0F;             // metres
    private static final int ORBIT_RECALC_TICKS = 10;           // 0.5 s

    /* ---------- FIELDS ---------- */
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int swingTicks;
    private Vec3d orbitPos = Vec3d.ZERO;
    
    /* ---------- DAMAGE TRACKING ---------- */
    public int lastDamageTime = 0; // Tracks when entity last took damage for renderer effects

    public LocusEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.setNoGravity(true);
        this.moveControl = new FlightMoveControl(this, 24, true);
        this.experiencePoints = 0; // no farm loot
    }

    /* ---------- ATTRIBUTES ---------- */
    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.9)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.6)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3);
    }

    /* ---------- NAVIGATION ---------- */
    @Override
    protected EntityNavigation createNavigation(World world) {
        BirdNavigation nav = new BirdNavigation(this, world) {
            @Override
            public boolean isValidPosition(BlockPos pos) {
                return true; // ignore roof obstruction: locusts can squeeze
            }
        };
                 nav.setCanPathThroughDoors(false);
         nav.setCanSwim(false);
        return nav;
    }

    /* ---------- GOALS ---------- */
    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new DiveAttackGoal(this));
        goalSelector.add(2, new OrbitTargetGoal(this));
        goalSelector.add(3, new ErraticDartGoal(this));
        goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 16));
        goalSelector.add(5, new LookAroundGoal(this));

        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    /* ---------- TICK ---------- */
    @Override
    public void tick() {
        super.tick();

        // cooldown decrements
        int cd = dataTracker.get(ATTACK_COOLDOWN);
        if (cd > 0) dataTracker.set(ATTACK_COOLDOWN, cd - 1);

        if (swingTicks > 0 && --swingTicks == 0)
            dataTracker.set(ATTACKING, false);

        // remove fall‑sound spam
        fallDistance = 0;
    }

    /* ---------- GOAL IMPLS ---------- */
    private static class OrbitTargetGoal extends Goal {
        private final LocusEntity mob;
        private int recalc;
        OrbitTargetGoal(LocusEntity e) { this.mob = e; setControls(EnumSet.of(Control.MOVE)); }
        @Override public boolean canStart() { return mob.getTarget() != null; }
        @Override public boolean shouldContinue() { return canStart(); }
        @Override public void tick() {
            if (--recalc <= 0) {
                recalc = ORBIT_RECALC_TICKS;
                LivingEntity tgt = mob.getTarget();
                if (tgt == null) return;
                double angle = (mob.age % 360) * Math.PI / 180.0; // slow spin
                mob.orbitPos = tgt.getPos().add(
                        ORBIT_RADIUS * Math.cos(angle),
                        tgt.getHeight() * 0.6, // mid‑torso height
                        ORBIT_RADIUS * Math.sin(angle));
            }
            mob.getMoveControl().moveTo(mob.orbitPos.x, mob.orbitPos.y, mob.orbitPos.z, 1.25);
        }
    }

    /** Dive straight at the target's eye position once in range. */
    private static class DiveAttackGoal extends Goal {
        private final LocusEntity mob;
        DiveAttackGoal(LocusEntity m) { this.mob = m; setControls(EnumSet.of(Control.MOVE)); }
        @Override public boolean canStart() {
            LivingEntity tgt = mob.getTarget();
            return tgt != null && tgt.isAlive()
                    && mob.dataTracker.get(ATTACK_COOLDOWN) == 0
                    && mob.squaredDistanceTo(tgt) < 16; // <4 blocks
        }
        @Override public void start() {
            LivingEntity tgt = mob.getTarget();
            if (tgt == null) return;
            Vec3d diveVec = tgt.getEyePos().subtract(mob.getPos()).normalize().multiply(1.5);
            mob.setVelocity(diveVec);
        }
        @Override public void tick() {
            if (mob.horizontalCollision) stop();
        }
        @Override public void stop() {
            mob.dataTracker.set(ATTACK_COOLDOWN, MAX_ATTACK_COOLDOWN);
        }
    }

    /** Short, random directional bursts when idle – lightweight. */
    private static class ErraticDartGoal extends Goal {
        private final LocusEntity mob;
        private int cooldown;
        ErraticDartGoal(LocusEntity m) { this.mob = m; setControls(EnumSet.of(Control.MOVE)); }
        @Override public boolean canStart() { return mob.getTarget() == null && mob.random.nextInt(60) == 0; }
        @Override public void start() {
            Vec3d dir = new Vec3d(mob.random.nextGaussian(), mob.random.nextGaussian()*0.2, mob.random.nextGaussian()).normalize();
            mob.setVelocity(dir.multiply(0.6));
            cooldown = 20;
        }
        @Override public boolean shouldContinue() { return --cooldown > 0; }
    }

    /* ---------- COMBAT ---------- */
    public boolean tryAttack(LivingEntity target) {
        if (dataTracker.get(ATTACK_COOLDOWN) > 0) return false;
        dataTracker.set(ATTACKING, true);
        swingTicks = 10;
        return super.tryAttack(target);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        // Track damage time for renderer hurt flash effect
        this.lastDamageTime = this.age;
        return super.damage(source, amount);
    }

    /* ---------- EXPERIENCE POINTS ---------- */
    /**
     * Get the experience points this entity drops when killed.
     * Accessible for swarm management and loot balancing.
     */
    public int getExperiencePoints() {
        return this.experiencePoints;
    }

    /**
     * Set the experience points this entity drops when killed.
     * Allows dynamic adjustment during swarm events.
     */
    public void setExperiencePoints(int points) {
        this.experiencePoints = points;
    }

    /* ---------- SOUNDS ---------- */
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ENTITY_BEE_LOOP; }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return SoundEvents.ENTITY_BEE_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_BEE_DEATH; }

    /* ---------- DATA & NBT ---------- */
    @Override protected void initDataTracker() {
        super.initDataTracker();
        dataTracker.startTracking(ATTACKING, false);
        dataTracker.startTracking(ATTACK_COOLDOWN, 0);
    }

    @Override public void writeCustomDataToNbt(NbtCompound n) { super.writeCustomDataToNbt(n); }
    @Override public void readCustomDataFromNbt(NbtCompound n) { super.readCustomDataFromNbt(n); }

    /* ---------- ANIMATION ---------- */
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<>(this, "ctrl", 2, this::predicate));
    }
    private <T extends GeoEntity> PlayState predicate(AnimationState<T> s) {
        if (dataTracker.get(ATTACKING)) {
            s.getController().setAnimation(RawAnimation.begin().then("animation.locus.attack", Animation.LoopType.PLAY_ONCE));
            return PlayState.CONTINUE;
        }
        if (!this.isOnGround()) {
            s.getController().setAnimation(RawAnimation.begin().then("animation.locus.fly", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }
        s.getController().setAnimation(RawAnimation.begin().then("animation.locus.idle", Animation.LoopType.LOOP));
        return PlayState.CONTINUE;
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
