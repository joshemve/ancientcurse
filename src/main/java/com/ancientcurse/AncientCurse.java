package com.ancientcurse;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.GeckoLib;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.WorldPreset;

import com.ancientcurse.effect.ModStatusEffects;


/**
 * Main mod class for Ancient Curse.
 * 
 * IMPORTANT MOD ARCHITECTURE GUIDELINES:
 * 
 * 1. REGISTRY SYSTEM:
 *    - All blocks are organized into specialized registry classes in com.ancientcurse.block.registry
 *    - Each registry class handles a specific type of block (e.g., CursedPlantBlocks, EgyptianPlantBlocks)
 *    - New blocks should be added to the appropriate registry class, NOT directly in ModBlocks
 *    - The BlockRegistry class coordinates all block registrations
 * 
 * 2. BLOCK REGISTRATION PROCESS:
 *    - Blocks are defined as public static final fields in their registry class
 *    - Blocks are registered to the game in the registerBlocks() method of their registry class
 *    - Block items are registered in the registerBlockItems() method of their registry class
 *    - All registry classes are called from BlockRegistry.registerAll() and BlockRegistry.registerBlockItems()
 * 
 * 3. CREATIVE MENU:
 *    - All blocks must be added to the creative menu in ModItemGroup
 *    - Blocks from registry classes should be referenced using their full path
 *    - Related blocks should be grouped together with appropriate comments
 * 
 * Following these guidelines prevents registration conflicts and makes the codebase more maintainable.
 * See previous fixes for registration conflicts in the mod's history.
 */
public class AncientCurse implements ModInitializer {
    public static final String MOD_ID = "ancientcurse";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    // Define our world preset key
    public static final RegistryKey<WorldPreset> ANCIENT_CURSE_PRESET = RegistryKey.of(
        RegistryKeys.WORLD_PRESET, new Identifier(MOD_ID, "ancient_curse")
    );

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Ancient Curse Mod");
        
        // Initialize GeckoLib
        GeckoLib.initialize();
        
        // Register content
        registerContent();
        
        // Register worldgen components
        registerWorldgenComponents();
        
        LOGGER.info("Ancient Curse Mod fully initialized");
    }
    
    /**
     * Registers all mod content (blocks, items, etc.)
     */
    private void registerContent() {
        // Register mod item groups
        ModItemGroup.registerItemGroups();
        
        // Register mod blocks
        ModBlocks.registerBlocks();
        
        // Register specialized block registries
        com.ancientcurse.block.registry.BlockRegistry.registerAll();
        com.ancientcurse.block.registry.BlockRegistry.registerBlockItems();
        
        // Register mod items
        ModItems.registerItems();
        
        // Register mod block entities
        ModBlockEntities.registerBlockEntities();
        
        // Register mod screen handlers
        ModScreenHandlers.registerScreenHandlers();
        
        // Register mod entities
        ModEntities.registerEntities();
        
        // Register mod commands
        ModCommands.registerCommands();
        
        // Register structures
        ModStructures.registerStructures();
        
        // Register status effects
        ModStatusEffects.registerStatusEffects();
    }
    
    /**
     * Registers all world generation components
     */
    private void registerWorldgenComponents() {
        // Comment out biome-related code to revert to vanilla generation
        // ModBiomes.registerBiomes();
        
        // Comment out dimension registration
        // ModDimensions.register();
        
        // Comment out world presets
        // ModWorldPresets.register();
        
        // Disable custom surface rules and biome modifiers
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            // Comment out surface rule registration
            // ModSurfaceRuleRegistration.register();
            
            // Comment out biome modifiers
            // BiomeModifier.register();
            
            // Initialize world generation with compatibility handling but skip custom biome code
            // AncientWorldGeneration.init();
            
            // Log that we're using vanilla generation
            LOGGER.info("Using vanilla Minecraft world generation (custom biomes disabled)");
        });
    }
}