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
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Random;

public class LocusSwarmCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("locusswarm")
            .requires(source -> source.hasPermissionLevel(2)) // Requires OP level 2
            .executes(LocusSwarmCommand::executeDefault)
            .then(CommandManager.argument("count", IntegerArgumentType.integer(1, 100))
                .executes(LocusSwarmCommand::executeWithCount)));
    }
    
    private static int executeDefault(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return executeSwarm(context, 20, 300, 50);
    }
    
    private static int executeWithCount(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int count = IntegerArgumentType.getInteger(context, "count");
        return executeSwarm(context, count, 300, 50);
    }
    
    private static int executeSwarm(CommandContext<ServerCommandSource> context, int count, int duration, int radius) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        
        if (player == null) {
            source.sendError(Text.literal("This command can only be used by players"));
            return 0;
        }
        
        World world = player.getWorld();
        Vec3d playerPos = player.getPos();
        Random random = new Random();
        
        // Start the swarm event
        LocusSwarmEvent swarmEvent = new LocusSwarmEvent(world, playerPos, count, duration, radius);
        swarmEvent.start();
        
        // Send feedback to player
        source.sendFeedback(() -> Text.literal("§6[Locus Swarm] §fSpawning " + count + " Locus entities for " + duration + " seconds within " + radius + " blocks radius"), true);
        
        AncientCurse.LOGGER.info("Locus swarm event started: " + count + " entities, " + duration + " seconds, " + radius + " blocks radius");
        
        return 1;
    }
} 