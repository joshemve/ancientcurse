package com.ancientcurse;

import com.ancientcurse.block.*;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.block.SandBlock;
import net.minecraft.block.PlantBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/**
 * Centralizes block registration for the mod
 */
public class ModBlocks {
    // Define blocks
    public static final Block SYCAMORE_FIG_LOG = new SycamoreFigLogBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.BROWN)
            .strength(2.0f)
            .sounds(BlockSoundGroup.WOOD)
            .ticksRandomly() // Enable random ticks for growth
    );
    
    public static final Block SYCAMORE_LEAVES = new SycamoreLeafBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.DARK_GREEN)
            .strength(0.2f)
            .sounds(BlockSoundGroup.GRASS)
            .nonOpaque()
            .ticksRandomly() // Enable random ticks for leaf decay
    );
    
    public static final Block DATE_PALM_LOG = new DatePalmLogBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_GRAY)
            .strength(2.0f)
            .sounds(BlockSoundGroup.WOOD)
    );
    
    public static final Block DATE_PALM_LEAVES = new DatePalmLeafBlock(
        FabricBlockSettings.copyOf(Blocks.OAK_LEAVES)
            .strength(0.2f)
            .nonOpaque()
            .sounds(BlockSoundGroup.GRASS)
    );
    
    public static final Block DATE_BLOCK = new DateBlock(
        FabricBlockSettings.create()
            .strength(0.2f)
            .nonOpaque()
            .sounds(BlockSoundGroup.WOOD)
            .breakInstantly()
    );
    
    // Smooth Sand - With Identical properties to vanilla sand
    public static final Block SMOOTH_SAND = new SandBlock(
        14406560, // Same color value as vanilla sand
        FabricBlockSettings.copyOf(Blocks.SAND)
    );
    
    // Nile River Sand - special sand for Nile shorelines
    public static final Block NILE_RIVER_SAND = new SandBlock(
        14535679, // Light tan color
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_LIGHT_GRAY)
            .strength(0.5f)
            .sounds(BlockSoundGroup.SAND)
    );
    
    // Fertile Nile Silt - rich soil block for the Nile floodplains
    public static final Block FERTILE_NILE_SILT = new FertileNileSiltBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_BROWN)
            .strength(0.5f)
            .sounds(BlockSoundGroup.GRAVEL)
    );
    
    // Dry Nile Silt - dried version of the Nile silt
    public static final Block DRY_NILE_SILT = new DryNileSiltBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_ORANGE)
            .strength(0.5f)
            .sounds(BlockSoundGroup.GRAVEL)
    );
    
    // Tilled Nile Silt - farmland version of Nile silt
    public static final Block TILLED_NILE_SILT = new TilledNileSiltBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_BROWN)
            .strength(0.6f)
            .sounds(BlockSoundGroup.GRAVEL)
            .ticksRandomly() // For moisture updates
    );
    
    // Arid Nile Turf - sparse grass-like surface for dry Nile regions
    public static final Block ARID_NILE_TURF = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.PALE_YELLOW)
            .strength(0.5f)
            .sounds(BlockSoundGroup.GRASS)
    );
    
    // Dead Papyrus Reed - dried plant that grows near the Nile
    public static final Block DEAD_PAPYRUS_REED = new DeadPapyrusReedBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.PALE_YELLOW)
            .strength(0.4f)
            .sounds(BlockSoundGroup.GRASS)
            .nonOpaque()
            .noCollision()
            .notSolid()
    );

    // Riverbed - sandy underwater terrain of the Nile
    public static final Block RIVERBED = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.DIRT_BROWN)
            .strength(0.5f)
            .sounds(BlockSoundGroup.GRAVEL)
    );

    // Heavy Marsh - dense vegetation and mud along the Nile
    public static final Block HEAVY_MARSH = new HeavyMarshBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.DARK_GREEN)
            .strength(0.6f)
            .sounds(BlockSoundGroup.WET_GRASS)
            .velocityMultiplier(0.4f) // Significant slowdown effect from dense vegetation
            .jumpVelocityMultiplier(0.6f) // Moderate difficulty jumping due to entanglement
    );

    // Riverbed Algae - algae-covered riverbed --> Renamed to RIVERBED_MOSS
    public static final Block RIVERBED_MOSS = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.DARK_GREEN)
            .strength(0.5f)
            .sounds(BlockSoundGroup.GRAVEL)
            .slipperiness(0.8f) // Slightly slippery due to algae
    );

    // New Algae block that sits on water
    public static final Block ALGAE = new AlgaeBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.DARK_GREEN)
            .strength(0.0f) // Instantly breakable
            .sounds(BlockSoundGroup.WET_GRASS) // Sound like wet grass
            .nonOpaque()
            .noCollision()
            .breakInstantly()
    );

    // Nile Mud - thick mud deposits from the Nile
    public static final Block NILE_MUD = new NileMudBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.DIRT_BROWN)
            .strength(0.5f)
            .sounds(BlockSoundGroup.MUD)
            .velocityMultiplier(0.3f) // Extreme slowdown effect like quicksand
            .jumpVelocityMultiplier(0.4f) // Make it hard to jump out
    );

    // Gold-Flaked Riverbed - riverbed with gold deposits
    public static final Block GOLD_FLAKED_RIVER_BED = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.GOLD)
            .strength(0.6f)
            .sounds(BlockSoundGroup.GRAVEL)
            .requiresTool() // Require a tool to emphasize it's valuable
    );

    // Mud Flat - flat dried mud terrain
    public static final Block MUD_FLAT = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_BROWN)
            .strength(0.5f)
            .sounds(BlockSoundGroup.PACKED_MUD)
    );

    // Salt Bed - crystallized salt deposits in dried areas
    public static final Block SALT_BED = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.WHITE)
            .strength(0.5f)
            .sounds(BlockSoundGroup.CALCITE) // Crystalline sound
    );

    // Dried Reed Thatch - bundled dried reeds for construction
    public static final Block DRIED_REED_THATCH = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.PALE_YELLOW)
            .strength(0.5f) 
            .sounds(BlockSoundGroup.GRASS)
    );

    // Riverbed Clay - wet clay deposits from the riverbed
    public static final Block RIVERBED_CLAY = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_GRAY)
            .strength(0.6f)
            .sounds(BlockSoundGroup.GRAVEL)
    );

    // Obelisk Stone - polished stone used for monuments
    public static final Block OBELISK_STONE = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.STONE_GRAY)
            .strength(1.5f)
            .requiresTool()
            .sounds(BlockSoundGroup.STONE)
    );

    // Mud Brick - dried mud formed into bricks
    public static final Block MUD_BRICK = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_BROWN)
            .strength(1.0f)
            .sounds(BlockSoundGroup.MUD_BRICKS)
    );

    // Light Nile Marsh - sparse marsh vegetation
    public static final Block LIGHT_NILE_MARSH = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.PALE_GREEN)
            .strength(0.5f)
            .sounds(BlockSoundGroup.GRASS)
    );

    // Reed Mat - woven reed flooring
    public static final Block REED_MAT = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.PALE_YELLOW)
            .strength(0.4f)
            .sounds(BlockSoundGroup.GRASS)
    );

    // Sunbaked Clay - clay hardened by the sun
    public static final Block SUNBAKED_CLAY = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_ORANGE)
            .strength(0.8f)
            .sounds(BlockSoundGroup.STONE)
    );

    // Rock blocks that adapt to the texture of the block they're placed on
    public static final Block SMALL_ROCK = new SmallRockBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.STONE_GRAY)
            .strength(0.5f)
            .sounds(BlockSoundGroup.STONE)
            .nonOpaque()
            .noCollision()
    );
    
    public static final Block MEDIUM_ROCK = new MediumRockBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.STONE_GRAY)
            .strength(0.7f)
            .sounds(BlockSoundGroup.STONE)
            .nonOpaque()
            .noCollision()
    );
    
    public static final Block LARGE_ROCK = new LargeRockBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.STONE_GRAY)
            .strength(1.0f)
            .sounds(BlockSoundGroup.STONE)
            .nonOpaque()
            .noCollision()
    );

    // Spotted Marsh - marsh with distinctive spotted vegetation
    public static final Block SPOTTED_MARSH = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.DARK_GREEN)
            .strength(0.5f)
            .sounds(BlockSoundGroup.WET_GRASS)
    );

    // Papyrus Reed - two-block tall plant native to Nile river
    public static final Block PAPYRUS_REED = new PapyrusReedBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.PALE_YELLOW)
            .strength(0.4f)
            .sounds(BlockSoundGroup.GRASS)
            .nonOpaque()
            .noCollision()
            .ticksRandomly()
            .notSolid()
    );

    // Flax - crop used for textiles
    public static final Block FLAX = new FlaxCropBlock(
        FabricBlockSettings.create()
            .nonOpaque()
            .noCollision()
            .ticksRandomly()
            .breakInstantly()
            .sounds(BlockSoundGroup.CROP)
            .notSolid()
    );

    // Barley - food crop
    public static final Block BARLEY = new BarleyCropBlock(
        FabricBlockSettings.create()
            .nonOpaque()
            .noCollision()
            .ticksRandomly() 
            .breakInstantly()
            .sounds(BlockSoundGroup.CROP)
            .notSolid()
    );
    
    // Lotus Flower Pad - decorative water plant that opens in day and closes at night
    public static final Block LOTUS_FLOWER_PAD = new LotusFlowerPadBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.DARK_GREEN)
            .strength(0.0f, 0.0f)
            .sounds(BlockSoundGroup.LILY_PAD)
            .nonOpaque()
            .noCollision()
            .ticksRandomly() // For day/night cycle updates
            .breakInstantly()
            .notSolid()
    );
    
    // Nile River Grass - lush grass that grows along the Nile riverbanks
    public static final Block NILE_RIVER_GRASS = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.DARK_GREEN)
            .strength(0.6f)
            .sounds(BlockSoundGroup.GRASS)
            .nonOpaque()
            .notSolid()
    );
    
    // Nile River Tall Grass - taller variant of grass for the Nile riverbanks
    public static final Block NILE_RIVER_TALL_GRASS = new NileRiverTallGrassBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.DARK_GREEN)
            .strength(0.5f)
            .sounds(BlockSoundGroup.GRASS)
            .nonOpaque()
            .noCollision()
            .notSolid()
    );
    
    // Deshret Brick - A brick made from the red desert material
    public static final Block DESHRET_BRICK = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_RED)
            .strength(1.5f, 6.0f)
            .requiresTool()
            .sounds(BlockSoundGroup.STONE)
    );
    
    // Deshret Cobblestone - Rough-cut stones made from red desert material
    public static final Block DESHRET_COBBLESTONE = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_RED)
            .strength(2.0f, 6.0f)
            .requiresTool()
            .sounds(BlockSoundGroup.STONE)
    );
    
    // Deshret Sand - The basic red desert sand block
    public static final Block DESHRET_SAND = new SandBlock(
        14378728, // Reddish color value
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_RED)
            .strength(0.5f)
            .sounds(BlockSoundGroup.SAND)
    );
    
    // Deshret Sandstone - Compressed red desert sand formed into stone
    public static final Block DESHRET_SANDSTONE = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_RED)
            .strength(0.8f)
            .requiresTool()
            .sounds(BlockSoundGroup.STONE)
    );
    
    // Deshret Wavy Sand - Red desert sand with a wavy, wind-blown pattern
    public static final Block DESHRET_WAVY_SAND = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_RED)
            .strength(0.5f)
            .sounds(BlockSoundGroup.SAND)
    );
    
    // Hardened Deshret Stone - A more solid, darker version of the red desert stone
    public static final Block HARDENED_DESHRET_STONE = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_RED)
            .strength(1.5f, 6.0f)
            .requiresTool()
            .sounds(BlockSoundGroup.DEEPSLATE)
    );
    
    // Polished Deshret Stone - Smoothed and refined red desert stone
    public static final Block POLISHED_DESHRET_STONE = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_RED)
            .strength(1.5f, 6.0f)
            .requiresTool()
            .sounds(BlockSoundGroup.POLISHED_DEEPSLATE)
    );
    
    // Spotted Deshret - Red desert material with distinctive spots or inclusions
    public static final Block SPOTTED_DESHRET = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_RED)
            .strength(1.2f, 6.0f)
            .requiresTool()
            .sounds(BlockSoundGroup.STONE)
    );

    // Dwarf Papyrus - small papyrus plant for decoration
    public static final Block DWARF_PAPYRUS = new DwarfPapyrusBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.PALE_YELLOW)
            .strength(0.3f)
            .sounds(BlockSoundGroup.GRASS)
            .nonOpaque()
            .noCollision()
            .notSolid()
    );

    // Egyptian Spinach - edible leafy plant
    public static final Block EGYPTIAN_SPINACH = new EgyptianSpinachBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.DARK_GREEN)
            .strength(0.3f)
            .sounds(BlockSoundGroup.GRASS)
            .nonOpaque()
            .noCollision()
            .notSolid()
    );

    // Euphorbia Helioscopia - desert flowering plant
    public static final Block EUPHORBIA_HELIOSCOPIA = new EuphorbiaHelioscopiaBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.PALE_GREEN)
            .strength(0.3f)
            .sounds(BlockSoundGroup.GRASS)
            .nonOpaque()
            .noCollision()
            .notSolid()
    );

    // Light Dead Fern - dried desert fern
    public static final Block LIGHT_DEAD_FERN = new LightDeadFernBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_BROWN)
            .strength(0.3f)
            .sounds(BlockSoundGroup.GRASS)
            .nonOpaque()
            .noCollision()
            .notSolid()
    );

    // Mini Cactus - small decorative cactus
    public static final Block MINI_CACTUS = new MiniCactusBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.DARK_GREEN)
            .strength(0.3f)
            .sounds(BlockSoundGroup.GRASS)
            .nonOpaque()
            .notSolid()
    );
    
    // Canopic Urn of Bastet - a ceremonial container with glowing eyes
    public static final Block CANOPIC_URN_OF_BASTET = new CanopicUrnOfBastetBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_GRAY)
            .strength(1.5f)
            .sounds(BlockSoundGroup.STONE)
            .nonOpaque()
            .luminance(state -> 5) // Soft glow from the eyes
    );
    
    // Scarab Sealed Urn - a ceremonial container sealed with a sacred scarab
    public static final Block SCARAB_SEALED_URN = new ScarabSealedUrnBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_GRAY)
            .strength(1.5f)
            .sounds(BlockSoundGroup.STONE)
            .nonOpaque()
    );
    
    // Pharaoh's Incense Jar - a ceremonial container for burning sacred incense
    public static final Block PHARAOHS_INCENSE_JAR = new PharaohsIncenseJarBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_GRAY)
            .strength(1.5f)
            .sounds(BlockSoundGroup.STONE)
            .nonOpaque()
            .luminance(state -> 4) // Soft glow from the incense
    );

    // Pistia Stratiotes - water lettuce, floats on water
    public static final Block PISTIA_STRATIOTES = new PistiaStratiotesBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.DARK_GREEN)
            .strength(0.0f)
            .sounds(BlockSoundGroup.WET_GRASS)
            .nonOpaque()
            .noCollision()
            .breakInstantly()
            .notSolid()
    );

    // Offering Pot - decorative vase used in ancient Egyptian rituals
    public static final Block OFFERING_POT = new OfferingPotBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_GRAY)
            .strength(1.5f)
            .sounds(BlockSoundGroup.STONE)
            .nonOpaque()
            .requiresTool()
    );
    
    // Clay Crucible - a special furnace for ancient metalworking
    public static final Block CLAY_CRUCIBLE = new ClayCrucibleBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_LIGHT_GRAY)
            .strength(1.25f)
            .luminance(state -> state.get(ClayCrucibleBlock.LIT) ? 13 : 0) // Glow when lit
            .sounds(BlockSoundGroup.STONE)
            .nonOpaque()
    );
    
    // Vessel of Whispering Winds - a mystical pottery item that glows
    public static final Block VESSEL_OF_WHISPERING_WINDS = new VesselOfWhisperingWindsBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_GRAY)
            .strength(1.5f)
            .sounds(BlockSoundGroup.STONE)
            .nonOpaque()
            .luminance(state -> 8) // Constant light level of 8
    );
    
    // Black stone variants
    public static final Block BLACK_COBBLESTONE = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.BLACK)
            .strength(2.0f, 6.0f)
            .requiresTool()
            .sounds(BlockSoundGroup.STONE)
    );
    
    public static final Block BLACK_DUST = new SandBlock(
        0x202020, // Dark color value for black dust
        FabricBlockSettings.create()
            .mapColor(MapColor.BLACK)
            .strength(0.5f)
            .sounds(BlockSoundGroup.SAND)
    );
    
    public static final Block BLACK_SAND = new SandBlock(
        0x252525, // Dark color value for black sand
        FabricBlockSettings.create()
            .mapColor(MapColor.BLACK)
            .strength(0.5f)
            .sounds(BlockSoundGroup.SAND)
    );
    
    public static final Block BLACK_STONE = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.BLACK)
            .strength(1.5f, 6.0f)
            .requiresTool()
            .sounds(BlockSoundGroup.STONE)
    );
    
    public static final Block BLACKSTONE_BRICK = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.BLACK)
            .strength(1.5f, 6.0f)
            .requiresTool()
            .sounds(BlockSoundGroup.STONE)
    );
    
    public static final Block HARDENED_BLACK_STONE = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.BLACK)
            .strength(2.0f, 7.0f) // Slightly harder than regular black stone
            .requiresTool()
            .sounds(BlockSoundGroup.DEEPSLATE)
    );
    
    // Wind Swept Blackstone - worn, smooth black stone
    public static final Block WIND_SWEPT_BLACKSTONE = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.BLACK)
            .strength(1.5f, 6.0f)
            .requiresTool()
            .sounds(BlockSoundGroup.STONE)
    );
    
    // Bronze Blocks
    public static final Block BRONZE_BLOCK = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_ORANGE)
            .strength(3.0f, 6.0f)
            .requiresTool()
            .sounds(BlockSoundGroup.METAL)
    );
    
    public static final Block BRONZE_PLATE = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_ORANGE)
            .strength(2.5f, 6.0f)
            .requiresTool()
            .sounds(BlockSoundGroup.METAL)
            .nonOpaque()
    );
    
    public static final Block CHISELED_BRONZE = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_ORANGE)
            .strength(3.0f, 6.0f)
            .requiresTool()
            .sounds(BlockSoundGroup.METAL)
    );
    
    public static final Block BRONZE_GRATE = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_ORANGE)
            .strength(2.5f, 6.0f)
            .requiresTool()
            .sounds(BlockSoundGroup.METAL)
            .nonOpaque()
    );

    // Declare the Anubus Glyph block
    public static final Block ANUBUS_GLYPH_BLOCK = new AnubusGlyphBlock(
            FabricBlockSettings.create()
                    .mapColor(MapColor.GRAY) // You can change this to any color you'd like
                    .strength(3.0f, 6.0f) // Adjust strength as needed
                    .requiresTool()
                    .sounds(BlockSoundGroup.STONE) // Adjust sound group if needed
    );

    /**
     * Registers all mod blocks
     */
    public static void registerBlocks() {
        AncientCurse.LOGGER.info("Registering blocks for " + AncientCurse.MOD_ID);
        
        // Register the sycamore fig log block
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "sycamore_fig_log"),
            SYCAMORE_FIG_LOG
        );


        // Register the sycamore leaves block
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "sycamore_leaves"),
            SYCAMORE_LEAVES
        );

        // Register the Anubus Glyph block
        Registry.register(
                Registries.BLOCK,
                new Identifier(AncientCurse.MOD_ID, "anubus__glyph"),
                ANUBUS_GLYPH_BLOCK
        );

        // Register the Anubus Glyph block item
        Registry.register(
                Registries.ITEM,
                new Identifier(AncientCurse.MOD_ID, "anubus__glyph"),
                new BlockItem(ANUBUS_GLYPH_BLOCK, new Item.Settings())
        );
        
        // Register the date palm log block
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "date_palm_log"),
            DATE_PALM_LOG
        );
        
        // Register Date Palm Leaves
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "date_palm_leaves"),
            DATE_PALM_LEAVES
        );
        
        // Register Date Block (fruit)
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "date_block"),
            DATE_BLOCK
        );
        
        // Register Smooth Sand
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "smooth_sand"),
            SMOOTH_SAND
        );
        
        // Register Nile River Sand
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "nile_river_sand"),
            NILE_RIVER_SAND
        );
        
        // Register Fertile Nile Silt
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "fertile_nile_silt"),
            FERTILE_NILE_SILT
        );
        
        // Register Dry Nile Silt
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "dry_nile_silt"),
            DRY_NILE_SILT
        );
        
        // Register Tilled Nile Silt
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "tilled_nile_silt"),
            TILLED_NILE_SILT
        );
        
        // Register Arid Nile Turf
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "arid_nile_turf"),
            ARID_NILE_TURF
        );
        
        // Register Dead Papyrus Reed
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "dead_papyrus_reed"),
            DEAD_PAPYRUS_REED
        );
        
        // Register Riverbed
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "riverbed"),
            RIVERBED
        );

        // Register Heavy Marsh
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "heavy_marsh"),
            HEAVY_MARSH
        );

        // Register Riverbed Moss (renamed from Riverbed Algae)
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "riverbed_moss"),
            RIVERBED_MOSS
        );

        // Register new Algae block
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "algae"),
            ALGAE
        );

        // Register Nile Mud
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "nile_mud"),
            NILE_MUD
        );

        // Register Gold-Flaked Riverbed
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "gold_flaked_river_bed"),
            GOLD_FLAKED_RIVER_BED
        );

        // Register Mud Flat
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "mud_flat"),
            MUD_FLAT
        );

        // Register Salt Bed
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "salt_bed"),
            SALT_BED
        );
        
        // Register Dried Reed Thatch
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "dried_reed_thatch"),
            DRIED_REED_THATCH
        );
        
        // Register Riverbed Clay
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "riverbed_clay"),
            RIVERBED_CLAY
        );
        
        // Register Obelisk Stone
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "obelisk_stone"),
            OBELISK_STONE
        );
        
        // Register Mud Brick
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "mud_brick"),
            MUD_BRICK
        );
        
        // Register Light Nile Marsh
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "light_nile_marsh"),
            LIGHT_NILE_MARSH
        );
        
        // Register Reed Mat
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "reed_mat"),
            REED_MAT
        );
        
        // Register Sunbaked Clay
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "sunbaked_clay"),
            SUNBAKED_CLAY
        );
        
        // Register Spotted Marsh
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "spotted_marsh"),
            SPOTTED_MARSH
        );
        
        // Register Papyrus Reed
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "papyrus_reed"),
            PAPYRUS_REED
        );
        
        // Register Flax crop
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "flax"),
            FLAX
        );
        
        // Register Barley crop
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "barley"),
            BARLEY
        );
        
        // Register Lotus Flower Pad
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "lotus_flower_pad"),
            LOTUS_FLOWER_PAD
        );
        
        // Register Nile River Grass
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "nile_river_grass"),
            NILE_RIVER_GRASS
        );
        
        // Register Nile River Tall Grass
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "nile_river_tall_grass"),
            NILE_RIVER_TALL_GRASS
        );
        
        // Register Deshret Brick
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "deshret_brick"),
            DESHRET_BRICK
        );
        
        // Register Deshret Cobblestone
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "deshret_cobblestone"),
            DESHRET_COBBLESTONE
        );
        
        // Register Deshret Sand
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "deshret_sand"),
            DESHRET_SAND
        );
        
        // Register Deshret Sandstone
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "deshret_sandstone"),
            DESHRET_SANDSTONE
        );
        
        // Register Deshret Wavy Sand
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "deshret_wavy_sand"),
            DESHRET_WAVY_SAND
        );
        
        // Register Hardened Deshret Stone
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "hardened_deshret_stone"),
            HARDENED_DESHRET_STONE
        );
        
        // Register Polished Deshret Stone
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "polished_deshret_stone"),
            POLISHED_DESHRET_STONE
        );
        
        // Register Spotted Deshret
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "spotted_deshret"),
            SPOTTED_DESHRET
        );
        
        // Register Small Rock
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "small_rock"),
            SMALL_ROCK
        );
        
        // Register Medium Rock
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "medium_rock"),
            MEDIUM_ROCK
        );
        
        // Register Large Rock
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "large_rock"),
            LARGE_ROCK
        );
        
        // Register Dwarf Papyrus
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "dwarf_papyrus"),
            DWARF_PAPYRUS
        );
        
        // Register Egyptian Spinach
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "egyptian_spinach"),
            EGYPTIAN_SPINACH
        );
        
        // Register Euphorbia Helioscopia
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "euphorbia_helioscopia"),
            EUPHORBIA_HELIOSCOPIA
        );
        
        // Register Light Dead Fern
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "light_dead_fern"),
            LIGHT_DEAD_FERN
        );
        
        // Register Mini Cactus
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "mini_cactus"),
            MINI_CACTUS
        );
        
        // Register Pistia Stratiotes
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "pistia_stratiotes"),
            PISTIA_STRATIOTES
        );
        
        // Register Offering Pot
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "offering_pot"),
            OFFERING_POT
        );
        
        // Register Clay Crucible
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "clay_crucible"),
            CLAY_CRUCIBLE
        );
        
        // Register Vessel of Whispering Winds
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "vessel_of_whispering_winds"),
            VESSEL_OF_WHISPERING_WINDS
        );
        
        // Register Canopic Urn of Bastet
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "canopic_urn_of_bastet"),
            CANOPIC_URN_OF_BASTET
        );
        
        // Register Scarab Sealed Urn
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "scarab_sealed_urn"),
            SCARAB_SEALED_URN
        );
        
        // Register Pharaoh's Incense Jar
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "pharaohs_incense_jar"),
            PHARAOHS_INCENSE_JAR
        );
        
        // Register Black Cobblestone
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "black_cobblestone"),
            BLACK_COBBLESTONE
        );
        
        // Register Black Dust
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "black_dust"),
            BLACK_DUST
        );
        
        // Register Black Sand
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "black_sand"),
            BLACK_SAND
        );
        
        // Register Black Stone
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "black_stone"),
            BLACK_STONE
        );
        
        // Register Blackstone Brick
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "blackstone_brick"),
            BLACKSTONE_BRICK
        );
        
        // Register Hardened Black Stone
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "hardened_black_stone"),
            HARDENED_BLACK_STONE
        );
        
        // Register Wind Swept Blackstone
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "wind_swept_blackstone"),
            WIND_SWEPT_BLACKSTONE
        );
        
        // Register Bronze Blocks
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "bronze_block"),
            BRONZE_BLOCK
        );
        
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "bronze_plate"),
            BRONZE_PLATE
        );
        
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "chiseled_bronze"),
            CHISELED_BRONZE
        );
        
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "bronze_grate"),
            BRONZE_GRATE
        );
        
        // Register block items
        registerBlockItems();
    }
    
    /**
     * Registers all block items
     */
    private static void registerBlockItems() {
        AncientCurse.LOGGER.info("Registering block items for " + AncientCurse.MOD_ID);
        
        // Register normal blocks
        registerBlockItem(SYCAMORE_FIG_LOG, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(SYCAMORE_LEAVES, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(DATE_PALM_LOG, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(DATE_PALM_LEAVES, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(SMOOTH_SAND, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(NILE_RIVER_SAND, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(FERTILE_NILE_SILT, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(DRY_NILE_SILT, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(TILLED_NILE_SILT, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(ARID_NILE_TURF, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(DEAD_PAPYRUS_REED, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(RIVERBED, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(HEAVY_MARSH, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(RIVERBED_MOSS, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(NILE_MUD, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(GOLD_FLAKED_RIVER_BED, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(MUD_FLAT, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(SALT_BED, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(DRIED_REED_THATCH, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(RIVERBED_CLAY, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(OBELISK_STONE, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(MUD_BRICK, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(LIGHT_NILE_MARSH, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(REED_MAT, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(SUNBAKED_CLAY, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(SPOTTED_MARSH, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(PAPYRUS_REED, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(FLAX, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(LOTUS_FLOWER_PAD, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(NILE_RIVER_GRASS, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(NILE_RIVER_TALL_GRASS, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(DESHRET_BRICK, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(DESHRET_COBBLESTONE, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(DESHRET_SAND, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(DESHRET_SANDSTONE, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(DESHRET_WAVY_SAND, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(HARDENED_DESHRET_STONE, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(POLISHED_DESHRET_STONE, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(SPOTTED_DESHRET, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(SMALL_ROCK, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(MEDIUM_ROCK, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(LARGE_ROCK, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(DWARF_PAPYRUS, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(EGYPTIAN_SPINACH, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(EUPHORBIA_HELIOSCOPIA, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(LIGHT_DEAD_FERN, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(MINI_CACTUS, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(PISTIA_STRATIOTES, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(OFFERING_POT, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(CLAY_CRUCIBLE, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(VESSEL_OF_WHISPERING_WINDS, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(CANOPIC_URN_OF_BASTET, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(SCARAB_SEALED_URN, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(PHARAOHS_INCENSE_JAR, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(BLACK_COBBLESTONE, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(BLACK_DUST, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(BLACK_SAND, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(BLACK_STONE, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(BLACKSTONE_BRICK, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(HARDENED_BLACK_STONE, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(WIND_SWEPT_BLACKSTONE, ModItemGroup.ANCIENT_CURSE);
        
        // Register Bronze Blocks
        registerBlockItem(BRONZE_BLOCK, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(BRONZE_PLATE, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(CHISELED_BRONZE, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(BRONZE_GRATE, ModItemGroup.ANCIENT_CURSE);
        
        // We don't register a BlockItem for DATE_BLOCK since it should only drop the Sekhem Date item
    }

    private static void registerBlockItem(Block block, ItemGroup group) {
        // Use Registry.register instead of Item.register to prevent duplicate registration
        Registry.register(Registries.ITEM, 
            new Identifier(AncientCurse.MOD_ID, Registries.BLOCK.getId(block).getPath()),
            new BlockItem(block, new FabricItemSettings()));
    }
}
