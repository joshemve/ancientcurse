package com.example.egyptianweapons.registry;

import com.example.egyptianweapons.EgyptianWeapons;

/**
 * This class has been deprecated and is kept only for reference.
 * All item registration is now done manually in ItemRegistry and ModelRegistry.
 */
public class ModelScanner {
    /**
     * This method is deprecated and no longer used.
     * All items are now registered manually in ItemRegistry.
     */
    public static void scanAndRegisterModels() {
        EgyptianWeapons.LOGGER.info("Auto-scanning for models is now disabled. All items are registered manually.");
    }
}