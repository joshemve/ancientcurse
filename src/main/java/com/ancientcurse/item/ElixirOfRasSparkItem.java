package com.ancientcurse.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.client.item.TooltipContext;
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

import java.util.List;

/**
 * Elixir of Ra's Spark - A magical potion that grants fire-related powers
 * Provides fire resistance and strength when consumed
 */
public class ElixirOfRasSparkItem extends Item {
    
    public ElixirOfRasSparkItem(Settings settings) {
        super(settings.maxCount(1)); // Can only stack to 1
    }
    
    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (user instanceof PlayerEntity player) {
            if (!player.getAbilities().creativeMode) {
                stack.decrement(1);
            }
            
            // Apply fire resistance and strength effects
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 1200, 0)); // 1 minute of fire resistance
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 600, 1)); // 30 seconds of Strength II
            
            // Create a fire ring around the player (client-side particle effect only)
            if (!world.isClient) {
                world.playSound(null, player.getX(), player.getY(), player.getZ(), 
                    SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.5F, 0.4F);
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
        tooltip.add(Text.translatable("item.ancientcurse.elixir_of_ras_spark.tooltip").formatted(Formatting.GOLD));
    }
}
