package com.ancientcurse.mixin.client;

import com.ancientcurse.item.armor.JackelBindsItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to prevent opening inventory/crafting screens when bound
 */
@Mixin(MinecraftClient.class)
public class JackelBindsScreenMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void ancientcurse$preventInventoryWhenBound(Screen screen, CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        PlayerEntity player = client.player;

        if (player == null) return;

        // Check if player is bound
        if (player.getEquippedStack(EquipmentSlot.CHEST).getItem() instanceof JackelBindsItem) {
            // Block inventory-related screens
            if (screen instanceof InventoryScreen ||
                screen instanceof CreativeInventoryScreen ||
                screen instanceof CraftingScreen ||
                screen instanceof AbstractInventoryScreen) {

                player.sendMessage(Text.literal("You cannot access your inventory while bound!").formatted(Formatting.RED), true);
                ci.cancel();
            }
        }
    }
}
