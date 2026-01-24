package com.ancientcurse.block.entity;

import com.ancientcurse.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

public class SekhemCactusBlockEntity extends BlockEntity implements GeoBlockEntity {
    // Cache the idle animation to avoid creating new RawAnimation objects every frame
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("animation.sekhem_cactus.idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean dateGrown1 = false;
    private boolean dateGrown2 = false;

    public SekhemCactusBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SEKHEM_CACTUS_BLOCK_ENTITY, pos, state);
    }

    public void setDateGrown(int slot, boolean grown) {
        if (slot == 1)
            this.dateGrown1 = grown;
        if (slot == 2)
            this.dateGrown2 = grown;
        markDirty();
        if (world != null && !world.isClient) {
            ((ServerWorld) world).getChunkManager().markForUpdate(pos);
        }
    }

    public boolean isDateGrown(int slot) {
        if (slot == 1)
            return dateGrown1;
        if (slot == 2)
            return dateGrown2;
        return false;
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        nbt.putBoolean("DateGrown1", dateGrown1);
        nbt.putBoolean("DateGrown2", dateGrown2);
        super.writeNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        dateGrown1 = nbt.getBoolean("DateGrown1");
        dateGrown2 = nbt.getBoolean("DateGrown2");
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

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> state.setAndContinue(IDLE_ANIMATION)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
