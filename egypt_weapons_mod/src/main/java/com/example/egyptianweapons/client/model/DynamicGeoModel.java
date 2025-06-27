package com.example.egyptianweapons.client.model;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
import com.example.egyptianweapons.EgyptianWeapons;
import software.bernie.geckolib.core.animatable.GeoAnimatable;

import java.io.InputStreamReader;
import java.util.Optional;

public class DynamicGeoModel<T extends GeoAnimatable> extends GeoModel<T> {
    private final String modelName;
    private JsonObject displaySettings;
    private JsonObject blockbenchDisplaySettings;

    public DynamicGeoModel(String modelName) {
        this.modelName = modelName;
        loadDisplaySettings();
    }

    private void loadDisplaySettings() {
        try {
            Identifier settingsId = new Identifier(EgyptianWeapons.MOD_ID, "display_settings/" + modelName + ".json");
            var resource = this.getClass().getClassLoader().getResourceAsStream(
                "assets/" + settingsId.getNamespace() + "/" + settingsId.getPath()
            );
            
            if (resource != null) {
                Gson gson = new Gson();
                JsonObject rawSettings = gson.fromJson(new InputStreamReader(resource), JsonObject.class);
                
                // Check if this is a Blockbench format file (has "display" object)
                if (rawSettings.has("display")) {
                    blockbenchDisplaySettings = rawSettings.getAsJsonObject("display");
                    displaySettings = null; // We'll use blockbenchDisplaySettings instead
                    EgyptianWeapons.LOGGER.info("Loaded Blockbench display settings for " + modelName);
                } else {
                    displaySettings = rawSettings; // Assume it's our custom format
                    blockbenchDisplaySettings = null;
                    EgyptianWeapons.LOGGER.info("Loaded custom display settings for " + modelName);
                }
            }
        } catch (Exception e) {
            // Display settings are optional, so we just log it at debug level
            EgyptianWeapons.LOGGER.debug("No display settings found for " + modelName + ": " + e.getMessage());
        }
    }

    public Optional<JsonObject> getDisplaySettings() {
        return Optional.ofNullable(displaySettings);
    }
    
    public Optional<JsonObject> getBlockbenchDisplaySettings() {
        return Optional.ofNullable(blockbenchDisplaySettings);
    }

    @Override
    public Identifier getModelResource(T animatable) {
        return new Identifier(EgyptianWeapons.MOD_ID, "geo/" + modelName + ".geo.json");
    }

    @Override
    public Identifier getTextureResource(T animatable) {
        return new Identifier(EgyptianWeapons.MOD_ID, "textures/item/" + modelName + ".png");
    }

    @Override
    public Identifier getAnimationResource(T animatable) {
        return new Identifier(EgyptianWeapons.MOD_ID, "animations/" + modelName + ".animation.json");
    }
}
