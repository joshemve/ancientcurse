package com.ancientcurse.block.registry;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.ModBlocks;
import com.ancientcurse.ModItemGroup;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
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
    
    // Sandstone variants
    public static final Block SANDSTONE_BRICK_TILES = new Block(
        FabricBlockSettings.copyOf(Blocks.SANDSTONE)
            .strength(0.8f, 0.8f)
            .sounds(BlockSoundGroup.STONE)
            .requiresTool()
    );
    
    // Metal variants
    public static final Block POLISHED_BRONZE = new Block(
        FabricBlockSettings.copyOf(Blocks.IRON_BLOCK)
            .strength(5.0f, 6.0f)
            .sounds(BlockSoundGroup.METAL)
            .requiresTool()
    );
    
    // Wood stairs
    public static final Block SYCAMORE_STAIRS = new StairsBlock(
        ModBlocks.SYCAMORE_PLANKS.getDefaultState(),
        FabricBlockSettings.copyOf(ModBlocks.SYCAMORE_PLANKS)
    );
    
    public static final Block DATE_PALM_STAIRS = new StairsBlock(
        ModBlocks.DATE_PALM_PLANKS.getDefaultState(),
        FabricBlockSettings.copyOf(ModBlocks.DATE_PALM_PLANKS)
    );
    
    // Wood slabs
    public static final Block SYCAMORE_SLAB = new SlabBlock(
        FabricBlockSettings.copyOf(ModBlocks.SYCAMORE_PLANKS)
    );
    
    public static final Block DATE_PALM_SLAB = new SlabBlock(
        FabricBlockSettings.copyOf(ModBlocks.DATE_PALM_PLANKS)
    );
    
    /**
     * Registers all construction blocks to the game registry
     */
    public static void registerBlocks() {
        AncientCurse.LOGGER.info("Registering construction blocks");
        
        // Register sandstone variants
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "sandstone_brick_tiles"),
            SANDSTONE_BRICK_TILES
        );
        
        // Register metal variants
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "polished_bronze"),
            POLISHED_BRONZE
        );
        
        // Register wood stairs
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "sycamore_stairs"),
            SYCAMORE_STAIRS
        );
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "date_palm_stairs"),
            DATE_PALM_STAIRS
        );
        
        // Register wood slabs
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "sycamore_slab"),
            SYCAMORE_SLAB
        );
        Registry.register(
            Registries.BLOCK,
            new Identifier(AncientCurse.MOD_ID, "date_palm_slab"),
            DATE_PALM_SLAB
        );
    }
    
    /**
     * Registers all construction block items to the game registry
     */
    public static void registerBlockItems() {
        AncientCurse.LOGGER.info("Registering construction block items");
        
        // Register sandstone variant items
        registerBlockItem(SANDSTONE_BRICK_TILES, "sandstone_brick_tiles", ModItemGroup.ANCIENT_CURSE);
        
        // Register metal variant items
        registerBlockItem(POLISHED_BRONZE, "polished_bronze", ModItemGroup.ANCIENT_CURSE);
        
        // Register wood stair items
        registerBlockItem(SYCAMORE_STAIRS, "sycamore_stairs", ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(DATE_PALM_STAIRS, "date_palm_stairs", ModItemGroup.ANCIENT_CURSE);
        
        // Register wood slab items
        registerBlockItem(SYCAMORE_SLAB, "sycamore_slab", ModItemGroup.ANCIENT_CURSE);
        registerBlockItem(DATE_PALM_SLAB, "date_palm_slab", ModItemGroup.ANCIENT_CURSE);
    }
    
    /**
     * Helper method to register a block item
     */
    private static void registerBlockItem(Block block, String name, ItemGroup itemGroup) {
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, name),
            new BlockItem(block, new FabricItemSettings())
        );
    }
}