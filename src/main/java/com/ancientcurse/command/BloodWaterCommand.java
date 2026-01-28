package com.ancientcurse.command;

import com.ancientcurse.system.BloodWaterConfig;
import com.ancientcurse.system.BloodWaterData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
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
 * Command to control the Blood Water plague event (1st Plague of Egypt).
 *
 * "The LORD said to Moses, 'Tell Aaron, Take your staff and stretch out your hand
 * over the waters of Egypt... and they will turn to blood.'" - Exodus 7:19
 *
 * Usage:
 *   /bloodwater start [duration_seconds] - Start the event (default: infinite)
 *   /bloodwater stop - Stop the event (will fade out)
 *   /bloodwater forcestop - Stop immediately without fadeout
 *   /bloodwater status - Check current status
 *   /bloodwater config <setting> <value> - Configure curse settings
 *   /bloodwater reload - Reload config from file
 */
public class BloodWaterCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess,
                                CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("bloodwater")
            .requires(source -> source.hasPermissionLevel(2))

            // /bloodwater start [duration_seconds]
            .then(CommandManager.literal("start")
                .executes(ctx -> startBloodWater(ctx.getSource(), -1))
                .then(CommandManager.argument("duration_seconds", IntegerArgumentType.integer(10, 86400))
                    .executes(ctx -> startBloodWater(
                        ctx.getSource(),
                        IntegerArgumentType.getInteger(ctx, "duration_seconds") * 20
                    ))
                )
            )

            // /bloodwater stop
            .then(CommandManager.literal("stop")
                .executes(ctx -> stopBloodWater(ctx.getSource()))
            )

            // /bloodwater forcestop
            .then(CommandManager.literal("forcestop")
                .executes(ctx -> forceStopBloodWater(ctx.getSource()))
            )

            // /bloodwater status
            .then(CommandManager.literal("status")
                .executes(ctx -> getStatus(ctx.getSource()))
            )

            // /bloodwater config
            .then(CommandManager.literal("config")
                // /bloodwater config curse <on|off>
                .then(CommandManager.literal("curse")
                    .then(CommandManager.literal("on")
                        .executes(ctx -> setCurseEnabled(ctx, true)))
                    .then(CommandManager.literal("off")
                        .executes(ctx -> setCurseEnabled(ctx, false))))
                // /bloodwater config damage <amount>
                .then(CommandManager.literal("damage")
                    .then(CommandManager.argument("amount", FloatArgumentType.floatArg(0.0f, 10.0f))
                        .executes(ctx -> setDamage(ctx,
                            FloatArgumentType.getFloat(ctx, "amount")))))
                // /bloodwater config interval <ticks>
                .then(CommandManager.literal("interval")
                    .then(CommandManager.argument("ticks", IntegerArgumentType.integer(1, 200))
                        .executes(ctx -> setDamageInterval(ctx,
                            IntegerArgumentType.getInteger(ctx, "ticks")))))
                // /bloodwater config linger <ticks>
                .then(CommandManager.literal("linger")
                    .then(CommandManager.argument("ticks", IntegerArgumentType.integer(0, 600))
                        .executes(ctx -> setLingerDuration(ctx,
                            IntegerArgumentType.getInteger(ctx, "ticks")))))
                // /bloodwater config nausea <on|off>
                .then(CommandManager.literal("nausea")
                    .then(CommandManager.literal("on")
                        .executes(ctx -> setNausea(ctx, true)))
                    .then(CommandManager.literal("off")
                        .executes(ctx -> setNausea(ctx, false))))
                // /bloodwater config poison <on|off>
                .then(CommandManager.literal("poison")
                    .then(CommandManager.literal("on")
                        .executes(ctx -> setPoison(ctx, true)))
                    .then(CommandManager.literal("off")
                        .executes(ctx -> setPoison(ctx, false))))
                // /bloodwater config hunger <on|off>
                .then(CommandManager.literal("hunger")
                    .then(CommandManager.literal("on")
                        .executes(ctx -> setHunger(ctx, true)))
                    .then(CommandManager.literal("off")
                        .executes(ctx -> setHunger(ctx, false))))
                // /bloodwater config overlay <on|off>
                .then(CommandManager.literal("overlay")
                    .then(CommandManager.literal("on")
                        .executes(ctx -> setOverlay(ctx, true)))
                    .then(CommandManager.literal("off")
                        .executes(ctx -> setOverlay(ctx, false))))
                // /bloodwater config flash <on|off>
                .then(CommandManager.literal("flash")
                    .then(CommandManager.literal("on")
                        .executes(ctx -> setDamageFlash(ctx, true)))
                    .then(CommandManager.literal("off")
                        .executes(ctx -> setDamageFlash(ctx, false))))
                // /bloodwater config save
                .then(CommandManager.literal("save")
                    .executes(BloodWaterCommand::saveConfig))
                // /bloodwater config show
                .then(CommandManager.literal("show")
                    .executes(BloodWaterCommand::showConfig)))

            // /bloodwater reload
            .then(CommandManager.literal("reload")
                .executes(BloodWaterCommand::reloadConfig))

            // /bloodwater debug
            .then(CommandManager.literal("debug")
                .executes(BloodWaterCommand::showDebug))
        );
    }

    private static int startBloodWater(ServerCommandSource source, int ticks) {
        ServerWorld world = source.getWorld();
        BloodWaterData data = BloodWaterData.getServerState(world);
        data.start(world, ticks);

        String durationText = ticks < 0 ? "indefinitely" : "for " + (ticks / 20) + " seconds";
        source.sendFeedback(() -> Text.literal("The Nile runs red with blood... (" + durationText + ")")
            .formatted(Formatting.DARK_RED), true);
        return 1;
    }

    private static int stopBloodWater(ServerCommandSource source) {
        ServerWorld world = source.getWorld();
        BloodWaterData data = BloodWaterData.getServerState(world);

        if (!data.isActive() && !data.hasEffect()) {
            source.sendError(Text.literal("Blood Water plague is not active"));
            return 0;
        }

        data.stop(world);
        source.sendFeedback(() -> Text.literal("The waters begin to clear...")
            .formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int forceStopBloodWater(ServerCommandSource source) {
        ServerWorld world = source.getWorld();
        BloodWaterData data = BloodWaterData.getServerState(world);
        data.forceStop(world);

        source.sendFeedback(() -> Text.literal("Blood Water plague force stopped")
            .formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int getStatus(ServerCommandSource source) {
        ServerWorld world = source.getWorld();
        BloodWaterData data = BloodWaterData.getServerState(world);
        BloodWaterConfig config = BloodWaterConfig.get();

        Text status = Text.literal("=== Blood Water Status ===\n").formatted(Formatting.DARK_RED)
            .append(Text.literal("Active: ").formatted(Formatting.GRAY))
            .append(Text.literal(data.isActive() ? "Yes" : "No")
                .formatted(data.isActive() ? Formatting.RED : Formatting.GREEN))
            .append(Text.literal("\n"))
            .append(Text.literal("Intensity: ").formatted(Formatting.GRAY))
            .append(Text.literal(String.format("%.1f%%", data.getIntensity() * 100))
                .formatted(Formatting.WHITE))
            .append(Text.literal("\n"))
            .append(Text.literal("Duration: ").formatted(Formatting.GRAY))
            .append(Text.literal(formatDuration(data.getRemainingDuration()))
                .formatted(Formatting.WHITE))
            .append(Text.literal("\n"))
            .append(Text.literal("Cursed Players: ").formatted(Formatting.GRAY))
            .append(Text.literal(String.valueOf(data.getCursedPlayerCount()))
                .formatted(Formatting.YELLOW))
            .append(Text.literal("\n"))
            .append(Text.literal("Curse Enabled: ").formatted(Formatting.GRAY))
            .append(Text.literal(config.isCurseEnabled() ? "Yes" : "No")
                .formatted(config.isCurseEnabled() ? Formatting.RED : Formatting.DARK_GRAY));

        source.sendFeedback(() -> status, false);
        return 1;
    }

    // === Config Setters ===

    private static int setCurseEnabled(CommandContext<ServerCommandSource> ctx, boolean enabled) {
        BloodWaterConfig.get().setCurseEnabled(enabled);
        ctx.getSource().sendFeedback(() ->
            Text.literal("Blood curse " + (enabled ? "enabled" : "disabled"))
                .formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int setDamage(CommandContext<ServerCommandSource> ctx, float damage) {
        BloodWaterConfig.get().setDamagePerTick(damage);
        ctx.getSource().sendFeedback(() ->
            Text.literal("Damage per tick set to " + damage)
                .formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int setDamageInterval(CommandContext<ServerCommandSource> ctx, int interval) {
        BloodWaterConfig.get().setDamageInterval(interval);
        ctx.getSource().sendFeedback(() ->
            Text.literal("Damage interval set to " + interval + " ticks")
                .formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int setLingerDuration(CommandContext<ServerCommandSource> ctx, int duration) {
        BloodWaterConfig.get().setCurseLingerDuration(duration);
        ctx.getSource().sendFeedback(() ->
            Text.literal("Curse linger duration set to " + duration + " ticks (" + (duration / 20.0f) + "s)")
                .formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int setNausea(CommandContext<ServerCommandSource> ctx, boolean enabled) {
        BloodWaterConfig.get().setApplyNausea(enabled);
        ctx.getSource().sendFeedback(() ->
            Text.literal("Nausea effect " + (enabled ? "enabled" : "disabled"))
                .formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int setPoison(CommandContext<ServerCommandSource> ctx, boolean enabled) {
        BloodWaterConfig.get().setApplyPoison(enabled);
        ctx.getSource().sendFeedback(() ->
            Text.literal("Poison effect " + (enabled ? "enabled" : "disabled"))
                .formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int setHunger(CommandContext<ServerCommandSource> ctx, boolean enabled) {
        BloodWaterConfig.get().setApplyHunger(enabled);
        ctx.getSource().sendFeedback(() ->
            Text.literal("Hunger effect " + (enabled ? "enabled" : "disabled"))
                .formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int setOverlay(CommandContext<ServerCommandSource> ctx, boolean enabled) {
        BloodWaterConfig.get().setShowCurseOverlay(enabled);
        ctx.getSource().sendFeedback(() ->
            Text.literal("Curse overlay " + (enabled ? "enabled" : "disabled"))
                .formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int setDamageFlash(CommandContext<ServerCommandSource> ctx, boolean enabled) {
        BloodWaterConfig.get().setShowDamageFlash(enabled);
        ctx.getSource().sendFeedback(() ->
            Text.literal("Damage flash " + (enabled ? "enabled" : "disabled"))
                .formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int saveConfig(CommandContext<ServerCommandSource> ctx) {
        BloodWaterConfig.get().save();
        ctx.getSource().sendFeedback(() ->
            Text.literal("Blood Water config saved to file")
                .formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int showConfig(CommandContext<ServerCommandSource> ctx) {
        BloodWaterConfig config = BloodWaterConfig.get();

        Text configText = Text.literal("=== Blood Water Config ===\n").formatted(Formatting.DARK_RED)
            .append(Text.literal("Curse:\n").formatted(Formatting.RED))
            .append(Text.literal("  Enabled: ").formatted(Formatting.GRAY))
            .append(Text.literal(config.isCurseEnabled() + "\n").formatted(Formatting.WHITE))
            .append(Text.literal("  Damage/tick: ").formatted(Formatting.GRAY))
            .append(Text.literal(config.getDamagePerTick() + "\n").formatted(Formatting.WHITE))
            .append(Text.literal("  Damage interval: ").formatted(Formatting.GRAY))
            .append(Text.literal(config.getDamageInterval() + " ticks\n").formatted(Formatting.WHITE))
            .append(Text.literal("  Linger duration: ").formatted(Formatting.GRAY))
            .append(Text.literal(config.getCurseLingerDuration() + " ticks\n").formatted(Formatting.WHITE))
            .append(Text.literal("  Linger damage: ").formatted(Formatting.GRAY))
            .append(Text.literal(config.getLingerDamagePerTick() + "/tick\n").formatted(Formatting.WHITE))
            .append(Text.literal("Effects:\n").formatted(Formatting.RED))
            .append(Text.literal("  Nausea: ").formatted(Formatting.GRAY))
            .append(Text.literal(config.isApplyNausea() + "\n").formatted(Formatting.WHITE))
            .append(Text.literal("  Poison: ").formatted(Formatting.GRAY))
            .append(Text.literal(config.isApplyPoison() + "\n").formatted(Formatting.WHITE))
            .append(Text.literal("  Hunger: ").formatted(Formatting.GRAY))
            .append(Text.literal(config.isApplyHunger() + "\n").formatted(Formatting.WHITE))
            .append(Text.literal("Visual:\n").formatted(Formatting.RED))
            .append(Text.literal("  Curse overlay: ").formatted(Formatting.GRAY))
            .append(Text.literal(config.isShowCurseOverlay() + "\n").formatted(Formatting.WHITE))
            .append(Text.literal("  Damage flash: ").formatted(Formatting.GRAY))
            .append(Text.literal(String.valueOf(config.isShowDamageFlash())).formatted(Formatting.WHITE));

        ctx.getSource().sendFeedback(() -> configText, false);
        return 1;
    }

    private static int reloadConfig(CommandContext<ServerCommandSource> ctx) {
        BloodWaterConfig.reload();
        ctx.getSource().sendFeedback(() ->
            Text.literal("Blood Water config reloaded from file")
                .formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int showDebug(CommandContext<ServerCommandSource> ctx) {
        ServerWorld world = ctx.getSource().getWorld();
        BloodWaterData data = BloodWaterData.getServerState(world);
        BloodWaterConfig config = BloodWaterConfig.get();

        Text debug = Text.literal("=== Blood Water Debug ===\n").formatted(Formatting.AQUA)
            .append(Text.literal("Active: ").formatted(Formatting.GRAY))
            .append(Text.literal(String.valueOf(data.isActive())).formatted(Formatting.WHITE))
            .append(Text.literal("\n"))
            .append(Text.literal("Intensity: ").formatted(Formatting.GRAY))
            .append(Text.literal(String.format("%.4f", data.getIntensity())).formatted(Formatting.WHITE))
            .append(Text.literal("\n"))
            .append(Text.literal("Has Effect: ").formatted(Formatting.GRAY))
            .append(Text.literal(String.valueOf(data.hasEffect())).formatted(Formatting.WHITE))
            .append(Text.literal("\n"))
            .append(Text.literal("Duration Ticks: ").formatted(Formatting.GRAY))
            .append(Text.literal(String.valueOf(data.getRemainingDuration())).formatted(Formatting.WHITE))
            .append(Text.literal("\n"))
            .append(Text.literal("Cursed Players: ").formatted(Formatting.GRAY))
            .append(Text.literal(String.valueOf(data.getCursedPlayerCount())).formatted(Formatting.WHITE))
            .append(Text.literal("\n"))
            .append(Text.literal("Min Intensity for Curse: ").formatted(Formatting.GRAY))
            .append(Text.literal(String.format("%.2f", config.getMinimumIntensityForCurse())).formatted(Formatting.WHITE));

        ctx.getSource().sendFeedback(() -> debug, false);
        return 1;
    }

    private static String formatDuration(int ticks) {
        if (ticks < 0) return "Infinite";
        if (ticks == 0) return "Ending";

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
