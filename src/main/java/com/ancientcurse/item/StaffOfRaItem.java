package com.ancientcurse.item;

import com.ancientcurse.ModBlocks;
import com.ancientcurse.network.CurseZonePackets;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.random.Random;
import org.joml.Vector3f;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.object.PlayState;

public class StaffOfRaItem extends CustomAnimatedItem {
    
    // Configuration constants
    private static final double BEAM_RANGE = 32.0D;
    private static final float BASE_DAMAGE = 4.0F;
    private static final float MAX_DAMAGE = 12.0F;
    private static final int CHARGE_TIME = 60; // 3 seconds for full charge
    
    // Sun beam colors
    private static final Vector3f CORE_COLOR = new Vector3f(1.0F, 1.0F, 0.9F); // Bright white-yellow
    private static final Vector3f INNER_COLOR = new Vector3f(1.0F, 0.95F, 0.7F); // Warm yellow
    private static final Vector3f OUTER_COLOR = new Vector3f(1.0F, 0.85F, 0.4F); // Golden orange

    public StaffOfRaItem(Item.Settings settings, String name) {
        super(settings, name);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private <T extends software.bernie.geckolib.core.animatable.GeoAnimatable> PlayState predicate(AnimationState<T> tAnimationState) {
        // Always play the sun animation
        tAnimationState.getController().setAnimation(RawAnimation.begin()
                .then("animation.ancientcurse.staff_of_ra.sun", Animation.LoopType.LOOP));
        return PlayState.CONTINUE;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 72000;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        
        // Only set current hand if not already using
        if (!user.isUsingItem()) {
            user.setCurrentHand(hand);
            
            // Initial activation sound
            if (!world.isClient) {
                world.playSound(null, user.getX(), user.getY(), user.getZ(), 
                    SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.5F, 1.5F);
            }
            
            return TypedActionResult.consume(itemStack);
        }
        
        return TypedActionResult.pass(itemStack);
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (world.isClient) {
            return;
        }

        if (user instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) user;
            Vec3d eyePos = player.getEyePos();
            Vec3d lookVec = player.getRotationVec(1.0F);
            
            // Calculate charge level
            int usedTicks = getMaxUseTime(stack) - remainingUseTicks;
            float chargeLevel = Math.min(1.0F, (float)usedTicks / CHARGE_TIME);
            
            // Ambient charging sound
            if (usedTicks % 20 == 0 && chargeLevel < 1.0F) {
                world.playSound(null, player.getX(), player.getY(), player.getZ(), 
                    SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.3F, 0.5F + chargeLevel);
            }
            
            // Continuous beam sound
            if (usedTicks % 10 == 0) {
                world.playSound(null, player.getX(), player.getY(), player.getZ(), 
                    SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.PLAYERS, 0.4F, 1.2F);
            }
            
            // Create sun beam effect
            createSunBeam((ServerWorld) world, eyePos, lookVec, chargeLevel);
            
            // Check for hits
            HitResult hitResult = player.raycast(BEAM_RANGE, 1.0F, false);
            processHit(world, player, hitResult, chargeLevel);
        }
    }
    
    private void createSunBeam(ServerWorld world, Vec3d start, Vec3d direction, float chargeLevel) {
        Random random = world.getRandom();
        double beamLength = BEAM_RANGE;
        
        // Beam gets more intense with charge
        float intensity = 0.3F + (0.7F * chargeLevel);
        
        // Main beam path
        for (double distance = 0; distance < beamLength; distance += 0.2D) {
            Vec3d pos = start.add(direction.multiply(distance));
            
            // Calculate beam width based on distance (slightly widens)
            double beamWidth = 0.1D + (distance * 0.01D);
            
            // Core beam - bright white center
            if (distance % 0.4D < 0.2D) {
                world.spawnParticles(
                    new DustParticleEffect(CORE_COLOR, 1.0F * intensity),
                    pos.x, pos.y, pos.z,
                    1, 0, 0, 0, 0
                );
            }
            
            // Inner glow - warm yellow
            double innerOffset = beamWidth * 0.3D;
            for (int i = 0; i < 3; i++) {
                double angle = (Math.PI * 2) * i / 3;
                double offsetX = Math.cos(angle) * innerOffset;
                double offsetZ = Math.sin(angle) * innerOffset;
                
                world.spawnParticles(
                    new DustParticleEffect(INNER_COLOR, 0.8F * intensity),
                    pos.x + offsetX, pos.y, pos.z + offsetZ,
                    1, 0, 0, 0, 0
                );
            }
            
            // Outer halo - golden particles
            if (distance % 0.6D < 0.2D) {
                double haloOffset = beamWidth * 0.6D;
                for (int i = 0; i < 6; i++) {
                    double angle = (Math.PI * 2) * i / 6 + (distance * 0.1);
                    double offsetX = Math.cos(angle) * haloOffset;
                    double offsetZ = Math.sin(angle) * haloOffset;
                    
                    world.spawnParticles(
                        new DustParticleEffect(OUTER_COLOR, 0.6F * intensity),
                        pos.x + offsetX, pos.y, pos.z + offsetZ,
                        1, 0, 0, 0, 0
                    );
                }
            }
            
            // Sparkles along the beam
            if (random.nextFloat() < 0.1F * intensity) {
                double sparkleOffset = random.nextDouble() * beamWidth;
                double sparkleAngle = random.nextDouble() * Math.PI * 2;
                
                world.spawnParticles(
                    ParticleTypes.END_ROD,
                    pos.x + Math.cos(sparkleAngle) * sparkleOffset,
                    pos.y + (random.nextDouble() - 0.5) * beamWidth,
                    pos.z + Math.sin(sparkleAngle) * sparkleOffset,
                    1, 0, 0, 0, 0.01
                );
            }
            
            // God rays effect - occasional bright flashes
            if (distance % 2.0D < 0.2D && random.nextFloat() < 0.3F * intensity) {
                world.spawnParticles(
                    ParticleTypes.INSTANT_EFFECT,
                    pos.x, pos.y, pos.z,
                    2, beamWidth, beamWidth, beamWidth, 0
                );
            }
        }
        
        // Source glow effect around the staff
        if (chargeLevel > 0.5F) {
            for (int i = 0; i < 3; i++) {
                double angle = (Math.PI * 2) * i / 3 + (world.getTime() * 0.1);
                double radius = 0.5D;
                
                world.spawnParticles(
                    new DustParticleEffect(INNER_COLOR, 1.5F),
                    start.x + Math.cos(angle) * radius,
                    start.y,
                    start.z + Math.sin(angle) * radius,
                    1, 0.1, 0.1, 0.1, 0
                );
            }
        }
    }
    
