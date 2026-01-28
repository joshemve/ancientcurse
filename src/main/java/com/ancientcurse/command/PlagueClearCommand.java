package com.ancientcurse.command;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.system.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Master command to stop all plague events at once.
 *
 * Usage:
 * - /plagueclear - Force stop all active plague events (Famine, Blood Water, Sandstorm)
 * - /plagueclear status - Show status of all plague events
 * - /stopplagues - Alias for /plagueclear
 */
public class PlagueClearCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                 CommandRegistryAccess registryAccess,
                                 CommandManager.RegistrationEnvironment environment) {

        // Main command: /plagueclear
        dispatcher.register(CommandManager.literal("plagueclear")
            .requires(source -> source.hasPermissionLevel(2))
            .executes(PlagueClearCommand::clearAllPlagues)
            .then(CommandManager.literal("status")
                .executes(PlagueClearCommand::showStatus))
        );

        // Alias: /stopplagues
        dispatcher.register(CommandManager.literal("stopplagues")
            .requires(source -> source.hasPermissionLevel(2))
            .executes(PlagueClearCommand::clearAllPlagues)
        );

        // Alias: /clearplagues
        dispatcher.register(CommandManager.literal("clearplagues")
            .requires(source -> source.hasPermissionLevel(2))
            .executes(PlagueClearCommand::clearAllPlagues)
        );
    }

    /**
     * Force stop all plague events
     */
    private static int clearAllPlagues(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();

        int stoppedCount = 0;
        StringBuilder stoppedEvents = new StringBuilder();

        // === Stop Famine ===
        FamineData famineData = FamineData.getServerState(world);
        if (famineData.isActive() || famineData.hasEffect()) {
            famineData.forceStop(world);
            stoppedCount++;
            stoppedEvents.append("Famine, ");
            AncientCurse.LOGGER.info("Plague clear: Famine force stopped");
        }

        // === Stop Blood Water ===
        BloodWaterData bloodWaterData = BloodWaterData.getServerState(world);
        if (bloodWaterData.isActive() || bloodWaterData.hasEffect()) {
            bloodWaterData.forceStop(world);
            stoppedCount++;
            stoppedEvents.append("Blood Water, ");
            AncientCurse.LOGGER.info("Plague clear: Blood Water force stopped");
        }

        // === Stop Sandstorm ===
        SandstormData sandstormData = SandstormData.getServerState(world);
        if (sandstormData.isActive() || sandstormData.hasEffect()) {
            sandstormData.forceStop(world);
            stoppedCount++;
            stoppedEvents.append("Sandstorm, ");
            AncientCurse.LOGGER.info("Plague clear: Sandstorm force stopped");
        }

        // Send feedback
        if (stoppedCount > 0) {
            // Remove trailing comma and space
            String events = stoppedEvents.substring(0, stoppedEvents.length() - 2);
            final String finalEvents = events;
            final int finalCount = stoppedCount;

            source.sendFeedback(() -> Text.literal("Stopped " + finalCount + " plague event(s): " + finalEvents)
                .formatted(Formatting.GREEN), true);
        } else {
            source.sendFeedback(() -> Text.literal("No active plague events to stop")
                .formatted(Formatting.YELLOW), false);
        }

        return stoppedCount > 0 ? 1 : 0;
    }

    /**
     * Show status of all plague events
     */
    private static int showStatus(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();

        FamineData famineData = FamineData.getServerState(world);
        BloodWaterData bloodWaterData = BloodWaterData.getServerState(world);
        SandstormData sandstormData = SandstormData.getServerState(world);

        Text status = Text.literal("=== Plague Events Status ===\n").formatted(Formatting.GOLD)
            // Famine
            .append(Text.literal("Famine: ").formatted(Formatting.DARK_RED))
            .append(Text.literal(getStatusText(famineData.isActive(), famineData.hasEffect()))
                .formatted(getStatusColor(famineData.isActive(), famineData.hasEffect())))
            .append(Text.literal(" (").formatted(Formatting.GRAY))
            .append(Text.literal(String.format("%.0f%%", famineData.getIntensity() * 100)).formatted(Formatting.WHITE))
            .append(Text.literal(")\n").formatted(Formatting.GRAY))

            // Blood Water
            .append(Text.literal("Blood Water: ").formatted(Formatting.DARK_RED))
            .append(Text.literal(getStatusText(bloodWaterData.isActive(), bloodWaterData.hasEffect()))
                .formatted(getStatusColor(bloodWaterData.isActive(), bloodWaterData.hasEffect())))
            .append(Text.literal(" (").formatted(Formatting.GRAY))
            .append(Text.literal(String.format("%.0f%%", bloodWaterData.getIntensity() * 100)).formatted(Formatting.WHITE))
            .append(Text.literal(")\n").formatted(Formatting.GRAY))

            // Sandstorm
            .append(Text.literal("Sandstorm: ").formatted(Formatting.YELLOW))
            .append(Text.literal(getStatusText(sandstormData.isActive(), sandstormData.hasEffect()))
                .formatted(getStatusColor(sandstormData.isActive(), sandstormData.hasEffect())))
            .append(Text.literal(" (").formatted(Formatting.GRAY))
            .append(Text.literal(String.format("%.0f%%", sandstormData.getIntensity() * 100)).formatted(Formatting.WHITE))
            .append(Text.literal(")").formatted(Formatting.GRAY));

        source.sendFeedback(() -> status, false);
        return 1;
    }

    private static String getStatusText(boolean active, boolean hasEffect) {
        if (active) {
            return "ACTIVE";
        } else if (hasEffect) {
            return "FADING";
        } else {
            return "Inactive";
        }
    }

    private static Formatting getStatusColor(boolean active, boolean hasEffect) {
        if (active) {
            return Formatting.RED;
        } else if (hasEffect) {
            return Formatting.YELLOW;
        } else {
            return Formatting.GREEN;
        }
    }
}
