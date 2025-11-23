package com.ancientcurse;

import com.ancientcurse.entity.AnubisEntity;
import com.ancientcurse.entity.DjeserhathEntity;
import com.ancientcurse.entity.KhamsinSpreadSmallEntity;
import com.ancientcurse.entity.LocusEntity;
import com.ancientcurse.entity.BabyLocusEntity;
import com.ancientcurse.entity.ScarabBeetleEntity;
import com.ancientcurse.entity.SnakeHeadProjectileEntity;
import com.ancientcurse.entity.SpitBallEntity;
import com.ancientcurse.entity.ThothEntity;
import com.ancientcurse.entity.ThothMagicBallEntity;
import com.ancientcurse.entity.KhamsinOrbEntity;
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
            .dimensions(EntityDimensions.fixed(1.5f, 3.0f)) // Larger hitbox for easier targeting
            .trackRangeBlocks(128) // Larger tracking range for boss
            .build()
    );
    
    // Register the Locus entity (correctly named to match asset files)
    public static final EntityType<LocusEntity> LOCUS = Registry.register(
        Registries.ENTITY_TYPE,
        new Identifier(AncientCurse.MOD_ID, "locus"),
        FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, LocusEntity::new)
            .dimensions(EntityDimensions.fixed(0.7f, 0.5f)) // Smaller, more accurate to flying insect
            .trackRangeBlocks(64)
            .trackedUpdateRate(3)
            .build()
    );

    // Register the Baby Locus entity (bug babies)
    public static final EntityType<BabyLocusEntity> BABY_LOCUS = Registry.register(
        Registries.ENTITY_TYPE,
        new Identifier(AncientCurse.MOD_ID, "baby_locus"),
        FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, BabyLocusEntity::new)
            .dimensions(EntityDimensions.fixed(0.35f, 0.25f)) // Half the size of adult
            .trackRangeBlocks(32)
            .trackedUpdateRate(3)
            .build()
    );

    // Register the Scarab Beetle entity (tameable beetle companion)
    public static final EntityType<ScarabBeetleEntity> SCARAB_BEETLE = Registry.register(
        Registries.ENTITY_TYPE,
        new Identifier(AncientCurse.MOD_ID, "scarab_beetle"),
        FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, ScarabBeetleEntity::new)
            .dimensions(EntityDimensions.fixed(1.0f, 0.9f)) // Increased height from 0.6 to 0.9 for easier hitting
            .trackRangeBlocks(48)
            .trackedUpdateRate(3)
            .build()
    );
    
    // Register the Thoth entity (Egyptian God of Wisdom - Boss)
    public static final EntityType<ThothEntity> THOTH = Registry.register(
        Registries.ENTITY_TYPE,
        new Identifier(AncientCurse.MOD_ID, "thoth"),
        FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, ThothEntity::new)
            .dimensions(EntityDimensions.fixed(1.8f, 3.5f)) // Larger hitbox for easier targeting
            .trackRangeBlocks(128) // Large tracking range for boss
            .trackedUpdateRate(1) // Frequent updates for smooth boss movement
            .build()
    );
    
    // Register the Khamsin Spread Small entity (Floating mystical rock - Curse system)
    public static final EntityType<KhamsinSpreadSmallEntity> KHAMSIN_SPREAD_SMALL = Registry.register(
        Registries.ENTITY_TYPE,
        new Identifier(AncientCurse.MOD_ID, "khamsin_spread_small"),
        FabricEntityTypeBuilder.create(SpawnGroup.MISC, KhamsinSpreadSmallEntity::new)
            .dimensions(EntityDimensions.fixed(0.8f, 0.8f)).build());

    public static final EntityType<KhamsinOrbEntity> KHAMSIN_ORB = Registry.register(
        Registries.ENTITY_TYPE,
        new Identifier(AncientCurse.MOD_ID, "khamsin_orb"),
        FabricEntityTypeBuilder.<KhamsinOrbEntity>create(SpawnGroup.MISC, KhamsinOrbEntity::new)
            .dimensions(EntityDimensions.fixed(0.5f, 0.5f))
            .trackRangeBlocks(64)
            .trackedUpdateRate(5) // Smooth projectile movement
            .build());
    
    // Register the Thoth Magic Ball projectile
    public static final EntityType<ThothMagicBallEntity> THOTH_MAGIC_BALL = Registry.register(
        Registries.ENTITY_TYPE,
        new Identifier(AncientCurse.MOD_ID, "thoth_magic_ball"),
        FabricEntityTypeBuilder.<ThothMagicBallEntity>create(SpawnGroup.MISC, ThothMagicBallEntity::new)
            .dimensions(EntityDimensions.fixed(0.5f, 0.5f))
            .trackRangeBlocks(64)
            .trackedUpdateRate(5) // Smooth projectile movement
            .build());
    
    /**
     * Registers all mod entities
     */
    public static void registerEntities() {
        AncientCurse.LOGGER.info("Registering entities for " + AncientCurse.MOD_ID);
        
        // Register entity attributes
        FabricDefaultAttributeRegistry.register(WITHERED_PHARAOH, WitheredPharaohEntity.createWitheredPharaohAttributes());
        FabricDefaultAttributeRegistry.register(DJESERHATH, DjeserhathEntity.createDjeserhathAttributes());
        FabricDefaultAttributeRegistry.register(ANUBIS, AnubisEntity.createAnubisAttributes());
        FabricDefaultAttributeRegistry.register(LOCUS, LocusEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(BABY_LOCUS, BabyLocusEntity.createBabyLocusAttributes());
        FabricDefaultAttributeRegistry.register(SCARAB_BEETLE, ScarabBeetleEntity.createScarabBeetleAttributes());
        FabricDefaultAttributeRegistry.register(THOTH, ThothEntity.createThothAttributes());
        FabricDefaultAttributeRegistry.register(KHAMSIN_SPREAD_SMALL, KhamsinSpreadSmallEntity.createKhamsinSpreadSmallAttributes());

    }
    
    /**
     * Registers client-side entity renderers
     * This should be called from the client initializer
     */
    public static void registerEntityRenderers() {
        // Client-side renderer registration is done in AncientCurseClient
    }
}