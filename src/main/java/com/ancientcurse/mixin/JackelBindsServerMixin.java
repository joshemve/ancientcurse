package com.ancientcurse.mixin;

import com.ancientcurse.item.armor.JackelBindsItem;
import com.ancientcurse.system.JackelBindsBreakoutManager;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalInt;

/**
 * Server-side mixin to block bound players from opening containers and handle breakout punching
 */
@Mixin(ServerPlayerEntity.class)
public class JackelBindsServerMixin {

    @Unique
    private long ancientcurse$lastSwingTime = 0;

    /**
     * Block opening screen handlers (chests, crafting tables, etc.) when bound
     */
    @Inject(method = "openHandledScreen", at = @At("HEAD"), cancellable = true)
    private void ancientcurse$preventScreenOpenWhenBound(NamedScreenHandlerFactory factory, CallbackInfoReturnable<OptionalInt> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        ItemStack chestpiece = player.getEquippedStack(EquipmentSlot.CHEST);

        if (chestpiece.getItem() instanceof JackelBindsItem) {
            player.sendMessage(Text.literal("You cannot open containers while bound!").formatted(Formatting.RED), true);
            cir.setReturnValue(OptionalInt.empty());
        }
    }

    /**
     * Detect hand swings for breakout mechanic
     * This catches when the player swings their hand (left-click)
     */
    @Inject(method = "swingHand", at = @At("HEAD"))
    private void ancientcurse$onSwingHand(Hand hand, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        ItemStack chestpiece = player.getEquippedStack(EquipmentSlot.CHEST);

        if (chestpiece.getItem() instanceof JackelBindsItem) {
            long currentTime = player.getWorld().getTime();

            // Only process if enough time has passed since last swing (prevents tick spam)
            if (currentTime - ancientcurse$lastSwingTime >= 5) { // 0.25 second minimum
                ancientcurse$lastSwingTime = currentTime;
                JackelBindsBreakoutManager.getInstance().onBoundPlayerPunch(player);
            }
        }
    }
}
