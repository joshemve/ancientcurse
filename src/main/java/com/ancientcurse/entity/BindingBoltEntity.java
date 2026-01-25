package com.ancientcurse.entity;

import com.ancientcurse.ModEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.ParticleTypes;
import org.joml.Vector3f;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

/**
 * Binding Bolt - A special projectile that wraps around Ra, causing hibernation.
 *
 * When this bolt hits Ra, it triggers his hibernation state.
 * The bolt uses rope/chain visual effects.
 */
public class BindingBoltEntity extends PersistentProjectileEntity {

    private static final int DESPAWN_DELAY = 40; // 2 seconds after hitting ground
    private int groundTimer = 0;
    private boolean hasHitGround = false;

    public BindingBoltEntity(EntityType<? extends PersistentProjectileEntity> entityType, World world) {
        super(entityType, world);
        this.setDamage(2.0); // Low damage - purpose is to bind, not hurt
    }

    public BindingBoltEntity(World world, LivingEntity owner) {
        super(ModEntities.BINDING_BOLT, owner, world);
        this.setDamage(2.0);
    }

    @Override
    protected ItemStack asItemStack() {
        return ItemStack.EMPTY; // Cannot be picked up
    }

    @Override
    public void tick() {
        super.tick();

        World world = this.getWorld();

        // Trail particles - rope/chain effect
        if (world instanceof ServerWorld serverWorld && !this.inGround) {
            // Golden binding particles
            serverWorld.spawnParticles(
                ParticleTypes.ENCHANTED_HIT,
                this.getX(), this.getY(), this.getZ(),
                2, 0.1, 0.1, 0.1, 0.0
            );

            // Subtle dust trail - golden to white transition
            serverWorld.spawnParticles(
                new DustColorTransitionParticleEffect(
                    new Vector3f(1.0f, 0.8f, 0.2f),  // Start: golden
                    new Vector3f(1.0f, 1.0f, 1.0f),  // End: white
                    1.0f),  // Scale
                this.getX(), this.getY(), this.getZ(),
                1, 0.05, 0.05, 0.05, 0.0
            );
        }

        // Handle ground despawn
        if (hasHitGround) {
            groundTimer++;
            if (groundTimer >= DESPAWN_DELAY) {
                if (world instanceof ServerWorld serverWorld) {
                    serverWorld.spawnParticles(
                        ParticleTypes.SMOKE,
                        this.getX(), this.getY(), this.getZ(),
                        5, 0.2, 0.1, 0.2, 0.02
                    );
                }
                this.discard();
            }
        } else if (this.inGround) {
            hasHitGround = true;
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        Entity target = entityHitResult.getEntity();
        World world = this.getWorld();

        // Check if we hit Ra
        if (target instanceof RaEntity ra) {
            // Trigger hibernation!
            ra.triggerHibernation();

            // Play binding sound
            world.playSound(
                null,
                this.getX(), this.getY(), this.getZ(),
                SoundEvents.ITEM_CROSSBOW_HIT,
                this.getSoundCategory(),
                1.0f, 0.8f
            );

            // Chain/wrap sound
            world.playSound(
                null,
                this.getX(), this.getY(), this.getZ(),
                SoundEvents.BLOCK_CHAIN_PLACE,
                this.getSoundCategory(),
                1.0f, 1.2f
            );

            // Binding particle burst
            if (world instanceof ServerWorld serverWorld) {
                // Golden explosion of particles
                serverWorld.spawnParticles(
                    ParticleTypes.ENCHANTED_HIT,
                    target.getX(), target.getY() + target.getHeight() / 2, target.getZ(),
                    30, 1.0, 1.5, 1.0, 0.1
                );

                // Chain-like particles wrapping around
                for (int i = 0; i < 20; i++) {
                    double angle = Math.toRadians(i * 18);
                    double radius = 1.5;
                    double x = target.getX() + Math.cos(angle) * radius;
                    double z = target.getZ() + Math.sin(angle) * radius;
                    double y = target.getY() + (i * 0.2);

                    serverWorld.spawnParticles(
                        ParticleTypes.END_ROD,
                        x, y, z,
                        1, 0.1, 0.1, 0.1, 0.0
                    );
                }
            }

            // Remove the projectile
            this.discard();
            return;
        }

        // Normal hit behavior for other entities
        super.onEntityHit(entityHitResult);
    }
}
