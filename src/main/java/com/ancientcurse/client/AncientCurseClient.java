package com.ancientcurse.client;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.ModEntities;
import com.ancientcurse.ModItems;
import com.ancientcurse.ModBlocks;
import com.ancientcurse.ModBlockEntities;
import com.ancientcurse.ModParticles;
import com.ancientcurse.block.registry.PotteryBlocks;
import com.ancientcurse.client.model.*;
import com.ancientcurse.client.particle.ZulmakParticle;
import com.ancientcurse.client.render.*;
import com.ancientcurse.client.render.entity.*;
import com.ancientcurse.block.entity.SekhemCactusBlockEntity;
import com.ancientcurse.client.renderer.block.SekhemCactusRenderer;
import com.ancientcurse.client.renderer.block.SolarSpireRenderer;
import com.ancientcurse.client.render.item.SerpentStaffRenderer;
import com.ancientcurse.client.color.RockColorProvider;
import com.ancientcurse.entity.model.*;
import com.ancientcurse.entity.renderer.*;
import com.ancientcurse.entity.renderer.ZulmakRenderer;
import com.ancientcurse.network.CurseZonePackets;
import com.ancientcurse.client.animation.ModAnimations;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
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
 * - This is the ONLY client initializer for the mod
 * - All client-side registrations must be done here or in methods called from
 * here
 * - NEVER create multiple classes implementing ClientModInitializer
 * 
 * 2. RENDER LAYER REGISTRATION:
 * - Transparent blocks must be registered with BlockRenderLayerMap
 * - Each block should only be registered ONCE
 * - NEVER register the same block for render layers in multiple places
 * 
 * 3. BLOCK PROPERTIES:
 * - For transparent blocks, use .nonOpaque() AND .notSolid() in block settings
 * - Both properties are required for proper transparency rendering
 * 
 * 4. TOOLTIP REGISTRATION:
 * - TooltipHelper.registerTooltipCallback() should only be called once
 * - This prevents duplicate "Ancient Curse" tags appearing in tooltips
 */
