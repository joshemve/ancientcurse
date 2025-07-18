package com.ancientcurse.item;

import com.ancientcurse.effect.KhamsinCurseEffect;
import com.ancientcurse.effect.ModStatusEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
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
 * Scroll of Unbinding - Reverts khamsin curse by 1 stage
 * Only reduces curse down to stage 1, does not clear stage 1
 */
public class ScrollOfUnbindingItem extends Item {
    private static final int USE_DURATION = 40; // 2 seconds
    
    public ScrollOfUnbindingItem(Settings settings) {
        super(settings.maxCount(16));
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        
        // Check if player has any Khamsin curse stage
        StatusEffectInstance currentCurse = null;
        int currentStage = 0;
        
        for (int stage = 5; stage >= 1; stage--) {
            StatusEffect curseEffect = ModStatusEffects.getCurseStage(stage);
            if (user.hasStatusEffect(curseEffect)) {
                currentCurse = user.getStatusEffect(curseEffect);
                currentStage = stage;
                break;
            }
        }
        
        if (currentCurse == null) {
            if (!world.isClient) {
                user.sendMessage(Text.translatable("item.ancientcurse.scroll_of_unbinding.no_curse"), true);
            }
            return TypedActionResult.fail(itemStack);
        }
        
        if (currentStage == 1) {
            if (!world.isClient) {
                user.sendMessage(Text.translatable("item.ancientcurse.scroll_of_unbinding.minimum_stage"), true);
            }
            return TypedActionResult.fail(itemStack);
        }
        
        return ItemUsage.consumeHeldItem(world, user, hand);
    }
    
    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (user instanceof PlayerEntity player) {
            if (!world.isClient) {
                // Find current curse stage
                int currentStage = 0;
                StatusEffect currentCurseEffect = null;
                int remainingDuration = 0;
                
                for (int stage = 5; stage >= 1; stage--) {
                    StatusEffect curseEffect = ModStatusEffects.getCurseStage(stage);
                    if (player.hasStatusEffect(curseEffect)) {
                        currentCurseEffect = curseEffect;
                        currentStage = stage;
                        remainingDuration = player.getStatusEffect(curseEffect).getDuration();
                        break;
                    }
                }
                
                if (currentStage > 1) {
                    // Remove current stage
                    player.removeStatusEffect(currentCurseEffect);
                    
                    // Apply previous stage with remaining duration
                    StatusEffect previousStage = ModStatusEffects.getCurseStage(currentStage - 1);
                    player.addStatusEffect(new StatusEffectInstance(
                        previousStage, 
                        remainingDuration, 
                        0, 
                        false, 
                        false, 
                        true
                    ));
                    
                    // Send feedback
                    player.sendMessage(Text.translatable("item.ancientcurse.scroll_of_unbinding.used", 
                        currentStage, currentStage - 1), true);
                    
                    // Play sound
                    world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS,
                        1.0F, 1.0F);
                    
                    // Consume item in survival
                    if (!player.getAbilities().creativeMode) {
                        stack.decrement(1);
                    }
                }
            } else {
                // Client-side particles
                for (int i = 0; i < 15; i++) {
                    double d = world.random.nextGaussian() * 0.02D;
                    double e = world.random.nextGaussian() * 0.02D;
                    double f = world.random.nextGaussian() * 0.02D;
                    world.addParticle(ParticleTypes.ENCHANT,
                        user.getX() + world.random.nextDouble() - 0.5D,
                        user.getY() + world.random.nextDouble() + 1.0D,
                        user.getZ() + world.random.nextDouble() - 0.5D,
                        d, e, f);
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
        return true;
    }
}