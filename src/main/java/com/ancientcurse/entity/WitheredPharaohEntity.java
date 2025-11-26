package com.ancientcurse.entity;
import com.ancientcurse.AncientCurse;
import com.ancientcurse.ModSounds;
import com.ancientcurse.entity.ai.AttackSolarSpireGoal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;

/**
 * The Withered Pharaoh entity - an ancient undead ruler of the desert
 */
public class WitheredPharaohEntity extends HostileEntity implements GeoEntity {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    
    private int magicAttackCooldown = 0;
    private int magicAttackWarmup = 0;
    private LivingEntity magicAttackTarget;
    private static final int MAGIC_COOLDOWN = 100; // 5 seconds cooldown (faster ranged attacks)
    private static final int MAGIC_WARMUP = 15; // 0.75 second warmup (faster response)
    private static final float MAGIC_ATTACK_RANGE = 20.0F; // Extended range for ranged attacks
    
    // Animation state tracking
    private boolean isScreaming = false;
    private int screamTime = 0;
    private static final int SCREAM_DURATION = 172; // 8.5833 seconds * 20 ticks (actual animation length)
    
    private boolean isFlying = false;
    private int flyingTime = 0;
    
    // Spawn animation tracking
    private boolean isSpawning = true; // Start with spawn animation
    private int spawnTime = 0;
    private static final int SPAWN_DURATION = 48; // 2.375 seconds * 20 ticks (actual animation length)
    
    // Animation state tracking for attacks
    private boolean isPerformingMagicAttack = false;
    private int magicAttackAnimationTime = 0;
    private static final int MAGIC_ATTACK_ANIMATION_DURATION = 119; // 5.9583 seconds * 20 ticks (actual animation length)

    private boolean isPerformingStaffAttack = false;
    private int staffAttackAnimationTime = 0;
    private static final int STAFF_ATTACK_ANIMATION_DURATION = 168; // 8.375 seconds * 20 ticks (actual animation length)

    private PharaohStaffAttackGoal staffAttackGoal;
    
    // Sound management
    private int grumbleSoundCooldown = 0; // Cooldown for random grumble sounds
    private static final int GRUMBLE_COOLDOWN_MIN = 200; // 10 seconds minimum between grumbles
    private static final int GRUMBLE_COOLDOWN_MAX = 600; // 30 seconds maximum
    private static final float GRUMBLE_CHANCE = 0.003f; // Chance per tick when idle/walking
    
