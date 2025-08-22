package com.ancientcurse.entity;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.ai.AttackSolarSpireGoal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import java.util.Collections;
import com.ancientcurse.entity.KhamsinOrbEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

/**
 * Khamsin Spread Small - A floating mystical rock that's part of the curse system
 * Remains dormant until a player approaches, then shoots slow-moving destructible orbs
 */
public class KhamsinSpreadSmallEntity extends MobEntity implements GeoEntity {
    
    /* ---------- DATA TRACKERS ---------- */
    private static final TrackedData<Boolean> IS_ACTIVATED = 
            DataTracker.registerData(KhamsinSpreadSmallEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IS_GLOWING = 
            DataTracker.registerData(KhamsinSpreadSmallEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> GLOW_TIMER = 
            DataTracker.registerData(KhamsinSpreadSmallEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SHOOT_COOLDOWN = 
            DataTracker.registerData(KhamsinSpreadSmallEntity.class, TrackedDataHandlerRegistry.INTEGER);
    
    /* ---------- CONSTANTS ---------- */
    private static final double ACTIVATION_RANGE = 8.0; // Blocks to activate when player approaches
    private static final int GLOW_CYCLE_DURATION = 80; // 4 seconds per glow cycle for smoother transition
    private static final int SHOOT_INTERVAL = 60; // 3 seconds between shots when activated
    private static final int DEACTIVATION_DELAY = 200; // 10 seconds after last player contact
    private static final float ACTIVATION_SPEED = 0.05f; // Speed of texture transition
    
    /* ---------- FIELDS ---------- */
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int deactivationTimer = 0;
    private Vec3d spawnPosition;
    private boolean isFalling = false;
    private int groundCheckCooldown = 0;
    
    // Smooth transition fields
    private float activationProgress = 0.0f;
    private float previousActivationProgress = 0.0f;
    
    public KhamsinSpreadSmallEntity(EntityType<? extends MobEntity> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(true); // Always float
        this.setInvulnerable(false); // Can be destroyed by players
    }
    
    /* ---------- ATTRIBUTES ---------- */
    public static DefaultAttributeContainer.Builder createKhamsinSpreadSmallAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0) // Moderate health
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0) // No movement - stays in place
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0) // Immune to knockback
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.0); // No flying movement
    }
    
    /* ---------- REQUIRED ABSTRACT METHODS ---------- */
    @Override
    public Iterable<ItemStack> getArmorItems() {
        return Collections.emptyList();
    }
    
    @Override
    public ItemStack getEquippedStack(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }
    
    @Override
    public void equipStack(EquipmentSlot slot, ItemStack stack) {
        // This entity doesn't use equipment
    }
    
    @Override
    public Arm getMainArm() {
        return Arm.RIGHT;
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Store spawn position for staying in place
        if (spawnPosition == null) {
            spawnPosition = this.getPos();
        }
        
        // Check for ground support and handle falling
        handleGroundCheck();
        
        // Force entity to stay in spawn position (only if not falling)
        if (!isFalling && spawnPosition != null && this.getPos().distanceTo(spawnPosition) > 0.1) {
            this.setPosition(spawnPosition);
            this.setVelocity(Vec3d.ZERO);
        }
        
        // Handle activation based on nearby players
        handlePlayerDetection();
        
        // Update activation progress for smooth transitions
        updateActivationProgress();
        
        // Handle glow effect when activated
        if (dataTracker.get(IS_ACTIVATED)) {
            handleGlowEffect();
            handleShooting();
        }
        
        // Handle deactivation timer
        handleDeactivation();
        
        // Manage floating state based on falling
        if (!isFalling) {
            this.setNoGravity(true);
            this.setOnGround(false);
        }
    }
    
    private void updateActivationProgress() {
        // Store previous progress for interpolation
        previousActivationProgress = activationProgress;
        
        if (isActivated()) {
            // Transition to light (1.0)
            if (activationProgress < 1.0f) {
                activationProgress = Math.min(1.0f, activationProgress + ACTIVATION_SPEED);
            }
        } else {
            // Transition to dark (0.0)
            if (activationProgress > 0.0f) {
                activationProgress = Math.max(0.0f, activationProgress - ACTIVATION_SPEED);
            }
        }
    }
    
    public float getActivationProgress(float partialTick) {
        // Interpolate between previous and current progress for smooth rendering
        return MathHelper.lerp(partialTick, previousActivationProgress, activationProgress);
    }
    
    private void handlePlayerDetection() {
        if (this.getWorld().isClient) return;
        
        // Check for nearby players (excluding creative mode)
        Box detectionBox = new Box(this.getBlockPos()).expand(ACTIVATION_RANGE);
        List<PlayerEntity> nearbyPlayers = this.getWorld().getNonSpectatingEntities(PlayerEntity.class, detectionBox);
        
        // Filter out creative mode players
        nearbyPlayers.removeIf(player -> player.isCreative() || player.isSpectator());
        
        boolean shouldBeActivated = !nearbyPlayers.isEmpty();
        boolean currentlyActivated = dataTracker.get(IS_ACTIVATED);
        
        if (shouldBeActivated && !currentlyActivated) {
            // Activate the entity
            dataTracker.set(IS_ACTIVATED, true);
            dataTracker.set(GLOW_TIMER, 0);
            dataTracker.set(SHOOT_COOLDOWN, SHOOT_INTERVAL);
            deactivationTimer = 0;
            
            // Play activation sound
            this.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE, 0.8f, 1.2f);
            
            // Create activation particles
            if (this.getWorld() instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(
                    ParticleTypes.PORTAL,
                    this.getX(), this.getY() + 0.5, this.getZ(),
                    15, 0.3, 0.3, 0.3, 0.1
                );
            }
        }
        
        if (shouldBeActivated) {
            deactivationTimer = 0; // Reset deactivation timer
        } else if (currentlyActivated) {
            deactivationTimer++;
        }
    }
    
    private void handleGlowEffect() {
        int glowTimer = dataTracker.get(GLOW_TIMER);
        glowTimer++;
        
        // Reset cycle when complete
        if (glowTimer >= GLOW_CYCLE_DURATION) {
            glowTimer = 0;
        }
        
        // Glow for 30% of the cycle (24 ticks out of 80), creating a smooth pulse
        boolean shouldGlow = glowTimer < (GLOW_CYCLE_DURATION * 0.3);
        dataTracker.set(IS_GLOWING, shouldGlow);
        dataTracker.set(GLOW_TIMER, glowTimer);
        
        // Ambient particles when pulsing
        if (this.isPulsing() && this.random.nextInt(4) == 0) {
            if (this.getWorld() instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(
                    ParticleTypes.END_ROD,
                    this.getX() + (this.random.nextGaussian() * 0.3),
                    this.getY() + 0.5 + (this.random.nextGaussian() * 0.2),
                    this.getZ() + (this.random.nextGaussian() * 0.3),
                    1, 0, 0.1, 0, 0.02
                );
            }
        }
    }
    
    private void handleShooting() {
        if (this.getWorld().isClient) return;
        
        int shootCooldown = dataTracker.get(SHOOT_COOLDOWN);
        if (shootCooldown > 0) {
            dataTracker.set(SHOOT_COOLDOWN, shootCooldown - 1);
            return;
        }
        
        // First check for active Solar Spire within range
        BlockPos spirePos = com.ancientcurse.block.SolarSpireBlock.getNearestActiveSpire(
            this.getWorld(), this.getBlockPos(), 50
        );
        
        // Attack spire if it's within 50 blocks (not just activation range)
        if (spirePos != null) {
            // Create a temporary entity at spire position to shoot at
            Vec3d spireCenter = Vec3d.ofCenter(spirePos);
            
            // Shoot orb at the spire
            shootOrbAtPosition(spireCenter);
            
            // Deal direct damage to the spire (6 damage per orb)
            com.ancientcurse.block.SolarSpireBlock.damageSpire(this.getWorld(), spirePos, 6);
            
            dataTracker.set(SHOOT_COOLDOWN, SHOOT_INTERVAL);
            return;
        }
        
        // If no spire, find target player to shoot at (excluding creative mode)
        Box targetBox = new Box(this.getBlockPos()).expand(ACTIVATION_RANGE);
        List<PlayerEntity> players = this.getWorld().getNonSpectatingEntities(PlayerEntity.class, targetBox);
        
        // Filter out creative mode and spectator players
        players.removeIf(player -> player.isCreative() || player.isSpectator());
        
        if (!players.isEmpty()) {
            PlayerEntity target = players.get(this.random.nextInt(players.size()));
            shootOrbAtTarget(target);
            dataTracker.set(SHOOT_COOLDOWN, SHOOT_INTERVAL);
        }
    }
    
    private void shootOrbAtTarget(PlayerEntity target) {
        // Create mystical orb projectile
        KhamsinOrbEntity orb = new KhamsinOrbEntity(this.getWorld(), this, target);
        
        // Position orb slightly in front of the entity
        Vec3d direction = target.getPos().subtract(this.getPos()).normalize();
        Vec3d orbStart = this.getPos().add(direction.multiply(1.0)).add(0, 0.5, 0);
        orb.setPosition(orbStart);
        
        this.getWorld().spawnEntity(orb);
        
        // Play shooting sound
        this.playSound(SoundEvents.ENTITY_SHULKER_SHOOT, 0.6f, 1.5f);
        
        // Create shooting particles
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                ParticleTypes.WITCH,
                orbStart.x, orbStart.y, orbStart.z,
                8, 0.2, 0.2, 0.2, 0.05
            );
        }
    }
    
    private void shootOrbAtPosition(Vec3d targetPos) {
        // Create mystical orb projectile aimed at a position (for Solar Spire)
        KhamsinOrbEntity orb = new KhamsinOrbEntity(this.getWorld(), this, null);
        
        // Position orb slightly in front of the entity
        Vec3d direction = targetPos.subtract(this.getPos()).normalize();
        Vec3d orbStart = this.getPos().add(direction.multiply(1.0)).add(0, 0.5, 0);
        orb.setPosition(orbStart);
        
        // Set velocity towards target
        Vec3d velocity = targetPos.subtract(orbStart).normalize().multiply(0.4);
        orb.setVelocity(velocity);
        
        this.getWorld().spawnEntity(orb);
        
        // Play shooting sound
        this.playSound(SoundEvents.ENTITY_SHULKER_SHOOT, 0.6f, 1.5f);
        
        // Create shooting particles
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                ParticleTypes.WITCH,
                orbStart.x, orbStart.y, orbStart.z,
                8, 0.2, 0.2, 0.2, 0.05
            );
        }
    }
    
    private void handleDeactivation() {
        if (deactivationTimer >= DEACTIVATION_DELAY && dataTracker.get(IS_ACTIVATED)) {
            // Deactivate the entity
            dataTracker.set(IS_ACTIVATED, false);
            dataTracker.set(IS_GLOWING, false);
            dataTracker.set(GLOW_TIMER, 0);
            dataTracker.set(SHOOT_COOLDOWN, 0);
            deactivationTimer = 0;
            
            // Play deactivation sound
            this.playSound(SoundEvents.BLOCK_BEACON_DEACTIVATE, 0.6f, 0.8f);
        }
    }
    
    private void handleGroundCheck() {
        // Only check for ground support every few ticks to avoid performance issues
        if (groundCheckCooldown > 0) {
            groundCheckCooldown--;
            return;
        }
        groundCheckCooldown = 10; // Check every 10 ticks (0.5 seconds)
        
        // Check if there's a solid block underneath
        Vec3d currentPos = this.getPos();
        BlockPos blockBelow = new BlockPos((int)currentPos.x, (int)(currentPos.y - 0.1), (int)currentPos.z);
        BlockState stateBelow = this.getWorld().getBlockState(blockBelow);
        
        boolean hasGroundSupport = !stateBelow.isAir() && stateBelow.isSolidBlock(this.getWorld(), blockBelow);
        
        if (!hasGroundSupport && !isFalling) {
            // Start falling
            startFalling();
        } else if (hasGroundSupport && isFalling && this.isOnGround()) {
            // Landed on solid ground
            landOnGround();
        }
    }
    
    private void startFalling() {
        isFalling = true;
        this.setNoGravity(false);
        
        // Play falling sound
        this.playSound(SoundEvents.BLOCK_STONE_BREAK, 0.5f, 0.8f);
        
        // Create falling particles
        if (!this.getWorld().isClient && this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                ParticleTypes.SMOKE,
                this.getX(), this.getY(), this.getZ(),
                8, 0.3, 0.1, 0.3, 0.1
            );
        }
    }
    
    private void landOnGround() {
        isFalling = false;
        spawnPosition = this.getPos(); // Update spawn position to new location
        this.setNoGravity(true);
        this.setVelocity(Vec3d.ZERO);
        
        // Play landing sound
        this.playSound(SoundEvents.BLOCK_STONE_PLACE, 0.6f, 1.0f);
        
        // Create landing particles
        if (!this.getWorld().isClient && this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                ParticleTypes.LARGE_SMOKE,
                this.getX(), this.getY(), this.getZ(),
                12, 0.4, 0.1, 0.4, 0.05
            );
        }
    }
    
    /* ---------- MOVEMENT OVERRIDES ---------- */
    @Override
    public void move(net.minecraft.entity.MovementType movementType, Vec3d movement) {
        // Allow falling movement, but restrict horizontal movement
        if (isFalling) {
            // Allow natural falling but prevent horizontal drift
            super.move(movementType, new Vec3d(0, movement.y, 0));
        } else if (spawnPosition != null) {
            // Keep entity in place when not falling
            this.setPosition(spawnPosition);
            this.setVelocity(Vec3d.ZERO);
        }
    }
    
    @Override
    public boolean isPushable() {
        return false; // Cannot be pushed
    }
    
    @Override
    protected void pushAway(Entity entity) {
        // Do nothing - cannot be pushed
    }
    
    /* ---------- GECKOLIB ANIMATIONS ---------- */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }
    
    private <T extends GeoEntity> PlayState predicate(AnimationState<T> state) {
        if (dataTracker.get(IS_ACTIVATED)) {
            // Activated animation - floating with energy
            state.getController().setAnimation(RawAnimation.begin().then("animation.khamsin_spread_small.activated", Animation.LoopType.LOOP));
        } else {
            // Dormant animation - gentle floating
            state.getController().setAnimation(RawAnimation.begin().then("animation.khamsin_spread_small.unactivated", Animation.LoopType.LOOP));
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
        this.dataTracker.startTracking(IS_ACTIVATED, false);
        this.dataTracker.startTracking(IS_GLOWING, false);
        this.dataTracker.startTracking(GLOW_TIMER, 0);
        this.dataTracker.startTracking(SHOOT_COOLDOWN, 0);
    }
    
    /* ---------- NBT ---------- */
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("IsActivated", this.dataTracker.get(IS_ACTIVATED));
        nbt.putBoolean("IsGlowing", this.dataTracker.get(IS_GLOWING));
        nbt.putInt("GlowTimer", this.dataTracker.get(GLOW_TIMER));
        nbt.putInt("ShootCooldown", this.dataTracker.get(SHOOT_COOLDOWN));
        nbt.putInt("DeactivationTimer", this.deactivationTimer);
        nbt.putBoolean("IsFalling", this.isFalling);
        nbt.putInt("GroundCheckCooldown", this.groundCheckCooldown);
        nbt.putFloat("ActivationProgress", this.activationProgress);
        nbt.putFloat("PreviousActivationProgress", this.previousActivationProgress);
        
        if (spawnPosition != null) {
            nbt.putDouble("SpawnX", spawnPosition.x);
            nbt.putDouble("SpawnY", spawnPosition.y);
            nbt.putDouble("SpawnZ", spawnPosition.z);
        }
    }
    
    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.dataTracker.set(IS_ACTIVATED, nbt.getBoolean("IsActivated"));
        this.dataTracker.set(IS_GLOWING, nbt.getBoolean("IsGlowing"));
        this.dataTracker.set(GLOW_TIMER, nbt.getInt("GlowTimer"));
        this.dataTracker.set(SHOOT_COOLDOWN, nbt.getInt("ShootCooldown"));
        this.deactivationTimer = nbt.getInt("DeactivationTimer");
        this.isFalling = nbt.getBoolean("IsFalling");
        this.groundCheckCooldown = nbt.getInt("GroundCheckCooldown");
        this.activationProgress = nbt.getFloat("ActivationProgress");
        this.previousActivationProgress = nbt.getFloat("PreviousActivationProgress");
        
        if (nbt.contains("SpawnX")) {
            this.spawnPosition = new Vec3d(
                nbt.getDouble("SpawnX"),
                nbt.getDouble("SpawnY"),
                nbt.getDouble("SpawnZ")
            );
        }
    }
    
    /* ---------- GETTERS ---------- */
    public boolean isActivated() { return dataTracker.get(IS_ACTIVATED); }
    public boolean isPulsing() { return dataTracker.get(IS_GLOWING); } // Renamed to avoid Minecraft's glow outline
    
    @Override
    public boolean isGlowing() {
        return false; // Always return false to disable Minecraft's outline glow effect
    }
    
    /**
     * Returns a smooth pulse intensity value from 0.0 to 1.0
     * Useful for smooth visual transitions
     */
    public float getPulseIntensity() {
        if (!isActivated()) return 0.0f;
        
        int glowTimer = dataTracker.get(GLOW_TIMER);
        float progress = (float) glowTimer / GLOW_CYCLE_DURATION;
        
        // Create a smooth sine wave pulse that peaks at 30% through the cycle
        float intensity = (float) Math.sin(progress * Math.PI * 2) * 0.5f + 0.5f;
        return Math.max(0.0f, Math.min(1.0f, intensity));
    }
    
    /* ---------- SOUNDS ---------- */
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.BLOCK_STONE_HIT;
    }
    
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BLOCK_STONE_BREAK;
    }
    
    @Override
    public boolean damage(DamageSource source, float amount) {
        // Create damage particles
        if (!this.getWorld().isClient) {
            ((ServerWorld)this.getWorld()).spawnParticles(
                ParticleTypes.CRIT,
                this.getX(), this.getY() + 0.5, this.getZ(),
                5, 0.2, 0.2, 0.2, 0.1
            );
            
            // Additional purple particles for visual feedback
            ((ServerWorld)this.getWorld()).spawnParticles(
                ParticleTypes.PORTAL,
                this.getX(), this.getY() + 0.5, this.getZ(),
                10, 0.3, 0.3, 0.3, 0.05
            );
        }
        
        // Play hurt sound
        this.playSound(this.getHurtSound(source), 1.0f, 1.0f);
        
        // Call super to handle damage normally (this will set hurtTime for red flash)
        return super.damage(source, amount);
    }
    
    public boolean cannotDespawn() {
        return true; // Curse entities should persist
    }
}