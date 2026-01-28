package com.ancientcurse.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

/**
 * Client-side handler for the Blood Water plague event (1st Plague of Egypt).
 * Stores the current state and provides smooth interpolation for rendering.
 *
 * Handles:
 * - Water color transformation (blood red)
 * - Curse state tracking for player
 * - Damage flash effects
 * - Curse overlay rendering data
 */
@Environment(EnvType.CLIENT)
public class BloodWaterClientHandler {
    // === Blood Water State ===
    private static boolean active = false;
    private static float targetIntensity = 0.0f;
    private static float currentIntensity = 0.0f;
    private static int remainingSeconds = -1;
    private static boolean wasActive = false;
    private static int reloadCooldown = 0;

    // === Curse State ===
    private static boolean isCursed = false;
    private static int curseLingerTicks = 0;
    private static int damageFlashTimer = 0;
    private static float lastDamageAmount = 0;

    // === Config values synced from server ===
    private static boolean curseEnabled = true;
    private static boolean showCurseOverlay = true;
    private static float overlayAlphaMultiplier = 1.0f;
    private static boolean showCurseTimer = true;
    private static boolean showDamageFlash = true;

    // Blood color RGB (deep crimson red)
    public static final float BLOOD_RED = 0.6f;
    public static final float BLOOD_GREEN = 0.1f;
    public static final float BLOOD_BLUE = 0.1f;

    /**
     * Called when receiving main sync packet from server
     */
    public static void update(boolean serverActive, float serverIntensity, int serverRemainingSeconds,
                               boolean serverCurseEnabled, boolean serverShowCurseOverlay,
                               float serverOverlayAlpha, boolean serverShowCurseTimer,
                               boolean serverShowDamageFlash) {
        boolean stateChanged = (active != serverActive);
        boolean intensityJump = Math.abs(currentIntensity - serverIntensity) > 0.3f;
        active = serverActive;
        targetIntensity = serverIntensity;
        // Also immediately set current intensity if it's a fresh join (big jump)
        if (intensityJump) {
            currentIntensity = serverIntensity;
        }
        remainingSeconds = serverRemainingSeconds;

        // Update config values
        curseEnabled = serverCurseEnabled;
        showCurseOverlay = serverShowCurseOverlay;
        overlayAlphaMultiplier = serverOverlayAlpha;
        showCurseTimer = serverShowCurseTimer;
        showDamageFlash = serverShowDamageFlash;

        // Trigger chunk reload on state change or big intensity jump (e.g., player just joined)
        if ((stateChanged || intensityJump) && reloadCooldown <= 0) {
            // Schedule delayed reload to ensure chunks are loaded
            pendingReloadDelay = 20; // 1 second delay
            reloadCooldown = 40;
        }
    }

    private static int pendingReloadDelay = 0;

    /**
     * Called when receiving curse state update from server
     */
    public static void updateCurseState(boolean cursed, int lingerTicks) {
        boolean wasNotCursed = !isCursed;
        isCursed = cursed;
        curseLingerTicks = lingerTicks;

        // Play sound when curse starts
        if (cursed && wasNotCursed) {
            playBloodCurseSound();
        }
    }

