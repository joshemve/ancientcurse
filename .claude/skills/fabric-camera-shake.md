# Fabric 1.20.1 Camera/Screen Shake Implementation

## Overview

Camera shake in Fabric mods can be implemented through:
1. **Library approach** - Using `fabric-camera-shake` library (recommended)
2. **DIY mixin approach** - Direct GameRenderer/Camera mixin injection
3. **MatrixStack manipulation** - Rotating the view matrix during rendering

---

## Approach 1: fabric-camera-shake Library (Recommended)

### Setup

**build.gradle repositories:**
```groovy
repositories {
    maven { url 'https://maven.logandark.net' }
}
```

**build.gradle dependencies:**
```groovy
dependencies {
    modImplementation 'net.logandark:camera-shake:<VERSION>'
    include 'net.logandark:camera-shake:<VERSION>'  // JiJ embed
}
```

**fabric.mod.json:**
```json
{
    "depends": {
        "camera-shake": "*"
    }
}
```

### Usage

**One-time shake event (explosion, impact, etc.):**
```java
import net.logandark.camerashake.api.CameraShakeManager;
import net.logandark.camerashake.api.BoomEvent;

// Trigger shake with magnitude 0.25 blocks, duration 0.5 seconds
CameraShakeManager.getInstance().addEvent(new BoomEvent(0.25f, 0, 0.5f));
```

**Custom shake event:**
```java
public class CustomShakeEvent implements CameraShakeEvent {
    private float intensity;
    private float duration;
    private float elapsed;

    public CustomShakeEvent(float intensity, float duration) {
        this.intensity = intensity;
        this.duration = duration;
        this.elapsed = 0;
    }

    @Override
    public float getIntensity(float tickDelta) {
        // Decay over time
        float progress = elapsed / duration;
        return intensity * (1.0f - progress);
    }

    @Override
    public boolean isFinished() {
        return elapsed >= duration;
    }

    @Override
    public void tick() {
        elapsed += 0.05f; // 1 tick = 0.05 seconds
    }
}
```

**Continuous shake provider (machinery, earthquake zone):**
```java
public class EarthquakeProvider implements CameraShakeProvider {
    @Override
    public float getIntensity(float tickDelta) {
        // Return shake intensity based on conditions
        if (isPlayerInEarthquakeZone()) {
            return 0.3f;
        }
        return 0;
    }
}

// Register provider
CameraShakeManager.getInstance().addProvider(new EarthquakeProvider());
```

---

## Approach 2: DIY Mixin Implementation

### Target Classes
- `net.minecraft.client.render.GameRenderer` - Main rendering class
- `net.minecraft.client.render.Camera` - Camera position/rotation

### Key Injection Points

**GameRenderer methods:**
- `renderWorld(float tickDelta, long limitTime, MatrixStack matrices)` - World rendering
- `bobViewWhenHurt(MatrixStack matrices, float tickDelta)` - Damage shake (modify/cancel)
- `renderHand(MatrixStack matrices, Camera camera, float tickDelta)` - Hand rendering

### Basic Mixin Example

**resources/modid.mixins.json:**
```json
{
    "required": true,
    "package": "com.yourmod.mixin",
    "compatibilityLevel": "JAVA_17",
    "client": [
        "GameRendererMixin"
    ],
    "injectors": {
        "defaultRequire": 1
    }
}
```

**GameRendererMixin.java:**
```java
package com.yourmod.mixin;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "renderWorld", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/Camera;update(Lnet/minecraft/world/BlockView;Lnet/minecraft/entity/Entity;ZZF)V",
            shift = At.Shift.AFTER))
    private void injectCameraShake(float tickDelta, long limitTime, MatrixStack matrices, CallbackInfo ci) {
        // Get shake values from your shake manager
        float shakeIntensity = YourShakeManager.getIntensity();

        if (shakeIntensity > 0) {
            // Generate pseudo-random shake using time
            long time = System.currentTimeMillis();
            float shakeX = (float) Math.sin(time * 0.1) * shakeIntensity;
            float shakeZ = (float) Math.cos(time * 0.13) * shakeIntensity;

            // Apply rotation to view
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(shakeX));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(shakeZ));
        }
    }
}
```

### Advanced: Perlin Noise Shake

```java
import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.util.math.random.Random;

public class ScreenShakeManager {
    private static final SimplexNoiseSampler NOISE = new SimplexNoiseSampler(Random.create(12345));

    private static float intensity = 0;
    private static float duration = 0;
    private static float elapsed = 0;
    private static float frequency = 20.0f; // Shake speed

    public static void shake(float intensity, float durationTicks) {
        ScreenShakeManager.intensity = intensity;
        ScreenShakeManager.duration = durationTicks;
        ScreenShakeManager.elapsed = 0;
    }

    public static void tick() {
        if (elapsed < duration) {
            elapsed++;
        }
    }

    public static float[] getShakeOffset(float tickDelta) {
        if (elapsed >= duration || intensity <= 0) {
            return new float[]{0, 0};
        }

        float progress = (elapsed + tickDelta) / duration;
        float decay = 1.0f - progress; // Linear decay
        // Or exponential: float decay = (float) Math.exp(-3.0 * progress);

        float time = (elapsed + tickDelta) * 0.05f * frequency;

        // Sample noise for smooth, organic shake
        float shakeX = (float) NOISE.sample(time, 0) * intensity * decay;
        float shakeZ = (float) NOISE.sample(0, time) * intensity * decay;

        return new float[]{shakeX, shakeZ};
    }
}
```

