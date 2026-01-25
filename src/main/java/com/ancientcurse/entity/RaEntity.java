package com.ancientcurse.entity;

import com.ancientcurse.ModItems;
import com.ancientcurse.entity.ai.RaFlightGoal;
import com.ancientcurse.entity.ai.RaGroundSmackGoal;
import com.ancientcurse.entity.ai.RaShardAttackGoal;
import net.minecraft.entity.EntityType;
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
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Ra Entity - The Egyptian God of the Sun (Final Boss)
 *
 * Phase System (HP percentage based):
 * - Phase 1 "The Awakened" (100%-60%): Ground combat, learning phase
 * - Phase 2 "Solar Wrath" (60%-30%): More flying, ranged attacks
 * - Phase 3 "Divine Fury" (30%-0%): Aggressive, all attacks, faster
 *
 * Combat States:
 * - IDLE: Standing still
 * - WALKING: Moving on ground
 * - FLYING: Hovering in air
 * - MELEE: Close combat attack
 * - GROUND_SMACK: Jump + twist + slam attack
 * - SHARD_ATTACK: Projectile attack
 * - FLYING_STAFF_ATTACK: Ranged beam from air
 * - HIBERNATING: Stunned state (wrapped by player)
 */
public class RaEntity extends HostileEntity implements GeoEntity {

    /* ========== PHASE SYSTEM ========== */
    /**
     * Boss phases based on HP percentage.
     * Using percentages ensures changing max HP doesn't break phase transitions.
     */
    public enum RaPhase {
        PHASE_1_AWAKENED(1.0f, 0.6f), // 100% - 60% HP
        PHASE_2_SOLAR_WRATH(0.6f, 0.3f), // 60% - 30% HP
        PHASE_3_DIVINE_FURY(0.3f, 0.0f); // 30% - 0% HP

        private final float upperThreshold;
        private final float lowerThreshold;

        RaPhase(float upper, float lower) {
            this.upperThreshold = upper;
            this.lowerThreshold = lower;
        }

        /**
         * Get phase based on current HP percentage.
         */
        public static RaPhase fromHealthPercent(float healthPercent) {
            if (healthPercent > PHASE_2_SOLAR_WRATH.upperThreshold) {
                return PHASE_1_AWAKENED;
            } else if (healthPercent > PHASE_3_DIVINE_FURY.upperThreshold) {
                return PHASE_2_SOLAR_WRATH;
            } else {
                return PHASE_3_DIVINE_FURY;
            }
        }
    }

    /**
     * Combat states for animation and behavior control.
     */
    public enum RaCombatState {
        IDLE(0),
        WALKING(1),
        FLYING(2),
        MELEE(3),
        GROUND_SMACK(4),
        SHARD_ATTACK(5),
        FLYING_STAFF_ATTACK(6),
        HIBERNATING(7);

        private final int id;

        RaCombatState(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static RaCombatState fromId(int id) {
            for (RaCombatState state : values()) {
                if (state.id == id)
                    return state;
            }
            return IDLE;
        }
    }

    /* ========== ANIMATION CONSTANTS ========== */
    private static final String ANIM_IDLE = "ra.idle";
    private static final String ANIM_WALKING = "ra.walking";
    private static final String ANIM_FLYING = "ra.flying";
    private static final String ANIM_MELEE = "ra.melee";
    private static final String ANIM_GROUND_SMACK = "ra.flying_ground_smack";
    private static final String ANIM_SHARD_ATTACK = "ra.shard_attack";
    private static final String ANIM_FLYING_STAFF_ATTACK = "ra.flying_staff_attack";
    private static final String ANIM_HIBERNATION = "ra.hibernation";
    private static final String ANIM_DEATH = "ra.death";

    private static final String CONTROLLER_MOVEMENT = "movement";
    private static final String CONTROLLER_ATTACK = "attack";
    private static final String CONTROLLER_DEATH = "death";

    private static final int TRANSITION_TICKS = 8;

    /* ========== FLIGHT CONSTANTS ========== */
    private static final float MAX_FLIGHT_HEIGHT = 6.0f; // Max blocks above ground
    private static final float FLIGHT_SPEED = 0.08f; // Vertical movement speed
    private static final float HOVER_SPEED = 0.04f; // Horizontal speed while flying

