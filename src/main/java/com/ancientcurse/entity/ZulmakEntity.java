package com.ancientcurse.entity;

import com.ancientcurse.ModBlocks;
import com.ancientcurse.ModEntities;
import com.ancientcurse.ModParticles;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

/**
 * Zulmak Entity - A powerful hostile mob
 *
 * Animation system ready for expansion:
 * - Currently supports: idle
 * - Add more animations to zulmak.animation.json as needed
 * - Use attack states and data trackers to sync animations client/server
 */
public class ZulmakEntity extends HostileEntity implements GeoEntity {

    /* ---------- DATA TRACKERS ---------- */
    // Used for client/server sync of animation states
    private static final TrackedData<Integer> ATTACK_STATE =
            DataTracker.registerData(ZulmakEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> IS_ATTACKING =
            DataTracker.registerData(ZulmakEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    /* ---------- CONSTANTS ---------- */
    // Attack state constants
    public static final int ATTACK_NONE = 0;
    public static final int ATTACK_MELEE = 1;
    public static final int STATE_BLOCKING = 2;
    public static final int ATTACK_SUMMON = 3;

    // Animation durations in ticks (adjusted for 0.6x speed)
    // Original: 0.5s = 10 ticks, at 0.6x speed = ~17 ticks
    private static final int ATTACK_1_DURATION_TICKS = 17;
    // Windup delay before damage is dealt (about 40% through the animation)
    private static final int ATTACK_1_DAMAGE_DELAY_TICKS = 7;
    // Original: 0.5833s ≈ 12 ticks, at 0.6x speed = 20 ticks
    private static final int DEATH_ANIMATION_TICKS = 20;
    // Blocking animation: 1.0s = 20 ticks, at 0.6x speed = ~33 ticks
    private static final int BLOCKING_DURATION_TICKS = 33;
    // Chance to block when taking damage (20%)
    private static final float BLOCK_CHANCE = 0.20f;
    // Cooldown between blocks (5 seconds = 100 ticks)
    private static final int BLOCK_COOLDOWN_TICKS = 100;

    // Summon attack: attack_2 is 0.5s looping at 0.6x speed = ~17 ticks per loop
    // Channel for 2 seconds (4 loops) before spawning
    private static final int SUMMON_CHANNEL_TICKS = 40; // 2 seconds of channeling
    private static final int SUMMON_COOLDOWN_TICKS = 600; // 30 seconds between summons
    private static final int MAX_SUMMONED_PHARAOHS = 2; // Max minions at once

    /* ---------- FIELDS ---------- */
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private ServerBossBar bossBar;

    // Animation timing
    private int attackAnimationTicks = 0;
    private int attackDamageDelayTicks = 0;
    private net.minecraft.entity.Entity pendingAttackTarget = null;

    // Blocking state
    private int blockingTicks = 0;
    private int blockCooldownTicks = 0;

    // Summon state
    private int summonChannelTicks = 0;
    private int summonCooldownTicks = 0;

    // Track attack state transitions for animation reset
    private int lastAttackState = ATTACK_NONE;

    // Boss bar optimization
    private float lastHealthPercentage = 1.0f;
    private int bossBarUpdateCooldown = 0;
    private static final double BOSS_BAR_RANGE = 64.0; // Range for boss bar visibility

    // Particle system - matches spinning blocks animation (360 degrees in 0.5 seconds = 720 deg/sec)
    private float particleAngleTier1 = 0;
    private float particleAngleTier2 = 0;
    private static final float PARTICLE_ROTATION_SPEED_TIER1 = -36f; // degrees per tick (720/20) - matches tier_1 rotation
    private static final float PARTICLE_ROTATION_SPEED_TIER2 = 36f;  // opposite direction for tier_2
    private static final double TIER1_Y_OFFSET = 1.8;  // Height of tier_1 spinning blocks (28.8 - base) / 16
    private static final double TIER2_Y_OFFSET = 1.0;  // Height of tier_2 spinning blocks (16 - base) / 16
    private static final double PARTICLE_ORBIT_RADIUS = 1.2; // Distance from entity center

    public ZulmakEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 20; // XP dropped on death
    }

    /* ---------- ATTRIBUTES ---------- */
    public static DefaultAttributeContainer.Builder createZulmakAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 80.0) // Tanky mob
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28) // Moderately fast
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 12.0) // Strong attacks
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0) // Detection range
                .add(EntityAttributes.GENERIC_ARMOR, 6.0) // Some armor
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.4); // Resist knockback
    }

    /* ---------- INITIALIZATION ---------- */
    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(ATTACK_STATE, ATTACK_NONE);
        this.dataTracker.startTracking(IS_ATTACKING, false);
    }

    @Override
    protected void initGoals() {
        // Combat goals
        this.goalSelector.add(1, new SwimGoal(this));
        // TODO: Add ZulmakSummonGoal when attack_2 animation is implemented
        // this.goalSelector.add(2, new ZulmakSummonGoal(this)); // Summon takes priority when conditions met
        this.goalSelector.add(2, new ZulmakMeleeAttackGoal(this, 1.2D, false));
        this.goalSelector.add(3, new WanderAroundFarGoal(this, 0.8D));
        this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(5, new LookAroundGoal(this));

        // Target selection
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    /**
     * Custom melee attack goal that respects the attack animation state.
     * Only allows attacks when no attack animation is currently playing.
     * Pauses while blocking.
     */
    private static class ZulmakMeleeAttackGoal extends MeleeAttackGoal {
        private final ZulmakEntity zulmak;
        private int attackCooldown = 0;

        public ZulmakMeleeAttackGoal(ZulmakEntity zulmak, double speed, boolean pauseWhenMobIdle) {
            super(zulmak, speed, pauseWhenMobIdle);
            this.zulmak = zulmak;
        }

        @Override
        public boolean canStart() {
            // Don't start if blocking
            if (zulmak.isBlocking()) {
                return false;
            }
            return super.canStart();
        }

        @Override
        public boolean shouldContinue() {
            // Stop pursuing if blocking
            if (zulmak.isBlocking()) {
                return false;
            }
            return super.shouldContinue();
        }

        @Override
        public void tick() {
            // Don't do anything while blocking
            if (zulmak.isBlocking()) {
                return;
            }
            // Decrement our cooldown
            if (attackCooldown > 0) {
                attackCooldown--;
            }
            super.tick();
        }

        @Override
        protected void attack(LivingEntity target, double squaredDistance) {
            // Don't attack while blocking
            if (zulmak.isBlocking()) {
                return;
            }

            double attackReach = this.getSquaredMaxAttackDistance(target);

            // Only attack if:
            // 1. In range
            // 2. Not already in an attack animation
            // 3. Our cooldown has expired
            if (squaredDistance <= attackReach && attackCooldown <= 0 && zulmak.getAttackState() == ATTACK_NONE) {
                // Reset cooldown to match the animation duration plus a small buffer
                attackCooldown = ATTACK_1_DURATION_TICKS + 5;

                // Swing hand for visual feedback
                this.mob.swingHand(Hand.MAIN_HAND);

                // Call tryAttack which will start the animation and queue damage
                this.mob.tryAttack(target);
            }
        }

        @Override
        protected double getSquaredMaxAttackDistance(LivingEntity entity) {
            // Slightly larger attack range for a boss
            return 4.0 + entity.getWidth();
        }
    }

    /**
     * Summon goal - Zulmak channels and summons a Withered Pharaoh minion.
     * Uses attack_2 animation (arms raised channeling pose).
     */
    private static class ZulmakSummonGoal extends Goal {
        private final ZulmakEntity zulmak;

        public ZulmakSummonGoal(ZulmakEntity zulmak) {
            this.zulmak = zulmak;
        }

        @Override
        public boolean canStart() {
            // Only summon if:
            // 1. Has a target
            // 2. Not in another attack state
            // 3. Summon cooldown expired
            // 4. Not at max minions
            // 5. Health below 75% (gets more aggressive when hurt)
            if (zulmak.getTarget() == null || !zulmak.getTarget().isAlive()) return false;
            if (zulmak.getAttackState() != ATTACK_NONE) return false;
            if (zulmak.summonCooldownTicks > 0) return false;
            if (zulmak.getHealth() > zulmak.getMaxHealth() * 0.75f) return false;

            // Count existing summoned pharaohs nearby
            int pharaohCount = zulmak.getWorld().getEntitiesByClass(
                    WitheredPharaohEntity.class,
                    new Box(zulmak.getBlockPos()).expand(32),
                    pharaoh -> pharaoh.isAlive()
            ).size();

            return pharaohCount < MAX_SUMMONED_PHARAOHS;
        }

        @Override
        public boolean shouldContinue() {
            // Continue while channeling
            return zulmak.getAttackState() == ATTACK_SUMMON && zulmak.summonChannelTicks > 0;
        }

        @Override
        public void start() {
            zulmak.setAttackState(ATTACK_SUMMON);
            zulmak.summonChannelTicks = SUMMON_CHANNEL_TICKS;
            zulmak.getNavigation().stop();

            // Play channeling sound
            zulmak.playSound(SoundEvents.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
        }

        @Override
        public void tick() {
            // Stop movement while channeling
            zulmak.getNavigation().stop();
            zulmak.setVelocity(0, zulmak.getVelocity().y, 0);

            // Look at target while channeling
            LivingEntity target = zulmak.getTarget();
            if (target != null) {
                zulmak.getLookControl().lookAt(target, 30.0F, 30.0F);
            }

            zulmak.summonChannelTicks--;

            // Spawn particles during channeling
            if (zulmak.getWorld() instanceof ServerWorld serverWorld) {
                // Dark energy swirling around Zulmak
                double angle = (SUMMON_CHANNEL_TICKS - zulmak.summonChannelTicks) * 0.3;
                for (int i = 0; i < 2; i++) {
                    double offsetAngle = angle + (Math.PI * i);
                    double x = zulmak.getX() + Math.cos(offsetAngle) * 1.5;
                    double z = zulmak.getZ() + Math.sin(offsetAngle) * 1.5;
                    serverWorld.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            x, zulmak.getY() + 1.0, z,
                            1, 0.1, 0.5, 0.1, 0.02);
                }

                // Rising smoke from ground
                if (zulmak.summonChannelTicks % 4 == 0) {
                    serverWorld.spawnParticles(ParticleTypes.SMOKE,
                            zulmak.getX(), zulmak.getY(), zulmak.getZ(),
                            5, 1.5, 0.1, 1.5, 0.01);
                }
            }

            // Summon complete
            if (zulmak.summonChannelTicks <= 0) {
                performSummon();
            }
        }

        @Override
        public void stop() {
            zulmak.setAttackState(ATTACK_NONE);
            zulmak.summonCooldownTicks = SUMMON_COOLDOWN_TICKS;
        }

        private void performSummon() {
            if (!(zulmak.getWorld() instanceof ServerWorld serverWorld)) return;

            // Find a valid spawn position near Zulmak for the Withered Pharaoh
            double spawnAngle = zulmak.getRandom().nextDouble() * Math.PI * 2;
            double spawnDist = 2.0 + zulmak.getRandom().nextDouble() * 2.0;
            double spawnX = zulmak.getX() + Math.cos(spawnAngle) * spawnDist;
            double spawnZ = zulmak.getZ() + Math.sin(spawnAngle) * spawnDist;
            double spawnY = zulmak.getY();

            // Create the Withered Pharaoh
            WitheredPharaohEntity pharaoh = ModEntities.WITHERED_PHARAOH.create(serverWorld);
            if (pharaoh != null) {
                pharaoh.refreshPositionAndAngles(spawnX, spawnY, spawnZ, zulmak.getYaw(), 0);
                pharaoh.initialize(serverWorld, serverWorld.getLocalDifficulty(pharaoh.getBlockPos()), SpawnReason.MOB_SUMMONED, null, null);

                // Set target to Zulmak's target
                if (zulmak.getTarget() != null) {
                    pharaoh.setTarget(zulmak.getTarget());
                }

                serverWorld.spawnEntity(pharaoh);

                // Spawn effects at summon location
                serverWorld.spawnParticles(ParticleTypes.SOUL,
                        spawnX, spawnY + 1.0, spawnZ,
                        20, 0.5, 0.5, 0.5, 0.05);
                serverWorld.spawnParticles(ParticleTypes.POOF,
                        spawnX, spawnY, spawnZ,
                        15, 0.5, 0.3, 0.5, 0.05);
            }

            // Spawn Cursed Earth in a radius around Zulmak
            spawnCursedEarth(serverWorld);

            // Spawn Khamsin Spread entities floating around
            spawnKhamsinSpread(serverWorld);

            // Play summon completion sound
            zulmak.playSound(SoundEvents.ENTITY_WITHER_AMBIENT, 1.0f, 0.8f);
        }

        /**
         * Spreads Cursed Earth blocks in a radius around Zulmak
         */
        private void spawnCursedEarth(ServerWorld serverWorld) {
            int radius = 4 + zulmak.getRandom().nextInt(3); // 4-6 block radius
            BlockPos centerPos = zulmak.getBlockPos();

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    // Circular spread pattern
                    if (x * x + z * z > radius * radius) continue;

                    // Random chance to place (creates patchy spread)
                    if (zulmak.getRandom().nextFloat() > 0.4f) continue;

                    BlockPos targetPos = centerPos.add(x, 0, z);

                    // Find the ground level
                    for (int y = 2; y >= -2; y--) {
                        BlockPos checkPos = targetPos.add(0, y, 0);
                        BlockPos abovePos = checkPos.up();

                        // Check if this is a valid spot (solid block with air above)
                        if (serverWorld.getBlockState(checkPos).isSolidBlock(serverWorld, checkPos)
                                && serverWorld.getBlockState(abovePos).isAir()) {

                            // Replace the top block with Cursed Earth
                            serverWorld.setBlockState(checkPos, ModBlocks.CURSED_EARTH.getDefaultState());

                            // Spawn dark particles at conversion
                            if (zulmak.getRandom().nextFloat() < 0.3f) {
                                serverWorld.spawnParticles(ParticleTypes.SOUL,
                                        checkPos.getX() + 0.5, checkPos.getY() + 1.0, checkPos.getZ() + 0.5,
                                        3, 0.3, 0.2, 0.3, 0.02);
                            }
                            break;
                        }
                    }
                }
            }
        }

        /**
         * Spawns Khamsin Spread Small entities floating around Zulmak
         */
        private void spawnKhamsinSpread(ServerWorld serverWorld) {
            int count = 2 + zulmak.getRandom().nextInt(2); // 2-3 entities

            for (int i = 0; i < count; i++) {
                double angle = zulmak.getRandom().nextDouble() * Math.PI * 2;
                double dist = 1.5 + zulmak.getRandom().nextDouble() * 2.5; // 1.5-4 blocks away
                double khamsinX = zulmak.getX() + Math.cos(angle) * dist;
                double khamsinZ = zulmak.getZ() + Math.sin(angle) * dist;
                double khamsinY = zulmak.getY() + 0.5 + zulmak.getRandom().nextDouble() * 1.5; // Floating slightly above ground

                KhamsinSpreadSmallEntity khamsin = ModEntities.KHAMSIN_SPREAD_SMALL.create(serverWorld);
                if (khamsin != null) {
                    khamsin.refreshPositionAndAngles(khamsinX, khamsinY, khamsinZ,
                            zulmak.getRandom().nextFloat() * 360, 0);

                    serverWorld.spawnEntity(khamsin);

                    // Spawn swirling sand particles
                    serverWorld.spawnParticles(ParticleTypes.CLOUD,
                            khamsinX, khamsinY, khamsinZ,
                            10, 0.3, 0.3, 0.3, 0.05);
                }
            }
        }
    }

    /* ---------- TICK & UPDATE ---------- */
    @Override
    public void tick() {
        super.tick();

        // Despawn in peaceful mode (like vanilla hostile mobs)
        if (this.getWorld().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
            // Clean up boss bar before despawning
            if (!this.getWorld().isClient && this.bossBar != null) {
                this.bossBar.setVisible(false);
                this.bossBar.clearPlayers();
                this.bossBar = null;
            }
            this.discard();
            return;
        }

        // Server-side only logic
        if (!this.getWorld().isClient) {
            // Decrement cooldowns
            if (blockCooldownTicks > 0) {
                blockCooldownTicks--;
            }
            if (summonCooldownTicks > 0) {
                summonCooldownTicks--;
            }

            // Handle blocking state
            if (blockingTicks > 0) {
                blockingTicks--;
                // Blocking finished - return to normal
                if (blockingTicks <= 0) {
                    setAttackState(ATTACK_NONE);
                }
            }

            // Attack animation and damage timing management
            if (attackAnimationTicks > 0) {
                attackAnimationTicks--;

                // Handle delayed damage - deal damage after windup
                if (attackDamageDelayTicks > 0) {
                    attackDamageDelayTicks--;
                    if (attackDamageDelayTicks <= 0 && pendingAttackTarget != null) {
                        // Now deal the actual damage
                        performDelayedAttack(pendingAttackTarget);
                        pendingAttackTarget = null;
                    }
                }

                // Animation finished - reset attack state
                if (attackAnimationTicks <= 0) {
                    setAttackState(ATTACK_NONE);
                    setAttacking(false);
                    pendingAttackTarget = null;
                }
            }

            // Handle boss bar
            handleBossBar();
        }

        // Client-side particle effects - spinning around the spinning blocks
        if (this.getWorld().isClient) {
            spawnOrbitingParticles();
        }
    }

    /**
     * Spawns particles that orbit around Zulmak's spinning blocks.
     * Particles match the rotation direction of tier_1 and tier_2 bones.
     */
    private void spawnOrbitingParticles() {
        // Update rotation angles (matches animation speed)
        particleAngleTier1 += PARTICLE_ROTATION_SPEED_TIER1;
        particleAngleTier2 += PARTICLE_ROTATION_SPEED_TIER2;

        // Keep angles in range
        if (particleAngleTier1 < 0) particleAngleTier1 += 360;
        if (particleAngleTier2 >= 360) particleAngleTier2 -= 360;

        // Spawn particles every few ticks to avoid overwhelming the system
        if (this.age % 2 == 0) {
            // Tier 1 particles (upper spinning blocks) - 3 particles evenly spaced
            for (int i = 0; i < 3; i++) {
                double angle1 = Math.toRadians(particleAngleTier1 + (i * 120)); // 120 degrees apart
                double x1 = this.getX() + Math.cos(angle1) * PARTICLE_ORBIT_RADIUS;
                double z1 = this.getZ() + Math.sin(angle1) * PARTICLE_ORBIT_RADIUS;
                double y1 = this.getY() + TIER1_Y_OFFSET;

                // Calculate tangential velocity for smooth orbital motion
                double velX1 = -Math.sin(angle1) * 0.05 * Math.signum(PARTICLE_ROTATION_SPEED_TIER1);
                double velZ1 = Math.cos(angle1) * 0.05 * Math.signum(PARTICLE_ROTATION_SPEED_TIER1);

                this.getWorld().addParticle(ModParticles.ZULMAK_PARTICLE, x1, y1, z1, velX1, 0.01, velZ1);
            }

            // Tier 2 particles (lower spinning blocks) - 3 particles evenly spaced
            for (int i = 0; i < 3; i++) {
                double angle2 = Math.toRadians(particleAngleTier2 + (i * 120)); // 120 degrees apart
                double x2 = this.getX() + Math.cos(angle2) * PARTICLE_ORBIT_RADIUS;
                double z2 = this.getZ() + Math.sin(angle2) * PARTICLE_ORBIT_RADIUS;
                double y2 = this.getY() + TIER2_Y_OFFSET;

                // Calculate tangential velocity for smooth orbital motion
                double velX2 = -Math.sin(angle2) * 0.05 * Math.signum(PARTICLE_ROTATION_SPEED_TIER2);
                double velZ2 = Math.cos(angle2) * 0.05 * Math.signum(PARTICLE_ROTATION_SPEED_TIER2);

                this.getWorld().addParticle(ModParticles.ZULMAK_PARTICLE, x2, y2, z2, velX2, 0.01, velZ2);
            }
        }
    }

    /* ---------- BOSS BAR ---------- */
    /**
     * Handles boss bar initialization, updates, and player tracking
     */
    private void handleBossBar() {
        // Initialize boss bar if needed
        if (this.bossBar == null) {
            try {
                this.bossBar = new ServerBossBar(
                    Text.literal("Zulmak"),
                    BossBar.Color.RED,
                    BossBar.Style.NOTCHED_10
                );
                this.bossBar.setDarkenSky(true); // Add dramatic effect
            } catch (Exception e) {
                // Skip boss bar if initialization fails
                return;
            }
        }

        try {
            // Only update health percentage if it has changed significantly (more than 1% change)
            float currentHealthPercentage = this.getHealth() / this.getMaxHealth();
            if (Math.abs(currentHealthPercentage - lastHealthPercentage) > 0.01f) {
                this.bossBar.setPercent(currentHealthPercentage);
                lastHealthPercentage = currentHealthPercentage;
            }

            // Only update player list every 20 ticks (1 second) to reduce overhead
            if (bossBarUpdateCooldown <= 0) {
                bossBarUpdateCooldown = 20; // Reset cooldown

                List<PlayerEntity> nearbyPlayers = this.getWorld().getNonSpectatingEntities(
                    PlayerEntity.class,
                    new Box(this.getBlockPos()).expand(BOSS_BAR_RANGE)
                );

                // Add nearby players who aren't already tracking
                for (PlayerEntity player : nearbyPlayers) {
                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        if (!this.bossBar.getPlayers().contains(serverPlayer)) {
                            this.bossBar.addPlayer(serverPlayer);
                        }
                    }
                }

                // Remove players who are too far away
                this.bossBar.getPlayers().removeIf(player -> {
                    double dist = this.squaredDistanceTo(player);
                    // Also check Y distance to prevent showing across dimensions/heights
                    double yDist = Math.abs(player.getY() - this.getY());
                    return dist > BOSS_BAR_RANGE * BOSS_BAR_RANGE || yDist > BOSS_BAR_RANGE || player.getWorld() != this.getWorld();
                });
            } else {
                bossBarUpdateCooldown--;
            }
        } catch (Exception e) {
            // Silently handle boss bar errors to prevent spam
        }
    }

    /**
     * Cleans up the boss bar, removing all players and hiding it
     */
    private void cleanupBossBar() {
        if (this.bossBar != null) {
            this.bossBar.setVisible(false);
            this.bossBar.clearPlayers();
            this.bossBar = null;
        }
    }

    /* ---------- COMBAT ---------- */
    @Override
    public boolean tryAttack(net.minecraft.entity.Entity target) {
        // Only start a new attack if not already in an attack animation
        if (!this.getWorld().isClient && getAttackState() == ATTACK_NONE) {
            // Start the attack animation
            setAttackState(ATTACK_MELEE);
            setAttacking(true);
            attackAnimationTicks = ATTACK_1_DURATION_TICKS;
            attackDamageDelayTicks = ATTACK_1_DAMAGE_DELAY_TICKS;
            pendingAttackTarget = target;

            // Return true to indicate attack was initiated (but damage is delayed)
            return true;
        }

        // Already attacking - don't allow another attack until animation completes
        return false;
    }

    /**
     * Performs the actual damage after the windup animation delay
     */
    private void performDelayedAttack(net.minecraft.entity.Entity target) {
        if (target == null || !target.isAlive()) return;

        // Check if target is still in range
        double distanceSq = this.squaredDistanceTo(target);
        double attackRange = this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) > 0 ? 4.0 : 2.0; // Generous range for boss

        if (distanceSq <= attackRange * attackRange) {
            // Deal the damage using parent's attack logic
            super.tryAttack(target);
        }
    }

    /**
     * Override damage to implement blocking mechanic.
     * When hit, Zulmak has a chance to enter blocking state and become invulnerable.
     */
    @Override
    public boolean damage(DamageSource source, float amount) {
        // If already blocking, ignore all damage
        if (getAttackState() == STATE_BLOCKING) {
            return false;
        }

        // Check if we should start blocking (only on server, not already in another action)
        if (!this.getWorld().isClient && getAttackState() == ATTACK_NONE && blockCooldownTicks <= 0) {
            // Random chance to block incoming damage
            if (this.random.nextFloat() < BLOCK_CHANCE) {
                startBlocking();
                return false; // Block the damage
            }
        }

        // Normal damage handling
        return super.damage(source, amount);
    }

    /**
     * Starts the blocking state - Zulmak becomes invulnerable and plays blocking animation
     */
    private void startBlocking() {
        setAttackState(STATE_BLOCKING);
        blockingTicks = BLOCKING_DURATION_TICKS;
        blockCooldownTicks = BLOCK_COOLDOWN_TICKS;
        // Cancel any pending attacks
        setAttacking(false);
        attackAnimationTicks = 0;
        pendingAttackTarget = null;
    }

    /**
     * Check if Zulmak is currently blocking
     */
    public boolean isBlocking() {
        return getAttackState() == STATE_BLOCKING;
    }

    /* ---------- DATA TRACKER GETTERS/SETTERS ---------- */
    public int getAttackState() {
        return this.dataTracker.get(ATTACK_STATE);
    }

    public void setAttackState(int state) {
        this.dataTracker.set(ATTACK_STATE, state);
    }

    public boolean isAttacking() {
        return this.dataTracker.get(IS_ATTACKING);
    }

    public void setAttacking(boolean attacking) {
        this.dataTracker.set(IS_ATTACKING, attacking);
    }

    /* ---------- NBT PERSISTENCE ---------- */
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("AttackState", getAttackState());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("AttackState")) {
            setAttackState(nbt.getInt("AttackState"));
        }
    }

    /* ---------- SOUNDS ---------- */
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_ZOMBIE_AMBIENT; // Replace with custom sound
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_ZOMBIE_HURT; // Replace with custom sound
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_ZOMBIE_DEATH; // Replace with custom sound
    }

    /* ---------- LIFECYCLE - BOSS BAR CLEANUP ---------- */
    @Override
    public void onDeath(DamageSource damageSource) {
        // Clean up boss bar on death
        if (!this.getWorld().isClient) {
            cleanupBossBar();
        }
        super.onDeath(damageSource);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        // Clean up boss bar BEFORE calling super to ensure it's removed
        if (!this.getWorld().isClient) {
            cleanupBossBar();
        }
        super.remove(reason);
    }

    @Override
    public void onRemoved() {
        // Clean up boss bar BEFORE calling super
        if (!this.getWorld().isClient) {
            cleanupBossBar();
        }
        super.onRemoved();
    }

    /* ---------- GECKOLIB ANIMATION ---------- */

    // Animation definitions - centralized for easy modification
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("animation.zulmak.idle");
    private static final RawAnimation ANIM_WALK = RawAnimation.begin().thenLoop("animation.zulmak.walking");
    private static final RawAnimation ANIM_ATTACK_1 = RawAnimation.begin().thenPlay("animation.zulmak.attack_1");
    private static final RawAnimation ANIM_DEATH = RawAnimation.begin().thenPlayAndHold("animation.zulmak.death");
    private static final RawAnimation ANIM_BLOCKING = RawAnimation.begin().thenPlay("animation.zulmak.blocking");
    private static final RawAnimation ANIM_SUMMON = RawAnimation.begin().thenLoop("animation.zulmak.attack_2"); // Channeling pose

    // Animation speed multiplier (animations were made at 60% speed, so play at 0.6x)
    private static final double ANIMATION_SPEED = 0.6;

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Main movement controller - handles idle and walking (with smooth transitions)
        AnimationController<ZulmakEntity> movementController = new AnimationController<>(this, "movement_controller", 5, this::movementAnimationPredicate);
        movementController.setAnimationSpeed(ANIMATION_SPEED);
        controllers.add(movementController);

        // Attack controller - separate to allow attack animations to play on top of movement
        // Uses 0 transition length for snappy attack response
        AnimationController<ZulmakEntity> attackController = new AnimationController<>(this, "attack_controller", 0, this::attackAnimationPredicate);
        attackController.setAnimationSpeed(ANIMATION_SPEED);
        controllers.add(attackController);
    }

    /**
     * Movement animation predicate - handles idle, walk, and death
     * Death takes priority, then blocking (stops movement), then walk/idle based on movement state
     */
    private PlayState movementAnimationPredicate(AnimationState<ZulmakEntity> state) {
        // Death animation takes absolute priority and holds on last frame
        if (this.isDead()) {
            state.getController().setAnimation(ANIM_DEATH);
            return PlayState.CONTINUE;
        }

        // While blocking or summoning, stay idle (no walking)
        if (getAttackState() == STATE_BLOCKING || getAttackState() == ATTACK_SUMMON) {
            state.getController().setAnimation(ANIM_IDLE);
            return PlayState.CONTINUE;
        }

        // Walking when moving
        if (state.isMoving()) {
            state.getController().setAnimation(ANIM_WALK);
            return PlayState.CONTINUE;
        }

        // Default to idle
        state.getController().setAnimation(ANIM_IDLE);
        return PlayState.CONTINUE;
    }

    /**
     * Attack animation predicate - handles attack and blocking animations
     * Plays on top of movement animations when attacking or blocking
     */
    private PlayState attackAnimationPredicate(AnimationState<ZulmakEntity> state) {
        // Don't play attacks if dead
        if (this.isDead()) {
            lastAttackState = ATTACK_NONE;
            return PlayState.STOP;
        }

        int attackState = getAttackState();

        if (attackState == ATTACK_MELEE) {
            // Check if we just transitioned INTO the attack state
            // This ensures the animation restarts for each new attack
            if (lastAttackState != ATTACK_MELEE) {
                state.getController().forceAnimationReset();
                state.getController().setAnimation(ANIM_ATTACK_1);
            }
            lastAttackState = attackState;
            return PlayState.CONTINUE;
        }

        if (attackState == STATE_BLOCKING) {
            // Check if we just transitioned INTO the blocking state
            if (lastAttackState != STATE_BLOCKING) {
                state.getController().forceAnimationReset();
                state.getController().setAnimation(ANIM_BLOCKING);
            }
            lastAttackState = attackState;
            return PlayState.CONTINUE;
        }

        if (attackState == ATTACK_SUMMON) {
            // Check if we just transitioned INTO the summon state
            if (lastAttackState != ATTACK_SUMMON) {
                state.getController().forceAnimationReset();
                state.getController().setAnimation(ANIM_SUMMON);
            }
            lastAttackState = attackState;
            return PlayState.CONTINUE;
        }

        // No attack, blocking, or summon - update last state
        lastAttackState = attackState;
        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