---

## Approach 3: MatrixStack Manipulation

### Core Concepts

**MatrixStack operations:**
```java
matrices.push();    // Save current state
matrices.pop();     // Restore previous state
matrices.translate(x, y, z);  // Move position
matrices.scale(x, y, z);      // Scale size
matrices.multiply(quaternion); // Rotate
```

**RotationAxis constants (1.20.1):**
```java
import net.minecraft.util.math.RotationAxis;

RotationAxis.POSITIVE_X  // Pitch (up/down tilt)
RotationAxis.POSITIVE_Y  // Yaw (left/right turn)
RotationAxis.POSITIVE_Z  // Roll (screen tilt)
```

**Creating rotations:**
```java
// Degrees
matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(15.0f));

// Radians
matrices.multiply(RotationAxis.POSITIVE_X.rotation(0.5f));
```

### Complete Shake Implementation

```java
public class CameraShakeRenderer {

    public static void applyShake(MatrixStack matrices, float tickDelta) {
        float[] shake = ScreenShakeManager.getShakeOffset(tickDelta);

        if (shake[0] != 0 || shake[1] != 0) {
            matrices.push();

            // Apply pitch shake (up/down)
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(shake[0]));

            // Apply roll shake (screen tilt)
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(shake[1]));

            // Optional: slight translation for more intense effect
            // matrices.translate(shake[0] * 0.01, shake[1] * 0.01, 0);
        }
    }

    public static void finishShake(MatrixStack matrices) {
        float[] shake = ScreenShakeManager.getShakeOffset(0);
        if (shake[0] != 0 || shake[1] != 0) {
            matrices.pop();
        }
    }
}
```

---

## Triggering Shake from Entities/Events

### From Entity Attack (Server -> Client)

**Server side (in entity goal or attack method):**
```java
// Send packet to nearby players
PacketByteBuf buf = PacketByteBufs.create();
buf.writeFloat(intensity);
buf.writeFloat(duration);
buf.writeBlockPos(this.getBlockPos());

for (ServerPlayerEntity player : world.getPlayers()) {
    if (player.squaredDistanceTo(this) < 64 * 64) {
        ServerPlayNetworking.send(player, SHAKE_PACKET_ID, buf);
    }
}
```

**Client side packet handler:**
```java
ClientPlayNetworking.registerGlobalReceiver(SHAKE_PACKET_ID, (client, handler, buf, sender) -> {
    float intensity = buf.readFloat();
    float duration = buf.readFloat();
    BlockPos origin = buf.readBlockPos();

    client.execute(() -> {
        // Distance-based intensity falloff
        if (client.player != null) {
            double dist = Math.sqrt(client.player.squaredDistanceTo(
                origin.getX(), origin.getY(), origin.getZ()));
            float falloff = (float) Math.max(0, 1.0 - dist / 32.0);
            ScreenShakeManager.shake(intensity * falloff, duration);
        }
    });
});
```

### From World Events

```java
// In your client tick handler
public static void onClientTick(MinecraftClient client) {
    ScreenShakeManager.tick();

    // Example: shake when player takes damage
    if (client.player != null && client.player.hurtTime == 9) {
        float damage = client.player.lastDamageTaken;
        ScreenShakeManager.shake(damage * 0.5f, 10);
    }
}
```

---

## Best Practices

1. **Always use decay** - Shake should fade out, not stop abruptly
2. **Distance falloff** - Shake intensity should decrease with distance from source
3. **Configurable** - Let players disable or reduce shake intensity (motion sickness)
4. **Don't overuse** - Reserve shake for impactful moments
5. **Use noise** - Perlin/Simplex noise creates smoother, more organic shake than random
6. **Consider frequency** - Faster shake (high frequency) feels sharp, slower feels heavy
7. **Combine axes** - Use both X (pitch) and Z (roll) for realistic shake
8. **Cap intensity** - Prevent extreme values that could cause disorientation

---

## Decay Functions

```java
// Linear decay (simple)
float decay = 1.0f - progress;

// Exponential decay (natural feel)
float decay = (float) Math.exp(-3.0 * progress);

// Ease-out cubic (smooth stop)
float decay = 1.0f - (progress * progress * progress);

// Bounce decay (for comedic effect)
float decay = (float) Math.abs(Math.cos(progress * Math.PI * 3)) * (1 - progress);
```

---

## References

- [fabric-camera-shake library](https://github.com/LoganDark/fabric-camera-shake)
- [CameraOverhaul mod](https://github.com/Mirsario/Minecraft-CameraOverhaul)
- [Fabric Rendering Concepts](https://docs.fabricmc.net/1.20.4/develop/rendering/basic-concepts)
- [MatrixStack API](https://maven.fabricmc.net/docs/yarn-1.20.6+build.2/net/minecraft/client/util/math/MatrixStack.html)
