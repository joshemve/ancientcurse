package com.ancientcurse.client;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.ModBlocks;
import com.ancientcurse.ModEntities;
import com.ancientcurse.ModItems;
import com.ancientcurse.block.registry.PotteryBlocks;
import com.ancientcurse.client.color.RockColorProvider;
import com.ancientcurse.client.render.WitheredPharaohRenderer;
import com.ancientcurse.entity.renderer.DjeserhathEntityRenderer;
import com.ancientcurse.entity.renderer.SpitBallRenderer;
import com.ancientcurse.util.TooltipHelper;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.RenderLayer;

/**
 * Client-side initialization for the Ancient Curse mod.
 * Handles client-specific features and block rendering.
 * 
 * IMPORTANT IMPLEMENTATION NOTES:
 * 
 * 1. SINGLE CLIENT INITIALIZER:
 *    - This is the ONLY client initializer class for the mod
 *    - Previously, there was a duplicate AncientCurseClient in the root package
 *    - Having multiple client initializers caused transparency issues and duplicate tooltips
 * 
 * 2. RENDER LAYER REGISTRATION:
 *    - All transparent blocks must be registered with the appropriate render layer
 *    - Use BlockRenderLayerMap.INSTANCE.putBlock(block, RenderLayer.getCutout())
 *    - For plant blocks, use CursedPlantRenderLayer.register() to register all cursed plants at once
 *    - NEVER register the same block for render layers in multiple places
 * 
 * 3. BLOCK PROPERTIES:
 *    - For transparent blocks, use .nonOpaque() AND .notSolid() in block settings
 *    - Both properties are required for proper transparency rendering
 * 
 * 4. TOOLTIP REGISTRATION:
 *    - TooltipHelper.registerTooltipCallback() should only be called once
 *    - This prevents duplicate "Ancient Curse" tags appearing in tooltips
 */
@Environment(EnvType.CLIENT)
public class AncientCurseClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        AncientCurse.LOGGER.info("Initializing Ancient Curse Client");
        
        // Register render layers for transparent blocks
        registerRenderLayers();
        
        // Register color providers
        registerColorProviders();
        
        // Register the Khamsin Curse HUD renderer
        KhamsinCurseHudRenderer.register();
        
        // Register render layers for cursed plants
        CursedPlantRenderLayer.register();
        
        // Register transparency handling for the Eternal Sigil
        // This ensures the transparent parts of the texture are properly rendered
        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
            // Return -1 for no tinting (allows transparency to work correctly)
            return -1;
        }, ModItems.ETERNAL_SIGIL);
        
        // Register entity renderers
        registerEntityRenderers();
        
        // Register tooltip callback to add "Ancient Curse" to all mod items
        TooltipHelper.registerTooltipCallback();
        
        AncientCurse.LOGGER.info("Ancient Curse Client initialized");
    }
    
    /**
     * Register entity renderers for the mod
     */
    private void registerEntityRenderers() {
        AncientCurse.LOGGER.info("Registering entity renderers for " + AncientCurse.MOD_ID);
        
        // Register the Withered Pharaoh renderer
        EntityRendererRegistry.register(ModEntities.WITHERED_PHARAOH, WitheredPharaohRenderer::new);
        
        // Register the Djeserhath renderer
        EntityRendererRegistry.register(ModEntities.DJESERHATH, DjeserhathEntityRenderer::new);
        
        // Register the SpitBall projectile renderer
        EntityRendererRegistry.register(ModEntities.SPIT_BALL, SpitBallRenderer::new);
    }
    
    /**
     * Register render layers for blocks that need special rendering
     */
    private void registerRenderLayers() {
        // Register cutout render layers for blocks with transparency
        
        // Add vegetation blocks with transparency here as needed
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.NILE_RIVER_GRASS, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.NILE_RIVER_TALL_GRASS, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.PAPYRUS_REED, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DEAD_PAPYRUS_REED, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DWARF_PAPYRUS, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.EGYPTIAN_SPINACH, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.EUPHORBIA_HELIOSCOPIA, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.LIGHT_DEAD_FERN, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.MINI_CACTUS, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.PISTIA_STRATIOTES, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.LOTUS_FLOWER_PAD, RenderLayer.getCutout());
        
        // Register jar blocks with cutout render layer for proper transparency
        BlockRenderLayerMap.INSTANCE.putBlock(PotteryBlocks.CANOPIC_URN_OF_BASTET, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(PotteryBlocks.SCARAB_SEALED_URN, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(PotteryBlocks.PHARAOHS_INCENSE_JAR, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(PotteryBlocks.SERPENT_VESSEL_OF_WADJET, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(PotteryBlocks.VESSEL_OF_WHISPERING_WINDS, RenderLayer.getCutout());
        
        // Cursed plant blocks are registered in CursedPlantRenderLayer.register()
        // which is called in onInitializeClient() - do not register them again here
    }
    
    /**
     * Register color providers for blocks that need dynamic coloring
     */
    private void registerColorProviders() {
        // Create color provider instances
        RockColorProvider rockColorProvider = new RockColorProvider();
        
        // Register block color providers
        ColorProviderRegistry.BLOCK.register(rockColorProvider, 
            ModBlocks.SMALL_ROCK, 
            ModBlocks.MEDIUM_ROCK,
            ModBlocks.LARGE_ROCK
        );
        
        // Register item color providers
        ColorProviderRegistry.ITEM.register(rockColorProvider,
            ModBlocks.SMALL_ROCK,
            ModBlocks.MEDIUM_ROCK,
            ModBlocks.LARGE_ROCK
        );
    }
}
