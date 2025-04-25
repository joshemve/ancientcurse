package com.ancientcurse;

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
    }
} 