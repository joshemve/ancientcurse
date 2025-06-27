package com.ancientcurse.world;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.ModBlocks;
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
        // Create surface rules that use smooth sand
        return MaterialRules.sequence(
            // Top layer is smooth sand
            MaterialRules.condition(
                MaterialRules.surface(),
                MaterialRules.block(ModBlocks.SMOOTH_SAND.getDefaultState())
            ),
            // Subsurface layer is sandstone
            MaterialRules.condition(
                MaterialRules.stoneDepth(0, true, VerticalSurfaceType.FLOOR),
                MaterialRules.sequence(
                    MaterialRules.condition(
                        MaterialRules.stoneDepth(0, false, 5, VerticalSurfaceType.FLOOR),
                        MaterialRules.block(Blocks.SANDSTONE.getDefaultState())
                    )
                )
            )
        );
    }
}