@Environment(EnvType.CLIENT)
public class AncientCurseClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        AncientCurse.LOGGER.info("Initializing Ancient Curse Client");

        // Register entity renderers FIRST before anything else
        registerEntityRenderers();

        // Register fluid renderers
        registerFluidRenderers();

        // Register block entity renderers
        registerBlockEntityRenderers();

        // Register particle factories
        registerParticleFactories();

        // Register render layers for transparent blocks
        registerRenderLayers();

        // Register color providers
        registerColorProviders();

        // Register the Khamsin Curse HUD renderer
        KhamsinCurseHudRenderer.register();

        // Register the Ankh Counter HUD renderer
        AnkhCounterHudRenderer.register();

        // Register the Jackel Binds HUD renderer
        JackelBindsHudRenderer.register();

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

        // Register tooltip callback to add "Ancient Curse" to all mod items
        // TooltipHelper.registerTooltipCallback(); - Removed, Minecraft already shows
        // mod ID

        // Register network packets
        CurseZonePackets.registerClientPackets();

        // Register player animations (PlayerAnimator auto-loads from player_animation folder)
        ModAnimations.register();

        // Note: Player animation layers are registered via PlayerAnimationMixin
        // which adds a ModifierLayer to each AbstractClientPlayerEntity

        // Register disconnect handler to clear client cache
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            CurseZoneClientCache.clear();
        });

        // Register world render events for curse zone visualization
        WorldRenderEvents.LAST.register((context) -> {
            CurseZoneRenderer.renderZones(
                    context.matrixStack(),
                    context.camera(),
                    context.tickDelta());

            // Render wand selection boxes
            WandSelectionRenderer.renderSelection(
                    context.matrixStack(),
                    context.camera(),
                    context.tickDelta());
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

        // Register the Zulmak renderer
        EntityRendererRegistry.register(ModEntities.ZULMAK, ZulmakRenderer::new);

        // Register the Ra renderer
        EntityRendererRegistry.register(ModEntities.RA, RaRenderer::new);
    }

    /**
     * Register fluid renderers for the mod
     */
    private void registerFluidRenderers() {
        net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry.INSTANCE.register(
                com.ancientcurse.registry.ModFluids.STILL_SOUL_LAVA,
                com.ancientcurse.registry.ModFluids.FLOWING_SOUL_LAVA,
                new net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler(
                        new Identifier(AncientCurse.MOD_ID, "block/soul_lava_still"),
                        new Identifier(AncientCurse.MOD_ID, "block/soul_lava_flow")));

        BlockRenderLayerMap.INSTANCE.putFluid(com.ancientcurse.registry.ModFluids.STILL_SOUL_LAVA,
                RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putFluid(com.ancientcurse.registry.ModFluids.FLOWING_SOUL_LAVA,
                RenderLayer.getTranslucent());
    }

    /**
     * Register block entity renderers for the mod
     */
    private void registerBlockEntityRenderers() {
        AncientCurse.LOGGER.info("Registering block entity renderers for " + AncientCurse.MOD_ID);

        // Register the Solar Spire renderer
        BlockEntityRendererRegistry.register(ModBlockEntities.SOLAR_SPIRE, SolarSpireRenderer::new);
        BlockEntityRendererRegistry.register(ModBlockEntities.SEKHEM_CACTUS_BLOCK_ENTITY, SekhemCactusRenderer::new);
    }

    /**
     * Register particle factories for custom particles
     */
    private void registerParticleFactories() {
        AncientCurse.LOGGER.info("Registering particle factories for " + AncientCurse.MOD_ID);

        // Register Zulmak particle factory - spinning particles around Zulmak's blocks
        // (purple)
        ParticleFactoryRegistry.getInstance().register(ModParticles.ZULMAK_PARTICLE, ZulmakParticle.Factory::new);

        // Register Zulmak shield particle factory - cyan/teal particles when blocking
        ParticleFactoryRegistry.getInstance().register(ModParticles.ZULMAK_SHIELD_PARTICLE,
                ZulmakParticle.Factory::new);

        AncientCurse.LOGGER.info("Particle factories registered");
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
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DESERT_SHRUB, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DEAD_DESERT_SHRUB, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DESHRET_BRUSH, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DESHRET_THORN_BUSH, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DESERT_CACTUS_PLANT, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.SOULBLOOM_BUSH, RenderLayer.getCutout());

        // Register crop blocks with transparency
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BARLEY, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.FLAX, RenderLayer.getCutout());

        // Register salt dust with transparency
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.SALT_DUST, RenderLayer.getCutout());

        // Register pitfall trap with transparency
        BlockRenderLayerMap.INSTANCE.putBlock(com.ancientcurse.block.registry.ConstructionBlocks.PITFALL_TRAP,
                RenderLayer.getCutout());

        // Register spike with transparency
        BlockRenderLayerMap.INSTANCE.putBlock(com.ancientcurse.block.registry.ConstructionBlocks.SPIKE,
                RenderLayer.getCutout());

        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.PISTIA_STRATIOTES, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.LOTUS_FLOWER_PAD, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BLOOD_LOTUS, RenderLayer.getCutout());

        // Date palm visuals
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DATE_BLOCK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DATE_PALM_SAPLING, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DATE_PALM_LEAVES, RenderLayer.getCutoutMipped());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.SYCAMORE_LEAVES, RenderLayer.getCutoutMipped());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.SYCAMORE_SAPLING, RenderLayer.getCutout());

        // Register torch transparency
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.SANDSTONE_TORCH, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.WALL_SANDSTONE_TORCH, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BLACK_STONE_TORCH, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.WALL_BLACK_STONE_TORCH, RenderLayer.getCutout());

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
        // Register rock color providers for dynamic coloring based on block below
        RockColorProvider rockColorProvider = new RockColorProvider();

        // Register for blocks
        ColorProviderRegistry.BLOCK.register(rockColorProvider,
                ModBlocks.SMALL_ROCK,
                ModBlocks.MEDIUM_ROCK,
                ModBlocks.LARGE_ROCK);

        // Register for items (shows gray in inventory)
        ColorProviderRegistry.ITEM.register(rockColorProvider,
                ModBlocks.SMALL_ROCK,
                ModBlocks.MEDIUM_ROCK,
                ModBlocks.LARGE_ROCK);

        // Override spawn egg tinting for boss eggs - use white (no tint) so custom
        // textures
        // show correctly
        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> -1,
                ModItems.ZULMAK_SPAWN_EGG,
                ModItems.ANUBIS_SPAWN_EGG,
                ModItems.THOTH_SPAWN_EGG);

        AncientCurse.LOGGER.info("Registered color providers");
    }
}
