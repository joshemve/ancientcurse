package com.ancientcurse.client;

import com.ancientcurse.AncientCurse;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Client-side utilities for the Khamsin Curse effect
 * We're now using the standard Minecraft status effect system for displaying the curse
 */
@Environment(EnvType.CLIENT)
public class KhamsinCurseHudRenderer {
    
    /**
     * Register any client-side components
     * This is now a no-op since we're using the standard status effect system
     */
    public static void register() {
        AncientCurse.LOGGER.info("Khamsin Curse now uses standard status effect rendering");
    }
}
