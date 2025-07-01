package com.ancientcurse.effect;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SandstormTornado {
    // Tornado geometry and particle parameters
    private static final double TORNADO_BASE_RADIUS = 1.5;
    private static final double TORNADO_TOP_RADIUS = 3.5;
    private static final double TORNADO_HEIGHT = 8.0;
    private static final int PARTICLES_PER_RING = 16;
    private static final int NUM_RINGS = 12;
    private static final double PARTICLE_SPEED = 0.15;
    private static final double BASE_ROTATION_SPEED = 0.2;
    private static final double ROTATION_ACCELERATION = 0.05;
    private static final double VERTICAL_SPEED = 0.1;
    private static final double OUTER_SAND_RADIUS = 6.0;
    private static final int OUTER_SAND_PARTICLES = 8;
    private static final double ENTITY_SPIN_SPEED = 15.0;
    private static final int FREEZE_DURATION = 20; // 1 second freeze
    private static final int LIFT_DURATION = 40;   // 2 seconds lift
    private static final int SPIN_DURATION = 60;   // 3 seconds spin
    private static final int THROW_DURATION = 20;  // 1 second throw
    private static final double MAX_LIFT_HEIGHT = 10.0;
    private static final double THROW_STRENGTH = 2.0;
    private static final int TOTAL_LIFESPAN = 70; // Total ticks of the effect
    private static final int LIGHTNING_STRIKE_TICK = TOTAL_LIFESPAN / 2; // Strike halfway through

    private final World world;
    private final BlockPos center;
    private final LivingEntity target;
    private final double initialY;
    private final double initialX;
    private final double initialZ;
    private double currentRotation = 0;
    public int age = 0;
    private boolean hasSpawnedLightning = false;
    private boolean active = true;
    private BlockPos targetPos;

    public SandstormTornado(World world, BlockPos center, LivingEntity target) {
        this.world = world;
        this.center = center;
        this.target = target;
        this.initialY = (target != null) ? target.getY() : center.getY();
        if (target != null) {
            this.initialX = target.getX();
            this.initialZ = target.getZ();
        } else {
            this.initialX = center.getX();
            this.initialZ = center.getZ();
        }
    }

    // Smoothstep easing function for smoother transitions
    private double smoothStep(double t) {
        return t * t * (3 - 2 * t);
    }

    public void tick(MinecraftServer server) {
        if (!active) return;

        age++;
        
        // Strike lightning halfway through the effect
        if (age == LIGHTNING_STRIKE_TICK && !hasSpawnedLightning && target != null) {
            Entity lightning = EntityType.LIGHTNING_BOLT.create(world);
            if (lightning != null) {
                lightning.setPos(target.getX(), target.getY(), target.getZ());
                world.spawnEntity(lightning);
                hasSpawnedLightning = true;
                
                // Play thunder sound
                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,
                    SoundCategory.WEATHER,
                    1.0F, 1.0F);
            }
        }

        if (age > TOTAL_LIFESPAN) {
            active = false;
            cleanup();
            return;
        }

        // Compute fade alpha using smoothstep at the beginning and end
        float fadeAlpha = 1.0F;
        if (age < 20) {
            fadeAlpha = (float) smoothStep(age / 20F);
        } else if (age > TOTAL_LIFESPAN - 20) {
            fadeAlpha = (float) smoothStep((TOTAL_LIFESPAN - age) / 20F);
        }

        // Handle entity movement phases
        if (target != null && target.isAlive()) {
            targetPos = target.getBlockPos();
            if (age <= FREEZE_DURATION) {
                freezeEntity();
            } else if (age <= FREEZE_DURATION + LIFT_DURATION) {
                liftEntity();
            } else if (age <= FREEZE_DURATION + LIFT_DURATION + SPIN_DURATION) {
                spinEntity();
            } else if (age <= FREEZE_DURATION + LIFT_DURATION + SPIN_DURATION + THROW_DURATION) {
                throwEntity();
            }
        }

        // Increase overall rotation with extra acceleration
        currentRotation += BASE_ROTATION_SPEED + (age * ROTATION_ACCELERATION);

        // Only spawn particles on the server side
        if (world instanceof ServerWorld serverWorld) {
            // Main tornado spiral: For each ring from bottom to top
            for (int ring = 0; ring < NUM_RINGS; ring++) {
                double heightRatio = ring / (double) (NUM_RINGS - 1);
                double radiusFactor = Math.pow(heightRatio, 1.5);
                double particleRadius = (TORNADO_BASE_RADIUS + (TORNADO_TOP_RADIUS - TORNADO_BASE_RADIUS) * radiusFactor)
                        * (0.3 + (heightRatio * 0.7));
                
                double heightOffset = smoothStep(heightRatio) * TORNADO_HEIGHT;
                double ringRotationOffset = Math.PI * 2 * Math.pow(heightRatio, 1.2);
                
                for (int i = 0; i < PARTICLES_PER_RING; i++) {
                    double angle = currentRotation + ringRotationOffset + (i * Math.PI * 2 / PARTICLES_PER_RING);
                    double xOffset = Math.cos(angle) * particleRadius;
                    double zOffset = Math.sin(angle) * particleRadius;
                    
                    double randomOffset = (world.random.nextDouble() - 0.5) * 0.3;
                    double wobble = Math.sin(age * 0.3 + ring) * 0.1;
                    
                    double particleX = targetPos.getX() + xOffset + 0.5 + randomOffset;
                    double particleY = targetPos.getY() + heightOffset + wobble;
                    double particleZ = targetPos.getZ() + zOffset + 0.5 + randomOffset;
                    
                    double tangentialSpeed = PARTICLE_SPEED * (1.0 + heightRatio * 0.5)
                            + (world.random.nextDouble() - 0.5) * 0.05;
                    double motionX = -Math.sin(angle) * tangentialSpeed;
                    double motionZ = Math.cos(angle) * tangentialSpeed;
                    double motionY = VERTICAL_SPEED * (1.0 + heightRatio)
                            + (world.random.nextDouble() - 0.5) * 0.03;
                    
                    serverWorld.spawnParticles(
                            new BlockStateParticleEffect(ParticleTypes.FALLING_DUST, Blocks.SAND.getDefaultState()),
                            particleX, particleY, particleZ,
                            1,
                            motionX, motionY, motionZ,
                            fadeAlpha * 0.5
                    );
                    
                    if (i % 4 == 0) {
                        double cloudOffset = 0.3;
                        serverWorld.spawnParticles(
                                ParticleTypes.CLOUD,
                                particleX + (Math.random() - 0.5) * cloudOffset,
                                particleY + (Math.random() - 0.5) * cloudOffset,
                                particleZ + (Math.random() - 0.5) * cloudOffset,
                                1,
                                motionX * 0.7, motionY * 0.7, motionZ * 0.7,
                                0.02 * fadeAlpha
                        );
                    }
                }
            }
            
            // Outer swirling layer of sand particles
            if (age % 2 == 0) {
                for (int i = 0; i < OUTER_SAND_PARTICLES; i++) {
                    double angle = currentRotation * 0.5 + (i * Math.PI * 2 / OUTER_SAND_PARTICLES);
                    double heightVariation = world.random.nextDouble() * TORNADO_HEIGHT;
                    
                    double xOffset = Math.cos(angle) * OUTER_SAND_RADIUS;
                    double zOffset = Math.sin(angle) * OUTER_SAND_RADIUS;
                    
                    double particleX = targetPos.getX() + xOffset + 0.5;
                    double particleY = targetPos.getY() + heightVariation;
                    double particleZ = targetPos.getZ() + zOffset + 0.5;
                    
                    double tangentialSpeed = PARTICLE_SPEED * 0.7;
                    double motionX = -Math.sin(angle) * tangentialSpeed;
                    double motionZ = Math.cos(angle) * tangentialSpeed;
                    double motionY = VERTICAL_SPEED * 0.5;
                    
                    serverWorld.spawnParticles(
                            new BlockStateParticleEffect(ParticleTypes.FALLING_DUST, Blocks.SAND.getDefaultState()),
                            particleX, particleY, particleZ,
                            1,
                            motionX, motionY, motionZ,
                            fadeAlpha * 0.3
                    );
                }
            }
            
            // Dense core layer for extra drama
            if (age % 3 == 0) {
                for (int i = 0; i < 6; i++) {
                    double angle = currentRotation + i * (Math.PI * 2 / 6);
                    double coreRadius = TORNADO_BASE_RADIUS * 0.5;
                    double xOffset = Math.cos(angle) * coreRadius;
                    double zOffset = Math.sin(angle) * coreRadius;
                    
                    double particleX = targetPos.getX() + xOffset + 0.5;
                    double particleY = targetPos.getY() + (TORNADO_HEIGHT * 0.3);
                    double particleZ = targetPos.getZ() + zOffset + 0.5;
                    
                    serverWorld.spawnParticles(
                            new BlockStateParticleEffect(ParticleTypes.FALLING_DUST, Blocks.SAND.getDefaultState()),
                            particleX, particleY, particleZ,
                            1,
                            0, VERTICAL_SPEED, 0,
                            fadeAlpha * 0.6
                    );
                }
            }
            
            // Ambient wind sound with subtle pitch variation
            if (age % 10 == 0) {
                float pitch = 0.5F + world.random.nextFloat() * 0.2F;
                world.playSound(null, 
                        targetPos.getX(), 
                        targetPos.getY(), 
                        targetPos.getZ(),
                        SoundEvents.ENTITY_PHANTOM_FLAP,
                        SoundCategory.AMBIENT,
                        0.3F * fadeAlpha,
                        pitch);
            }
        }
    }

    // Freeze Phase: Immobilize and add a swirling dust aura
    private void freezeEntity() {
        if (target != null) {
            target.setVelocity(0, 0, 0);
            target.setPos(initialX, initialY, initialZ);
            target.velocityModified = true;
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 30, 7));
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 30, 7));
            // Spawn swirling dust around the entity's feet
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.SMOKE, target.getX(), target.getY(), target.getZ(),
                        5, 0.3, 0.1, 0.3, 0.01);
            }
        }
    }

    // Lift Phase: Gradually raise the entity
    private void liftEntity() {
        if (target != null) {
            double liftProgress = (age - FREEZE_DURATION) / (double) LIFT_DURATION;
            double smoothProgress = smoothStep(liftProgress);
            double targetHeight = initialY + (MAX_LIFT_HEIGHT * smoothProgress);
            target.setVelocity(0, 0.2, 0);
            target.setPos(initialX, targetHeight, initialZ);
            target.velocityModified = true;
            // Spawn ascending particles to simulate a rising aura
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.END_ROD, target.getX(), target.getY(), target.getZ(),
                        3, 0.2, 0.5, 0.2, 0.02);
            }
        }
    }

    // Spin Phase: Rotate the entity dramatically
    private void spinEntity() {
        if (target != null) {
            double spinProgress = (age - (FREEZE_DURATION + LIFT_DURATION)) / (double) SPIN_DURATION;
            double easedSpin = smoothStep(spinProgress);
            
            // Calculate spin angle and orbit radius
            double spinAngle = age * ENTITY_SPIN_SPEED;
            double orbitRadius = 2.0 * (1.0 + easedSpin);
            
            // Update position in a circular orbit
            double x = initialX + Math.cos(Math.toRadians(spinAngle)) * orbitRadius;
            double z = initialZ + Math.sin(Math.toRadians(spinAngle)) * orbitRadius;
            
            // Add some vertical bobbing
            double heightOffset = Math.sin(age * 0.2) * 0.5;
            double y = initialY + MAX_LIFT_HEIGHT + heightOffset;
            
            // Set position and update entity rotation
            target.setPos(x, y, z);
            target.setYaw((float)spinAngle % 360);
            target.prevYaw = target.getYaw();
            
            // Add horizontal velocity for spinning motion
            double velX = -Math.sin(Math.toRadians(spinAngle)) * ENTITY_SPIN_SPEED * 0.2;
            double velZ = Math.cos(Math.toRadians(spinAngle)) * ENTITY_SPIN_SPEED * 0.2;
            target.setVelocity(velX, 0, velZ);
            target.velocityModified = true;

            // Spawn crit particles to emphasize the rapid spin
            if (world instanceof ServerWorld serverWorld) {
                for (int i = 0; i < 4; i++) {
                    double particleX = x + (world.random.nextDouble() - 0.5) * 2;
                    double particleZ = z + (world.random.nextDouble() - 0.5) * 2;
                    serverWorld.spawnParticles(ParticleTypes.CRIT, 
                        particleX, y + 1, particleZ,
                        2, 0.2, 0.2, 0.2, 0.05);
                }
            }
        }
    }

    // Throw Phase: Propel the entity with an explosive burst
    private void throwEntity() {
        if (target != null) {
            // Calculate throw angle based on final spin position
            double throwAngle = Math.toRadians(age * ENTITY_SPIN_SPEED);
            
            // Start with upward velocity, then transition to outward throw
            double throwProgress = (age - (FREEZE_DURATION + LIFT_DURATION + SPIN_DURATION)) / (double) THROW_DURATION;
            double upwardForce = Math.max(0, 1.0 - throwProgress * 2) * 0.8;
            
            // Calculate velocities
            double velX = Math.cos(throwAngle) * THROW_STRENGTH * throwProgress;
            double velY = upwardForce;
            double velZ = Math.sin(throwAngle) * THROW_STRENGTH * throwProgress;
            
            // Apply velocities
            target.setVelocity(velX, velY, velZ);
            target.velocityModified = true;

            // Spawn particles and effects
            if (world instanceof ServerWorld serverWorld) {
                // Spawn explosion particles
                serverWorld.spawnParticles(ParticleTypes.EXPLOSION, 
                    target.getX(), target.getY(), target.getZ(),
                    5, 0.5, 0.5, 0.5, 0.1);
            }
            
            // Play explosion sound at the start of throw
            if (throwProgress < 0.1) {
                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0F, 0.8F);
            }
        }
    }

    // Cleanup: Release the entity and play a final sound
    public void cleanup() {
        if (target != null && target.isAlive()) {
            target.setVelocity(0, -0.1, 0);
        }
        world.playSound(null, center.getX(), center.getY(), center.getZ(),
                SoundEvents.BLOCK_SAND_BREAK, SoundCategory.AMBIENT,
                1.0F, 0.5F);
    }
} 