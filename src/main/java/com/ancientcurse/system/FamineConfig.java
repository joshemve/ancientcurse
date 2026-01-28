package com.ancientcurse.system;

import com.ancientcurse.AncientCurse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration for the Famine event system.
 * Loaded from config file on server start, synced to clients.
 */
public class FamineConfig {

    private static FamineConfig INSTANCE;
    private static final String CONFIG_FILE = "ancientcurse_famine.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // === General Settings ===

    /** Speed multiplier for famine intensity fade in (default 1.0) */
    private float intensityFadeInSpeed = 1.0f;

    /** Speed multiplier for famine intensity fade out (default 1.0) */
    private float intensityFadeOutSpeed = 1.0f;

    /** Maximum intensity the famine can reach (0.0 to 1.0) */
    private float maxIntensity = 1.0f;

    // === Crop Death Settings ===

    /** Radius around players to check for crops (blocks) */
    private int cropDeathRadius = 64;

    /** Base chance per crop per check at full intensity */
    private float cropDeathChanceBase = 0.001f;

    /** Multiplier for crop death speed (higher = faster death) */
    private float cropDeathSpeedMultiplier = 1.0f;

    /** Maximum crops to kill per tick (performance) */
    private int maxCropDeathsPerTick = 5;

    /** Ticks between chunk scans for crops */
    private int cropChunkScanCooldown = 200;

    // === Food Spoilage Settings ===

    /** Ticks between spoilage checks (default 100 = 5 seconds) */
    private int spoilageCheckInterval = 100;

    /** Base chance per food stack per check at full intensity */
    private float spoilageChanceBase = 0.02f;

    /** Multiplier for spoilage speed (higher = faster spoilage) */
    private float spoilageSpeedMultiplier = 1.0f;

    /** Base ticks before food spoils at full intensity */
    private int baseSpoilageTicks = 6000;

    // === Animal Starvation Settings ===

    /** Whether animal starvation is enabled */
    private boolean animalStarvationEnabled = true;

    /** Radius around players to check for animals */
    private int animalStarvationRadius = 48;

    /** Rate at which starvation timer increases per check */
    private int animalStarvationRate = 10;

    /** Starvation timer threshold before damage is dealt */
    private int animalStarvationDamageThreshold = 1200;

    /** Damage dealt per starvation tick */
    private float animalStarvationDamage = 2.0f;

    /** Multiplier for animal starvation speed */
    private float animalStarvationSpeedMultiplier = 1.0f;

    // === Visual Settings (synced to client) ===

    /** Alpha multiplier for famine vignette (0.0 to 1.0) */
    private float vignetteAlphaMultiplier = 1.0f;

    /** Whether to show the famine timer on screen */
    private boolean showFamineTimer = true;

    /** Whether to show spoilage flash effect */
    private boolean showSpoilageFlash = true;

    // === Constructor ===

    public FamineConfig() {
        // Default values set above
    }

    // === Static Access ===

    /**
     * Get the singleton config instance
     */
    public static FamineConfig get() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    /**
     * Reload config from file
     */
    public static void reload() {
        INSTANCE = load();
        AncientCurse.LOGGER.info("Famine config reloaded");
    }

