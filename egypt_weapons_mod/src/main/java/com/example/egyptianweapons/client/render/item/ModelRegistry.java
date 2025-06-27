package com.example.egyptianweapons.client.render.item;

import com.example.egyptianweapons.EgyptianWeapons;
import com.example.egyptianweapons.registry.ItemRegistry;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.util.math.RotationAxis;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.io.InputStreamReader;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for managing model registration and rendering.
 * This class handles the registration of models and custom renderers with optimized caching
 * and efficient resource management.
 */
public class ModelRegistry {
    // Map to store renderers by item
    private static final Map<Item, GeoItemRenderer<?>> RENDERERS = new HashMap<>();
    
    // Map to store display settings by item
    private static final Map<Item, JsonObject> DISPLAY_SETTINGS = new HashMap<>();
    
    /**
     * Register a model for an item with GeoLib integration.
     * 
     * @param item The item to register the model for
     * @param renderer The renderer to use for the item
     * @param displaySettingsPath The path to the display settings JSON file
     */
    public static <T extends Item & GeoAnimatable> void registerModel(T item, GeoItemRenderer<?> renderer, String displaySettingsPath) {
        EgyptianWeapons.LOGGER.info("Registering model for: " + item.getClass().getSimpleName());
        
        try {
            // Store the renderer
            RENDERERS.put(item, renderer);
            
            // Load display settings
            JsonObject displaySettings = loadDisplaySettings(displaySettingsPath);
            DISPLAY_SETTINGS.put(item, displaySettings);
            
            // Create the renderer consumer
            BuiltinItemRendererRegistry.DynamicItemRenderer itemRenderer = (stack, mode, matrices, vertexConsumers, light, overlay) -> {
                try {
                    matrices.push();
                    
                    // Apply transformations based on display settings and mode
                    applyModelTransformations(matrices, mode, displaySettings);
                    
                    // Render the model
                    renderer.render(stack, mode, matrices, vertexConsumers, light, overlay);
                    
                    matrices.pop();
                } catch (Exception e) {
                    EgyptianWeapons.LOGGER.error("Error rendering model: " + e.getMessage(), e);
                    matrices.pop(); // Ensure matrix stack is balanced
                }
            };
            
            // Register the item renderer
            BuiltinItemRendererRegistry.INSTANCE.register(item, itemRenderer);
            
            EgyptianWeapons.LOGGER.info("Successfully registered model for: " + item.getClass().getSimpleName());
        } catch (Exception e) {
            EgyptianWeapons.LOGGER.error("Error registering model: " + e.getMessage(), e);
        }
    }
    
    /**
     * Load display settings from file.
     * 
     * @param path The path to the display settings JSON file
     * @return The display settings as a JsonObject
     */
    private static JsonObject loadDisplaySettings(String path) {
        JsonObject result = new JsonObject();
        
        try {
            String resourcePath = "assets/" + EgyptianWeapons.MOD_ID + "/display_settings/" + path + ".json";
            ClassLoader classLoader = ModelRegistry.class.getClassLoader();
            
            try (InputStream inputStream = classLoader.getResourceAsStream(resourcePath)) {
                if (inputStream == null) {
                    EgyptianWeapons.LOGGER.warn("Display settings file not found: " + resourcePath);
                    return result;
                }
                
                try (InputStreamReader reader = new InputStreamReader(inputStream)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    if (json.has("display")) {
                        result = json.getAsJsonObject("display");
                        EgyptianWeapons.LOGGER.info("Successfully loaded display settings from: " + resourcePath);
                    } else {
                        EgyptianWeapons.LOGGER.warn("No display settings found in file: " + resourcePath);
                    }
                }
            }
        } catch (Exception e) {
            EgyptianWeapons.LOGGER.error("Error loading display settings from file " + path + ": " + e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     * Apply model transformations based on display settings.
     * 
     * @param matrices The matrix stack to apply transformations to
     * @param mode The current transformation mode
     * @param settings The display settings
     */
    private static void applyModelTransformations(MatrixStack matrices, 
                                                ModelTransformationMode mode, 
                                                JsonObject settings) {
        try {
            if (settings == null || settings.entrySet().isEmpty()) {
                return;
            }
            
            String modeKey = getModeKey(mode);
            if (!settings.has(modeKey)) {
                return;
            }
            
            JsonObject transformations = settings.getAsJsonObject(modeKey);
            
            // Apply rotation
            if (transformations.has("rotation")) {
                JsonElement rotationElement = transformations.get("rotation");
                if (rotationElement.isJsonArray()) {
                    JsonArray rotation = rotationElement.getAsJsonArray();
                    if (rotation.size() >= 3) {
                        float x = rotation.get(0).getAsFloat();
                        float y = rotation.get(1).getAsFloat();
                        float z = rotation.get(2).getAsFloat();
                        
                        if (x != 0) matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(x));
                        if (y != 0) matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(y));
                        if (z != 0) matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(z));
                    }
                }
            }
            
            // Apply translation
            if (transformations.has("translation")) {
                JsonElement translationElement = transformations.get("translation");
                if (translationElement.isJsonArray()) {
                    JsonArray translation = translationElement.getAsJsonArray();
                    if (translation.size() >= 3) {
                        float x = translation.get(0).getAsFloat() / 16.0f; // Convert from pixels to blocks
                        float y = translation.get(1).getAsFloat() / 16.0f;
                        float z = translation.get(2).getAsFloat() / 16.0f;
                        
                        matrices.translate(x, y, z);
                    }
                }
            }
            
            // Apply scale
            if (transformations.has("scale")) {
                JsonElement scaleElement = transformations.get("scale");
                if (scaleElement.isJsonArray()) {
                    JsonArray scale = scaleElement.getAsJsonArray();
                    if (scale.size() >= 3) {
                        float x = scale.get(0).getAsFloat();
                        float y = scale.get(1).getAsFloat();
                        float z = scale.get(2).getAsFloat();
                        
                        matrices.scale(x, y, z);
                    }
                }
            }
        } catch (Exception e) {
            EgyptianWeapons.LOGGER.error("Error applying transformations: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get the transform mode key for json lookup.
     * 
     * @param mode The transformation mode
     * @return The key to use for json lookup
     */
    private static String getModeKey(ModelTransformationMode mode) {
        return switch (mode) {
            case FIRST_PERSON_LEFT_HAND -> "firstperson_lefthand";
            case FIRST_PERSON_RIGHT_HAND -> "firstperson_righthand";
            case THIRD_PERSON_LEFT_HAND -> "thirdperson_lefthand";
            case THIRD_PERSON_RIGHT_HAND -> "thirdperson_righthand";
            case HEAD -> "head";
            case GUI -> "gui";
            case GROUND -> "ground";
            case FIXED -> "fixed";
            default -> "none";
        };
    }
} 