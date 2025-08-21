package com.ancientcurse.entity;

import com.ancientcurse.ModEntities;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.block.Blocks;
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
        
        // Spawn trail particles - minimal ball-like effect
        if (this.getWorld() instanceof ServerWorld) {
            ServerWorld serverWorld = (ServerWorld) this.getWorld();
            
            // Core of the spit ball - dense particle
            serverWorld.spawnParticles(
                ParticleTypes.ITEM_SLIME, 
                pos.x, pos.y, pos.z,
                1, // single particle
                0.05, 0.05, 0.05, // very small spread to keep it ball-like
                0.0 // no extra speed
            );
            
            // Occasional trailing effect
            if (this.age % 3 == 0) {
                serverWorld.spawnParticles(
                    ParticleTypes.SPIT,
                    pos.x - velocity.x * 0.5, pos.y - velocity.y * 0.5, pos.z - velocity.z * 0.5,
                    1, // minimal particles
                    0.02, 0.02, 0.02, // tight spread
                    0.0
                );
            }
        }
        
        // Increment age and remove if too old
        if (++this.age > this.maxAge) {
            this.discard();
        }
    }
    
    @Override
    protected boolean canHit(Entity entity) {
        // Ignore creative/spectator players
        if (entity instanceof PlayerEntity player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        // Don't hit the owner or other projectiles
        return super.canHit(entity) && entity != this.getOwner() && !(entity instanceof ProjectileEntity);
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
                
                // Create lingering particles that follow the entity
                createLingeringEffect(living);
            }
            
            // Spawn acid splash particles at the exact hit location
            createSplashParticles(entityHitResult.getPos());
            
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
        createSplashParticles(blockHitResult.getPos());
        
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
    
    private void createSplashParticles(Vec3d targetPos) {
        if (!(this.getWorld() instanceof ServerWorld)) return;
        
        ServerWorld world = (ServerWorld) this.getWorld();
        
        // Center the effect on the entity that was hit
        double centerX = targetPos.x;
        double centerY = targetPos.y + 0.5;
        double centerZ = targetPos.z;
        
        // Main splash effect - slime particles
        world.spawnParticles(
            ParticleTypes.ITEM_SLIME,
            centerX, centerY, centerZ,
            8, // reduced particle count
            0.3, 0.2, 0.3, // moderate spread
            0.1 // slight velocity
        );
        
        // Small poison cloud
        world.spawnParticles(
            ParticleTypes.SNEEZE,
            centerX, centerY, centerZ,
            5,
            0.2, 0.2, 0.2,
            0.02
        );
        
        // A few splash particles for impact
        world.spawnParticles(
            ParticleTypes.SPIT,
            centerX, centerY, centerZ,
            6,
            0.4, 0.1, 0.4,
            0.05
        );
    }
    
    private void createLingeringEffect(LivingEntity entity) {
        // Particles will be spawned by the poison effect itself
        // The poison effect duration matches the particle duration
        // This ensures particles follow the entity naturally
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
