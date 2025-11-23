package com.ancientcurse.item;

import com.ancientcurse.entity.AnubisEntity;
import com.ancientcurse.entity.ThothEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import software.bernie.geckolib.animatable.GeoEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Debug stick for monitoring entity animations in real-time
 * Right-click on any GeckoLib entity to start monitoring it
 * Prints animation changes to chat as they happen
 */
public class AnimationDebugStick extends Item {

    // Track which player is monitoring which entity
    private static final Map<UUID, UUID> playerTracking = new HashMap<>();

    // Track last known state for each monitored entity
    private static final Map<UUID, EntityDebugState> lastStates = new WeakHashMap<>();

    public AnimationDebugStick(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (user.getWorld().isClient) {
            return ActionResult.SUCCESS;
        }

        // Check if entity is a GeckoLib animated entity
        if (!(entity instanceof GeoEntity)) {
            user.sendMessage(Text.literal("§c[Animation Debug] Entity is not animated (not a GeoEntity)"), false);
            return ActionResult.SUCCESS;
        }

        UUID playerUUID = user.getUuid();
        UUID entityUUID = entity.getUuid();

        // Toggle tracking
        if (playerTracking.containsKey(playerUUID) && playerTracking.get(playerUUID).equals(entityUUID)) {
            // Stop tracking
            playerTracking.remove(playerUUID);
            lastStates.remove(entityUUID);
            user.sendMessage(Text.literal("§e[Animation Debug] §fStopped monitoring §6" + entity.getType().getName().getString()), false);
        } else {
            // Start tracking
            playerTracking.put(playerUUID, entityUUID);
            String entityName = entity.getType().getName().getString();
            user.sendMessage(Text.literal("§e[Animation Debug] §fNow monitoring §6" + entityName + "§f. Keep the debug stick in your inventory."), false);
            user.sendMessage(Text.literal("§7Animation changes will appear in chat. Right-click again to stop."), false);
        }

        return ActionResult.SUCCESS;
    }

    /**
     * Called every tick to check for animation changes
     * This should be called from a server tick event
     */
    public static void tick(ServerPlayerEntity player) {
        UUID playerUUID = player.getUuid();

        // Check if player is tracking an entity
        if (!playerTracking.containsKey(playerUUID)) {
            return;
        }

        // Check if player has debug stick in inventory
        if (!hasDebugStick(player)) {
            playerTracking.remove(playerUUID);
            lastStates.remove(playerTracking.get(playerUUID));
            return;
        }

        UUID entityUUID = playerTracking.get(playerUUID);

        // Find the entity in the world
        LivingEntity entity = findEntity(player, entityUUID);
        if (entity == null || entity.isRemoved()) {
            player.sendMessage(Text.literal("§c[Animation Debug] Lost track of entity"), false);
            playerTracking.remove(playerUUID);
            lastStates.remove(entityUUID);
            return;
        }

        // Check for state changes and print them
        if (entity instanceof ThothEntity thoth) {
            checkThothStateChanges(player, thoth);
        } else if (entity instanceof AnubisEntity anubis) {
            checkAnubisStateChanges(player, anubis);
        }
    }

