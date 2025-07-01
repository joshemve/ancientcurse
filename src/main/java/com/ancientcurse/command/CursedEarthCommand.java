package com.ancientcurse.command;

import com.ancientcurse.block.CursedEarthBlock;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

/**
 * Admin commands for managing Cursed Earth
 * Implements the server admin controls from the roadmap
 */
public class CursedEarthCommand {
    
    private static boolean cursedEarthEnabled = true;
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("cursedearth")
            .requires(source -> source.hasPermissionLevel(2)) // OP level 2 required
            .then(CommandManager.literal("disable")
                .executes(CursedEarthCommand::disableCursedEarth))
            .then(CommandManager.literal("enable")
                .executes(CursedEarthCommand::enableCursedEarth))
            .then(CommandManager.literal("clear")
                .then(CommandManager.argument("radius", IntegerArgumentType.integer(1, 1000))
                    .executes(CursedEarthCommand::clearCursedEarth)))
            .then(CommandManager.literal("stats")
                .executes(CursedEarthCommand::showStats))
            .then(CommandManager.literal("rollback")
                .then(CommandManager.argument("minutes", IntegerArgumentType.integer(1, 1440))
                    .executes(CursedEarthCommand::rollbackCursedEarth)))
        );
    }
    
    /**
     * Disables cursed earth spreading
     */
    private static int disableCursedEarth(CommandContext<ServerCommandSource> context) {
        cursedEarthEnabled = false;
        context.getSource().sendMessage(Text.literal("§cCursed Earth spreading has been disabled."));
        return 1;
    }
    
    /**
     * Enables cursed earth spreading
     */
    private static int enableCursedEarth(CommandContext<ServerCommandSource> context) {
        cursedEarthEnabled = true;
        context.getSource().sendMessage(Text.literal("§aCursed Earth spreading has been enabled."));
        return 1;
    }
    
    /**
     * Clears cursed earth in a radius around the player
     */
    private static int clearCursedEarth(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        int radius = IntegerArgumentType.getInteger(context, "radius");
        
        if (source.getPlayer() == null) {
            source.sendMessage(Text.literal("§cThis command must be run by a player."));
            return 0;
        }
        
        ServerWorld world = source.getWorld();
        BlockPos center = source.getPlayer().getBlockPos();
        int blocksCleared = 0;
        
        // Clear cursed earth in radius
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -radius; y <= radius; y++) {
                    BlockPos pos = center.add(x, y, z);
                    if (world.getBlockState(pos).getBlock().getClass().getSimpleName().equals("CursedEarthBlock")) {
                        world.setBlockState(pos, net.minecraft.block.Blocks.DIRT.getDefaultState());
                        blocksCleared++;
                    }
                }
            }
        }
        
        // Update chunk counts
        ChunkPos chunkPos = new ChunkPos(center);
        CursedEarthCommand.clearChunkCurseCount(chunkPos);
        
        source.sendMessage(Text.literal("§aCleared " + blocksCleared + " cursed earth blocks in a " + radius + " block radius."));
        return blocksCleared;
    }
    
    /**
     * Shows cursed earth statistics
     */
    private static int showStats(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        int totalCursedBlocks = CursedEarthBlock.getTotalCursedBlocks();
        boolean enabled = cursedEarthEnabled;
        
        source.sendMessage(Text.literal("§6=== Cursed Earth Statistics ==="));
        source.sendMessage(Text.literal("§eStatus: " + (enabled ? "§aEnabled" : "§cDisabled")));
        source.sendMessage(Text.literal("§eTotal Cursed Blocks: §f" + totalCursedBlocks));
        source.sendMessage(Text.literal("§eChunks with Cursed Earth: §f" + getChunkCount()));
        
        return 1;
    }
    
    /**
     * Rollback cursed earth (placeholder for future implementation)
     */
    private static int rollbackCursedEarth(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        int minutes = IntegerArgumentType.getInteger(context, "minutes");
        
        source.sendMessage(Text.literal("§cRollback functionality not yet implemented."));
        source.sendMessage(Text.literal("§7Would rollback " + minutes + " minutes of cursed earth spread."));
        
        return 0;
    }
    
    /**
     * Gets the number of chunks with cursed earth
     */
    private static int getChunkCount() {
        // This would need to be implemented in CursedEarthBlock
        return 0; // Placeholder
    }
    
    /**
     * Clears chunk curse count (for admin commands)
     */
    private static void clearChunkCurseCount(ChunkPos chunkPos) {
        CursedEarthBlock.clearChunkCurseCount(chunkPos);
    }
    
    /**
     * Checks if cursed earth is enabled
     */
    public static boolean isEnabled() {
        return cursedEarthEnabled;
    }
} 