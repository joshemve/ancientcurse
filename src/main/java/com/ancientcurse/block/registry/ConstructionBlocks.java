package com.ancientcurse.block.registry;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.ModBlocks;
import com.ancientcurse.ModItemGroup;
import com.ancientcurse.block.BlackSandPathBlock;
import com.ancientcurse.block.DesertPathBlock;
import com.ancientcurse.block.DeshretPathBlock;
import com.ancientcurse.block.SandPathBlock;
import com.ancientcurse.block.SandstonePathBlock;
import com.ancientcurse.block.custom.PitfallTrapBlock;
import com.ancientcurse.block.custom.SpikeBlock;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/**
 * Registry for construction and building blocks
 * Includes bricks, tiles, and other decorative building materials
 */
public class ConstructionBlocks {

        // Path blocks for desert environments
        public static final Block SAND_PATH = new SandPathBlock(
                        FabricBlockSettings.create()
                                        .mapColor(MapColor.PALE_YELLOW)
                                        .strength(0.65f)
                                        .sounds(BlockSoundGroup.SAND));

        public static final Block SANDSTONE_PATH = new SandstonePathBlock(
                        FabricBlockSettings.create()
                                        .mapColor(MapColor.PALE_YELLOW)
                                        .strength(0.8f)
                                        .sounds(BlockSoundGroup.STONE)
                                        .requiresTool());

        public static final Block DESHRET_PATH = new DeshretPathBlock(
                        FabricBlockSettings.create()
                                        .mapColor(MapColor.TERRACOTTA_RED)
                                        .strength(0.65f)
                                        .sounds(BlockSoundGroup.SAND));

        public static final Block DESERT_PATH = new DesertPathBlock(
                        FabricBlockSettings.create()
                                        .mapColor(MapColor.PALE_YELLOW)
                                        .strength(0.65f)
                                        .sounds(BlockSoundGroup.SAND));

        public static final Block BLACK_SAND_PATH = new BlackSandPathBlock(
                        FabricBlockSettings.create()
                                        .mapColor(MapColor.BLACK)
                                        .strength(0.65f)
                                        .sounds(BlockSoundGroup.SAND));

        // Sandstone variants
        public static final Block SANDSTONE_BRICK_TILES = new Block(
                        FabricBlockSettings.copyOf(Blocks.SANDSTONE)
                                        .strength(0.8f, 0.8f)
                                        .sounds(BlockSoundGroup.STONE)
                                        .requiresTool());

        // Sandstone Bricks variants (from ModBlocks.SANDSTONE_BRICKS)
        public static final Block SANDSTONE_BRICKS_SLAB = new SlabBlock(
                        FabricBlockSettings.copyOf(ModBlocks.SANDSTONE_BRICKS));

        public static final Block SANDSTONE_BRICKS_STAIRS = new StairsBlock(
                        ModBlocks.SANDSTONE_BRICKS.getDefaultState(),
                        FabricBlockSettings.copyOf(ModBlocks.SANDSTONE_BRICKS));

        public static final Block SANDSTONE_BRICKS_WALL = new WallBlock(
                        FabricBlockSettings.copyOf(ModBlocks.SANDSTONE_BRICKS));

        // Sandstone Brick Tiles variants
        public static final Block SANDSTONE_BRICK_TILES_SLAB = new SlabBlock(
                        FabricBlockSettings.copyOf(SANDSTONE_BRICK_TILES));

        public static final Block SANDSTONE_BRICK_TILES_STAIRS = new StairsBlock(
                        SANDSTONE_BRICK_TILES.getDefaultState(),
                        FabricBlockSettings.copyOf(SANDSTONE_BRICK_TILES));

        public static final Block SANDSTONE_BRICK_TILES_WALL = new WallBlock(
                        FabricBlockSettings.copyOf(SANDSTONE_BRICK_TILES));

        // Metal variants
        public static final Block POLISHED_BRONZE = new Block(
                        FabricBlockSettings.copyOf(Blocks.IRON_BLOCK)
                                        .strength(5.0f, 6.0f)
                                        .sounds(BlockSoundGroup.METAL)
                                        .requiresTool());

        // Wood stairs
        public static final Block SYCAMORE_STAIRS = new StairsBlock(
                        ModBlocks.SYCAMORE_PLANKS.getDefaultState(),
                        FabricBlockSettings.copyOf(ModBlocks.SYCAMORE_PLANKS));

