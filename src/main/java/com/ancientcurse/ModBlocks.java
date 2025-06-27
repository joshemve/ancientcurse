package com.ancientcurse;

import com.ancientcurse.block.*;
// PotteryBlocks is referenced in comments only, no import needed
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.block.SandBlock;
import net.minecraft.item.BlockItem;
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
    public static final Block SMOOTH_SAND = new SmoothSandBlock(
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
        FabricBlockSettings.copyOf(Blocks.GRASS)
            .nonOpaque()
            .noCollision()
            .breakInstantly()
            .sounds(BlockSoundGroup.GRASS)
            .luminance(0)
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
            .notSolid()
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
        FabricBlockSettings.copyOf(Blocks.GRASS)
            .nonOpaque()
            .noCollision()
            .breakInstantly()
            .sounds(BlockSoundGroup.GRASS)
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
            .strength(0.4f)
            .sounds(BlockSoundGroup.WOOL)
            .nonOpaque()
            .breakInstantly()
    );
    
    // POTTERY BLOCKS MOVED TO com.ancientcurse.block.registry.PotteryBlocks
    // This includes:
    // - CANOPIC_URN_OF_BASTET
    // - SCARAB_SEALED_URN
    // - PHARAOHS_INCENSE_JAR

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

    // OFFERING_POT moved to com.ancientcurse.block.registry.PotteryBlocks
    // Access it via PotteryBlocks.OFFERING_POT
    
    // Clay Crucible - a special furnace for ancient metalworking
    public static final Block CLAY_CRUCIBLE = new ClayCrucibleBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_LIGHT_GRAY)
            .strength(1.25f)
            .luminance(state -> state.get(ClayCrucibleBlock.LIT) ? 13 : 0) // Glow when lit
            .sounds(BlockSoundGroup.STONE)
            .nonOpaque()
    );
    
    // VESSEL_OF_WHISPERING_WINDS moved to com.ancientcurse.block.registry.PotteryBlocks
    // Access it via PotteryBlocks.VESSEL_OF_WHISPERING_WINDS
    
    
    // SERPENT_VESSEL_OF_WADJET moved to com.ancientcurse.block.registry.PotteryBlocks
    // Access it via PotteryBlocks.SERPENT_VESSEL_OF_WADJET
    
    // ALL POTTERY BLOCKS MOVED TO com.ancientcurse.block.registry.PotteryBlocks
    
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
    
    public static final Block CURSED_EARTH = new CursedEarthBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_PURPLE)
            .strength(0.5f)
            .sounds(BlockSoundGroup.GRAVEL)
            .ticksRandomly() // Enable random ticks for effects
            .luminance(state -> 2) // Slight glow effect
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
    
    // New cursed plant blocks
    public static final Block BLOODSHADE_THICKET = new BloodshadeThicketBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.DARK_RED)
            .strength(0.4f)
            .sounds(BlockSoundGroup.GRASS)
            .nonOpaque()
            .noCollision()
            .luminance(state -> 2) // Slight glow
    );
    
    public static final Block CURSED_SPRIG = new CursedSprigBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_PURPLE)
            .strength(0.2f)
            .sounds(BlockSoundGroup.GRASS)
            .nonOpaque()
            .noCollision()
    );
    
    public static final Block CURSED_SPROUT = new CursedSproutBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_PURPLE)
            .strength(0.2f)
            .sounds(BlockSoundGroup.GRASS)
            .nonOpaque()
            .noCollision()
    );
    
    public static final Block DUAT_FERN = new DuatFernBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_BLUE)
            .strength(0.3f)
            .sounds(BlockSoundGroup.GRASS)
            .nonOpaque()
            .noCollision()
            .luminance(state -> 3) // Mystical glow
    );
    
    public static final Block VINE_OF_APEP = new VineOfApepBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.DARK_GREEN)
            .strength(0.4f)
            .sounds(BlockSoundGroup.GRASS)
            .nonOpaque()
            .noCollision()
    );
    
    // New Egyptian-themed plant blocks
    public static final Block DUAMUTEF_CAP = new DuamutefCapBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_ORANGE)
            .strength(0.3f)
            .sounds(BlockSoundGroup.GRASS)
            .nonOpaque()
            .noCollision()
    );
    
    public static final Block ISFET_FROND = new IsfetFrondBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.PALE_GREEN)
            .strength(0.3f)
            .sounds(BlockSoundGroup.GRASS)
            .nonOpaque()
            .noCollision()
    );
    
    public static final Block ISFET_SHRUB = new IsfetShrubBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.PALE_GREEN)
            .strength(0.4f)
            .sounds(BlockSoundGroup.GRASS)
            .nonOpaque()
            .noCollision()
    );
    
    public static final Block KHEMNU_POD = new KhemnuPodBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_YELLOW)
            .strength(0.3f)
            .sounds(BlockSoundGroup.GRASS)
            .nonOpaque()
            .noCollision()
            .luminance(state -> 7) // Glowing pod
    );
    
    public static final Block KHERU_MOSS = new KheruMossBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.DARK_GREEN)
            .strength(0.2f)
            .sounds(BlockSoundGroup.GRASS)
            .nonOpaque()
            .noCollision()
    );
    
    public static final Block MENFET_SPRIG = new MenfetSprigBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_BLUE)
            .strength(0.2f)
            .sounds(BlockSoundGroup.GRASS)
            .nonOpaque()
            .noCollision()
    );
    
    public static final Block REED_OF_SEKHEM = new ReedOfSekhemBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.PALE_YELLOW)
            .strength(0.4f)
            .sounds(BlockSoundGroup.GRASS)
            .nonOpaque()
            .noCollision()
    );
    
    public static final Block SUTEKH_COIL = new SutekhCoilBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_RED)
            .strength(0.4f)
            .sounds(BlockSoundGroup.GRASS)
            .nonOpaque()
            .noCollision()
    );
    
    // REMOVED: Anubus Glyph block was causing registration conflicts
    // public static final Block ANUBUS_GLYPH_BLOCK = new AnubusGlyphBlock(...);

    /**
     * Registers all mod blocks
     */
    public static void registerBlocks() {
        AncientCurse.LOGGER.info("Registering blocks for " + AncientCurse.MOD_ID);
        
        // Clear any existing registrations to prevent conflicts
        AncientCurse.LOGGER.info("Registering all blocks for " + AncientCurse.MOD_ID);
        
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

        // REMOVED: Anubus Glyph block was causing registration conflicts
        // Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, "anubus_glyph"), ANUBUS_GLYPH_BLOCK);
        // Registry.register(Registries.ITEM, new Identifier(AncientCurse.MOD_ID, "anubus_glyph"), new BlockItem(...));
        
        
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
        
        // OFFERING_POT registration moved to com.ancientcurse.block.registry.PotteryBlocks
        
        // Register Clay Crucible
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "clay_crucible"),
            CLAY_CRUCIBLE
        );
        
        // POTTERY BLOCKS REGISTRATION MOVED TO com.ancientcurse.block.registry.PotteryBlocks
        
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
        
        // Register Cursed Earth
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "cursed_earth"),
            CURSED_EARTH
        );
        
        // Register Bronze Blocks
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "bronze_block"),
            BRONZE_BLOCK
        );
        
        // Egyptian-themed plant blocks are now registered in EgyptianPlantBlocks registry class
        
        // Register all block items
        registerBlockItems();
        
        // Ensure all blocks are properly registered
        AncientCurse.LOGGER.info("Validating block registrations...");
        
        // This will help identify any blocks that might be causing issues
        // Validate all blocks to ensure they're properly registered
        validateAndFixBlockRegistrations();
    }
    
    /**
     * Validates all block registrations and fixes any issues
     */
    private static void validateAndFixBlockRegistrations() {
        // Check Egyptian plant blocks
        Block[] egyptianPlantBlocks = new Block[] {
            DUAMUTEF_CAP, ISFET_FROND, ISFET_SHRUB, KHEMNU_POD, 
            KHERU_MOSS, MENFET_SPRIG, REED_OF_SEKHEM, SUTEKH_COIL
        };
        
        for (Block block : egyptianPlantBlocks) {
            validateBlockRegistration(block);
        }
        
        // Check cursed plant blocks
        Block[] cursedPlantBlocks = new Block[] {
            CURSED_SPRIG, CURSED_SPROUT, BLOODSHADE_THICKET, DUAT_FERN, VINE_OF_APEP
        };
        
        for (Block block : cursedPlantBlocks) {
            validateBlockRegistration(block);
        }
    }
    
    /**
     * Validates a block registration and fixes it if needed
     */
    private static void validateBlockRegistration(Block block) {
        if (block == null || block == Blocks.AIR) {
            return;
        }
        
        Identifier id = Registries.BLOCK.getId(block);
        if (id == null || id.equals(new Identifier("minecraft:air"))) {
            // Block not properly registered, register it now
            String path = block.getClass().getSimpleName().toLowerCase().replace("block", "");
            Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, path), block);
            AncientCurse.LOGGER.info("Fixed registration for block: " + path);
        } else {
            AncientCurse.LOGGER.info("Block registration check: " + id);
        }
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
        // OFFERING_POT moved to PotteryBlocks
        registerBlockItem(CLAY_CRUCIBLE, ModItemGroup.ANCIENT_CURSE);
        
        // POTTERY BLOCK ITEMS REGISTRATION MOVED TO com.ancientcurse.block.registry.PotteryBlocks
        registerBlockItem(BLACK_COBBLESTONE, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(BLACK_DUST, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(BLACK_SAND, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(BLACK_STONE, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(BLACKSTONE_BRICK, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(HARDENED_BLACK_STONE, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(WIND_SWEPT_BLACKSTONE, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(CURSED_EARTH, ModItemGroup.ANCIENT_CURSE);
        
        // Register Bronze Blocks
        registerBlockItem(BRONZE_BLOCK, ModItemGroup.ANCIENT_CURSE);
        
        // Egyptian-themed plant blocks and cursed plant blocks are now registered in their respective registry classes
        
        // We don't register a BlockItem for DATE_BLOCK since it should only drop the Sekhem Date item
        // We don't register Anubus Glyph here because it's registered directly in registerBlocks to avoid ID conflicts
    }

    private static void registerBlockItem(Block block, ItemGroup group) {
        // Skip registration for null blocks or air blocks
        if (block == null || block == Blocks.AIR) {
            return;
        }
        
        // First, ensure the block itself is properly registered
        validateBlockRegistration(block);
        
        // Now get the block ID (which should be valid after validation)
        Identifier blockId = Registries.BLOCK.getId(block);
        if (blockId == null || blockId.equals(new Identifier("minecraft:air"))) {
            AncientCurse.LOGGER.error("Failed to register item for block: " + block.getClass().getSimpleName());
            return;
        }
        
        // Create the item ID based on the block ID
        Identifier itemId = new Identifier(AncientCurse.MOD_ID, blockId.getPath());
        
        // Check if the item is already registered to prevent conflicts
        if (Registries.ITEM.containsId(itemId)) {
            // Skip registration if item already exists
            System.out.println("Skipping duplicate item registration for: " + itemId);
            return;
        }
        
        // Register the item
        Registry.register(Registries.ITEM, itemId, new BlockItem(block, new FabricItemSettings()));
        AncientCurse.LOGGER.info("Registered item for block: " + blockId);
    }
}
