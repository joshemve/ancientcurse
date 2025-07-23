package com.ancientcurse.client;

import com.ancientcurse.util.CurseZoneArea;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

/**
 * Client-side cache of curse zone areas for rendering
 */
public class CurseZoneClientCache {
    private static final Map<String, ZoneAreaData> cachedAreas = new HashMap<>();
    
    /**
     * Simple data class for zone area information
     */
    public static class ZoneAreaData {
        public final BlockPos min;
        public final BlockPos max;
        public String zoneName;
        public int khamsinLevel;
        public int ankhDrainRate;
        public boolean effectsEnabled;
        
        public ZoneAreaData(BlockPos min, BlockPos max) {
            this.min = min;
            this.max = max;
        }
    }
    
    /**
     * Update or add a zone area to the cache
     */
    public static void updateArea(String id, BlockPos min, BlockPos max, String zoneName, 
                                 int khamsinLevel, int ankhDrainRate, boolean effectsEnabled) {
        ZoneAreaData area = new ZoneAreaData(min, max);
        area.zoneName = zoneName;
        area.khamsinLevel = khamsinLevel;
        area.ankhDrainRate = ankhDrainRate;
        area.effectsEnabled = effectsEnabled;
        
        cachedAreas.put(id, area);
    }
    
    /**
     * Remove a zone area from the cache
     */
    public static void removeArea(String id) {
        cachedAreas.remove(id);
    }
    
    /**
     * Clear all cached areas
     */
    public static void clearCache() {
        cachedAreas.clear();
    }
    
    /**
     * Get all cached areas
     */
    public static Map<String, ZoneAreaData> getAllAreas() {
        return cachedAreas;
    }
    
    /**
     * Get a specific area by ID
     */
    public static ZoneAreaData getArea(String id) {
        return cachedAreas.get(id);
    }
    
    /**
     * Clear all cached areas
     */
    public static void clear() {
        cachedAreas.clear();
    }
}