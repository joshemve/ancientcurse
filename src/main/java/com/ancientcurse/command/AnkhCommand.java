package com.ancientcurse.command;

import com.ancientcurse.player.AnkhDataManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Collection;

/**
 * Command to manage player ankh values
 * Usage: 
 * - /ankh heal <player> - Fully heals player's ankh to 100
 * - /ankh <1-100> <player> - Sets player's ankh to specified value
 */
public class AnkhCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("ankh")
            .requires(source -> source.hasPermissionLevel(2)) // Require permission level 2 (admin/op)
            // /ankh heal <player>
            .then(CommandManager.literal("heal")
                .then(CommandManager.argument("targets", EntityArgumentType.players())
                    .executes(context -> healAnkh(
                        context,
                        EntityArgumentType.getPlayers(context, "targets")
                    ))
                )
            )
            // /ankh <1-100> <player>
            .then(CommandManager.argument("value", IntegerArgumentType.integer(1, 100))
                .then(CommandManager.argument("targets", EntityArgumentType.players())
                    .executes(context -> setAnkh(
                        context,
                        IntegerArgumentType.getInteger(context, "value"),
                        EntityArgumentType.getPlayers(context, "targets")
                    ))
                )
            )
        );
    }
    
    private static int healAnkh(CommandContext<ServerCommandSource> context, Collection<ServerPlayerEntity> targets) throws CommandSyntaxException {
        int count = 0;
        
        for (ServerPlayerEntity player : targets) {
            AnkhDataManager.setAnkhValue(player, 100);
            count++;
            
            // Send feedback to the player being healed
            player.sendMessage(
                Text.literal("Your ankh has been fully restored!")
                    .formatted(Formatting.GREEN),
                false
            );
        }
        
        // Send feedback to the command executor
        if (count == 1) {
            context.getSource().sendFeedback(() -> 
                Text.literal("Fully healed ankh for ")
                    .append(targets.iterator().next().getDisplayName())
                    .formatted(Formatting.GREEN), 
                true
            );
        } else {
            final int playerCount = count;
            context.getSource().sendFeedback(() -> 
                Text.literal("Fully healed ankh for " + playerCount + " players")
                    .formatted(Formatting.GREEN), 
                true
            );
        }
        
        return count;
    }
    
    private static int setAnkh(CommandContext<ServerCommandSource> context, int value, Collection<ServerPlayerEntity> targets) throws CommandSyntaxException {
        int count = 0;
        
        for (ServerPlayerEntity player : targets) {
            int previousValue = AnkhDataManager.getAnkhValue(player);
            AnkhDataManager.setAnkhValue(player, value);
            count++;
            
            // Send feedback to the player whose ankh was changed
            if (value > previousValue) {
                player.sendMessage(
                    Text.literal("Your ankh has been restored to " + value + "!")
                        .formatted(Formatting.GREEN),
                    false
                );
            } else if (value < previousValue) {
                player.sendMessage(
                    Text.literal("Your ankh has been reduced to " + value + "!")
                        .formatted(Formatting.RED),
                    false
                );
            } else {
                player.sendMessage(
                    Text.literal("Your ankh remains at " + value)
                        .formatted(Formatting.YELLOW),
                    false
                );
            }
        }
        
        // Send feedback to the command executor
        if (count == 1) {
            context.getSource().sendFeedback(() -> 
                Text.literal("Set ankh to " + value + " for ")
                    .append(targets.iterator().next().getDisplayName())
                    .formatted(Formatting.GREEN), 
                true
            );
        } else {
            final int playerCount = count;
            context.getSource().sendFeedback(() -> 
                Text.literal("Set ankh to " + value + " for " + playerCount + " players")
                    .formatted(Formatting.GREEN), 
                true
            );
        }
        
        return count;
    }
}