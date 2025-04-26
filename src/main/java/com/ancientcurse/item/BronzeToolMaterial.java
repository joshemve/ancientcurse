package com.ancientcurse.item;

import com.ancientcurse.ModItems;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;

/**
 * Custom tool material for bronze tools in the Ancient Curse mod.
 * Bronze is historically between stone and iron in terms of durability and effectiveness.
 */
public class BronzeToolMaterial implements ToolMaterial {
    // Singleton instance
    public static final BronzeToolMaterial INSTANCE = new BronzeToolMaterial();
    
    // Private constructor to enforce singleton pattern
    private BronzeToolMaterial() {}
    
    @Override
    public int getDurability() {
        // Bronze durability - between stone (131) and iron (250)
        return 180;
    }
    
    @Override
    public float getMiningSpeedMultiplier() {
        // Bronze mining speed - between stone (4.0) and iron (6.0)
        return 5.0f;
    }
    
    @Override
    public float getAttackDamage() {
        // Base attack damage - slightly better than stone (1.0) but less than iron (2.0)
        return 1.5f;
    }
    
    @Override
    public int getMiningLevel() {
        // Mining level 2 (same as iron) - can mine iron ore and similar
        return 2;
    }
    
    @Override
    public int getEnchantability() {
        // Enchantability - between stone (5) and iron (14)
        return 10;
    }
    
    @Override
    public Ingredient getRepairIngredient() {
        // Bronze tools can be repaired with bronze ingots
        return Ingredient.ofItems(ModItems.BRONZE_INGOT);
    }
}
