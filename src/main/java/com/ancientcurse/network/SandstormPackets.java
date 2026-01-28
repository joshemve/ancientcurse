package com.ancientcurse.network;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.client.SandstormClientHandler;
import com.ancientcurse.system.SandstormData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

public class SandstormPackets {
    public static final Identifier SYNC_SANDSTORM = new Identifier(AncientCurse.MOD_ID, "sync_sandstorm");

    public static void registerClientPackets() {
        ClientPlayNetworking.registerGlobalReceiver(SYNC_SANDSTORM, (client, handler, buf, responseSender) -> {
            boolean active = buf.readBoolean();
            float intensity = buf.readFloat();
            client.execute(() -> {
                SandstormClientHandler.update(active, intensity);
            });
        });
    }

    public static void sendSyncPacket(ServerWorld world, SandstormData data) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(data.isActive());
        buf.writeFloat(data.getIntensity());
        
        // Send to all players in the world
        for (ServerPlayerEntity player : world.getPlayers()) {
            ServerPlayNetworking.send(player, SYNC_SANDSTORM, buf);
        }
    }
    
    public static void sendSyncPacket(ServerPlayerEntity player, SandstormData data) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(data.isActive());
        buf.writeFloat(data.getIntensity());
        ServerPlayNetworking.send(player, SYNC_SANDSTORM, buf);
    }
}
