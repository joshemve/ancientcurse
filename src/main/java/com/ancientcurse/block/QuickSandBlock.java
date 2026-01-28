package com.ancientcurse.block;

import com.ancientcurse.AncientCurse;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * QuickSand block that traps and slowly kills entities.
 *
 * Mechanics:
 * - Entities sink slowly into the quicksand
 * - Movement is severely restricted (struggling makes you sink faster)
 * - Suffocation damage when fully submerged
 * - Unique death message
 * - Visual and audio feedback (bubbles, sucking sounds)
 * - Affects all entities (players, mobs, items)
 */
public class QuickSandBlock extends Block {

    // Custom damage type for quicksand suffocation
    public static final RegistryKey<DamageType> QUICKSAND_DAMAGE = RegistryKey.of(
        RegistryKeys.DAMAGE_TYPE,
        new Identifier(AncientCurse.MOD_ID, "quicksand")
    );

    // Collision shape - slightly shorter to allow entities to sink in
    private static final VoxelShape COLLISION_SHAPE = Block.createCuboidShape(0, 0, 0, 16, 14, 16);

    // How deep an entity has sunk (tracked via Y position relative to block)
    private static final float SINK_RATE_STILL = 0.02f;      // Sink rate when not moving
    private static final float SINK_RATE_STRUGGLING = 0.04f; // Sink rate when moving (struggling)
    private static final float MAX_HORIZONTAL_SPEED = 0.01f; // Max horizontal movement allowed
    private static final int SUFFOCATION_DEPTH_TICKS = 60;   // Ticks before taking damage when deep
    private static final float SUFFOCATION_DAMAGE = 1.5f;    // Damage per tick when suffocating