    /* ========== SYNCED DATA ========== */
    private static final TrackedData<Integer> COMBAT_STATE = DataTracker.registerData(
            RaEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> FLYING = DataTracker.registerData(
            RaEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> SUN_BEAM_SLICE_TICKS = DataTracker.registerData(
            RaEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> SUN_BEAM_DIR_X = DataTracker.registerData(
            RaEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> SUN_BEAM_DIR_Y = DataTracker.registerData(
            RaEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> SUN_BEAM_DIR_Z = DataTracker.registerData(
            RaEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> SHARD_ATTACK_TICKS = DataTracker.registerData(
            RaEntity.class, TrackedDataHandlerRegistry.INTEGER);

    /* ========== FIELDS ========== */
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private ServerBossBar bossBar;

    // AI Goals (server-side only, kept as references for tick updates)
    private RaFlightGoal flightGoal;
    private RaGroundSmackGoal groundSmackGoal;
    private RaShardAttackGoal shardAttackGoal;

    // Phase tracking
    private RaPhase currentPhase = RaPhase.PHASE_1_AWAKENED;

    // Animation timers (in ticks)
    private int actionAnimationTicks = 0;
    private int postActionGraceTicks = 0; // Grace period to maintain walking after actions
    private static final int POST_ACTION_GRACE_DURATION = 5; // Ticks

    // Flight state
    private double groundY = 0; // Y position of ground below Ra
    private double targetFlightHeight = 0; // Target hover height

    // Hibernation (stun) state
    private int hibernationTicks = 0;
    private static final int BASE_HIBERNATION_DURATION = 60; // 3 seconds base

    /*
     * ========== ANIMATION DURATIONS ==========
     * IMPORTANT: These values MUST match the animation_length in ra.animation.json!
     * Source of truth:
     * src/main/resources/assets/ancientcurse/animations/ra.animation.json
     *
     * When modifying animations in Blockbench, update BOTH:
     * 1. The animation JSON file
     * 2. The corresponding constant below
     *
     * Formula: duration_ticks = animation_length_seconds * 20
     *
     * Last verified against ra.animation.json: 2026-01-24
     */
    private static final int MELEE_DURATION = 60; // ra.melee: 3.0s
    private static final int GROUND_SMACK_DURATION = 60; // ra.flying_ground_smack: 3.0s
    private static final int SHARD_ATTACK_DURATION = 60; // ra.shard_attack: 3.0s
    private static final int FLYING_STAFF_ATTACK_DURATION = 60; // ra.flying_staff_attack: 3.0s
    public static final int SUN_BEAM_SLICE_DURATION = 40; // Projectile duration (merged into ground smack)

    // Debug animation cycling
    private int debugAnimationIndex = 0;
    private static final RaCombatState[] DEBUG_ANIMATION_CYCLE = {
            RaCombatState.IDLE,
            RaCombatState.WALKING,
            RaCombatState.FLYING,
            RaCombatState.MELEE,
            RaCombatState.GROUND_SMACK,
            RaCombatState.SHARD_ATTACK,
            RaCombatState.FLYING_STAFF_ATTACK,
            RaCombatState.HIBERNATING
    };

    /* ========== CONSTRUCTOR ========== */
    public RaEntity(EntityType<? extends RaEntity> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(false); // Gravity handled manually when flying
    }

    /* ========== INITIALIZATION ========== */
    @Override
    protected void initGoals() {
        // Priority 0: Survival
        this.goalSelector.add(0, new SwimGoal(this));

        // Priority 1: Ra-specific combat goals (server-side AI)
        this.flightGoal = new RaFlightGoal(this);
        this.groundSmackGoal = new RaGroundSmackGoal(this);
        this.shardAttackGoal = new RaShardAttackGoal(this);
        this.goalSelector.add(1, this.groundSmackGoal); // Ground smack takes priority over melee
        this.goalSelector.add(2, this.shardAttackGoal); // Shard attack
        this.goalSelector.add(3, this.flightGoal); // Flight behavior

        // Priority 3: Standard melee when in range
        this.goalSelector.add(3, new MeleeAttackGoal(this, 1.2D, false));

        // Priority 4-5: Movement
        this.goalSelector.add(4, new WanderAroundFarGoal(this, 1.0D));
        this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(6, new LookAroundGoal(this));

        // Targeting
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(COMBAT_STATE, RaCombatState.IDLE.getId());
        this.dataTracker.startTracking(FLYING, false);
        this.dataTracker.startTracking(SUN_BEAM_SLICE_TICKS, 0);
        this.dataTracker.startTracking(SUN_BEAM_DIR_X, 0.0f);
        this.dataTracker.startTracking(SUN_BEAM_DIR_Y, -1.0f);
        this.dataTracker.startTracking(SUN_BEAM_DIR_Z, 0.0f);
        this.dataTracker.startTracking(SHARD_ATTACK_TICKS, 0);
    }

    /* ========== ATTRIBUTES ========== */
    public static DefaultAttributeContainer.Builder createRaAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0)
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 200.0)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 0.5)
                .add(EntityAttributes.GENERIC_ARMOR, 8.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8);
    }

    /* ========== PHASE & STATE GETTERS ========== */
    /**
     * Get current HP as a percentage (0.0 to 1.0).
     */
    public float getHealthPercent() {
        return this.getHealth() / this.getMaxHealth();
    }

    /**
     * Get current phase based on HP percentage.
     */
    public RaPhase getCurrentPhase() {
        return RaPhase.fromHealthPercent(getHealthPercent());
    }

    /**
     * Check if phase has changed and handle transitions.
     */
    private void updatePhase() {
        RaPhase newPhase = getCurrentPhase();
        if (newPhase != this.currentPhase) {
            onPhaseChange(this.currentPhase, newPhase);
            this.currentPhase = newPhase;
        }
    }

    /**
     * Called when Ra transitions to a new phase.
     */
    private void onPhaseChange(RaPhase oldPhase, RaPhase newPhase) {
        // TODO: Add phase transition effects (particles, sounds, etc.)
        // Could trigger a special animation or brief invulnerability
    }

    /* ========== COMBAT STATE MANAGEMENT ========== */
    public RaCombatState getCombatState() {
        return RaCombatState.fromId(this.dataTracker.get(COMBAT_STATE));
    }

    public void setCombatState(RaCombatState state) {
        this.dataTracker.set(COMBAT_STATE, state.getId());
    }

    /**
     * Trigger a combat action with a specified duration.
     */
    public void triggerAction(RaCombatState state, int duration) {
        setCombatState(state);
        this.actionAnimationTicks = duration;
    }

    public boolean isFlying() {
        return this.dataTracker.get(FLYING);
    }

    public void setFlying(boolean flying) {
        this.dataTracker.set(FLYING, flying);
        this.setNoGravity(flying);
    }

    public boolean isHibernating() {
        return getCombatState() == RaCombatState.HIBERNATING;
    }

    public boolean isPerformingAction() {
        RaCombatState state = getCombatState();
        return state == RaCombatState.MELEE ||
                state == RaCombatState.GROUND_SMACK ||
                state == RaCombatState.SHARD_ATTACK ||
                state == RaCombatState.FLYING_STAFF_ATTACK;
    }

    /* ========== TICK & STATE UPDATES ========== */
    @Override
    public void tick() {
        super.tick();

        if (!this.getWorld().isClient) {
            // Update phase based on HP
            updatePhase();

            // Update AI goal timers (server-side only)
            if (this.flightGoal != null) {
                this.flightGoal.tickGroundTimer();
            }
            if (this.groundSmackGoal != null) {
                this.groundSmackGoal.tickCooldown();
            }
            if (this.shardAttackGoal != null) {
                this.shardAttackGoal.tickCooldown();
            }

            // Update sun beam slice ticks for client-side rendering
            int beamTicks = this.dataTracker.get(SUN_BEAM_SLICE_TICKS);
            if (beamTicks > 0) {
                this.dataTracker.set(SUN_BEAM_SLICE_TICKS, beamTicks - 1);
            }

            // Handle hibernation (stun)
            if (this.hibernationTicks > 0) {
                this.hibernationTicks--;
                if (this.hibernationTicks == 0) {
                    breakOutOfHibernation();
                }
                return; // Skip other logic while stunned
            }

            // Handle action animation timers
            if (this.actionAnimationTicks > 0) {
                this.actionAnimationTicks--;
                if (this.actionAnimationTicks == 0) {
                    onActionComplete();
                }
            }

            // Decrement post-action grace period
            if (this.postActionGraceTicks > 0) {
                this.postActionGraceTicks--;
            }

            // Handle flight
            if (isFlying()) {
                tickFlight();
            }
        }
    }

    /**
     * Handle flight physics and movement.
     */
    private void tickFlight() {
        // Update ground reference
        this.groundY = findGroundY();

        double currentHeight = this.getY() - this.groundY;
        double heightDiff = this.targetFlightHeight - currentHeight;

        // Move toward target height
        if (Math.abs(heightDiff) > 0.1) {
            double verticalSpeed = Math.signum(heightDiff) * FLIGHT_SPEED;
            this.setVelocity(this.getVelocity().add(0, verticalSpeed, 0));
        }

        // Clamp to max height
        if (currentHeight > MAX_FLIGHT_HEIGHT) {
            this.setPosition(this.getX(), this.groundY + MAX_FLIGHT_HEIGHT, this.getZ());
            this.setVelocity(this.getVelocity().x, 0, this.getVelocity().z);
        }

        // Fix rotation: Sync body yaw to head yaw while flying
        // Since RaFlightGoal makes him face the target with LookControl,
        // we need to ensure his body turns with his head while hovering.
        this.bodyYaw = this.headYaw;
        this.setYaw(this.headYaw);
    }

    /**
     * Find the Y position of the ground below Ra.
     */
    private double findGroundY() {
        BlockPos pos = this.getBlockPos();
        for (int y = pos.getY(); y > this.getWorld().getBottomY(); y--) {
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
            if (!this.getWorld().getBlockState(checkPos).isAir()) {
                return y + 1;
            }
        }
        return this.getWorld().getBottomY();
    }

    /**
     * Called when an action animation completes.
     */
    private void onActionComplete() {
        RaCombatState currentState = getCombatState();

        // Return to appropriate idle state
        if (isFlying()) {
            setCombatState(RaCombatState.FLYING);
        } else {
            setCombatState(RaCombatState.IDLE);
        }

        // Start grace period to maintain walking animation if we have a target
        if (this.getTarget() != null) {
            this.postActionGraceTicks = POST_ACTION_GRACE_DURATION;
        }
    }

    /* ========== COMBAT ACTIONS ========== */
    @Override
    public boolean tryAttack(net.minecraft.entity.Entity target) {
        if (isHibernating() || isPerformingAction()) {
            return false;
        }

        // Choose attack based on phase and state
        if (isFlying()) {
            triggerFlyingStaffAttack();
        } else {
            triggerMeleeAttack();
        }

        return super.tryAttack(target);
    }

    public void triggerMeleeAttack() {
        setCombatState(RaCombatState.MELEE);
        this.actionAnimationTicks = MELEE_DURATION;
    }

    public void triggerGroundSmack() {
        if (isFlying()) {
            setFlying(false);
        }
        setCombatState(RaCombatState.GROUND_SMACK);
        this.actionAnimationTicks = GROUND_SMACK_DURATION;
        this.dataTracker.set(SUN_BEAM_SLICE_TICKS, GROUND_SMACK_DURATION);
    }

    public void triggerShardAttack() {
        if (isPerformingAction())
            return;
        setCombatState(RaCombatState.SHARD_ATTACK);
        this.actionAnimationTicks = SHARD_ATTACK_DURATION;
    }

    public void triggerFlyingStaffAttack() {
        if (!isFlying() || isPerformingAction())
            return;
        setCombatState(RaCombatState.FLYING_STAFF_ATTACK);
        this.actionAnimationTicks = FLYING_STAFF_ATTACK_DURATION;
    }

    public void setSunBeamDirection(Vec3d dir) {
        this.dataTracker.set(SUN_BEAM_DIR_X, (float) dir.x);
        this.dataTracker.set(SUN_BEAM_DIR_Y, (float) dir.y);
        this.dataTracker.set(SUN_BEAM_DIR_Z, (float) dir.z);
    }

    public Vec3d getSunBeamDirection() {
        return new Vec3d(
                this.dataTracker.get(SUN_BEAM_DIR_X),
                this.dataTracker.get(SUN_BEAM_DIR_Y),
                this.dataTracker.get(SUN_BEAM_DIR_Z));
    }

    /**
     * Check if currently performing sun beam slice attack
     */
    public boolean isPerformingSunBeamSlice() {
        return getCombatState() == RaCombatState.GROUND_SMACK
                && this.dataTracker.get(SUN_BEAM_SLICE_TICKS) > 0;
    }

    /**
     * Check if currently performing shard attack
     */
    public boolean isPerformingShardAttack() {
        return getCombatState() == RaCombatState.SHARD_ATTACK
                && this.dataTracker.get(SHARD_ATTACK_TICKS) > 0;
    }

    /**
     * Get remaining shard attack ticks (for client-side rendering)
     */
    public int getShardAttackTicks() {
        return this.dataTracker.get(SHARD_ATTACK_TICKS);
    }

    public void setShardAttackTicks(int ticks) {
        this.dataTracker.set(SHARD_ATTACK_TICKS, ticks);
    }

    /**
     * Get remaining sun beam slice ticks (for client-side rendering)
     */
    public int getSunBeamSliceTicks() {
        return this.dataTracker.get(SUN_BEAM_SLICE_TICKS);
    }

    public void setSunBeamTicks(int ticks) {
        this.dataTracker.set(SUN_BEAM_SLICE_TICKS, ticks);
    }

    /* ========== DEBUG INTERACTIONS ========== */
    /**
     * Debug stick interaction for testing animations.
     * OP players can use debug stick to test Ra's animations:
     * - SHIFT + right-click: Cycle through animations and trigger next one
     * - Right-click (no shift): Show current state debug info
     */
    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack heldItem = player.getStackInHand(hand);

        // Check if player is OP and holding debug stick
        if (heldItem.isOf(Items.DEBUG_STICK) && player.hasPermissionLevel(2)) {
            if (!this.getWorld().isClient) {
                if (player.isSneaking()) {
                    // SHIFT + click: Cycle to next animation
                    debugCycleAnimation(player);
                } else {
                    // Regular click: Show current state info
                    debugShowCurrentState(player);
                }
            }
            return ActionResult.SUCCESS;
        }

        return super.interactMob(player, hand);
    }

    /**
     * Cycle through animations for debugging.
     */
    private void debugCycleAnimation(PlayerEntity player) {
        RaCombatState targetState = DEBUG_ANIMATION_CYCLE[debugAnimationIndex];
        debugAnimationIndex = (debugAnimationIndex + 1) % DEBUG_ANIMATION_CYCLE.length;

        // Set player as target so Ra faces them
        this.setTarget(player);

        // Get the animation name that SHOULD play for this state
        String expectedAnimation = getAnimationNameForState(targetState);
        int duration = getAnimationDurationForState(targetState);

        // Handle state-specific setup
        switch (targetState) {
            case FLYING, FLYING_STAFF_ATTACK -> {
                if (!isFlying()) {
                    this.groundY = findGroundY();
                    this.targetFlightHeight = 4.0f;
                    setFlying(true);
                }
            }
            case IDLE, WALKING, MELEE, GROUND_SMACK, SHARD_ATTACK, HIBERNATING -> {
                if (isFlying()) {
                    setFlying(false);
                }
            }
        }

        // Set the combat state (set to IDLE first if we're forcing a goal to bypass
        // checks)
        // If the state has a dedicated Goal with forceStart, we use that for full
        // effect
        if (targetState == RaCombatState.GROUND_SMACK && this.groundSmackGoal != null) {
            setCombatState(RaCombatState.IDLE);
            this.groundSmackGoal.forceStart();
        } else if (targetState == RaCombatState.SHARD_ATTACK && this.shardAttackGoal != null) {
            setCombatState(RaCombatState.IDLE);
            this.shardAttackGoal.forceStart();
        } else {
            setCombatState(targetState);
            this.actionAnimationTicks = duration;
        }

        // Build detailed debug output
        StringBuilder msg = new StringBuilder();
        msg.append("§6[Ra Debug] §eTriggered: §f").append(targetState.name());
        msg.append("\n§7Animation: §b").append(expectedAnimation);
        msg.append("\n§7Duration: §f").append(duration).append(" ticks (")
                .append(String.format("%.1f", duration / 20.0)).append("s)");
        msg.append("\n§7Flying: §f").append(isFlying());
        msg.append("\n§7Next: §8").append(DEBUG_ANIMATION_CYCLE[debugAnimationIndex].name());

        // Send to chat (not action bar) for full visibility
        player.sendMessage(Text.literal(msg.toString()), false);

        // Play a sound for feedback
        this.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 0.5f, 1.5f);
    }

