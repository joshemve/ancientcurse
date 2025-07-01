package com.ancientcurse.event;

import com.ancientcurse.block.CursedEarthBlock;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Handles player death events to trigger cursed earth death bursts
 * Implements the death mechanic from the Cursed Earth roadmap
 */
public class PlayerDeathEventHandler {
    
    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof PlayerEntity player) {
                // Register death event for this player
                player.getServer().getPlayerManager().getPlayerList().forEach(serverPlayer -> {
                    if (serverPlayer.equals(player)) {
                        // This will be handled by the death event
                    }
                });
            }
        });
    }
    
    /**
     * Called when a player dies to create a death burst
     * This should be called from a death event
     */
    public static void onPlayerDeath(PlayerEntity player) {
        if (player.getWorld() instanceof ServerWorld serverWorld) {
            BlockPos deathPos = player.getBlockPos();
            
            // Create death burst
            CursedEarthBlock.createDeathBurst(serverWorld, deathPos);
        }
    }
} 