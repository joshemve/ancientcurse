package com.ancientcurse.command;

import com.ancientcurse.effect.KhamsinCurseEffect;
import com.ancientcurse.effect.ModStatusEffects;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;

/**
 * Command to apply the Khamsin Curse to players
 * Usage: /khamsin <players> <stage>
 */
public class KhamsinCurseCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("khamsin")
            .requires(source -> source.hasPermissionLevel(2)) // Require permission level 2 (admin/op)
            .then(CommandManager.argument("targets", EntityArgumentType.players())
                .then(CommandManager.argument("stage", IntegerArgumentType.integer(1, 5))
                    .executes(context -> applyKhamsinCurse(
                        context,
                        EntityArgumentType.getPlayers(context, "targets"),
                        IntegerArgumentType.getInteger(context, "stage")
                    ))
                )
            )
        );
    }
    
    private static int applyKhamsinCurse(CommandContext<ServerCommandSource> context, Collection<ServerPlayerEntity> targets, int stage) throws CommandSyntaxException {
        // Get the appropriate status effect based on the stage
        StatusEffect effect = switch (stage) {
            case 1 -> ModStatusEffects.KHAMSIN_CURSE_STAGE_1;
            case 2 -> ModStatusEffects.KHAMSIN_CURSE_STAGE_2;
            case 3 -> ModStatusEffects.KHAMSIN_CURSE_STAGE_3;
            case 4 -> ModStatusEffects.KHAMSIN_CURSE_STAGE_4;
            case 5 -> ModStatusEffects.KHAMSIN_CURSE_STAGE_5;
            default -> ModStatusEffects.KHAMSIN_CURSE_STAGE_1; // Default to stage 1
        };
        
        // Apply the effect to all target players
        final int[] count = {0}; // Use an array to make it effectively final
        
        for (ServerPlayerEntity player : targets) {
            // Remove any existing Khamsin Curse effects
            player.removeStatusEffect(ModStatusEffects.KHAMSIN_CURSE_STAGE_1);
            player.removeStatusEffect(ModStatusEffects.KHAMSIN_CURSE_STAGE_2);
            player.removeStatusEffect(ModStatusEffects.KHAMSIN_CURSE_STAGE_3);
            player.removeStatusEffect(ModStatusEffects.KHAMSIN_CURSE_STAGE_4);
            player.removeStatusEffect(ModStatusEffects.KHAMSIN_CURSE_STAGE_5);
            
            // Apply the new effect with the specified stage
            player.addStatusEffect(new StatusEffectInstance(
                effect,
                KhamsinCurseEffect.STAGE_DURATION, // 25 minutes
                0, // Amplifier
                false, // Ambient
                true, // Show particles
                true  // Show icon
            ));
            
            count[0]++;
        }
        
        // Store the final count
        final int playersAffected = count[0];
        
        // Send feedback to the command executor
        if (playersAffected == 1) {
            context.getSource().sendFeedback(() -> 
                Text.translatable("commands.khamsin.success.single", stage, targets.iterator().next().getDisplayName()), 
                true);
        } else {
            context.getSource().sendFeedback(() -> 
                Text.translatable("commands.khamsin.success.multiple", stage, playersAffected), 
                true);
        }
        
        return playersAffected;
    }
}
