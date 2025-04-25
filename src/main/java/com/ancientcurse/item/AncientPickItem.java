package com.ancientcurse.item;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/**
 * Ancient Pick - A mystical mining tool with special properties
 * Provides haste effect when mining and has a chance to not take durability damage
 * A starter-level tool with some special properties but not overpowered
 */
public class AncientPickItem extends PickaxeItem {
    
    public AncientPickItem(Settings settings) {
        super(ToolMaterials.IRON, 1, -2.8F, settings.maxDamage(250)); // Similar to iron but with special properties
    }
    
    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!world.isClient && state.getHardness(world, pos) != 0.0F) {
            // 15% chance to not take durability damage - reduced from 20%
            if (world.random.nextFloat() > 0.15f) {
                stack.damage(1, miner, e -> e.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND));
            } else if (miner instanceof PlayerEntity) {
                // Play a special sound when the pick preserves durability
                world.playSound(null, miner.getX(), miner.getY(), miner.getZ(), 
                    SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.5F, 1.0F);
            }
            
            // Apply a short haste effect when mining - reduced chance to 5%
            if (miner instanceof PlayerEntity && world.random.nextFloat() < 0.05f) {
                ((PlayerEntity) miner).addStatusEffect(
                    new StatusEffectInstance(StatusEffects.HASTE, 60, 0, false, false, true)
                );
            }
        }
        
        return true;
    }
    
    @Override
    public float getMiningSpeedMultiplier(ItemStack stack, BlockState state) {
        // Enhanced mining speed for ancient structures and stone
        if (state.isIn(BlockTags.BASE_STONE_OVERWORLD) || 
            state.isIn(BlockTags.STONE_ORE_REPLACEABLES) || 
            state.isIn(BlockTags.SAND)) {
            return 8.0F; // Better than iron but not as good as diamond for these blocks
        }
        return super.getMiningSpeedMultiplier(stack, state);
    }
    
    // The PickaxeItem parent class already handles the isSuitableFor method
    // so we don't need to override it - the tool will use IRON mining level
    
    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(Text.translatable("item.ancientcurse.ancient_pick.tooltip").formatted(Formatting.GOLD));
    }
}
