package com.ancientcurse.mixin;

import com.ancientcurse.player.ThirstDataManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to make food items restore a small amount of thirst.
 * Juicy foods like melons restore more.
 */
@Mixin(Item.class)
public class FoodConsumptionMixin {

    @Inject(method = "finishUsing", at = @At("HEAD"))
    private void ancientcurse$onEatFood(ItemStack stack, World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        if (user instanceof ServerPlayerEntity player && !world.isClient) {
            // Only process food items
            if (stack.getItem().isFood()) {
                var foodComponent = stack.getItem().getFoodComponent();
                if (foodComponent != null) {
                    int hunger = foodComponent.getHunger();

                    // Calculate thirst restoration based on food type
                    // Juicy foods restore more thirst
                    int thirstRestored = 0;
                    float saturationRestored = 0;

                    String itemName = stack.getItem().toString().toLowerCase();

                    // Very juicy foods (melons, berries, apples)
                    if (itemName.contains("melon") || itemName.contains("berry") ||
                            itemName.contains("apple") || itemName.contains("chorus")) {
                        thirstRestored = Math.min(4, hunger);
                        saturationRestored = 2.0f;
                    }
                    // Moderately hydrating foods (soups, stews)
                    else if (itemName.contains("soup") || itemName.contains("stew") ||
                            itemName.contains("mushroom")) {
                        thirstRestored = Math.min(6, hunger);
                        saturationRestored = 3.0f;
                    }
                    // Mod-specific juicy foods
                    else if (itemName.contains("fig") || itemName.contains("date") ||
                            itemName.contains("lotus") || itemName.contains("spinach")) {
                        thirstRestored = Math.min(3, hunger);
                        saturationRestored = 1.5f;
                    }
                    // Dry foods (bread, meat, fish) - minimal hydration
                    else if (hunger > 0) {
                        thirstRestored = 1;
                        saturationRestored = 0.5f;
                    }

                    if (thirstRestored > 0) {
                        ThirstDataManager.restoreThirst(player, thirstRestored, saturationRestored);
                    }
                }
            }
        }
    }
}
