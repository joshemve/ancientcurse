package com.ancientcurse.item;

import com.ancientcurse.player.AnkhDataManager;
import net.minecraft.entity.LivingEntity;
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
 * Pharaoh's Blood - Fully restores Ankh to 100
 * Similar to the cure from Fallout
 * Rare and powerful restoration item
 */
public class PharaohsBloodItem extends Item {
    private static final int USE_DURATION = 40; // 2 seconds
    
    public PharaohsBloodItem(Settings settings) {
        super(settings.maxCount(1)); // Very rare, only stack 1
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        
        // Check if player needs restoration
        int currentAnkh = AnkhDataManager.getAnkhValue(user);
        if (currentAnkh >= 100) {
            if (!world.isClient) {
                user.sendMessage(Text.translatable("item.ancientcurse.pharaohs_blood.full"), true);
            }
            return TypedActionResult.fail(itemStack);
        }
        
        return ItemUsage.consumeHeldItem(world, user, hand);
    }
    
    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (user instanceof PlayerEntity player) {
            if (!world.isClient) {
                // Fully restore Ankh
                int oldAnkh = AnkhDataManager.getAnkhValue(player);
                AnkhDataManager.setAnkhValue(player, 100);
                
                // Send feedback
                player.sendMessage(Text.translatable("item.ancientcurse.pharaohs_blood.used", 
                    100 - oldAnkh), true);
                
                // Play sound
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS,
                    1.0F, 1.0F);
                
                // Spawn divine particles
                for (int i = 0; i < 20; i++) {
                    double d = world.random.nextGaussian() * 0.02D;
                    double e = world.random.nextGaussian() * 0.02D;
                    double f = world.random.nextGaussian() * 0.02D;
                    world.addParticle(ParticleTypes.END_ROD,
                        player.getX() + world.random.nextDouble() - 0.5D,
                        player.getY() + world.random.nextDouble() + 1.0D,
                        player.getZ() + world.random.nextDouble() - 0.5D,
                        d, e, f);
                }
                
                // Consume item in survival
                if (!player.getAbilities().creativeMode) {
                    stack.decrement(1);
                }
            } else {
                // Client-side particles while drinking
                if (world.getTime() % 5 == 0) {
                    world.addParticle(ParticleTypes.END_ROD,
                        player.getX() + (world.random.nextDouble() - 0.5D) * 0.5D,
                        player.getY() + 1.5D,
                        player.getZ() + (world.random.nextDouble() - 0.5D) * 0.5D,
                        0, 0.1D, 0);
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
        return true; // Always enchanted looking
    }
    
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false; // Cannot be enchanted
    }
}