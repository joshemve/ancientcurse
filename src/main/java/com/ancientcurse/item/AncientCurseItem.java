package com.ancientcurse.item;

import net.minecraft.item.Item;

/**
 * Base class for all items in the Ancient Curse mod.
 * Automatically adds the "Ancient Curse" tooltip to all items.
 */
public class AncientCurseItem extends Item {
    
    public AncientCurseItem(Settings settings) {
        super(settings);
    }

}
