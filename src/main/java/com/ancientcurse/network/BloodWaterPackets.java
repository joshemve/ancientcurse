package com.ancientcurse.network;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.client.BloodWaterClientHandler;
import com.ancientcurse.system.BloodWaterConfig;
import com.ancientcurse.system.BloodWaterData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

/**
 * Network packets for syncing Blood Water plague event state between server and clients.
 *
 * Packet types:
 * - SYNC_BLOOD_WATER: General state sync (active, intensity, config)
 * - CURSE_UPDATE: Notify client of their curse status (cursed, linger time)
 * - DAMAGE_FLASH: Trigger damage visual effect on client
 */
public class BloodWaterPackets {
    public static final Identifier SYNC_BLOOD_WATER = new Identifier(AncientCurse.MOD_ID, "sync_blood_water");
    public static final Identifier CURSE_UPDATE = new Identifier(AncientCurse.MOD_ID, "blood_water_curse");
    public static final Identifier DAMAGE_FLASH = new Identifier(AncientCurse.MOD_ID, "blood_water_damage");

    /**
     * Register client-side packet receiver.
     * Call this from client mod initializer.
     */
    public static void registerClientPackets() {
        // Main state sync
        ClientPlayNetworking.registerGlobalReceiver(SYNC_BLOOD_WATER, (client, handler, buf, responseSender) -> {
            boolean active = buf.readBoolean();
            float intensity = buf.readFloat();
            int remainingSeconds = buf.readInt();

            // Read config values for client rendering
            boolean curseEnabled = buf.readBoolean();
            boolean showCurseOverlay = buf.readBoolean();
            float overlayAlpha = buf.readFloat();
            boolean showCurseTimer = buf.readBoolean();
            boolean showDamageFlash = buf.readBoolean();

            // Execute on main client thread
            client.execute(() -> {
                BloodWaterClientHandler.update(active, intensity, remainingSeconds,
                    curseEnabled, showCurseOverlay, overlayAlpha, showCurseTimer, showDamageFlash);
            });
        });

        // Curse state update for individual player
        ClientPlayNetworking.registerGlobalReceiver(CURSE_UPDATE, (client, handler, buf, responseSender) -> {
            boolean cursed = buf.readBoolean();
            int lingerTicks = buf.readInt();

            client.execute(() -> {
                BloodWaterClientHandler.updateCurseState(cursed, lingerTicks);
            });
        });

        // Damage flash effect
        ClientPlayNetworking.registerGlobalReceiver(DAMAGE_FLASH, (client, handler, buf, responseSender) -> {
            float damageAmount = buf.readFloat();

            client.execute(() -> {
                BloodWaterClientHandler.onDamageReceived(damageAmount);
            });
        });
    }

    /**
     * Send sync packet to all players in a world
     */
    public static void sendSyncPacket(ServerWorld world, BloodWaterData data) {
        BloodWaterConfig config = BloodWaterConfig.get();

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(data.isActive());
        buf.writeFloat(data.getIntensity());
        buf.writeInt(data.getRemainingDuration() > 0 ? data.getRemainingDuration() / 20 : -1);

        // Write config values for client rendering
        buf.writeBoolean(config.isCurseEnabled());
        buf.writeBoolean(config.isShowCurseOverlay());
        buf.writeFloat(config.getOverlayAlphaMultiplier());
        buf.writeBoolean(config.isShowCurseTimer());
        buf.writeBoolean(config.isShowDamageFlash());

        for (ServerPlayerEntity player : world.getPlayers()) {
            ServerPlayNetworking.send(player, SYNC_BLOOD_WATER, PacketByteBufs.copy(buf));
        }
    }

    /**
     * Send sync packet to a specific player (e.g., when they join)
     */
    public static void sendSyncPacket(ServerPlayerEntity player, BloodWaterData data) {
        BloodWaterConfig config = BloodWaterConfig.get();

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(data.isActive());
        buf.writeFloat(data.getIntensity());
        buf.writeInt(data.getRemainingDuration() > 0 ? data.getRemainingDuration() / 20 : -1);

        // Write config values for client rendering
        buf.writeBoolean(config.isCurseEnabled());
        buf.writeBoolean(config.isShowCurseOverlay());
        buf.writeFloat(config.getOverlayAlphaMultiplier());
        buf.writeBoolean(config.isShowCurseTimer());
        buf.writeBoolean(config.isShowDamageFlash());

        ServerPlayNetworking.send(player, SYNC_BLOOD_WATER, buf);
    }

    /**
     * Send curse state update to a specific player
     */
    // Recompile fix
    public static void sendCursePacket(ServerPlayerEntity player, boolean cursed, int lingerTicks) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(cursed);
        buf.writeInt(lingerTicks);
        ServerPlayNetworking.send(player, CURSE_UPDATE, buf);
    }

    /**
     * Send damage flash effect to a player
     */
    public static void sendDamageFlashPacket(ServerPlayerEntity player, float damageAmount) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeFloat(damageAmount);
        ServerPlayNetworking.send(player, DAMAGE_FLASH, buf);
    }
}
