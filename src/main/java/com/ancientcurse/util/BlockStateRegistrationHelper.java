package com.ancientcurse.util;

import com.ancientcurse.AncientCurse;

/**
 * Helper class to ensure proper registration of block states
 * to prevent "Some intrusive holders were not registered" errors.
 * 
 * DISABLED: This class has been disabled to prevent intrusive holders errors.
 * Accessing vanilla block states before registries are frozen causes intrusive holders errors.
 */
public class BlockStateRegistrationHelper {

    /**
     * Register this helper to be called at the appropriate lifecycle events
     * DISABLED: This method has been disabled to prevent intrusive holders errors.
     */
    public static void register() {
        AncientCurse.LOGGER.info("BlockStateRegistrationHelper has been disabled to prevent intrusive holders errors");
        // All functionality has been disabled to prevent intrusive holders errors
    }
    
    /**
     * Pre-registers commonly used block states to ensure they are properly
     * registered before world generation starts.
     * DISABLED: This method has been disabled to prevent intrusive holders errors.
     */
    private static void preRegisterBlockStates() {
        // All functionality has been disabled to prevent intrusive holders errors
    }
}
