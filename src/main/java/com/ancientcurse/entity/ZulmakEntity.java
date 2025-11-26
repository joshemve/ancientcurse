package com.ancientcurse.entity;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.ModBlocks;
import com.ancientcurse.ModEntities;
import com.ancientcurse.ModParticles;
import com.ancientcurse.ModSounds;
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
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import java.util.Optional;
import java.util.UUID;
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
    // UUID of the player being grabbed (empty optional if none)
    private static final TrackedData<Optional<UUID>> GRABBED_PLAYER_UUID =
            DataTracker.registerData(ZulmakEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    // Pickup animation tick counter (synced to client for player positioning)
    private static final TrackedData<Integer> PICKUP_ANIMATION_TICK =
            DataTracker.registerData(ZulmakEntity.class, TrackedDataHandlerRegistry.INTEGER);
    // Track if Zulmak has entered combat (for first hit sound)
    private static final TrackedData<Boolean> HAS_ENTERED_COMBAT =
            DataTracker.registerData(ZulmakEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    // Track if locust swarm is active
    private static final TrackedData<Boolean> IS_LOCUST_SWARM_ACTIVE =
            DataTracker.registerData(ZulmakEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    // Track downed phase state (for animation and rendering)
    private static final TrackedData<Boolean> IS_DOWNED =
            DataTracker.registerData(ZulmakEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    // Track how many downed phases have occurred (0-3)
    private static final TrackedData<Integer> DOWNED_PHASE_COUNT =
            DataTracker.registerData(ZulmakEntity.class, TrackedDataHandlerRegistry.INTEGER);
    // Track downed animation ticks for client sync
    private static final TrackedData<Integer> DOWNED_TICKS =
            DataTracker.registerData(ZulmakEntity.class, TrackedDataHandlerRegistry.INTEGER);

    /* ---------- CONSTANTS ---------- */
    // Attack state constants
    public static final int ATTACK_NONE = 0;
    public static final int ATTACK_MELEE = 1;
    public static final int STATE_BLOCKING = 2;
    public static final int ATTACK_SUMMON = 3;
    public static final int ATTACK_WIND = 4;
    public static final int ATTACK_PICKUP = 5;
    public static final int ATTACK_LOCUST_SWARM = 6;
    public static final int STATE_DOWNED = 7;

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

    // Cursed Earth spread attack - uses attack_2 animation with Khamsin spread
    private static final int CURSED_EARTH_CHANNEL_TICKS = 30; // 1.5 seconds channeling (attack_2 animation)
    private static final int CURSED_EARTH_COOLDOWN_TICKS = 300; // 15 seconds between attacks (longer cooldown)
    private static final float CURSED_EARTH_COMBAT_CHANCE = 0.008f; // 0.8% chance per tick (much rarer)
    private int cursedEarthCooldownTicks = 0;
    private int cursedEarthChannelTicks = 0;
    public static final int ATTACK_CURSED_EARTH = 8; // New attack state for cursed earth

    // Wind attack: uses attack_2 animation, pushes and damages nearby players
    // Note: Cursed earth spreads passively WITHOUT triggering this animation
    private static final int WIND_ATTACK_CHANNEL_TICKS = 25; // 1.25 seconds of channeling
    private static final int WIND_ATTACK_COOLDOWN_TICKS = 400; // 20 seconds between wind attacks (longer cooldown)
    private static final float WIND_ATTACK_CHANCE = 0.03f; // 3% chance per second (reduced - wind attack is rare)
    private static final double WIND_ATTACK_RADIUS = 6.0; // Range of wind attack
    private static final float WIND_ATTACK_DAMAGE = 6.0f; // Damage dealt
    private static final double WIND_KNOCKBACK_STRENGTH = 1.5; // Horizontal knockback
    private static final double WIND_LIFT_STRENGTH = 0.6; // Vertical lift

    // Pickup attack: animation.zulmak.pickup - 2.75s at 0.6x speed = ~92 ticks
    private static final int PICKUP_ANIMATION_TICKS = 92;
    private static final int PICKUP_COOLDOWN_TICKS = 400; // 20 seconds between pickups
    private static final double PICKUP_GRAB_RANGE = 4.5; // Range to grab player (increased for easier triggering)
    // Key timing points in ticks
    private static final int PICKUP_GRAB_TICK = 8;      // Player grabbed
    private static final int PICKUP_HOLD_START = 18;    // Player at face level
    private static final int PICKUP_THROW_START = 40;   // Throw windup starts
    private static final int PICKUP_RELEASE_TICK = 45;  // 2.25s - Player thrown!
    private static final float PICKUP_THROW_POWER = 2.5f; // How hard the player is thrown

    // Locust swarm attack: uses attack_2 animation, summons locust entities
    private static final int LOCUST_SWARM_CHANNEL_TICKS = 40; // 2 seconds of channeling
    private static final int LOCUST_SWARM_COOLDOWN_TICKS = 200; // 10 seconds between locust attacks
    private static final int LOCUST_SPAWN_COUNT = 5; // Number of locusts to spawn
    private static final double LOCUST_SWARM_RADIUS = 10.0; // Range of locust swarm
    private static final float LOCUST_SWARM_DAMAGE = 4.0f; // Damage per player in range

    // Downed phase: triggered at 75%, 50%, 25% health - Zulmak falls, becomes invulnerable, spawns minions
    private static final int DOWNED_DURATION_TICKS = 160; // 8 seconds downed
    private static final double DOWNED_SHOCKWAVE_RADIUS = 12.0; // Radius of the initial shockwave
    private static final double DOWNED_SHOCKWAVE_KNOCKBACK = 2.5; // Knockback strength
    private static final double DOWNED_SHOCKWAVE_LIFT = 0.8; // Vertical lift strength
    private static final int DOWNED_PHARAOH_SPAWN_COUNT = 2; // Number of Withered Pharaohs to spawn
    private static final float[] DOWNED_HEALTH_THRESHOLDS = {0.75f, 0.50f, 0.25f}; // Health % to trigger downed

    // Sound timing constants
    private static final int AWAKENED_SOUND_MIN_INTERVAL = 400; // 20 seconds minimum between awakened sounds
    private static final int AWAKENED_SOUND_MAX_INTERVAL = 1200; // 60 seconds maximum
    private static final float AWAKENED_SOUND_CHANCE = 0.002f; // Chance per tick when idle
    private static final double VORTEX_SOUND_RANGE = 32.0; // Range for vortex wind loop

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

    // Wind attack state
    private int windAttackChannelTicks = 0;
    private int windAttackCooldownTicks = 0;

    // Pickup attack state
    private int pickupAnimationTicks = 0;
    private int pickupCooldownTicks = 0;
    private PlayerEntity grabbedPlayerCache = null; // Server-side cache of grabbed player
    private boolean playerHasBeenThrown = false; // Track if throw has happened this animation

    // Locust swarm state
    private int locustSwarmChannelTicks = 0;
    private int locustSwarmCooldownTicks = 0;

    // Downed phase state
    private boolean hasTriggeredShockwave = false; // Track if shockwave has been triggered this downed phase
    private boolean hasSpawnedPharaohs = false; // Track if pharaohs have been spawned this downed phase
    private float downedParticleAngle = 0; // For swirling particles during downed phase

    // Track attack state transitions for animation reset
    private int lastAttackState = ATTACK_NONE;

    // Sound management
    private int awakenedSoundCooldown = 0; // Cooldown for awakened ambient sound
    private int vortexSoundTicks = 0; // Ticks for vortex wind loop timing
    private boolean hasPlayedFirstHitSound = false; // Track if first hit sound has played

    // Death animation handling
    private boolean playingDeathAnimation = false;
    private int deathAnimationTicks = 0;
    // Death animation: 0.5833s at 0.6x speed = ~20 ticks, add buffer for visual
    private static final int DEATH_ANIMATION_DURATION = 30;
    // 40% health death animation (epic fake death with resurrection)
    private boolean hasPlayed40PercentDeathAnimation = false;

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
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 500.0) // Boss-tier health
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28) // Moderately fast
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 7.0) // Moderate attacks (spawns minions to compensate)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0) // Detection range
                .add(EntityAttributes.GENERIC_ARMOR, 10.0) // Strong armor
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8); // High knockback resist
    }

    /* ---------- INITIALIZATION ---------- */
    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(ATTACK_STATE, ATTACK_NONE);
        this.dataTracker.startTracking(IS_ATTACKING, false);
        this.dataTracker.startTracking(GRABBED_PLAYER_UUID, Optional.empty());
        this.dataTracker.startTracking(PICKUP_ANIMATION_TICK, 0);
        this.dataTracker.startTracking(HAS_ENTERED_COMBAT, false);
        this.dataTracker.startTracking(IS_LOCUST_SWARM_ACTIVE, false);
        this.dataTracker.startTracking(IS_DOWNED, false);
        this.dataTracker.startTracking(DOWNED_PHASE_COUNT, 0);
        this.dataTracker.startTracking(DOWNED_TICKS, 0);
    }

    @Override
    protected void initGoals() {
        // Combat goals
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new ZulmakPickupGoal(this)); // Pickup attack - grabs and throws players
        // TODO: Add ZulmakSummonGoal when attack_2 animation is implemented
        // this.goalSelector.add(3, new ZulmakSummonGoal(this)); // Summon takes priority when conditions met
        this.goalSelector.add(3, new ZulmakMeleeAttackGoal(this, 1.2D, false));
        this.goalSelector.add(4, new WanderAroundFarGoal(this, 0.8D));
        this.goalSelector.add(5, new ZulmakLookAtTargetGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(6, new LookAroundGoal(this));

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

            // Play summon sounds (with chance for scream layer)
            zulmak.playSummonSounds();
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

            // Play cursed earth attack sound
            zulmak.playCursedEarthAttackSound();
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

    /**
     * Pickup goal - Zulmak grabs a nearby player, holds them up, looks at them, then throws them.
     * Uses animation.zulmak.pickup with player_locator bone for player positioning.
     */
    private static class ZulmakPickupGoal extends Goal {
        private final ZulmakEntity zulmak;
        private PlayerEntity targetPlayer;

        public ZulmakPickupGoal(ZulmakEntity zulmak) {
            this.zulmak = zulmak;
            this.setControls(java.util.EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            // Only pickup if:
            // 1. Not in another attack state
            // 2. Pickup cooldown expired
            // 3. Has a player target in range
            // 4. Health below 50% (desperate attack)
            if (zulmak.getAttackState() != ATTACK_NONE) {
                if (zulmak.age % 100 == 0) AncientCurse.LOGGER.debug("Pickup blocked: already in attack state {}", zulmak.getAttackState());
                return false;
            }
            if (zulmak.pickupCooldownTicks > 0) {
                if (zulmak.age % 100 == 0) AncientCurse.LOGGER.debug("Pickup blocked: cooldown {} ticks remaining", zulmak.pickupCooldownTicks);
                return false;
            }
            float healthPercent = zulmak.getHealth() / zulmak.getMaxHealth();
            if (healthPercent > 0.5f) {
                if (zulmak.age % 100 == 0) AncientCurse.LOGGER.debug("Pickup blocked: health too high ({}% > 50%)", (int)(healthPercent * 100));
                return false;
            }
            if (zulmak.isDowned()) return false; // Don't pickup during downed phase
            if (zulmak.isPlayingDeathAnimation()) return false; // Don't pickup during death

            // Find a nearby player in grab range
            LivingEntity target = zulmak.getTarget();
            if (!(target instanceof PlayerEntity player)) {
                if (zulmak.age % 100 == 0) AncientCurse.LOGGER.debug("Pickup blocked: target is not a player (target={})", target);
                return false;
            }
            if (!player.isAlive() || player.isSpectator()) return false;

            double distSq = zulmak.squaredDistanceTo(player);
            double dist = Math.sqrt(distSq);
            if (distSq > PICKUP_GRAB_RANGE * PICKUP_GRAB_RANGE) {
                if (zulmak.age % 100 == 0) AncientCurse.LOGGER.debug("Pickup blocked: player too far ({} > {} blocks)", String.format("%.1f", dist), PICKUP_GRAB_RANGE);
                return false;
            }

            // Don't grab players who are flying or in creative
            if (player.getAbilities().flying || player.isCreative()) return false;

            targetPlayer = player;

            // Debug log
            AncientCurse.LOGGER.info("=== Zulmak STARTING PICKUP ATTACK on player {} at distance {} blocks (health: {}%) ===",
                player.getName().getString(), String.format("%.1f", dist), (int)(healthPercent * 100));

            return true;
        }

        @Override
        public boolean shouldContinue() {
            // Continue until animation is complete
            return zulmak.getAttackState() == ATTACK_PICKUP && zulmak.pickupAnimationTicks < PICKUP_ANIMATION_TICKS;
        }

        @Override
        public void start() {
            if (targetPlayer == null) return;

            zulmak.setAttackState(ATTACK_PICKUP);
            zulmak.pickupAnimationTicks = 0;
            zulmak.playerHasBeenThrown = false;
            zulmak.grabbedPlayerCache = targetPlayer;
            zulmak.setGrabbedPlayerUUID(targetPlayer.getUuid());
            zulmak.getNavigation().stop();

            // Play grab sound
            zulmak.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.7f);
        }

        @Override
        public void tick() {
            zulmak.pickupAnimationTicks++;

            // Stop movement during pickup
            zulmak.getNavigation().stop();
            zulmak.setVelocity(0, zulmak.getVelocity().y, 0);

            // Look at grabbed player during hold phase
            PlayerEntity grabbed = zulmak.grabbedPlayerCache;
            if (grabbed != null && zulmak.pickupAnimationTicks >= PICKUP_HOLD_START &&
                zulmak.pickupAnimationTicks < PICKUP_THROW_START) {
                zulmak.getLookControl().lookAt(grabbed, 30.0F, 30.0F);
            }
        }

        @Override
        public void stop() {
            zulmak.setAttackState(ATTACK_NONE);
            zulmak.pickupCooldownTicks = PICKUP_COOLDOWN_TICKS;
            zulmak.setGrabbedPlayerUUID(null);
            zulmak.grabbedPlayerCache = null;
            zulmak.playerHasBeenThrown = false;
        }
    }

    /**
     * Custom look at target goal that respects downed state.
     * Prevents Zulmak from rotating/looking around while downed.
     */
    private static class ZulmakLookAtTargetGoal extends LookAtEntityGoal {
        private final ZulmakEntity zulmak;

        public ZulmakLookAtTargetGoal(ZulmakEntity entity, Class<? extends LivingEntity> targetType, float range) {
            super(entity, targetType, range);
            this.zulmak = entity;
        }

        @Override
        public boolean canStart() {
            // Don't look around while downed (channeling regeneration)
            if (zulmak.isDowned()) {
                return false;
            }
            // Don't look around during death animation
            if (zulmak.isPlayingDeathAnimation()) {
                return false;
            }
            return super.canStart();
        }

        @Override
        public boolean shouldContinue() {
            // Stop looking if we enter downed state
            if (zulmak.isDowned()) {
                return false;
            }
            // Stop looking during death animation
            if (zulmak.isPlayingDeathAnimation()) {
                return false;
            }
            return super.shouldContinue();
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
            if (windAttackCooldownTicks > 0) {
                windAttackCooldownTicks--;
            }
            if (pickupCooldownTicks > 0) {
                pickupCooldownTicks--;
            }
            if (locustSwarmCooldownTicks > 0) {
                locustSwarmCooldownTicks--;
            }
            if (awakenedSoundCooldown > 0) {
                awakenedSoundCooldown--;
            }
            if (cursedEarthCooldownTicks > 0) {
                cursedEarthCooldownTicks--;
            }

            // Try cursed earth spread attack around players during combat
            tryCursedEarthSpreadAttack();

            // Handle cursed earth channeling
            if (getAttackState() == ATTACK_CURSED_EARTH && cursedEarthChannelTicks > 0) {
                tickCursedEarthAttack();
            }

            // Handle locust swarm channeling
            if (locustSwarmChannelTicks > 0) {
                locustSwarmChannelTicks--;
                spawnLocustSwarmParticles();
                
                // Locust swarm complete - deal damage and spawn effects
                if (locustSwarmChannelTicks <= 0) {
                    performLocustSwarmAttack();
                    setAttackState(ATTACK_NONE);
                    this.dataTracker.set(IS_LOCUST_SWARM_ACTIVE, false);
                    locustSwarmCooldownTicks = LOCUST_SWARM_COOLDOWN_TICKS;
                }
            }

            // Try to start locust swarm attack
            tryStartLocustSwarmAttack();

            // Handle downed phase
            if (isDowned()) {
                tickDownedPhase();
            }

            // Check if we should trigger a downed phase
            checkDownedPhaseThreshold();

            // Handle awakened ambient sound (random idle sound)
            handleAwakenedSound();

            // Handle blocking state
            if (blockingTicks > 0) {
                blockingTicks--;
                // Blocking finished - return to normal
                if (blockingTicks <= 0) {
                    setAttackState(ATTACK_NONE);
                }
            }

            // Handle wind attack channeling
            if (windAttackChannelTicks > 0) {
                windAttackChannelTicks--;
                // Spawn swirling wind particles during channel
                spawnWindChannelParticles();

                // Wind attack complete - release the blast
                if (windAttackChannelTicks <= 0) {
                    performWindAttack();
                    setAttackState(ATTACK_NONE);
                    windAttackCooldownTicks = WIND_ATTACK_COOLDOWN_TICKS;
                }
            }

            // Check if we should start a wind attack (random chance when players nearby)
            tryStartWindAttack();

            // Handle pickup attack - update grabbed player position
            if (getAttackState() == ATTACK_PICKUP) {
                tickPickupAttack();
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

            // Check for 40% health threshold - trigger epic death animation
            check40PercentHealthThreshold();
        }

        // Client-side particle effects - spinning around the spinning blocks
        if (this.getWorld().isClient) {
            spawnOrbitingParticles();
            // Spawn swirling magic particles during downed phase
            if (isDowned()) {
                spawnDownedPhaseParticlesClient();
            }
        }

        // Handle vortex wind loop sound (plays when players are nearby)
        handleVortexWindSound();

        // Both client and server: Update grabbed player position
        if (getAttackState() == ATTACK_PICKUP) {
            updateGrabbedPlayerPosition();
        }
    }

    /**
     * Spawns particles that orbit around Zulmak's spinning blocks.
     * Particles match the rotation direction of tier_1 and tier_2 bones.
     * Uses cyan/teal shield particles when blocking, purple particles otherwise.
     */
    private void spawnOrbitingParticles() {
        // Update rotation angles (matches animation speed)
        particleAngleTier1 += PARTICLE_ROTATION_SPEED_TIER1;
        particleAngleTier2 += PARTICLE_ROTATION_SPEED_TIER2;

        // Keep angles in range
        if (particleAngleTier1 < 0) particleAngleTier1 += 360;
        if (particleAngleTier2 >= 360) particleAngleTier2 -= 360;

        // Choose particle type based on blocking state
        var particleType = isBlocking() ? ModParticles.ZULMAK_SHIELD_PARTICLE : ModParticles.ZULMAK_PARTICLE;

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

                this.getWorld().addParticle(particleType, x1, y1, z1, velX1, 0.01, velZ1);
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

                this.getWorld().addParticle(particleType, x2, y2, z2, velX2, 0.01, velZ2);
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

    /**
     * Checks if Zulmak has reached 40% health and triggers epic death animation
     * This is called every tick on server side
     */
    private void check40PercentHealthThreshold() {
        // Don't trigger if already played, already downed, playing death animation, or actually dead
        if (hasPlayed40PercentDeathAnimation || isDowned() || playingDeathAnimation || this.isDead()) {
            return;
        }

        float healthPercent = this.getHealth() / this.getMaxHealth();
        if (healthPercent <= 0.40f) {
            trigger40PercentDeathAnimation();
        }
    }

    /**
     * Triggers the epic 40% health death animation with godlike effects
     * After animation completes, Zulmak will resurrect with 10% health restored
     */
    private void trigger40PercentDeathAnimation() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        hasPlayed40PercentDeathAnimation = true;
        playingDeathAnimation = true;
        deathAnimationTicks = 0;

        // Cancel all actions and release grabbed player
        setAttackState(ATTACK_NONE);
        if (getGrabbedPlayer() != null) {
            setGrabbedPlayerUUID(null);
            grabbedPlayerCache = null;
        }

        // Stop all movement
        this.getNavigation().stop();
        this.setVelocity(0, this.getVelocity().y, 0);

        // Epic dramatic sounds - layered for godlike effect
        this.playSound(ModSounds.ZULMAK_DEATH, 2.5f, 0.5f);
        this.playSound(SoundEvents.ENTITY_WITHER_DEATH, 1.5f, 0.3f);
        this.playSound(SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 1.2f, 0.4f);

        // Play dramatic sound for all nearby players
        List<ServerPlayerEntity> nearbyPlayers = serverWorld.getPlayers(player ->
                player.squaredDistanceTo(this) <= 64.0 * 64.0);

        for (ServerPlayerEntity player : nearbyPlayers) {
            player.playSound(SoundEvents.BLOCK_BELL_USE, SoundCategory.HOSTILE, 1.5f, 0.5f);
            player.getWorld().playSound(null, player.getBlockPos(),
                    SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.HOSTILE, 0.8f, 0.8f);
        }

        // Massive particle burst - 50 soul fire and witch particles
        for (int i = 0; i < 50; i++) {
            double offsetX = (this.random.nextDouble() - 0.5) * 4.0;
            double offsetY = this.random.nextDouble() * 3.0;
            double offsetZ = (this.random.nextDouble() - 0.5) * 4.0;

            serverWorld.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    this.getX() + offsetX,
                    this.getY() + offsetY,
                    this.getZ() + offsetZ,
                    1, 0, 0, 0, 0.1);

            serverWorld.spawnParticles(ParticleTypes.WITCH,
                    this.getX() + offsetX,
                    this.getY() + offsetY,
                    this.getZ() + offsetZ,
                    1, 0, 0, 0, 0.05);
        }

        // Shockwave ring of enchant particles
        for (int angle = 0; angle < 360; angle += 10) {
            double radian = Math.toRadians(angle);
            double radius = 3.0;
            double x = this.getX() + Math.cos(radian) * radius;
            double z = this.getZ() + Math.sin(radian) * radius;
            serverWorld.spawnParticles(ParticleTypes.ENCHANT,
                    x, this.getY() + 0.5, z,
                    1, 0, 0, 0, 0.1);
        }

        // Flash effect
        serverWorld.spawnParticles(ParticleTypes.FLASH,
                this.getX(), this.getY() + 1.5, this.getZ(),
                1, 0, 0, 0, 0);
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
            boolean didDamage = super.tryAttack(target);
            
            // Play player zap sound if we hit a player
            if (didDamage && target instanceof PlayerEntity player) {
                playPlayerZapSound(player);
            }
        }
    }

    /**
     * Override damage to implement blocking mechanic and first hit sound.
     * When hit, Zulmak has a chance to enter blocking state and become invulnerable.
     */
    @Override
    public boolean damage(DamageSource source, float amount) {
        // If downed (regenerating phase), completely invulnerable
        if (isDowned()) {
            return false;
        }

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

        // Handle first hit sound (entering combat mode)
        if (!this.getWorld().isClient && !hasPlayedFirstHitSound && !this.dataTracker.get(HAS_ENTERED_COMBAT)) {
            hasPlayedFirstHitSound = true;
            this.dataTracker.set(HAS_ENTERED_COMBAT, true);
            this.playSound(ModSounds.ZULMAK_FIRST_HIT, 1.5f, 1.0f);
        }

        // Normal damage handling - let parent handle it
        boolean damaged = super.damage(source, amount);
        
        // Manually play hurt sound to ensure it's audible (parent may use low volume)
        if (damaged && !this.getWorld().isClient) {
            // Play hurt sound with random pitch variation
            this.playSound(ModSounds.ZULMAK_HURT, 1.2f, 0.9f + this.random.nextFloat() * 0.2f);
        }
        
        return damaged;
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
        
        // Play blocking sound
        this.playSound(ModSounds.ZULMAK_BLOCKING, 1.2f, 1.0f);
    }

    /**
     * Check if Zulmak is currently blocking
     */
    public boolean isBlocking() {
        return getAttackState() == STATE_BLOCKING;
    }

    /* ---------- WIND ATTACK ---------- */

    /**
     * Checks if conditions are met to start a wind attack and randomly triggers it
     */
    private void tryStartWindAttack() {
        // Only try if not in another action and cooldown expired
        if (getAttackState() != ATTACK_NONE || windAttackCooldownTicks > 0) {
            return;
        }

        // Check if there are players nearby
        List<PlayerEntity> nearbyPlayers = this.getWorld().getEntitiesByClass(
                PlayerEntity.class,
                new Box(this.getBlockPos()).expand(WIND_ATTACK_RADIUS),
                player -> player.isAlive() && !player.isSpectator() && !player.isCreative()
        );

        if (nearbyPlayers.isEmpty()) {
            return;
        }

        // Random chance to trigger wind attack
        if (this.random.nextFloat() < WIND_ATTACK_CHANCE / 20f) { // Divide by 20 since this runs every tick
            startWindAttack();
        }
    }

    /**
     * Starts the wind attack - Zulmak channels energy before releasing a wind blast
     */
    private void startWindAttack() {
        setAttackState(ATTACK_WIND);
        windAttackChannelTicks = WIND_ATTACK_CHANNEL_TICKS;

        // Cancel any pending melee attacks
        setAttacking(false);
        attackAnimationTicks = 0;
        pendingAttackTarget = null;

        // Play wind-up sound
        this.playSound(SoundEvents.ENTITY_WITHER_SHOOT, 0.8f, 0.5f);
    }

    /**
     * Spawns swirling wind particles during the channel phase
     */
    private void spawnWindChannelParticles() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) return;

        // Calculate swirl intensity based on channel progress
        float progress = 1.0f - ((float) windAttackChannelTicks / WIND_ATTACK_CHANNEL_TICKS);
        int particleCount = (int) (5 + progress * 15); // More particles as attack charges

        for (int i = 0; i < particleCount; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2;
            double radius = WIND_ATTACK_RADIUS * (0.3 + this.random.nextDouble() * 0.7);
            double height = this.random.nextDouble() * 2.5;

            double x = this.getX() + Math.cos(angle) * radius;
            double z = this.getZ() + Math.sin(angle) * radius;
            double y = this.getY() + height;

            // Velocity swirling inward toward Zulmak
            double velX = (this.getX() - x) * 0.1;
            double velZ = (this.getZ() - z) * 0.1;
            double velY = 0.05 + this.random.nextDouble() * 0.1;

            serverWorld.spawnParticles(ParticleTypes.CLOUD, x, y, z, 1, 0.1, 0.1, 0.1, 0.02);

            // Add some soul particles for mystical effect
            if (this.random.nextFloat() < 0.3f) {
                serverWorld.spawnParticles(ParticleTypes.SOUL, x, y, z, 1, 0.1, 0.1, 0.1, 0.01);
            }
        }
    }

    /**
     * Performs the wind attack - pushes and damages all nearby players
     */
    private void performWindAttack() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) return;

        // Get all players in range
        List<PlayerEntity> nearbyPlayers = this.getWorld().getEntitiesByClass(
                PlayerEntity.class,
                new Box(this.getBlockPos()).expand(WIND_ATTACK_RADIUS),
                player -> player.isAlive() && !player.isSpectator()
        );

        // Play explosion sound
        this.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);
        this.playSound(SoundEvents.ENTITY_PHANTOM_FLAP, 2.0f, 0.5f);

        for (PlayerEntity player : nearbyPlayers) {
            // Calculate direction from Zulmak to player
            double dx = player.getX() - this.getX();
            double dz = player.getZ() - this.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);

            if (distance < 0.1) distance = 0.1; // Prevent division by zero

            // Normalize direction
            double dirX = dx / distance;
            double dirZ = dz / distance;

            // Calculate knockback strength (stronger when closer)
            double distanceFactor = 1.0 - (distance / WIND_ATTACK_RADIUS);
            distanceFactor = Math.max(0.3, distanceFactor); // Minimum knockback

            double knockbackX = dirX * WIND_KNOCKBACK_STRENGTH * distanceFactor;
            double knockbackZ = dirZ * WIND_KNOCKBACK_STRENGTH * distanceFactor;
            double lift = WIND_LIFT_STRENGTH * distanceFactor;

            // Add swirl effect (perpendicular to knockback direction)
            double swirlStrength = 0.3 * distanceFactor;
            knockbackX += -dirZ * swirlStrength;
            knockbackZ += dirX * swirlStrength;

            // Apply velocity to player
            player.setVelocity(
                    player.getVelocity().x + knockbackX,
                    player.getVelocity().y + lift,
                    player.getVelocity().z + knockbackZ
            );
            player.velocityModified = true;

            // Deal damage (bypass armor slightly with magic damage)
            if (!player.isCreative()) {
                player.damage(this.getDamageSources().magic(), WIND_ATTACK_DAMAGE * (float) distanceFactor);
            }
        }

        // Spawn explosion of wind particles
        for (int i = 0; i < 60; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2;
            double radius = this.random.nextDouble() * WIND_ATTACK_RADIUS;
            double height = this.random.nextDouble() * 3.0;

            double x = this.getX() + Math.cos(angle) * radius;
            double z = this.getZ() + Math.sin(angle) * radius;
            double y = this.getY() + height;

            // Velocity outward from Zulmak
            double velX = Math.cos(angle) * 0.3;
            double velZ = Math.sin(angle) * 0.3;
            double velY = 0.1 + this.random.nextDouble() * 0.2;

            serverWorld.spawnParticles(ParticleTypes.CLOUD, x, y, z, 1, 0.2, 0.2, 0.2, 0.1);
            serverWorld.spawnParticles(ParticleTypes.SWEEP_ATTACK, x, y, z, 1, 0.1, 0.1, 0.1, 0.0);
        }
    }

    /* ---------- LOCUST SWARM ATTACK ---------- */

    /**
     * Checks if conditions are met to start a locust swarm attack
     */
    private void tryStartLocustSwarmAttack() {
        // Only try if not in another action and cooldown expired
        if (getAttackState() != ATTACK_NONE || locustSwarmCooldownTicks > 0) {
            return;
        }

        // Don't use during downed phase
        if (isDowned()) {
            return;
        }

        // Check if there are players nearby (within range)
        List<PlayerEntity> nearbyPlayers = this.getWorld().getEntitiesByClass(
                PlayerEntity.class,
                new Box(this.getBlockPos()).expand(LOCUST_SWARM_RADIUS + 5),
                player -> player.isAlive() && !player.isSpectator() && !player.isCreative()
        );

        if (nearbyPlayers.isEmpty()) {
            return;
        }

        // Higher chance to trigger - scales with missing health
        // Base 1.5% per tick, up to 3% when low health
        float healthPercent = this.getHealth() / this.getMaxHealth();
        float triggerChance = 0.015f + (1.0f - healthPercent) * 0.015f;

        if (this.random.nextFloat() < triggerChance) {
            startLocustSwarmAttack();
        }
    }

    /**
     * Starts the locust swarm attack - Zulmak channels and summons locusts
     */
    private void startLocustSwarmAttack() {
        setAttackState(ATTACK_LOCUST_SWARM);
        locustSwarmChannelTicks = LOCUST_SWARM_CHANNEL_TICKS;
        this.dataTracker.set(IS_LOCUST_SWARM_ACTIVE, true);

        // Cancel any pending melee attacks
        setAttacking(false);
        attackAnimationTicks = 0;
        pendingAttackTarget = null;

        // Stop movement
        this.getNavigation().stop();

        // Play locust swarm sound
        this.playSound(ModSounds.ZULMAK_LOCUST_SWARM, 1.5f, 1.0f);
    }

    /**
     * Spawns locust swarm particles during the channel phase (optimized - only every 3 ticks)
     */
    private void spawnLocustSwarmParticles() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) return;

        // Only spawn particles every 3 ticks for optimization
        if (locustSwarmChannelTicks % 3 != 0) return;

        // Calculate swarm intensity based on channel progress
        float progress = 1.0f - ((float) locustSwarmChannelTicks / LOCUST_SWARM_CHANNEL_TICKS);
        int particleCount = (int) (5 + progress * 15); // Reduced particle count

        for (int i = 0; i < particleCount; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2;
            double radius = 3.0 + this.random.nextDouble() * 4.0; // Closer to Zulmak
            double height = this.random.nextDouble() * 3.0;

            double x = this.getX() + Math.cos(angle) * radius;
            double z = this.getZ() + Math.sin(angle) * radius;
            double y = this.getY() + height;

            // Use campfire smoke for locust-like effect
            serverWorld.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 1, 0.1, 0.1, 0.1, 0.02);
        }
    }

    /**
     * Performs the locust swarm attack - spawns actual Locus entities that chase players
     */
    private void performLocustSwarmAttack() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) return;

        // Get nearby players for targeting
        List<PlayerEntity> nearbyPlayers = this.getWorld().getEntitiesByClass(
                PlayerEntity.class,
                new Box(this.getBlockPos()).expand(LOCUST_SWARM_RADIUS + 5),
                player -> player.isAlive() && !player.isSpectator() && !player.isCreative()
        );

        // Spawn Locus entities around Zulmak
        for (int i = 0; i < LOCUST_SPAWN_COUNT; i++) {
            // Calculate spawn position in a circle around Zulmak
            double angle = (Math.PI * 2 * i) / LOCUST_SPAWN_COUNT + this.random.nextDouble() * 0.5;
            double spawnRadius = 2.0 + this.random.nextDouble() * 2.0;
            double spawnX = this.getX() + Math.cos(angle) * spawnRadius;
            double spawnZ = this.getZ() + Math.sin(angle) * spawnRadius;
            double spawnY = this.getY() + 0.5 + this.random.nextDouble();

            // Create and spawn the Locus entity
            LocusEntity locus = ModEntities.LOCUS.create(serverWorld);
            if (locus != null) {
                locus.refreshPositionAndAngles(spawnX, spawnY, spawnZ, this.random.nextFloat() * 360.0f, 0.0f);

                // Set target to nearest player if available
                if (!nearbyPlayers.isEmpty()) {
                    PlayerEntity target = nearbyPlayers.get(this.random.nextInt(nearbyPlayers.size()));
                    locus.setTarget(target);
                }

                // Spawn with particles
                serverWorld.spawnEntity(locus);

                // Spawn effect at spawn location
                serverWorld.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, spawnX, spawnY, spawnZ,
                        5, 0.3, 0.3, 0.3, 0.05);
                serverWorld.spawnParticles(ParticleTypes.SMOKE, spawnX, spawnY, spawnZ,
                        3, 0.2, 0.2, 0.2, 0.02);
            }
        }

        // Spawn explosion of locust-like particles around Zulmak
        for (int i = 0; i < 40; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2;
            double radius = this.random.nextDouble() * 4.0;
            double height = this.random.nextDouble() * 3.0;

            double x = this.getX() + Math.cos(angle) * radius;
            double z = this.getZ() + Math.sin(angle) * radius;
            double y = this.getY() + height;

            serverWorld.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 1, 0.2, 0.2, 0.2, 0.1);
        }

        // Play buzzing/swarm sound effect
        this.playSound(SoundEvents.ENTITY_BEE_LOOP_AGGRESSIVE, 2.0f, 0.5f);
    }

    /**
     * Spreads Cursed Earth blocks in a radius around Zulmak during attacks
     */
    private void spawnCursedEarthFromAttack(ServerWorld serverWorld) {
        // Play cursed earth sound
        playCursedEarthAttackSound();

        int radius = 3 + this.random.nextInt(2); // 3-4 block radius
        BlockPos centerPos = this.getBlockPos();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                // Circular spread pattern
                if (x * x + z * z > radius * radius) continue;

                // Random chance to place (creates patchy spread)
                if (this.random.nextFloat() > 0.5f) continue;

                BlockPos targetPos = centerPos.add(x, 0, z);

                // Find the ground level
                for (int y = 2; y >= -2; y--) {
                    BlockPos checkPos = targetPos.add(0, y, 0);
                    BlockPos abovePos = checkPos.up();

                    // Check if this is a valid spot (solid block with air above)
                    if (serverWorld.getBlockState(checkPos).isSolidBlock(serverWorld, checkPos)
                            && serverWorld.getBlockState(abovePos).isAir()) {

                        // Replace the top block with Cursed Earth or Cursed Sand
                        net.minecraft.block.BlockState currentState = serverWorld.getBlockState(checkPos);
                        if (currentState.isOf(net.minecraft.block.Blocks.SAND) || 
                            currentState.isOf(net.minecraft.block.Blocks.RED_SAND)) {
                            // Use cursed sand for sand blocks
                            serverWorld.setBlockState(checkPos, ModBlocks.CURSED_SAND.getDefaultState());
                        } else {
                            // Use cursed earth for other blocks
                            serverWorld.setBlockState(checkPos, ModBlocks.CURSED_EARTH.getDefaultState());
                        }

                        // Spawn dark particles at conversion
                        if (this.random.nextFloat() < 0.4f) {
                            serverWorld.spawnParticles(ParticleTypes.SOUL,
                                    checkPos.getX() + 0.5, checkPos.getY() + 1.0, checkPos.getZ() + 0.5,
                                    3, 0.3, 0.2, 0.3, 0.02);
                        }
                        break;
                    }
                }
            }
        }

        // Spawn some Khamsin Spread entities
        int khamsinCount = 1 + this.random.nextInt(2); // 1-2 entities
        for (int i = 0; i < khamsinCount; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2;
            double dist = 1.5 + this.random.nextDouble() * 2.5;
            double khamsinX = this.getX() + Math.cos(angle) * dist;
            double khamsinZ = this.getZ() + Math.sin(angle) * dist;
            double khamsinY = this.getY() + 0.5 + this.random.nextDouble() * 1.5;

            KhamsinSpreadSmallEntity khamsin = ModEntities.KHAMSIN_SPREAD_SMALL.create(serverWorld);
            if (khamsin != null) {
                khamsin.refreshPositionAndAngles(khamsinX, khamsinY, khamsinZ,
                        this.random.nextFloat() * 360, 0);
                serverWorld.spawnEntity(khamsin);
            }
        }
    }

    /* ---------- CURSED EARTH SPREAD ATTACK ---------- */

    /**
     * Attempts to start the cursed earth attack - channels with attack_2 animation
     * then spreads cursed earth around nearby players.
     */
    private void tryCursedEarthSpreadAttack() {
        if (this.getWorld().isClient) return;
        if (cursedEarthCooldownTicks > 0) return;
        if (isDowned()) return;

        // Don't start if already in another attack
        if (getAttackState() != ATTACK_NONE) return;

        // Only attack when in combat (has a target)
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) return;

        // Check if there are players nearby
        List<PlayerEntity> nearbyPlayers = this.getWorld().getEntitiesByClass(
                PlayerEntity.class,
                new Box(this.getBlockPos()).expand(16),
                player -> player.isAlive() && !player.isSpectator() && !player.isCreative()
        );
        if (nearbyPlayers.isEmpty()) return;

        // Random chance each tick to start the attack
        if (this.random.nextFloat() > CURSED_EARTH_COMBAT_CHANCE) return;

        // Start the cursed earth attack!
        startCursedEarthAttack();
    }

    /**
     * Starts the cursed earth attack - plays attack_2 animation while channeling
     */
    private void startCursedEarthAttack() {
        setAttackState(ATTACK_CURSED_EARTH);
        cursedEarthChannelTicks = CURSED_EARTH_CHANNEL_TICKS;

        // Stop movement while channeling
        this.getNavigation().stop();

        // Play cursed earth attack sound at start
        playCursedEarthAttackSound();
    }

    /**
     * Handles the cursed earth channeling tick - spawns particles during channel,
     * then releases the attack when channel completes.
     */
    private void tickCursedEarthAttack() {
        if (cursedEarthChannelTicks <= 0) return;

        // Stop movement during channel
        this.getNavigation().stop();
        this.setVelocity(0, this.getVelocity().y, 0);

        // Look at target while channeling
        LivingEntity target = this.getTarget();
        if (target != null) {
            this.getLookControl().lookAt(target, 30.0F, 30.0F);
        }

        cursedEarthChannelTicks--;

        // Spawn dark channeling particles
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            // Dark energy rising from ground
            double angle = (CURSED_EARTH_CHANNEL_TICKS - cursedEarthChannelTicks) * 0.4;
            for (int i = 0; i < 3; i++) {
                double offsetAngle = angle + (Math.PI * 2 * i / 3);
                double x = this.getX() + Math.cos(offsetAngle) * 2.0;
                double z = this.getZ() + Math.sin(offsetAngle) * 2.0;
                serverWorld.spawnParticles(ParticleTypes.SOUL,
                        x, this.getY() + 0.2, z,
                        2, 0.2, 0.3, 0.2, 0.02);
            }

            // Smoke rising
            if (cursedEarthChannelTicks % 5 == 0) {
                serverWorld.spawnParticles(ParticleTypes.SMOKE,
                        this.getX(), this.getY(), this.getZ(),
                        8, 2.0, 0.1, 2.0, 0.01);
            }
        }

        // Channel complete - release the attack!
        if (cursedEarthChannelTicks <= 0) {
            performCursedEarthAttack();
            setAttackState(ATTACK_NONE);
            cursedEarthCooldownTicks = CURSED_EARTH_COOLDOWN_TICKS;
        }
    }

    /**
     * Performs the actual cursed earth spread around all nearby players
     */
    private void performCursedEarthAttack() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) return;

        // Find all nearby players
        List<PlayerEntity> nearbyPlayers = serverWorld.getEntitiesByClass(
                PlayerEntity.class,
                new Box(this.getBlockPos()).expand(16),
                player -> player.isAlive() && !player.isSpectator() && !player.isCreative()
        );

        // Play the cursed earth sound ONCE at the start (not per player)
        playCursedEarthAttackSound();

        // Spread cursed earth around each player
        for (PlayerEntity player : nearbyPlayers) {
            spawnCursedEarthAroundPlayer(serverWorld, player);
        }
    }

    /**
     * Spreads cursed earth in a radius around a target player position.
     * Also spawns Khamsin Spread entities for visual effect.
     */
    private void spawnCursedEarthAroundPlayer(ServerWorld serverWorld, PlayerEntity player) {
        BlockPos centerPos = player.getBlockPos();
        int radius = 2 + this.random.nextInt(3); // 2-4 block radius around player

        int blocksConverted = 0;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                // Circular spread pattern
                if (x * x + z * z > radius * radius) continue;

                // Random chance to place (creates patchy spread)
                if (this.random.nextFloat() > 0.6f) continue;

                BlockPos targetPos = centerPos.add(x, 0, z);

                // Find the ground level
                for (int y = 2; y >= -2; y--) {
                    BlockPos checkPos = targetPos.add(0, y, 0);
                    BlockPos abovePos = checkPos.up();

                    // Check if this is a valid spot (solid block with air above)
                    if (serverWorld.getBlockState(checkPos).isSolidBlock(serverWorld, checkPos)
                            && serverWorld.getBlockState(abovePos).isAir()) {

                        // Don't convert if already cursed
                        net.minecraft.block.BlockState currentState = serverWorld.getBlockState(checkPos);
                        if (currentState.isOf(ModBlocks.CURSED_EARTH) || currentState.isOf(ModBlocks.CURSED_SAND)) {
                            break;
                        }

                        // Replace the top block with Cursed Earth or Cursed Sand
                        if (currentState.isOf(net.minecraft.block.Blocks.SAND) ||
                            currentState.isOf(net.minecraft.block.Blocks.RED_SAND)) {
                            serverWorld.setBlockState(checkPos, ModBlocks.CURSED_SAND.getDefaultState());
                        } else {
                            serverWorld.setBlockState(checkPos, ModBlocks.CURSED_EARTH.getDefaultState());
                        }
                        blocksConverted++;

                        // Spawn dark particles at conversion
                        if (this.random.nextFloat() < 0.5f) {
                            serverWorld.spawnParticles(ParticleTypes.SOUL,
                                    checkPos.getX() + 0.5, checkPos.getY() + 1.0, checkPos.getZ() + 0.5,
                                    2, 0.3, 0.2, 0.3, 0.02);
                        }
                        break;
                    }
                }
            }
        }

        // Spawn Khamsin Spread entities around the affected area (just 1-2)
        int khamsinCount = 1 + this.random.nextInt(2); // 1-2 entities (reduced)
        for (int i = 0; i < khamsinCount; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2;
            double dist = 1.0 + this.random.nextDouble() * (radius + 1);
            double khamsinX = player.getX() + Math.cos(angle) * dist;
            double khamsinZ = player.getZ() + Math.sin(angle) * dist;
            double khamsinY = player.getY() + 0.3 + this.random.nextDouble() * 1.0;

            KhamsinSpreadSmallEntity khamsin = ModEntities.KHAMSIN_SPREAD_SMALL.create(serverWorld);
            if (khamsin != null) {
                khamsin.refreshPositionAndAngles(khamsinX, khamsinY, khamsinZ,
                        this.random.nextFloat() * 360, 0);
                serverWorld.spawnEntity(khamsin);
            }
        }

        // Spawn additional particles in a burst around player
        serverWorld.spawnParticles(ParticleTypes.SMOKE,
                player.getX(), player.getY() + 0.5, player.getZ(),
                20, radius * 0.5, 0.5, radius * 0.5, 0.05);
    }

    /* ---------- PICKUP ATTACK ---------- */

    /**
     * Server-side tick for the pickup attack - handles throw timing and damage
     */
    private void tickPickupAttack() {
        PlayerEntity grabbed = grabbedPlayerCache;
        if (grabbed == null || !grabbed.isAlive()) {
            // Player died or disconnected, cancel the attack
            setAttackState(ATTACK_NONE);
            setGrabbedPlayerUUID(null);
            grabbedPlayerCache = null;
            pickupCooldownTicks = PICKUP_COOLDOWN_TICKS / 2; // Shorter cooldown on cancel
            return;
        }

        // Check animation phases
        int tick = pickupAnimationTicks;

        // Throw phase - release the player with velocity
        if (tick >= PICKUP_RELEASE_TICK && !playerHasBeenThrown) {
            playerHasBeenThrown = true;
            throwGrabbedPlayer(grabbed);
        }

        // Animation complete
        if (tick >= PICKUP_ANIMATION_TICKS) {
            setAttackState(ATTACK_NONE);
            setGrabbedPlayerUUID(null);
            grabbedPlayerCache = null;
            pickupCooldownTicks = PICKUP_COOLDOWN_TICKS;
        }
    }

    /**
     * Throws the grabbed player with force in Zulmak's facing direction
     */
    private void throwGrabbedPlayer(PlayerEntity player) {
        // Calculate throw direction based on Zulmak's facing
        float yaw = this.getYaw() * (float) (Math.PI / 180.0);
        float pitch = -30.0f * (float) (Math.PI / 180.0); // Throw upward at 30 degrees

        double throwX = -Math.sin(yaw) * Math.cos(pitch) * PICKUP_THROW_POWER;
        double throwY = Math.sin(-pitch) * PICKUP_THROW_POWER * 0.8;
        double throwZ = Math.cos(yaw) * Math.cos(pitch) * PICKUP_THROW_POWER;

        // Apply velocity
        player.setVelocity(throwX, throwY, throwZ);
        player.velocityModified = true;

        // Deal some damage on throw
        player.damage(this.getDamageSources().mobAttack(this), 6.0f);

        // Play throw sound
        this.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.8f);

        // Spawn particles at release point
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.CRIT,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    15, 0.5, 0.5, 0.5, 0.2);
        }
    }

    /**
     * Updates the grabbed player's position based on the animation's player_locator bone.
     * This runs on both client and server to keep the player smoothly attached.
     *
     * The player_locator positions from the animation are in model units (pixels).
     * Blockbench uses 16 pixels = 1 block.
     *
     * Animation values are ABSOLUTE positions relative to the model origin (Zulmak's feet).
     * At peak (y=93 model units = 5.8 blocks), which matches Zulmak's raised hand height.
     */
    private void updateGrabbedPlayerPosition() {
        PlayerEntity grabbed = getGrabbedPlayer();
        if (grabbed == null || playerHasBeenThrown) {
            if (grabbed == null && !this.getWorld().isClient) {
                AncientCurse.LOGGER.warn("updateGrabbedPlayerPosition: grabbed player is null! pickupAnimationTicks={}, attackState={}, grabbedPlayerCache={}",
                    pickupAnimationTicks, getAttackState(), grabbedPlayerCache != null ? "present" : "null");
            }
            return;
        }

        int tick = pickupAnimationTicks;

        // Debug log every 10 ticks
        if (!this.getWorld().isClient && tick % 10 == 0) {
            AncientCurse.LOGGER.info("Updating grabbed player position: tick={}, player={}", tick, grabbed.getName().getString());
        }

        // Get the animated offset from keyframes
        Vec3d animOffset = interpolatePlayerLocator(tick);

        // Convert from model units (pixels) to world units (blocks)
        // 16 pixels = 1 block in Blockbench
        // NOTE: We negate X and Z because Blockbench model faces North (-Z), 
        // but Minecraft entity facing South (+Z) requires 180 degree rotation.
        double animX = -animOffset.x / 16.0;
        double animY = animOffset.y / 16.0;
        double animZ = -animOffset.z / 16.0;

        // Apply Zulmak's rotation to the horizontal offset
        // Note: Negative Z in model space is "in front of" the entity
        float yaw = this.getYaw() * (float) (Math.PI / 180.0);
        double rotatedX = animX * Math.cos(yaw) - animZ * Math.sin(yaw);
        double rotatedZ = animX * Math.sin(yaw) + animZ * Math.cos(yaw);

        // Calculate final position relative to Zulmak's feet
        // Animation Y values are absolute heights (93 model units = ~5.8 blocks at peak)
        double finalX = this.getX() + rotatedX;
        double finalY = this.getY() + animY;
        double finalZ = this.getZ() + rotatedZ;

        // Teleport the player to the calculated position
        grabbed.teleport(finalX, finalY, finalZ);
        grabbed.setVelocity(0, 0, 0);
        grabbed.velocityModified = true;

        // Prevent the player from moving/jumping while grabbed
        grabbed.fallDistance = 0;
    }

    /**
     * Interpolates the player_locator bone position based on animation tick.
     * Returns offset in model units (1/16 blocks).
     */
    private Vec3d interpolatePlayerLocator(int tick) {
        // Animation keyframe data at 0.6x speed (tick values multiplied by ~1.67)
        // Format: {tick, x, y, z}
        double[][] keyframes = {
            {0, 0, 0, -12},
            {6, 0, 7, -16},
            {14, 0, 65, -17},
            {22, 0, 90, -5},
            {31, 0, 93, 2},
            {39, 0, 93, 2},
            {47, 0, 72.3, -6.3},
            {56, -7.3, 72.3, -14.2},
            {67, -1.2, 78.1, -3.8},
            {72, -1.2, 85, 15.4},
            {80, -1.2, 85.1, -59.6}, // Release point
            {89, -1.2, 55, -94.2}
        };

        // Find the two keyframes to interpolate between
        int prevIdx = 0;
        int nextIdx = 0;
        for (int i = 0; i < keyframes.length - 1; i++) {
            if (tick >= keyframes[i][0] && tick < keyframes[i + 1][0]) {
                prevIdx = i;
                nextIdx = i + 1;
                break;
            }
            if (i == keyframes.length - 2) {
                prevIdx = i;
                nextIdx = i + 1;
            }
        }

        // Calculate interpolation factor
        double prevTick = keyframes[prevIdx][0];
        double nextTick = keyframes[nextIdx][0];
        double t = (nextTick > prevTick) ? (tick - prevTick) / (nextTick - prevTick) : 0;
        t = Math.max(0, Math.min(1, t)); // Clamp to [0, 1]

        // Smooth interpolation (ease in/out)
        t = t * t * (3 - 2 * t);

        // Interpolate position
        double x = keyframes[prevIdx][1] + (keyframes[nextIdx][1] - keyframes[prevIdx][1]) * t;
        double y = keyframes[prevIdx][2] + (keyframes[nextIdx][2] - keyframes[prevIdx][2]) * t;
        double z = keyframes[prevIdx][3] + (keyframes[nextIdx][3] - keyframes[prevIdx][3]) * t;

        return new Vec3d(x, y, z);
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

    public Optional<UUID> getGrabbedPlayerUUID() {
        return this.dataTracker.get(GRABBED_PLAYER_UUID);
    }

    public void setGrabbedPlayerUUID(UUID uuid) {
        this.dataTracker.set(GRABBED_PLAYER_UUID, Optional.ofNullable(uuid));
    }

    /**
     * Get the currently grabbed player entity (works on both client and server)
     */
    public PlayerEntity getGrabbedPlayer() {
        // Server-side: use cache
        if (!this.getWorld().isClient && grabbedPlayerCache != null) {
            return grabbedPlayerCache;
        }
        // Client-side: look up by UUID
        Optional<UUID> uuid = getGrabbedPlayerUUID();
        if (uuid.isPresent()) {
            return this.getWorld().getPlayerByUuid(uuid.get());
        }
        return null;
    }

    /* ---------- NBT PERSISTENCE ---------- */
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("AttackState", getAttackState());
        nbt.putInt("DownedPhaseCount", getDownedPhaseCount());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("AttackState")) {
            setAttackState(nbt.getInt("AttackState"));
        }
        if (nbt.contains("DownedPhaseCount")) {
            this.dataTracker.set(DOWNED_PHASE_COUNT, nbt.getInt("DownedPhaseCount"));
        }
    }

    /* ---------- SOUNDS ---------- */
    @Override
    protected SoundEvent getAmbientSound() {
        // Ambient sound is handled separately via handleAwakenedSound()
        // Return null to prevent default ambient sounds
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        // Uses zulmak_hurt which has hurt_1 and hurt_2 variants at different pitches
        return ModSounds.ZULMAK_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        // Return null - we play the death sound manually for more control
        return null;
    }
    
    @Override
    protected float getSoundVolume() {
        // Louder sounds for boss entity
        return 1.5f;
    }

    /**
     * Handles the awakened ambient sound - plays randomly when idle and not attacking
     */
    private void handleAwakenedSound() {
        // Only play when not in combat and cooldown expired
        if (getAttackState() != ATTACK_NONE || awakenedSoundCooldown > 0) {
            return;
        }

        // Check if there are players nearby to hear it
        List<PlayerEntity> nearbyPlayers = this.getWorld().getEntitiesByClass(
                PlayerEntity.class,
                new Box(this.getBlockPos()).expand(32),
                player -> player.isAlive() && !player.isSpectator()
        );

        if (nearbyPlayers.isEmpty()) {
            return;
        }

        // Random chance to play awakened sound
        if (this.random.nextFloat() < AWAKENED_SOUND_CHANCE) {
            this.playSound(ModSounds.ZULMAK_AWAKENED, 1.5f, 0.9f + this.random.nextFloat() * 0.2f);
            // Set cooldown to prevent spam (20-60 seconds)
            awakenedSoundCooldown = AWAKENED_SOUND_MIN_INTERVAL + 
                    this.random.nextInt(AWAKENED_SOUND_MAX_INTERVAL - AWAKENED_SOUND_MIN_INTERVAL);
        }
    }

    /**
     * Handles the vortex wind loop sound - plays continuously when players are nearby
     */
    private void handleVortexWindSound() {
        vortexSoundTicks++;
        
        // Play the looping sound every ~2.5 seconds (50 ticks) to keep it continuous
        // The sound file should be designed to loop seamlessly
        if (vortexSoundTicks >= 50) {
            vortexSoundTicks = 0;
            
            // Check if players are nearby
            List<PlayerEntity> nearbyPlayers = this.getWorld().getEntitiesByClass(
                    PlayerEntity.class,
                    new Box(this.getBlockPos()).expand(VORTEX_SOUND_RANGE),
                    player -> player.isAlive() && !player.isSpectator()
            );

            if (!nearbyPlayers.isEmpty() && !playingDeathAnimation) {
                // Play vortex wind loop at entity position
                this.getWorld().playSound(
                        null, // null = all players can hear
                        this.getX(), this.getY(), this.getZ(),
                        ModSounds.ZULMAK_VORTEX_WIND_LOOP,
                        SoundCategory.HOSTILE,
                        0.8f,
                        0.95f + this.random.nextFloat() * 0.1f
                );
            }
        }
    }

    /**
     * Plays the player zap sound when attack_1 hits a player
     * Also has a chance to play wind_hit layered
     */
    public void playPlayerZapSound(PlayerEntity target) {
        if (this.getWorld().isClient) return;
        
        // Play main zap sound
        this.getWorld().playSound(
                null,
                target.getX(), target.getY(), target.getZ(),
                ModSounds.ZULMAK_PLAYER_ZAP,
                SoundCategory.HOSTILE,
                1.2f,
                1.0f
        );
        
        // 50% chance to layer wind hit sound
        if (this.random.nextFloat() < 0.5f) {
            this.getWorld().playSound(
                    null,
                    target.getX(), target.getY(), target.getZ(),
                    ModSounds.ZULMAK_WIND_HIT,
                    SoundCategory.HOSTILE,
                    0.8f,
                    0.9f + this.random.nextFloat() * 0.2f
            );
        }
    }

    /**
     * Plays summon sounds when Withered Pharaohs are summoned
     * Has a chance to layer summon_scream
     */
    public void playSummonSounds() {
        if (this.getWorld().isClient) return;
        
        // Play main summon sound (randomly picks summon_1 or summon_2 with pitch variation)
        this.playSound(ModSounds.ZULMAK_SUMMON, 1.3f, 0.95f + this.random.nextFloat() * 0.1f);
        
        // 30% chance to layer summon scream
        if (this.random.nextFloat() < 0.3f) {
            this.playSound(ModSounds.ZULMAK_SUMMON_SCREAM, 1.8f, 1.0f);
        }
    }

    /**
     * Plays cursed earth attack sound when spreading cursed earth
     * VERY LOUD - this is a major boss attack with earth rising/rumbling
     */
    public void playCursedEarthAttackSound() {
        if (this.getWorld().isClient) return;
        // Play at 3.0 volume for dramatic effect - earth is rising and rumbling!
        this.playSound(ModSounds.ZULMAK_CURSED_EARTH_ATTACK, 3.0f, 1.0f);
    }

    /* ---------- LIFECYCLE - BOSS BAR CLEANUP ---------- */
    @Override
    public void onDeath(DamageSource damageSource) {
        // Start death animation instead of immediately dying
        if (!playingDeathAnimation) {
            playingDeathAnimation = true;
            deathAnimationTicks = 0;

            // Play dramatic death scream (louder and lower pitch)
            this.playSound(ModSounds.ZULMAK_SUMMON_SCREAM, 2.5f, 0.6f);

            // Clean up boss bar on death
            if (!this.getWorld().isClient) {
                cleanupBossBar();
            }

            // Cancel any ongoing attacks
            setAttackState(ATTACK_NONE);
            setAttacking(false);

            // Release grabbed player if any
            if (grabbedPlayerCache != null) {
                setGrabbedPlayerUUID(null);
                grabbedPlayerCache = null;
            }
        }

        // Don't call super.onDeath yet - we'll do that after the animation
    }

    @Override
    protected void updatePostDeath() {
        // Custom death handling - delay removal until animation completes
        if (playingDeathAnimation) {
            deathAnimationTicks++;

            // Stop all movement during death
            this.setVelocity(0, this.getVelocity().y * 0.98, 0);
            this.setTarget(null);

            // Spawn death particles
            if (this.getWorld() instanceof ServerWorld serverWorld && deathAnimationTicks % 3 == 0) {
                // Soul particles escaping
                for (int i = 0; i < 2; i++) {
                    double offsetX = (this.random.nextDouble() - 0.5) * 0.8;
                    double offsetZ = (this.random.nextDouble() - 0.5) * 0.8;
                    serverWorld.spawnParticles(ParticleTypes.SOUL,
                            this.getX() + offsetX,
                            this.getY() + this.getHeight() * 0.5,
                            this.getZ() + offsetZ,
                            1, 0, 0.1, 0, 0.02);
                }

                // Smoke particles
                serverWorld.spawnParticles(ParticleTypes.SMOKE,
                        this.getX(), this.getY() + 0.5, this.getZ(),
                        2, 0.3, 0.3, 0.3, 0.01);
            }

            // After death animation finishes, check if this is 40% fake death or real death
            if (deathAnimationTicks >= DEATH_ANIMATION_DURATION) {
                // Check if this was the 40% "fake" death or real death
                if (hasPlayed40PercentDeathAnimation && !this.isDead()) {
                    // 40% health "death" - Zulmak rises again with godlike power!
                    if (this.getWorld() instanceof ServerWorld serverWorld) {
                        // Epic resurrection burst
                        serverWorld.spawnParticles(ParticleTypes.EXPLOSION_EMITTER,
                                this.getX(), this.getY() + 1.0, this.getZ(),
                                1, 0, 0, 0, 0);

                        // 30 soul fire particles burst upward
                        for (int i = 0; i < 30; i++) {
                            double offsetX = (this.random.nextDouble() - 0.5) * 2.0;
                            double offsetZ = (this.random.nextDouble() - 0.5) * 2.0;
                            serverWorld.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                                    this.getX() + offsetX,
                                    this.getY() + 0.2,
                                    this.getZ() + offsetZ,
                                    1, 0, 0.5, 0, 0.15);
                        }

                        // Flash of power
                        serverWorld.spawnParticles(ParticleTypes.FLASH,
                                this.getX(), this.getY() + 1.5, this.getZ(),
                                1, 0, 0, 0, 0);

                        // Resurrection sounds
                        this.playSound(SoundEvents.ENTITY_EVOKER_CAST_SPELL, 2.0f, 0.5f);
                        this.playSound(SoundEvents.ENTITY_VEX_CHARGE, 1.5f, 0.6f);
                        this.playSound(SoundEvents.ENTITY_WITHER_SPAWN, 1.0f, 0.7f);
                    }

                    // Reset death animation state
                    playingDeathAnimation = false;
                    deathAnimationTicks = 0;
                    setAttackState(ATTACK_NONE);

                    // Heal 10% of max health
                    float healAmount = this.getMaxHealth() * 0.1f;
                    this.setHealth(this.getHealth() + healAmount);

                } else {
                    // Real death - remove the entity
                    // Final burst of particles
                    if (this.getWorld() instanceof ServerWorld serverWorld) {
                        serverWorld.spawnParticles(ParticleTypes.POOF,
                                this.getX(), this.getY() + this.getHeight() * 0.5, this.getZ(),
                                20, 0.5, 0.5, 0.5, 0.05);
                        serverWorld.spawnParticles(ParticleTypes.SOUL,
                                this.getX(), this.getY() + 1.0, this.getZ(),
                                10, 0.3, 0.5, 0.3, 0.1);
                    }

                    // Now actually die
                    this.remove(Entity.RemovalReason.KILLED);
                }
            }
        }
    }

    /**
     * Check if currently playing death animation (for animation controller)
     */
    public boolean isPlayingDeathAnimation() {
        return playingDeathAnimation;
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
    private static final RawAnimation ANIM_PICKUP = RawAnimation.begin().thenPlay("animation.zulmak.pickup"); // Grab and throw
    private static final RawAnimation ANIM_LOCUST_SWARM = RawAnimation.begin().thenLoop("animation.zulmak.attack_2"); // Uses attack_2 for locust swarm
    private static final RawAnimation ANIM_DOWNED = RawAnimation.begin().thenPlayAndHold("animation.zulmak.downed"); // Downed phase - vulnerable pose

    // Animation speed multiplier (animations were made at 60% speed, so play at 0.6x)
    private static final double ANIMATION_SPEED = 0.6;

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Main movement controller - handles idle and walking (with smooth transitions)
        AnimationController<ZulmakEntity> movementController = new AnimationController<>(this, "movement_controller", 5, this::movementAnimationPredicate);
        movementController.setAnimationSpeed(ANIMATION_SPEED);
        controllers.add(movementController);

        // Attack controller - separate to allow attack animations to play on top of movement
        // Uses 5 tick transition for smooth blending when exiting animations
        AnimationController<ZulmakEntity> attackController = new AnimationController<>(this, "attack_controller", 5, this::attackAnimationPredicate);
        attackController.setAnimationSpeed(ANIMATION_SPEED);
        controllers.add(attackController);
    }

    /**
     * Movement animation predicate - handles idle, walk, and death
     * Death takes priority, then blocking/channeling (stops movement), then walk/idle based on movement state
     */
    private PlayState movementAnimationPredicate(AnimationState<ZulmakEntity> state) {
        // Death animation takes absolute priority and holds on last frame
        if (playingDeathAnimation || this.isDead()) {
            state.getController().setAnimation(ANIM_DEATH);
            return PlayState.CONTINUE;
        }

        // Downed animation - holds on downed pose
        if (isDowned()) {
            state.getController().setAnimation(ANIM_DOWNED);
            return PlayState.CONTINUE;
        }

        // While blocking, summoning, wind attack, pickup, locust swarm, or cursed earth, stay idle (no walking)
        int attackState = getAttackState();
        if (attackState == STATE_BLOCKING || attackState == ATTACK_SUMMON ||
            attackState == ATTACK_WIND || attackState == ATTACK_PICKUP ||
            attackState == ATTACK_LOCUST_SWARM || attackState == ATTACK_CURSED_EARTH) {
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
        // Don't play attacks if dead, dying, or downed
        if (playingDeathAnimation || this.isDead() || isDowned()) {
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

        if (attackState == ATTACK_WIND) {
            // Wind attack uses same animation as summon (attack_2 - arms raised channeling)
            if (lastAttackState != ATTACK_WIND) {
                state.getController().forceAnimationReset();
                state.getController().setAnimation(ANIM_SUMMON);
            }
            lastAttackState = attackState;
            return PlayState.CONTINUE;
        }

        if (attackState == ATTACK_PICKUP) {
            // Pickup attack - grabs player, holds up, throws
            if (lastAttackState != ATTACK_PICKUP) {
                state.getController().forceAnimationReset();
                state.getController().setAnimation(ANIM_PICKUP);
            }
            lastAttackState = attackState;
            return PlayState.CONTINUE;
        }

        if (attackState == ATTACK_LOCUST_SWARM) {
            // Locust swarm attack - uses attack_2 animation (arms raised channeling)
            if (lastAttackState != ATTACK_LOCUST_SWARM) {
                state.getController().forceAnimationReset();
                state.getController().setAnimation(ANIM_LOCUST_SWARM);
            }
            lastAttackState = attackState;
            return PlayState.CONTINUE;
        }

        if (attackState == ATTACK_CURSED_EARTH) {
            // Cursed earth attack - uses attack_2 animation (arms raised channeling)
            if (lastAttackState != ATTACK_CURSED_EARTH) {
                state.getController().forceAnimationReset();
                state.getController().setAnimation(ANIM_SUMMON); // Uses same animation as summon
            }
            lastAttackState = attackState;
            return PlayState.CONTINUE;
        }

        // No active attack animation - update last state
        lastAttackState = attackState;
        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    /* ---------- DOWNED PHASE ---------- */

    /**
     * Check if Zulmak is currently in downed phase
     */
    public boolean isDowned() {
        return this.dataTracker.get(IS_DOWNED);
    }

    /**
     * Set downed state (syncs to client)
     */
    private void setDowned(boolean downed) {
        this.dataTracker.set(IS_DOWNED, downed);
    }

    /**
     * Get current downed phase count (0-3)
     */
    public int getDownedPhaseCount() {
        return this.dataTracker.get(DOWNED_PHASE_COUNT);
    }

    /**
     * Get current downed ticks (for animation and particles)
     */
    public int getDownedTicks() {
        return this.dataTracker.get(DOWNED_TICKS);
    }

    /**
     * Check if we should trigger a downed phase based on health thresholds
     * Called every tick on server side
     */
    private void checkDownedPhaseThreshold() {
        if (isDowned() || playingDeathAnimation) return;

        int currentPhaseCount = getDownedPhaseCount();
        if (currentPhaseCount >= 3) return; // Already used all 3 downed phases

        float healthPercent = this.getHealth() / this.getMaxHealth();
        float threshold = DOWNED_HEALTH_THRESHOLDS[currentPhaseCount];

        // Trigger downed phase when health drops below next threshold
        if (healthPercent <= threshold) {
            startDownedPhase();
        }
    }

    /**
     * Start the downed phase - initial shockwave, become invulnerable, spawn minions
     */
    private void startDownedPhase() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) return;

        // Set downed state
        setDowned(true);
        this.dataTracker.set(DOWNED_TICKS, 0);
        this.dataTracker.set(DOWNED_PHASE_COUNT, getDownedPhaseCount() + 1);
        setAttackState(STATE_DOWNED);

        // Reset phase-specific flags
        hasTriggeredShockwave = false;
        hasSpawnedPharaohs = false;

        // Cancel all current actions
        setAttacking(false);
        attackAnimationTicks = 0;
        pendingAttackTarget = null;
        this.getNavigation().stop();

        // Clear target during downed phase
        this.setTarget(null);

        // Play downed sound
        this.playSound(SoundEvents.ENTITY_WITHER_HURT, 2.0f, 0.5f);
    }

    /**
     * Tick the downed phase - handle shockwave, particles, and recovery
     */
    private void tickDownedPhase() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) return;

        int currentTick = this.dataTracker.get(DOWNED_TICKS);
        this.dataTracker.set(DOWNED_TICKS, currentTick + 1);

        // Stop all movement
        this.getNavigation().stop();
        this.setVelocity(0, this.getVelocity().y, 0);

        // Trigger shockwave at the start (tick 0-5)
        if (!hasTriggeredShockwave && currentTick <= 5) {
            hasTriggeredShockwave = true;
            performDownedShockwave(serverWorld);
        }

        // Spawn Withered Pharaohs shortly after going down (around tick 40 = 2 seconds)
        if (!hasSpawnedPharaohs && currentTick >= 40 && currentTick <= 45) {
            hasSpawnedPharaohs = true;
            spawnDownedPhasePharaohs(serverWorld);
        }

        // Spawn swirling particles on server
        if (currentTick % 3 == 0) {
            spawnDownedPhaseParticlesServer(serverWorld);
        }

        // Recovery complete - exit downed phase
        if (currentTick >= DOWNED_DURATION_TICKS) {
            endDownedPhase();
        }
    }

    /**
     * Perform the initial shockwave blast when entering downed phase
     */
    private void performDownedShockwave(ServerWorld serverWorld) {
        // Play massive explosion sound
        this.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, 2.0f, 0.6f);
        this.playSound(SoundEvents.ENTITY_WITHER_BREAK_BLOCK, 1.5f, 0.8f);

        // Knock back all nearby entities
        List<LivingEntity> nearbyEntities = this.getWorld().getEntitiesByClass(
                LivingEntity.class,
                new Box(this.getBlockPos()).expand(DOWNED_SHOCKWAVE_RADIUS),
                entity -> entity != this && entity.isAlive()
        );

        for (LivingEntity entity : nearbyEntities) {
            // Calculate direction from Zulmak to entity
            double dx = entity.getX() - this.getX();
            double dz = entity.getZ() - this.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);

            if (distance < 0.5) distance = 0.5; // Prevent division by zero

            // Normalize direction
            double dirX = dx / distance;
            double dirZ = dz / distance;

            // Calculate knockback strength (stronger when closer)
            double distanceFactor = 1.0 - (distance / DOWNED_SHOCKWAVE_RADIUS);
            distanceFactor = Math.max(0.3, distanceFactor);

            double knockbackX = dirX * DOWNED_SHOCKWAVE_KNOCKBACK * distanceFactor;
            double knockbackZ = dirZ * DOWNED_SHOCKWAVE_KNOCKBACK * distanceFactor;
            double lift = DOWNED_SHOCKWAVE_LIFT * distanceFactor;

            // Apply velocity
            entity.setVelocity(
                    entity.getVelocity().x + knockbackX,
                    entity.getVelocity().y + lift,
                    entity.getVelocity().z + knockbackZ
            );
            entity.velocityModified = true;
        }

        // Create expanding ring of particles (shockwave visual)
        for (int ring = 0; ring < 5; ring++) {
            double radius = 2.0 + ring * 2.5;
            int particleCount = (int) (radius * 8);

            for (int i = 0; i < particleCount; i++) {
                double angle = (Math.PI * 2 / particleCount) * i;
                double x = this.getX() + Math.cos(angle) * radius;
                double z = this.getZ() + Math.sin(angle) * radius;
                double y = this.getY() + 0.5;

                // Outward velocity
                double velX = Math.cos(angle) * 0.4;
                double velZ = Math.sin(angle) * 0.4;

                serverWorld.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 2, 0.1, 0.3, 0.1, 0.02);
                serverWorld.spawnParticles(ParticleTypes.CLOUD, x, y, z, 1, 0.2, 0.2, 0.2, 0.1);
            }
        }

        // Central burst particles
        serverWorld.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.0, this.getZ(), 1, 0, 0, 0, 0);
        serverWorld.spawnParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 1.0, this.getZ(), 30, 1.0, 1.0, 1.0, 0.15);
        serverWorld.spawnParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 1.0, this.getZ(), 50, 0.5, 0.5, 0.5, 0.5);
    }

    /**
     * Spawn Withered Pharaohs during downed phase
     */
    private void spawnDownedPhasePharaohs(ServerWorld serverWorld) {
        for (int i = 0; i < DOWNED_PHARAOH_SPAWN_COUNT; i++) {
            double angle = (Math.PI * 2 / DOWNED_PHARAOH_SPAWN_COUNT) * i;
            double spawnDist = 3.0 + this.random.nextDouble() * 2.0;
            double spawnX = this.getX() + Math.cos(angle) * spawnDist;
            double spawnZ = this.getZ() + Math.sin(angle) * spawnDist;
            double spawnY = this.getY();

            WitheredPharaohEntity pharaoh = ModEntities.WITHERED_PHARAOH.create(serverWorld);
            if (pharaoh != null) {
                pharaoh.refreshPositionAndAngles(spawnX, spawnY, spawnZ, this.getYaw() + (180 / DOWNED_PHARAOH_SPAWN_COUNT * i), 0);
                pharaoh.initialize(serverWorld, serverWorld.getLocalDifficulty(pharaoh.getBlockPos()), SpawnReason.MOB_SUMMONED, null, null);
                pharaoh.setSummoner(this); // Set summoner for AI coordination

                serverWorld.spawnEntity(pharaoh);

                // Spawn effects at summon location
                serverWorld.spawnParticles(ParticleTypes.SOUL,
                        spawnX, spawnY + 1.0, spawnZ,
                        20, 0.5, 0.5, 0.5, 0.05);
                serverWorld.spawnParticles(ParticleTypes.POOF,
                        spawnX, spawnY, spawnZ,
                        15, 0.5, 0.3, 0.5, 0.05);
            }
        }

        // Play summon sound
        this.playSound(SoundEvents.ENTITY_WITHER_SPAWN, 1.0f, 0.7f);
    }

    /**
     * Spawn swirling magic particles during downed phase (server-side)
     */
    private void spawnDownedPhaseParticlesServer(ServerWorld serverWorld) {
        int tick = getDownedTicks();

        // Swirling soul particles around Zulmak
        for (int i = 0; i < 4; i++) {
            double angle = Math.toRadians((tick * 8) + (i * 90));
            double radius = 1.5 + Math.sin(tick * 0.1) * 0.5;
            double x = this.getX() + Math.cos(angle) * radius;
            double z = this.getZ() + Math.sin(angle) * radius;
            double y = this.getY() + 1.0 + Math.sin(tick * 0.15 + i) * 0.5;

            serverWorld.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 1, 0.05, 0.05, 0.05, 0.01);
        }

        // Rising portal particles (mystical regeneration effect)
        if (tick % 5 == 0) {
            for (int i = 0; i < 3; i++) {
                double offsetX = (this.random.nextDouble() - 0.5) * 1.5;
                double offsetZ = (this.random.nextDouble() - 0.5) * 1.5;
                serverWorld.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                        this.getX() + offsetX, this.getY(), this.getZ() + offsetZ,
                        3, 0.1, 0.5, 0.1, 0.05);
            }
        }
    }

    /**
     * Spawn swirling magic particles during downed phase (client-side)
     */
    private void spawnDownedPhaseParticlesClient() {
        // Update particle angle
        downedParticleAngle += 6f;
        if (downedParticleAngle >= 360) downedParticleAngle -= 360;

        int tick = getDownedTicks();

        // Inner ring of soul particles
        for (int i = 0; i < 3; i++) {
            double angle = Math.toRadians(downedParticleAngle + (i * 120));
            double radius = 1.0;
            double x = this.getX() + Math.cos(angle) * radius;
            double z = this.getZ() + Math.sin(angle) * radius;
            double y = this.getY() + 1.2 + Math.sin(tick * 0.1) * 0.3;

            this.getWorld().addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0, 0.02, 0);
        }

        // Outer ring of enchant particles
        if (tick % 2 == 0) {
            for (int i = 0; i < 2; i++) {
                double angle = Math.toRadians(-downedParticleAngle * 0.5 + (i * 180));
                double radius = 2.0;
                double x = this.getX() + Math.cos(angle) * radius;
                double z = this.getZ() + Math.sin(angle) * radius;
                double y = this.getY() + 0.5;

                this.getWorld().addParticle(ParticleTypes.ENCHANT, x, y + this.random.nextDouble() * 2, z, 0, 0.1, 0);
            }
        }
    }

    /**
     * End the downed phase and return to combat
     */
    private void endDownedPhase() {
        setDowned(false);
        setAttackState(ATTACK_NONE);

        // Reset flags for next time
        hasTriggeredShockwave = false;
        hasSpawnedPharaohs = false;

        // Play recovery sound
        this.playSound(SoundEvents.ENTITY_WITHER_AMBIENT, 1.5f, 0.9f);

        // Spawn recovery burst particles
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.SOUL,
                    this.getX(), this.getY() + 1.0, this.getZ(),
                    25, 0.8, 0.8, 0.8, 0.1);
            serverWorld.spawnParticles(ParticleTypes.FLASH,
                    this.getX(), this.getY() + 1.0, this.getZ(),
                    1, 0, 0, 0, 0);
        }
    }

    /* ---------- STATIC HELPER METHODS ---------- */
    
    /**
     * Checks if a player is currently grabbed by any Zulmak entity
     */
    public static boolean isPlayerGrabbed(PlayerEntity player) {
        // For now, check if player has a grabbed UUID tracked
        // This prevents conflicts with Jackel Binds
        return false; // TODO: Implement proper tracking if needed
    }
    
    /**
     * Releases a player from any Zulmak grab
     * Called when player disconnects to prevent issues
     */
    public static void releaseGrabbedPlayer(net.minecraft.server.network.ServerPlayerEntity player) {
        // Find any Zulmak entities that might be grabbing this player
        if (player.getWorld() != null) {
            player.getWorld().getEntitiesByClass(
                ZulmakEntity.class,
                player.getBoundingBox().expand(50),
                zulmak -> {
                    PlayerEntity grabbed = zulmak.getGrabbedPlayer();
                    return grabbed != null && grabbed.getUuid().equals(player.getUuid());
                }
            ).forEach(zulmak -> {
                zulmak.setGrabbedPlayerUUID(null);
                zulmak.grabbedPlayerCache = null;
                zulmak.setAttackState(ATTACK_NONE);
            });
        }
    }

}
