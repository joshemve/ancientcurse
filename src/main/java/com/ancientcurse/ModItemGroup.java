package com.ancientcurse;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroup {
    public static final ItemGroup ANCIENT_CURSE = Registry.register(
        Registries.ITEM_GROUP,
        new Identifier(AncientCurse.MOD_ID, "main"),
        FabricItemGroup.builder()
            .displayName(Text.translatable("itemgroup.ancientcurse"))
            .icon(() -> new ItemStack(ModItems.STAFF_OF_RA, 1))
            .entries((displayContext, entries) -> {
                // Tools and Items
                safeAdd(entries, ModItems.STAFF_OF_RA);
                
                // Food items
                safeAdd(entries, ModItems.SYCAMORE_FIG);
                safeAdd(entries, ModItems.GOLDEN_SYCAMORE_FIG);
                safeAdd(entries, ModItems.SEKHEM_DATE);
                safeAdd(entries, ModItems.SPINACH);
                
                // Seeds are automatically added by AliasedBlockItem
                // Do not add them again here to avoid duplicates
                
                // Harvested Crops / Fibers
                safeAdd(entries, ModItems.BARLEY);
                safeAdd(entries, ModItems.FLAX_FIBER);
                safeAdd(entries, ModItems.PAPYRUS_PAPER);
                safeAdd(entries, ModItems.RAW_RIVERBED_CLAY);
                safeAdd(entries, ModItems.LOTUS_FLOWER);
                safeAdd(entries, ModItems.ROPE);
                
                // Tree blocks
                safeAdd(entries, ModBlocks.SYCAMORE_FIG_LOG);
                safeAdd(entries, ModBlocks.SYCAMORE_LEAVES);
                safeAdd(entries, ModBlocks.DATE_PALM_LOG);
                safeAdd(entries, ModBlocks.DATE_PALM_LEAVES);
                
                // Terrain blocks
                safeAdd(entries, ModBlocks.SMOOTH_SAND);
                safeAdd(entries, ModBlocks.NILE_RIVER_SAND);
                safeAdd(entries, ModBlocks.FERTILE_NILE_SILT);
                safeAdd(entries, ModBlocks.DRY_NILE_SILT);
                safeAdd(entries, ModBlocks.TILLED_NILE_SILT);
                safeAdd(entries, ModBlocks.ARID_NILE_TURF);
                
                // Nile environment blocks
                safeAdd(entries, ModBlocks.RIVERBED);
                safeAdd(entries, ModBlocks.NILE_MUD);
                safeAdd(entries, ModBlocks.GOLD_FLAKED_RIVER_BED);
                safeAdd(entries, ModBlocks.MUD_FLAT);
                safeAdd(entries, ModBlocks.SALT_BED);
                
                // Building materials
                safeAdd(entries, ModBlocks.DRIED_REED_THATCH);
                safeAdd(entries, ModBlocks.RIVERBED_CLAY);
                safeAdd(entries, ModBlocks.OBELISK_STONE);
                safeAdd(entries, ModBlocks.MUD_BRICK);
                safeAdd(entries, ModBlocks.SUNBAKED_CLAY);
                safeAdd(entries, ModBlocks.OFFERING_POT);
                safeAdd(entries, ModBlocks.VESSEL_OF_WHISPERING_WINDS);
                
                // Plants and vegetation
                safeAdd(entries, ModBlocks.LIGHT_NILE_MARSH);
                safeAdd(entries, ModBlocks.HEAVY_MARSH);
                safeAdd(entries, ModBlocks.RIVERBED_MOSS);
                safeAdd(entries, ModBlocks.ALGAE);
                safeAdd(entries, ModBlocks.REED_MAT);
                safeAdd(entries, ModBlocks.SPOTTED_MARSH);
                safeAdd(entries, ModBlocks.DEAD_PAPYRUS_REED);
                safeAdd(entries, ModBlocks.PAPYRUS_REED);
                safeAdd(entries, ModBlocks.DWARF_PAPYRUS);
                safeAdd(entries, ModBlocks.EGYPTIAN_SPINACH);
                safeAdd(entries, ModBlocks.EUPHORBIA_HELIOSCOPIA);
                safeAdd(entries, ModBlocks.LIGHT_DEAD_FERN);
                safeAdd(entries, ModBlocks.MINI_CACTUS);
                safeAdd(entries, ModBlocks.PISTIA_STRATIOTES);
                safeAdd(entries, ModBlocks.LOTUS_FLOWER_PAD);
                
                // Bronze Materials
                safeAdd(entries, ModItems.BRONZE_BLEND);
                safeAdd(entries, ModItems.RAW_BRONZE_NUGGET);
                safeAdd(entries, ModItems.BRONZE_INGOT);
                safeAdd(entries, ModItems.BRONZE_NUGGET);
                
                // Bronze Blocks
                safeAdd(entries, ModBlocks.BRONZE_BLOCK);
                safeAdd(entries, ModBlocks.BRONZE_PLATE);
                safeAdd(entries, ModBlocks.CHISELED_BRONZE);
                safeAdd(entries, ModBlocks.BRONZE_GRATE);
                
                // Bronze Tools
                safeAdd(entries, ModItems.BRONZE_SWORD);
                safeAdd(entries, ModItems.BRONZE_PICKAXE);
                safeAdd(entries, ModItems.BRONZE_AXE);
                safeAdd(entries, ModItems.BRONZE_SHOVEL);
                safeAdd(entries, ModItems.BRONZE_HOE);
                
                // Bronze Armor
                safeAdd(entries, ModItems.BRONZE_HELMET);
                safeAdd(entries, ModItems.BRONZE_CHESTPLATE);
                safeAdd(entries, ModItems.BRONZE_LEGGINGS);
                safeAdd(entries, ModItems.BRONZE_BOOTS);
                
                // Crops (Blocks themselves, for placing if needed)
                safeAdd(entries, ModBlocks.FLAX);
                safeAdd(entries, ModBlocks.BARLEY);
                
                // Nile River Grass blocks
                safeAdd(entries, ModBlocks.NILE_RIVER_GRASS);
                
                // Deshret blocks (Egyptian red desert materials)
                safeAdd(entries, ModBlocks.DESHRET_SAND);
                safeAdd(entries, ModBlocks.DESHRET_WAVY_SAND);
                safeAdd(entries, ModBlocks.DESHRET_SANDSTONE);
                safeAdd(entries, ModBlocks.DESHRET_BRICK);
                safeAdd(entries, ModBlocks.DESHRET_COBBLESTONE);
                safeAdd(entries, ModBlocks.HARDENED_DESHRET_STONE);
                safeAdd(entries, ModBlocks.POLISHED_DESHRET_STONE);
                safeAdd(entries, ModBlocks.SPOTTED_DESHRET);
                
                // Rock blocks
                safeAdd(entries, ModBlocks.SMALL_ROCK);
                safeAdd(entries, ModBlocks.MEDIUM_ROCK);
                safeAdd(entries, ModBlocks.LARGE_ROCK);
                
                // Black blocks - explicitly create new ItemStacks with count=1
                safeAdd(entries, ModBlocks.BLACK_COBBLESTONE);
                safeAdd(entries, ModBlocks.BLACK_DUST);
                safeAdd(entries, ModBlocks.BLACK_SAND);
                safeAdd(entries, ModBlocks.BLACK_STONE);
                safeAdd(entries, ModBlocks.BLACKSTONE_BRICK);
                safeAdd(entries, ModBlocks.HARDENED_BLACK_STONE);
                safeAdd(entries, ModBlocks.WIND_SWEPT_BLACKSTONE);


                safeAdd(entries, ModItems.CANOPIC_HEART_JAR);
                safeAdd(entries, ModItems.EYE_OF_APOPHIS);
                safeAdd(entries, ModItems.SCARAB_TALISMAN);
                safeAdd(entries, ModItems.THE_BROKEN_CROOK);
                safeAdd(entries, ModItems.VESSEL_OF_THE_DUAT);
            })
            .build()
    );

    /**
     * A utility method to safely add items to creative tab entries with stack size exactly 1
     */
    private static void safeAdd(ItemGroup.Entries entries, ItemConvertible item) {
        if (item != null) {
            try {
                // Create a new ItemStack with exactly 1 item
                ItemStack stack = new ItemStack(item);
                stack.setCount(1);
                entries.add(stack);
            } catch (Exception e) {
                AncientCurse.LOGGER.error("Failed to add item to creative tab: " + item, e);
            }
        }
    }

    public static void registerItemGroups() {
        AncientCurse.LOGGER.info("Registering item groups for " + AncientCurse.MOD_ID);
    }
}