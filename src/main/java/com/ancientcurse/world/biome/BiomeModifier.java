package com.ancientcurse.world.biome;

import com.ancientcurse.AncientCurse;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.GenerationStep;

/**
 * Handles biome modifications for Ancient Curse
 */
public class BiomeModifier {
    
    /**
     * Register biome modifiers
     */
    public static void register() {
        AncientCurse.LOGGER.info("Registering Ancient Curse biome modifiers");
        
        // Remove some water features from desert biomes
        BiomeModifications.create(new Identifier(AncientCurse.MOD_ID, "reduce_desert_water"))
            .add(net.fabricmc.fabric.api.biome.v1.ModificationPhase.REMOVALS,
                BiomeSelectors.includeByKey(BiomeKeys.DESERT),
                context -> {
                    // Remove surface water lakes
                    context.getGenerationSettings().removeFeature(GenerationStep.Feature.LAKES,
                        RegistryKey.of(RegistryKeys.PLACED_FEATURE, 
                            new Identifier("minecraft", "lake_lava_surface")));
                });
    }
}
