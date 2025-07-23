package com.ancientcurse;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Client-side initialization for Ancient Curse mod
 * All client-only code should be initialized here, not in the main mod class
 */
@Environment(EnvType.CLIENT)
public class AncientCurseClient implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        AncientCurse.LOGGER.info("Initializing Ancient Curse client...");
        
        // This is a minimal client initialization that ensures client-only code
        // is properly separated from server code
        
        AncientCurse.LOGGER.info("Ancient Curse client initialized!");
    }
}
