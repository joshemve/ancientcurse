package com.ancientcurse.world.biome;

import com.ancientcurse.AncientCurse;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

/**
 * Registers custom biomes for the Ancient Curse mod.
 */
public class ModBiomes {
    
    // Define registry keys for custom biomes
    public static final RegistryKey<Biome> ANCIENT_DESERT = RegistryKey.of(
        RegistryKeys.BIOME, new Identifier(AncientCurse.MOD_ID, "ancient_desert"));
    
    public static final RegistryKey<Biome> DESHRET_DESERT = RegistryKey.of(
        RegistryKeys.BIOME, new Identifier(AncientCurse.MOD_ID, "deshret_desert"));
    
    public static final RegistryKey<Biome> NILE_RIVER = RegistryKey.of(
        RegistryKeys.BIOME, new Identifier(AncientCurse.MOD_ID, "nile_river"));
    
    /**
     * Register biomes - biomes are loaded from JSON files in data/ancientcurse/worldgen/biome/
     */
    public static void registerBiomes() {
        AncientCurse.LOGGER.info("Registering custom biomes for " + AncientCurse.MOD_ID);
        
        // Log the biome registry keys that will be loaded from JSON
        AncientCurse.LOGGER.info("Ancient Desert biome key: " + ANCIENT_DESERT.getValue());
        AncientCurse.LOGGER.info("Deshret Desert biome key: " + DESHRET_DESERT.getValue());
        AncientCurse.LOGGER.info("Nile River biome key: " + NILE_RIVER.getValue());
        
        // Note: Actual biome registration happens automatically from JSON files
        // in data/ancientcurse/worldgen/biome/ when the world is loaded
        AncientCurse.LOGGER.info("Custom biomes will be loaded from JSON definitions");
    }
} 