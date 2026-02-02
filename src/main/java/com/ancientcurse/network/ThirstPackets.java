package com.ancientcurse.network;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.config.ModConfig;
import com.ancientcurse.player.ThirstDataManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Handles network packets for syncing thirst data between server and client.
 */
public class ThirstPackets {
    public static final Identifier SYNC_THIRST = new Identifier(AncientCurse.MOD_ID, "sync_thirst");
    public static final Identifier SYNC_CONFIG = new Identifier(AncientCurse.MOD_ID, "sync_thirst_config");

    /**
     * Register server-side packet handlers.
     * Called from AncientCurse main class initialization.
     */
    public static void registerServerPackets() {
        // Currently no client -> server packets needed for thirst
        // Server is authoritative over thirst values
    }

    /**
     * Register client-side packet handlers.
     * Called from AncientCurseClient initialization.
     */
    public static void registerClientPackets() {
        ClientPlayNetworking.registerGlobalReceiver(SYNC_THIRST, ThirstPackets::handleSyncThirst);
        ClientPlayNetworking.registerGlobalReceiver(SYNC_CONFIG, ThirstPackets::handleSyncConfig);
    }

    /**
     * Send thirst data from server to a specific client.
     * Called whenever thirst value changes on the server.
     *
     * @param player The player to send the update to
     * @param thirst The current thirst value (0-20)
     * @param saturation The current thirst saturation
     */
    public static void sendThirstToClient(ServerPlayerEntity player, int thirst, float saturation) {
        // Validate values before sending to ensure data integrity
        int validatedThirst = Math.max(0, Math.min(ThirstDataManager.MAX_THIRST, thirst));
        float validatedSaturation = Math.max(0.0f, Math.min(20.0f, saturation));

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(validatedThirst);
        buf.writeFloat(validatedSaturation);
        ServerPlayNetworking.send(player, SYNC_THIRST, buf);
    }

    /**
     * Client-side handler for thirst sync packet.
     */
    private static void handleSyncThirst(MinecraftClient client, ClientPlayNetworkHandler handler,
                                         PacketByteBuf buf, PacketSender responseSender) {
        int thirst = buf.readInt();
        float saturation = buf.readFloat();

        // Validate received values to prevent malicious packets from corrupting client state
        final int validatedThirst = Math.max(0, Math.min(ThirstDataManager.MAX_THIRST, thirst));
        final float validatedSaturation = Math.max(0.0f, Math.min(20.0f, saturation));

        // Execute on client thread
        client.execute(() -> {
            if (client.player != null) {
                ThirstDataManager.setClientThirstValue(validatedThirst, validatedSaturation);
            }
        });
    }

    /**
     * Send thirst config from server to a specific client.
     * Called when player joins to sync server config.
     *
     * @param player The player to send the config to
     */
    public static void sendConfigToClient(ServerPlayerEntity player) {
        ModConfig config = ModConfig.get();
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeFloat(config.getThirstDrainMultiplier());
        buf.writeBoolean(config.isThirstEnabled());
        buf.writeFloat(config.getDesertDrainMultiplier());
        ServerPlayNetworking.send(player, SYNC_CONFIG, buf);
        AncientCurse.LOGGER.debug("Sent thirst config to player {}: drainMultiplier={}, enabled={}, desertMultiplier={}",
            player.getName().getString(), config.getThirstDrainMultiplier(), config.isThirstEnabled(), config.getDesertDrainMultiplier());
    }

    /**
     * Client-side handler for config sync packet.
     */
    private static void handleSyncConfig(MinecraftClient client, ClientPlayNetworkHandler handler,
                                         PacketByteBuf buf, PacketSender responseSender) {
        float drainMultiplier = buf.readFloat();
        boolean enabled = buf.readBoolean();
        float desertMultiplier = buf.readFloat();

        // Validate config values to prevent exploits
        final float validatedDrainMultiplier = Math.max(0.0f, Math.min(10.0f, drainMultiplier));
        final float validatedDesertMultiplier = Math.max(0.0f, Math.min(10.0f, desertMultiplier));

        // Execute on client thread
        client.execute(() -> {
            ModConfig.get().updateFromServer(validatedDrainMultiplier, enabled, validatedDesertMultiplier);
        });
    }
}
