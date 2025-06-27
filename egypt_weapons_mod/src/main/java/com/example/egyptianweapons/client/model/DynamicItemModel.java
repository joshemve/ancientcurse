package com.example.egyptianweapons.client.model;

import com.example.egyptianweapons.EgyptianWeapons;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

import java.io.InputStreamReader;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A dynamic model class that supports runtime model loading and caching.
 * This class provides optimized resource management and caching for GeckoLib models.
 *
 * @param <T> The type of the animatable item
 */
public class DynamicItemModel<T extends GeoAnimatable> extends GeoModel<T> {
    private static final Map<String, JsonObject> DISPLAY_SETTINGS_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, JsonObject> BLOCKBENCH_SETTINGS_CACHE = new ConcurrentHashMap<>();
    private static final Gson GSON = new Gson();

    private final String modelName;
    private final Identifier modelId;
    private final Identifier textureId;
    private final Identifier animationId;

    public DynamicItemModel(String modelName) {
        this.modelName = modelName;
        this.modelId = new Identifier(EgyptianWeapons.MOD_ID, "geo/" + modelName + ".geo.json");
        this.textureId = new Identifier(EgyptianWeapons.MOD_ID, "textures/item/" + modelName + ".png");
        this.animationId = new Identifier(EgyptianWeapons.MOD_ID, "animations/" + modelName + ".animation.json");
        loadDisplaySettings();
    }

    private void loadDisplaySettings() {
        try {
            String cacheKey = modelName + "_display";
            if (!DISPLAY_SETTINGS_CACHE.containsKey(cacheKey)) {
                Identifier settingsId = new Identifier(EgyptianWeapons.MOD_ID, "display_settings/" + modelName + ".json");
                var resource = this.getClass().getClassLoader().getResourceAsStream(
                    "assets/" + settingsId.getNamespace() + "/" + settingsId.getPath()
                );
                
                if (resource != null) {
                    JsonObject rawSettings = GSON.fromJson(new InputStreamReader(resource), JsonObject.class);
                    
                    if (rawSettings.has("display")) {
                        BLOCKBENCH_SETTINGS_CACHE.put(cacheKey, rawSettings.getAsJsonObject("display"));
                        EgyptianWeapons.LOGGER.info("Cached Blockbench display settings for " + modelName);
                    } else {
                        DISPLAY_SETTINGS_CACHE.put(cacheKey, rawSettings);
                        EgyptianWeapons.LOGGER.info("Cached custom display settings for " + modelName);
                    }
                }
            }
        } catch (Exception e) {
            EgyptianWeapons.LOGGER.debug("No display settings found for " + modelName + ": " + e.getMessage());
        }
    }

    public Optional<JsonObject> getDisplaySettings() {
        return Optional.ofNullable(DISPLAY_SETTINGS_CACHE.get(modelName + "_display"));
    }
    
    public Optional<JsonObject> getBlockbenchDisplaySettings() {
        return Optional.ofNullable(BLOCKBENCH_SETTINGS_CACHE.get(modelName + "_display"));
    }

    @Override
    public Identifier getModelResource(T animatable) {
        return modelId;
    }

    @Override
    public Identifier getTextureResource(T animatable) {
        return textureId;
    }

    @Override
    public Identifier getAnimationResource(T animatable) {
        return animationId;
    }

    /**
     * Clears all cached settings.
     * This should be called during resource reloading to prevent memory leaks.
     */
    public static void clearCache() {
        DISPLAY_SETTINGS_CACHE.clear();
        BLOCKBENCH_SETTINGS_CACHE.clear();
    }
}
