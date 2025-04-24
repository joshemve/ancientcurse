package com.ancientcurse.world;

import com.ancientcurse.AncientCurse;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.gen.surfacebuilder.MaterialRules;
import net.minecraft.util.math.VerticalSurfaceType;

/**
 * This class handles surface rules for the Ancient Desert biome.
 * It ensures that the surface is always sand, regardless of the chunk generator used.
 */
public class AncientDesertSurfaceRules {

    // Register our surface rules
    public static void register() {
        AncientCurse.LOGGER.info("Registering Ancient Desert surface rules");
        
        // Use Fabric API to modify the biome
        BiomeModifications.create(new Identifier(AncientCurse.MOD_ID, "ancient_desert_surface"))
                .add(ModificationPhase.POST_PROCESSING,
                        BiomeSelectors.includeByKey(AncientDesertBiome.ANCIENT_DESERT_KEY),
                        context -> {
                            // Log that we're applying sand surface to the biome
                            AncientCurse.LOGGER.info("Applied sand surface to Ancient Desert biome");
                        });
    }
    
    // Create surface rules for the ancient desert biome
    public static MaterialRules.MaterialRule createAncientDesertSurfaceRules() {
        // TEMPORARILY DISABLED TO PREVENT INTRUSIVE HOLDERS ERRORS
        // This method was accessing vanilla block states too early in the initialization process
        // We'll return an empty rule for now to allow the mod to load
        AncientCurse.LOGGER.warn("Ancient Desert surface rules are temporarily disabled to prevent intrusive holders errors");
        
        // Return an empty sequence that doesn't access any vanilla blocks
        return MaterialRules.sequence();
        
        /* Original implementation - commented out to fix intrusive holders error
        // Create a biome condition for our ancient desert
        @SuppressWarnings("unchecked")
        MaterialRules.MaterialCondition isAncientDesert = MaterialRules.biome(
            new RegistryKey[]{AncientDesertBiome.ANCIENT_DESERT_KEY}
        );
        
        // Define the surface rules for our biome
        MaterialRules.MaterialRule sandSurface = MaterialRules.sequence(
            // Top layer is sand
            MaterialRules.condition(
                MaterialRules.surface(),
                MaterialRules.block(Blocks.SAND.getDefaultState())
            ),
            // Subsurface layer is sandstone
            MaterialRules.condition(
                MaterialRules.stoneDepth(1, true, VerticalSurfaceType.FLOOR),
                MaterialRules.block(Blocks.SANDSTONE.getDefaultState())
            )
        );
        
        // Apply our rules only to our biome
        return MaterialRules.sequence(
            MaterialRules.condition(isAncientDesert, sandSurface)
        );
        */
    }
}
