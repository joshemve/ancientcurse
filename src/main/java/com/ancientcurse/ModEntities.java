package com.ancientcurse;

import com.ancientcurse.entity.AnubisEntity;
import com.ancientcurse.entity.DjeserhathEntity;
import com.ancientcurse.entity.SnakeHeadProjectileEntity;
import com.ancientcurse.entity.SpitBallEntity;
import com.ancientcurse.entity.WitheredPharaohEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Centralizes entity registration for the mod
 */
public class ModEntities {
    
        // Entity type declarations
    public static final EntityType<WitheredPharaohEntity> WITHERED_PHARAOH = Registry.register(
        Registries.ENTITY_TYPE,
        new Identifier(AncientCurse.MOD_ID, "withered_pharaoh"),
        FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, WitheredPharaohEntity::new)
            .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
            .trackRangeBlocks(64)
            .build()
    );
    
    public static final EntityType<DjeserhathEntity> DJESERHATH = Registry.register(
        Registries.ENTITY_TYPE,
        new Identifier(AncientCurse.MOD_ID, "djeserhath"),
        FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, DjeserhathEntity::new)
            .dimensions(EntityDimensions.fixed(1.0f, 2.0f))
            .trackRangeBlocks(64)
            .build()
    );
    
    // Register the spit ball projectile
    public static final EntityType<SpitBallEntity> SPIT_BALL = Registry.register(
        Registries.ENTITY_TYPE,
        new Identifier(AncientCurse.MOD_ID, "spit_ball"),
        FabricEntityTypeBuilder.<SpitBallEntity>create(SpawnGroup.MISC, SpitBallEntity::new)
            .dimensions(EntityDimensions.fixed(0.5f, 0.5f))
            .trackRangeBlocks(64)
            .trackedUpdateRate(10) // Update more frequently for smooth movement
            .build()
    );
    
    // Register the snake head projectile
    public static final EntityType<SnakeHeadProjectileEntity> SNAKE_HEAD_PROJECTILE = Registry.register(
        Registries.ENTITY_TYPE,
        new Identifier(AncientCurse.MOD_ID, "snake_head_projectile"),
        FabricEntityTypeBuilder.<SnakeHeadProjectileEntity>create(SpawnGroup.MISC, SnakeHeadProjectileEntity::new)
            .dimensions(EntityDimensions.fixed(0.5f, 0.5f))
            .trackRangeBlocks(64)
            .trackedUpdateRate(10) // Update more frequently for smooth movement
            .build()
    );
    
    // Register the Anubis boss entity
    public static final EntityType<AnubisEntity> ANUBIS = Registry.register(
        Registries.ENTITY_TYPE,
        new Identifier(AncientCurse.MOD_ID, "anubis"),
        FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, AnubisEntity::new)
            .dimensions(EntityDimensions.fixed(1.2f, 2.5f)) // Boss size
            .trackRangeBlocks(128) // Larger tracking range for boss
            .build()
    );
    
    /**
     * Registers all mod entities
     */
    public static void registerEntities() {
        AncientCurse.LOGGER.info("Registering entities for " + AncientCurse.MOD_ID);
        
        // Register entity attributes
        FabricDefaultAttributeRegistry.register(WITHERED_PHARAOH, WitheredPharaohEntity.createWitheredPharaohAttributes());
        FabricDefaultAttributeRegistry.register(DJESERHATH, DjeserhathEntity.createDjeserhathAttributes());
        FabricDefaultAttributeRegistry.register(ANUBIS, AnubisEntity.createAnubisAttributes());
    }
    
    /**
     * Registers client-side entity renderers
     * This should be called from the client initializer
     */
    public static void registerEntityRenderers() {
        // Client-side renderer registration is done in AncientCurseClient
    }
}