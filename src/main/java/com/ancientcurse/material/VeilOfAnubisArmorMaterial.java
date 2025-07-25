package com.ancientcurse.material;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

public class VeilOfAnubisArmorMaterial implements ArmorMaterial {
    private static final int[] BASE_DURABILITY = new int[] {13, 15, 16, 11};
    private static final int[] PROTECTION_VALUES = new int[] {2, 6, 8, 2}; // Updated helmet to 2
    
    @Override
    public int getDurability(ArmorItem.Type type) {
        return BASE_DURABILITY[type.getEquipmentSlot().getEntitySlotId()] * 15; // Reduced for medium-high tier
    }
    
    @Override
    public int getProtection(ArmorItem.Type type) {
        return PROTECTION_VALUES[type.getEquipmentSlot().getEntitySlotId()];
    }
    
    @Override
    public int getEnchantability() {
        return 25;
    }
    
    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ITEM_ARMOR_EQUIP_GOLD;
    }
    
    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.EMPTY;
    }
    
    @Override
    public String getName() {
        return "veil_of_anubis";
    }
    
    @Override
    public float getToughness() {
        return 0.0F; // No toughness as requested
    }
    
    @Override
    public float getKnockbackResistance() {
        return 0.0F; // No knockback resistance as requested
    }
}