package com.ancientcurse.event;

import com.ancientcurse.network.CurseZonePackets;
import com.ancientcurse.player.AnkhDataManager;
import com.ancientcurse.util.CurseZoneArea;
import com.ancientcurse.util.CurseZoneAreaManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.world.ServerWorld;

/**
 * Handles player join events to sync curse zones and player data
 */
public class PlayerJoinHandler {
    
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerWorld world = handler.player.getServerWorld();
            CurseZoneAreaManager areaManager = CurseZoneAreaManager.get(world);
            
            // Initialize and sync ankh data for the joining player
            AnkhDataManager.initializePlayer(handler.player);
            
            // Sync all existing zones to the joining player
            for (CurseZoneArea area : areaManager.getAllAreas()) {
                CurseZonePackets.sendZoneAreaSyncToPlayer(
                    handler.player,
                    area.getId(),
                    area.getMin(),
                    area.getMax(),
                    area.getZoneName(),
                    area.getKhamsinLevel(),
                    area.getAnkhDrainRate(),
                    area.isEffectsEnabled()
                );
            }
        });
    }
}