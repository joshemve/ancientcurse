package com.ancientcurse.network;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.client.DarknessClientHandler;
import com.ancientcurse.system.DarknessConfig;
import com.ancientcurse.system.DarknessData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

/**
 * Network packets for syncing Darkness plague event state between server and clients.
 *
 * Packet types:
 * - SYNC_DARKNESS: General state sync (active, intensity, duration, config)
 * - LIGHTNING_FLASH: Notify clients of a lightning flash event
 */
public class DarknessPackets {
    public static final Identifier SYNC_DARKNESS = new Identifier(AncientCurse.MOD_ID, "sync_darkness");
    public static final Identifier LIGHTNING_FLASH = new Identifier(AncientCurse.MOD_ID, "darkness_flash");

    /**
     * Register client-side packet receivers.
     * Call this from client mod initializer.
     */
    public static void registerClientPackets() {
        // Main state sync
        ClientPlayNetworking.registerGlobalReceiver(SYNC_DARKNESS, (client, handler, buf, responseSender) -> {
            // Read basic state
            boolean active = buf.readBoolean();
            float intensity = buf.readFloat();
            float flashIntensity = buf.readFloat();
            int remainingSeconds = buf.readInt();
            int remainingDays = buf.readInt();

            // Read config values for client-side rendering
            float fadeInSpeed = buf.readFloat();
            float fadeOutSpeed = buf.readFloat();
            float maxIntensity = buf.readFloat();
            float minAmbientLight = buf.readFloat();
            float overlayAlpha = buf.readFloat();
            boolean showTimer = buf.readBoolean();
            boolean applyDesaturation = buf.readBoolean();
            float desaturationStrength = buf.readFloat();
            boolean affectSky = buf.readBoolean();
            boolean enableFlashes = buf.readBoolean();
            boolean dimLights = buf.readBoolean();
            float lightEffectiveness = buf.readFloat();
            boolean playAmbient = buf.readBoolean();
            float ambientVolume = buf.readFloat();

            client.execute(() -> {
                DarknessClientHandler.update(
                    active, intensity, flashIntensity, remainingSeconds, remainingDays,
                    fadeInSpeed, fadeOutSpeed, maxIntensity, minAmbientLight,
                    overlayAlpha, showTimer, applyDesaturation, desaturationStrength,
                    affectSky, enableFlashes, dimLights, lightEffectiveness,
                    playAmbient, ambientVolume
                );
            });
        });

        // Lightning flash notification (for dramatic visual + sound)
        ClientPlayNetworking.registerGlobalReceiver(LIGHTNING_FLASH, (client, handler, buf, responseSender) -> {
            float flashStrength = buf.readFloat();

            client.execute(() -> {
                DarknessClientHandler.onLightningFlash(flashStrength);
            });
        });
    }

    /**
     * Send sync packet to all players in a world
     */
    public static void sendSyncPacket(ServerWorld world, DarknessData data) {
        DarknessConfig config = DarknessConfig.get();

        PacketByteBuf buf = PacketByteBufs.create();

        // Write basic state
        buf.writeBoolean(data.isActive());
        buf.writeFloat(data.getIntensity());
        buf.writeFloat(data.getFlashIntensity());

        // Convert remaining duration to seconds and days
        int remainingTicks = data.getRemainingDuration();
        buf.writeInt(remainingTicks > 0 ? remainingTicks / 20 : -1); // seconds
        buf.writeInt(remainingTicks > 0 ? remainingTicks / 24000 : -1); // days

        // Write config values for client rendering
        buf.writeFloat(config.getIntensityFadeInSpeed());
        buf.writeFloat(config.getIntensityFadeOutSpeed());
        buf.writeFloat(config.getMaxIntensity());
        buf.writeFloat(config.getMinimumAmbientLight());
        buf.writeFloat(config.getOverlayAlphaMultiplier());
        buf.writeBoolean(config.showDarknessTimer());
        buf.writeBoolean(config.applyDesaturation());
        buf.writeFloat(config.getDesaturationStrength());
        buf.writeBoolean(config.affectSkyColor());
        buf.writeBoolean(config.enableLightningFlashes());
        buf.writeBoolean(config.dimLightSources());
        buf.writeFloat(config.getLightSourceEffectiveness());
        buf.writeBoolean(config.playAmbientSounds());
        buf.writeFloat(config.getAmbientSoundVolume());

        for (ServerPlayerEntity player : world.getPlayers()) {
            ServerPlayNetworking.send(player, SYNC_DARKNESS, PacketByteBufs.copy(buf));
        }
    }

    /**
     * Send sync packet to a specific player (e.g., on join or dimension change)
     */
    public static void sendSyncPacket(ServerPlayerEntity player, DarknessData data) {
        DarknessConfig config = DarknessConfig.get();

        PacketByteBuf buf = PacketByteBufs.create();

        // Write basic state
        buf.writeBoolean(data.isActive());
        buf.writeFloat(data.getIntensity());
        buf.writeFloat(data.getFlashIntensity());

        // Convert remaining duration to seconds and days
        int remainingTicks = data.getRemainingDuration();
        buf.writeInt(remainingTicks > 0 ? remainingTicks / 20 : -1); // seconds
        buf.writeInt(remainingTicks > 0 ? remainingTicks / 24000 : -1); // days

        // Write config values for client rendering
        buf.writeFloat(config.getIntensityFadeInSpeed());
        buf.writeFloat(config.getIntensityFadeOutSpeed());
        buf.writeFloat(config.getMaxIntensity());
        buf.writeFloat(config.getMinimumAmbientLight());
        buf.writeFloat(config.getOverlayAlphaMultiplier());
        buf.writeBoolean(config.showDarknessTimer());
        buf.writeBoolean(config.applyDesaturation());
        buf.writeFloat(config.getDesaturationStrength());
        buf.writeBoolean(config.affectSkyColor());
        buf.writeBoolean(config.enableLightningFlashes());
        buf.writeBoolean(config.dimLightSources());
        buf.writeFloat(config.getLightSourceEffectiveness());
        buf.writeBoolean(config.playAmbientSounds());
        buf.writeFloat(config.getAmbientSoundVolume());

        ServerPlayNetworking.send(player, SYNC_DARKNESS, buf);
    }

    /**
     * Notify all players of a lightning flash
     */
    public static void sendLightningFlashPacket(ServerWorld world, float strength) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeFloat(strength);

        for (ServerPlayerEntity player : world.getPlayers()) {
            ServerPlayNetworking.send(player, LIGHTNING_FLASH, PacketByteBufs.copy(buf));
        }
    }
}