    public QuickSandBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        // Return slightly shorter collision so entities sink in
        return COLLISION_SHAPE;
    }

    @Override
    public VoxelShape getSidesShape(BlockState state, BlockView world, BlockPos pos) {
        return VoxelShapes.fullCube();
    }

    @Override
    public VoxelShape getCameraCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.fullCube();
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (entity.isSpectator()) {
            return;
        }

        // Calculate how deep the entity is in the quicksand
        double entityBottom = entity.getY();
        double blockTop = pos.getY() + 1.0;
        double depth = blockTop - entityBottom;

        // Get current velocity to detect struggling
        Vec3d velocity = entity.getVelocity();
        boolean isStruggling = Math.abs(velocity.x) > 0.01 || Math.abs(velocity.z) > 0.01 || velocity.y > 0.01;

        // Calculate sink rate (struggling makes you sink faster!)
        float sinkRate = isStruggling ? SINK_RATE_STRUGGLING : SINK_RATE_STILL;

        // Apply sinking effect
        double newVelY = Math.min(velocity.y, -sinkRate);

        // Severely restrict horizontal movement
        double dampedX = velocity.x * 0.1;
        double dampedZ = velocity.z * 0.1;

        // Clamp horizontal speed
        dampedX = Math.max(-MAX_HORIZONTAL_SPEED, Math.min(MAX_HORIZONTAL_SPEED, dampedX));
        dampedZ = Math.max(-MAX_HORIZONTAL_SPEED, Math.min(MAX_HORIZONTAL_SPEED, dampedZ));

        entity.setVelocity(dampedX, newVelY, dampedZ);

        // Slow movement effect
        entity.slowMovement(state, new Vec3d(0.1, 0.5, 0.1));

        // Handle living entities
        if (entity instanceof LivingEntity livingEntity) {
            // Apply blindness when deep enough (eyes covered)
            if (depth > 1.5) {
                livingEntity.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.BLINDNESS, 40, 0, false, false, true
                ));
            }

            // Apply slowness always
            livingEntity.addStatusEffect(new StatusEffectInstance(
                StatusEffects.SLOWNESS, 40, 3, false, false, true
            ));

            // Apply mining fatigue (can't dig out easily)
            livingEntity.addStatusEffect(new StatusEffectInstance(
                StatusEffects.MINING_FATIGUE, 40, 2, false, false, true
            ));

            // Suffocation damage when deep enough
            if (depth > 1.8 && !world.isClient) {
                // Check if entity has been submerged long enough
                int airTicks = livingEntity.getAir();
                if (airTicks > 0) {
                    // Drain air faster than water
                    livingEntity.setAir(Math.max(0, airTicks - 4));
                } else {
                    // Deal suffocation damage
                    if (world.getTime() % 10 == 0) {
                        DamageSource damageSource = createQuicksandDamageSource(world);
                        livingEntity.damage(damageSource, SUFFOCATION_DAMAGE);
                    }
                }
            }

            // Sound effects
            if (world.getTime() % 20 == 0 && isStruggling) {
                world.playSound(null, pos, SoundEvents.BLOCK_MUD_STEP, SoundCategory.BLOCKS,
                    0.6f, 0.5f + world.random.nextFloat() * 0.3f);
            }

            // Sucking sound when sinking
            if (world.getTime() % 40 == 0 && depth > 0.3) {
                world.playSound(null, pos, SoundEvents.BLOCK_HONEY_BLOCK_SLIDE, SoundCategory.BLOCKS,
                    0.4f, 0.3f + world.random.nextFloat() * 0.2f);
            }
        }

        // Spawn particles on client
        if (world.isClient) {
            spawnQuicksandParticles(world, entity, pos, isStruggling);
        }

        // Special handling for players
        if (entity instanceof PlayerEntity player) {
            // Reset fall distance while in quicksand
            player.fallDistance = 0;

            // Additional feedback for players
            if (world.isClient && isStruggling && world.getTime() % 10 == 0) {
                // More intense particles when struggling - use sand particles
                BlockStateParticleEffect dustParticle = new BlockStateParticleEffect(ParticleTypes.FALLING_DUST, state);
                for (int i = 0; i < 3; i++) {
                    double offsetX = (world.random.nextDouble() - 0.5) * 0.8;
                    double offsetZ = (world.random.nextDouble() - 0.5) * 0.8;
                    world.addParticle(dustParticle,
                        player.getX() + offsetX,
                        player.getY() + 0.1,
                        player.getZ() + offsetZ,
                        0, 0.05, 0);
                }
            }
        }
    }

    /**
     * Creates the quicksand damage source for death messages
     */
    private DamageSource createQuicksandDamageSource(World world) {
        if (world instanceof ServerWorld serverWorld) {
            return new DamageSource(
                serverWorld.getRegistryManager()
                    .get(RegistryKeys.DAMAGE_TYPE)
                    .entryOf(QUICKSAND_DAMAGE)
            );
        }
        // Fallback for client (shouldn't be used for damage)
        return world.getDamageSources().generic();
    }

    /**
     * Spawn quicksand particles around the entity
     */
    private void spawnQuicksandParticles(World world, Entity entity, BlockPos pos, boolean struggling) {
        Random random = world.random;
        BlockState state = world.getBlockState(pos);

        // Basic ambient particles
        if (random.nextInt(3) == 0) {
            double x = entity.getX() + (random.nextDouble() - 0.5) * entity.getWidth();
            double y = pos.getY() + 1.0;
            double z = entity.getZ() + (random.nextDouble() - 0.5) * entity.getWidth();

            // Sand dust particles
            BlockStateParticleEffect dustParticle = new BlockStateParticleEffect(ParticleTypes.FALLING_DUST, state);
            world.addParticle(dustParticle,
                x, y, z,
                0, 0, 0);
        }

        // Bubble-like particles when struggling
        if (struggling && random.nextInt(2) == 0) {
            double x = entity.getX() + (random.nextDouble() - 0.5) * 0.5;
            double y = pos.getY() + 1.0 + random.nextDouble() * 0.3;
            double z = entity.getZ() + (random.nextDouble() - 0.5) * 0.5;

            world.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                x, y, z,
                (random.nextDouble() - 0.5) * 0.02,
                0.02,
                (random.nextDouble() - 0.5) * 0.02);
        }
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        // Ambient particles on the surface
        if (random.nextInt(16) == 0) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + 1.0;
            double z = pos.getZ() + random.nextDouble();

            // Occasional dust puff
            BlockStateParticleEffect dustParticle = new BlockStateParticleEffect(ParticleTypes.FALLING_DUST, state);
            world.addParticle(dustParticle,
                x, y, z,
                0, 0, 0);
        }

        // Rare bubble effect
        if (random.nextInt(50) == 0) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + 1.0;
            double z = pos.getZ() + random.nextDouble();

            world.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                x, y, z,
                0, 0.01, 0);

            // Subtle bubbling sound
            world.playSound(x, y, z, SoundEvents.BLOCK_BUBBLE_COLUMN_BUBBLE_POP,
                SoundCategory.BLOCKS, 0.1f, 0.5f + random.nextFloat() * 0.3f, false);
        }
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (!entity.isSpectator() && entity instanceof LivingEntity) {
            // Play initial stepping sound
            world.playSound(null, pos, SoundEvents.BLOCK_MUD_STEP, SoundCategory.BLOCKS,
                0.5f, 0.6f + world.random.nextFloat() * 0.2f);
        }
        super.onSteppedOn(world, pos, state, entity);
    }

    @Override
    public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        // No fall damage in quicksand - it cushions the fall but traps you
        entity.handleFallDamage(fallDistance, 0.0f, world.getDamageSources().fall());

        if (!entity.isSpectator()) {
            // Loud splash effect when landing
            world.playSound(null, pos, SoundEvents.BLOCK_MUD_BREAK, SoundCategory.BLOCKS,
                0.8f, 0.4f + world.random.nextFloat() * 0.2f);

            // Spawn lots of particles on landing
            if (world.isClient) {
                BlockStateParticleEffect dustParticle = new BlockStateParticleEffect(ParticleTypes.FALLING_DUST, state);
                for (int i = 0; i < 10; i++) {
                    double offsetX = (world.random.nextDouble() - 0.5) * 1.5;
                    double offsetZ = (world.random.nextDouble() - 0.5) * 1.5;
                    world.addParticle(dustParticle,
                        entity.getX() + offsetX,
                        pos.getY() + 1.0,
                        entity.getZ() + offsetZ,
                        0, 0.1, 0);
                }
            }
        }
    }
}
