package com.ancientcurse.block.registry;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.block.PillarBlock;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/**
 * Registry for all pillar-related blocks
 */
public class PillarBlocks {
    // Pillar blocks - initialize immediately to prevent intrusive holders errors
    public static final Block DESHRET_PILLAR = new PillarBlock(FabricBlockSettings.copyOf(Blocks.STONE)
        .strength(2.0f, 6.0f)
        .sounds(BlockSoundGroup.STONE)
        .requiresTool());
        
    public static final Block EGYPTIAN_GOLD_PILLAR = new PillarBlock(FabricBlockSettings.copyOf(Blocks.GOLD_BLOCK)
        .strength(3.0f, 6.0f)
        .sounds(BlockSoundGroup.METAL)
        .requiresTool());
        
    public static final Block MUDSTONE_PILLAR = new PillarBlock(FabricBlockSettings.copyOf(Blocks.STONE)
        .strength(1.8f, 5.0f)
        .sounds(BlockSoundGroup.STONE)
        .requiresTool());
        
    public static final Block STACKED_PILLAR = new PillarBlock(FabricBlockSettings.copyOf(Blocks.STONE)
        .strength(2.2f, 6.0f)
        .sounds(BlockSoundGroup.STONE)
        .requiresTool());

    /**
     * Registers all pillar blocks
     */
    public static void registerBlocks() {
        AncientCurse.LOGGER.info("Registering pillar blocks for " + AncientCurse.MOD_ID);
        
        // Register pillar blocks - blocks are already initialized at declaration
        Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, "deshret_pillar"), DESHRET_PILLAR);
        Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, "egyptian_gold_pillar"), EGYPTIAN_GOLD_PILLAR);
        Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, "mudstone_pillar"), MUDSTONE_PILLAR);
        Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, "stacked_pillar"), STACKED_PILLAR);
    }
    
    /**
     * Registers all pillar block items
     */
    public static void registerBlockItems() {
        AncientCurse.LOGGER.info("Registering pillar block items for " + AncientCurse.MOD_ID);
        
        // Register pillar block items
        Registry.register(Registries.ITEM, new Identifier(AncientCurse.MOD_ID, "deshret_pillar"), new net.minecraft.item.BlockItem(DESHRET_PILLAR, new net.fabricmc.fabric.api.item.v1.FabricItemSettings()));
        Registry.register(Registries.ITEM, new Identifier(AncientCurse.MOD_ID, "egyptian_gold_pillar"), new net.minecraft.item.BlockItem(EGYPTIAN_GOLD_PILLAR, new net.fabricmc.fabric.api.item.v1.FabricItemSettings()));
        Registry.register(Registries.ITEM, new Identifier(AncientCurse.MOD_ID, "mudstone_pillar"), new net.minecraft.item.BlockItem(MUDSTONE_PILLAR, new net.fabricmc.fabric.api.item.v1.FabricItemSettings()));
        Registry.register(Registries.ITEM, new Identifier(AncientCurse.MOD_ID, "stacked_pillar"), new net.minecraft.item.BlockItem(STACKED_PILLAR, new net.fabricmc.fabric.api.item.v1.FabricItemSettings()));
    }
}
