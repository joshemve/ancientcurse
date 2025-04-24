package com.ancientcurse.block.registry;

import com.ancientcurse.AncientCurse;

/**
 * Central registry for all block registrations
 */
public class BlockRegistry {
    /**
     * Registers all blocks from all block registry classes
     */
    public static void registerAll() {
        AncientCurse.LOGGER.info("Registering all blocks for " + AncientCurse.MOD_ID);
        
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
    }
}
