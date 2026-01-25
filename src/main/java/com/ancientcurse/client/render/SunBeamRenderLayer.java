package com.ancientcurse.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

/**
 * Custom RenderLayer for Ra's Sun Beam effect.
 * Uses the custom GLSL shader at assets/ancientcurse/shaders/core/rendertype_sun_beam
 *
 * Features provided by the shader:
 * - Animated energy flow
 * - Divine sparkle effect
 * - Radial glow with bloom
 * - Golden edge rim
 * - Distance-based intensity
 * - Fog piercing (divine light partially ignores fog)
 */
public class SunBeamRenderLayer extends RenderPhase {

    private static final Identifier SUN_BEAM_TEXTURE = new Identifier("textures/entity/beacon_beam.png");

    // Custom shader reference - points to our GLSL shader
    private static final ShaderProgram SUN_BEAM_SHADER = new ShaderProgram(
            () -> SunBeamShaderLoader.getSunBeamShader()
    );

    // The actual RenderLayer that uses our custom shader
    private static final RenderLayer SUN_BEAM_LAYER = RenderLayer.of(
            "ancientcurse_sun_beam",
            VertexFormats.POSITION_COLOR_TEXTURE,
            VertexFormat.DrawMode.QUADS,
            256,
            false, // affectsCrumbling
            true,  // sortOnUpload (transparency)
            RenderLayer.MultiPhaseParameters.builder()
                    .program(SUN_BEAM_SHADER)
                    .texture(new Texture(SUN_BEAM_TEXTURE, false, false))
                    .transparency(ADDITIVE_TRANSPARENCY)
                    .writeMaskState(COLOR_MASK)
                    .cull(DISABLE_CULLING)
                    .lightmap(DISABLE_LIGHTMAP)
                    .depthTest(LEQUAL_DEPTH_TEST)
                    .build(false)
    );

    // Fallback layer using vanilla beacon beam (if shader fails to load)
    private static final RenderLayer FALLBACK_LAYER = RenderLayer.getBeaconBeam(SUN_BEAM_TEXTURE, true);

    private SunBeamRenderLayer(String name, Runnable beginAction, Runnable endAction) {
        super(name, beginAction, endAction);
    }

    /**
     * Get the sun beam render layer.
     * Returns the custom shader layer if available, otherwise falls back to vanilla beacon beam.
     */
    public static RenderLayer getSunBeamLayer() {
        if (SunBeamShaderLoader.isShaderLoaded()) {
            return SUN_BEAM_LAYER;
        }
        return FALLBACK_LAYER;
    }

    /**
     * Get the sun beam render layer with a custom texture.
     */
    public static RenderLayer getSunBeamLayer(Identifier texture) {
        if (SunBeamShaderLoader.isShaderLoaded()) {
            return RenderLayer.of(
                    "ancientcurse_sun_beam_custom",
                    VertexFormats.POSITION_COLOR_TEXTURE,
                    VertexFormat.DrawMode.QUADS,
                    256,
                    false,
                    true,
                    RenderLayer.MultiPhaseParameters.builder()
                            .program(SUN_BEAM_SHADER)
                            .texture(new Texture(texture, false, false))
                            .transparency(ADDITIVE_TRANSPARENCY)
                            .writeMaskState(COLOR_MASK)
                            .cull(DISABLE_CULLING)
                            .lightmap(DISABLE_LIGHTMAP)
                            .depthTest(LEQUAL_DEPTH_TEST)
                            .build(false)
            );
        }
        return RenderLayer.getBeaconBeam(texture, true);
    }
}
