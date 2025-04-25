package com.ancientcurse;

import com.ancientcurse.client.CursedPlantRenderLayer;
import com.ancientcurse.client.KhamsinCurseHudRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Client-side initialization for the Ancient Curse mod
 */
@Environment(EnvType.CLIENT)
public class AncientCurseClient implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        AncientCurse.LOGGER.info("Initializing Ancient Curse Client");
        
        // Register the Khamsin Curse HUD renderer
        KhamsinCurseHudRenderer.register();
        
        // Register render layers for cursed plants
        CursedPlantRenderLayer.register();
    }
}
