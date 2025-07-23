package com.ancientcurse.block.registry;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.ModItemGroup;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
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
    }
    
    /**
     * Registers all construction block items to the game registry
     */
    public static void registerBlockItems() {
        AncientCurse.LOGGER.info("Registering construction block items");
        
        // Register sandstone variant items
        registerBlockItem(SANDSTONE_BRICK_TILES, "sandstone_brick_tiles", ModItemGroup.ANCIENT_CURSE);
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