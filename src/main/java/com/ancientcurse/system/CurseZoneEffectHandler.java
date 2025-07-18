package com.ancientcurse.system;

import com.ancientcurse.util.AnkhManager;
import com.ancientcurse.util.CurseZoneArea;
import com.ancientcurse.util.CurseZoneAreaManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public class CurseZoneEffectHandler {
    
    private static int tickCounter = 0;
    private static final int TICKS_PER_SECOND = 20;
    private static final int DRAIN_INTERVAL_SECONDS = 3; // Apply drain every 3 seconds
    
    /**
     * Called every server tick to handle curse zone effects
     */
    public static void tick(MinecraftServer server) {
        tickCounter++;
        
        // Only process every 3 seconds to avoid excessive drain
        if (tickCounter < TICKS_PER_SECOND * DRAIN_INTERVAL_SECONDS) {
            return;
        }
        
        tickCounter = 0;
        
        // Process each player
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.isSpectator() || player.isCreative()) {
                continue; // Skip spectators and creative mode players
            }
            
            BlockPos playerPos = player.getBlockPos();
            
            // Check if player is in any curse zone
            CurseZoneAreaManager areaManager = CurseZoneAreaManager.get(player.getServerWorld());
            for (CurseZoneArea zone : areaManager.getAllAreas()) {
                if (zone.contains(playerPos)) {
                    // Apply ankh drain
                    int drainRate = zone.getAnkhDrainRate();
                    if (drainRate > 0) {
                        // Calculate drain amount for the interval
                        // drainRate is per minute, so divide by 60 for per second, then multiply by interval
                        float drainPerInterval = (drainRate / 60.0f) * DRAIN_INTERVAL_SECONDS;
                        int drainAmount = Math.max(1, Math.round(drainPerInterval));
                        
                        int newAnkh = AnkhManager.decreaseAnkhValue(player, drainAmount);
                        
                        // TODO: Send update packet to client to update HUD
                        // For now, the client will poll the value
                    }
                    
                    // Apply Khamsin curse if configured
                    if (zone.getKhamsinLevel() > 0 && zone.isEffectsEnabled()) {
                        // TODO: Apply Khamsin curse effect based on level
                        // This would integrate with the existing curse system
                    }
                    
                    // Only process the first zone the player is in
                    break;
                }
            }
        }
    }
}