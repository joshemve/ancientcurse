package com.ancientcurse.util;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.ChunkPos;

public class CurseZoneData {
    private final ChunkPos chunkPos;
    private String zoneName;
    private int khamsinLevel;
    private int ankhDrainRate;
    private boolean effectsEnabled;
    
    public CurseZoneData(ChunkPos chunkPos) {
        this.chunkPos = chunkPos;
        this.zoneName = "";
        this.khamsinLevel = 0;
        this.ankhDrainRate = 0;
        this.effectsEnabled = true;
    }
    
    public CurseZoneData(ChunkPos chunkPos, String zoneName, int khamsinLevel, int ankhDrainRate) {
        this.chunkPos = chunkPos;
        this.zoneName = zoneName;
        this.khamsinLevel = khamsinLevel;
        this.ankhDrainRate = ankhDrainRate;
        this.effectsEnabled = true;
    }
    
    // Getters
    public ChunkPos getChunkPos() { return chunkPos; }
    public String getZoneName() { return zoneName; }
    public int getKhamsinLevel() { return khamsinLevel; }
    public int getAnkhDrainRate() { return ankhDrainRate; }
    public boolean isEffectsEnabled() { return effectsEnabled; }
    
    // Setters
    public void setZoneName(String zoneName) { this.zoneName = zoneName; }
    public void setKhamsinLevel(int level) { this.khamsinLevel = Math.max(0, Math.min(10, level)); }
    public void setAnkhDrainRate(int rate) { this.ankhDrainRate = Math.max(0, Math.min(10, rate)); }
    public void setEffectsEnabled(boolean enabled) { this.effectsEnabled = enabled; }
    
    // Get severity for color coding (0.0 to 1.0)
    public float getSeverity() {
        return Math.max(khamsinLevel, ankhDrainRate) / 10.0f;
    }
    
    // Serialization
    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putLong("ChunkPos", chunkPos.toLong());
        nbt.putString("ZoneName", zoneName);
        nbt.putInt("KhamsinLevel", khamsinLevel);
        nbt.putInt("AnkhDrainRate", ankhDrainRate);
        nbt.putBoolean("EffectsEnabled", effectsEnabled);
        return nbt;
    }
    
    public static CurseZoneData fromNbt(NbtCompound nbt) {
        ChunkPos pos = new ChunkPos(nbt.getLong("ChunkPos"));
        CurseZoneData data = new CurseZoneData(pos);
        data.zoneName = nbt.getString("ZoneName");
        data.khamsinLevel = nbt.getInt("KhamsinLevel");
        data.ankhDrainRate = nbt.getInt("AnkhDrainRate");
        data.effectsEnabled = nbt.getBoolean("EffectsEnabled");
        return data;
    }
}