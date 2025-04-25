package com.ancientcurse.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import net.minecraft.client.item.TooltipContext;

import java.util.List;

/**
 * Phial of Lotus Essence - A magical potion that grants water-related powers
 * Provides water breathing, night vision, and dolphin's grace when consumed
 * Perfect for underwater exploration in ancient ruins
 */
public class PhialOfLotusEssenceItem extends Item {
    
    public PhialOfLotusEssenceItem(Settings settings) {
        super(settings.maxCount(1)); // Can only stack to 1
    }
    
    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (user instanceof PlayerEntity player) {
            if (!player.getAbilities().creativeMode) {
                stack.decrement(1);
            }
            
            // Apply water-related effects
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 3600, 0)); // 3 minutes of water breathing
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 3600, 0)); // 3 minutes of night vision
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 2400, 0)); // 2 minutes of dolphin's grace
            
            // Create a water particle effect around the player
            if (!world.isClient) {
                world.playSound(null, player.getX(), player.getY(), player.getZ(), 
                    SoundEvents.ENTITY_DOLPHIN_SPLASH, SoundCategory.PLAYERS, 0.5F, 1.0F);
                
                // Server can't spawn particles directly, so we need to send a packet to clients
                // This is handled by the game automatically when we call the playSound method above
            }
            
            player.incrementStat(Stats.USED.getOrCreateStat(this));
            
            // Return an empty glass bottle when consumed
            if (stack.isEmpty()) {
                return new ItemStack(Items.GLASS_BOTTLE);
            } else {
                // If the player is not in creative mode, give them an empty bottle
                if (player instanceof PlayerEntity && !((PlayerEntity)player).getAbilities().creativeMode) {
                    ItemStack itemStack = new ItemStack(Items.GLASS_BOTTLE);
                    if (!player.getInventory().insertStack(itemStack)) {
                        player.dropItem(itemStack, false);
                    }
                }
                return stack;
            }
        }
        
        return stack;
    }
    
    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 32; // Drinking time
    }
    
    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return ItemUsage.consumeHeldItem(world, user, hand);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(Text.translatable("item.ancientcurse.phial_of_lotus_essence.tooltip").formatted(Formatting.AQUA));
    }
}
