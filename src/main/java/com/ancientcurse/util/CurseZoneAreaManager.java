package com.ancientcurse.util;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.*;

/**
 * Manages curse zone areas for the world
 */
public class CurseZoneAreaManager extends PersistentState {
    private static final String DATA_NAME = "ancient_curse_zone_areas";
    private final Map<String, CurseZoneArea> areas = new HashMap<>();
    private int nextId = 1;
    
    public CurseZoneAreaManager() {
        super();
    }
    
    public static CurseZoneAreaManager get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
            CurseZoneAreaManager::fromNbt,
            CurseZoneAreaManager::new,
            DATA_NAME
        );
    }
    
    /**
     * Add a new curse zone area
     */
    public String addArea(BlockPos pos1, BlockPos pos2, String zoneName, int khamsinLevel, int ankhDrainRate, boolean effectsEnabled) {
        String id = "zone_" + nextId++;
        CurseZoneArea area = new CurseZoneArea(id, pos1, pos2);
        area.setZoneName(zoneName);
        area.setKhamsinLevel(khamsinLevel);
        area.setAnkhDrainRate(ankhDrainRate);
        area.setEffectsEnabled(effectsEnabled);
        
        areas.put(id, area);
        markDirty();
        return id;
    }
    
    /**
     * Remove a curse zone area
     */
    public void removeArea(String id) {
        areas.remove(id);
        markDirty();
    }
    
    /**
     * Get all areas
     */
    public Collection<CurseZoneArea> getAllAreas() {
        return areas.values();
    }
    
    /**
     * Get area by ID
     */
    public CurseZoneArea getArea(String id) {
        return areas.get(id);
    }
    
    /**
     * Update an existing area
     */
    public void updateArea(String id, String zoneName, int khamsinLevel, int ankhDrainRate, boolean effectsEnabled) {
        CurseZoneArea area = areas.get(id);
        if (area != null) {
            area.setZoneName(zoneName);
            area.setKhamsinLevel(khamsinLevel);
            area.setAnkhDrainRate(ankhDrainRate);
            area.setEffectsEnabled(effectsEnabled);
            markDirty();
        }
    }
    
    /**
     * Get all areas that contain a position
     */
    public List<CurseZoneArea> getAreasAt(BlockPos pos) {
        List<CurseZoneArea> result = new ArrayList<>();
        for (CurseZoneArea area : areas.values()) {
            if (area.contains(pos)) {
                result.add(area);
            }
        }
        return result;
    }
    
    /**
     * Get all areas that overlap a chunk
     */
    public List<CurseZoneArea> getAreasInChunk(ChunkPos chunkPos) {
        List<CurseZoneArea> result = new ArrayList<>();
        for (CurseZoneArea area : areas.values()) {
            if (area.overlapsChunk(chunkPos)) {
                result.add(area);
            }
        }
        return result;
    }
    
    /**
     * Get the highest curse values at a position
     */
    public CurseValues getCurseValuesAt(BlockPos pos) {
        int maxKhamsin = 0;
        int maxAnkhDrain = 0;
        boolean hasEffects = false;
        
        for (CurseZoneArea area : getAreasAt(pos)) {
            maxKhamsin = Math.max(maxKhamsin, area.getKhamsinLevel());
            maxAnkhDrain = Math.max(maxAnkhDrain, area.getAnkhDrainRate());
            if (area.isEffectsEnabled()) hasEffects = true;
        }
        
        return new CurseValues(maxKhamsin, maxAnkhDrain, hasEffects);
    }
    
    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList areasList = new NbtList();
        for (CurseZoneArea area : areas.values()) {
            areasList.add(area.toNbt());
        }
        nbt.put("Areas", areasList);
        nbt.putInt("NextId", nextId);
        return nbt;
    }
    
    public static CurseZoneAreaManager fromNbt(NbtCompound nbt) {
        CurseZoneAreaManager manager = new CurseZoneAreaManager();
        
        NbtList areasList = nbt.getList("Areas", 10); // 10 = Compound tag
        for (int i = 0; i < areasList.size(); i++) {
            CurseZoneArea area = CurseZoneArea.fromNbt(areasList.getCompound(i));
            manager.areas.put(area.getId(), area);
        }
        
        manager.nextId = nbt.getInt("NextId");
        return manager;
    }
    
    public static class CurseValues {
        public final int khamsinLevel;
        public final int ankhDrainRate;
        public final boolean hasEffects;
        
        public CurseValues(int khamsinLevel, int ankhDrainRate, boolean hasEffects) {
            this.khamsinLevel = khamsinLevel;
            this.ankhDrainRate = ankhDrainRate;
            this.hasEffects = hasEffects;
        }
    }
}