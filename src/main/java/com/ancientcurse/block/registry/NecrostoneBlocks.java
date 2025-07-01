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
 * Registry for all necrostone-related blocks
 */
public class NecrostoneBlocks {
    // Necrostone blocks - initialize immediately to prevent intrusive holders errors
    public static final Block NECROSTONE_BRICK = new Block(FabricBlockSettings.copyOf(Blocks.STONE)
        .strength(2.0f, 6.0f)
        .sounds(BlockSoundGroup.STONE)
        .requiresTool());
        
    public static final Block NECROSTONE_CRACKED = new Block(FabricBlockSettings.copyOf(Blocks.STONE)
        .strength(1.8f, 5.5f)
        .sounds(BlockSoundGroup.STONE)
        .requiresTool());
        
    public static final Block NECROSTONE_ROUGH = new Block(FabricBlockSettings.copyOf(Blocks.STONE)
        .strength(2.2f, 6.5f)
        .sounds(BlockSoundGroup.STONE)
        .requiresTool());
    
    public static final Block NECROSTONE_WALL = new Block(FabricBlockSettings.copyOf(Blocks.STONE)
        .strength(2.0f, 6.0f)
        .sounds(BlockSoundGroup.STONE)
        .requiresTool());
        
    public static final Block NECROSTONE_PILLAR = new Block(FabricBlockSettings.copyOf(Blocks.STONE)
        .strength(2.0f, 6.0f)
        .sounds(BlockSoundGroup.STONE)
        .requiresTool());
        
    public static final Block NECROSTONE_CHISELED = new Block(FabricBlockSettings.copyOf(Blocks.STONE)
        .strength(2.0f, 6.0f)
        .sounds(BlockSoundGroup.STONE)
        .requiresTool());
        
    public static final Block NECROSTONE_STAIRS = new Block(FabricBlockSettings.copyOf(Blocks.STONE)
        .strength(2.0f, 6.0f)
        .sounds(BlockSoundGroup.STONE)
        .requiresTool());
        
    public static final Block NECROSTONE_CHAIN = new Block(FabricBlockSettings.copyOf(Blocks.STONE)
        .strength(2.0f, 6.0f)
        .sounds(BlockSoundGroup.STONE)
        .requiresTool());
        
    public static final Block NECROSTONE_EYE = new Block(FabricBlockSettings.copyOf(Blocks.STONE)
        .strength(2.0f, 6.0f)
        .sounds(BlockSoundGroup.STONE)
        .requiresTool()
        .luminance(4)); // Slight glow for the eye
        
    public static final Block NECROSTONE_SLAB = new Block(FabricBlockSettings.copyOf(Blocks.STONE)
        .strength(2.0f, 6.0f)
        .sounds(BlockSoundGroup.STONE)
        .requiresTool());
        
    public static final Block NECROSTONE_CARVED = new Block(FabricBlockSettings.copyOf(Blocks.STONE)
        .strength(2.0f, 6.0f)
        .sounds(BlockSoundGroup.STONE)
        .requiresTool());
        
    public static final Block NECROSTONE_SMOOTH = new Block(FabricBlockSettings.copyOf(Blocks.STONE)
        .strength(2.0f, 6.0f)
        .sounds(BlockSoundGroup.STONE)
        .requiresTool());
        
    public static final Block NECROSTONE_ANKH = new Block(FabricBlockSettings.copyOf(Blocks.STONE)
        .strength(2.0f, 6.0f)
        .sounds(BlockSoundGroup.STONE)
        .requiresTool());
        
    public static final Block NECROSTONE_FORK = new Block(FabricBlockSettings.copyOf(Blocks.STONE)
        .strength(2.0f, 6.0f)
        .sounds(BlockSoundGroup.STONE)
        .requiresTool());
        
    public static final Block DECORATIVE_NECROSTONE = new Block(FabricBlockSettings.copyOf(Blocks.STONE)
        .strength(2.0f, 6.0f)
        .sounds(BlockSoundGroup.STONE)
        .requiresTool());

