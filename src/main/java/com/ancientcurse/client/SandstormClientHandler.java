package com.ancientcurse.client;

import com.ancientcurse.ModParticles;
import com.ancientcurse.ModSounds;
import com.ancientcurse.client.sound.SandstormLoopSoundInstance;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public class SandstormClientHandler {
    private static boolean active;
    private static float targetIntensity;
    private static float currentIntensity;

    private static SandstormLoopSoundInstance deepLoop;
    private static SandstormLoopSoundInstance dustLoop;

    // === Dynamic Wind System ===
    private static float windAngle = 0.0f;           // Current wind direction in radians
    private static float targetWindAngle = 0.0f;     // Target wind direction
    private static float windSpeed = 0.6f;           // Base wind speed
    private static float gustStrength = 0.0f;        // Current gust intensity (0-1)
    private static int gustTimer = 0;                // Ticks until next gust change
    private static int windChangeTimer = 0;          // Ticks until wind direction change
    private static long tickCount = 0;               // For turbulence calculation

    // === Sudden Direction Shift System ===
    private static boolean isShifting = false;       // Currently doing a sudden shift
    private static float shiftTargetAngle = 0.0f;    // Target angle during shift
    private static int shiftDuration = 0;            // Remaining ticks in shift

    // === Mega Gust System ===
    private static boolean isMegaGust = false;       // Currently in a mega gust
    private static int megaGustTimer = 0;            // Ticks until mega gust ends
    private static int megaGustCooldown = 0;         // Ticks until next mega gust can occur
    private static float megaGustStrength = 0.0f;    // Current mega gust intensity (0-2+)

    public static void update(boolean serverActive, float serverIntensity) {
        active = serverActive;
        targetIntensity = serverIntensity;
    }

    public static void reset() {
        active = false;
        targetIntensity = 0;
        currentIntensity = 0;
        windAngle = 0;
        targetWindAngle = 0;
        gustStrength = 0;
        isMegaGust = false;
        megaGustTimer = 0;
        megaGustCooldown = 0;
        megaGustStrength = 0;
        // Sounds will fade out on their own as they check intensity
    }

    public static void tick() {
        tickCount++;

        // Smooth interpolation for intensity
        if (currentIntensity < targetIntensity) {
            currentIntensity = Math.min(targetIntensity, currentIntensity + 0.005f);
        } else if (currentIntensity > targetIntensity) {
            currentIntensity = Math.max(targetIntensity, currentIntensity - 0.005f);
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && client.player != null) {
            // Update wind dynamics
            updateWind(client);

            // Manage sounds
            if (isRenderingSandstorm()) {
                if (deepLoop == null || deepLoop.isDone()) {
                    deepLoop = new SandstormLoopSoundInstance(ModSounds.SANDSTORM_DEEP_LOOP, 0.8F);
                    client.getSoundManager().play(deepLoop);
                }
                if (dustLoop == null || dustLoop.isDone()) {
                    dustLoop = new SandstormLoopSoundInstance(ModSounds.SANDSTORM_DUST_LOOP, 0.6F);
                    client.getSoundManager().play(dustLoop);
                }
            }

            if (isRenderingSandstorm()) {
                spawnParticles(client);
            }
        }
    }

    /**
     * Update wind direction and gusts naturally with occasional sudden shifts
     */
    private static void updateWind(MinecraftClient client) {
        // Handle sudden direction shifts (dramatic wind direction change)
        if (isShifting) {
            shiftDuration--;
            // Fast interpolation during shift
            float angleDiff = shiftTargetAngle - windAngle;
            // Normalize angle difference
            while (angleDiff > Math.PI) angleDiff -= Math.PI * 2;
            while (angleDiff < -Math.PI) angleDiff += Math.PI * 2;
            windAngle += angleDiff * 0.15f; // Fast rotation during shift

            if (shiftDuration <= 0) {
                isShifting = false;
                targetWindAngle = windAngle;
            }
        } else {
            // Normal gradual wind direction change
            windChangeTimer--;
            if (windChangeTimer <= 0) {
                // Small chance for sudden dramatic shift
                if (client.world.random.nextFloat() < 0.15f * currentIntensity) {
                    // Sudden shift! Pick a very different direction
                    isShifting = true;
                    shiftTargetAngle = windAngle + (float)(Math.PI * 0.5f + client.world.random.nextFloat() * Math.PI); // 90-270 degree change
                    shiftDuration = 30 + client.world.random.nextInt(20); // 1.5-2.5 seconds
                    gustStrength = 0.8f + client.world.random.nextFloat() * 0.2f; // Strong gust with shift
                    windChangeTimer = 200 + client.world.random.nextInt(200); // Wait longer after shift
                } else {
                    // Normal gradual change
                    float change = (client.world.random.nextFloat() - 0.5f) * 1.5f;
                    targetWindAngle = windAngle + change;
                    windChangeTimer = 80 + client.world.random.nextInt(160);
                }
            }

            // Smoothly interpolate toward target angle
            float angleDiff = targetWindAngle - windAngle;
            windAngle += angleDiff * 0.02f;
        }

        // Gust system - occasional stronger bursts
        gustTimer--;
        if (gustTimer <= 0) {
            if (client.world.random.nextFloat() < 0.4f) {
                // Start a gust
                gustStrength = Math.max(gustStrength, 0.4f + client.world.random.nextFloat() * 0.6f);
            }
            gustTimer = 15 + client.world.random.nextInt(45); // Every 0.75-3 seconds
        }

        // Decay gust strength
        gustStrength *= 0.93f;

        // === MEGA GUST SYSTEM - rare, really powerful gusts ===
        megaGustCooldown--;
        if (isMegaGust) {
            megaGustTimer--;
            if (megaGustTimer <= 0) {
                // End mega gust
                isMegaGust = false;
                megaGustCooldown = 400 + client.world.random.nextInt(600); // 20-50 second cooldown
            }
            // Decay mega gust strength slowly during the gust
            megaGustStrength *= 0.985f;
        } else if (megaGustCooldown <= 0 && currentIntensity > 0.3f) {
            // Chance to start a mega gust (rarer at low intensity, more common at high)
            float megaGustChance = 0.002f * currentIntensity; // ~0.2% per tick at full intensity
            if (client.world.random.nextFloat() < megaGustChance) {
                // START MEGA GUST!
                isMegaGust = true;
                megaGustTimer = 40 + client.world.random.nextInt(60); // 2-5 seconds duration
                megaGustStrength = 1.5f + client.world.random.nextFloat() * 1.0f; // 1.5-2.5x normal strength

                // Mega gusts also trigger a direction shift
                isShifting = true;
                shiftTargetAngle = windAngle + (float)((client.world.random.nextFloat() - 0.5f) * Math.PI);
                shiftDuration = 20 + client.world.random.nextInt(20);

                // Also boost regular gust strength
                gustStrength = 1.0f;
            }
        }
    }

    /**
     * Spawn particles with natural wind movement - lots of them for immersion
     */
    private static void spawnParticles(MinecraftClient client) {
        // Calculate mega gust multiplier
        float megaMultiplier = isMegaGust ? (1.0f + megaGustStrength) : 1.0f;

        // MUCH more particles based on intensity (boosted during mega gusts)
        int distantParticles = (int)(currentIntensity * 25 * megaMultiplier); // Distant swirling sand
        int midRangeParticles = (int)(currentIntensity * 15 * megaMultiplier); // Mid-range particles
        int closeParticles = (int)(currentIntensity * 8);     // Close to face particles

        // Calculate current wind vector (mega gusts make wind much stronger)
        float effectiveSpeed = windSpeed + gustStrength * 0.5f + (isMegaGust ? megaGustStrength * 0.8f : 0f);
        double windX = Math.cos(windAngle) * effectiveSpeed;
        double windZ = Math.sin(windAngle) * effectiveSpeed;

        // === DISTANT PARTICLES (far swirling sand clouds) ===
        for (int i = 0; i < distantParticles; i++) {
            if (client.world.random.nextFloat() < 0.9f) {
                double range = 35.0;

                // Spawn particles upwind so they blow past the player
                double offsetX = -windX * 25 + (client.world.random.nextDouble() - 0.5) * range * 2;
                double offsetZ = -windZ * 25 + (client.world.random.nextDouble() - 0.5) * range * 2;

                double x = client.player.getX() + offsetX;
                double y = client.player.getY() + (client.world.random.nextDouble() - 0.3) * 18.0;
                double z = client.player.getZ() + offsetZ;

                // Add turbulence
                double turbulenceX = (client.world.random.nextDouble() - 0.5) * 0.2;
                double turbulenceZ = (client.world.random.nextDouble() - 0.5) * 0.2;
                double turbulenceY = (client.world.random.nextDouble() - 0.5) * 0.1;

                // Swirl effect
                double swirlPhase = (x + z) * 0.1 + tickCount * 0.05;
                double swirlX = Math.sin(swirlPhase) * 0.06;
                double swirlZ = Math.cos(swirlPhase) * 0.06;

                double vx = windX + turbulenceX + swirlX;
                double vy = 0.02 + turbulenceY + gustStrength * 0.12;
                double vz = windZ + turbulenceZ + swirlZ;

                client.world.addParticle(ModParticles.SAND_DUST, x, y, z, vx, vy, vz);
            }
        }

        // === MID-RANGE PARTICLES (sand at medium distance) ===
        for (int i = 0; i < midRangeParticles; i++) {
            double range = 12.0;
            double offsetX = -windX * 10 + (client.world.random.nextDouble() - 0.5) * range * 2;
            double offsetZ = -windZ * 10 + (client.world.random.nextDouble() - 0.5) * range * 2;
            double x = client.player.getX() + offsetX;
            double y = client.player.getY() + (client.world.random.nextDouble() - 0.2) * 8.0;
            double z = client.player.getZ() + offsetZ;
            double turbulenceX = (client.world.random.nextDouble() - 0.5) * 0.18;
            double turbulenceZ = (client.world.random.nextDouble() - 0.5) * 0.18;
            double vx = windX * 1.1 + turbulenceX;
            double vy = 0.01 + (client.world.random.nextDouble() - 0.5) * 0.06;
            double vz = windZ * 1.1 + turbulenceZ;
            client.world.addParticle(ModParticles.SAND_DUST, x, y, z, vx, vy, vz);
        }

        // === CLOSE-UP FACE PARTICLES (disabled - too distracting on screen) ===
        /*
        for (int i = 0; i < closeParticles; i++) {
            // Spawn very close to player, slightly upwind
            double closeRange = 3.0;

            // Offset slightly upwind
            double offsetX = -windX * 2.5 + (client.world.random.nextDouble() - 0.5) * closeRange;
            double offsetZ = -windZ * 2.5 + (client.world.random.nextDouble() - 0.5) * closeRange;

            double x = client.player.getX() + offsetX;
            // Eye level area
            double y = client.player.getY() + 1.2 + (client.world.random.nextDouble() - 0.5) * 1.5;
            double z = client.player.getZ() + offsetZ;

            // These particles move faster - whipping past your face
            double speedMult = 1.3 + client.world.random.nextDouble() * 0.4;
            double turbulenceX = (client.world.random.nextDouble() - 0.5) * 0.25;
            double turbulenceZ = (client.world.random.nextDouble() - 0.5) * 0.25;

            double vx = windX * speedMult + turbulenceX;
            double vy = (client.world.random.nextDouble() - 0.5) * 0.1;
            double vz = windZ * speedMult + turbulenceZ;

            client.world.addParticle(ModParticles.SAND_DUST, x, y, z, vx, vy, vz);
        }
        */

        // === GROUND LEVEL DUST (sand being kicked up from ground) ===
        int groundDust = (int)(currentIntensity * 10);
        for (int i = 0; i < groundDust; i++) {
            double range = 20.0;

            double offsetX = (client.world.random.nextDouble() - 0.5) * range * 2;
            double offsetZ = (client.world.random.nextDouble() - 0.5) * range * 2;

            double x = client.player.getX() + offsetX;
            double y = client.player.getY() - 1 + client.world.random.nextDouble() * 2; // Near ground
            double z = client.player.getZ() + offsetZ;

            // Upward component for dust being lifted
            double vx = windX * 0.8 + (client.world.random.nextDouble() - 0.5) * 0.15;
            double vy = 0.05 + client.world.random.nextDouble() * 0.1; // Rising dust
            double vz = windZ * 0.8 + (client.world.random.nextDouble() - 0.5) * 0.15;

            client.world.addParticle(ModParticles.SAND_DUST, x, y, z, vx, vy, vz);
        }
    }

    // === Getters for other systems ===

    public static float getIntensity() {
        return currentIntensity;
    }

    public static boolean isRenderingSandstorm() {
        return currentIntensity > 0.01f;
    }

    /**
     * Get current wind angle in radians for screen effects
     */
    public static float getWindAngle() {
        return windAngle;
    }

    /**
     * Get current gust strength (0-1) for screen shake/effects
     */
    public static float getGustStrength() {
        return gustStrength;
    }

    /**
     * Get wind direction as normalized X component
     */
    public static float getWindX() {
        return (float)Math.cos(windAngle);
    }

    /**
     * Get wind direction as normalized Z component
     */
    public static float getWindZ() {
        return (float)Math.sin(windAngle);
    }

    /**
     * Check if currently in a mega gust
     */
    public static boolean isMegaGust() {
        return isMegaGust;
    }

    /**
     * Get mega gust strength (0 when not in mega gust, 1.5-2.5 during mega gust)
     */
    public static float getMegaGustStrength() {
        return megaGustStrength;
    }
}
