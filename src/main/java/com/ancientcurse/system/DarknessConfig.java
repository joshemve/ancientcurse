package com.ancientcurse.system;

import com.ancientcurse.AncientCurse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration for the Darkness plague event (9th Plague of Egypt).
 * Total darkness covering the land for three days.
 *
 * Loaded from config file on server start, synced to clients.
 */
public class DarknessConfig {

    private static DarknessConfig INSTANCE;
    private static final String CONFIG_FILE = "ancientcurse_darkness.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // === Duration Settings ===

    /** Default duration in Minecraft days (default: 3 days = 72000 ticks at 24000 ticks/day) */
    private int defaultDurationDays = 3;

    /** Minimum allowed duration in days */
    private int minDurationDays = 1;

    /** Maximum allowed duration in days */
    private int maxDurationDays = 30;

    // === Intensity Settings ===

    /** Speed multiplier for darkness intensity fade in (default 1.0) */
    private float intensityFadeInSpeed = 1.0f;

    /** Speed multiplier for darkness intensity fade out (default 1.0) */
    private float intensityFadeOutSpeed = 1.0f;

    /** Maximum darkness intensity (0.0 to 1.0, where 1.0 is complete darkness) */
    private float maxIntensity = 0.75f;

    /** Minimum ambient light level during full darkness (0.0 to 1.0) */
    private float minimumAmbientLight = 0.15f;

    // === Visual Settings (synced to client) ===

    /** Alpha multiplier for darkness overlay (0.0 to 2.0) */
    private float overlayAlphaMultiplier = 0.7f;

    /** Whether to show the darkness timer on screen */
    private boolean showDarknessTimer = true;

    /** Whether to apply desaturation effect during darkness */
    private boolean applyDesaturation = true;

    /** Desaturation strength (0.0 to 1.0) */
    private float desaturationStrength = 0.7f;

    /** Whether darkness affects sky rendering */
    private boolean affectSkyColor = true;

    /** Whether to show occasional lightning flashes (brief moments of light) */
    private boolean enableLightningFlashes = false;

    /** Chance per tick for a lightning flash (0.0 to 1.0) */
    private float lightningFlashChance = 0.0001f;

    // === Gameplay Settings ===

    /** Whether hostile mobs spawn more frequently during darkness */
    private boolean increasedMobSpawning = true;

    /** Mob spawn rate multiplier during full darkness */
    private float mobSpawnMultiplier = 2.0f;

    /** Whether players get the darkness status effect */
    private boolean applyDarknessEffect = true;

    /** Darkness effect level (0 = I, 1 = II, etc.) */
    private int darknessEffectLevel = 0;

    /** Whether torches and other light sources are dimmed */
    private boolean dimLightSources = true;

    /** Light source effectiveness during darkness (0.0 to 1.0) */
    private float lightSourceEffectiveness = 0.3f;

    // === Audio Settings ===

    /** Whether to play ambient sounds during darkness */
    private boolean playAmbientSounds = true;

    /** Volume of darkness ambient sounds (0.0 to 1.0) */
    private float ambientSoundVolume = 0.5f;

    // === Constructor ===

    public DarknessConfig() {
        // Default values set above
    }

    // === Static Access ===

    /**
     * Get the singleton config instance
     */
    public static DarknessConfig get() {
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
        AncientCurse.LOGGER.info("Darkness config reloaded");
    }

