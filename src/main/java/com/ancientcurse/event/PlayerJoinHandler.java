package com.ancientcurse.event;

import com.ancientcurse.network.BloodWaterPackets;
import com.ancientcurse.network.CurseZonePackets;
import com.ancientcurse.network.ThirstPackets;
import com.ancientcurse.player.AnkhDataManager;
import com.ancientcurse.player.ThirstDataManager;
import com.ancientcurse.system.BloodWaterData;
import com.ancientcurse.util.CurseZoneArea;
import com.ancientcurse.util.CurseZoneAreaManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Handles player join and respawn events to sync data and initialize systems
 */
public class PlayerJoinHandler {

    public static void register() {
        // Handle player join
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            ServerWorld world = player.getServerWorld();
            CurseZoneAreaManager areaManager = CurseZoneAreaManager.get(world);

            // Initialize and sync ankh data for the joining player
            AnkhDataManager.initializePlayer(player);

            // Initialize and sync thirst data for the joining player
            ThirstDataManager.initializePlayer(player);

            // Send thirst config to client
            ThirstPackets.sendConfigToClient(player);

            // Sync blood water state if active
            BloodWaterData bloodWaterData = BloodWaterData.getServerState(world);
            if (bloodWaterData.hasEffect()) {
                BloodWaterPackets.sendSyncPacket(world, bloodWaterData);
            }

            // Sync all existing curse zones to the joining player
            for (CurseZoneArea area : areaManager.getAllAreas()) {
                CurseZonePackets.sendZoneAreaSyncToPlayer(
                        player,
                        area.getId(),
                        area.getMin(),
                        area.getMax(),
                        area.getZoneName(),
                        area.getKhamsinLevel(),
                        area.getAnkhDrainRate(),
                        area.isEffectsEnabled());
            }
        });

        // Handle player respawn (after death)
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            // Reset thirst to full on respawn
            ThirstDataManager.onPlayerRespawn(newPlayer);

            // Grant blood water immunity for 5 seconds after respawn to prevent instant death loop
            ServerWorld world = newPlayer.getServerWorld();
            BloodWaterData bloodWaterData = BloodWaterData.getServerState(world);
            if (bloodWaterData.isActive()) {
                bloodWaterData.grantImmunity(newPlayer, 100); // 5 seconds immunity
            }

            // Sync thirst config again (in case client cache was cleared)
            ThirstPackets.sendConfigToClient(newPlayer);
        });
    }
}
