package com.ancientcurse.entity;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.ModEntities;
import com.ancientcurse.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import java.util.UUID;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity.RemovalReason;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.List;

/**
 * Thoth Entity - The Egyptian God of Wisdom, Magic, and Knowledge
 * A powerful boss that can cast time magic, summon entities, and perform devastating attacks
 */
public class ThothEntity extends HostileEntity implements GeoEntity {
    
    /* ---------- DATA TRACKERS ---------- */
    private static final TrackedData<Integer> ATTACK_STATE = 
            DataTracker.registerData(ThothEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> ATTACK_COOLDOWN = 
            DataTracker.registerData(ThothEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> IS_IN_COMBAT = 
            DataTracker.registerData(ThothEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IS_READING = 
            DataTracker.registerData(ThothEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IS_CASTING_TIME_MAGIC = 
            DataTracker.registerData(ThothEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> SUMMONING_COOLDOWN = 
            DataTracker.registerData(ThothEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> HAS_BEEN_IN_COMBAT = 
            DataTracker.registerData(ThothEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    
    /* ---------- CONSTANTS ---------- */
    private static final int ATTACK_NONE = 0;
    private static final int ATTACK_SCROLL_BLAST = 1;
    private static final int ATTACK_TIME_BEND = 2;
    private static final int ATTACK_ENTITY_SUMMON = 3;
    private static final int ATTACK_MELEE = 4;

    private static final int MAX_ATTACK_COOLDOWN = 30; // 1.5 seconds (reduced from 4s for aggressive melee)
    private static final int MAX_SUMMONING_COOLDOWN = 400; // 20 seconds
    private static final int TIME_MAGIC_DURATION = 103; // 5.125 seconds - matches TIME_BEND_ANIMATION_DURATION

    // Animation durations - MUST MATCH the actual animation file lengths (in ticks, 20 ticks = 1 second)
    // From thoth.animation.json:
    private static final int SPAWN_ANIMATION_DURATION = 60; // 3.0 seconds (entity_spawn animation is 3s)
    private static final int ATTACK_1_ANIMATION_DURATION = 45; // 2.25 seconds (attack_1 animation is 2.25s)
    private static final int ATTACK_2_ANIMATION_DURATION = 110; // 5.5 seconds (attack_2 animation is 5.5s)
    private static final int TIME_BEND_ANIMATION_DURATION = 103; // 5.125 seconds (time_bend animation is 5.125s)
    private static final int SUMMON_ANIMATION_DURATION = 60; // 3.0 seconds (entity_spawn is reused for summon)
    public static final int SPAWN_TRANSITION_DURATION = 60; // 3.0 seconds (matches entity_spawn animation)
    
    /* ---------- FIELDS ---------- */
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private ServerBossBar bossBar;
    private boolean hasSpawned = false;
    private int timeMagicTicks = 0;
    private boolean initialized = false;
    private int spawnTransitionTicks = 0;

    // Animation tracking
    private int attackAnimationTicks = 0;
    private boolean animationLocked = false; // Prevents animation interruptions
    private int lastAttackState = ATTACK_NONE; // Track last attack to prevent glitching

    // Combat state management
    private int combatTimeout = 0;
    private static final int MAX_COMBAT_TIMEOUT = 100; // 5 seconds
    private int timeBendCooldown = 0; // Dedicated cooldown for Time Bend to prevent spam
    private int retreatTicks = 0; // Timer for tactical retreat

    // Phase system
    private boolean hasEnteredPhase2 = false;
    private boolean hasEnteredPhase3 = false;

    // Performance optimization
    private float lastHealthPercentage = 1.0f; // Track last health percentage for boss bar updates
    private int bossBarUpdateCooldown = 0; // Cooldown for boss bar player list updates
    
    // Time distortion system - tracks entities frozen in time
    private final java.util.Map<UUID, Integer> timeFrozenEntities = new java.util.HashMap<>();
    private static final int TIME_DISTORTION_DURATION = 100; // 5 seconds
    private static final float TIME_DISTORTION_MULTIPLIER = 0.2f; // 20% speed (80% reduction)
    
    // Time Bend lift and throw system - tracks players being lifted
    private final java.util.Map<UUID, Integer> liftedPlayers = new java.util.HashMap<>(); // UUID -> tick when lift started
    private boolean hasThrown = false; // Track if we've thrown players this cast
    
    // Death animation system
    private boolean isPlayingDeathAnimation = false;
    private int deathAnimationTicks = 0;
    private static final int DEATH_ANIMATION_DURATION = 60; // 3 seconds for dramatic death
    
    public ThothEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 100; // Boss-level XP
        
        // Don't create boss bar in constructor - it will be created lazily in handleBossBar()
        // This prevents duplicate boss bars when entity is loaded from NBT
    }
    
    /* ---------- ATTRIBUTES ---------- */
    public static DefaultAttributeContainer.Builder createThothAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 300.0) // Boss health
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25) // Increased from 0.15 for better ground movement
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 15.0) // High damage
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0) // Increased from 32 to 64 for better ranged detection
                .add(EntityAttributes.GENERIC_ARMOR, 10.0) // High armor
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.6) // Normal knockback resistance
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.2);
    }
    
    /* ---------- INITIALIZATION ---------- */
    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new ThothRetreatGoal(this));
        this.goalSelector.add(1, new ThothTimeBendGoal(this));
        this.goalSelector.add(2, new ThothSummonEntitiesGoal(this));
        this.goalSelector.add(3, new ThothMagicAttackGoal(this));
        // REMOVED: Vanilla MeleeAttackGoal was conflicting with custom magic attack AI
        // All melee attacks are now handled through chooseStrategicAttack() in ThothMagicAttackGoal
        this.goalSelector.add(4, new WanderAroundFarGoal(this, 0.6) {
            @Override
            public boolean canStart() {
                // CRITICAL FIX: Don't wander during attacks
                return super.canStart() &&
                       ThothEntity.this.attackAnimationTicks == 0 &&
                       !ThothEntity.this.animationLocked;
            }
        });
        this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 16.0f));
        this.goalSelector.add(6, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }
    
    /* ---------- TICK ---------- */
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
        
        // Initialize entity on first tick with smooth transition
        if (!initialized) {
            initializeEntity();
            initialized = true;
        }
        
        // Handle spawn transition smoothly
        if (!hasSpawned && spawnTransitionTicks <= 0) {
            playSpawnAnimation();
            hasSpawned = true;
        }
        
        // Handle attack animation timer with animation locking and synced effects
        if (attackAnimationTicks > 0) {
            animationLocked = true; // Lock animation during attack

            // IMPROVED: Trigger effects at specific animation frames for perfect synchronization
            // Server-side only to prevent duplicate effects
            if (!this.getWorld().isClient) {
                handleAnimationFrameEffects();
            }

            attackAnimationTicks--;
            if (attackAnimationTicks == 0) {
                // Reset attack state when animation completes
                lastAttackState = dataTracker.get(ATTACK_STATE);
                dataTracker.set(ATTACK_STATE, ATTACK_NONE);
                dataTracker.set(IS_READING, false); // Reset reading state
                animationLocked = false; // Unlock animation
                
                // Clear lifted players when Time Bend ends
                liftedPlayers.clear();
                hasThrown = false;
            }
        } else {
            animationLocked = false; // Ensure animation is unlocked when not attacking
        }
        
        // Handle cooldowns
        int attackCd = dataTracker.get(ATTACK_COOLDOWN);
        if (attackCd > 0) dataTracker.set(ATTACK_COOLDOWN, attackCd - 1);
        
        int summonCd = dataTracker.get(SUMMONING_COOLDOWN);
        if (summonCd > 0) dataTracker.set(SUMMONING_COOLDOWN, summonCd - 1);
        
        if (timeBendCooldown > 0) timeBendCooldown--;
        if (retreatTicks > 0) retreatTicks--;
        
        // Handle time distortion effect on frozen entities
        if (!this.getWorld().isClient && !timeFrozenEntities.isEmpty()) {
            java.util.Iterator<java.util.Map.Entry<UUID, Integer>> iterator = timeFrozenEntities.entrySet().iterator();
            while (iterator.hasNext()) {
                java.util.Map.Entry<UUID, Integer> entry = iterator.next();
                UUID entityUUID = entry.getKey();
                int remainingTicks = entry.getValue();
                
                // Find the entity
                Entity entity = ((ServerWorld)this.getWorld()).getEntity(entityUUID);
                
                // Remove if entity is gone or dead
                if (entity == null || !entity.isAlive()) {
                    iterator.remove();
                    continue;
                }
                
                // Apply velocity scaling for smooth slow-motion effect
                Vec3d velocity = entity.getVelocity();
                // Only scale if entity is actually moving to avoid jittery standing
                if (velocity.horizontalLengthSquared() > 0.001 || Math.abs(velocity.y) > 0.01) {
                    entity.setVelocity(velocity.multiply(TIME_DISTORTION_MULTIPLIER));
                    entity.velocityModified = true;
                }
                
                // Spawn time distortion particles around entity
                if (this.age % 8 == 0 && entity instanceof LivingEntity) {
                    // Purple witch particles
                    ((ServerWorld)this.getWorld()).spawnParticles(
                        ParticleTypes.WITCH,
                        entity.getX(), entity.getY() + entity.getHeight() / 2, entity.getZ(),
                        2, 0.3, 0.3, 0.3, 0.05
                    );
                    // Portal particles for time distortion effect
                    ((ServerWorld)this.getWorld()).spawnParticles(
                        ParticleTypes.PORTAL,
                        entity.getX(), entity.getY() + entity.getHeight() / 2, entity.getZ(),
                        3, 0.3, 0.3, 0.3, 0.2
                    );
                }
                
                // Countdown and remove when expired
                remainingTicks--;
                if (remainingTicks <= 0) {
                    iterator.remove();
                } else {
                    entry.setValue(remainingTicks);
                }
            }
        }
        
        // Handle death animation sequence
        if (isPlayingDeathAnimation) {
            // CRITICAL: Keep entity completely frozen in place
            this.setVelocity(Vec3d.ZERO);
            this.velocityModified = true;
            
            if (!this.getWorld().isClient) {
                this.getNavigation().stop();
            }
            
            deathAnimationTicks--;
            int frame = DEATH_ANIMATION_DURATION - deathAnimationTicks;
            
            // Lightning strikes at key moments
            if (frame == 10 || frame == 25 || frame == 40 || frame == 55) {
                // Spawn lightning bolt at Thoth's position
                net.minecraft.entity.LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(this.getWorld());
                if (lightning != null) {
                    lightning.refreshPositionAfterTeleport(this.getX(), this.getY(), this.getZ());
                    lightning.setCosmetic(true); // Visual only, no fire/damage
                    this.getWorld().spawnEntity(lightning);
                }
                
                // Epic thunder sound
                this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, this.getSoundCategory(), 3.0f, 0.8f);
            }
            
            // Continuous mystical particle effects with progression
            if (!this.getWorld().isClient && this.getWorld() instanceof ServerWorld serverWorld) {
                double progress = frame / (double)DEATH_ANIMATION_DURATION; // 0.0 to 1.0
                
                // Circular particle ring that expands and rises
                if (frame % 2 == 0) {
                    double radius = 1.0 + (progress * 3.0); // Expands from 1 to 4 blocks
                    double height = this.getY() + (progress * 2.0); // Rises up
                    int particleCount = 12;
                    
                    for (int i = 0; i < particleCount; i++) {
                        double angle = (i / (double)particleCount) * Math.PI * 2.0;
                        double px = this.getX() + Math.cos(angle) * radius;
                        double pz = this.getZ() + Math.sin(angle) * radius;
                        
                        // Purple witch particles forming ring
                        serverWorld.spawnParticles(
                            ParticleTypes.WITCH,
                            px, height, pz,
                            1, 0, 0.1, 0, 0.05
                        );
                        
                        // Portal particles for mystical effect
                        if (i % 2 == 0) {
                            serverWorld.spawnParticles(
                                ParticleTypes.PORTAL,
                                px, height, pz,
                                2, 0.1, 0.1, 0.1, 0.3
                            );
                        }
                    }
                }
                
                // Rising soul particles - intensity increases over time
                if (frame % 3 == 0) {
                    int soulCount = (int)(3 + progress * 7); // 3 to 10 particles
                    serverWorld.spawnParticles(
                        ParticleTypes.SOUL,
                        this.getX(), this.getY() + 0.5, this.getZ(),
                        soulCount, 0.3, 0.3, 0.3, 0.15
                    );
                }
                
                // Intensifying effects in final third
                if (progress > 0.66) {
                    // Reverse portal vortex
                    if (frame % 2 == 0) {
                        serverWorld.spawnParticles(
                            ParticleTypes.REVERSE_PORTAL,
                            this.getX(), this.getY() + 1.5, this.getZ(),
                            15, 0.8, 1.0, 0.8, 0.4
                        );
                    }
                    
                    // Enchantment glyphs swirling
                    if (frame % 3 == 0) {
                        serverWorld.spawnParticles(
                            ParticleTypes.ENCHANT,
                            this.getX(), this.getY() + 1.0, this.getZ(),
                            8, 1.0, 1.0, 1.0, 0.5
                        );
                    }
                }
            }
            
            // Sound effects throughout
            if (frame == 15) {
                this.playSound(SoundEvents.BLOCK_END_PORTAL_SPAWN, 2.0f, 0.5f);
            } else if (frame == 30) {
                this.playSound(SoundEvents.ENTITY_WARDEN_DEATH, 2.0f, 0.7f);
            } else if (frame == 50) {
                this.playSound(SoundEvents.ENTITY_ENDER_DRAGON_DEATH, 1.5f, 1.2f);
            }
            
            // Final explosion and removal
            if (deathAnimationTicks <= 0) {
                // Massive particle explosion
                if (this.getWorld() instanceof ServerWorld serverWorld) {
                    serverWorld.spawnParticles(
                        ParticleTypes.EXPLOSION_EMITTER,
                        this.getX(), this.getY() + 1.0, this.getZ(),
                        1, 0, 0, 0, 0
                    );
                    
                    serverWorld.spawnParticles(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        this.getX(), this.getY() + 1.0, this.getZ(),
                        50, 2.0, 2.0, 2.0, 0.3
                    );
                }
                
                // Final sound
                this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENTITY_GENERIC_EXPLODE, this.getSoundCategory(), 3.0f, 0.5f);
                
                // Clean up boss bar
                if (this.bossBar != null) {
                    this.bossBar.setVisible(false);
                    this.bossBar.clearPlayers();
                    this.bossBar = null;
                }
                
                // Actually kill the entity now
                this.setInvulnerable(false);
                this.kill();
                return;
            }
        }
        
        // Handle spawn transition timer
        if (spawnTransitionTicks > 0) {
            spawnTransitionTicks--;
            // Prevent movement during spawn animation
            this.setVelocity(Vec3d.ZERO);
            if (spawnTransitionTicks == 0) {
                hasSpawned = true; // Mark spawn as complete
                // Enable floating after spawn
                this.setNoGravity(true);
                // Ensure spawn animation state is cleared
                dataTracker.set(ATTACK_STATE, ATTACK_NONE);
            }
        }
        
        // Update combat state based on target and timeout
        boolean hasTarget = this.getTarget() != null;
        if (hasTarget) {
            combatTimeout = MAX_COMBAT_TIMEOUT;
        } else if (combatTimeout > 0) {
            combatTimeout--;
        }

        boolean isInCombat = combatTimeout > 0;
        if (isInCombat != dataTracker.get(IS_IN_COMBAT)) {
            dataTracker.set(IS_IN_COMBAT, isInCombat);

            if (isInCombat) {
                dataTracker.set(HAS_BEEN_IN_COMBAT, true); // Set this permanently
                // Always use gravity in combat to prevent floating exploitation
                this.setNoGravity(false);
            } else if (hasSpawned) {
                // Only float when idle and on solid ground
                BlockPos below = this.getBlockPos().down();
                if (this.getWorld().getBlockState(below).isSolidBlock(this.getWorld(), below)) {
                    this.setNoGravity(true);
                } else {
                    // Don't float over cliffs or in the air
                    this.setNoGravity(false);
                }
            }
        }

        // Additional safety check: prevent floating too high
        if (this.getY() > 320 || (!this.isOnGround() && this.fallDistance > 5)) {
            this.setNoGravity(false); // Force gravity if too high or falling
        }
        
        // Handle reading behavior (when peaceful and not moving)
        if (!isInCombat && !dataTracker.get(HAS_BEEN_IN_COMBAT) && this.getVelocity().lengthSquared() < 0.01 && hasSpawned) {
            // 30% chance to be reading when idle and peaceful
            if (this.random.nextFloat() < 0.3f) {
                dataTracker.set(IS_READING, true);
            } else if (this.random.nextFloat() < 0.1f) {
                // 10% chance to stop reading
                dataTracker.set(IS_READING, false);
            }
        } else {
            dataTracker.set(IS_READING, false);
        }
        
        // Handle time magic effects
        if (dataTracker.get(IS_CASTING_TIME_MAGIC)) {
            handleTimeMagic();
        }
        
        // Handle boss bar
        if (!this.getWorld().isClient) {
            handleBossBar();
            checkPhaseTransitions();
        }
        
        // Handle ambient magic particles (both client and server)
        if (hasSpawned && !isInSpawnTransition()) {
            handleMagicParticles();
        }
    }
    
    /**
     * Handle animation-synced effects by triggering them at specific frames
     * This ensures particles, projectiles, and damage sync perfectly with the animation
     */
    private void handleAnimationFrameEffects() {
        int attackState = dataTracker.get(ATTACK_STATE);
        
        // Calculate current frame using the correct duration constant for each attack type
        int currentFrame = switch(attackState) {
            case ATTACK_MELEE -> ATTACK_1_ANIMATION_DURATION - attackAnimationTicks;
            case ATTACK_SCROLL_BLAST -> ATTACK_2_ANIMATION_DURATION - attackAnimationTicks;
            case ATTACK_TIME_BEND -> TIME_BEND_ANIMATION_DURATION - attackAnimationTicks;
            case ATTACK_ENTITY_SUMMON -> SUMMON_ANIMATION_DURATION - attackAnimationTicks;
            default -> 0;
        };

        switch (attackState) {
            case ATTACK_MELEE:
                // attack_1 animation is 45 ticks (2.25s)
                // Ground slam hits at 1.5 seconds (30 ticks elapsed) = attackAnimationTicks 15
                // attackAnimationTicks counts DOWN from 45, so 1.5s into animation = 45 - 30 = 15 ticks remaining
                if (attackAnimationTicks == 15) {
                    // Play wind gust sound when staff hits ground
                    if (!this.getWorld().isClient) {
                        this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                            ModSounds.THOTH_WIND_GUST, this.getSoundCategory(), 1.3f, 0.9f);
                    }
                    applyMeleeDamage();
                }
                break;

            case ATTACK_SCROLL_BLAST:
                // attack_2 animation is 110 ticks (5.5s)
                // Apply blast damage at frame 90 (4.5 seconds in, when scroll releases energy)
                if (attackAnimationTicks == 20) {
                    // Play yell sound right when scroll releases energy (lower pitch for power)
                    if (!this.getWorld().isClient) {
                        this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                            ModSounds.THOTH_YELL, this.getSoundCategory(), 1.8f, 0.85f);
                        // Play wind gust sound for impact effect
                        this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                            ModSounds.THOTH_WIND_GUST, this.getSoundCategory(), 1.5f, 1.0f);
                    }
                    applyScrollBlastDamage();
                }

                // ENHANCED BUILDUP with particles, sounds, and screen shake
                int buildupFrame = ATTACK_2_ANIMATION_DURATION - attackAnimationTicks;

                // Particle intensity increases as buildup progresses
                if (buildupFrame < 85 && !this.getWorld().isClient) { // Stop at frame 85 (4.25s)
                    // More frequent particles as it builds
                    if (buildupFrame % 3 == 0) { // Every 3 ticks for intense swirl
                        spawnScrollWindupParticles();
                    }

                    // Screen shake for nearby players (increases with time)
                    if (buildupFrame % 5 == 0) {
                        applyScreenShakeToNearbyPlayers(buildupFrame);
                    }

                    // Buildup sound effects at key moments
                    if (buildupFrame == 20) {
                        // Early warning - mystical charging
                        this.playSound(SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 1.5f, 0.8f);
                    } else if (buildupFrame == 40) {
                        // Mid buildup - power intensifying
                        this.playSound(SoundEvents.BLOCK_BEACON_POWER_SELECT, 1.8f, 0.6f);
                    } else if (buildupFrame == 60) {
                        // Late buildup - critical mass approaching
                        this.playSound(SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, 2.0f, 0.5f);
                    } else if (buildupFrame == 75) {
                        // Final warning - about to release!
                        this.playSound(SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, 2.5f, 0.7f);
                    }
                }
                break;

            case ATTACK_TIME_BEND:
                // time_bend animation is 103 ticks (5.125s)
                // Start lifting players at beginning of animation
                if (attackAnimationTicks == 103) { // Animation just started
                    startLiftingPlayers();
                }
                // Apply gradual lift during animation (every tick)
                if (attackAnimationTicks > 51) { // Before throw
                    applyGradualLift();
                }
                // Sonic boom sound at 2.67 seconds (53 ticks into animation)
                if (attackAnimationTicks == 50) { // 103 - 53 = 50 ticks remaining (approx 2.67s)
                    this.playSound(SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 3.0f, 0.8f);
                }
                // THROW players at 2.6 seconds (52 ticks into animation)
                if (attackAnimationTicks == 51) { // 103 - 52 = 51 ticks remaining
                    throwLiftedPlayers();
                }
                // Time pulse visual effects at 2.75 seconds
                if (attackAnimationTicks == 48) { // 103 - 55 = 48 ticks remaining
                    performTimePulse();
                }
                break;

            case ATTACK_ENTITY_SUMMON:
                // entity_spawn animation is 60 ticks (3s)
                // Purple tornado particles build up from frame 0-40
                if (attackAnimationTicks > 20 && attackAnimationTicks <= 60) {
                    createSummonTornadoParticles();
                }
                // Summon entities at frame 40 (near end of casting)
                if (attackAnimationTicks == 20 && !this.getWorld().isClient) {
                    spawnSummonedEntities();
                }
                break;
        }
    }


    /**
     * Apply melee damage at animation peak - GROUND SLAM ATTACK
     * Triggered at frame 23/45 (staff hits ground in attack_1 animation)
     */
    private void applyMeleeDamage() {
        if (this.getWorld().isClient) return; // Server-side only

        // Ground slam affects area around Thoth (AoE melee)
        Box area = new Box(this.getBlockPos()).expand(6); // 6 block radius
        List<LivingEntity> nearbyEntities = this.getWorld().getNonSpectatingEntities(LivingEntity.class, area);

        for (LivingEntity entity : nearbyEntities) {
            if (entity != this && entity instanceof PlayerEntity player) {
                double distance = this.squaredDistanceTo(entity);

                // Damage falls off with distance
                float damage = 18.0f;
                if (distance > 16) { // 4+ blocks away
                    damage = 12.0f;
                }

                entity.damage(this.getDamageSources().mobAttack(this), damage);

                // Radial knockback from impact point
                Vec3d direction = entity.getPos().subtract(this.getPos()).normalize();
                double knockbackStrength = 1.5 - (Math.sqrt(distance) / 10.0); // Stronger close, weaker far
                entity.addVelocity(direction.x * knockbackStrength, 0.7, direction.z * knockbackStrength);

                // Brief slow effect only (removed Weakness)
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 0)); // Reduced duration 60->40, level 1->0

                // CAMERA SHAKE - Ground slam impact
                // Apply stronger shake for closer players
                int shakeTicks = distance > 16 ? 8 : 12; // Closer = longer shake
                player.hurtTime = shakeTicks;
                player.maxHurtTime = shakeTicks;
                player.velocityModified = true;
            }
        }

        // GROUND SLAM VISUAL EFFECTS
        BlockPos groundPos = this.getBlockPos().down();

        // 1. Central explosion effect
        ((ServerWorld)this.getWorld()).spawnParticles(
            ParticleTypes.EXPLOSION,
            this.getX(), this.getY(), this.getZ(),
            1, 0, 0, 0, 0
        );

        // 2. Radial ground particles (optimized - 16 particles in circle)
        for (int i = 0; i < 16; i++) {
            double angle = (i / 16.0) * Math.PI * 2;
            double radius = 3.0 + (this.random.nextDouble() * 2.0); // 3-5 blocks out

            double particleX = this.getX() + Math.cos(angle) * radius;
            double particleZ = this.getZ() + Math.sin(angle) * radius;
            double particleY = this.getY() + 0.1;

            // Block break particles flying outward
            ((ServerWorld)this.getWorld()).spawnParticles(
                ParticleTypes.POOF,
                particleX, particleY, particleZ,
                3, 0.2, 0.1, 0.2, 0.1
            );

            // Magical energy particles
            ((ServerWorld)this.getWorld()).spawnParticles(
                ParticleTypes.ENCHANT,
                particleX, particleY, particleZ,
                2, 0.1, 0.3, 0.1, 0.2
            );
        }

        // 3. Crit particles at impact center
        ((ServerWorld)this.getWorld()).spawnParticles(
            ParticleTypes.CRIT,
            this.getX(), this.getY(), this.getZ(),
            20, 1.5, 0.3, 1.5, 0.3
        );

        // 4. Shockwave effect with sweep attack particles
        ((ServerWorld)this.getWorld()).spawnParticles(
            ParticleTypes.SWEEP_ATTACK,
            this.getX(), this.getY() + 0.5, this.getZ(),
            3, 2.0, 0.1, 2.0, 0
        );

        // Play ground slam sound - use world.playSound for proper client sync
        this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
            ModSounds.THOTH_ATTACK_MELEE, this.getSoundCategory(), 1.5f, 0.7f);

        // Camera shake effect (additional impact sound)
        this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.ENTITY_WARDEN_SONIC_BOOM, this.getSoundCategory(), 0.8f, 1.5f);
    }

    /**
     * Apply scroll blast damage at animation peak
     * Creates ground pillars that rise up, then launch outward
     */
    private void applyScrollBlastDamage() {
        if (!this.getWorld().isClient) {
            Box area = new Box(this.getBlockPos()).expand(8);
            List<LivingEntity> entities = this.getWorld().getNonSpectatingEntities(LivingEntity.class, area);

            // Spawn ground pillars at each player's position
            for (LivingEntity entity : entities) {
                if (entity != this && entity instanceof PlayerEntity) {
                    // Create pillar effect at player's feet
                    spawnMagicPillar(entity.getPos());

                    // Apply damage
                    entity.damage(this.getDamageSources().mobAttack(this), 8.0f);
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 200, 1));

                    // Knockback away from Thoth
                    Vec3d direction = entity.getPos().subtract(this.getPos()).normalize();
                    entity.addVelocity(direction.x * 0.8, 0.5, direction.z * 0.8);
                }
            }

            // SHOCKWAVE EFFECT - Expanding rings of particles from Thoth's position
            spawnScrollBlastShockwave();

            // Create swirling magic that converges at Thoth, then explodes outward
            spawnScrollExplosionEffect();

            // Play primary blast sound (custom scroll blast)
            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSounds.THOTH_ATTACK_SCROLL_BLAST, this.getSoundCategory(), 2.0f, 0.8f);

            // Play EXPLOSION sound to match the visual blast
            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_GENERIC_EXPLODE, this.getSoundCategory(), 1.5f, 0.9f);

            // Add sonic boom for impact emphasis
            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_WARDEN_SONIC_BOOM, this.getSoundCategory(), 0.8f, 1.2f);
        }
    }

    /**
     * Spawn a magic pillar effect rising from the ground
     */
    private void spawnMagicPillar(Vec3d position) {
        ServerWorld world = (ServerWorld) this.getWorld();

        // Find ground level
        BlockPos groundPos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
            new BlockPos((int)position.x, (int)position.y, (int)position.z));
        double groundY = groundPos.getY();

        // Create pillar rising from ground (optimized - 4 height levels only)
        for (int height = 0; height <= 4; height++) {
            double y = groundY + height * 0.5;
            int particlesAtHeight = 8 - height; // Fewer particles as it rises

            // Ring of particles at each height
            for (int ring = 0; ring < particlesAtHeight; ring++) {
                double angle = (ring / (double)particlesAtHeight) * Math.PI * 2.0;
                double radius = 0.6 - (height * 0.1); // Pillar tapers as it rises

                double px = position.x + Math.cos(angle) * radius;
                double pz = position.z + Math.sin(angle) * radius;

                // Purple witch particles forming pillar
                world.spawnParticles(
                    ParticleTypes.WITCH,
                    px, y, pz,
                    1, 0.05, 0.1, 0.05, 0.02
                );

                // Add some enchantment glyphs for magic effect
                if (height % 2 == 0) {
                    world.spawnParticles(
                        ParticleTypes.ENCHANT,
                        px, y, pz,
                        1, 0.1, 0.1, 0.1, 0.3
                    );
                }
            }
        }

        // Play pillar rising sound
        this.getWorld().playSound(null, position.x, groundY, position.z,
            SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, this.getSoundCategory(), 0.8f, 0.7f);
    }

    /**
     * Create expanding shockwave rings on the ground
     * Multiple rings expand outward at different speeds for dramatic effect
     */
    private void spawnScrollBlastShockwave() {
        ServerWorld world = (ServerWorld) this.getWorld();

        // Find ground level beneath Thoth
        BlockPos groundPos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, this.getBlockPos());
        double groundY = groundPos.getY() + 0.1; // Slightly above ground

        // Create 4 expanding shockwave rings at different radii
        int[] radii = {2, 4, 6, 8}; // Inner to outer rings

        for (int ringIndex = 0; ringIndex < radii.length; ringIndex++) {
            int radius = radii[ringIndex];
            int particlesInRing = radius * 16; // More particles for larger rings

            for (int i = 0; i < particlesInRing; i++) {
                double angle = (i / (double) particlesInRing) * Math.PI * 2.0;
                double px = this.getX() + Math.cos(angle) * radius;
                double pz = this.getZ() + Math.sin(angle) * radius;

                // Outward velocity for expanding effect
                double velocityMultiplier = 0.15 + (ringIndex * 0.05); // Outer rings move faster
                double vx = Math.cos(angle) * velocityMultiplier;
                double vz = Math.sin(angle) * velocityMultiplier;

                // Purple witch particles for magical shockwave
                world.spawnParticles(
                    ParticleTypes.WITCH,
                    px, groundY, pz,
                    2, vx, 0.05, vz, 0.1
                );

                // Add flame particles for impact heat (every 4th particle)
                if (i % 4 == 0) {
                    world.spawnParticles(
                        ParticleTypes.FLAME,
                        px, groundY, pz,
                        1, vx * 0.5, 0.1, vz * 0.5, 0.05
                    );
                }

                // Enchanted hit sparkles on outer rings for extra visual pop
                if (ringIndex >= 2 && i % 6 == 0) {
                    world.spawnParticles(
                        ParticleTypes.ENCHANTED_HIT,
                        px, groundY + 0.2, pz,
                        1, 0, 0.1, 0, 0.1
                    );
                }

                // Sweep attack particles on the ground rings for impact lines
                if (ringIndex == 2 && i % 8 == 0) {
                    world.spawnParticles(
                        ParticleTypes.SWEEP_ATTACK,
                        px, groundY + 0.3, pz,
                        1, 0, 0, 0, 0
                    );
                }
            }
        }

        // Ground impact dust at center
        world.spawnParticles(
            ParticleTypes.POOF,
            this.getX(), groundY, this.getZ(),
            20, 1.5, 0.1, 1.5, 0.1
        );

        // Explosive burst at center
        world.spawnParticles(
            ParticleTypes.EXPLOSION_EMITTER,
            this.getX(), groundY + 0.5, this.getZ(),
            1, 0, 0, 0, 0
        );
    }

    /**
     * Create the main scroll explosion - swirling magic that launches outward
     */
    private void spawnScrollExplosionEffect() {
        ServerWorld world = (ServerWorld) this.getWorld();
        Vec3d center = this.getPos().add(0, this.getHeight() * 0.5, 0);

        // Create 3 spiral rings expanding outward (camera-friendly - above ground)
        for (int ring = 0; ring < 3; ring++) {
            double ringRadius = 2.0 + ring * 2.0; // 2, 4, 6 blocks
            int particleCount = 12 + ring * 4; // More particles in outer rings

            for (int i = 0; i < particleCount; i++) {
                double angle = (i / (double)particleCount) * Math.PI * 2.0;
                double px = center.x + Math.cos(angle) * ringRadius;
                double py = center.y + 0.3; // Slightly above center for visibility
                double pz = center.z + Math.sin(angle) * ringRadius;

                // Outward velocity for launch effect
                double vx = Math.cos(angle) * 0.3;
                double vz = Math.sin(angle) * 0.3;

                // Purple witch particles
                world.spawnParticles(
                    ParticleTypes.WITCH,
                    px, py, pz,
                    1, vx, 0.1, vz, 0.2
                );

                // Green portal particles for variety
                if (i % 3 == 0) {
                    world.spawnParticles(
                        ParticleTypes.PORTAL,
                        px, py, pz,
                        2, vx * 0.5, 0.05, vz * 0.5, 0.5
                    );
                }
            }
        }

        // Central explosion burst
        world.spawnParticles(
            ParticleTypes.EXPLOSION,
            center.x, center.y, center.z,
            3, 0.5, 0.5, 0.5, 0.0
        );

        // Ambient magical energy particles
        world.spawnParticles(
            ParticleTypes.SOUL,
            center.x, center.y, center.z,
            15, 1.5, 1.0, 1.5, 0.1
        );
    }

    /**
     * Apply screen shake to nearby players during scroll blast buildup
     * Intensity increases as the attack approaches
     * Uses velocity modification and damage ticks for visible camera shake
     */
    private void applyScreenShakeToNearbyPlayers(int buildupFrame) {
        if (this.getWorld().isClient) return;

        Box area = new Box(this.getBlockPos()).expand(12);
        List<PlayerEntity> players = this.getWorld().getNonSpectatingEntities(PlayerEntity.class, area);

        for (PlayerEntity player : players) {
            double distance = this.squaredDistanceTo(player);
            if (distance <= 144) { // Within 12 blocks
                // Shake intensity increases with buildup progress (0.5s to 4.25s)
                float progress = buildupFrame / 85.0f; // 0.0 to 1.0

                // FABRIC-COMPATIBLE SCREEN SHAKE
                // Uses velocity-only approach - no damage needed
                // This properly syncs in Fabric/multiplayer

                // Calculate shake intensity with distance falloff
                float baseStrength = 0.3f + (progress * 0.7f); // 0.3 to 1.0 (very strong)
                float distanceFactor = 1.0f - (float)(Math.sqrt(distance) / 12.0f);
                float shakeStrength = baseStrength * distanceFactor;

                // Apply shake every tick, but with punctuated bursts for better feel
                float currentShake = shakeStrength;
                if (buildupFrame % 5 == 0) {
                    currentShake *= 1.8f; // 80% stronger every 5 ticks (punctuated shake)
                }

                // Random shake in all directions
                double shakeX = (this.random.nextDouble() - 0.5) * currentShake;
                double shakeY = (this.random.nextDouble() - 0.5) * currentShake * 0.6; // More vertical for camera effect
                double shakeZ = (this.random.nextDouble() - 0.5) * currentShake;

                // Apply shake via addVelocity - this syncs properly in Fabric
                player.addVelocity(shakeX, shakeY, shakeZ);
                player.velocityModified = true; // Standard Minecraft velocity sync flag

                // AUDIO FEEDBACK: Rumble sound for immersion
                if (buildupFrame % 15 == 0 && progress > 0.3f) {
                    float pitch = 0.5f + (progress * 0.5f); // Rising pitch
                    player.playSound(SoundEvents.ENTITY_WARDEN_HEARTBEAT, 0.3f, pitch);
                }

                // VISUAL FEEDBACK: Ground particles beneath player
                if (buildupFrame % 8 == 0) {
                    ((ServerWorld)this.getWorld()).spawnParticles(
                        ParticleTypes.POOF,
                        player.getX(), player.getY(), player.getZ(),
                        3, 0.3, 0.0, 0.3, 0.02
                    );

                    // Electric sparks for intense buildup
                    if (progress > 0.5f) {
                        ((ServerWorld)this.getWorld()).spawnParticles(
                            ParticleTypes.ELECTRIC_SPARK,
                            player.getX(), player.getY() + 0.1, player.getZ(),
                            2, 0.2, 0.0, 0.2, 0.05
                        );
                    }
                }
            }
        }
    }

    /**
     * Spawn scroll windup particles during charging
     * Creates an intense swirling vortex building up to the blast
     */
    private void spawnScrollWindupParticles() {
        ServerWorld world = (ServerWorld) this.getWorld();
        Vec3d scrollPos = this.getPos().add(0, this.getHeight() * 0.7, 0);

        // Use age for consistent rotation
        double time = this.age * 0.2;

        // Create dual-layer swirling vortex
        for (int layer = 0; layer < 2; layer++) {
            double layerRadius = 1.8 - (layer * 0.4); // Outer: 1.8, Inner: 1.4
            double layerHeight = layer * 0.3; // Slight vertical offset
            int particleCount = 8 - (layer * 2); // Outer: 8, Inner: 6

            for (int i = 0; i < particleCount; i++) {
                // Spiral angle - rotates over time
                double angle = (i / (double)particleCount) * Math.PI * 2.0 + time + (layer * Math.PI / 4);

                double px = scrollPos.x + Math.cos(angle) * layerRadius;
                double py = scrollPos.y + layerHeight;
                double pz = scrollPos.z + Math.sin(angle) * layerRadius;

                // Tangential velocity for swirling + inward for converging
                double vx = -Math.sin(angle) * 0.08 - Math.cos(angle) * 0.12; // Swirl + converge
                double vy = 0.03; // Gentle upward drift
                double vz = Math.cos(angle) * 0.08 - Math.sin(angle) * 0.12;

                // Purple witch particles forming vortex
                world.spawnParticles(
                    ParticleTypes.WITCH,
                    px, py, pz,
                    1, vx, vy, vz, 0.03
                );

                // Enchantment glyphs for magical buildup (every other particle)
                if (i % 2 == 0) {
                    world.spawnParticles(
                        ParticleTypes.ENCHANT,
                        px, py, pz,
                        2, vx * 0.6, vy * 1.5, vz * 0.6, 0.4
                    );
                }

                // Portal particles for mystical depth (outer layer only)
                if (layer == 0 && i % 3 == 0) {
                    world.spawnParticles(
                        ParticleTypes.PORTAL,
                        px, py, pz,
                        1, vx * 0.3, vy * 0.8, vz * 0.3, 0.5
                    );
                }
            }
        }

        // Center concentration particles
        if (this.age % 2 == 0) {
            world.spawnParticles(
                ParticleTypes.SOUL,
                scrollPos.x, scrollPos.y, scrollPos.z,
                2, 0.1, 0.1, 0.1, 0.02
            );
        }
    }

    /**
     * Create purple tornado particles at each spawn location during summoning animation
     * Called every tick during the summon animation (40 ticks before entities spawn)
     */
    private void createSummonTornadoParticles() {
        if (this.getWorld().isClient) return; // Server-side only

        // Calculate number of spawn locations based on phase (same logic as spawnSummonedEntities)
        int beetleCount = 1; // Phase 1: 1 beetle
        if (hasEnteredPhase3) {
            beetleCount = 2; // Phase 3: 2 beetles
        } else if (hasEnteredPhase2) {
            beetleCount = 2; // Phase 2: 2 beetles
        }

        // Calculate animation progress (0.0 = start, 1.0 = beetles spawn)
        // attackAnimationTicks counts DOWN from 60 to 0
        // At frame 40 (ticks=20), beetles spawn, so we want progress from 0 to 1 as ticks go from 60 to 20
        float progress = (60 - attackAnimationTicks) / 40.0f; // 0.0 at start, 1.0 when beetles spawn
        progress = Math.min(1.0f, Math.max(0.0f, progress)); // Clamp to [0, 1]

        // Spawn tornado particles at each beetle spawn location
        for (int i = 0; i < beetleCount; i++) {
            // Same positioning logic as spawnSummonedEntities
            double angle = (i / (double) beetleCount) * Math.PI * 2.0;
            double spawnX = this.getX() + Math.cos(angle) * 3.0; // 3 blocks away
            double spawnZ = this.getZ() + Math.sin(angle) * 3.0;

            // Find ground level
            BlockPos spawnPos = new BlockPos((int) spawnX, (int) this.getY(), (int) spawnZ);
            spawnPos = this.getWorld().getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, spawnPos);

            double baseX = spawnPos.getX() + 0.5;
            double baseY = spawnPos.getY();
            double baseZ = spawnPos.getZ() + 0.5;

            // Create simple tornado effect - just a few spinning particles
            int particlesPerRing = 3; // Only 3 particles per ring
            int rings = 2; // 2 rings total

            for (int ring = 0; ring < rings; ring++) {
                // Height increases with progress (tornado builds upward)
                double height = progress * 2.5 * (ring / (double) rings);

                // Radius for tornado shape
                double radius = 0.6;

                for (int p = 0; p < particlesPerRing; p++) {
                    // Spiral angle for swirling effect
                    double particleAngle = (p / (double) particlesPerRing) * Math.PI * 2.0;
                    particleAngle += (ring * 0.5) + (progress * Math.PI * 4.0); // Spinning effect

                    double offsetX = Math.cos(particleAngle) * radius;
                    double offsetZ = Math.sin(particleAngle) * radius;

                    double particleX = baseX + offsetX;
                    double particleY = baseY + height;
                    double particleZ = baseZ + offsetZ;

                    // Dark purple particles (DRAGON_BREATH for dark purple glow)
                    ((ServerWorld)this.getWorld()).spawnParticles(
                        ParticleTypes.DRAGON_BREATH,
                        particleX, particleY, particleZ,
                        1, 0.01, 0.03, 0.01, 0.01
                    );

                    // Enchant particles (only on first ring)
                    if (ring == 0) {
                        ((ServerWorld)this.getWorld()).spawnParticles(
                            ParticleTypes.ENCHANT,
                            particleX, particleY, particleZ,
                            1, 0.05, 0.1, 0.05, 0.2
                        );
                    }
                }
            }
        }

        // Sound effect that increases in pitch as tornado builds
        if (attackAnimationTicks % 10 == 0) { // Every 10 ticks (0.5 seconds)
            float pitch = 0.5f + (progress * 0.5f); // Pitch rises from 0.5 to 1.0
            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.BLOCK_PORTAL_AMBIENT, this.getSoundCategory(), 0.3f, pitch);
        }
    }

    /**
     * Spawn summoned entities at animation peak
     */
    private void spawnSummonedEntities() {
        if (this.getWorld().isClient) return; // Server-side only

        // Calculate number of beetles to summon based on phase
        int beetleCount = 1; // Phase 1: 1 beetle
        if (hasEnteredPhase3) {
            beetleCount = 2; // Phase 3: 2 beetles
        } else if (hasEnteredPhase2) {
            beetleCount = 2; // Phase 2: 2 beetles
        }

        // Spawn scarab beetles in a circle around Thoth
        for (int i = 0; i < beetleCount; i++) {
            double angle = (i / (double) beetleCount) * Math.PI * 2.0;
            double spawnX = this.getX() + Math.cos(angle) * 3.0; // 3 blocks away
            double spawnZ = this.getZ() + Math.sin(angle) * 3.0;

            // Find valid spawn position (ground level)
            BlockPos spawnPos = new BlockPos((int) spawnX, (int) this.getY(), (int) spawnZ);
            spawnPos = this.getWorld().getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, spawnPos);

            try {
                // Create scarab beetle entity
                ScarabBeetleEntity beetle = ModEntities.SCARAB_BEETLE.create(this.getWorld());
                if (beetle != null) {
                    beetle.refreshPositionAndAngles(
                        spawnPos.getX() + 0.5,
                        spawnPos.getY(),
                        spawnPos.getZ() + 0.5,
                        this.random.nextFloat() * 360.0f,
                        0.0f
                    );

                    // Make beetle aggressive (not tamed, not sitting)
                    beetle.setTamed(false);
                    beetle.setSitting(false); // CRITICAL: Ensure beetle is standing and ready to attack
                    
                    // Mark as summoned with 3-minute lifespan and link to Thoth
                    beetle.setSummoned(this.getUuid());

                    // Set target to Thoth's target (if any)
                    if (this.getTarget() != null) {
                        beetle.setTarget(this.getTarget());
                    }

                    // Spawn the beetle
                    this.getWorld().spawnEntity(beetle);

                    // Spawn particles at summon location
                    ((ServerWorld)this.getWorld()).spawnParticles(
                        ParticleTypes.POOF,
                        beetle.getX(), beetle.getY() + 0.5, beetle.getZ(),
                        10, 0.3, 0.3, 0.3, 0.1
                    );

                    // Dark purple magical particles
                    ((ServerWorld)this.getWorld()).spawnParticles(
                        ParticleTypes.DRAGON_BREATH,
                        beetle.getX(), beetle.getY() + 0.5, beetle.getZ(),
                        2, 0.3, 0.5, 0.3, 0.05
                    );

                    AncientCurse.LOGGER.info("Thoth summoned scarab beetle #{} at ({}, {}, {})",
                        i + 1, beetle.getX(), beetle.getY(), beetle.getZ());
                }
            } catch (Exception e) {
                AncientCurse.LOGGER.error("Failed to summon scarab beetle: {}", e.getMessage());
            }
        }

        // Play summon completion sound
        this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.ENTITY_EVOKER_CAST_SPELL, this.getSoundCategory(), 1.5f, 0.8f);
    }

    private void initializeEntity() {
        dataTracker.set(IS_IN_COMBAT, false);
        dataTracker.set(IS_READING, false);
        dataTracker.set(IS_CASTING_TIME_MAGIC, false);
        dataTracker.set(ATTACK_STATE, ATTACK_NONE);
        dataTracker.set(ATTACK_COOLDOWN, 0);
        dataTracker.set(SUMMONING_COOLDOWN, 0);
        
        // Start with gravity enabled to prevent initial shaking
        this.setNoGravity(false);
        this.setVelocity(Vec3d.ZERO); // Ensure no initial velocity
        
        // Set spawn animation duration
        spawnTransitionTicks = 80; // 4 seconds for spawn animation
        hasSpawned = false; // Ensure spawn animation plays
        
        // Play spawn sound
        this.playSound(ModSounds.THOTH_AMBIENT, 2.0f, 0.8f);
    }
    
    private void handleTimeMagic() {
        timeMagicTicks++;
        
        if (!this.getWorld().isClient) {
            // Enhanced time magic effects
            Box area = new Box(this.getBlockPos()).expand(16);
            List<LivingEntity> entities = this.getWorld().getNonSpectatingEntities(LivingEntity.class, area);
            
            for (LivingEntity entity : entities) {
                if (entity != this && entity instanceof PlayerEntity) {
                    // TIME DISTORTION: Make players float and spin weirdly
                    Vec3d currentVel = entity.getVelocity();
                    
                    // Random upward/downward floating (20% chance per tick)
                    if (this.random.nextFloat() < 0.2f) {
                        double floatStrength = (this.random.nextDouble() - 0.5) * 0.4; // Random up/down
                        entity.addVelocity(0, floatStrength, 0);
                        entity.velocityModified = true;
                        
                        // Whoosh sound for movement
                        if (this.random.nextFloat() < 0.3f) {
                            this.getWorld().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                                SoundEvents.ENTITY_PHANTOM_FLAP, this.getSoundCategory(), 0.5f, 1.5f);
                        }
                    }
                    
                    // Random horizontal push/pull (15% chance per tick)
                    if (this.random.nextFloat() < 0.15f) {
                        Vec3d toThoth = this.getPos().subtract(entity.getPos()).normalize();
                        // Sometimes pull toward Thoth, sometimes push away
                        double pushStrength = (this.random.nextBoolean() ? 0.3 : -0.3);
                        entity.addVelocity(toThoth.x * pushStrength, 0, toThoth.z * pushStrength);
                        entity.velocityModified = true;
                        
                        // Whoosh sound for push/pull
                        if (this.random.nextFloat() < 0.3f) {
                            this.getWorld().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                                SoundEvents.ENTITY_PHANTOM_FLAP, this.getSoundCategory(), 0.6f, 0.8f);
                        }
                    }
                    
                    // Micro-teleport shift (5% chance) - disorients player
                    if (this.random.nextFloat() < 0.05f) {
                        double shiftX = (this.random.nextDouble() - 0.5) * 2.0; // ±1 block
                        double shiftZ = (this.random.nextDouble() - 0.5) * 2.0;
                        entity.teleport(entity.getX() + shiftX, entity.getY(), entity.getZ() + shiftZ);
                        
                        // Enderman teleport sound for micro-shift
                        this.getWorld().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                            SoundEvents.ENTITY_ENDERMAN_TELEPORT, this.getSoundCategory(), 0.3f, 2.0f);
                    }
                }
            }
            
            // Heal Thoth during time magic - reduced healing
            if (timeMagicTicks % 40 == 0) { // Changed from every 20 ticks to every 40 ticks
                this.heal(1.5f); // Reduced from 3.0f to 1.5f
            }
        }
        
        // CUSTOM: Purple tornado/portal vortex effect around Thoth during Time Bend - SERVER ONLY
        if (timeMagicTicks % 3 == 0 && !this.getWorld().isClient) { // Every 3 ticks for smooth animation
            double time = timeMagicTicks * 0.15; // Rotation speed for swirling effect
            Vec3d center = this.getPos().add(0, this.getHeight() * 0.5, 0);

            // Create spiraling tornado/portal effect with 4 helixes rising upward
            for (int helix = 0; helix < 4; helix++) {
                double helixOffset = (helix / 4.0) * Math.PI * 2; // Evenly space 4 helixes

                // Each helix has particles at different heights creating upward spiral
                for (int level = 0; level < 6; level++) {
                    double heightRatio = level / 6.0; // 0 to 1 from bottom to top
                    double y = center.y - 1.0 + (heightRatio * 3.5); // -1 to +2.5 blocks (3.5 block tall vortex)

                    // Tornado shape: wider at bottom and top, narrower in middle
                    double radiusFactor = 0.6 + 0.4 * Math.sin(heightRatio * Math.PI); // 0.6 to 1.0
                    double radius = (2.5 - heightRatio * 0.8) * radiusFactor; // Starts wide, tapers up

                    // Spiral angle: rotates as it goes up
                    double spiralRotation = time + helixOffset + (heightRatio * Math.PI * 3);
                    double x = center.x + Math.cos(spiralRotation) * radius;
                    double z = center.z + Math.sin(spiralRotation) * radius;

                    // Velocity for swirling upward motion
                    double vx = -Math.sin(spiralRotation) * 0.05; // Tangential velocity
                    double vz = Math.cos(spiralRotation) * 0.05;
                    double vy = 0.08; // Gentle upward drift

                    // Purple witch particles (main color)
                    ((ServerWorld)this.getWorld()).spawnParticles(
                        ParticleTypes.WITCH,
                        x, y, z,
                        1, vx, vy, vz, 0.02
                    );

                    // Portal particles for mystical effect (every other level for optimization)
                    if (level % 2 == 0) {
                        ((ServerWorld)this.getWorld()).spawnParticles(
                            ParticleTypes.PORTAL,
                            x, y, z,
                            2, vx * 0.5, vy * 0.5, vz * 0.5, 0.3
                        );
                    }

                    // Dragon breath particles at middle heights for depth
                    if (level >= 2 && level <= 4 && helix % 2 == 0) {
                        ((ServerWorld)this.getWorld()).spawnParticles(
                            ParticleTypes.DRAGON_BREATH,
                            x, y, z,
                            1, vx * 0.3, vy * 0.6, vz * 0.3, 0.01
                        );
                    }
                }
            }

            // Ground ring effect - purple particles swirling at base
            if (timeMagicTicks % 6 == 0) {
                for (int i = 0; i < 12; i++) {
                    double angle = (i / 12.0) * Math.PI * 2 + time * 0.5;
                    double groundRadius = 2.8;
                    double x = center.x + Math.cos(angle) * groundRadius;
                    double z = center.z + Math.sin(angle) * groundRadius;
                    double y = center.y - 1.0;

                    // Witch particles at ground level
                    ((ServerWorld)this.getWorld()).spawnParticles(
                        ParticleTypes.WITCH,
                        x, y, z,
                        1, 0, 0.1, 0, 0.02
                    );

                    // Soul particles for ethereal glow
                    if (i % 3 == 0) {
                        ((ServerWorld)this.getWorld()).spawnParticles(
                            ParticleTypes.SOUL,
                            x, y, z,
                            1, 0, 0.05, 0, 0.01
                        );
                    }
                }
            }
        }
        
        if (timeMagicTicks >= TIME_MAGIC_DURATION) {
            dataTracker.set(IS_CASTING_TIME_MAGIC, false);
            timeMagicTicks = 0;
        }
    }
    
    private void handleBossBar() {
        // Initialize boss bar if needed
        if (this.bossBar == null) {
            try {
                this.bossBar = new ServerBossBar(
                    Text.literal("Thoth, the God of Wisdom"),
                    BossBar.Color.PURPLE,
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
                    new Box(this.getBlockPos()).expand(64)
                );

                // Add nearby players who aren't already tracking
                for (PlayerEntity player : nearbyPlayers) {
                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        if (!this.bossBar.getPlayers().contains(serverPlayer)) {
                            this.bossBar.addPlayer(serverPlayer);
                        }
                    }
                }

                // Remove players who are too far away (limit to 64 blocks)
                this.bossBar.getPlayers().removeIf(player -> {
                    double dist = this.squaredDistanceTo(player);
                    // Also check Y distance to prevent showing across dimensions/heights
                    double yDist = Math.abs(player.getY() - this.getY());
                    return dist > 64 * 64 || yDist > 64 || player.getWorld() != this.getWorld();
                });
            } else {
                bossBarUpdateCooldown--;
            }
        } catch (Exception e) {
            // Silently handle boss bar errors to prevent spam
        }
    }
    
    private void handleMagicParticles() {
        // Spawn particles on both client and server for better visibility
        if (this.random.nextInt(6) == 0) { // Reduced frequency to prevent lag
            for (int i = 0; i < 2; i++) { // Reduced particle count
                Vec3d particlePos = this.getPos().add(
                    (this.random.nextDouble() - 0.5) * 2.5,
                    this.random.nextDouble() * 2.5,
                    (this.random.nextDouble() - 0.5) * 2.5
                );
                
                if (!this.getWorld().isClient) {
                    // Server-side particles
                    ((ServerWorld)this.getWorld()).spawnParticles(
                        ParticleTypes.PORTAL,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0, 0.1, 0, 0.05
                    );
                } else {
                    // Client-side particles for immediate visual feedback
                    this.getWorld().addParticle(
                        ParticleTypes.PORTAL,
                        particlePos.x, particlePos.y, particlePos.z,
                        0, 0.1, 0
                    );
                }
            }
        }
    }
    
    private void playSpawnAnimation() {
        // Don't set attack state for spawn - it has its own priority
        spawnTransitionTicks = SPAWN_ANIMATION_DURATION;
        attackAnimationTicks = 0; // Don't interfere with spawn animation
        
        // Create dramatic spawn particles (improved)
        for (int i = 0; i < 15; i++) { // Reduced count
            Vec3d particlePos = this.getPos().add(
                (this.random.nextDouble() - 0.5) * 3,
                this.random.nextDouble() * 3,
                (this.random.nextDouble() - 0.5) * 3
            );
            
            if (!this.getWorld().isClient) {
                ((ServerWorld)this.getWorld()).spawnParticles(
                    ParticleTypes.DRAGON_BREATH,
                    particlePos.x, particlePos.y, particlePos.z,
                    2, 0.1, 0.1, 0.1, 0.1
                );
            } else {
                this.getWorld().addParticle(
                    ParticleTypes.DRAGON_BREATH,
                    particlePos.x, particlePos.y, particlePos.z,
                    0, 0.1, 0
                );
            }
        }
        
        // Play spawn sound
        this.playSound(ModSounds.THOTH_SPAWN, 2.0f, 0.8f);
    }
    
    private void checkPhaseTransitions() {
        float healthPercent = this.getHealth() / this.getMaxHealth();
        
        if (healthPercent <= 0.5f && !hasEnteredPhase2) {
            hasEnteredPhase2 = true;
            enterPhase2();
        }
        
        if (healthPercent <= 0.25f && !hasEnteredPhase3) {
            hasEnteredPhase3 = true;
            enterPhase3();
        }
    }
    
    private void enterPhase2() {
        this.playSound(ModSounds.THOTH_SUMMON, 3.0f, 0.5f);
        
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 100, 2));
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 200, 1));
        
        summonEntities();
        
        // Enhanced particles with client-server support
        for (int i = 0; i < 30; i++) { // Reduced count
            Vec3d particlePos = this.getPos().add(
                (this.random.nextDouble() - 0.5) * 6,
                this.random.nextDouble() * 4,
                (this.random.nextDouble() - 0.5) * 6
            );
            
            if (!this.getWorld().isClient) {
                ((ServerWorld)this.getWorld()).spawnParticles(
                    ParticleTypes.FLAME,
                    particlePos.x, particlePos.y, particlePos.z,
                    2, 0.2, 0.2, 0.2, 0.1
                );
            } else {
                this.getWorld().addParticle(
                    ParticleTypes.FLAME,
                    particlePos.x, particlePos.y, particlePos.z,
                    0, 0.1, 0
                );
            }
        }
    }
    
    private void enterPhase3() {
        hasEnteredPhase3 = true;
        
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 60, 4, false, false));
        
        if (!this.getWorld().isClient) {
            Box timeArea = new Box(this.getBlockPos()).expand(32);
            
            // VELOCITY-BASED TIME DISTORTION - affects all entities in range
            List<LivingEntity> affectedEntities = this.getWorld().getEntitiesByClass(
                LivingEntity.class, timeArea,
                entity -> entity != this && entity.isAlive() // Exclude Thoth himself
            );
            
            for (LivingEntity entity : affectedEntities) {
                // Add entity to time-frozen map with duration
                timeFrozenEntities.put(entity.getUuid(), TIME_DISTORTION_DURATION);
                
                // Send message to players
                if (entity instanceof ServerPlayerEntity serverPlayer) {
                    serverPlayer.sendMessage(
                        Text.literal("§5§lTime itself bends to Thoth's will!"),
                        true
                    );
                    
                    // Visual effects only (no movement debuffs)
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 100, 0, false, false));
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 20, 0, false, false));
                }
                
                // Initial burst of time distortion particles
                if (this.getWorld() instanceof ServerWorld serverWorld) {
                    serverWorld.spawnParticles(
                        ParticleTypes.WITCH,
                        entity.getX(), entity.getY() + entity.getHeight() / 2, entity.getZ(),
                        20, 0.5, 0.5, 0.5, 0.1
                    );
                    serverWorld.spawnParticles(
                        ParticleTypes.PORTAL,
                        entity.getX(), entity.getY() + entity.getHeight() / 2, entity.getZ(),
                        30, 0.5, 0.5, 0.5, 0.5
                    );
                }
            }
        }
        
        // Enhanced particles with client-server support
        for (int i = 0; i < 35; i++) { // Reduced count
            Vec3d particlePos = this.getPos().add(
                (this.random.nextDouble() - 0.5) * 14,
                this.random.nextDouble() * 10,
                (this.random.nextDouble() - 0.5) * 14
            );
            
            if (!this.getWorld().isClient) {
                ((ServerWorld)this.getWorld()).spawnParticles(
                    ParticleTypes.END_ROD,
                    particlePos.x, particlePos.y, particlePos.z,
                    1, 0, 0, 0, 0.1
                );
            } else {
                this.getWorld().addParticle(
                    ParticleTypes.END_ROD,
                    particlePos.x, particlePos.y, particlePos.z,
                    0, 0.1, 0
                );
            }
        }
        
        for (int i = 0; i < 6; i++) { // Reduced count
            Vec3d lightningPos = this.getPos().add(
                (this.random.nextDouble() - 0.5) * 10,
                this.random.nextDouble() * 6,
                (this.random.nextDouble() - 0.5) * 10
            );
            
            if (!this.getWorld().isClient) {
                ((ServerWorld)this.getWorld()).spawnParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    lightningPos.x, lightningPos.y, lightningPos.z,
                    20, 1.2, 1.2, 1.2, 0.3
                );
            } else {
                this.getWorld().addParticle(
                    ParticleTypes.ELECTRIC_SPARK,
                    lightningPos.x, lightningPos.y, lightningPos.z,
                    0, 0.3, 0
                );
            }
        }
        
        // EPIC LAYERED SOUND EFFECTS for reality-warping phase transition
        
        // 1. Primary time magic sound
        this.playSound(ModSounds.THOTH_ATTACK_TIME_BEND, 3.0f, 0.3f);
        
        // 2. Reality shattering - End portal frame fill (high pitched crack)
        this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, this.getSoundCategory(), 2.5f, 0.5f);
        
        // 3. Time stopping - Warden sonic boom (massive bass impact)
        this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.ENTITY_WARDEN_SONIC_BOOM, this.getSoundCategory(), 2.0f, 0.3f);
        
        // 4. Ancient power - Dragon death roar (epic dramatic)
        this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.ENTITY_ENDER_DRAGON_GROWL, this.getSoundCategory(), 1.5f, 0.6f);
        
        // 5. Thoth's voice
        this.playSound(ModSounds.THOTH_AMBIENT, 2.0f, 0.5f);
        
        dataTracker.set(ATTACK_COOLDOWN, 0);
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 2)); // Reduced from 3 to 2
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 1)); // Reduced duration from 300 to 200
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 200, 1, false, false)); // Changed from MAX_VALUE to 200
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 200, 1, false, false)); // Changed from MAX_VALUE to 200

        // Don't recursively call performTimeBend() here - it's already being triggered
    }
    
    private void chooseStrategicAttack() {
        LivingEntity target = this.getTarget();
        if (target == null) return;
        if (!(target instanceof PlayerEntity)) return;
        
        double distance = this.squaredDistanceTo(target);
        float healthPercent = this.getHealth() / this.getMaxHealth();
        
        // Phase 3 specific behavior: very aggressive, prioritize melee at close range
        if (hasEnteredPhase3) {
            // Super close range (≤4 blocks): Always melee
            if (distance <= 16) {
                performMeleeAttack();
            // Close range (4-12 blocks): 80% melee, 20% scroll blast for variety
            } else if (distance <= 144) {
                if (this.random.nextFloat() < 0.8f) {
                    performMeleeAttack();
                } else {
                    performScrollBlast();
                }
            // Time bend opportunity (low health emergency)
            } else if (!dataTracker.get(IS_CASTING_TIME_MAGIC) && timeBendCooldown == 0 && this.random.nextFloat() < 0.15f) {
                performTimeBend();
            // Medium range (12-32 blocks): Scroll blast
            } else if (distance <= 1024) {
                performScrollBlast();
            }
            return;
        }

        // Phase 2 specific behavior: aggressive melee preference
        if (hasEnteredPhase2) {
            // Emergency time bend
            if (healthPercent < 0.25f && !dataTracker.get(IS_CASTING_TIME_MAGIC) && timeBendCooldown == 0) {
                performTimeBend();
            // Super close range (≤6 blocks): Always melee
            } else if (distance <= 36) {
                performMeleeAttack();
            // Close range (6-12 blocks): 70% melee, 30% other attacks
            } else if (distance <= 144) {
                float roll = this.random.nextFloat();
                if (roll < 0.7f) {
                    performMeleeAttack();
                } else if (roll < 0.85f && dataTracker.get(SUMMONING_COOLDOWN) == 0) {
                    summonEntities();
                } else {
                    performScrollBlast();
                }
            // Medium range: Scroll blast or summon
            } else if (distance <= 1024) {
                if (dataTracker.get(SUMMONING_COOLDOWN) == 0 && this.random.nextFloat() < 0.4f) {
                    summonEntities();
                } else {
                    performScrollBlast();
                }
            }
            return;
        }

        // Phase 1 behavior: melee priority at close range
        // Emergency time bend
        if (healthPercent < 0.25f && !dataTracker.get(IS_CASTING_TIME_MAGIC) && timeBendCooldown == 0) {
            performTimeBend();
        // Close range (≤12 blocks): Prioritize melee
        } else if (distance <= 144) {
            performMeleeAttack();
        // Medium range (12-32 blocks): Scroll blast or summon
        } else if (distance <= 1024) {
            if (healthPercent < 0.5f && dataTracker.get(SUMMONING_COOLDOWN) == 0 && this.random.nextFloat() < 0.3f) {
                summonEntities();
            } else {
                performScrollBlast();
            }
        }
    }
    
    private void performTimePulse() {
        // Server-side only for damage and effects
        if (this.getWorld().isClient) return;

        // GLOBAL TIME PULSE ANNOUNCEMENT SOUNDS (at Thoth's position)
        // 1. Massive sonic boom - reality shattering
        this.playSound(SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 3.0f, 0.5f);

        // 2. End portal travel - dimension bending
        this.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 2.5f, 0.3f);

        // 3. Explosion rumble - temporal shockwave
        this.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, 2.0f, 0.6f);

        Box area = new Box(this.getBlockPos()).expand(20);
        List<LivingEntity> entities = this.getWorld().getNonSpectatingEntities(LivingEntity.class, area);

        for (LivingEntity entity : entities) {
            if (entity != this && entity instanceof PlayerEntity) {
                // TIME PULSE: Visual and audio feedback only (no launch - conflicts with slow-motion)
                // The slow-motion effect is already applied via velocity scaling in tick()
                
                // Add visual disorientation
                if (entity instanceof PlayerEntity player) {
                    // Nausea for visual spinning effect
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 100, 0, false, false));
                    // Brief blindness for "time stopping" effect
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 15, 0, false, false));
                }

                // Time distortion sound at player position
                entity.playSound(SoundEvents.BLOCK_BEACON_DEACTIVATE, 1.5f, 0.5f);
                entity.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.3f);

                // Purple particle beams from Thoth's hands toward player (vertical emphasis)
                // Position particles at hand/staff height (higher up on the body)
                Vec3d thothHandPos = this.getPos().add(0, this.getHeight() * 0.8, 0); // Hands are higher
                Vec3d playerHeadPos = entity.getPos().add(0, entity.getHeight() * 0.75, 0); // Target player's head/chest

                // Create 8 particles in a beam from Thoth's hands to player (reduced from 12)
                for (int i = 0; i < 8; i++) {
                    double t = i / 8.0;
                    Vec3d particlePos = thothHandPos.lerp(playerHeadPos, t);

                    // WITCH - purple magical particles (NOT blue soul fire!)
                    ((ServerWorld)this.getWorld()).spawnParticles(
                        ParticleTypes.WITCH,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0.05, 0.2, 0.05, 0.1 // Tight beam with vertical emphasis
                    );

                    // ENCHANTED_HIT for purple sparkles (every other particle)
                    if (i % 2 == 0) {
                        ((ServerWorld)this.getWorld()).spawnParticles(
                            ParticleTypes.ENCHANTED_HIT,
                            particlePos.x, particlePos.y, particlePos.z,
                            1, 0.05, 0.15, 0.05, 0.08
                        );
                    }
                }
            }
        }

        // Time distortion wave particles - expanding ring
        double waveRadius = 20.0;
        int ringParticles = 24;
        for (int i = 0; i < ringParticles; i++) {
            double angle = (i / (double)ringParticles) * Math.PI * 2.0;
            double px = this.getX() + Math.cos(angle) * waveRadius;
            double pz = this.getZ() + Math.sin(angle) * waveRadius;
            
            // Purple witch particles forming expanding ring
            ((ServerWorld)this.getWorld()).spawnParticles(
                ParticleTypes.WITCH,
                px, this.getY() + 1.0, pz,
                3, 0.2, 0.5, 0.2, 0.1
            );
            
            // Portal particles for time distortion
            if (i % 2 == 0) {
                ((ServerWorld)this.getWorld()).spawnParticles(
                    ParticleTypes.PORTAL,
                    px, this.getY() + 1.0, pz,
                    5, 0.3, 0.5, 0.3, 0.3
                );
            }
        }

        this.playSound(ModSounds.THOTH_ATTACK_TIME_BEND, 1.0f, 0.3f);
    }
    
    /**
     * Start lifting nearby players at the beginning of Time Bend animation
     */
    private void startLiftingPlayers() {
        if (this.getWorld().isClient) return;
        
        // Reset throw flag for new cast
        hasThrown = false;
        liftedPlayers.clear();
        
        // Find all players within 20 block radius
        Box area = new Box(this.getBlockPos()).expand(20);
        List<PlayerEntity> players = this.getWorld().getNonSpectatingEntities(PlayerEntity.class, area);
        
        for (PlayerEntity player : players) {
            // Track when we started lifting this player
            liftedPlayers.put(player.getUuid(), this.age);
            
            // Play lift sound at player position
            player.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
        }
    }
    
    /**
     * Apply gradual upward lift to tracked players during Time Bend animation
     */
    private void applyGradualLift() {
        if (this.getWorld().isClient) return;
        
        for (UUID playerUUID : liftedPlayers.keySet()) {
            Entity entity = ((ServerWorld)this.getWorld()).getEntity(playerUUID);
            if (entity instanceof PlayerEntity player) {
                // Calculate lift progress (0.0 to 1.0 over ~2.6 seconds)
                int liftStartTick = liftedPlayers.get(playerUUID);
                int ticksLifting = this.age - liftStartTick;
                float liftProgress = Math.min(ticksLifting / 52.0f, 1.0f); // 52 ticks = 2.6 seconds
                
                // Gradual upward velocity - starts slow, gets faster
                float upwardForce = 0.08f + (liftProgress * 0.12f); // 0.08 to 0.2
                
                // Apply upward lift
                Vec3d currentVel = player.getVelocity();
                player.setVelocity(currentVel.x * 0.8, upwardForce, currentVel.z * 0.8); // Slow horizontal movement
                player.velocityModified = true;
                
                // Spawn purple lifting particles around player
                if (this.age % 3 == 0) { // Every 3 ticks
                    ((ServerWorld)this.getWorld()).spawnParticles(
                        ParticleTypes.WITCH,
                        player.getX(), player.getY() + 0.5, player.getZ(),
                        3, 0.3, 0.3, 0.3, 0.05
                    );
                    ((ServerWorld)this.getWorld()).spawnParticles(
                        ParticleTypes.PORTAL,
                        player.getX(), player.getY() + 0.5, player.getZ(),
                        2, 0.2, 0.2, 0.2, 0.1
                    );
                }
            }
        }
    }
    
    /**
     * Throw all lifted players away from Thoth at 2.6 seconds into animation
     */
    private void throwLiftedPlayers() {
        if (this.getWorld().isClient || hasThrown) return;
        
        hasThrown = true;
        
        for (UUID playerUUID : liftedPlayers.keySet()) {
            Entity entity = ((ServerWorld)this.getWorld()).getEntity(playerUUID);
            if (entity instanceof PlayerEntity player) {
                // Calculate direction away from Thoth
                Vec3d directionAway = player.getPos().subtract(this.getPos()).normalize();
                
                // Throw with strong horizontal force and some upward force
                double throwStrength = 2.5; // Strong throw
                Vec3d throwVelocity = new Vec3d(
                    directionAway.x * throwStrength,
                    0.8, // Upward component
                    directionAway.z * throwStrength
                );
                
                player.setVelocity(throwVelocity);
                player.velocityModified = true;
                
                // Damage player from the throw
                player.damage(this.getDamageSources().magic(), 8.0f);
                
                // Camera shake from impact
                player.hurtTime = 15;
                player.maxHurtTime = 15;
                
                // Explosion particles at throw point
                ((ServerWorld)this.getWorld()).spawnParticles(
                    ParticleTypes.EXPLOSION,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    1, 0, 0, 0, 0
                );
                
                // Purple magic burst
                ((ServerWorld)this.getWorld()).spawnParticles(
                    ParticleTypes.WITCH,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    20, 0.5, 0.5, 0.5, 0.3
                );
                
                // Throw sound
                player.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, 1.5f, 1.2f);
                player.playSound(SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.5f);
            }
        }
        
        // Clear lifted players after throwing
        liftedPlayers.clear();
    }
    
    private void performTimePunch() {
        if (!this.getWorld().isClient) {
            // Calculate punch area in front of Thoth
            Vec3d forward = this.getRotationVec(1.0f);
            Vec3d origin = this.getEyePos().add(forward.multiply(0.5));
            Vec3d target = origin.add(forward.multiply(5.0)); // Increased reach to 5 blocks

            // Improved hitbox detection - use spherical area check instead of box
            Box searchArea = new Box(this.getBlockPos()).expand(8); // 8 block search radius
            List<LivingEntity> nearbyEntities = this.getWorld().getNonSpectatingEntities(LivingEntity.class, searchArea);

            for (LivingEntity entity : nearbyEntities) {
                if (entity != this && entity instanceof PlayerEntity) {
                    // Check if entity is in front of Thoth (cone check)
                    Vec3d toEntity = entity.getPos().subtract(this.getPos()).normalize();
                    double dot = forward.dotProduct(toEntity);

                    // Distance check
                    double distanceSq = this.squaredDistanceTo(entity);
                    double distance = Math.sqrt(distanceSq);

                    // If in front (dot > 0.6 = ~53 degree cone) and within 6 blocks
                    if (dot > 0.6 && distance <= 6.0) {
                        // Apply punch damage with falloff
                        float damage = 20.0f;
                        if (distance > 3.0) {
                            // Damage falloff beyond 3 blocks
                            damage = Math.max(12.0f, 20.0f - (float)(distance - 3.0) * 2.5f);
                        }

                        entity.damage(this.getDamageSources().mobAttack(this), damage);

                        // Massive knockback with upward component
                        Vec3d knockback = forward.multiply(2.5).add(0, 0.8, 0); // Increased upward knockback
                        entity.addVelocity(knockback.x, knockback.y, knockback.z);
                        entity.velocityModified = true;

                        // Stun effect only (removed Slowness)
                        entity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 60, 0)); // Reduced duration 80->60, level 1->0

                        // Play impact sound
                        this.getWorld().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                            SoundEvents.ENTITY_WARDEN_ATTACK_IMPACT, this.getSoundCategory(), 1.5f, 0.5f);
                    }
                }
            }

            // Purple time magic punch trail particles - REDUCED
            for (int i = 0; i < 6; i++) { // Reduced from 10 to 6
                double t = i / 6.0;
                double x = origin.x + (target.x - origin.x) * t;
                double y = origin.y + (target.y - origin.y) * t;
                double z = origin.z + (target.z - origin.z) * t;

                // Purple witch particles for magical punch trail
                ((ServerWorld)this.getWorld()).spawnParticles(
                    ParticleTypes.WITCH,
                    x, y, z,
                    2, 0.1, 0.3, 0.1, 0.1 // Reduced from 3 to 2
                );

                // Purple enchanted hit sparkles (every other particle)
                if (i % 2 == 0) {
                    ((ServerWorld)this.getWorld()).spawnParticles(
                        ParticleTypes.ENCHANTED_HIT,
                        x, y, z,
                        1, 0.1, 0.2, 0.1, 0.08 // Reduced from 2 to 1
                    );
                }

                // Dragon breath for purple glow (only every 3rd particle)
                if (i % 3 == 0) {
                    ((ServerWorld)this.getWorld()).spawnParticles(
                        ParticleTypes.DRAGON_BREATH,
                        x, y, z,
                        1, 0.1, 0.2, 0.1, 0.05
                    );
                }
            }
        }

        // Play punch swoosh sound
        this.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 2.0f, 0.5f);
    }
    
    /* ---------- ATTACK METHODS ---------- */
    public void performScrollBlast() {
        // Don't interrupt ongoing animations or if on cooldown
        if (dataTracker.get(ATTACK_COOLDOWN) > 0 || animationLocked || attackAnimationTicks > 0) return;

        dataTracker.set(ATTACK_STATE, ATTACK_SCROLL_BLAST);
        dataTracker.set(ATTACK_COOLDOWN, MAX_ATTACK_COOLDOWN);
        attackAnimationTicks = ATTACK_2_ANIMATION_DURATION;

        // IMPROVED: Damage and effects now applied via handleAnimationFrameEffects()
        // at frame 90 (when scroll releases energy) for perfect synchronization with animation
        
        // 25% chance to laugh after casting scroll blast (showing off power)
        if (this.random.nextFloat() < 0.25f) {
            playRandomLaugh(0.9f, 0.8f + this.random.nextFloat() * 0.3f);
        }
    }
    
    public void performTimeBend() {
        // Don't interrupt ongoing animations or if on cooldown
        if (dataTracker.get(ATTACK_COOLDOWN) > 0 || animationLocked || attackAnimationTicks > 0) return;
        
        dataTracker.set(ATTACK_STATE, ATTACK_TIME_BEND);
        dataTracker.set(ATTACK_COOLDOWN, MAX_ATTACK_COOLDOWN * 2);
        dataTracker.set(IS_CASTING_TIME_MAGIC, true);
        timeMagicTicks = 0;
        attackAnimationTicks = TIME_BEND_ANIMATION_DURATION; // Longer for time magic
        timeBendCooldown = 1200; // 60 seconds cooldown (increased from 30s)
        
        // Time pulse will happen at 2.75 seconds via handleAnimationFrameEffects()

        // LAYERED SOUND DESIGN for epic time-bending effect

        // 1. Primary time magic sound (custom Thoth sound)
        this.playSound(ModSounds.THOTH_ATTACK_TIME_BEND, 2.5f, 0.6f);

        // 2. Magic attack - mystical time energy
        this.playSound(ModSounds.THOTH_MAGIC_ATTACK, 2.0f, 0.9f);

        // 3. Deep reality-warping bass (end portal open - very low, ominous)
        this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.BLOCK_END_PORTAL_SPAWN, this.getSoundCategory(), 1.8f, 0.4f);

        // 4. Mystical energy buildup (enchantment table)
        this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, this.getSoundCategory(), 2.0f, 0.3f);

        // 5. Ancient power awakening (warden emergence - deep rumble)
        this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.ENTITY_WARDEN_EMERGE, this.getSoundCategory(), 1.2f, 0.6f);
        
        // 30% chance to laugh after time bend (enjoying temporal manipulation)
        if (this.random.nextFloat() < 0.3f) {
            playRandomLaugh(1.0f, 0.75f + this.random.nextFloat() * 0.3f);
        }
    }
    
    public void summonEntities() {
        // Don't interrupt ongoing animations or if on cooldown
        if (dataTracker.get(SUMMONING_COOLDOWN) > 0 || animationLocked || attackAnimationTicks > 0) return;

        dataTracker.set(ATTACK_STATE, ATTACK_ENTITY_SUMMON);
        dataTracker.set(SUMMONING_COOLDOWN, MAX_SUMMONING_COOLDOWN);
        attackAnimationTicks = SUMMON_ANIMATION_DURATION;

        // Play summoning sound (actual summoning happens at animation frame 40 via handleAnimationFrameEffects)
        this.playSound(ModSounds.THOTH_SUMMON, 2.0f, 1.0f);
    }
    
    public void performMeleeAttack() {
        // Don't interrupt ongoing animations or if on cooldown
        if (dataTracker.get(ATTACK_COOLDOWN) > 0 || animationLocked || attackAnimationTicks > 0) return;

        dataTracker.set(ATTACK_STATE, ATTACK_MELEE);
        dataTracker.set(ATTACK_COOLDOWN, MAX_ATTACK_COOLDOWN); // Same cooldown as other attacks for better pacing
        attackAnimationTicks = ATTACK_1_ANIMATION_DURATION;

        // Play wind gust at start of melee attack
        this.playSound(ModSounds.THOTH_WIND_GUST, 2.0f, 0.8f);

        // Play yell sound when melee attack begins (server-side for proper sync, lower pitch)
        if (!this.getWorld().isClient) {
            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSounds.THOTH_YELL, this.getSoundCategory(), 1.5f, 0.85f);
        }

        // IMPROVED: Damage and effects now applied via handleAnimationFrameEffects()
        // at frame 23 (peak of melee swing) for perfect synchronization with animation
        
        // 20% chance to laugh after starting melee attack (confident/mocking)
        if (this.random.nextFloat() < 0.2f) {
            playRandomLaugh(0.8f, 0.85f + this.random.nextFloat() * 0.3f);
        }
    }
    
    /* ---------- CUSTOM GOALS ---------- */
    public static class ThothMagicAttackGoal extends Goal {
        private final ThothEntity thoth;
        private int attackTimer = 0;
        private LivingEntity target;

        public ThothMagicAttackGoal(ThothEntity thoth) {
            this.thoth = thoth;
            // CRITICAL FIX: Control MOVE and LOOK to prevent other goals from interfering during attacks
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }
        
        @Override
        public boolean canStart() {
            this.target = thoth.getTarget();
            return thoth.isAlive() &&
                   target != null &&
                   thoth.dataTracker.get(ATTACK_COOLDOWN) == 0 &&
                   !thoth.animationLocked && // Don't start if animation is locked
                   thoth.attackAnimationTicks == 0 && // Don't interrupt animations
                   thoth.squaredDistanceTo(target) < 4096; // Increased from 256 (16 blocks) to 4096 (64 blocks)
        }

        @Override
        public boolean shouldContinue() {
            return target != null && target.isAlive() &&
                   thoth.squaredDistanceTo(target) < 5184; // Increased from 400 (20 blocks) to 5184 (72 blocks)
        }
        
        @Override
        public void start() {
            attackTimer = 0;
            thoth.setNoGravity(false); // Ensure grounded
        }
        
        @Override
        public void tick() {
            if (target != null && target.isAlive()) {
                thoth.getLookControl().lookAt(target, 30.0f, 30.0f);

                // CRITICAL FIX: Stop ALL movement during attack animations
                if (thoth.attackAnimationTicks > 0 || thoth.animationLocked) {
                    // Force stop navigation during attacks to prevent walking animation from playing
                    thoth.getNavigation().stop();

                    // Aggressive deceleration to prevent animation flickering
                    Vec3d currentVel = thoth.getVelocity();
                    double horizontalSpeed = currentVel.horizontalLengthSquared();

                    if (horizontalSpeed > 0.001) {
                        // Quick deceleration while still moving
                        thoth.setVelocity(currentVel.multiply(0.5, 1.0, 0.5));
                    } else {
                        // Completely stop when velocity drops below animation threshold
                        thoth.setVelocity(0, currentVel.y, 0);
                    }
                } else {
                    // Dynamic movement based on distance
                    double distanceSq = thoth.squaredDistanceTo(target);
                    
                    if (distanceSq > 256) { // More than 16 blocks - move closer
                        thoth.getNavigation().startMovingTo(target, 1.2);
                    } else if (distanceSq > 64) { // 8-16 blocks - approach slowly
                        thoth.getNavigation().startMovingTo(target, 0.9);
                    } else if (distanceSq > 4) { // 2-8 blocks - strafe/circle around player
                        // Circle around the player to avoid being cornered
                        if (thoth.age % 15 == 0) { // Change direction every 0.75s (increased from 1s)
                            // Calculate a position to the side of the player
                            double angle = thoth.random.nextDouble() * Math.PI * 2;
                            double radius = 4.5; // Circle at 4.5 blocks (increased from 4)
                            double offsetX = target.getX() + Math.cos(angle) * radius;
                            double offsetZ = target.getZ() + Math.sin(angle) * radius;
                            thoth.getNavigation().startMovingTo(offsetX, target.getY(), offsetZ, 1.15); // Faster movement (1.15 from 1.1)
                        }
                    } else {
                        // At very close range (<2 blocks) - constantly strafe side to side
                        if (thoth.age % 10 == 0) { // Quick side steps every 0.5s (increased from 0.75s)
                            double angle = thoth.random.nextDouble() * Math.PI * 2;
                            double offsetX = target.getX() + Math.cos(angle) * 3.0; // Increased from 2.5
                            double offsetZ = target.getZ() + Math.sin(angle) * 3.0;
                            thoth.getNavigation().startMovingTo(offsetX, target.getY(), offsetZ, 1.1); // Faster (1.1 from 1.0)
                        }
                    }
                }

                // 80 ticks (4 seconds) between attacks to give more time for movement
                // Balances attacking with dynamic movement behavior
                if (++attackTimer >= 80 && thoth.attackAnimationTicks == 0) {
                    thoth.chooseStrategicAttack();
                    attackTimer = 0;
                }
            }
        }
        
        @Override
        public void stop() {
            target = null;
            attackTimer = 0;
            thoth.getNavigation().stop();
        }
    }
    
    public static class ThothSummonEntitiesGoal extends Goal {
        private final ThothEntity thoth;
        
        public ThothSummonEntitiesGoal(ThothEntity thoth) {
            this.thoth = thoth;
            this.setControls(EnumSet.of(Control.MOVE)); // Control movement during summoning
        }
        
        @Override
        public boolean canStart() {
            return thoth.getTarget() != null &&
                   thoth.dataTracker.get(SUMMONING_COOLDOWN) == 0 &&
                   !thoth.animationLocked && // Don't start if animation is locked
                   thoth.attackAnimationTicks == 0 &&
                   thoth.getHealth() < thoth.getMaxHealth() * 0.5f;
        }
        
        @Override
        public boolean shouldContinue() {
            // Continue until animation completes
            return thoth.attackAnimationTicks > 0 && thoth.dataTracker.get(ATTACK_STATE) == ATTACK_ENTITY_SUMMON;
        }
        
        @Override
        public void start() {
            thoth.summonEntities();
            thoth.getNavigation().stop(); // Stop movement immediately
        }
        
        @Override
        public void tick() {
            // Keep Thoth stationary during summoning
            thoth.getNavigation().stop();
            thoth.setVelocity(0, thoth.getVelocity().y, 0);
        }
        
        @Override
        public void stop() {
            // Clean up when summoning ends
            thoth.getNavigation().stop();
        }
    }
    
    public static class ThothTimeBendGoal extends Goal {
        private final ThothEntity thoth;
        
        public ThothTimeBendGoal(ThothEntity thoth) {
            this.thoth = thoth;
        }
        
        @Override
        public boolean canStart() {
            return thoth.getTarget() != null &&
                   thoth.dataTracker.get(ATTACK_COOLDOWN) == 0 &&
                   !thoth.animationLocked && // Don't start if animation is locked
                   thoth.attackAnimationTicks == 0 &&
                   thoth.getHealth() < thoth.getMaxHealth() * 0.3f &&
                   !thoth.dataTracker.get(IS_CASTING_TIME_MAGIC) &&
                   thoth.timeBendCooldown == 0;
        }
        
        @Override
        public void start() {
            thoth.performTimeBend();
        }
    }
    
    public static class ThothRetreatGoal extends Goal {
        private final ThothEntity thoth;
        
        public ThothRetreatGoal(ThothEntity thoth) {
            this.thoth = thoth;
            this.setControls(EnumSet.of(Control.MOVE));
        }
        
        @Override
        public boolean canStart() {
            return thoth.retreatTicks > 0 && thoth.getTarget() != null;
        }
        
        @Override
        public void start() {
            LivingEntity target = thoth.getTarget();
            if (target != null) {
                // Calculate position away from target
                Vec3d direction = thoth.getPos().subtract(target.getPos()).normalize();
                Vec3d dest = thoth.getPos().add(direction.multiply(8.0));
                thoth.getNavigation().startMovingTo(dest.x, dest.y, dest.z, 1.3);
            }
        }
        
        @Override
        public boolean shouldContinue() {
            return thoth.retreatTicks > 0 && !thoth.getNavigation().isIdle();
        }
        
        @Override
        public void stop() {
            thoth.retreatTicks = 0;
        }
    }
    
    /* ---------- GECKOLIB ANIMATIONS ---------- */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Use 8 tick transition time for smooth animation blending and to prevent flickering
        controllers.add(new AnimationController<>(this, "controller", 8, this::predicate));
    }
    
    private <T extends GeoEntity> PlayState predicate(AnimationState<T> state) {
        int attackState = this.dataTracker.get(ATTACK_STATE);
        boolean isInCombat = this.dataTracker.get(IS_IN_COMBAT);
        boolean isCastingTime = this.dataTracker.get(IS_CASTING_TIME_MAGIC);
        boolean hasBeenInCombat = this.dataTracker.get(HAS_BEEN_IN_COMBAT);
        boolean isReading = this.dataTracker.get(IS_READING);

        // Priority 0: Death animation (absolute highest priority) - dramatic vanishing
        if (isPlayingDeathAnimation) {
            // Play entity_spawn animation once (reverse spawn = death/vanishing effect)
            // Then hold on final frame for dramatic fade-out
            state.getController().setAnimation(RawAnimation.begin().then("animation.thoth.entity_spawn", Animation.LoopType.HOLD_ON_LAST_FRAME));
            return PlayState.CONTINUE;
        }
        
        // Priority 1: Spawn animation (highest priority) - let it complete fully
        if (spawnTransitionTicks > 0 && !hasSpawned) {
            state.getController().setAnimation(RawAnimation.begin().then("animation.thoth.entity_spawn", Animation.LoopType.PLAY_ONCE));
            return PlayState.CONTINUE;
        }

        // Priority 2: Attack animations (when actively attacking) - let each complete fully
        // Use the lastAttackState when transitioning to prevent animation glitching
        if (attackAnimationTicks > 0 || animationLocked) {
            int effectiveAttackState = (attackState != ATTACK_NONE) ? attackState : lastAttackState;

            // CRITICAL FIX: If effectiveAttackState is ATTACK_NONE, cancel the attack immediately
            // This prevents the "freeze and explode" bug where entity is locked but no animation plays
            if (effectiveAttackState == ATTACK_NONE) {
                attackAnimationTicks = 0;
                animationLocked = false;
                // Fall through to normal animation logic below
            } else {
                switch (effectiveAttackState) {
                    case ATTACK_MELEE:
                        // Melee attack - uses attack_1 animation
                        state.getController().setAnimation(RawAnimation.begin().then("animation.thoth.attack_1", Animation.LoopType.PLAY_ONCE));
                        return PlayState.CONTINUE;

                    case ATTACK_SCROLL_BLAST:
                        // Scroll reading blast attack - uses attack_2 animation
                        state.getController().setAnimation(RawAnimation.begin().then("animation.thoth.attack_2", Animation.LoopType.PLAY_ONCE));
                        return PlayState.CONTINUE;

                    case ATTACK_TIME_BEND:
                        // Time magic - uses time_bend animation (play once)
                        state.getController().setAnimation(RawAnimation.begin().then("animation.thoth.time_bend", Animation.LoopType.PLAY_ONCE));
                        return PlayState.CONTINUE;

                    case ATTACK_ENTITY_SUMMON:
                        // Entity summoning - uses entity_spawn animation (dramatic summoning)
                        state.getController().setAnimation(RawAnimation.begin().then("animation.thoth.entity_spawn", Animation.LoopType.PLAY_ONCE));
                        return PlayState.CONTINUE;

                    default:
                        // Unknown attack state - cancel animation lock to prevent freezing
                        attackAnimationTicks = 0;
                        animationLocked = false;
                        break;
                }
            }
        }
        
        // Priority 3: Reading behavior (when peaceful and reading tome)
        if (isReading && !hasBeenInCombat && !isInCombat) {
            // Peaceful reading - floating with tome (idle animation)
            state.getController().setAnimation(RawAnimation.begin().then("animation.thoth.idle", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }
        
        // Priority 5: Movement animations
        // CRITICAL FIX: Only play walking if NOT attacking AND NOT locked in animation
        // Use velocity-based movement detection to prevent animation flickering
        double velocitySquared = this.getVelocity().horizontalLengthSquared();
        boolean isActuallyMoving = velocitySquared > 0.001; // Low threshold to catch slow movement (circling/strafing)

        if (isActuallyMoving && attackAnimationTicks == 0 && !animationLocked && attackState == ATTACK_NONE) {
            // Walking/floating movement
            state.getController().setAnimation(RawAnimation.begin().then("animation.thoth.walking", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }
        
        // Priority 6: Idle animations (default states)
        if (hasBeenInCombat || isInCombat) {
            // Combat idle - standing ready for battle
            state.getController().setAnimation(RawAnimation.begin().then("animation.thoth.idle_standing", Animation.LoopType.LOOP));
        } else {
            // Peaceful idle - floating and reading tome
            state.getController().setAnimation(RawAnimation.begin().then("animation.thoth.idle", Animation.LoopType.LOOP));
        }
        
        return PlayState.CONTINUE;
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
    
    /* ---------- DATA TRACKER ---------- */
    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(ATTACK_STATE, ATTACK_NONE);
        this.dataTracker.startTracking(ATTACK_COOLDOWN, 0);
        this.dataTracker.startTracking(IS_IN_COMBAT, false);
        this.dataTracker.startTracking(IS_READING, false);
        this.dataTracker.startTracking(IS_CASTING_TIME_MAGIC, false);
        this.dataTracker.startTracking(SUMMONING_COOLDOWN, 0);
        this.dataTracker.startTracking(HAS_BEEN_IN_COMBAT, false);
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        if (ATTACK_STATE.equals(data)) {
            int attackState = this.dataTracker.get(ATTACK_STATE);
            if (this.getWorld().isClient) {
                switch (attackState) {
                    case ATTACK_MELEE:
                        this.attackAnimationTicks = ATTACK_1_ANIMATION_DURATION;
                        this.animationLocked = true;
                        break;
                    case ATTACK_SCROLL_BLAST:
                        this.attackAnimationTicks = ATTACK_2_ANIMATION_DURATION;
                        this.animationLocked = true;
                        break;
                    case ATTACK_TIME_BEND:
                        this.attackAnimationTicks = TIME_BEND_ANIMATION_DURATION;
                        this.animationLocked = true;
                        break;
                    case ATTACK_ENTITY_SUMMON:
                        this.attackAnimationTicks = SUMMON_ANIMATION_DURATION;
                        this.animationLocked = true;
                        break;
                    case ATTACK_NONE:
                        this.attackAnimationTicks = 0;
                        this.animationLocked = false;
                        this.lastAttackState = ATTACK_NONE;
                        break;
                }
            }
        }
        super.onTrackedDataSet(data);
    }
    
    /* ---------- NBT ---------- */
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("AttackState", this.dataTracker.get(ATTACK_STATE));
        nbt.putInt("AttackCooldown", this.dataTracker.get(ATTACK_COOLDOWN));
        nbt.putBoolean("IsInCombat", this.dataTracker.get(IS_IN_COMBAT));
        nbt.putBoolean("IsReading", this.dataTracker.get(IS_READING));
        nbt.putBoolean("IsCastingTimeMagic", this.dataTracker.get(IS_CASTING_TIME_MAGIC));
        nbt.putInt("SummoningCooldown", this.dataTracker.get(SUMMONING_COOLDOWN));
        nbt.putBoolean("HasSpawned", this.hasSpawned);
        nbt.putInt("SpawnTransitionTicks", this.spawnTransitionTicks);
        nbt.putBoolean("HasEnteredPhase2", this.hasEnteredPhase2);
        nbt.putBoolean("HasEnteredPhase3", this.hasEnteredPhase3);
        nbt.putInt("AttackAnimationTicks", this.attackAnimationTicks);
        nbt.putInt("CombatTimeout", this.combatTimeout);
        nbt.putBoolean("HasBeenInCombat", this.dataTracker.get(HAS_BEEN_IN_COMBAT));
    }
    
    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.dataTracker.set(ATTACK_STATE, nbt.getInt("AttackState"));
        this.dataTracker.set(ATTACK_COOLDOWN, nbt.getInt("AttackCooldown"));
        this.dataTracker.set(IS_IN_COMBAT, nbt.getBoolean("IsInCombat"));
        this.dataTracker.set(IS_READING, nbt.getBoolean("IsReading"));
        this.dataTracker.set(IS_CASTING_TIME_MAGIC, nbt.getBoolean("IsCastingTimeMagic"));
        this.dataTracker.set(SUMMONING_COOLDOWN, nbt.getInt("SummoningCooldown"));
        this.hasSpawned = nbt.getBoolean("HasSpawned");
        this.spawnTransitionTicks = nbt.getInt("SpawnTransitionTicks");
        this.hasEnteredPhase2 = nbt.getBoolean("HasEnteredPhase2");
        this.hasEnteredPhase3 = nbt.getBoolean("HasEnteredPhase3");
        this.attackAnimationTicks = nbt.getInt("AttackAnimationTicks");
        this.combatTimeout = nbt.getInt("CombatTimeout");
        this.dataTracker.set(HAS_BEEN_IN_COMBAT, nbt.getBoolean("HasBeenInCombat"));
    }
    
    /* ---------- GETTERS ---------- */
    public boolean isInCombat() { return dataTracker.get(IS_IN_COMBAT); }
    public boolean isReading() { return dataTracker.get(IS_READING); }
    public boolean isCastingTimeMagic() { return dataTracker.get(IS_CASTING_TIME_MAGIC); }
    public int getAttackState() { return dataTracker.get(ATTACK_STATE); }
    public int getSpawnTransitionTicks() { return spawnTransitionTicks; }
    public boolean isInSpawnTransition() { return spawnTransitionTicks > 0; }
    
    // Attack state checkers
    public boolean isScrollBlastAttack() { return dataTracker.get(ATTACK_STATE) == ATTACK_SCROLL_BLAST; }
    public boolean isTimeBendAttack() { return dataTracker.get(ATTACK_STATE) == ATTACK_TIME_BEND; }
    public boolean isEntitySummonAttack() { return dataTracker.get(ATTACK_STATE) == ATTACK_ENTITY_SUMMON; }
    public boolean isMeleeAttack() { return dataTracker.get(ATTACK_STATE) == ATTACK_MELEE; }
    public boolean isAttackingWithMagic() {
        int state = dataTracker.get(ATTACK_STATE);
        return state == ATTACK_SCROLL_BLAST || state == ATTACK_TIME_BEND;
    }
    
    /* ---------- SOUNDS ---------- */
    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.THOTH_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        // Randomly choose between hurt variants for variety
        return this.random.nextBoolean() ? ModSounds.THOTH_HURT : ModSounds.THOTH_HIT_2;
    }

    /**
     * Play a random laugh sound from the 4 laugh variants
     * @param volume Volume multiplier
     * @param pitch Pitch multiplier
     */
    private void playRandomLaugh(float volume, float pitch) {
        SoundEvent[] laughs = {
            ModSounds.THOTH_LAUGH_1,
            ModSounds.THOTH_LAUGH_2,
            ModSounds.THOTH_LAUGH_3,
            ModSounds.THOTH_LAUGH_4
        };
        
        SoundEvent randomLaugh = laughs[this.random.nextInt(laughs.length)];
        this.playSound(randomLaugh, volume, pitch);
    }
    
    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.THOTH_DEATH;
    }
    
    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        // Thoth walks on the ground - use heavy, imposing footsteps
        // Wither skeleton steps for ancient, powerful sound
        this.playSound(SoundEvents.ENTITY_WITHER_SKELETON_STEP, 0.15f, 0.8f);
    }
    
    /* ---------- BOSS BEHAVIOR ---------- */
    @Override
    public boolean damage(DamageSource source, float amount) {
        // During time magic, reduce damage but not as extremely
        if (dataTracker.get(IS_CASTING_TIME_MAGIC)) {
            amount *= 0.7f; // 30% damage reduction during time magic
        }

        // Phase 3 damage reduction - balanced
        float healthPercent = this.getHealth() / this.getMaxHealth();
        if (healthPercent < 0.25f) {
            amount *= 0.85f; // 15% damage reduction in phase 3
        }

        // FIXED: Call super.damage() which handles knockback and hit reactions
        boolean damageTaken = super.damage(source, amount);

        if (!damageTaken) return false;

        // Play hurt sound and particles for visual feedback
        if (!this.getWorld().isClient) {
            // Spawn hurt particles for visual feedback
            ((ServerWorld)this.getWorld()).spawnParticles(
                ParticleTypes.DAMAGE_INDICATOR,
                this.getX(), this.getY() + this.getHeight() / 2, this.getZ(),
                5, 0.3, 0.3, 0.3, 0.1
            );

            // Add blood particles for more impact
            ((ServerWorld)this.getWorld()).spawnParticles(
                ParticleTypes.LARGE_SMOKE,
                this.getX(), this.getY() + this.getHeight() / 2, this.getZ(),
                3, 0.2, 0.3, 0.2, 0.05
            );
            
            // 30% chance to play breath or laugh when hit (personality)
            if (this.random.nextFloat() < 0.3f) {
                if (this.random.nextBoolean()) {
                    // Play breath sound (exertion/impact)
                    this.playSound(ModSounds.THOTH_BREATH_1, 1.2f, 0.9f + this.random.nextFloat() * 0.2f);
                } else {
                    // Play random laugh (mocking/confident)
                    playRandomLaugh(1.0f, 0.9f + this.random.nextFloat() * 0.2f);
                }
            }
        }

        // If attacked by a player (including ranged), ensure we target them
        if (source.getAttacker() instanceof PlayerEntity player) {
            this.setTarget(player);
            // Enter combat state immediately
            combatTimeout = MAX_COMBAT_TIMEOUT;

            // Play "Intruders!" sound only the first time entering combat
            boolean wasInCombatBefore = dataTracker.get(HAS_BEEN_IN_COMBAT);
            if (!wasInCombatBefore && !this.getWorld().isClient) {
                this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.THOTH_INTRUDERS, this.getSoundCategory(), 2.0f, 1.0f);
            }

            dataTracker.set(IS_IN_COMBAT, true);
            dataTracker.set(HAS_BEEN_IN_COMBAT, true);
        }

        // Set hurt time for red flash effect (standard Minecraft hit feedback)
        // This makes Thoth flash red when hit, providing clear visual feedback
        this.hurtTime = 10; // Standard hit flash duration

        // Prevent healing above max health
        if (this.getHealth() > this.getMaxHealth()) {
            this.setHealth(this.getMaxHealth());
        }

        // TACTICAL REACTIVITY: Make Thoth highly responsive when hit
        if (!this.getWorld().isClient && !animationLocked && source.getAttacker() instanceof LivingEntity attacker) {
            double distanceToAttacker = this.squaredDistanceTo(attacker);
            int attackCooldown = this.dataTracker.get(ATTACK_COOLDOWN);
            
            // Close range (within 12 blocks) - balanced response
            if (distanceToAttacker <= 144 && attackCooldown == 0) {
                // 30% chance to immediately counterattack with melee (reduced from 60%)
                if (this.random.nextFloat() < 0.3f) {
                    // Instant melee counterattack
                    this.dataTracker.set(ATTACK_STATE, ATTACK_MELEE);
                    this.dataTracker.set(ATTACK_COOLDOWN, MAX_ATTACK_COOLDOWN);
                    this.attackAnimationTicks = ATTACK_1_ANIMATION_DURATION;
                    this.animationLocked = true;
                    this.lastAttackState = ATTACK_MELEE;

                    // Play yell sound when counterattack starts (lower pitch for aggressive tone)
                    this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                        ModSounds.THOTH_YELL, this.getSoundCategory(), 1.5f, 0.85f);

                    // Play attack sound
                    this.playSound(ModSounds.THOTH_ATTACK_MELEE, 1.5f, 1.0f);
                    return true;
                }
            }
            
            // Prioritize movement over attacking
            float teleportChance = 0.15f;
            if (this.getHealth() < this.getMaxHealth() * 0.3f) {
                teleportChance = 0.25f; // Low health panic
            }
            
            if (this.random.nextFloat() < teleportChance) {
                performEvasiveTeleport();
            } else if (this.random.nextFloat() < 0.6f) {
                // 60% chance to strafe/circle (increased from 40%)
                // Force movement by setting retreat ticks (will trigger circling behavior)
                this.retreatTicks = 50; // 2.5 seconds of active movement (increased from 2s)
            } else {
                // Otherwise, reduce attack cooldown slightly
                if (attackCooldown > 15) {
                    this.dataTracker.set(ATTACK_COOLDOWN, 15); // Attack again in 0.75s (increased from 0.5s)
                }
            }
        }

        return true;
    }

    private void performEvasiveTeleport() {
        if (this.getWorld().isClient) return;
        
        double oldX = this.getX();
        double oldY = this.getY();
        double oldZ = this.getZ();
        
        // Try to find a valid spot
        for (int i = 0; i < 16; i++) {
            double targetX = this.getX() + (this.random.nextDouble() - 0.5) * 32.0;
            double targetY = MathHelper.clamp(this.getY() + (this.random.nextInt(16) - 8), this.getWorld().getBottomY(), (this.getWorld().getTopY() - 1));
            double targetZ = this.getZ() + (this.random.nextDouble() - 0.5) * 32.0;

            if (this.teleport(targetX, targetY, targetZ, true)) {
                // Success! Spawn particles at old location
                ((ServerWorld)this.getWorld()).spawnParticles(
                    ParticleTypes.REVERSE_PORTAL,
                    oldX, oldY + 1.0, oldZ,
                    20, 0.5, 1.0, 0.5, 0.1
                );
                
                // Spawn particles at new location
                ((ServerWorld)this.getWorld()).spawnParticles(
                    ParticleTypes.PORTAL,
                    this.getX(), this.getY() + 1.0, this.getZ(),
                    20, 0.5, 1.0, 0.5, 0.1
                );
                
                // Play sound
                this.getWorld().playSound(null, oldX, oldY, oldZ, SoundEvents.ENTITY_ENDERMAN_TELEPORT, this.getSoundCategory(), 1.0f, 1.0f);
                this.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                
                // Reset pathfinding
                this.getNavigation().stop();
                
                break; // Done
            }
        }
    }

    @Override
    public void addVelocity(double deltaX, double deltaY, double deltaZ) {
        // NO KNOCKBACK during death animation - must stay perfectly still
        if (isPlayingDeathAnimation) {
            return;
        }
        
        // BEST PRACTICE: Boss should have visible knockback but remain grounded
        // Players expect to see impact when hitting a boss

        // Vertical knockback: allow upward movement but cap to prevent launch
        if (deltaY > 0.8) {
            deltaY = 0.8; // Increased from 0.6 to 0.8 for more visible hit reaction
        }

        // Horizontal knockback: 70% retained for visible pushback
        // Boss still feels heavy but player sees clear impact
        deltaX *= 0.7; // Increased from 0.5 to 0.7 (70% knockback)
        deltaZ *= 0.7;

        super.addVelocity(deltaX, deltaY, deltaZ);
    }
    
    @Override
    public void onDeath(DamageSource source) {
        // Start death animation sequence instead of dying immediately
        if (!isPlayingDeathAnimation && !this.getWorld().isClient) {
            isPlayingDeathAnimation = true;
            deathAnimationTicks = DEATH_ANIMATION_DURATION;
            
            // Prevent actual death - keep at 1 HP to avoid death triggers
            this.setHealth(1.0f);
            
            // Make completely invulnerable and immobile during death animation
            this.setInvulnerable(true);
            this.setNoGravity(true); // Prevent falling
            this.setAiDisabled(true); // Disable all AI
            
            // CRITICAL: Clear all attack state to prevent attack animations from playing
            this.dataTracker.set(ATTACK_STATE, ATTACK_NONE);
            this.attackAnimationTicks = 0;
            this.animationLocked = false; // Don't lock - death animation has its own flag
            this.lastAttackState = ATTACK_NONE;
            
            // Stop all movement and navigation
            this.setVelocity(Vec3d.ZERO);
            this.velocityModified = true;
            this.getNavigation().stop();
            
            // Clear time distortion effects
            timeFrozenEntities.clear();
            
            // Clear target to prevent AI from trying to attack
            this.setTarget(null);
            
            // Epic death announcement
            Box announceArea = new Box(this.getBlockPos()).expand(64);
            List<PlayerEntity> nearbyPlayers = this.getWorld().getNonSpectatingEntities(PlayerEntity.class, announceArea);
            for (PlayerEntity player : nearbyPlayers) {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    serverPlayer.sendMessage(
                        Text.literal("§5§l§oThoth's essence fades from this realm..."),
                        false
                    );
                }
            }
            
            // Play initial death sound
            this.playSound(SoundEvents.ENTITY_ENDER_DRAGON_HURT, 2.0f, 0.5f);
            
            // Don't call super.onDeath() yet - we'll do it after animation completes
            return;
        }
        
        // If animation already playing or on client, proceed with cleanup
        super.onDeath(source);
        
        // Clean up time distortion effects
        timeFrozenEntities.clear();
        
        // Properly clean up boss bar on death
        if (!this.getWorld().isClient && this.bossBar != null) {
            this.bossBar.setVisible(false);
            this.bossBar.clearPlayers();
            this.bossBar = null;
        }
    }
    
    @Override
    public void remove(RemovalReason reason) {
        // Clean up time distortion effects
        timeFrozenEntities.clear();
        
        // Clean up boss bar BEFORE calling super to ensure it's removed
        if (!this.getWorld().isClient && this.bossBar != null) {
            this.bossBar.setVisible(false);
            this.bossBar.clearPlayers();
            this.bossBar = null;
        }
        
        super.remove(reason);
    }
    
    @Override
    public void onRemoved() {
        // Clean up time distortion effects
        timeFrozenEntities.clear();
        
        // Clean up boss bar BEFORE calling super
        if (!this.getWorld().isClient && this.bossBar != null) {
            this.bossBar.setVisible(false);
            this.bossBar.clearPlayers();
            this.bossBar = null;
        }
        
        super.onRemoved();
    }
    
    @Override
    public boolean canBreatheInWater() {
        return true;
    }
    
    @Override
    public boolean cannotDespawn() {
        return true;
    }
    
    /* ---------- DYNAMIC LIGHTING ---------- */
    /**
     * Provides dynamic lighting from glowing eyes
     * Returns light level 0-15 based on current state
     */
    public int getEyeLightLevel() {
        if (isCastingTimeMagic()) {
            return 12; // Bright purple glow during time magic
        } else if (isAttackingWithMagic()) {
            return 10; // Bright glow during magical attacks
        } else if (isInCombat()) {
            return 8; // Moderate glow during combat
        } else if (isReading()) {
            return 6; // Subtle glow while reading
        }
        return 4; // Base glow always present
    }
    
    /**
     * Gets the color of the eye glow as RGB values (0.0-1.0)
     */
    public Vec3d getEyeGlowColor() {
        if (isCastingTimeMagic()) {
            return new Vec3d(0.7, 0.3, 1.0); // Purple
        } else if (isScrollBlastAttack()) {
            return new Vec3d(1.0, 0.8, 0.3); // Gold
        } else if (isInCombat()) {
            return new Vec3d(1.0, 0.5, 0.2); // Orange-red
        }
        return new Vec3d(1.0, 1.0, 1.0); // White
    }
    
    /**
     * Gets the intensity of the eye glow (0.0-1.0)
     */
    public float getEyeGlowIntensity() {
        float baseIntensity = 0.3f;
        
        if (isCastingTimeMagic()) {
            baseIntensity = 0.9f;
        } else if (isAttackingWithMagic()) {
            baseIntensity = 0.8f;
        } else if (isInCombat()) {
            baseIntensity = 0.6f;
        } else if (isReading()) {
            baseIntensity = 0.4f;
        }
        
        // Add pulsing effect
        float pulse = (float) Math.sin((age * 0.15f)) * 0.1f + 0.9f;
        return baseIntensity * pulse;
    }
}