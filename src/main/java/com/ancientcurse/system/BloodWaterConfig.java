package com.ancientcurse.system;

import com.ancientcurse.AncientCurse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration for the Blood Water plague event (1st Plague of Egypt).
 * Water turns to blood and curses those who touch it.
 *
 * "The LORD said to Moses, 'Tell Aaron, Take your staff and stretch out your hand
 * over the waters of Egypt... and they will turn to blood.'" - Exodus 7:19
 */
public class BloodWaterConfig {

    private static BloodWaterConfig INSTANCE;
    private static final String CONFIG_FILE = "ancientcurse_bloodwater.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // === Curse Settings ===

    /** Whether touching blood water causes the curse effect */
    private boolean curseEnabled = true;

    /** Damage per tick when in blood water (default 0.5 = 1/4 heart per tick) */
    private float damagePerTick = 0.5f;

    /** Ticks between damage applications (default 20 = 1 second) */
    private int damageInterval = 20;

    /** How long the curse lingers after leaving blood water (ticks, default 100 = 5 seconds) */
    private int curseLingerDuration = 100;

    /** Damage per tick while curse is lingering (usually less than direct contact) */
    private float lingerDamagePerTick = 0.25f;

    /** Ticks between linger damage applications */
    private int lingerDamageInterval = 40;

    /** Whether drinking blood water causes instant damage */
    private boolean drinkingCausesDamage = true;

    /** Damage when drinking blood water */
    private float drinkDamage = 4.0f;

    /** Whether swimming in blood water applies nausea */
    private boolean applyNausea = true;

    /** Nausea duration when in blood water (ticks) */
    private int nauseaDuration = 100;

    /** Nausea amplifier (0 = level I) */
    private int nauseaAmplifier = 0;

    /** Whether to apply poison effect */
    private boolean applyPoison = false;

    /** Poison duration (ticks) */
    private int poisonDuration = 60;

    /** Poison amplifier (0 = level I) */
    private int poisonAmplifier = 0;

    /** Whether to apply hunger effect */
    private boolean applyHunger = true;

    /** Hunger duration (ticks) */
    private int hungerDuration = 200;

    /** Hunger amplifier (0 = level I) */
    private int hungerAmplifier = 1;

    // === Visual Settings ===

    /** Whether to show curse overlay when affected */
    private boolean showCurseOverlay = true;

    /** Overlay alpha multiplier */
    private float overlayAlphaMultiplier = 1.0f;

    /** Whether to show curse timer on screen */
    private boolean showCurseTimer = true;

    /** Whether to flash screen when taking damage */
    private boolean showDamageFlash = true;

    // === Intensity Scaling ===

    /** Whether damage scales with blood water intensity */
    private boolean damageScalesWithIntensity = true;

    /** Minimum intensity required for curse to activate (0.0 to 1.0) */
    private float minimumIntensityForCurse = 0.3f;

    // === Immunity Settings ===

    /** Ticks of immunity after respawn */
    private int respawnImmunityTicks = 100;

    // === Constructor ===

    public BloodWaterConfig() {
        // Default values set above
    }

    // === Static Access ===

    public static BloodWaterConfig get() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    public static void reload() {
        INSTANCE = load();
        AncientCurse.LOGGER.info("Blood Water config reloaded");
    }

