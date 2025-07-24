package com.ancientcurse;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Manages the creative menu tab for the Ancient Curse mod
 * 
 * IMPORTANT CREATIVE MENU GUIDELINES:
 * 1. All blocks and items must be added to the creative menu using the safeAdd() method
 * 2. Blocks from specialized registry classes should be referenced using their full path:
 *    - com.ancientcurse.block.registry.CursedPlantBlocks.BLOCK_NAME
 *    - com.ancientcurse.block.registry.EgyptianPlantBlocks.BLOCK_NAME
 *    - com.ancientcurse.block.registry.NecrostoneBlocks.BLOCK_NAME
 *    - etc.
 * 3. Group related blocks together with appropriate comments
 * 4. When adding a new block type, add all blocks of that type in a single section
 * 5. DO NOT reference blocks directly from ModBlocks if they have been moved to a registry class
 * 
 * Following these guidelines prevents missing blocks in the creative menu and makes
 * the codebase more maintainable.
 */
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
                safeAdd(entries, ModItems.SALT);
                
                // Tree blocks
                safeAdd(entries, ModBlocks.SYCAMORE_FIG_LOG);
                safeAdd(entries, ModBlocks.SYCAMORE_PLANKS);
                safeAdd(entries, ModBlocks.SYCAMORE_LEAVES);
                safeAdd(entries, ModBlocks.DATE_PALM_LOG);
                safeAdd(entries, ModBlocks.DATE_PALM_PLANKS);
                safeAdd(entries, ModBlocks.DATE_PALM_LEAVES);
                
                // Terrain blocks
                safeAdd(entries, ModBlocks.SMOOTH_SAND);
                // REMOVED: Anubus Glyph block was causing registration conflicts
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
                safeAdd(entries, ModBlocks.MUD_BRICK_STAIRS);
                safeAdd(entries, ModBlocks.MUD_BRICK_SLAB);
                safeAdd(entries, ModBlocks.SUNBAKED_CLAY);
                
                // Pottery and vessel blocks
                safeAdd(entries, com.ancientcurse.block.registry.PotteryBlocks.OFFERING_POT);
                safeAdd(entries, com.ancientcurse.block.registry.PotteryBlocks.VESSEL_OF_WHISPERING_WINDS);
                safeAdd(entries, com.ancientcurse.block.registry.PotteryBlocks.SERPENT_VESSEL_OF_WADJET);
                safeAdd(entries, com.ancientcurse.block.registry.PotteryBlocks.SCARAB_SEALED_URN);
                safeAdd(entries, com.ancientcurse.block.registry.PotteryBlocks.PHARAOHS_INCENSE_JAR);
                safeAdd(entries, com.ancientcurse.block.registry.PotteryBlocks.CANOPIC_URN_OF_BASTET);
                
                // Plants and vegetation
                safeAdd(entries, ModBlocks.LIGHT_NILE_MARSH);
                safeAdd(entries, ModBlocks.HEAVY_MARSH);
                safeAdd(entries, ModBlocks.RIVERBED_MOSS);
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
                
                // All cursed plant blocks
                safeAdd(entries, com.ancientcurse.block.registry.CursedPlantBlocks.CURSED_SPRIG);
                safeAdd(entries, com.ancientcurse.block.registry.CursedPlantBlocks.CURSED_SPROUT);
                safeAdd(entries, com.ancientcurse.block.registry.CursedPlantBlocks.DUAT_FERN);
                safeAdd(entries, com.ancientcurse.block.registry.CursedPlantBlocks.VINE_OF_APEP);
                safeAdd(entries, com.ancientcurse.block.registry.CursedPlantBlocks.BLOODSHADE_THICKET);
                
                // Additional cursed plant blocks (moved from EgyptianPlantBlocks)
                safeAdd(entries, com.ancientcurse.block.registry.CursedPlantBlocks.DUAMUTEF_CAP);
                safeAdd(entries, com.ancientcurse.block.registry.CursedPlantBlocks.ISFET_FROND);
                safeAdd(entries, com.ancientcurse.block.registry.CursedPlantBlocks.ISFET_SHRUB);
                safeAdd(entries, com.ancientcurse.block.registry.CursedPlantBlocks.KHEMNU_POD);
                safeAdd(entries, com.ancientcurse.block.registry.CursedPlantBlocks.KHERU_MOSS);
                safeAdd(entries, com.ancientcurse.block.registry.CursedPlantBlocks.MENFET_SPRIG);
                safeAdd(entries, com.ancientcurse.block.registry.CursedPlantBlocks.REED_OF_SEKHEM);
                safeAdd(entries, com.ancientcurse.block.registry.CursedPlantBlocks.SUTEKH_COIL);
                
                // Bronze Materials
                safeAdd(entries, ModItems.BRONZE_BLEND);
                safeAdd(entries, ModItems.RAW_BRONZE_NUGGET);
                safeAdd(entries, ModItems.BRONZE_INGOT);
                safeAdd(entries, ModItems.BRONZE_NUGGET);
                
                // Bronze Blocks
                safeAdd(entries, ModBlocks.BRONZE_BLOCK);
                safeAdd(entries, ModItems.BRONZE_PLATE);
                
                // Bronze tools
                safeAdd(entries, ModItems.BRONZE_SWORD);
                safeAdd(entries, ModItems.BRONZE_PICKAXE);
                safeAdd(entries, ModItems.BRONZE_AXE);
                safeAdd(entries, ModItems.BRONZE_CEREMONIAL_AXE);
                safeAdd(entries, ModItems.BRONZE_SHOVEL);
                safeAdd(entries, ModItems.BRONZE_HOE);
                safeAdd(entries, ModItems.BRONZE_KHOPESH);
                safeAdd(entries, ModItems.HORUS_MACE);
                
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
                
                // Construction blocks (decorative building materials)
                safeAdd(entries, com.ancientcurse.block.registry.ConstructionBlocks.SANDSTONE_BRICK_TILES);
                safeAdd(entries, com.ancientcurse.block.registry.ConstructionBlocks.POLISHED_BRONZE);
                
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
                safeAdd(entries, ModBlocks.BLACKSTONE_STAIRS);
                safeAdd(entries, ModBlocks.BLACKSTONE_SLAB);
                safeAdd(entries, ModBlocks.HARDENED_BLACK_STONE);
                safeAdd(entries, ModBlocks.WIND_SWEPT_BLACKSTONE);
                safeAdd(entries, ModBlocks.WIND_SWEPT_BLACKSTONE_STAIRS);
                safeAdd(entries, ModBlocks.WIND_SWEPT_BLACKSTONE_SLAB);
                safeAdd(entries, ModBlocks.CURSED_EARTH);
                safeAdd(entries, ModBlocks.CLEANSING_STATION);

                // Necrostone blocks
                safeAdd(entries, com.ancientcurse.block.registry.NecrostoneBlocks.NECROSTONE_BRICK);
                safeAdd(entries, com.ancientcurse.block.registry.NecrostoneBlocks.NECROSTONE_CRACKED);
                safeAdd(entries, com.ancientcurse.block.registry.NecrostoneBlocks.NECROSTONE_ROUGH);
                safeAdd(entries, com.ancientcurse.block.registry.NecrostoneBlocks.NECROSTONE_WALL);
                safeAdd(entries, com.ancientcurse.block.registry.NecrostoneBlocks.NECROSTONE_PILLAR);
                safeAdd(entries, com.ancientcurse.block.registry.NecrostoneBlocks.NECROSTONE_CHISELED);
                safeAdd(entries, com.ancientcurse.block.registry.NecrostoneBlocks.NECROSTONE_STAIRS);
                safeAdd(entries, com.ancientcurse.block.registry.NecrostoneBlocks.NECROSTONE_SLAB);
                safeAdd(entries, com.ancientcurse.block.registry.NecrostoneBlocks.NECROSTONE_CARVED);
                safeAdd(entries, com.ancientcurse.block.registry.NecrostoneBlocks.NECROSTONE_SMOOTH);
                safeAdd(entries, com.ancientcurse.block.registry.NecrostoneBlocks.NECROSTONE_CHAIN);
                safeAdd(entries, com.ancientcurse.block.registry.NecrostoneBlocks.NECROSTONE_EYE);
                safeAdd(entries, com.ancientcurse.block.registry.NecrostoneBlocks.NECROSTONE_ANKH);
                safeAdd(entries, com.ancientcurse.block.registry.NecrostoneBlocks.NECROSTONE_FORK);
                safeAdd(entries, com.ancientcurse.block.registry.NecrostoneBlocks.DECORATIVE_NECROSTONE);
                
                // Pillar blocks
                safeAdd(entries, com.ancientcurse.block.registry.PillarBlocks.DESHRET_PILLAR);
                safeAdd(entries, com.ancientcurse.block.registry.PillarBlocks.EGYPTIAN_GOLD_PILLAR);
                safeAdd(entries, com.ancientcurse.block.registry.PillarBlocks.MUDSTONE_PILLAR);
                
                // Headstone blocks
                safeAdd(entries, com.ancientcurse.block.registry.HeadstoneBlocks.BLUE_HEAD_STONE);
                safeAdd(entries, com.ancientcurse.block.registry.HeadstoneBlocks.GREEN_HEAD_STONE);
                safeAdd(entries, com.ancientcurse.block.registry.HeadstoneBlocks.GOLD_HEADSTONE);
                
                // Furniture blocks
                safeAdd(entries, com.ancientcurse.block.registry.FurnitureBlocks.SCROLL_SHELF);
                safeAdd(entries, com.ancientcurse.block.registry.FurnitureBlocks.WOODEN_CRATE);
                safeAdd(entries, com.ancientcurse.block.registry.FurnitureBlocks.ANCIENT_WOOD);
                safeAdd(entries, com.ancientcurse.block.registry.FurnitureBlocks.ANCIENT_UDJAT);
                
                // Deshret blocks
                safeAdd(entries, com.ancientcurse.block.registry.DeshretBlocks.DESHRET_SCALE);

                // Artifact items
                safeAdd(entries, ModItems.CANOPIC_HEART_JAR);
                safeAdd(entries, ModItems.EYE_OF_APOPHIS);
                safeAdd(entries, ModItems.SCARAB_TALISMAN);
                safeAdd(entries, ModItems.SCARAB_SHELL);
                safeAdd(entries, ModItems.SCARAB_SHELL_FRAGMENT);
                // Temporarily disabled items
                // safeAdd(entries, ModItems.THE_BROKEN_CROOK);
                // safeAdd(entries, ModItems.VESSEL_OF_THE_DUAT);
                // Vessel items are already added in the Blocks section
                
                // Magical items
                safeAdd(entries, ModItems.PHIAL_OF_LOTUS_ESSENCE);
                safeAdd(entries, ModItems.ELIXIR_OF_RAS_SPARK);
                safeAdd(entries, ModItems.ANCIENT_PICK);
                safeAdd(entries, ModItems.SERPENT_STAFF);
                safeAdd(entries, ModItems.WITHERED_STAFF);
                safeAdd(entries, ModItems.SCARAB_INCENSE_ITEM);
                safeAdd(entries, ModItems.ETERNAL_SIGIL);
                
                // Ankh restoration items
                safeAdd(entries, ModItems.PHARAOHS_BLOOD);
                safeAdd(entries, ModItems.MUMMY_HEART);
                safeAdd(entries, ModBlocks.BLOOD_LOTUS);
                
                // Curse removal items
                safeAdd(entries, ModItems.SCROLL_OF_UNBINDING);
                safeAdd(entries, ModItems.HIEROGLYPH_FRAGMENT);
                
                // Admin tools
                safeAdd(entries, ModItems.CURSE_ZONE_ADMIN_WAND);
                
                // Spawn eggs
                safeAdd(entries, ModItems.WITHERED_PHARAOH_SPAWN_EGG);
                safeAdd(entries, ModItems.DJESERHATH_SPAWN_EGG);
                safeAdd(entries, ModItems.ANUBIS_SPAWN_EGG);
                safeAdd(entries, ModItems.LOCUS_SPAWN_EGG);
                safeAdd(entries, ModItems.SCARAB_BEETLE_SPAWN_EGG);
                safeAdd(entries, ModItems.THOTH_SPAWN_EGG);
                safeAdd(entries, ModItems.KHAMSIN_SPREAD_SMALL_SPAWN_EGG);
                // Lotus spawn egg removed to fix conflicts
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