    private void processHit(World world, PlayerEntity player, HitResult hitResult, float chargeLevel) {
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) hitResult;
            if (entityHit.getEntity() instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) entityHit.getEntity();
                
                // Scale damage with charge
                float damage = BASE_DAMAGE + (MAX_DAMAGE - BASE_DAMAGE) * chargeLevel;
                target.damage(world.getDamageSources().magic(), damage);
                
                // Fire duration scales with charge
                target.setOnFireFor((int)(4 + 8 * chargeLevel));
                
                // Status effects
                // Sun flash effect - blinding light of the sun god (scales with charge)
                CurseZonePackets.sendSunFlash((ServerWorld) world, target.getBlockPos(),
                        0.5f + 0.5f * chargeLevel, (int)(40 + 40 * chargeLevel), 32.0);
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, (int)(60 + 60 * chargeLevel), 1));
                
                // Bonus damage to undead
                if (target.isUndead()) {
                    target.damage(world.getDamageSources().magic(), damage * 0.5F);
                }
                
                // Impact effects
                createImpactEffect((ServerWorld) world, entityHit.getPos(), chargeLevel, true);
            }
        } else if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hitResult;
            
            // Set fires with charge-based intensity
            if (chargeLevel > 0.5F) {
                createFireBurst(world, blockHit.getBlockPos(), chargeLevel);
            }
            
            // Impact effects
            createImpactEffect((ServerWorld) world, blockHit.getPos(), chargeLevel, false);
        }
    }
    
    private void createImpactEffect(ServerWorld world, Vec3d pos, float chargeLevel, boolean isEntity) {
        // Sun burst effect
        int particleCount = (int)(10 + 20 * chargeLevel);
        
        // Central flash
        world.spawnParticles(
            ParticleTypes.INSTANT_EFFECT,
            pos.x, pos.y, pos.z,
            (int)(3 * chargeLevel), 0.1, 0.1, 0.1, 0
        );
        
        // Radial burst
        for (int i = 0; i < particleCount; i++) {
            double angle = (Math.PI * 2) * i / particleCount;
            double speed = 0.3D + 0.2D * chargeLevel;
            
            world.spawnParticles(
                new DustParticleEffect(OUTER_COLOR, 1.5F),
                pos.x, pos.y, pos.z,
                0, 
                Math.cos(angle) * speed,
                0.1,
                Math.sin(angle) * speed,
                1.0
            );
        }
        
        // Sound effect
        world.playSound(null, pos.x, pos.y, pos.z,
            isEntity ? SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST : SoundEvents.BLOCK_FIRE_EXTINGUISH,
            SoundCategory.PLAYERS, 0.7F, 1.2F);
    }
    
    private void createFireBurst(World world, BlockPos center, float chargeLevel) {
        int radius = (int)(1 + 2 * chargeLevel);
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + z * z <= radius * radius) {
                        BlockPos firePos = center.add(x, y, z);
                        if (world.getBlockState(firePos).isAir() && 
                            world.getRandom().nextFloat() < 0.3F * chargeLevel) {
                            world.setBlockState(firePos, Blocks.FIRE.getDefaultState());
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!world.isClient) {
            // Release sound
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 0.5F, 1.0F);
            
            // Clear the current hand to stop animation
            if (user instanceof PlayerEntity) {
                ((PlayerEntity) user).clearActiveItem();
            }
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, net.minecraft.entity.Entity entity, int slot, boolean selected) {
        if (world.isClient) {
            return;
        }

        if (entity instanceof PlayerEntity && selected) {
            PlayerEntity player = (PlayerEntity) entity;
            
            // Ambient glow particles when held
            if (world.getTime() % 20 == 0) {
                ((ServerWorld) world).spawnParticles(
                    new DustParticleEffect(INNER_COLOR, 0.5F),
                    player.getX(), player.getY() + 1, player.getZ(),
                    3, 0.3, 0.3, 0.3, 0.01
                );
            }
        }
    }
} 