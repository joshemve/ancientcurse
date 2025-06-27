package com.ancientcurse.world.biome;

import com.ancientcurse.AncientCurse;

/**
 * Handles biome modifications for Ancient Curse
 */
public class BiomeModifier {
    
    /**
     * Register biome modifiers
     */
    public static void register() {
        AncientCurse.LOGGER.info("Registering Ancient Curse biome modifiers");
        
        // Biome modifications will be handled by the chunk generator mixin
        // to ensure smooth sand replacement works properly
    }
}
