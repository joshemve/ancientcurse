package com.ancientcurse;

import com.ancientcurse.block.entity.ClayCrucibleBlockEntity;
import com.ancientcurse.block.entity.SolarSpireBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Centralizes block entity registration for the mod
 */
public class ModBlockEntities {
    // Define block entity types
    public static BlockEntityType<ClayCrucibleBlockEntity> CLAY_CRUCIBLE;
    public static BlockEntityType<SolarSpireBlockEntity> SOLAR_SPIRE;
    
    /**
     * Registers all block entities
     */
    public static void registerBlockEntities() {
        AncientCurse.LOGGER.info("Registering block entities for Ancient Curse");
        
        // Create the block entity type with its block
        CLAY_CRUCIBLE = FabricBlockEntityTypeBuilder.create(
            ClayCrucibleBlockEntity::new, 
            ModBlocks.CLAY_CRUCIBLE
        ).build();
        
        // Register Clay Crucible block entity
        Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(AncientCurse.MOD_ID, "clay_crucible"),
            CLAY_CRUCIBLE
        );
        
        AncientCurse.LOGGER.info("Clay Crucible block entity registered successfully");
        
        // Create the Solar Spire block entity type
        SOLAR_SPIRE = FabricBlockEntityTypeBuilder.create(
            SolarSpireBlockEntity::new,
            ModBlocks.SOLAR_SPIRE
        ).build();
        
        // Register Solar Spire block entity
        Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(AncientCurse.MOD_ID, "solar_spire"),
            SOLAR_SPIRE
        );
        
        AncientCurse.LOGGER.info("Solar Spire block entity registered successfully");
    }
    

}
