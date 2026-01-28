package com.ancientcurse.mixin;

import com.ancientcurse.event.WaterInteractionHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to hook into potion drinking to restore thirst.
 */
@Mixin(PotionItem.class)
public class PotionItemMixin {

    @Inject(method = "finishUsing", at = @At("HEAD"))
    private void ancientcurse$onFinishDrinking(ItemStack stack, World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        if (user instanceof PlayerEntity player && !world.isClient) {
            // Check if it's a water bottle (no effects)
            var potion = PotionUtil.getPotion(stack);
            if (potion == Potions.WATER) {
                // Water bottle - restores more thirst
                WaterInteractionHandler.onDrinkWaterBottle(player);
            } else {
                // Other potions - restore some thirst since you're drinking liquid
                WaterInteractionHandler.onDrinkPotion(player);
            }
        }
    }
}
