package com.ancientcurse.block.registry;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.block.BloodshadeThicketBlock;
import com.ancientcurse.block.CursedSprigBlock;
import com.ancientcurse.block.CursedSproutBlock;
import com.ancientcurse.block.DuamutefCapBlock;
import com.ancientcurse.block.DuatFernBlock;
import com.ancientcurse.block.IsfetFrondBlock;
import com.ancientcurse.block.IsfetShrubBlock;
import com.ancientcurse.block.KhemnuPodBlock;
import com.ancientcurse.block.KheruMossBlock;
import com.ancientcurse.block.MenfetSprigBlock;
import com.ancientcurse.block.ReedOfSekhemBlock;
import com.ancientcurse.block.SutekhCoilBlock;
import com.ancientcurse.block.VineOfApepBlock;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/**
 * Registry class for all cursed plant blocks
 * 
 * IMPORTANT USAGE GUIDELINES:
 * 1. All cursed plant blocks MUST be registered here, not in ModBlocks.java
 * 2. To add a new cursed plant block:
 *    - Define it as a public static final field in this class
 *    - Add it to the registerBlocks() method
 *    - Add it to the registerBlockItems() method
 *    - Add it to the ModItemGroup class to make it appear in the creative menu
 * 3. All cursed plant blocks should extend CursedPlantBlock or a subclass of it
 * 4. Reference blocks from this class using the full path: com.ancientcurse.block.registry.CursedPlantBlocks.BLOCK_NAME
 * 
 * This registry was created to organize blocks by type and prevent registration conflicts.
 */
public class CursedPlantBlocks {
    // Cursed plant blocks
    public static final Block CURSED_SPRIG = new CursedSprigBlock(
        FabricBlockSettings.copyOf(Blocks.GRASS)
            .nonOpaque()
            .noCollision()
            .breakInstantly()
            .sounds(BlockSoundGroup.GRASS)
            .luminance(2) // Slight glow
    );
    
    public static final Block CURSED_SPROUT = new CursedSproutBlock(
        FabricBlockSettings.copyOf(Blocks.GRASS)
            .nonOpaque()
            .noCollision()
            .breakInstantly()
            .sounds(BlockSoundGroup.GRASS)
            .luminance(1) // Very slight glow
    );
    
    public static final Block DUAT_FERN = new DuatFernBlock(
        FabricBlockSettings.copyOf(Blocks.GRASS)
            .nonOpaque()
            .noCollision()
            .breakInstantly()
            .sounds(BlockSoundGroup.GRASS)
            .luminance(3) // Moderate glow
    );
    
    public static final Block VINE_OF_APEP = new VineOfApepBlock(
        FabricBlockSettings.copyOf(Blocks.GRASS)
            .nonOpaque()
            .noCollision()
            .breakInstantly()
            .sounds(BlockSoundGroup.GRASS)
            .luminance(2) // Slight glow
    );
    
    public static final Block BLOODSHADE_THICKET = new BloodshadeThicketBlock(
        FabricBlockSettings.copyOf(Blocks.GRASS)
            .nonOpaque()
            .noCollision()
            .breakInstantly()
            .sounds(BlockSoundGroup.GRASS)
            .luminance(4) // Stronger glow
    );
    
    // Additional cursed plant blocks (moved from EgyptianPlantBlocks)
    public static final Block DUAMUTEF_CAP = new DuamutefCapBlock(
        FabricBlockSettings.copyOf(Blocks.GRASS)
            .nonOpaque()
            .noCollision()
            .breakInstantly()
            .sounds(BlockSoundGroup.GRASS)
            .luminance(2) // Added glow effect for cursed plants
    );
    
    public static final Block ISFET_FROND = new IsfetFrondBlock(
        FabricBlockSettings.copyOf(Blocks.GRASS)
            .nonOpaque()
            .noCollision()
            .breakInstantly()
            .sounds(BlockSoundGroup.GRASS)
            .luminance(1) // Added glow effect for cursed plants
    );
    
    public static final Block ISFET_SHRUB = new IsfetShrubBlock(
        FabricBlockSettings.copyOf(Blocks.GRASS)
            .nonOpaque()
            .noCollision()
            .breakInstantly()
            .sounds(BlockSoundGroup.GRASS)
            .luminance(2) // Added glow effect for cursed plants
    );
    