    /**
     * Registers all necrostone blocks
     */
    public static void registerBlocks() {
        AncientCurse.LOGGER.info("Registering necrostone blocks for " + AncientCurse.MOD_ID);
        
        // Register all necrostone blocks - blocks are already initialized at declaration
        Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, "necrostone_brick"), NECROSTONE_BRICK);
        Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, "necrostone_cracked"), NECROSTONE_CRACKED);
        Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, "necrostone_rough"), NECROSTONE_ROUGH);
        Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, "necrostone_wall"), NECROSTONE_WALL);
        Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, "necrostone_pillar"), NECROSTONE_PILLAR);
        Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, "necrostone_chiseled"), NECROSTONE_CHISELED);
        Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, "necrostone_stairs"), NECROSTONE_STAIRS);
        Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, "necrostone_chain"), NECROSTONE_CHAIN);
        Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, "necrostone_eye"), NECROSTONE_EYE);
        Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, "necrostone_slab"), NECROSTONE_SLAB);
        Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, "necrostone_carved"), NECROSTONE_CARVED);
        Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, "necrostone_smooth"), NECROSTONE_SMOOTH);
        Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, "necrostone_ankh"), NECROSTONE_ANKH);
        Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, "necrostone_fork"), NECROSTONE_FORK);
        Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, "decorative_necrostone"), DECORATIVE_NECROSTONE);
    }
    
    /**
     * Registers all necrostone block items
     */
    public static void registerBlockItems() {
        AncientCurse.LOGGER.info("Registering necrostone block items for " + AncientCurse.MOD_ID);
        
        // Register necrostone block items
        Registry.register(Registries.ITEM, new Identifier(AncientCurse.MOD_ID, "necrostone_brick"), new net.minecraft.item.BlockItem(NECROSTONE_BRICK, new net.fabricmc.fabric.api.item.v1.FabricItemSettings()));
        Registry.register(Registries.ITEM, new Identifier(AncientCurse.MOD_ID, "necrostone_cracked"), new net.minecraft.item.BlockItem(NECROSTONE_CRACKED, new net.fabricmc.fabric.api.item.v1.FabricItemSettings()));
        Registry.register(Registries.ITEM, new Identifier(AncientCurse.MOD_ID, "necrostone_rough"), new net.minecraft.item.BlockItem(NECROSTONE_ROUGH, new net.fabricmc.fabric.api.item.v1.FabricItemSettings()));
        Registry.register(Registries.ITEM, new Identifier(AncientCurse.MOD_ID, "necrostone_wall"), new net.minecraft.item.BlockItem(NECROSTONE_WALL, new net.fabricmc.fabric.api.item.v1.FabricItemSettings()));
        Registry.register(Registries.ITEM, new Identifier(AncientCurse.MOD_ID, "necrostone_pillar"), new net.minecraft.item.BlockItem(NECROSTONE_PILLAR, new net.fabricmc.fabric.api.item.v1.FabricItemSettings()));
        Registry.register(Registries.ITEM, new Identifier(AncientCurse.MOD_ID, "necrostone_chiseled"), new net.minecraft.item.BlockItem(NECROSTONE_CHISELED, new net.fabricmc.fabric.api.item.v1.FabricItemSettings()));
        Registry.register(Registries.ITEM, new Identifier(AncientCurse.MOD_ID, "necrostone_stairs"), new net.minecraft.item.BlockItem(NECROSTONE_STAIRS, new net.fabricmc.fabric.api.item.v1.FabricItemSettings()));
        Registry.register(Registries.ITEM, new Identifier(AncientCurse.MOD_ID, "necrostone_chain"), new net.minecraft.item.BlockItem(NECROSTONE_CHAIN, new net.fabricmc.fabric.api.item.v1.FabricItemSettings()));
        Registry.register(Registries.ITEM, new Identifier(AncientCurse.MOD_ID, "necrostone_eye"), new net.minecraft.item.BlockItem(NECROSTONE_EYE, new net.fabricmc.fabric.api.item.v1.FabricItemSettings()));
        Registry.register(Registries.ITEM, new Identifier(AncientCurse.MOD_ID, "necrostone_slab"), new net.minecraft.item.BlockItem(NECROSTONE_SLAB, new net.fabricmc.fabric.api.item.v1.FabricItemSettings()));
        Registry.register(Registries.ITEM, new Identifier(AncientCurse.MOD_ID, "necrostone_carved"), new net.minecraft.item.BlockItem(NECROSTONE_CARVED, new net.fabricmc.fabric.api.item.v1.FabricItemSettings()));
        Registry.register(Registries.ITEM, new Identifier(AncientCurse.MOD_ID, "necrostone_smooth"), new net.minecraft.item.BlockItem(NECROSTONE_SMOOTH, new net.fabricmc.fabric.api.item.v1.FabricItemSettings()));
        Registry.register(Registries.ITEM, new Identifier(AncientCurse.MOD_ID, "necrostone_ankh"), new net.minecraft.item.BlockItem(NECROSTONE_ANKH, new net.fabricmc.fabric.api.item.v1.FabricItemSettings()));
        Registry.register(Registries.ITEM, new Identifier(AncientCurse.MOD_ID, "necrostone_fork"), new net.minecraft.item.BlockItem(NECROSTONE_FORK, new net.fabricmc.fabric.api.item.v1.FabricItemSettings()));
        Registry.register(Registries.ITEM, new Identifier(AncientCurse.MOD_ID, "decorative_necrostone"), new net.minecraft.item.BlockItem(DECORATIVE_NECROSTONE, new net.fabricmc.fabric.api.item.v1.FabricItemSettings()));
    }
}
