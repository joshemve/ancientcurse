package com.ancientcurse.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Hieroglyph Fragment - An ancient piece of sacred text
 * Used as a crafting ingredient for the Tablet of Thoth
 */
public class HieroglyphFragmentItem extends Item {
    
    public HieroglyphFragmentItem(Settings settings) {
        super(settings.maxCount(64));
    }
    
    @Override
    public boolean hasGlint(ItemStack stack) {
        return true; // Has a subtle magical glint
    }
}