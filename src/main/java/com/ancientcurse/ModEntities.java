package com.ancientcurse;

/**
 * Centralizes entity registration for the mod
 */
public class ModEntities {
    
    // Example entity registration (commented out until ready to implement)
    /*
    public static final EntityType<SunGolemEntity> SUN_GOLEM = Registry.register(
        Registries.ENTITY_TYPE,
        new Identifier(AncientCurse.MOD_ID, "sun_golem"),
        FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, SunGolemEntity::new)
            .dimensions(EntityDimensions.fixed(0.9f, 1.8f))
            .build()
    );
    */
    
        // Tentacle entity registrations removed
    
    /**
     * Registers all mod entities
     */
    public static void registerEntities() {
        AncientCurse.LOGGER.info("Registering entities for " + AncientCurse.MOD_ID);
        
        // Tentacle entity registrations removed
        
        // When you're ready to implement the SunGolem entity, uncomment the code above
        // and then uncomment the following code for client-side rendering:
        
        /*
        EntityRendererRegistry.register(SUN_GOLEM, SunGolemRenderer::new);
        */
    }
    
    /**
     * Registers client-side entity renderers
     * This should be called from the client initializer
     */
    public static void registerEntityRenderers() {
        // Tentacle entity renderers removed
    }
}