package com.ancientcurse.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.entity.Entity.RemovalReason;
import software.bernie.geckolib.animatable.GeoEntity;

import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Anubis - The Jackal-Headed God Boss
 * 
 * Based on Egyptian mythology, Anubis is the god of the afterlife, death, and judgment.
 * This boss entity implements special animations and behavior:
 * - attack2: Sky yell animation (looking up and howling)
 * - judgement_idle: Launches into air, hovers with eyes closed, judging the player
 * - judgement_safe: Hovers in air, waves arms while speaking to player, grants safety
 */
public class AnubisEntity extends HostileEntity implements GeoEntity {

    /* ------------------------------------------------------------------------
     *  STATIC CONSTANTS + DATA TRACKERS
     * --------------------------------------------------------------------- */

    // Boss behavior constants
    private static final float BOSS_DETECTION_RADIUS = 32.0F;
    // Combat parameters
    private static final float JUDGEMENT_HEIGHT = 3.0F;
    private static final int JUDGEMENT_DURATION = 200; // 10 seconds
    private static final int SKY_YELL_DURATION = 100;  // 5 seconds - faster, more aggressive attack
    private static final int SAFE_STATE_DURATION = 600; // 30 seconds

    // Animation state tracking
    private static final TrackedData<Integer> BOSS_PHASE = 
            DataTracker.registerData(AnubisEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> IS_HOVERING = 
            DataTracker.registerData(AnubisEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IS_JUDGING = 
            DataTracker.registerData(AnubisEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IS_SKY_YELLING = 
            DataTracker.registerData(AnubisEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> PLAYER_IS_SAFE = 
            DataTracker.registerData(AnubisEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Float> HOVER_HEIGHT = 
            DataTracker.registerData(AnubisEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> IS_ATTACKING = 
            DataTracker.registerData(AnubisEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IS_DYING = 
            DataTracker.registerData(AnubisEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    // Boss phases
    public enum BossPhase {
        DORMANT(0),      // Not engaged
        AWAKENING(1),    // Initial encounter
        COMBAT(2),       // Active fighting
        JUDGING(3),      // Judgment phase - hovering and evaluating
        MERCIFUL(4),     // Player has been deemed safe
        ENRAGED(5),      // Player attacked during judgment
        DEAD(6);         // Anubis is defeated

        private final int id;
        BossPhase(int id) { this.id = id; }
        public int getId() { return id; }
        
        public static BossPhase fromId(int id) {
            for (BossPhase phase : values()) {
                if (phase.id == id) return phase;
            }
            return DORMANT;
        }
    }

    /* -------------------------------------------------------------------- */
    /*  FIELDS                                                              */
    /* -------------------------------------------------------------------- */

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossBar bossBar;

    // State tracking
    private BossPhase currentPhase = BossPhase.DORMANT;
    private int phaseTimer = 0;
    private final Set<UUID> safePlayers = new HashSet<>();

    // Animation tracking
    private String currentAnimation = "";
    private boolean playingSpecialAnimation = false;
    
    // Attack animation tracking - ensures animations play through fully
    // Animation plays at 2x speed, so timings are halved
    private boolean isPlayingAttackAnimation = false;
    private int attackAnimationTicksRemaining = 0;
    private static final int ATTACK_1_DURATION_TICKS = 25; // ~1.2 seconds at 2x speed (was 49 at 1x)
    private static final int ATTACK_1_DAMAGE_TICK = 10; // Deal damage ~0.5 seconds into animation at 2x speed (was 20)
    
    // Pending attack damage - for delayed damage dealing
    private LivingEntity pendingAttackTarget = null;
    private int pendingDamageTick = 0;

    // Layered whoosh sound - plays 0.1 seconds (2 ticks) into attack animation at 2x speed
    private int whooshLayerTick = 0;
    private static final int WHOOSH_LAYER_DELAY = 2; // 0.1 seconds into attack at 2x speed (was 4)
    
    // Movement animation smoothing - prevents flickering between walk/idle
    private int movementGracePeriod = 0; // Ticks to keep "moving" state after stopping
    private static final int MOVEMENT_GRACE_TICKS = 6; // Keep walking animation for 6 ticks after stopping
    private static final double MOVEMENT_THRESHOLD = 0.003; // Lower threshold - entities move slowly

    private PlayerEntity judgedPlayer;
    private int judgementTimer;
    private float targetHoverHeight;

    // Performance optimization
    private float lastHealthPercentage = 1.0f; // Track last health percentage for boss bar updates
    private int bossBarUpdateCooldown = 0; // Cooldown for boss bar player list updates

    // Step sound tracking for big footsteps
    private int stepSoundCooldown = 0;
    private static final int STEP_SOUND_INTERVAL_WALK = 10; // Ticks between steps when walking
    private static final int STEP_SOUND_INTERVAL_RUN = 6;   // Ticks between steps when running (faster)
    
    // Judgment cooldown to prevent spamming judgment phase
    private int judgementCooldown = 0;
    private static final int JUDGEMENT_COOLDOWN = 2400; // 2 minutes between judgments

    // Judgment particle system - optimized swirling effect around player
    private int judgementParticleTick = 0;
    private int judgementSoundLoopTick = 0;
    private static final int JUDGEMENT_SOUND_LOOP_INTERVAL = 60; // Play loop sound every 3 seconds

    // Death sequence tracking - ensures epic death animation plays fully
    private boolean isDying = false;
    private int deathAnimationTicks = 0;
    private DamageSource deathDamageSource = null; // Store for proper loot drops
    private static final int DEATH_ANIMATION_DURATION = 191; // 9.5417 seconds * 20 ticks = ~191 ticks
    private static final int DEATH_EFFECT_PHASE_1 = 40;  // Initial shock wave
    private static final int DEATH_EFFECT_PHASE_2 = 100; // Soul release
    private static final int DEATH_EFFECT_PHASE_3 = 160; // Final collapse

    public AnubisEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 100; // Boss XP
        
        // Create boss bar
        this.bossBar = new ServerBossBar(
            Text.translatable("entity.ancientcurse.anubis"), 
            BossBar.Color.YELLOW, 
            BossBar.Style.PROGRESS
        );
    }

    /* -------------------------------------------------------------------- */
    /*  ATTRIBUTES                                                          */
    /* -------------------------------------------------------------------- */

    public static DefaultAttributeContainer.Builder createAnubisAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 150.0D) // Boss health (reduced for balance)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.4D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0D) // Balanced damage
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.5D) // Moderate knockback resistance
                .add(EntityAttributes.GENERIC_ARMOR, 8.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0D); // Reasonable aggro range
    }

    /* -------------------------------------------------------------------- */
    /*  DATATRACKER + GOALS                                                 */
    /* -------------------------------------------------------------------- */

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        dataTracker.startTracking(BOSS_PHASE, BossPhase.DORMANT.getId());
        dataTracker.startTracking(IS_HOVERING, false);
        dataTracker.startTracking(IS_JUDGING, false);
        dataTracker.startTracking(IS_SKY_YELLING, false);
        dataTracker.startTracking(PLAYER_IS_SAFE, false);
        dataTracker.startTracking(HOVER_HEIGHT, 0.0F);
        dataTracker.startTracking(IS_ATTACKING, false);
        dataTracker.startTracking(IS_DYING, false);
    }

    @Override
    protected void initGoals() {
        // Boss AI goals - priority order matters!
        // Lower numbers = higher priority
        this.goalSelector.add(0, new SwimGoal(this)); // Prevent drowning
        this.goalSelector.add(1, new AnubisJudgementGoal(this)); // Judgment phase takes priority
        this.goalSelector.add(2, new AnubisSkyYellGoal(this)); // Sky yell attack
        this.goalSelector.add(3, new AnubisCombatGoal(this)); // Main combat behavior
        this.goalSelector.add(4, new WanderAroundFarGoal(this, 0.8) {
            @Override
            public boolean canStart() {
                // Only wander when not attacking and in combat phase
                return currentPhase == BossPhase.COMBAT && 
                       !isAttacking() && 
                       getTarget() == null &&
                       super.canStart();
            }
        });
        this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 32.0F));
        this.goalSelector.add(6, new LookAroundGoal(this));

        // Target selection - these run in parallel with goals
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true) {
            @Override
            public boolean canStart() {
                // Allow targeting in COMBAT, ENRAGED, and AWAKENING phases
                return (currentPhase == BossPhase.COMBAT || 
                        currentPhase == BossPhase.ENRAGED ||
                        currentPhase == BossPhase.AWAKENING) && super.canStart();
            }
            
            @Override
            public boolean shouldContinue() {
                // Keep targeting even when phase changes
                return (currentPhase == BossPhase.COMBAT || 
                        currentPhase == BossPhase.ENRAGED) && super.shouldContinue();
            }
        });
    }

    /* -------------------------------------------------------------------- */
    /*  MAIN TICK METHOD                                                    */
    /* -------------------------------------------------------------------- */

    @Override
    public void tick() {
        super.tick();

        // Despawn in peaceful mode (like vanilla hostile mobs)
        if (this.getWorld().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
            // Clean up boss bar before despawning
            if (!this.getWorld().isClient && this.bossBar != null) {
                this.bossBar.setVisible(false);
                this.bossBar.clearPlayers();
            }
            this.discard();
            return;
        }
        
        // Handle death animation sequence - prevents normal updates during death
        if (isDying) {
            updateDeathSequence();
            return; // Skip normal tick logic during death
        }

        if (!this.getWorld().isClient) {
            updateBossPhase();
            updateBossBar();
            updateHovering();
            updateStepSounds();
            updateAttackAnimationTimer();
        }

        // Client-side effects
        if (this.getWorld().isClient) {
            spawnPhaseParticles();
        }

        phaseTimer++;
    }
    
    /**
     * Handles the epic death sequence with multiple phases of effects.
     * This ensures the death animation plays fully before the entity is removed.
     */
    private void updateDeathSequence() {
        deathAnimationTicks++;
        
        // Server-side death effects
        if (!this.getWorld().isClient) {
            // Phase 1: Initial shock wave and golden light burst
            if (deathAnimationTicks == DEATH_EFFECT_PHASE_1) {
                playDeathEffectPhase1();
            }
            // Phase 2: Soul particles rising, divine whispers
            else if (deathAnimationTicks == DEATH_EFFECT_PHASE_2) {
                playDeathEffectPhase2();
            }
            // Phase 3: Final collapse, treasure burst
            else if (deathAnimationTicks == DEATH_EFFECT_PHASE_3) {
                playDeathEffectPhase3();
            }
            // Final removal after animation completes
            else if (deathAnimationTicks >= DEATH_ANIMATION_DURATION) {
                finalizeDeathAndDropLoot();
            }
        }
        
        // Client-side continuous death particles
        if (this.getWorld().isClient) {
            spawnDeathParticles();
        }
    }
    
    /**
     * Phase 1: Golden shock wave emanates from Anubis
     */
    private void playDeathEffectPhase1() {
        // Play dramatic sound
        this.getWorld().playSound(
            null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.ENTITY_WITHER_DEATH, SoundCategory.HOSTILE,
            2.0F, 0.5F // Deep, booming
        );
        
        // Screen shake effect via explosion (no damage)
        this.getWorld().createExplosion(
            this, this.getX(), this.getY() + 1, this.getZ(),
            0.0F, false, World.ExplosionSourceType.NONE
        );
    }
    
    /**
     * Phase 2: Soul release - divine energy escaping
     */
    private void playDeathEffectPhase2() {
        // Play ethereal sound
        this.getWorld().playSound(
            null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE,
            1.5F, 1.5F // Higher pitch for ethereal feel
        );
        
        // Play Anubis howl as final cry
        playRandomHowl();
    }
    
    /**
     * Phase 3: Final collapse - treasure revealed
     */
    private void playDeathEffectPhase3() {
        // Play treasure reveal sound
        this.getWorld().playSound(
            null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.HOSTILE,
            2.0F, 1.0F
        );
        
        // Additional mystical sound
        this.getWorld().playSound(
            null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE,
            2.0F, 0.7F
        );
    }
    
    /**
     * Spawns continuous death particles on the client side
     */
    private void spawnDeathParticles() {
        // Golden end rod particles spiraling upward
        if (deathAnimationTicks % 2 == 0) {
            double angle = (deathAnimationTicks * 0.2) % (Math.PI * 2);
            double radius = 1.0 + (deathAnimationTicks * 0.01);
            double x = getX() + Math.cos(angle) * radius;
            double z = getZ() + Math.sin(angle) * radius;
            double y = getY() + 1.0 + (deathAnimationTicks * 0.02);
            
            getWorld().addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0.1, 0);
        }
        
        // Soul particles rising
        if (deathAnimationTicks % 5 == 0) {
            double x = getX() + (random.nextDouble() - 0.5) * 2;
            double y = getY() + 1.0 + random.nextDouble() * 2;
            double z = getZ() + (random.nextDouble() - 0.5) * 2;
            getWorld().addParticle(ParticleTypes.SOUL, x, y, z, 0, 0.15, 0);
        }
        
        // Enchant particles during middle phase
        if (deathAnimationTicks > DEATH_EFFECT_PHASE_1 && deathAnimationTicks < DEATH_EFFECT_PHASE_3) {
            if (deathAnimationTicks % 3 == 0) {
                double x = getX() + (random.nextDouble() - 0.5) * 3;
                double y = getY() + random.nextDouble() * 3;
                double z = getZ() + (random.nextDouble() - 0.5) * 3;
                getWorld().addParticle(ParticleTypes.ENCHANT, x, y, z, 0, 0.5, 0);
            }
        }
        
        // Explosion particles at final phase
        if (deathAnimationTicks > DEATH_EFFECT_PHASE_3) {
            if (deathAnimationTicks % 4 == 0) {
                double x = getX() + (random.nextDouble() - 0.5) * 2;
                double y = getY() + 0.5 + random.nextDouble();
                double z = getZ() + (random.nextDouble() - 0.5) * 2;
                getWorld().addParticle(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 
                    (random.nextDouble() - 0.5) * 0.5, 0.3, (random.nextDouble() - 0.5) * 0.5);
            }
        }
    }
    
    /**
     * Final death cleanup - drop loot and remove entity
     */
    private void finalizeDeathAndDropLoot() {
        // Grant advancement/experience to nearby players
        for (PlayerEntity player : this.getWorld().getPlayers()) {
            if (player.squaredDistanceTo(this) <= 50 * 50) {
                // Send victory message
                player.sendMessage(
                    Text.literal("§6§l⚱ §eAnubis, God of the Dead, has been vanquished! §6§l⚱"),
                    false
                );
            }
        }
        
        // Final burst of particles (server-side for all clients)
        if (this.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
            // Totem-like particle explosion
            for (int i = 0; i < 50; i++) {
                double vx = (random.nextDouble() - 0.5) * 0.8;
                double vy = random.nextDouble() * 0.5 + 0.3;
                double vz = (random.nextDouble() - 0.5) * 0.8;
                serverWorld.spawnParticles(
                    ParticleTypes.TOTEM_OF_UNDYING,
                    this.getX(), this.getY() + 1, this.getZ(),
                    1, vx, vy, vz, 0.2
                );
            }
        }
        
        // Drop experience orbs (boss-level XP)
        if (this.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
            net.minecraft.entity.ExperienceOrbEntity.spawn(
                serverWorld, this.getPos(), 500 // Boss-level XP
            );
        }
        
        // Call super.onDeath() to trigger loot table drops
        if (deathDamageSource != null) {
            super.onDeath(deathDamageSource);
        }
        
        // Now actually remove the entity
        this.remove(RemovalReason.KILLED);
    }
    
    /**
     * Updates the attack animation timer to ensure animations play through fully.
     * The attack state is set by startMeleeAttack() and this method handles the countdown.
     * The IS_ATTACKING data tracker syncs this state to the client for proper animation rendering.
     * Also handles delayed damage dealing when the attack animation connects.
     */
    private void updateAttackAnimationTimer() {
        // Decrement attack animation timer
        if (attackAnimationTicksRemaining > 0) {
            attackAnimationTicksRemaining--;
            if (attackAnimationTicksRemaining <= 0) {
                isPlayingAttackAnimation = false;
                setAttacking(false); // Sync to client - animation complete
            }
        }
        
        // Handle delayed damage - deal damage when the swing connects
        if (pendingDamageTick > 0) {
            pendingDamageTick--;

            // Continue tracking target during attack wind-up (before damage tick)
            if (pendingDamageTick > 0 && pendingAttackTarget != null && !isHovering()) {
                // Turn to face target during attack
                this.getLookControl().lookAt(pendingAttackTarget, 30.0F, 30.0F);
            }

            if (pendingDamageTick <= 0 && pendingAttackTarget != null) {
                // Check if target is still valid and in range - 5 blocks for better feel
                if (pendingAttackTarget.isAlive() && this.squaredDistanceTo(pendingAttackTarget) <= 25.0) { // 5 blocks
                    // Deal the actual damage now
                    this.tryAttack(pendingAttackTarget);
                }
                // Clear pending attack
                pendingAttackTarget = null;
            }
        }

        // Handle layered whoosh sound - plays 0.2 seconds into attack
        if (whooshLayerTick > 0) {
            whooshLayerTick--;

            if (whooshLayerTick <= 0) {
                // Play the layered whoosh with slight pitch variation
                this.getWorld().playSound(
                    null,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    com.ancientcurse.ModSounds.ANUBIS_ATTACK_1_WHOOSH_LAYER,
                    SoundCategory.HOSTILE,
                    1.0F,
                    0.95F + this.random.nextFloat() * 0.1F  // Pitch variation (0.95-1.05)
                );
            }
        }
    }

    private void updateBossPhase() {
        PlayerEntity nearestPlayer = this.getWorld().getClosestPlayer(this, BOSS_DETECTION_RADIUS);
        
        switch (currentPhase) {
            case DORMANT:
                if (nearestPlayer != null && canSee(nearestPlayer)) {
                    setBossPhase(BossPhase.AWAKENING);
                    phaseTimer = 0;
                }
                break;
                
            case AWAKENING:
                if (phaseTimer > 60) { // 3 seconds
                    setBossPhase(BossPhase.COMBAT);
                    setTarget(nearestPlayer);
                }
                break;
                
            case COMBAT:
                // Transition to judgment after some combat time (only if cooldown has expired)
                if (phaseTimer > 400 && nearestPlayer != null && judgementCooldown <= 0) { // 20 seconds
                    initiateCombatJudgement(nearestPlayer);
                }
                // Decrement judgment cooldown
                if (judgementCooldown > 0) {
                    judgementCooldown--;
                }
                break;
                
            case JUDGING:
                if (judgementTimer <= 0) {
                    concludeJudgement();
                } else {
                    judgementTimer--;
                    // Play looping ambient judgment sound every 3 seconds
                    judgementSoundLoopTick++;
                    if (judgementSoundLoopTick >= JUDGEMENT_SOUND_LOOP_INTERVAL) {
                        judgementSoundLoopTick = 0;
                        this.getWorld().playSound(
                            null,
                            this.getX(),
                            this.getY(),
                            this.getZ(),
                            com.ancientcurse.ModSounds.ANUBIS_JUDGEMENT_LOOP,
                            SoundCategory.HOSTILE,
                            1.0F,
                            0.9F + this.random.nextFloat() * 0.2F
                        );
                    }
                    // Spawn swirling particles around the judged player (server-side)
                    if (judgedPlayer != null && this.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                        spawnJudgementParticlesAroundPlayer(serverWorld, judgedPlayer);
                    }
                }
                break;

            case MERCIFUL:
                // Stay merciful for a while, then return to combat if attacked
                if (phaseTimer > SAFE_STATE_DURATION) {
                    setBossPhase(BossPhase.COMBAT);
                }
                break;
                
            case ENRAGED:
                // Enhanced combat phase
                break;
                
            case DEAD:
                // Anubis is defeated
                break;
        }
    }

    private void updateBossBar() {
        // Only update health percentage if it has changed significantly (more than 1% change)
        float currentHealthPercentage = getHealth() / getMaxHealth();
        if (Math.abs(currentHealthPercentage - lastHealthPercentage) > 0.01f) {
            bossBar.setPercent(currentHealthPercentage);
            lastHealthPercentage = currentHealthPercentage;
        }

        // Only update player list every 20 ticks (1 second) to reduce overhead
        if (bossBarUpdateCooldown <= 0) {
            bossBarUpdateCooldown = 20; // Reset cooldown

            // Add/remove players from boss bar
            for (PlayerEntity player : getWorld().getPlayers()) {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    boolean isNearby = squaredDistanceTo(player) <= BOSS_DETECTION_RADIUS * BOSS_DETECTION_RADIUS;
                    boolean isTracking = bossBar.getPlayers().contains(serverPlayer);

                    if (isNearby && !isTracking) {
                        bossBar.addPlayer(serverPlayer);
                    } else if (!isNearby && isTracking) {
                        bossBar.removePlayer(serverPlayer);
                    }
                }
            }
        } else {
            bossBarUpdateCooldown--;
        }
    }

    private void updateHovering() {
        if (isHovering()) {
            // Smooth hovering motion
            float currentHeight = dataTracker.get(HOVER_HEIGHT);
            if (Math.abs(currentHeight - targetHoverHeight) > 0.1F) {
                float newHeight = currentHeight + (targetHoverHeight - currentHeight) * 0.1F;
                dataTracker.set(HOVER_HEIGHT, newHeight);
            }
            
            // Apply hovering effect
            if (!this.hasVehicle()) {
                // Disable gravity while hovering
                this.setNoGravity(true);
                
                // Gentle floating motion
                double hoverOffset = Math.sin(this.age * 0.1) * 0.1;
                this.setVelocity(this.getVelocity().x, hoverOffset, this.getVelocity().z);
            }
        } else {
            // Re-enable gravity when not hovering
            this.setNoGravity(false);
            dataTracker.set(HOVER_HEIGHT, 0.0F);
        }
    }

    private void initiateCombatJudgement(PlayerEntity player) {
        setBossPhase(BossPhase.JUDGING);
        judgedPlayer = player;
        judgementTimer = JUDGEMENT_DURATION;
        judgementCooldown = JUDGEMENT_COOLDOWN; // Start cooldown after judgment

        // Reset particle and sound loop counters
        judgementParticleTick = 0;
        judgementSoundLoopTick = 0;

        // Start hovering
        setHovering(true);
        setJudging(true);
        targetHoverHeight = JUDGEMENT_HEIGHT;

        // Stop current combat
        setTarget(null);

        // Play judgment voice line and whoosh effect
        if (!this.getWorld().isClient) {
            // Play the judgment whoosh sound effect first
            this.getWorld().playSound(
                null,
                this.getX(),
                this.getY(),
                this.getZ(),
                com.ancientcurse.ModSounds.ANUBIS_JUDGEMENT_WHOOSH,
                SoundCategory.HOSTILE,
                1.5F,
                1.0F
            );
            
            // Then play the judgment voice line
            this.getWorld().playSound(
                null,
                this.getX(),
                this.getY(),
                this.getZ(),
                com.ancientcurse.ModSounds.ANUBIS_JUDGEMENT,
                SoundCategory.HOSTILE,
                2.0F,
                1.0F
            );
        }
        
        phaseTimer = 0;
    }

    private void concludeJudgement() {
        if (judgedPlayer != null) {
            // Simple judgment logic - could be enhanced based on player actions
            boolean playerIsWorthyOfMercy = judgedPlayer.getHealth() < judgedPlayer.getMaxHealth() * 0.3f ||
                                          !judgedPlayer.getMainHandStack().isEmpty();
            
            if (playerIsWorthyOfMercy) {
                setBossPhase(BossPhase.MERCIFUL);
                safePlayers.add(judgedPlayer.getUuid());
                setPlayerSafe(true);
                
                // Play safe/merciful voice line
                if (!this.getWorld().isClient) {
                    this.getWorld().playSound(
                        null,
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        com.ancientcurse.ModSounds.ANUBIS_JUDGEMENT_SAFE,
                        SoundCategory.HOSTILE,
                        2.0F,
                        1.0F
                    );
                }
                
                // Send message to player
                judgedPlayer.sendMessage(Text.translatable("message.ancientcurse.anubis.mercy"), false);
            } else {
                setBossPhase(BossPhase.ENRAGED);
                setTarget(judgedPlayer);
                
                // Play "heart is heavy" voice line when entering enraged/combat
                if (!this.getWorld().isClient) {
                    this.getWorld().playSound(
                        null,
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        com.ancientcurse.ModSounds.ANUBIS_HEART_IS_HEAVY,
                        SoundCategory.HOSTILE,
                        2.0F,
                        1.0F
                    );
                }
            }
        }
        
        // Play magic shimmer sound when judgment concludes
        if (!this.getWorld().isClient) {
            this.getWorld().playSound(
                null,
                this.getX(),
                this.getY(),
                this.getZ(),
                com.ancientcurse.ModSounds.ANUBIS_MAGIC_SHIMMER,
                SoundCategory.HOSTILE,
                1.2F,
                1.0F
            );
        }
        
        // End hovering and judging
        setHovering(false);
        setJudging(false);
        targetHoverHeight = 0.0F;
        judgedPlayer = null;
        phaseTimer = 0;
    }

    /**
     * Spawns optimized swirling judgment particles around the player being judged.
     * Creates a dramatic spiraling effect with golden and mystical particles.
     * Performance optimized: only spawns particles every 2 ticks and uses efficient patterns.
     */
    private void spawnJudgementParticlesAroundPlayer(net.minecraft.server.world.ServerWorld serverWorld, PlayerEntity player) {
        judgementParticleTick++;

        // Only spawn particles every 2 ticks for performance
        if (judgementParticleTick % 2 != 0) {
            return;
        }

        double playerX = player.getX();
        double playerY = player.getY();
        double playerZ = player.getZ();

        // Calculate rotation angle based on tick for smooth spinning
        double baseAngle = (judgementParticleTick * 0.15) % (Math.PI * 2);

        // === LAYER 1: Inner golden spiral (close to player) ===
        for (int i = 0; i < 3; i++) {
            double angle = baseAngle + (i * Math.PI * 2 / 3);
            double radius = 0.8;
            double height = playerY + 0.5 + Math.sin(baseAngle * 2 + i) * 0.5;

            double x = playerX + Math.cos(angle) * radius;
            double z = playerZ + Math.sin(angle) * radius;

            // Golden end rod particles - divine judgment light
            serverWorld.spawnParticles(ParticleTypes.END_ROD, x, height, z, 1, 0.05, 0.05, 0.05, 0.01);
        }

        // === LAYER 2: Outer enchantment spiral (wider orbit) ===
        for (int i = 0; i < 4; i++) {
            double angle = -baseAngle * 0.7 + (i * Math.PI * 2 / 4); // Counter-rotation
            double radius = 1.5 + Math.sin(baseAngle) * 0.3; // Pulsing radius
            double height = playerY + 1.0 + (i * 0.4);

            double x = playerX + Math.cos(angle) * radius;
            double z = playerZ + Math.sin(angle) * radius;

            // Enchantment particles swirling around player
            serverWorld.spawnParticles(ParticleTypes.ENCHANT, x, height, z, 1, 0.1, 0.1, 0.1, 0.02);
        }

        // === LAYER 3: Soul wisps rising upward (every 4 ticks) ===
        if (judgementParticleTick % 4 == 0) {
            for (int i = 0; i < 2; i++) {
                double angle = baseAngle * 1.5 + (i * Math.PI);
                double radius = 1.0;
                double x = playerX + Math.cos(angle) * radius;
                double z = playerZ + Math.sin(angle) * radius;

                // Soul particles rising - representing the weighing of the soul
                serverWorld.spawnParticles(ParticleTypes.SOUL, x, playerY + 0.2, z, 1, 0.1, 0.3, 0.1, 0.03);
            }
        }

        // === LAYER 4: Overhead halo particles (every 6 ticks) ===
        if (judgementParticleTick % 6 == 0) {
            double haloRadius = 0.6;
            for (int i = 0; i < 6; i++) {
                double angle = baseAngle * 0.5 + (i * Math.PI * 2 / 6);
                double x = playerX + Math.cos(angle) * haloRadius;
                double z = playerZ + Math.sin(angle) * haloRadius;

                // Golden glow above player's head like a judgment halo
                serverWorld.spawnParticles(ParticleTypes.END_ROD, x, playerY + 2.2, z, 1, 0.02, 0.02, 0.02, 0.005);
            }
        }

        // === Connection beam between Anubis and player (every 3 ticks) ===
        if (judgementParticleTick % 3 == 0) {
            double anubisEyeX = this.getX();
            double anubisEyeY = this.getY() + 2.5; // Eye level
            double anubisEyeZ = this.getZ();

            // Spawn 3 particles along the line connecting Anubis to player
            for (int i = 1; i <= 3; i++) {
                double t = i / 4.0;
                double beamX = anubisEyeX + (playerX - anubisEyeX) * t;
                double beamY = anubisEyeY + (playerY + 1.0 - anubisEyeY) * t;
                double beamZ = anubisEyeZ + (playerZ - anubisEyeZ) * t;

                serverWorld.spawnParticles(ParticleTypes.ENCHANT, beamX, beamY, beamZ, 1, 0.05, 0.05, 0.05, 0.01);
            }
        }
    }

    private void spawnPhaseParticles() {
        switch (currentPhase) {
            case AWAKENING:
                // Golden particles during awakening
                if (age % 5 == 0) {
                    for (int i = 0; i < 3; i++) {
                        double x = getX() + (random.nextDouble() - 0.5) * 2.0;
                        double y = getY() + 1.0 + random.nextDouble();
                        double z = getZ() + (random.nextDouble() - 0.5) * 2.0;
                        getWorld().addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0.05, 0);
                    }
                }
                break;
                
            case JUDGING:
                // Sacred judgment particles
                if (age % 3 == 0) {
                    double x = getX() + (random.nextDouble() - 0.5) * 1.5;
                    double y = getY() + 2.0 + random.nextDouble();
                    double z = getZ() + (random.nextDouble() - 0.5) * 1.5;
                    getWorld().addParticle(ParticleTypes.ENCHANT, x, y, z, 0, -0.02, 0);
                }
                break;
                
            case MERCIFUL:
                // Peaceful particles
                if (age % 10 == 0) {
                    double x = getX() + (random.nextDouble() - 0.5);
                    double y = getY() + 1.5;
                    double z = getZ() + (random.nextDouble() - 0.5);
                    getWorld().addParticle(ParticleTypes.HAPPY_VILLAGER, x, y, z, 0, 0.1, 0);
                }
                break;
                
            case ENRAGED:
                // Angry particles
                if (age % 7 == 0) {
                    double x = getX() + (random.nextDouble() - 0.5) * 1.2;
                    double y = getY() + 0.5 + random.nextDouble();
                    double z = getZ() + (random.nextDouble() - 0.5) * 1.2;
                    getWorld().addParticle(ParticleTypes.ANGRY_VILLAGER, x, y, z, 0, 0, 0);
                }
                break;
                
            case DEAD:
                // Death particles
                if (age % 5 == 0) {
                    double x = getX() + (random.nextDouble() - 0.5);
                    double y = getY() + 1.5;
                    double z = getZ() + (random.nextDouble() - 0.5);
                    getWorld().addParticle(ParticleTypes.SOUL, x, y, z, 0, 0.1, 0);
                }
                break;
                
            case DORMANT:
                // Dormant particles
                if (age % 10 == 0) {
                    double x = getX() + (random.nextDouble() - 0.5);
                    double y = getY() + 1.5;
                    double z = getZ() + (random.nextDouble() - 0.5);
                    getWorld().addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.1, 0);
                }
                break;
                
            case COMBAT:
                // Combat particles
                if (age % 7 == 0) {
                    double x = getX() + (random.nextDouble() - 0.5) * 1.2;
                    double y = getY() + 0.5 + random.nextDouble();
                    double z = getZ() + (random.nextDouble() - 0.5) * 1.2;
                    getWorld().addParticle(ParticleTypes.CRIT, x, y, z, 0, 0, 0);
                }
                break;
        }
    }

    /* -------------------------------------------------------------------- */
    /*  CUSTOM AI GOALS                                                     */
    /* -------------------------------------------------------------------- */

    /**
     * Sky Yell Attack (attack_2) - Anubis howls at the sky, lifting nearby players
     * into the air with swirling particles and dealing damage over time.
     */
    private class AnubisSkyYellGoal extends Goal {
        private final AnubisEntity anubis;
        private int yellTimer = 0;
        private int skyYellCooldown = 0;
        
        // Effect parameters - tuned for 5 second (100 tick) duration
        private static final double EFFECT_RADIUS = 8.0; // Affects players within 8 blocks
        private static final float DAMAGE_PER_TICK = 0.5f; // Damage every tick during effect
        private static final double LIFT_HEIGHT = 4.0; // How high to lift players
        private static final int LIFT_DURATION = 50; // 2.5 seconds of lifting (first half of attack)
        private static final int SLAM_TICK = 20; // Slam happens 1 second before end
        private static final int COOLDOWN_TICKS = 200; // 10 second cooldown between sky yells

        AnubisSkyYellGoal(AnubisEntity anubis) {
            this.anubis = anubis;
            this.setControls(EnumSet.of(Goal.Control.LOOK, Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            // Decrement cooldown
            if (skyYellCooldown > 0) {
                skyYellCooldown--;
                return false;
            }
            
            // Can use in COMBAT or ENRAGED phases
            if (anubis.currentPhase != BossPhase.COMBAT && anubis.currentPhase != BossPhase.ENRAGED) {
                return false;
            }
            
            // Need a target
            if (anubis.getTarget() == null) {
                return false;
            }
            
            // Don't interrupt melee attacks
            if (anubis.isAttacking()) {
                return false;
            }
            
            // 1/80 chance per tick when conditions are met (~every 4 seconds on average)
            // Higher chance when enraged
            int chance = anubis.currentPhase == BossPhase.ENRAGED ? 50 : 80;
            return anubis.random.nextInt(chance) == 0;
        }

        @Override
        public void start() {
            yellTimer = SKY_YELL_DURATION;
            anubis.setSkyYelling(true);
            
            // Stop movement during sky yell
            anubis.getNavigation().stop();

            // Play howl voice at start
            if (!anubis.getWorld().isClient) {
                anubis.playRandomHowl();

                // Play swirly whoosh wind sound
                anubis.getWorld().playSound(null, anubis.getX(), anubis.getY(), anubis.getZ(),
                    SoundEvents.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.HOSTILE, 2.0f, 0.8f);

                // Play ominous warden boom
                anubis.getWorld().playSound(null, anubis.getX(), anubis.getY(), anubis.getZ(),
                    SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 1.5f, 0.5f);
            }
        }

        @Override
        public boolean shouldContinue() {
            return yellTimer > 0 && anubis.isAlive();
        }

        @Override
        public void tick() {
            yellTimer--;
            
            // Stop all movement during sky yell
            anubis.getNavigation().stop();
            anubis.setVelocity(0, anubis.getVelocity().y, 0);
            
            // Server-side effects
            if (!anubis.getWorld().isClient) {
                // Play additional howls during the attack to match the long animation
                // Howls at tick 70 and 40 (spaced ~1.5 seconds apart to avoid overlap)
                if (yellTimer == 70 || yellTimer == 40) {
                    anubis.playRandomHowl();
                }
                
                // Get all nearby players
                net.minecraft.util.math.Box effectArea = new net.minecraft.util.math.Box(
                    anubis.getX() - EFFECT_RADIUS, anubis.getY() - 2, anubis.getZ() - EFFECT_RADIUS,
                    anubis.getX() + EFFECT_RADIUS, anubis.getY() + LIFT_HEIGHT + 2, anubis.getZ() + EFFECT_RADIUS
                );
                
                java.util.List<PlayerEntity> nearbyPlayers = anubis.getWorld().getEntitiesByClass(
                    PlayerEntity.class, effectArea, p -> !p.isCreative() && !p.isSpectator()
                );
                
                // Lift phase (first 3 seconds)
                if (yellTimer > SKY_YELL_DURATION - LIFT_DURATION) {
                    // Play whoosh wind sounds periodically during lift
                    if (yellTimer % 15 == 0) {
                        anubis.getWorld().playSound(null, anubis.getX(), anubis.getY(), anubis.getZ(),
                            SoundEvents.ITEM_TRIDENT_THROW, SoundCategory.HOSTILE, 1.5f, 1.2f);
                    }

                    for (PlayerEntity player : nearbyPlayers) {
                        // Lift player up
                        double targetY = anubis.getY() + LIFT_HEIGHT;
                        if (player.getY() < targetY) {
                            player.setVelocity(player.getVelocity().x * 0.5, 0.3, player.getVelocity().z * 0.5);
                            player.velocityModified = true;
                        }
                        
                        // Deal damage every 10 ticks (0.5 seconds)
                        if (yellTimer % 10 == 0) {
                            player.damage(anubis.getWorld().getDamageSources().magic(), DAMAGE_PER_TICK * 5);
                        }
                        
                        // Spawn swirling white wind particles around player
                        if (anubis.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                            double angle = (anubis.age + yellTimer) * 0.3; // Faster swirl
                            for (int i = 0; i < 5; i++) { // More particles for denser effect
                                double offsetAngle = angle + (i * Math.PI * 2 / 5);
                                double radius = 1.5 + Math.sin(angle) * 0.3; // Pulsing radius
                                double px = player.getX() + Math.cos(offsetAngle) * radius;
                                double pz = player.getZ() + Math.sin(offsetAngle) * radius;
                                double py = player.getY() + (yellTimer % 20) * 0.15;

                                // White cloud/wind particles (snowflake for white, cloud for puffs)
                                serverWorld.spawnParticles(ParticleTypes.SNOWFLAKE,
                                    px, py, pz, 1, 0.1, 0.1, 0.1, 0.02);
                                serverWorld.spawnParticles(ParticleTypes.CLOUD,
                                    px, py + 0.5, pz, 1, 0.05, 0.05, 0.05, 0.01);

                                // Add some sweep particles for wind effect
                                if (i % 2 == 0) {
                                    serverWorld.spawnParticles(ParticleTypes.SWEEP_ATTACK,
                                        px, py + 1, pz, 1, 0, 0, 0, 0);
                                }
                            }
                        }
                    }
                }
                
                // Slam down phase (last part of animation)
                if (yellTimer == SLAM_TICK) {
                    for (PlayerEntity player : nearbyPlayers) {
                        // Slam player down
                        player.setVelocity(0, -1.5, 0);
                        player.velocityModified = true;
                        
                        // Big damage on slam
                        player.damage(anubis.getWorld().getDamageSources().magic(), 6.0f);
                    }
                    
                    // Ground slam particles
                    if (anubis.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                        serverWorld.spawnParticles(ParticleTypes.EXPLOSION, 
                            anubis.getX(), anubis.getY(), anubis.getZ(), 3, 2, 0.5, 2, 0);
                        serverWorld.spawnParticles(ParticleTypes.SOUL, 
                            anubis.getX(), anubis.getY() + 1, anubis.getZ(), 30, 3, 1, 3, 0.1);
                    }
                    
                    // Slam sound
                    anubis.getWorld().playSound(null, anubis.getX(), anubis.getY(), anubis.getZ(),
                        SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.0f, 0.8f);
                }
                
                // Spawn white wind particles around Anubis during the yell
                if (anubis.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                    // Swirling white wind around Anubis
                    for (int i = 0; i < 3; i++) {
                        double angle = (anubis.age * 0.2) + (i * Math.PI * 2 / 3);
                        double radius = 2.0 + Math.sin(anubis.age * 0.1) * 0.5;
                        double px = anubis.getX() + Math.cos(angle) * radius;
                        double pz = anubis.getZ() + Math.sin(angle) * radius;
                        double py = anubis.getY() + 1.5 + anubis.random.nextDouble();

                        serverWorld.spawnParticles(ParticleTypes.CLOUD, px, py, pz, 1, 0.1, 0.1, 0.1, 0.02);
                        serverWorld.spawnParticles(ParticleTypes.SNOWFLAKE, px, py, pz, 1, 0, 0.2, 0, 0.05);
                    }
                }
            }
        }

        @Override
        public void stop() {
            anubis.setSkyYelling(false);
            skyYellCooldown = COOLDOWN_TICKS; // Set cooldown
        }
    }

    private class AnubisJudgementGoal extends Goal {
        private final AnubisEntity anubis;

        AnubisJudgementGoal(AnubisEntity anubis) {
            this.anubis = anubis;
            this.setControls(EnumSet.of(Goal.Control.LOOK, Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            return anubis.currentPhase == BossPhase.JUDGING;
        }

        @Override
        public boolean shouldContinue() {
            return anubis.currentPhase == BossPhase.JUDGING;
        }

        @Override
        public void tick() {
            // Look at the player being judged
            if (anubis.judgedPlayer != null) {
                anubis.getLookControl().lookAt(anubis.judgedPlayer);
            }
            
            // Hovering behavior is handled in the main tick method
        }
    }
    
    /**
     * Custom combat goal for Anubis that handles melee attacks with proper animation timing.
     * This is a standalone Goal (not extending MeleeAttackGoal) for full control over behavior.
     * Similar to Thoth's ThothMagicAttackGoal approach.
     */
    private class AnubisCombatGoal extends Goal {
        private final AnubisEntity anubis;
        private int attackCooldown = 0;
        private LivingEntity target;
        
        // Attack reach - Anubis is a large boss with long arms
        private static final double ATTACK_REACH_SQ = 12.25; // 3.5 blocks squared
        
        AnubisCombatGoal(AnubisEntity anubis) {
            this.anubis = anubis;
            // Control MOVE and LOOK to prevent other goals from interfering
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }
        
        @Override
        public boolean canStart() {
            // Only attack during combat or enraged phases
            if (anubis.currentPhase != BossPhase.COMBAT && anubis.currentPhase != BossPhase.ENRAGED) {
                return false;
            }
            
            this.target = anubis.getTarget();
            if (this.target == null || !this.target.isAlive()) {
                return false;
            }
            
            return true;
        }
        
        @Override
        public boolean shouldContinue() {
            // Stop if we're in a non-combat phase
            if (anubis.currentPhase != BossPhase.COMBAT && anubis.currentPhase != BossPhase.ENRAGED) {
                return false;
            }
            
            // Stop if target is gone or dead
            if (this.target == null || !this.target.isAlive()) {
                return false;
            }
            
            // Stop if target is too far (64 blocks)
            if (anubis.squaredDistanceTo(this.target) > 4096) {
                return false;
            }
            
            return true;
        }
        
        @Override
        public void start() {
            // Reset cooldown on start so Anubis attacks quickly when engaging
            this.attackCooldown = 10; // Small initial delay
        }
        
        @Override
        public void stop() {
            this.target = null;
            anubis.getNavigation().stop();
        }
        
        @Override
        public void tick() {
            if (this.target == null || !this.target.isAlive()) {
                return;
            }
            
            // Always look at target
            anubis.getLookControl().lookAt(this.target, 30.0f, 30.0f);
            
            // Decrement cooldown
            if (attackCooldown > 0) {
                attackCooldown--;
            }
            
            // If currently attacking, stop movement for clean animation
            if (anubis.isAttacking()) {
                anubis.getNavigation().stop();
                // Stop horizontal velocity during attack for cleaner animation
                Vec3d vel = anubis.getVelocity();
                anubis.setVelocity(vel.x * 0.3, vel.y, vel.z * 0.3);
                return;
            }
            
            double distanceSq = anubis.squaredDistanceTo(this.target);
            
            // Move towards target if not in range
            if (distanceSq > ATTACK_REACH_SQ) {
                // Move faster when enraged
                double speed = anubis.currentPhase == BossPhase.ENRAGED ? 1.4 : 1.1;
                anubis.getNavigation().startMovingTo(this.target, speed);
            } else {
                // In range - stop and attack if cooldown is ready
                anubis.getNavigation().stop();
                
                if (attackCooldown <= 0 && anubis.canSee(this.target)) {
                    // Start attack animation and play whoosh sound
                    anubis.startMeleeAttack(this.target);
                    
                    // Set cooldown - shorter when enraged for more aggressive attacks
                    int baseCooldown = ATTACK_1_DURATION_TICKS + 5;
                    attackCooldown = anubis.currentPhase == BossPhase.ENRAGED ? 
                        (int)(baseCooldown * 0.7) : baseCooldown;
                }
            }
        }
    }

    /* -------------------------------------------------------------------- */
    /*  ANIMATION CONTROLLERS                                               */
    /* -------------------------------------------------------------------- */

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar r) {
        // CRITICAL: Use a SINGLE controller like Thoth does
        // Multiple controllers can conflict and cause animations to not play
        // Transition time of 5 ticks for smooth blending
        r.add(new AnimationController<>(this, "controller", 5, this::animationPredicate));
    }

    /**
     * Single unified animation predicate that handles ALL animations with clear priority.
     * This approach (used by Thoth) is more reliable than multiple controllers.
     */
    private <T extends GeoAnimatable> PlayState animationPredicate(AnimationState<T> state) {
        // Get synced phase from data tracker (works on both client and server)
        BossPhase phase = getBossPhase();

        // ===== PRIORITY 0: Death animation (absolute highest priority) =====
        if (phase == BossPhase.DEAD || dataTracker.get(IS_DYING)) {
            state.getController().setAnimation(RawAnimation.begin()
                    .then("animation.anubis.death", Animation.LoopType.HOLD_ON_LAST_FRAME));
            return PlayState.CONTINUE;
        }

        // ===== PRIORITY 1: Sky Yell attack (attack_2) =====
        if (isSkyYelling()) {
            state.getController().setAnimation(RawAnimation.begin()
                    .then("animation.anubis.attack_2", Animation.LoopType.PLAY_ONCE));
            return PlayState.CONTINUE;
        }

        // ===== PRIORITY 2: Melee attack (attack_1) - plays at 2x speed =====
        if (isAttacking()) {
            state.getController().setAnimationSpeed(2.0); // 2x faster attack animation
            state.getController().setAnimation(RawAnimation.begin()
                    .then("animation.anubis.attack_1", Animation.LoopType.PLAY_ONCE));
            return PlayState.CONTINUE;
        } else {
            // Reset animation speed to normal when not attacking
            state.getController().setAnimationSpeed(1.0);
        }

        // ===== PRIORITY 3: Special phase animations =====
        // Judgment idle animation
        if (phase == BossPhase.JUDGING && isJudging()) {
            state.getController().setAnimation(RawAnimation.begin()
                    .then("animation.anubis.judgement_idle", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }

        // Merciful/safe animation
        if (phase == BossPhase.MERCIFUL) {
            state.getController().setAnimation(RawAnimation.begin()
                    .then("animation.anubis.judgement_safe", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }

        // Awakening animation
        if (phase == BossPhase.AWAKENING) {
            state.getController().setAnimation(RawAnimation.begin()
                    .then("animation.anubis.idle", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }

        // Hovering (during judgment transition)
        // IMPORTANT: Only hover animation if we're actually in JUDGING phase
        // This prevents T-pose bug when transitioning JUDGING->ENRAGED with hovering flag delay
        if (isHovering() && phase == BossPhase.JUDGING) {
            state.getController().setAnimation(RawAnimation.begin()
                    .then("animation.anubis.judgement_idle", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }

        // ===== PRIORITY 4: Movement animations =====
        // Use multiple detection methods for reliability
        boolean isCurrentlyMoving = isActuallyMoving(state);
        
        if (isCurrentlyMoving) {
            // Reset grace period when moving
            movementGracePeriod = MOVEMENT_GRACE_TICKS;
            
            // Use running animation for enraged phase
            if (phase == BossPhase.ENRAGED) {
                state.getController().setAnimation(RawAnimation.begin()
                        .then("animation.anubis.running", Animation.LoopType.LOOP));
            } else {
                state.getController().setAnimation(RawAnimation.begin()
                        .then("animation.anubis.walking", Animation.LoopType.LOOP));
            }
            return PlayState.CONTINUE;
        } else if (movementGracePeriod > 0) {
            // Still in grace period - keep walking animation to prevent flicker
            movementGracePeriod--;
            
            if (phase == BossPhase.ENRAGED) {
                state.getController().setAnimation(RawAnimation.begin()
                        .then("animation.anubis.running", Animation.LoopType.LOOP));
            } else {
                state.getController().setAnimation(RawAnimation.begin()
                        .then("animation.anubis.walking", Animation.LoopType.LOOP));
            }
            return PlayState.CONTINUE;
        }

        // ===== PRIORITY 5: Idle animation (default) =====
        state.getController().setAnimation(RawAnimation.begin()
                .then("animation.anubis.idle", Animation.LoopType.LOOP));
        return PlayState.CONTINUE;
    }
    
    /**
     * Checks if the entity is actually moving using multiple detection methods.
     * Combines velocity check, GeckoLib's isMoving(), and limb swing for reliability.
     * This works on both client and server side.
     */
    private <T extends GeoAnimatable> boolean isActuallyMoving(AnimationState<T> s) {
        // Method 1: Check velocity (works on both sides)
        double velocityX = this.getVelocity().x;
        double velocityZ = this.getVelocity().z;
        double horizontalSpeed = Math.sqrt(velocityX * velocityX + velocityZ * velocityZ);
        if (horizontalSpeed > MOVEMENT_THRESHOLD) {
            return true;
        }
        
        // Method 2: Use GeckoLib's built-in movement detection
        if (s.isMoving()) {
            return true;
        }
        
        // Method 3: Check limb swing amount (most reliable for walking animations)
        // limbSwingAmount is > 0 when the entity is walking
        if (this.limbAnimator.getSpeed() > 0.01f) {
            return true;
        }
        
        return false;
    }

    /* -------------------------------------------------------------------- */
    /*  SOUNDS & DAMAGE HANDLING                                            */
    /* -------------------------------------------------------------------- */

    @Override
    protected SoundEvent getAmbientSound() {
        // Use breathing sounds for ambient - varies by phase
        return switch (currentPhase) {
            case ENRAGED -> com.ancientcurse.ModSounds.ANUBIS_RUNNING_BREATHING;
            case JUDGING, MERCIFUL, AWAKENING -> null; // Silent during special phases
            case DEAD -> null; // No breathing when dead
            default -> playRandomBreath(); // Normal breathing when idle/combat
        };
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        // Randomly select from 4 hurt voice variations
        return playRandomHurt();
    }

    /**
     * Play a random hurt voice variation
     */
    private SoundEvent playRandomHurt() {
        int variant = this.random.nextInt(4);
        return switch (variant) {
            case 0 -> com.ancientcurse.ModSounds.ANUBIS_HURT_1;
            case 1 -> com.ancientcurse.ModSounds.ANUBIS_HURT_2;
            case 2 -> com.ancientcurse.ModSounds.ANUBIS_HURT_3;
            default -> com.ancientcurse.ModSounds.ANUBIS_HURT_4;
        };
    }

    /**
     * Play a random breathing sound
     */
    private SoundEvent playRandomBreath() {
        return this.random.nextBoolean() 
            ? com.ancientcurse.ModSounds.ANUBIS_BREATH_1 
            : com.ancientcurse.ModSounds.ANUBIS_BREATH_2;
    }

    /**
     * Play a random howl sound
     */
    private void playRandomHowl() {
        SoundEvent howl = this.random.nextBoolean() 
            ? com.ancientcurse.ModSounds.ANUBIS_HOWL_1 
            : com.ancientcurse.ModSounds.ANUBIS_HOWL_2;
        this.getWorld().playSound(
            null, 
            this.getX(), 
            this.getY(), 
            this.getZ(), 
            howl,
            SoundCategory.HOSTILE,
            2.0F,  // Very loud for dramatic effect
            1.0F
        );
    }

    /**
     * Play a random evil laugh (when successfully hitting player)
     */
    private void playRandomEvilLaugh() {
        if (this.random.nextFloat() < 0.3f) { // 30% chance on hit
            SoundEvent laugh = this.random.nextBoolean() 
                ? com.ancientcurse.ModSounds.ANUBIS_EVIL_LAUGH_1 
                : com.ancientcurse.ModSounds.ANUBIS_EVIL_LAUGH_2;
            this.getWorld().playSound(
                null, 
                this.getX(), 
                this.getY(), 
                this.getZ(), 
                laugh,
                SoundCategory.HOSTILE,
                1.5F,
                1.0F
            );
        }
    }

    /**
     * Play a random whoosh sound for attacks
     */
    private void playRandomWhoosh() {
        SoundEvent whoosh = this.random.nextBoolean() 
            ? com.ancientcurse.ModSounds.ANUBIS_WHOOSH_1 
            : com.ancientcurse.ModSounds.ANUBIS_WHOOSH_2;
        this.getWorld().playSound(
            null, 
            this.getX(), 
            this.getY(), 
            this.getZ(), 
            whoosh,
            SoundCategory.HOSTILE,
            1.0F,
            1.0F
        );
    }

    @Override
    protected SoundEvent getDeathSound() {
        // Use one of the hurt sounds for death (or create a dedicated death sound)
        return com.ancientcurse.ModSounds.ANUBIS_HURT_4;
    }

    @Override
    public boolean cannotDespawn() {
        // Boss entities should never despawn
        return true;
    }

    /**
     * Update and play step sounds based on movement
     * Big boss = big footsteps!
     */
    private void updateStepSounds() {
        // Don't play step sounds if not moving or if hovering
        if (!this.isOnGround() || isHovering()) {
            return;
        }

        // Check if entity is moving (lower threshold for more responsive footsteps)
        double velocitySquared = this.getVelocity().horizontalLengthSquared();
        boolean isMoving = velocitySquared > 0.001;

        if (!isMoving) {
            stepSoundCooldown = 0; // Reset cooldown when not moving
            return;
        }

        // Decrement cooldown
        if (stepSoundCooldown > 0) {
            stepSoundCooldown--;
            return;
        }

        // Determine if running (enraged or high velocity)
        boolean isRunning = currentPhase == BossPhase.ENRAGED || velocitySquared > 0.05;

        // Play appropriate step sound
        if (isRunning) {
            // Heavy, powerful steps for running/enraged
            this.getWorld().playSound(
                null,
                this.getX(),
                this.getY(),
                this.getZ(),
                com.ancientcurse.ModSounds.ANUBIS_STEP_HEAVY,
                SoundCategory.HOSTILE,
                1.2F,  // Louder for big boss
                0.9F + this.random.nextFloat() * 0.2F  // Slight pitch variation
            );
            stepSoundCooldown = STEP_SOUND_INTERVAL_RUN;
        } else {
            // Normal walking steps - randomly select from available footstep sounds
            SoundEvent stepSound;
            int soundChoice = this.random.nextInt(2);
            if (soundChoice == 0) {
                stepSound = com.ancientcurse.ModSounds.ANUBIS_STEP;
            } else {
                stepSound = com.ancientcurse.ModSounds.ANUBIS_FOOTSTEP_1;
            }

            this.getWorld().playSound(
                null,
                this.getX(),
                this.getY(),
                this.getZ(),
                stepSound,
                SoundCategory.HOSTILE,
                1.0F,
                0.9F + this.random.nextFloat() * 0.2F  // Slight pitch variation (0.9-1.1)
            );
            stepSoundCooldown = STEP_SOUND_INTERVAL_WALK;
        }
    }

    /**
     * Starts a melee attack - triggers the animation, plays whoosh sound, and schedules delayed damage.
     * Damage is dealt when the animation's swing actually connects (ATTACK_1_DAMAGE_TICK ticks later).
     * @param target The entity to damage when the attack connects
     */
    public void startMeleeAttack(LivingEntity target) {
        if (!this.getWorld().isClient) {
            // Set attacking state to trigger animation
            isPlayingAttackAnimation = true;
            attackAnimationTicksRemaining = ATTACK_1_DURATION_TICKS;
            setAttacking(true); // Sync to client for animation
            
            // Schedule delayed damage - will be dealt when swing connects
            pendingAttackTarget = target;
            pendingDamageTick = ATTACK_1_DAMAGE_TICK;

            // Schedule layered whoosh sound to play 0.2 seconds into attack
            whooshLayerTick = WHOOSH_LAYER_DELAY;

            // Face target before lunging
            this.getLookControl().lookAt(target, 30.0F, 30.0F);

            // Lunge forward toward target to close distance during attack
            Vec3d direction = target.getPos().subtract(this.getPos()).normalize();
            double lungeSpeed = currentPhase == BossPhase.ENRAGED ? 0.5 : 0.35;
            this.setVelocity(direction.x * lungeSpeed, 0.15, direction.z * lungeSpeed);
            this.velocityModified = true;

            // Play attack sounds immediately when attack starts
            playRandomWhoosh(); // Wind-up whoosh

            // Play breath sound during attack - LOUD and aggressive
            SoundEvent breath = this.random.nextBoolean()
                ? com.ancientcurse.ModSounds.ANUBIS_BREATH_1
                : com.ancientcurse.ModSounds.ANUBIS_BREATH_2;
            this.getWorld().playSound(
                null,
                this.getX(),
                this.getY(),
                this.getZ(),
                breath,
                SoundCategory.HOSTILE,
                1.8F, // Much louder - 80% louder than before
                0.8F + this.random.nextFloat() * 0.3F // Pitch range 0.8-1.1
            );
        }
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);

        if (hit && target instanceof LivingEntity livingTarget && !this.getWorld().isClient) {
            // Strong knockback effect to make hits feel impactful
            Vec3d knockbackDirection = target.getPos().subtract(this.getPos()).normalize();
            double knockbackStrength = currentPhase == BossPhase.ENRAGED ? 1.2 : 0.8;
            livingTarget.setVelocity(
                knockbackDirection.x * knockbackStrength,
                0.4, // Upward component
                knockbackDirection.z * knockbackStrength
            );
            livingTarget.velocityModified = true;

            // ENRAGED phase: Deal extra damage and apply effects
            if (currentPhase == BossPhase.ENRAGED) {
                // Apply Wither effect for 3 seconds (god of death curse)
                livingTarget.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                    net.minecraft.entity.effect.StatusEffects.WITHER, 60, 0
                ));
                // 30% chance to apply Blindness (divine judgment)
                if (this.random.nextFloat() < 0.3f) {
                    livingTarget.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                        net.minecraft.entity.effect.StatusEffects.BLINDNESS, 40, 0
                    ));
                }
            }
        }

        return hit;
    }

    /**
     * Handles damage to Anubis with special phase transitions and tactical reactions.
     */
    private boolean handleDamage(DamageSource source, float amount) {
        // If attacked by a player (including ranged), ensure we target them
        if (source.getAttacker() instanceof PlayerEntity player) {
            // Sometimes play evil laugh when player hits us
            if (!this.getWorld().isClient) {
                playRandomEvilLaugh();
            }
            
            // IMMEDIATE COMBAT: If dormant or awakening, skip straight to combat when attacked
            if (currentPhase == BossPhase.DORMANT || currentPhase == BossPhase.AWAKENING) {
                setBossPhase(BossPhase.COMBAT);
                setTarget(player);
                // Play howl to signal combat start
                playRandomHowl();
            }
            
            // Always target the attacker if we don't have a target
            if (this.getTarget() == null && currentPhase != BossPhase.MERCIFUL) {
                this.setTarget(player);
            }

            // If player attacks during judgment, become enraged
            if (currentPhase == BossPhase.JUDGING) {
                setBossPhase(BossPhase.ENRAGED);
                setTarget(player);
                setHovering(false);
                setJudging(false);
                targetHoverHeight = 0.0F;
            }

            // If player attacks during merciful phase, they lose protection and Anubis becomes enraged
            if (currentPhase == BossPhase.MERCIFUL) {
                if (safePlayers.contains(player.getUuid())) {
                    safePlayers.remove(player.getUuid());
                    setPlayerSafe(false);
                    amount *= 0.1F; // Still reduce this hit's damage since they were safe
                }
                // Transition to enraged for betraying mercy
                setBossPhase(BossPhase.ENRAGED);
                setTarget(player);
            }
        }
        
        // TACTICAL REACTIVITY: Evasive Leap
        // 30% chance to leap away when hit to reset combat spacing
        if (source.getAttacker() instanceof LivingEntity attacker && !this.getWorld().isClient) {
             if (this.random.nextFloat() < 0.3f && this.isOnGround()) {
                 performTacticalLeap(attacker);
             }
        }

        return true; // Damage will be applied by the calling method
    }
    
    private void performTacticalLeap(LivingEntity attacker) {
        Vec3d awayDir = this.getPos().subtract(attacker.getPos()).normalize();
        // Leap backwards/sideways with force
        this.addVelocity(awayDir.x * 1.2, 0.6, awayDir.z * 1.2);
        this.velocityModified = true;
        // Play evil laugh when dodging
        if (!this.getWorld().isClient) {
            playRandomEvilLaugh();
        }
    }

    @Override
    public void addVelocity(double deltaX, double deltaY, double deltaZ) {
        // Limit upward velocity to prevent Anubis from flying too high when hit
        // This is especially important for abilities like the Mace of Horus
        if (deltaY > 0.3) {
            deltaY = 0.3; // Cap upward velocity at 0.3 blocks/tick
        }

        // Also reduce horizontal knockback for this boss
        deltaX *= 0.2;
        deltaZ *= 0.2;

        super.addVelocity(deltaX, deltaY, deltaZ);
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        // Don't call super.onDeath() yet - we want to delay the death
        // Start the epic death sequence instead of dying immediately
        if (!isDying) {
            isDying = true;
            deathDamageSource = damageSource; // Store for loot drops later
            dataTracker.set(IS_DYING, true); // Sync to client for animation
            deathAnimationTicks = 0;
            setBossPhase(BossPhase.DEAD);
            
            // Clear boss bar
            bossBar.clearPlayers();
            
            // Stop all AI and movement
            this.setTarget(null);
            this.setVelocity(0, 0, 0);
            this.setNoGravity(true); // Freeze in place during death
            
            // Play initial death sound
            this.getWorld().playSound(
                null, this.getX(), this.getY(), this.getZ(),
                com.ancientcurse.ModSounds.ANUBIS_HURT_4, SoundCategory.HOSTILE,
                2.0F, 0.6F // Deep, dramatic
            );
            
            // Make invulnerable during death animation
            // (handled by damage() method checking isDying)
        }
        // Note: super.onDeath() is NOT called here - it will be called in finalizeDeathAndDropLoot()
    }
    
    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        
        // Ensure boss bar is cleared when entity is removed for any reason
        if (!this.getWorld().isClient && this.bossBar != null) {
            this.bossBar.clearPlayers();
        }
    }
    
    @Override
    public boolean damage(DamageSource source, float amount) {
        // Invulnerable during death animation
        if (isDying) {
            return false;
        }
        
        // Handle special damage logic (phase transitions, etc.)
        handleDamage(source, amount);
        
        return super.damage(source, amount);
    }

    /* -------------------------------------------------------------------- */
    /*  ACCESSORS & UTILITIES                                               */
    /* -------------------------------------------------------------------- */

    public BossPhase getBossPhase() { 
        return BossPhase.fromId(dataTracker.get(BOSS_PHASE)); 
    }
    
    public void setBossPhase(BossPhase phase) { 
        this.currentPhase = phase;
        dataTracker.set(BOSS_PHASE, phase.getId()); 
        phaseTimer = 0;
    }

    public boolean isHovering() { return dataTracker.get(IS_HOVERING); }
    public void setHovering(boolean hovering) { dataTracker.set(IS_HOVERING, hovering); }

    public boolean isJudging() { return dataTracker.get(IS_JUDGING); }
    public void setJudging(boolean judging) { dataTracker.set(IS_JUDGING, judging); }

    public boolean isSkyYelling() { return dataTracker.get(IS_SKY_YELLING); }
    public void setSkyYelling(boolean skyYelling) { dataTracker.set(IS_SKY_YELLING, skyYelling); }

    public boolean isPlayerSafe() { return dataTracker.get(PLAYER_IS_SAFE); }
    public void setPlayerSafe(boolean safe) { dataTracker.set(PLAYER_IS_SAFE, safe); }
    
    public boolean isAttacking() { return dataTracker.get(IS_ATTACKING); }
    public void setAttacking(boolean attacking) { dataTracker.set(IS_ATTACKING, attacking); }

    @Override 
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    
    @Override 
    public double getTick(Object unused) { return this.age; }
}
