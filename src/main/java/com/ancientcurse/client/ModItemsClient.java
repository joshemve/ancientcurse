package com.ancientcurse.client;

import com.ancientcurse.AncientCurse;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Client-side initialization for mod items
 */
@Environment(EnvType.CLIENT)
public class ModItemsClient implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        AncientCurse.LOGGER.info("Initializing client-side rendering for Ancient Curse mod");
        
        // For items with transparency, we don't need to register a specific render layer
        // as items with transparency will automatically use the correct render layer
        // The .mcmeta file will handle the animation frames
    }
}
