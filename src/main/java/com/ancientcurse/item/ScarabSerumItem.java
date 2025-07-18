package com.ancientcurse.item;

import com.ancientcurse.util.AnkhManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

/**
 * Scarab Serum - Restores 50 Ankh points
 * Similar to Rad-Away from Fallout
 */
public class ScarabSerumItem extends Item {
    private static final int ANKH_RESTORATION = 50;
    private static final int USE_DURATION = 32; // 1.6 seconds
    
    public ScarabSerumItem(Settings settings) {
        super(settings.maxCount(16));
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        
        // Check if player needs restoration
        int currentAnkh = AnkhManager.getAnkhValue(user);
        if (currentAnkh >= 100) {
            if (!world.isClient) {
                user.sendMessage(Text.translatable("item.ancientcurse.scarab_serum.full"), true);
            }
            return TypedActionResult.fail(itemStack);
        }
        
        return ItemUsage.consumeHeldItem(world, user, hand);
    }
    
    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (user instanceof PlayerEntity player) {
            if (!world.isClient) {
                // Restore Ankh
                int oldAnkh = AnkhManager.getAnkhValue(player);
                int newAnkh = AnkhManager.increaseAnkhValue(player, ANKH_RESTORATION);
                
                // Send feedback
                player.sendMessage(Text.translatable("item.ancientcurse.scarab_serum.used", 
                    newAnkh - oldAnkh, newAnkh), true);
                
                // Play sound
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS,
                    1.0F, 1.0F);
                
                // Consume item in survival
                if (!player.getAbilities().creativeMode) {
                    stack.decrement(1);
                    
                    // Give empty bottle back
                    ItemStack emptyBottle = new ItemStack(Items.GLASS_BOTTLE);
                    if (!player.giveItemStack(emptyBottle)) {
                        player.dropItem(emptyBottle, false);
                    }
                }
            }
        }
        
        return stack;
    }
    
    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }
    
    @Override
    public int getMaxUseTime(ItemStack stack) {
        return USE_DURATION;
    }
    
    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}