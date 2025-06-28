package com.ancientcurse.entity;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.ModEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class KhamsinOrbEntity extends ProjectileEntity implements GeoEntity {

    private LivingEntity owner;
    private LivingEntity target;
    private int lifeTime = 0;
    private static final int MAX_LIFETIME = 200; // 10 seconds
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public KhamsinOrbEntity(EntityType<? extends KhamsinOrbEntity> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(true);
    }

    public KhamsinOrbEntity(World world, LivingEntity owner, LivingEntity target) {
        this(ModEntities.KHAMSIN_ORB, world); // Use our custom entity type
        this.owner = owner;
        this.target = target;
        this.setOwner(owner);
    }

    @Override
    protected void initDataTracker() {
        // No specific data to track for now
    }

    @Override
    public void tick() {
        super.tick();
        lifeTime++;

        // Remove after max lifetime
        if (lifeTime >= MAX_LIFETIME) {
            this.discard();
            return;
        }

        // Homing behavior towards target
        if (target != null && target.isAlive()) {
            Vec3d direction = target.getEyePos().subtract(this.getPos()).normalize();
            Vec3d currentVelocity = this.getVelocity();

            // Gradually adjust velocity toward target (slow homing)
            Vec3d newVelocity = currentVelocity.add(direction.multiply(0.05)); // Increased homing strength

            // Limit speed to keep it slow and mystical
            double speed = newVelocity.length();
            if (speed > 0.2) { // Slightly faster than before
                newVelocity = newVelocity.normalize().multiply(0.2);
            }

            this.setVelocity(newVelocity);
        } else { // If target is lost, continue in current direction
            this.setVelocity(this.getVelocity().multiply(0.98)); // Gradually slow down
        }

        // Move the projectile
        this.setPosition(this.getX() + this.getVelocity().x, this.getY() + this.getVelocity().y, this.getZ() + this.getVelocity().z);

        // Handle collisions
        HitResult hitResult = ProjectileUtil.getCollision(this, this::canHit);
        if (hitResult.getType() != HitResult.Type.MISS) {
            onCollision(hitResult);
        }

        // Create mystical trail particles
        if (this.getWorld() instanceof ServerWorld serverWorld && this.random.nextInt(2) == 0) {
            serverWorld.spawnParticles(
                ParticleTypes.PORTAL,
                this.getX(), this.getY(), this.getZ(),
                1, 0.1, 0.1, 0.1, 0.01
            );
        }
    }

    @Override
    protected boolean canHit(Entity entity) {
        // Prevent hitting the owner and other projectiles
        return !entity.isSpectator() && entity.isAlive() && entity.canBeHitByProjectile() && entity != this.owner && !(entity instanceof ProjectileEntity);
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
        Entity entity = entityHitResult.getEntity();

        if (entity instanceof LivingEntity livingEntity) {
            // Damage player and apply curse effect
            livingEntity.damage(this.getDamageSources().magic(), 8.0f); // Increased damage

            // Create impact particles
            if (this.getWorld() instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(
                    ParticleTypes.WITCH,
                    this.getX(), this.getY(), this.getZ(),
                    15, 0.4, 0.4, 0.4, 0.1
                );
            }

            // Play impact sound
            this.playSound(net.minecraft.sound.SoundEvents.ENTITY_SHULKER_BULLET_HIT, 1.0f, 1.2f);

            this.discard();
        }
    }

    @Override
    protected void onBlockHit(net.minecraft.util.hit.BlockHitResult blockHitResult) {
        super.onBlockHit(blockHitResult);
        // Destroy orb on block collision
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                ParticleTypes.SMOKE,
                this.getX(), this.getY(), this.getZ(),
                8, 0.3, 0.3, 0.3, 0.05
            );
        }
        this.discard();
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        // Allow players to destroy orbs by hitting them
        if (source.getAttacker() instanceof LivingEntity attacker) {
            if (this.getWorld() instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(
                    ParticleTypes.CRIT,
                    this.getX(), this.getY(), this.getZ(),
                    10, 0.4, 0.4, 0.4, 0.1
                );
            }
            this.playSound(net.minecraft.sound.SoundEvents.BLOCK_GLASS_BREAK, 0.8f, 1.5f);
            this.discard();
            return true;
        }
        return false;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (this.owner != null) {
            nbt.putUuid("OwnerUUID", this.owner.getUuid());
        }
        if (this.target != null) {
            nbt.putUuid("TargetUUID", this.target.getUuid());
        }
        nbt.putInt("LifeTime", this.lifeTime);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.containsUuid("OwnerUUID")) {
            // Re-assign owner if needed (e.g., for persistent projectiles)
        }
        if (nbt.containsUuid("TargetUUID")) {
            // Re-assign target if needed
        }
        this.lifeTime = nbt.getInt("LifeTime");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private <T extends GeoEntity> PlayState predicate(AnimationState<T> state) {
        state.getController().setAnimation(RawAnimation.begin().then("animation.khamsin_orb.idle", Animation.LoopType.LOOP));
        return PlayState.CONTINUE;
    }
}