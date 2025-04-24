package com.ancientcurse.world;

import com.ancientcurse.AncientCurse;
import net.minecraft.block.BlockState;

/**
 * Handles world initialization to prevent issues with unregistered blocks
 * during world generation.
 * 
 * DISABLED: This class has been completely disabled to prevent intrusive holders errors.
 * Accessing vanilla block states before registries are frozen causes intrusive holders errors.
 */
public class WorldInitHandler {
    
    /**
     * Register event handlers for server lifecycle and world loading
     * DISABLED: This method has been disabled to prevent intrusive holders errors.
     */
    public static void register() {
        AncientCurse.LOGGER.info("WorldInitHandler has been disabled to prevent intrusive holders errors");
        // All functionality has been disabled to prevent intrusive holders errors
    }
    
    /**
     * Called during world generation to ensure any blocks being placed
     * are properly registered.
     * DISABLED: This method has been disabled to prevent intrusive holders errors.
     */
    public static BlockState safeGetBlockState(BlockState original) {
        // All functionality has been disabled to prevent intrusive holders errors
        return original;
    }
    
    /**
     * Get the empty state for world generation
     * DISABLED: This method has been disabled to prevent intrusive holders errors.
     */
    public static BlockState getEmptyState() {
        // All functionality has been disabled to prevent intrusive holders errors
        return null;
    }
}
