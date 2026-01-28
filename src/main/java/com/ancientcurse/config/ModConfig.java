package com.ancientcurse.config;

import com.ancientcurse.AncientCurse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration manager for Ancient Curse mod.
 * Handles loading/saving config from JSON file and provides access to config values.
 *
 * Config is server-authoritative - clients receive config values from server on join.
 */
public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "ancientcurse.json";

    // Singleton instance
    private static ModConfig INSTANCE;

    // Config values with defaults
    private ConfigData data = new ConfigData();

    /**
     * Inner class holding all config values.
     * This is what gets serialized to/from JSON.
     */
    public static class ConfigData {
        // Thirst system settings
        public ThirstConfig thirst = new ThirstConfig();

        public static class ThirstConfig {
            /**
             * Multiplier for thirst drain speed.
             * 1.0 = normal speed, 0.5 = half speed (drains slower), 2.0 = double speed (drains faster)
             * Set to 0 to disable thirst drain entirely.
             */
            public float drainSpeedMultiplier = 1.0f;

            /**
             * Whether thirst system is enabled at all.
             * If false, thirst won't drain and no dehydration effects will apply.
             */
            public boolean enabled = true;

            /**
             * Multiplier for thirst drain in hot/desert biomes.
             * Applied on top of drainSpeedMultiplier.
             * 1.0 = normal desert penalty, 0.0 = no extra drain in deserts
             */
            public float desertDrainMultiplier = 1.0f;
        }
    }

    private ModConfig() {
        // Private constructor for singleton
    }

    /**
     * Get the config instance. Loads from file if not already loaded.
     */
    public static ModConfig get() {
        if (INSTANCE == null) {
            INSTANCE = new ModConfig();
            INSTANCE.load();
        }
        return INSTANCE;
    }

    /**
     * Get the current config data.
     */
    public ConfigData getData() {
        return data;
    }

    /**
     * Load config from file, or create default if it doesn't exist.
     */
    public void load() {
        Path configPath = getConfigPath();

        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                ConfigData loaded = GSON.fromJson(json, ConfigData.class);
                if (loaded != null) {
                    this.data = loaded;
                    AncientCurse.LOGGER.info("Loaded Ancient Curse config from {}", configPath);
                }
            } catch (IOException e) {
                AncientCurse.LOGGER.error("Failed to load config file, using defaults", e);
            } catch (Exception e) {
                AncientCurse.LOGGER.error("Failed to parse config file, using defaults", e);
            }
        } else {
            // Create default config file
            save();
            AncientCurse.LOGGER.info("Created default Ancient Curse config at {}", configPath);
        }

        // Validate and clamp values
        validateConfig();
    }

    /**
     * Save current config to file.
     */
    public void save() {
        Path configPath = getConfigPath();

        try {
            Files.createDirectories(configPath.getParent());
            String json = GSON.toJson(data);
            Files.writeString(configPath, json);
        } catch (IOException e) {
            AncientCurse.LOGGER.error("Failed to save config file", e);
        }
    }

    /**
     * Validate config values and clamp to acceptable ranges.
     */
    private void validateConfig() {
        // Clamp drain speed multiplier (0 to 10)
        data.thirst.drainSpeedMultiplier = Math.max(0.0f, Math.min(10.0f, data.thirst.drainSpeedMultiplier));
        data.thirst.desertDrainMultiplier = Math.max(0.0f, Math.min(10.0f, data.thirst.desertDrainMultiplier));
    }

    /**
     * Get the path to the config file.
     */
    private Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
    }

    // Convenience getters for common config values

    /**
     * Get the thirst drain speed multiplier.
     */
    public float getThirstDrainMultiplier() {
        return data.thirst.drainSpeedMultiplier;
    }

    /**
     * Check if thirst system is enabled.
     */
    public boolean isThirstEnabled() {
        return data.thirst.enabled;
    }

    /**
     * Get the desert drain multiplier.
     */
    public float getDesertDrainMultiplier() {
        return data.thirst.desertDrainMultiplier;
    }

    /**
     * Update config from server sync packet.
     * Called on client when receiving config from server.
     */
    public void updateFromServer(float drainMultiplier, boolean enabled, float desertMultiplier) {
        data.thirst.drainSpeedMultiplier = drainMultiplier;
        data.thirst.enabled = enabled;
        data.thirst.desertDrainMultiplier = desertMultiplier;
        AncientCurse.LOGGER.debug("Updated config from server: drainMultiplier={}, enabled={}, desertMultiplier={}",
            drainMultiplier, enabled, desertMultiplier);
    }
}
