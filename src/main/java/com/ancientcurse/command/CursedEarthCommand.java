package com.ancientcurse.command;

import com.ancientcurse.block.CursedEarthBlock;
import com.ancientcurse.system.OriginalBlockTracker;
import com.ancientcurse.system.CursedEarthManager;
import com.ancientcurse.ModBlocks;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import java.util.Set;
import java.util.HashSet;

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
    public static int clearCursedEarthCommand(CommandContext<ServerCommandSource> context) {
        return clearCursedEarth(context);
    }
    
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
        int restoredToOriginal = 0;
        
        // Get the original block tracker
        OriginalBlockTracker tracker = OriginalBlockTracker.get(world);
        
        // Clear cursed earth in radius
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -radius; y <= radius; y++) {
                    BlockPos pos = center.add(x, y, z);
                    BlockState currentState = world.getBlockState(pos);
                    
                    // Check if it's cursed earth
                    if (currentState.isOf(ModBlocks.CURSED_EARTH)) {
                        // Try to get the original block
                        BlockState originalState = tracker.getOriginalBlock(pos);
                        
                        if (originalState != null) {
                            // Restore to original block
                            world.setBlockState(pos, originalState);
                            tracker.clearTracking(pos); // Clean up tracking data
                            restoredToOriginal++;
                        } else {
                            // Fallback to dirt if no original tracked
                            world.setBlockState(pos, Blocks.DIRT.getDefaultState());
                        }
                        
                        // Decrement chunk curse count
                        CursedEarthBlock.decrementChunkCurseCount(pos);
                        blocksCleared++;
                    }
                }
            }
        }
        
        // Update chunk counts
        ChunkPos chunkPos = new ChunkPos(center);
        CursedEarthBlock.clearChunkCurseCount(chunkPos);
        
        // Report statistics
        if (restoredToOriginal > 0) {
            source.sendMessage(Text.literal("§aCleared " + blocksCleared + " cursed earth blocks in a " + radius + " block radius."));
            source.sendMessage(Text.literal("§a" + restoredToOriginal + " blocks restored to their original type."));
            
            // Show memory usage if significant
            OriginalBlockTracker.TrackerStats stats = tracker.getStats();
            if (stats.blocksTracked > 1000) {
                source.sendMessage(Text.literal("§7Tracker memory usage: " + stats.getMemoryUsageString()));
            }
        } else {
            source.sendMessage(Text.literal("§aCleared " + blocksCleared + " cursed earth blocks in a " + radius + " block radius."));
        }
        
        return blocksCleared;
    }
    
    /**
     * Shows cursed earth statistics
     */
    public static int showStatsCommand(CommandContext<ServerCommandSource> context) {
        return showStats(context);
    }
    
    private static int showStats(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        
        int totalCursedBlocks = CursedEarthBlock.getTotalCursedBlocks();
        boolean enabled = cursedEarthEnabled;
        
        source.sendMessage(Text.literal("§6=== Cursed Earth Statistics ==="));
        source.sendMessage(Text.literal("§eStatus: " + (enabled ? "§aEnabled" : "§cDisabled")));
        source.sendMessage(Text.literal("§eTotal Cursed Blocks: §f" + totalCursedBlocks));
        source.sendMessage(Text.literal("§eChunks with Cursed Earth: §f" + getChunkCount()));
        
        // Show original block tracking stats
        OriginalBlockTracker tracker = OriginalBlockTracker.get(world);
        OriginalBlockTracker.TrackerStats stats = tracker.getStats();
        
        source.sendMessage(Text.literal("§6=== Memory Usage ==="));
        source.sendMessage(Text.literal("§eBlocks Tracked: §f" + stats.blocksTracked));
        source.sendMessage(Text.literal("§eChunks Tracked: §f" + stats.chunksTracked));
        source.sendMessage(Text.literal("§eUnique Block Types: §f" + stats.uniqueBlockTypes));
        source.sendMessage(Text.literal("§eEstimated Memory: §f" + stats.getMemoryUsageString()));
        
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
    
    /**
     * Sets whether cursed earth spreading is enabled
     */
    public static void setSpreadEnabled(boolean enabled) {
        cursedEarthEnabled = enabled;
    }
    
    /**
     * Clears cleansing protection for all connected protected blocks
     */
    public static int clearPreventionCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        PlayerEntity player = source.getPlayer();
        
        if (player == null) {
            source.sendMessage(Text.literal("§cThis command must be run by a player."));
            return 0;
        }
        
        BlockPos playerPos = player.getBlockPos();
        ServerWorld world = source.getWorld();
        CursedEarthManager manager = CursedEarthManager.getInstance();
        
        // Increase search radius to 50 blocks
        int searchRadius = 50;
        int protectedBlocksFound = 0;
        Set<BlockPos> protectedBlocks = new HashSet<>();
        
        // Find ALL protected blocks in the area
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -10; y <= 10; y++) { // Reasonable Y range
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    if (manager.isProtectedByCleansingStation(pos)) {
                        protectedBlocks.add(pos);
                        protectedBlocksFound++;
                    }
                }
            }
        }
        
        if (protectedBlocks.isEmpty()) {
            source.sendMessage(Text.literal("§eNo protected blocks found within " + searchRadius + " blocks of your position."));
            source.sendMessage(Text.literal("§7Stand near a protected area to clear its protection."));
            return 0;
        }
        
        source.sendMessage(Text.literal("§eFound " + protectedBlocksFound + " protected blocks in range."));
        source.sendMessage(Text.literal("§eClearing all protection..."));
        
        // Clear ALL found protections
        int clearedCount = 0;
        for (BlockPos pos : protectedBlocks) {
            if (manager.clearProtectionAt(pos)) {
                clearedCount++;
            }
        }
        
        if (clearedCount > 0) {
            source.sendMessage(Text.literal("§aCleared protection from " + clearedCount + " connected blocks."));
            source.sendMessage(Text.literal("§7Cursed earth can now spread in this entire area again."));
            
            // Visual feedback
            world.playSound(player, playerPos, net.minecraft.sound.SoundEvents.BLOCK_BEACON_DEACTIVATE, 
                net.minecraft.sound.SoundCategory.BLOCKS, 0.5f, 1.5f);
        }
        
        return clearedCount;
    }
    
    /**
     * Clears cleansing protection with a specified radius
     */
    public static int clearPreventionCommandWithRadius(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        PlayerEntity player = source.getPlayer();
        int radius = IntegerArgumentType.getInteger(context, "radius");
        
        if (player == null) {
            source.sendMessage(Text.literal("§cThis command must be run by a player."));
            return 0;
        }
        
        BlockPos playerPos = player.getBlockPos();
        ServerWorld world = source.getWorld();
        CursedEarthManager manager = CursedEarthManager.getInstance();
        
        source.sendMessage(Text.literal("§eScanning for protected blocks within " + radius + " blocks..."));
        
        Set<BlockPos> protectedBlocks = new HashSet<>();
        
        // Find ALL protected blocks in the specified radius
        for (int x = -radius; x <= radius; x++) {
            for (int y = -Math.min(radius, 20); y <= Math.min(radius, 20); y++) { // Cap Y to reasonable range
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    if (pos.isWithinDistance(playerPos, radius) && manager.isProtectedByCleansingStation(pos)) {
                        protectedBlocks.add(pos);
                    }
                }
            }
        }
        
        if (protectedBlocks.isEmpty()) {
            source.sendMessage(Text.literal("§eNo protected blocks found within " + radius + " blocks."));
            return 0;
        }
        
        source.sendMessage(Text.literal("§eFound " + protectedBlocks.size() + " protected blocks."));
        source.sendMessage(Text.literal("§aClearing all protection..."));
        
        // Clear ALL found protections
        int clearedCount = 0;
        for (BlockPos pos : protectedBlocks) {
            if (manager.clearProtectionAt(pos)) {
                clearedCount++;
            }
        }
        
        if (clearedCount > 0) {
            source.sendMessage(Text.literal("§aCleared protection from " + clearedCount + " blocks."));
            source.sendMessage(Text.literal("§7Cursed earth can now spread in this area again."));
            
            // Visual feedback
            world.playSound(player, playerPos, net.minecraft.sound.SoundEvents.BLOCK_BEACON_DEACTIVATE, 
                net.minecraft.sound.SoundCategory.BLOCKS, 0.5f, 1.5f);
        }
        
        return clearedCount;
    }
} 