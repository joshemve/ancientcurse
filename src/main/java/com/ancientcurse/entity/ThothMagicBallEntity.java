package com.ancientcurse.entity;

import com.ancientcurse.ModEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

public class ThothMagicBallEntity extends ThrownItemEntity {
    
    public ThothMagicBallEntity(EntityType<? extends ThrownItemEntity> entityType, World world) {
        super(entityType, world);
    }
    
    public ThothMagicBallEntity(World world, LivingEntity owner) {
        super(ModEntities.THOTH_MAGIC_BALL, owner, world);
    }
    
    public ThothMagicBallEntity(World world, double x, double y, double z) {
        super(ModEntities.THOTH_MAGIC_BALL, x, y, z, world);
    }
    
    @Override
    protected Item getDefaultItem() {
        return Items.ENDER_PEARL; // Visual item for the projectile
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Slow down the projectile
        this.setVelocity(this.getVelocity().multiply(0.95));
        
        // Add magical particles
        if (this.getWorld().isClient) {
            for (int i = 0; i < 3; i++) {
                this.getWorld().addParticle(
                    ParticleTypes.WITCH,
                    this.getX() + (this.random.nextDouble() - 0.5) * 0.5,
                    this.getY() + (this.random.nextDouble() - 0.5) * 0.5,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 0.5,
                    0, 0.05, 0
                );
            }
        }
        
        // Remove after 10 seconds
        if (this.age > 200) {
            this.discard();
        }
    }
    
    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
        Entity entity = entityHitResult.getEntity();
        Entity owner = this.getOwner();
        
        if (entity != owner && entity instanceof LivingEntity livingEntity) {
            // Deal damage
            DamageSource damageSource = owner != null ? 
                this.getDamageSources().mobProjectile(this, (LivingEntity) owner) : 
                this.getDamageSources().generic();
            
            livingEntity.damage(damageSource, 12.0f);
            
            // Apply weakness effect
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 200, 1));
            
            // Knockback
            if (owner != null) {
                double dx = livingEntity.getX() - owner.getX();
                double dz = livingEntity.getZ() - owner.getZ();
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > 0) {
                    livingEntity.addVelocity(dx / distance * 0.5, 0.3, dz / distance * 0.5);
                }
            }
        }
    }
    
    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        
        // Create impact particles
        if (!this.getWorld().isClient) {
            for (int i = 0; i < 8; i++) {
                this.getWorld().addParticle(
                    ParticleTypes.WITCH,
                    this.getX() + (this.random.nextDouble() - 0.5),
                    this.getY() + (this.random.nextDouble() - 0.5),
                    this.getZ() + (this.random.nextDouble() - 0.5),
                    0, 0, 0
                );
            }
        }
        
        // Play impact sound
        this.playSound(SoundEvents.ENTITY_WITCH_HURT, 1.0f, 1.2f);
        
        // Remove the projectile
        this.discard();
    }
} 