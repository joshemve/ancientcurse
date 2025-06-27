package com.ancientcurse.entity;

import com.ancientcurse.ModEntities;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

/**
 * Custom projectile entity for the Djeserhath's poison spit attack
 * Uses GeckoLib for animations
 */
public class SpitBallEntity extends ProjectileEntity implements GeoEntity {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private int age;
    private int maxAge = 60; // Despawn after 3 seconds if it doesn't hit anything
    
    public SpitBallEntity(EntityType<? extends ProjectileEntity> entityType, World world) {
        super(entityType, world);
    }
    
    public SpitBallEntity(World world, LivingEntity owner) {
        this((EntityType<SpitBallEntity>) ModEntities.SPIT_BALL, world);
        this.setOwner(owner);
        this.setPosition(owner.getX(), owner.getY() + 1.5, owner.getZ());
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Use a longer transition time (5 ticks) to prevent animation glitching
        controllers.add(new AnimationController<>(this, "controller", 5, this::animationPredicate));
    }
    
    private <T extends GeoAnimatable> PlayState animationPredicate(AnimationState<T> state) {
        // Use LoopType.LOOP instead of thenPlay for smoother continuous animation
        state.getController().setAnimation(RawAnimation.begin().then("animation.spit_ball.spin", Animation.LoopType.LOOP));
        return PlayState.CONTINUE;
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
    
    @Override
    protected void initDataTracker() {
        // No data to track
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Basic projectile physics
        Vec3d velocity = this.getVelocity();
        Vec3d pos = this.getPos();
        
        // Check for collision
        HitResult hitResult = ProjectileUtil.getCollision(this, this::canHit);
        this.onCollision(hitResult);
        
        // Update position
        this.setPosition(pos.x + velocity.x, pos.y + velocity.y, pos.z + velocity.z);
        ProjectileUtil.setRotationFromVelocity(this, 0.5f);
        
        // Spawn trail particles
        if (this.getWorld() instanceof ServerWorld) {
            ServerWorld serverWorld = (ServerWorld) this.getWorld();
            
            // Purple particle trail
            serverWorld.spawnParticles(
                ParticleTypes.WITCH, 
                pos.x, pos.y, pos.z,
                1, // count
                0.05, 0.05, 0.05, // spread
                0.0 // speed
            );
            
            // Dripping effect
            if (this.random.nextFloat() < 0.3f) {
                serverWorld.spawnParticles(
                    ParticleTypes.DRIPPING_OBSIDIAN_TEAR,
                    pos.x, pos.y - 0.1, pos.z,
                    1, // count
                    0.05, 0, 0.05, // spread
                    0.01 // speed
                );
            }
        }
        
        // Increment age and remove if too old
        if (++this.age > this.maxAge) {
            this.discard();
        }
    }
    
    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
        
        // Don't hit owner or other spit balls
        Entity entity = entityHitResult.getEntity();
        Entity owner = this.getOwner();
        
        if (entity != owner && !(entity instanceof SpitBallEntity)) {
            // Apply poison effect to hit entity if it's a living entity
            if (entity instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) entity;
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 100, 1)); // Poison II for 5 seconds
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 60, 0)); // Nausea for 3 seconds
            }
            
            // Spawn acid splash particles
            createSplashParticles();
            
            // Play acid splash sound
            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), 
                SoundEvents.BLOCK_SLIME_BLOCK_BREAK, SoundCategory.HOSTILE, 
                1.0F, 0.8F + this.random.nextFloat() * 0.4F);
            
            // Burn/sizzle sound
            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.HOSTILE, 
                0.7F, 1.2F + this.random.nextFloat() * 0.3F);
            
            // Remove projectile after hit
            this.discard();
        }
    }
    
    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        super.onBlockHit(blockHitResult);
        
        // Create acid splash particles on block impact
        createSplashParticles();
        
        // Play acid splash sound
        this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), 
            SoundEvents.BLOCK_SLIME_BLOCK_BREAK, SoundCategory.HOSTILE, 
            1.0F, 0.8F + this.random.nextFloat() * 0.4F);
            
        // Burn/sizzle sound
        this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.HOSTILE, 
            0.7F, 1.2F + this.random.nextFloat() * 0.3F);
        
        // Remove projectile after hit
        this.discard();
    }
    
    private void createSplashParticles() {
        if (!(this.getWorld() instanceof ServerWorld)) return;
        
        ServerWorld world = (ServerWorld) this.getWorld();
        Vec3d pos = this.getPos();
        
        // ===== IMPROVED PARTICLES WITH GRAVITY =====
        
        // Purple burst particles - more dynamic spread with gravity effect
        for (int i = 0; i < 25; i++) {
            double offsetX = (this.random.nextDouble() - 0.5) * 0.5;
            double offsetY = (this.random.nextDouble() - 0.5) * 0.5;
            double offsetZ = (this.random.nextDouble() - 0.5) * 0.5;
            
            // Add gravity/falling motion
            double velocityX = offsetX * 0.3;
            double velocityY = offsetY * 0.2 - 0.1; // Slight downward bias
            double velocityZ = offsetZ * 0.3;
            
            world.spawnParticles(
                ParticleTypes.WITCH,
                pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                0, // count of 0 means just 1 particle at exact position
                velocityX, velocityY, velocityZ, // velocity components
                0.6 // speed multiplier
            );
        }
        
        // Acid drips - always fall down
        for (int i = 0; i < 18; i++) {
            double offsetX = (this.random.nextDouble() - 0.5) * 0.4;
            double offsetY = (this.random.nextDouble() - 0.3) * 0.3; // Higher position
            double offsetZ = (this.random.nextDouble() - 0.5) * 0.4;
            
            world.spawnParticles(
                ParticleTypes.DRIPPING_OBSIDIAN_TEAR,
                pos.x + offsetX, pos.y + offsetY + 0.2, pos.z + offsetZ,
                0,
                offsetX * 0.1, -0.15, offsetZ * 0.1, // Always moves downward
                0.8
            );
        }
        
        // Acid splash effect - falling droplets
        for (int i = 0; i < 12; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double distance = 0.1 + this.random.nextDouble() * 0.2;
            double offsetX = Math.cos(angle) * distance;
            double offsetZ = Math.sin(angle) * distance;
            
            world.spawnParticles(
                ParticleTypes.FALLING_OBSIDIAN_TEAR,
                pos.x + offsetX, pos.y + 0.3, pos.z + offsetZ,
                0,
                offsetX * 0.2, -0.2, offsetZ * 0.2,
                1.0
            );
        }
        
        // Smoke effect - rising slightly
        for (int i = 0; i < 10; i++) {
            double offsetX = (this.random.nextDouble() - 0.5) * 0.3;
            double offsetY = (this.random.nextDouble() - 0.3) * 0.2;
            double offsetZ = (this.random.nextDouble() - 0.5) * 0.3;
            
            world.spawnParticles(
                ParticleTypes.SMOKE,
                pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                0,
                offsetX * 0.05, 0.05, offsetZ * 0.05, // Slight upward drift
                0.2
            );
        }
        
        // Damage effect with proper motion
        for (int i = 0; i < 5; i++) {
            double offsetX = (this.random.nextDouble() - 0.5) * 0.4;
            double offsetZ = (this.random.nextDouble() - 0.5) * 0.4;
            
            world.spawnParticles(
                ParticleTypes.DAMAGE_INDICATOR,
                pos.x, pos.y + 0.5, pos.z,
                0,
                offsetX, 0.1, offsetZ, // Rising slightly
                0.5
            );
        }
    }
    
    @Override
    public boolean hasNoGravity() {
        // Spit ball has slight gravity
        return false;
    }
    
    // Custom gravity for the projectile
    protected float getGravityVelocity() {
        // Less gravity than normal projectiles
        return 0.02F;
    }
    
    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.age = nbt.getInt("Age");
    }
    
    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("Age", this.age);
    }
}
