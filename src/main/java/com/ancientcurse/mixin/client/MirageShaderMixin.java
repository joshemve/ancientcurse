package com.ancientcurse.mixin.client;

import com.ancientcurse.client.MirageClientHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects mirage post-processing shader into the render pipeline.
 * Applied after world rendering completes but before HUD drawing,
 * so the world is distorted but UI remains crisp.
 */
@Mixin(GameRenderer.class)
@Environment(EnvType.CLIENT)
public class MirageShaderMixin {

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/GameRenderer;renderWorld(FJLnet/minecraft/client/util/math/MatrixStack;)V",
            shift = At.Shift.AFTER
        )
    )
    private void ancientcurse$applyMirageShader(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        MirageClientHandler.applyPostProcessing(tickDelta);
    }

    @Inject(method = "onResized", at = @At("TAIL"))
    private void ancientcurse$resizeMirageShader(int width, int height, CallbackInfo ci) {
        MirageClientHandler.onResized(width, height);
    }
}
