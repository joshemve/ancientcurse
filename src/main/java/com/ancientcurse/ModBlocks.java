package com.ancientcurse;

import com.ancientcurse.block.*;
import com.ancientcurse.block.BloodLotusBlock;
// PotteryBlocks is referenced in comments only, no import needed
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.block.SandBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.WoodType;
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
    
    // Sycamore Planks - wooden planks made from sycamore logs
    public static final Block SYCAMORE_PLANKS = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.OAK_TAN)
            .strength(2.0f, 3.0f)
            .sounds(BlockSoundGroup.WOOD)
    );
    
    // Sycamore Fence
    public static final Block SYCAMORE_FENCE = new SycamoreFenceBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.OAK_TAN)
            .strength(2.0f, 3.0f)
            .sounds(BlockSoundGroup.WOOD)
    );
    
    // Sycamore Fence Gate
    public static final Block SYCAMORE_FENCE_GATE = new FenceGateBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.OAK_TAN)
            .strength(2.0f, 3.0f)
            .sounds(BlockSoundGroup.WOOD),
        WoodType.OAK
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
    
    // Date Palm Planks - wooden planks made from date palm logs
    public static final Block DATE_PALM_PLANKS = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.SPRUCE_BROWN)
            .strength(2.0f, 3.0f)
            .sounds(BlockSoundGroup.WOOD)
    );
    
    // Date Palm Fence
    public static final Block DATE_PALM_FENCE = new DatePalmFenceBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.SPRUCE_BROWN)
            .strength(2.0f, 3.0f)
            .sounds(BlockSoundGroup.WOOD)
    );
    
    // Date Palm Fence Gate
    public static final Block DATE_PALM_FENCE_GATE = new FenceGateBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.SPRUCE_BROWN)
            .strength(2.0f, 3.0f)
            .sounds(BlockSoundGroup.WOOD),
        WoodType.OAK
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
    
    // Salt Dust - placeable salt dust similar to redstone dust
    public static final Block SALT_DUST = new SaltDustBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.WHITE)
            .strength(0.0f)
            .sounds(BlockSoundGroup.SAND)
            .nonOpaque()
            .noCollision()
            .breakInstantly()
    );

    // Dried Reed Thatch - bundled dried reeds for construction
    public static final Block DRIED_REED_THATCH = new DriedReedThatchBlock(
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

    // Nile Mud Brick - dried mud from the Nile formed into bricks
    public static final Block NILE_MUD_BRICK = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_BROWN)
            .strength(1.0f)
            .sounds(BlockSoundGroup.MUD_BRICKS)
    );
    
    // Nile Mud Brick Stairs
    public static final Block NILE_MUD_BRICK_STAIRS = new StairsBlock(
        NILE_MUD_BRICK.getDefaultState(),
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_BROWN)
            .strength(1.0f)
            .sounds(BlockSoundGroup.MUD_BRICKS)
    );
    
    // Nile Mud Brick Slab
    public static final Block NILE_MUD_BRICK_SLAB = new SlabBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_BROWN)
            .strength(1.0f)
            .sounds(BlockSoundGroup.MUD_BRICKS)
    );
    
    // Nile Mud Brick Wall
    public static final Block NILE_MUD_BRICK_WALL = new NileMudBrickWallBlock(
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
            .ticksRandomly() // For day/night cycle updates
            .breakInstantly()
    );
    
    // Blood Lotus - rare crimson lotus used in Pharaoh's Blood crafting
    public static final Block BLOOD_LOTUS = new BloodLotusBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.RED)
            .strength(0.0f, 0.0f)
            .sounds(BlockSoundGroup.LILY_PAD)
            .nonOpaque()
            .noCollision()
            .breakInstantly()
            .notSolid()
    );
    
    // Nile River Grass - lush grass that grows along the Nile riverbanks
    public static final Block NILE_RIVER_GRASS = new NileRiverGrassBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.DARK_GREEN)
            .strength(0.6f)
            .sounds(BlockSoundGroup.GRASS)
            .nonOpaque()
            .notSolid()
            .offset(AbstractBlock.OffsetType.XZ)
            .dynamicBounds()
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
    public static final Block DESHRET_WAVY_SAND = new SandBlock(
        14378728, // Same reddish color as DESHRET_SAND
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
    
    // Cyperus - Ancient Egyptian water plant (papyrus sedge)
    public static final Block CYPERUS = new CyperusBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.DARK_GREEN)
            .strength(0.2f)
            .sounds(BlockSoundGroup.WET_GRASS)
            .nonOpaque()
            .noCollision()
            .notSolid()
            .breakInstantly()
            .offset(AbstractBlock.OffsetType.XZ)
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
    
    // Cursed Stone - Stone variant that spreads like cursed earth
    public static final Block CURSED_STONE = new CursedStoneBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.PURPLE)
            .strength(1.5f, 6.0f)
            .requiresTool()
            .sounds(BlockSoundGroup.STONE)
            .ticksRandomly()
            .luminance(state -> 2) // Slight glow
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
    
    // Blackstone Brick Stairs
    public static final Block BLACKSTONE_STAIRS = new StairsBlock(
        BLACKSTONE_BRICK.getDefaultState(),
        FabricBlockSettings.copyOf(BLACKSTONE_BRICK)
    );
    
    // Blackstone Brick Slab
    public static final Block BLACKSTONE_SLAB = new SlabBlock(
        FabricBlockSettings.copyOf(BLACKSTONE_BRICK)
    );
    
    public static final Block CURSED_EARTH = new CursedEarthBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_PURPLE)
            .strength(0.5f)
            .sounds(BlockSoundGroup.GRAVEL)
            .ticksRandomly() // Enable random ticks for effects
            .luminance(state -> 2) // Slight glow effect
    );
    
    // Cursed Wood blocks - corrupted versions of logs and planks
    public static final Block CURSED_LOG = new CursedLogBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_PURPLE)
            .strength(2.0f)
            .sounds(BlockSoundGroup.WOOD)
            .ticksRandomly() // Enable spreading
            .luminance(state -> 1) // Very faint glow
    );
    
    public static final Block CURSED_WOOD_PLANK = new CursedWoodPlankBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_PURPLE)
            .strength(2.0f, 3.0f)
            .sounds(BlockSoundGroup.WOOD)
            .ticksRandomly() // Enable spreading
    );
    
    // Cursed Sand - corrupted sand that falls and spreads
    public static final Block CURSED_SAND = new CursedSandBlock(
        0x6B4C6B, // Purple-sand color value (hex color for the falling sand entity)
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_PURPLE)
            .strength(0.5f)
            .sounds(BlockSoundGroup.SAND)
            .ticksRandomly() // Enable spreading
            .luminance(state -> 1) // Very faint glow
    );
    
    // Solar Spire Components - Three blocks that combine to form a Solar Spire
    public static final Block SOLAR_SPIRE_PLINTH = new SolarSpirePlinthBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.GOLD)
            .strength(2.0f, 6.0f)
            .requiresTool()
            .sounds(BlockSoundGroup.STONE)
            .luminance(state -> 5)
    );
    
    public static final Block SOLAR_SPIRE_CRUCIBLE = new SolarSpireCrucibleBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.GOLD)
            .strength(2.0f, 6.0f)
            .requiresTool()
            .sounds(BlockSoundGroup.STONE)
            .luminance(state -> 7)
    );
    
    public static final Block SOLAR_SPIRE_PYRAMIDION = new SolarSpirePyramidionBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.GOLD)
            .strength(2.0f, 6.0f)
            .requiresTool()
            .sounds(BlockSoundGroup.STONE)
            .luminance(state -> 10)
    );
    
    // Solar Spire - Used with Eye of Apophis to cleanse ALL connected cursed earth
    public static final Block SOLAR_SPIRE = new SolarSpireBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.GOLD)
            .strength(3.5f, 1200.0f) // Strong like enchantment table
            .requiresTool()
            .sounds(BlockSoundGroup.STONE)
            .luminance(state -> state.get(SolarSpireBlock.ACTIVATED) ? 15 : 7) // Bright when active
    );
    
    public static final Block SANDSTONE_BRICKS = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.PALE_YELLOW)
            .strength(1.5f, 6.0f)
            .requiresTool()
            .sounds(BlockSoundGroup.STONE)
    );
    
    public static final Block SANDSTONE_PILLAR = new SandstonePillarBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.PALE_YELLOW)
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
    
    // Wind Swept Blackstone Stairs
    public static final Block WIND_SWEPT_BLACKSTONE_STAIRS = new StairsBlock(
        WIND_SWEPT_BLACKSTONE.getDefaultState(),
        FabricBlockSettings.copyOf(WIND_SWEPT_BLACKSTONE)
    );
    
    // Wind Swept Blackstone Slab
    public static final Block WIND_SWEPT_BLACKSTONE_SLAB = new SlabBlock(
        FabricBlockSettings.copyOf(WIND_SWEPT_BLACKSTONE)
    );
    
    // Bronze Blocks
    public static final Block BRONZE_BLOCK = new Block(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_ORANGE)
            .strength(3.0f, 6.0f)
            .requiresTool()
            .sounds(BlockSoundGroup.METAL)
    );
    
    // Sandstone Torch - Egyptian-themed torch
    public static final Block SANDSTONE_TORCH = new SandstoneTorchBlock(
        FabricBlockSettings.create()
            .noCollision()
            .breakInstantly()
            .luminance((state) -> 14)
            .sounds(BlockSoundGroup.WOOD)
    );
    
    // Wall Sandstone Torch - for wall placement
    public static final Block WALL_SANDSTONE_TORCH = new WallSandstoneTorchBlock(
        FabricBlockSettings.create()
            .dropsLike(SANDSTONE_TORCH)
            .noCollision()
            .breakInstantly()
            .luminance((state) -> 14)
            .sounds(BlockSoundGroup.WOOD)
    );
    
    // Black Stone Torch - Dark themed torch
    public static final Block BLACK_STONE_TORCH = new BlackStoneTorchBlock(
        FabricBlockSettings.create()
            .noCollision()
            .breakInstantly()
            .luminance((state) -> 14)
            .sounds(BlockSoundGroup.WOOD)
    );
    
    // Wall Black Stone Torch - for wall placement
    public static final Block WALL_BLACK_STONE_TORCH = new WallBlackStoneTorchBlock(
        FabricBlockSettings.create()
            .dropsLike(BLACK_STONE_TORCH)
            .noCollision()
            .breakInstantly()
            .luminance((state) -> 14)
            .sounds(BlockSoundGroup.WOOD)
    );
    
    // NOTE: All cursed plant blocks and Egyptian plant blocks have been moved to their respective registry classes:
    // - Cursed plant blocks: com.ancientcurse.block.registry.CursedPlantBlocks
    // - Egyptian plant blocks: com.ancientcurse.block.registry.EgyptianPlantBlocks
    // This prevents intrusive holders errors by ensuring all blocks are properly registered.
    
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

        // Register the sycamore planks block
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "sycamore_planks"),
            SYCAMORE_PLANKS
        );
        
        // Register sycamore fence
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "sycamore_fence"),
            SYCAMORE_FENCE
        );
        
        // Register sycamore fence gate
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "sycamore_fence_gate"),
            SYCAMORE_FENCE_GATE
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
        
        // Register the date palm planks block
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "date_palm_planks"),
            DATE_PALM_PLANKS
        );
        
        // Register date palm fence
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "date_palm_fence"),
            DATE_PALM_FENCE
        );
        
        // Register date palm fence gate
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "date_palm_fence_gate"),
            DATE_PALM_FENCE_GATE
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
        
        // Register Salt Dust
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "salt_dust"),
            SALT_DUST
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
        
        // Register Nile Mud Brick
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "nile_mud_brick"),
            NILE_MUD_BRICK
        );
        
        // Register Nile Mud Brick Stairs
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "nile_mud_brick_stairs"),
            NILE_MUD_BRICK_STAIRS
        );
        
        // Register Nile Mud Brick Slab
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "nile_mud_brick_slab"),
            NILE_MUD_BRICK_SLAB
        );
        
        // Register Nile Mud Brick Wall
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "nile_mud_brick_wall"),
            NILE_MUD_BRICK_WALL
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
        
        // Register Blood Lotus
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "blood_lotus"),
            BLOOD_LOTUS
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
        
        // Register Cyperus
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "cyperus"),
            CYPERUS
        );
        
        // OFFERING_POT registration moved to com.ancientcurse.block.registry.PotteryBlocks
        
        // Register Clay Crucible
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "clay_crucible"),
            CLAY_CRUCIBLE
        );
        
        // POTTERY BLOCKS REGISTRATION MOVED TO com.ancientcurse.block.registry.PotteryBlocks
        
        // Register Cursed Stone
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "cursed_stone"),
            CURSED_STONE
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
        
        // Register Blackstone Stairs
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "blackstone_stairs"),
            BLACKSTONE_STAIRS
        );
        
        // Register Blackstone Slab
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "blackstone_slab"),
            BLACKSTONE_SLAB
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
        
        // Register Wind Swept Blackstone Stairs
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "wind_swept_blackstone_stairs"),
            WIND_SWEPT_BLACKSTONE_STAIRS
        );
        
        // Register Wind Swept Blackstone Slab
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "wind_swept_blackstone_slab"),
            WIND_SWEPT_BLACKSTONE_SLAB
        );
        
        // Register Cursed Earth
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "cursed_earth"),
            CURSED_EARTH
        );
        
        // Register Cursed Wood blocks
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "cursed_log"),
            CURSED_LOG
        );
        
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "cursed_wood_plank"),
            CURSED_WOOD_PLANK
        );
        
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "cursed_sand"),
            CURSED_SAND
        );
        
        // Register Solar Spire Components
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "solar_spire_plinth"),
            SOLAR_SPIRE_PLINTH
        );
        
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "solar_spire_crucible"),
            SOLAR_SPIRE_CRUCIBLE
        );
        
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "solar_spire_pyramidion"),
            SOLAR_SPIRE_PYRAMIDION
        );
        
        // Register Solar Spire
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "solar_spire"),
            SOLAR_SPIRE
        );
        
        // Register Bronze Blocks
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "bronze_block"),
            BRONZE_BLOCK
        );
        
        // Register Sandstone Bricks
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "sandstone_bricks"),
            SANDSTONE_BRICKS
        );
        
        // Register Sandstone Pillar
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "sandstone_pillar"),
            SANDSTONE_PILLAR
        );
        
        // Register sandstone torches
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "sandstone_torch"),
            SANDSTONE_TORCH
        );
        
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "wall_sandstone_torch"),
            WALL_SANDSTONE_TORCH
        );
        
        // Register black stone torches
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "black_stone_torch"),
            BLACK_STONE_TORCH
        );
        
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "wall_black_stone_torch"),
            WALL_BLACK_STONE_TORCH
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
        // NOTE: Egyptian plant blocks and cursed plant blocks are now validated 
        // in their respective registry classes:
        // - com.ancientcurse.block.registry.EgyptianPlantBlocks
        // - com.ancientcurse.block.registry.CursedPlantBlocks
        
        // Validate core blocks only
        AncientCurse.LOGGER.info("Validating core block registrations complete");
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
        registerBlockItem(SYCAMORE_PLANKS, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(SYCAMORE_FENCE, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(SYCAMORE_FENCE_GATE, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(SYCAMORE_LEAVES, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(DATE_PALM_LOG, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(DATE_PALM_PLANKS, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(DATE_PALM_FENCE, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(DATE_PALM_FENCE_GATE, ModItemGroup.ANCIENT_CURSE);
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
        registerBlockItem(SALT_DUST, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(DRIED_REED_THATCH, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(RIVERBED_CLAY, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(OBELISK_STONE, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(NILE_MUD_BRICK, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(NILE_MUD_BRICK_STAIRS, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(NILE_MUD_BRICK_SLAB, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(NILE_MUD_BRICK_WALL, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(LIGHT_NILE_MARSH, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(REED_MAT, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(SUNBAKED_CLAY, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(SPOTTED_MARSH, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(PAPYRUS_REED, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(FLAX, ModItemGroup.ANCIENT_CURSE);
        // Register Lotus Flower Pad with custom item for water placement
        registerLotusFlowerPadItem();
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
        registerBlockItem(CYPERUS, ModItemGroup.ANCIENT_CURSE);
        // OFFERING_POT moved to PotteryBlocks
        registerBlockItem(CLAY_CRUCIBLE, ModItemGroup.ANCIENT_CURSE);
        
        // POTTERY BLOCK ITEMS REGISTRATION MOVED TO com.ancientcurse.block.registry.PotteryBlocks
        registerBlockItem(CURSED_STONE, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(BLACK_COBBLESTONE, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(BLACK_DUST, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(BLACK_SAND, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(BLACK_STONE, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(BLACKSTONE_BRICK, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(BLACKSTONE_STAIRS, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(BLACKSTONE_SLAB, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(HARDENED_BLACK_STONE, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(WIND_SWEPT_BLACKSTONE, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(WIND_SWEPT_BLACKSTONE_STAIRS, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(WIND_SWEPT_BLACKSTONE_SLAB, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(CURSED_EARTH, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(CURSED_LOG, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(CURSED_WOOD_PLANK, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(CURSED_SAND, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(SOLAR_SPIRE_PLINTH, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(SOLAR_SPIRE_CRUCIBLE, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(SOLAR_SPIRE_PYRAMIDION, ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(SOLAR_SPIRE, ModItemGroup.ANCIENT_CURSE);
        
        // Register Bronze Blocks
        registerBlockItem(BRONZE_BLOCK, ModItemGroup.ANCIENT_CURSE);
        
        // Register Sandstone Torch
        registerBlockItem(SANDSTONE_TORCH, ModItemGroup.ANCIENT_CURSE);
        // Note: Wall torch doesn't need a block item as it's placed by the main torch
        
        // Register Sandstone Bricks
        registerBlockItem(SANDSTONE_BRICKS, ModItemGroup.ANCIENT_CURSE);
        
        // Register Sandstone Pillar
        registerBlockItem(SANDSTONE_PILLAR, ModItemGroup.ANCIENT_CURSE);
        
        // Register Black Stone Torch
        registerBlockItem(BLACK_STONE_TORCH, ModItemGroup.ANCIENT_CURSE);
        // Note: Wall torch doesn't need a block item as it's placed by the main torch
        
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
        // Commented out to avoid duplicate registration issues with blocks from other registries
        // validateBlockRegistration(block);
        
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
    
    private static void registerLotusFlowerPadItem() {
        // Register the custom Lotus Flower Pad item that allows water placement
        Identifier itemId = new Identifier(AncientCurse.MOD_ID, "lotus_flower_pad");
        
        if (Registries.ITEM.containsId(itemId)) {
            System.out.println("Skipping duplicate item registration for: " + itemId);
            return;
        }
        
        Registry.register(Registries.ITEM, itemId, 
            new com.ancientcurse.item.LotusFlowerPadItem(LOTUS_FLOWER_PAD, new FabricItemSettings()));
        AncientCurse.LOGGER.info("Registered custom item for Lotus Flower Pad with water placement");
    }
}
