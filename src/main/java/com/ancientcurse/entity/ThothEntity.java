package com.ancientcurse.entity;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.ModEntities;
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
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.List;

/**
 * Thoth Entity - The Egyptian God of Wisdom, Magic, and Knowledge
 * A powerful floating boss that can cast time magic, summon entities, and perform devastating attacks
 */
public class ThothEntity extends HostileEntity implements GeoEntity {
    
    /* ---------- DATA TRACKERS ---------- */
    private static final TrackedData<Integer> ATTACK_STATE = 
            DataTracker.registerData(ThothEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> ATTACK_COOLDOWN = 
            DataTracker.registerData(ThothEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> IS_FLOATING = 
            DataTracker.registerData(ThothEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IS_READING = 
            DataTracker.registerData(ThothEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IS_CASTING_TIME_MAGIC = 
            DataTracker.registerData(ThothEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> SUMMONING_COOLDOWN = 
            DataTracker.registerData(ThothEntity.class, TrackedDataHandlerRegistry.INTEGER);
    
    /* ---------- CONSTANTS ---------- */
    private static final int ATTACK_NONE = 0;
    private static final int ATTACK_MAGIC_BALL = 1;
    private static final int ATTACK_SCROLL_BLAST = 2;
    private static final int ATTACK_TIME_BEND = 3;
    private static final int ATTACK_ENTITY_SUMMON = 4;
    
    private static final int MAX_ATTACK_COOLDOWN = 120; // 6 seconds (increased from 3)
    private static final int MAX_SUMMONING_COOLDOWN = 400; // 20 seconds
    private static final int TIME_MAGIC_DURATION = 200; // 10 seconds
    private static final float FLOATING_HEIGHT = 2.0f;
    
    /* ---------- FIELDS ---------- */
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private ServerBossBar bossBar;
    private int magicCastingTicks = 0;
    private int floatingTicks = 0;
    private Vec3d originalPosition;
    private boolean hasSpawned = false;
    private int timeMagicTicks = 0;
    private boolean initialized = false;
    private int spawnTransitionTicks = 0;
    public static final int SPAWN_TRANSITION_DURATION = 100; // 5 seconds
    
    // Phase system
    private boolean hasEnteredPhase2 = false;
    private boolean hasEnteredPhase3 = false;
    
    // Performance caching
    private PlayerEntity cachedTarget;
    private int targetCacheTime = 0;
    
    // Combat landing system
    private boolean isLandingForCombat = false;
    private boolean isGroundedForCombat = false;
    private int combatLandingTicks = 0;
    private static final int LANDING_DURATION = 20; // 1 second to land
    private static final int TAKEOFF_DURATION = 15; // 0.75 seconds to takeoff
    
    public ThothEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 100; // Boss-level XP
        
        // Initialize boss bar only on server side and with null safety
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
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.15) // Slow and deliberate
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 15.0) // High damage
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0) // Large detection range
                .add(EntityAttributes.GENERIC_ARMOR, 10.0) // High armor
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0) // Immune to knockback
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.2);
    }
    
    /* ---------- INITIALIZATION ---------- */
    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new ThothTimeBendGoal(this));
        this.goalSelector.add(2, new ThothSummonEntitiesGoal(this));
        this.goalSelector.add(3, new ThothMagicAttackGoal(this));
        this.goalSelector.add(4, new ThothFloatingGoal(this));
        this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 16.0f));
        this.goalSelector.add(6, new LookAroundGoal(this));
        
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }
    
    /* ---------- TICK ---------- */
    @Override
    public void tick() {
        super.tick();
        
        // Initialize entity on first tick
        if (!initialized) {
            initializeEntity();
            initialized = true;
        }
        
        // Handle cooldowns
        int attackCd = dataTracker.get(ATTACK_COOLDOWN);
        if (attackCd > 0) dataTracker.set(ATTACK_COOLDOWN, attackCd - 1);
        
        int summonCd = dataTracker.get(SUMMONING_COOLDOWN);
        if (summonCd > 0) dataTracker.set(SUMMONING_COOLDOWN, summonCd - 1);
        
        // Handle spawn transition timer
        if (spawnTransitionTicks > 0) {
            spawnTransitionTicks--;
        }
        
        // Handle combat landing/takeoff system
        handleCombatMovement();
        
        // Check for phase transitions
        if (!this.getWorld().isClient) {
            checkPhaseTransitions();
        }
        
        // Handle floating movement (only if not in combat landing mode)
        if (dataTracker.get(IS_FLOATING) && !isLandingForCombat && !isGroundedForCombat) {
            handleFloatingMovement();
        }
        
        // Handle time magic effects
        if (dataTracker.get(IS_CASTING_TIME_MAGIC)) {
            handleTimeMagic();
        }
        
        // Server-side behaviors
        if (!this.getWorld().isClient) {
            handleBossBar();
            handleMagicParticles();
            
            // Play spawn animation once
            if (!hasSpawned) {
                playSpawnAnimation();
                hasSpawned = true;
            }
            
            // Phase 3 special abilities - periodic time pulses
            if (hasEnteredPhase3 && this.age % 100 == 0) {
                performTimePulse();
            }
        }
    }
    
    private void initializeEntity() {
        // Set original position now that entity is properly spawned
        this.originalPosition = this.getPos();
        
        // Set initial floating state
        dataTracker.set(IS_FLOATING, true);
        
        // Ensure no gravity for floating boss
        this.setNoGravity(true);
        
        AncientCurse.LOGGER.info("Thoth entity initialized at position: " + this.getPos());
    }
    
    private void handleFloatingMovement() {
        // Initialize original position if needed with extra safety
        if (originalPosition == null) {
            originalPosition = this.getPos();
            if (originalPosition == null) return; // Extra safety check
        }
        
        floatingTicks++;
        
        // Gentle floating bobbing motion
        double bobOffset = Math.sin(floatingTicks * 0.1) * 0.2;
        Vec3d targetPos = originalPosition.add(0, FLOATING_HEIGHT + bobOffset, 0);
        
        // Smooth movement toward floating position
        Vec3d currentPos = this.getPos();
        if (currentPos == null) return; // Safety check
        
        Vec3d diff = targetPos.subtract(currentPos);
        if (diff.length() > 0.1) {
            Vec3d movement = diff.multiply(0.1);
            this.setPosition(currentPos.add(movement));
        }
        
        // Ensure gravity is disabled
        this.setNoGravity(true);
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
                    
                    // Chance to "freeze" movement (reduce velocity significantly)
                    if (this.random.nextFloat() < 0.2f) {
                        entity.setVelocity(entity.getVelocity().multiply(0.1));
                    }
                    
                    // Visual distortion effect
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 60, 0, false, false));
                }
            }
            
            // Heal Thoth during time magic (showing mastery over time)
            if (timeMagicTicks % 20 == 0) {
                this.heal(3.0f);
            }
            
            // Enhanced time distortion particles
            for (int i = 0; i < 8; i++) {
                Vec3d particlePos = this.getPos().add(
                    (this.random.nextDouble() - 0.5) * 12,
                    this.random.nextDouble() * 6,
                    (this.random.nextDouble() - 0.5) * 12
                );
                ((ServerWorld)this.getWorld()).spawnParticles(
                    ParticleTypes.ENCHANT,
                    particlePos.x, particlePos.y, particlePos.z,
                    2, 0.1, 0.1, 0.1, 0.05
                );
                
                // Additional portal particles for dramatic effect
                ((ServerWorld)this.getWorld()).spawnParticles(
                    ParticleTypes.REVERSE_PORTAL,
                    particlePos.x, particlePos.y, particlePos.z,
                    1, 0.2, 0.2, 0.2, 0.1
                );
            }
        }
        
        if (timeMagicTicks >= TIME_MAGIC_DURATION) {
            dataTracker.set(IS_CASTING_TIME_MAGIC, false);
            timeMagicTicks = 0;
        }
    }
    
    private void handleBossBar() {
        // Safety check for boss bar
        if (this.bossBar == null) {
            return;
        }
        
        try {
            // Update boss bar
            this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());
            
            // Add/remove players from boss bar
            List<PlayerEntity> nearbyPlayers = this.getWorld().getNonSpectatingEntities(
                PlayerEntity.class, 
                new Box(this.getBlockPos()).expand(64)
            );
            
            for (PlayerEntity player : nearbyPlayers) {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    this.bossBar.addPlayer(serverPlayer);
                }
            }
            
            // Remove distant players
            this.bossBar.getPlayers().removeIf(player -> {
                return this.squaredDistanceTo(player) > 64 * 64;
            });
        } catch (Exception e) {
            AncientCurse.LOGGER.warn("Error handling boss bar for Thoth: " + e.getMessage());
        }
    }
    
    private void handleMagicParticles() {
        if (this.random.nextInt(4) == 0) {
            // Ambient magic particles around Thoth
            for (int i = 0; i < 3; i++) {
                Vec3d particlePos = this.getPos().add(
                    (this.random.nextDouble() - 0.5) * 3,
                    this.random.nextDouble() * 3,
                    (this.random.nextDouble() - 0.5) * 3
                );
                ((ServerWorld)this.getWorld()).spawnParticles(
                    ParticleTypes.PORTAL,
                    particlePos.x, particlePos.y, particlePos.z,
                    1, 0, 0.1, 0, 0.05
                );
            }
        }
    }
    
    private void playSpawnAnimation() {
        dataTracker.set(ATTACK_STATE, ATTACK_ENTITY_SUMMON);
        dataTracker.set(IS_FLOATING, true);
        spawnTransitionTicks = SPAWN_TRANSITION_DURATION; // Start transition timer
        
        // Create dramatic spawn particles
        for (int i = 0; i < 20; i++) {
            Vec3d particlePos = this.getPos().add(
                (this.random.nextDouble() - 0.5) * 4,
                this.random.nextDouble() * 4,
                (this.random.nextDouble() - 0.5) * 4
            );
            ((ServerWorld)this.getWorld()).spawnParticles(
                ParticleTypes.DRAGON_BREATH,
                particlePos.x, particlePos.y, particlePos.z,
                3, 0.1, 0.1, 0.1, 0.1
            );
        }
        
        this.playSound(SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.8f);
    }
    
    private void handleCombatMovement() {
        boolean hasTarget = this.getTarget() != null;
        boolean isAttacking = dataTracker.get(ATTACK_STATE) != ATTACK_NONE;
        
        // Start landing when we have a target and are about to attack
        if (hasTarget && !isLandingForCombat && !isGroundedForCombat && dataTracker.get(IS_FLOATING)) {
            startLandingForCombat();
        }
        
        // Handle landing process
        if (isLandingForCombat) {
            combatLandingTicks++;
            performLanding();
            
            if (combatLandingTicks >= LANDING_DURATION) {
                completeLanding();
            }
        }
        
        // Start takeoff when combat ends (no target or not attacking)
        if (isGroundedForCombat && (!hasTarget || (!isAttacking && dataTracker.get(ATTACK_COOLDOWN) == 0))) {
            startTakeoff();
        }
        
        // Handle takeoff process
        if (!isLandingForCombat && isGroundedForCombat && combatLandingTicks > 0) {
            combatLandingTicks--;
            performTakeoff();
            
            if (combatLandingTicks <= 0) {
                completeTakeoff();
            }
        }
    }
    
    private void startLandingForCombat() {
        isLandingForCombat = true;
        combatLandingTicks = 0;
        this.playSound(SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 1.2f);
    }
    
    private void performLanding() {
        // Gradually descend to ground level
        float landingProgress = (float) combatLandingTicks / LANDING_DURATION;
        Vec3d currentPos = this.getPos();
        
        if (originalPosition != null && currentPos != null) {
            // Find safe landing position
            BlockPos landingPos = findSafeLandingPosition(currentPos);
            double groundY = landingPos.getY() + 0.1;
            
            // Smoothly interpolate from floating height to ground
            double startY = originalPosition.y + FLOATING_HEIGHT;
            double targetY = Math.max(groundY, originalPosition.y); // Don't go below original ground level
            
            // Smooth descent with easing
            double easedProgress = 1 - Math.pow(1 - landingProgress, 3); // Ease-out cubic
            double finalY = startY + (targetY - startY) * easedProgress;
            
            // Move to safe landing position
            this.setPosition(landingPos.getX() + 0.5, finalY, landingPos.getZ() + 0.5);
            
            // Create landing particles
            if (!this.getWorld().isClient && combatLandingTicks % 4 == 0) {
                ((ServerWorld)this.getWorld()).spawnParticles(
                    ParticleTypes.CLOUD,
                    landingPos.getX() + 0.5, finalY, landingPos.getZ() + 0.5,
                    2, 0.3, 0.1, 0.3, 0.02
                );
            }
        }
        
        // Gradually reduce no-gravity effect
        if (landingProgress > 0.8f) {
            this.setNoGravity(false);
        }
    }
    
    private BlockPos findSafeLandingPosition(Vec3d currentPos) {
        BlockPos centerPos = new BlockPos((int)currentPos.x, (int)currentPos.y, (int)currentPos.z);
        
        // Try positions in expanding radius
        for (int radius = 0; radius <= 3; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (radius > 0 && Math.abs(x) != radius && Math.abs(z) != radius) continue; // Only check perimeter
                    
                    BlockPos testPos = centerPos.add(x, 0, z);
                    BlockPos groundPos = findGroundLevel(testPos);
                    
                    if (isValidLandingPosition(groundPos)) {
                        return groundPos;
                    }
                }
            }
        }
        
        // Fallback to original position
        return centerPos;
    }
    
    private BlockPos findGroundLevel(BlockPos startPos) {
        // Search downward for solid ground
        for (int y = 0; y >= -10; y--) {
            BlockPos checkPos = startPos.add(0, y, 0);
            if (this.getWorld().getBlockState(checkPos.down()).isSolidBlock(this.getWorld(), checkPos.down())) {
                return checkPos;
            }
        }
        return startPos; // Fallback
    }
    
    private boolean isValidLandingPosition(BlockPos pos) {
        // Check if the ground is solid
        if (!this.getWorld().getBlockState(pos.down()).isSolidBlock(this.getWorld(), pos.down())) {
            return false;
        }
        
        // Check if there's enough space (3 blocks high for Thoth)
        for (int y = 0; y < 3; y++) {
            BlockPos checkPos = pos.up(y);
            if (!this.getWorld().getBlockState(checkPos).isAir() && 
                !this.getWorld().getBlockState(checkPos).getFluidState().isEmpty()) {
                return false;
            }
        }
        
        // Avoid landing in liquids
        if (!this.getWorld().getBlockState(pos).getFluidState().isEmpty()) {
            return false;
        }
        
        // Don't land on dangerous blocks
        String blockName = this.getWorld().getBlockState(pos.down()).getBlock().toString().toLowerCase();
        if (blockName.contains("lava") || blockName.contains("fire") || blockName.contains("cactus")) {
            return false;
        }
        
        return true;
    }
    
    private void completeLanding() {
        isLandingForCombat = false;
        isGroundedForCombat = true;
        combatLandingTicks = TAKEOFF_DURATION; // Prepare for potential takeoff
        this.setNoGravity(false);
        
        // Landing impact sound and particles
        this.playSound(SoundEvents.ENTITY_IRON_GOLEM_STEP, 0.6f, 0.8f);
        
        if (!this.getWorld().isClient) {
            Vec3d pos = this.getPos();
            ((ServerWorld)this.getWorld()).spawnParticles(
                ParticleTypes.LARGE_SMOKE,
                pos.x, pos.y, pos.z,
                8, 0.5, 0.1, 0.5, 0.05
            );
            
            // Debug logging
            AncientCurse.LOGGER.info("Thoth completed landing - isGroundedForCombat: " + isGroundedForCombat + 
                                  ", isFloating: " + dataTracker.get(IS_FLOATING));
        }
    }
    
    private void startTakeoff() {
        // Reset to floating state preparation
        combatLandingTicks = TAKEOFF_DURATION;
        this.playSound(SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 1.0f);
    }
    
    private void performTakeoff() {
        // Gradually ascend back to floating height
        float takeoffProgress = 1.0f - ((float) combatLandingTicks / TAKEOFF_DURATION);
        Vec3d currentPos = this.getPos();
        
        if (originalPosition != null && currentPos != null) {
            // Calculate target floating position
            double groundY = currentPos.y;
            double targetY = originalPosition.y + FLOATING_HEIGHT;
            
            // Smooth ascent with easing
            double easedProgress = 1 - Math.pow(1 - takeoffProgress, 2); // Ease-out quadratic
            double finalY = groundY + (targetY - groundY) * easedProgress;
            
            this.setPosition(currentPos.x, finalY, currentPos.z);
            
            // Create takeoff particles
            if (!this.getWorld().isClient && combatLandingTicks % 3 == 0) {
                ((ServerWorld)this.getWorld()).spawnParticles(
                    ParticleTypes.PORTAL,
                    currentPos.x, finalY - 0.5, currentPos.z,
                    3, 0.4, 0.1, 0.4, 0.1
                );
            }
        }
        
        // Gradually restore no-gravity effect
        if (takeoffProgress > 0.3f) {
            this.setNoGravity(true);
        }
    }
    
    private void completeTakeoff() {
        isGroundedForCombat = false;
        combatLandingTicks = 0;
        dataTracker.set(IS_FLOATING, true);
        this.setNoGravity(true);
        
        // Takeoff completion sound
        this.playSound(SoundEvents.ENTITY_VEX_AMBIENT, 1.0f, 0.8f);
        
        if (!this.getWorld().isClient) {
            // Debug logging
            AncientCurse.LOGGER.info("Thoth completed takeoff - isGroundedForCombat: " + isGroundedForCombat + 
                                  ", isFloating: " + dataTracker.get(IS_FLOATING));
        }
    }
    
    private PlayerEntity getCachedTarget() {
        if (targetCacheTime <= 0 || this.age % 20 == 0) {
            LivingEntity target = this.getTarget();
            cachedTarget = target instanceof PlayerEntity ? (PlayerEntity) target : null;
            targetCacheTime = 20;
        }
        targetCacheTime--;
        return cachedTarget;
    }
    
    private void checkPhaseTransitions() {
        float healthPercent = this.getHealth() / this.getMaxHealth();
        
        // Phase 2: Enhanced aggression at 50% health
        if (healthPercent <= 0.5f && !hasEnteredPhase2) {
            hasEnteredPhase2 = true;
            enterPhase2();
        }
        
        // Phase 3: Desperation/time mastery at 25% health
        if (healthPercent <= 0.25f && !hasEnteredPhase3) {
            hasEnteredPhase3 = true;
            enterPhase3();
        }
    }
    
    private void enterPhase2() {
        // Dramatic phase transition with protective effects
        this.playSound(SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 3.0f, 0.5f);
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 100, 2));
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 200, 1));
        
        // Immediately summon scarabs for protection
        summonEntities();
        
        // Create dramatic particle explosion
        if (!this.getWorld().isClient) {
            for (int i = 0; i < 50; i++) {
                Vec3d particlePos = this.getPos().add(
                    (this.random.nextDouble() - 0.5) * 8,
                    this.random.nextDouble() * 6,
                    (this.random.nextDouble() - 0.5) * 8
                );
                ((ServerWorld)this.getWorld()).spawnParticles(
                    ParticleTypes.FLAME,
                    particlePos.x, particlePos.y, particlePos.z,
                    3, 0.2, 0.2, 0.2, 0.1
                );
            }
        }
    }
    
    private void enterPhase3() {
        hasEnteredPhase3 = true;
        
        // Grant temporary invulnerability during dramatic transition
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 60, 4, false, false));
        
        // Create time distortion field
        if (!this.getWorld().isClient) {
            Box timeArea = new Box(this.getBlockPos()).expand(32);
            List<PlayerEntity> affectedPlayers = this.getWorld().getNonSpectatingEntities(
                PlayerEntity.class, timeArea);
            
            for (PlayerEntity player : affectedPlayers) {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    // Time distortion message
                    serverPlayer.sendMessage(
                        Text.literal("§5§lTime itself bends to Thoth's will!"), 
                        true
                    );
                    
                    // Brief disorientation effect
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 100, 0, false, false));
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 20, 0, false, false));
                }
            }
            
            // Epic particle effects - time shattering
            for (int i = 0; i < 50; i++) {
                Vec3d particlePos = this.getPos().add(
                    (this.random.nextDouble() - 0.5) * 16,
                    this.random.nextDouble() * 12,
                    (this.random.nextDouble() - 0.5) * 16
                );
                ((ServerWorld)this.getWorld()).spawnParticles(
                    ParticleTypes.END_ROD,
                    particlePos.x, particlePos.y, particlePos.z,
                    1, 0, 0, 0, 0.1
                );
            }
            
            // Lightning storm
            for (int i = 0; i < 8; i++) {
                Vec3d lightningPos = this.getPos().add(
                    (this.random.nextDouble() - 0.5) * 12,
                    this.random.nextDouble() * 8,
                    (this.random.nextDouble() - 0.5) * 12
                );
                ((ServerWorld)this.getWorld()).spawnParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    lightningPos.x, lightningPos.y, lightningPos.z,
                    30, 1.5, 1.5, 1.5, 0.3
                );
            }
        }
        
        // Epic sound sequence
        this.playSound(SoundEvents.ENTITY_WITHER_SPAWN, 3.0f, 0.3f);
        this.playSound(SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.5f);
        
        // Enhanced abilities in final phase
        dataTracker.set(ATTACK_COOLDOWN, 0); // Reset cooldown immediately
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 3));
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 300, 1));
        
        // Boost Thoth's power for final phase
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, Integer.MAX_VALUE, 1, false, false));
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, Integer.MAX_VALUE, 1, false, false));
        
        // Immediately trigger time magic
        performTimeBend();
    }
    
    private void chooseStrategicAttack() {
        PlayerEntity target = getCachedTarget();
        if (target == null) return;
        
        double distance = this.squaredDistanceTo(target);
        float healthPercent = this.getHealth() / this.getMaxHealth();
        
        // Strategic attack selection based on context
        if (healthPercent < 0.25f && !dataTracker.get(IS_CASTING_TIME_MAGIC)) {
            // Phase 3: Prefer time magic
            performTimeBend();
        } else if (distance > 144) { // Far away (12+ blocks)
            // Use ranged magic ball attack
            performMagicBallAttack();
        } else if (distance < 64 && healthPercent > 0.3f) { // Close (8 blocks)
            // Use area attack when healthy and close
            performScrollBlast();
        } else if (healthPercent < 0.5f && dataTracker.get(SUMMONING_COOLDOWN) == 0) {
            // Summon help when below 50% health
            summonEntities();
        } else {
            // Default to magic ball
            performMagicBallAttack();
        }
    }
    
    private void performTimePulse() {
        // Brief area-wide time freeze effect in Phase 3
        Box area = new Box(this.getBlockPos()).expand(20);
        List<LivingEntity> entities = this.getWorld().getNonSpectatingEntities(LivingEntity.class, area);
        
        for (LivingEntity entity : entities) {
            if (entity != this && entity instanceof PlayerEntity) {
                // Intense but brief time disruption
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 5, false, false));
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 40, 3, false, false));
                
                // "Freeze" their movement briefly
                entity.setVelocity(Vec3d.ZERO);
            }
        }
        
        // Create time distortion particles
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
        
        // Ominous time pulse sound
        this.playSound(SoundEvents.BLOCK_PORTAL_AMBIENT, 1.0f, 0.3f);
    }
    
    /* ---------- ATTACK METHODS ---------- */
    public void performMagicBallAttack() {
        if (dataTracker.get(ATTACK_COOLDOWN) > 0) return;
        
        dataTracker.set(ATTACK_STATE, ATTACK_MAGIC_BALL);
        dataTracker.set(ATTACK_COOLDOWN, MAX_ATTACK_COOLDOWN);
        
        LivingEntity target = this.getTarget();
        if (target != null && !this.getWorld().isClient) {
            // Create magic projectile effect
            Vec3d direction = target.getPos().subtract(this.getPos()).normalize();
            Vec3d projectilePos = this.getPos().add(direction.multiply(2));
            
            // Spawn magic particles in a line toward target
            for (int i = 0; i < 20; i++) {
                Vec3d particlePos = projectilePos.add(direction.multiply(i * 0.5));
                ((ServerWorld)this.getWorld()).spawnParticles(
                    ParticleTypes.WITCH,
                    particlePos.x, particlePos.y, particlePos.z,
                    2, 0.1, 0.1, 0.1, 0.05
                );
            }
            
            // Damage target if close enough
            if (this.squaredDistanceTo(target) < 256) {
                target.damage(this.getDamageSources().mobAttack(this), 12.0f);
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 200, 1));
            }
        }
        
        this.playSound(SoundEvents.ENTITY_EVOKER_CAST_SPELL, 1.5f, 1.0f);
    }
    
    public void performScrollBlast() {
        if (dataTracker.get(ATTACK_COOLDOWN) > 0) return;
        
        dataTracker.set(ATTACK_STATE, ATTACK_SCROLL_BLAST);
        dataTracker.set(ATTACK_COOLDOWN, MAX_ATTACK_COOLDOWN);
        dataTracker.set(IS_READING, true);
        
        if (!this.getWorld().isClient) {
            // Area of effect attack
            Box area = new Box(this.getBlockPos()).expand(8);
            List<LivingEntity> entities = this.getWorld().getNonSpectatingEntities(LivingEntity.class, area);
            
            for (LivingEntity entity : entities) {
                if (entity != this && entity instanceof PlayerEntity) {
                    entity.damage(this.getDamageSources().mobAttack(this), 8.0f);
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 200, 1));
                    
                    // Knockback effect
                    Vec3d direction = entity.getPos().subtract(this.getPos()).normalize();
                    entity.addVelocity(direction.x * 0.8, 0.5, direction.z * 0.8);
                }
            }
            
            // Create explosion particles
            for (int i = 0; i < 30; i++) {
                Vec3d particlePos = this.getPos().add(
                    (this.random.nextDouble() - 0.5) * 16,
                    this.random.nextDouble() * 8,
                    (this.random.nextDouble() - 0.5) * 16
                );
                ((ServerWorld)this.getWorld()).spawnParticles(
                    ParticleTypes.EXPLOSION,
                    particlePos.x, particlePos.y, particlePos.z,
                    1, 0, 0, 0, 0
                );
            }
        }
        
        this.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);
    }
    
    public void performTimeBend() {
        if (dataTracker.get(ATTACK_COOLDOWN) > 0) return;
        
        dataTracker.set(ATTACK_STATE, ATTACK_TIME_BEND);
        dataTracker.set(ATTACK_COOLDOWN, MAX_ATTACK_COOLDOWN * 2); // Longer cooldown for powerful ability
        dataTracker.set(IS_CASTING_TIME_MAGIC, true);
        timeMagicTicks = 0;
        
        this.playSound(SoundEvents.BLOCK_PORTAL_AMBIENT, 2.0f, 0.5f);
    }
    
    public void summonEntities() {
        if (dataTracker.get(SUMMONING_COOLDOWN) > 0) return;
        
        dataTracker.set(ATTACK_STATE, ATTACK_ENTITY_SUMMON);
        dataTracker.set(SUMMONING_COOLDOWN, MAX_SUMMONING_COOLDOWN);
        
        if (!this.getWorld().isClient) {
            // Summon scarab beetles to aid in battle
            for (int i = 0; i < 3; i++) {
                ScarabBeetleEntity scarab = ModEntities.SCARAB_BEETLE.create(this.getWorld());
                if (scarab != null) {
                    Vec3d spawnPos = this.getPos().add(
                        (this.random.nextDouble() - 0.5) * 8,
                        0,
                        (this.random.nextDouble() - 0.5) * 8
                    );
                    scarab.setPosition(spawnPos);
                    this.getWorld().spawnEntity(scarab);
                    
                    // Make them target the same target as Thoth
                    if (this.getTarget() != null) {
                        scarab.setTarget(this.getTarget());
                    }
                }
            }
        }
        
        this.playSound(SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, 2.0f, 1.0f);
    }
    
    /* ---------- CUSTOM GOALS ---------- */
    public static class ThothFloatingGoal extends Goal {
        private final ThothEntity thoth;
        
        public ThothFloatingGoal(ThothEntity thoth) {
            this.thoth = thoth;
            this.setControls(EnumSet.of(Control.MOVE));
        }
        
        @Override
        public boolean canStart() {
            return thoth.getTarget() == null;
        }
        
        @Override
        public void start() {
            thoth.dataTracker.set(IS_FLOATING, true);
        }
        
        @Override
        public boolean shouldContinue() {
            return thoth.getTarget() == null;
        }
        
        @Override
        public void stop() {
            // Keep floating even when not idle
        }
    }
    
    public static class ThothMagicAttackGoal extends Goal {
        private final ThothEntity thoth;
        private int attackTimer = 0;
        
        public ThothMagicAttackGoal(ThothEntity thoth) {
            this.thoth = thoth;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }
        
        @Override
        public boolean canStart() {
            return thoth.isAlive() && 
                   thoth.getTarget() != null && 
                   thoth.dataTracker.get(ATTACK_COOLDOWN) == 0 &&
                   thoth.squaredDistanceTo(thoth.getTarget()) < 256; // 16 block range
        }
        
        @Override
        public void start() {
            attackTimer = 0;
        }
        
        @Override
        public void tick() {
            LivingEntity target = thoth.getTarget();
            if (target != null) {
                thoth.getLookControl().lookAt(target);
                
                if (++attackTimer >= 40) { // 2 second wind-up (increased from 1 second)
                    // Use strategic attack selection instead of random
                    thoth.chooseStrategicAttack();
                    attackTimer = 0;
                }
            }
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
                   thoth.getHealth() < thoth.getMaxHealth() * 0.5f; // Only when below 50% health
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
                   thoth.getHealth() < thoth.getMaxHealth() * 0.3f && // Only when below 30% health
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
        boolean isFloating = this.dataTracker.get(IS_FLOATING);
        boolean isReading = this.dataTracker.get(IS_READING);
        boolean isCastingTime = this.dataTracker.get(IS_CASTING_TIME_MAGIC);
        
        // Priority order for animations
        if (attackState == ATTACK_MAGIC_BALL) {
            state.getController().setAnimation(RawAnimation.begin().then("animation.thoth.attack_1", Animation.LoopType.PLAY_ONCE));
            // Reset attack state after animation completes
            if (state.getController().getAnimationState() == AnimationController.State.STOPPED) {
                this.dataTracker.set(ATTACK_STATE, ATTACK_NONE);
                // Force immediate transition to appropriate idle state
                return PlayState.STOP;
            }
        } else if (attackState == ATTACK_SCROLL_BLAST) {
            state.getController().setAnimation(RawAnimation.begin().then("animation.thoth.attack_2", Animation.LoopType.PLAY_ONCE));
            if (state.getController().getAnimationState() == AnimationController.State.STOPPED) {
                this.dataTracker.set(ATTACK_STATE, ATTACK_NONE);
                this.dataTracker.set(IS_READING, false);
                // Force immediate transition to appropriate idle state
                return PlayState.STOP;
            }
        } else if (attackState == ATTACK_TIME_BEND || isCastingTime) {
            state.getController().setAnimation(RawAnimation.begin().then("animation.thoth.time_bend", Animation.LoopType.PLAY_ONCE));
        } else if (attackState == ATTACK_ENTITY_SUMMON) {
            // Handle spawn animation and transition
            if (spawnTransitionTicks > SPAWN_TRANSITION_DURATION * 0.6f) {
                // First 60% - play spawn animation
                state.getController().setAnimation(RawAnimation.begin().then("animation.thoth.entity_spawn", Animation.LoopType.PLAY_ONCE));
            } else if (spawnTransitionTicks > 0) {
                // Last 40% - transition to appropriate idle state
                if (isGroundedForCombat) {
                    state.getController().setAnimation(RawAnimation.begin().then("animation.thoth.idle_standing", Animation.LoopType.LOOP));
                } else {
                    state.getController().setAnimation(RawAnimation.begin().then("animation.thoth.idle", Animation.LoopType.LOOP));
                }
            } else {
                // Transition complete - reset attack state and force transition
                this.dataTracker.set(ATTACK_STATE, ATTACK_NONE);
                return PlayState.STOP;
            }
        } else {
            // Idle state logic - prioritize grounded combat state
            if (isGroundedForCombat) {
                // Always use standing animations when grounded for combat
                if (state.isMoving()) {
                    state.getController().setAnimation(RawAnimation.begin().then("animation.thoth.walking", Animation.LoopType.LOOP));
                } else {
                    state.getController().setAnimation(RawAnimation.begin().then("animation.thoth.idle_standing", Animation.LoopType.LOOP));
                }
            } else if (isFloating) {
                // Use floating animations when not grounded for combat
                if (state.isMoving()) {
                    state.getController().setAnimation(RawAnimation.begin().then("animation.thoth.walking", Animation.LoopType.LOOP));
                } else {
                    state.getController().setAnimation(RawAnimation.begin().then("animation.thoth.idle", Animation.LoopType.LOOP));
                }
            } else {
                // Default to standing animation when not floating
                state.getController().setAnimation(RawAnimation.begin().then("animation.thoth.idle_standing", Animation.LoopType.LOOP));
            }
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
        this.dataTracker.startTracking(IS_FLOATING, true);
        this.dataTracker.startTracking(IS_READING, false);
        this.dataTracker.startTracking(IS_CASTING_TIME_MAGIC, false);
        this.dataTracker.startTracking(SUMMONING_COOLDOWN, 0);
    }
    
    /* ---------- NBT ---------- */
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("AttackState", this.dataTracker.get(ATTACK_STATE));
        nbt.putInt("AttackCooldown", this.dataTracker.get(ATTACK_COOLDOWN));
        nbt.putBoolean("IsFloating", this.dataTracker.get(IS_FLOATING));
        nbt.putBoolean("IsReading", this.dataTracker.get(IS_READING));
        nbt.putBoolean("IsCastingTimeMagic", this.dataTracker.get(IS_CASTING_TIME_MAGIC));
        nbt.putInt("SummoningCooldown", this.dataTracker.get(SUMMONING_COOLDOWN));
        nbt.putBoolean("HasSpawned", this.hasSpawned);
        nbt.putInt("SpawnTransitionTicks", this.spawnTransitionTicks);
        nbt.putBoolean("HasEnteredPhase2", this.hasEnteredPhase2);
        nbt.putBoolean("HasEnteredPhase3", this.hasEnteredPhase3);
        nbt.putBoolean("IsLandingForCombat", this.isLandingForCombat);
        nbt.putBoolean("IsGroundedForCombat", this.isGroundedForCombat);
        nbt.putInt("CombatLandingTicks", this.combatLandingTicks);
    }
    
    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.dataTracker.set(ATTACK_STATE, nbt.getInt("AttackState"));
        this.dataTracker.set(ATTACK_COOLDOWN, nbt.getInt("AttackCooldown"));
        this.dataTracker.set(IS_FLOATING, nbt.getBoolean("IsFloating"));
        this.dataTracker.set(IS_READING, nbt.getBoolean("IsReading"));
        this.dataTracker.set(IS_CASTING_TIME_MAGIC, nbt.getBoolean("IsCastingTimeMagic"));
        this.dataTracker.set(SUMMONING_COOLDOWN, nbt.getInt("SummoningCooldown"));
        this.hasSpawned = nbt.getBoolean("HasSpawned");
        this.spawnTransitionTicks = nbt.getInt("SpawnTransitionTicks");
        this.hasEnteredPhase2 = nbt.getBoolean("HasEnteredPhase2");
        this.hasEnteredPhase3 = nbt.getBoolean("HasEnteredPhase3");
        this.isLandingForCombat = nbt.getBoolean("IsLandingForCombat");
        this.isGroundedForCombat = nbt.getBoolean("IsGroundedForCombat");
        this.combatLandingTicks = nbt.getInt("CombatLandingTicks");
    }
    
    /* ---------- GETTERS ---------- */
    public boolean isFloating() { return dataTracker.get(IS_FLOATING); }
    public boolean isReading() { return dataTracker.get(IS_READING); }
    public boolean isCastingTimeMagic() { return dataTracker.get(IS_CASTING_TIME_MAGIC); }
    public int getAttackState() { return dataTracker.get(ATTACK_STATE); }
    public int getSpawnTransitionTicks() { return spawnTransitionTicks; }
    public boolean isInSpawnTransition() { return spawnTransitionTicks > 0; }
    public boolean isGroundedForCombat() { return isGroundedForCombat; }
    public boolean isLandingForCombat() { return isLandingForCombat; }
    
    // Attack state checkers to avoid magic numbers in renderer
    public boolean isMagicBallAttack() { return dataTracker.get(ATTACK_STATE) == ATTACK_MAGIC_BALL; }
    public boolean isScrollBlastAttack() { return dataTracker.get(ATTACK_STATE) == ATTACK_SCROLL_BLAST; }
    public boolean isTimeBendAttack() { return dataTracker.get(ATTACK_STATE) == ATTACK_TIME_BEND; }
    public boolean isEntitySummonAttack() { return dataTracker.get(ATTACK_STATE) == ATTACK_ENTITY_SUMMON; }
    public boolean isAttackingWithMagic() { 
        int state = dataTracker.get(ATTACK_STATE);
        return state == ATTACK_MAGIC_BALL || state == ATTACK_SCROLL_BLAST || state == ATTACK_TIME_BEND;
    }
    
    /* ---------- SOUNDS ---------- */
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_EVOKER_AMBIENT;
    }
    
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.ENTITY_EVOKER_HURT;
    }
    
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_EVOKER_DEATH;
    }
    
    /* ---------- BOSS BEHAVIOR ---------- */
    @Override
    public boolean damage(DamageSource source, float amount) {
        // Enhanced damage reduction system
        if (dataTracker.get(IS_CASTING_TIME_MAGIC)) {
            amount *= 0.3f; // 70% damage reduction during time magic (enhanced)
        }
        
        // Additional damage reduction during phase transitions and final phase
        float healthPercent = this.getHealth() / this.getMaxHealth();
        if (healthPercent < 0.25f) {
            amount *= 0.8f; // 20% damage reduction in final phase
        }
        
        return super.damage(source, amount);
    }
    
    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        
        // Remove boss bar safely
        if (this.bossBar != null) {
            this.bossBar.clearPlayers();
        }
    }
    
    @Override
    public boolean canBreatheInWater() {
        return true; // God-like entity
    }
    
    @Override
    public boolean cannotDespawn() {
        return true; // Boss should never despawn
    }
} 