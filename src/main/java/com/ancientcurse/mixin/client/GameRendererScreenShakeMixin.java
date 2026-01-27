package com.ancientcurse.mixin.client;

import com.ancientcurse.client.ScreenShakeManager;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to apply screen shake effects during world rendering.
 * Injects after tiltViewWhenHurt to rotate the view matrix.
 */
@Mixin(GameRenderer.class)
public class GameRendererScreenShakeMixin {

    /**
     * Inject after the tiltViewWhenHurt call to add our custom shake on top.
     */
    @Inject(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;tiltViewWhenHurt(Lnet/minecraft/client/util/math/MatrixStack;F)V", shift = At.Shift.AFTER))
    private void ancientcurse$applyScreenShake(float tickDelta, long limitTime, MatrixStack matrices, CallbackInfo ci) {
        if (ScreenShakeManager.isShaking()) {
            float[] shake = ScreenShakeManager.getShakeOffset(tickDelta);

            // ENTRY LOG

            if (shake[0] != 0 || shake[1] != 0) {

                // Apply pitch shake (up/down tilt)
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(shake[0]));

                // Apply roll shake (screen tilt)
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(shake[1]));
            }
        }
    }
}
