package com.ancientcurse.item;

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
import net.minecraft.client.item.TooltipContext;

import java.util.List;

/**
 * Scarab Talisman - A magical amulet that provides protection and healing
 * When activated, it grants temporary protection and regeneration
 * Has a cooldown period between uses
 */
public class ScarabTalismanItem extends Item {
    
    private static final int COOLDOWN_TICKS = 6000; // 5-minute cooldown (20 ticks per second * 60 seconds * 5 minutes)
    private static final String LAST_USED_KEY = "LastUsedTime";
    
    public ScarabTalismanItem(Settings settings) {
        super(settings.maxCount(1)); // Can only stack to 1
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        
        // Check if the talisman is on cooldown
        if (isOnCooldown(stack, world)) {
            if (!world.isClient) {
                long timeLeft = getCooldownTimeLeft(stack, world);
                int secondsLeft = (int) (timeLeft / 20);
                player.sendMessage(Text.literal("The Scarab Talisman is still recharging. (" + secondsLeft + "s remaining)").formatted(Formatting.RED), true);
            }
            return TypedActionResult.fail(stack);
        }
        
        // Activate the talisman's protective effects
        if (!world.isClient) {
            // Apply protection and regeneration effects
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 1)); // 10 seconds of Resistance II
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 1)); // 5 seconds of Regeneration II
            
            // Set the cooldown
            setLastUsedTime(stack, world.getTime());
            
            // Play activation sound
            world.playSound(null, player.getX(), player.getY(), player.getZ(), 
                SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 0.5F, 1.2F);
        } else {
            // Client-side particle effects
            for (int i = 0; i < 20; i++) {
                double offsetX = world.random.nextGaussian() * 0.2;
                double offsetY = world.random.nextGaussian() * 0.2;
                double offsetZ = world.random.nextGaussian() * 0.2;
                
                world.addParticle(
                    ParticleTypes.ENCHANT,
                    player.getX() + offsetX,
                    player.getY() + 1.0 + offsetY,
                    player.getZ() + offsetZ,
                    0, 0.1, 0
                );
            }
        }
        
        return TypedActionResult.success(stack);
    }
    
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        
        // Add a subtle effect when the talisman is ready to use again
        if (!world.isClient && entity instanceof PlayerEntity && !isOnCooldown(stack, world)) {
            if (world.getTime() % 80 == 0 && (selected || isInOffhand((PlayerEntity)entity, stack))) {
                // Play a subtle sound when ready
                world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), 
                    SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.15F, 1.5F);
            }
        }
    }
    
    private boolean isInOffhand(PlayerEntity player, ItemStack stack) {
        return player.getOffHandStack() == stack;
    }
    
    private boolean isOnCooldown(ItemStack stack, World world) {
        long lastUsed = getLastUsedTime(stack);
        return world.getTime() - lastUsed < COOLDOWN_TICKS;
    }
    
    private long getCooldownTimeLeft(ItemStack stack, World world) {
        long lastUsed = getLastUsedTime(stack);
        return COOLDOWN_TICKS - (world.getTime() - lastUsed);
    }
    
    private long getLastUsedTime(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        return nbt.getLong(LAST_USED_KEY);
    }
    
    private void setLastUsedTime(ItemStack stack, long time) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putLong(LAST_USED_KEY, time);
    }
    
    @Override
    public boolean hasGlint(ItemStack stack) {
        // Always show the enchantment glint
        return true;
    }
    
    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(Text.translatable("item.ancientcurse.scarab_talisman.tooltip").formatted(Formatting.GOLD));
        
        // Show cooldown information if the talisman is on cooldown
        if (world != null && isOnCooldown(stack, world)) {
            long timeLeft = getCooldownTimeLeft(stack, world);
            int secondsLeft = (int) (timeLeft / 20);
            tooltip.add(Text.literal("Recharging: " + secondsLeft + "s").formatted(Formatting.RED));
        } else {
            tooltip.add(Text.literal("Ready to use").formatted(Formatting.GREEN));
        }
    }
}