    /**
     * Show current animation state for debugging.
     * Double-click (within 10 ticks) toggles console logging.
     */
    private int lastDebugClickTick = 0;

    private void debugShowCurrentState(PlayerEntity player) {
        // Check for double-click to toggle console logging
        int currentTick = player.age;
        if (currentTick - lastDebugClickTick < 10) {
            animationDebugEnabled = !animationDebugEnabled;
            player.sendMessage(Text.literal("§6[Ra Debug] §eConsole logging: " +
                    (animationDebugEnabled ? "§aENABLED" : "§cDISABLED")), false);
            lastDebugClickTick = 0;
            return;
        }
        lastDebugClickTick = currentTick;

        RaCombatState combatState = getCombatState();
        String expectedAnimation = getAnimationNameForState(combatState);

        // Determine what the movement controller would play
        String movementAnim = "§8(stopped - attack playing)";
        if (!isPerformingAction() && !isDead()) {
            if (combatState == RaCombatState.HIBERNATING) {
                movementAnim = ANIM_HIBERNATION;
            } else if (isFlying()) {
                movementAnim = ANIM_FLYING;
            } else if (this.getVelocity().horizontalLengthSquared() > 0.003) {
                movementAnim = ANIM_WALKING;
            } else {
                movementAnim = ANIM_IDLE;
            }
        }

        // Determine what the attack controller would play
        String attackAnim = "§8(stopped)";
        if (isPerformingAction()) {
            attackAnim = expectedAnimation;
        }

        StringBuilder msg = new StringBuilder();
        msg.append("§6[Ra Debug] §eCurrent State:");
        msg.append("\n§7Combat State: §f").append(combatState.name()).append(" §8(id: ").append(combatState.getId())
                .append(")");
        msg.append("\n§7Phase: §f").append(getCurrentPhase().name());
        msg.append("\n§7Health: §c").append(String.format("%.1f", getHealth())).append("§7/§c")
                .append(String.format("%.0f", getMaxHealth()));
        msg.append(" §8(").append(String.format("%.0f%%", getHealthPercent() * 100)).append(")");
        msg.append("\n§7Flying: §f").append(isFlying());
        msg.append("\n§7Performing Action: §f").append(isPerformingAction());
        msg.append("\n§7Action Ticks Left: §f").append(actionAnimationTicks);
        if (getShardAttackTicks() > 0) {
            msg.append("\n§7Shard Ticks: §f").append(getShardAttackTicks());
        }
        msg.append("\n§7---");
        msg.append("\n§7Movement Controller: §b").append(movementAnim);
        msg.append("\n§7Attack Controller: §b").append(attackAnim);
        if (getSunBeamSliceTicks() > 0) {
            msg.append("\n§7Sun Beam Ticks: §f").append(getSunBeamSliceTicks());
        }
        if (isHibernating()) {
            msg.append("\n§7Hibernation Ticks: §f").append(getHibernationTicks());
        }
        msg.append("\n§7---");
        msg.append("\n§7Console Log: ").append(animationDebugEnabled ? "§aON" : "§cOFF");
        msg.append(" §8(double-click to toggle)");

        player.sendMessage(Text.literal(msg.toString()), false);
    }

