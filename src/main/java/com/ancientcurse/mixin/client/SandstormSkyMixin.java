package com.ancientcurse.mixin.client;

import com.ancientcurse.client.SandstormClientHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.class)
public class SandstormSkyMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void modifySkyColor(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
        if (SandstormClientHandler.isRenderingSandstorm()) {
            float intensity = SandstormClientHandler.getIntensity();
            Vec3d original = cir.getReturnValue();
            
            // Sand color
            Vec3d sandColor = new Vec3d(0.8, 0.6, 0.4);
            
            // Interpolate
            cir.setReturnValue(original.lerp(sandColor, intensity));
        }
    }
}
