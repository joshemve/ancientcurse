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
 * Registry for all deshret-related blocks (Egyptian red desert materials)
 */
public class DeshretBlocks {
    // Deshret blocks - initialize immediately to prevent intrusive holders errors
    public static final Block DESHRET_SCALE = new Block(FabricBlockSettings.copyOf(Blocks.STONE)
        .strength(1.8f, 5.0f)
        .sounds(BlockSoundGroup.STONE)
        .requiresTool());

    /**
     * Registers all deshret blocks
     */
    public static void registerBlocks() {
        AncientCurse.LOGGER.info("Registering deshret blocks for " + AncientCurse.MOD_ID);
        
        // Register deshret blocks - blocks are already initialized at declaration
        Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, "deshret_scale"), DESHRET_SCALE);
    }
    
    /**
     * Registers all deshret block items
     */
    public static void registerBlockItems() {
        AncientCurse.LOGGER.info("Registering deshret block items for " + AncientCurse.MOD_ID);
        
        // Register deshret block items
        Registry.register(Registries.ITEM, new Identifier(AncientCurse.MOD_ID, "deshret_scale"), new net.minecraft.item.BlockItem(DESHRET_SCALE, new net.fabricmc.fabric.api.item.v1.FabricItemSettings()));
    }
}
