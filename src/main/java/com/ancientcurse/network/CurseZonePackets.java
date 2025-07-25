package com.ancientcurse.network;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.client.CurseZoneClientCache;
import com.ancientcurse.util.CurseZoneData;
import com.ancientcurse.util.CurseZoneManager;
import com.ancientcurse.util.CurseZoneAreaManager;
import com.ancientcurse.util.CurseZoneArea;
import com.ancientcurse.util.WandSelectionManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import java.util.UUID;

public class CurseZonePackets {
    public static final Identifier UPDATE_ZONE = new Identifier(AncientCurse.MOD_ID, "update_curse_zone");
    public static final Identifier UPDATE_BOX_ZONE = new Identifier(AncientCurse.MOD_ID, "update_curse_box_zone");
    public static final Identifier SYNC_ZONES = new Identifier(AncientCurse.MOD_ID, "sync_curse_zones");
    public static final Identifier SYNC_ZONE_AREA = new Identifier(AncientCurse.MOD_ID, "sync_curse_zone_area");
    public static final Identifier REMOVE_ZONE_AREA = new Identifier(AncientCurse.MOD_ID, "remove_curse_zone_area");
    public static final Identifier DELETE_ZONE_REQUEST = new Identifier(AncientCurse.MOD_ID, "delete_zone_request");
    public static final Identifier UPDATE_ZONE_REQUEST = new Identifier(AncientCurse.MOD_ID, "update_zone_request");
    public static final Identifier SYNC_WAND_SELECTION = new Identifier(AncientCurse.MOD_ID, "sync_wand_selection");
    public static final Identifier CLEAR_WAND_SELECTION = new Identifier(AncientCurse.MOD_ID, "clear_wand_selection");
    
    public static void registerServerPackets() {
        ServerPlayNetworking.registerGlobalReceiver(UPDATE_ZONE, CurseZonePackets::handleUpdateZone);
        ServerPlayNetworking.registerGlobalReceiver(UPDATE_BOX_ZONE, CurseZonePackets::handleUpdateBoxZone);
        ServerPlayNetworking.registerGlobalReceiver(DELETE_ZONE_REQUEST, CurseZonePackets::handleDeleteZoneRequest);
        ServerPlayNetworking.registerGlobalReceiver(UPDATE_ZONE_REQUEST, CurseZonePackets::handleUpdateZoneRequest);
    }
    
    public static void registerClientPackets() {
        ClientPlayNetworking.registerGlobalReceiver(SYNC_ZONES, CurseZonePackets::handleSyncZones);
        ClientPlayNetworking.registerGlobalReceiver(SYNC_ZONE_AREA, CurseZonePackets::handleSyncZoneArea);
        ClientPlayNetworking.registerGlobalReceiver(REMOVE_ZONE_AREA, CurseZonePackets::handleRemoveZoneArea);
        ClientPlayNetworking.registerGlobalReceiver(SYNC_WAND_SELECTION, CurseZonePackets::handleSyncWandSelection);
        ClientPlayNetworking.registerGlobalReceiver(CLEAR_WAND_SELECTION, CurseZonePackets::handleClearWandSelection);
    }
    
    // Client -> Server: Update zone data
    public static void sendUpdateToServer(ChunkPos chunkPos, String zoneName, int khamsinLevel, int ankhDrain, boolean effectsEnabled) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeLong(chunkPos.toLong());
        buf.writeString(zoneName);
        buf.writeInt(khamsinLevel);
        buf.writeInt(ankhDrain);
        buf.writeBoolean(effectsEnabled);
        