    // Track if the entity is dead for animation purposes
    private static final TrackedData<Boolean> IS_DYING = DataTracker.registerData(WitheredPharaohEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private int deathTime = 0;
    private boolean playingDeathAnimation = false;

    // Track if stuck (for magic attack fallback)
    private boolean stuckForMagic = false;

    // The Zulmak entity that summoned this pharaoh (if any)
    private ZulmakEntity summoner;
    
    public WitheredPharaohEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.setPathfindingPenalty(PathNodeType.WATER, -1.0F);
        this.experiencePoints = 12; // Mid-tier minion XP (between regular mob and boss)
    }
    
    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(IS_DYING, false);
    }
    
    public void setSummoner(ZulmakEntity summoner) {
        this.summoner = summoner;
    }

    @Override
    protected void initGoals() {
        // Priority system: lower number = higher priority
        // ZOMBIE-LIKE BEHAVIOR: Relentlessly walk toward player, melee when close
        // Magic attack only as last resort when truly stuck

        this.goalSelector.add(1, new SwimGoal(this));

        // Staff melee attack - when very close (within 3 blocks)
        this.staffAttackGoal = new PharaohStaffAttackGoal(this);
        this.goalSelector.add(2, this.staffAttackGoal);

        // MAIN BEHAVIOR: Relentlessly chase player like a zombie
        // High priority, always tries to close distance
        this.goalSelector.add(3, new PharaohZombieChaseGoal(this, 1.0D));

        // Bodyguard: Patrol around summoner when they are downed (and we have no target)
        this.goalSelector.add(4, new PharaohBodyguardGoal(this));

        // Magic ranged attack - ONLY when stuck for extended time (last resort)
        this.goalSelector.add(5, new PharaohMagicAttackGoal(this));

        // Attack Solar Spire (environmental target)
        this.goalSelector.add(6, new AttackSolarSpireGoal(this, 1.0, 50, 8));

        // Idle behaviors - wander slowly when no target
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 0.6D));
        this.goalSelector.add(8, new LookAroundGoal(this));

        // Targeting goals - very aggressive target acquisition
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    // ZOMBIE-LIKE CHASE: Relentlessly pursue player, always walking toward them
    private class PharaohZombieChaseGoal extends Goal {
        private final WitheredPharaohEntity pharaoh;
        private final double speed;
        private LivingEntity target;
        private int pathUpdateCooldown;
        private int stuckTicks;
        private Vec3d lastPos;
        private static final int PATH_UPDATE_INTERVAL = 4; // Update path frequently for responsive chasing
        private static final float MELEE_RANGE = 2.5f; // Stop walking when this close

        public PharaohZombieChaseGoal(WitheredPharaohEntity entity, double speed) {
            this.pharaoh = entity;
            this.speed = speed;
            this.setControls(java.util.EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            // Don't chase during special states
            if (pharaoh.isSpawning || pharaoh.isDying() || pharaoh.isScreaming) {
                return false;
            }
            // Don't chase during attacks
            if (pharaoh.isPerformingMagicAttack || (pharaoh.staffAttackGoal != null && pharaoh.staffAttackGoal.isAttacking())) {
                return false;
            }

            this.target = pharaoh.getTarget();
            if (this.target == null || !this.target.isAlive()) {
                return false;
            }

            // Always chase if we have a target and aren't in melee range
            double distSq = pharaoh.squaredDistanceTo(this.target);
            return distSq > (MELEE_RANGE * MELEE_RANGE);
        }

        @Override
        public boolean shouldContinue() {
            // Stop if attacking
            if (pharaoh.isPerformingMagicAttack || (pharaoh.staffAttackGoal != null && pharaoh.staffAttackGoal.isAttacking())) {
                return false;
            }
            if (this.target == null || !this.target.isAlive()) {
                return false;
            }
            // Keep chasing until we're in melee range
            double distSq = pharaoh.squaredDistanceTo(this.target);
            return distSq > (MELEE_RANGE * MELEE_RANGE);
        }

        @Override
        public void start() {
            this.pathUpdateCooldown = 0;
            this.stuckTicks = 0;
            this.lastPos = pharaoh.getPos();
            // Immediately start moving
            if (this.target != null) {
                pharaoh.getNavigation().startMovingTo(this.target, this.speed);
            }
        }

        @Override
        public void tick() {
            if (this.target == null) return;

            // Always face the target while walking
            pharaoh.getLookControl().lookAt(this.target, 30.0F, 30.0F);

            // Update path frequently for responsive zombie-like chasing
            this.pathUpdateCooldown--;
            if (this.pathUpdateCooldown <= 0) {
                this.pathUpdateCooldown = PATH_UPDATE_INTERVAL;

                // Navigate directly to target - like a zombie, go straight for them
                pharaoh.getNavigation().startMovingTo(this.target, this.speed);

                // Debug logging
                if (pharaoh.age % 40 == 0) {
                    double vel = pharaoh.getVelocity().horizontalLengthSquared();
                    AncientCurse.LOGGER.info("Pharaoh chase goal: target={}, distance={}, velocity={}, navigating={}",
                        this.target.getName().getString(),
                        String.format("%.1f", Math.sqrt(pharaoh.squaredDistanceTo(this.target))),
                        String.format("%.4f", vel),
                        !pharaoh.getNavigation().isIdle());
                }
            }

            // Track if stuck for magic attack fallback
            if (pharaoh.age % 20 == 0) {
                double movedDist = pharaoh.getPos().squaredDistanceTo(this.lastPos);
                if (movedDist < 0.5) {
                    this.stuckTicks += 20;
                } else {
                    this.stuckTicks = Math.max(0, this.stuckTicks - 10);
                }
                this.lastPos = pharaoh.getPos();

                // If stuck for 5+ seconds, allow magic attack to potentially trigger
                if (this.stuckTicks > 100) {
                    pharaoh.stuckForMagic = true;
                } else {
                    pharaoh.stuckForMagic = false;
                }
            }
        }

        @Override
        public void stop() {
            this.target = null;
            this.stuckTicks = 0;
            pharaoh.stuckForMagic = false;
        }
    }
    
    // Custom aggressive look-at goal for better targeting
    private class PharaohLookAtTargetGoal extends Goal {
        private final WitheredPharaohEntity pharaoh;
        private LivingEntity target;
        private int lookTime;
        private final float maxLookDistance;

        public PharaohLookAtTargetGoal(WitheredPharaohEntity entity) {
            this.pharaoh = entity;
            this.maxLookDistance = 48.0F; // Extended range for boss
        }

        @Override
        public boolean canStart() {
            // Always try to look at target if we have one
            this.target = this.pharaoh.getTarget();
            if (this.target == null) {
                // If no target, look for nearby players
                this.target = this.pharaoh.getWorld().getClosestPlayer(this.pharaoh, this.maxLookDistance);
            }

            return this.target != null && this.target.isAlive() &&
                   this.pharaoh.squaredDistanceTo(this.target) <= (double)(this.maxLookDistance * this.maxLookDistance);
        }

        @Override
        public boolean shouldContinue() {
            if (!this.target.isAlive()) {
                return false;
            }

            if (this.pharaoh.squaredDistanceTo(this.target) > (double)(this.maxLookDistance * this.maxLookDistance)) {
                return false;
            }

            return this.lookTime > 0;
        }

        @Override
        public void start() {
            this.lookTime = 60 + this.pharaoh.getRandom().nextInt(60); // Look for 3-6 seconds
        }

        @Override
        public void tick() {
            // More aggressive head tracking - predict player movement
            if (this.target instanceof PlayerEntity) {
                PlayerEntity player = (PlayerEntity) this.target;
                // Lead the target based on their velocity
                double leadX = player.getX() + player.getVelocity().x * 3.0;
                double leadY = player.getEyeY();
                double leadZ = player.getZ() + player.getVelocity().z * 3.0;
                this.pharaoh.getLookControl().lookAt(leadX, leadY, leadZ, 45.0F, 45.0F);
            } else {
                this.pharaoh.getLookControl().lookAt(this.target, 45.0F, 45.0F);
            }
            --this.lookTime;
        }
    }
    
    // Magic attack - LAST RESORT only when truly stuck and can't reach player
    private class PharaohMagicAttackGoal extends Goal {
        private final WitheredPharaohEntity pharaoh;

        public PharaohMagicAttackGoal(WitheredPharaohEntity entity) {
            this.pharaoh = entity;
        }

        @Override
        public boolean canStart() {
            if (pharaoh.magicAttackCooldown > 0 || pharaoh.isPerformingMagicAttack) {
                return false;
            }
            if (pharaoh.isSpawning || pharaoh.isDying() || pharaoh.isScreaming) {
                return false;
            }
            // Don't shoot if staff attack is happening
            if (pharaoh.staffAttackGoal != null && pharaoh.staffAttackGoal.isAttacking()) {
                return false;
            }

            LivingEntity target = pharaoh.getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            }

            // ONLY use magic attack when:
            // 1. We've been stuck for a while (stuckForMagic flag set by chase goal)
            // 2. AND we can see the target
            // 3. AND target is within range
            // This makes the pharaoh prioritize walking like a zombie
            double distSq = pharaoh.squaredDistanceTo(target);
            boolean canSeeTarget = pharaoh.canSee(target);

            return pharaoh.stuckForMagic && canSeeTarget && distSq < MAGIC_ATTACK_RANGE * MAGIC_ATTACK_RANGE && distSq > 16.0;
        }

        @Override
        public void start() {
            pharaoh.magicAttackWarmup = MAGIC_WARMUP;
            pharaoh.magicAttackTarget = pharaoh.getTarget();
        }

        @Override
        public boolean shouldContinue() {
            return pharaoh.magicAttackWarmup > 0 || pharaoh.isPerformingMagicAttack;
        }

        @Override
        public void tick() {
            if (pharaoh.magicAttackWarmup > 0) {
                pharaoh.magicAttackWarmup--;

                // Enhanced targeting during warmup - predict target position
                if (pharaoh.magicAttackTarget != null && pharaoh.magicAttackTarget.isAlive()) {
                    // Stop moving during warmup
                    pharaoh.getNavigation().stop();

                    if (pharaoh.magicAttackTarget instanceof PlayerEntity) {
                        PlayerEntity player = (PlayerEntity) pharaoh.magicAttackTarget;
                        // Predict further ahead during warmup
                        double predictedX = player.getX() + player.getVelocity().x * 10.0;
                        double predictedZ = player.getZ() + player.getVelocity().z * 10.0;
                        pharaoh.getLookControl().lookAt(predictedX, player.getEyeY(), predictedZ, 50.0F, 50.0F);
                    } else {
                        pharaoh.getLookControl().lookAt(pharaoh.magicAttackTarget, 50.0F, 50.0F);
                    }
                }

                // When warmup is done, trigger the animation
                if (pharaoh.magicAttackWarmup == 0 && pharaoh.magicAttackTarget != null && pharaoh.magicAttackTarget.isAlive()) {
                    pharaoh.triggerMagicAttack();
                    pharaoh.magicAttackCooldown = MAGIC_COOLDOWN;
                }
            }
        }
    }

    // Bodyguard Goal: Patrol around summoner when they are downed
    private class PharaohBodyguardGoal extends Goal {
        private final WitheredPharaohEntity pharaoh;
        private final double speed = 1.0;

        public PharaohBodyguardGoal(WitheredPharaohEntity entity) {
            this.pharaoh = entity;
            this.setControls(java.util.EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            // Only if we have a living summoner
            if (pharaoh.summoner == null || !pharaoh.summoner.isAlive()) return false;
            
            // Don't interrupt attacks
            if (pharaoh.isPerformingMagicAttack || (pharaoh.staffAttackGoal != null && pharaoh.staffAttackGoal.isAttacking())) return false;

            // Start if summoner is downed (channeling regeneration)
            return pharaoh.summoner.isDowned();
        }
        
        @Override
        public boolean shouldContinue() {
            if (pharaoh.summoner == null || !pharaoh.summoner.isAlive() || !pharaoh.summoner.isDowned()) return false;
            if (pharaoh.isPerformingMagicAttack || (pharaoh.staffAttackGoal != null && pharaoh.staffAttackGoal.isAttacking())) return false;
            return true;
        }

        @Override
        public void tick() {
            if (pharaoh.summoner == null) return;

            // Patrol in a circle around summoner (radius ~5 blocks)
            // Offset based on entity ID to space them out
            double angle = (pharaoh.getWorld().getTime() * 0.05) + (pharaoh.getId() * 2.0); 
            double radius = 5.0;
            double x = pharaoh.summoner.getX() + Math.cos(angle) * radius;
            double z = pharaoh.summoner.getZ() + Math.sin(angle) * radius;
            
            pharaoh.getNavigation().startMovingTo(x, pharaoh.summoner.getY(), z, this.speed);
            pharaoh.getLookControl().lookAt(pharaoh.summoner, 30.0f, 30.0f); // Look at master
        }
    }

    // Staff melee attack at close range - zombie-like behavior
    private class PharaohStaffAttackGoal extends Goal {
        private final WitheredPharaohEntity pharaoh;
        private int attackCooldown = 0;
        private int staffAttackAnimationTime = 0;
        private boolean isPerformingStaffAttack = false;
        private static final int STAFF_ATTACK_DURATION = 168; // 8.375 seconds * 20 ticks
        private static final int STAFF_ATTACK_COOLDOWN = 40; // 2 seconds between attacks
        private static final float MELEE_RANGE = 3.0f; // Attack within 3 blocks (closer for zombie feel)
        private static final float MELEE_RANGE_SQ = MELEE_RANGE * MELEE_RANGE;

        public PharaohStaffAttackGoal(WitheredPharaohEntity entity) {
            this.pharaoh = entity;
            this.setControls(java.util.EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            if (attackCooldown > 0 || isPerformingStaffAttack || pharaoh.isPerformingMagicAttack) {
                return false;
            }
            if (pharaoh.isSpawning || pharaoh.isDying() || pharaoh.isScreaming) {
                return false;
            }

            LivingEntity target = pharaoh.getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            }

            double distSq = pharaoh.squaredDistanceTo(target);
            // Use staff attack when within melee range (0-3.5 blocks)
            return distSq <= MELEE_RANGE_SQ && pharaoh.canSee(target);
        }

        @Override
        public void start() {
            isPerformingStaffAttack = true;
            staffAttackAnimationTime = 0;
            pharaoh.getNavigation().stop();
        }

        @Override
        public boolean shouldContinue() {
            return isPerformingStaffAttack && staffAttackAnimationTime < STAFF_ATTACK_DURATION;
        }

        @Override
        public void tick() {
            // Decrement cooldown even when not attacking
            if (attackCooldown > 0 && !isPerformingStaffAttack) {
                attackCooldown--;
            }

            if (isPerformingStaffAttack) {
                staffAttackAnimationTime++;

                // Keep facing target during attack
                LivingEntity target = pharaoh.getTarget();
                if (target != null && target.isAlive()) {
                    pharaoh.getLookControl().lookAt(target, 50.0F, 50.0F);

                    // Rotate body to face target smoothly
                    double dx = target.getX() - pharaoh.getX();
                    double dz = target.getZ() - pharaoh.getZ();
                    float targetYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
                    pharaoh.setYaw(targetYaw);
                    pharaoh.bodyYaw = targetYaw;
                }

                // Stop movement during attack
                pharaoh.setVelocity(0, pharaoh.getVelocity().y, 0);
                pharaoh.getNavigation().stop();

                // Deal damage at the right moment in animation (around 3.4 seconds = 68 ticks)
                if (staffAttackAnimationTime == 68 && target != null && target.isAlive()) {
                    double distSq = pharaoh.squaredDistanceTo(target);
                    if (distSq <= 20.25) { // 4.5 block range for staff hit (slight lunge)
                        pharaoh.tryAttack(target);
                        // Add knockback effect
                        Vec3d knockback = new Vec3d(target.getX() - pharaoh.getX(), 0.3, target.getZ() - pharaoh.getZ()).normalize().multiply(1.5);
                        target.setVelocity(target.getVelocity().add(knockback));
                        target.velocityModified = true;

                        // Play impact sound
                        pharaoh.playSound(SoundEvents.ENTITY_IRON_GOLEM_ATTACK, 1.0F, 0.8F);
                    }
                }

                // End attack
                if (staffAttackAnimationTime >= STAFF_ATTACK_DURATION) {
                    isPerformingStaffAttack = false;
                    staffAttackAnimationTime = 0;
                    attackCooldown = STAFF_ATTACK_COOLDOWN;
                }
            }
        }

        @Override
        public void stop() {
            isPerformingStaffAttack = false;
            staffAttackAnimationTime = 0;
        }

        public boolean isAttacking() {
            return isPerformingStaffAttack;
        }

        public int getAnimationTime() {
            return staffAttackAnimationTime;
        }
    }
    
    /**
     * Set up entity attributes like health, movement speed, attack damage
     * Stats tuned for mid-tier minion role (summoned by Zulmak)
     */
    public static DefaultAttributeContainer.Builder createWitheredPharaohAttributes() {
        return HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0D) // Reduced health for minion role
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25D)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0D) // Reduced damage for minion role
            .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 1.0D)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0D) // Reduced follow range
            .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.4D); // Reduced knockback resistance
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        // Death controller needs highest priority (lowest number)
        controllerRegistrar.add(new AnimationController<>(this, "deathController", -1, this::deathPredicate));
        
        // Spawn controller (very high priority)
        controllerRegistrar.add(new AnimationController<>(this, "spawnController", 0, this::spawnPredicate));
        
        // Scream controller (high priority for intimidation)
        controllerRegistrar.add(new AnimationController<>(this, "screamController", 1, this::screamPredicate));
        
        // Flying controller (medium priority)
        controllerRegistrar.add(new AnimationController<>(this, "flyingController", 2, this::flyingPredicate));
        
        // Attack controllers (medium priority)
        controllerRegistrar.add(new AnimationController<>(this, "magicAttackController", 3, this::magicAttackPredicate));
        controllerRegistrar.add(new AnimationController<>(this, "staffAttackController", 3, this::staffAttackPredicate));
        
        // Main movement controller (lowest priority)
        controllerRegistrar.add(new AnimationController<>(this, "movementController", 4, this::movementPredicate));
    }
    
    @Override
    public void tick() {
        // Handle spawn animation
        if (this.isSpawning) {
            this.spawnTime++;
            
            // Prevent movement during spawn animation
            this.setVelocity(0, 0, 0);
            this.setTarget(null);
            this.getNavigation().stop();
            
            // Spawn particles - magical summoning effect (reduced frequency)
            if (this.getWorld() instanceof ServerWorld && this.spawnTime % 8 == 0) {
                ServerWorld serverWorld = (ServerWorld) this.getWorld();

                // Swirling soul particles rising up (magical emergence) - less frequent
                double angle = Math.toRadians(this.spawnTime * 15);
                double radius = 0.8 + (this.spawnTime / (double) SPAWN_DURATION) * 0.5;
                double spiralX = this.getX() + Math.cos(angle) * radius;
                double spiralZ = this.getZ() + Math.sin(angle) * radius;
                double spiralY = this.getY() + (this.spawnTime / (double) SPAWN_DURATION) * this.getHeight();

                serverWorld.spawnParticles(
                    ParticleTypes.SOUL,
                    spiralX, spiralY, spiralZ,
                    1, 0.05, 0.1, 0.05, 0.01
                );

                // Enchant particles at feet (mystical ground effect) - reduced count
                if (this.spawnTime % 16 == 0) {
                    serverWorld.spawnParticles(
                        ParticleTypes.ENCHANT,
                        this.getX(), this.getY() + 0.2, this.getZ(),
                        1, 0.4, 0.1, 0.4, 0.5
                    );
                }

                // Reverse portal particles as it materializes (later in animation) - reduced
                if (this.spawnTime > 20 && this.spawnTime % 16 == 0) {
                    serverWorld.spawnParticles(
                        ParticleTypes.REVERSE_PORTAL,
                        this.getX(), this.getY() + this.getHeight() * 0.5, this.getZ(),
                        1, 0.2, 0.3, 0.2, 0.02
                    );
                }
            }
            
            // Play magical summoning sounds at key moments
            if (this.spawnTime == 1) {
                // Initial summoning whoosh
                this.playSound(SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, 1.2f, 0.7f);
            } else if (this.spawnTime == 20) {
                // Mid-spawn magical shimmer
                this.playSound(SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 0.6f);
            }
            
            // End spawn animation
            if (this.spawnTime >= SPAWN_DURATION) {
                this.isSpawning = false;
                this.spawnTime = 0;
                
                // Play grumble sound when fully spawned (with slight pitch variation)
                this.playSound(ModSounds.WITHERED_PHARAOH_GRUMBLE, 1.2f, 0.9f + this.random.nextFloat() * 0.2f);
                // Play emergence sound - mystical completion
                this.playSound(SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, 1.5f, 0.8f);
                
                // Final burst of magical particles
                if (this.getWorld() instanceof ServerWorld) {
                    ServerWorld serverWorld = (ServerWorld) this.getWorld();
                    // Soul burst
                    serverWorld.spawnParticles(
                        ParticleTypes.SOUL,
                        this.getX(), this.getY() + 1.0, this.getZ(),
                        8, 0.4, 0.4, 0.4, 0.08
                    );
                    // Flash effect
                    serverWorld.spawnParticles(
                        ParticleTypes.FLASH,
                        this.getX(), this.getY() + 1.0, this.getZ(),
                        1, 0, 0, 0, 0
                    );
                }
            }
            
            // Skip normal tick logic during spawn animation
            return;
        }
        
        // Target Sync with Summoner
        if (this.summoner != null && this.summoner.isAlive() && !this.getWorld().isClient) {
            // If summoner has a target, switch to theirs (hive mind)
            LivingEntity summonerTarget = this.summoner.getTarget();
            if (summonerTarget != null && summonerTarget.isAlive()) {
                 if (this.getTarget() == null || this.getTarget() != summonerTarget) {
                     this.setTarget(summonerTarget);
                 }
            }
        }
        
        // If we're playing the death animation, handle it specially
        if (this.playingDeathAnimation) {
            this.deathTime++;
            
            // Prevent movement and other behaviors during death animation
            this.setVelocity(0, this.getVelocity().y * 0.98, 0);
            this.setTarget(null);
            
            // Death particles - reduced frequency and count
            if (this.getWorld() instanceof ServerWorld && this.deathTime % 10 == 0) {
                ServerWorld serverWorld = (ServerWorld) this.getWorld();

                // Soul particle escaping (just 1, less frequently)
                double offsetX = (this.random.nextDouble() - 0.5) * 0.5;
                double offsetZ = (this.random.nextDouble() - 0.5) * 0.5;
                double yVelocity = 0.1 + this.random.nextDouble() * 0.1;

                serverWorld.spawnParticles(
                    ParticleTypes.SOUL,
                    this.getX() + offsetX,
                    this.getY() + this.getHeight() * 0.5,
                    this.getZ() + offsetZ,
                    1, 0, yVelocity, 0, 0.02
                );

                // Dust particles as body crumbles (reduced count)
                if (this.deathTime > 60) {
                    serverWorld.spawnParticles(
                        new DustParticleEffect(new Vector3f(0.4f, 0.3f, 0.2f), 1.5f),
                        this.getX(), this.getY() + 0.5, this.getZ(),
                        2, 0.3, 0.3, 0.3, 0
                    );
                }
            }
            
            // Log every second during death animation (for debugging)
            if (!this.getWorld().isClient && this.deathTime % 20 == 0) {
                AncientCurse.LOGGER.debug("Death animation progress: " + this.deathTime + "/193 ticks");
            }
            
            // After death animation finishes (9.625 seconds = 193 ticks), actually remove entity
            if (this.deathTime >= 193) {
                if (!this.getWorld().isClient) {
                    AncientCurse.LOGGER.debug("Death animation complete, removing entity");
                    
                    // Final explosion of particles
                    if (this.getWorld() instanceof ServerWorld) {
                        ServerWorld serverWorld = (ServerWorld) this.getWorld();
                        serverWorld.spawnParticles(
                            ParticleTypes.POOF,
                            this.getX(), this.getY() + this.getHeight() * 0.5, this.getZ(),
                            20, 0.5, 0.5, 0.5, 0.05
                        );
                    }
                }
                this.playingDeathAnimation = false;
                this.remove(RemovalReason.KILLED); // Force removal after animation
                return;
            }
            
            // Skip normal tick logic during death animation
            return;
        }
        
        super.tick();
        
        // Update animation timers
        if (this.isPerformingMagicAttack) {
            this.magicAttackAnimationTime++;
            
            // Prevent movement during magic attack
            this.setVelocity(0, this.getVelocity().y, 0);
            this.getNavigation().stop();
            
            // Shoot the wither skull at 2 seconds (40 ticks) into the animation to better sync with arm raising
            if (this.magicAttackAnimationTime == 40 && this.magicAttackTarget != null && this.magicAttackTarget.isAlive()) {
                // Perform the actual attack
                if (this.getWorld() instanceof ServerWorld) {
                    ServerWorld serverWorld = (ServerWorld) this.getWorld();

                    // Play sound
                    this.playSound(SoundEvents.ENTITY_WITHER_SHOOT, 1.0F, 1.0F);

                    // Calculate staff tip position
                    Vec3d lookVec = this.getRotationVector();
                    double staffLength = 1.5;
                    double staffTipX = this.getX() + lookVec.x * staffLength;
                    double staffTipY = this.getY() + this.getHeight() * 0.7;
                    double staffTipZ = this.getZ() + lookVec.z * staffLength;

                    // Create wither skull projectile with prediction
                    if (this.magicAttackTarget == null || !this.magicAttackTarget.isAlive()) {
                        this.magicAttackTarget = null;
                        return;
                    }

                    // Calculate distance to target for travel time estimation
                    double distance = Math.sqrt(this.squaredDistanceTo(this.magicAttackTarget));
                    double projectileSpeed = 0.4; // Wither skull speed
                    double travelTime = distance / projectileSpeed;

                    // Predict target position based on velocity and travel time
                    Vec3d targetVelocity = this.magicAttackTarget.getVelocity();
                    double predictedX = this.magicAttackTarget.getX() + targetVelocity.x * travelTime;
                    double predictedY = this.magicAttackTarget.getBodyY(0.5D) + targetVelocity.y * travelTime * 0.5; // Less vertical prediction
                    double predictedZ = this.magicAttackTarget.getZ() + targetVelocity.z * travelTime;

                    // Calculate direction to predicted position
                    double d = predictedX - this.getX();
                    double e = predictedY - this.getBodyY(0.5D);
                    double f = predictedZ - this.getZ();

                    WitherSkullEntity witherSkull = new WitherSkullEntity(this.getWorld(), this, d, e, f);
                    witherSkull.setPos(staffTipX, staffTipY, staffTipZ);
                    this.getWorld().spawnEntity(witherSkull);

                    // Burst of particles when firing
                    for (int i = 0; i < 10; i++) {
                        serverWorld.spawnParticles(
                            ParticleTypes.SOUL_FIRE_FLAME,
                            staffTipX + (this.random.nextDouble() - 0.5) * 0.3,
                            staffTipY + (this.random.nextDouble() - 0.5) * 0.3,
                            staffTipZ + (this.random.nextDouble() - 0.5) * 0.3,
                            1, 0, 0, 0, 0.05
                        );
                    }
                }
            }
            
            // Add particle effects during magic attack from staff
            if (this.getWorld() instanceof ServerWorld && this.magicAttackAnimationTime % 3 == 0) {
                ServerWorld serverWorld = (ServerWorld) this.getWorld();
                Vec3d lookVec = this.getRotationVector();
                double staffLength = 1.5;
                double staffTipX = this.getX() + lookVec.x * staffLength;
                double staffTipY = this.getY() + this.getHeight() * 0.7;
                double staffTipZ = this.getZ() + lookVec.z * staffLength;
                
                // Magic buildup at staff tip
                serverWorld.spawnParticles(
                    ParticleTypes.WITCH,
                    staffTipX,
                    staffTipY,
                    staffTipZ,
                    2, 0.1, 0.1, 0.1, 0.01
                );
                
                // Energy swirls
                serverWorld.spawnParticles(
                    new DustParticleEffect(new Vector3f(0.7f, 0.0f, 1.0f), 0.8f),
                    staffTipX, staffTipY, staffTipZ,
                    3, 0.2, 0.2, 0.2, 0
                );
            }
            
            if (this.magicAttackAnimationTime >= MAGIC_ATTACK_ANIMATION_DURATION) {
                this.isPerformingMagicAttack = false;
                this.magicAttackAnimationTime = 0;
                this.magicAttackTarget = null;
            }
        }
        
        // Staff attack disabled for now
        
        // Update scream timer
        if (this.isScreaming) {
            this.screamTime++;
            
            // Prevent movement during scream
            this.setVelocity(0, this.getVelocity().y, 0);
            this.getNavigation().stop();
            
            // Scream particles - dark energy emanating from mouth (reduced)
            if (this.getWorld() instanceof ServerWorld && this.screamTime % 15 == 0) {
                ServerWorld serverWorld = (ServerWorld) this.getWorld();
                double mouthHeight = this.getY() + this.getHeight() * 0.85;
                Vec3d lookVec = this.getRotationVector();

                serverWorld.spawnParticles(
                    ParticleTypes.SCULK_SOUL,
                    this.getX() + lookVec.x * 0.5,
                    mouthHeight,
                    this.getZ() + lookVec.z * 0.5,
                    1, 0.1, 0.1, 0.1, 0.05
                );

                // Fear particles around the pharaoh (reduced)
                serverWorld.spawnParticles(
                    ParticleTypes.SMOKE,
                    this.getX(), mouthHeight, this.getZ(),
                    2, 0.5, 0.5, 0.5, 0.02
                );
            }
            
            if (this.screamTime >= SCREAM_DURATION) {
                this.isScreaming = false;
                this.screamTime = 0;
            }
        }
        
        // Update flying timer
        if (this.isFlying) {
            this.flyingTime++;
            
            // Flying particles - sand swirling beneath
            if (this.getWorld() instanceof ServerWorld && this.flyingTime % 2 == 0) {
                ServerWorld serverWorld = (ServerWorld) this.getWorld();
                
                // Swirling sand effect
                double angle = this.flyingTime * 0.1;
                for (int i = 0; i < 3; i++) {
                    double offsetAngle = angle + (Math.PI * 2 * i / 3);
                    serverWorld.spawnParticles(
                        new DustParticleEffect(new Vector3f(0.86f, 0.8f, 0.6f), 1.0f),
                        this.getX() + Math.cos(offsetAngle) * 1.5,
                        this.getY() - 0.5,
                        this.getZ() + Math.sin(offsetAngle) * 1.5,
                        1, 0, 0, 0, 0
                    );
                }
            }
        }
        
        // Update cooldowns
        if (this.magicAttackCooldown > 0) {
            this.magicAttackCooldown--;
        }
        
        // Prevent movement during magic attack warmup
        if (this.magicAttackWarmup > 0) {
            this.setVelocity(0, this.getVelocity().y, 0);
            this.getNavigation().stop();
        }
        
        // Random chance to start screaming (intimidation) - rare occurrence
        if (!this.isScreaming && !this.isDying() && !this.isSpawning && this.getTarget() != null && this.random.nextInt(800) == 0) {
            this.startScream();
        }
        
        // Check if should be flying - ONLY when falling or significantly elevated
        // The Pharaoh should primarily WALK toward players, flying is for special cases only
        LivingEntity target = this.getTarget();
        boolean isAttacking = this.isPerformingMagicAttack || (this.staffAttackGoal != null && this.staffAttackGoal.isAttacking());

        if (!this.isDying() && !this.isSpawning && !isAttacking) {
            boolean shouldFly = false;

            // Fly ONLY if falling significantly (not just small steps)
            if (!this.isOnGround() && this.getVelocity().y < -0.3) {
                shouldFly = true;
            }

            // Fly if very elevated above target (3+ blocks)
            if (target != null && !shouldFly) {
                double heightAboveTarget = this.getY() - target.getY();
                if (heightAboveTarget > 3.0 && !this.isOnGround()) {
                    shouldFly = true;
                }
            }

            if (shouldFly && !this.isFlying) {
                this.startFlying();
            } else if (!shouldFly && this.isFlying) {
                this.stopFlying();
            }
        } else if (this.isFlying) {
            this.stopFlying();
        }
        
        // Dynamic movement speed - increase speed when chasing a player
        // Reuse isAttacking variable from flying check above
        if (target instanceof PlayerEntity && target.isAlive() && this.canSee(target)) {
            // Calculate distance to target
            double distSq = this.squaredDistanceTo(target);

            // Speed boost when chasing but not during attacks
            if (!isAttacking && distSq > 16.0 && distSq < 100.0) { // Between 4-10 blocks away
                // Apply speed boost (30% faster)
                this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.325D);
            } else if (!isAttacking) {
                // Reset to normal speed
                this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.25D);
            }
        } else {
            // Reset to normal speed when not chasing
            if (!isAttacking) {
                this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.25D);
            }
        }
        
        // Handle random grumble sounds while idle or walking
        handleGrumbleSound();
    }
    
    /**
     * Handles random grumble sounds - plays occasionally when idle or walking
     * with slight pitch variation for variety
     */
    private void handleGrumbleSound() {
        // Only run on server
        if (this.getWorld().isClient) return;
        
        // Decrement cooldown
        if (grumbleSoundCooldown > 0) {
            grumbleSoundCooldown--;
            return;
        }
        
        // Don't grumble during attacks, spawning, dying, or screaming
        boolean isAttacking = this.isPerformingMagicAttack || (this.staffAttackGoal != null && this.staffAttackGoal.isAttacking());
        if (isAttacking || this.isSpawning || this.isDying() || this.isScreaming) {
            return;
        }
        
        // Random chance to grumble
        if (this.random.nextFloat() < GRUMBLE_CHANCE) {
            // Play grumble with random pitch variation (0.85 - 1.15)
            float pitch = 0.85f + this.random.nextFloat() * 0.3f;
            this.playSound(ModSounds.WITHERED_PHARAOH_GRUMBLE, 0.8f, pitch);
            
            // Set cooldown (10-30 seconds)
            grumbleSoundCooldown = GRUMBLE_COOLDOWN_MIN + this.random.nextInt(GRUMBLE_COOLDOWN_MAX - GRUMBLE_COOLDOWN_MIN);
        }
    }
    
    /**
     * Spawn animation controller
     */
    private <T extends GeoAnimatable> PlayState spawnPredicate(AnimationState<T> state) {
        if (this.isSpawning) {
            state.getController().setAnimation(RawAnimation.begin().then("animation.the_withered_pharaoh.spawn", Animation.LoopType.PLAY_ONCE));
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }
    
    /**
     * Main movement animation controller for idle/walk animations
     * Walking plays when moving on ground, idle when stationary
     * Flying controller handles aerial movement separately
     */
    private <T extends GeoAnimatable> PlayState movementPredicate(AnimationState<T> state) {
        // Don't play movement animations if dying, spawning, screaming, or performing any attacks
        boolean isAttacking = this.isPerformingMagicAttack || (this.staffAttackGoal != null && this.staffAttackGoal.isAttacking());
        if (this.isDying() || this.isSpawning || this.isScreaming || isAttacking) {
            return PlayState.STOP;
        }

        // If flying, let the flying controller handle it
        if (this.isFlying) {
            return PlayState.STOP;
        }

        // Check horizontal velocity for movement - more reliable than state.isMoving()
        double velocity = this.getVelocity().horizontalLengthSquared();
        boolean isMoving = velocity > 0.0001 || state.isMoving();

        // Walking animation when moving on the ground
        if (isMoving) {
            // Force animation to always play walk when moving
            state.getController().setAnimation(RawAnimation.begin().then("animation.the_withered_pharaoh.walk", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }

        // Idle animation when stationary
        state.getController().setAnimation(RawAnimation.begin().then("animation.the_withered_pharaoh.idle", Animation.LoopType.LOOP));
        return PlayState.CONTINUE;
    }
    
    /**
     * Magic attack animation controller
     */
    private <T extends GeoAnimatable> PlayState magicAttackPredicate(AnimationState<T> state) {
        // Don't play attack animations if dying or spawning
        if (this.isDying() || this.isSpawning) {
            return PlayState.STOP;
        }
        
        if (this.isPerformingMagicAttack) {
            // Force animation reset to ensure it plays from the beginning
            if (this.magicAttackAnimationTime <= 1) {
                state.getController().forceAnimationReset();
            }
            // Use magic_attack2 for wither skull projectile attacks
            state.getController().setAnimation(RawAnimation.begin().then("animation.the_withered_pharaoh.magic_attack2", Animation.LoopType.PLAY_ONCE));
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }
    
    /**
     * Staff attack animation controller
     */
    private <T extends GeoAnimatable> PlayState staffAttackPredicate(AnimationState<T> state) {
        // Don't play attack animations if dying or spawning
        if (this.isDying() || this.isSpawning) {
            return PlayState.STOP;
        }

        if (this.staffAttackGoal != null && this.staffAttackGoal.isAttacking()) {
            // Force animation reset to ensure it plays from the beginning
            if (this.staffAttackGoal.getAnimationTime() <= 1) {
                state.getController().forceAnimationReset();
            }
            state.getController().setAnimation(RawAnimation.begin().then("animation.the_withered_pharaoh.staff hit", Animation.LoopType.PLAY_ONCE));
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }
    
    /**
     * Scream animation controller
     */
    private <T extends GeoAnimatable> PlayState screamPredicate(AnimationState<T> state) {
        if (this.isScreaming) {
            state.getController().setAnimation(RawAnimation.begin().then("animation.the_withered_pharaoh.scream", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }
    
    /**
     * Flying animation controller
     * Plays flying animation when entity is airborne or moving toward distant target
     */
    private <T extends GeoAnimatable> PlayState flyingPredicate(AnimationState<T> state) {
        // Don't fly during death, spawn, scream, or attacks
        if (this.isDying() || this.isSpawning || this.isScreaming || this.isPerformingMagicAttack) {
            return PlayState.STOP;
        }

        if (this.isFlying) {
            state.getController().setAnimation(RawAnimation.begin().then("animation.the_withered_pharaoh.flying", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }
    
    /**
     * Death animation controller
     */
    private <T extends GeoAnimatable> PlayState deathPredicate(AnimationState<T> state) {
        if (this.isDying()) {
            // Force animation reset to ensure it plays from the beginning
            state.getController().forceAnimationReset();
            // Set animation with explicit name matching the animation file
            state.getController().setAnimation(RawAnimation.begin().then("animation.the_withered_pharaoh.death", Animation.LoopType.PLAY_ONCE));
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }
    
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_WITHER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_WITHER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_WITHER_DEATH;
    }

    @Override
    public boolean cannotDespawn() {
        // Allow despawn - Withered Pharaoh is a mid-tier minion, not a boss
        return false;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        // Make the pharaoh immediately target players who attack it
        if (source.getAttacker() instanceof PlayerEntity) {
            this.setTarget((LivingEntity)source.getAttacker());
            this.magicAttackCooldown = 20; // Short cooldown before potential magic attack
            
            // Immediately look at the attacker
            this.getLookControl().lookAt(source.getAttacker(), 45.0F, 45.0F);
            
            // Staff attack disabled for now
        }
        
        // Chance to scream when taking significant damage (reduced chance)
        if (amount > 6.0f && !this.isScreaming && this.random.nextInt(8) == 0) {
            this.startScream();
        }
        
        return super.damage(source, amount);
    }
    
    /**
     * Override onDeath to handle death animation
     */
    @Override
    public void onDeath(DamageSource source) {
        // Start death animation instead of immediately dying
        if (!this.isDying() && !this.isDead()) {
            // Set dying state
            this.setDying(true);
            this.playingDeathAnimation = true;
            this.deathTime = 0;
            
            // Play death sound
            this.playSound(this.getDeathSound(), this.getSoundVolume(), this.getSoundPitch());

            // Call the parent onDeath to trigger loot table drops
            // This ensures items are dropped according to the loot table
            super.onDeath(source);
        }
    }
    
    /**
     * Check if entity is in dying state
     */
    public boolean isDying() {
        return this.dataTracker.get(IS_DYING);
    }
    
    /**
     * Set entity dying state
     */
    public void setDying(boolean dying) {
        this.dataTracker.set(IS_DYING, dying);
    }
    
    /**
     * Start the scream animation
     */
    public void startScream() {
        if (!this.isScreaming && !this.isDying() && !this.isSpawning) {
            this.isScreaming = true;
            this.screamTime = 0;
            // Play a scary sound
            this.playSound(SoundEvents.ENTITY_WITHER_AMBIENT, 1.5f, 0.5f);
        }
    }
    
    /**
     * Start the flying animation
     */
    public void startFlying() {
        if (!this.isFlying && !this.isDying() && !this.isSpawning) {
            this.isFlying = true;
            this.flyingTime = 0;
        }
    }
    
    /**
     * Stop the flying animation
     */
    public void stopFlying() {
        this.isFlying = false;
        this.flyingTime = 0;
    }
    
    /**
     * Check if entity is screaming
     */
    public boolean isScreaming() {
        return this.isScreaming;
    }
    
    /**
     * Check if entity is flying
     */
    public boolean isFlying() {
        return this.isFlying;
    }
    
    /**
     * Trigger a magic attack animation (magic_attack2 - fires wither skull)
     */
    public void triggerMagicAttack() {
        if (!this.isPerformingMagicAttack && !this.isSpawning && !this.isDying()) {
            this.isPerformingMagicAttack = true;
            this.magicAttackAnimationTime = 0;
            // Stop flying during magic attack for better aim
            if (this.isFlying) {
                this.stopFlying();
            }
        }
    }
    
    /**
     * Trigger a staff attack animation (disabled)
     */
    public void triggerStaffAttack() {
        // Staff attack disabled for now
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
    
    @Override
    public double getTick(Object entity) {
        return getWorld().getTime();
    }
    
    /**
     * Override isAlive to prevent entity from being removed before death animation completes
     */
    @Override
    public boolean isAlive() {
        return super.isAlive() || this.playingDeathAnimation;
    }
    
    /**
     * Override tryAttack to trigger staff attack animation
     */
    @Override
    public boolean tryAttack(Entity target) {
        // Use normal melee attack for now
        return super.tryAttack(target);
    }
    
    /**
     * Add ambient particle effects
     */
    @Override
    public void tickMovement() {
        super.tickMovement();
        
        // Ambient particles when not in special animations
        if (!this.isDying() && !this.isScreaming && !this.isSpawning && this.age % 20 == 0) {
            if (this.getWorld() instanceof ServerWorld) {
                ServerWorld serverWorld = (ServerWorld) this.getWorld();
                
                // Occasional dark wisps
                if (this.random.nextInt(3) == 0) {
                    serverWorld.spawnParticles(
                        ParticleTypes.SMOKE,
                        this.getX() + (this.random.nextDouble() - 0.5) * 0.5,
                        this.getY() + this.getHeight() * 0.8,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 0.5,
                        1, 0, 0.05, 0, 0.01
                    );
                }
                
                // Glowing eyes particle effect
                Vec3d lookVec = this.getRotationVector();
                double eyeHeight = this.getY() + this.getHeight() * 0.9;
                
                serverWorld.spawnParticles(
                    new DustParticleEffect(new Vector3f(0.8f, 0.2f, 0.2f), 0.5f),
                    this.getX() + lookVec.x * 0.3,
                    eyeHeight,
                    this.getZ() + lookVec.z * 0.3,
                    1, 0.05, 0, 0.05, 0
                );
            }
        }
    }
}
