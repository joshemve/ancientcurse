package com.ancientcurse.event;

import com.ancientcurse.item.AnimationDebugStick;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Handles server-side ticking for the Animation Debug Stick
 */
public class AnimationDebugHandler {

    public static void register() {
        // Tick all players each server tick
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                AnimationDebugStick.tick(player);
            }
        });

        // Clean up when player disconnects
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            AnimationDebugStick.cleanup(handler.getPlayer().getUuid());
        });
    }
}
