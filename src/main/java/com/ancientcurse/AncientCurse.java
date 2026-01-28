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
import com.ancientcurse.command.LocusSwarmCommand;
import com.ancientcurse.command.CursedEarthCommand;
import com.ancientcurse.system.CursedEarthManager;
import com.ancientcurse.ModSounds;
import com.ancientcurse.network.CurseZonePackets;
import com.ancientcurse.event.PlayerJoinHandler;
import com.ancientcurse.event.PathCreationHandler;
import com.ancientcurse.event.AnimationDebugHandler;
import com.ancientcurse.event.WaterInteractionHandler;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
// import com.ancientcurse.screen.ModScreenHandlers;
// import com.ancientcurse.worldgen.ModWorldGen;
// import com.ancientcurse.worldgen.ModWorldPresets;
import com.ancientcurse.effect.ModStatusEffects;
// import com.ancientcurse.block.ModBlocks;
import com.ancientcurse.ModBlockEntities;

/**
 * Main mod class for Ancient Curse.
 * 
 * IMPORTANT MOD ARCHITECTURE GUIDELINES:
 * 
 * 1. REGISTRY SYSTEM:
 * - All blocks are organized into specialized registry classes in
 * com.ancientcurse.block.registry
 * - Each registry class handles a specific type of block (e.g.,
 * CursedPlantBlocks, EgyptianPlantBlocks)
 * - New blocks should be added to the appropriate registry class, NOT directly
 * in ModBlocks
 * - The BlockRegistry class coordinates all block registrations
 * 
 * 2. BLOCK REGISTRATION PROCESS:
 * - Blocks are defined as public static final fields in their registry class
 * - Blocks are registered to the game in the registerBlocks() method of their
 * registry class
 * - Block items are registered in the registerBlockItems() method of their
 * registry class
 * - All registry classes are called from BlockRegistry.registerAll() and
 * BlockRegistry.registerBlockItems()
 * 
 * 3. CREATIVE MENU:
 * - All blocks must be added to the creative menu in ModItemGroup
 * - Blocks from registry classes should be referenced using their full path
 * - Related blocks should be grouped together with appropriate comments
 * 
 * 4. CLIENT INITIALIZATION:
 * - The ONLY client initializer is com.ancientcurse.client.AncientCurseClient
 * - All client-side registrations (render layers, HUD elements, etc.) should be
 * done there
 * - NEVER create multiple classes implementing ClientModInitializer
 * - Duplicate client initializers cause transparency issues and duplicate
 * tooltips
 * 
 * 5. TRANSPARENCY RENDERING:
 * - For transparent blocks, use BOTH .nonOpaque() AND .notSolid() in block
 * settings
 * - Register transparent blocks with
 * BlockRenderLayerMap.INSTANCE.putBlock(block, RenderLayer.getCutout())
 * - Only register each block for render layers ONCE to avoid conflicts
 * 
 * Following these guidelines prevents registration conflicts and makes the
 * codebase more maintainable.
 * See previous fixes for registration conflicts and transparency issues in the
 * mod's history.
 */
public class AncientCurse implements ModInitializer {
    public static final String MOD_ID = "ancientcurse";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Define our world preset key
    public static final RegistryKey<WorldPreset> ANCIENT_CURSE_PRESET = RegistryKey.of(
            RegistryKeys.WORLD_PRESET, new Identifier(MOD_ID, "ancient_curse"));
    public static final RegistryKey<World> ANCIENT_EGYPT_DIMENSION = RegistryKey.of(RegistryKeys.WORLD,
            new Identifier(MOD_ID, "ancient_egypt"));

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

        // Register network packets
        CurseZonePackets.registerServerPackets();
        LOGGER.info("Curse Zone network packets registered");

        // Initialize Cursed Earth Manager
        CursedEarthManager.getInstance();
        LOGGER.info("Cursed Earth performance system initialized");

        // Register fuel values for wood blocks
        ModFuelRegistry.registerFuels();
        LOGGER.info("Fuel values registered for wood blocks");

