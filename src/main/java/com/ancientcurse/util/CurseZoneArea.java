package com.ancientcurse.util;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a curse zone area defined by two corner positions
 */
public class CurseZoneArea {
    private final String id;
    private final BlockPos min;
    private final BlockPos max;
    private String zoneName;
    private int khamsinLevel;
    private int ankhDrainRate;
    private boolean effectsEnabled;
    
    public CurseZoneArea(String id, BlockPos pos1, BlockPos pos2) {
        this.id = id;
        this.min = new BlockPos(
            Math.min(pos1.getX(), pos2.getX()),
            Math.min(pos1.getY(), pos2.getY()),
            Math.min(pos1.getZ(), pos2.getZ())
        );
        this.max = new BlockPos(
            Math.max(pos1.getX(), pos2.getX()),
            Math.max(pos1.getY(), pos2.getY()),
            Math.max(pos1.getZ(), pos2.getZ())
        );
        this.zoneName = "";
        this.khamsinLevel = 0;
        this.ankhDrainRate = 0;
        this.effectsEnabled = true;
    }
    
    // Getters
    public String getId() { return id; }
    public BlockPos getMin() { return min; }
    public BlockPos getMax() { return max; }
    public String getZoneName() { return zoneName; }
    public int getKhamsinLevel() { return khamsinLevel; }
    public int getAnkhDrainRate() { return ankhDrainRate; }
    public boolean isEffectsEnabled() { return effectsEnabled; }
    
    // Setters
    public void setZoneName(String zoneName) { this.zoneName = zoneName; }
    public void setKhamsinLevel(int level) { this.khamsinLevel = Math.max(0, Math.min(5, level)); }
    public void setAnkhDrainRate(int rate) { this.ankhDrainRate = Math.max(0, Math.min(100, rate)); }
    public void setEffectsEnabled(boolean enabled) { this.effectsEnabled = enabled; }
    
    /**
     * Check if a position is within this zone
     */
    public boolean contains(BlockPos pos) {
        return pos.getX() >= min.getX() && pos.getX() <= max.getX() &&
               pos.getY() >= min.getY() && pos.getY() <= max.getY() &&
               pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }
    
    /**
     * Check if a chunk overlaps with this zone
     */
    public boolean overlapsChunk(ChunkPos chunkPos) {
        int chunkMinX = chunkPos.getStartX();
        int chunkMaxX = chunkPos.getEndX();
        int chunkMinZ = chunkPos.getStartZ();
        int chunkMaxZ = chunkPos.getEndZ();
        
        return !(chunkMinX > max.getX() || chunkMaxX < min.getX() ||
                chunkMinZ > max.getZ() || chunkMaxZ < min.getZ());
    }
    
    /**
     * Get all chunks that this zone covers
     */
    public List<ChunkPos> getAffectedChunks() {
        List<ChunkPos> chunks = new ArrayList<>();
        int minChunkX = min.getX() >> 4;
        int maxChunkX = max.getX() >> 4;
        int minChunkZ = min.getZ() >> 4;
        int maxChunkZ = max.getZ() >> 4;
        
        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                chunks.add(new ChunkPos(x, z));
            }
        }
        return chunks;
    }
    
    /**
     * Get the bounding box for rendering
     */
    public Box getBoundingBox() {
        return new Box(min.getX(), min.getY(), min.getZ(),
                      max.getX() + 1, max.getY() + 1, max.getZ() + 1);
    }
    
    /**
     * Get severity for color coding (0.0 to 1.0)
     */
    public float getSeverity() {
        // Normalize khamsin (0-5 range) and ankh drain (0-100 range) to 0-1
        float normalizedKhamsin = khamsinLevel / 5.0f;
        float normalizedAnkh = ankhDrainRate / 100.0f;
        return Math.max(normalizedKhamsin, normalizedAnkh);
    }
    
    // Serialization
    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("Id", id);
        nbt.putLong("MinPos", min.asLong());
        nbt.putLong("MaxPos", max.asLong());
        nbt.putString("ZoneName", zoneName);
        nbt.putInt("KhamsinLevel", khamsinLevel);
        nbt.putInt("AnkhDrainRate", ankhDrainRate);
        nbt.putBoolean("EffectsEnabled", effectsEnabled);
        return nbt;
    }
    
    public static CurseZoneArea fromNbt(NbtCompound nbt) {
        String id = nbt.getString("Id");
        BlockPos min = BlockPos.fromLong(nbt.getLong("MinPos"));
        BlockPos max = BlockPos.fromLong(nbt.getLong("MaxPos"));
        
        CurseZoneArea area = new CurseZoneArea(id, min, max);
        area.zoneName = nbt.getString("ZoneName");
        area.khamsinLevel = nbt.getInt("KhamsinLevel");
        area.ankhDrainRate = nbt.getInt("AnkhDrainRate");
        area.effectsEnabled = nbt.getBoolean("EffectsEnabled");
        return area;
    }
}