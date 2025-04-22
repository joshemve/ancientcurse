package com.ancientcurse;

import com.ancientcurse.item.CustomAnimatedItem;
import com.ancientcurse.item.SekhemDateItem;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.AliasedBlockItem;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Centralizes item registration for the mod
 */
public class ModItems {
    // Define items
    public static final CustomAnimatedItem STAFF_OF_RA = new CustomAnimatedItem(new FabricItemSettings(), "staff_of_ra");


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
    public  static  final Item EYE_OF_APOPHIS = new Item(new FabricItemSettings());

    // CANOPIC HEART JAR
    public  static  final Item CANOPIC_HEART_JAR  = new Item(new FabricItemSettings());

    // THE BROKEN CROOK
    public  static  final Item THE_BROKEN_CROOK  = new Item(new FabricItemSettings());

    //SCARAB TALISMAN
    public  static  final Item SCARAB_TALISMAN  = new Item(new FabricItemSettings());

    // VESSELE OF DUAT
    public  static  final Item VESSEL_OF_THE_DUAT  = new Item(new FabricItemSettings());

    // Bronze materials
    public static final Item BRONZE_BLEND = new Item(new FabricItemSettings());
    public static final Item RAW_BRONZE_NUGGET = new Item(new FabricItemSettings());
    public static final Item BRONZE_INGOT = new Item(new FabricItemSettings());
    public static final Item BRONZE_NUGGET = new Item(new FabricItemSettings());
    
    // Bronze tools
    public static final Item BRONZE_SWORD = new Item(new FabricItemSettings());
    public static final Item BRONZE_PICKAXE = new Item(new FabricItemSettings());
    public static final Item BRONZE_AXE = new Item(new FabricItemSettings());
    public static final Item BRONZE_SHOVEL = new Item(new FabricItemSettings());
    public static final Item BRONZE_HOE = new Item(new FabricItemSettings());
    
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
                new Identifier(AncientCurse.MOD_ID, "scarab_talis"),
                SCARAB_TALISMAN
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
            new Identifier(AncientCurse.MOD_ID, "bronze_shovel"),
            BRONZE_SHOVEL
        );
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, "bronze_hoe"),
            BRONZE_HOE
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
    }
}