        public static final Block DATE_PALM_STAIRS = new StairsBlock(
                        ModBlocks.DATE_PALM_PLANKS.getDefaultState(),
                        FabricBlockSettings.copyOf(ModBlocks.DATE_PALM_PLANKS));

        // Wood slabs
        public static final Block SYCAMORE_SLAB = new SlabBlock(
                        FabricBlockSettings.copyOf(ModBlocks.SYCAMORE_PLANKS));

        public static final Block DATE_PALM_SLAB = new SlabBlock(
                        FabricBlockSettings.copyOf(ModBlocks.DATE_PALM_PLANKS));

        // Traps
        public static final Block PITFALL_TRAP = new PitfallTrapBlock(
                        FabricBlockSettings.create()
                                        .mapColor(MapColor.OAK_TAN)
                                        .strength(0.5f)
                                        .sounds(BlockSoundGroup.SCAFFOLDING)
                                        .nonOpaque() // Required for transparency - allows seeing block below
        );

        // Spike trap - wooden spikes that damage entities
        public static final Block SPIKE = new SpikeBlock(
                        FabricBlockSettings.create()
                                        .mapColor(MapColor.OAK_TAN)
                                        .strength(0.5f)
                                        .sounds(BlockSoundGroup.WOOD)
                                        .nonOpaque() // Required for transparency
                                        .noCollision() // Entities can walk into spikes
        );

        /**
         * Registers all construction blocks to the game registry
         */
        public static void registerBlocks() {
                AncientCurse.LOGGER.info("Registering construction blocks");

                // Register path blocks
                Registry.register(
                                Registries.BLOCK,
                                new Identifier(AncientCurse.MOD_ID, "sand_path"),
                                SAND_PATH);
                Registry.register(
                                Registries.BLOCK,
                                new Identifier(AncientCurse.MOD_ID, "sandstone_path"),
                                SANDSTONE_PATH);
                Registry.register(
                                Registries.BLOCK,
                                new Identifier(AncientCurse.MOD_ID, "deshret_path"),
                                DESHRET_PATH);
                Registry.register(
                                Registries.BLOCK,
                                new Identifier(AncientCurse.MOD_ID, "desert_path"),
                                DESERT_PATH);
                Registry.register(
                                Registries.BLOCK,
                                new Identifier(AncientCurse.MOD_ID, "black_sand_path"),
                                BLACK_SAND_PATH);
                // Register sandstone variants
                Registry.register(
                                Registries.BLOCK,
                                new Identifier(AncientCurse.MOD_ID, "sandstone_brick_tiles"),
                                SANDSTONE_BRICK_TILES);

                // Register sandstone bricks variants
                Registry.register(
                                Registries.BLOCK,
                                new Identifier(AncientCurse.MOD_ID, "sandstone_bricks_slab"),
                                SANDSTONE_BRICKS_SLAB);
                Registry.register(
                                Registries.BLOCK,
                                new Identifier(AncientCurse.MOD_ID, "sandstone_bricks_stairs"),
                                SANDSTONE_BRICKS_STAIRS);
                Registry.register(
                                Registries.BLOCK,
                                new Identifier(AncientCurse.MOD_ID, "sandstone_bricks_wall"),
                                SANDSTONE_BRICKS_WALL);

                // Register sandstone brick tiles variants
                Registry.register(
                                Registries.BLOCK,
                                new Identifier(AncientCurse.MOD_ID, "sandstone_brick_tiles_slab"),
                                SANDSTONE_BRICK_TILES_SLAB);
                Registry.register(
                                Registries.BLOCK,
                                new Identifier(AncientCurse.MOD_ID, "sandstone_brick_tiles_stairs"),
                                SANDSTONE_BRICK_TILES_STAIRS);
                Registry.register(
                                Registries.BLOCK,
                                new Identifier(AncientCurse.MOD_ID, "sandstone_brick_tiles_wall"),
                                SANDSTONE_BRICK_TILES_WALL);

                // Register metal variants
                Registry.register(
                                Registries.BLOCK,
                                new Identifier(AncientCurse.MOD_ID, "polished_bronze"),
                                POLISHED_BRONZE);

                // Register wood stairs
                Registry.register(
                                Registries.BLOCK,
                                new Identifier(AncientCurse.MOD_ID, "sycamore_stairs"),
                                SYCAMORE_STAIRS);
                Registry.register(
                                Registries.BLOCK,
                                new Identifier(AncientCurse.MOD_ID, "date_palm_stairs"),
                                DATE_PALM_STAIRS);

                // Register wood slabs
                Registry.register(
                                Registries.BLOCK,
                                new Identifier(AncientCurse.MOD_ID, "sycamore_slab"),
                                SYCAMORE_SLAB);
                Registry.register(
                                Registries.BLOCK,
                                new Identifier(AncientCurse.MOD_ID, "date_palm_slab"),
                                DATE_PALM_SLAB);

                // Register traps
                Registry.register(
                                Registries.BLOCK,
                                new Identifier(AncientCurse.MOD_ID, "pitfall_trap"),
                                PITFALL_TRAP);
                Registry.register(
                                Registries.BLOCK,
                                new Identifier(AncientCurse.MOD_ID, "spike"),
                                SPIKE);
        }

