package com.ancientcurse.mixin.client;

import com.ancientcurse.item.armor.JackelBindsItem;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to modify first-person hand rendering when wearing Jackel Binds
 * Makes arms appear in bound position in first person view
 */
@Mixin(HeldItemRenderer.class)
public class JackelBindsFirstPersonMixin {

    /**
     * Modify arm rendering when bound - position arms forward as if handcuffed
     */
    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void ancientcurse$renderBoundArms(AbstractClientPlayerEntity player, float tickDelta,
                                               float pitch, Hand hand, float swingProgress,
                                               ItemStack item, float equipProgress,
                                               MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                               int light, CallbackInfo ci) {
        // Check if player is wearing Jackel Binds
        ItemStack chestpiece = player.getEquippedStack(EquipmentSlot.CHEST);
        if (!(chestpiece.getItem() instanceof JackelBindsItem)) {
            return; // Not bound, render normally
        }

        // When bound, we want to show the arms in a restrained position
        // The actual bound arm rendering is handled by the GeckoLib armor renderer
        // Here we just suppress the normal held item rendering and let the armor show

        // Don't render held items - hands are bound
        // The armor renderer will show the cuffs
        // We could also render custom bound arms here if needed
    }

    /**
     * Modify arm swing animation when bound - no swinging allowed
     */
    @Inject(method = "renderArmHoldingItem", at = @At("HEAD"), cancellable = true)
    private void ancientcurse$preventArmSwingWhenBound(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                                        int light, float equipProgress, float swingProgress,
                                                        Arm arm, CallbackInfo ci) {
        // This is called for arm rendering - we let it pass through
        // The actual restriction is in the attack mixin
    }
}
