package com.ancientcurse.entity;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.ModEntities;
import com.ancientcurse.ModSounds;
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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
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
    private static final int ATTACK_MAGIC_BALL = 1;
    private static final int ATTACK_SCROLL_BLAST = 2;
    private static final int ATTACK_TIME_BEND = 3;
    private static final int ATTACK_ENTITY_SUMMON = 4;
    private static final int ATTACK_MELEE = 5;
    
    private static final int MAX_ATTACK_COOLDOWN = 120; // 6 seconds
    private static final int MAX_SUMMONING_COOLDOWN = 400; // 20 seconds
    private static final int TIME_MAGIC_DURATION = 200; // 10 seconds
    
    // Animation durations based on actual animation lengths (in ticks, 20 ticks = 1 second)
    private static final int SPAWN_ANIMATION_DURATION = 100; // 5 seconds for entity_spawn
    private static final int ATTACK_1_ANIMATION_DURATION = 60; // 3 seconds for attack_1 (magic ball & melee)
    private static final int ATTACK_2_ANIMATION_DURATION = 80; // 4 seconds for attack_2 (scroll blast)
    private static final int TIME_BEND_ANIMATION_DURATION = 120; // 6 seconds for time_bend
    private static final int SUMMON_ANIMATION_DURATION = 100; // 5 seconds for entity_spawn (summon)
    public static final int SPAWN_TRANSITION_DURATION = 100; // 5 seconds for spawn transition effects
    
    /* ---------- FIELDS ---------- */
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private ServerBossBar bossBar;
    private int magicCastingTicks = 0;
    private boolean hasSpawned = false;
    private int timeMagicTicks = 0;
    private boolean initialized = false;
    private int spawnTransitionTicks = 0;

    // Animation tracking
    private int attackAnimationTicks = 0;
    private boolean shouldBeGrounded = false;

    // Combat state management
    private int combatTimeout = 0;
    private static final int MAX_COMBAT_TIMEOUT = 100; // 5 seconds

    // Phase system
    private boolean hasEnteredPhase2 = false;
    private boolean hasEnteredPhase3 = false;

    // Performance caching
    private PlayerEntity cachedTarget;
    private int targetCacheTime = 0;
    private float lastHealthPercentage = 1.0f; // Track last health percentage for boss bar updates
    private int bossBarUpdateCooldown = 0; // Cooldown for boss bar player list updates
    
    public ThothEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 100; // Boss-level XP
        
        // Initialize boss bar only on server side
        if (!world.isClient) {
            try {
                this.bossBar = new ServerBossBar(
                    Text.translatable("entity.ancientcurse.thoth"), 
                    BossBar.Color.PURPLE, 
                    BossBar.Style.PROGRESS
                );
            } catch (Exception e) {
                AncientCurse.LOGGER.warn("Failed to create boss bar for Thoth: " + e.getMessage());
                this.bossBar = null;
            }
        }
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
        this.goalSelector.add(1, new ThothTimeBendGoal(this));
        this.goalSelector.add(2, new ThothSummonEntitiesGoal(this));
        this.goalSelector.add(3, new ThothMagicAttackGoal(this));
        this.goalSelector.add(4, new MeleeAttackGoal(this, 1.0, false) {
            @Override
            public boolean canStart() {
                // Only use vanilla melee when very close and not doing special attacks
                return super.canStart() && 
                       ThothEntity.this.squaredDistanceTo(ThothEntity.this.getTarget()) <= 256 && // 16 block range to match strategic attack
                       ThothEntity.this.dataTracker.get(ATTACK_COOLDOWN) == 0 &&
                       ThothEntity.this.attackAnimationTicks == 0;
            }
        }); // Reduced speed to prevent jittery movement
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.6)); // Reduced speed
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 16.0f));
        this.goalSelector.add(7, new LookAroundGoal(this));
        
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }
    
    /* ---------- TICK ---------- */
    @Override
    public void tick() {
        super.tick();
        
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
        
        // Handle attack animation timer
        if (attackAnimationTicks > 0) {
            attackAnimationTicks--;
            if (attackAnimationTicks == 0) {
                // Reset attack state when animation completes
                dataTracker.set(ATTACK_STATE, ATTACK_NONE);
                dataTracker.set(IS_READING, false); // Reset reading state
            }
        }
        
        // Handle cooldowns
        int attackCd = dataTracker.get(ATTACK_COOLDOWN);
        if (attackCd > 0) dataTracker.set(ATTACK_COOLDOWN, attackCd - 1);
        
        int summonCd = dataTracker.get(SUMMONING_COOLDOWN);
        if (summonCd > 0) dataTracker.set(SUMMONING_COOLDOWN, summonCd - 1);
        
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
                // Smoothly transition to ground-based movement
                this.setNoGravity(false);
            } else if (hasSpawned) {
                // Return to floating when not in combat (but only after spawn)
                this.setNoGravity(true);
            }
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
        
        // Play spawn sound with fallback
        try {
            this.playSound(ModSounds.THOTH_AMBIENT, 2.0f, 0.8f);
        } catch (Exception e) {
            this.playSound(SoundEvents.ENTITY_WITCH_AMBIENT, 1.5f, 1.2f);
        }
    }
    
    private void handleTimeMagic() {
        timeMagicTicks++;
        
        if (!this.getWorld().isClient) {
            // Enhanced time magic effects
            Box area = new Box(this.getBlockPos()).expand(16);
            List<LivingEntity> entities = this.getWorld().getNonSpectatingEntities(LivingEntity.class, area);
            
            for (LivingEntity entity : entities) {
                if (entity != this && entity instanceof PlayerEntity) {
                    // More powerful time effects
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 3, false, false));
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 40, 2, false, false));
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 40, 1, false, false));
                    
                    // Chance to "freeze" movement
                    if (this.random.nextFloat() < 0.2f) {
                        entity.setVelocity(entity.getVelocity().multiply(0.1));
                    }
                    
                    // Visual distortion effect
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 60, 0, false, false));
                }
            }
            
            // Heal Thoth during time magic - reduced healing
            if (timeMagicTicks % 40 == 0) { // Changed from every 20 ticks to every 40 ticks
                this.heal(1.5f); // Reduced from 3.0f to 1.5f
            }
        }
        
        // Enhanced time distortion particles (both client and server)
        if (timeMagicTicks % 4 == 0) { // Reduced frequency
            for (int i = 0; i < 6; i++) { // Reduced count
                Vec3d particlePos = this.getPos().add(
                    (this.random.nextDouble() - 0.5) * 10,
                    this.random.nextDouble() * 5,
                    (this.random.nextDouble() - 0.5) * 10
                );
                
                if (!this.getWorld().isClient) {
                    ((ServerWorld)this.getWorld()).spawnParticles(
                        ParticleTypes.ENCHANT,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0.1, 0.1, 0.1, 0.05
                    );
                    
                    // Additional portal particles
                    ((ServerWorld)this.getWorld()).spawnParticles(
                        ParticleTypes.REVERSE_PORTAL,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0.2, 0.2, 0.2, 0.1
                    );
                } else {
                    this.getWorld().addParticle(
                        ParticleTypes.ENCHANT,
                        particlePos.x, particlePos.y, particlePos.z,
                        0, 0.05, 0
                    );
                    
                    this.getWorld().addParticle(
                        ParticleTypes.REVERSE_PORTAL,
                        particlePos.x, particlePos.y, particlePos.z,
                        0, 0.1, 0
                    );
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

                // Remove players who are too far away
                this.bossBar.getPlayers().removeIf(player -> {
                    return this.squaredDistanceTo(player) > 64 * 64;
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
        
        // Play spawn sound with fallback
        try {
            this.playSound(ModSounds.THOTH_SPAWN, 2.0f, 0.8f);
        } catch (Exception e) {
            // Fallback to vanilla sound if custom sound fails
            this.playSound(SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 1.2f);
            AncientCurse.LOGGER.warn("Thoth spawn sound failed, using fallback: " + e.getMessage());
        }
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
        try {
            this.playSound(ModSounds.THOTH_SUMMON, 3.0f, 0.5f);
        } catch (Exception e) {
            this.playSound(SoundEvents.ENTITY_WITHER_SPAWN, 2.0f, 0.7f);
        }
        
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
            List<PlayerEntity> affectedPlayers = this.getWorld().getNonSpectatingEntities(
                PlayerEntity.class, timeArea);
            
            for (PlayerEntity player : affectedPlayers) {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    serverPlayer.sendMessage(
                        Text.literal("§5§lTime itself bends to Thoth's will!"), 
                        true
                    );
                    
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 100, 0, false, false));
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 20, 0, false, false));
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
        
        // Play sounds with fallback
        try {
            this.playSound(ModSounds.THOTH_ATTACK_TIME_BEND, 3.0f, 0.3f);
            this.playSound(ModSounds.THOTH_AMBIENT, 2.0f, 0.5f);
        } catch (Exception e) {
            this.playSound(SoundEvents.BLOCK_PORTAL_AMBIENT, 2.5f, 0.2f);
            this.playSound(SoundEvents.ENTITY_WITCH_AMBIENT, 1.8f, 0.7f);
        }
        
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
        
        // Phase 3 specific behavior: prioritize time bend and powerful attacks
        if (hasEnteredPhase3) {
            if (!dataTracker.get(IS_CASTING_TIME_MAGIC) && this.random.nextFloat() < 0.4f) {
                performTimeBend();
            } else if (distance <= 16) { // Close range melee
                performMeleeAttack();
            } else if (distance > 100) {
                performMagicBallAttack();
            } else {
                performScrollBlast();
            }
            return;
        }

        // Phase 2 specific behavior: more aggressive, mix attacks
        if (hasEnteredPhase2) {
            if (healthPercent < 0.25f && !dataTracker.get(IS_CASTING_TIME_MAGIC)) {
                performTimeBend();
            } else if (dataTracker.get(SUMMONING_COOLDOWN) == 0 && this.random.nextFloat() < 0.3f) {
                summonEntities();
            } else if (distance <= 16) { // Close range melee
                performMeleeAttack();
            } else if (distance > 144) {
                performMagicBallAttack();
            } else if (distance < 64) {
                performScrollBlast();
            } else {
                performMagicBallAttack(); // Default for mid-range
            }
            return;
        }

        // Phase 1 behavior: balanced attacks with melee priority for close range
        if (healthPercent < 0.25f && !dataTracker.get(IS_CASTING_TIME_MAGIC)) {
            performTimeBend();
        } else if (distance <= 16) { // Close range melee - highest priority for close combat
            performMeleeAttack();
        } else if (distance > 144) {
            performMagicBallAttack();
        } else if (distance < 64 && healthPercent > 0.3f) {
            performScrollBlast();
        } else if (healthPercent < 0.5f && dataTracker.get(SUMMONING_COOLDOWN) == 0) {
            summonEntities();
        } else {
            performMagicBallAttack();
        }
    }
    
    private void performTimePulse() {
        Box area = new Box(this.getBlockPos()).expand(20);
        List<LivingEntity> entities = this.getWorld().getNonSpectatingEntities(LivingEntity.class, area);
        
        for (LivingEntity entity : entities) {
            if (entity != this && entity instanceof PlayerEntity) {
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 5, false, false));
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 40, 3, false, false));
                entity.setVelocity(Vec3d.ZERO);
            }
        }
        
        if (!this.getWorld().isClient) {
            for (int i = 0; i < 20; i++) {
                Vec3d particlePos = this.getPos().add(
                    (this.random.nextDouble() - 0.5) * 24,
                    this.random.nextDouble() * 6,
                    (this.random.nextDouble() - 0.5) * 24
                );
                ((ServerWorld)this.getWorld()).spawnParticles(
                    ParticleTypes.REVERSE_PORTAL,
                    particlePos.x, particlePos.y, particlePos.z,
                    3, 0.2, 0.2, 0.2, 0.1
                );
            }
        }
        
        this.playSound(ModSounds.THOTH_ATTACK_TIME_BEND, 1.0f, 0.3f);
    }
    
    /* ---------- ATTACK METHODS ---------- */
    public void performMagicBallAttack() {
        if (dataTracker.get(ATTACK_COOLDOWN) > 0 || attackAnimationTicks > 0) return;
        
        dataTracker.set(ATTACK_STATE, ATTACK_MAGIC_BALL);
        dataTracker.set(ATTACK_COOLDOWN, MAX_ATTACK_COOLDOWN);
        attackAnimationTicks = ATTACK_1_ANIMATION_DURATION;
        
        // Play attack sound with fallback
        try {
            this.playSound(ModSounds.THOTH_ATTACK_MAGIC_BALL, 1.5f, 1.0f);
        } catch (Exception e) {
            this.playSound(SoundEvents.ENTITY_WITCH_THROW, 1.5f, 1.0f);
        }
        
        LivingEntity target = this.getTarget();
        if (target != null && !this.getWorld().isClient) {
            // Create and launch the magic ball projectile
            ThothMagicBallEntity magicBall = new ThothMagicBallEntity(this.getWorld(), this);
            
            // Position the projectile in front of Thoth
            Vec3d direction = target.getPos().subtract(this.getPos()).normalize();
            Vec3d projectilePos = this.getPos().add(0, this.getHeight() * 0.8, 0).add(direction.multiply(1.5));
            
            magicBall.setPosition(projectilePos.x, projectilePos.y, projectilePos.z);
            
            // Set velocity towards target (slower than normal projectiles)
            Vec3d velocity = direction.multiply(0.8); // Slow magical projectile
            magicBall.setVelocity(velocity.x, velocity.y + 0.1, velocity.z);
            
            // Spawn the projectile
            this.getWorld().spawnEntity(magicBall);
            
            // Spawn initial casting particles
            ((ServerWorld)this.getWorld()).spawnParticles(
                ParticleTypes.WITCH,
                projectilePos.x, projectilePos.y, projectilePos.z,
                8, 0.3, 0.3, 0.3, 0.1
            );
        }
        
        // Play sound with fallback
        try {
            this.playSound(ModSounds.THOTH_ATTACK_MAGIC_BALL, 1.5f, 1.0f);
        } catch (Exception e) {
            this.playSound(SoundEvents.ENTITY_BLAZE_SHOOT, 1.2f, 1.2f);
        }
    }
    
    public void performScrollBlast() {
        if (dataTracker.get(ATTACK_COOLDOWN) > 0 || attackAnimationTicks > 0) return;
        
        dataTracker.set(ATTACK_STATE, ATTACK_SCROLL_BLAST);
        dataTracker.set(ATTACK_COOLDOWN, MAX_ATTACK_COOLDOWN);
        dataTracker.set(IS_READING, true);
        attackAnimationTicks = ATTACK_2_ANIMATION_DURATION;
        
        if (!this.getWorld().isClient) {
            Box area = new Box(this.getBlockPos()).expand(8);
            List<LivingEntity> entities = this.getWorld().getNonSpectatingEntities(LivingEntity.class, area);
            
            for (LivingEntity entity : entities) {
                if (entity != this && entity instanceof PlayerEntity) {
                    entity.damage(this.getDamageSources().mobAttack(this), 8.0f);
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 200, 1));
                    
                    Vec3d direction = entity.getPos().subtract(this.getPos()).normalize();
                    entity.addVelocity(direction.x * 0.8, 0.5, direction.z * 0.8);
                }
            }
        }
        
        // Enhanced particles - server-side only for proper synchronization
        if (!this.getWorld().isClient) {
            for (int i = 0; i < 15; i++) {
                Vec3d particlePos = this.getPos().add(
                    (this.random.nextDouble() - 0.5) * 12,
                    this.random.nextDouble() * 6,
                    (this.random.nextDouble() - 0.5) * 12
                );
                
                ((ServerWorld)this.getWorld()).spawnParticles(
                    ParticleTypes.EXPLOSION,
                    particlePos.x, particlePos.y, particlePos.z,
                    1, 0.2, 0.2, 0.2, 0.1
                );
                
                // Add additional witch particles for magical effect
                ((ServerWorld)this.getWorld()).spawnParticles(
                    ParticleTypes.WITCH,
                    particlePos.x, particlePos.y, particlePos.z,
                    2, 0.3, 0.3, 0.3, 0.05
                );
            }
        }
        
        // Play sound with fallback
        try {
            this.playSound(ModSounds.THOTH_ATTACK_SCROLL_BLAST, 2.0f, 0.8f);
        } catch (Exception e) {
            this.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, 1.5f, 1.1f);
        }
    }
    
    public void performTimeBend() {
        if (dataTracker.get(ATTACK_COOLDOWN) > 0 || attackAnimationTicks > 0) return;
        
        dataTracker.set(ATTACK_STATE, ATTACK_TIME_BEND);
        dataTracker.set(ATTACK_COOLDOWN, MAX_ATTACK_COOLDOWN * 2);
        dataTracker.set(IS_CASTING_TIME_MAGIC, true);
        timeMagicTicks = 0;
        attackAnimationTicks = TIME_BEND_ANIMATION_DURATION; // Longer for time magic
        
        // Immediate time pulse effect
        performTimePulse();
        
        // Play time magic sound with fallback
        try {
            this.playSound(ModSounds.THOTH_ATTACK_TIME_BEND, 2.0f, 0.5f);
        } catch (Exception e) {
            this.playSound(SoundEvents.BLOCK_PORTAL_AMBIENT, 1.5f, 0.3f);
        }
    }
    
    public void summonEntities() {
        if (dataTracker.get(SUMMONING_COOLDOWN) > 0 || attackAnimationTicks > 0) return;
        
        dataTracker.set(ATTACK_STATE, ATTACK_ENTITY_SUMMON);
        dataTracker.set(SUMMONING_COOLDOWN, MAX_SUMMONING_COOLDOWN);
        attackAnimationTicks = SUMMON_ANIMATION_DURATION;
        
        if (!this.getWorld().isClient) {
            AncientCurse.LOGGER.info("Thoth attempted to summon scarab beetles");
        }
        
        // Play sound with fallback
        try {
            this.playSound(ModSounds.THOTH_SUMMON, 2.0f, 1.0f);
        } catch (Exception e) {
            this.playSound(SoundEvents.ENTITY_EVOKER_CAST_SPELL, 1.8f, 0.8f);
        }
    }
    
    public void performMeleeAttack() {
        if (dataTracker.get(ATTACK_COOLDOWN) > 0 || attackAnimationTicks > 0) return;
        
        dataTracker.set(ATTACK_STATE, ATTACK_MELEE);
        dataTracker.set(ATTACK_COOLDOWN, MAX_ATTACK_COOLDOWN / 2); // Shorter cooldown for melee
        attackAnimationTicks = ATTACK_1_ANIMATION_DURATION;
        
        LivingEntity target = this.getTarget();
        if (target != null && !this.getWorld().isClient) {
            // Direct melee damage
            target.damage(this.getDamageSources().mobAttack(this), 18.0f); // Higher damage for melee
            
            // Knockback effect
            Vec3d direction = target.getPos().subtract(this.getPos()).normalize();
            target.addVelocity(direction.x * 1.2, 0.6, direction.z * 1.2);
            
            // Brief weakness effect
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 0));
        }
        
        // Melee impact particles - server-side only for proper synchronization
        if (!this.getWorld().isClient) {
            for (int i = 0; i < 12; i++) {
                Vec3d particlePos = this.getPos().add(
                    (this.random.nextDouble() - 0.5) * 3,
                    this.random.nextDouble() * 2,
                    (this.random.nextDouble() - 0.5) * 3
                );
                
                ((ServerWorld)this.getWorld()).spawnParticles(
                    ParticleTypes.CRIT,
                    particlePos.x, particlePos.y, particlePos.z,
                    1, 0.3, 0.3, 0.3, 0.2
                );
                
                // Add magical energy particles for melee attacks
                ((ServerWorld)this.getWorld()).spawnParticles(
                    ParticleTypes.ENCHANT,
                    particlePos.x, particlePos.y, particlePos.z,
                    2, 0.2, 0.2, 0.2, 0.1
                );
            }
        }
        
        // Play melee sound with fallback
        try {
            this.playSound(ModSounds.THOTH_ATTACK_MAGIC_BALL, 1.0f, 0.8f); // Reuse magic ball sound
        } catch (Exception e) {
            this.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 1.5f, 0.9f);
        }
    }
    
    /* ---------- CUSTOM GOALS ---------- */
    public static class ThothMagicAttackGoal extends Goal {
        private final ThothEntity thoth;
        private int attackTimer = 0;
        private LivingEntity target;
        
        public ThothMagicAttackGoal(ThothEntity thoth) {
            this.thoth = thoth;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }
        
        @Override
        public boolean canStart() {
            this.target = thoth.getTarget();
            return thoth.isAlive() &&
                   target != null &&
                   thoth.dataTracker.get(ATTACK_COOLDOWN) == 0 &&
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
            thoth.shouldBeGrounded = true;
        }
        
        @Override
        public void tick() {
            if (target != null && target.isAlive()) {
                thoth.getLookControl().lookAt(target, 30.0f, 30.0f);
                
                double distanceSq = thoth.squaredDistanceTo(target);
                if (distanceSq > 64) {
                    thoth.getNavigation().startMovingTo(target, 1.2);
                } else {
                    thoth.getNavigation().stop();
                }
                
                if (++attackTimer >= 40 && thoth.attackAnimationTicks == 0) {
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
        }
        
        @Override
        public boolean canStart() {
            return thoth.getTarget() != null && 
                   thoth.dataTracker.get(SUMMONING_COOLDOWN) == 0 &&
                   thoth.attackAnimationTicks == 0 &&
                   thoth.getHealth() < thoth.getMaxHealth() * 0.5f;
        }
        
        @Override
        public void start() {
            thoth.summonEntities();
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
                   thoth.attackAnimationTicks == 0 &&
                   thoth.getHealth() < thoth.getMaxHealth() * 0.3f &&
                   !thoth.dataTracker.get(IS_CASTING_TIME_MAGIC);
        }
        
        @Override
        public void start() {
            thoth.performTimeBend();
        }
    }
    
    /* ---------- GECKOLIB ANIMATIONS ---------- */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }
    
    private <T extends GeoEntity> PlayState predicate(AnimationState<T> state) {
        int attackState = this.dataTracker.get(ATTACK_STATE);
        boolean isInCombat = this.dataTracker.get(IS_IN_COMBAT);
        boolean isCastingTime = this.dataTracker.get(IS_CASTING_TIME_MAGIC);
        boolean hasBeenInCombat = this.dataTracker.get(HAS_BEEN_IN_COMBAT);
        boolean isReading = this.dataTracker.get(IS_READING);
        
        // Priority 1: Spawn animation (highest priority) - let it complete fully
        if (spawnTransitionTicks > 0 && !hasSpawned) {
            state.getController().setAnimation(RawAnimation.begin().then("animation.thoth.entity_spawn", Animation.LoopType.PLAY_ONCE));
            return PlayState.CONTINUE;
        }
        
        // Priority 2: Attack animations (when actively attacking) - let each complete fully
        if (attackAnimationTicks > 0) {
            switch (attackState) {
                case ATTACK_MAGIC_BALL:
                    // Magic ball projectile attack - uses attack_1 animation
                    state.getController().setAnimation(RawAnimation.begin().then("animation.thoth.attack_1", Animation.LoopType.PLAY_ONCE));
                    return PlayState.CONTINUE;
                    
                case ATTACK_MELEE:
                    // Melee attack - also uses attack_1 animation but with different effects
                    state.getController().setAnimation(RawAnimation.begin().then("animation.thoth.attack_1", Animation.LoopType.PLAY_ONCE));
                    return PlayState.CONTINUE;
                    
                case ATTACK_SCROLL_BLAST:
                    // Scroll reading blast attack - uses attack_2 animation
                    state.getController().setAnimation(RawAnimation.begin().then("animation.thoth.attack_2", Animation.LoopType.PLAY_ONCE));
                    return PlayState.CONTINUE;
                    
                case ATTACK_TIME_BEND:
                    // Time magic - uses time_bend animation (looping during effect)
                    state.getController().setAnimation(RawAnimation.begin().then("animation.thoth.time_bend", Animation.LoopType.LOOP));
                    return PlayState.CONTINUE;
                    
                case ATTACK_ENTITY_SUMMON:
                    // Entity summoning - uses entity_spawn animation (dramatic summoning)
                    state.getController().setAnimation(RawAnimation.begin().then("animation.thoth.entity_spawn", Animation.LoopType.PLAY_ONCE));
                    return PlayState.CONTINUE;
            }
        }
        
        // Priority 3: Ongoing time magic casting (continuous effect)
        if (isCastingTime && attackAnimationTicks == 0) {
            state.getController().setAnimation(RawAnimation.begin().then("animation.thoth.time_bend", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }
        
        // Priority 4: Reading behavior (when peaceful and reading tome)
        if (isReading && !hasBeenInCombat && !isInCombat) {
            // Peaceful reading - floating with tome (idle animation)
            state.getController().setAnimation(RawAnimation.begin().then("animation.thoth.idle", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }
        
        // Priority 5: Movement animations
        if (state.isMoving() && attackAnimationTicks == 0) {
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
    public boolean isMagicBallAttack() { return dataTracker.get(ATTACK_STATE) == ATTACK_MAGIC_BALL; }
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
        try {
            return ModSounds.THOTH_AMBIENT;
        } catch (Exception e) {
            return SoundEvents.ENTITY_WITCH_AMBIENT;
        }
    }
    
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        try {
            return ModSounds.THOTH_HURT;
        } catch (Exception e) {
            return SoundEvents.ENTITY_WITCH_HURT;
        }
    }
    
    @Override
    protected SoundEvent getDeathSound() {
        try {
            return ModSounds.THOTH_DEATH;
        } catch (Exception e) {
            return SoundEvents.ENTITY_WITCH_DEATH;
        }
    }
    
    /* ---------- BOSS BEHAVIOR ---------- */
    @Override
    public boolean damage(DamageSource source, float amount) {
        // If attacked by a player (including ranged), ensure we target them
        if (source.getAttacker() instanceof PlayerEntity player && this.getTarget() == null) {
            this.setTarget(player);
            // Enter combat state immediately
            combatTimeout = MAX_COMBAT_TIMEOUT;
            dataTracker.set(IS_IN_COMBAT, true);
            dataTracker.set(HAS_BEEN_IN_COMBAT, true);
        }

        // During time magic, reduce damage but not as extremely
        if (dataTracker.get(IS_CASTING_TIME_MAGIC)) {
            amount *= 0.5f; // Changed from 0.3f to 0.5f (50% damage instead of 30%)
        }

        // Phase 3 damage reduction - less extreme
        float healthPercent = this.getHealth() / this.getMaxHealth();
        if (healthPercent < 0.25f) {
            amount *= 0.9f; // Changed from 0.8f to 0.9f (10% reduction instead of 20%)
        }

        // Prevent healing above max health
        if (this.getHealth() > this.getMaxHealth()) {
            this.setHealth(this.getMaxHealth());
        }

        return super.damage(source, amount);
    }

    @Override
    public void addVelocity(double deltaX, double deltaY, double deltaZ) {
        // Limit upward velocity to prevent Thoth from flying too high when hit
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
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        
        if (this.bossBar != null) {
            this.bossBar.clearPlayers();
        }
    }
    
    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        
        // Ensure boss bar is cleared when entity is removed for any reason
        if (!this.getWorld().isClient && this.bossBar != null) {
            this.bossBar.clearPlayers();
            this.bossBar = null;
        }
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
        } else if (isMagicBallAttack()) {
            return new Vec3d(0.3, 0.7, 1.0); // Blue
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