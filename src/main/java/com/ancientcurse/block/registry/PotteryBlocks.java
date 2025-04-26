package com.ancientcurse.block.registry;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.block.CanopicUrnOfBastetBlock;
import com.ancientcurse.block.OfferingPotBlock;
import com.ancientcurse.block.PharaohsIncenseJarBlock;
import com.ancientcurse.block.ScarabSealedUrnBlock;
import com.ancientcurse.block.SerpentVesselOfWadjetBlock;
import com.ancientcurse.block.VesselOfWhisperingWindsBlock;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.sound.BlockSoundGroup;

/**
 * Registry for all pottery and vessel blocks in the Ancient Curse mod.
 * This includes decorative pottery, urns, vessels, and other similar items.
 */
public class PotteryBlocks {
    // Offering Pot - a decorative pot that can be used for offerings
    public static final Block OFFERING_POT = new OfferingPotBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_GRAY)
            .strength(1.5f)
            .sounds(BlockSoundGroup.STONE)
            .nonOpaque()
            .requiresTool()
    );
    
    // Vessel of Whispering Winds - a magical vessel that emits a soft glow
    public static final Block VESSEL_OF_WHISPERING_WINDS = new VesselOfWhisperingWindsBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_GRAY)
            .strength(1.5f)
            .sounds(BlockSoundGroup.STONE)
            .nonOpaque()
            .luminance(state -> 8) // Constant light level of 8
    );
    
    // Serpent Vessel of Wadjet - a vessel decorated with serpent imagery
    public static final Block SERPENT_VESSEL_OF_WADJET = new SerpentVesselOfWadjetBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_GRAY)
            .strength(1.5f)
            .sounds(BlockSoundGroup.STONE)
            .nonOpaque()
    );
    
    // Canopic Urn of Bastet - a ceremonial container with glowing eyes
    public static final Block CANOPIC_URN_OF_BASTET = new CanopicUrnOfBastetBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_GRAY)
            .strength(1.5f)
            .sounds(BlockSoundGroup.STONE)
            .nonOpaque()
            .luminance(state -> 5) // Soft glow from the eyes
    );
    
    // Scarab Sealed Urn - a ceremonial container sealed with a sacred scarab
    public static final Block SCARAB_SEALED_URN = new ScarabSealedUrnBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_GRAY)
            .strength(1.5f)
            .sounds(BlockSoundGroup.STONE)
            .nonOpaque()
    );
    
    // Pharaoh's Incense Jar - a ceremonial container for burning sacred incense
    public static final Block PHARAOHS_INCENSE_JAR = new PharaohsIncenseJarBlock(
        FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_GRAY)
            .strength(1.5f)
            .sounds(BlockSoundGroup.STONE)
            .nonOpaque()
            .luminance(state -> 4) // Soft glow from the incense
    );
    
    /**
     * Registers all pottery blocks
     */
    public static void registerBlocks() {
        // We need to register OFFERING_POT here to ensure it's properly referenced
        // Since we've changed the registration order in AncientCurse.java, this will be registered first
        Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, "offering_pot"), OFFERING_POT);
        Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, "vessel_of_whispering_winds"), VESSEL_OF_WHISPERING_WINDS);
        Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, "serpent_vessel_of_wadjet"), SERPENT_VESSEL_OF_WADJET);
        Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, "canopic_urn_of_bastet"), CANOPIC_URN_OF_BASTET);
        Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, "scarab_sealed_urn"), SCARAB_SEALED_URN);
        Registry.register(Registries.BLOCK, new Identifier(AncientCurse.MOD_ID, "pharaohs_incense_jar"), PHARAOHS_INCENSE_JAR);
    }
    
    /**
     * Registers all pottery block items
     */
    public static void registerBlockItems() {
        // Register the OFFERING_POT block item here for consistency
        registerBlockItem(OFFERING_POT, "offering_pot");
        registerBlockItem(VESSEL_OF_WHISPERING_WINDS, "vessel_of_whispering_winds");
        registerBlockItem(SERPENT_VESSEL_OF_WADJET, "serpent_vessel_of_wadjet");
        registerBlockItem(CANOPIC_URN_OF_BASTET, "canopic_urn_of_bastet");
        registerBlockItem(SCARAB_SEALED_URN, "scarab_sealed_urn");
        registerBlockItem(PHARAOHS_INCENSE_JAR, "pharaohs_incense_jar");
    }
    
    /**
     * Helper method to register a block item
     */
    private static void registerBlockItem(Block block, String path) {
        Registry.register(
            Registries.ITEM,
            new Identifier(AncientCurse.MOD_ID, path),
            new BlockItem(block, new FabricItemSettings())
        );
    }
}
