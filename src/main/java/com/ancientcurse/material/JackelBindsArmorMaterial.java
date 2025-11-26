package com.ancientcurse.material;

import com.ancientcurse.ModItems;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

/**
 * Armor material for Jackel Binds - restraining handcuffs
 * Low protection since they're restraints, not protective armor
 */
public class JackelBindsArmorMaterial implements ArmorMaterial {
    private static final int[] BASE_DURABILITY = new int[] {13, 15, 16, 11};
    private static final int[] PROTECTION_VALUES = new int[] {0, 0, 0, 0}; // No protection - these are restraints

    @Override
    public int getDurability(ArmorItem.Type type) {
        return BASE_DURABILITY[type.getEquipmentSlot().getEntitySlotId()] * 25; // Durable restraints
    }

    @Override
    public int getProtection(ArmorItem.Type type) {
        return PROTECTION_VALUES[type.getEquipmentSlot().getEntitySlotId()];
    }

    @Override
    public int getEnchantability() {
        return 1; // Very low - magical restraints don't enchant well
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.BLOCK_CHAIN_PLACE;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.ofItems(ModItems.BRONZE_INGOT);
    }

    @Override
    public String getName() {
        return "jackel_binds";
    }

    @Override
    public float getToughness() {
        return 0.0F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.0F;
    }
}