        LOGGER.info("Ancient Curse Mod fully initialized");
    }

    /**
     * Registers all mod content (blocks, items, etc.)
     */
    private void registerContent() {
        // Register mod item groups
        ModItemGroup.registerItemGroups();

        // Register mod fluids
        com.ancientcurse.registry.ModFluids.registerFluids();

        // Register cauldron behaviors
        com.ancientcurse.registry.ModCauldronBehavior.registerBehaviors();

        // Register ModBlocks first to establish baseline registrations
        // This ensures that blocks like OFFERING_POT are registered first
        // ModBlocks.registerBlocks();

        // Then register specialized block registries
        // Note: We've modified PotteryBlocks.java to avoid re-registering OFFERING_POT
        com.ancientcurse.block.registry.BlockRegistry.registerAll();
        com.ancientcurse.block.registry.BlockRegistry.registerBlockItems();

        // Register flammable blocks (must be after all blocks are registered)
        ModBlocks.registerFlammableBlocks();

        // Register mod items
        // ModBlocks.registerBlocks(); // REMOVED: Blocks are now registered through
        // BlockRegistry system
        ModItems.registerItems();
        ModSounds.registerSounds();

        // Register mod particles
        ModParticles.registerParticles();

        // Register mod entities
        ModEntities.registerEntities();

        // Register mod block entities
        ModBlockEntities.registerBlockEntities();

        // Register mod screen handlers
        // ModScreenHandlers.registerScreenHandlers();

        // Register mod entities
        ModEntities.registerEntities();

        // Register mod commands
        ModCommands.registerCommands();

        // Register structures
        ModStructures.registerStructures();

        // Register custom features
        com.ancientcurse.world.gen.ModFeatures.registerFeatures();

        // Register status effects
        ModStatusEffects.registerStatusEffects();

        // Register server tick event for tornado management and locust swarms
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            com.ancientcurse.effect.TornadoManager.tick(server);
            LocusSwarmCommand.tickSwarm();
            com.ancientcurse.system.CurseZoneEffectHandler.tick(server);

            // Apply Jackel Binds effects to bound players and tick breakout manager
            long currentTime = server.getOverworld().getTime();
            com.ancientcurse.system.JackelBindsBreakoutManager.getInstance().tick(currentTime);
            for (var player : server.getPlayerManager().getPlayerList()) {
                com.ancientcurse.item.armor.JackelBindsItem.applyBindEffects(player);
            }
        });

        // Register player join handler for curse zone syncing
        PlayerJoinHandler.register();

        // Register player disconnect handler to clean up breakout progress and Zulmak
        // grabs
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            com.ancientcurse.system.JackelBindsBreakoutManager.getInstance().resetProgress(handler.player.getUuid());

            // Release player from any Zulmak grab on disconnect
            com.ancientcurse.entity.ZulmakEntity.releaseGrabbedPlayer(handler.player);
        });

        // Register path creation handler for sand/sandstone paths
        PathCreationHandler.register();

        // Register animation debug handler for debug stick
        AnimationDebugHandler.register();

        // Register water interaction handler for water skin refilling
        WaterInteractionHandler.register();
    }

    /**
     * Registers all world generation components
     */
    private void registerWorldgenComponents() {
        // Enable world presets only - this will show the button
        com.ancientcurse.world.ModWorldPresets.register();

        LOGGER.info("World presets registered - Ancient Curse world type available");

        com.ancientcurse.world.biome.ModBiomes.registerBiomes();
        // TEMPORARILY DISABLED: com.ancientcurse.world.ModChunkGenerators.register();
    }

    /**
     * Register commands for the mod
     */
    private void registerCommands() {
        LOGGER.info("Registering commands for " + MOD_ID);

        // Register the Locus Swarm command
        CommandRegistrationCallback.EVENT.register(LocusSwarmCommand::register);

        // Register the Cursed Earth command
        CommandRegistrationCallback.EVENT.register(CursedEarthCommand::register);

        // Register plague commands
        CommandRegistrationCallback.EVENT.register(com.ancientcurse.command.SandstormCommand::register);
        CommandRegistrationCallback.EVENT.register(com.ancientcurse.command.BloodWaterCommand::register);
        CommandRegistrationCallback.EVENT.register(com.ancientcurse.command.DarknessCommand::register);
        CommandRegistrationCallback.EVENT.register(com.ancientcurse.command.FamineCommand::register);
        CommandRegistrationCallback.EVENT.register(com.ancientcurse.command.PlagueClearCommand::register);
    }
}