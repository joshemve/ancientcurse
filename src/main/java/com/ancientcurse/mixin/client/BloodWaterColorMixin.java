package com.ancientcurse.mixin.client;

import com.ancientcurse.client.BloodWaterClientHandler;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.world.BlockRenderView;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to override water color when Blood Water event is active.
 * Intercepts BiomeColors.getWaterColor to return blood-red.
 * This is the most efficient approach as it only affects rendering,
 * not actual block states.
 *
 * The chunk reload in BloodWaterClientHandler ensures all water
 * blocks get re-rendered when the state changes.
 */
@Mixin(BiomeColors.class)
public class BloodWaterColorMixin {

    /**
     * Modify water surface color to blood red when event is active.
     * This is called by FluidRenderer for every water block vertex.
     */
    @Inject(method = "getWaterColor", at = @At("RETURN"), cancellable = true)
    private static void modifyWaterColor(BlockRenderView world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (BloodWaterClientHandler.isBloodWaterActive()) {
            int originalColor = cir.getReturnValue();
            int bloodColor = BloodWaterClientHandler.getBloodWaterColor(originalColor);
            cir.setReturnValue(bloodColor);
        }
    }
}
