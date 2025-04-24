package com.ancientcurse.block.registry;

import com.ancientcurse.AncientCurse;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Registry class for all Egyptian-themed plant blocks
 * 
 * IMPORTANT USAGE GUIDELINES:
 * 1. All Egyptian-themed plant blocks MUST be registered here, not in ModBlocks.java
 * 2. To add a new Egyptian-themed plant block:
 *    - Define it as a public static final field in this class
 *    - Add it to the registerBlocks() method
 *    - Add it to the registerBlockItems() method
 *    - Add it to the ModItemGroup class to make it appear in the creative menu
 * 3. All Egyptian-themed plant blocks should extend EgyptianPlantBlock or a subclass of it
 * 4. Reference blocks from this class using the full path: com.ancientcurse.block.registry.EgyptianPlantBlocks.BLOCK_NAME
 * 
 * This registry was created to organize blocks by type and prevent registration conflicts.
 */
public class EgyptianPlantBlocks {
    // Note: All Egyptian plant blocks have been moved to CursedPlantBlocks
    // This class is kept for compatibility but contains no blocks
    
    /**
     * Register all Egyptian plant blocks
     * 
     * Note: All blocks have been moved to CursedPlantBlocks
     */
    public static void registerBlocks() {
        AncientCurse.LOGGER.info("No Egyptian plant blocks to register - all moved to CursedPlantBlocks");
        // All blocks have been moved to CursedPlantBlocks
    }
    
    /**
     * Register all Egyptian plant block items
     * 
     * Note: All blocks have been moved to CursedPlantBlocks
     */
    public static void registerBlockItems() {
        AncientCurse.LOGGER.info("No Egyptian plant block items to register - all moved to CursedPlantBlocks");
        // All blocks have been moved to CursedPlantBlocks
    }
    
    /**
     * Helper method to register a block with a specific identifier
     */
    private static void register(Block block, String path) {
        Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, path), block);
    }
    
    /**
     * Helper method to register a block item with a specific identifier
     */
    private static void registerBlockItem(Block block, String path) {
        Identifier id = new Identifier(AncientCurse.MOD_ID, path);
        Registry.register(Registries.ITEM, id, new net.minecraft.item.BlockItem(block, new net.fabricmc.fabric.api.item.v1.FabricItemSettings()));
    }
}