    /**
     * Called when taking damage from blood water
     */
    public static void onDamageReceived(float damageAmount) {
        if (showDamageFlash) {
            damageFlashTimer = 10; // 0.5 seconds
            lastDamageAmount = damageAmount;
        }

        // Play hurt sound
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.playSound(
                SoundEvents.ENTITY_PLAYER_HURT,
                SoundCategory.PLAYERS,
                0.5f,
                0.8f + client.world.random.nextFloat() * 0.2f
            );
        }
    }

    /**
     * Play eerie sound when blood curse activates
     */
    private static void playBloodCurseSound() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.playSound(
                SoundEvents.AMBIENT_CAVE.value(),
                SoundCategory.AMBIENT,
                0.6f,
                0.5f
            );
        }
    }

    /**
     * Called when disconnecting from server
     */
    public static void reset() {
        boolean needsReload = isBloodWaterActive();
        active = false;
        targetIntensity = 0.0f;
        currentIntensity = 0.0f;
        remainingSeconds = -1;
        wasActive = false;
        reloadCooldown = 0;
        pendingReloadDelay = 0;

        // Reset curse state
        isCursed = false;
        curseLingerTicks = 0;
        damageFlashTimer = 0;

        // Reset config to defaults
        curseEnabled = true;
        showCurseOverlay = true;
        overlayAlphaMultiplier = 1.0f;
        showCurseTimer = true;
        showDamageFlash = true;

        if (needsReload) {
            scheduleChunkReload();
        }
    }

    /**
     * Called every client tick for smooth interpolation
     */
    public static void tick() {
        float previousIntensity = currentIntensity;

        // Smooth interpolation towards target
        if (currentIntensity < targetIntensity) {
            currentIntensity = Math.min(targetIntensity, currentIntensity + 0.02f);
        } else if (currentIntensity > targetIntensity) {
            currentIntensity = Math.max(targetIntensity, currentIntensity - 0.02f);
        }

        // Cooldown timer
        if (reloadCooldown > 0) {
            reloadCooldown--;
        }

        // Handle delayed reload (for when player joins with event already active)
        if (pendingReloadDelay > 0) {
            pendingReloadDelay--;
            if (pendingReloadDelay == 0) {
                scheduleChunkReload();
            }
        }

        // Damage flash timer
        if (damageFlashTimer > 0) {
            damageFlashTimer--;
        }

        // Detect when transitioning between active/inactive states
        boolean isNowActive = isBloodWaterActive();
        if (wasActive != isNowActive && reloadCooldown <= 0) {
            scheduleChunkReload();
            reloadCooldown = 40;
        }
        wasActive = isNowActive;
    }

    /**
     * Schedule a chunk reload to update water colors.
     */
    private static void scheduleChunkReload() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.worldRenderer != null) {
            client.worldRenderer.reload();
        }
    }

    // === Getters for Blood Water State ===

    public static boolean isActive() {
        return active;
    }

    public static boolean isBloodWaterActive() {
        return currentIntensity > 0.01f;
    }

    public static float getIntensity() {
        return currentIntensity;
    }

    public static int getRemainingSeconds() {
        return remainingSeconds;
    }

    // === Getters for Curse State ===

    public static boolean isCursed() {
        return isCursed && curseEnabled;
    }

    public static int getCurseLingerTicks() {
        return curseLingerTicks;
    }

    public static float getCurseLingerSeconds() {
        return curseLingerTicks / 20.0f;
    }

    public static boolean isDamageFlashing() {
        return damageFlashTimer > 0 && showDamageFlash;
    }

    public static float getDamageFlashAlpha() {
        if (damageFlashTimer <= 0 || !showDamageFlash) return 0;
        return (damageFlashTimer / 10.0f) * 0.6f;
    }

    // === Config Getters ===

    public static boolean isCurseEnabled() {
        return curseEnabled;
    }

    public static boolean shouldShowCurseOverlay() {
        return showCurseOverlay;
    }

    public static float getOverlayAlphaMultiplier() {
        return overlayAlphaMultiplier;
    }

    public static boolean shouldShowCurseTimer() {
        return showCurseTimer;
    }

    public static boolean shouldShowDamageFlash() {
        return showDamageFlash;
    }

    // === Color Calculation Methods ===

    /**
     * Get the blood water color with current intensity applied.
     * Returns an int color in ARGB format for biome color blending.
     */
    public static int getBloodWaterColor(int originalColor) {
        if (!isBloodWaterActive()) {
            return originalColor;
        }

        float intensity = currentIntensity;

        // Extract original color components
        int origR = (originalColor >> 16) & 0xFF;
        int origG = (originalColor >> 8) & 0xFF;
        int origB = originalColor & 0xFF;

        // Blood color in 0-255 range
        int bloodR = (int)(BLOOD_RED * 255);
        int bloodG = (int)(BLOOD_GREEN * 255);
        int bloodB = (int)(BLOOD_BLUE * 255);

        // Lerp between original and blood color based on intensity
        int newR = (int)(origR * (1 - intensity) + bloodR * intensity);
        int newG = (int)(origG * (1 - intensity) + bloodG * intensity);
        int newB = (int)(origB * (1 - intensity) + bloodB * intensity);

        // Clamp values
        newR = Math.min(255, Math.max(0, newR));
        newG = Math.min(255, Math.max(0, newG));
        newB = Math.min(255, Math.max(0, newB));

        return (newR << 16) | (newG << 8) | newB;
    }

    /**
     * Get blood-tinted fog color for underwater effect
     */
    public static float[] getBloodFogColor(float origR, float origG, float origB) {
        if (!isBloodWaterActive()) {
            return new float[]{origR, origG, origB};
        }

        float intensity = currentIntensity;

        float newR = origR * (1 - intensity) + BLOOD_RED * intensity;
        float newG = origG * (1 - intensity) + BLOOD_GREEN * intensity;
        float newB = origB * (1 - intensity) + BLOOD_BLUE * intensity;

        return new float[]{newR, newG, newB};
    }

    /**
     * Get the curse overlay color (deep red with pulsing effect)
     */
    public static int getCurseOverlayColor() {
        if (!isCursed() || !showCurseOverlay) return 0;

        // Pulsing effect based on game time
        MinecraftClient client = MinecraftClient.getInstance();
        float pulse = 0.5f + 0.5f * (float)Math.sin(
            (client.world != null ? client.world.getTime() : 0) * 0.1f);

        float baseAlpha = 0.3f * overlayAlphaMultiplier;
        float alpha = baseAlpha * pulse;

        // Calculate alpha based on remaining curse time (more intense when fresh)
        float lingerFactor = Math.min(1.0f, curseLingerTicks / 60.0f); // Full intensity for first 3 seconds
        alpha *= lingerFactor;

        int a = (int)(alpha * 255);
        int r = 150;
        int g = 20;
        int b = 20;

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Get the damage flash overlay color (bright red flash)
     */
    public static int getDamageFlashColor() {
        if (!isDamageFlashing()) return 0;

        float alpha = getDamageFlashAlpha();
        int a = (int)(alpha * 255);
        int r = 200;
        int g = 30;
        int b = 30;

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Format remaining curse time for display
     */
    public static String getFormattedCurseTime() {
        if (!isCursed()) return "";

        float seconds = getCurseLingerSeconds();
        if (seconds > 0) {
            return String.format("%.1fs", seconds);
        }
        return "Cursed";
    }
}
