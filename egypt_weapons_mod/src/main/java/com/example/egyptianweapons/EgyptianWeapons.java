package com.example.egyptianweapons;

import com.example.egyptianweapons.registry.ModelScanner;
import com.example.egyptianweapons.registry.EntityRegistry;
import com.example.egyptianweapons.registry.ParticleRegistry;
import com.example.egyptianweapons.registry.ItemRegistry;
import com.example.egyptianweapons.effect.TornadoManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EgyptianWeapons implements ModInitializer {
    public static final String MOD_ID = "ancient_curse";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // Register entities
        EntityRegistry.registerEntities();
        
        // Register particles
        ParticleRegistry.registerParticles();
        
        // Register custom items (now includes all manually registered items)
        ItemRegistry.registerItems();
        
        // Register server tick event for tornado management
        ServerTickEvents.END_SERVER_TICK.register(server -> TornadoManager.tick(server));
        
        LOGGER.info("Ancient Curse initialized");
    }
}
