package com.ancientcurse.entity;

import net.minecraft.entity.EntityType;
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
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.block.Blocks;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.List;

/**
 * Baby Locus Entity - Smaller, faster version of LocusEntity that can grow into adults.
 * 
 * Key Features:
 * - 50% size of adult locus
 * - Faster movement and attack speed
 * - Can grow into adult locus over time
 * - Used during early swarm eruption phases
 */
public class BabyLocusEntity extends HostileEntity implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    private LocusEntity parent;
    private int growthTicks = 0;
    private static final int GROWTH_TIME = 1200; // 60 seconds to mature
    private boolean emerging = false;
    
    /* ---------- DAMAGE TRACKING ---------- */
    public int lastDamageTime = 0; // Tracks when entity last took damage for renderer effects
    
    /* ---------- DATA TRACKERS ---------- */
    private static final TrackedData<Boolean> ATTACKING =
            DataTracker.registerData(BabyLocusEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    
    public BabyLocusEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(true);
        this.moveControl = new FlightMoveControl(this, 20, true);
        this.experiencePoints = 2; // Less XP than adults
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(ATTACKING, false);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new BabyLocusFollowParentGoal(this, 1.2D));
        this.goalSelector.add(2, new BabyLocusSwarmGoal(this));
        this.goalSelector.add(3, new BabyLocusWanderGoal(this));
        this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(5, new LookAroundGoal(this));
        
        // Babies flee from players instead of attacking
        this.goalSelector.add(1, new FleeEntityGoal<>(this, PlayerEntity.class, 8.0F, 1.0D, 1.2D));
    }

    public static DefaultAttributeContainer.Builder createBabyLocusAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 15)     // Less health than adults
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.8) // Faster movement
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32)    // Shorter range
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2)   // Less damage
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 1.1); // Flying speed
    }

    @Override
    protected EntityNavigation createNavigation(World world) {
        BirdNavigation navigation = new BirdNavigation(this, world);
        navigation.setCanPathThroughDoors(false);
        navigation.setCanSwim(false);
        return navigation;
    }

    @Override
    public void tick() {
        super.tick();
        
        // Handle emergence animation
        if (emerging && this.age < 40) {
            // Rising from ground effect
            this.setVelocity(getVelocity().add(0, 0.02, 0));
        } else {
            emerging = false;
        }
        
        // Growth over time (only in survival)
        if (!this.getWorld().isClient && !this.isRemoved()) {
            growthTicks++;
            
            // Grow into adult after time
            if (growthTicks >= GROWTH_TIME) {
                growIntoAdult();
            }
        }
        
        // Tiny wing buzz particles
        if (age % 10 == 0 && !isOnGround()) {
            getWorld().addParticle(ParticleTypes.SMOKE,
                getX() + (random.nextDouble() - 0.5) * 0.2,
                getY(),
                getZ() + (random.nextDouble() - 0.5) * 0.2,
                0, 0.01, 0);
        }
    }

    /**
     * Set the emerging state for the baby locus
     */
    public void setEmerging(boolean emerging) {
        this.emerging = emerging;
        if (emerging) {
            // Start slightly underground
            this.setPosition(this.getX(), this.getY() - 0.5, this.getZ());
        }
    }

    /**
     * Transform this baby into an adult locus
     */
    private void growIntoAdult() {
        if (this.getWorld().isClient) return;
        
        // Create adult locus at same position
        LocusEntity adult = com.ancientcurse.ModEntities.LOCUS.create(this.getWorld());
        if (adult != null) {
            adult.setPosition(this.getPos());
            adult.setYaw(this.getYaw());
            adult.setPitch(this.getPitch());
            adult.setVelocity(this.getVelocity());
            
            // Transfer health proportionally
            float healthRatio = this.getHealth() / this.getMaxHealth();
            adult.setHealth(adult.getMaxHealth() * healthRatio);
            
            // Transfer target if any
            if (this.getTarget() != null) {
                adult.setTarget(this.getTarget());
            }
            
            // Spawn particles for transformation
            for (int i = 0; i < 10; i++) {
                Vec3d particlePos = this.getPos().add(
                    (this.random.nextDouble() - 0.5) * 2,
                    this.random.nextDouble() * 2,
                    (this.random.nextDouble() - 0.5) * 2
                );
                // Note: Using simple approach since we can't access ParticleTypes easily
            }
            
            // Spawn adult and remove baby
            this.getWorld().spawnEntity(adult);
            this.discard();
        }
    }

    public void setParent(LocusEntity parent) {
        this.parent = parent;
    }

    public LocusEntity getParent() {
        return this.parent;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        // Track damage time for renderer hurt flash effect
        this.lastDamageTime = this.age;
        return super.damage(source, amount);
    }

    /**
     * Set the experience points this entity drops when killed.
     */
    public void setExperiencePoints(int experiencePoints) {
        this.experiencePoints = experiencePoints;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_BEE_POLLINATE; // Cute buzzing
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_BEE_HURT;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F; // Quieter than adults
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("GrowthTicks", growthTicks);
        nbt.putBoolean("Emerging", emerging);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        growthTicks = nbt.getInt("GrowthTicks");
        emerging = nbt.getBoolean("Emerging");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 2, this::predicate));
    }

    private <T extends GeoEntity> PlayState predicate(AnimationState<T> state) {
        if (this.dataTracker.get(ATTACKING)) {
            state.getController().setAnimation(RawAnimation.begin()
                .then("animation.baby_locus.attack", Animation.LoopType.PLAY_ONCE));
            return PlayState.CONTINUE;
        }
        
        if (!this.isOnGround()) {
            state.getController().setAnimation(RawAnimation.begin()
                .then("animation.baby_locus.fly", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }
        
        state.getController().setAnimation(RawAnimation.begin()
            .then("animation.baby_locus.idle", Animation.LoopType.LOOP));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // Custom AI Goals
    static class BabyLocusFollowParentGoal extends Goal {
        private final BabyLocusEntity baby;
        private final double speed;
        private int timeToRecalcPath;

        public BabyLocusFollowParentGoal(BabyLocusEntity baby, double speed) {
            this.baby = baby;
            this.speed = speed;
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            if (baby.parent == null || !baby.parent.isAlive()) {
                // Find new parent if orphaned
                List<LocusEntity> nearbyAdults = baby.getWorld().getEntitiesByClass(
                    LocusEntity.class,
                    baby.getBoundingBox().expand(10),
                    adult -> adult.isAlive()
                );
                
                if (!nearbyAdults.isEmpty()) {
                    baby.parent = nearbyAdults.get(0);
                }
            }
            
            return baby.parent != null && baby.parent.isAlive() && 
                   baby.squaredDistanceTo(baby.parent) > 9.0;
        }

        @Override
        public void tick() {
            if (--timeToRecalcPath <= 0) {
                timeToRecalcPath = 10;
                Vec3d targetPos = baby.parent.getPos().add(
                    baby.random.nextGaussian() * 2,
                    1,
                    baby.random.nextGaussian() * 2
                );
                baby.getMoveControl().moveTo(targetPos.x, targetPos.y, targetPos.z, speed);
            }
        }
    }

    static class BabyLocusSwarmGoal extends Goal {
        private final BabyLocusEntity baby;

        public BabyLocusSwarmGoal(BabyLocusEntity baby) {
            this.baby = baby;
        }

        @Override
        public boolean canStart() {
            return baby.parent == null && baby.random.nextInt(20) == 0;
        }

        @Override
        public void tick() {
            // Swarm with other babies
            List<BabyLocusEntity> nearbyBabies = baby.getWorld().getEntitiesByClass(
                BabyLocusEntity.class,
                baby.getBoundingBox().expand(8),
                b -> b != baby && b.isAlive()
            );
            
            if (!nearbyBabies.isEmpty()) {
                Vec3d swarmCenter = Vec3d.ZERO;
                for (BabyLocusEntity other : nearbyBabies) {
                    swarmCenter = swarmCenter.add(other.getPos());
                }
                swarmCenter = swarmCenter.multiply(1.0 / nearbyBabies.size());
                
                baby.getMoveControl().moveTo(swarmCenter.x, swarmCenter.y, swarmCenter.z, 0.8);
            }
        }
    }

    static class BabyLocusWanderGoal extends Goal {
        private final BabyLocusEntity baby;

        public BabyLocusWanderGoal(BabyLocusEntity baby) {
            this.baby = baby;
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            return baby.getMoveControl().isMoving() == false && baby.random.nextInt(10) == 0;
        }

        @Override
        public void start() {
            Vec3d wanderTarget = baby.getPos().add(
                (baby.random.nextDouble() - 0.5) * 10,
                (baby.random.nextDouble() - 0.5) * 3,
                (baby.random.nextDouble() - 0.5) * 10
            );
            
            // Keep above ground
            wanderTarget = new Vec3d(
                wanderTarget.x,
                Math.max(baby.getWorld().getBottomY() + 2, wanderTarget.y),
                wanderTarget.z
            );
            
            baby.getMoveControl().moveTo(wanderTarget.x, wanderTarget.y, wanderTarget.z, 0.6);
        }
    }

    @Override
    protected float getActiveEyeHeight(net.minecraft.entity.EntityPose pose, net.minecraft.entity.EntityDimensions dimensions) {
        return dimensions.height * 0.85F; // Adjust eye height for smaller size
    }
}