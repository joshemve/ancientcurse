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

import java.util.List;

/**
 * Eternal Sigil - A powerful ancient artifact that provides long-lasting protection
 * When activated, it grants the user powerful protective effects and can be toggled on/off
 */
public class EternalSigilItem extends Item {
    
    private static final int COOLDOWN_TICKS = 1200; // 1-minute cooldown (20 ticks per second * 60 seconds)
    private static final String ACTIVE_KEY = "Active";
    private static final String LAST_USED_KEY = "LastUsedTime";
    
    public EternalSigilItem(Settings settings) {
        super(settings.maxCount(1).fireproof()); // Can only stack to 1 and is fireproof
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        
        // Check if the sigil is on cooldown
        if (isOnCooldown(stack, world)) {
            if (!world.isClient) {
                long timeLeft = getCooldownTimeLeft(stack, world);
                int secondsLeft = (int) (timeLeft / 20);
                player.sendMessage(Text.literal("The Eternal Sigil is still recharging. (" + secondsLeft + "s remaining)").formatted(Formatting.RED), true);
            }
            return TypedActionResult.fail(stack);
        }
        
        // Toggle the sigil's active state
        boolean isActive = isActive(stack);
        setActive(stack, !isActive);
        
        if (!world.isClient) {
            // Set the cooldown
            setLastUsedTime(stack, world.getTime());
            
            if (isActive(stack)) {
                // Sigil was just activated
                player.sendMessage(Text.literal("The Eternal Sigil awakens with ancient power").formatted(Formatting.GOLD), true);
                
                // Apply powerful protective effects
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 6000, 1)); // 5 minutes of Resistance II
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 6000, 0)); // 5 minutes of Fire Resistance
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 6000, 1)); // 5 minutes of Absorption II
                
                // Play activation sound
                world.playSound(null, player.getX(), player.getY(), player.getZ(), 
                    SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 0.8F, 0.8F);
            } else {
                // Sigil was just deactivated
                player.sendMessage(Text.literal("The Eternal Sigil returns to dormancy").formatted(Formatting.GRAY), true);
                
                // Play deactivation sound
                world.playSound(null, player.getX(), player.getY(), player.getZ(), 
                    SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 0.8F, 0.8F);
            }
        } else {
            // Client-side particle effects
            createToggleParticles(world, player, isActive(stack));
        }
        
        return TypedActionResult.success(stack);
    }
    
    private void createToggleParticles(World world, PlayerEntity player, boolean active) {
        // Create different particle effects based on whether the sigil is being activated or deactivated
        for (int i = 0; i < 25; i++) {
            double radius = 1.0;
            double angle = i * (Math.PI * 2) / 25;
            
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            
            if (active) {
                // Golden particles for activation
                world.addParticle(
                    ParticleTypes.END_ROD,
                    player.getX() + offsetX,
                    player.getY() + 1.0,
                    player.getZ() + offsetZ,
                    offsetX * 0.1, 0.1, offsetZ * 0.1
                );
            } else {
                // Purple particles for deactivation
                world.addParticle(
                    ParticleTypes.REVERSE_PORTAL,
                    player.getX() + offsetX,
                    player.getY() + 1.0,
                    player.getZ() + offsetZ,
                    0, -0.05, 0
                );
            }
        }
    }
    
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        
        // Add ambient effects when the sigil is active or selected
        if (world.isClient) {
            // Emit purple light when held in hand
            if (selected || (entity instanceof PlayerEntity player && player.getOffHandStack() == stack)) {
                // Emit purple particles for the light effect
                if (world.getTime() % 5 == 0) { // More frequent for better light effect
                    double offsetX = (world.random.nextFloat() - 0.5) * 0.5;
                    double offsetY = (world.random.nextFloat() - 0.5) * 0.5;
                    double offsetZ = (world.random.nextFloat() - 0.5) * 0.5;
                    
                    world.addParticle(
                        ParticleTypes.PORTAL, // Purple particles
                        entity.getX() + offsetX,
                        entity.getY() + 1.0 + offsetY,
                        entity.getZ() + offsetZ,
                        0, 0.1, 0 // Slight upward movement
                    );
                }
            }
            
            // Additional effects when active
            if (isActive(stack) && entity instanceof PlayerEntity) {
                if (world.getTime() % 20 == 0) { // Once per second
                    // Emit subtle particles around the player
                    double offsetX = world.random.nextGaussian() * 0.5;
                    double offsetY = world.random.nextGaussian() * 0.5;
                    double offsetZ = world.random.nextGaussian() * 0.5;
                    
                    world.addParticle(
                        ParticleTypes.END_ROD,
                        entity.getX() + offsetX,
                        entity.getY() + 1.0 + offsetY,
                        entity.getZ() + offsetZ,
                        0, 0, 0
                    );
                }
            }
        }
        
        // Apply effects if active
        if (!world.isClient && entity instanceof PlayerEntity player && isActive(stack)) {
            // Apply effects every 20 ticks (once per second)
            if (world.getTime() % 20 == 0) {
                // Apply protection effects
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 1, false, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 40, 0, false, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 6000, 0, false, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 6000, 1, false, false, true));
            }
        }
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
    
    private boolean isActive(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        return nbt.getBoolean(ACTIVE_KEY);
    }
    
    private void setActive(ItemStack stack, boolean active) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putBoolean(ACTIVE_KEY, active);
    }
    
    @Override
    public boolean hasGlint(ItemStack stack) {
        // Show the enchantment glint when the sigil is active
        return isActive(stack);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(Text.translatable("item.ancientcurse.eternal_sigil.tooltip").formatted(Formatting.GOLD));
        
        // Show active state
        if (isActive(stack)) {
            tooltip.add(Text.literal("Status: Active").formatted(Formatting.GREEN));
        } else {
            tooltip.add(Text.literal("Status: Dormant").formatted(Formatting.GRAY));
        }
        
        // Show cooldown information if the sigil is on cooldown
        if (world != null && isOnCooldown(stack, world)) {
            long timeLeft = getCooldownTimeLeft(stack, world);
            int secondsLeft = (int) (timeLeft / 20);
            tooltip.add(Text.literal("Recharging: " + secondsLeft + "s").formatted(Formatting.RED));
        }
    }
}
