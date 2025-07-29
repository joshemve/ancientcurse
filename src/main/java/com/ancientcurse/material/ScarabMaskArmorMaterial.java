package com.ancientcurse.material;

import com.ancientcurse.ModItems;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

public class ScarabMaskArmorMaterial implements ArmorMaterial {
    
    @Override
    public int getDurability(ArmorItem.Type type) {
        return 275; // Between iron (240) and diamond (363)
    }
    
    @Override
    public int getProtection(ArmorItem.Type type) {
        return 3; // Same as iron helmet
    }
    
    @Override
    public int getEnchantability() {
        return 15; // Same as gold, very enchantable
    }
    
    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ITEM_ARMOR_EQUIP_GOLD; // Golden/metallic sound
    }
    
    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.ofItems(ModItems.SCARAB_SHELL);
    }
    
    @Override
    public String getName() {
        return "scarab_mask";
    }
    
    @Override
    public float getToughness() {
        return 1.0f; // Between iron (0) and diamond (2)
    }
    
    @Override
    public float getKnockbackResistance() {
        return 0.1f; // Small knockback resistance
    }
}