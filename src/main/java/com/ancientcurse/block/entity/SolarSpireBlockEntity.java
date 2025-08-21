package com.ancientcurse.block.entity;

import com.ancientcurse.ModBlockEntities;
import com.ancientcurse.ModItems;
import com.ancientcurse.block.SolarSpireBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
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
        
        // Play power-up animation during power-up sequence
        if (powerUpStage > 0 && powerUpStage <= 7) {
            controller.setAnimation(POWER_UP_ANIM);
            return PlayState.CONTINUE;
        }
        
        // Play working animation when actively cleansing
        if (isActivated) {
            controller.setAnimation(WORKING_ANIM);
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
        this.isActivated = false;
        this.hasEye = false;
        this.powerUpStage = 0;
        this.powerUpTimer = 0;
        markDirty();
    }
    
    public void setWorking(boolean working) {
        // This triggers the working_state animation
        this.isActivated = working;
        markDirty();
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
            // Progress to next stage every 20 ticks (1 second) - doubled from 10 ticks
            if (blockEntity.powerUpTimer >= 20) {
                blockEntity.powerUpStage++;
                blockEntity.powerUpTimer = 0;
                blockEntity.markDirty();
                
                // When we reach stage 7, freeze there and signal ready for cleansing
                if (blockEntity.powerUpStage == 7) {
                    blockEntity.isActivated = true; // Mark as activated
                    blockEntity.hasEye = true; // Show the eye on top
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
        
        // Update eye rotation for rendering
        if (blockEntity.hasEye) {
            blockEntity.eyeRotation += 2.0f; // Rotate 2 degrees per tick
            if (blockEntity.eyeRotation >= 360) {
                blockEntity.eyeRotation = 0;
            }
            
            // Bob up and down
            blockEntity.eyeBobOffset = (float)(Math.sin(world.getTime() * 0.1) * 0.1);
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
    
    public boolean isPoweringUp() {
        return powerUpStage > 0 && powerUpStage < 7;  // Changed to < 7 since 7 means complete
    }
    
    public boolean isPowerUpComplete() {
        return powerUpStage == 7 && isActivated && hasEye;
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