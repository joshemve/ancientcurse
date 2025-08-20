package com.ancientcurse.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.particle.ParticleTypes;
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
import com.ancientcurse.player.AnkhDataManager;

import java.util.List;

/**
 * Scarab Incense Item - A magical incense that restores Ankh points and provides protection
 * When used, it restores 30 Ankh points and grants beneficial effects
 */
public class ScarabIncenseItem extends Item {
    
    private static final int USE_TIME = 32; // Time to use the item (in ticks)
    
    public ScarabIncenseItem(Settings settings) {
        super(settings.maxCount(16)); // Can stack up to 16
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        return ItemUsage.consumeHeldItem(world, player, hand);
    }
    
    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (user instanceof PlayerEntity player) {
            player.incrementStat(Stats.USED.getOrCreateStat(this));
            
            if (!world.isClient) {
                // Restore Ankh points
                int currentAnkh = AnkhDataManager.getAnkhValue(player);
                int restored = 0;
                if (currentAnkh < 100) {
                    int newAnkh = AnkhDataManager.increaseAnkhValue(player, 30);
                    restored = newAnkh - currentAnkh;
                    player.sendMessage(Text.literal(String.format("§bAnkh restored by %d points! §a(Now at %d/100)§r", restored, newAnkh)).formatted(Formatting.AQUA), false);
                } else {
                    player.sendMessage(Text.literal("Your Ankh is already at maximum").formatted(Formatting.YELLOW), true);
                }
                
                // Apply beneficial effects
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 600, 0)); // 30 seconds of Resistance I
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 0)); // 10 seconds of Regeneration I
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1200, 0)); // 60 seconds of Night Vision
                
                // Play sound effect
                world.playSound(null, player.getX(), player.getY(), player.getZ(), 
                    SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 0.5F, 0.8F);
                
                // Removed chat message for cleaner experience
                
                // Consume the item
                if (!player.getAbilities().creativeMode) {
                    stack.decrement(1);
                }
            } else {
                // Client-side particle effects
                createIncenseParticles(world, player);
            }
        }
        
        return stack;
    }
    
    private void createIncenseParticles(World world, PlayerEntity player) {
        // Create a cloud of particles around the player
        for (int i = 0; i < 30; i++) {
            double radius = 1.5;
            double angle = world.random.nextDouble() * Math.PI * 2;
            
            double offsetX = Math.cos(angle) * radius * world.random.nextDouble();
            double offsetY = world.random.nextDouble() * 2;
            double offsetZ = Math.sin(angle) * radius * world.random.nextDouble();
            
            world.addParticle(
                ParticleTypes.SMOKE,
                player.getX() + offsetX,
                player.getY() + offsetY,
                player.getZ() + offsetZ,
                0, 0.05, 0
            );
            
            if (i % 3 == 0) {
                world.addParticle(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    player.getX() + offsetX * 0.5,
                    player.getY() + offsetY * 0.5,
                    player.getZ() + offsetZ * 0.5,
                    0, 0.01, 0
                );
            }
        }
    }
    
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        
        // Occasionally emit subtle particles when held
        if (world.isClient && entity instanceof PlayerEntity && selected && world.random.nextInt(20) == 0) {
            double offsetX = world.random.nextGaussian() * 0.1;
            double offsetY = world.random.nextGaussian() * 0.1 + 0.2;
            double offsetZ = world.random.nextGaussian() * 0.1;
            
            world.addParticle(
                ParticleTypes.SMOKE,
                entity.getX() + offsetX,
                entity.getY() + offsetY,
                entity.getZ() + offsetZ,
                0, 0.01, 0
            );
        }
    }
    
    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.SPEAR; // Use a spear-like animation when using the incense
    }
    
    @Override
    public int getMaxUseTime(ItemStack stack) {
        return USE_TIME;
    }
    
    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(Text.translatable("item.ancientcurse.scarab_incense_item.tooltip").formatted(Formatting.GOLD));
        tooltip.add(Text.literal("Restores 30 Ankh points when used").formatted(Formatting.AQUA));
    }
}
