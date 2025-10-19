package com.ancientcurse.block.entity;

import com.ancientcurse.ModBlockEntities;
import com.ancientcurse.ModItems;
import com.ancientcurse.block.SolarSpireBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class SolarSpireBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    // Animation states
    private static final String CONTROLLER_NAME = "controller";
    private static final RawAnimation SPAWN_ANIM = RawAnimation.begin().thenPlay("animation.solarspire.spawn");
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.solarspire.idle");
    private static final RawAnimation ACTIVATE_ANIM = RawAnimation.begin().thenPlay("animation.solarspire.activate");
    private static final RawAnimation POWER_UP_ANIM = RawAnimation.begin().thenPlay("animation.solarspire.power_up");
    private static final RawAnimation WORKING_ANIM = RawAnimation.begin().thenLoop("animation.solarspire.working_state");
    
    private boolean justPlaced = false;
    private boolean playingSpawnAnimation = false;
    private int spawnAnimationTimer = 0; // Track spawn animation duration
    private boolean isActivated = false;
    private boolean hasEye = false;
    private int activationTimer = 0;
    private int powerUpStage = 0; // 0 = not powering, 1-7 = power up stages
    private int powerUpTimer = 0; // Timer for power-up animation
    
    // Eye rotation for floating effect
    private float eyeRotation = 0;
    private float eyeBobOffset = 0;
    private int eyeAppearTimer = 0; // Timer for eye sliding up animation
    private static final int EYE_APPEAR_DURATION = 20; // 1 second to slide up
    private boolean eyeDisappearing = false; // True when eye is sliding back down
    private int eyeDisappearTimer = 0; // Timer for eye sliding down animation
    private static final int EYE_DISAPPEAR_DURATION = 20; // 1 second to slide down
    
    // Health and boss bar for defense mode
    private static final int MAX_HEALTH = 200;
    private int health = MAX_HEALTH;
    private ServerBossBar bossBar;
    private long lastDamageTime = 0;
    private static final int BOSS_BAR_RANGE = 50; // Players within this range see the boss bar
    
    public SolarSpireBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLAR_SPIRE, pos, state);
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, CONTROLLER_NAME, 0, this::predicate));
    }
    
    private PlayState predicate(AnimationState<SolarSpireBlockEntity> state) {
        AnimationController<SolarSpireBlockEntity> controller = state.getController();
        
        // Play spawn animation when assembled from components
        if (playingSpawnAnimation || spawnAnimationTimer > 0) {
            controller.setAnimation(SPAWN_ANIM);
            controller.setAnimationSpeed(1.0); // Normal speed for spawn animation
            return PlayState.CONTINUE;
        }
        
        // Play activation animation when Eye of Apophis is used
        if (activationTimer > 0) {
            controller.setAnimation(ACTIVATE_ANIM);
            return PlayState.CONTINUE;
        }
        
        // Play working animation when actively cleansing (check this before power-up)
        if (isActivated && powerUpStage == 7) {
            controller.setAnimation(WORKING_ANIM);
            return PlayState.CONTINUE;
        }
        
        // Play power-up animation during power-up sequence (but not at stage 7 if working)
        if (powerUpStage > 0 && powerUpStage < 7) {
            controller.setAnimation(POWER_UP_ANIM);
            return PlayState.CONTINUE;
        }
        
        // Default idle animation
        controller.setAnimation(IDLE_ANIM);
        return PlayState.CONTINUE;
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
    
    /**
     * Triggers the spawn animation when the spire is assembled from components
     */
    public void playSpawnAnimation() {
        this.playingSpawnAnimation = true;
        this.spawnAnimationTimer = 15; // 0.75 seconds (15 ticks) to match animation length
        this.justPlaced = false; // Ensure normal placed flag is false
        markDirty();
        if (world != null) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }
    
    public void activate(ItemStack eyeStack) {
        // Start activation sequence
        this.activationTimer = 60; // 3 seconds for activation animation
        this.hasEye = false; // Don't show the eye immediately - wait for power-up to complete
        this.powerUpStage = 1; // Start power-up sequence
        this.powerUpTimer = 0;
        markDirty();
    }
    
    public void deactivate() {
        // Start the eye disappearing animation
        if (this.hasEye && !this.eyeDisappearing) {
            this.eyeDisappearing = true;
            this.eyeDisappearTimer = 0;
            markDirty();
            // Don't immediately remove the eye - let it animate down first
        } else {
            // If already disappearing or no eye, just clean up
            this.isActivated = false;
            this.hasEye = false;
            this.powerUpStage = 0;  // Only reset when fully deactivating
            this.powerUpTimer = 0;
            this.eyeDisappearing = false;
            this.eyeDisappearTimer = 0;
            removeBossBar();
            markDirty();
        }
    }
    
    @Override
    public void markRemoved() {
        super.markRemoved();
        removeBossBar();
    }
    
    public void setWorking(boolean working) {
        // This triggers the working_state animation
        this.isActivated = working;
        
        // Create boss bar when starting work
        if (working && world != null && !world.isClient) {
            createBossBar();
        }
        
        markDirty();
    }
    
    private void createBossBar() {
        if (bossBar == null) {
            bossBar = new ServerBossBar(
                Text.literal("§6Solar Spire"),
                BossBar.Color.YELLOW,
                BossBar.Style.PROGRESS
            );
            bossBar.setPercent(1.0f);
        }
        updateBossBarPlayers();
    }
    
    private void updateBossBarPlayers() {
        if (bossBar == null || world == null || world.isClient) return;
        
        // Remove all current players
        bossBar.clearPlayers();
        
        // Add nearby players
        Box searchBox = new Box(pos).expand(BOSS_BAR_RANGE);
        List<ServerPlayerEntity> nearbyPlayers = world.getEntitiesByClass(
            ServerPlayerEntity.class, searchBox, player -> true
        );
        
        for (ServerPlayerEntity player : nearbyPlayers) {
            bossBar.addPlayer(player);
        }
    }
    
    private void removeBossBar() {
        if (bossBar != null) {
            bossBar.clearPlayers();
            bossBar = null;
        }
    }
    
    public void damage(int amount) {
        if (world == null || world.isClient) return;
        
        health = Math.max(0, health - amount);
        lastDamageTime = world.getTime();
        
        // Update boss bar
        if (bossBar != null) {
            float healthPercent = (float) health / MAX_HEALTH;
            bossBar.setPercent(healthPercent);
            
            // Change color based on health
            if (healthPercent < 0.25f) {
                bossBar.setColor(BossBar.Color.RED);
            } else if (healthPercent < 0.5f) {
                bossBar.setColor(BossBar.Color.PINK);
            }
        }
        
        // Check if destroyed
        if (health <= 0) {
            onDestroyed();
        }
        
        markDirty();
    }
    
    private void onDestroyed() {
        // Drop the Eye of Apophis
        dropStoredEye();
        
        // Remove boss bar
        removeBossBar();
        
        // Notify the block that the spire was destroyed
        if (world != null && !world.isClient) {
            BlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof SolarSpireBlock) {
                ((SolarSpireBlock) state.getBlock()).onSpireDestroyed(world, pos);
            }
        }
    }
    
    public int getHealth() {
        return health;
    }
    
    public int getMaxHealth() {
        return MAX_HEALTH;
    }
    
    public void dropStoredEye() {
        if (hasEye && world != null && !world.isClient) {
            ItemEntity itemEntity = new ItemEntity(world, 
                pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                new ItemStack(ModItems.EYE_OF_APOPHIS));
            world.spawnEntity(itemEntity);
            hasEye = false;
        }
    }
    
    public static void tick(World world, BlockPos pos, BlockState state, SolarSpireBlockEntity blockEntity) {
        // Handle spawn animation timer countdown
        if (blockEntity.spawnAnimationTimer > 0) {
            blockEntity.spawnAnimationTimer--;
            if (blockEntity.spawnAnimationTimer == 0) {
                blockEntity.playingSpawnAnimation = false; // Animation finished
                blockEntity.markDirty();
            }
        }
        
        // Handle activation timer countdown
        if (blockEntity.activationTimer > 0) {
            blockEntity.activationTimer--;
            if (blockEntity.activationTimer == 0) {
                // Activation animation finished, start power-up if not already done
                if (blockEntity.powerUpStage == 1) {
                    blockEntity.powerUpStage = 2; // Move to next power-up stage
                }
            }
        }
        
        // Handle power-up sequence
        if (blockEntity.powerUpStage > 0 && blockEntity.powerUpStage < 7) {
            blockEntity.powerUpTimer++;
            
            // Safety check: if timer exceeds reasonable bounds, force progression
            // This prevents infinite charging if the state gets corrupted
            if (blockEntity.powerUpTimer > 100) { // 5 seconds max per stage (way more than needed)
                blockEntity.powerUpStage++;
                blockEntity.powerUpTimer = 0;
                blockEntity.markDirty();
            }
            
            // Progress to next stage every 20 ticks (1 second) - doubled from 10 ticks
            if (blockEntity.powerUpTimer >= 20) {
                blockEntity.powerUpStage++;
                blockEntity.powerUpTimer = 0;
                blockEntity.markDirty();
                
                // When we reach stage 7, freeze there and signal ready for cleansing
                if (blockEntity.powerUpStage == 7) {
                    blockEntity.isActivated = true; // Mark as activated
                    blockEntity.hasEye = true; // Show the eye on top
                    blockEntity.eyeAppearTimer = 0; // Start the eye appearance animation
                    blockEntity.markDirty();
                    
                    // Signal to the block that power-up is complete and cleansing can begin
                    if (!world.isClient) {
                        BlockState blockState = world.getBlockState(pos);
                        if (blockState.getBlock() instanceof SolarSpireBlock) {
                            // The block will start the cleansing process on its next tick check
                            world.updateListeners(pos, blockState, blockState, 3);
                        }
                    }
                    // Stay at stage 7 - don't reset or progress further
                }
            }
        }
        
        // Update eye disappearing animation
        if (blockEntity.eyeDisappearing) {
            blockEntity.eyeDisappearTimer++;
            if (blockEntity.eyeDisappearTimer >= EYE_DISAPPEAR_DURATION) {
                // Animation complete, now actually remove the eye and reset everything
                blockEntity.hasEye = false;
                blockEntity.isActivated = false;
                blockEntity.powerUpStage = 0;  // Reset power-up stage when fully deactivated
                blockEntity.powerUpTimer = 0;
                blockEntity.eyeDisappearing = false;
                blockEntity.eyeDisappearTimer = 0;
                blockEntity.removeBossBar();
                blockEntity.markDirty();
            }
        }
        
        // Update eye appearance animation
        if (blockEntity.hasEye && !blockEntity.eyeDisappearing && blockEntity.eyeAppearTimer < EYE_APPEAR_DURATION) {
            blockEntity.eyeAppearTimer++;
        }
        
        // Update eye rotation for rendering
        if (blockEntity.hasEye) {
            blockEntity.eyeRotation += 2.0f; // Rotate 2 degrees per tick
            if (blockEntity.eyeRotation >= 360) {
                blockEntity.eyeRotation = 0;
            }
            
            // Bob up and down (only after fully appeared and not disappearing)
            if (blockEntity.eyeAppearTimer >= EYE_APPEAR_DURATION && !blockEntity.eyeDisappearing) {
                blockEntity.eyeBobOffset = (float)(Math.sin(world.getTime() * 0.1) * 0.1);
            }
        }
        
        // Update boss bar players periodically
        if (blockEntity.bossBar != null && world.getTime() % 20 == 0) {
            blockEntity.updateBossBarPlayers();
        }
    }
    
    // Getters for renderer
    public boolean hasEye() {
        return hasEye;
    }
    
    public float getEyeRotation() {
        return eyeRotation;
    }
    
    public float getEyeBobOffset() {
        return eyeBobOffset;
    }
    
    public float getEyeAppearProgress() {
        if (eyeDisappearing) {
            // When disappearing, reverse the progress (1.0 -> 0.0)
            return Math.max(0.0f, 1.0f - (float)eyeDisappearTimer / EYE_DISAPPEAR_DURATION);
        } else {
            // When appearing, normal progress (0.0 -> 1.0)
            return Math.min(1.0f, (float)eyeAppearTimer / EYE_APPEAR_DURATION);
        }
    }
    
    public boolean isPoweringUp() {
        return powerUpStage > 0 && powerUpStage < 7;  // Changed to < 7 since 7 means complete
    }
    
    public boolean isPowerUpComplete() {
        return powerUpStage == 7 && isActivated && hasEye;
    }
    
    public boolean isWorking() {
        return isActivated && powerUpStage == 7;
    }
    
    public int getPowerUpStage() {
        return powerUpStage;
    }
    
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.justPlaced = nbt.getBoolean("JustPlaced");
        this.playingSpawnAnimation = nbt.getBoolean("PlayingSpawnAnimation");
        this.spawnAnimationTimer = nbt.getInt("SpawnAnimationTimer");
        this.isActivated = nbt.getBoolean("IsActivated");
        this.hasEye = nbt.getBoolean("HasEye");
        this.activationTimer = nbt.getInt("ActivationTimer");
        this.powerUpStage = nbt.getInt("PowerUpStage");
        this.powerUpTimer = nbt.getInt("PowerUpTimer");
        this.health = nbt.contains("Health") ? nbt.getInt("Health") : MAX_HEALTH;
        this.eyeDisappearing = nbt.getBoolean("EyeDisappearing");
        this.eyeDisappearTimer = nbt.getInt("EyeDisappearTimer");
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putBoolean("JustPlaced", justPlaced);
        nbt.putBoolean("PlayingSpawnAnimation", playingSpawnAnimation);
        nbt.putInt("SpawnAnimationTimer", spawnAnimationTimer);
        nbt.putBoolean("IsActivated", isActivated);
        nbt.putBoolean("HasEye", hasEye);
        nbt.putInt("ActivationTimer", activationTimer);
        nbt.putInt("PowerUpStage", powerUpStage);
        nbt.putInt("PowerUpTimer", powerUpTimer);
        nbt.putInt("Health", health);
        nbt.putBoolean("EyeDisappearing", eyeDisappearing);
        nbt.putInt("EyeDisappearTimer", eyeDisappearTimer);
    }
    
    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
    
    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }
}