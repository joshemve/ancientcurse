package com.ancientcurse.network;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.client.MirageClientHandler;
import com.ancientcurse.system.MirageData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

public class MiragePackets {
    public static final Identifier SYNC_MIRAGE = new Identifier(AncientCurse.MOD_ID, "sync_mirage");

    public static void registerClientPackets() {
        ClientPlayNetworking.registerGlobalReceiver(SYNC_MIRAGE, (client, handler, buf, responseSender) -> {
            boolean active = buf.readBoolean();
            float intensity = buf.readFloat();
            client.execute(() -> {
                MirageClientHandler.update(active, intensity);
            });
        });
    }

    public static void sendSyncPacket(ServerWorld world, MirageData data) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(data.isActive());
        buf.writeFloat(data.getIntensity());

        for (ServerPlayerEntity player : world.getPlayers()) {
            ServerPlayNetworking.send(player, SYNC_MIRAGE, buf);
        }
    }

    public static void sendSyncPacket(ServerPlayerEntity player, MirageData data) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(data.isActive());
        buf.writeFloat(data.getIntensity());
        ServerPlayNetworking.send(player, SYNC_MIRAGE, buf);
    }
}