        /**
         * Registers all construction block items to the game registry
         */
        public static void registerBlockItems() {
                AncientCurse.LOGGER.info("Registering construction block items");

                // Register path block items
                registerBlockItem(SAND_PATH, "sand_path", ModItemGroup.ANCIENT_CURSE);
                registerBlockItem(SANDSTONE_PATH, "sandstone_path", ModItemGroup.ANCIENT_CURSE);
                registerBlockItem(DESHRET_PATH, "deshret_path", ModItemGroup.ANCIENT_CURSE);
                registerBlockItem(DESERT_PATH, "desert_path", ModItemGroup.ANCIENT_CURSE);
                registerBlockItem(BLACK_SAND_PATH, "black_sand_path", ModItemGroup.ANCIENT_CURSE);

                // Register sandstone variant items
                registerBlockItem(SANDSTONE_BRICK_TILES, "sandstone_brick_tiles", ModItemGroup.ANCIENT_CURSE);

                // Register sandstone bricks variant items
                registerBlockItem(SANDSTONE_BRICKS_SLAB, "sandstone_bricks_slab", ModItemGroup.ANCIENT_CURSE);
                registerBlockItem(SANDSTONE_BRICKS_STAIRS, "sandstone_bricks_stairs", ModItemGroup.ANCIENT_CURSE);
                registerBlockItem(SANDSTONE_BRICKS_WALL, "sandstone_bricks_wall", ModItemGroup.ANCIENT_CURSE);

                // Register sandstone brick tiles variant items
                registerBlockItem(SANDSTONE_BRICK_TILES_SLAB, "sandstone_brick_tiles_slab", ModItemGroup.ANCIENT_CURSE);
                registerBlockItem(SANDSTONE_BRICK_TILES_STAIRS, "sandstone_brick_tiles_stairs",
                                ModItemGroup.ANCIENT_CURSE);
                registerBlockItem(SANDSTONE_BRICK_TILES_WALL, "sandstone_brick_tiles_wall", ModItemGroup.ANCIENT_CURSE);

                // Register metal variant items
                registerBlockItem(POLISHED_BRONZE, "polished_bronze", ModItemGroup.ANCIENT_CURSE);

                // Register wood stair items
                registerBlockItem(SYCAMORE_STAIRS, "sycamore_stairs", ModItemGroup.ANCIENT_CURSE);
                registerBlockItem(DATE_PALM_STAIRS, "date_palm_stairs", ModItemGroup.ANCIENT_CURSE);

                // Register wood slab items
                registerBlockItem(SYCAMORE_SLAB, "sycamore_slab", ModItemGroup.ANCIENT_CURSE);
                registerBlockItem(DATE_PALM_SLAB, "date_palm_slab", ModItemGroup.ANCIENT_CURSE);

                // Register trap items
                registerBlockItem(PITFALL_TRAP, "pitfall_trap", ModItemGroup.ANCIENT_CURSE);
                registerBlockItem(SPIKE, "spike", ModItemGroup.ANCIENT_CURSE);
        }

        /**
         * Helper method to register a block item
         */
        private static void registerBlockItem(Block block, String name, ItemGroup itemGroup) {
                Registry.register(
                                Registries.ITEM,
                                new Identifier(AncientCurse.MOD_ID, name),
                                new BlockItem(block, new FabricItemSettings()));
        }
}