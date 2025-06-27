package com.example.egyptianweapons.client;

import com.example.egyptianweapons.registry.ItemRegistry;
import com.example.egyptianweapons.client.render.entity.SnakeHeadProjectileRenderer;
import com.example.egyptianweapons.registry.EntityRegistry;
import com.example.egyptianweapons.client.particle.GlowingRingParticle;
import com.example.egyptianweapons.client.particle.SweepingSlashParticle;
import com.example.egyptianweapons.registry.ParticleRegistry;
import com.example.egyptianweapons.EgyptianWeapons;
import com.example.egyptianweapons.client.render.item.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.GeckoLib;

public class EgyptianWeaponsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Initialize GeckoLib first
        GeckoLib.initialize();
        
        EgyptianWeapons.LOGGER.info("Initializing Ancient Curse Client...");

        // Register entity renderers
        EntityRendererRegistry.register(EntityRegistry.SNAKE_HEAD_PROJECTILE, SnakeHeadProjectileRenderer::new);

        // Register particle factories
        ParticleFactoryRegistry.getInstance().register(ParticleRegistry.SWEEPING_SLASH, SweepingSlashParticle.Factory::new);
        
        // Register item models with our new approach
        try {
            EgyptianWeapons.LOGGER.info("Registering item models...");
            
            // Register Cursed Mace
            ModelRegistry.registerModel(
                ItemRegistry.CURSED_MACE, 
                new HorusMaceRenderer(), 
                "cursed_mace"
            );
            
            // Register Serpent Staff
            ModelRegistry.registerModel(
                ItemRegistry.SERPENT_STAFF, 
                new SerpentStaffRenderer(), 
                "serpent_staff"
            );
            
            // Register War Axe
            ModelRegistry.registerModel(
                ItemRegistry.WAR_AXE, 
                new WarAxeRenderer(), 
                "war_axe"
            );
            
            // Register Soul Orb
            ModelRegistry.registerModel(
                ItemRegistry.SOUL_ORB, 
                new GrowingOrbRenderer(), 
                "soul_orb"
            );
            
            // Register Viper Head
            ModelRegistry.registerModel(
                ItemRegistry.VIPER_HEAD, 
                new SnakeHeadRenderer(), 
                "viper_head"
            );
            
            // Register Staff of Souls
            ModelRegistry.registerModel(
                ItemRegistry.STAFF_OF_SOULS, 
                new StaffOfRaRenderer(), 
                "staff_of_souls"
            );
            
            EgyptianWeapons.LOGGER.info("Item model registration complete!");
        } catch (Exception e) {
            EgyptianWeapons.LOGGER.error("Failed to register item models: " + e.getMessage(), e);
        }
        
        EgyptianWeapons.LOGGER.info("Ancient Curse Client initialization complete!");
    }
}
