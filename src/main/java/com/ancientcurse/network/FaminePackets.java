package com.ancientcurse.network;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.client.FamineClientHandler;
import com.ancientcurse.system.FamineConfig;
import com.ancientcurse.system.FamineData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

/**
 * Network packets for syncing Famine event state between server and clients.
 *
 * Packet types:
 * - SYNC_FAMINE: General state sync (active, intensity, config)
 * - FOOD_SPOILED: Notify client when their food spoils (for effects)
 */
public class FaminePackets {
    public static final Identifier SYNC_FAMINE = new Identifier(AncientCurse.MOD_ID, "sync_famine");
    public static final Identifier FOOD_SPOILED = new Identifier(AncientCurse.MOD_ID, "food_spoiled");

    /**
     * Register client-side packet receivers.
     * Call this from client mod initializer.
     */
    public static void registerClientPackets() {
        // Main state sync
        ClientPlayNetworking.registerGlobalReceiver(SYNC_FAMINE, (client, handler, buf, responseSender) -> {
            boolean active = buf.readBoolean();
            float intensity = buf.readFloat();
            int remainingSeconds = buf.readInt();

            // Read config values for client-side rendering
            float fadeInSpeed = buf.readFloat();
            float fadeOutSpeed = buf.readFloat();
            float vignetteAlpha = buf.readFloat();
            boolean showTimer = buf.readBoolean();
            boolean showSpoilageFlash = buf.readBoolean();

            client.execute(() -> {
                FamineClientHandler.update(active, intensity, remainingSeconds,
                    fadeInSpeed, fadeOutSpeed, vignetteAlpha, showTimer, showSpoilageFlash);
            });
        });

        // Food spoilage notification (for visual/audio feedback)
        ClientPlayNetworking.registerGlobalReceiver(FOOD_SPOILED, (client, handler, buf, responseSender) -> {
            int slot = buf.readInt();
            int amount = buf.readInt();

            client.execute(() -> {
                FamineClientHandler.onFoodSpoiled(slot, amount);
            });
        });
    }

    /**
     * Send sync packet to all players in a world
     */
    public static void sendSyncPacket(ServerWorld world, FamineData data) {
        FamineConfig config = FamineConfig.get();

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(data.isActive());
        buf.writeFloat(data.getIntensity());
        buf.writeInt(data.getRemainingDuration() > 0 ? data.getRemainingDuration() / 20 : -1);

        // Write config values for client rendering
        buf.writeFloat(config.getIntensityFadeInSpeed());
        buf.writeFloat(config.getIntensityFadeOutSpeed());
        buf.writeFloat(config.getVignetteAlphaMultiplier());
        buf.writeBoolean(config.showFamineTimer());
        buf.writeBoolean(config.showSpoilageFlash());

        for (ServerPlayerEntity player : world.getPlayers()) {
            ServerPlayNetworking.send(player, SYNC_FAMINE, PacketByteBufs.copy(buf));
        }
    }

    /**
     * Send sync packet to a specific player (e.g., on join)
     */
    public static void sendSyncPacket(ServerPlayerEntity player, FamineData data) {
        FamineConfig config = FamineConfig.get();

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(data.isActive());
        buf.writeFloat(data.getIntensity());
        buf.writeInt(data.getRemainingDuration() > 0 ? data.getRemainingDuration() / 20 : -1);

        // Write config values for client rendering
        buf.writeFloat(config.getIntensityFadeInSpeed());
        buf.writeFloat(config.getIntensityFadeOutSpeed());
        buf.writeFloat(config.getVignetteAlphaMultiplier());
        buf.writeBoolean(config.showFamineTimer());
        buf.writeBoolean(config.showSpoilageFlash());

        ServerPlayNetworking.send(player, SYNC_FAMINE, buf);
    }

    /**
     * Notify a player that some of their food spoiled
     */
    public static void sendFoodSpoiledPacket(ServerPlayerEntity player, int slot, int amount) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(slot);
        buf.writeInt(amount);
        ServerPlayNetworking.send(player, FOOD_SPOILED, buf);
    }
}
