package com.example.egyptianweapons.effect;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import com.example.egyptianweapons.registry.EntityRegistry;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class SnakeHeadProjectileEntity extends PersistentProjectileEntity implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final int POISON_DURATION = 100; // 5 seconds
    private static final int POISON_AMPLIFIER = 1;    // Poison II
    private static final double KNOCKBACK_STRENGTH = 0.5; // Adjust knockback strength as needed
    private static final int GROUND_DESPAWN_DELAY = 60; // 3 seconds (60 ticks)
    
    private boolean hasHitGround = false;
    private int groundTimer = 0;

    public SnakeHeadProjectileEntity(EntityType<? extends PersistentProjectileEntity> entityType, World world) {
        super(entityType, world);
        this.setDamage(4.0); // Base damage
    }

    public SnakeHeadProjectileEntity(World world, LivingEntity owner) {
        super(EntityRegistry.SNAKE_HEAD_PROJECTILE, owner, world);
        this.setDamage(4.0);
    }

    @Override
    protected ItemStack asItemStack() {
        return ItemStack.EMPTY; // The projectile cannot be picked up
    }

    @Override
    public void tick() {
        super.tick();

        // Check if the projectile has hit the ground and handle despawning
        if (hasHitGround) {
            groundTimer++;
            if (groundTimer >= GROUND_DESPAWN_DELAY) {
                // Play disappearing effects before removing
                World world = this.getWorld();
                if (world instanceof ServerWorld serverWorld) {
                    // Smoke puff effect
                    serverWorld.spawnParticles(ParticleTypes.LARGE_SMOKE,
                            this.getX(), this.getY() + 0.1, this.getZ(),
                            8, 0.2, 0.1, 0.2, 0.02);
                    
                    // Some mystical particles
                    serverWorld.spawnParticles(ParticleTypes.ENCHANTED_HIT,
                            this.getX(), this.getY() + 0.1, this.getZ(),
                            12, 0.3, 0.1, 0.3, 0.1);
                    
                    // Play a soft "poof" sound
                    world.playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.BLOCKS,
                            0.3F, 1.5F); // Lower volume, higher pitch for a softer effect
                }
                
                this.discard(); // Remove the entity from the world
                return;
            }
            
            // Add some particle effects while waiting to despawn
            if (groundTimer % 10 == 0) { // Every 10 ticks
                World world = this.getWorld();
                if (world instanceof ServerWorld serverWorld) {
                    serverWorld.spawnParticles(ParticleTypes.SMOKE,
                            this.getX(), this.getY() + 0.1, this.getZ(),
                            1, 0.1, 0.1, 0.1, 0.01);
                }
            }
        } else if (this.inGround) {
            hasHitGround = true;
        }

        // --- Dynamic Light Emission ---
        // Spawn END_ROD particles (which are naturally bright) along the flight path.
        World world = this.getWorld();
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.END_ROD,
                    this.getX(), this.getY(), this.getZ(),
                    1, // count
                    0.05, 0.05, 0.05, // small offset
                    0.0);
        }

        // --- Trail Particles ---
        if (world instanceof ServerWorld serverWorld) {
            // Standard smoke trail
            serverWorld.spawnParticles(ParticleTypes.SMOKE,
                    this.getX(), this.getY(), this.getZ(),
                    2, // count
                    0.1, 0.1, 0.1, // offset
                    0.0);
            // Critical hit particles for a magical feel
            serverWorld.spawnParticles(ParticleTypes.CRIT,
                    this.getX(), this.getY(), this.getZ(),
                    1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
        Entity target = entityHitResult.getEntity();
        World world = this.getWorld();

        if (target instanceof LivingEntity livingEntity) {
            // Apply poison effect
            livingEntity.addStatusEffect(
                new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.POISON, POISON_DURATION, POISON_AMPLIFIER)
            );
            
            // --- Knockback ---
            // Calculate a normalized vector from projectile to target.
            Vec3d projectilePos = this.getPos();
            Vec3d targetPos = target.getPos();
            Vec3d knockbackDir = targetPos.subtract(projectilePos).normalize();
            livingEntity.addVelocity(knockbackDir.x * KNOCKBACK_STRENGTH, 0.2, knockbackDir.z * KNOCKBACK_STRENGTH);
            livingEntity.velocityModified = true;

            // --- Layered Sound Effects with Pitch Modulation ---
            // First layer: a deep, ominous sound.
            float pitch1 = 0.8F + world.getRandom().nextFloat() * 0.2F;
            world.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.PLAYERS, 1.0F, pitch1);
            // Second layer: a sharp impact sound.
            float pitch2 = 1.0F + world.getRandom().nextFloat() * 0.3F;
            world.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.8F, pitch2);

            // --- Impact Particle Burst ---
            if (world instanceof ServerWorld serverWorld) {
                // Burst of explosion-like particles.
                serverWorld.spawnParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(), this.getZ(),
                        10, 0.5, 0.5, 0.5, 0.1);
                // Burst of crit particles for extra flair.
                serverWorld.spawnParticles(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(),
                        8, 0.3, 0.3, 0.3, 0.05);
                // Spawn some block particles using falling dust (using SAND as a placeholder for your texture).
                serverWorld.spawnParticles(new BlockStateParticleEffect(ParticleTypes.FALLING_DUST, Blocks.SAND.getDefaultState()),
                        this.getX(), this.getY(), this.getZ(),
                        5, 0.2, 0.2, 0.2, 0.02);
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event -> {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("animation.snake_head.mouth"));
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
