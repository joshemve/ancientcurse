package com.ancientcurse.util;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class CurseZoneManager extends PersistentState {
    private static final String DATA_NAME = "ancient_curse_zones";
    private final Map<ChunkPos, CurseZoneData> zones = new HashMap<>();
    
    public CurseZoneManager() {
        super();
    }
    
    public static CurseZoneManager get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
            CurseZoneManager::fromNbt,
            CurseZoneManager::new,
            DATA_NAME
        );
    }
    
    // Zone management
    public void setZone(ChunkPos chunk, CurseZoneData data) {
        zones.put(chunk, data);
        markDirty();
    }
    
    public void removeZone(ChunkPos chunk) {
        zones.remove(chunk);
        markDirty();
    }
    
    public CurseZoneData getZone(ChunkPos chunk) {
        return zones.get(chunk);
    }
    
    public boolean hasZone(ChunkPos chunk) {
        return zones.containsKey(chunk);
    }
    
    // Get interpolated values for smooth gradients
    public float getInterpolatedKhamsinLevel(BlockPos pos) {
        ChunkPos centerChunk = new ChunkPos(pos);
        float totalWeight = 0;
        float weightedSum = 0;
        
        // Check 3x3 chunk area for smooth interpolation
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ChunkPos checkChunk = new ChunkPos(centerChunk.x + dx, centerChunk.z + dz);
                CurseZoneData zone = zones.get(checkChunk);
                
                if (zone != null && zone.isEffectsEnabled()) {
                    // Calculate distance-based weight
                    double distance = Math.sqrt(dx * dx + dz * dz);
                    float weight = (float)(1.0 / (1.0 + distance));
                    
                    totalWeight += weight;
                    weightedSum += zone.getKhamsinLevel() * weight;
                }
            }
        }
        
        return totalWeight > 0 ? weightedSum / totalWeight : 0;
    }
    
    public float getInterpolatedAnkhDrain(BlockPos pos) {
        ChunkPos centerChunk = new ChunkPos(pos);
        float totalWeight = 0;
        float weightedSum = 0;
        
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ChunkPos checkChunk = new ChunkPos(centerChunk.x + dx, centerChunk.z + dz);
                CurseZoneData zone = zones.get(checkChunk);
                
                if (zone != null && zone.isEffectsEnabled()) {
                    double distance = Math.sqrt(dx * dx + dz * dz);
                    float weight = (float)(1.0 / (1.0 + distance));
                    
                    totalWeight += weight;
                    weightedSum += zone.getAnkhDrainRate() * weight;
                }
            }
        }
        
        return totalWeight > 0 ? weightedSum / totalWeight : 0;
    }
    
    // Get all zones for rendering
    public Map<ChunkPos, CurseZoneData> getAllZones() {
        return new HashMap<>(zones);
    }
    
    // Static helper methods
    public static void setZone(World world, ChunkPos chunk, int khamsinLevel, int ankhDrain, String name) {
        if (world instanceof ServerWorld serverWorld) {
            CurseZoneManager manager = get(serverWorld);
            CurseZoneData data = new CurseZoneData(chunk, name, khamsinLevel, ankhDrain);
            manager.setZone(chunk, data);
        }
    }
    
    // Serialization
    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList zonesList = new NbtList();
        for (CurseZoneData zone : zones.values()) {
            zonesList.add(zone.toNbt());
        }
        nbt.put("Zones", zonesList);
        return nbt;
    }
    
    public static CurseZoneManager fromNbt(NbtCompound nbt) {
        CurseZoneManager manager = new CurseZoneManager();
        NbtList zonesList = nbt.getList("Zones", 10); // 10 = Compound tag
        
        for (int i = 0; i < zonesList.size(); i++) {
            CurseZoneData zone = CurseZoneData.fromNbt(zonesList.getCompound(i));
            manager.zones.put(zone.getChunkPos(), zone);
        }
        
        return manager;
    }
}