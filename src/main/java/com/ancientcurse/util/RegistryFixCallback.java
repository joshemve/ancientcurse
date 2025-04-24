package com.ancientcurse.util;

import com.ancientcurse.AncientCurse;

/**
 * This class provides callbacks to fix registry issues with vanilla blocks,
 * particularly addressing the "Some intrusive holders were not registered" error 
 * with air blocks in Minecraft 1.20.1.
 * 
 * DISABLED: This class has been disabled to prevent intrusive holders errors.
 * Accessing vanilla block states before registries are frozen causes intrusive holders errors.
 */
public class RegistryFixCallback {

    /**
     * Initialize registry callbacks to ensure proper block registration
     * DISABLED: This method has been disabled to prevent intrusive holders errors.
     */
    public static void init() {
        AncientCurse.LOGGER.info("RegistryFixCallback has been disabled to prevent intrusive holders errors");
        // All functionality has been disabled to prevent intrusive holders errors
    }
    
    /**
     * Ensures the air block and other essential blocks are properly registered
     * DISABLED: This method has been disabled to prevent intrusive holders errors.
     */
    private static void ensureAirBlockRegistration() {
        // All functionality has been disabled to prevent intrusive holders errors
    }
}
