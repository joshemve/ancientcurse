package com.ancientcurse;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.GeckoLib;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.WorldPreset;
import com.ancientcurse.command.LotusSwarmCommand;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
// import com.ancientcurse.screen.ModScreenHandlers;
// import com.ancientcurse.worldgen.ModWorldGen;
// import com.ancientcurse.worldgen.ModWorldPresets;
// import com.ancientcurse.ModStatusEffects;
// import com.ancientcurse.block.ModBlocks;
// import com.ancientcurse.block.entity.ModBlockEntities;

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
 * 4. CLIENT INITIALIZATION:
 *    - The ONLY client initializer is com.ancientcurse.client.AncientCurseClient
 *    - All client-side registrations (render layers, HUD elements, etc.) should be done there
 *    - NEVER create multiple classes implementing ClientModInitializer
 *    - Duplicate client initializers cause transparency issues and duplicate tooltips
 * 
 * 5. TRANSPARENCY RENDERING:
 *    - For transparent blocks, use BOTH .nonOpaque() AND .notSolid() in block settings
 *    - Register transparent blocks with BlockRenderLayerMap.INSTANCE.putBlock(block, RenderLayer.getCutout())
 *    - Only register each block for render layers ONCE to avoid conflicts
 * 
 * Following these guidelines prevents registration conflicts and makes the codebase more maintainable.
 * See previous fixes for registration conflicts and transparency issues in the mod's history.
 */
public class AncientCurse implements ModInitializer {
    public static final String MOD_ID = "ancientcurse";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    // Define our world preset key
    public static final RegistryKey<WorldPreset> ANCIENT_CURSE_PRESET = RegistryKey.of(
        RegistryKeys.WORLD_PRESET, new Identifier(MOD_ID, "ancient_curse")
    );
    public static final RegistryKey<World> ANCIENT_EGYPT_DIMENSION =
            RegistryKey.of(RegistryKeys.WORLD, new Identifier(MOD_ID, "ancient_egypt"));

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Ancient Curse Mod");
        
        // Initialize GeckoLib
        GeckoLib.initialize();
        
        // Register content
        registerContent();
        
        // Register worldgen components
        registerWorldgenComponents();
        
        // Register commands
        registerCommands();
        
        LOGGER.info("Ancient Curse Mod fully initialized");
    }
    
    /**
     * Registers all mod content (blocks, items, etc.)
     */
    private void registerContent() {
        // Register mod item groups
        ModItemGroup.registerItemGroups();
        
        // Register ModBlocks first to establish baseline registrations
        // This ensures that blocks like OFFERING_POT are registered first
        // ModBlocks.registerBlocks();
        
        // Then register specialized block registries
        // Note: We've modified PotteryBlocks.java to avoid re-registering OFFERING_POT
        com.ancientcurse.block.registry.BlockRegistry.registerAll();
        com.ancientcurse.block.registry.BlockRegistry.registerBlockItems();
        
        // Register mod items
        ModItems.registerItems();
        
        // Register mod entities
        ModEntities.registerEntities();
        
        // Register mod block entities
        // ModBlockEntities.registerBlockEntities();
        
        // Register mod screen handlers
        // ModScreenHandlers.registerScreenHandlers();
        
        // Register mod entities
        ModEntities.registerEntities();
        
        // Register mod commands
        ModCommands.registerCommands();
        
        // Register structures
        ModStructures.registerStructures();
        
        // Register status effects
        // ModStatusEffects.registerStatusEffects();
        
        // Register server tick event for tornado management
        ServerTickEvents.END_SERVER_TICK.register(server -> com.ancientcurse.effect.TornadoManager.tick(server));
    }
    
    /**
     * Registers all world generation components
     */
    private void registerWorldgenComponents() {
        // Enable world presets only - this will show the button
        // com.ancientcurse.world.ModWorldPresets.register();
        
        LOGGER.info("World presets registered - Ancient Curse world type available");
        
        // TEMPORARILY DISABLED: com.ancientcurse.world.biome.ModBiomes.registerBiomes();
        // TEMPORARILY DISABLED: com.ancientcurse.world.ModChunkGenerators.register();
    }

    /**
     * Register commands for the mod
     */
    private void registerCommands() {
        LOGGER.info("Registering commands for " + MOD_ID);
        
        // Register the Lotus Swarm command
        CommandRegistrationCallback.EVENT.register(LotusSwarmCommand::register);
    }
}