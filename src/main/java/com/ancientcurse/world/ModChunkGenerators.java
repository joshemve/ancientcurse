package com.ancientcurse.world;

import com.ancientcurse.AncientCurse;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Registers custom chunk generators for the mod
 */
public class ModChunkGenerators {
    
    public static void register() {
        AncientCurse.LOGGER.info("Registering Ancient Curse chunk generators");
        
        Registry.register(
            Registries.CHUNK_GENERATOR,
            new Identifier(AncientCurse.MOD_ID, "ancient_desert"),
            AncientDesertChunkGenerator.CODEC
        );
    }
}