    /**
     * Load config from file, creating default if not exists
     */
    private static FamineConfig load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);

        if (Files.exists(configPath)) {
            try (Reader reader = new FileReader(configPath.toFile())) {
                FamineConfig config = GSON.fromJson(reader, FamineConfig.class);
                if (config != null) {
                    config.validate();
                    AncientCurse.LOGGER.info("Loaded famine config from {}", CONFIG_FILE);
                    return config;
                }
            } catch (Exception e) {
                AncientCurse.LOGGER.error("Failed to load famine config, using defaults", e);
            }
        }

        // Create default config
        FamineConfig config = new FamineConfig();
        config.save();
        return config;
    }

    /**
     * Save config to file
     */
    public void save() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);

        try (Writer writer = new FileWriter(configPath.toFile())) {
            GSON.toJson(this, writer);
            AncientCurse.LOGGER.info("Saved famine config to {}", CONFIG_FILE);
        } catch (Exception e) {
            AncientCurse.LOGGER.error("Failed to save famine config", e);
        }
    }

    /**
     * Validate and clamp config values
     */
    private void validate() {
        intensityFadeInSpeed = clamp(intensityFadeInSpeed, 0.1f, 10.0f);
        intensityFadeOutSpeed = clamp(intensityFadeOutSpeed, 0.1f, 10.0f);
        maxIntensity = clamp(maxIntensity, 0.1f, 1.0f);

        cropDeathRadius = clamp(cropDeathRadius, 16, 128);
        cropDeathChanceBase = clamp(cropDeathChanceBase, 0.0001f, 0.1f);
        cropDeathSpeedMultiplier = clamp(cropDeathSpeedMultiplier, 0.1f, 10.0f);
        maxCropDeathsPerTick = clamp(maxCropDeathsPerTick, 1, 50);
        cropChunkScanCooldown = clamp(cropChunkScanCooldown, 20, 1200);

        spoilageCheckInterval = clamp(spoilageCheckInterval, 20, 600);
        spoilageChanceBase = clamp(spoilageChanceBase, 0.001f, 0.5f);
        spoilageSpeedMultiplier = clamp(spoilageSpeedMultiplier, 0.1f, 10.0f);
        baseSpoilageTicks = clamp(baseSpoilageTicks, 600, 72000);

        animalStarvationRadius = clamp(animalStarvationRadius, 16, 128);
        animalStarvationRate = clamp(animalStarvationRate, 1, 100);
        animalStarvationDamageThreshold = clamp(animalStarvationDamageThreshold, 100, 12000);
        animalStarvationDamage = clamp(animalStarvationDamage, 0.5f, 20.0f);
        animalStarvationSpeedMultiplier = clamp(animalStarvationSpeedMultiplier, 0.1f, 10.0f);

        vignetteAlphaMultiplier = clamp(vignetteAlphaMultiplier, 0.0f, 2.0f);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    // === Getters ===

    // General
    public float getIntensityFadeInSpeed() { return intensityFadeInSpeed; }
    public float getIntensityFadeOutSpeed() { return intensityFadeOutSpeed; }
    public float getMaxIntensity() { return maxIntensity; }

    // Crop Death
    public int getCropDeathRadius() { return cropDeathRadius; }
    public float getCropDeathChanceBase() { return cropDeathChanceBase; }
    public float getCropDeathSpeedMultiplier() { return cropDeathSpeedMultiplier; }
    public int getMaxCropDeathsPerTick() { return maxCropDeathsPerTick; }
    public int getCropChunkScanCooldown() { return cropChunkScanCooldown; }

    // Food Spoilage
    public int getSpoilageCheckInterval() { return spoilageCheckInterval; }
    public float getSpoilageChanceBase() { return spoilageChanceBase; }
    public float getSpoilageSpeedMultiplier() { return spoilageSpeedMultiplier; }
    public int getBaseSpoilageTicks() { return baseSpoilageTicks; }

    // Animal Starvation
    public boolean isAnimalStarvationEnabled() { return animalStarvationEnabled; }
    public int getAnimalStarvationRadius() { return animalStarvationRadius; }
    public int getAnimalStarvationRate() { return (int)(animalStarvationRate * animalStarvationSpeedMultiplier); }
    public int getAnimalStarvationDamageThreshold() { return animalStarvationDamageThreshold; }
    public float getAnimalStarvationDamage() { return animalStarvationDamage; }
    public float getAnimalStarvationSpeedMultiplier() { return animalStarvationSpeedMultiplier; }

    // Visual
    public float getVignetteAlphaMultiplier() { return vignetteAlphaMultiplier; }
    public boolean showFamineTimer() { return showFamineTimer; }
    public boolean showSpoilageFlash() { return showSpoilageFlash; }

    // === Setters (for runtime modification via commands) ===

    public void setCropDeathRadius(int radius) {
        this.cropDeathRadius = clamp(radius, 16, 128);
    }

    public void setSpoilageCheckInterval(int interval) {
        this.spoilageCheckInterval = clamp(interval, 20, 600);
    }

    public void setAnimalStarvationEnabled(boolean enabled) {
        this.animalStarvationEnabled = enabled;
    }

    public void setCropDeathSpeedMultiplier(float multiplier) {
        this.cropDeathSpeedMultiplier = clamp(multiplier, 0.1f, 10.0f);
    }

    public void setSpoilageSpeedMultiplier(float multiplier) {
        this.spoilageSpeedMultiplier = clamp(multiplier, 0.1f, 10.0f);
    }

    public void setAnimalStarvationSpeedMultiplier(float multiplier) {
        this.animalStarvationSpeedMultiplier = clamp(multiplier, 0.1f, 10.0f);
    }

    public void setIntensityFadeInSpeed(float speed) {
        this.intensityFadeInSpeed = clamp(speed, 0.1f, 10.0f);
    }

    public void setIntensityFadeOutSpeed(float speed) {
        this.intensityFadeOutSpeed = clamp(speed, 0.1f, 10.0f);
    }

    // === Serialization for network sync ===

    /**
     * Write config to NBT for network sync
     */
    public void writeToBuffer(net.minecraft.network.PacketByteBuf buf) {
        buf.writeFloat(intensityFadeInSpeed);
        buf.writeFloat(intensityFadeOutSpeed);
        buf.writeFloat(maxIntensity);
        buf.writeFloat(vignetteAlphaMultiplier);
        buf.writeBoolean(showFamineTimer);
        buf.writeBoolean(showSpoilageFlash);
        buf.writeFloat(spoilageSpeedMultiplier);
    }

    /**
     * Read config from network buffer (client-side)
     */
    public static FamineConfig readFromBuffer(net.minecraft.network.PacketByteBuf buf) {
        FamineConfig config = new FamineConfig();
        config.intensityFadeInSpeed = buf.readFloat();
        config.intensityFadeOutSpeed = buf.readFloat();
        config.maxIntensity = buf.readFloat();
        config.vignetteAlphaMultiplier = buf.readFloat();
        config.showFamineTimer = buf.readBoolean();
        config.showSpoilageFlash = buf.readBoolean();
        config.spoilageSpeedMultiplier = buf.readFloat();
        return config;
    }
}
