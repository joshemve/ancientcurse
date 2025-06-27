package com.example.egyptianweapons.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.world.RaycastContext;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

public class SunRayBeamEffect {
    private static final float BEAM_DAMAGE = 2.0f; // Damage per tick
    private static final int CHARGE_TICKS = 10; // 0.5 seconds
    private static final int MAX_BEAM_TICKS = 120; // 6 seconds max duration
    private static final float MAX_BEAM_DISTANCE = 32.0f;
    private static final double BEAM_RADIUS = 0.5D;

    public static void createBeamEffect(World world, LivingEntity user, float chargeTime) {
        // Calculate beam start position (slightly in front of the user)
        Vec3d startPos = user.getEyePos();
        Vec3d lookVec = user.getRotationVector();
        Vec3d endPos = startPos.add(lookVec.multiply(MAX_BEAM_DISTANCE));

        // Perform ray trace
        HitResult hitResult = world.raycast(new RaycastContext(
            startPos,
            endPos,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            user
        ));

        // Update end position based on hit
        if (hitResult.getType() != HitResult.Type.MISS) {
            endPos = hitResult.getPos();
        }

        // Apply effects along beam path
        applyBeamEffects(world, user, startPos, endPos, chargeTime >= CHARGE_TICKS);

        // Handle block interactions
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            handleBlockInteraction(world, ((BlockHitResult) hitResult).getBlockPos());
        }

        // Play sound effects
        playBeamSounds(world, user, chargeTime);

        // Spawn particles
        spawnBeamParticles(world, startPos, endPos);

        // Optional: If beam is fully charged, spawn extra luminous particles
        if (chargeTime >= CHARGE_TICKS) {
            spawnExtraLuminousParticles(world, startPos, endPos);
        }
    }

    private static void applyBeamEffects(World world, LivingEntity user, Vec3d start, Vec3d end, boolean fullyCharged) {
        // Calculate beam box for entity detection
        Vec3d diff = end.subtract(start);
        Box beamBox = new Box(
            Math.min(start.x, end.x) - BEAM_RADIUS,
            Math.min(start.y, end.y) - BEAM_RADIUS,
            Math.min(start.z, end.z) - BEAM_RADIUS,
            Math.max(start.x, end.x) + BEAM_RADIUS,
            Math.max(start.y, end.y) + BEAM_RADIUS,
            Math.max(start.z, end.z) + BEAM_RADIUS
        );

        // Get entities in beam path
        List<Entity> entities = world.getEntitiesByClass(
            Entity.class,
            beamBox,
            entity -> entity != user && entity instanceof LivingEntity
        );

        // Apply effects to entities
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity livingEntity) {
                // Apply damage
                livingEntity.damage(world.getDamageSources().magic(), BEAM_DAMAGE);
                
                // Set on fire
                livingEntity.setOnFireFor(3);
                
                // Apply status effects
                livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 1));
                
                if (fullyCharged) {
                    livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0));
                }
            }
        }
    }

    private static void handleBlockInteraction(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        // Handle ice and snow melting
        if (block == Blocks.ICE || block == Blocks.SNOW || block == Blocks.SNOW_BLOCK) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState());
            world.addParticle(ParticleTypes.CLOUD, 
                pos.getX() + 0.5, 
                pos.getY() + 0.5, 
                pos.getZ() + 0.5, 
                0, 0.1, 0
            );
        }
    }

    private static void playBeamSounds(World world, LivingEntity user, float chargeTime) {
        if (chargeTime == 1) { // Start charging
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.BLOCK_BEACON_ACTIVATE,
                SoundCategory.PLAYERS, 1.0F, 2.0F);
        } else if (chargeTime >= CHARGE_TICKS) { // Fully charged
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.BLOCK_BEACON_POWER_SELECT,
                SoundCategory.PLAYERS, 0.5F, 1.0F);
        }
    }

    private static void spawnBeamParticles(World world, Vec3d start, Vec3d end) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        // Calculate particle positions along beam
        Vec3d direction = end.subtract(start).normalize();
        double distance = start.distanceTo(end);
        int particles = (int) (distance * 10); // Increased particles per block

        for (int i = 0; i < particles; i++) {
            double progress = i / (double) particles;
            Vec3d pos = start.add(direction.multiply(distance * progress));

            // Add slight randomness to core beam particles
            double spread = 0.05;
            pos = pos.add(
                (world.random.nextDouble() - 0.5) * spread,
                (world.random.nextDouble() - 0.5) * spread,
                (world.random.nextDouble() - 0.5) * spread
            );

            // Spawn core beam particles
            serverWorld.spawnParticles(
                ParticleTypes.END_ROD,
                pos.x, pos.y, pos.z,
                1, // count
                0, 0, 0, // offset
                0 // speed
            );

            // Spawn heat distortion particles
            if (world.random.nextFloat() < 0.3) {
                serverWorld.spawnParticles(
                    ParticleTypes.SMOKE,
                    pos.x, pos.y, pos.z,
                    1, // count
                    0, 0.05, 0, // offset
                    0 // speed
                );
            }
        }
    }

    private static void spawnExtraLuminousParticles(World world, Vec3d start, Vec3d end) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        Vec3d direction = end.subtract(start).normalize();
        double distance = start.distanceTo(end);
        int extraParticles = (int) (distance * 5); // Increased particles

        for (int i = 0; i < extraParticles; i++) {
            double progress = i / (double) extraParticles;
            Vec3d pos = start.add(direction.multiply(distance * progress));

            // Add randomness for a thicker beam
            double spread = 0.15; // Increased spread
            pos = pos.add(
                (world.random.nextDouble() - 0.5) * spread,
                (world.random.nextDouble() - 0.5) * spread,
                (world.random.nextDouble() - 0.5) * spread
            );

            // Spawn extra luminous particles
            serverWorld.spawnParticles(
                ParticleTypes.END_ROD,
                pos.x, pos.y, pos.z,
                1, // count
                (world.random.nextDouble() - 0.5) * 0.01,
                (world.random.nextDouble() - 0.5) * 0.01,
                (world.random.nextDouble() - 0.5) * 0.01,
                0.01 // speed
            );
        }
    }
}
