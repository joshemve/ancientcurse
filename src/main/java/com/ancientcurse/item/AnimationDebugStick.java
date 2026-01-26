package com.ancientcurse.item;

import com.ancientcurse.entity.AnubisEntity;
import com.ancientcurse.entity.RaEntity;
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
        // Only process main hand to prevent double-triggering
        if (hand != Hand.MAIN_HAND) {
            return ActionResult.PASS;
        }

        if (user.getWorld().isClient) {
            return ActionResult.SUCCESS;
        }

        // Check if entity is a GeckoLib animated entity
        if (!(entity instanceof GeoEntity)) {
            user.sendMessage(Text.literal("§c[Animation Debug] Entity is not animated (not a GeoEntity)"), false);
            return ActionResult.SUCCESS;
        }

        // Check if entity type is supported
        boolean supported = (entity instanceof ThothEntity) || (entity instanceof AnubisEntity)
                || (entity instanceof RaEntity);
        if (!supported) {
            user.sendMessage(Text.literal(
                    "§c[Animation Debug] Entity type not supported: §f" + entity.getType().getName().getString()),
                    false);
            user.sendMessage(Text.literal("§7Supported: Thoth, Anubis, Ra"), false);
            return ActionResult.SUCCESS;
        }

        UUID playerUUID = user.getUuid();
        UUID entityUUID = entity.getUuid();

        // Toggle tracking
        if (playerTracking.containsKey(playerUUID) && playerTracking.get(playerUUID).equals(entityUUID)) {
            // Stop tracking
            playerTracking.remove(playerUUID);
            lastStates.remove(entityUUID);
            user.sendMessage(
                    Text.literal(
                            "§e[Animation Debug] §fStopped monitoring §6" + entity.getType().getName().getString()),
                    false);
        } else {
            // Start tracking
            playerTracking.put(playerUUID, entityUUID);
            String entityName = entity.getType().getName().getString();
            user.sendMessage(Text.literal("§e[Animation Debug] §fNow monitoring §6" + entityName + "§f."), false);
            user.sendMessage(Text.literal("§7Animation changes will appear in chat. Right-click again to stop."),
                    false);
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
        } else if (entity instanceof RaEntity ra) {
            checkRaStateChanges(player, ra);
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
        net.minecraft.entity.Entity entity = ((net.minecraft.server.world.ServerWorld) player.getWorld())
                .getEntity(entityUUID);
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
            String animation = getThothAnimationName(currentAttackState, currentCasting, currentInCombat,
                    currentSpawning, thoth);
            player.sendMessage(Text.literal("§a[Animation] §bCurrent: §f" + animation), false);
            return;
        }

        // Check for attack state changes
        if (currentAttackState != lastState.thothAttackState) {
            String oldAttack = getThothAttackName(lastState.thothAttackState);
            String newAttack = getThothAttackName(currentAttackState);
            String newAnimation = getThothAnimationName(currentAttackState, currentCasting, currentInCombat,
                    currentSpawning, thoth);

            player.sendMessage(
                    Text.literal("§a[Animation] §e" + oldAttack + " §7-> §b" + newAttack + " §7| §f" + newAnimation),
                    false);
            lastState.thothAttackState = currentAttackState;
        }

        // Check for casting state changes
        if (currentCasting != lastState.thothCasting) {
            if (currentCasting) {
                player.sendMessage(Text.literal("§a[Animation] §bStarted Time Magic §7| §fanimation.thoth.time_bend"),
                        false);
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
                player.sendMessage(Text.literal("§a[Animation] §dSpawn Started §7| §fanimation.thoth.entity_spawn"),
                        false);
            } else {
                player.sendMessage(Text.literal("§a[Animation] §7Spawn Complete"), false);
            }
            lastState.thothSpawning = currentSpawning;
        }
    }

    private static void checkAnubisStateChanges(PlayerEntity player, AnubisEntity anubis) {
        UUID entityUUID = anubis.getUuid();
        EntityDebugState lastState = lastStates.get(entityUUID);

        AnubisEntity.BossPhase currentPhase = anubis.getBossPhase();
        boolean currentHovering = anubis.isHovering();
        boolean currentJudging = anubis.isJudging();
        boolean currentSkyYelling = anubis.isSkyYelling();
        boolean currentPlayerSafe = anubis.isPlayerSafe();
        boolean currentAttacking = anubis.handSwinging && anubis.handSwingTicks > 0;

        // Calculate movement state
        double velocitySquared = anubis.getVelocity().horizontalLengthSquared();
        boolean currentMoving = velocitySquared > 0.01;
        boolean currentRunning = velocitySquared > 0.05;

        // First time tracking this entity
        if (lastState == null) {
            lastState = new EntityDebugState();
            lastState.anubisPhase = currentPhase;
            lastState.anubisHovering = currentHovering;
            lastState.anubisJudging = currentJudging;
            lastState.anubisSkyYelling = currentSkyYelling;
            lastState.anubisPlayerSafe = currentPlayerSafe;
            lastState.anubisAttacking = currentAttacking;
            lastState.anubisMoving = currentMoving;
            lastState.anubisRunning = currentRunning;
            lastStates.put(entityUUID, lastState);

            // Print initial state
            String animation = getAnubisAnimationName(currentPhase, currentHovering, currentJudging, currentSkyYelling,
                    currentPlayerSafe, anubis);
            player.sendMessage(Text.literal("§a[Animation] §bCurrent: §f" + animation), false);
            player.sendMessage(Text.literal("§a[Animation] §bPhase: §f" + currentPhase.name()), false);
            player.sendMessage(Text.literal("§7[Debug] Moving: " + currentMoving + " | Running: " + currentRunning
                    + " | Attacking: " + currentAttacking), false);
            return;
        }

        // Check for phase changes
        if (currentPhase != lastState.anubisPhase) {
            String newAnimation = getAnubisAnimationName(currentPhase, currentHovering, currentJudging,
                    currentSkyYelling, currentPlayerSafe, anubis);
            player.sendMessage(Text.literal("§a[Animation] §ePhase: §7" + lastState.anubisPhase.name() + " §7-> §b"
                    + currentPhase.name() + " §7| §f" + newAnimation), false);
            lastState.anubisPhase = currentPhase;
        }

        // Check for hovering state changes
        if (currentHovering != lastState.anubisHovering) {
            if (currentHovering) {
                player.sendMessage(Text.literal("§a[Animation] §bStarted Hovering"), false);
            } else {
                player.sendMessage(Text.literal("§a[Animation] §7Stopped Hovering"), false);
            }
            lastState.anubisHovering = currentHovering;
        }

        // Check for judging state changes
        if (currentJudging != lastState.anubisJudging) {
            if (currentJudging) {
                player.sendMessage(
                        Text.literal("§a[Animation] §dStarted Judging §7| §fanimation.anubis.judgement_idle"), false);
            } else {
                player.sendMessage(Text.literal("§a[Animation] §7Stopped Judging"), false);
            }
            lastState.anubisJudging = currentJudging;
        }

        // Check for sky yelling state changes
        if (currentSkyYelling != lastState.anubisSkyYelling) {
            if (currentSkyYelling) {
                player.sendMessage(Text.literal("§a[Animation] §6Started Sky Yell §7| §fanimation.anubis.attack_2"),
                        false);
            } else {
                player.sendMessage(Text.literal("§a[Animation] §7Stopped Sky Yell"), false);
            }
            lastState.anubisSkyYelling = currentSkyYelling;
        }

        // Check for player safe state changes
        if (currentPlayerSafe != lastState.anubisPlayerSafe) {
            if (currentPlayerSafe) {
                player.sendMessage(
                        Text.literal("§a[Animation] §2Player Deemed Safe §7| §fanimation.anubis.judgement_safe"),
                        false);
            } else {
                player.sendMessage(Text.literal("§a[Animation] §7Player No Longer Safe"), false);
            }
            lastState.anubisPlayerSafe = currentPlayerSafe;
        }

        // Check for attacking state changes (melee attack)
        if (currentAttacking != lastState.anubisAttacking) {
            if (currentAttacking) {
                player.sendMessage(Text.literal("§a[Animation] §cStarted Melee Attack §7| §fanimation.anubis.attack_1"),
                        false);
            } else {
                player.sendMessage(Text.literal("§a[Animation] §7Stopped Melee Attack"), false);
            }
            lastState.anubisAttacking = currentAttacking;
        }

        // Check for movement state changes
        if (currentMoving != lastState.anubisMoving) {
            if (currentMoving) {
                String moveAnim = currentRunning ? "animation.anubis.running" : "animation.anubis.walking";
                player.sendMessage(Text.literal("§a[Animation] §3Started Moving §7| §f" + moveAnim), false);
            } else {
                player.sendMessage(Text.literal("§a[Animation] §7Stopped Moving §7| §fanimation.anubis.idle"), false);
            }
            lastState.anubisMoving = currentMoving;
            lastState.anubisRunning = currentRunning;
        } else if (currentMoving && currentRunning != lastState.anubisRunning) {
            // Still moving but speed changed
            if (currentRunning) {
                player.sendMessage(Text.literal("§a[Animation] §3Speed Up - Running §7| §fanimation.anubis.running"),
                        false);
            } else {
                player.sendMessage(Text.literal("§a[Animation] §3Slowed Down - Walking §7| §fanimation.anubis.walking"),
                        false);
            }
            lastState.anubisRunning = currentRunning;
        }
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

    private static String getThothAnimationName(int attackState, boolean isCasting, boolean inCombat, boolean spawning,
            ThothEntity thoth) {
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

    private static String getAnubisAnimationName(AnubisEntity.BossPhase phase, boolean hovering, boolean judging,
            boolean skyYelling, boolean playerSafe, AnubisEntity anubis) {
        // Death animation takes absolute priority
        if (phase == AnubisEntity.BossPhase.DEAD) {
            return "animation.anubis.death";
        }

        // Special animations take priority
        if (skyYelling) {
            return "animation.anubis.attack_2";
        }

        if (judging && playerSafe) {
            return "animation.anubis.judgement_safe";
        }

        if (judging) {
            return "animation.anubis.judgement_idle";
        }

        // Attack animations (check if attacking)
        if (anubis.handSwinging) {
            return "animation.anubis.attack_1";
        }

        // Movement animations
        double velocitySquared = anubis.getVelocity().horizontalLengthSquared();
        boolean isMoving = velocitySquared > 0.01; // Movement threshold

        if (isMoving) {
            // Determine if running or walking based on speed
            if (velocitySquared > 0.05) {
                return "animation.anubis.running";
            } else {
                return "animation.anubis.walking";
            }
        }

        // Default to idle
        return "animation.anubis.idle";
    }

    /* ========== RA ENTITY DEBUG ========== */

    private static void checkRaStateChanges(PlayerEntity player, RaEntity ra) {
        UUID entityUUID = ra.getUuid();
        EntityDebugState lastState = lastStates.get(entityUUID);

        // Current state
        RaEntity.RaCombatState currentCombatState = ra.getCombatState();
        RaEntity.RaPhase currentPhase = ra.getCurrentPhase();
        boolean currentFlying = ra.isFlying();
        boolean currentPerformingAction = ra.isPerformingAction();
        boolean currentHibernating = ra.isHibernating();
        boolean currentDead = ra.isDead();
        double velocitySquared = ra.getVelocity().horizontalLengthSquared();
        boolean currentMoving = velocitySquared > 0.003;

        // Calculate expected animation based on state
        String expectedAnimation = getRaExpectedAnimation(ra, currentCombatState, currentFlying,
                currentPerformingAction, currentDead, currentMoving);

        // First time tracking this entity
        if (lastState == null) {
            lastState = new EntityDebugState();
            lastState.raCombatState = currentCombatState;
            lastState.raPhase = currentPhase;
            lastState.raFlying = currentFlying;
            lastState.raPerformingAction = currentPerformingAction;
            lastState.raHibernating = currentHibernating;
            lastState.raMoving = currentMoving;
            lastState.raDead = currentDead;
            lastState.raExpectedAnimation = expectedAnimation;
            lastStates.put(entityUUID, lastState);

            // Print initial state summary
            player.sendMessage(Text.literal("§6[Ra Debug] §eInitial State:"), false);
            player.sendMessage(
                    Text.literal("§7  Combat: §f" + currentCombatState.name() + " §7| Phase: §f" + currentPhase.name()),
                    false);
            player.sendMessage(
                    Text.literal("§7  Flying: §f" + currentFlying + " §7| Action: §f" + currentPerformingAction),
                    false);
            player.sendMessage(Text.literal("§7  Health: §c" + String.format("%.0f", ra.getHealth()) + "§7/§c"
                    + String.format("%.0f", ra.getMaxHealth())), false);
            player.sendMessage(Text.literal("§a[Animation] §bExpected: §f" + expectedAnimation), false);
            return;
        }

        // Check for death state change (highest priority)
        if (currentDead != lastState.raDead) {
            if (currentDead) {
                player.sendMessage(Text.literal("§6[Ra Debug] §4DIED §7-> §fra.death"), false);
            }
            lastState.raDead = currentDead;
        }

        // Check for combat state changes
        if (currentCombatState != lastState.raCombatState) {
            String stateColor = getStateColor(currentCombatState);
            player.sendMessage(Text.literal("§6[Ra Debug] §eCombat State: §7" + lastState.raCombatState.name()
                    + " §7-> " + stateColor + currentCombatState.name()), false);

            // Show which controller will handle this
            String controller = currentPerformingAction ? "ATTACK" : "MOVEMENT";
            player.sendMessage(Text.literal("§7  Controller: §f" + controller + " §7-> §b" + expectedAnimation), false);

            lastState.raCombatState = currentCombatState;
        }

        // Check for phase changes
        if (currentPhase != lastState.raPhase) {
            String phaseColor = switch (currentPhase) {
                case PHASE_1_AWAKENED -> "§a";
                case PHASE_2_SOLAR_WRATH -> "§e";
                case PHASE_3_DIVINE_FURY -> "§c";
            };
            player.sendMessage(Text.literal("§6[Ra Debug] §dPHASE CHANGE: §7" + lastState.raPhase.name() + " §7-> "
                    + phaseColor + currentPhase.name()), false);
            player.sendMessage(Text.literal("§7  Health: §c" + String.format("%.0f", ra.getHealth()) + " §7(§c"
                    + String.format("%.0f%%", ra.getHealthPercent() * 100) + "§7)"), false);
            lastState.raPhase = currentPhase;
        }

        // Check for flying state changes
        if (currentFlying != lastState.raFlying) {
            if (currentFlying) {
                player.sendMessage(Text.literal("§6[Ra Debug] §bTOOK FLIGHT §7-> §fra.flying"), false);
            } else {
                player.sendMessage(Text.literal("§6[Ra Debug] §7LANDED §7-> §fra.idle"), false);
            }
            lastState.raFlying = currentFlying;
        }

        // Check for action state changes
        if (currentPerformingAction != lastState.raPerformingAction) {
            if (currentPerformingAction) {
                player.sendMessage(Text.literal("§6[Ra Debug] §cACTION START §7(movement controller stopped)"), false);
            } else {
                player.sendMessage(Text.literal("§6[Ra Debug] §7ACTION END §7(movement controller resumed)"), false);
            }
            lastState.raPerformingAction = currentPerformingAction;
        }

        // Check for hibernation state changes
        if (currentHibernating != lastState.raHibernating) {
            if (currentHibernating) {
                player.sendMessage(Text.literal("§6[Ra Debug] §5HIBERNATING (STUNNED) §7-> §fra.hibernation"), false);
            } else {
                player.sendMessage(Text.literal("§6[Ra Debug] §7Woke from hibernation"), false);
            }
            lastState.raHibernating = currentHibernating;
        }

        // Check for movement state changes (only when not in action)
        if (!currentPerformingAction && currentMoving != lastState.raMoving) {
            if (currentMoving) {
                player.sendMessage(Text.literal("§6[Ra Debug] §3Started moving §7-> §fra.walking"), false);
            } else {
                player.sendMessage(Text.literal("§6[Ra Debug] §7Stopped moving §7-> §fra.idle"), false);
            }
            lastState.raMoving = currentMoving;
        }

        // Check if expected animation changed
        if (!expectedAnimation.equals(lastState.raExpectedAnimation)) {
            player.sendMessage(
                    Text.literal("§a[Animation] §7" + lastState.raExpectedAnimation + " §7-> §b" + expectedAnimation),
                    false);
            lastState.raExpectedAnimation = expectedAnimation;
        }
    }

    private static String getStateColor(RaEntity.RaCombatState state) {
        return switch (state) {
            case IDLE -> "§7";
            case WALKING -> "§f";
            case FLYING -> "§b";
            case MELEE -> "§c";
            case GROUND_SMACK -> "§6";
            case SHARD_ATTACK -> "§e";
            case FLYING_STAFF_ATTACK -> "§d";
            case HIBERNATING -> "§5";
            case DYING -> "§4";
        };
    }

    private static String getRaExpectedAnimation(RaEntity ra, RaEntity.RaCombatState combatState, boolean flying,
            boolean performingAction, boolean dead, boolean moving) {
        // Death takes absolute priority
        if (dead) {
            return "ra.death";
        }

        // Attack controller animations (when performing action)
        if (performingAction) {
            return switch (combatState) {
                case MELEE -> "ra.melee";
                case GROUND_SMACK -> "ra.flying_ground_smack";
                case SHARD_ATTACK -> "ra.shard_attack";
                case FLYING_STAFF_ATTACK -> "ra.flying_staff_attack";
                default -> "§c(unexpected action state: " + combatState.name() + ")";
            };
        }

        // Movement controller animations
        if (combatState == RaEntity.RaCombatState.HIBERNATING) {
            return "ra.hibernation";
        }

        if (flying) {
            return "ra.flying";
        }

        if (moving) {
            return "ra.walking";
        }

        return "ra.idle";
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
        // Thoth state
        int thothAttackState = 0;
        boolean thothCasting = false;
        boolean thothInCombat = false;
        boolean thothReading = false;
        boolean thothSpawning = false;

        // Anubis state
        AnubisEntity.BossPhase anubisPhase = AnubisEntity.BossPhase.DORMANT;
        boolean anubisHovering = false;
        boolean anubisJudging = false;
        boolean anubisSkyYelling = false;
        boolean anubisPlayerSafe = false;
        boolean anubisAttacking = false;
        boolean anubisMoving = false;
        boolean anubisRunning = false;

        // Ra state
        RaEntity.RaCombatState raCombatState = RaEntity.RaCombatState.IDLE;
        RaEntity.RaPhase raPhase = RaEntity.RaPhase.PHASE_1_AWAKENED;
        boolean raFlying = false;
        boolean raPerformingAction = false;
        boolean raHibernating = false;
        boolean raMoving = false;
        boolean raDead = false;
        String raExpectedAnimation = "";
    }
}
