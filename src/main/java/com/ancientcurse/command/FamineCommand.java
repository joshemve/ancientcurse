package com.ancientcurse.command;

import com.ancientcurse.system.FamineAnimalHandler;
import com.ancientcurse.system.FamineConfig;
import com.ancientcurse.system.FamineCropHandler;
import com.ancientcurse.system.FamineData;
import com.ancientcurse.system.FamineFoodSpoilageHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Command for controlling the Famine event.
 *
 * Usage:
 * - /famine start [duration_seconds] - Start famine (default infinite)
 * - /famine stop - Stop famine (with fadeout)
 * - /famine forcestop - Stop famine immediately
 * - /famine status - Show current famine status
 * - /famine config <setting> <value> - Configure famine settings
 * - /famine clearspoilage - Clear spoilage from all players' food
 * - /famine clearanimals - Clear animal starvation data
 * - /famine debug - Show debug information
 * - /famine reload - Reload config from file
 */
public class FamineCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                 CommandRegistryAccess registryAccess,
                                 CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("famine")
            .requires(source -> source.hasPermissionLevel(2))

            // /famine start [duration]
            .then(CommandManager.literal("start")
                .executes(context -> startFamine(context, -1))
                .then(CommandManager.argument("duration", IntegerArgumentType.integer(1))
                    .executes(context -> startFamine(context,
                        IntegerArgumentType.getInteger(context, "duration")))))

            // /famine stop
            .then(CommandManager.literal("stop")
                .executes(FamineCommand::stopFamine))

            // /famine forcestop
            .then(CommandManager.literal("forcestop")
                .executes(FamineCommand::forceStopFamine))

            // /famine status
            .then(CommandManager.literal("status")
                .executes(FamineCommand::showStatus))

            // /famine config
            .then(CommandManager.literal("config")
                // /famine config radius <value>
                .then(CommandManager.literal("radius")
                    .then(CommandManager.argument("value", IntegerArgumentType.integer(16, 128))
                        .executes(context -> setRadius(context,
                            IntegerArgumentType.getInteger(context, "value")))))
                // /famine config spoilage <ticks>
                .then(CommandManager.literal("spoilage")
                    .then(CommandManager.argument("ticks", IntegerArgumentType.integer(20, 600))
                        .executes(context -> setSpoilageInterval(context,
                            IntegerArgumentType.getInteger(context, "ticks")))))
                // /famine config cropspeed <multiplier>
                .then(CommandManager.literal("cropspeed")
                    .then(CommandManager.argument("multiplier", FloatArgumentType.floatArg(0.1f, 10.0f))
                        .executes(context -> setCropSpeed(context,
                            FloatArgumentType.getFloat(context, "multiplier")))))
                // /famine config spoilagespeed <multiplier>
                .then(CommandManager.literal("spoilagespeed")
                    .then(CommandManager.argument("multiplier", FloatArgumentType.floatArg(0.1f, 10.0f))
                        .executes(context -> setSpoilageSpeed(context,
                            FloatArgumentType.getFloat(context, "multiplier")))))
                // /famine config animalspeed <multiplier>
                .then(CommandManager.literal("animalspeed")
                    .then(CommandManager.argument("multiplier", FloatArgumentType.floatArg(0.1f, 10.0f))
                        .executes(context -> setAnimalSpeed(context,
                            FloatArgumentType.getFloat(context, "multiplier")))))
                // /famine config animals <enabled>
                .then(CommandManager.literal("animals")
                    .then(CommandManager.literal("on")
                        .executes(context -> setAnimalsEnabled(context, true)))
                    .then(CommandManager.literal("off")
                        .executes(context -> setAnimalsEnabled(context, false))))
                // /famine config fadein <speed>
                .then(CommandManager.literal("fadein")
                    .then(CommandManager.argument("speed", FloatArgumentType.floatArg(0.1f, 10.0f))
                        .executes(context -> setFadeInSpeed(context,
                            FloatArgumentType.getFloat(context, "speed")))))
                // /famine config fadeout <speed>
                .then(CommandManager.literal("fadeout")
                    .then(CommandManager.argument("speed", FloatArgumentType.floatArg(0.1f, 10.0f))
                        .executes(context -> setFadeOutSpeed(context,
                            FloatArgumentType.getFloat(context, "speed")))))
                // /famine config save
                .then(CommandManager.literal("save")
                    .executes(FamineCommand::saveConfig))
                // /famine config show
                .then(CommandManager.literal("show")
                    .executes(FamineCommand::showConfig)))

            // /famine clearspoilage
            .then(CommandManager.literal("clearspoilage")
                .executes(FamineCommand::clearSpoilage))

            // /famine clearanimals
            .then(CommandManager.literal("clearanimals")
                .executes(FamineCommand::clearAnimals))

            // /famine reload
            .then(CommandManager.literal("reload")
                .executes(FamineCommand::reloadConfig))

            // /famine debug
            .then(CommandManager.literal("debug")
                .executes(FamineCommand::showDebug))
        );
    }

    private static int startFamine(CommandContext<ServerCommandSource> context, int durationSeconds) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();

        FamineData data = FamineData.getServerState(world);
        int durationTicks = durationSeconds > 0 ? durationSeconds * 20 : -1;
        data.start(world, durationTicks);

        String message;
        if (durationSeconds > 0) {
            message = "Famine started for " + durationSeconds + " seconds";
        } else {
            message = "Famine started indefinitely";
        }

        source.sendFeedback(() -> Text.literal(message).formatted(Formatting.DARK_RED), true);
        return 1;
    }

    private static int stopFamine(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();

        FamineData data = FamineData.getServerState(world);
        if (!data.isActive() && !data.hasEffect()) {
            source.sendError(Text.literal("Famine is not active"));
            return 0;
        }

        data.stop(world);
        source.sendFeedback(() -> Text.literal("Famine stopped (fading out)").formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int forceStopFamine(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();

        FamineData data = FamineData.getServerState(world);
        data.forceStop(world);

        source.sendFeedback(() -> Text.literal("Famine force stopped and cleaned up").formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int showStatus(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();

        FamineData data = FamineData.getServerState(world);
        FamineConfig config = FamineConfig.get();

        Text status = Text.literal("=== Famine Status ===\n").formatted(Formatting.GOLD)
            .append(Text.literal("Active: ").formatted(Formatting.GRAY))
            .append(Text.literal(data.isActive() ? "Yes" : "No")
                .formatted(data.isActive() ? Formatting.RED : Formatting.GREEN))
            .append(Text.literal("\n"))
            .append(Text.literal("Intensity: ").formatted(Formatting.GRAY))
            .append(Text.literal(String.format("%.1f%%", data.getIntensity() * 100))
                .formatted(Formatting.YELLOW))
            .append(Text.literal("\n"))
            .append(Text.literal("Duration: ").formatted(Formatting.GRAY))
            .append(Text.literal(formatDuration(data.getRemainingDuration()))
                .formatted(Formatting.WHITE))
            .append(Text.literal("\n"))
            .append(Text.literal("Crop Death Radius: ").formatted(Formatting.GRAY))
            .append(Text.literal(config.getCropDeathRadius() + " blocks")
                .formatted(Formatting.WHITE))
            .append(Text.literal("\n"))
            .append(Text.literal("Animal Starvation: ").formatted(Formatting.GRAY))
            .append(Text.literal(config.isAnimalStarvationEnabled() ? "Enabled" : "Disabled")
                .formatted(config.isAnimalStarvationEnabled() ? Formatting.YELLOW : Formatting.GRAY))
            .append(Text.literal("\n"))
            .append(Text.literal("Tracked Animals: ").formatted(Formatting.GRAY))
            .append(Text.literal(String.valueOf(FamineAnimalHandler.getTrackedAnimalCount()))
                .formatted(Formatting.WHITE));

        source.sendFeedback(() -> status, false);
        return 1;
    }

    private static int setRadius(CommandContext<ServerCommandSource> context, int radius) {
        FamineConfig config = FamineConfig.get();
        config.setCropDeathRadius(radius);

        context.getSource().sendFeedback(() ->
            Text.literal("Crop death radius set to " + radius + " blocks").formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int setSpoilageInterval(CommandContext<ServerCommandSource> context, int ticks) {
        FamineConfig config = FamineConfig.get();
        config.setSpoilageCheckInterval(ticks);

        context.getSource().sendFeedback(() ->
            Text.literal("Spoilage check interval set to " + ticks + " ticks (" +
                (ticks / 20f) + "s)").formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int setCropSpeed(CommandContext<ServerCommandSource> context, float multiplier) {
        FamineConfig config = FamineConfig.get();
        config.setCropDeathSpeedMultiplier(multiplier);

        context.getSource().sendFeedback(() ->
            Text.literal("Crop death speed multiplier set to " + multiplier + "x").formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int setSpoilageSpeed(CommandContext<ServerCommandSource> context, float multiplier) {
        FamineConfig config = FamineConfig.get();
        config.setSpoilageSpeedMultiplier(multiplier);

        context.getSource().sendFeedback(() ->
            Text.literal("Spoilage speed multiplier set to " + multiplier + "x").formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int setAnimalSpeed(CommandContext<ServerCommandSource> context, float multiplier) {
        FamineConfig config = FamineConfig.get();
        config.setAnimalStarvationSpeedMultiplier(multiplier);

        context.getSource().sendFeedback(() ->
            Text.literal("Animal starvation speed multiplier set to " + multiplier + "x").formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int setAnimalsEnabled(CommandContext<ServerCommandSource> context, boolean enabled) {
        FamineConfig config = FamineConfig.get();
        config.setAnimalStarvationEnabled(enabled);

        if (!enabled) {
            // Clear animal data when disabling
            ServerWorld world = context.getSource().getWorld();
            FamineAnimalHandler.clearAllStarvationEffects(world);
            FamineAnimalHandler.reset();
        }

        context.getSource().sendFeedback(() ->
            Text.literal("Animal starvation " + (enabled ? "enabled" : "disabled")).formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int setFadeInSpeed(CommandContext<ServerCommandSource> context, float speed) {
        FamineConfig config = FamineConfig.get();
        config.setIntensityFadeInSpeed(speed);

        context.getSource().sendFeedback(() ->
            Text.literal("Fade in speed set to " + speed + "x").formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int setFadeOutSpeed(CommandContext<ServerCommandSource> context, float speed) {
        FamineConfig config = FamineConfig.get();
        config.setIntensityFadeOutSpeed(speed);

        context.getSource().sendFeedback(() ->
            Text.literal("Fade out speed set to " + speed + "x").formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int saveConfig(CommandContext<ServerCommandSource> context) {
        FamineConfig.get().save();

        context.getSource().sendFeedback(() ->
            Text.literal("Famine config saved to file").formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int showConfig(CommandContext<ServerCommandSource> context) {
        FamineConfig config = FamineConfig.get();

        Text configText = Text.literal("=== Famine Config ===\n").formatted(Formatting.GOLD)
            .append(Text.literal("Crop Death:\n").formatted(Formatting.YELLOW))
            .append(Text.literal("  Radius: ").formatted(Formatting.GRAY))
            .append(Text.literal(config.getCropDeathRadius() + " blocks\n").formatted(Formatting.WHITE))
            .append(Text.literal("  Speed: ").formatted(Formatting.GRAY))
            .append(Text.literal(config.getCropDeathSpeedMultiplier() + "x\n").formatted(Formatting.WHITE))
            .append(Text.literal("Spoilage:\n").formatted(Formatting.YELLOW))
            .append(Text.literal("  Interval: ").formatted(Formatting.GRAY))
            .append(Text.literal(config.getSpoilageCheckInterval() + " ticks\n").formatted(Formatting.WHITE))
            .append(Text.literal("  Speed: ").formatted(Formatting.GRAY))
            .append(Text.literal(config.getSpoilageSpeedMultiplier() + "x\n").formatted(Formatting.WHITE))
            .append(Text.literal("Animals:\n").formatted(Formatting.YELLOW))
            .append(Text.literal("  Enabled: ").formatted(Formatting.GRAY))
            .append(Text.literal(config.isAnimalStarvationEnabled() + "\n").formatted(Formatting.WHITE))
            .append(Text.literal("  Speed: ").formatted(Formatting.GRAY))
            .append(Text.literal(config.getAnimalStarvationSpeedMultiplier() + "x\n").formatted(Formatting.WHITE))
            .append(Text.literal("Intensity:\n").formatted(Formatting.YELLOW))
            .append(Text.literal("  Fade In: ").formatted(Formatting.GRAY))
            .append(Text.literal(config.getIntensityFadeInSpeed() + "x\n").formatted(Formatting.WHITE))
            .append(Text.literal("  Fade Out: ").formatted(Formatting.GRAY))
            .append(Text.literal(config.getIntensityFadeOutSpeed() + "x").formatted(Formatting.WHITE));

        context.getSource().sendFeedback(() -> configText, false);
        return 1;
    }

    private static int clearSpoilage(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();

        int playerCount = 0;
        for (ServerPlayerEntity player : world.getPlayers()) {
            FamineFoodSpoilageHandler.clearSpoilageTimers(player);
            playerCount++;
        }

        final int count = playerCount;
        source.sendFeedback(() ->
            Text.literal("Cleared spoilage timers for " + count + " players").formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int clearAnimals(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();

        int animalCount = FamineAnimalHandler.getTrackedAnimalCount();
        FamineAnimalHandler.clearAllStarvationEffects(world);
        FamineAnimalHandler.reset();

        source.sendFeedback(() ->
            Text.literal("Cleared starvation data for " + animalCount + " animals").formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int reloadConfig(CommandContext<ServerCommandSource> context) {
        FamineConfig.reload();

        context.getSource().sendFeedback(() ->
            Text.literal("Famine config reloaded from file").formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int showDebug(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();

        FamineData data = FamineData.getServerState(world);
        FamineConfig config = FamineConfig.get();

        Text debug = Text.literal("=== Famine Debug ===\n").formatted(Formatting.AQUA)
            .append(Text.literal("Pending crop deaths: ").formatted(Formatting.GRAY))
            .append(Text.literal(String.valueOf(FamineCropHandler.getPendingDeathCount()))
                .formatted(Formatting.WHITE))
            .append(Text.literal("\n"))
            .append(Text.literal("Crop death chance: ").formatted(Formatting.GRAY))
            .append(Text.literal(String.format("%.6f", data.getCropDeathChance()))
                .formatted(Formatting.WHITE))
            .append(Text.literal("\n"))
            .append(Text.literal("Spoilage chance: ").formatted(Formatting.GRAY))
            .append(Text.literal(String.format("%.6f", data.getSpoilageChance()))
                .formatted(Formatting.WHITE))
            .append(Text.literal("\n"))
            .append(Text.literal("Tracked animals: ").formatted(Formatting.GRAY))
            .append(Text.literal(String.valueOf(FamineAnimalHandler.getTrackedAnimalCount()))
                .formatted(Formatting.WHITE))
            .append(Text.literal("\n"))
            .append(Text.literal("Has effect: ").formatted(Formatting.GRAY))
            .append(Text.literal(data.hasEffect() ? "Yes" : "No")
                .formatted(data.hasEffect() ? Formatting.YELLOW : Formatting.GRAY))
            .append(Text.literal("\n"))
            .append(Text.literal("Config loaded: ").formatted(Formatting.GRAY))
            .append(Text.literal("Yes").formatted(Formatting.GREEN));

        source.sendFeedback(() -> debug, false);
        return 1;
    }

    private static String formatDuration(int ticks) {
        if (ticks < 0) {
            return "Infinite";
        }
        if (ticks == 0) {
            return "Ending";
        }

        int seconds = ticks / 20;
        int minutes = seconds / 60;
        int hours = minutes / 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
}