    /**
     * Get the animation name that should play for a given combat state.
     */
    private String getAnimationNameForState(RaCombatState state) {
        return switch (state) {
            case IDLE -> ANIM_IDLE;
            case WALKING -> ANIM_WALKING;
            case FLYING -> ANIM_FLYING;
            case MELEE -> ANIM_MELEE;
            case GROUND_SMACK -> ANIM_GROUND_SMACK;
            case SHARD_ATTACK -> ANIM_SHARD_ATTACK;
            case FLYING_STAFF_ATTACK -> ANIM_FLYING_STAFF_ATTACK;
            case HIBERNATING -> ANIM_HIBERNATION;
        };
    }

    /**
     * Get the duration for an animation state.
     */
    private int getAnimationDurationForState(RaCombatState state) {
        return switch (state) {
            case IDLE, WALKING, FLYING, HIBERNATING -> 60; // 3 seconds for looping anims
            case MELEE -> MELEE_DURATION;
            case GROUND_SMACK -> GROUND_SMACK_DURATION;
            case SHARD_ATTACK -> SHARD_ATTACK_DURATION;
            case FLYING_STAFF_ATTACK -> FLYING_STAFF_ATTACK_DURATION;
        };
    }

    /* ========== FLIGHT CONTROL ========== */
    /**
     * Make Ra take flight to a target height.
     */
    public void takeOff(float targetHeight) {
        if (isHibernating() || isFlying())
            return;

        this.groundY = findGroundY();
        this.targetFlightHeight = Math.min(targetHeight, MAX_FLIGHT_HEIGHT);
        setFlying(true);
        setCombatState(RaCombatState.FLYING);
    }

