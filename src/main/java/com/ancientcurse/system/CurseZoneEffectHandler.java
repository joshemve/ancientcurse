package com.ancientcurse.system;

import com.ancientcurse.effect.KhamsinCurseEffect;
import com.ancientcurse.effect.ModStatusEffects;
import com.ancientcurse.player.AnkhDataManager;
import com.ancientcurse.util.CurseZoneArea;
import com.ancientcurse.util.CurseZoneAreaManager;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CurseZoneEffectHandler {

    private static int tickCounter = 0;
    private static final int TICKS_PER_SECOND = 20;
    private static final int DRAIN_INTERVAL_SECONDS = 3; // Apply drain every 3 seconds

    // Cooldown tracking for Ankh depletion effects (prevents spam)
    private static final Map<UUID, Long> depletionEffectCooldowns = new HashMap<>();
    private static final long DEPLETION_EFFECT_COOLDOWN_TICKS = 60; // 3 seconds between depletion effects
    
    /**
     * Called every server tick to handle curse zone effects
     */
    public static void tick(MinecraftServer server) {
        tickCounter++;
        
        // Only process every 3 seconds to avoid excessive drain
        if (tickCounter < TICKS_PER_SECOND * DRAIN_INTERVAL_SECONDS) {
            return;
        }
        
        tickCounter = 0;
        
        // Process each player
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.isSpectator() || player.isCreative()) {
                continue; // Skip spectators and creative mode players
            }
            
            BlockPos playerPos = player.getBlockPos();
            
            // Check if player is in any curse zone
            CurseZoneAreaManager areaManager = CurseZoneAreaManager.get(player.getServerWorld());
            for (CurseZoneArea zone : areaManager.getAllAreas()) {
                if (zone.contains(playerPos)) {
                    // Apply ankh drain
                    int drainRate = zone.getAnkhDrainRate();
                    if (drainRate > 0) {
                        // Calculate drain amount for the interval
                        // drainRate is per minute, so divide by 60 for per second, then multiply by interval
                        float drainPerInterval = (drainRate / 60.0f) * DRAIN_INTERVAL_SECONDS;
                        int drainAmount = Math.max(1, Math.round(drainPerInterval));

                        int newAnkh = AnkhDataManager.decreaseAnkhValue(player, drainAmount);

                        // Handle Ankh depletion (when meter hits 0)
                        if (newAnkh <= 0) {
                            handleAnkhDepletion(player, zone);
                        }

                        // Send update packet to client to update HUD
                        com.ancientcurse.network.CurseZonePackets.sendAnkhValueToClient(player, newAnkh);
                    }
                    
                    // Apply Khamsin curse if configured
                    if (zone.getKhamsinLevel() > 0 && zone.isEffectsEnabled()) {
                        applyKhamsinCurse(player, zone.getKhamsinLevel());
                    }
                    
                    // Only process the first zone the player is in
                    break;
                }
            }
        }
    }
    
    /**
     * Handles what happens when a player's Ankh meter is fully depleted (hits 0).
     * This applies severe consequences including direct damage and powerful curses.
     * Uses cooldown system to prevent spam effects.
     */
    private static void handleAnkhDepletion(ServerPlayerEntity player, CurseZoneArea zone) {
        UUID playerId = player.getUuid();
        long currentTime = player.getWorld().getTime();

        // Check cooldown to prevent spam
        Long lastEffect = depletionEffectCooldowns.get(playerId);
        if (lastEffect != null && (currentTime - lastEffect) < DEPLETION_EFFECT_COOLDOWN_TICKS) {
            return; // Still on cooldown, skip effects
        }

        // Update cooldown
        depletionEffectCooldowns.put(playerId, currentTime);

        // Apply direct damage when Ankh is depleted (1 heart every 3 seconds)
        player.damage(player.getWorld().getDamageSources().magic(), 2.0f);

        // Automatically apply maximum Khamsin curse when depleted
        if (zone.isEffectsEnabled()) {
            // Force Stage 5 curse (most severe) when Ankh is depleted
            StatusEffect maxCurse = ModStatusEffects.KHAMSIN_CURSE_STAGE_5;

            // Apply the maximum curse with extended duration
            player.addStatusEffect(new StatusEffectInstance(
                maxCurse,
                300, // 15 seconds (longer than normal refresh)
                0,
                false,
                true,
                true
            ));
        }

        // Apply additional debilitating effects when Ankh is fully depleted
        player.addStatusEffect(new StatusEffectInstance(
            net.minecraft.entity.effect.StatusEffects.DARKNESS,
            200, // 10 seconds
            0,
            false,
            true,
            true
        ));

        // Add a visual/audio warning to the player
        player.sendMessage(
            net.minecraft.text.Text.literal("Your Ankh protection has failed! The curse consumes you...")
                .formatted(net.minecraft.util.Formatting.DARK_RED, net.minecraft.util.Formatting.BOLD),
            true // Show as action bar (above hotbar)
        );

        // Play ominous sound effect
        player.getWorld().playSound(
            null,
            player.getX(), player.getY(), player.getZ(),
            net.minecraft.sound.SoundEvents.ENTITY_WARDEN_AMBIENT,
            player.getSoundCategory(),
            0.8f,
            0.5f
        );

        // Clean up stale cooldowns (every 100 activations or so)
        if (depletionEffectCooldowns.size() > 100) {
            depletionEffectCooldowns.entrySet().removeIf(entry ->
                (currentTime - entry.getValue()) > DEPLETION_EFFECT_COOLDOWN_TICKS * 10
            );
        }
    }

    /**
     * Applies Khamsin curse effect to a player based on the zone's level
     */
    private static void applyKhamsinCurse(ServerPlayerEntity player, int level) {
        // Clamp level to valid range
        level = Math.max(1, Math.min(5, level));
        
        // Get the appropriate status effect based on the level
        StatusEffect effect = switch (level) {
            case 1 -> ModStatusEffects.KHAMSIN_CURSE_STAGE_1;
            case 2 -> ModStatusEffects.KHAMSIN_CURSE_STAGE_2;
            case 3 -> ModStatusEffects.KHAMSIN_CURSE_STAGE_3;
            case 4 -> ModStatusEffects.KHAMSIN_CURSE_STAGE_4;
            case 5 -> ModStatusEffects.KHAMSIN_CURSE_STAGE_5;
            default -> ModStatusEffects.KHAMSIN_CURSE_STAGE_1;
        };
        
        // Check if player already has a khamsin curse
        boolean hasAnyCurse = player.hasStatusEffect(ModStatusEffects.KHAMSIN_CURSE_STAGE_1) ||
                              player.hasStatusEffect(ModStatusEffects.KHAMSIN_CURSE_STAGE_2) ||
                              player.hasStatusEffect(ModStatusEffects.KHAMSIN_CURSE_STAGE_3) ||
                              player.hasStatusEffect(ModStatusEffects.KHAMSIN_CURSE_STAGE_4) ||
                              player.hasStatusEffect(ModStatusEffects.KHAMSIN_CURSE_STAGE_5);
        
        // Only apply if player doesn't already have a curse or if the zone level is higher
        if (!hasAnyCurse) {
            // Apply the new effect with a short duration that refreshes while in the zone
            player.addStatusEffect(new StatusEffectInstance(
                effect,
                200, // 10 seconds, will refresh every 3 seconds while in zone
                0, // Amplifier
                false, // Ambient
                true, // Show particles
                true  // Show icon
            ));
        } else {
            // Check if we need to upgrade the curse level
            StatusEffectInstance existingCurse = null;
            int currentLevel = 0;
            
            for (int i = 1; i <= 5; i++) {
                StatusEffect checkEffect = switch (i) {
                    case 1 -> ModStatusEffects.KHAMSIN_CURSE_STAGE_1;
                    case 2 -> ModStatusEffects.KHAMSIN_CURSE_STAGE_2;
                    case 3 -> ModStatusEffects.KHAMSIN_CURSE_STAGE_3;
                    case 4 -> ModStatusEffects.KHAMSIN_CURSE_STAGE_4;
                    case 5 -> ModStatusEffects.KHAMSIN_CURSE_STAGE_5;
                    default -> null;
                };
                
                if (checkEffect != null && player.hasStatusEffect(checkEffect)) {
                    existingCurse = player.getStatusEffect(checkEffect);
                    currentLevel = i;
                    break;
                }
            }
            
            // If zone level is higher than current curse level, upgrade it
            if (level > currentLevel) {
                // Remove current curse
                if (existingCurse != null) {
                    player.removeStatusEffectInternal(existingCurse.getEffectType());
                }
                
                // Apply new curse
                player.addStatusEffect(new StatusEffectInstance(
                    effect,
                    200, // 10 seconds
                    0,
                    false,
                    true,
                    true
                ));
            } else if (existingCurse != null && existingCurse.getDuration() < 100) {
                // Refresh the existing curse duration if it's about to expire
                player.removeStatusEffectInternal(existingCurse.getEffectType());
                player.addStatusEffect(new StatusEffectInstance(
                    existingCurse.getEffectType(),
                    200, // Refresh to 10 seconds
                    0,
                    false,
                    true,
                    true
                ));
            }
        }
    }
}