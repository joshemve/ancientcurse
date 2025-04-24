package com.ancientcurse.block.registry;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.block.ScrollShelfBlock;
import com.ancientcurse.block.WoodenCrateBlock;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/**
 * Registry for all furniture-related blocks
 */
public class FurnitureBlocks {
    // Furniture blocks - initialize immediately to prevent intrusive holders errors
    public static final Block SCROLL_SHELF = new ScrollShelfBlock(FabricBlockSettings.copyOf(Blocks.OAK_PLANKS)
        .strength(2.0f, 3.0f)
        .sounds(BlockSoundGroup.WOOD));
        
    public static final Block WOODEN_CRATE = new WoodenCrateBlock(FabricBlockSettings.copyOf(Blocks.OAK_PLANKS)
        .strength(2.0f, 3.0f)
        .sounds(BlockSoundGroup.WOOD));
        
    public static final Block ANCIENT_WOOD = new Block(FabricBlockSettings.copyOf(Blocks.OAK_PLANKS)
        .strength(2.0f, 3.0f)
        .sounds(BlockSoundGroup.WOOD));
        
    public static final Block ANCIENT_UDJAT = new Block(FabricBlockSettings.copyOf(Blocks.STONE)
        .strength(2.5f, 6.0f)
        .sounds(BlockSoundGroup.STONE)
        .requiresTool());

    /**
     * Registers all furniture blocks
     */
    public static void registerBlocks() {
        AncientCurse.LOGGER.info("Registering furniture blocks for " + AncientCurse.MOD_ID);
        
        // Register furniture blocks - blocks are already initialized at declaration
        Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, "scroll_shelf"), SCROLL_SHELF);
        Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, "wooden_crate"), WOODEN_CRATE);
        Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, "ancient_wood"), ANCIENT_WOOD);
        Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, "ancient_udjat"), ANCIENT_UDJAT);
    }
    
    /**
     * Registers all furniture block items
     */
    public static void registerBlockItems() {
        AncientCurse.LOGGER.info("Registering furniture block items for " + AncientCurse.MOD_ID);
        
        // Register furniture block items
        Registry.register(Registries.ITEM, new Identifier(AncientCurse.MOD_ID, "scroll_shelf"), new net.minecraft.item.BlockItem(SCROLL_SHELF, new net.fabricmc.fabric.api.item.v1.FabricItemSettings()));
        Registry.register(Registries.ITEM, new Identifier(AncientCurse.MOD_ID, "wooden_crate"), new net.minecraft.item.BlockItem(WOODEN_CRATE, new net.fabricmc.fabric.api.item.v1.FabricItemSettings()));
        Registry.register(Registries.ITEM, new Identifier(AncientCurse.MOD_ID, "ancient_wood"), new net.minecraft.item.BlockItem(ANCIENT_WOOD, new net.fabricmc.fabric.api.item.v1.FabricItemSettings()));
        Registry.register(Registries.ITEM, new Identifier(AncientCurse.MOD_ID, "ancient_udjat"), new net.minecraft.item.BlockItem(ANCIENT_UDJAT, new net.fabricmc.fabric.api.item.v1.FabricItemSettings()));
    }
}
