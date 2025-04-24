package com.ancientcurse.block.registry;

import com.ancientcurse.AncientCurse;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/**
 * Registry for all headstone-related blocks
 */
public class HeadstoneBlocks {
    // Headstone blocks - initialize immediately to prevent intrusive holders errors
    public static final Block BLUE_HEAD_STONE = new Block(FabricBlockSettings.copyOf(Blocks.STONE)
        .strength(2.0f, 6.0f)
        .sounds(BlockSoundGroup.STONE)
        .requiresTool());
        
    public static final Block GREEN_HEAD_STONE = new Block(FabricBlockSettings.copyOf(Blocks.STONE)
        .strength(2.0f, 6.0f)
        .sounds(BlockSoundGroup.STONE)
        .requiresTool());
        
    public static final Block GOLD_HEADSTONE = new Block(FabricBlockSettings.copyOf(Blocks.GOLD_BLOCK)
        .strength(3.0f, 6.0f)
        .sounds(BlockSoundGroup.METAL)
        .requiresTool());

    /**
     * Registers all headstone blocks
     */
    public static void registerBlocks() {
        AncientCurse.LOGGER.info("Registering headstone blocks for " + AncientCurse.MOD_ID);
        
        // Register headstone blocks - blocks are already initialized at declaration
        Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, "blue_head_stone"), BLUE_HEAD_STONE);
        Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, "green_head_stone"), GREEN_HEAD_STONE);
        Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, "gold_headstone"), GOLD_HEADSTONE);
    }
    
    /**
     * Registers all headstone block items
     */
    public static void registerBlockItems() {
        AncientCurse.LOGGER.info("Registering headstone block items for " + AncientCurse.MOD_ID);
        
        // Register headstone block items
        Registry.register(Registries.ITEM, new Identifier(AncientCurse.MOD_ID, "blue_head_stone"), new net.minecraft.item.BlockItem(BLUE_HEAD_STONE, new net.fabricmc.fabric.api.item.v1.FabricItemSettings()));
        Registry.register(Registries.ITEM, new Identifier(AncientCurse.MOD_ID, "green_head_stone"), new net.minecraft.item.BlockItem(GREEN_HEAD_STONE, new net.fabricmc.fabric.api.item.v1.FabricItemSettings()));
        Registry.register(Registries.ITEM, new Identifier(AncientCurse.MOD_ID, "gold_headstone"), new net.minecraft.item.BlockItem(GOLD_HEADSTONE, new net.fabricmc.fabric.api.item.v1.FabricItemSettings()));
    }
}
