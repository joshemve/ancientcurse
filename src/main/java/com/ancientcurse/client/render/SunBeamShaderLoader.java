package com.ancientcurse.client.render;

import com.ancientcurse.AncientCurse;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Handles loading of the custom sun beam shader.
 *
 * The shader files are located at:
 * - assets/ancientcurse/shaders/core/rendertype_sun_beam.json
 * - assets/ancientcurse/shaders/core/rendertype_sun_beam.vsh
 * - assets/ancientcurse/shaders/core/rendertype_sun_beam.fsh
 */
public class SunBeamShaderLoader {

    private static final Identifier SHADER_ID = new Identifier(AncientCurse.MOD_ID, "rendertype_sun_beam");

    @Nullable
    private static ShaderProgram sunBeamShader = null;
    private static boolean registrationAttempted = false;
    private static boolean loadFailed = false;

    /**
     * Register the shader with Fabric's shader system.
     * Call this from your client mod initializer.
     */
    public static void register() {
        if (registrationAttempted) {
            return;
        }
        registrationAttempted = true;

        CoreShaderRegistrationCallback.EVENT.register(context -> {
            try {
                context.register(
                        SHADER_ID,
                        VertexFormats.POSITION_COLOR_TEXTURE,
                        program -> {
                            sunBeamShader = program;
                            AncientCurse.LOGGER.info("Successfully loaded sun beam shader: {}", SHADER_ID);
                        }
                );
            } catch (Exception e) {
                loadFailed = true;
                AncientCurse.LOGGER.error("Failed to register sun beam shader: {}", e.getMessage());
            }
        });
    }

    /**
     * Get the loaded shader program.
     * Returns null if the shader hasn't loaded or failed to load.
     */
    @Nullable
    public static ShaderProgram getSunBeamShader() {
        return sunBeamShader;
    }

    /**
     * Check if the shader is loaded and ready to use.
     */
    public static boolean isShaderLoaded() {
        return sunBeamShader != null && !loadFailed;
    }

    /**
     * Check if the shader failed to load.
     */
    public static boolean didLoadFail() {
        return loadFailed;
    }
}