    /**
     * Load config from file, creating default if not exists
     */
    private static DarknessConfig load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);

        if (Files.exists(configPath)) {
            try (Reader reader = new FileReader(configPath.toFile())) {
                DarknessConfig config = GSON.fromJson(reader, DarknessConfig.class);
                if (config != null) {
                    config.validate();
                    AncientCurse.LOGGER.info("Loaded darkness config from {}", CONFIG_FILE);
                    return config;
                }
            } catch (Exception e) {
                AncientCurse.LOGGER.error("Failed to load darkness config, using defaults", e);
            }
        }

        // Create default config
        DarknessConfig config = new DarknessConfig();
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
            AncientCurse.LOGGER.info("Saved darkness config to {}", CONFIG_FILE);
        } catch (Exception e) {
            AncientCurse.LOGGER.error("Failed to save darkness config", e);
        }
    }

    /**
     * Validate and clamp config values
     */
    private void validate() {
        // Duration settings
        defaultDurationDays = clamp(defaultDurationDays, 1, 30);
        minDurationDays = clamp(minDurationDays, 1, 30);
        maxDurationDays = clamp(maxDurationDays, minDurationDays, 30);

        // Intensity settings
        intensityFadeInSpeed = clamp(intensityFadeInSpeed, 0.1f, 10.0f);
        intensityFadeOutSpeed = clamp(intensityFadeOutSpeed, 0.1f, 10.0f);
        maxIntensity = clamp(maxIntensity, 0.5f, 1.0f);
        minimumAmbientLight = clamp(minimumAmbientLight, 0.0f, 0.2f);

        // Visual settings
        overlayAlphaMultiplier = clamp(overlayAlphaMultiplier, 0.0f, 2.0f);
        desaturationStrength = clamp(desaturationStrength, 0.0f, 1.0f);
        lightningFlashChance = clamp(lightningFlashChance, 0.0f, 0.01f);

        // Gameplay settings
        mobSpawnMultiplier = clamp(mobSpawnMultiplier, 1.0f, 5.0f);
        darknessEffectLevel = clamp(darknessEffectLevel, 0, 3);
        lightSourceEffectiveness = clamp(lightSourceEffectiveness, 0.1f, 1.0f);

        // Audio settings
        ambientSoundVolume = clamp(ambientSoundVolume, 0.0f, 1.0f);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    // === Duration Helpers ===

    /**
     * Get default duration in ticks (24000 ticks per Minecraft day)
     */
    public int getDefaultDurationTicks() {
        return defaultDurationDays * 24000;
    }

    /**
     * Convert days to ticks
     */
    public int daysToTicks(int days) {
        return clamp(days, minDurationDays, maxDurationDays) * 24000;
    }

    /**
     * Convert ticks to days (rounded)
     */
    public int ticksToDays(int ticks) {
        return Math.max(0, ticks / 24000);
    }

    // === Getters ===

    // Duration
    public int getDefaultDurationDays() { return defaultDurationDays; }
    public int getMinDurationDays() { return minDurationDays; }
    public int getMaxDurationDays() { return maxDurationDays; }

    // Intensity
    public float getIntensityFadeInSpeed() { return intensityFadeInSpeed; }
    public float getIntensityFadeOutSpeed() { return intensityFadeOutSpeed; }
    public float getMaxIntensity() { return maxIntensity; }
    public float getMinimumAmbientLight() { return minimumAmbientLight; }

    // Visual
    public float getOverlayAlphaMultiplier() { return overlayAlphaMultiplier; }
    public boolean showDarknessTimer() { return showDarknessTimer; }
    public boolean applyDesaturation() { return applyDesaturation; }
    public float getDesaturationStrength() { return desaturationStrength; }
    public boolean affectSkyColor() { return affectSkyColor; }
    public boolean enableLightningFlashes() { return enableLightningFlashes; }
    public float getLightningFlashChance() { return lightningFlashChance; }

    // Gameplay
    public boolean increasedMobSpawning() { return increasedMobSpawning; }
    public float getMobSpawnMultiplier() { return mobSpawnMultiplier; }
    public boolean applyDarknessEffect() { return applyDarknessEffect; }
    public int getDarknessEffectLevel() { return darknessEffectLevel; }
    public boolean dimLightSources() { return dimLightSources; }
    public float getLightSourceEffectiveness() { return lightSourceEffectiveness; }

    // Audio
    public boolean playAmbientSounds() { return playAmbientSounds; }
    public float getAmbientSoundVolume() { return ambientSoundVolume; }

    // === Setters (for runtime modification via commands) ===

    public void setDefaultDurationDays(int days) {
        this.defaultDurationDays = clamp(days, minDurationDays, maxDurationDays);
    }

    public void setIntensityFadeInSpeed(float speed) {
        this.intensityFadeInSpeed = clamp(speed, 0.1f, 10.0f);
    }

    public void setIntensityFadeOutSpeed(float speed) {
        this.intensityFadeOutSpeed = clamp(speed, 0.1f, 10.0f);
    }

    public void setMaxIntensity(float intensity) {
        this.maxIntensity = clamp(intensity, 0.5f, 1.0f);
    }

    public void setOverlayAlphaMultiplier(float multiplier) {
        this.overlayAlphaMultiplier = clamp(multiplier, 0.0f, 2.0f);
    }

    public void setShowDarknessTimer(boolean show) {
        this.showDarknessTimer = show;
    }

    public void setApplyDesaturation(boolean apply) {
        this.applyDesaturation = apply;
    }

    public void setDesaturationStrength(float strength) {
        this.desaturationStrength = clamp(strength, 0.0f, 1.0f);
    }

    public void setEnableLightningFlashes(boolean enable) {
        this.enableLightningFlashes = enable;
    }

    public void setLightningFlashChance(float chance) {
        this.lightningFlashChance = clamp(chance, 0.0f, 0.01f);
    }

    public void setIncreasedMobSpawning(boolean enabled) {
        this.increasedMobSpawning = enabled;
    }

    public void setMobSpawnMultiplier(float multiplier) {
        this.mobSpawnMultiplier = clamp(multiplier, 1.0f, 5.0f);
    }

    public void setApplyDarknessEffect(boolean apply) {
        this.applyDarknessEffect = apply;
    }

    public void setDimLightSources(boolean dim) {
        this.dimLightSources = dim;
    }

    public void setLightSourceEffectiveness(float effectiveness) {
        this.lightSourceEffectiveness = clamp(effectiveness, 0.1f, 1.0f);
    }

    public void setPlayAmbientSounds(boolean play) {
        this.playAmbientSounds = play;
    }

    public void setAmbientSoundVolume(float volume) {
        this.ambientSoundVolume = clamp(volume, 0.0f, 1.0f);
    }

    // === Serialization for network sync ===

    /**
     * Write config values needed by client to buffer
     */
    public void writeToBuffer(net.minecraft.network.PacketByteBuf buf) {
        buf.writeFloat(intensityFadeInSpeed);
        buf.writeFloat(intensityFadeOutSpeed);
        buf.writeFloat(maxIntensity);
        buf.writeFloat(minimumAmbientLight);
        buf.writeFloat(overlayAlphaMultiplier);
        buf.writeBoolean(showDarknessTimer);
        buf.writeBoolean(applyDesaturation);
        buf.writeFloat(desaturationStrength);
        buf.writeBoolean(affectSkyColor);
        buf.writeBoolean(enableLightningFlashes);
        buf.writeFloat(lightningFlashChance);
        buf.writeBoolean(dimLightSources);
        buf.writeFloat(lightSourceEffectiveness);
        buf.writeBoolean(playAmbientSounds);
        buf.writeFloat(ambientSoundVolume);
    }

    /**
     * Read config from network buffer (client-side)
     */
    public static DarknessConfig readFromBuffer(net.minecraft.network.PacketByteBuf buf) {
        DarknessConfig config = new DarknessConfig();
        config.intensityFadeInSpeed = buf.readFloat();
        config.intensityFadeOutSpeed = buf.readFloat();
        config.maxIntensity = buf.readFloat();
        config.minimumAmbientLight = buf.readFloat();
        config.overlayAlphaMultiplier = buf.readFloat();
        config.showDarknessTimer = buf.readBoolean();
        config.applyDesaturation = buf.readBoolean();
        config.desaturationStrength = buf.readFloat();
        config.affectSkyColor = buf.readBoolean();
        config.enableLightningFlashes = buf.readBoolean();
        config.lightningFlashChance = buf.readFloat();
        config.dimLightSources = buf.readBoolean();
        config.lightSourceEffectiveness = buf.readFloat();
        config.playAmbientSounds = buf.readBoolean();
        config.ambientSoundVolume = buf.readFloat();
        return config;
    }
}
