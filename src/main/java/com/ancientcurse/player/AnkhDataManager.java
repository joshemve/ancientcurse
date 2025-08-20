package com.ancientcurse.player;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.network.CurseZonePackets;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Manages ankh values for players using proper persistent storage.
 * Replaces the old AnkhManager with proper NBT data persistence.
 */
public class AnkhDataManager {
    private static final String ANKH_KEY = "AnkhValue";
    private static final int DEFAULT_ANKH_VALUE = 100;
    private static final int MIN_ANKH_VALUE = 0;
    private static final int MAX_ANKH_VALUE = 100;
    
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
        
        // Get persistent data from player
        PlayerDataStorage storage = (PlayerDataStorage) player;
        NbtCompound data = storage.ancientcurse$getPersistentData();
        
        // Return stored value or default
        if (data.contains(ANKH_KEY)) {
            return data.getInt(ANKH_KEY);
        }
        
        return DEFAULT_ANKH_VALUE;
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
        
        // Store in persistent data
        PlayerDataStorage storage = (PlayerDataStorage) player;
        NbtCompound data = storage.ancientcurse$getPersistentData();
        data.putInt(ANKH_KEY, clampedValue);
        
        // Sync to client if on server
        if (player instanceof ServerPlayerEntity serverPlayer) {
            CurseZonePackets.sendAnkhValueToClient(serverPlayer, clampedValue);
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
     * Initialize ankh data for a player joining the server.
     * 
     * @param player The player
     */
    public static void initializePlayer(ServerPlayerEntity player) {
        // Check if player already has ankh data
        PlayerDataStorage storage = (PlayerDataStorage) player;
        NbtCompound data = storage.ancientcurse$getPersistentData();
        
        if (!data.contains(ANKH_KEY)) {
            // New player, set default value
            data.putInt(ANKH_KEY, DEFAULT_ANKH_VALUE);
            AncientCurse.LOGGER.info("Initialized ankh value for new player " + player.getName().getString());
        }
        
        // Sync current value to client
        int ankhValue = data.getInt(ANKH_KEY);
        CurseZonePackets.sendAnkhValueToClient(player, ankhValue);
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