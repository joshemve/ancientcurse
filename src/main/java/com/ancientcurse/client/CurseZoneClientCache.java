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
    private static final Map<String, CurseZoneArea> cachedAreas = new HashMap<>();
    
    /**
     * Update or add a zone area to the cache
     */
    public static void updateArea(String id, BlockPos min, BlockPos max, String zoneName, 
                                 int khamsinLevel, int ankhDrainRate, boolean effectsEnabled) {
        CurseZoneArea area = new CurseZoneArea(id, min, max);
        area.setZoneName(zoneName);
        area.setKhamsinLevel(khamsinLevel);
        area.setAnkhDrainRate(ankhDrainRate);
        area.setEffectsEnabled(effectsEnabled);
        
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
    public static Collection<CurseZoneArea> getAllAreas() {
        return cachedAreas.values();
    }
    
    /**
     * Get a specific area by ID
     */
    public static CurseZoneArea getArea(String id) {
        return cachedAreas.get(id);
    }
    
    /**
     * Clear all cached areas
     */
    public static void clear() {
        cachedAreas.clear();
    }
}