package com.ancientcurse.block.entity;

import com.ancientcurse.ModBlockEntities;
import com.ancientcurse.ModItems;
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
    private static final RawAnimation WORKING_ANIM = RawAnimation.begin().thenLoop("animation.solarspire.working_state");
    
    private boolean justPlaced = true;
    private boolean isActivated = false;
    private boolean hasEye = false;
    private int activationTimer = 0;
    
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
        
        if (justPlaced) {
            justPlaced = false;
            controller.setAnimation(SPAWN_ANIM);
            return PlayState.CONTINUE;
        }
        
        if (activationTimer > 0) {
            controller.setAnimation(ACTIVATE_ANIM);
            return PlayState.CONTINUE;
        }
        
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
    
    public void activate(ItemStack eyeStack) {
        this.isActivated = true;
        this.hasEye = true;
        this.activationTimer = 60; // 3 seconds for activation animation
        markDirty();
    }
    
    public void deactivate() {
        this.isActivated = false;
        this.hasEye = false;
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
        if (blockEntity.activationTimer > 0) {
            blockEntity.activationTimer--;
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
    
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.justPlaced = nbt.getBoolean("JustPlaced");
        this.isActivated = nbt.getBoolean("IsActivated");
        this.hasEye = nbt.getBoolean("HasEye");
        this.activationTimer = nbt.getInt("ActivationTimer");
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putBoolean("JustPlaced", justPlaced);
        nbt.putBoolean("IsActivated", isActivated);
        nbt.putBoolean("HasEye", hasEye);
        nbt.putInt("ActivationTimer", activationTimer);
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