    /**
     * Make Ra land on the ground.
     */
    public void land() {
        if (!isFlying())
            return;

        this.targetFlightHeight = 0;
        setFlying(false);
        setCombatState(RaCombatState.IDLE);
    }

    /* ========== HIBERNATION (STUN) SYSTEM ========== */
    /**
     * Put Ra into hibernation (stunned state).
     * Duration is reduced based on phase for increased difficulty.
     */
    public void triggerHibernation() {
        if (isHibernating())
            return;

        // Reduce stun duration in later phases
        int duration = switch (getCurrentPhase()) {
            case PHASE_1_AWAKENED -> BASE_HIBERNATION_DURATION;
            case PHASE_2_SOLAR_WRATH -> (int) (BASE_HIBERNATION_DURATION * 0.7f);
            case PHASE_3_DIVINE_FURY -> (int) (BASE_HIBERNATION_DURATION * 0.4f);
        };

        this.hibernationTicks = duration;
        setCombatState(RaCombatState.HIBERNATING);

        // Force landing if flying
        if (isFlying()) {
            setFlying(false);
        }
    }

    /**
     * Called when Ra breaks out of hibernation.
     */
    private void breakOutOfHibernation() {
        setCombatState(RaCombatState.IDLE);

        // Reset flight goal timer
        if (this.flightGoal != null) {
            this.flightGoal.forceResetTimer();
        }

        // Play break-out sound
        this.playSound(SoundEvents.ENTITY_IRON_GOLEM_REPAIR, 1.0f, 0.5f);
    }

