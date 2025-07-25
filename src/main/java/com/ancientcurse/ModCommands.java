package com.ancientcurse;

import com.ancientcurse.command.AnkhCommand;
import com.ancientcurse.command.CursedEarthCommand;
import com.ancientcurse.command.KhamsinCurseCommand;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

/**
 * Registers custom commands for the mod
 */
public class ModCommands {
    
    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register(ModCommands::register);
    }
    
    private static void register(CommandDispatcher<ServerCommandSource> dispatcher, 
                                CommandRegistryAccess registryAccess,
                                CommandManager.RegistrationEnvironment environment) {
        
        // Register /ra_staff command to give the Staff of Ra
        dispatcher.register(literal("ra_staff")
            .requires(source -> source.hasPermissionLevel(2)) // Requires permission level 2 (ops)
            .executes(context -> {
                // Execute /give command to give the staff
                context.getSource().getServer().getCommandManager()
                    .executeWithPrefix(context.getSource(), "give @p ancientcurse:staff_of_ra");
                
                // Send success message
                context.getSource().sendFeedback(() -> 
                    Text.literal("Given Staff of Ra to " + context.getSource().getName()), false);
                
                return 1;
            })
        );
        
        // Register the Khamsin Curse command
        KhamsinCurseCommand.register(dispatcher, registryAccess, environment);
        
        // Register the Ankh command
        AnkhCommand.register(dispatcher, registryAccess, environment);
        
        // Register /ancientcurse (alias /ac) command - this includes all cursed earth controls
        registerAncientCurseCommand(dispatcher);
    }
    
    private static void registerAncientCurseCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Main command: /ancientcurse with all subcommands
        var ancientCurseCommand = literal("ancientcurse")
            .requires(source -> source.hasPermissionLevel(2))
            .executes(context -> {
                // Show help by default
                showHelp(context.getSource());
                return 1;
            })
            // Spread control
            .then(literal("spread")
                .then(literal("enable")
                    .executes(context -> {
                        CursedEarthCommand.setSpreadEnabled(true);
                        context.getSource().sendFeedback(() -> 
                            Text.literal("§aCursed Earth spreading enabled"), true);
                        return 1;
                    }))
                .then(literal("disable")
                    .executes(context -> {
                        CursedEarthCommand.setSpreadEnabled(false);
                        context.getSource().sendFeedback(() -> 
                            Text.literal("§cCursed Earth spreading disabled"), true);
                        return 1;
                    }))
                .then(literal("status")
                    .executes(context -> {
                        boolean enabled = CursedEarthCommand.isEnabled();
                        context.getSource().sendFeedback(() -> 
                            Text.literal("Cursed Earth spreading is " + 
                                (enabled ? "§aenabled" : "§cdisabled")), false);
                        return 1;
                    })))
            // Clear command
            .then(literal("clear")
                .then(CommandManager.argument("radius", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 1000))
                    .executes(context -> {
                        return CursedEarthCommand.clearCursedEarthCommand(context);
                    })))
            // Stats command
            .then(literal("stats")
                .executes(context -> {
                    return CursedEarthCommand.showStatsCommand(context);
                }))
            // Prevention commands
            .then(literal("prevention")
                .then(literal("clear")
                    .executes(context -> {
                        return CursedEarthCommand.clearPreventionCommand(context);
                    })
                    .then(CommandManager.argument("radius", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 250))
                        .executes(context -> {
                            return CursedEarthCommand.clearPreventionCommandWithRadius(context);
                        }))))
            // Help command
            .then(literal("help")
                .executes(context -> {
                    showHelp(context.getSource());
                    return 1;
                }));
        
        // Register main command
        dispatcher.register(ancientCurseCommand);
        
        // Alias: /ac
        dispatcher.register(literal("ac")
            .requires(source -> source.hasPermissionLevel(2))
            .redirect(dispatcher.getRoot().getChild("ancientcurse"))
        );
    }
    
    private static void showHelp(ServerCommandSource source) {
        source.sendFeedback(() -> 
            Text.literal("§6=== Ancient Curse Commands ==="), false);
        source.sendFeedback(() -> 
            Text.literal("§e/ac spread enable/disable §7- Toggle cursed earth spreading"), false);
        source.sendFeedback(() -> 
            Text.literal("§e/ac spread status §7- Check spreading status"), false);
        source.sendFeedback(() -> 
            Text.literal("§e/ac clear <radius> §7- Remove cursed earth in radius"), false);
        source.sendFeedback(() -> 
            Text.literal("§e/ac stats §7- Show cursed earth statistics"), false);
        source.sendFeedback(() -> 
            Text.literal("§e/ac prevention clear [radius] §7- Clear protection (default 50, max 250)"), false);
        source.sendFeedback(() -> 
            Text.literal("§e/khamsin <player> <stage> §7- Apply Khamsin curse"), false);
        source.sendFeedback(() -> 
            Text.literal("§e/ankh heal <player> §7- Fully heal player's ankh"), false);
        source.sendFeedback(() -> 
            Text.literal("§e/ankh <1-100> <player> §7- Set player's ankh value"), false);
    }
} 