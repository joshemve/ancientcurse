package com.ancientcurse.system;

import com.ancientcurse.item.armor.JackelBindsItem;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the breakout mechanic for Jackel Binds
 * Players can slowly punch to break free, with escalating chain sounds
 */
public class JackelBindsBreakoutManager {
    private static final JackelBindsBreakoutManager INSTANCE = new JackelBindsBreakoutManager();

    // Breakout configuration
    private static final int PUNCHES_TO_BREAK_FREE = 30; // Number of punches needed
    private static final long TIMEOUT_TICKS = 60; // 3 seconds before progress resets
    private static final long PUNCH_COOLDOWN_TICKS = 8; // Minimum 0.4 seconds between punches (2-3 punches per second max)
    private static final double WARNING_RADIUS = 20.0; // Radius to warn nearby players

    // Track breakout progress per player
    private final Map<UUID, BreakoutProgress> breakoutProgress = new HashMap<>();

    private JackelBindsBreakoutManager() {}

    public static JackelBindsBreakoutManager getInstance() {
        return INSTANCE;
    }

    /**
     * Called when a bound player attempts to punch (attack with no target)
     * @return true if the punch should be allowed (for breakout), false otherwise
     */
    public boolean onBoundPlayerPunch(ServerPlayerEntity player) {
        if (!isPlayerBound(player)) {
            return false;
        }

        UUID playerId = player.getUuid();
        long currentTime = player.getWorld().getTime();

        // Get or create progress
        BreakoutProgress progress = breakoutProgress.computeIfAbsent(playerId,
            id -> new BreakoutProgress(currentTime));

        // Check if progress should reset due to timeout
        if (currentTime - progress.lastPunchTime > TIMEOUT_TICKS) {
            progress.punchCount = 0;
            // Play reset sound (rope tightening back)
            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_WOOL_STEP, SoundCategory.PLAYERS, 0.5f, 0.5f);
        }

        // Check cooldown - prevent spam punching (must wait between punches)
        if (currentTime - progress.lastPunchTime < PUNCH_COOLDOWN_TICKS) {
            return true; // Punch registered but too soon, ignore it
        }

        // Increment punch count
        progress.punchCount++;
        progress.lastPunchTime = currentTime;

        // Calculate volume based on progress (gets louder as they progress)
        float progressPercent = (float) progress.punchCount / PUNCHES_TO_BREAK_FREE;
        float volume = 0.3f + (progressPercent * 1.2f); // 0.3 to 1.5
        float pitch = 0.8f + (progressPercent * 0.4f); // 0.8 to 1.2

        // Play rope straining sound (louder as progress increases)
        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.BLOCK_WOOL_HIT, SoundCategory.PLAYERS, volume, pitch);

        // Warn nearby players at certain thresholds
        if (progress.punchCount == 10) {
            warnNearbyPlayers(player, "is struggling against their binds...", Formatting.YELLOW);
        } else if (progress.punchCount == 20) {
            warnNearbyPlayers(player, "is close to breaking free!", Formatting.GOLD);
        } else if (progress.punchCount == PUNCHES_TO_BREAK_FREE - 3) {
            warnNearbyPlayers(player, "is about to escape!", Formatting.RED);
        }

        // Check if player broke free
        if (progress.punchCount >= PUNCHES_TO_BREAK_FREE) {
            breakFree(player);
            breakoutProgress.remove(playerId);
            return true;
        }

        // No text feedback - only the escalating chain sounds indicate progress
        return true;
    }

    /**
     * Warns nearby players that someone is trying to break free
     */
    private void warnNearbyPlayers(ServerPlayerEntity escapingPlayer, String message, Formatting color) {
        ServerWorld world = escapingPlayer.getServerWorld();
        String playerName = escapingPlayer.getName().getString();

        for (ServerPlayerEntity nearbyPlayer : world.getPlayers()) {
            if (nearbyPlayer == escapingPlayer) continue;

            double distance = nearbyPlayer.distanceTo(escapingPlayer);
            if (distance <= WARNING_RADIUS) {
                nearbyPlayer.sendMessage(Text.literal(playerName + " " + message).formatted(color), false);
            }
        }
    }

    /**
     * Called when a player successfully breaks free
     */
    private void breakFree(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();

        // Remove the binds
        ItemStack binds = player.getEquippedStack(EquipmentSlot.CHEST);
        if (binds.getItem() instanceof JackelBindsItem) {
            // Damage the binds significantly when broken out of
            binds.damage(binds.getMaxDamage() / 2, player, p -> {});

            // Drop the damaged binds
            player.equipStack(EquipmentSlot.CHEST, ItemStack.EMPTY);
            player.dropStack(binds);
        }

        // Play dramatic break-free sound (rope snapping)
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.PLAYERS, 1.5f, 0.6f);
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.BLOCK_WOOL_BREAK, SoundCategory.PLAYERS, 1.5f, 0.4f);

        // Notify the player
        player.sendMessage(Text.literal("You broke free from your binds!").formatted(Formatting.GREEN), true);

        // Warn nearby players
        warnNearbyPlayers(player, "has broken free from their binds!", Formatting.RED);
    }

    /**
     * Resets a player's breakout progress (called on death, disconnect, etc.)
     */
    public void resetProgress(UUID playerId) {
        breakoutProgress.remove(playerId);
    }

    /**
     * Called every tick to clean up stale entries and handle timeouts
     */
    public void tick(long currentTime) {
        Iterator<Map.Entry<UUID, BreakoutProgress>> iterator = breakoutProgress.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, BreakoutProgress> entry = iterator.next();
            BreakoutProgress progress = entry.getValue();

            // Remove entries that have timed out (no activity for 10 seconds)
            if (currentTime - progress.lastPunchTime > 200) {
                iterator.remove();
            }
        }
    }

    /**
     * Check if player is currently bound
     */
    private boolean isPlayerBound(PlayerEntity player) {
        ItemStack chestpiece = player.getEquippedStack(EquipmentSlot.CHEST);
        return chestpiece.getItem() instanceof JackelBindsItem;
    }

    /**
     * Get current breakout progress for a player (0.0 to 1.0)
     */
    public float getBreakoutProgress(UUID playerId) {
        BreakoutProgress progress = breakoutProgress.get(playerId);
        if (progress == null) return 0.0f;
        return (float) progress.punchCount / PUNCHES_TO_BREAK_FREE;
    }

    /**
     * Inner class to track breakout progress
     */
    private static class BreakoutProgress {
        int punchCount = 0;
        long lastPunchTime;

        BreakoutProgress(long startTime) {
            this.lastPunchTime = startTime;
        }
    }
}
