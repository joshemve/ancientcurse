package com.ancientcurse.material;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

public class VeilOfAnubisArmorMaterial implements ArmorMaterial {
    private static final int[] BASE_DURABILITY = new int[] {13, 15, 16, 11};
    private static final int[] PROTECTION_VALUES = new int[] {3, 6, 8, 3};
    
    @Override
    public int getDurability(ArmorItem.Type type) {
        return BASE_DURABILITY[type.getEquipmentSlot().getEntitySlotId()] * 33;
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
        return 2.0F;
    }
    
    @Override
    public float getKnockbackResistance() {
        return 0.1F;
    }
}