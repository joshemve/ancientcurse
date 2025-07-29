package com.ancientcurse.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.ancientcurse.AncientCurse;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Manager for the Ankh counter system.
 * Handles storing and retrieving ankh values for players.
 */
public class AnkhManager {
    
    private static final String ANKH_KEY = "ancientcurse.ankh_value";
    private static final int DEFAULT_ANKH_VALUE = 100;
    private static final int MIN_ANKH_VALUE = 0;
    private static final int MAX_ANKH_VALUE = 100;
    
    // Cache for ankh values (UUID -> ankh value)
    private static final Map<UUID, Integer> ankhValues = new HashMap<>();
    
    // Client-side ankh value for the local player
    private static int clientAnkhValue = DEFAULT_ANKH_VALUE;
    
    /**
     * Get the current ankh value for a player.
     * 
     * @param player The player
     * @return The ankh value (0-100)
     */
    public static int getAnkhValue(PlayerEntity player) {
        if (player == null) {
            return DEFAULT_ANKH_VALUE;
        }
        
        UUID playerUuid = player.getUuid();
        
        // Check cache first
        if (ankhValues.containsKey(playerUuid)) {
            return ankhValues.get(playerUuid);
        }
        
        // For now, return default value
        // In a real implementation, we would load from persistent storage
        // This is a placeholder until we implement proper data storage
        int ankhValue = DEFAULT_ANKH_VALUE;
        
        // If this is a server player, we could load from server data
        if (player instanceof ServerPlayerEntity serverPlayer) {
            // In the future, implement loading from server data
            // For example:
            // PlayerData data = ((ServerPlayerEntity) player).getServer().getPlayerManager().loadPlayerData(player);
            // if (data != null && data.contains(ANKH_KEY)) {
            //     ankhValue = data.getInt(ANKH_KEY);
            // }
        }
        
        // Cache the value
        ankhValues.put(playerUuid, ankhValue);
        
        return ankhValue;
    }
    
    /**
     * Set the ankh value for a player.
     * 
     * @param player The player
     * @param value The new ankh value (will be clamped to 0-100)
     */
    public static void setAnkhValue(PlayerEntity player, int value) {
        if (player == null) {
            return;
        }
        
        // Clamp value to valid range
        int clampedValue = Math.max(MIN_ANKH_VALUE, Math.min(MAX_ANKH_VALUE, value));
        
        // Update cache
        ankhValues.put(player.getUuid(), clampedValue);
        
        // Sync to client if on server
        if (player instanceof ServerPlayerEntity serverPlayer) {
            // In the future, we'll implement proper syncing and saving here
            // For now, just log the change
            AncientCurse.LOGGER.info("Ankh value for player " + player.getName().getString() + " set to " + clampedValue);
        }
    }
    
    /**
     * Decrease the ankh value for a player.
     * 
     * @param player The player
     * @param amount The amount to decrease by
     * @return The new ankh value
     */
    public static int decreaseAnkhValue(PlayerEntity player, int amount) {
        int currentValue = getAnkhValue(player);
        int newValue = Math.max(MIN_ANKH_VALUE, currentValue - amount);
        setAnkhValue(player, newValue);
        return newValue;
    }
    
    /**
     * Increase the ankh value for a player.
     * 
     * @param player The player
     * @param amount The amount to increase by
     * @return The new ankh value
     */
    public static int increaseAnkhValue(PlayerEntity player, int amount) {
        int currentValue = getAnkhValue(player);
        int newValue = Math.min(MAX_ANKH_VALUE, currentValue + amount);
        setAnkhValue(player, newValue);
        return newValue;
    }
    
    /**
     * Reset the ankh value for a player to the default value.
     * 
     * @param player The player
     */
    public static void resetAnkhValue(PlayerEntity player) {
        setAnkhValue(player, DEFAULT_ANKH_VALUE);
    }
    
    /**
     * Clear the cached ankh value for a player.
     * This should be called when a player disconnects.
     * 
     * @param playerUuid The player's UUID
     */
    public static void clearCachedValue(UUID playerUuid) {
        ankhValues.remove(playerUuid);
    }
    
    /**
     * Set the client-side ankh value for display in the HUD.
     * This is called when the server sends an ankh update packet.
     * 
     * @param value The new ankh value
     */
    public static void setClientAnkhValue(int value) {
        clientAnkhValue = Math.max(MIN_ANKH_VALUE, Math.min(MAX_ANKH_VALUE, value));
    }
    
    /**
     * Get the client-side ankh value for HUD display.
     * 
     * @return The client ankh value
     */
    public static int getClientAnkhValue() {
        return clientAnkhValue;
    }
}
