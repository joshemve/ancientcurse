package com.ancientcurse.item;

import com.ancientcurse.effect.ModStatusEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

/**
 * Tablet of Thoth - Completely removes all Khamsin curse stages
 * A powerful magical artifact that cleanses all curse effects
 */
public class TabletOfThothItem extends Item {
    private static final int USE_DURATION = 60; // 3 seconds for powerful effect
    
    public TabletOfThothItem(Settings settings) {
        super(settings.maxCount(1)); // Very rare item
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        
        // Check if player has any Khamsin curse stage
        boolean hasCurse = false;
        for (int stage = 1; stage <= 5; stage++) {
            StatusEffect curseEffect = ModStatusEffects.getCurseStage(stage);
            if (user.hasStatusEffect(curseEffect)) {
                hasCurse = true;
                break;
            }
        }
        
        if (!hasCurse) {
            if (!world.isClient) {
                user.sendMessage(Text.translatable("item.ancientcurse.tablet_of_thoth.no_curse"), true);
            }
            return TypedActionResult.fail(itemStack);
        }
        
        return ItemUsage.consumeHeldItem(world, user, hand);
    }
    
    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (user instanceof PlayerEntity player) {
            if (!world.isClient) {
                // Remove all curse stages
                boolean removedCurse = false;
                for (int stage = 1; stage <= 5; stage++) {
                    StatusEffect curseEffect = ModStatusEffects.getCurseStage(stage);
                    if (player.hasStatusEffect(curseEffect)) {
                        player.removeStatusEffect(curseEffect);
                        removedCurse = true;
                    }
                }
                
                if (removedCurse) {
                    // Send feedback
                    player.sendMessage(Text.translatable("item.ancientcurse.tablet_of_thoth.used"), true);
                    
                    // Play powerful sound effect
                    world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS,
                        1.0F, 0.8F);
                    
                    // Additional enchanting sound for magical effect
                    world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS,
                        1.0F, 1.2F);
                    
                    // Consume item in survival
                    if (!player.getAbilities().creativeMode) {
                        stack.decrement(1);
                    }
                }
            } else {
                // Client-side magical particles
                // Create a burst of enchant particles
                for (int i = 0; i < 30; i++) {
                    double d = world.random.nextGaussian() * 0.05D;
                    double e = world.random.nextGaussian() * 0.05D;
                    double f = world.random.nextGaussian() * 0.05D;
                    world.addParticle(ParticleTypes.ENCHANT,
                        user.getX() + world.random.nextDouble() * 2.0D - 1.0D,
                        user.getY() + world.random.nextDouble() + 1.0D,
                        user.getZ() + world.random.nextDouble() * 2.0D - 1.0D,
                        d, e, f);
                }
                
                // Add some portal particles for extra magical effect
                for (int i = 0; i < 20; i++) {
                    world.addParticle(ParticleTypes.PORTAL,
                        user.getX() + world.random.nextDouble() * 2.0D - 1.0D,
                        user.getY() + world.random.nextDouble() * 2.0D,
                        user.getZ() + world.random.nextDouble() * 2.0D - 1.0D,
                        0, 0.5D, 0);
                }
                
                // Add happy villager particles to show cleansing
                for (int i = 0; i < 10; i++) {
                    world.addParticle(ParticleTypes.HAPPY_VILLAGER,
                        user.getX() + world.random.nextDouble() - 0.5D,
                        user.getY() + world.random.nextDouble() + 1.5D,
                        user.getZ() + world.random.nextDouble() - 0.5D,
                        0, 0, 0);
                }
            }
        }
        
        return stack;
    }
    
    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW; // Reading animation
    }
    
    @Override
    public int getMaxUseTime(ItemStack stack) {
        return USE_DURATION;
    }
    
    @Override
    public boolean hasGlint(ItemStack stack) {
        return true; // Always has enchantment glint
    }
    
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false; // Cannot be enchanted (already magical)
    }
}