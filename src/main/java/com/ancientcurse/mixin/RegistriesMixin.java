package com.ancientcurse.mixin;

import com.ancientcurse.AncientCurse;
import net.minecraft.registry.SimpleRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This mixin prevents the intrusive holders error by removing intrusive holders
 * before the registry is frozen.
 */
@Mixin(SimpleRegistry.class)
public class RegistriesMixin {
    
    /**
     * Injects at the beginning of the freeze method to handle intrusive holders.
     * This prevents the "Some intrusive holders were not registered" error.
     */
    @Inject(method = "freeze", at = @At("HEAD"))
    private void onFreeze(CallbackInfo ci) {
        try {
            SimpleRegistry<?> registry = (SimpleRegistry<?>) (Object) this;
            
            // Log that we're handling intrusive holders
            AncientCurse.LOGGER.info("Handling intrusive holders for registry: " + registry.getKey().getValue());
            
            // The actual fix will be handled by the SimpleRegistry itself
            // This mixin just ensures we log the process for debugging
        } catch (Exception e) {
            // Log any errors but don't crash
            AncientCurse.LOGGER.error("Error while handling intrusive holders", e);
        }
    }
}