    public static final Block KHEMNU_POD = new KhemnuPodBlock(
        FabricBlockSettings.copyOf(Blocks.GRASS)
            .nonOpaque()
            .noCollision()
            .breakInstantly()
            .sounds(BlockSoundGroup.GRASS)
            .luminance(3) // Added glow effect for cursed plants
    );
    
    public static final Block KHERU_MOSS = new KheruMossBlock(
        FabricBlockSettings.copyOf(Blocks.GRASS)
            .nonOpaque()
            .noCollision()
            .breakInstantly()
            .sounds(BlockSoundGroup.GRASS)
            .luminance(2) // Added glow effect for cursed plants
    );
    
    public static final Block MENFET_SPRIG = new MenfetSprigBlock(
        FabricBlockSettings.copyOf(Blocks.GRASS)
            .nonOpaque()
            .noCollision()
            .breakInstantly()
            .sounds(BlockSoundGroup.GRASS)
            .luminance(1) // Added glow effect for cursed plants
    );
    
    public static final Block REED_OF_SEKHEM = new ReedOfSekhemBlock(
        FabricBlockSettings.copyOf(Blocks.GRASS)
            .nonOpaque()
            .noCollision()
            .breakInstantly()
            .sounds(BlockSoundGroup.GRASS)
            .luminance(2) // Added glow effect for cursed plants
    );
    
    public static final Block SUTEKH_COIL = new SutekhCoilBlock(
        FabricBlockSettings.copyOf(Blocks.GRASS)
            .nonOpaque()
            .noCollision()
            .breakInstantly()
            .sounds(BlockSoundGroup.GRASS)
            .luminance(3) // Added glow effect for cursed plants
    );
    
    /**
     * Register all cursed plant blocks
     */
    public static void registerBlocks() {
        AncientCurse.LOGGER.info("Registering cursed plant blocks for " + AncientCurse.MOD_ID);
        
        // Register original cursed plant blocks
        register(CURSED_SPRIG, "cursed_sprig");
        register(CURSED_SPROUT, "cursed_sprout");
        register(DUAT_FERN, "duat_fern");
        register(VINE_OF_APEP, "vine_of_apep");
        register(BLOODSHADE_THICKET, "bloodshade_thicket");
        
        // Register additional cursed plant blocks (moved from EgyptianPlantBlocks)
        register(DUAMUTEF_CAP, "duamutef_cap");
        register(ISFET_FROND, "isfet_frond");
        register(ISFET_SHRUB, "isfet_shrub");
        register(KHEMNU_POD, "khemnu_pod");
        register(KHERU_MOSS, "kheru_moss");
        register(MENFET_SPRIG, "menfet_sprig");
        register(REED_OF_SEKHEM, "reed_of_sekhem");
        register(SUTEKH_COIL, "sutekh_coil");
    }
    
    /**
     * Register all cursed plant block items
     */
    public static void registerBlockItems() {
        AncientCurse.LOGGER.info("Registering cursed plant block items for " + AncientCurse.MOD_ID);
        
        // Register original cursed plant block items
        registerBlockItem(CURSED_SPRIG, "cursed_sprig");
        registerBlockItem(CURSED_SPROUT, "cursed_sprout");
        registerBlockItem(DUAT_FERN, "duat_fern");
        registerBlockItem(VINE_OF_APEP, "vine_of_apep");
        registerBlockItem(BLOODSHADE_THICKET, "bloodshade_thicket");
        
        // Register additional cursed plant block items (moved from EgyptianPlantBlocks)
        registerBlockItem(DUAMUTEF_CAP, "duamutef_cap");
        registerBlockItem(ISFET_FROND, "isfet_frond");
        registerBlockItem(ISFET_SHRUB, "isfet_shrub");
        registerBlockItem(KHEMNU_POD, "khemnu_pod");
        registerBlockItem(KHERU_MOSS, "kheru_moss");
        registerBlockItem(MENFET_SPRIG, "menfet_sprig");
        registerBlockItem(REED_OF_SEKHEM, "reed_of_sekhem");
        registerBlockItem(SUTEKH_COIL, "sutekh_coil");
    }
    
    /**
     * Helper method to register a block with a specific identifier
     */
    private static void register(Block block, String path) {
        Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, path), block);
    }
    
    /**
     * Helper method to register a block item with a specific identifier
     */
    private static void registerBlockItem(Block block, String path) {
        Identifier id = new Identifier(AncientCurse.MOD_ID, path);
        Registry.register(Registries.ITEM, id, new net.minecraft.item.BlockItem(block, new net.fabricmc.fabric.api.item.v1.FabricItemSettings()));
    }
}