        ClientPlayNetworking.send(UPDATE_ZONE, buf);
    }
    
    // Server -> Client: Send zone update
    public static void sendZoneUpdate(World world, ChunkPos chunkPos) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        
        CurseZoneManager manager = CurseZoneManager.get(serverWorld);
        CurseZoneData zone = manager.getZone(chunkPos);
        
        if (zone == null) return;
        
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeLong(chunkPos.toLong());
        buf.writeString(zone.getZoneName());
        buf.writeInt(zone.getKhamsinLevel());
        buf.writeInt(zone.getAnkhDrainRate());
        buf.writeBoolean(zone.isEffectsEnabled());
        
        // Send to all players in the world
        for (ServerPlayerEntity player : serverWorld.getPlayers()) {
            ServerPlayNetworking.send(player, SYNC_ZONES, buf);
        }
    }
    
    // Server packet handler
    private static void handleUpdateZone(MinecraftServer server, ServerPlayerEntity player, 
                                       ServerPlayNetworkHandler handler, PacketByteBuf buf, 
                                       PacketSender responseSender) {
        // Read packet data
        ChunkPos chunkPos = new ChunkPos(buf.readLong());
        String zoneName = buf.readString();
        int khamsinLevel = buf.readInt();
        int ankhDrain = buf.readInt();
        boolean effectsEnabled = buf.readBoolean();
        
        // Execute on server thread
        server.execute(() -> {
            // Verify player has permission
            if (!player.hasPermissionLevel(2)) {
                return;
            }
            
            ServerWorld world = player.getServerWorld();
            CurseZoneManager manager = CurseZoneManager.get(world);
            
            // Update zone data
            CurseZoneData zone = new CurseZoneData(chunkPos, zoneName, khamsinLevel, ankhDrain);
            zone.setEffectsEnabled(effectsEnabled);
            manager.setZone(chunkPos, zone);
            
            // Sync to all players
            sendZoneUpdate(world, chunkPos);
        });
    }
    
    // Client packet handler
    private static void handleSyncZones(MinecraftClient client, ClientPlayNetworkHandler handler, 
                                      PacketByteBuf buf, PacketSender responseSender) {
        // Read packet data
        ChunkPos chunkPos = new ChunkPos(buf.readLong());
        String zoneName = buf.readString();
        int khamsinLevel = buf.readInt();
        int ankhDrain = buf.readInt();
        boolean effectsEnabled = buf.readBoolean();
        
        // Execute on client thread
        client.execute(() -> {
            // Update client-side zone cache for rendering
            // This would need a client-side zone cache implementation
        });
    }
    
    // Client -> Server: Update box zone data
    public static void sendBoxUpdateToServer(BlockPos pos1, BlockPos pos2, String zoneName, int khamsinLevel, int ankhDrain, boolean effectsEnabled) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos1);
        buf.writeBlockPos(pos2);
        buf.writeString(zoneName);
        buf.writeInt(khamsinLevel);
        buf.writeInt(ankhDrain);
        buf.writeBoolean(effectsEnabled);
        
        ClientPlayNetworking.send(UPDATE_BOX_ZONE, buf);
    }
    
    // Server packet handler for box zones
    private static void handleUpdateBoxZone(MinecraftServer server, ServerPlayerEntity player, 
                                          ServerPlayNetworkHandler handler, PacketByteBuf buf, 
                                          PacketSender responseSender) {
        // Read packet data
        BlockPos pos1 = buf.readBlockPos();
        BlockPos pos2 = buf.readBlockPos();
        String zoneName = buf.readString();
        int khamsinLevel = buf.readInt();
        int ankhDrain = buf.readInt();
        boolean effectsEnabled = buf.readBoolean();
        
        // Execute on server thread
        server.execute(() -> {
            // Verify player has permission
            if (!player.hasPermissionLevel(2)) {
                return;
            }
            
            ServerWorld world = player.getServerWorld();
            CurseZoneAreaManager areaManager = CurseZoneAreaManager.get(world);
            
            // Add the new area
            String areaId = areaManager.addArea(pos1, pos2, zoneName, khamsinLevel, ankhDrain, effectsEnabled);
            
            // Sync to all players
            sendZoneAreaSync(world, areaId, pos1, pos2, zoneName, khamsinLevel, ankhDrain, effectsEnabled);
            
            // Clear the wand selection for this player after zone creation
            sendWandSelectionClear(world, player);
        });
    }
    
    // Server -> Client: Sync zone area
    public static void sendZoneAreaSync(ServerWorld world, String areaId, BlockPos min, BlockPos max,
                                       String zoneName, int khamsinLevel, int ankhDrain, boolean effectsEnabled) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(areaId);
        buf.writeBlockPos(min);
        buf.writeBlockPos(max);
        buf.writeString(zoneName);
        buf.writeInt(khamsinLevel);
        buf.writeInt(ankhDrain);
        buf.writeBoolean(effectsEnabled);
        
        // Send to all players in the world
        for (ServerPlayerEntity player : world.getPlayers()) {
            ServerPlayNetworking.send(player, SYNC_ZONE_AREA, buf);
        }
    }
    
    // Server -> Client: Sync zone area to specific player
    public static void sendZoneAreaSyncToPlayer(ServerPlayerEntity player, String areaId, BlockPos min, BlockPos max,
                                       String zoneName, int khamsinLevel, int ankhDrain, boolean effectsEnabled) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(areaId);
        buf.writeBlockPos(min);
        buf.writeBlockPos(max);
        buf.writeString(zoneName);
        buf.writeInt(khamsinLevel);
        buf.writeInt(ankhDrain);
        buf.writeBoolean(effectsEnabled);
        
        ServerPlayNetworking.send(player, SYNC_ZONE_AREA, buf);
    }
    
    // Server -> Client: Remove zone area
    public static void sendZoneAreaRemove(ServerWorld world, String areaId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(areaId);
        
        // Send to all players in the world
        for (ServerPlayerEntity player : world.getPlayers()) {
            ServerPlayNetworking.send(player, REMOVE_ZONE_AREA, buf);
        }
    }
    
    // Client packet handler for zone area sync
    private static void handleSyncZoneArea(MinecraftClient client, ClientPlayNetworkHandler handler,
                                          PacketByteBuf buf, PacketSender responseSender) {
        // Read packet data
        String areaId = buf.readString();
        BlockPos min = buf.readBlockPos();
        BlockPos max = buf.readBlockPos();
        String zoneName = buf.readString();
        int khamsinLevel = buf.readInt();
        int ankhDrain = buf.readInt();
        boolean effectsEnabled = buf.readBoolean();
        
        // Execute on client thread
        client.execute(() -> {
            CurseZoneClientCache.updateArea(areaId, min, max, zoneName, khamsinLevel, ankhDrain, effectsEnabled);
        });
    }
    
    // Client packet handler for zone area removal
    private static void handleRemoveZoneArea(MinecraftClient client, ClientPlayNetworkHandler handler,
                                            PacketByteBuf buf, PacketSender responseSender) {
        String areaId = buf.readString();
        
        // Execute on client thread
        client.execute(() -> {
            CurseZoneClientCache.removeArea(areaId);
        });
    }
    
    // Client -> Server: Request to delete a zone
    public static void sendDeleteZoneRequest(String areaId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(areaId);
        ClientPlayNetworking.send(DELETE_ZONE_REQUEST, buf);
    }
    
    // Client -> Server: Request to update existing zone
    public static void sendUpdateZoneRequest(String areaId, String zoneName, int khamsinLevel, int ankhDrain, boolean effectsEnabled) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(areaId);
        buf.writeString(zoneName);
        buf.writeInt(khamsinLevel);
        buf.writeInt(ankhDrain);
        buf.writeBoolean(effectsEnabled);
        ClientPlayNetworking.send(UPDATE_ZONE_REQUEST, buf);
    }
    
    // Server packet handler for delete zone request
    private static void handleDeleteZoneRequest(MinecraftServer server, ServerPlayerEntity player,
                                              ServerPlayNetworkHandler handler, PacketByteBuf buf,
                                              PacketSender responseSender) {
        String areaId = buf.readString();
        
        server.execute(() -> {
            // Verify player has permission
            if (!player.hasPermissionLevel(2)) {
                return;
            }
            
            ServerWorld world = player.getServerWorld();
            CurseZoneAreaManager areaManager = CurseZoneAreaManager.get(world);
            
            // Remove the area
            areaManager.removeArea(areaId);
            
            // Sync removal to all players
            sendZoneAreaRemove(world, areaId);
        });
    }
    
    // Server packet handler for update zone request
    private static void handleUpdateZoneRequest(MinecraftServer server, ServerPlayerEntity player,
                                              ServerPlayNetworkHandler handler, PacketByteBuf buf,
                                              PacketSender responseSender) {
        String areaId = buf.readString();
        String zoneName = buf.readString();
        int khamsinLevel = buf.readInt();
        int ankhDrain = buf.readInt();
        boolean effectsEnabled = buf.readBoolean();
        
        server.execute(() -> {
            // Verify player has permission
            if (!player.hasPermissionLevel(2)) {
                return;
            }
            
            ServerWorld world = player.getServerWorld();
            CurseZoneAreaManager areaManager = CurseZoneAreaManager.get(world);
            CurseZoneArea area = areaManager.getArea(areaId);
            
            if (area != null) {
                // Update the area
                area.setZoneName(zoneName);
                area.setKhamsinLevel(khamsinLevel);
                area.setAnkhDrainRate(ankhDrain);
                area.setEffectsEnabled(effectsEnabled);
                areaManager.markDirty();
                
                // Sync update to all players
                sendZoneAreaSync(world, areaId, area.getMin(), area.getMax(), zoneName, khamsinLevel, ankhDrain, effectsEnabled);
            }
        });
    }
    
    // Server -> Client: Sync wand selection to other players
    public static void sendWandSelectionSync(ServerWorld world, PlayerEntity player, BlockPos pos1, BlockPos pos2) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(player.getUuid());
        buf.writeBoolean(pos1 != null);
        if (pos1 != null) {
            buf.writeBlockPos(pos1);
        }
        buf.writeBoolean(pos2 != null);
        if (pos2 != null) {
            buf.writeBlockPos(pos2);
        }
        
        // Send to all players in the world
        for (ServerPlayerEntity otherPlayer : world.getPlayers()) {
            ServerPlayNetworking.send(otherPlayer, SYNC_WAND_SELECTION, buf);
        }
    }
    
    // Server -> Client: Clear wand selection for a player
    public static void sendWandSelectionClear(ServerWorld world, PlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(player.getUuid());
        
        // Send to all players in the world
        for (ServerPlayerEntity otherPlayer : world.getPlayers()) {
            ServerPlayNetworking.send(otherPlayer, CLEAR_WAND_SELECTION, buf);
        }
    }
    
    // Client packet handler for wand selection sync
    private static void handleSyncWandSelection(MinecraftClient client, ClientPlayNetworkHandler handler,
                                               PacketByteBuf buf, PacketSender responseSender) {
        UUID playerUuid = buf.readUuid();
        BlockPos pos1 = null;
        BlockPos pos2 = null;
        
        if (buf.readBoolean()) {
            pos1 = buf.readBlockPos();
        }
        if (buf.readBoolean()) {
            pos2 = buf.readBlockPos();
        }
        
        final BlockPos finalPos1 = pos1;
        final BlockPos finalPos2 = pos2;
        
        // Execute on client thread
        client.execute(() -> {
            // Update the visual selection for the specified player
            if (client.world != null) {
                PlayerEntity player = client.world.getPlayerByUuid(playerUuid);
                if (player != null) {
                    WandSelectionManager.clearSelection(player);
                    if (finalPos1 != null) {
                        WandSelectionManager.setFirstPosition(player, finalPos1);
                    }
                    if (finalPos2 != null) {
                        WandSelectionManager.setSecondPosition(player, finalPos2);
                    }
                }
            }
        });
    }
    
    // Client packet handler for clearing wand selection
    private static void handleClearWandSelection(MinecraftClient client, ClientPlayNetworkHandler handler,
                                                PacketByteBuf buf, PacketSender responseSender) {
        UUID playerUuid = buf.readUuid();
        
        // Execute on client thread
        client.execute(() -> {
            // Clear the visual selection for the specified player
            if (client.world != null) {
                PlayerEntity player = client.world.getPlayerByUuid(playerUuid);
                if (player != null) {
                    WandSelectionManager.clearSelection(player);
                }
            }
        });
    }
}