    private static boolean hasDebugStick(PlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() instanceof AnimationDebugStick) {
                return true;
            }
        }
        return false;
    }

    private static LivingEntity findEntity(PlayerEntity player, UUID entityUUID) {
        net.minecraft.entity.Entity entity = ((net.minecraft.server.world.ServerWorld) player.getWorld()).getEntity(entityUUID);
        if (entity instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    private static void checkThothStateChanges(PlayerEntity player, ThothEntity thoth) {
        UUID entityUUID = thoth.getUuid();
        EntityDebugState lastState = lastStates.get(entityUUID);

        int currentAttackState = thoth.getAttackState();
        boolean currentCasting = thoth.isCastingTimeMagic();
        boolean currentInCombat = thoth.isInCombat();
        boolean currentReading = thoth.isReading();
        boolean currentSpawning = thoth.isInSpawnTransition();

        // First time tracking this entity
        if (lastState == null) {
            lastState = new EntityDebugState();
            lastState.thothAttackState = currentAttackState;
            lastState.thothCasting = currentCasting;
            lastState.thothInCombat = currentInCombat;
            lastState.thothReading = currentReading;
            lastState.thothSpawning = currentSpawning;
            lastStates.put(entityUUID, lastState);

            // Print initial state
            String animation = getThothAnimationName(currentAttackState, currentCasting, currentInCombat, currentSpawning, thoth);
            player.sendMessage(Text.literal("§a[Animation] §bCurrent: §f" + animation), false);
            return;
        }

        // Check for attack state changes
        if (currentAttackState != lastState.thothAttackState) {
            String oldAttack = getThothAttackName(lastState.thothAttackState);
            String newAttack = getThothAttackName(currentAttackState);
            String newAnimation = getThothAnimationName(currentAttackState, currentCasting, currentInCombat, currentSpawning, thoth);

            player.sendMessage(Text.literal("§a[Animation] §e" + oldAttack + " §7-> §b" + newAttack + " §7| §f" + newAnimation), false);
            lastState.thothAttackState = currentAttackState;
        }

        // Check for casting state changes
        if (currentCasting != lastState.thothCasting) {
            if (currentCasting) {
                player.sendMessage(Text.literal("§a[Animation] §bStarted Time Magic §7| §fanimation.thoth.time_bend"), false);
            } else {
                player.sendMessage(Text.literal("§a[Animation] §7Stopped Time Magic"), false);
            }
            lastState.thothCasting = currentCasting;
        }

        // Check for combat state changes
        if (currentInCombat != lastState.thothInCombat) {
            if (currentInCombat) {
                player.sendMessage(Text.literal("§a[Animation] §cEntered Combat"), false);
            } else {
                player.sendMessage(Text.literal("§a[Animation] §7Exited Combat §7| §fanimation.thoth.idle"), false);
            }
            lastState.thothInCombat = currentInCombat;
        }

        // Check for spawning state changes
        if (currentSpawning != lastState.thothSpawning) {
            if (currentSpawning) {
                player.sendMessage(Text.literal("§a[Animation] §dSpawn Started §7| §fanimation.thoth.entity_spawn"), false);
            } else {
                player.sendMessage(Text.literal("§a[Animation] §7Spawn Complete"), false);
            }
            lastState.thothSpawning = currentSpawning;
        }
    }

    private static void checkAnubisStateChanges(PlayerEntity player, AnubisEntity anubis) {
        UUID entityUUID = anubis.getUuid();
        EntityDebugState lastState = lastStates.get(entityUUID);

        // First time tracking
        if (lastState == null) {
            lastState = new EntityDebugState();
            lastStates.put(entityUUID, lastState);
            player.sendMessage(Text.literal("§a[Animation] §fMonitoring Anubis - state tracking not fully implemented"), false);
        }

        // TODO: Add Anubis-specific state tracking when needed
    }

    private static String getThothAttackName(int attackState) {
        return switch (attackState) {
            case 0 -> "NONE";
            case 1 -> "SCROLL_BLAST";
            case 2 -> "TIME_BEND";
            case 3 -> "ENTITY_SUMMON";
            case 4 -> "MELEE";
            default -> "UNKNOWN";
        };
    }

    private static String getThothAnimationName(int attackState, boolean isCasting, boolean inCombat, boolean spawning, ThothEntity thoth) {
        if (spawning) {
            return "animation.thoth.entity_spawn";
        }

        switch (attackState) {
            case 1: // SCROLL_BLAST
                return "animation.thoth.attack_2";
            case 2: // TIME_BEND
                return "animation.thoth.time_bend";
            case 3: // ENTITY_SUMMON
                return "animation.thoth.entity_spawn";
            case 4: // MELEE
                return "animation.thoth.attack_1";
            default:
                if (isCasting) {
                    return "animation.thoth.time_bend";
                }
                
                // Check if actually moving to determine walking vs idle
                double velocitySquared = thoth.getVelocity().horizontalLengthSquared();
                boolean isMoving = velocitySquared > 0.001; // Same threshold as ThothEntity
                
                if (isMoving) {
                    return "animation.thoth.walking";
                } else if (inCombat) {
                    return "animation.thoth.idle_standing";
                } else {
                    return "animation.thoth.idle";
                }
        }
    }

    /**
     * Clean up tracking when player disconnects
     */
    public static void cleanup(UUID playerUUID) {
        UUID entityUUID = playerTracking.remove(playerUUID);
        if (entityUUID != null) {
            lastStates.remove(entityUUID);
        }
    }

    private static class EntityDebugState {
        int thothAttackState = 0;
        boolean thothCasting = false;
        boolean thothInCombat = false;
        boolean thothReading = false;
        boolean thothSpawning = false;
    }
}
