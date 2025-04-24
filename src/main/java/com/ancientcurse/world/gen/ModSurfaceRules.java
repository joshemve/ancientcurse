package com.ancientcurse.world.gen;

import com.ancientcurse.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.world.gen.surfacebuilder.MaterialRules;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRule;

/**
 * Template for surface rules that can be implemented later.
 * Currently using vanilla Minecraft surface generation.
 */
public class ModSurfaceRules {
    
    /**
     * Create surface rules for the mod.
     * This is a placeholder that can be expanded later.
     */
    public static MaterialRule createSurfaceRules() {
        // TEMPORARILY DISABLED TO PREVENT INTRUSIVE HOLDERS ERRORS
        // This method was accessing vanilla block states too early in the initialization process
        // Return an empty sequence that doesn't access any vanilla blocks
        return MaterialRules.sequence();
        
        /* Original implementation - commented out to fix intrusive holders error
        return MaterialRules.sequence(
            MaterialRules.condition(
                MaterialRules.surface(),
                block(Blocks.GRASS_BLOCK)
            )
        );
        */
    }
    
    // Helper method to create block rules - commented out to prevent intrusive holders errors
    private static MaterialRule block(Block block) {
        // This method accesses block.getDefaultState() which causes intrusive holders errors
        // when called too early in the initialization process
        return MaterialRules.sequence(); // Empty rule as placeholder
        // return MaterialRules.block(block.getDefaultState());
    }
} 