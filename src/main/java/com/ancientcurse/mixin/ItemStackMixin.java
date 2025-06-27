package com.ancientcurse.mixin;

import com.ancientcurse.AncientCurse;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    
    @Inject(method = "getTooltip", at = @At("RETURN"))
    private void appendModTooltip(List<Text> tooltip, TooltipContext context, CallbackInfo ci) {
        ItemStack stack = (ItemStack) (Object) this;
        
        // Only add tooltip to items from our mod
        if (stack.getItem().toString().contains(AncientCurse.MOD_ID)) {
            tooltip.add(Text.translatable("tooltip.ancientcurse.ancient_curse").formatted(Formatting.DARK_PURPLE));
        }
    }
}
