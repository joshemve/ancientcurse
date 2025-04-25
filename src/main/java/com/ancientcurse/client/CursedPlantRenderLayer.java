package com.ancientcurse.client;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.block.registry.CursedPlantBlocks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;

/**
 * Client-side class to set the render layer for cursed plants
 * This ensures transparent textures render correctly
 */
@Environment(EnvType.CLIENT)
public class CursedPlantRenderLayer {
    
    /**
     * Register all cursed plants to use the CUTOUT render layer
     * This is required for transparent textures to render correctly
     */
    public static void register() {
        // Set all cursed plant blocks to use the CUTOUT render layer
        BlockRenderLayerMap.INSTANCE.putBlock(CursedPlantBlocks.CURSED_SPRIG, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(CursedPlantBlocks.CURSED_SPROUT, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(CursedPlantBlocks.DUAT_FERN, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(CursedPlantBlocks.VINE_OF_APEP, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(CursedPlantBlocks.BLOODSHADE_THICKET, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(CursedPlantBlocks.DUAMUTEF_CAP, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(CursedPlantBlocks.ISFET_FROND, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(CursedPlantBlocks.ISFET_SHRUB, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(CursedPlantBlocks.KHEMNU_POD, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(CursedPlantBlocks.KHERU_MOSS, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(CursedPlantBlocks.MENFET_SPRIG, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(CursedPlantBlocks.REED_OF_SEKHEM, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(CursedPlantBlocks.SUTEKH_COIL, RenderLayer.getCutout());
        
        AncientCurse.LOGGER.info("Registered render layers for cursed plants");
    }
}
