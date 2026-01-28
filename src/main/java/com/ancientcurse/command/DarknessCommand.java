package com.ancientcurse.command;

import com.ancientcurse.system.DarknessConfig;
import com.ancientcurse.system.DarknessData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Command for controlling the Darkness plague event (9th Plague of Egypt).
 *
 * "Then the LORD said to Moses, 'Stretch out your hand toward the sky so that
 * darkness spreads over Egypt—darkness that can be felt.'"
 * - Exodus 10:21
 *
 * Usage:
 * - /darkness start [days] - Start darkness for specified days (default: 3)
 * - /darkness stop - Stop darkness (with fadeout)
 * - /darkness forcestop - Stop darkness immediately
 * - /darkness status - Show current darkness status
 * - /darkness config <setting> <value> - Configure darkness settings
 * - /darkness reload - Reload config from file
 * - /darkness debug - Show debug information
 */
public class DarknessCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                 CommandRegistryAccess registryAccess,
                                 CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("darkness")
            .requires(source -> source.hasPermissionLevel(2))

            // /darkness start [days]
            .then(CommandManager.literal("start")
                .executes(context -> startDarkness(context, -1)) // Use default from config
                .then(CommandManager.argument("days", IntegerArgumentType.integer(1, 30))
                    .executes(context -> startDarkness(context,
                        IntegerArgumentType.getInteger(context, "days")))))

            // /darkness startseconds [seconds] - For testing with shorter durations
            .then(CommandManager.literal("startseconds")
                .then(CommandManager.argument("seconds", IntegerArgumentType.integer(1, 86400))
                    .executes(context -> startDarknessSeconds(context,
                        IntegerArgumentType.getInteger(context, "seconds")))))

            // /darkness stop
            .then(CommandManager.literal("stop")
                .executes(DarknessCommand::stopDarkness))

            // /darkness forcestop
            .then(CommandManager.literal("forcestop")
                .executes(DarknessCommand::forceStopDarkness))

            // /darkness status
            .then(CommandManager.literal("status")
                .executes(DarknessCommand::showStatus))

            // /darkness config
            .then(CommandManager.literal("config")
                // /darkness config duration <days>
                .then(CommandManager.literal("duration")
                    .then(CommandManager.argument("days", IntegerArgumentType.integer(1, 30))
                        .executes(context -> setDefaultDuration(context,
                            IntegerArgumentType.getInteger(context, "days")))))
                // /darkness config fadein <speed>
                .then(CommandManager.literal("fadein")
                    .then(CommandManager.argument("speed", FloatArgumentType.floatArg(0.1f, 10.0f))
                        .executes(context -> setFadeInSpeed(context,
                            FloatArgumentType.getFloat(context, "speed")))))
                // /darkness config fadeout <speed>
                .then(CommandManager.literal("fadeout")
                    .then(CommandManager.argument("speed", FloatArgumentType.floatArg(0.1f, 10.0f))
                        .executes(context -> setFadeOutSpeed(context,
                            FloatArgumentType.getFloat(context, "speed")))))
                // /darkness config maxintensity <value>
                .then(CommandManager.literal("maxintensity")
                    .then(CommandManager.argument("value", FloatArgumentType.floatArg(0.5f, 1.0f))
                        .executes(context -> setMaxIntensity(context,
                            FloatArgumentType.getFloat(context, "value")))))
                // /darkness config timer <on|off>
                .then(CommandManager.literal("timer")
                    .then(CommandManager.literal("on")
                        .executes(context -> setShowTimer(context, true)))
                    .then(CommandManager.literal("off")
                        .executes(context -> setShowTimer(context, false))))
                // /darkness config flashes <on|off>
                .then(CommandManager.literal("flashes")
                    .then(CommandManager.literal("on")
                        .executes(context -> setLightningFlashes(context, true)))
                    .then(CommandManager.literal("off")
                        .executes(context -> setLightningFlashes(context, false))))
                // /darkness config mobspawning <on|off>
                .then(CommandManager.literal("mobspawning")
                    .then(CommandManager.literal("on")
                        .executes(context -> setMobSpawning(context, true)))
                    .then(CommandManager.literal("off")
                        .executes(context -> setMobSpawning(context, false))))
                // /darkness config dimlights <on|off>
                .then(CommandManager.literal("dimlights")
                    .then(CommandManager.literal("on")
                        .executes(context -> setDimLights(context, true)))
                    .then(CommandManager.literal("off")
                        .executes(context -> setDimLights(context, false))))
                // /darkness config ambient <on|off>
                .then(CommandManager.literal("ambient")
                    .then(CommandManager.literal("on")
                        .executes(context -> setAmbientSounds(context, true)))
                    .then(CommandManager.literal("off")
                        .executes(context -> setAmbientSounds(context, false))))
                // /darkness config save
                .then(CommandManager.literal("save")
                    .executes(DarknessCommand::saveConfig))
                // /darkness config show
                .then(CommandManager.literal("show")
                    .executes(DarknessCommand::showConfig)))

            // /darkness reload
            .then(CommandManager.literal("reload")
                .executes(DarknessCommand::reloadConfig))

            // /darkness debug
            .then(CommandManager.literal("debug")
                .executes(DarknessCommand::showDebug))
        );
    }

    private static int startDarkness(CommandContext<ServerCommandSource> context, int days) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        DarknessConfig config = DarknessConfig.get();

        DarknessData data = DarknessData.getServerState(world);

        if (days <= 0) {
            // Use default from config
            days = config.getDefaultDurationDays();
        }

        // Clamp to config limits
        days = Math.max(config.getMinDurationDays(), Math.min(config.getMaxDurationDays(), days));

        data.startDays(world, days);

        final int finalDays = days;
        source.sendFeedback(() -> Text.literal("Darkness plague started for " + finalDays + " day" +
            (finalDays != 1 ? "s" : "")).formatted(Formatting.DARK_GRAY), true);
        return 1;
    }

    private static int startDarknessSeconds(CommandContext<ServerCommandSource> context, int seconds) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();

        DarknessData data = DarknessData.getServerState(world);
        data.start(world, seconds * 20); // Convert to ticks

        source.sendFeedback(() -> Text.literal("Darkness plague started for " + seconds + " seconds")
            .formatted(Formatting.DARK_GRAY), true);
        return 1;
    }

    private static int stopDarkness(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();

        DarknessData data = DarknessData.getServerState(world);
        if (!data.isActive() && !data.hasEffect()) {
            source.sendError(Text.literal("Darkness plague is not active"));
            return 0;
        }

        data.stop(world);
        source.sendFeedback(() -> Text.literal("Darkness plague stopped (fading out)")
            .formatted(Formatting.GRAY), true);
        return 1;
    }

    private static int forceStopDarkness(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();

        DarknessData data = DarknessData.getServerState(world);
        data.forceStop(world);

        source.sendFeedback(() -> Text.literal("Darkness plague force stopped")
            .formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int showStatus(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();

        DarknessData data = DarknessData.getServerState(world);
        DarknessConfig config = DarknessConfig.get();

        Text status = Text.literal("=== Darkness Plague Status ===\n").formatted(Formatting.DARK_GRAY)
            .append(Text.literal("Active: ").formatted(Formatting.GRAY))
            .append(Text.literal(data.isActive() ? "Yes" : "No")
                .formatted(data.isActive() ? Formatting.DARK_RED : Formatting.GREEN))
            .append(Text.literal("\n"))
            .append(Text.literal("Intensity: ").formatted(Formatting.GRAY))
            .append(Text.literal(String.format("%.1f%%", data.getIntensity() * 100))
                .formatted(Formatting.DARK_PURPLE))
            .append(Text.literal("\n"))
            .append(Text.literal("Duration: ").formatted(Formatting.GRAY))
            .append(Text.literal(formatDuration(data.getRemainingDuration()))
                .formatted(Formatting.WHITE))
            .append(Text.literal("\n"))
            .append(Text.literal("Days Remaining: ").formatted(Formatting.GRAY))
            .append(Text.literal(data.getRemainingDays() >= 0 ?
                String.valueOf(data.getRemainingDays()) : "Infinite")
                .formatted(Formatting.YELLOW))
            .append(Text.literal("\n"))
            .append(Text.literal("Lightning Flashes: ").formatted(Formatting.GRAY))
            .append(Text.literal(config.enableLightningFlashes() ? "Enabled" : "Disabled")
                .formatted(config.enableLightningFlashes() ? Formatting.YELLOW : Formatting.DARK_GRAY))
            .append(Text.literal("\n"))
            .append(Text.literal("Is Flashing: ").formatted(Formatting.GRAY))
            .append(Text.literal(data.isFlashing() ? "Yes" : "No")
                .formatted(data.isFlashing() ? Formatting.WHITE : Formatting.DARK_GRAY));

        source.sendFeedback(() -> status, false);
        return 1;
    }

    private static int setDefaultDuration(CommandContext<ServerCommandSource> context, int days) {
        DarknessConfig config = DarknessConfig.get();
        config.setDefaultDurationDays(days);

        context.getSource().sendFeedback(() ->
            Text.literal("Default darkness duration set to " + days + " days").formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int setFadeInSpeed(CommandContext<ServerCommandSource> context, float speed) {
        DarknessConfig config = DarknessConfig.get();
        config.setIntensityFadeInSpeed(speed);

        context.getSource().sendFeedback(() ->
            Text.literal("Fade in speed set to " + speed + "x").formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int setFadeOutSpeed(CommandContext<ServerCommandSource> context, float speed) {
        DarknessConfig config = DarknessConfig.get();
        config.setIntensityFadeOutSpeed(speed);

        context.getSource().sendFeedback(() ->
            Text.literal("Fade out speed set to " + speed + "x").formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int setMaxIntensity(CommandContext<ServerCommandSource> context, float value) {
        DarknessConfig config = DarknessConfig.get();
        config.setMaxIntensity(value);

        context.getSource().sendFeedback(() ->
            Text.literal("Max darkness intensity set to " + (int)(value * 100) + "%").formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int setShowTimer(CommandContext<ServerCommandSource> context, boolean show) {
        DarknessConfig config = DarknessConfig.get();
        config.setShowDarknessTimer(show);

        context.getSource().sendFeedback(() ->
            Text.literal("Darkness timer " + (show ? "enabled" : "disabled")).formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int setLightningFlashes(CommandContext<ServerCommandSource> context, boolean enabled) {
        DarknessConfig config = DarknessConfig.get();
        config.setEnableLightningFlashes(enabled);

        context.getSource().sendFeedback(() ->
            Text.literal("Lightning flashes " + (enabled ? "enabled" : "disabled")).formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int setMobSpawning(CommandContext<ServerCommandSource> context, boolean enabled) {
        DarknessConfig config = DarknessConfig.get();
        config.setIncreasedMobSpawning(enabled);

        context.getSource().sendFeedback(() ->
            Text.literal("Increased mob spawning " + (enabled ? "enabled" : "disabled")).formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int setDimLights(CommandContext<ServerCommandSource> context, boolean enabled) {
        DarknessConfig config = DarknessConfig.get();
        config.setDimLightSources(enabled);

        context.getSource().sendFeedback(() ->
            Text.literal("Light source dimming " + (enabled ? "enabled" : "disabled")).formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int setAmbientSounds(CommandContext<ServerCommandSource> context, boolean enabled) {
        DarknessConfig config = DarknessConfig.get();
        config.setPlayAmbientSounds(enabled);

        context.getSource().sendFeedback(() ->
            Text.literal("Ambient sounds " + (enabled ? "enabled" : "disabled")).formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int saveConfig(CommandContext<ServerCommandSource> context) {
        DarknessConfig.get().save();

        context.getSource().sendFeedback(() ->
            Text.literal("Darkness config saved to file").formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int showConfig(CommandContext<ServerCommandSource> context) {
        DarknessConfig config = DarknessConfig.get();

        Text configText = Text.literal("=== Darkness Config ===\n").formatted(Formatting.DARK_GRAY)
            .append(Text.literal("Duration:\n").formatted(Formatting.DARK_PURPLE))
            .append(Text.literal("  Default: ").formatted(Formatting.GRAY))
            .append(Text.literal(config.getDefaultDurationDays() + " days\n").formatted(Formatting.WHITE))
            .append(Text.literal("  Min: ").formatted(Formatting.GRAY))
            .append(Text.literal(config.getMinDurationDays() + " days\n").formatted(Formatting.WHITE))
            .append(Text.literal("  Max: ").formatted(Formatting.GRAY))
            .append(Text.literal(config.getMaxDurationDays() + " days\n").formatted(Formatting.WHITE))
            .append(Text.literal("Intensity:\n").formatted(Formatting.DARK_PURPLE))
            .append(Text.literal("  Max: ").formatted(Formatting.GRAY))
            .append(Text.literal((int)(config.getMaxIntensity() * 100) + "%\n").formatted(Formatting.WHITE))
            .append(Text.literal("  Fade In: ").formatted(Formatting.GRAY))
            .append(Text.literal(config.getIntensityFadeInSpeed() + "x\n").formatted(Formatting.WHITE))
            .append(Text.literal("  Fade Out: ").formatted(Formatting.GRAY))
            .append(Text.literal(config.getIntensityFadeOutSpeed() + "x\n").formatted(Formatting.WHITE))
            .append(Text.literal("Effects:\n").formatted(Formatting.DARK_PURPLE))
            .append(Text.literal("  Lightning Flashes: ").formatted(Formatting.GRAY))
            .append(Text.literal(config.enableLightningFlashes() + "\n").formatted(Formatting.WHITE))
            .append(Text.literal("  Dim Light Sources: ").formatted(Formatting.GRAY))
            .append(Text.literal(config.dimLightSources() + "\n").formatted(Formatting.WHITE))
            .append(Text.literal("  Increased Mobs: ").formatted(Formatting.GRAY))
            .append(Text.literal(config.increasedMobSpawning() + "\n").formatted(Formatting.WHITE))
            .append(Text.literal("  Ambient Sounds: ").formatted(Formatting.GRAY))
            .append(Text.literal(String.valueOf(config.playAmbientSounds())).formatted(Formatting.WHITE));

        context.getSource().sendFeedback(() -> configText, false);
        return 1;
    }

    private static int reloadConfig(CommandContext<ServerCommandSource> context) {
        DarknessConfig.reload();

        context.getSource().sendFeedback(() ->
            Text.literal("Darkness config reloaded from file").formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int showDebug(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();

        DarknessData data = DarknessData.getServerState(world);
        DarknessConfig config = DarknessConfig.get();

        Text debug = Text.literal("=== Darkness Debug ===\n").formatted(Formatting.AQUA)
            .append(Text.literal("Raw Intensity: ").formatted(Formatting.GRAY))
            .append(Text.literal(String.format("%.4f", data.getIntensity()))
                .formatted(Formatting.WHITE))
            .append(Text.literal("\n"))
            .append(Text.literal("Effective Darkness: ").formatted(Formatting.GRAY))
            .append(Text.literal(String.format("%.4f", data.getEffectiveDarkness()))
                .formatted(Formatting.WHITE))
            .append(Text.literal("\n"))
            .append(Text.literal("Flash Intensity: ").formatted(Formatting.GRAY))
            .append(Text.literal(String.format("%.4f", data.getFlashIntensity()))
                .formatted(Formatting.WHITE))
            .append(Text.literal("\n"))
            .append(Text.literal("Ambient Light Level: ").formatted(Formatting.GRAY))
            .append(Text.literal(String.format("%.4f", data.getAmbientLightLevel()))
                .formatted(Formatting.WHITE))
            .append(Text.literal("\n"))
            .append(Text.literal("Duration Ticks: ").formatted(Formatting.GRAY))
            .append(Text.literal(String.valueOf(data.getRemainingDuration()))
                .formatted(Formatting.WHITE))
            .append(Text.literal("\n"))
            .append(Text.literal("Elapsed Time: ").formatted(Formatting.GRAY))
            .append(Text.literal(data.getElapsedTime(world) + " ticks")
                .formatted(Formatting.WHITE))
            .append(Text.literal("\n"))
            .append(Text.literal("Has Effect: ").formatted(Formatting.GRAY))
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

        int totalSeconds = ticks / 20;
        int days = totalSeconds / 86400; // 24 * 60 * 60 (but MC days are 24000 ticks = 20 min)
        int mcDays = ticks / 24000; // Minecraft days

        if (mcDays > 0) {
            int remainingTicks = ticks % 24000;
            int hours = (remainingTicks / 20) / 60;
            return String.format("%d MC day%s %dh", mcDays, mcDays != 1 ? "s" : "", hours);
        }

        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
}