    private static BloodWaterConfig load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);

        if (Files.exists(configPath)) {
            try (Reader reader = new FileReader(configPath.toFile())) {
                BloodWaterConfig config = GSON.fromJson(reader, BloodWaterConfig.class);
                if (config != null) {
                    config.validate();
                    AncientCurse.LOGGER.info("Loaded blood water config from {}", CONFIG_FILE);
                    return config;
                }
            } catch (Exception e) {
                AncientCurse.LOGGER.error("Failed to load blood water config, using defaults", e);
            }
        }

        BloodWaterConfig config = new BloodWaterConfig();
        config.save();
        return config;
    }

    public void save() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);

        try (Writer writer = new FileWriter(configPath.toFile())) {
            GSON.toJson(this, writer);
            AncientCurse.LOGGER.info("Saved blood water config to {}", CONFIG_FILE);
        } catch (Exception e) {
            AncientCurse.LOGGER.error("Failed to save blood water config", e);
        }
    }

    private void validate() {
        damagePerTick = clamp(damagePerTick, 0.0f, 10.0f);
        damageInterval = clamp(damageInterval, 1, 200);
        curseLingerDuration = clamp(curseLingerDuration, 0, 600);
        lingerDamagePerTick = clamp(lingerDamagePerTick, 0.0f, 5.0f);
        lingerDamageInterval = clamp(lingerDamageInterval, 1, 200);
        drinkDamage = clamp(drinkDamage, 0.0f, 20.0f);
        nauseaDuration = clamp(nauseaDuration, 0, 600);
        nauseaAmplifier = clamp(nauseaAmplifier, 0, 3);
        poisonDuration = clamp(poisonDuration, 0, 600);
        poisonAmplifier = clamp(poisonAmplifier, 0, 3);
        hungerDuration = clamp(hungerDuration, 0, 600);
        hungerAmplifier = clamp(hungerAmplifier, 0, 3);
        overlayAlphaMultiplier = clamp(overlayAlphaMultiplier, 0.0f, 2.0f);
        minimumIntensityForCurse = clamp(minimumIntensityForCurse, 0.0f, 1.0f);
        respawnImmunityTicks = clamp(respawnImmunityTicks, 0, 600);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    // === Getters ===

    public boolean isCurseEnabled() { return curseEnabled; }
    public float getDamagePerTick() { return damagePerTick; }
    public int getDamageInterval() { return damageInterval; }
    public int getCurseLingerDuration() { return curseLingerDuration; }
    public float getLingerDamagePerTick() { return lingerDamagePerTick; }
    public int getLingerDamageInterval() { return lingerDamageInterval; }
    public boolean isDrinkingCausesDamage() { return drinkingCausesDamage; }
    public float getDrinkDamage() { return drinkDamage; }
    public boolean isApplyNausea() { return applyNausea; }
    public int getNauseaDuration() { return nauseaDuration; }
    public int getNauseaAmplifier() { return nauseaAmplifier; }
    public boolean isApplyPoison() { return applyPoison; }
    public int getPoisonDuration() { return poisonDuration; }
    public int getPoisonAmplifier() { return poisonAmplifier; }
    public boolean isApplyHunger() { return applyHunger; }
    public int getHungerDuration() { return hungerDuration; }
    public int getHungerAmplifier() { return hungerAmplifier; }
    public boolean isShowCurseOverlay() { return showCurseOverlay; }
    public float getOverlayAlphaMultiplier() { return overlayAlphaMultiplier; }
    public boolean isShowCurseTimer() { return showCurseTimer; }
    public boolean isShowDamageFlash() { return showDamageFlash; }
    public boolean isDamageScalesWithIntensity() { return damageScalesWithIntensity; }
    public float getMinimumIntensityForCurse() { return minimumIntensityForCurse; }
    public int getRespawnImmunityTicks() { return respawnImmunityTicks; }

    // === Setters ===

    public void setCurseEnabled(boolean enabled) { this.curseEnabled = enabled; }
    public void setDamagePerTick(float damage) { this.damagePerTick = clamp(damage, 0.0f, 10.0f); }
    public void setDamageInterval(int interval) { this.damageInterval = clamp(interval, 1, 200); }
    public void setCurseLingerDuration(int duration) { this.curseLingerDuration = clamp(duration, 0, 600); }
    public void setLingerDamagePerTick(float damage) { this.lingerDamagePerTick = clamp(damage, 0.0f, 5.0f); }
    public void setApplyNausea(boolean apply) { this.applyNausea = apply; }
    public void setApplyPoison(boolean apply) { this.applyPoison = apply; }
    public void setApplyHunger(boolean apply) { this.applyHunger = apply; }
    public void setShowCurseOverlay(boolean show) { this.showCurseOverlay = show; }
    public void setShowDamageFlash(boolean show) { this.showDamageFlash = show; }
    public void setDamageScalesWithIntensity(boolean scales) { this.damageScalesWithIntensity = scales; }

    // === Network Serialization ===

    public void writeToBuffer(net.minecraft.network.PacketByteBuf buf) {
        buf.writeBoolean(curseEnabled);
        buf.writeFloat(damagePerTick);
        buf.writeInt(damageInterval);
        buf.writeInt(curseLingerDuration);
        buf.writeFloat(lingerDamagePerTick);
        buf.writeInt(lingerDamageInterval);
        buf.writeBoolean(applyNausea);
        buf.writeBoolean(applyPoison);
        buf.writeBoolean(applyHunger);
        buf.writeBoolean(showCurseOverlay);
        buf.writeFloat(overlayAlphaMultiplier);
        buf.writeBoolean(showCurseTimer);
        buf.writeBoolean(showDamageFlash);
        buf.writeBoolean(damageScalesWithIntensity);
        buf.writeFloat(minimumIntensityForCurse);
    }

    public static BloodWaterConfig readFromBuffer(net.minecraft.network.PacketByteBuf buf) {
        BloodWaterConfig config = new BloodWaterConfig();
        config.curseEnabled = buf.readBoolean();
        config.damagePerTick = buf.readFloat();
        config.damageInterval = buf.readInt();
        config.curseLingerDuration = buf.readInt();
        config.lingerDamagePerTick = buf.readFloat();
        config.lingerDamageInterval = buf.readInt();
        config.applyNausea = buf.readBoolean();
        config.applyPoison = buf.readBoolean();
        config.applyHunger = buf.readBoolean();
        config.showCurseOverlay = buf.readBoolean();
        config.overlayAlphaMultiplier = buf.readFloat();
        config.showCurseTimer = buf.readBoolean();
        config.showDamageFlash = buf.readBoolean();
        config.damageScalesWithIntensity = buf.readBoolean();
        config.minimumIntensityForCurse = buf.readFloat();
        return config;
    }
}
