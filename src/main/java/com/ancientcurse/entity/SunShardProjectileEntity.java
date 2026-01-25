package com.ancientcurse.entity;

import com.ancientcurse.ModEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class SunShardProjectileEntity extends ThrownItemEntity {

    public SunShardProjectileEntity(EntityType<? extends ThrownItemEntity> entityType, World world) {
        super(entityType, world);
    }

    public SunShardProjectileEntity(World world, LivingEntity owner) {
        super(ModEntities.SUN_SHARD_PROJECTILE, owner, world);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.GOLD_NUGGET; // Visual placeholder for standard rendering
    }

    @Override
    public void tick() {
        super.tick();

        // Accurate flight - less gravity than normal thrown items
        Vec3d velocity = this.getVelocity();
        if (!this.hasNoGravity()) {
            this.setVelocity(velocity.x, velocity.y + 0.03, velocity.z);
        }

        if (this.getWorld().isClient) {
            // Solar trail
            this.getWorld().addParticle(
                    ParticleTypes.FLAME,
                    this.getX(), this.getY(), this.getZ(),
                    0, 0, 0);

            if (this.age % 2 == 0) {
                this.getWorld().addParticle(
                        ParticleTypes.END_ROD,
                        this.getX(), this.getY(), this.getZ(),
                        (this.random.nextDouble() - 0.5) * 0.05,
                        (this.random.nextDouble() - 0.5) * 0.05,
                        (this.random.nextDouble() - 0.5) * 0.05);
            }
        }

        if (this.age > 100) {
            this.discard();
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
        Entity entity = entityHitResult.getEntity();
        Entity owner = this.getOwner();

        if (entity != owner && entity instanceof LivingEntity livingEntity) {
            DamageSource damageSource = owner != null
                    ? this.getDamageSources().mobProjectile(this, (LivingEntity) owner)
                    : this.getDamageSources().generic();

            // Deal fire and magic damage
            livingEntity.damage(damageSource, 10.0f);
            livingEntity.setOnFireFor(3);

            // Play slice sound
            this.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.5f);
        }
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);

        if (!this.getWorld().isClient) {
            // Solar impact burst
            this.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 1.2f);
            this.playSound(SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0f, 1.0f);

            this.discard();
        }
    }

    @Override
    protected float getGravity() {
        return 0.01f; // Very light gravity
    }
}
