package com.ancientcurse;

import com.ancientcurse.item.AncientPickItem;
import com.ancientcurse.item.CustomAnimatedItem;
import com.ancientcurse.item.ElixirOfRasSparkItem;
import com.ancientcurse.item.EternalSigilItem;
import com.ancientcurse.item.HorusMaceItem;
import com.ancientcurse.item.PhialOfLotusEssenceItem;
import com.ancientcurse.item.ScarabIncenseItem;
import com.ancientcurse.item.ScarabTalismanItem;
import com.ancientcurse.item.SekhemDateItem;
import com.ancientcurse.item.SerpentStaffItem;
import com.ancientcurse.item.StaffOfRaItem;
import com.ancientcurse.item.WitheredStaffItem;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.AliasedBlockItem;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.AxeItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import com.ancientcurse.item.BronzeToolMaterial;

/**
 * Centralizes item registration for the mod
 */
public class ModItems {
    // Define items
    public static final StaffOfRaItem STAFF_OF_RA = new StaffOfRaItem(new FabricItemSettings(), "staff_of_ra");


    // Define the Sycamore Fig food item
    public static final Item SYCAMORE_FIG = new Item(
        new FabricItemSettings()
            .food(new FoodComponent.Builder()
                .hunger(4)
                .saturationModifier(0.6f)
                .build()
            )
    );
    
