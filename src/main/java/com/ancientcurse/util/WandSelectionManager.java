package com.ancientcurse.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages visual selection for the admin wand on the client side
 */
public class WandSelectionManager {
    private static final Map<UUID, SelectionData> playerSelections = new HashMap<>();
    
    public static class SelectionData {
        public BlockPos pos1;
        public BlockPos pos2;
        public long lastUpdate;
        
        public SelectionData() {
            this.lastUpdate = System.currentTimeMillis();
        }
    }
    
    public static void setFirstPosition(PlayerEntity player, BlockPos pos) {
        SelectionData data = playerSelections.computeIfAbsent(player.getUuid(), k -> new SelectionData());
        data.pos1 = pos;
        data.pos2 = null; // Clear second position when setting first
        data.lastUpdate = System.currentTimeMillis();
    }
    
    public static void setSecondPosition(PlayerEntity player, BlockPos pos) {
        SelectionData data = playerSelections.computeIfAbsent(player.getUuid(), k -> new SelectionData());
        data.pos2 = pos;
        data.lastUpdate = System.currentTimeMillis();
    }
    
    public static void clearSelection(PlayerEntity player) {
        playerSelections.remove(player.getUuid());
    }
    
    public static SelectionData getSelection(PlayerEntity player) {
        return playerSelections.get(player.getUuid());
    }
    
    /**
     * Clean up old selections (called periodically)
     */
    public static void cleanupOldSelections() {
        long now = System.currentTimeMillis();
        playerSelections.entrySet().removeIf(entry -> 
            now - entry.getValue().lastUpdate > 300000 // Remove after 5 minutes
        );
    }
    
    /**
     * Get the minimum corner of the selection box
     */
    public static BlockPos getMinPos(SelectionData data) {
        if (data.pos1 == null || data.pos2 == null) return data.pos1;
        return new BlockPos(
            Math.min(data.pos1.getX(), data.pos2.getX()),
            Math.min(data.pos1.getY(), data.pos2.getY()),
            Math.min(data.pos1.getZ(), data.pos2.getZ())
        );
    }
    
    /**
     * Get the maximum corner of the selection box
     */
    public static BlockPos getMaxPos(SelectionData data) {
        if (data.pos1 == null || data.pos2 == null) return data.pos1;
        return new BlockPos(
            Math.max(data.pos1.getX(), data.pos2.getX()),
            Math.max(data.pos1.getY(), data.pos2.getY()),
            Math.max(data.pos1.getZ(), data.pos2.getZ())
        );
    }
}