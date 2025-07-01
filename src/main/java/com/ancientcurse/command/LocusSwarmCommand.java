package com.ancientcurse.command;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.ModEntities;
import com.ancientcurse.entity.LocusEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

public class LocusSwarmCommand {
    // Track active swarm for the event
    private static LocusSwarmEvent activeSwarm = null;
    private static final Random random = new Random();
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("locusswarm")
            .requires(source -> source.hasPermissionLevel(2)) // Requires OP level 2
            .executes(LocusSwarmCommand::executeSwarm)
            .then(CommandManager.argument("target", net.minecraft.command.argument.EntityArgumentType.player())
                .executes(context -> executeSwarmAtPlayer(context, 
                    net.minecraft.command.argument.EntityArgumentType.getPlayer(context, "target"))))
            .then(CommandManager.literal("stop")
                .executes(LocusSwarmCommand::stopSwarm))
            .then(CommandManager.literal("info")
                .executes(LocusSwarmCommand::showInfo)));
    }
    
    private static int executeSwarm(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        
        if (player == null) {
            source.sendError(Text.literal("This command can only be used by players"));
            return 0;
        }
        
        // Check if swarm already active
        if (activeSwarm != null && activeSwarm.isActive()) {
            source.sendError(Text.literal("§cA locust swarm is already active! Use /locusswarm stop first."));
            return 0;
        }
        
        ServerWorld world = player.getServerWorld();
        Vec3d playerPos = player.getPos();
        
        // Create the swarm centered on admin with default settings
        activeSwarm = new LocusSwarmEvent(world, playerPos, 100); // 100 block radius
        activeSwarm.beginInfestation();
        
        // Send subtle admin confirmation
        source.sendFeedback(() -> Text.literal("§eLocust eggs planted at your location..."), false);
        
        // No immediate announcement to players - let them discover it naturally
        // The swarm will build up over time
        
        AncientCurse.LOGGER.info("Locust swarm started by " + player.getName().getString() + " at " + playerPos);
        
        return 1;
    }
    
    private static int executeSwarmAtPlayer(CommandContext<ServerCommandSource> context, ServerPlayerEntity targetPlayer) {
        ServerCommandSource source = context.getSource();
        
        // Check if swarm already active
        if (activeSwarm != null && activeSwarm.isActive()) {
            source.sendError(Text.literal("§cA locust swarm is already active! Use /locusswarm stop first."));
            return 0;
        }
        
        ServerWorld world = targetPlayer.getServerWorld();
        Vec3d targetPos = targetPlayer.getPos();
        
        // Create the swarm centered on target player
        activeSwarm = new LocusSwarmEvent(world, targetPos, 100); // 100 block radius
        activeSwarm.beginInfestation();
        
        // Send subtle admin confirmation
        source.sendFeedback(() -> Text.literal("§eLocust eggs planted near " + targetPlayer.getName().getString() + "..."), false);
        
        // Maybe a subtle hint to the target player, but not dramatic
        if (random.nextDouble() < 0.3) { // 30% chance
            targetPlayer.sendMessage(Text.literal("§7You notice the ground seems unusually disturbed...").formatted(Formatting.ITALIC), false);
        }
        
        AncientCurse.LOGGER.info("Locust swarm started by " + source.getName() + " targeting " + targetPlayer.getName().getString() + " at " + targetPos);
        
        return 1;
    }
    
    private static int stopSwarm(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (activeSwarm == null || !activeSwarm.isActive()) {
            source.sendError(Text.literal("§cNo active locust swarm to stop"));
            return 0;
        }
        
        activeSwarm.naturalDispersion();
        activeSwarm = null;
        
        source.sendFeedback(() -> Text.literal("§eLocust swarm dispersed"), false);
        
        // Natural dispersion message to players who can see locusts
        source.getWorld().getPlayers().forEach(p -> {
            if (p.getPos().distanceTo(activeSwarm.getSwarmCenter()) < 150) {
                p.sendMessage(Text.literal("§7The swarm begins to scatter to the winds...").formatted(Formatting.ITALIC), false);
            }
        });
        
        return 1;
    }
    
    private static int showInfo(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (activeSwarm == null || !activeSwarm.isActive()) {
            source.sendFeedback(() -> Text.literal("§eNo active locust swarm"), false);
            return 0;
        }
        
        source.sendFeedback(() -> Text.literal("§6═══ LOCUST PLAGUE STATUS ═══"), false);
        source.sendFeedback(() -> Text.literal("§eActive Locusts: §f" + activeSwarm.getSwarmSize()), false);
        source.sendFeedback(() -> Text.literal("§eDormant Eggs: §f" + activeSwarm.getDormantEggs()), false);
        source.sendFeedback(() -> Text.literal("§eSwarm Agitation: §f" + String.format("%.0f%%", activeSwarm.getSwarmAgitation() * 100)), false);
        source.sendFeedback(() -> Text.literal("§ePhase: §f" + activeSwarm.getPhaseDescription()), false);
        
        return 1;
    }
    
    // Called from your main mod's tick event
    public static void tickSwarm() {
        if (activeSwarm != null && activeSwarm.isActive()) {
            activeSwarm.updateSwarmBehavior();
        } else if (activeSwarm != null && !activeSwarm.isActive()) {
            activeSwarm = null;
        }
    }
}