    // Define the Golden Sycamore Fig food item with regeneration effect
    public static final Item GOLDEN_SYCAMORE_FIG = new Item(
        new FabricItemSettings()
            .food(new FoodComponent.Builder()
                .hunger(6)
                .saturationModifier(0.8f)
                .statusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 60, 0), 1.0f) // 3 seconds of Regeneration I
                .build()
            )
    );
    
    // Define the Sekhem Date food item with fire resistance effect
    public static final Item SEKHEM_DATE = new SekhemDateItem(new FabricItemSettings());
    
    // Define crop seeds
    public static final Item FLAX_SEEDS = new AliasedBlockItem(ModBlocks.FLAX, new FabricItemSettings());
    public static final Item BARLEY_SEEDS = new AliasedBlockItem(ModBlocks.BARLEY, new FabricItemSettings());

    // Define harvested crop items
    public static final Item BARLEY = new Item(new FabricItemSettings());
    public static final Item FLAX_FIBER = new Item(new FabricItemSettings());
    
    // Define Lotus Flower item
    public static final Item LOTUS_FLOWER = new Item(
        new FabricItemSettings()
            .food(new FoodComponent.Builder()
                .hunger(2)
                .saturationModifier(0.3f)
                .statusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 200, 0), 1.0f) // 10 seconds of Water Breathing
                .build()
            )
    );
    
    // Define Raw Riverbed Clay item - drops from Riverbed Clay block
    public static final Item RAW_RIVERBED_CLAY = new Item(new FabricItemSettings());
    
    // Define Papyrus Paper item - crafted from Papyrus Reed
    public static final Item PAPYRUS_PAPER = new Item(new FabricItemSettings());
    
    // Define Spinach food item - drops from Egyptian Spinach plant
    public static final Item SPINACH = new Item(
        new FabricItemSettings()
            .food(new FoodComponent.Builder()
                .hunger(3)
                .saturationModifier(0.4f)
                .statusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 100, 0), 0.7f) // 5 seconds of Night Vision with 70% chance
                .build()
            )
    );
    
    // Define Rope item - crafting material
    public static final Item ROPE = new Item(new FabricItemSettings());

    // EYE_OF_APOPHIS
    public static final Item EYE_OF_APOPHIS = new Item(new FabricItemSettings());

    // CANOPIC HEART JAR
    public static final Item CANOPIC_HEART_JAR  = new Item(new FabricItemSettings());

    // THE BROKEN CROOK
    public static final Item THE_BROKEN_CROOK  = new Item(new FabricItemSettings());

    // Scarab Talisman - A magical amulet with protective properties
    public static final ScarabTalismanItem SCARAB_TALISMAN = new ScarabTalismanItem(new FabricItemSettings());

    // Vessel items
    public static final Item VESSEL_OF_THE_DUAT = new Item(new FabricItemSettings());
    
    // New magical items
    public static final PhialOfLotusEssenceItem PHIAL_OF_LOTUS_ESSENCE = new PhialOfLotusEssenceItem(new FabricItemSettings());
    public static final ElixirOfRasSparkItem ELIXIR_OF_RAS_SPARK = new ElixirOfRasSparkItem(new FabricItemSettings());
    public static final AncientPickItem ANCIENT_PICK = new AncientPickItem(new FabricItemSettings());
    public static final SerpentStaffItem SERPENT_STAFF = new SerpentStaffItem(new FabricItemSettings());
    public static final WitheredStaffItem WITHERED_STAFF = new WitheredStaffItem(new FabricItemSettings());
    public static final ScarabIncenseItem SCARAB_INCENSE_ITEM = new ScarabIncenseItem(new FabricItemSettings());
    public static final EternalSigilItem ETERNAL_SIGIL = new EternalSigilItem(
        new FabricItemSettings().maxCount(1)
    );
    
    // Define the Withered Pharaoh spawn egg
    public static final SpawnEggItem WITHERED_PHARAOH_SPAWN_EGG = new SpawnEggItem(
        ModEntities.WITHERED_PHARAOH, 
        0x8B4513, // Primary color (dark brown)
        0xFFD700, // Secondary color (gold)
        new FabricItemSettings()
    );
    
    // Define the Djeserhath spawn egg
    public static final SpawnEggItem DJESERHATH_SPAWN_EGG = new SpawnEggItem(
        ModEntities.DJESERHATH, 
        0x4B7F52, // Primary color (dark green)
        0xAD4E4E, // Secondary color (reddish)
        new FabricItemSettings()
    );
    
    // Anubis Boss spawn egg
    public static final SpawnEggItem ANUBIS_SPAWN_EGG = new SpawnEggItem(
        ModEntities.ANUBIS, 
        0x2B1B17, // Primary color (dark brown/black)
        0xFFD700, // Secondary color (gold)
        new FabricItemSettings()
    );
    
    // Locus spawn egg (correctly named to match asset files: locus.geo.json, locus.png, etc.)
    public static final SpawnEggItem LOCUS_SPAWN_EGG = new SpawnEggItem(
        ModEntities.LOCUS, 
        0x4A5D23, // Primary color (dark olive green)
        0x7B8A3A, // Secondary color (olive green)
        new FabricItemSettings()
    );
    
    // Scarab Beetle spawn egg
    public static final SpawnEggItem SCARAB_BEETLE_SPAWN_EGG = new SpawnEggItem(
        ModEntities.SCARAB_BEETLE, 
        0x2B1B0A, // Primary color (dark brown/black - beetle shell)
        0xB8860B, // Secondary color (dark golden rod - metallic sheen)
        new FabricItemSettings()
    );

    // Thoth spawn egg (Egyptian God boss)
    public static final SpawnEggItem THOTH_SPAWN_EGG = new SpawnEggItem(
        ModEntities.THOTH,
        0x9932CC, // Primary color (dark orchid - magical purple)
        0xFFD700, // Secondary color (gold - divine)
        new FabricItemSettings()
    );
    
    public static final Item SCARAB_SHELL = new Item(new FabricItemSettings());
    public static final Item SCARAB_SHELL_FRAGMENT = new Item(new FabricItemSettings());

    // Bronze materials
    public static final Item BRONZE_BLEND = new Item(new FabricItemSettings());
    public static final Item RAW_BRONZE_NUGGET = new Item(new FabricItemSettings());
    public static final Item BRONZE_INGOT = new Item(new FabricItemSettings());
    public static final Item BRONZE_NUGGET = new Item(new FabricItemSettings());
    public static final Item BRONZE_PLATE = new Item(new FabricItemSettings());
    
    // Bronze tools
    public static final Item BRONZE_SWORD = new SwordItem(BronzeToolMaterial.INSTANCE, 3, -2.4F, new FabricItemSettings());
    public static final Item BRONZE_PICKAXE = new PickaxeItem(BronzeToolMaterial.INSTANCE, 1, -2.8F, new FabricItemSettings());
    public static final Item BRONZE_AXE = new AxeItem(BronzeToolMaterial.INSTANCE, 6.0F, -3.1F, new FabricItemSettings());
    public static final Item BRONZE_CEREMONIAL_AXE = new AxeItem(BronzeToolMaterial.INSTANCE, 5.5F, -3.0F, new FabricItemSettings());
    public static final Item BRONZE_SHOVEL = new ShovelItem(BronzeToolMaterial.INSTANCE, 1.5F, -3.0F, new FabricItemSettings());
    public static final Item BRONZE_HOE = new HoeItem(BronzeToolMaterial.INSTANCE, -1, -2.0F, new FabricItemSettings());
    public static final Item BRONZE_KHOPESH = new SwordItem(BronzeToolMaterial.INSTANCE, 4, -2.2F, new FabricItemSettings());
    
    // Horus Mace - Divine weapon of the sky god
    public static final HorusMaceItem HORUS_MACE = new HorusMaceItem(
        BronzeToolMaterial.INSTANCE, 
        7, // High base damage
        -2.8F, // Moderate attack speed
        new FabricItemSettings().maxCount(1)
    );
    
    // Bronze armor
    public static final Item BRONZE_HELMET = new Item(new FabricItemSettings());
    public static final Item BRONZE_CHESTPLATE = new Item(new FabricItemSettings());
    public static final Item BRONZE_LEGGINGS = new Item(new FabricItemSettings());
    public static final Item BRONZE_BOOTS = new Item(new FabricItemSettings());

    /**
     * Registers all mod items
     */
    public static void registerItems() {
        AncientCurse.LOGGER.info("Registering items for " + AncientCurse.MOD_ID);
        
        // Register the Staff of Ra
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "staff_of_ra"),
            STAFF_OF_RA
        );
        
        // Register the Sycamore Fig
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "sycamore_fig"),
            SYCAMORE_FIG
        );
        
        // Register the Golden Sycamore Fig
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "golden_sycamore_fig"),
            GOLDEN_SYCAMORE_FIG
        );

        // Register the
        Registry.register(
                Registries.ITEM,
                new Identifier(AncientCurse.MOD_ID, "eye_of_apophis"),
                EYE_OF_APOPHIS
        );

        // Register the
        Registry.register(
                Registries.ITEM,
                new Identifier(AncientCurse.MOD_ID, "canopic_heart_jar"),
                CANOPIC_HEART_JAR
        );

         // Register the
        Registry.register(
                Registries.ITEM,
                new Identifier(AncientCurse.MOD_ID, "the_broken_crook"),
                THE_BROKEN_CROOK
        );

        // Register the
        Registry.register(
                Registries.ITEM,
                new Identifier(AncientCurse.MOD_ID, "vessel_of_the_duat"),
                VESSEL_OF_THE_DUAT
        );

        // Register the Sekhem Date
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "sekhem_date"),
            SEKHEM_DATE
        );
        
        // Register crop seeds
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "flax_seeds"),
            FLAX_SEEDS
        );
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "barley_seeds"),
            BARLEY_SEEDS
        );
        
        // Register harvested crops
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "barley"),
            BARLEY
        );
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "flax_fiber"),
            FLAX_FIBER
        );
        
        // Register Lotus Flower
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "lotus_flower"),
            LOTUS_FLOWER
        );
        
        // Register Raw Riverbed Clay
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "raw_riverbed_clay"),
            RAW_RIVERBED_CLAY
        );
        
        // Register Papyrus Paper
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "papyrus_paper"),
            PAPYRUS_PAPER
        );
        
        // Register Spinach
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "spinach"),
            SPINACH
        );
        
        // Register Rope
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "rope"),
            ROPE
        );
        
        // Register Bronze materials
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "bronze_blend"),
            BRONZE_BLEND
        );
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "raw_bronze_nugget"),
            RAW_BRONZE_NUGGET
        );
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "bronze_ingot"),
            BRONZE_INGOT
        );
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "bronze_nugget"),
            BRONZE_NUGGET
        );
        
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "bronze_plate"),
            BRONZE_PLATE
        );
        
        // Register Bronze tools
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "bronze_sword"),
            BRONZE_SWORD
        );
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "bronze_pickaxe"),
            BRONZE_PICKAXE
        );
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "bronze_axe"),
            BRONZE_AXE
        );
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "bronze_ceremonial_axe"),
            BRONZE_CEREMONIAL_AXE
        );
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "bronze_shovel"),
            BRONZE_SHOVEL
        );
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "bronze_hoe"),
            BRONZE_HOE
        );
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "horus_mace"),
            HORUS_MACE
        );
        
        // Register Bronze armor
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "bronze_helmet"),
            BRONZE_HELMET
        );
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "bronze_chestplate"),
            BRONZE_CHESTPLATE
        );
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "bronze_leggings"),
            BRONZE_LEGGINGS
        );
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "bronze_boots"),
            BRONZE_BOOTS
        );
        
        // Register Scarab Talisman
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "scarab_talisman"),
            SCARAB_TALISMAN
        );
        
        // Register new magical items
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "phial_of_lotus_essence"),
            PHIAL_OF_LOTUS_ESSENCE
        );
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "elixir_of_ras_spark"),
            ELIXIR_OF_RAS_SPARK
        );
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "ancient_pick"),
            ANCIENT_PICK
        );
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "serpent_staff"),
            SERPENT_STAFF
        );
        
        // Register Withered Staff
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "withered_staff"),
            WITHERED_STAFF
        );
        
        // Register Scarab Incense Item
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "scarab_incense_item"),
            SCARAB_INCENSE_ITEM
        );
        
        // Register Eternal Sigil
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "eternal_sigil"),
            ETERNAL_SIGIL
        );
        
        // Register Scarab Shell and Fragment
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "scarab_shell"),
            SCARAB_SHELL
        );
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "scarab_shell_fragment"),
            SCARAB_SHELL_FRAGMENT
        );
        
        // Register Bronze Khopesh
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "bronze_khopesh"),
            BRONZE_KHOPESH
        );
        
        // Register the Withered Pharaoh spawn egg
        Registry.register(
            Registries.ITEM, 
            new Identifier(AncientCurse.MOD_ID, "withered_pharaoh_spawn_egg"),
            WITHERED_PHARAOH_SPAWN_EGG
        );
        
        // Register the Djeserhath spawn egg
        Registry.register(
            Registries.ITEM, 
            new Identifier(AncientCurse.MOD_ID, "djeserhath_spawn_egg"),
            DJESERHATH_SPAWN_EGG
        );
        
        // Register the Anubis spawn egg
        Registry.register(
            Registries.ITEM, 
            new Identifier(AncientCurse.MOD_ID, "anubis_spawn_egg"),
            ANUBIS_SPAWN_EGG
        );
        
        // Register the Locus spawn egg
        Registry.register(
            Registries.ITEM, 
            new Identifier(AncientCurse.MOD_ID, "locus_spawn_egg"),
            LOCUS_SPAWN_EGG
        );
        
        // Register the Scarab Beetle spawn egg
        Registry.register(
            Registries.ITEM, 
            new Identifier(AncientCurse.MOD_ID, "scarab_beetle_spawn_egg"),
            SCARAB_BEETLE_SPAWN_EGG
        );
        
        // Register the Thoth spawn egg
        Registry.register(
            Registries.ITEM, 
            new Identifier(AncientCurse.MOD_ID, "thoth_spawn_egg"),
            THOTH_SPAWN_EGG
        );
    }
}