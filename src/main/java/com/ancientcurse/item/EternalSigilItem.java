package com.ancientcurse.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import com.ancientcurse.effect.ModStatusEffects;

import java.util.List;

/**
 * Eternal Sigil - A powerful ancient artifact that provides long-lasting protection
 * When activated, it removes all Khamsin curse effects and grants powerful protective buffs
 */
public class EternalSigilItem extends Item {
    
    // Removed cooldown and active state tracking since item is consumed
    
    public EternalSigilItem(Settings settings) {
        super(settings.maxCount(1).fireproof()); // Can only stack to 1 and is fireproof
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        
        if (!world.isClient) {
            // Clear ALL status effects (both positive and negative)
            player.clearStatusEffects();
            
            player.sendMessage(Text.literal("The Eternal Sigil's divine power cleanses your body and soul!").formatted(Formatting.GOLD), false);
            
            // Apply golden apple-like effects
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 400, 1)); // 20 seconds of Regeneration II
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 6000, 0)); // 5 minutes of Resistance I
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 6000, 0)); // 5 minutes of Fire Resistance
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 2400, 3)); // 2 minutes of Absorption IV (8 extra hearts)
            
            // Play activation sound
            world.playSound(null, player.getX(), player.getY(), player.getZ(), 
                SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0F, 1.0F);
            world.playSound(null, player.getX(), player.getY(), player.getZ(), 
                SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.5F, 0.8F);
            
            // Consume the item
            if (!player.getAbilities().creativeMode) {
                stack.decrement(1);
            }
        } else {
            // Client-side particle effects
            createActivationParticles(world, player);
        }
        
        return TypedActionResult.success(stack, world.isClient());
    }
    
    private void createActivationParticles(World world, PlayerEntity player) {
        // Create a burst of divine particles
        for (int i = 0; i < 50; i++) {
            double radius = 2.0;
            double angle = i * (Math.PI * 2) / 50;
            
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            
            // Golden particles
            world.addParticle(
                ParticleTypes.END_ROD,
                player.getX() + offsetX,
                player.getY() + 1.0,
                player.getZ() + offsetZ,
                offsetX * 0.1, 0.2, offsetZ * 0.1
            );
            
            // Some totem particles for extra effect
            if (i % 5 == 0) {
                world.addParticle(
                    ParticleTypes.TOTEM_OF_UNDYING,
                    player.getX() + offsetX * 0.5,
                    player.getY() + 2.0,
                    player.getZ() + offsetZ * 0.5,
                    offsetX * 0.05, 0.1, offsetZ * 0.05
                );
            }
        }
    }
    
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        
        // Add ambient effects when held
        if (world.isClient) {
            // Emit divine light when held in hand
            if (selected || (entity instanceof PlayerEntity player && player.getOffHandStack() == stack)) {
                // Emit golden particles for the light effect
                if (world.getTime() % 10 == 0) {
                    double offsetX = (world.random.nextFloat() - 0.5) * 0.5;
                    double offsetY = (world.random.nextFloat() - 0.5) * 0.5;
                    double offsetZ = (world.random.nextFloat() - 0.5) * 0.5;
                    
                    world.addParticle(
                        ParticleTypes.END_ROD,
                        entity.getX() + offsetX,
                        entity.getY() + 1.0 + offsetY,
                        entity.getZ() + offsetZ,
                        0, 0.05, 0
                    );
                }
            }
        }
    }
    

    // Removed state tracking methods since item is consumed on use
    
    @Override
    public boolean hasGlint(ItemStack stack) {
        // Always show enchantment glint for this divine artifact
        return true;
    }
    
    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(Text.translatable("item.ancientcurse.eternal_sigil.tooltip").formatted(Formatting.GOLD));
        tooltip.add(Text.literal("Cleanses all effects and grants divine protection").formatted(Formatting.LIGHT_PURPLE));
        tooltip.add(Text.literal("Consumed on use").formatted(Formatting.RED));
    }
}
