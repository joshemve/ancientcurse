package com.ancientcurse.client;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.ModEntities;
import com.ancientcurse.ModItems;
import com.ancientcurse.block.ModBlocks;
import com.ancientcurse.block.registry.PotteryBlocks;
import com.ancientcurse.client.model.*;
import com.ancientcurse.client.render.entity.*;
import com.ancientcurse.client.render.item.SerpentStaffRenderer;
import com.ancientcurse.entity.model.*;
import com.ancientcurse.entity.renderer.*;
import com.ancientcurse.network.CurseZonePackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * Client-side initialization for Ancient Curse mod.
 * 
 * IMPORTANT CLIENT ARCHITECTURE GUIDELINES:
 * 
 * 1. CLIENT INITIALIZATION:
 *    - This is the ONLY client initializer for the mod
 *    - All client-side registrations must be done here or in methods called from here
 *    - NEVER create multiple classes implementing ClientModInitializer
 * 
 * 2. RENDER LAYER REGISTRATION:
 *    - Transparent blocks must be registered with BlockRenderLayerMap
 *    - Each block should only be registered ONCE
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
        
        // Register the Ankh Counter HUD renderer
        AnkhCounterHudRenderer.register();
        
        // Register render layers for cursed plants
        CursedPlantRenderLayer.register();
        
        // Register transparency handling for the Eternal Sigil
        // This ensures the transparent parts of the texture are properly rendered
        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
            // Return -1 for no tinting (allows transparency to work correctly)
            return -1;
        }, ModItems.ETERNAL_SIGIL);
        
        // Register transparency handling for Pharaoh's Blood
        // This ensures the bottle/liquid transparency renders correctly
        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
            // Return -1 for no tinting (allows transparency to work correctly)
            return -1;
        }, ModItems.PHARAOHS_BLOOD);
        
        // Register entity renderers
        registerEntityRenderers();
        
        // Register tooltip callback to add "Ancient Curse" to all mod items
        // TooltipHelper.registerTooltipCallback(); - Removed, Minecraft already shows mod ID
        
        // Register network packets
        CurseZonePackets.registerClientPackets();
        
        // Register disconnect handler to clear client cache
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            CurseZoneClientCache.clear();
        });
        
        // Register world render events for curse zone visualization
        WorldRenderEvents.LAST.register((context) -> {
            CurseZoneRenderer.renderZones(
                context.matrixStack(),
                context.camera(),
                context.tickDelta()
            );
            
            // Render wand selection boxes
            WandSelectionRenderer.renderSelection(
                context.matrixStack(),
                context.camera(),
                context.tickDelta()
            );
        });
        
        AncientCurse.LOGGER.info("Ancient Curse Client initialized");
    }
    
    /**
     * Register entity renderers for the mod
     */
    private void registerEntityRenderers() {
        AncientCurse.LOGGER.info("Registering entity renderers for " + AncientCurse.MOD_ID);
        
        // Register the Withered Pharaoh renderer
        EntityRendererRegistry.register(ModEntities.WITHERED_PHARAOH, WitheredPharaohRenderer::new);
        
        // Register the Anubis renderer
        EntityRendererRegistry.register(ModEntities.ANUBIS, AnubisEntityRenderer::new);
        
        // Register the Djeserhath renderer
        EntityRendererRegistry.register(ModEntities.DJESERHATH, DjeserhathEntityRenderer::new);
        
        // Register the Locus renderer (correct naming to match asset files)
        EntityRendererRegistry.register(ModEntities.LOCUS, LocusRenderer::new);
        
        // Register the Baby Locus renderer (bug babies)
        EntityRendererRegistry.register(ModEntities.BABY_LOCUS, BabyLocusRenderer::new);
        
        // Register the Scarab Beetle renderer (ground-based beetle)
        EntityRendererRegistry.register(ModEntities.SCARAB_BEETLE, ScarabBeetleRenderer::new);
        
        // Register the Thoth renderer (Egyptian God boss)
        EntityRendererRegistry.register(ModEntities.THOTH, ThothRenderer::new);
        
        // Register the Khamsin Spread Small renderer (Floating mystical rock)
        EntityRendererRegistry.register(ModEntities.KHAMSIN_SPREAD_SMALL, KhamsinSpreadSmallRenderer::new);
        
        // Register the SpitBall projectile renderer
        EntityRendererRegistry.register(ModEntities.SPIT_BALL, SpitBallRenderer::new);
        
        // Register the SnakeHeadProjectile renderer
        EntityRendererRegistry.register(ModEntities.SNAKE_HEAD_PROJECTILE, SnakeHeadProjectileRenderer::new);

        // Register the Khamsin Orb renderer
        EntityRendererRegistry.register(ModEntities.KHAMSIN_ORB, KhamsinOrbRenderer::new);
        
        // Register the Thoth Magic Ball projectile renderer
        EntityRendererRegistry.register(ModEntities.THOTH_MAGIC_BALL, FlyingItemEntityRenderer::new);
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
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BLOOD_LOTUS, RenderLayer.getCutout());
        
        // Register jar blocks with cutout render layer for proper transparency
        BlockRenderLayerMap.INSTANCE.putBlock(PotteryBlocks.CANOPIC_URN_OF_BASTET, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(PotteryBlocks.SCARAB_SEALED_URN, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(PotteryBlocks.PHARAOHS_INCENSE_JAR, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(PotteryBlocks.SERPENT_VESSEL_OF_WADJET, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(PotteryBlocks.VESSEL_OF_WHISPERING_WINDS, RenderLayer.getCutout());
        
        // Cursed plant blocks are registered in CursedPlantRenderLayer.register()
        
        AncientCurse.LOGGER.info("Registered render layers for blocks");
    }
    
    /**
     * Register color providers for blocks and items
     */
    private void registerColorProviders() {
        // Rock color providers are registered in RockColorProvider
        RockColorProvider.register();
        
        AncientCurse.LOGGER.info("Registered color providers");
    }
}