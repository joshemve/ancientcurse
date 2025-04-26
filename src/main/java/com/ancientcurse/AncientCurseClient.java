package com.ancientcurse;

import com.ancientcurse.client.CursedPlantRenderLayer;
import com.ancientcurse.client.KhamsinCurseHudRenderer;
import com.ancientcurse.util.TooltipHelper;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;

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
        
        // Register transparency handling for the Eternal Sigil
        // This ensures the transparent parts of the texture are properly rendered
        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
            // Return -1 for no tinting (allows transparency to work correctly)
            return -1;
        }, ModItems.ETERNAL_SIGIL);
        
        // Register tooltip callback to add "Ancient Curse" to all mod items
        TooltipHelper.registerTooltipCallback();
    }
}
