package com.ancientcurse.block.registry;

import com.ancientcurse.AncientCurse;

/**
 * Central registry for all block registrations
 * 
 * IMPORTANT REGISTRY GUIDELINES:
 * 1. All blocks should be registered through specialized registry classes (e.g., NecrostoneBlocks, CursedPlantBlocks)
 * 2. Each registry class should handle its own block type and follow the same pattern:
 *    - Define blocks as public static final fields
 *    - Provide registerBlocks() method to register the blocks
 *    - Provide registerBlockItems() method to register the block items
 * 3. DO NOT register blocks directly in ModBlocks.java - use the appropriate registry class
 * 4. When adding a new block type, create a new registry class and add it to this central registry
 * 5. Always update both registerAll() and registerBlockItems() methods when adding a new registry class
 * 
 * This approach prevents duplicate registrations and makes the codebase more maintainable.
 */
public class BlockRegistry {
    /**
     * Registers all blocks from all block registry classes
     */
    public static void registerAll() {
        AncientCurse.LOGGER.info("Registering all blocks for " + AncientCurse.MOD_ID);
        
        // CRITICAL: Register ModBlocks first to ensure all core blocks are registered
        com.ancientcurse.ModBlocks.registerBlocks();
        
        // Register blocks from each registry class
        // Note: These registry classes will be implemented in future updates
        // NatureBlocks.registerBlocks();
        // ConstructionBlocks.registerBlocks();
        // TerrainBlocks.registerBlocks();
        // MetalBlocks.registerBlocks();
        // DecorationBlocks.registerBlocks();
        
        // Register new block types
        NecrostoneBlocks.registerBlocks();
        PillarBlocks.registerBlocks();
        HeadstoneBlocks.registerBlocks();
        FurnitureBlocks.registerBlocks();
        DeshretBlocks.registerBlocks();
        PotteryBlocks.registerBlocks();
        
        // Register plant blocks
        CursedPlantBlocks.registerBlocks();
        EgyptianPlantBlocks.registerBlocks();
    }
    
    /**
     * Registers all block items from all block registry classes
     */
    public static void registerBlockItems() {
        AncientCurse.LOGGER.info("Registering all block items for " + AncientCurse.MOD_ID);
        
        // Register block items from each registry class
        // Note: These registry classes will be implemented in future updates
        // NatureBlocks.registerBlockItems();
        // ConstructionBlocks.registerBlockItems();
        // TerrainBlocks.registerBlockItems();
        // MetalBlocks.registerBlockItems();
        // DecorationBlocks.registerBlockItems();
        
        // Register new block item types
        NecrostoneBlocks.registerBlockItems();
        PillarBlocks.registerBlockItems();
        HeadstoneBlocks.registerBlockItems();
        FurnitureBlocks.registerBlockItems();
        DeshretBlocks.registerBlockItems();
        PotteryBlocks.registerBlockItems();
        
        // Register plant block items
        CursedPlantBlocks.registerBlockItems();
        EgyptianPlantBlocks.registerBlockItems();
    }
}
