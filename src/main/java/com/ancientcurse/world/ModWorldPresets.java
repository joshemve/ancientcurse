package com.ancientcurse.world;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.world.dimension.ModDimensions;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.WorldPreset;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/**
 * Registers custom world presets.
 */
public class ModWorldPresets {

    // Declare registry keys for world presets
    public static final RegistryKey<WorldPreset> ANCIENT_CURSE_PRESET = RegistryKey.of(
        RegistryKeys.WORLD_PRESET, new Identifier(AncientCurse.MOD_ID, "ancient_curse"));
    
    public static final RegistryKey<WorldPreset> NILE_RIVER_SINGLE_BIOME = RegistryKey.of(
        RegistryKeys.WORLD_PRESET, new Identifier(AncientCurse.MOD_ID, "nile_river_single_biome"));

    /**
     * Registers all world presets for the mod.
     */
    public static void register() {
        // Simply define our world preset keys, actual registration happens in JSON
        AncientCurse.LOGGER.info("Ancient Curse preset key: " + ANCIENT_CURSE_PRESET.getValue());
        AncientCurse.LOGGER.info("Nile River preset key: " + NILE_RIVER_SINGLE_BIOME.getValue());
        
        // Register server lifecycle event to check if our preset is recognized
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            var registry = server.getRegistryManager().get(RegistryKeys.WORLD_PRESET);
            
            // Check if our presets are registered
            boolean ancientCurseRegistered = registry.contains(ANCIENT_CURSE_PRESET);
            boolean nileRiverRegistered = registry.contains(NILE_RIVER_SINGLE_BIOME);
            
            AncientCurse.LOGGER.info("Ancient Curse preset registered: " + ancientCurseRegistered);
            AncientCurse.LOGGER.info("Nile River preset registered: " + nileRiverRegistered);
            
            // Available world presets for debugging
            AncientCurse.LOGGER.info("Available world presets:");
            registry.getKeys().forEach(key -> {
                AncientCurse.LOGGER.info(" - " + key.getValue());
            });
        });
    }
}
