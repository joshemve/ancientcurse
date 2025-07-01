package com.ancientcurse.entity;

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
import net.minecraft.world.World;
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
    private static final int SKY_YELL_DURATION = 60;   // 3 seconds
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

    private PlayerEntity judgedPlayer;
    private int judgementTimer;
    private float targetHoverHeight;

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
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 200.0D) // Boss health
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.4D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 15.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8D)
                .add(EntityAttributes.GENERIC_ARMOR, 8.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0D);
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
    }

    @Override
    protected void initGoals() {
        // Boss AI goals
        this.goalSelector.add(1, new AnubisSkyYellGoal(this));
        this.goalSelector.add(2, new AnubisJudgementGoal(this));
        this.goalSelector.add(3, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.add(4, new WanderAroundFarGoal(this, 0.8) {
            @Override
            public boolean canStart() {
                return currentPhase == BossPhase.COMBAT && super.canStart();
            }
        });
        this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 32.0F));
        this.goalSelector.add(6, new LookAroundGoal(this));

        // Target selection
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true) {
            @Override
            public boolean canStart() {
                return currentPhase == BossPhase.COMBAT && super.canStart();
            }
        });
    }

    /* -------------------------------------------------------------------- */
    /*  MAIN TICK METHOD                                                    */
    /* -------------------------------------------------------------------- */

    @Override
    public void tick() {
        super.tick();
        
        if (!this.getWorld().isClient) {
            updateBossPhase();
            updateBossBar();
            updateHovering();
        }
        
        // Client-side effects
        if (this.getWorld().isClient) {
            spawnPhaseParticles();
        }
        
        phaseTimer++;
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
                // Transition to judgment after some combat time
                if (phaseTimer > 400 && nearestPlayer != null) { // 20 seconds
                    initiateCombatJudgement(nearestPlayer);
                }
                break;
                
            case JUDGING:
                if (judgementTimer <= 0) {
                    concludeJudgement();
                } else {
                    judgementTimer--;
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
        // Update boss bar health
        bossBar.setPercent(getHealth() / getMaxHealth());
        
        // Add/remove players from boss bar
        for (PlayerEntity player : getWorld().getPlayers()) {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                if (squaredDistanceTo(player) <= BOSS_DETECTION_RADIUS * BOSS_DETECTION_RADIUS) {
                    bossBar.addPlayer(serverPlayer);
                } else {
                    bossBar.removePlayer(serverPlayer);
                }
            }
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
        
        // Start hovering
        setHovering(true);
        setJudging(true);
        targetHoverHeight = JUDGEMENT_HEIGHT;
        
        // Stop current combat
        setTarget(null);
        
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
                
                // Send message to player
                judgedPlayer.sendMessage(Text.translatable("message.ancientcurse.anubis.mercy"), false);
            } else {
                setBossPhase(BossPhase.ENRAGED);
                setTarget(judgedPlayer);
            }
        }
        
        // End hovering and judging
        setHovering(false);
        setJudging(false);
        targetHoverHeight = 0.0F;
        judgedPlayer = null;
        phaseTimer = 0;
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

    private class AnubisSkyYellGoal extends Goal {
        private final AnubisEntity anubis;
        private int yellTimer = 0;

        AnubisSkyYellGoal(AnubisEntity anubis) {
            this.anubis = anubis;
            this.setControls(EnumSet.of(Goal.Control.LOOK, Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            return anubis.currentPhase == BossPhase.COMBAT && 
                   anubis.getTarget() != null && 
                   anubis.random.nextInt(200) == 0; // Random chance
        }

        @Override
        public void start() {
            yellTimer = SKY_YELL_DURATION;
            anubis.setSkyYelling(true);
            anubis.playingSpecialAnimation = true;
        }

        @Override
        public boolean shouldContinue() {
            return yellTimer > 0;
        }

        @Override
        public void tick() {
            yellTimer--;
            
            // Look up to the sky
            anubis.setYaw(anubis.getYaw());
            anubis.setPitch(-70.0F); // Look up
            
            // Play sound periodically
            if (yellTimer % 20 == 0) {
                anubis.getWorld().playSound(null, anubis.getX(), anubis.getY(), anubis.getZ(),
                        SoundEvents.ENTITY_WOLF_HOWL, SoundCategory.HOSTILE, 2.0F, 0.8F);
            }
        }

        @Override
        public void stop() {
            anubis.setSkyYelling(false);
            anubis.playingSpecialAnimation = false;
            anubis.setPitch(0.0F);
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

    /* -------------------------------------------------------------------- */
    /*  ANIMATION CONTROLLERS                                               */
    /* -------------------------------------------------------------------- */

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar r) {
        // Main controller handles movement and idle states
        r.add(new AnimationController<>(this, "main", 5, this::mainPredicate));
        // Attack controller handles all attack animations with priority
        r.add(new AnimationController<>(this, "attack", 2, this::attackPredicate));
        // Special state controllers for unique boss mechanics
        r.add(new AnimationController<>(this, "special", 3, this::specialPredicate));
    }

    private <T extends GeoAnimatable> PlayState mainPredicate(AnimationState<T> s) {
        // Don't play if special animation or attack is active
        if (playingSpecialAnimation || this.handSwinging) return PlayState.STOP;
        
        // Death animation takes priority
        if (currentPhase == BossPhase.DEAD) {
            s.getController().setAnimation(RawAnimation.begin()
                    .then("animation.anubis.death", Animation.LoopType.PLAY_ONCE));
            return PlayState.CONTINUE;
        }
        
        // Movement animations
        if (s.isMoving()) {
            // Use running animation for enraged phase
            if (currentPhase == BossPhase.ENRAGED) {
                s.getController().setAnimation(RawAnimation.begin()
                        .then("animation.anubis.running", Animation.LoopType.LOOP));
            } else {
                s.getController().setAnimation(RawAnimation.begin()
                        .then("animation.anubis.walking", Animation.LoopType.LOOP));
            }
            return PlayState.CONTINUE;
        }
        
        // Default to idle
        s.getController().setAnimation(RawAnimation.begin()
                .then("animation.anubis.idle", Animation.LoopType.LOOP));
        return PlayState.CONTINUE;
    }

    private <T extends GeoAnimatable> PlayState attackPredicate(AnimationState<T> s) {
        // Handle attack animations
        if (this.handSwinging && this.handSwingTicks > 0) {
            s.getController().setAnimation(RawAnimation.begin()
                    .then("animation.anubis.attack_1", Animation.LoopType.PLAY_ONCE));
            return PlayState.CONTINUE;
        }
        
        // Sky yelling special attack
        if (isSkyYelling()) {
            s.getController().setAnimation(RawAnimation.begin()
                    .then("animation.anubis.attack_2_howl", Animation.LoopType.PLAY_ONCE));
            return PlayState.CONTINUE;
        }
        
        return PlayState.STOP;
    }

    private <T extends GeoAnimatable> PlayState specialPredicate(AnimationState<T> s) {
        // Judgment idle animation
        if (isJudging()) {
            s.getController().setAnimation(RawAnimation.begin()
                    .then("animation.anubis.judgement_idle", Animation.LoopType.LOOP));
            playingSpecialAnimation = true;
            return PlayState.CONTINUE;
        }
        
        // Merciful/safe animation
        if (currentPhase == BossPhase.MERCIFUL) {
            s.getController().setAnimation(RawAnimation.begin()
                    .then("animation.anubis.judgement_safe", Animation.LoopType.LOOP));
            playingSpecialAnimation = true;
            return PlayState.CONTINUE;
        }
        
        // Awakening animation (using idle with special effects)
        if (currentPhase == BossPhase.AWAKENING) {
            s.getController().setAnimation(RawAnimation.begin()
                    .then("animation.anubis.idle", Animation.LoopType.LOOP));
            playingSpecialAnimation = true;
            return PlayState.CONTINUE;
        }
        
        playingSpecialAnimation = false;
        return PlayState.STOP;
    }

    /* -------------------------------------------------------------------- */
    /*  SOUNDS & DAMAGE HANDLING                                            */
    /* -------------------------------------------------------------------- */

    @Override
    protected SoundEvent getAmbientSound() {
        return switch (currentPhase) {
            case JUDGING -> SoundEvents.ENTITY_VILLAGER_AMBIENT;
            case MERCIFUL -> SoundEvents.ENTITY_VILLAGER_YES;
            case ENRAGED -> SoundEvents.ENTITY_WOLF_GROWL;
            case DEAD -> SoundEvents.ENTITY_WOLF_DEATH;
            default -> SoundEvents.ENTITY_WOLF_AMBIENT;
        };
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.ENTITY_WOLF_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_WOLF_DEATH;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        // If player attacks during judgment, become enraged
        if (currentPhase == BossPhase.JUDGING && source.getAttacker() instanceof PlayerEntity) {
            setBossPhase(BossPhase.ENRAGED);
            setTarget((LivingEntity) source.getAttacker());
            setHovering(false);
            setJudging(false);
            targetHoverHeight = 0.0F;
        }
        
        // If player is deemed safe, reduce damage significantly
        if (currentPhase == BossPhase.MERCIFUL && 
            source.getAttacker() instanceof PlayerEntity player &&
            safePlayers.contains(player.getUuid())) {
            amount *= 0.1F; // 90% damage reduction
        }
        
        return super.damage(source, amount);
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);
        // Remove boss bar when dead
        bossBar.clearPlayers();
        setBossPhase(BossPhase.DEAD);
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

    @Override 
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    
    @Override 
    public double getTick(Object unused) { return this.age; }
}
