package com.ancientcurse;

import com.ancientcurse.world.structure.ModStructurePieceTypes;
import com.ancientcurse.world.structure.NileRiverStructureType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModStructures {
    // Use fully qualified class name to avoid import conflicts
    public static final RegistryKey<net.minecraft.world.gen.structure.Structure> PYRAMID_KEY = RegistryKey.of(
        RegistryKeys.STRUCTURE, 
        new Identifier("ancientcurse", "pyramid")
    );
    
    public static final RegistryKey<net.minecraft.world.gen.structure.Structure> NILE_RIVER_KEY = RegistryKey.of(
        RegistryKeys.STRUCTURE, 
        new Identifier("ancientcurse", "nile_river")
    );

    public static void registerStructures() {
        
        // In Minecraft 1.20.1, structure registration is primarily done through JSON files
        // The structure is defined in data/ancientcurse/worldgen/structure/pyramid.json
        // The structure set is defined in data/ancientcurse/worldgen/structure_set/pyramid.json
        // The template pool is defined in data/ancientcurse/worldgen/template_pool/pyramid/start_pool.json
        
        // Register custom structure types and pieces
        registerStructureTypes();
        ModStructurePieceTypes.registerStructurePieceTypes();
        NileRiverStructureType.registerStructureTypes();
        
        AncientCurse.LOGGER.info("Ancient Egypt structures registered successfully");
        AncientCurse.LOGGER.info("Nile River structure system initialized");
    }
    
    private static void registerStructureTypes() {
        // In Minecraft 1.20.1, structure types are registered through the Registry system
        // The actual registration happens through JSON, but we log it here for clarity
        AncientCurse.LOGGER.info("Structure types are registered through JSON in Minecraft 1.20.1");
        AncientCurse.LOGGER.info("Pyramid structure is defined in data/ancientcurse/worldgen/structure/pyramid.json");
        AncientCurse.LOGGER.info("Pyramid structure set is defined in data/ancientcurse/worldgen/structure_set/pyramid.json");
        AncientCurse.LOGGER.info("Nile River structure is defined in data/ancientcurse/worldgen/structure/nile_river.json");
        AncientCurse.LOGGER.info("Nile River structure set is defined in data/ancientcurse/worldgen/structure_set/nile_river.json");
    }
}