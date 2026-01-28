package com.ancientcurse.command;

import com.ancientcurse.system.SandstormData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

public class SandstormCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, 
                              CommandRegistryAccess registryAccess,
                              CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("sandstorm")
            .requires(source -> source.hasPermissionLevel(2))
            .then(CommandManager.literal("start")
                .executes(ctx -> startSandstorm(ctx.getSource(), 12000)) // Default 10 mins (12000 ticks)
                .then(CommandManager.argument("duration_seconds", IntegerArgumentType.integer(10, 86400))
                    .executes(ctx -> startSandstorm(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "duration_seconds") * 20))
                )
            )
            .then(CommandManager.literal("stop")
                .executes(ctx -> stopSandstorm(ctx.getSource()))
            )
            .then(CommandManager.literal("status")
                .executes(ctx -> getStatus(ctx.getSource()))
            )
        );
    }

    private static int startSandstorm(ServerCommandSource source, int ticks) {
        ServerWorld world = source.getWorld();
        SandstormData data = SandstormData.getServerState(world);
        data.start(world, ticks);
        source.sendFeedback(() -> Text.literal("§eSandstorm started for " + (ticks / 20) + " seconds!"), true);
        return 1;
    }

    private static int stopSandstorm(ServerCommandSource source) {
        ServerWorld world = source.getWorld();
        SandstormData data = SandstormData.getServerState(world);
        data.stop(world);
        source.sendFeedback(() -> Text.literal("§eSandstorm stopped!"), true);
        return 1;
    }
    
    private static int getStatus(ServerCommandSource source) {
        ServerWorld world = source.getWorld();
        SandstormData data = SandstormData.getServerState(world);
        String status = data.isActive() ? "§aActive" : "§cInactive";
        String intensity = String.format("%.2f", data.getIntensity());
        source.sendFeedback(() -> Text.literal("Sandstorm Status: " + status + " (Intensity: " + intensity + ")"), false);
        return 1;
    }
}
