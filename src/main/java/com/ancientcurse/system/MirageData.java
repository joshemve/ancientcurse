package com.ancientcurse.system;

import com.ancientcurse.network.MiragePackets;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

public class MirageData extends PersistentState {
    private boolean active;
    private int duration;
    private float intensity;

    private static final String KEY = "ancient_curse_mirage";

    private static final float MAX_INTENSITY = 1.0f;
    private static final float FADE_IN_SPEED = 0.005f;  // ~10 seconds to full
    private static final float FADE_OUT_SPEED = 0.01f;  // ~5 seconds to zero

    public static MirageData getServerState(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
            MirageData::readNbt,
            MirageData::new,
            KEY
        );
    }

    public static MirageData readNbt(NbtCompound nbt) {
        MirageData data = new MirageData();
        data.active = nbt.getBoolean("active");
        data.duration = nbt.getInt("duration");
        data.intensity = nbt.getFloat("intensity");
        return data;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putBoolean("active", active);
        nbt.putInt("duration", duration);
        nbt.putFloat("intensity", intensity);
        return nbt;
    }

    public void tick(ServerWorld world) {
        boolean dirty = false;

        if (this.active) {
            this.duration--;

            // Fade in intensity
            if (this.intensity < MAX_INTENSITY) {
                this.intensity = Math.min(MAX_INTENSITY, this.intensity + FADE_IN_SPEED);
                dirty = true;
            }

            if (this.duration <= 0) {
                stop(world);
                dirty = false; // stop() marks dirty
            } else {
                dirty = true;
            }
        } else {
            // Fade out intensity
            if (this.intensity > 0) {
                this.intensity = Math.max(0, this.intensity - FADE_OUT_SPEED);
                dirty = true;
            }
        }

        if (dirty) {
            this.markDirty();
        }

        // Sync to clients every second
        if (world.getTime() % 20 == 0) {
            MiragePackets.sendSyncPacket(world, this);
        }
    }

    public void start(ServerWorld world, int duration) {
        this.active = true;
        this.duration = duration;
        this.intensity = Math.max(0.1f, this.intensity);
        this.markDirty();
        MiragePackets.sendSyncPacket(world, this);
    }

    public void stop(ServerWorld world) {
        this.active = false;
        this.duration = 0;
        this.markDirty();
        MiragePackets.sendSyncPacket(world, this);
    }

    public boolean isActive() { return active; }
    public float getIntensity() { return intensity; }

    public boolean hasEffect() {
        return intensity > 0.01f;
    }
}
