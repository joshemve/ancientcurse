package com.ancientcurse.material;

import com.ancientcurse.ModItems;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

public class BronzeArmorMaterial implements ArmorMaterial {
    private static final int[] BASE_DURABILITY = new int[] {13, 15, 16, 11};
    private static final int[] PROTECTION_VALUES = new int[] {2, 5, 6, 2}; // Better than iron, worse than diamond
    
    @Override
    public int getDurability(ArmorItem.Type type) {
        return BASE_DURABILITY[type.getEquipmentSlot().getEntitySlotId()] * 20; // Between iron and diamond
    }
    
    @Override
    public int getProtection(ArmorItem.Type type) {
        return PROTECTION_VALUES[type.getEquipmentSlot().getEntitySlotId()];
    }
    
    @Override
    public int getEnchantability() {
        return 12; // Better than iron (9), worse than gold (25)
    }
    
    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ITEM_ARMOR_EQUIP_IRON;
    }
    
    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.ofItems(ModItems.BRONZE_INGOT);
    }
    
    @Override
    public String getName() {
        return "bronze";
    }
    
    @Override
    public float getToughness() {
        return 1.0F; // Between iron (0) and diamond (2)
    }
    
    @Override
    public float getKnockbackResistance() {
        return 0.0F;
    }
}