    /**
     * Get remaining hibernation time in ticks.
     */
    public int getHibernationTicks() {
        return this.hibernationTicks;
    }

    /* ========== BOSS BAR ========== */
    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        if (this.bossBar == null) {
            this.bossBar = new ServerBossBar(Text.literal("Ra"), BossBar.Color.YELLOW, BossBar.Style.NOTCHED_20);
            this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());
        }
        this.bossBar.addPlayer(player);
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        if (this.bossBar != null) {
            this.bossBar.removePlayer(player);
        }
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (this.bossBar != null) {
            this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());
        }
    }

    /* ========== DAMAGE & DEATH ========== */
    private boolean hasDroppedLoot = false;

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);
    }

    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        // Prevent double loot drops
        if (hasDroppedLoot)
            return;
        hasDroppedLoot = true;

        super.dropLoot(source, causedByPlayer);
        if (causedByPlayer && this.random.nextFloat() < 0.8f) {
            this.dropStack(new ItemStack(ModItems.STAFF_OF_RA, 1));
        }
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        // Ra is immune to fall damage - he's a flying sun god
        return false;
    }

    /* ========== SOUNDS ========== */
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_GENERIC_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_GENERIC_DEATH;
    }

    /* ========== GECKOLIB ANIMATION SYSTEM ========== */
    // Debug tracking for animation changes
    private String lastMovementAnim = "";
    private String lastAttackAnim = "";
    private boolean animationDebugEnabled = false;

    /**
     * Enable/disable animation debug logging to console.
     */
    public void setAnimationDebugEnabled(boolean enabled) {
        this.animationDebugEnabled = enabled;
    }

    public boolean isAnimationDebugEnabled() {
        return this.animationDebugEnabled;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers
                .add(new AnimationController<>(this, CONTROLLER_MOVEMENT, TRANSITION_TICKS, this::movementController));
        controllers.add(new AnimationController<>(this, CONTROLLER_ATTACK, TRANSITION_TICKS, this::attackController));
        controllers.add(new AnimationController<>(this, CONTROLLER_DEATH, 0, this::deathController));
    }

    /**
     * Movement controller - handles base movement animations.
     * Idle, walking, flying, and hibernation.
     * Stops during attacks to let attack controller take over.
     */
    private PlayState movementController(AnimationState<RaEntity> state) {
        // Stop all movement animation when dead - let death controller handle it
        if (this.isDead()) {
            logAnimationChange("movement", "(STOPPED - dead)");
            return PlayState.STOP;
        }

        RaCombatState combatState = getCombatState();

        // Stop movement animation during attacks - let attack controller handle it
        if (isPerformingAction()) {
            logAnimationChange("movement", "(STOPPED - action playing)");
            return PlayState.STOP;
        }

        String animToPlay;

        // Hibernation overrides movement
        if (combatState == RaCombatState.HIBERNATING) {
            animToPlay = ANIM_HIBERNATION;
            state.getController().setAnimation(
                    RawAnimation.begin().thenLoop(animToPlay));
            logAnimationChange("movement", animToPlay);
            return PlayState.CONTINUE;
        }

        // Flying state
        if (isFlying()) {
            animToPlay = ANIM_FLYING;
            state.getController().setAnimation(
                    RawAnimation.begin().thenLoop(animToPlay));
            logAnimationChange("movement", animToPlay);
            return PlayState.CONTINUE;
        }

        // Ground movement
        // FIX: Also continue walking during grace period after attacks if we have a
        // target
        // This prevents the jarring IDLE pause when velocity momentarily drops between
        // pathfinding updates
        boolean inGracePeriod = postActionGraceTicks > 0 && this.getTarget() != null;

        if (state.isMoving() || inGracePeriod) {
            animToPlay = ANIM_WALKING;
        } else {
            animToPlay = ANIM_IDLE;
        }
        state.getController().setAnimation(
                RawAnimation.begin().thenLoop(animToPlay));
        logAnimationChange("movement", animToPlay);

        return PlayState.CONTINUE;
    }

    /**
     * Attack controller - handles all attack animations.
     * Plays once per attack, then stops.
     */
    private PlayState attackController(AnimationState<RaEntity> state) {
        // Stop attack animation when dead - let death controller handle it
        if (this.isDead()) {
            logAnimationChange("attack", "(STOPPED - dead)");
            return PlayState.STOP;
        }

        RaCombatState combatState = getCombatState();

        String animation = switch (combatState) {
            case MELEE -> ANIM_MELEE;
            case GROUND_SMACK -> ANIM_GROUND_SMACK;
            case SHARD_ATTACK -> ANIM_SHARD_ATTACK;
            case FLYING_STAFF_ATTACK -> ANIM_FLYING_STAFF_ATTACK;
            default -> null;
        };

        if (animation != null) {
            // Only set the animation if it's not already playing to prevent restarting it
            // every tick
            if (state.getController().getCurrentAnimation() == null ||
                    !state.getController().getCurrentAnimation().animation().name().equals(animation)) {

                state.getController().setAnimation(RawAnimation.begin().thenPlay(animation));
                logAnimationChange("attack", animation);
            }
            return PlayState.CONTINUE;
        }

        logAnimationChange("attack", "(STOPPED - no attack)");
        return PlayState.STOP;
    }

    /**
     * Death controller - highest priority, overrides everything.
     */
    private PlayState deathController(AnimationState<RaEntity> state) {
        if (this.isDead()) {
            state.getController().setAnimation(
                    RawAnimation.begin().thenPlayAndHold(ANIM_DEATH));
            logAnimationChange("death", ANIM_DEATH);
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }

    /**
     * Log animation changes for debugging.
     * Only logs when debug is enabled and animation actually changed.
     */
    private void logAnimationChange(String controller, String animation) {
        if (!animationDebugEnabled)
            return;

        boolean changed = false;
        if (controller.equals("movement") && !animation.equals(lastMovementAnim)) {
            lastMovementAnim = animation;
            changed = true;
        } else if (controller.equals("attack") && !animation.equals(lastAttackAnim)) {
            lastAttackAnim = animation;
            changed = true;
        } else if (controller.equals("death")) {
            changed = true; // Always log death
        }

        if (changed) {
            System.out.println("[Ra Animation] " + controller.toUpperCase() + " -> " + animation +
                    " | State: " + getCombatState().name() +
                    " | Flying: " + isFlying() +
                    " | ActionTicks: " + actionAnimationTicks);
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
