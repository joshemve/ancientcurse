package com.example.egyptianweapons.registry;

import com.example.egyptianweapons.EgyptianWeapons;
import com.example.egyptianweapons.items.*;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Registry class for custom items in the Ancient Curse mod.
 */
public class ItemRegistry {
    // Register the Cursed Mace (formerly Smiting Mace of Horus)
    public static final SmitingMaceOfHorus CURSED_MACE = registerItem("cursed_mace", 
            new SmitingMaceOfHorus());
    
    // Register the Serpent Staff (keeping the same name but with new ID)
    public static final SerpentStaff SERPENT_STAFF = registerItem("serpent_staff",
            new SerpentStaff());
    
    // Register the War Axe (keeping the same name but with new ID)
    public static final WarAxe WAR_AXE = registerItem("war_axe",
            new WarAxe());
    
    // Register the Soul Orb (formerly Growing Orb)
    public static final GrowingOrb SOUL_ORB = registerItem("soul_orb",
            new GrowingOrb());
    
    // Register the Viper Head (formerly Snake Head)
    public static final SnakeHead VIPER_HEAD = registerItem("viper_head",
            new SnakeHead());
    
    // Register the Staff of Souls (formerly Staff of Ra)
    public static final StaffOfRa STAFF_OF_SOULS = registerItem("staff_of_souls",
            new StaffOfRa());
    
    /**
     * Register all custom items
     */
    public static void registerItems() {
        // Add items to the combat item group
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(content -> {
            content.add(CURSED_MACE);
            content.add(SERPENT_STAFF);
            content.add(WAR_AXE);
            content.add(SOUL_ORB);
            content.add(VIPER_HEAD);
            content.add(STAFF_OF_SOULS);
        });
        
        EgyptianWeapons.LOGGER.info("Registered Ancient Curse items");
    }
    
    /**
     * Helper method to register an item
     * 
     * @param name The registry name of the item
     * @param item The item instance
     * @return The registered item
     */
    private static <T extends Item> T registerItem(String name, T item) {
        return Registry.register(Registries.ITEM, new Identifier(EgyptianWeapons.MOD_ID, name), item);
    }
}
