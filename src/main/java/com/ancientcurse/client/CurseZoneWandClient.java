package com.ancientcurse.client;

import com.ancientcurse.gui.CurseZoneMenuScreen;
import com.ancientcurse.util.WandSelectionManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

/**
 * Client-side handler for CurseZoneAdminWand functionality
 * This keeps all client-only code separate from the main item class
 */
@Environment(EnvType.CLIENT)
public class CurseZoneWandClient {
    
    /**
     * Opens the curse zone menu screen
     * @param pos1 First selected position
     * @param pos2 Second selected position
     * @param onClear Callback when selection is cleared
     */
    public static void openCurseZoneMenu(BlockPos pos1, BlockPos pos2, Runnable onClear) {
        MinecraftClient.getInstance().setScreen(new CurseZoneMenuScreen(pos1, pos2, onClear));
    }
    
    /**
     * Clears the visual selection for a player
     * @param player The player to clear selection for
     */
    public static void clearSelection(PlayerEntity player) {
        WandSelectionManager.clearSelection(player);
    }
    
    /**
     * Sets the first position in the visual selection
     * @param player The player to update selection for
     * @param pos The position to set
     */
    public static void setFirstPosition(PlayerEntity player, BlockPos pos) {
        WandSelectionManager.setFirstPosition(player, pos);
    }
    
    /**
     * Sets the second position in the visual selection
     * @param player The player to update selection for
     * @param pos The position to set
     */
    public static void setSecondPosition(PlayerEntity player, BlockPos pos) {
        WandSelectionManager.setSecondPosition(player, pos);
    }
}
