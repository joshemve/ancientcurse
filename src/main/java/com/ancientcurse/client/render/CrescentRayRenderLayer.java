package com.ancientcurse.client.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple RenderLayer for crescent ray textures.
 * Uses additive blending for glowing effect without custom shader distortions.
 */
public class CrescentRayRenderLayer extends RenderPhase {

    // Cache render layers to avoid creating new ones every frame
    private static final Map<Identifier, RenderLayer> LAYER_CACHE = new HashMap<>();

    private CrescentRayRenderLayer(String name, Runnable beginAction, Runnable endAction) {
        super(name, beginAction, endAction);
    }

    /**
     * Get a render layer for crescent ray textures with additive blending.
     * This creates a glowing effect where the texture adds light to the scene.
     * Cached to avoid creating new layers every frame.
     */
    public static RenderLayer getAdditiveCrescentLayer(Identifier texture) {
        return LAYER_CACHE.computeIfAbsent(texture, tex -> {
            return RenderLayer.of(
                    "ancientcurse_crescent_" + tex.getPath().replace("/", "_"),
                    VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                    VertexFormat.DrawMode.QUADS,
                    256,
                    false, // affectsCrumbling
                    true,  // sortOnUpload (for transparency)
                    RenderLayer.MultiPhaseParameters.builder()
                            .program(ENTITY_TRANSLUCENT_PROGRAM)
                            .texture(new Texture(tex, false, false))
                            .transparency(ADDITIVE_TRANSPARENCY)
                            .cull(DISABLE_CULLING)
                            .lightmap(DISABLE_LIGHTMAP)
                            .overlay(DISABLE_OVERLAY_COLOR)
                            .depthTest(LEQUAL_DEPTH_TEST)
                            .build(false)
            );
        });
    }
}
